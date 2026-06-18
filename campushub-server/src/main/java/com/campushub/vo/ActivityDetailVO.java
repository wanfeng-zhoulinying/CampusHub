package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityDetailVO {

    private Long id;
    private Long publisherId;
    private String title;
    private String coverUrl;
    private String content;
    private String location;
    private Long venueId;
    private Integer signupLimit;
    private Integer currentSignupCount;
    private Integer waitLimit;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
}
