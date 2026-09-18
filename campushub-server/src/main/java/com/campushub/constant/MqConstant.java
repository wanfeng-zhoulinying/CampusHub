package com.campushub.constant;

/**
 * MQ拓扑常量：交换机、队列、路由键名称统一收敛在此，避免魔法值散落。
 */
public class MqConstant {

    /**
     * 预约域交换机（direct）：预约被判定违约后发布业务事件。
     */
    public static final String BOOKING_EXCHANGE = "campushub.booking.exchange";

    /**
     * 路由键：预约违约事件。
     */
    public static final String BOOKING_BREACH_ROUTING_KEY = "booking.breach";

    /**
     * 消费队列：信用分扣减（扣分 + 流水）。
     */
    public static final String CREDIT_BREACH_QUEUE = "campushub.credit.breach.queue";

    /**
     * 消费队列：站内通知发送。
     */
    public static final String NOTIFY_BREACH_QUEUE = "campushub.notify.breach.queue";

    /**
     * 死信交换机（fanout）：消费失败且超过重试上限的消息统一落入死信队列。
     */
    public static final String DEAD_EXCHANGE = "campushub.dead.exchange";

    /**
     * 死信队列：人工排查兜底。
     */
    public static final String DEAD_QUEUE = "campushub.dead.queue";

    /**
     * 事件类型：预约违约（本地消息表 event_type 取值）。
     */
    public static final String EVENT_TYPE_BOOKING_BREACH = "BOOKING_BREACH";

    /**
     * 事件来源：系统自动判定（扫描任务触发，operatorId 为空）。
     */
    public static final String EVENT_SOURCE_SYSTEM = "SYSTEM";

    /**
     * 事件来源：管理员手动登记。
     */
    public static final String EVENT_SOURCE_ADMIN = "ADMIN";

    /**
     * 延迟交换机（x-delayed-message 插件类型，内部按 direct 规则路由）：
     * 预约创建后发延迟消息，预约结束时间到点投递核销检查。
     */
    public static final String BOOKING_DELAY_EXCHANGE = "campushub.booking.delay.exchange";

    /**
     * 路由键：预约到点核销检查。
     */
    public static final String BOOKING_DELAY_CHECK_ROUTING_KEY = "booking.delay.check";

    /**
     * 消费队列：到点核销检查（到点仍未核销则判违约）。
     */
    public static final String BOOKING_DELAY_CHECK_QUEUE = "campushub.booking.delay.check.queue";

    private MqConstant() {
    }
}
