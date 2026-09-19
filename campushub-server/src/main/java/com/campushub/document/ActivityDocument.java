package com.campushub.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动在ES中的文档模型（查询视图）：只保留搜索和列表展示需要的字段，
 * 不镜像MySQL全字段——ES是查询侧投影，不是第二份真相源。
 * 时间字段经容器ObjectMapper（JavaTimeModule）序列化为ISO字符串，对应索引date类型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDocument {

    /** 活动ID（文档_id，与MySQL主键一致：同步幂等靠它覆盖写） */
    private Long id;
    /** 活动标题（ik_max_word细粒度索引，另带keyword子字段供精确匹配） */
    private String title;
    /** 活动内容 */
    private String content;
    /** 活动地点 */
    private String location;
    /** 封面图（仅展示，不索引） */
    private String coverUrl;
    /** 关联场地ID */
    private Long venueId;
    /** 报名人数上限 */
    private Integer signupLimit;
    /** 当前报名人数 */
    private Integer currentSignupCount;
    /** 活动状态，ActivityStatusConstant */
    private Integer status;
    /** 审核状态，ActivityAuditStatusConstant（搜索默认只放行审核通过的） */
    private Integer auditStatus;
    /** 活动开始时间 */
    private LocalDateTime activityStartTime;
    /** 活动结束时间 */
    private LocalDateTime activityEndTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}
