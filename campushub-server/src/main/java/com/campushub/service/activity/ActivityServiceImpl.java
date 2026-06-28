package com.campushub.service.activity;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.constant.ActivitySignStatusConstant;
import com.campushub.constant.ActivitySignupStatusConstant;
import com.campushub.constant.ActivityStatusConstant;
import com.campushub.constant.MessageTypeConstant;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.entity.Activity;
import com.campushub.entity.ActivitySignup;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.service.message.MessageService;
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
    private final MessageService messageService;

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
    public ActivityDetailVO getActivityDetail(Long activityId) {
        return activityMapper.getActivityDetailById(activityId);
    }

    /**
     * 活动报名。
     * 报名成功后生成活动通知，方便用户在消息中心看到结果。
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

        messageService.createMessage(
                currentUserId,
                "活动报名成功",
                "你已成功报名活动：" + activity.getTitle(),
                MessageTypeConstant.ACTIVITY,
                signup.getId()
        );
        return signup.getId();
    }

    /**
     * 查询当前登录用户的活动报名列表。
     * “我的报名”统一从登录态读取用户信息，不接受前端传 userId。
     */
    @Override
    public List<ActivitySignupVO> listMySignups(ActivitySignupQueryDTO queryDTO) {
        return activityMapper.listUserSignups(getCurrentUserId(), queryDTO.getSignupStatus());
    }

    /**
     * 取消活动报名。
     * 取消成功后同步回滚活动报名人数，并生成通知。
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
        Activity activity = activityMapper.getActivityById(signup.getActivityId());
        messageService.createMessage(
                currentUserId,
                "活动报名取消",
                "你已取消报名活动：" + getActivityTitle(activity),
                MessageTypeConstant.ACTIVITY,
                signupId
        );
    }

    /**
     * 活动签到。
     * 签到成功后生成活动签到通知。
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
        Activity activity = activityMapper.getActivityById(signup.getActivityId());
        messageService.createMessage(
                currentUserId,
                "活动签到成功",
                "你已完成活动签到：" + getActivityTitle(activity),
                MessageTypeConstant.ACTIVITY,
                signupId
        );
    }

    private String getActivityTitle(Activity activity) {
        return activity == null ? "活动" : activity.getTitle();
    }

    /**
     * 获取当前登录用户 ID。
     * activity 模块所有“我的数据”和写操作，都以登录态为准。
     */
    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
