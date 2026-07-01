package com.campushub.constant;

/** 默认信用分 100 预约违约默认扣 10 最低信用分 0 */
public final class CreditRuleConstant {

    public static final Integer DEFAULT_SCORE = 100;
    public static final Integer BOOKING_BREACH_DEDUCT_SCORE = 10;
    public static final Integer MIN_SCORE = 0;

    private CreditRuleConstant() {
    }
}
