package com.campushub.constant;

/** 1 已报名 2 已取消 3 候补中 4 候补转正 */
public final class ActivitySignupStatusConstant {

    /** 已报名 */
    public static final Integer SIGNED_UP = 1;
    /** 已取消 */
    public static final Integer CANCELED = 2;
    /** 候补中 */
    public static final Integer WAITLISTED = 3;
    /** 候补转正 */
    public static final Integer WAITLIST_CONFIRMED = 4;

    private ActivitySignupStatusConstant() {
    }
}
