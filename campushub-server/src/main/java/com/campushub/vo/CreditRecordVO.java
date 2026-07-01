package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditRecordVO {

    private Long id;
    private Integer changeType;
    private Integer changeScore;
    private Integer currentScore;
    private String reason;
    private Integer businessType;
    private Long businessId;
    private LocalDateTime createTime;
}
