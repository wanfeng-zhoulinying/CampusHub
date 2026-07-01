package com.campushub.controller.venue;

import com.campushub.common.Result;
import com.campushub.dto.VenueQueryDTO;
import com.campushub.service.venue.VenueService;
import com.campushub.vo.VenueDetailVO;
import com.campushub.vo.VenueListVO;
import com.campushub.vo.VenueSlotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/venue")
@RequiredArgsConstructor
@Slf4j
public class VenueController {

    private final VenueService venueService;

    /**
     * 场地列表查询接口。
     * 支持按分类、关键字和状态筛选场地。
     */
    @GetMapping("/list")
    public Result<List<VenueListVO>> listVenues(VenueQueryDTO queryDTO) {
        log.info("[Venue] list category={}, keyword={}, status={}",
                queryDTO.getCategory(), queryDTO.getKeyword(), queryDTO.getStatus());
        return Result.success(venueService.listVenues(queryDTO));
    }

    /**
     * 场地详情查询接口。
     * 根据场地 ID 返回单个场地的基础展示信息。
     */
    @GetMapping("/{venueId}")
    public Result<VenueDetailVO> getVenueDetail(@PathVariable("venueId") Long venueId) {
        log.info("[Venue] detail venueId={}", venueId);
        return Result.success(venueService.getVenueDetail(venueId));
    }

    /**
     * 场地时间段查询接口。
     * 查询指定场地在某一天可展示的时间段信息，为后续预约下单提供数据。
     */
    @GetMapping("/{venueId}/slots")
    public Result<List<VenueSlotVO>> listVenueSlots(
            @PathVariable("venueId") Long venueId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("[Venue] slots venueId={}, date={}", venueId, date);
        return Result.success(venueService.listVenueSlots(venueId, date));
    }
}
