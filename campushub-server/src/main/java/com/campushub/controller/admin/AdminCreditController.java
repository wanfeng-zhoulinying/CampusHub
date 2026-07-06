package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.dto.AdminBookingBreachAppealAuditDTO;
import com.campushub.dto.AdminCreditBreachDTO;
import com.campushub.service.credit.CreditService;
import com.campushub.vo.AdminBookingBreachAppealVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/credit")
@RequiredArgsConstructor
@Slf4j
public class AdminCreditController {

    private final CreditService creditService;

    /**
     * 后台登记预约违约接口。
     */
    @PutMapping("/booking/{bookingId}/breach")
    public Result<Void> markBookingBreach(@PathVariable("bookingId") Long bookingId,
                                          @RequestBody AdminCreditBreachDTO breachDTO) {
        log.info("[AdminCredit] booking breach bookingId={}, deductScore={}",
                bookingId, breachDTO.getDeductScore());
        creditService.markBookingBreach(bookingId, breachDTO);
        return Result.success();
    }

    /**
     * 后台违约申诉列表接口。
     */
    @GetMapping("/appeals")
    public Result<List<AdminBookingBreachAppealVO>> listBookingBreachAppeals(
            @RequestParam(value = "appealStatus", required = false) Integer appealStatus) {
        log.info("[AdminCredit] list appeals appealStatus={}", appealStatus);
        return Result.success(creditService.listAdminBookingBreachAppeals(appealStatus));
    }

    /**
     * 后台审核违约申诉接口。
     */
    @PutMapping("/appeals/{appealId}/audit")
    public Result<Void> auditBookingBreachAppeal(@PathVariable("appealId") Long appealId,
                                                 @RequestBody AdminBookingBreachAppealAuditDTO auditDTO) {
        log.info("[AdminCredit] audit appealId={}, auditStatus={}", appealId, auditDTO.getAuditStatus());
        creditService.auditBookingBreachAppeal(appealId, auditDTO);
        return Result.success();
    }
}
