package com.campushub.mq.consumer;

import com.campushub.constant.MqConstant;
import com.campushub.mq.event.BookingBreachEvent;
import com.campushub.service.message.MessageService;
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
 * 站内通知消费者：消费预约违约事件，给被违约用户发站内信。
 * 与信用分消费者消费的是同一个事件的两个副本（direct交换机按routing key
 * 分发到所有绑定队列，每个队列一份完整拷贝），各自独立消费、独立ack，
 * 通知消费失败不会影响信用分消费，反之亦然——下游故障隔离。
 * 失败语义与幂等同 CreditBreachConsumer。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyBreachConsumer {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = MqConstant.NOTIFY_BREACH_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        BookingBreachEvent event;
        try {
            event = objectMapper.readValue(payload, BookingBreachEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[NotifyConsumer] 消息反序列化失败，跳过重试直接进入死信队列 payload={}", payload, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        messageService.sendBookingBreachNotify(event);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("[NotifyConsumer] 违约事件消费成功 eventId={}", event.getEventId());
    }
}
