package com.campushub.task;

import com.campushub.constant.MqConstant;
import com.campushub.entity.MqEvent;
import com.campushub.mapper.MqEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地消息表补发任务（relay）：轮询 mq_event 中 PENDING 状态的事件，
 * 发布到 RabbitMQ 并同步等待 publisher confirm 确认，确认成功才标记 SENT。
 *
 * 可靠性语义（面试要点）：
 * 1. 至少一次投递：broker确认成功但 markSent 前进程崩溃，事件仍是 PENDING，
 *    下一轮会重发，因此消费端必须幂等（按 eventId 去重）。
 * 2. 发送失败不丢弃：累加 retry_count 留在表里等下一轮重试；
 *    超过 MAX_RETRY_COUNT 标记 DEAD，人工介入兜底。
 * 3. 不加 @Transactional：发送MQ与标记SENT是两个动作，本就不在一个事务里，
 *    靠状态机 + 幂等消费收敛，而非强一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqEventRelayTask {

    /**
     * 每轮最多补发的事件数。
     */
    private static final int BATCH_SIZE = 50;

    /**
     * 最大重试次数：发送失败达到该次数后标记 DEAD。
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 等待 broker confirm 的超时时间（毫秒）。
     */
    private static final long CONFIRM_TIMEOUT_MILLIS = 5000;

    private final MqEventMapper mqEventMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 每 3 秒轮询一次本地消息表，补发待发送事件。
     */
    @Scheduled(fixedDelay = 3000)
    public void relayPendingEvents() {
        List<MqEvent> pendingEvents = mqEventMapper.listPendingEvents(BATCH_SIZE);
        if (pendingEvents.isEmpty()) {
            return;
        }

        int sentCount = 0;
        for (MqEvent event : pendingEvents) {
            try {
                publishWithConfirm(event);
                mqEventMapper.markSent(event.getId());
                sentCount++;
            } catch (Exception e) {
                handleSendFailure(event, e);
            }
        }
        log.info("[MqEventRelayTask] 本地消息补发完成 total={}, sent={}", pendingEvents.size(), sentCount);
    }

    /**
     * 私：发布事件并同步等待 broker confirm，未确认到交换机视为发送失败。
     */
    private void publishWithConfirm(MqEvent event) throws Exception {
        CorrelationData correlationData = new CorrelationData(event.getEventId());
        MessagePostProcessor messagePostProcessor = message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setMessageId(event.getEventId());
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return message;
        };

        rabbitTemplate.convertAndSend(
                MqConstant.BOOKING_EXCHANGE,
                resolveRoutingKey(event.getEventType()),
                event.getPayload(),
                messagePostProcessor,
                correlationData
        );

        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(CONFIRM_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            throw new IllegalStateException("消息未被broker确认 eventId=" + event.getEventId()
                    + (confirm == null ? "" : " reason=" + confirm.getReason()));
        }
    }

    /**
     * 私：事件类型到路由键的映射，新事件类型在此扩展。
     */
    private String resolveRoutingKey(String eventType) {
        return switch (eventType) {
            case MqConstant.EVENT_TYPE_BOOKING_BREACH -> MqConstant.BOOKING_BREACH_ROUTING_KEY;
            default -> throw new IllegalArgumentException("未知事件类型：" + eventType);
        };
    }

    /**
     * 私：发送失败处理——未超上限则累加重试次数，超限则标记 DEAD。
     */
    private void handleSendFailure(MqEvent event, Exception e) {
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();
        if (retryCount + 1 >= MAX_RETRY_COUNT) {
            mqEventMapper.markDead(event.getId());
            log.error("[MqEventRelayTask] 事件超过重试上限，标记DEAD等待人工排查 eventId={}, retryCount={}",
                    event.getEventId(), retryCount, e);
            return;
        }
        mqEventMapper.increaseRetryCount(event.getId());
        log.warn("[MqEventRelayTask] 事件发送失败，等待下轮重试 eventId={}, retryCount={}",
                event.getEventId(), retryCount, e);
    }
}
