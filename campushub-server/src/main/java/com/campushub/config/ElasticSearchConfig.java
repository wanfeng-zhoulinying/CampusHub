package com.campushub.config;

import com.campushub.service.es.ActivityIndexService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ES配置：RestHighLevelClient（7.12.1，与服务器同版本）。
 */
@Slf4j
@Configuration
public class ElasticSearchConfig {

    /**
     * 高级客户端：内部维护连接池，容器关闭时同步关闭（destroyMethod）。
     */
    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient(
            @Value("${campushub.elasticsearch.host}") String host,
            @Value("${campushub.elasticsearch.port}") int port) {
        log.info("[ES] RestHighLevelClient 初始化 {}:{}", host, port);
        return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }

    /**
     * 启动索引初始化（对称RabbitMqConfig的拓扑fail-fast声明）：
     * 索引不存在则创建mapping并触发全量灌数据；已存在则幂等跳过。
     * ES连不上时启动直接失败，而非等到第一次搜索才暴露。
     */
    @Bean
    public ApplicationRunner esIndexInitializer(ActivityIndexService activityIndexService) {
        return args -> {
            boolean created = activityIndexService.ensureIndexExists();
            if (created) {
                activityIndexService.bulkSyncAll();
            }
        };
    }
}
