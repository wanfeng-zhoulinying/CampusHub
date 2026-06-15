package com.campushub.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class VenueSlot {

    private Long id;
    /** 所属场地ID */
    private Long venueId;
    /** 时间段对应日期 */
    private LocalDate slotDate;
    /** 时间段开始时间 */
    private LocalTime startTime;
    /** 时间段结束时间 */
    private LocalTime endTime;
    /** 当前时间段最大容量 */
    private Integer maxCapacity;
    /** 当前时间段剩余可预约容量 */
    private Integer availableCapacity;
    /** 时间段状态：1可预约 0关闭 2已满 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
