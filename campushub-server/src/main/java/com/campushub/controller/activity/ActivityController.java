package com.campushub.controller.activity;

import com.campushub.common.Result;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySignupCancelDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.service.activity.ActivityService;
import com.campushub.vo.ActivityDetailVO;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySignupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 活动列表查询接口。
     * 支持按关键字、状态、审核状态筛选活动。
     */
    @GetMapping("/list")
    public Result<List<ActivityListVO>> listActivities(ActivityQueryDTO queryDTO) {
        return Result.success(activityService.listActivities(queryDTO));
    }

    /**
     * 活动详情查询接口。
     * 根据活动ID查询单个活动的详细信息。
     */
    @GetMapping("/{id}")
    public Result<ActivityDetailVO> getActivityDetail(@PathVariable("id") Long id) {
        return Result.success(activityService.getActivityDetail(id));
    }

    /**
     * 活动报名接口。
     * 用户提交报名信息后返回报名记录ID。
     */
    @PostMapping("/signup")
    public Result<Long> signupActivity(@RequestBody ActivitySignupDTO signupDTO) {
        return Result.success(activityService.signupActivity(signupDTO));
    }

    /**
     * 我的报名记录查询接口。
     * 根据用户条件查询当前用户的活动报名列表。
     */
    @GetMapping("/my")
    public Result<List<ActivitySignupVO>> listMySignups(ActivitySignupQueryDTO queryDTO) {
        return Result.success(activityService.listMySignups(queryDTO));
    }

    /**
     * 取消活动报名接口。
     * 根据报名记录ID取消对应报名。
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelSignup(@PathVariable("id") Long id, @RequestBody ActivitySignupCancelDTO cancelDTO) {
        activityService.cancelSignup(id, cancelDTO);
        return Result.success();
    }
}
