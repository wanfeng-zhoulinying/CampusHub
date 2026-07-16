package com.campushub.service.booking;

import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.campushub.entity.VenueSlot;
import com.campushub.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingRedisService {

    private static final String LUA_RESULT_RESERVED = "RESERVED";
    private static final String LUA_RESULT_FULL = "FULL";

    private static final DefaultRedisScript<String> RESERVE_SLOT_CAPACITY_SCRIPT = createReserveSlotCapacityScript();

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取短期预约幂等锁，拦截同一用户对同一时间段的重复点击和重复提交。
     */
    public void acquireRequestLock(Long slotId, Long userId) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                buildRequestKey(slotId, userId),
                "1",
                Duration.ofSeconds(RedisTtlConstant.BOOKING_REQUEST_SECONDS)
        );
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("请勿重复提交场地预约请求");
        }
    }

    /**
     * 初始化时间段容量计数，并通过 Lua 原子预占本次预约人数。
     * Redis 先拦住超容量流量，数据库继续做最终一致性兜底。
     */
    public BookingReserveResult reserveSlotCapacity(VenueSlot slot, Long userId, Integer personCount) {
        initCounterIfAbsent(slot);

        String result = stringRedisTemplate.execute(
                RESERVE_SLOT_CAPACITY_SCRIPT,
                List.of(buildSlotStockKey(slot.getId())),
                String.valueOf(personCount)
        );

        if (result == null) {
            throw new BusinessException("场地预约请求繁忙，请稍后重试");
        }
        if (LUA_RESULT_FULL.equals(result)) {
            clearRequestLock(slot.getId(), userId);
            return BookingReserveResult.full();
        }
        if (result.startsWith(LUA_RESULT_RESERVED + ":")) {
            int remain = Integer.parseInt(result.substring((LUA_RESULT_RESERVED + ":").length()));
            log.info("[BookingRedis] 场地预约容量预占成功 slotId={}, userId={}, personCount={}, remain={}",
                    slot.getId(), userId, personCount, remain);
            return BookingReserveResult.reserved(personCount);
        }

        clearRequestLock(slot.getId(), userId);
        throw new BusinessException("场地预约请求繁忙，请稍后重试");
    }

    /**
     * 数据库事务失败后回滚 Redis 预占容量，并释放短期幂等锁。
     */
    public void releaseReservedCapacity(Long slotId, Long userId, BookingReserveResult reserveResult) {
        clearRequestLock(slotId, userId);
        if (reserveResult == null || !reserveResult.isReserved()) {
            return;
        }
        incrementByIfPresent(buildSlotStockKey(slotId), reserveResult.getPersonCount());
        log.info("[BookingRedis] 回滚场地预约容量 slotId={}, userId={}, personCount={}",
                slotId, userId, reserveResult.getPersonCount());
    }

    /**
     * 取消预约成功后回补 Redis 容量，让后续预约流量与数据库状态保持一致。
     */
    public void syncAfterCancel(Long slotId, Integer personCount) {
        incrementByIfPresent(buildSlotStockKey(slotId), personCount);
        log.info("[BookingRedis] 取消预约后回补场地预约容量 slotId={}, personCount={}", slotId, personCount);
    }

    /**
     * 清理指定时间段容量计数，下次预约时重新按数据库剩余容量初始化。
     */
    public void clearSlotCounter(Long slotId) {
        stringRedisTemplate.delete(buildSlotStockKey(slotId));
        log.info("[BookingRedis] 删除场地预约容量计数 slotId={}", slotId);
    }

    private void initCounterIfAbsent(VenueSlot slot) {
        long ttlSeconds = calculateCounterTtlSeconds(slot);
        int remainCapacity = Math.max(slot.getAvailableCapacity(), 0);
        stringRedisTemplate.opsForValue().setIfAbsent(
                buildSlotStockKey(slot.getId()),
                String.valueOf(remainCapacity),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    private void clearRequestLock(Long slotId, Long userId) {
        stringRedisTemplate.delete(buildRequestKey(slotId, userId));
    }

    private void incrementByIfPresent(String key, Integer personCount) {
        Boolean existed = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(existed)) {
            stringRedisTemplate.opsForValue().increment(key, personCount);
        }
    }

    private long calculateCounterTtlSeconds(VenueSlot slot) {
        long fallbackSeconds = Duration.ofMinutes(RedisTtlConstant.BOOKING_SLOT_STOCK_FALLBACK_MINUTES).getSeconds();
        if (slot.getSlotDate() == null || slot.getEndTime() == null) {
            return fallbackSeconds;
        }
        LocalDateTime expireTime = LocalDateTime.of(slot.getSlotDate(), slot.getEndTime()).plusHours(1);
        long ttlSeconds = Duration.between(LocalDateTime.now(), expireTime).getSeconds();
        return Math.max(ttlSeconds, fallbackSeconds);
    }

    private String buildSlotStockKey(Long slotId) {
        return RedisKeyConstant.BOOKING_SLOT_STOCK + slotId;
    }

    private String buildRequestKey(Long slotId, Long userId) {
        return RedisKeyConstant.BOOKING_REQUEST + slotId + ":" + userId;
    }

    private static DefaultRedisScript<String> createReserveSlotCapacityScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(
                "local remainCapacity = tonumber(redis.call('get', KEYS[1]) or '-1') " +
                        "local personCount = tonumber(ARGV[1]) " +
                        "if remainCapacity >= personCount then " +
                        "  local remain = redis.call('decrby', KEYS[1], personCount) " +
                        "  return 'RESERVED:' .. tostring(remain) " +
                        "end " +
                        "return 'FULL'"
        );
        return script;
    }

    @Getter
    public static final class BookingReserveResult {

        private final boolean reserved;
        private final boolean full;
        private final Integer personCount;

        private BookingReserveResult(boolean reserved, boolean full, Integer personCount) {
            this.reserved = reserved;
            this.full = full;
            this.personCount = personCount;
        }

        public static BookingReserveResult reserved(Integer personCount) {
            return new BookingReserveResult(true, false, personCount);
        }

        public static BookingReserveResult full() {
            return new BookingReserveResult(false, true, null);
        }
    }
}
