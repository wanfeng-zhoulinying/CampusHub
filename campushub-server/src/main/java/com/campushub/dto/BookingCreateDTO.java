package com.campushub.dto;

import lombok.Data;

@Data
public class BookingCreateDTO {

    private Long userId;
    private Long venueId;
    private Long slotId;
    private Integer personCount;
    private String remark;
}
