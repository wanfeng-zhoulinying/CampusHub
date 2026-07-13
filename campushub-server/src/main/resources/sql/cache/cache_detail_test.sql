USE campushub;

-- Redis 详情缓存测试数据
-- 说明：
-- 1. 第一部分是稳定详情测试数据，时间放在远未来，避免被定时任务影响。
-- 2. 第二部分是定时任务批量删活动缓存测试数据，专门用于触发活动状态自动流转。

-- =========================================================
-- 一、稳定详情缓存测试数据
-- 用途：
-- 1. 测活动详情缓存命中
-- 2. 测场地详情缓存命中
-- 3. 测后台修改后删缓存再重建
-- 建议测试 ID：
-- 场地：9001
-- 活动：9001
-- =========================================================

INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted
) VALUES (
    9001, '缓存测试场地-固定样本', '活动室', '缓存测试楼 301', 120,
    '/images/venue/cache-test-room.jpg',
    '用于 Redis 详情缓存测试的固定场地，不参与预约定时任务流转。',
    116.400001, 39.900001, 1, 0
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
    is_deleted = VALUES(is_deleted);

INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit, status, audit_status,
    audit_remark, audit_user_id, audit_time, is_deleted, create_time, update_time
) VALUES (
    9001, 1, '缓存测试活动-固定样本', '/images/activity/cache-test-poster.jpg',
    '用于 Redis 活动详情缓存测试的固定活动，不参与当前定时任务状态流转。',
    '缓存测试楼 301', 9001,
    '2030-01-01 08:00:00', '2030-01-02 18:00:00',
    '2030-01-05 19:00:00', '2030-01-05 21:00:00',
    80, 12, 10, 1, 1,
    '缓存测试数据审核通过', 1001, NOW(), 0, NOW(), NOW()
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
    current_signup_count = VALUES(current_signup_count),
    wait_limit = VALUES(wait_limit),
    status = VALUES(status),
    audit_status = VALUES(audit_status),
    audit_remark = VALUES(audit_remark),
    audit_user_id = VALUES(audit_user_id),
    audit_time = VALUES(audit_time),
    is_deleted = VALUES(is_deleted),
    update_time = NOW();

-- 可选校验
SELECT id, name, status, is_deleted
FROM venue
WHERE id = 9001;

SELECT id, title, status, audit_status, venue_id, is_deleted,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE id = 9001;

-- =========================================================
-- 二、定时任务批量删活动缓存测试数据
-- 用途：
-- 1. 先调 GET /activity/9101 ~ 9104 写入 Redis 活动详情缓存
-- 2. 等待定时任务执行
-- 3. 观察控制台是否出现：
--    [RedisCache] 按前缀批量删除缓存 keyPrefix=campushub:cache:activity:detail:
-- 4. 再次调活动详情接口，确认缓存被重建
-- 预期状态流转：
-- 9101: 1 -> 2
-- 9102: 2 -> 6
-- 9103: 6 -> 3
-- 9104: 3 -> 4
-- =========================================================

INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit, status, audit_status,
    audit_remark, audit_user_id, audit_time, is_deleted, create_time, update_time
) VALUES
(
    9101, 1, '缓存测试-定时任务-进入报名中', '/images/activity/cache-task-1.jpg',
    '用于测试定时任务触发后按前缀批量删除活动详情缓存。', '缓存测试楼 301', 9001,
    DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE),
    DATE_ADD(NOW(), INTERVAL 40 MINUTE), DATE_ADD(NOW(), INTERVAL 60 MINUTE),
    100, 0, 10, 1, 1,
    '缓存批量删除测试', 1001, NOW(), 0, NOW(), NOW()
),
(
    9102, 1, '缓存测试-定时任务-报名结束待开始', '/images/activity/cache-task-2.jpg',
    '用于测试定时任务触发后按前缀批量删除活动详情缓存。', '缓存测试楼 301', 9001,
    DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE),
    DATE_ADD(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 40 MINUTE),
    100, 0, 10, 2, 1,
    '缓存批量删除测试', 1001, NOW(), 0, NOW(), NOW()
),
(
    9103, 1, '缓存测试-定时任务-进入进行中', '/images/activity/cache-task-3.jpg',
    '用于测试定时任务触发后按前缀批量删除活动详情缓存。', '缓存测试楼 301', 9001,
    DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE),
    DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE),
    100, 0, 10, 6, 1,
    '缓存批量删除测试', 1001, NOW(), 0, NOW(), NOW()
),
(
    9104, 1, '缓存测试-定时任务-进入已结束', '/images/activity/cache-task-4.jpg',
    '用于测试定时任务触发后按前缀批量删除活动详情缓存。', '缓存测试楼 301', 9001,
    DATE_SUB(NOW(), INTERVAL 70 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE),
    DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE),
    100, 0, 10, 3, 1,
    '缓存批量删除测试', 1001, NOW(), 0, NOW(), NOW()
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
    current_signup_count = VALUES(current_signup_count),
    wait_limit = VALUES(wait_limit),
    status = VALUES(status),
    audit_status = VALUES(audit_status),
    audit_remark = VALUES(audit_remark),
    audit_user_id = VALUES(audit_user_id),
    audit_time = VALUES(audit_time),
    is_deleted = VALUES(is_deleted),
    update_time = NOW();

-- 初始化后立即查询，确认当前是 1 / 2 / 6 / 3
SELECT id, title, status, audit_status,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE id IN (9101, 9102, 9103, 9104)
ORDER BY id;

-- 等待定时任务执行后再次查询，预期变为 2 / 6 / 3 / 4
SELECT id, title, status, audit_status,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE id IN (9101, 9102, 9103, 9104)
ORDER BY id;

-- =========================================================
-- 三、可选清理 SQL
-- 说明：
-- 1. 如果你想重复做“首次查询写缓存”测试，可以删测试活动/场地后重跑本文件。
-- 2. Redis key 仍建议在 Another Redis Desktop Manager 中手动删，更直观。
-- =========================================================

-- DELETE FROM activity WHERE id IN (9001, 9101, 9102, 9103, 9104);
-- DELETE FROM venue WHERE id = 9001;
