package com.campushub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminActivitySaveDTO {

    private Long publisherId;
    private String title;
    private String coverUrl;
    private String content;
    private String location;
    private Long venueId;
    private LocalDateTime signupStartTime;
    private LocalDateTime signupEndTime;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private Integer signupLimit;
    private Integer waitLimit;
    private Integer status;
}
