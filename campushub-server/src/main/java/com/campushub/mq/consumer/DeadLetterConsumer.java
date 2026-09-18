package com.campushub.mq.consumer;

import com.campushub.constant.MqConstant;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 死信消费者：消费重试耗尽后的死信消息，人工排查的入口。
 * 消息为什么死了，答案在headers里的 x-death 数组：
 *   - reason=rejected：消费端nack且requeue=false（重试耗尽/坏消息）
 *   - exchange/queue/routing-keys：消息的原始来源，定位是哪个消费者的问题
 *   - count：死信次数
 * 死信消费者必须ack：死信是最后的兜底环节，再抛异常只会无限循环。
 * 真实生产中这里应对接告警渠道（钉钉机器人/邮件/ELK），本项目打ERROR日志演示。
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = MqConstant.DEAD_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        log.error("[DeadLetter] 收到死信消息，需要人工排查 routingKey={}, headers={}, payload={}",
                message.getMessageProperties().getReceivedRoutingKey(), headers, payload);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
