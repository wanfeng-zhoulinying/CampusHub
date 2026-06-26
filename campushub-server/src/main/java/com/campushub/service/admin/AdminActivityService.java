package com.campushub.service.admin;

import com.campushub.dto.AdminActivityAuditDTO;
import com.campushub.dto.AdminActivityQueryDTO;
import com.campushub.dto.AdminActivitySaveDTO;
import com.campushub.vo.AdminActivityListVO;

import java.util.List;

public interface AdminActivityService {

    List<AdminActivityListVO> listActivities(AdminActivityQueryDTO queryDTO);

    Long createActivity(AdminActivitySaveDTO saveDTO);

    void updateActivity(Long activityId, AdminActivitySaveDTO saveDTO);

    void auditActivity(Long activityId, AdminActivityAuditDTO auditDTO);

    void updateActivityStatus(Long activityId, Integer status);
}
