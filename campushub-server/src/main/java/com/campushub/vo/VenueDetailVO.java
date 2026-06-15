package com.campushub.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VenueDetailVO {

    private Long id;
    private String name;
    private String category;
    private String location;
    private Integer capacity;
    private String coverUrl;
    private String description;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
}
