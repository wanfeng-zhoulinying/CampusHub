package com.campushub.constant;

/** 1 未开始 2 报名中 3 进行中 4 已结束 5 已取消 6 报名结束待开始 */
public final class ActivityStatusConstant {

    /** 未开始 */
    public static final Integer NOT_STARTED = 1;
    /** 报名中 */
    public static final Integer SIGNING_UP = 2;
    /** 进行中 */
    public static final Integer IN_PROGRESS = 3;
    /** 已结束 */
    public static final Integer FINISHED = 4;
    /** 已取消 */
    public static final Integer CANCELED = 5;
    /** 报名结束待开始 */
    public static final Integer SIGNUP_ENDED = 6;

    private ActivityStatusConstant() {
    }
}
