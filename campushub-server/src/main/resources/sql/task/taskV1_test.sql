-- 活动状态自动流转 和 预约超时未核销自动违约扣分测试

-- 一、活动状态自动流转
-- 1.初始化数据
INSERT INTO activity (
    publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit,
    status, audit_status, audit_remark, audit_user_id, audit_time,
    is_deleted, create_time, update_time
) VALUES
      (1, '定时任务测试-进入报名中', '/images/activity/poster-code.jpg', '测试活动状态流转', '测试地点', 1,
       DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 10 MINUTE),
       DATE_ADD(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 30 MINUTE),
       100, 0, 10, 1, 1, '测试通过', 1001, NOW(), 0, NOW(), NOW()),

      (1, '定时任务测试-报名结束待开始', '/images/activity/poster-code.jpg', '测试活动状态流转', '测试地点', 1,
       DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE),
       DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 20 MINUTE),
       100, 0, 10, 2, 1, '测试通过', 1001, NOW(), 0, NOW(), NOW()),

      (1, '定时任务测试-进入进行中', '/images/activity/poster-code.jpg', '测试活动状态流转', '测试地点', 1,
       DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE),
       DATE_SUB(NOW(), INTERVAL 1 MINUTE), DATE_ADD(NOW(), INTERVAL 10 MINUTE),
       100, 0, 10, 6, 1, '测试通过', 1001, NOW(), 0, NOW(), NOW()),

      (1, '定时任务测试-进入已结束', '/images/activity/poster-code.jpg', '测试活动状态流转', '测试地点', 1,
       DATE_SUB(NOW(), INTERVAL 40 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE),
       DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 1 MINUTE),
       100, 0, 10, 3, 1, '测试通过', 1001, NOW(), 0, NOW(), NOW());

-- 等 1 分钟后查,预期2634
SELECT id, title, status, audit_status,
       signup_start_time, signup_end_time, activity_start_time, activity_end_time
FROM activity
WHERE title LIKE '定时任务测试-%'
ORDER BY id DESC;

-- 二、预约超时未核销自动违约扣分
-- 先记录用户当前信用分
SELECT id, username, credit_score
FROM sys_user
WHERE id = 5;
-- 插入一条已经过期、未核销、未违约的预约
INSERT INTO booking (
    booking_no, user_id, venue_id, slot_id,
    booking_date, start_time, end_time,
    person_count, status, cancel_reason, checkin_time,
    breach_flag, remark, create_time, update_time
) VALUES (
             CONCAT('BKAUTO', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s')),
             5, 1, 101,
             CURDATE(),
             DATE_SUB(CURTIME(), INTERVAL 2 HOUR),
             DATE_SUB(CURTIME(), INTERVAL 1 HOUR),
             1, 1, NULL, NULL,
             0, '定时任务自动违约测试', NOW(), NOW()
         );
-- 等一分钟后查，预期status = 4，breach_flag = 1
SELECT id, booking_no, user_id, status, breach_flag, remark, update_time
FROM booking
WHERE remark = '定时任务自动违约测试'
ORDER BY id DESC
LIMIT 1;

-- 再查信用分记录
SELECT id, user_id, change_type, change_score, current_score, reason, business_type, business_id, operator_id, create_time
FROM credit_record
WHERE user_id = 5
ORDER BY id DESC
LIMIT 5;

-- 最后查消息
SELECT id, user_id, title, content, type, business_id, read_status, create_time
FROM message
WHERE user_id = 5
ORDER BY id DESC
LIMIT 5;