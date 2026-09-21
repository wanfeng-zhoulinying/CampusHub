package com.campushub.service.cache;

import com.campushub.constant.RedisCacheConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    /**
     * Cache Aside 查询。
     * 先查 Redis，命中空值缓存则直接返回 null，未命中再回源数据库并回填缓存。
     */
    public <T> T queryWithPassThrough(String key, Class<T> clazz, long baseMinutes, Supplier<T> dbLoader) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            log.info("[RedisCache] 缓存未命中 key={}", key);
        } else if (RedisCacheConstant.NULL_CACHE_VALUE.equals(value)) {
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
     * Cache Aside 查询（互斥锁防击穿），锁基于 Redisson：
     * 1. 等待期通过 Pub/Sub 订阅锁释放信号、唤醒后立即竞争，替代旧版自旋sleep轮询；
     * 2. 不指定 leaseTime，看门狗默认30s自动续期——规避"重建耗时超过锁TTL、锁提前
     *    过期后并发重建"的隐患（旧版10s固定TTL两难：设短了业务没跑完锁先没，
     *    设长了宕机后其他线程干等）；
     * 3. 拿到锁后double check：等待期间他人可能已完成重建，直接返回避免重复回源。
     */
    public <T> T queryWithMutex(String key, Class<T> clazz, long baseMinutes, Supplier<T> dbLoader) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null) {
            if (RedisCacheConstant.NULL_CACHE_VALUE.equals(value)) {
                return null;
            }
            try {
                return objectMapper.readValue(value, clazz);
            } catch (JsonProcessingException e) {
                log.warn("[RedisCache] invalid cache value, deleting key={}", key, e);
                delete(key);
            }
        }

        RLock lock = redissonClient.getLock(RedisKeyConstant.CACHE_MUTEX_LOCK + key);
        boolean locked = false;
        try {
            // 等待窗口与旧版自旋重试等宽（次数×间隔），超时视为重建卡死兜底回源
            long waitMillis = (long) RedisCacheConstant.CACHE_MUTEX_RETRY_TIMES
                    * RedisTtlConstant.CACHE_MUTEX_RETRY_SLEEP_MILLIS;
            try {
                locked = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[RedisCache] mutex wait interrupted, fallback to database key={}", key);
                return loadAndCache(key, baseMinutes, dbLoader);
            }
            if (!locked) {
                log.warn("[RedisCache] mutex rebuild timeout, fallback to database key={}", key);
                return loadAndCache(key, baseMinutes, dbLoader);
            }

            String latest = stringRedisTemplate.opsForValue().get(key);
            if (latest != null) {
                if (RedisCacheConstant.NULL_CACHE_VALUE.equals(latest)) {
                    return null;
                }
                try {
                    return objectMapper.readValue(latest, clazz);
                } catch (JsonProcessingException e) {
                    log.warn("[RedisCache] invalid cache value, deleting key={}", key, e);
                    delete(key);
                }
            }
            return loadAndCache(key, baseMinutes, dbLoader);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 私：回源数据库并回填缓存（含空值缓存）。
     */
    private <T> T loadAndCache(String key, long baseMinutes, Supplier<T> dbLoader) {
        T dbValue = dbLoader.get();
        if (dbValue == null) {
            setNull(key);
            return null;
        }
        set(key, dbValue, baseMinutes);
        return dbValue;
    }

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
                RedisCacheConstant.NULL_CACHE_VALUE,
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
        Set<String> keys = scanKeysByPrefix(keyPrefix);
        if (keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
        log.info("[RedisCache] 按前缀批量删除缓存 keyPrefix={}, count={}", keyPrefix, keys.size());
    }

    private Set<String> scanKeysByPrefix(String keyPrefix) {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(keyPrefix + "*")
                .count(RedisCacheConstant.SCAN_COUNT)
                .build();
        Set<String> keys = new java.util.LinkedHashSet<>();

        try (Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    private long randomMinutes() {
        return ThreadLocalRandom.current().nextLong(RedisTtlConstant.CACHE_RANDOM_MINUTES + 1);
    }
}
