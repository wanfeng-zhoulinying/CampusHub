package com.campushub.service.message;

import com.campushub.mq.event.BookingBreachEvent;
import com.campushub.vo.MessageVO;

import java.util.List;

public interface MessageService {

    void createMessage(Long userId, String title, String content, Integer type, Long businessId);

    /**
     * MQ消费：处理预约违约事件（幂等发站内通知），由NotifyBreachConsumer调用。
     */
    void sendBookingBreachNotify(BookingBreachEvent event);

    List<MessageVO> listMyMessages(Integer readStatus);

    void markAsRead(Long messageId);

    Integer countMyUnreadMessages();
}
