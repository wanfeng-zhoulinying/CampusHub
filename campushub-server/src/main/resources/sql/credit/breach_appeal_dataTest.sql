USE campushub;

SET @user4_id = (SELECT id FROM sys_user WHERE username = 'test_user_4' LIMIT 1);
SET @admin_id = (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);
SET @slot_id = (SELECT id FROM venue_slot ORDER BY id LIMIT 1);
SET @venue_id = (SELECT venue_id FROM venue_slot WHERE id = @slot_id);
SET @slot_date = (SELECT slot_date FROM venue_slot WHERE id = @slot_id);
SET @start_time = (SELECT start_time FROM venue_slot WHERE id = @slot_id);
SET @end_time = (SELECT end_time FROM venue_slot WHERE id = @slot_id);

-- 如果 user4 / admin / venue_slot 没查到，先补基础测试数据
SELECT @user4_id AS user4_id, @admin_id AS admin_id, @slot_id AS slot_id;

-- 清理旧数据
DELETE FROM booking_breach_appeal WHERE booking_id = 9101;
DELETE FROM credit_record WHERE business_id = 9101 AND business_type IN (1, 2);
DELETE FROM booking WHERE id = 9101;

-- 先把 user4 信用分设回 90，模拟“已扣 10 分的违约用户”
UPDATE sys_user
SET credit_score = 90,
    update_time = NOW()
WHERE id = @user4_id;

-- 插入一条“已违约”的预约
INSERT INTO booking (
    id,
    booking_no,
    user_id,
    venue_id,
    slot_id,
    booking_date,
    start_time,
    end_time,
    person_count,
    status,
    cancel_reason,
    checkin_time,
    breach_flag,
    remark,
    create_time,
    update_time
) VALUES (
             9101,
             'BKAPPEAL9101',
             @user4_id,
             @venue_id,
             @slot_id,
             @slot_date,
             @start_time,
             @end_time,
             1,
             4,
             NULL,
             NULL,
             1,
             '违约申诉测试预约',
             NOW(),
             NOW()
         );

-- 插入一条“违约扣分记录”
INSERT INTO credit_record (
    user_id,
    change_type,
    change_score,
    current_score,
    reason,
    business_type,
    business_id,
    operator_id,
    create_time,
    update_time
) VALUES (
             @user4_id,
             2,
             10,
             90,
             '测试：管理员登记预约违约',
             1,
             9101,
             @admin_id,
             NOW(),
             NOW()
         );

-- 检查初始状态
SELECT id, booking_no, user_id, status, breach_flag
FROM booking
WHERE id = 9101;

SELECT id, username, credit_score
FROM sys_user
WHERE id = @user4_id;

SELECT id, user_id, change_type, change_score, current_score, business_type, business_id
FROM credit_record
WHERE business_id = 9101
ORDER BY id;