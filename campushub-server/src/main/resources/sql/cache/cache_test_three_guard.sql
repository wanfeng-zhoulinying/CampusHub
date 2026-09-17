-- =========================================================================
-- CampusHub 缓存三防测试 · 固定样本数据 SQL
-- 用途：创建"缓存永久稳定、不被定时任务干扰"的活动/场地样本，
--       供缓存穿透 / 击穿 / 雪崩测试使用。
--
-- 执行方式：mysql 客户端或命令行执行本文件。
--   mysql -h 127.0.0.1 -P 3306 -uroot -p campushub < 本文件路径
--
-- 说明：
--   1) 造一个归档活动 id=9501 和归档场地 id=9501。
--   2) 9501 活动 status=4(已结束)、signup/activity 时间全部在 2020 年，
--      定时任务 refreshActivityStatus 只流转 status∈{1,2,3} 的活动，
--      因此 9501 永不发生状态流转 —— ScheduleTask 不会 deleteByPrefix
--      删掉它的详情缓存，缓存完全由测试者手动控制。
--   3) 场地没有任何定时任务，9501 场地缓存天然稳定。
-- =========================================================================

-- ---------- 1. 容错：若以前执行过本脚本，先清掉旧样本 ----------
DELETE FROM activity      WHERE id = 9501;
DELETE FROM venue         WHERE id = 9501;
DELETE FROM venue_slot    WHERE venue_id = 9501;

-- ---------- 2. 造"归档固定活动样本 9501"（排除定时任务干扰） ----------
INSERT INTO activity (
    id, publisher_id, title, cover_url, content, location, venue_id,
    signup_start_time, signup_end_time, activity_start_time, activity_end_time,
    signup_limit, current_signup_count, wait_limit,
    status, audit_status, audit_remark, audit_user_id, audit_time,
    is_deleted, create_time, update_time
) VALUES (
    9501, 1,
    '缓存三防测试-归档固定样本-已结束',
    '/images/activity/poster-code.jpg',
    '用于 Redis 缓存穿透/击穿/雪崩测试的归档固定活动，status=4 已结束，时间全部在过去，不参与任何定时任务状态流转。',
    '缓存测试楼 501', 9501,
    '2020-01-01 08:00:00', '2020-01-02 18:00:00', '2020-01-03 19:00:00', '2020-01-03 21:00:00',
    999, 0, 199,
    4, 1, '缓存测试固定样本', 1001, '2020-01-03 00:00:00',
    0, '2020-01-03 08:00:00', '2020-01-03 08:00:00'
);

-- ---------- 3. 造"归档固定场地样本 9501" ----------
INSERT INTO venue (
    id, name, category, location, capacity, cover_url, description,
    longitude, latitude, status, is_deleted, create_time, update_time
) VALUES (
    9501, '缓存三防测试场地-固定样本', '活动室', '缓存测试楼 501', 100,
    '/images/venue/cache-test-room.jpg',
    '用于 Redis 缓存穿透/击穿/雪崩测试的归档固定场地。',
    116.410001, 39.910001, 1, 0, '2020-01-03 08:00:00', '2020-01-03 08:00:00'
);

-- ---------- 4. 校验样本已就绪 ----------
SELECT id, title, status, is_deleted,
       signup_start_time, signup_end_time,
       activity_start_time, activity_end_time
FROM activity WHERE id = 9501;
-- 期望：status=4(已结束)，四段时间都在 2020 年 = 定时任务永不再流转它

SELECT id, name, status, is_deleted FROM venue WHERE id = 9501;
-- 期望：status=1，is_deleted=0

-- ---------- 5. 确认"穿透反例 ID"确实不存在（穿透测试前提） ----------
SELECT COUNT(*) AS not_exists_activity FROM activity WHERE id = 999999999;  -- 期望 0
SELECT COUNT(*) AS not_exists_venue   FROM venue    WHERE id = 999999999;  -- 期望 0