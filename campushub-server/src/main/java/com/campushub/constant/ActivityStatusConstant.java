package com.campushub.constant;

/** 1 未开始 2 报名中 3 进行中 4 已结束 5 已取消 */
public final class ActivityStatusConstant {

    public static final Integer NOT_STARTED = 1;
    public static final Integer SIGNING_UP = 2;
    public static final Integer IN_PROGRESS = 3;
    public static final Integer FINISHED = 4;
    public static final Integer CANCELED = 5;

    private ActivityStatusConstant() {
    }
}
