package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表记录：业务与事件同事务写入，relay 轮询补发到 MQ，保证最终一致。
 */
@Data
public class MqEvent {

    /** 主键ID */
    private Long id;
    /** 全局唯一事件ID（UUID），用于链路追踪与消费端幂等去重 */
    private String eventId;
    /** 事件类型，MqConstant.EVENT_TYPE_XXX */
    private String eventType;
    /** 事件JSON负载 */
    private String payload;
    /** 投递状态，MqEventStatusConstant */
    private Integer status;
    /** 已重试次数 */
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
