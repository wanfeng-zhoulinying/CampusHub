package com.campushub.service.es;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.document.ActivityDocument;
import com.campushub.entity.Activity;
import com.campushub.mapper.ActivityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * ES索引服务实现：RestHighLevelClient（与服务器7.12.1严格同版本）。
 *
 * 设计要点：
 * 1. 文档_id = MySQL活动ID：重复同步是覆盖写而非追加，天然幂等——
 *    和违约链路"消费端幂等"同一思想，此处靠ES文档主键实现。
 * 2. 索引不存在才创建+全量，存在则跳过：启动幂等，不会重复灌数据。
 * 3. 失败抛异常阻止启动（fail-fast），与RabbitMQ拓扑声明的策略对齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityIndexServiceImpl implements ActivityIndexService {

    /** 索引名，对齐MQ拓扑的campushub.*命名 */
    public static final String INDEX_NAME = "campushub.activity";

    private final RestHighLevelClient client;
    private final ActivityMapper activityMapper;
    private final ObjectMapper objectMapper;

    /**
     * mapping：搜索三字段（title/content/location）ik_max_word细粒度索引；
     * title/location另带keyword子字段，聚合/精确匹配走子字段。
     * 时间双格式兼容ISO与空格分隔。coverUrl仅展示不索引。
     *
     * 坑位备忘：CreateIndexRequest.source() 要求顶层必须包一层 mappings——
     * 裸 {"properties": ...} 是 PutMapping 的请求体格式，服务端会报
     * "unknown key [properties] for create index"，而高级客户端会静默丢弃未知顶层键，
     * 建出无mapping索引，写入时触发动态映射走standard分词器中文逐字切分。
     * 验证mapping必须看 GET /{index}/_mapping，不能只看建索引返回ack。
     */
    private static final String ACTIVITY_MAPPING = """
            {
              "mappings": {
                "properties": {
                  "id":                     { "type": "keyword" },
                  "title":                  { "type": "text", "analyzer": "ik_max_word",
                                              "fields": { "keyword": { "type": "keyword" } } },
                  "content":                { "type": "text", "analyzer": "ik_max_word" },
                  "location":               { "type": "text", "analyzer": "ik_max_word",
                                              "fields": { "keyword": { "type": "keyword" } } },
                  "coverUrl":               { "type": "keyword", "index": false },
                  "venueId":                { "type": "long" },
                  "signupLimit":            { "type": "integer" },
                  "currentSignupCount":     { "type": "integer" },
                  "status":                 { "type": "integer" },
                  "auditStatus":            { "type": "integer" },
                  "activityStartTime":      { "type": "date",
                                              "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss||epoch_millis" },
                  "activityEndTime":        { "type": "date",
                                              "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss||epoch_millis" },
                  "createTime":             { "type": "date",
                                              "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss||epoch_millis" }
                }
              }
            }
            """;

    @Override
    public boolean ensureIndexExists() {
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
            if (client.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
                log.info("[ActivityIndex] 索引已存在 {}", INDEX_NAME);
                return false;
            }
            CreateIndexRequest createRequest = new CreateIndexRequest(INDEX_NAME);
            createRequest.source(ACTIVITY_MAPPING, XContentType.JSON);
            client.indices().create(createRequest, RequestOptions.DEFAULT);
            log.info("[ActivityIndex] 索引创建完成 {}（ik_max_word分词）", INDEX_NAME);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("ES索引初始化失败，请检查ES连接与IK分词器：" + INDEX_NAME, e);
        }
    }

    @Override
    public void bulkSyncAll() {
        List<Activity> activities = activityMapper.listAllActivitiesForSync();
        if (activities.isEmpty()) {
            log.info("[ActivityIndex] 无活动数据，跳过全量同步");
            return;
        }
        try {
            BulkRequest bulkRequest = new BulkRequest(INDEX_NAME);
            for (Activity activity : activities) {
                bulkRequest.add(new IndexRequest(INDEX_NAME)
                        .id(String.valueOf(activity.getId()))
                        .source(objectMapper.writeValueAsString(toDocument(activity)), XContentType.JSON));
            }
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new IllegalStateException("ES全量同步存在失败项：" + bulkResponse.buildFailureMessage());
            }
            log.info("[ActivityIndex] 全量同步完成，共{}条", activities.size());
        } catch (IOException e) {
            throw new IllegalStateException("ES全量同步失败", e);
        }
    }

    @Override
    public void syncActivityById(Long activityId) {
        Activity activity = activityMapper.getActivityById(activityId);
        try {
            // 回查为空（被物理删除或数据异常）或已逻辑删除：移除ES文档保持一致
            if (activity == null || DeleteStatusConstant.DELETED.equals(activity.getIsDeleted())) {
                client.delete(new DeleteRequest(INDEX_NAME, String.valueOf(activityId)), RequestOptions.DEFAULT);
                log.info("[ActivityIndex] 活动已删除出索引 activityId={}", activityId);
                return;
            }
            IndexRequest request = new IndexRequest(INDEX_NAME)
                    .id(String.valueOf(activity.getId()))
                    .source(objectMapper.writeValueAsString(toDocument(activity)), XContentType.JSON);
            client.index(request, RequestOptions.DEFAULT);
            log.info("[ActivityIndex] 活动已同步 activityId={}", activityId);
        } catch (IOException e) {
            throw new IllegalStateException("ES同步失败 activityId=" + activityId, e);
        }
    }

    /**
     * 私：Activity实体转ES文档投影。
     */
    private ActivityDocument toDocument(Activity activity) {
        return ActivityDocument.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .content(activity.getContent())
                .location(activity.getLocation())
                .coverUrl(activity.getCoverUrl())
                .venueId(activity.getVenueId())
                .signupLimit(activity.getSignupLimit())
                .currentSignupCount(activity.getCurrentSignupCount())
                .status(activity.getStatus())
                .auditStatus(activity.getAuditStatus())
                .activityStartTime(activity.getActivityStartTime())
                .activityEndTime(activity.getActivityEndTime())
                .createTime(activity.getCreateTime())
                .build();
    }
}
