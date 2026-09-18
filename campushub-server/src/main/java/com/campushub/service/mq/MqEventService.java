package com.campushub.service.mq;

/**
 * 本地消息表服务：事件与业务同事务写入 mq_event，由 relay 异步补发。
 */
public interface MqEventService {

    /**
     * 保存事件到本地消息表（必须运行在业务事务内，与业务UPDATE原子提交）。
     *
     * @param eventId   全局唯一事件ID（UUID）
     * @param eventType 事件类型，MqConstant.EVENT_TYPE_XXX
     * @param event     事件对象，序列化为JSON后存入payload
     */
    void saveEvent(String eventId, String eventType, Object event);
}
