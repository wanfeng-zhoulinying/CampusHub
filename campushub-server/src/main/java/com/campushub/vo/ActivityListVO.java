package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityListVO {

    private Long id;
    private String title;
    private String coverUrl;
    private String location;
    private Integer signupLimit;
    private Integer currentSignupCount;
    private Integer waitLimit;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
}
