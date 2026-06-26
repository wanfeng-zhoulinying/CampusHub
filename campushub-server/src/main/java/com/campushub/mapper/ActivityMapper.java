package com.campushub.mapper;

import com.campushub.entity.Activity;
import com.campushub.entity.ActivitySignup;
import com.campushub.vo.ActivityDetailVO;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySignupVO;
import com.campushub.vo.AdminActivityListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper {

    List<ActivityListVO> listActivities(@Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        @Param("auditStatus") Integer auditStatus);

    ActivityDetailVO getActivityDetailById(@Param("id") Long id);

    int saveSignup(ActivitySignup activitySignup);

    ActivitySignup getSignupById(@Param("id") Long id);

    ActivitySignup getSignupByActivityIdAndUserId(@Param("activityId") Long activityId,
                                                  @Param("userId") Long userId);

    List<ActivitySignupVO> listUserSignups(@Param("userId") Long userId,
                                           @Param("signupStatus") Integer signupStatus);

    int cancelSignup(@Param("id") Long id);

    int increaseSignupCount(@Param("activityId") Long activityId);

    int decreaseSignupCount(@Param("activityId") Long activityId);

    Activity getActivityById(@Param("id") Long id);

    int signActivity(@Param("id") Long id);

    List<AdminActivityListVO> listAdminActivities(@Param("title") String title,
                                                  @Param("status") Integer status,
                                                  @Param("auditStatus") Integer auditStatus);

    int saveActivity(Activity activity);

    int updateActivity(Activity activity);

    int auditActivity(@Param("activityId") Long activityId,
                      @Param("auditStatus") Integer auditStatus,
                      @Param("auditRemark") String auditRemark,
                      @Param("auditUserId") Long auditUserId);

    int updateActivityStatus(@Param("activityId") Long activityId, @Param("status") Integer status);
}
