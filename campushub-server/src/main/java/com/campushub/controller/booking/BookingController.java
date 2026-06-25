package com.campushub.controller.booking;

import com.campushub.common.Result;
import com.campushub.dto.BookingCancelDTO;
import com.campushub.dto.BookingCreateDTO;
import com.campushub.dto.BookingQueryDTO;
import com.campushub.service.booking.BookingService;
import com.campushub.vo.BookingListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * 创建预约接口。
     * 当前用户身份由 JWT 登录态提供，请求体只保留场地预约本身需要的业务字段。
     */
    @PostMapping("/create")
    public Result<Long> createBooking(@RequestBody BookingCreateDTO createDTO) {
        return Result.success(bookingService.createBooking(createDTO));
    }

    /**
     * 我的预约列表接口。
     * 预约记录默认按当前登录用户查询，可按预约状态筛选。
     */
    @GetMapping("/my")
    public Result<List<BookingListVO>> listMyBookings(BookingQueryDTO queryDTO) {
        return Result.success(bookingService.listMyBookings(queryDTO));
    }

    /**
     * 取消预约接口。
     * 只有当前登录用户自己的预约记录，才允许执行取消操作。
     */
    @PutMapping("/{bookingId}/cancel")
    public Result<Void> cancelBooking(@PathVariable("bookingId") Long bookingId, @RequestBody BookingCancelDTO cancelDTO) {
        bookingService.cancelBooking(bookingId, cancelDTO);
        return Result.success();
    }

    /**
     * 场地预约核销接口。
     * 根据预约记录 ID 完成当前登录用户本人的预约核销。
     */
    @PutMapping("/{bookingId}/checkin")
    public Result<Void> checkinBooking(@PathVariable("bookingId") Long bookingId) {
        bookingService.checkinBooking(bookingId);
        return Result.success();
    }
}
