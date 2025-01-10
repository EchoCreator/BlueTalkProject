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

    @Test
    public void test() {
        List<VoucherInfoVO> list = voucherService.getVoucherInfoFromDB();
        redisUtil.setWithLogicalExpire(SystemConstant.REDIS_VOUCHER_INFO_KEY, list, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);
    }
}
