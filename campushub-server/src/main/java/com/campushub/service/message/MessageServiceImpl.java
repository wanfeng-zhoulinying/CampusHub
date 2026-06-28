package com.campushub.service.message;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.MessageReadStatusConstant;
import com.campushub.entity.Message;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.MessageMapper;
import com.campushub.utils.UserContext;
import com.campushub.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    /**
     * 创建站内通知。
     * 由其他业务模块调用，统一写入消息表，当前 V1 先不做实时推送。
     */
    @Override
    public void createMessage(Long userId, String title, String content, Integer type, Long businessId) {
        if (userId == null) {
            throw new BusinessException("消息接收人不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException("消息标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        Message message = new Message();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setBusinessId(businessId);
        message.setReadStatus(MessageReadStatusConstant.UNREAD);
        message.setIsDeleted(DeleteStatusConstant.NOT_DELETED);
        messageMapper.saveMessage(message);
    }

    /**
     * 查询当前登录用户的通知列表。
     * 只从登录态读取用户 ID，避免前端传 userId 查询别人通知。
     */
    @Override
    public List<MessageVO> listMyMessages(Integer readStatus) {
        return messageMapper.listUserMessages(getCurrentUserId(), readStatus);
    }

    /**
     * 标记当前登录用户自己的通知为已读。
     * SQL 会同时校验 messageId 和 userId，防止操作其他人的消息。
     */
    @Override
    public void markAsRead(Long messageId) {
        if (messageId == null) {
            throw new BusinessException("消息ID不能为空");
        }
        int affectedRows = messageMapper.markAsRead(messageId, getCurrentUserId());
        if (affectedRows == 0) {
            throw new BusinessException("消息不存在或无权操作");
        }
    }

    /**
     * 统计当前登录用户未读通知数量。
     * 后面前端右上角红点或消息徽标可以直接使用这个接口。
     */
    @Override
    public Integer countMyUnreadMessages() {
        return messageMapper.countUnread(getCurrentUserId());
    }

    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
