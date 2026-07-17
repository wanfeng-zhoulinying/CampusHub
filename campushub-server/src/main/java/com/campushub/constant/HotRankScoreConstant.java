package com.campushub.constant;

/** 热门排行榜行为加分常量 */
public final class HotRankScoreConstant {

    /** 查看活动详情，加 1 分 */
    public static final double ACTIVITY_DETAIL_VIEW = 1D;

    /** 活动报名或候补成功，加 10 分 */
    public static final double ACTIVITY_SIGNUP_SUCCESS = 10D;

    /** 收藏活动，加 5 分 */
    public static final double ACTIVITY_FAVORITE = 5D;

    /** 查看场地详情，加 1 分 */
    public static final double VENUE_DETAIL_VIEW = 1D;

    /** 查看场地时间段，加 2 分 */
    public static final double VENUE_SLOT_VIEW = 2D;

    /** 场地预约成功，加 10 分 */
    public static final double VENUE_BOOKING_SUCCESS = 10D;

    private HotRankScoreConstant() {
    }
}
