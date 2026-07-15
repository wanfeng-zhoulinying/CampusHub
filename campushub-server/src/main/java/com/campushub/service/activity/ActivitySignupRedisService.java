package com.campushub.service.activity;

import com.campushub.constant.ActivitySignupStatusConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.campushub.entity.Activity;
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
public class ActivitySignupRedisService {

    private static final String LUA_RESULT_DUPLICATE = "DUPLICATE";
    private static final String LUA_RESULT_FULL = "FULL";
    private static final String LUA_RESULT_SIGNED = "SIGNED";
    private static final String LUA_RESULT_WAIT = "WAIT";

    private static final DefaultRedisScript<String> RESERVE_SIGNUP_SCRIPT = createReserveSignupScript();

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取短期幂等锁，先挡住重复点击和重复提交。
     */
    public void acquireRequestLock(Long activityId, Long userId) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                buildRequestKey(activityId, userId),
                "1",
                Duration.ofSeconds(RedisTtlConstant.ACTIVITY_SIGNUP_REQUEST_SECONDS)
        );
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("请勿重复提交活动报名请求");
        }
    }

    /**
     * 初始化活动报名并发计数，并原子预占正式名额或候补名额。
     * Redis 先挡住超卖流量，数据库继续做最终一致性兜底。
     */
    public SignupReserveResult reserveSignup(Activity activity, Long userId, int currentWaitCount) {
        initCounterIfAbsent(activity, currentWaitCount);

        String result = stringRedisTemplate.execute(
                RESERVE_SIGNUP_SCRIPT,
                List.of(
                        buildSignupStockKey(activity.getId()),
                        buildWaitStockKey(activity.getId())
                )
        );

        if (result == null) {
            throw new BusinessException("活动报名请求繁忙，请稍后重试");
        }
        if (LUA_RESULT_DUPLICATE.equals(result)) {
            throw new BusinessException("请勿重复提交活动报名请求");
        }
        if (LUA_RESULT_FULL.equals(result)) {
            clearRequestLock(activity.getId(), userId);
            return SignupReserveResult.full();
        }
        if (result.startsWith(LUA_RESULT_SIGNED + ":")) {
            log.info("[ActivitySignupRedis] 正式报名名额预占成功 activityId={}, userId={}", activity.getId(), userId);
            return SignupReserveResult.signed();
        }
        if (result.startsWith(LUA_RESULT_WAIT + ":")) {
            int remain = Integer.parseInt(result.substring((LUA_RESULT_WAIT + ":").length()));
            int waitOrder = buildWaitOrder(activity.getWaitLimit(), remain);
            log.info("[ActivitySignupRedis] 候补名额预占成功 activityId={}, userId={}, waitOrder={}",
                    activity.getId(), userId, waitOrder);
            return SignupReserveResult.waitlisted(waitOrder);
        }
        clearRequestLock(activity.getId(), userId);
        throw new BusinessException("活动报名请求繁忙，请稍后重试");
    }

    /**
     * 数据库事务失败后回滚 Redis 预占名额，并释放短期幂等锁。
     */
    public void releaseReservedQuota(Long activityId, Long userId, SignupReserveResult reserveResult) {
        clearRequestLock(activityId, userId);
        if (reserveResult == null) {
            return;
        }
        if (reserveResult.isSigned()) {
            incrementIfPresent(buildSignupStockKey(activityId));
            log.info("[ActivitySignupRedis] 回滚正式报名名额 activityId={}, userId={}", activityId, userId);
            return;
        }
        if (reserveResult.isWaitlisted()) {
            incrementIfPresent(buildWaitStockKey(activityId));
            log.info("[ActivitySignupRedis] 回滚候补名额 activityId={}, userId={}", activityId, userId);
        }
    }

    /**
     * 取消报名后同步 Redis 计数，保持后续报名令牌与数据库状态一致。
     */
    public void syncAfterCancel(Long activityId, Integer signupStatus, boolean promotedWaitlisted) {
        if (ActivitySignupStatusConstant.WAITLISTED.equals(signupStatus)) {
            incrementIfPresent(buildWaitStockKey(activityId));
            log.info("[ActivitySignupRedis] 候补取消后回补候补名额 activityId={}", activityId);
            return;
        }

        if (promotedWaitlisted) {
            incrementIfPresent(buildWaitStockKey(activityId));
            log.info("[ActivitySignupRedis] 正式报名取消并补位后回补候补名额 activityId={}", activityId);
            return;
        }

        incrementIfPresent(buildSignupStockKey(activityId));
        log.info("[ActivitySignupRedis] 正式报名取消后回补正式名额 activityId={}", activityId);
    }

    /**
     * 活动基础信息变更后删除报名并发计数，下次报名时重新按数据库状态初始化。
     */
    public void clearActivityCounters(Long activityId) {
        stringRedisTemplate.delete(List.of(
                buildSignupStockKey(activityId),
                buildWaitStockKey(activityId)
        ));
        log.info("[ActivitySignupRedis] 删除活动报名并发计数 activityId={}", activityId);
    }

    private void initCounterIfAbsent(Activity activity, int currentWaitCount) {
        long ttlSeconds = calculateCounterTtlSeconds(activity);
        int remainSignup = Math.max(activity.getSignupLimit() - activity.getCurrentSignupCount(), 0);
        int remainWait = Math.max(getWaitLimit(activity) - currentWaitCount, 0);

        stringRedisTemplate.opsForValue().setIfAbsent(
                buildSignupStockKey(activity.getId()),
                String.valueOf(remainSignup),
                Duration.ofSeconds(ttlSeconds)
        );
        stringRedisTemplate.opsForValue().setIfAbsent(
                buildWaitStockKey(activity.getId()),
                String.valueOf(remainWait),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    private void clearRequestLock(Long activityId, Long userId) {
        stringRedisTemplate.delete(buildRequestKey(activityId, userId));
    }

    private void incrementIfPresent(String key) {
        Boolean existed = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(existed)) {
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

    private long calculateCounterTtlSeconds(Activity activity) {
        long fallbackSeconds = Duration.ofMinutes(RedisTtlConstant.ACTIVITY_SIGNUP_COUNTER_FALLBACK_MINUTES).getSeconds();
        LocalDateTime expireTime = activity.getActivityEndTime();
        if (expireTime == null) {
            return fallbackSeconds;
        }
        long ttlSeconds = Duration.between(LocalDateTime.now(), expireTime.plusHours(1)).getSeconds();
        return Math.max(ttlSeconds, fallbackSeconds);
    }

    private int buildWaitOrder(Integer waitLimit, int remainWait) {
        return getWaitLimit(waitLimit) - remainWait;
    }

    private int getWaitLimit(Activity activity) {
        return getWaitLimit(activity.getWaitLimit());
    }

    private int getWaitLimit(Integer waitLimit) {
        return waitLimit == null ? 0 : waitLimit;
    }

    private String buildSignupStockKey(Long activityId) {
        return RedisKeyConstant.ACTIVITY_SIGNUP_STOCK + activityId;
    }

    private String buildWaitStockKey(Long activityId) {
        return RedisKeyConstant.ACTIVITY_WAIT_STOCK + activityId;
    }

    private String buildRequestKey(Long activityId, Long userId) {
        return RedisKeyConstant.ACTIVITY_SIGNUP_REQUEST + activityId + ":" + userId;
    }

    private static DefaultRedisScript<String> createReserveSignupScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(
                "local signupRemain = tonumber(redis.call('get', KEYS[1]) or '-1') " +
                        "if signupRemain > 0 then " +
                        "  local remain = redis.call('decr', KEYS[1]) " +
                        "  return 'SIGNED:' .. tostring(remain) " +
                        "end " +
                        "local waitRemain = tonumber(redis.call('get', KEYS[2]) or '-1') " +
                        "if waitRemain > 0 then " +
                        "  local remain = redis.call('decr', KEYS[2]) " +
                        "  return 'WAIT:' .. tostring(remain) " +
                        "end " +
                        "return 'FULL'"
        );
        return script;
    }

    @Getter
    public static final class SignupReserveResult {

        private final boolean signed;
        private final boolean waitlisted;
        private final boolean full;
        private final Integer waitOrder;

        private SignupReserveResult(boolean signed, boolean waitlisted, boolean full, Integer waitOrder) {
            this.signed = signed;
            this.waitlisted = waitlisted;
            this.full = full;
            this.waitOrder = waitOrder;
        }

        public static SignupReserveResult signed() {
            return new SignupReserveResult(true, false, false, null);
        }

        public static SignupReserveResult waitlisted(Integer waitOrder) {
            return new SignupReserveResult(false, true, false, waitOrder);
        }

        public static SignupReserveResult full() {
            return new SignupReserveResult(false, false, true, null);
        }
    }
}
