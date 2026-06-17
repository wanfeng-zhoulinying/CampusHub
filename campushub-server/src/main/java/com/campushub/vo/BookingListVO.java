package com.campushub.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class BookingListVO {

    private Long id;
    private String bookingNo;
    private Long venueId;
    private String venueName;
    private String venueLocation;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer personCount;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
