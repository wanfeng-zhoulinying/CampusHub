package com.campushub.dto;

import lombok.Data;

@Data
public class ActivityCommentCreateDTO {

    private Long activityId;
    private String content;
}
