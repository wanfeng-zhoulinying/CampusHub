USE campushub;

-- Redis 活动报名高并发优化 V1 测试数据
-- 用途：
-- 1. 测活动报名短期幂等
-- 2. 测正式名额不能超卖
-- 3. 测候补名额分配
-- 4. 测候补满后继续报名失败
-- 5. 测取消报名后的 Redis 计数回补

-- =========================================================
-- 一、固定测试用户
-- 登录账号：
-- signup_user_a / 123456
-- signup_user_b / 123456
-- signup_user_c / 123456
-- =========================================================

INSERT INTO sys_user (
    id, username, password, real_name, phone, email, avatar, student_no,
    credit_score, role, status, is_deleted, create_time, update_time
) VALUES
(
    9201, 'signup_user_a', '123456', '报名并发测试用户A',
    '13800920001', 'signup_user_a@campushub.com', NULL, 'SIGNUP9201',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9202, 'signup_user_b', '123456', '报名并发测试用户B',
    '13800920002', 'signup_user_b@campushub.com', NULL, 'SIGNUP9202',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9203, 'signup_user_c', '123456', '报名并发测试用户C',
    '13800920003', 'signup_user_c@campushub.com', NULL, 'SIGNUP9203',
    100, 0, 1, 0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    email = VALUES(email),
    avatar = VALUES(avatar),
    student_no = VALUES(student_no),
    credit_score = VALUES(credit_score),
    role = VALUES(role),
    status = VALUES(status),
    is_deleted = VALUES(is_deleted),
    update_time = NOW();

-- =========================================================
-- 二、固定测试场地
-- =========================================================

INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted, create_time, update_time
) VALUES (
    9201, '报名并发测试场地', '活动室', '报名测试楼 201', 50,
    '/images/venue/signup-concurrency-room.jpg',
    '用于 Redis 活动报名高并发优化 V1 测试的固定场地。',
    116.401001, 39.901001, 1, 0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    location = VALUES(location),
    capacity = VALUES(capacity),
    cover_url = VALUES(cover_url),
    description = VALUES(description),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    status = VALUES(status),
    is_deleted = VALUES(is_deleted),
    update_time = NOW();

-- =========================================================
-- 三、固定测试活动
-- 设计：
-- signupLimit = 1
-- waitLimit = 1
-- 这样便于点对点验证：
-- 第一个用户 -> 正式报名
-- 第二个用户 -> 候补
-- 第三个用户 -> 失败
-- =========================================================

DELETE FROM activity_signup
WHERE activity_id = 9201;

DELETE FROM message
WHERE business_id = 9201
  AND type = 2;

INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit,
    status, audit_status, audit_remark, audit_user_id, audit_time,
    is_deleted, create_time, update_time
) VALUES (
    9201, 1001, '报名高并发测试活动', '/images/activity/signup-concurrency-test.jpg',
    '用于 Redis 活动报名高并发优化 V1 测试的固定活动。',
    '报名测试楼 201', 9201,
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 2 HOUR),
    1, 0, 1,
    2, 1, '报名并发测试已审核通过', 1001, NOW(),
    0, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    publisher_id = VALUES(publisher_id),
    title = VALUES(title),
    cover_url = VALUES(cover_url),
    content = VALUES(content),
    location = VALUES(location),
    venue_id = VALUES(venue_id),
    signup_start_time = VALUES(signup_start_time),
    signup_end_time = VALUES(signup_end_time),
    activity_start_time = VALUES(activity_start_time),
    activity_end_time = VALUES(activity_end_time),
    signup_limit = VALUES(signup_limit),
    current_signup_count = 0,
    wait_limit = VALUES(wait_limit),
    status = VALUES(status),
    audit_status = VALUES(audit_status),
    audit_remark = VALUES(audit_remark),
    audit_user_id = VALUES(audit_user_id),
    audit_time = VALUES(audit_time),
    is_deleted = VALUES(is_deleted),
    update_time = NOW();

-- =========================================================
-- 四、初始化后校验 SQL
-- =========================================================

SELECT id, username, credit_score, role, status
FROM sys_user
WHERE id IN (9201, 9202, 9203)
ORDER BY id;

SELECT id, title, signup_limit, current_signup_count, wait_limit, status, audit_status,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE id = 9201;

SELECT id, activity_id, user_id, signup_status, wait_order
FROM activity_signup
WHERE activity_id = 9201
ORDER BY id;
