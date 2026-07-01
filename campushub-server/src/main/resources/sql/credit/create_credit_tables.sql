DROP TABLE credit_record;

CREATE TABLE IF NOT EXISTS credit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    change_type INT NOT NULL COMMENT '变动类型：1加分 2扣分',
    change_score INT NOT NULL COMMENT '变动分值',
    current_score INT NOT NULL COMMENT '变动后信用分',
    reason VARCHAR(255) NOT NULL COMMENT '变动原因',
    business_type INT NOT NULL COMMENT '业务类型：1预约违约',
    business_id BIGINT DEFAULT NULL COMMENT '业务ID',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_credit_record_user_id (user_id),
    INDEX idx_credit_record_business (business_type, business_id)
) COMMENT='信用分变动记录表';
