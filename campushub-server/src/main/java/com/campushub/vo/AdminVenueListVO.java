package com.campushub.vo;

import lombok.Data;

@Data
public class AdminVenueListVO {

    private Long id;
    private String name;
    private String category;
    private String location;
    private Integer capacity;
    private String coverUrl;
    private Integer status;
}
