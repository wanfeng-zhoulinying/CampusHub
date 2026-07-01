package com.campushub.vo;

import lombok.Data;

@Data
public class AdminDashboardOverviewVO {

    private Integer userCount;
    private Integer venueCount;
    private Integer activityCount;
    private Integer bookingCount;
    private Integer messageCount;
    private Integer breachBookingCount;
}
