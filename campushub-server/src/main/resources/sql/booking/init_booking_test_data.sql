USE campushub;

-- 1. 准备测试用户
INSERT INTO sys_user (
    id, username, password, real_name, phone, email, avatar, student_no,
    credit_score, role, status, is_deleted
) VALUES
      (1, 'test_user_1', '123456', '测试用户1', '13800000001', 'test1@example.com', NULL, '20260001', 100, 0, 1, 0),
      (2, 'test_user_2', '123456', '测试用户2', '13800000002', 'test2@example.com', NULL, '20260002', 100, 0, 1, 0)
ON DUPLICATE KEY UPDATE
                     username = VALUES(username),
                     password = VALUES(password),
                     real_name = VALUES(real_name),
                     phone = VALUES(phone),
                     email = VALUES(email),
                     student_no = VALUES(student_no),
                     credit_score = VALUES(credit_score),
                     role = VALUES(role),
                     status = VALUES(status),
                     is_deleted = VALUES(is_deleted);

-- 2. 清空旧预约数据
DELETE FROM booking;

-- 3. 重置测试时间段容量
UPDATE venue_slot
SET available_capacity = 32, status = 1
WHERE id = 101;

UPDATE venue_slot
SET available_capacity = 0, status = 2
WHERE id = 102;

UPDATE venue_slot
SET available_capacity = 24, status = 1
WHERE id = 103;

UPDATE venue_slot
SET available_capacity = 10, status = 1
WHERE id = 104;
