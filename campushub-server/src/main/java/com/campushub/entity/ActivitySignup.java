package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySignup {

    /** 主键ID */
    private Long id;
    /** 活动ID */
    private Long activityId;
    /** 用户ID */
    private Long userId;
    /** 报名时间 */
    private LocalDateTime signupTime;
    /** 报名状态，ActivitySignupStatusConstant */
    private Integer signupStatus;
    /** 签到状态，ActivitySignStatusConstant */
    private Integer signStatus;
    /** 签到时间 */
    private LocalDateTime signTime;
    /** 取消时间 */
    private LocalDateTime cancelTime;
    /** 候补顺位 */
    private Integer waitOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
