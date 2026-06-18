USE campushub;

-- 活动测试数据
INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit, status, audit_status,
    audit_remark, audit_user_id, audit_time, is_deleted
) VALUES
(
    1, 1, '考研经验分享会', '/images/activity/poster-kaoyan.jpg',
    '邀请高年级学长分享考研备考规划、资料选择和复习节奏。',
    '学生活动中心201', 3,
    '2026-06-14 08:00:00', '2026-06-15 20:00:00',
    '2026-06-18 19:00:00', '2026-06-18 21:00:00',
    60, 18, 10, 2, 1,
    NULL, 1, '2026-06-13 18:00:00', 0
),
(
    2, 1, '羽毛球友谊赛', '/images/activity/poster-badminton.jpg',
    '面向全校同学的羽毛球交流赛，欢迎组队报名参与。',
    '体育馆一层', 2,
    '2026-06-14 09:00:00', '2026-06-16 18:00:00',
    '2026-06-20 14:00:00', '2026-06-20 17:00:00',
    32, 30, 8, 2, 1,
    NULL, 1, '2026-06-13 18:10:00', 0
),
(
    3, 2, '图书馆自习打卡活动', '/images/activity/poster-study.jpg',
    '通过连续签到完成自习任务，可获得学习积分和纪念徽章。',
    '图书馆三楼', 1,
    '2026-06-15 08:00:00', '2026-06-17 20:00:00',
    '2026-06-19 08:30:00', '2026-06-19 12:00:00',
    80, 42, 20, 2, 1,
    NULL, 1, '2026-06-13 18:20:00', 0
),
(
    4, 2, '摄影入门工作坊', '/images/activity/poster-photo.jpg',
    '讲解手机摄影构图、光线和后期基础，适合零基础同学参加。',
    '学生活动中心201', 3,
    '2026-06-16 10:00:00', '2026-06-18 20:00:00',
    '2026-06-22 19:00:00', '2026-06-22 21:30:00',
    50, 12, 10, 1, 1,
    NULL, 1, '2026-06-13 18:30:00', 0
),
(
    5, 1, '编程交流夜', '/images/activity/poster-code.jpg',
    '围绕 Java、前端和项目实战展开自由交流，适合想做项目的同学参加。',
    '学生活动中心201', 3,
    '2026-06-17 09:00:00', '2026-06-19 20:00:00',
    '2026-06-23 19:00:00', '2026-06-23 21:00:00',
    40, 0, 10, 1, 0,
    NULL, NULL, NULL, 0
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
    is_deleted = VALUES(is_deleted);

-- 活动报名测试数据
INSERT INTO activity_signup (
    id, activity_id, user_id, signup_time, signup_status, sign_status, wait_order
) VALUES
(1, 1, 1, '2026-06-14 09:00:00', 1, 0, NULL),
(2, 1, 2, '2026-06-14 09:05:00', 1, 0, NULL),
(3, 2, 1, '2026-06-15 10:00:00', 1, 0, NULL),
(4, 3, 2, '2026-06-15 11:00:00', 1, 0, NULL)
ON DUPLICATE KEY UPDATE
    activity_id = VALUES(activity_id),
    user_id = VALUES(user_id),
    signup_time = VALUES(signup_time),
    signup_status = VALUES(signup_status),
    sign_status = VALUES(sign_status),
    wait_order = VALUES(wait_order);
