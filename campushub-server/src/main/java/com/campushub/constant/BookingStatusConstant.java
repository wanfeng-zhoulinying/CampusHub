package com.campushub.constant;

/** 1 待使用 2 已完成 3 已取消 4 已违约 */
public final class BookingStatusConstant {

    public static final Integer BOOKED = 1;
    public static final Integer CHECKED_IN = 2;
    public static final Integer CANCELED = 3;
    public static final Integer BREACHED = 4;

    private BookingStatusConstant() {
    }
}
