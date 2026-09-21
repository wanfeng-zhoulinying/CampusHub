package com.campushub.constant;

/** Redis 缓存行为常量 */
public final class RedisCacheConstant {

    /** 空值缓存标记值 */
    public static final String NULL_CACHE_VALUE = "__NULL__";

    /** SCAN 前缀扫描每次建议扫描数量 */
    public static final long SCAN_COUNT = 500L;

    /** 缓存互斥重建等待窗口系数：×重试间隔=Redisson tryLock总等待时长（毫秒） */
    public static final int CACHE_MUTEX_RETRY_TIMES = 20;

    private RedisCacheConstant() {
    }
}
