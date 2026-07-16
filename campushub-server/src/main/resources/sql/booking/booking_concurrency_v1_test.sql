USE campushub;

-- Redis 场地预约并发优化 V1 测试数据
-- 用途：
-- 1. 同一用户重复提交预约请求
-- 2. 多用户并发抢同一时间段容量
-- 3. 取消预约后验证 Redis 和数据库容量回补

-- =========================================================
-- 一、固定测试用户
-- 登录账号：
-- booking_user_a / 123456
-- booking_user_b / 123456
-- booking_user_c / 123456
-- booking_user_d / 123456
-- =========================================================

INSERT INTO sys_user (
    id, username, password, real_name, phone, email, avatar, student_no,
    credit_score, role, status, is_deleted, create_time, update_time
) VALUES
(
    9301, 'booking_user_a', '123456', '预约并发测试用户A',
    '13800930001', 'booking_user_a@campushub.com', NULL, 'BOOKING9301',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9302, 'booking_user_b', '123456', '预约并发测试用户B',
    '13800930002', 'booking_user_b@campushub.com', NULL, 'BOOKING9302',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9303, 'booking_user_c', '123456', '预约并发测试用户C',
    '13800930003', 'booking_user_c@campushub.com', NULL, 'BOOKING9303',
    100, 0, 1, 0, NOW(), NOW()
),
(
    9304, 'booking_user_d', '123456', '预约并发测试用户D',
    '13800930004', 'booking_user_d@campushub.com', NULL, 'BOOKING9304',
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
-- 二、固定测试场地和时间段
-- 设计：
-- slotId = 9301
-- maxCapacity = 2
-- availableCapacity = 2
-- 多用户并发时，最多只能成功预约 2 人。
-- =========================================================

INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted, create_time, update_time
) VALUES (
    9301, '预约并发测试场地', '自习室', '预约测试楼 101', 2,
    '/images/venue/booking-concurrency-room.jpg',
    '用于 Redis 场地预约并发优化 V1 测试的固定场地。',
    116.402001, 39.902001, 1, 0, NOW(), NOW()
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

DELETE FROM booking
WHERE slot_id = 9301;

DELETE FROM message
WHERE user_id IN (9301, 9302, 9303, 9304)
  AND type = 1;

INSERT INTO venue_slot (
    id, venue_id, slot_date, start_time, end_time,
    max_capacity, available_capacity, status, create_time, update_time
) VALUES (
    9301, 9301, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', '11:00:00',
    2, 2, 1, NOW(), NOW()
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
-- 三、初始化后校验 SQL
-- =========================================================

SELECT id, username, credit_score, role, status
FROM sys_user
WHERE id IN (9301, 9302, 9303, 9304)
ORDER BY id;

SELECT id, venue_id, slot_date, start_time, end_time, max_capacity, available_capacity, status
FROM venue_slot
WHERE id = 9301;

SELECT id, booking_no, user_id, venue_id, slot_id, person_count, status, create_time
FROM booking
WHERE slot_id = 9301
ORDER BY id;
