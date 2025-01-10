package com.example.common.constant;

public class SystemConstant {
    public static final String USERNAME_PREFIX = "user_";

    /*redis*/
    public static final String REDIS_COMMODITY_TYPE_KEY = "commodity_type"; // 存储商品类
    public static final Long REDIS_COMMODITY_TYPE_EXPIRATION = 30L; // 30天

    public static final String REDIS_COMMODITY_LIST_KEY = "commodity_type_"; // 存储某一类的所有商品
    public static final Long REDIS_COMMODITY_LIST_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_COMMODITY_KEY = "commodity_"; // 存储某一个商品
    public static final Long REDIS_COMMODITY_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_COMMODITY_COMMENTS_KEY = "commodity_comments_"; // 存储某一个商品的评论
    public static final Long REDIS_COMMODITY_COMMENTS_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_USER_KEY = "user_"; // 存储用户信息
    public static final String REDIS_USER_INFO_KEY = "user_info_"; // 存储用户具体信息
    public static final Long REDIS_USER_EXPIRATION = 1L; // 1分钟

    public static final String REDIS_USER_COMMODITY_KEY = "user_commodity_"; // 存储某个用户的店铺商品
    public static final Long REDIS_USER_COMMODITY_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_LOCK_KEY = "lock_key_"; // 互斥锁的前缀（防缓存击穿）

    public static final Long REDIS_NULL_EXPIRATION = 1L; // 缓存空值的时间（防缓存穿透），1分钟
}
