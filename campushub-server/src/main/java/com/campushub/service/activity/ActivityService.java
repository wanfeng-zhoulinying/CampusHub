package com.campushub.service.activity;

import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupCancelDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.vo.ActivityDetailVO;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySignupVO;

import java.util.List;

public interface ActivityService {

    List<ActivityListVO> listActivities(ActivityQueryDTO queryDTO);

    ActivityDetailVO getActivityDetail(Long id);

    Long signupActivity(ActivitySignupDTO signupDTO);

    List<ActivitySignupVO> listMySignups(ActivitySignupQueryDTO queryDTO);

    void cancelSignup(Long signupId, ActivitySignupCancelDTO cancelDTO);
}
