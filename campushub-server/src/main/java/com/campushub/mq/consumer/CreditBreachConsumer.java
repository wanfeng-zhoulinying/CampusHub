package com.campushub.mq.consumer;

import com.campushub.constant.MqConstant;
import com.campushub.mq.event.BookingBreachEvent;
import com.campushub.service.credit.CreditService;
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
 * 信用分扣减消费者：消费预约违约事件，完成扣分与信用流水。
 * 消费者只做三件事——反序列化、调域服务、ack/nack，业务逻辑收敛在CreditService。
 *
 * 失败语义（消费端可靠性三件套）：
 * 1. 手动ack：业务成功才 basicAck，broker 才会把消息从队列删除；
 * 2. 本地重试：业务异常直接上抛（不catch），监听容器按yml配置重试3次；
 * 3. 死信兜底：重试耗尽后容器 basicNack(requeue=false)，消息进入死信队列。
 * 坏消息（JSON解析失败）重试也不可能成功，跳过重试直接进死信。
 * 幂等：由域服务按 credit_record 天然业务键去重，配合relay至少一次投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditBreachConsumer {

    private final CreditService creditService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = MqConstant.CREDIT_BREACH_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        BookingBreachEvent event;
        try {
            event = objectMapper.readValue(payload, BookingBreachEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[CreditConsumer] 消息反序列化失败，跳过重试直接进入死信队列 payload={}", payload, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        // 业务异常上抛交给容器重试，这里不做任何吞异常
        creditService.consumeBookingBreachEvent(event);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("[CreditConsumer] 违约事件消费成功 eventId={}", event.getEventId());
    }
}
