package com.campushub.service.es;

import com.campushub.dto.ActivitySearchDTO;
import com.campushub.vo.ActivitySearchVO;

/**
 * 活动搜索服务（CQRS读侧）：multi_match三字段相关性搜索 + BM25排序 + 高亮。
 * ES异常时降级MySQL LIKE，接口契约不变。
 */
public interface ActivitySearchService {

    /**
     * 按关键词搜索审核通过的活动，按BM25相关性排序。
     *
     * @param searchDTO 关键词 + 分页参数
     * @return 命中总数与当前页记录（含高亮标题、相关性得分）
     */
    ActivitySearchVO search(ActivitySearchDTO searchDTO);
}
