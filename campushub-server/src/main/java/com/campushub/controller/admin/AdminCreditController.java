package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.service.credit.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/credit")
@RequiredArgsConstructor
public class AdminCreditController {

    private final CreditService creditService;

    /**
     * 后台登记预约违约接口。
     */
    @PutMapping("/booking/{bookingId}/breach")
    public Result<Void> markBookingBreach(@PathVariable("bookingId") Long bookingId,
                                          @RequestBody AdminCreditBreachDTO breachDTO) {
        creditService.markBookingBreach(bookingId, breachDTO);
        return Result.success();
    }
}
