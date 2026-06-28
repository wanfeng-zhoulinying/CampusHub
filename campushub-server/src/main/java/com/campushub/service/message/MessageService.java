package com.campushub.service.message;

import com.campushub.vo.MessageVO;

import java.util.List;

public interface MessageService {

    void createMessage(Long userId, String title, String content, Integer type, Long businessId);

    List<MessageVO> listMyMessages(Integer readStatus);

    void markAsRead(Long messageId);

    Integer countMyUnreadMessages();
}
