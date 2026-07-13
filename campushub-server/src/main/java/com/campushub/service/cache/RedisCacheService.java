package com.campushub.service.cache;

import com.campushub.constant.RedisTtlConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private static final String NULL_CACHE_VALUE = "__NULL__";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache Aside 查询。
     * 先查 Redis，命中空值缓存则直接返回 null，未命中再回源数据库并回填缓存。
     */
    public <T> T queryWithPassThrough(String key, Class<T> clazz, long baseMinutes, Supplier<T> dbLoader) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            log.info("[RedisCache] 缓存未命中 key={}", key);
        } else if (NULL_CACHE_VALUE.equals(value)) {
            log.info("[RedisCache] 命中空值缓存 key={}", key);
            return null;
        } else {
            try {
                log.info("[RedisCache] 命中缓存 key={}", key);
                return objectMapper.readValue(value, clazz);
            } catch (JsonProcessingException e) {
                log.warn("[RedisCache] 缓存反序列化失败，准备删除坏缓存 key={}", key, e);
                delete(key);
            }
        }

        T dbValue = dbLoader.get();
        if (dbValue == null) {
            setNull(key);
            return null;
        }

        set(key, dbValue, baseMinutes);
        return dbValue;
    }

    /**
     * 写入对象缓存。
     * 在基础 TTL 上增加随机偏移，避免同类热点缓存同一时间集中失效。
     */
    public void set(String key, Object value, long baseMinutes) {
        try {
            long ttlMinutes = baseMinutes + randomMinutes();
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMinutes(ttlMinutes));
            log.info("[RedisCache] 写入缓存 key={}, ttlMinutes={}", key, ttlMinutes);
        } catch (JsonProcessingException e) {
            log.warn("[RedisCache] 缓存序列化失败 key={}", key, e);
        }
    }

    /**
     * 写入空值缓存。
     * 用较短 TTL 拦截不存在 ID 的重复访问，降低缓存穿透对数据库的压力。
     */
    public void setNull(String key) {
        stringRedisTemplate.opsForValue().set(
                key,
                NULL_CACHE_VALUE,
                Duration.ofMinutes(RedisTtlConstant.NULL_CACHE_MINUTES)
        );
        log.info("[RedisCache] 写入空值缓存 key={}, ttlMinutes={}", key, RedisTtlConstant.NULL_CACHE_MINUTES);
    }

    /**
     * 删除缓存。
     * 管理端更新和业务状态变化后主动删除，下一次查询再回源并重建缓存。
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
        log.info("[RedisCache] 删除缓存 key={}", key);
    }

    /**
     * 按前缀批量删除缓存。
     * 适用于定时任务这类批量状态流转场景，避免旧详情缓存继续返回过期状态。
     */
    public void deleteByPrefix(String keyPrefix) {
        Set<String> keys = stringRedisTemplate.keys(keyPrefix + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
        log.info("[RedisCache] 按前缀批量删除缓存 keyPrefix={}, count={}", keyPrefix, keys.size());
    }

    private long randomMinutes() {
        return ThreadLocalRandom.current().nextLong(RedisTtlConstant.CACHE_RANDOM_MINUTES + 1);
    }
}
