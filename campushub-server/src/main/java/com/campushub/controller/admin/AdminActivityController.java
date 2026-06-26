package com.campushub.controller.admin;

import com.campushub.common.Result;
import com.campushub.dto.AdminActivityAuditDTO;
import com.campushub.dto.AdminActivityQueryDTO;
import com.campushub.dto.AdminActivitySaveDTO;
import com.campushub.service.admin.AdminActivityService;
import com.campushub.vo.AdminActivityListVO;
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
@RequestMapping("/admin/activity")
@RequiredArgsConstructor
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    /**
     * 后台活动列表接口。
     * 支持按标题、活动状态、审核状态筛选活动。
     */
    @GetMapping("/list")
    public Result<List<AdminActivityListVO>> listActivities(AdminActivityQueryDTO queryDTO) {
        return Result.success(adminActivityService.listActivities(queryDTO));
    }

    /**
     * 后台新增活动接口。
     * 创建活动基础信息并初始化活动状态。
     */
    @PostMapping
    public Result<Long> createActivity(@RequestBody AdminActivitySaveDTO saveDTO) {
        return Result.success(adminActivityService.createActivity(saveDTO));
    }

    /**
     * 后台修改活动接口。
     * 根据活动 ID 修改活动基础信息。
     */
    @PutMapping("/{activityId}")
    public Result<Void> updateActivity(@PathVariable("activityId") Long activityId,
                                       @RequestBody AdminActivitySaveDTO saveDTO) {
        adminActivityService.updateActivity(activityId, saveDTO);
        return Result.success();
    }

    /**
     * 后台活动审核接口。
     * 管理员可对活动执行通过或驳回操作。
     */
    @PutMapping("/{activityId}/audit")
    public Result<Void> auditActivity(@PathVariable("activityId") Long activityId,
                                      @RequestBody AdminActivityAuditDTO auditDTO) {
        adminActivityService.auditActivity(activityId, auditDTO);
        return Result.success();
    }

    /**
     * 后台活动状态维护接口。
     * 直接切换活动状态，便于管理端维护活动生命周期。
     */
    @PutMapping("/{activityId}/status")
    public Result<Void> updateActivityStatus(@PathVariable("activityId") Long activityId,
                                             @RequestParam("status") Integer status) {
        adminActivityService.updateActivityStatus(activityId, status);
        return Result.success();
    }
}
