package com.campushub.service.cache;

import com.campushub.constant.RedisCacheConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private static final DefaultRedisScript<Long> RELEASE_MUTEX_LOCK_SCRIPT =
            createReleaseMutexLockScript();

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
     * 写入对象缓存。
     * 在基础 TTL 上增加随机偏移，避免同类热点缓存同一时间集中失效。
     */
    public <T> T queryWithMutex(String key, Class<T> clazz, long baseMinutes, Supplier<T> dbLoader) {
        for (int retry = 0; retry <= RedisCacheConstant.CACHE_MUTEX_RETRY_TIMES; retry++) {
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

            String lockKey = RedisKeyConstant.CACHE_MUTEX_LOCK + key;
            String lockToken = UUID.randomUUID().toString();
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    lockToken,
                    Duration.ofSeconds(RedisTtlConstant.CACHE_MUTEX_LOCK_SECONDS)
            );
            if (Boolean.TRUE.equals(locked)) {
                try {
                    T dbValue = dbLoader.get();
                    if (dbValue == null) {
                        setNull(key);
                        return null;
                    }
                    set(key, dbValue, baseMinutes);
                    return dbValue;
                } finally {
                    releaseMutexLock(lockKey, lockToken);
                }
            }

            if (retry == RedisCacheConstant.CACHE_MUTEX_RETRY_TIMES) {
                log.warn("[RedisCache] mutex rebuild timeout, fallback to database key={}", key);
                T dbValue = dbLoader.get();
                if (dbValue == null) {
                    setNull(key);
                    return null;
                }
                set(key, dbValue, baseMinutes);
                return dbValue;
            }

            try {
                Thread.sleep(RedisTtlConstant.CACHE_MUTEX_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("cache mutex rebuild interrupted", e);
            }
        }

        throw new IllegalStateException("cache mutex rebuild failed");
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

    private void releaseMutexLock(String lockKey, String lockToken) {
        stringRedisTemplate.execute(
                RELEASE_MUTEX_LOCK_SCRIPT,
                List.of(lockKey),
                lockToken
        );
    }

    private static DefaultRedisScript<Long> createReleaseMutexLockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "end " +
                        "return 0"
        );
        return script;
    }

    private long randomMinutes() {
        return ThreadLocalRandom.current().nextLong(RedisTtlConstant.CACHE_RANDOM_MINUTES + 1);
    }
}
