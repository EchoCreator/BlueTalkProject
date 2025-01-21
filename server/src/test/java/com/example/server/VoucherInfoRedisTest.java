package com.example.server;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.vo.FollowUserVO;
import com.example.pojo.vo.VoucherInfoVO;
import com.example.server.mapper.VoucherMapper;
import com.example.server.service.impl.BlogServiceImpl;
import com.example.server.service.impl.FollowServiceImpl;
import com.example.server.service.impl.VoucherServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class VoucherInfoRedisTest {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private VoucherServiceImpl voucherServiceImpl;
    @Autowired
    private BlogServiceImpl blogServiceImpl;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FollowServiceImpl followServiceImpl;

    @Test
    public void test() {
        // 注意：通过redis的Stream实现消息队列还需要通过
        // XGROUP CREATE stream_userVouchers group1 0 MKSTREAM
        // 命令生成名为stream_userVouchers的消息队列和名为group1的组

        List<VoucherInfoVO> list = voucherServiceImpl.getVoucherInfoFromDB();
        redisUtil.setWithLogicalExpire(SystemConstant.REDIS_VOUCHER_INFO_KEY, list, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);

        // 把优惠券信息提前写入redis
        for (VoucherInfoVO v : list) {
            if (v.getType() == 1) {
                stringRedisTemplate.opsForValue().set(SystemConstant.REDIS_LUA_VOUCHER_STOCK_KEY + v.getId(), v.getStock().toString());
            }
        }


        // 把‘我’关注的用户的笔记推送给‘我’（还包含时间戳作为标识）
        Long userId = 1L;
        String feedStreamKey = SystemConstant.REDIS_FEED_STREAM_KEY + userId;
        Long id = 28L;
        while (id <= 35) {
            stringRedisTemplate.opsForZSet().add(feedStreamKey, id.toString(), System.currentTimeMillis());
            id++;
        }

        // 把笔记的经纬度信息保存在redis中
        blogServiceImpl.loadBlogsGeo();
    }
}
