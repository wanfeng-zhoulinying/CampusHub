package com.campushub.mq.consumer;

import com.campushub.constant.BookingBreachFlagConstant;
import com.campushub.constant.BookingStatusConstant;
import com.campushub.constant.MqConstant;
import com.campushub.entity.Booking;
import com.campushub.mapper.BookingMapper;
import com.campushub.service.credit.CreditService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 到点核销检查消费者：预约结束时间一到，检查预约最终状态——
 * 仍未核销（status=BOOKED）则走违约链路（outbox -> relay -> 扣分/通知）。
 *
 * 软检查设计：延迟期间预约可能已被用户取消/核销，或已被兜底扫描判过违约。
 * 状态不满足就静默跳过（ack），不判违约也不报错——
 * 消息只是"到点看一眼"的触发器，一切以数据库当前状态为准。
 * markBookingBreachBySystem 内部还有条件UPDATE幂等，双保险。
 * 失败语义与其他消费者一致：坏消息直接死信，业务异常上抛本地重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingDelayCheckConsumer {

    private final BookingMapper bookingMapper;
    private final CreditService creditService;

    @RabbitListener(queues = MqConstant.BOOKING_DELAY_CHECK_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        Long bookingId;
        try {
            bookingId = Long.parseLong(payload.trim());
        } catch (NumberFormatException e) {
            log.error("[DelayCheckConsumer] 消息不是合法的预约ID，进入死信队列 payload={}", payload, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        // 软检查：以到点时刻的数据库状态为准
        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            log.warn("[DelayCheckConsumer] 预约不存在，跳过 bookingId={}", bookingId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        if (!BookingStatusConstant.BOOKED.equals(booking.getStatus())
                || BookingBreachFlagConstant.BREACHED.equals(booking.getBreachFlag())) {
            log.info("[DelayCheckConsumer] 预约延迟期间已核销/取消/判违约，跳过 bookingId={}, status={}, breachFlag={}",
                    bookingId, booking.getStatus(), booking.getBreachFlag());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        // 仍是待核销状态且已到点：登记违约，后续扣分/通知走既有MQ链路
        creditService.markBookingBreachBySystem(bookingId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        log.info("[DelayCheckConsumer] 到点核销检查完成，预约转违约 bookingId={}", bookingId);
    }
}
