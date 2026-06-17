package com.campushub.service.booking;

import com.campushub.dto.BookingCancelDTO;
import com.campushub.dto.BookingCreateDTO;
import com.campushub.dto.BookingQueryDTO;
import com.campushub.entity.Booking;
import com.campushub.entity.VenueSlot;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.BookingMapper;
import com.campushub.vo.BookingListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final DateTimeFormatter BOOKING_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BookingMapper bookingMapper;

    /**
     * 创建预约记录。
     * 当前阶段先按场地时段做容量校验和库存扣减，不接登录态，用户身份由 userId 临时传入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBooking(BookingCreateDTO createDTO) {
        if (createDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }
        if (createDTO.getVenueId() == null || createDTO.getSlotId() == null) {
            throw new BusinessException("场地或时间段不能为空");
        }
        if (createDTO.getPersonCount() == null || createDTO.getPersonCount() <= 0) {
            throw new BusinessException("预约人数必须大于0");
        }

        VenueSlot slot = bookingMapper.getVenueSlotById(createDTO.getSlotId());
        if (slot == null) {
            throw new BusinessException("预约时间段不存在");
        }
        if (!slot.getVenueId().equals(createDTO.getVenueId())) {
            throw new BusinessException("场地和时间段不匹配");
        }
        if (slot.getStatus() == null || slot.getStatus() != 1) {
            throw new BusinessException("当前时间段不可预约");
        }
        if (slot.getAvailableCapacity() < createDTO.getPersonCount()) {
            throw new BusinessException("当前时间段剩余容量不足");
        }

        int affectedRows = bookingMapper.decreaseSlotCapacity(createDTO.getSlotId(), createDTO.getPersonCount());
        if (affectedRows == 0) {
            throw new BusinessException("预约失败，请刷新后重试");
        }

        Booking booking = new Booking();
        booking.setBookingNo(generateBookingNo());
        booking.setUserId(createDTO.getUserId());
        booking.setVenueId(createDTO.getVenueId());
        booking.setSlotId(createDTO.getSlotId());
        booking.setBookingDate(slot.getSlotDate());
        booking.setStartTime(slot.getStartTime());
        booking.setEndTime(slot.getEndTime());
        booking.setPersonCount(createDTO.getPersonCount());
        booking.setStatus(1);
        booking.setBreachFlag(0);
        booking.setRemark(createDTO.getRemark());

        bookingMapper.saveBooking(booking);
        return booking.getId();
    }

    /**
     * 查询当前用户的预约列表。
     * 这里的“我的预约”先通过 userId 做筛选，后续接 JWT 后再替换成当前登录用户上下文。
     */
    @Override
    public List<BookingListVO> listMyBookings(BookingQueryDTO queryDTO) {
        if (queryDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }
        return bookingMapper.listUserBookings(queryDTO.getUserId(), queryDTO.getStatus());
    }

    /**
     * 取消预约。
     * 取消成功后会把时间段容量回滚，保证后续用户还能继续预约这个时段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long bookingId, BookingCancelDTO cancelDTO) {
        if (cancelDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }

        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!booking.getUserId().equals(cancelDTO.getUserId())) {
            throw new BusinessException("无权取消他人的预约");
        }
        if (booking.getStatus() == null || booking.getStatus() != 1) {
            throw new BusinessException("当前预约状态不允许取消");
        }

        int affectedRows = bookingMapper.cancelBooking(bookingId, cancelDTO.getCancelReason());
        if (affectedRows == 0) {
            throw new BusinessException("取消预约失败");
        }

        bookingMapper.restoreSlotCapacity(booking.getSlotId(), booking.getPersonCount());
    }

    /**
     * 生成预约单号。
     * 单号由固定前缀、当前时间和四位随机数组成，用于区分业务预约记录。
     */
    private String generateBookingNo() {
        return "BK" + LocalDateTime.now().format(BOOKING_NO_TIME_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
