package com.campushub.dto;

import lombok.Data;

@Data
public class AdminActivityQueryDTO {

    private String title;
    private Integer status;
    private Integer auditStatus;
}
