package com.campushub.dto;

import lombok.Data;

@Data
public class BookingCreateDTO {

    private Long venueId;
    private Long slotId;
    private Integer personCount;
    private String remark;
}
