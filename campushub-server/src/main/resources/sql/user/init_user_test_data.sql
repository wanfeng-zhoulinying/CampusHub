USE campushub;

INSERT INTO sys_user (
    id, username, password, real_name, phone, email, avatar, student_no,
    credit_score, status, is_deleted
) VALUES
    (1, 'test_user_1', '123456', '测试用户1', '13800000001', 'test1@example.com', NULL, '20260001', 100, 1, 0),
    (2, 'test_user_2', '123456', '测试用户2', '13800000002', 'test2@example.com', NULL, '20260002', 100, 1, 0),
    (3, 'test_user_3', '123456', '测试用户3', '13800000003', 'test3@example.com', NULL, '20260003', 100, 1, 0)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password = VALUES(password),
    real_name = VALUES(real_name),
    phone = VALUES(phone),
    email = VALUES(email),
    avatar = VALUES(avatar),
    student_no = VALUES(student_no),
    credit_score = VALUES(credit_score),
    status = VALUES(status),
    is_deleted = VALUES(is_deleted);
