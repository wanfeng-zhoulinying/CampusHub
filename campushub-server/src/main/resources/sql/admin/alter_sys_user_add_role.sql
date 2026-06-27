USE campushub;

ALTER TABLE sys_user
    ADD COLUMN role INT NOT NULL DEFAULT 0 COMMENT '用户角色：0普通用户 1管理员' AFTER credit_score;
