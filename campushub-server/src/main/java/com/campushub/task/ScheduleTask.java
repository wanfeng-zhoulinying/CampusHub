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
            log.info("[ScheduleTask] 活动状态刷新完成 signupStarted={}, signupFinished={}, inProgressStarted={}, finished={}",
                    signupStarted, signupFinished, inProgressStarted, finished);
        }
    }

    /**
     * 每分钟扫描超时未核销预约，并自动判定违约。
     */
    @Scheduled(fixedDelay = 60_000)
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
