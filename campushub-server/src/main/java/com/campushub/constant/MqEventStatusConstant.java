package com.campushub.constant;

/**
 * 本地消息表投递状态常量，对应 mq_event.status。
 */
public class MqEventStatusConstant {

    /**
     * 待发送：已随业务事务落库，等待 relay 轮询补发。
     */
    public static final Integer PENDING = 0;

    /**
     * 已发送：broker confirm 确认已到达交换机。
     */
    public static final Integer SENT = 1;

    /**
     * 死亡：超过重试上限仍发送失败，等待人工排查。
     */
    public static final Integer DEAD = 2;

    private MqEventStatusConstant() {
    }
}
