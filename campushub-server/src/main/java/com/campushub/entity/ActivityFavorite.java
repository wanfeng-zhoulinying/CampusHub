package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityFavorite {

    /** 主键ID */
    private Long id;
    /** 活动ID */
    private Long activityId;
    /** 收藏用户ID */
    private Long userId;
    /** 收藏状态，SocialStatusConstant */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
