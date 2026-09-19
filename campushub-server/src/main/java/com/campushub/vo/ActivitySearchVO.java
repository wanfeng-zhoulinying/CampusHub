package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动搜索结果VO：命中总数 + 当前页记录。
 * ES路径带BM25得分与高亮片段（命中词包裹em标签）；
 * MySQL降级路径score恒为0、无高亮，但结构同构，前端无感知切换。
 */
@Data
public class ActivitySearchVO {

    /** 命中总数 */
    private Long total;

    /** 当前页记录 */
    private List<Item> records;

    @Data
    public static class Item {

        private Long id;
        /** 标题（ES路径为高亮结果，命中词包裹em标签） */
        private String title;
        /** 内容摘要（ES路径为命中片段，天然充当结果摘要；降级路径为null） */
        private String content;
        private String location;
        private String coverUrl;
        private Integer signupLimit;
        private Integer currentSignupCount;
        /** 活动状态，ActivityStatusConstant */
        private Integer status;
        private LocalDateTime activityStartTime;
        private LocalDateTime activityEndTime;
        /** BM25相关性得分（降级路径恒为0） */
        private Float score;
    }

    public static ActivitySearchVO empty() {
        ActivitySearchVO vo = new ActivitySearchVO();
        vo.setTotal(0L);
        vo.setRecords(List.of());
        return vo;
    }
}
