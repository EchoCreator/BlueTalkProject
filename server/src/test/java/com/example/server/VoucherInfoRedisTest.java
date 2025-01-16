package com.example.server;

import com.example.common.constant.SystemConstant;
import com.example.common.utils.RedisUtil;
import com.example.pojo.vo.VoucherInfoVO;
import com.example.server.mapper.VoucherMapper;
import com.example.server.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class VoucherInfoRedisTest {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private VoucherServiceImpl voucherService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test() {
        // 注意：通过redis的Stream实现消息队列还需要通过
        // XGROUP CREATE stream_userVouchers group1 0 MKSTREAM
        // 命令生成名为stream_userVouchers的消息队列和名为group1的组

        List<VoucherInfoVO> list = voucherService.getVoucherInfoFromDB();
        redisUtil.setWithLogicalExpire(SystemConstant.REDIS_VOUCHER_INFO_KEY, list, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);

        for (VoucherInfoVO v : list) {
            if (v.getType() == 1) {
                stringRedisTemplate.opsForValue().set(SystemConstant.REDIS_LUA_VOUCHER_STOCK_KEY + v.getId(), v.getStock().toString());
            }
        }
    }
}
