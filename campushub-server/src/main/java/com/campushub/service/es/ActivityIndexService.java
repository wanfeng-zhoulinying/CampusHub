package com.campushub.service.es;

import com.campushub.entity.Activity;

/**
 * 活动搜索索引服务：负责ES索引campushub.activity的生命周期与数据同步。
 * 增量同步（Phase ③）由MQ事件消费者调用syncActivity实现。
 */
public interface ActivityIndexService {

    /**
     * 确保索引存在：不存在则按mapping创建。
     *
     * @return true表示本次新建了索引（调用方应随后执行全量初始化）
     */
    boolean ensureIndexExists();

    /**
     * 全量同步：MySQL所有未删除活动批量灌入ES（索引重建/首次初始化用）。
     */
    void bulkSyncAll();

    /**
     * 按ID增量同步：回查数据库拿最新值写入ES（文档_id=活动ID，覆盖写天然幂等）；
     * 活动不存在或已逻辑删除则从ES移除文档。
 * 由MQ同步消费者调用——不信任事件快照，以数据库当前值为准（防乱序旧盖新）。
     */
    void syncActivityById(Long activityId);
}
