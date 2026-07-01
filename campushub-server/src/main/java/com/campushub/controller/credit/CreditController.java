package com.campushub.controller.credit;

import com.campushub.common.Result;
import com.campushub.service.credit.CreditService;
import com.campushub.vo.CreditOverviewVO;
import com.campushub.vo.CreditRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    /**
     * 查询我的信用分概览接口。
     */
    @GetMapping("/my")
    public Result<CreditOverviewVO> getMyCreditOverview() {
        return Result.success(creditService.getMyCreditOverview());
    }

    /**
     * 查询我的信用分变动记录接口。
     */
    @GetMapping("/records/my")
    public Result<List<CreditRecordVO>> listMyCreditRecords() {
        return Result.success(creditService.listMyCreditRecords());
    }
}
