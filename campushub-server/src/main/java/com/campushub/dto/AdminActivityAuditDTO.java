package com.campushub.dto;

import lombok.Data;

@Data
public class AdminActivityAuditDTO {

    private Long auditUserId;
    private Integer auditStatus;
    private String auditRemark;
}
