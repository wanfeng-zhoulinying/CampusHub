package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreditRecord {

    /** 主键ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 变动类型，CreditChangeTypeConstant */
    private Integer changeType;
    /** 变动分值 */
    private Integer changeScore;
    /** 变动后信用分 */
    private Integer currentScore;
    /** 变动原因 */
    private String reason;
    /** 业务类型，CreditBusinessTypeConstant */
    private Integer businessType;
    /** 业务ID */
    private Long businessId;
    /** 操作人ID */
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
