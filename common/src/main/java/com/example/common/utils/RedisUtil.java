package com.example.common.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.common.constant.SystemConstant;
import com.example.pojo.vo.RedisDataVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@Data
@AllArgsConstructor
public class RedisUtil {
    @Autowired
    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long expire, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), expire, timeUnit);
    }

    /*设计逻辑过期时间：
    具有逻辑过期时间的数据实际上不会过期，但是一旦超过其逻辑过期时间，
    就说明该数据需要更新了，线程会去数据库请求新的数据
    * 这一步主要是为了解决缓存击穿问题
    （热点key值数据【如首页打折的商品】一旦在redis中过期，会导致大量线程访问数据库，造成数据库瘫痪。
    通过逻辑过期时间和互斥锁，可以让第一个访问到过期数据的线程先返回旧数据并上锁，然后再从数据库访问新数据；这样其他线程
    无法访问数据库也会先返回旧数据。当拿到锁的线程获得新数据后会向redis中更新数据，然后释放互斥锁。）*/
    public void setWithLogicalExpire(String key, Object value, Long expire, TimeUnit timeUnit) {
        RedisDataVO redisDataVO = new RedisDataVO();
        if (value instanceof List) {
            redisDataVO.setDataList((List<Object>) value);
        } else {
            redisDataVO.setData(value);
        }
        redisDataVO.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expire)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisDataVO));
    }

    /*解决缓存穿透（请求数据库没有的数据会在redis中保存空值）*/
    /*Function<Field, T> getDataFromDB中Field是参数，T是返回值；而Supplier<T> getDataFromDBWithoutParam是范围值为T的无参函数*/
    public <T, Field> T queryWithCachePenetration(String keyPrefix, Field field, Class<T> type, Function<Field, T> getDataFromDB, Supplier<T> getDataFromDBWithoutParam, Long expire, TimeUnit timeUnit) {
        String key = (field != null) ? keyPrefix + field : keyPrefix;

        // 从redis中查询缓存
        String jsonStr = stringRedisTemplate.opsForValue().get(key);

        // 若缓存中存在且不是空值和null，则直接返回数据
        if (StrUtil.isNotBlank(jsonStr)) {
            return JSONUtil.toBean(jsonStr, type);
        }

        // 若缓存中存放的是空值，说明数据库中不存在该数据，返回null
        if (jsonStr != null) {
            return null;
        }

        // 若缓存中没有（null），则向数据库中查找
        T data = (field != null) ? getDataFromDB.apply(field) : getDataFromDBWithoutParam.get();

        // 若数据库中不存在，则向redis中保存空值，并且返回null
        if (data == null) {
            this.set(key, "", SystemConstant.REDIS_NULL_EXPIRATION, TimeUnit.MINUTES);
            return null;
        }

        // 若数据库中存在，则将数据保存在redis中并返回数据
        this.set(key, JSONUtil.toJsonStr(data), expire, timeUnit);
        return data;
    }

    public <T, Field> List<T> queryListWithCachePenetration(String keyPrefix, Field field, Class<T> type, Function<Field, List<T>> getDataFromDB, Supplier<List<T>> getDataFromDBWithoutParam, Long expire, TimeUnit timeUnit) {
        String key = (field != null) ? keyPrefix + field : keyPrefix;

        // 从redis中查询缓存
        String jsonStr = stringRedisTemplate.opsForValue().get(key);

        // 若缓存中存在且不是空值和null，则直接返回数据
        if (StrUtil.isNotBlank(jsonStr)) {
            JSONArray jsonArray = JSONUtil.parseArray(jsonStr);
            return JSONUtil.toList(jsonArray, type);
        }

        // 若缓存中存放的是空值，说明数据库中不存在该数据，返回null
        if (jsonStr != null) {
            return null;
        }

        // 若缓存中没有（null），则向数据库中查找
        List<T> data = (field != null) ? getDataFromDB.apply(field) : getDataFromDBWithoutParam.get();

        // 若数据库中不存在，则向redis中保存空值，并且返回null
        if (data == null) {
            this.set(key, "", SystemConstant.REDIS_NULL_EXPIRATION, TimeUnit.MINUTES);
            return null;
        }

        // 若数据库中存在，则将数据保存在redis中并返回数据
        this.set(key, JSONUtil.toJsonStr(data), expire, timeUnit);
        return data;
    }

    /*解决缓存击穿*/
    public <T, Field> T queryWithCacheBreakdown(String keyPrefix, Field field, Class<T> type, Function<Field, T> getDataFromDB, Supplier<T> getDataFromDBWithoutParam, Long expire, TimeUnit timeUnit) {
        String key = (field != null) ? keyPrefix + field : keyPrefix;

        // 从redis中查询数据
        String jsonStr = stringRedisTemplate.opsForValue().get(key);

        // 如果不存在，返回null抛出异常（注：做防止缓存击穿的数据一般是热点数据，这类数据一般会提前添加到缓存中，且只有逻辑过期不存在真正的过期，因此数据库和redis中一定会有该数据，若没有抛出异常报错就行了）
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }

        // 若存在，则判断数据是否逻辑过期
        RedisDataVO redisDataVO = JSONUtil.toBean(jsonStr, RedisDataVO.class);
        /*这一步类似于：T data=(T)redisDataVO.getData();但更加安全
         * 由于JSONUtil.toBean是将Json类型的数据（如JsonObject,String,JsonArray）转化成另一个具体的类，
         * 而redisDataVO.getData()是Object类，因此要转换成Json的对象格式*/
        T data = JSONUtil.toBean((JSONObject) redisDataVO.getData(), type);
        LocalDateTime expireTime = redisDataVO.getExpireTime();

        // 若未过期，则直接返回数据
        if (expireTime.isAfter(LocalDateTime.now())) {
            return data;
        }

        // 若已过期，需要获取互斥锁并缓存重建
        // 尝试获取互斥锁
        String lockKey = (field != null) ? SystemConstant.REDIS_LOCK_KEY + field : SystemConstant.REDIS_LOCK_KEY;
        boolean isGetLockSuccess = tryLock(lockKey);
        // 如果获取互斥锁成功，则开启独立线程去实现缓存重建，自己先返回空数据
        if (isGetLockSuccess) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    T newData = (field != null) ? getDataFromDB.apply(field) : getDataFromDBWithoutParam.get();
                    // 写入redis
                    this.setWithLogicalExpire(key, newData, expire, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放互斥锁
                    unlock(lockKey);
                }
            });
        }
        return data;
    }

    public <T, Field> List<T> queryListWithCacheBreakdown(String keyPrefix, Field field, Class<T> type, Function<Field, List<T>> getDataFromDB, Supplier<List<T>> getDataFromDBWithoutParam, Long expire, TimeUnit timeUnit) {
        String key = (field != null) ? keyPrefix + field : keyPrefix;

        // 从redis中查询数据
        String jsonStr = stringRedisTemplate.opsForValue().get(key);

        // 如果不存在，返回null抛出异常（注：做防止缓存击穿的数据一般是热点数据，这类数据一般会提前添加到缓存中，且只有逻辑过期不存在真正的过期，因此数据库和redis中一定会有该数据，若没有抛出异常报错就行了）
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }

        // 若存在，则判断数据是否逻辑过期
        RedisDataVO redisDataVO = JSONUtil.toBean(jsonStr, RedisDataVO.class);
        /*这一步类似于：T data=(T)redisDataVO.getData();但更加安全
         * 由于JSONUtil.toBean是将Json类型的数据（如JsonObject,String,JsonArray）转化成另一个具体的类，
         * 而redisDataVO.getData()是Object类，因此要转换成Json的对象格式*/
        JSONArray jsonArray = JSONUtil.parseArray(redisDataVO.getDataList());
        List<T> data = JSONUtil.toList(jsonArray, type);
        LocalDateTime expireTime = redisDataVO.getExpireTime();

        // 若未过期，则直接返回数据
        if (expireTime.isAfter(LocalDateTime.now())) {
            return data;
        }

        // 若已过期，需要获取互斥锁并缓存重建
        // 尝试获取互斥锁
        String lockKey = (field != null) ? SystemConstant.REDIS_LOCK_KEY + field : SystemConstant.REDIS_LOCK_KEY;
        boolean isGetLockSuccess = tryLock(lockKey);
        // 如果获取互斥锁成功，则开启独立线程去实现缓存重建，自己先返回空数据
        if (isGetLockSuccess) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    List<T> newData = (field != null) ? getDataFromDB.apply(field) : getDataFromDBWithoutParam.get();
                    // 写入redis
                    this.setWithLogicalExpire(key, newData, expire, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放互斥锁
                    unlock(lockKey);
                }
            });
        }
        return data;
    }

    /*线程池，创建10个线程
    （用于缓存击穿问题中拿到锁的用户线程开辟一条新的线程，
    从数据库中获取新数据更新redis中的信息）*/
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // 获取互斥锁
    private boolean tryLock(String key) {
        // 如果互斥锁空闲，将锁值设置为1，并且至多10s内其他线程都无法拿到这把锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放互斥锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
