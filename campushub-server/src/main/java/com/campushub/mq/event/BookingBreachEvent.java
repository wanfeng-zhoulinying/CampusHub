package com.campushub.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预约违约事件：预约被判定违约（系统自动/管理员手动）后发布。
 * 消费方：信用分消费者（扣分 + 流水）、站内通知消费者（发信）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingBreachEvent {

    /**
     * 全局唯一事件ID（UUID），用于链路追踪与消费端幂等去重。
     */
    private String eventId;

    /**
     * 预约ID。
     */
    private Long bookingId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 预约单号。
     */
    private String bookingNo;

    /**
     * 违约原因。
     */
    private String reason;

    /**
     *规则预设扣分值（实际扣减以消费者按当前信用分计算为准）。
     */
    private Integer deductScore;

    /**
     * 操作人ID：null 表示系统自动判定，非 null 表示管理员手动登记。
     */
    private Long operatorId;

    /**
     * 违约来源：SYSTEM / ADMIN。
     */
    private String source;

    /**
     * 事件发生时间。
     */
    private LocalDateTime occurredAt;
}
