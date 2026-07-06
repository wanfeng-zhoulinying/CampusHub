package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingBreachAppealVO {

    private Long id;
    private Long bookingId;
    private String bookingNo;
    private String reason;
    private Integer appealStatus;
    private Integer deductScore;
    private String auditRemark;
    private LocalDateTime appealTime;
    private LocalDateTime auditTime;
}
