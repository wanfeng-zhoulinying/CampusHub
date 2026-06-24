package com.campushub.service.booking;

import com.campushub.constant.BookingBreachFlagConstant;
import com.campushub.constant.BookingStatusConstant;
import com.campushub.constant.VenueSlotStatusConstant;
import com.campushub.dto.BookingCancelDTO;
import com.campushub.dto.BookingCreateDTO;
import com.campushub.dto.BookingQueryDTO;
import com.campushub.entity.Booking;
import com.campushub.entity.VenueSlot;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.BookingMapper;
import com.campushub.utils.UserContext;
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
     * 当前用户身份从登录态获取，业务层只处理场地、时间段与容量校验。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBooking(BookingCreateDTO createDTO) {
        Long currentUserId = getCurrentUserId();
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
        if (!VenueSlotStatusConstant.AVAILABLE.equals(slot.getStatus())) {
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
        booking.setUserId(currentUserId);
        booking.setVenueId(createDTO.getVenueId());
        booking.setSlotId(createDTO.getSlotId());
        booking.setBookingDate(slot.getSlotDate());
        booking.setStartTime(slot.getStartTime());
        booking.setEndTime(slot.getEndTime());
        booking.setPersonCount(createDTO.getPersonCount());
        booking.setStatus(BookingStatusConstant.BOOKED);
        booking.setBreachFlag(BookingBreachFlagConstant.NO_BREACH);
        booking.setRemark(createDTO.getRemark());

        bookingMapper.saveBooking(booking);
        return booking.getId();
    }

    /**
     * 查询当前登录用户的预约列表。
     * “我的预约”统一从登录态读取用户信息，不再接受前端传 userId。
     */
    @Override
    public List<BookingListVO> listMyBookings(BookingQueryDTO queryDTO) {
        return bookingMapper.listUserBookings(getCurrentUserId(), queryDTO.getStatus());
    }

    /**
     * 取消预约。
     * 取消成功后回滚时间段容量，保证后续用户仍可继续预约这个时间段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long bookingId, BookingCancelDTO cancelDTO) {
        Long currentUserId = getCurrentUserId();

        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!booking.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权取消他人的预约");
        }
        if (!BookingStatusConstant.BOOKED.equals(booking.getStatus())) {
            throw new BusinessException("当前预约状态不允许取消");
        }

        int affectedRows = bookingMapper.cancelBooking(bookingId, cancelDTO.getCancelReason());
        if (affectedRows == 0) {
            throw new BusinessException("取消预约失败");
        }

        bookingMapper.restoreSlotCapacity(booking.getSlotId(), booking.getPersonCount());
    }

    /**
     * 场地预约核销。
     * 只有当前登录用户自己的预约记录，且状态为已预约时，才允许完成核销。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkinBooking(Long bookingId) {
        Long currentUserId = getCurrentUserId();

        Booking booking = bookingMapper.getBookingById(bookingId);
        if (booking == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!booking.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权核销他人的预约");
        }
        if (!BookingStatusConstant.BOOKED.equals(booking.getStatus())) {
            throw new BusinessException("当前预约状态不允许核销");
        }

        int affectedRows = bookingMapper.checkinBooking(bookingId);
        if (affectedRows == 0) {
            throw new BusinessException("预约核销失败");
        }
    }

    /**
     * 生成预约单号。
     * 单号由固定前缀、当前时间和四位随机数组成，用于区分业务预约记录。
     */
    private String generateBookingNo() {
        return "BK" + LocalDateTime.now().format(BOOKING_NO_TIME_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /**
     * 获取当前登录用户 ID。
     * booking 模块所有“我的数据”与写操作，都应以登录态为准而不是信任前端传参。
     */
    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
