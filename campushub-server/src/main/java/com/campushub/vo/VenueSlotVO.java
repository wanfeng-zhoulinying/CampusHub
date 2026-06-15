package com.campushub.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class VenueSlotVO {

    private Long id;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxCapacity;
    private Integer availableCapacity;
    private Integer status;
}
