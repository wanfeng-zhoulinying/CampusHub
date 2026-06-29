package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCommentVO {

    private Long id;
    private Long activityId;
    private Long userId;
    private String username;
    private String realName;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createTime;
}
