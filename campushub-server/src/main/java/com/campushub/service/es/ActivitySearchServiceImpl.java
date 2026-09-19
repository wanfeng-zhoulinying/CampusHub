package com.campushub.service.es;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.document.ActivityDocument;
import com.campushub.dto.ActivitySearchDTO;
import com.campushub.mapper.ActivityMapper;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySearchVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ES搜索服务实现：解决MySQL LIKE的三大缺陷——无分词、无相关性排序、无内容匹配。
 *
 * 查询设计（对应"搜篮球命中羽毛球"问题的三重对策）：
 * 1. 查询侧analyzer用ik_smart粗粒度切词："篮球"保持整词，
 *    不再像索引侧ik_max_word那样拆成"篮/球"去误命中"羽毛球"（索引细保召回、查询粗保精度）；
 * 2. minimumShouldMatch("75%")：至少匹配75%的查询词，单字/低相关噪音直接出局；
 * 3. 标题权重x3：标题命中的活动比仅在正文提到的更相关（业务先验）。
 *
 * 过滤条件走filter子句不参与BM25评分，且可被ES查询缓存复用。
 * 降级：任何ES侧异常（连接/超时/解析）都回退MySQL LIKE保可用性，返回VO同构前端无感知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySearchServiceImpl implements ActivitySearchService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final RestHighLevelClient client;
    private final ActivityMapper activityMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ActivitySearchVO search(ActivitySearchDTO searchDTO) {
        String keyword = searchDTO.getKeyword() == null ? "" : searchDTO.getKeyword().trim();
        if (keyword.isEmpty()) {
            return ActivitySearchVO.empty();
        }
        int pageNum = normalizePageNum(searchDTO.getPageNum());
        int pageSize = normalizePageSize(searchDTO.getPageSize());
        try {
            return searchFromEs(keyword, pageNum, pageSize);
        } catch (Exception e) {
            // 降级口径要宽：搜索接口的可用性优先于ES侧的任何故障
            log.error("[ActivitySearch] ES搜索异常，降级MySQL LIKE keyword={}", keyword, e);
            return searchFromMysql(keyword, pageNum, pageSize);
        }
    }

    /**
     * 主路径：ES multi_match相关性搜索。
     */
    private ActivitySearchVO searchFromEs(String keyword, int pageNum, int pageSize) throws IOException {
        MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(keyword)
                .field("title", 3.0f)
                .field("content")
                .field("location")
                // 覆盖查询分析器为ik_smart：查询侧粗粒度切词，保精度
                .analyzer("ik_smart")
                // 至少匹配75%的查询词，过滤低相关命中
                .minimumShouldMatch("75%");

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                // 审核状态是精确过滤不是相关性维度，走filter：不参与评分且可被查询缓存
                .filter(QueryBuilders.termQuery("auditStatus", ActivityAuditStatusConstant.APPROVED))
                .must(multiMatch);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(boolQuery)
                .from((pageNum - 1) * pageSize)
                .size(pageSize)
                // 精确总数（默认1万封顶够用，但语义上明确要精确值）
                .trackTotalHits(true)
                .highlighter(new HighlightBuilder()
                        .field("title")
                        // 内容高亮片段天然充当结果摘要
                        .field("content")
                        .preTags("<em>")
                        .postTags("</em>"));

        SearchRequest searchRequest = new SearchRequest(ActivityIndexServiceImpl.INDEX_NAME)
                .source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        ActivitySearchVO vo = new ActivitySearchVO();
        vo.setTotal(response.getHits().getTotalHits().value);
        vo.setRecords(parseHits(response));
        log.info("[ActivitySearch] ES搜索完成 keyword={}, total={}, 本页{}条, took={}ms",
                keyword, vo.getTotal(), vo.getRecords().size(), response.getTook().getMillis());
        return vo;
    }

    /**
     * 降级路径：MySQL LIKE。
     * 代价是无分词（整串模糊匹配）、无相关性排序（按id倒序）、不搜内容（LIKE只覆盖title/location）。
     */
    private ActivitySearchVO searchFromMysql(String keyword, int pageNum, int pageSize) {
        List<ActivityListVO> list = activityMapper.listActivities(keyword, null, ActivityAuditStatusConstant.APPROVED);

        ActivitySearchVO vo = new ActivitySearchVO();
        vo.setTotal((long) list.size());
        int from = (pageNum - 1) * pageSize;
        if (from >= list.size()) {
            vo.setRecords(List.of());
            return vo;
        }
        int to = Math.min(from + pageSize, list.size());
        List<ActivitySearchVO.Item> records = new ArrayList<>();
        for (ActivityListVO listVO : list.subList(from, to)) {
            records.add(toItem(listVO));
        }
        vo.setRecords(records);
        log.warn("[ActivitySearch] MySQL降级搜索完成 keyword={}, total={}", keyword, vo.getTotal());
        return vo;
    }

    /**
     * 私：ES响应转VO，高亮片段优先于原文。
     */
    private List<ActivitySearchVO.Item> parseHits(SearchResponse response) throws IOException {
        List<ActivitySearchVO.Item> records = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            ActivityDocument doc = objectMapper.readValue(hit.getSourceAsString(), ActivityDocument.class);
            ActivitySearchVO.Item item = new ActivitySearchVO.Item();
            item.setId(doc.getId());
            item.setTitle(resolveHighlight(hit, "title", doc.getTitle()));
            item.setContent(resolveHighlight(hit, "content", doc.getContent()));
            item.setLocation(doc.getLocation());
            item.setCoverUrl(doc.getCoverUrl());
            item.setSignupLimit(doc.getSignupLimit());
            item.setCurrentSignupCount(doc.getCurrentSignupCount());
            item.setStatus(doc.getStatus());
            item.setActivityStartTime(doc.getActivityStartTime());
            item.setActivityEndTime(doc.getActivityEndTime());
            item.setScore(hit.getScore());
            records.add(item);
        }
        return records;
    }

    /**
     * 私：字段有高亮片段则拼接片段，否则回退原文。
     */
    private String resolveHighlight(SearchHit hit, String field, String fallback) {
        HighlightField highlightField = hit.getHighlightFields().get(field);
        if (highlightField == null || highlightField.getFragments().length == 0) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (Text fragment : highlightField.getFragments()) {
            sb.append(fragment.string());
        }
        return sb.toString();
    }

    /**
     * 私：MySQL降级结果转同构VO。
     */
    private ActivitySearchVO.Item toItem(ActivityListVO listVO) {
        ActivitySearchVO.Item item = new ActivitySearchVO.Item();
        item.setId(listVO.getId());
        item.setTitle(listVO.getTitle());
        item.setLocation(listVO.getLocation());
        item.setCoverUrl(listVO.getCoverUrl());
        item.setSignupLimit(listVO.getSignupLimit());
        item.setCurrentSignupCount(listVO.getCurrentSignupCount());
        item.setStatus(listVO.getStatus());
        item.setActivityStartTime(listVO.getActivityStartTime());
        item.setActivityEndTime(listVO.getActivityEndTime());
        item.setScore(0f);
        return item;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
