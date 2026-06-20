package com.campushub.service.activity;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.constant.ActivitySignStatusConstant;
import com.campushub.constant.ActivitySignupStatusConstant;
import com.campushub.constant.ActivityStatusConstant;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupCancelDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.entity.Activity;
import com.campushub.entity.ActivitySignup;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.vo.ActivityDetailVO;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySignupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;

    /**
     * 查询活动列表。
     * 默认只查询已通过审核且处于报名中的活动，并按 id 倒序返回。
     */
    @Override
    public List<ActivityListVO> listActivities(ActivityQueryDTO queryDTO) {
        Integer status = queryDTO.getStatus() == null ? ActivityStatusConstant.SIGNING_UP : queryDTO.getStatus();
        Integer auditStatus = queryDTO.getAuditStatus() == null
                ? ActivityAuditStatusConstant.APPROVED
                : queryDTO.getAuditStatus();
        return activityMapper.listActivities(queryDTO.getKeyword(), status, auditStatus);
    }

    @Override
    public ActivityDetailVO getActivityDetail(Long id) {
        return activityMapper.getActivityDetailById(id);
    }

    /**
     * 活动报名。
     * 当前阶段先做最小闭环校验：活动是否存在、是否通过审核、是否重复报名、是否已满员。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long signupActivity(ActivitySignupDTO signupDTO) {
        if (signupDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }
        if (signupDTO.getActivityId() == null) {
            throw new BusinessException("activityId不能为空");
        }

        Activity activity = activityMapper.getActivityById(signupDTO.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (!ActivityAuditStatusConstant.APPROVED.equals(activity.getAuditStatus())) {
            throw new BusinessException("活动未通过审核");
        }
        if (!ActivityStatusConstant.SIGNING_UP.equals(activity.getStatus())) {
            throw new BusinessException("当前活动不允许报名");
        }

        ActivitySignup existedSignup = activityMapper.getSignupByActivityIdAndUserId(
                signupDTO.getActivityId(),
                signupDTO.getUserId()
        );
        if (existedSignup != null
                && ActivitySignupStatusConstant.SIGNED_UP.equals(existedSignup.getSignupStatus())) {
            throw new BusinessException("你已经报名过该活动");
        }

        if (activity.getCurrentSignupCount() >= activity.getSignupLimit()) {
            throw new BusinessException("活动报名人数已满");
        }

        activityMapper.increaseSignupCount(signupDTO.getActivityId());

        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(signupDTO.getActivityId());
        signup.setUserId(signupDTO.getUserId());
        signup.setSignupTime(LocalDateTime.now());
        signup.setSignupStatus(ActivitySignupStatusConstant.SIGNED_UP);
        signup.setSignStatus(ActivitySignStatusConstant.NOT_SIGNED);
        signup.setWaitOrder(null);
        activityMapper.saveSignup(signup);
        return signup.getId();
    }

    /**
     * 查询当前用户的活动报名列表。
     */
    @Override
    public List<ActivitySignupVO> listMySignups(ActivitySignupQueryDTO queryDTO) {
        if (queryDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }
        return activityMapper.listUserSignups(queryDTO.getUserId(), queryDTO.getSignupStatus());
    }

    /**
     * 取消活动报名。
     * 只有当前用户自己的报名记录才能取消，取消后同步回滚活动报名人数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long signupId, ActivitySignupCancelDTO cancelDTO) {
        if (cancelDTO.getUserId() == null) {
            throw new BusinessException("userId不能为空");
        }

        ActivitySignup signup = activityMapper.getSignupById(signupId);
        if (signup == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!signup.getUserId().equals(cancelDTO.getUserId())) {
            throw new BusinessException("无权取消他人的报名");
        }
        if (!ActivitySignupStatusConstant.SIGNED_UP.equals(signup.getSignupStatus())) {
            throw new BusinessException("当前报名状态不允许取消");
        }

        int affectedRows = activityMapper.cancelSignup(signupId);
        if (affectedRows == 0) {
            throw new BusinessException("取消报名失败");
        }

        activityMapper.decreaseSignupCount(signup.getActivityId());
    }

    /**
     * 活动签到。
     * 只有报名成功且尚未签到的记录，才允许执行签到。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signActivity(Long signupId, Long userId) {
        if (userId == null) {
            throw new BusinessException("userId不能为空");
        }

        ActivitySignup signup = activityMapper.getSignupById(signupId);
        if (signup == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!signup.getUserId().equals(userId)) {
            throw new BusinessException("无权签到他人的活动报名");
        }
        if (!ActivitySignupStatusConstant.SIGNED_UP.equals(signup.getSignupStatus())) {
            throw new BusinessException("当前报名状态不允许签到");
        }
        if (!ActivitySignStatusConstant.NOT_SIGNED.equals(signup.getSignStatus())) {
            throw new BusinessException("当前报名记录已签到");
        }

        int affectedRows = activityMapper.signActivity(signupId);
        if (affectedRows == 0) {
            throw new BusinessException("活动签到失败");
        }
    }
}
