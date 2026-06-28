USE campushub;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content VARCHAR(500) NOT NULL COMMENT '消息内容',
    type TINYINT NOT NULL COMMENT '消息类型：1预约通知 2活动通知 3审核通知',
    business_id BIGINT NULL COMMENT '关联业务ID',
    read_status TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态：0未读 1已读',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_message_user_read (user_id, read_status),
    INDEX idx_message_business (type, business_id)
) COMMENT '站内通知表';
