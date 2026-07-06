CREATE TABLE IF NOT EXISTS booking_breach_appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    booking_id BIGINT NOT NULL COMMENT '预约ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reason VARCHAR(255) NOT NULL COMMENT '申诉原因',
    appeal_status INT NOT NULL DEFAULT 0 COMMENT '申诉状态：0待审核 1通过 2驳回',
    deduct_score INT NOT NULL COMMENT '违约扣分分值快照',
    audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    audit_user_id BIGINT DEFAULT NULL COMMENT '审核人ID',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    is_deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_booking_breach_appeal_booking_id (booking_id),
    INDEX idx_booking_breach_appeal_user_id (user_id),
    INDEX idx_booking_breach_appeal_status (appeal_status)
) COMMENT='预约违约申诉表';
