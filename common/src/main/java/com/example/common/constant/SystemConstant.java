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
    public static final String REDIS_USER_VOUCHER_LOCK_KEY = "lock_user_voucher_key_"; // redisson锁相同用户领取优惠券的前缀

    public static final Long REDIS_NULL_EXPIRATION = 1L; // 缓存空值的时间（防缓存穿透），1分钟

    public static final String REDIS_VOUCHER_INFO_KEY = "voucher_info"; // 存储优惠券信息
    public static final Long REDIS_VOUCHER_INFO_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_USER_VOUCHER_KEY = "user_voucher_"; // 存储优惠券订单

    public static final String REDIS_LUA_VOUCHER_STOCK_KEY = "pickUpVoucher_stock_"; // 通过lua脚本存储秒杀优惠券库存
    public static final String REDIS_LUA_USER_VOUCHER_KEY = "pickUpVoucher_userVoucher_"; // 通过lua脚本存储领取某一优惠券的用户set
    public static final String REDIS_LUA_STREAM_USER_VOUCHER = "stream_userVouchers"; // 通过lua脚本向名为stream_userVoucher的消息队列添加userVoucher信息

    public static final String REDIS_BLOGS_KEY = "blogs"; // 存储帖子列表
    public static final Long REDIS_BLOGS_EXPIRATION = 5L; // 5分钟

    public static final String REDIS_BLOG_COMMENTS_KEY = "blog_comments_"; // 存储单个帖子的一级评论列表
    public static final String REDIS_BLOG_CHILDREN_COMMENTS_KEY = "blog_children_comments_"; // 存储单个帖子的一级评论的回复列表
    public static final Long REDIS_BLOG_COMMENTS_EXPIRATION = 5L; // 5分钟
}
