package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCommentLike {

    /** 主键ID */
    private Long id;
    /** 评论ID */
    private Long commentId;
    /** 点赞用户ID */
    private Long userId;
    /** 点赞状态，SocialStatusConstant */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
