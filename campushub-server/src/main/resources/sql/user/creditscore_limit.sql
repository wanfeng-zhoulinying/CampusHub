-- 信用分驱动限制专用sql

select id, username, credit_score
from sys_user
where username = 'test_user_4';

update sys_user
set credit_score = 50,
    update_time = now()
where username = 'test_user_4';

update sys_user
set credit_score = 100,
    update_time = now()
where username = 'test_user_4';

update sys_user
set credit_score = 60,
    update_time = now()
where username = 'test_user_4';