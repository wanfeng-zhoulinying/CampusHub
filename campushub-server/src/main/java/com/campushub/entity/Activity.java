package com.campushub.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Activity {

    /** 主键ID */
    private Long id;
    /** 发布者ID */
    private Long publisherId;
    /** 活动标题 */
    private String title;
    /** 封面图 */
    private String coverUrl;
    /** 活动内容 */
    private String content;
    /** 活动地点 */
    private String location;
    /** 关联场地ID */
    private Long venueId;
    /** 报名开始时间 */
    private LocalDateTime signupStartTime;
    /** 报名结束时间 */
    private LocalDateTime signupEndTime;
    /** 活动开始时间 */
    private LocalDateTime activityStartTime;
    /** 活动结束时间 */
    private LocalDateTime activityEndTime;
    /** 报名人数上限 */
    private Integer signupLimit;
    /** 当前报名人数 */
    private Integer currentSignupCount;
    /** 候补上限 */
    private Integer waitLimit;
    /** 活动状态，ActivityStatusConstant */
    private Integer status;
    /** 审核状态，ActivityAuditStatusConstant */
    private Integer auditStatus;
    /** 审核备注 */
    private String auditRemark;
    /** 审核人ID */
    private Long auditUserId;
    /** 审核时间 */
    private LocalDateTime auditTime;
    /** 逻辑删除标记，DeleteStatusConstant */
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
