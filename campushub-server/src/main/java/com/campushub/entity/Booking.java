package com.campushub.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class Booking {

    /** 主键ID */
    private Long id;
    /** 预约单号 */
    private String bookingNo;
    /** 用户ID */
    private Long userId;
    /** 场地ID */
    private Long venueId;
    /** 时间段ID */
    private Long slotId;
    /** 预约日期 */
    private LocalDate bookingDate;
    /** 预约开始时间 */
    private LocalTime startTime;
    /** 预约结束时间 */
    private LocalTime endTime;
    /** 预约人数 */
    private Integer personCount;
    /** 预约状态：1待使用 2已完成 3已取消 4已违约 */
    private Integer status;
    /** 取消原因 */
    private String cancelReason;
    /** 核销时间 */
    private LocalDateTime checkinTime;
    /** 是否违约：0否 1是 */
    private Integer breachFlag;
    /** 备注 */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
