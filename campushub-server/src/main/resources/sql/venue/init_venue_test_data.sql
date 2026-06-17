USE campushub;

-- 场地测试数据
INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted
) VALUES
(
    1, '第一自习室', '自习室', '图书馆三楼', 80, '/images/venue/study-room-1.jpg',
    '适合日常自习和小组学习，靠近图书馆借阅区。',
    116.397128, 39.916527, 1, 0
),
(
    2, '羽毛球馆1号场', '运动场馆', '体育馆一层', 8, '/images/venue/badminton-court-1.jpg',
    '标准羽毛球场地，适合课余锻炼和社团活动。',
    116.398217, 39.917136, 1, 0
),
(
    3, '学生活动中心201', '活动室', '学生活动中心二楼', 60, '/images/venue/activity-room-201.jpg',
    '适合社团例会、培训分享和小型讲座。',
    116.396982, 39.915864, 1, 0
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

-- 2026-06-15 场地时间段
INSERT INTO venue_slot (
    id, venue_id, slot_date, start_time, end_time,
    max_capacity, available_capacity, status
) VALUES
(101, 1, '2026-06-15', '08:00:00', '10:00:00', 80, 32, 1),
(102, 1, '2026-06-15', '10:00:00', '12:00:00', 80, 0, 2),
(103, 1, '2026-06-15', '14:00:00', '16:00:00', 80, 24, 1),
(104, 1, '2026-06-15', '19:00:00', '21:00:00', 80, 10, 1),

(201, 2, '2026-06-15', '18:00:00', '19:00:00', 8, 4, 1),
(202, 2, '2026-06-15', '19:00:00', '20:00:00', 8, 0, 2),
(203, 2, '2026-06-15', '20:00:00', '21:00:00', 8, 6, 1),

(301, 3, '2026-06-15', '09:00:00', '11:00:00', 60, 20, 1),
(302, 3, '2026-06-15', '14:00:00', '16:00:00', 60, 60, 1),
(303, 3, '2026-06-15', '19:00:00', '21:00:00', 60, 12, 1)
ON DUPLICATE KEY UPDATE
    max_capacity = VALUES(max_capacity),
    available_capacity = VALUES(available_capacity),
    status = VALUES(status);

-- 2026-06-16 场地时间段
INSERT INTO venue_slot (
    id, venue_id, slot_date, start_time, end_time,
    max_capacity, available_capacity, status
) VALUES
(111, 1, '2026-06-16', '08:00:00', '10:00:00', 80, 40, 1),
(112, 1, '2026-06-16', '10:00:00', '12:00:00', 80, 36, 1),
(113, 1, '2026-06-16', '14:00:00', '16:00:00', 80, 18, 1),

(211, 2, '2026-06-16', '18:00:00', '19:00:00', 8, 5, 1),
(212, 2, '2026-06-16', '19:00:00', '20:00:00', 8, 3, 1),
(213, 2, '2026-06-16', '20:00:00', '21:00:00', 8, 2, 1),

(311, 3, '2026-06-16', '09:00:00', '11:00:00', 60, 28, 1),
(312, 3, '2026-06-16', '14:00:00', '16:00:00', 60, 15, 1),
(313, 3, '2026-06-16', '19:00:00', '21:00:00', 60, 60, 1)
ON DUPLICATE KEY UPDATE
    max_capacity = VALUES(max_capacity),
    available_capacity = VALUES(available_capacity),
    status = VALUES(status);
