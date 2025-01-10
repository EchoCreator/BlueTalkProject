package com.example.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@Data
@AllArgsConstructor
public class UniqueIDGenerator {
    private static final long BEGIN_TIMESTAMP = 984787200L;
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public Long generateUniqueID(String keyPrefix) {
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        Long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;

        // 利用redis生成序列号
        // 获取当前日期，精确到天，key为前缀加上当前日期，从而可以将当天所有的订单的key前缀设置为一样的，也方便统计
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long serialNumber = stringRedisTemplate.opsForValue().increment(keyPrefix + date);

        // 全局唯一ID的组成：开头的0表示正数符号+时间戳占31位+序列号占后32位，因此时间戳要左移32位
        return timestamp << COUNT_BITS | serialNumber;
    }
}
