package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingBreachAppeal {

    /** 主键ID */
    private Long id;
    /** 预约ID */
    private Long bookingId;
    /** 用户ID */
    private Long userId;
    /** 申诉原因 */
    private String reason;
    /** 申诉状态，BookingAppealStatusConstant */
    private Integer appealStatus;
    /** 违约扣分分值快照 */
    private Integer deductScore;
    /** 审核备注 */
    private String auditRemark;
    /** 审核人ID */
    private Long auditUserId;
    /** 审核时间 */
    private LocalDateTime auditTime;
    /** 删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
