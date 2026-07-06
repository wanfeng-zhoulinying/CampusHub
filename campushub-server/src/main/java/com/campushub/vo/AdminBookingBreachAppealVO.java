package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBookingBreachAppealVO {

    private Long id;
    private Long bookingId;
    private String bookingNo;
    private Long userId;
    private String username;
    private String realName;
    private String reason;
    private Integer appealStatus;
    private Integer deductScore;
    private String auditRemark;
    private Long auditUserId;
    private LocalDateTime appealTime;
    private LocalDateTime auditTime;
}
