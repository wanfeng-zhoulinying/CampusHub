package com.campushub.task;

import com.campushub.constant.RedisKeyConstant;
import com.campushub.entity.Booking;
import com.campushub.mapper.ActivityMapper;
import com.campushub.mapper.BookingMapper;
import com.campushub.service.cache.RedisCacheService;
import com.campushub.service.credit.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleTask {

    private final ActivityMapper activityMapper;
    private final BookingMapper bookingMapper;
    private final CreditService creditService;
    private final RedisCacheService redisCacheService;

    /**
     * 每分钟刷新活动状态，并清理活动详情缓存。
     */
    @Scheduled(fixedDelay = 60_000)
    public void refreshActivityStatus() {
        int signupStarted = activityMapper.startSignupActivities();
        int signupFinished = activityMapper.finishSignupActivities();
        int inProgressStarted = activityMapper.startInProgressActivities();
        int finished = activityMapper.finishActivities();

        if (signupStarted + signupFinished + inProgressStarted + finished > 0) {
            redisCacheService.deleteByPrefix(RedisKeyConstant.ACTIVITY_DETAIL);
            redisCacheService.deleteByPrefix(RedisKeyConstant.ACTIVITY_SIGNUP_STOCK);
            redisCacheService.deleteByPrefix(RedisKeyConstant.ACTIVITY_WAIT_STOCK);
            log.info("[ScheduleTask] 活动状态刷新完成 signupStarted={}, signupFinished={}, inProgressStarted={}, finished={}",
                    signupStarted, signupFinished, inProgressStarted, finished);
        }
    }

    /**
     * 兜底扫描超时未核销预约（降级为5分钟一次）。
     * 主路径已切换为延迟消息（BookingDelayCheckProducer，到点精确判定）；
     * 本任务仅在延迟消息丢失/插件异常时补偿，最坏滞后5分钟。
     */
    @Scheduled(fixedDelay = 300_000)
    public void markExpiredUncheckedBookings() {
        List<Booking> expiredBookings = bookingMapper.listExpiredUncheckedBookings();
        if (expiredBookings.isEmpty()) {
            return;
        }

        int successCount = 0;
        for (Booking booking : expiredBookings) {
            try {
                creditService.markBookingBreachBySystem(booking.getId());
                successCount++;
            } catch (Exception e) {
                log.warn("[ScheduleTask] 自动判定预约违约失败 bookingId={}", booking.getId(), e);
            }
        }
        log.info("[ScheduleTask] 超时未核销预约扫描完成 total={}, success={}",
                expiredBookings.size(), successCount);
    }
}
