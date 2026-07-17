USE campushub;

-- Redis ZSet 热门活动 / 热门场地排行 V1 测试数据
-- 用途：
-- 1. 测试活动详情、活动报名、活动收藏会增加活动热度
-- 2. 测试场地详情、场地时间段、场地预约会增加场地热度
-- 3. 测试 /activity/hot 和 /venue/hot 会按 Redis ZSet 分数倒序返回
--
-- RESP 里建议先执行：
-- DEL campushub:rank:hot:activity campushub:rank:hot:venue
-- DEL campushub:cache:activity:detail:9401 campushub:cache:activity:detail:9402
-- DEL campushub:cache:venue:detail:9401 campushub:cache:venue:detail:9402
-- DEL campushub:activity:signup:stock:9401 campushub:activity:signup:wait:9401 campushub:activity:signup:request:9401:9401
-- DEL campushub:booking:slot:stock:9401 campushub:booking:request:9401:9401

-- =========================================================
-- 一、固定测试用户
-- 登录账号：
-- rank_user_a / 123456
-- rank_user_b / 123456
-- =========================================================

INSERT INTO sys_user (
    id, username, password, real_name, phone, email, avatar, student_no,
    credit_score, role, status, is_deleted, create_time, update_time
) VALUES
(
    9401, 'rank_user_a', '123456', '热门排行测试用户A',
    '13800940001', 'rank_user_a@campushub.com', NULL, 'RANK9401',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9402, 'rank_user_b', '123456', '热门排行测试用户B',
    '13800940002', 'rank_user_b@campushub.com', NULL, 'RANK9402',
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
-- 设计：
-- venueId = 9401 用来制造高热度
-- venueId = 9402 用来做排序对照
-- =========================================================

DELETE FROM booking
WHERE slot_id IN (9401, 9402);

DELETE FROM message
WHERE user_id IN (9401, 9402);

INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted, create_time, update_time
) VALUES
(
    9401, '热门排行测试场地A', '自习室', '排行测试楼 401', 20,
    '/images/venue/hot-rank-a.jpg',
    '用于 Redis ZSet 热门场地排行测试的主测试场地。',
    116.404001, 39.904001, 1, 0, NOW(), NOW()
),
(
    9402, '热门排行测试场地B', '活动室', '排行测试楼 402', 20,
    '/images/venue/hot-rank-b.jpg',
    '用于 Redis ZSet 热门场地排行测试的对照场地。',
    116.404002, 39.904002, 1, 0, NOW(), NOW()
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

INSERT INTO venue_slot (
    id, venue_id, slot_date, start_time, end_time,
    max_capacity, available_capacity, status, create_time, update_time
) VALUES
(
    9401, 9401, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', '11:00:00',
    20, 20, 1, NOW(), NOW()
),
(
    9402, 9402, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '16:00:00',
    20, 20, 1, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    venue_id = VALUES(venue_id),
    slot_date = VALUES(slot_date),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    max_capacity = VALUES(max_capacity),
    available_capacity = VALUES(available_capacity),
    status = VALUES(status),
    update_time = NOW();

-- =========================================================
-- 三、固定测试活动
-- 设计：
-- activityId = 9401 用来制造高热度
-- activityId = 9402 用来做排序对照
-- =========================================================

DELETE FROM activity_signup
WHERE activity_id IN (9401, 9402);

DELETE FROM activity_favorite
WHERE activity_id IN (9401, 9402)
  AND user_id IN (9401, 9402);

INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit,
    status, audit_status, audit_remark, audit_user_id, audit_time,
    is_deleted, create_time, update_time
) VALUES
(
    9401, 1001, '热门排行测试活动A', '/images/activity/hot-rank-a.jpg',
    '用于 Redis ZSet 热门活动排行测试的主测试活动。',
    '排行测试楼 401', 9401,
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 2 HOUR),
    20, 0, 5,
    2, 1, '热门排行测试已审核通过', 1001, NOW(),
    0, NOW(), NOW()
),
(
    9402, 1001, '热门排行测试活动B', '/images/activity/hot-rank-b.jpg',
    '用于 Redis ZSet 热门活动排行测试的对照活动。',
    '排行测试楼 402', 9402,
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    DATE_ADD(NOW(), INTERVAL 30 MINUTE),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 2 HOUR),
    20, 0, 5,
    2, 1, '热门排行测试已审核通过', 1001, NOW(),
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
WHERE id IN (9401, 9402)
ORDER BY id;

SELECT id, title, signup_limit, current_signup_count, wait_limit, status, audit_status,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE id IN (9401, 9402)
ORDER BY id;

SELECT id, name, category, capacity, status
FROM venue
WHERE id IN (9401, 9402)
ORDER BY id;

SELECT id, venue_id, slot_date, start_time, end_time, max_capacity, available_capacity, status
FROM venue_slot
WHERE id IN (9401, 9402)
ORDER BY id;
