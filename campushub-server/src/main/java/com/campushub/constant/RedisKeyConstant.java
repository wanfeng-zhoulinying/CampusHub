package com.campushub.constant;

/** Redis 缓存 key 常量 */
public final class RedisKeyConstant {

    /** 活动详情缓存 key 前缀 */
    public static final String ACTIVITY_DETAIL = "campushub:cache:activity:detail:";

    /** 场地详情缓存 key 前缀 */
    public static final String VENUE_DETAIL = "campushub:cache:venue:detail:";

    private RedisKeyConstant() {
    }
}
