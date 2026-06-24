package com.campushub.controller.activity;

import com.campushub.common.Result;
import com.campushub.dto.ActivityQueryDTO;
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
     * 支持按关键字、活动状态、审核状态筛选活动。
     */
    @GetMapping("/list")
    public Result<List<ActivityListVO>> listActivities(ActivityQueryDTO queryDTO) {
        return Result.success(activityService.listActivities(queryDTO));
    }

    /**
     * 活动详情查询接口。
     * 根据活动 ID 查询单个活动的详细信息。
     */
    @GetMapping("/{id}")
    public Result<ActivityDetailVO> getActivityDetail(@PathVariable("id") Long id) {
        return Result.success(activityService.getActivityDetail(id));
    }

    /**
     * 活动报名接口。
     * 当前登录用户提交报名后，返回活动报名记录 ID。
     */
    @PostMapping("/signup")
    public Result<Long> signupActivity(@RequestBody ActivitySignupDTO signupDTO) {
        return Result.success(activityService.signupActivity(signupDTO));
    }

    /**
     * 我的报名记录查询接口。
     * 根据当前登录用户查询活动报名记录，可按报名状态筛选。
     */
    @GetMapping("/my")
    public Result<List<ActivitySignupVO>> listMySignups(ActivitySignupQueryDTO queryDTO) {
        return Result.success(activityService.listMySignups(queryDTO));
    }

    /**
     * 取消活动报名接口。
     * 根据报名记录 ID 取消当前登录用户自己的活动报名。
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelSignup(@PathVariable("id") Long id) {
        activityService.cancelSignup(id);
        return Result.success();
    }

    /**
     * 活动签到接口。
     * 根据活动报名记录 ID 完成当前登录用户本人的活动签到。
     */
    @PutMapping("/{id}/checkin")
    public Result<Void> signActivity(@PathVariable("id") Long id) {
        activityService.signActivity(id);
        return Result.success();
    }
}
