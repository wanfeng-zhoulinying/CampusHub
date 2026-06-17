package com.campushub.dto;

import lombok.Data;

@Data
public class BookingCancelDTO {

    private Long userId;
    private String cancelReason;
}
