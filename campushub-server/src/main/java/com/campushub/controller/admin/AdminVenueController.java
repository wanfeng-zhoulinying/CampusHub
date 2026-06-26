package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.dto.AdminVenueQueryDTO;
import com.campushub.dto.AdminVenueSaveDTO;
import com.campushub.service.admin.AdminVenueService;
import com.campushub.vo.AdminVenueListVO;
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
@RequestMapping("/admin/venue")
@RequiredArgsConstructor
public class AdminVenueController {

    private final AdminVenueService adminVenueService;

    /**
     * 后台场地列表接口。
     * 支持按名称、分类、状态筛选场地，便于管理端维护。
     */
    @GetMapping("/list")
    public Result<List<AdminVenueListVO>> listVenues(AdminVenueQueryDTO queryDTO) {
        return Result.success(adminVenueService.listVenues(queryDTO));
    }

    /**
     * 后台新增场地接口。
     * 创建新的场地基础信息记录。
     */
    @PostMapping
    public Result<Long> createVenue(@RequestBody AdminVenueSaveDTO saveDTO) {
        return Result.success(adminVenueService.createVenue(saveDTO));
    }

    /**
     * 后台修改场地接口。
     * 根据场地 ID 修改场地基础信息。
     */
    @PutMapping("/{venueId}")
    public Result<Void> updateVenue(@PathVariable("venueId") Long venueId, @RequestBody AdminVenueSaveDTO saveDTO) {
        adminVenueService.updateVenue(venueId, saveDTO);
        return Result.success();
    }

    /**
     * 后台修改场地状态接口。
     * 直接切换场地启用或关闭状态。
     */
    @PutMapping("/{venueId}/status")
    public Result<Void> updateVenueStatus(@PathVariable("venueId") Long venueId,
                                          @RequestParam("status") Integer status) {
        adminVenueService.updateVenueStatus(venueId, status);
        return Result.success();
    }
}
