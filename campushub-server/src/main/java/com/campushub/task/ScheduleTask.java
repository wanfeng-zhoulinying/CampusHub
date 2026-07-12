package com.campushub.task;

import com.campushub.entity.Booking;
import com.campushub.mapper.ActivityMapper;
import com.campushub.mapper.BookingMapper;
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

    @Scheduled(fixedDelay = 60_000)
    public void refreshActivityStatus() {
        int signupStarted = activityMapper.startSignupActivities();
        int signupFinished = activityMapper.finishSignupActivities();
        int inProgressStarted = activityMapper.startInProgressActivities();
        int finished = activityMapper.finishActivities();

        if (signupStarted + signupFinished + inProgressStarted + finished > 0) {
            log.info("[ScheduleTask] refresh activity status signupStarted={}, signupFinished={}, inProgressStarted={}, finished={}",
                    signupStarted, signupFinished, inProgressStarted, finished);
        }
    }

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
                log.warn("[ScheduleTask] mark expired booking breach failed bookingId={}", booking.getId(), e);
            }
        }
        log.info("[ScheduleTask] mark expired unchecked bookings total={}, success={}",
                expiredBookings.size(), successCount);
    }
}
