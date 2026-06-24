package com.campushub.service.activity;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.constant.ActivitySignStatusConstant;
import com.campushub.constant.ActivitySignupStatusConstant;
import com.campushub.constant.ActivityStatusConstant;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.entity.Activity;
import com.campushub.entity.ActivitySignup;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.utils.UserContext;
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
     * 当前阶段先完成最小闭环校验：活动存在、审核通过、未重复报名、报名名额未满。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long signupActivity(ActivitySignupDTO signupDTO) {
        Long currentUserId = getCurrentUserId();
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
                currentUserId
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
        signup.setUserId(currentUserId);
        signup.setSignupTime(LocalDateTime.now());
        signup.setSignupStatus(ActivitySignupStatusConstant.SIGNED_UP);
        signup.setSignStatus(ActivitySignStatusConstant.NOT_SIGNED);
        signup.setWaitOrder(null);
        activityMapper.saveSignup(signup);
        return signup.getId();
    }

    /**
     * 查询当前登录用户的活动报名列表。
     * “我的报名”统一从登录态读取用户信息，不再接受前端传 userId。
     */
    @Override
    public List<ActivitySignupVO> listMySignups(ActivitySignupQueryDTO queryDTO) {
        return activityMapper.listUserSignups(getCurrentUserId(), queryDTO.getSignupStatus());
    }

    /**
     * 取消活动报名。
     * 只有当前登录用户自己的报名记录才能取消，取消后同步回滚活动报名人数。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long signupId) {
        Long currentUserId = getCurrentUserId();

        ActivitySignup signup = activityMapper.getSignupById(signupId);
        if (signup == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!signup.getUserId().equals(currentUserId)) {
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
     * 只有当前登录用户自己的报名记录，且报名成功并且尚未签到时，才允许签到。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void signActivity(Long signupId) {
        Long currentUserId = getCurrentUserId();

        ActivitySignup signup = activityMapper.getSignupById(signupId);
        if (signup == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!signup.getUserId().equals(currentUserId)) {
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

    /**
     * 获取当前登录用户 ID。
     * activity 模块所有“我的数据”与写操作，都应以登录态为准而不是信任前端传参。
     */
    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
