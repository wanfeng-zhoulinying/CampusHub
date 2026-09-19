package com.campushub.controller.activity;

import com.campushub.common.Result;
import com.campushub.dto.ActivityQueryDTO;
import com.campushub.dto.ActivitySearchDTO;
import com.campushub.dto.ActivitySignupDTO;
import com.campushub.dto.ActivitySignupQueryDTO;
import com.campushub.service.activity.ActivityService;
import com.campushub.service.es.ActivitySearchService;
import com.campushub.vo.ActivityDetailVO;
import com.campushub.vo.ActivityListVO;
import com.campushub.vo.ActivitySearchVO;
import com.campushub.vo.ActivitySignupVO;
import com.campushub.vo.HotActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
@Slf4j
public class ActivityController {

    private final ActivityService activityService;
    private final ActivitySearchService activitySearchService;

    /**
     * 活动列表查询接口。
     * 支持按关键字、活动状态、审核状态筛选活动。
     */
    @GetMapping("/list")
    public Result<List<ActivityListVO>> listActivities(ActivityQueryDTO queryDTO) {
        log.info("[Activity] 查询活动列表 keyword={}, status={}, auditStatus={}",
                queryDTO.getKeyword(), queryDTO.getStatus(), queryDTO.getAuditStatus());
        return Result.success(activityService.listActivities(queryDTO));
    }

    /**
     * 活动搜索接口（ES）。
     * multi_match三字段相关性搜索 + BM25排序 + 标题/内容高亮，
     * ES不可用时自动降级MySQL LIKE，返回结构不变。
     * 注：/search精确路径优先于/{activityId}路径变量匹配，无路由冲突。
     */
    @GetMapping("/search")
    public Result<ActivitySearchVO> searchActivities(ActivitySearchDTO searchDTO) {
        log.info("[Activity] 搜索活动 keyword={}, pageNum={}, pageSize={}",
                searchDTO.getKeyword(), searchDTO.getPageNum(), searchDTO.getPageSize());
        return Result.success(activitySearchService.search(searchDTO));
    }

    /**
     * 热门活动排行榜查询接口。
     * 基于 Redis ZSet 中累计的活动热度分，返回当前热门活动列表。
     */
    @GetMapping("/hot")
    public Result<List<HotActivityVO>> listHotActivities(@RequestParam(value = "limit", required = false) Integer limit) {
        log.info("[Activity] 查询热门活动排行榜 limit={}", limit);
        return Result.success(activityService.listHotActivities(limit));
    }

    /**
     * 活动详情查询接口。
     * 根据活动 ID 查询单个活动的详细信息。
     */
    @GetMapping("/{activityId}")
    public Result<ActivityDetailVO> getActivityDetail(@PathVariable("activityId") Long activityId) {
        log.info("[Activity] 查询活动详情 activityId={}", activityId);
        return Result.success(activityService.getActivityDetail(activityId));
    }

    /**
     * 活动报名接口。
     * 当前登录用户提交报名后，返回活动报名记录 ID。
     */
    @PostMapping("/signup")
    public Result<Long> signupActivity(@RequestBody ActivitySignupDTO signupDTO) {
        log.info("[Activity] 活动报名 activityId={}", signupDTO.getActivityId());
        return Result.success(activityService.signupActivity(signupDTO));
    }

    /**
     * 我的报名记录查询接口。
     * 根据当前登录用户查询活动报名记录，可按报名状态筛选。
     */
    @GetMapping("/my")
    public Result<List<ActivitySignupVO>> listMySignups(ActivitySignupQueryDTO queryDTO) {
        log.info("[Activity] 查询我的活动报名 signupStatus={}", queryDTO.getSignupStatus());
        return Result.success(activityService.listMySignups(queryDTO));
    }

    /**
     * 取消活动报名接口。
     * 根据报名记录 ID 取消当前登录用户自己的活动报名。
     */
    @PutMapping("/{signupId}/cancel")
    public Result<Void> cancelSignup(@PathVariable("signupId") Long signupId) {
        log.info("[Activity] 取消活动报名 signupId={}", signupId);
        activityService.cancelSignup(signupId);
        return Result.success();
    }

    /**
     * 活动签到接口。
     * 根据活动报名记录 ID 完成当前登录用户本人的活动签到。
     */
    @PutMapping("/{signupId}/checkin")
    public Result<Void> signActivity(@PathVariable("signupId") Long signupId) {
        log.info("[Activity] 活动签到 signupId={}", signupId);
        activityService.signActivity(signupId);
        return Result.success();
    }
}
