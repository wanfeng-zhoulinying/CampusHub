package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityFavoriteVO {

    private Long favoriteId;
    private Long activityId;
    private String activityTitle;
    private String coverUrl;
    private String location;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private LocalDateTime createTime;
}
