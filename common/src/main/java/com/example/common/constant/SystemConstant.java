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
    public static final Long REDIS_USER_EXPIRATION = 1L; // 1分钟

    public static final String REDIS_OTHER_USER_KEY = "other_user_"; // 存储其他用户信息
    public static final Long REDIS_OTHER_USER_EXPIRATION = 1L; // 1分钟
}
