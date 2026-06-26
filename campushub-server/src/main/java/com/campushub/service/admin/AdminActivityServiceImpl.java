package com.campushub.service.admin;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.constant.ActivityStatusConstant;
import com.campushub.constant.DeleteStatusConstant;
import com.campushub.dto.AdminActivityAuditDTO;
import com.campushub.dto.AdminActivityQueryDTO;
import com.campushub.dto.AdminActivitySaveDTO;
import com.campushub.entity.Activity;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.vo.AdminActivityListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminActivityServiceImpl implements AdminActivityService {

    private final ActivityMapper activityMapper;

    /**
     * 后台活动列表。
     * 支持按标题、活动状态、审核状态筛选，方便做管理和审核操作。
     */
    @Override
    public List<AdminActivityListVO> listActivities(AdminActivityQueryDTO queryDTO) {
        return activityMapper.listAdminActivities(queryDTO.getTitle(), queryDTO.getStatus(), queryDTO.getAuditStatus());
    }

    /**
     * 新增活动。
     * 后台创建活动时默认写入未删除记录，并将报名人数初始化为0。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(AdminActivitySaveDTO saveDTO) {
        validateActivitySaveDTO(saveDTO);

        Activity activity = new Activity();
        activity.setPublisherId(saveDTO.getPublisherId());
        activity.setTitle(saveDTO.getTitle());
        activity.setCoverUrl(saveDTO.getCoverUrl());
        activity.setContent(saveDTO.getContent());
        activity.setLocation(saveDTO.getLocation());
        activity.setVenueId(saveDTO.getVenueId());
        activity.setSignupStartTime(saveDTO.getSignupStartTime());
        activity.setSignupEndTime(saveDTO.getSignupEndTime());
        activity.setActivityStartTime(saveDTO.getActivityStartTime());
        activity.setActivityEndTime(saveDTO.getActivityEndTime());
        activity.setSignupLimit(saveDTO.getSignupLimit());
        activity.setCurrentSignupCount(0);
        activity.setWaitLimit(saveDTO.getWaitLimit());
        activity.setStatus(saveDTO.getStatus() == null ? ActivityStatusConstant.NOT_STARTED : saveDTO.getStatus());
        activity.setAuditStatus(ActivityAuditStatusConstant.PENDING);
        activity.setIsDeleted(DeleteStatusConstant.NOT_DELETED);

        activityMapper.saveActivity(activity);
        return activity.getId();
    }

    /**
     * 修改活动。
     * 后台只修改活动基础信息，不重置已有报名人数和审核结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(Long activityId, AdminActivitySaveDTO saveDTO) {
        validateActivitySaveDTO(saveDTO);

        Activity existedActivity = activityMapper.getActivityById(activityId);
        if (existedActivity == null || DeleteStatusConstant.DELETED.equals(existedActivity.getIsDeleted())) {
            throw new BusinessException("活动不存在");
        }

        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setPublisherId(saveDTO.getPublisherId());
        activity.setTitle(saveDTO.getTitle());
        activity.setCoverUrl(saveDTO.getCoverUrl());
        activity.setContent(saveDTO.getContent());
        activity.setLocation(saveDTO.getLocation());
        activity.setVenueId(saveDTO.getVenueId());
        activity.setSignupStartTime(saveDTO.getSignupStartTime());
        activity.setSignupEndTime(saveDTO.getSignupEndTime());
        activity.setActivityStartTime(saveDTO.getActivityStartTime());
        activity.setActivityEndTime(saveDTO.getActivityEndTime());
        activity.setSignupLimit(saveDTO.getSignupLimit());
        activity.setWaitLimit(saveDTO.getWaitLimit());
        activity.setStatus(saveDTO.getStatus() == null ? existedActivity.getStatus() : saveDTO.getStatus());

        int affectedRows = activityMapper.updateActivity(activity);
        if (affectedRows == 0) {
            throw new BusinessException("活动修改失败");
        }
    }

    /**
     * 审核活动。
     * 审核接口只处理审核状态、审核备注、审核人和审核时间。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditActivity(Long activityId, AdminActivityAuditDTO auditDTO) {
        if (auditDTO.getAuditStatus() == null) {
            throw new BusinessException("审核状态不能为空");
        }
        if (auditDTO.getAuditUserId() == null) {
            throw new BusinessException("审核人不能为空");
        }

        int affectedRows = activityMapper.auditActivity(
                activityId,
                auditDTO.getAuditStatus(),
                auditDTO.getAuditRemark(),
                auditDTO.getAuditUserId()
        );
        if (affectedRows == 0) {
            throw new BusinessException("活动审核失败");
        }
    }

    /**
     * 修改活动状态。
     * 管理端可以直接维护活动的当前状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivityStatus(Long activityId, Integer status) {
        if (status == null) {
            throw new BusinessException("活动状态不能为空");
        }

        int affectedRows = activityMapper.updateActivityStatus(activityId, status);
        if (affectedRows == 0) {
            throw new BusinessException("活动状态修改失败");
        }
    }

    private void validateActivitySaveDTO(AdminActivitySaveDTO saveDTO) {
        if (saveDTO.getPublisherId() == null) {
            throw new BusinessException("发布人不能为空");
        }
        if (saveDTO.getTitle() == null || saveDTO.getTitle().isBlank()) {
            throw new BusinessException("活动标题不能为空");
        }
        if (saveDTO.getLocation() == null || saveDTO.getLocation().isBlank()) {
            throw new BusinessException("活动地点不能为空");
        }
        if (saveDTO.getSignupStartTime() == null || saveDTO.getSignupEndTime() == null) {
            throw new BusinessException("报名时间不能为空");
        }
        if (saveDTO.getActivityStartTime() == null || saveDTO.getActivityEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (saveDTO.getSignupLimit() == null || saveDTO.getSignupLimit() <= 0) {
            throw new BusinessException("报名上限必须大于0");
        }
        if (saveDTO.getWaitLimit() == null || saveDTO.getWaitLimit() < 0) {
            throw new BusinessException("候补上限不能小于0");
        }
    }
}
