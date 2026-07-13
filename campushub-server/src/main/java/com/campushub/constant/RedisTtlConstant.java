package com.campushub.constant;

/** Redis 缓存过期时间常量 */
public final class RedisTtlConstant {

    /** 活动详情缓存基础过期时间，分钟 */
    public static final long ACTIVITY_DETAIL_MINUTES = 30;

    /** 场地详情缓存基础过期时间，分钟 */
    public static final long VENUE_DETAIL_MINUTES = 30;

    /** 空值缓存过期时间，分钟 */
    public static final long NULL_CACHE_MINUTES = 2;

    /** 缓存随机过期偏移最大分钟数 */
    public static final long CACHE_RANDOM_MINUTES = 10;

    private RedisTtlConstant() {
    }
}
