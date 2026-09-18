package com.campushub.service.mq;

import com.campushub.constant.MqEventStatusConstant;
import com.campushub.entity.MqEvent;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.MqEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 本地消息表服务实现：事件序列化落库。
 * 注意：本方法不加@Transactional，事务由调用方的业务方法携带——
 * 事件INSERT必须与业务UPDATE在同一个事务里，要么都提交、要么都回滚，
 * 这正是Outbox模式解决双写一致性的核心。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqEventServiceImpl implements MqEventService {

    private final MqEventMapper mqEventMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void saveEvent(String eventId, String eventType, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // 序列化失败直接抛异常回滚整个业务事务：
            // 事件是后续扣分/通知的唯一载体，不能出现"违约已登记但事件丢失"。
            throw new BusinessException("MQ事件序列化失败：" + eventType);
        }

        MqEvent mqEvent = new MqEvent();
        mqEvent.setEventId(eventId);
        mqEvent.setEventType(eventType);
        mqEvent.setPayload(payload);
        mqEvent.setStatus(MqEventStatusConstant.PENDING);
        mqEventMapper.saveMqEvent(mqEvent);
        log.info("[MqEvent] 事件落库成功 eventId={}, type={}", eventId, eventType);
    }
}
