-- 活动候补补位增强代码测试sql
USE campushub;

-- 1. 清理旧测试数据
DELETE FROM activity_signup WHERE activity_id = 9001;
DELETE FROM activity WHERE id = 9001;

-- 2. 创建“候补补位专用活动”
INSERT INTO activity (
    id,
    publisher_id,
    title,
    cover_url,
    content,
    location,
    venue_id,
    signup_start_time,
    signup_end_time,
    activity_start_time,
    activity_end_time,
    signup_limit,
    current_signup_count,
    wait_limit,
    status,
    audit_status,
    audit_remark,
    audit_user_id,
    audit_time,
    is_deleted,
    create_time,
    update_time
) VALUES (
             9001,
             (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1),
             '候补补位测试活动',
             '/images/activity/waitlist-test.jpg',
             '这是专门用于测试活动候补补位流程的活动。',
             '学生活动中心201',
             3,
             '2026-07-01 00:00:00',
             '2026-07-31 23:59:59',
             '2026-08-10 19:00:00',
             '2026-08-10 21:00:00',
             1,
             0,
             2,
             1,
             1,
             NULL,
             (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1),
             NOW(),
             0,
             NOW(),
             NOW()
         );

-- 3. 检查测试用户是否存在
SELECT id, username
FROM sys_user
WHERE username IN ('test_user_4', 'test_user_5');

-- 4. 检查测试活动
SELECT id, title, signup_limit, current_signup_count, wait_limit, status, audit_status
FROM activity
WHERE id = 9001;

-- 5. 测试结束后，如需查看报名结果，可执行
SELECT
    id,
    activity_id,
    user_id,
    signup_status,
    sign_status,
    wait_order,
    signup_time,
    cancel_time,
    sign_time
FROM activity_signup
WHERE activity_id = 9001
ORDER BY id ASC;

USE campushub;

UPDATE activity
SET status = 2,
    audit_status = 1,
    update_time = NOW()
WHERE id = 9001;