package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.service.statistics.StatisticsService;
import com.campushub.vo.AdminBookingStatusStatVO;
import com.campushub.vo.AdminDashboardOverviewVO;
import com.campushub.vo.AdminHotActivityVO;
import com.campushub.vo.AdminHotVenueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 后台看板总览接口。
     */
    @GetMapping("/overview")
    public Result<AdminDashboardOverviewVO> getDashboardOverview() {
        return Result.success(statisticsService.getDashboardOverview());
    }

    /**
     * 后台预约状态分布接口。
     */
    @GetMapping("/booking/status")
    public Result<List<AdminBookingStatusStatVO>> listBookingStatusStats() {
        return Result.success(statisticsService.listBookingStatusStats());
    }

    /**
     * 后台热门场地排行接口。
     */
    @GetMapping("/venue/hot")
    public Result<List<AdminHotVenueVO>> listHotVenues(@RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(statisticsService.listHotVenues(limit));
    }

    /**
     * 后台热门活动排行接口。
     */
    @GetMapping("/activity/hot")
    public Result<List<AdminHotActivityVO>> listHotActivities(@RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(statisticsService.listHotActivities(limit));
    }
}
