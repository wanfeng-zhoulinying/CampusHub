USE campushub;

CREATE TABLE IF NOT EXISTS activity_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数量',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_activity_comment_activity (activity_id),
    INDEX idx_activity_comment_user (user_id)
) COMMENT '活动评论表';

CREATE TABLE IF NOT EXISTS activity_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '点赞状态：0无效 1有效',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment_like_user (user_id)
) COMMENT '活动评论点赞表';

CREATE TABLE IF NOT EXISTS activity_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '收藏用户ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '收藏状态：0无效 1有效',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_activity_user (activity_id, user_id),
    INDEX idx_favorite_user (user_id)
) COMMENT '活动收藏表';
