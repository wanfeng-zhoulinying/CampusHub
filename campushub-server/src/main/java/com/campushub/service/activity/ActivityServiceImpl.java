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
     * 名额未满时直接报名成功；名额已满时进入候补队列。
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

        ActivitySignup existedSignup = activityMapper.getSignupByActivityIdAndUserId(signupDTO.getActivityId(), currentUserId);
        if (existedSignup != null && isActiveSignup(existedSignup.getSignupStatus())) {
            throw new BusinessException("你已经报名过该活动");
        }

        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(signupDTO.getActivityId());
        signup.setUserId(currentUserId);
        signup.setSignupTime(LocalDateTime.now());
        signup.setSignStatus(ActivitySignStatusConstant.NOT_SIGNED);

        if (activity.getCurrentSignupCount() < activity.getSignupLimit()) {
            int affectedRows = activityMapper.increaseSignupCount(signupDTO.getActivityId());
            if (affectedRows == 0) {
                throw new BusinessException("活动报名失败，请刷新后重试");
            }

            signup.setSignupStatus(ActivitySignupStatusConstant.SIGNED_UP);
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

        if (activity.getWaitLimit() == null || activity.getWaitLimit() <= 0) {
            throw new BusinessException("活动报名人数已满");
        }

        Integer currentWaitCount = activityMapper.countWaitlistedSignups(signupDTO.getActivityId());
        if (currentWaitCount >= activity.getWaitLimit()) {
            throw new BusinessException("活动候补人数已满");
        }

        signup.setSignupStatus(ActivitySignupStatusConstant.WAITLISTED);
        signup.setWaitOrder(currentWaitCount + 1);
        activityMapper.saveSignup(signup);
        messageService.createMessage(
                currentUserId,
                "活动候补成功",
                "当前活动正式名额已满，你已进入候补队列，当前候补顺位：" + signup.getWaitOrder() + "。活动：" + activity.getTitle(),
                MessageTypeConstant.ACTIVITY,
                signup.getId()
        );
        return signup.getId();
    }

    /**
     * 查询我的活动报名列表。
     */
    @Override
    public List<ActivitySignupVO> listMySignups(ActivitySignupQueryDTO queryDTO) {
        return activityMapper.listUserSignups(getCurrentUserId(), queryDTO.getSignupStatus());
    }

    /**
     * 取消活动报名。
     * 正式报名取消后会尝试自动补位候补用户。
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
        if (!isCancelableSignup(signup.getSignupStatus())) {
            throw new BusinessException("当前报名状态不允许取消");
        }

        int affectedRows = activityMapper.cancelSignup(signupId);
        if (affectedRows == 0) {
            throw new BusinessException("取消报名失败");
        }

        Activity activity = activityMapper.getActivityById(signup.getActivityId());

        if (ActivitySignupStatusConstant.WAITLISTED.equals(signup.getSignupStatus())) {
            activityMapper.adjustWaitlistOrderAfterRemoval(signup.getActivityId(), signup.getWaitOrder());
            messageService.createMessage(
                    currentUserId,
                    "活动候补取消",
                    "你已取消候补活动：" + getActivityTitle(activity),
                    MessageTypeConstant.ACTIVITY,
                    signupId
            );
            return;
        }

        ActivitySignup waitlistedSignup = activityMapper.getFirstWaitlistedSignup(signup.getActivityId());
        if (waitlistedSignup != null) {
            int promoteRows = activityMapper.promoteWaitlistedSignup(waitlistedSignup.getId());
            if (promoteRows == 0) {
                throw new BusinessException("候补补位失败");
            }
            activityMapper.adjustWaitlistOrderAfterRemoval(signup.getActivityId(), waitlistedSignup.getWaitOrder());
            messageService.createMessage(
                    waitlistedSignup.getUserId(),
                    "活动候补补位成功",
                    "你候补的活动已补位成功，请及时查看。活动：" + getActivityTitle(activity),
                    MessageTypeConstant.ACTIVITY,
                    waitlistedSignup.getId()
            );
        } else {
            activityMapper.decreaseSignupCount(signup.getActivityId());
        }

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
     * 正式报名和候补转正用户都可以签到。
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
        if (!isSignableSignup(signup.getSignupStatus())) {
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

    private boolean isActiveSignup(Integer signupStatus) {
        return ActivitySignupStatusConstant.SIGNED_UP.equals(signupStatus)
                || ActivitySignupStatusConstant.WAITLISTED.equals(signupStatus)
                || ActivitySignupStatusConstant.WAITLIST_CONFIRMED.equals(signupStatus);
    }

    private boolean isCancelableSignup(Integer signupStatus) {
        return ActivitySignupStatusConstant.SIGNED_UP.equals(signupStatus)
                || ActivitySignupStatusConstant.WAITLISTED.equals(signupStatus)
                || ActivitySignupStatusConstant.WAITLIST_CONFIRMED.equals(signupStatus);
    }

    private boolean isSignableSignup(Integer signupStatus) {
        return ActivitySignupStatusConstant.SIGNED_UP.equals(signupStatus)
                || ActivitySignupStatusConstant.WAITLIST_CONFIRMED.equals(signupStatus);
    }

    private String getActivityTitle(Activity activity) {
        return activity == null ? "活动" : activity.getTitle();
    }

    /**
     * 获取当前登录用户 ID。
     */
    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
