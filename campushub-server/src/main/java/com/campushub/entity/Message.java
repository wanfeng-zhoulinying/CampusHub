package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {

    /** 主键ID */
    private Long id;
    /** 接收用户ID */
    private Long userId;
    /** 消息标题 */
    private String title;
    /** 消息内容 */
    private String content;
    /** 消息类型，MessageTypeConstant */
    private Integer type;
    /** 关联业务ID，如预约ID、报名ID、活动ID */
    private Long businessId;
    /** 已读状态，MessageReadStatusConstant */
    private Integer readStatus;
    /** 逻辑删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
