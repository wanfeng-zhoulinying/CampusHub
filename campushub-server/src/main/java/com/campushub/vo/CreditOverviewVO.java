package com.campushub.vo;

import lombok.Data;

@Data
public class CreditOverviewVO {

    private Long userId;
    private Integer creditScore;
    private Integer totalDeductScore;
    private Integer breachCount;
}
