package com.campushub.constant;

/** 0 待审核 1 通过 2 驳回 */
public final class ActivityAuditStatusConstant {

    public static final Integer PENDING = 0;
    public static final Integer APPROVED = 1;
    public static final Integer REJECTED = 2;

    private ActivityAuditStatusConstant() {
    }
}
