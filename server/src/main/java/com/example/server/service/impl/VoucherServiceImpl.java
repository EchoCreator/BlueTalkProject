package com.example.server.service.impl;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.constant.UserVoucherConstant;
import com.example.common.exception.EventEndedException;
import com.example.common.exception.EventYet2StartException;
import com.example.common.exception.SeckillVoucherStockEmpty;
import com.example.common.exception.UserVoucherExist;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.common.utils.UniqueIDGenerator;
import com.example.pojo.entity.SeckillVoucher;
import com.example.pojo.entity.Voucher;
import com.example.pojo.entity.UserVoucher;
import com.example.pojo.vo.VoucherInfoVO;
import com.example.server.mapper.VoucherMapper;
import com.example.server.service.VoucherService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class VoucherServiceImpl implements VoucherService {
    @Autowired
    private VoucherMapper voucherMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UniqueIDGenerator uniqueIDGenerator;
    @Autowired
    private RedissonClient redissonClient;

    /*VoucherServiceImpl一旦初始化完毕，就需要开启线程池执行线程任务*/
    @PostConstruct
    private void init() {
        USER_VOUCHER_EXECUTOR.submit(new UserVoucherHandler());
    }

    // 加载lua脚本
    private static final DefaultRedisScript<Long> PICKUP_VOUCHER_SCRIPT;

    static {
        PICKUP_VOUCHER_SCRIPT = new DefaultRedisScript<>();
        PICKUP_VOUCHER_SCRIPT.setLocation(new ClassPathResource("pickUpVoucher.lua"));
        PICKUP_VOUCHER_SCRIPT.setResultType(Long.class);
    }

    /*阻塞队列（用户领取优惠券后，把领取该优惠券的信息保存在队列中，
    方便异步往数据库中添加同时不妨碍领取优惠券尤其是秒杀优惠券的进程）*/
    private BlockingQueue<UserVoucher> userVoucherTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 线程池（线程池）
    private static final ExecutorService USER_VOUCHER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 线程任务
    private class UserVoucherHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    UserVoucher userVoucher = userVoucherTasks.take();
                    handleUserVoucher(userVoucher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 代理对象（通过代理对象启动方法的事务管理，如事务回滚）
    private VoucherService proxy;

    // 通过线程任务将userVoucher信息保存在数据库中（这里redisson锁其实已经可有可无了，主要是为了兜底，防止lua脚本执行出现问题）
    private void handleUserVoucher(UserVoucher userVoucher) {
        // 获取用户
        Long userId = userVoucher.getUserId();
        // 获取锁对象，给每个用户线程加一把锁，防止用户重复获取优惠券等操作
        RLock lock = redissonClient.getLock(SystemConstant.REDIS_USER_VOUCHER_LOCK_KEY + userId);
        // 获取锁
        boolean isGetLockSuccess = lock.tryLock();
        if (!isGetLockSuccess) {
            return;
        }
        try {
            // 把userVoucher保存在数据库中
            proxy.saveUserVoucher2DB(userVoucher);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<VoucherInfoVO> getVouchers() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_VOUCHER_INFO_KEY;
        List<VoucherInfoVO> voucherInfoVOList = redisUtil.queryListWithCacheBreakdown(key, null, VoucherInfoVO.class, null, this::getVoucherInfoFromDB, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);
        if (voucherInfoVOList == null) {
            throw new RuntimeException();
        }

        // 依次当前用户是否已领取优惠券
        for (int i = 0; i < voucherInfoVOList.size(); i++) {
            VoucherInfoVO voucherInfoVO = voucherInfoVOList.get(i);
            UserVoucher userVoucher = voucherMapper.getUserVoucher(voucherInfoVOList.get(i).getId(), userId);
            int isPickedUp = 0;
            if (userVoucher != null) {
                isPickedUp = 1;
            }
            voucherInfoVO.setIsPickedUp(isPickedUp);
            voucherInfoVOList.set(i, voucherInfoVO);
        }
        return voucherInfoVOList;
    }

    @Override
    public void pickupVoucher(Long voucherId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_VOUCHER_INFO_KEY;
        List<VoucherInfoVO> voucherInfoVOList = redisUtil.queryListWithCacheBreakdown(key, null, VoucherInfoVO.class, null, this::getVoucherInfoFromDB, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);
        if (voucherInfoVOList == null) {
            throw new RuntimeException();
        }

        Integer voucherType = null;
        for (VoucherInfoVO v : voucherInfoVOList) {
            if (v.getId().equals(voucherId)) {
                voucherType = v.getType();
                // 如果是秒杀券，判断是否在秒杀时间内
                if (v.getType() == 1) {
                    if (v.getSeckillBeginTime().isAfter(LocalDateTime.now())) {
                        throw new EventYet2StartException("秒杀活动尚未开始！请耐心等待哦");
                    }
                    if (v.getSeckillEndTime().isBefore(LocalDateTime.now())) {
                        throw new EventEndedException("秒杀活动已经结束！再看看别的优惠吧");
                    }
                }
                break;
            }
        }
        if (voucherType == null) {
            throw new RuntimeException();
        }

        // 执行lua脚本（Collections.emptyList()说明key参数为空）
        Long result = stringRedisTemplate.execute(
                PICKUP_VOUCHER_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), voucherType.toString()
        );

        assert result != null;
        int r = result.intValue();
        // 返回结果为1说明秒杀优惠券库存不足，如果为2说明该用户已有该优惠券
        if (r == 1) {
            throw new SeckillVoucherStockEmpty("该秒杀券已被抢光");
        }
        if (r == 2) {
            throw new UserVoucherExist("您已有该优惠券，不可重复领取！");
        }

        // 为0说明该用户可以正常领取该优惠券，把下单信息储存到阻塞队列
        UserVoucher userVoucher = new UserVoucher();
        Long id = uniqueIDGenerator.generateUniqueID(SystemConstant.REDIS_USER_VOUCHER_KEY);
        userVoucher.setId(id);
        userVoucher.setVoucherId(voucherId);
        userVoucher.setUserId(userId);
        userVoucher.setStatus(UserVoucherConstant.VOUCHER_STATUS_UNUSED);
        userVoucher.setType(voucherType);
        userVoucherTasks.add(userVoucher);

        // 获取代理对象
        proxy = (VoucherService) AopContext.currentProxy();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserVoucher2DB(UserVoucher userVoucher) {
        Long userId = userVoucher.getUserId();
        Long voucherId = userVoucher.getVoucherId();
        // 检查该用户是否已有该优惠券
        UserVoucher existedUserVoucher = voucherMapper.getUserVoucher(voucherId, userId);
        if (existedUserVoucher != null) {
            throw new UserVoucherExist("您已有该优惠券，不可重复领取！");
        }

        // 如果是秒杀优惠券，还需要让其库存减1
        if (userVoucher.getType() == 1) {
            SeckillVoucher seckillVoucher = voucherMapper.getSeckillVoucherById(voucherId);
            if (seckillVoucher.getCurrentStock() < 1) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
            Boolean success = voucherMapper.setSeckillVoucherCurrentStock(voucherId);
            if (!success) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
        }

        voucherMapper.addUserVoucher(userVoucher);
    }

//    @Transactional(rollbackFor = Exception.class)
//    public void saveUserVoucher(Long voucherId, Long userId) {
//        Voucher voucher = voucherMapper.getVoucherById(voucherId);
//        UserVoucher userVoucher = new UserVoucher();
//
//        // 检查该用户是否已有该优惠券
//        UserVoucher existedUserVoucher = voucherMapper.getUserVoucher(voucherId, userId);
//        if (existedUserVoucher != null) {
//            throw new UserVoucherExist("您已有该优惠券，不可重复领取！");
//        }
//
//
//        // 如果是秒杀优惠券，还需要让其库存减1
//        if (voucher.getType() == 1) {
//            SeckillVoucher seckillVoucher = voucherMapper.getSeckillVoucherById(voucherId);
//            if (seckillVoucher.getSeckillBeginTime().isAfter(LocalDateTime.now())) {
//                throw new EventYet2StartException("秒杀活动尚未开始！请耐心等待哦");
//            }
//            if (seckillVoucher.getSeckillEndTime().isBefore(LocalDateTime.now())) {
//                throw new EventEndedException("秒杀活动已经结束！再看看别的优惠吧");
//            }
//            if (seckillVoucher.getCurrentStock() < 1) {
//                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
//            }
//            Boolean success = voucherMapper.setSeckillVoucherCurrentStock(voucherId);
//            if (!success) {
//                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
//            }
//        }
//
//        Long id = uniqueIDGenerator.generateUniqueID(SystemConstant.REDIS_USER_VOUCHER_KEY);
//        userVoucher.setId(id);
//        userVoucher.setVoucherId(voucherId);
//        userVoucher.setUserId(userId);
//        userVoucher.setStatus(VoucherOrderConstant.VOUCHER_STATUS_UNUSED);
//
//        voucherMapper.addUserVoucher(userVoucher);
//    }

    public List<VoucherInfoVO> getVoucherInfoFromDB() {
        List<VoucherInfoVO> voucherInfoVOList = new ArrayList<>();
        List<Voucher> voucherList = voucherMapper.getVouchers();
        for (Voucher voucher : voucherList) {
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            if (voucher.getType() == 1) {
                seckillVoucher = voucherMapper.getSeckillVoucherById(voucher.getId());
            }

            VoucherInfoVO voucherInfoVO = VoucherInfoVO.builder()
                    .id(voucher.getId())
                    .shopId(voucher.getShopId())
                    .title(voucher.getTitle())
                    .subTitle(voucher.getSubTitle())
                    .rules(voucher.getRules())
                    .type(voucher.getType())
                    .status(voucher.getStatus())
                    .stock(seckillVoucher.getStock())
                    .seckillBeginTime(seckillVoucher.getSeckillBeginTime())
                    .seckillEndTime(seckillVoucher.getSeckillBeginTime())
                    .expireBeginTime(voucher.getExpireBeginTime())
                    .expireEndTime(voucher.getExpireEndTime())
                    .build();
            voucherInfoVOList.add(voucherInfoVO);
        }
        return voucherInfoVOList;
    }
}
