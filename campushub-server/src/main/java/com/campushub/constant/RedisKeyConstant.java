package com.campushub.constant;

/** Redis 缓存 key 常量 */
public final class RedisKeyConstant {

    /** 活动详情缓存 key 前缀 */
    public static final String ACTIVITY_DETAIL = "campushub:cache:activity:detail:";

    /** 场地详情缓存 key 前缀 */
    public static final String VENUE_DETAIL = "campushub:cache:venue:detail:";

    /** 活动报名剩余名额 key 前缀 */
    public static final String ACTIVITY_SIGNUP_STOCK = "campushub:activity:signup:stock:";

    /** 活动候补剩余名额 key 前缀 */
    public static final String ACTIVITY_WAIT_STOCK = "campushub:activity:signup:wait:";

    /** 活动报名请求幂等 key 前缀 */
    public static final String ACTIVITY_SIGNUP_REQUEST = "campushub:activity:signup:request:";

    /** 场地预约时间段剩余容量 key 前缀 */
    public static final String BOOKING_SLOT_STOCK = "campushub:booking:slot:stock:";

    /** 场地预约请求幂等 key 前缀 */
    public static final String BOOKING_REQUEST = "campushub:booking:request:";

    /** 热门活动排行榜 ZSet key */
    public static final String HOT_ACTIVITY_RANK = "campushub:rank:hot:activity";

    /** 热门场地排行榜 ZSet key */
    public static final String HOT_VENUE_RANK = "campushub:rank:hot:venue";

    private RedisKeyConstant() {
    }
}
