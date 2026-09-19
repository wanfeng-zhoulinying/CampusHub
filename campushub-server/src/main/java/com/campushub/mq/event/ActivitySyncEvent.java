package com.campushub.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动数据同步事件（MySQL -> ES）。
 *
 * 设计要点：事件只携带activityId，不携带活动数据快照——
 * 消费者按ID回查数据库拿最新值再写ES，避免乱序消费时旧数据覆盖新数据
 * （与延迟消息的"软检查"同一思想：消息只做触发信号，真相在数据库）。
 * action仅用于日志排查，不参与同步逻辑。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySyncEvent {

    /** 事件唯一ID（UUID，与本地消息表event_id一致） */
    private String eventId;
    /** 活动ID：消费者按此回查数据库 */
    private Long activityId;
    /** 触发动作：CREATE / UPDATE / AUDIT / STATUS（MqConstant.SYNC_ACTION_*，仅排查用） */
    private String action;
    /** 事件产生时间 */
    private LocalDateTime occurredAt;
}
