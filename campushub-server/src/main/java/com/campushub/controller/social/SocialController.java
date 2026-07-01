package com.campushub.controller.social;

import com.campushub.common.Result;
import com.campushub.dto.ActivityCommentCreateDTO;
import com.campushub.service.social.SocialService;
import com.campushub.vo.ActivityCommentVO;
import com.campushub.vo.ActivityFavoriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@Slf4j
public class SocialController {

    private final SocialService socialService;

    /**
     * 发布活动评论。
     */
    @PostMapping("/activity/comment")
    public Result<Long> createActivityComment(@RequestBody ActivityCommentCreateDTO createDTO) {
        log.info("[Social] create comment activityId={}", createDTO.getActivityId());
        return Result.success(socialService.createActivityComment(createDTO));
    }

    /**
     * 查询活动评论列表。
     */
    @GetMapping("/activity/{activityId}/comments")
    public Result<List<ActivityCommentVO>> listActivityComments(@PathVariable("activityId") Long activityId) {
        log.info("[Social] comments activityId={}", activityId);
        return Result.success(socialService.listActivityComments(activityId));
    }

    /**
     * 切换评论点赞状态。
     */
    @PostMapping("/comment/{commentId}/like")
    public Result<Boolean> toggleCommentLike(@PathVariable("commentId") Long commentId) {
        log.info("[Social] toggle comment like commentId={}", commentId);
        return Result.success(socialService.toggleCommentLike(commentId));
    }

    /**
     * 切换活动收藏状态。
     */
    @PostMapping("/activity/{activityId}/favorite")
    public Result<Boolean> toggleActivityFavorite(@PathVariable("activityId") Long activityId) {
        log.info("[Social] toggle activity favorite activityId={}", activityId);
        return Result.success(socialService.toggleActivityFavorite(activityId));
    }

    /**
     * 查询我的活动收藏列表。
     */
    @GetMapping("/activity/favorites/my")
    public Result<List<ActivityFavoriteVO>> listMyFavorites() {
        log.info("[Social] my favorites");
        return Result.success(socialService.listMyFavorites());
    }
}
