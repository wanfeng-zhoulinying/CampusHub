package com.campushub.dto;

import lombok.Data;

@Data
public class ActivityQueryDTO {

    private String keyword;
    private Integer status;
    private Integer auditStatus;
}
