USE campushub;

INSERT INTO sys_user (
    id,
    username,
    password,
    real_name,
    phone,
    email,
    avatar,
    student_no,
    credit_score,
    role,
    status,
    is_deleted
) VALUES (
    1001,
    'admin',
    '123456',
    '系统管理员',
    '13800009999',
    'admin@campushub.com',
    NULL,
    'ADMIN0001',
    100,
    1,
    1,
    0
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
    is_deleted = VALUES(is_deleted);
