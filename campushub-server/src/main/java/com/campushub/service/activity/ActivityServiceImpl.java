package com.campushub.service.activity;

import com.campushub.constant.ActivityAuditStatusConstant;
import com.campushub.constant.ActivitySignStatusConstant;
import com.campushub.constant.ActivitySignupStatusConstant;
import com.campushub.constant.ActivityStatusConstant;
import com.campushub.constant.CreditRuleConstant;
import com.campushub.constant.MessageTypeConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.entity.Activity;
import com.campushub.entity.ActivitySignup;
import com.campushub.entity.SysUser;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.service.cache.RedisCacheService;
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
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final RedisCacheService redisCacheService;
    private final ActivitySignupRedisService activitySignupRedisService;

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
        String cacheKey = buildActivityDetailKey(activityId);
        // 活动详情使用 Cache Aside，命中空值缓存时不再重复查询数据库。
        return redisCacheService.queryWithPassThrough(
                cacheKey,
                ActivityDetailVO.class,
                RedisTtlConstant.ACTIVITY_DETAIL_MINUTES,
                () -> activityMapper.getActivityDetailById(activityId)
        );
    }

    /**
     * 活动报名。
     * 名额未满时直接报名成功；名额已满时进入候补队列。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long signupActivity(ActivitySignupDTO signupDTO) {
        Long currentUserId = getCurrentUserId();
        validateActivitySignupCreditScore(currentUserId);
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

        ActivitySignupRedisService.SignupReserveResult reserveResult = null;
        boolean requestLocked = false;
        try {
            activitySignupRedisService.acquireRequestLock(signupDTO.getActivityId(), currentUserId);
            requestLocked = true;

            ActivitySignup existedSignup = activityMapper.getSignupByActivityIdAndUserId(signupDTO.getActivityId(), currentUserId);
            if (existedSignup != null && isActiveSignup(existedSignup.getSignupStatus())) {
                throw new BusinessException("你已经报名过该活动");
            }

            // Redis 幂等锁先拦重复点击，再通过原子预占名额减少高并发直接打数据库。
            Integer currentWaitCount = activityMapper.countWaitlistedSignups(signupDTO.getActivityId());
            reserveResult = activitySignupRedisService.reserveSignup(
                    activity,
                    currentUserId,
                    currentWaitCount == null ? 0 : currentWaitCount
            );
            if (reserveResult.isFull()) {
                if (activity.getWaitLimit() == null || activity.getWaitLimit() <= 0) {
                    throw new BusinessException("活动报名人数已满");
                }
                throw new BusinessException("活动候补人数已满");
            }

            ActivitySignup signup = new ActivitySignup();
            signup.setActivityId(signupDTO.getActivityId());
            signup.setUserId(currentUserId);
            signup.setSignupTime(LocalDateTime.now());
            signup.setSignStatus(ActivitySignStatusConstant.NOT_SIGNED);

            if (reserveResult.isSigned()) {
                int affectedRows = activityMapper.increaseSignupCount(signupDTO.getActivityId());
                if (affectedRows == 0) {
                    activitySignupRedisService.clearActivityCounters(signupDTO.getActivityId());
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
                evictActivityDetailCache(signupDTO.getActivityId());
                return signup.getId();
            }

            signup.setSignupStatus(ActivitySignupStatusConstant.WAITLISTED);
            signup.setWaitOrder(reserveResult.getWaitOrder());
            activityMapper.saveSignup(signup);
            messageService.createMessage(
                    currentUserId,
                    "活动候补成功",
                    "当前活动正式名额已满，你已进入候补队列，当前候补顺位：" + signup.getWaitOrder() + "。活动：" + activity.getTitle(),
                    MessageTypeConstant.ACTIVITY,
                    signup.getId()
            );
            return signup.getId();
        } catch (RuntimeException e) {
            if (requestLocked) {
                activitySignupRedisService.releaseReservedQuota(signupDTO.getActivityId(), currentUserId, reserveResult);
            }
            throw e;
        }
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
            activitySignupRedisService.syncAfterCancel(signup.getActivityId(), signup.getSignupStatus(), false);
            evictActivityDetailCache(signup.getActivityId());
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
            activitySignupRedisService.syncAfterCancel(signup.getActivityId(), signup.getSignupStatus(), true);
        } else {
            activityMapper.decreaseSignupCount(signup.getActivityId());
            activitySignupRedisService.syncAfterCancel(signup.getActivityId(), signup.getSignupStatus(), false);
        }

        messageService.createMessage(
                currentUserId,
                "活动报名取消",
                "你已取消报名活动：" + getActivityTitle(activity),
                MessageTypeConstant.ACTIVITY,
                signupId
        );
        evictActivityDetailCache(signup.getActivityId());
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
     * 构建活动详情缓存 key。
     */
    private String buildActivityDetailKey(Long activityId) {
        return RedisKeyConstant.ACTIVITY_DETAIL + activityId;
    }

    /**
     * 删除活动详情缓存。
     */
    private void evictActivityDetailCache(Long activityId) {
        redisCacheService.delete(buildActivityDetailKey(activityId));
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

    private void validateActivitySignupCreditScore(Long userId) {
        SysUser user = userMapper.getById(userId);
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }
        Integer creditScore = user.getCreditScore() == null ? CreditRuleConstant.MIN_SCORE : user.getCreditScore();
        if (creditScore < CreditRuleConstant.ACTIVITY_SIGNUP_MIN_SCORE) {
            throw new BusinessException("当前信用分过低，暂不可报名活动");
        }
    }
}
