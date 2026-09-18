package com.campushub.mq.producer;

import com.campushub.constant.MqConstant;
import com.campushub.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 到点核销检查消息的生产者：预约创建成功后，按"预约结束时间 - 当前时间"
 * 计算延迟，发一条延迟消息，到点由 BookingDelayCheckConsumer 判定违约。
 *
 * 设计取舍（面试要点）：
 * 1. 事务提交后才真正发送（afterCommit 回调）——事务回滚则不发，
 *    避免"预约根本没创建成功、到点却收到检查消息"的无效消息；
 *    这也是"事务内不要做可能拖垮DB连接的网络IO"的落地。
 * 2. 不走本地消息表、不等publisher confirm——这条消息允许丢失：
 *    判违约不是资金类操作，兜底扫描任务每5分钟补扫，
 *    丢消息的代价只是违约判定最多滞后5分钟。可靠性分级，够用即可。
 * 3. 到点的判定不依赖消息里的快照，消费者到点后查库拿最新状态——
 *    延迟期间预约可能被取消/核销，消息只是"到点看一眼"的触发器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingDelayCheckProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 登记一条到点检查消息。必须在预约创建事务内调用，
     * 实际发送动作被推迟到事务成功提交之后。
     */
    public void sendDelayCheck(Booking booking) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(booking);
                }
            });
        } else {
            // 兜底：无事务上下文时直接发送（正常流程走不到这里）
            doSend(booking);
        }
    }

    /**
     * 私：真正发送延迟消息。x-delay 头由插件的延迟交换机识别。
     */
    private void doSend(Booking booking) {
        LocalDateTime checkTime = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        long delayMillis = Duration.between(LocalDateTime.now(), checkTime).toMillis();
        if (delayMillis < 0) {
            // 防御：结束时间已过的预约（如造测试数据），立即投递不等延迟
            delayMillis = 0;
        }
        // x-delay 头是32位整数，上限约49天，预约场景远小于该值
        long delay = Math.min(delayMillis, MessageProperties.X_DELAY_MAX);

        rabbitTemplate.convertAndSend(
                MqConstant.BOOKING_DELAY_EXCHANGE,
                MqConstant.BOOKING_DELAY_CHECK_ROUTING_KEY,
                String.valueOf(booking.getId()),
                message -> {
                    MessageProperties properties = message.getMessageProperties();
                    properties.setDelayLong(delay);
                    properties.setMessageId("delay-check-" + booking.getId());
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );
        log.info("[BookingDelayProducer] 延迟核销检查已登记 bookingId={}, 到点时间={}, 延迟{}ms",
                booking.getId(), checkTime, delay);
    }
}
