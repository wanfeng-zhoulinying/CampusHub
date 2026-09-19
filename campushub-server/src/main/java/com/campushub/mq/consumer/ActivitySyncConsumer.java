package com.campushub.mq.consumer;

import com.campushub.constant.MqConstant;
import com.campushub.mq.event.ActivitySyncEvent;
import com.campushub.service.es.ActivityIndexService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 活动同步消费者：消费ACTIVITY_SYNC事件，回查数据库最新值写入ES索引。
 * 消费者只做三件事——反序列化、调索引服务、ack/nack，同步逻辑收敛在ActivityIndexService。
 *
 * 幂等设计（与违约链路对比，面试要点）：
 * - 违约扣分：业务语义不可重复（扣分/发信），靠credit_record/message业务键查重；
 * - ES同步：覆盖写语义天然可重复（文档_id=活动ID，重复写结果不变），
 *   幂等直接由ES文档主键保证，无需查重表——同一模式在不同业务的落地成本差异。
 *
 * 失败语义与CreditBreachConsumer一致：手动ack、坏消息直接死信、业务异常上抛容器重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySyncConsumer {

    private final ActivityIndexService activityIndexService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = MqConstant.ACTIVITY_SYNC_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        ActivitySyncEvent event;
        try {
            event = objectMapper.readValue(payload, ActivitySyncEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[ActivitySyncConsumer] 消息反序列化失败，跳过重试直接进入死信队列 payload={}", payload, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        // ES不可用等业务异常上抛交给容器重试，重试耗尽进死信
        activityIndexService.syncActivityById(event.getActivityId());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("[ActivitySyncConsumer] 活动同步事件消费成功 eventId={}, activityId={}, action={}",
                event.getEventId(), event.getActivityId(), event.getAction());
    }
}
