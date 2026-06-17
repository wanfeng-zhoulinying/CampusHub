package com.campushub.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Venue {

    /** 主键ID */
    private Long id;
    /** 场地名称 */
    private String name;
    /** 场地分类 */
    private String category;
    /** 场地位置 */
    private String location;
    /** 场地总容量 */
    private Integer capacity;
    /** 场地封面图 */
    private String coverUrl;
    /** 场地描述 */
    private String description;
    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;
    /** 场地状态，VenueStatusConstant */
    private Integer status;
    /** 逻辑删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
