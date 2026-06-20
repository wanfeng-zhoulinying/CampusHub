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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * 创建预约接口。
     * 当前版本通过请求体直接传 userId，后续接入登录后再切换成从登录态获取用户信息。
     */
    @PostMapping("/create")
    public Result<Long> createBooking(@RequestBody BookingCreateDTO createDTO) {
        return Result.success(bookingService.createBooking(createDTO));
    }

    /**
     * 我的预约列表接口。
     * 当前版本通过 userId 查询当前用户的预约记录，可按状态筛选。
     */
    @GetMapping("/my")
    public Result<List<BookingListVO>> listMyBookings(BookingQueryDTO queryDTO) {
        return Result.success(bookingService.listMyBookings(queryDTO));
    }

    /**
     * 取消预约接口。
     * 只有预约记录所属用户才能发起取消操作。
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelBooking(@PathVariable("id") Long id, @RequestBody BookingCancelDTO cancelDTO) {
        bookingService.cancelBooking(id, cancelDTO);
        return Result.success();
    }

    /**
     * 场地预约核销接口。
     * 根据预约记录ID完成当前预约的签到核销。
     */
    @PutMapping("/{id}/checkin")
    public Result<Void> checkinBooking(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        bookingService.checkinBooking(id, userId);
        return Result.success();
    }
}
