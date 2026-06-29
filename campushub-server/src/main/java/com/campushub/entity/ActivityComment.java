package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityComment {

    /** 主键ID */
    private Long id;
    /** 活动ID */
    private Long activityId;
    /** 评论用户ID */
    private Long userId;
    /** 评论内容 */
    private String content;
    /** 点赞数量 */
    private Integer likeCount;
    /** 逻辑删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
