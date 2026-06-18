package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySignupVO {

    private Long id;
    private Long activityId;
    private String activityTitle;
    private String activityLocation;
    private LocalDateTime signupTime;
    private Integer signupStatus;
    private Integer signStatus;
    private LocalDateTime signTime;
    private LocalDateTime cancelTime;
    private Integer waitOrder;
}
