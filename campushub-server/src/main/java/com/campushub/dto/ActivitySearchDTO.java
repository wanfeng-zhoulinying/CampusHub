package com.campushub.dto;

import lombok.Data;

/**
 * 活动搜索入参（GET查询参数绑定，与ActivityQueryDTO同为参数对象风格）。
 */
@Data
public class ActivitySearchDTO {

    /** 搜索关键词 */
    private String keyword;
    /** 页码，从1开始，默认1 */
    private Integer pageNum;
    /** 每页条数，默认10，上限50 */
    private Integer pageSize;
}
