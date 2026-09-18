-- =====================================================================
-- 本地消息表（Outbox 模式）—— MQ 可靠性改造 Phase 2
-- =====================================================================
-- 作用：预约违约业务 UPDATE 与违约事件 INSERT 在同一个数据库事务内原子落库，
--       解决"业务成功了、但消息发出去失败/丢失"的双写一致性问题。
--       MqEventRelayTask 每 3 秒轮询 status=0 的事件，补发到 RabbitMQ，
--       broker 确认成功后标记 status=1；发送失败累加 retry_count，
--       超过重试上限标记 status=2（DEAD）等待人工排查。
--
-- 状态机：0 PENDING（待发送） -> 1 SENT（已送达交换机）
--                              -> 2 DEAD（超过重试上限，人工兜底）
-- =====================================================================

use campushub;

create table if not exists mq_event (
    id          bigint       not null auto_increment comment '主键ID',
    event_id    varchar(64)  not null comment '全局唯一事件ID（UUID），用于链路追踪与消费端幂等去重',
    event_type  varchar(32)  not null comment '事件类型，对应 MqConstant.EVENT_TYPE_XXX',
    payload     text         not null comment '事件JSON负载（BookingBreachEvent序列化结果）',
    status      tinyint      not null default 0 comment '投递状态：0待发送 1已发送 2死亡',
    retry_count int          not null default 0 comment '已重试次数',
    create_time datetime     not null default current_timestamp comment '创建时间',
    update_time datetime     not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (id),
    unique key uk_event_id (event_id),
    key idx_status_id (status, id)
) engine = innodb default charset = utf8mb4 comment = '本地消息表：业务与事件同事务写入，relay轮询补发';

-- 验证建表结果：
-- show create table mq_event;

-- 2. 造一条超时未核销预约 （触发扫描任务的自动违约）：
insert into booking (booking_no, user_id, venue_id, slot_id, booking_date, start_time, end_time, person_count, status, breach_flag, remark)
values ('TEST-BREACH-MQ-001',
        (select id from sys_user where is_deleted = 0 order by id limit 1),
        1, 1, curdate() - interval 1 day, '09:00', '10:00', 1, 1, 0, 'MQ链路测试');
