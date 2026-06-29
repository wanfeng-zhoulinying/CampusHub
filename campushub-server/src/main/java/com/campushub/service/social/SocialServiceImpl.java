package com.campushub.service.social;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.SocialStatusConstant;
import com.campushub.dto.ActivityCommentCreateDTO;
import com.campushub.entity.Activity;
import com.campushub.entity.ActivityComment;
import com.campushub.entity.ActivityCommentLike;
import com.campushub.entity.ActivityFavorite;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.ActivityMapper;
import com.campushub.mapper.SocialMapper;
import com.campushub.utils.UserContext;
import com.campushub.vo.ActivityCommentVO;
import com.campushub.vo.ActivityFavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final SocialMapper socialMapper;
    private final ActivityMapper activityMapper;

    /**
     * 发布活动评论。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivityComment(ActivityCommentCreateDTO createDTO) {
        Long currentUserId = getCurrentUserId();
        validateActivityExists(createDTO.getActivityId());
        if (createDTO.getContent() == null || createDTO.getContent().isBlank()) {
            throw new BusinessException("评论内容不能为空");
        }

        ActivityComment comment = new ActivityComment();
        comment.setActivityId(createDTO.getActivityId());
        comment.setUserId(currentUserId);
        comment.setContent(createDTO.getContent());
        comment.setLikeCount(0);
        comment.setIsDeleted(DeleteStatusConstant.NOT_DELETED);
        socialMapper.saveComment(comment);
        return comment.getId();
    }

    /**
     * 查询活动评论列表。
     */
    @Override
    public List<ActivityCommentVO> listActivityComments(Long activityId) {
        validateActivityExists(activityId);
        return socialMapper.listActivityComments(activityId, UserContext.getCurrentUserId());
    }

    /**
     * 切换评论点赞状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleCommentLike(Long commentId) {
        Long currentUserId = getCurrentUserId();
        ActivityComment comment = getValidComment(commentId);

        ActivityCommentLike existedLike = socialMapper.getCommentLike(commentId, currentUserId);
        if (existedLike == null) {
            ActivityCommentLike like = new ActivityCommentLike();
            like.setCommentId(comment.getId());
            like.setUserId(currentUserId);
            like.setStatus(SocialStatusConstant.VALID);
            socialMapper.saveCommentLike(like);
            socialMapper.increaseCommentLikeCount(commentId);
            return true;
        }

        if (SocialStatusConstant.VALID.equals(existedLike.getStatus())) {
            socialMapper.updateCommentLikeStatus(commentId, currentUserId, SocialStatusConstant.INVALID);
            socialMapper.decreaseCommentLikeCount(commentId);
            return false;
        }

        socialMapper.updateCommentLikeStatus(commentId, currentUserId, SocialStatusConstant.VALID);
        socialMapper.increaseCommentLikeCount(commentId);
        return true;
    }

    /**
     * 切换活动收藏状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleActivityFavorite(Long activityId) {
        Long currentUserId = getCurrentUserId();
        validateActivityExists(activityId);

        ActivityFavorite existedFavorite = socialMapper.getActivityFavorite(activityId, currentUserId);
        if (existedFavorite == null) {
            ActivityFavorite favorite = new ActivityFavorite();
            favorite.setActivityId(activityId);
            favorite.setUserId(currentUserId);
            favorite.setStatus(SocialStatusConstant.VALID);
            socialMapper.saveActivityFavorite(favorite);
            return true;
        }

        if (SocialStatusConstant.VALID.equals(existedFavorite.getStatus())) {
            socialMapper.updateActivityFavoriteStatus(activityId, currentUserId, SocialStatusConstant.INVALID);
            return false;
        }

        socialMapper.updateActivityFavoriteStatus(activityId, currentUserId, SocialStatusConstant.VALID);
        return true;
    }

    /**
     * 查询我的活动收藏列表。
     */
    @Override
    public List<ActivityFavoriteVO> listMyFavorites() {
        return socialMapper.listUserFavorites(getCurrentUserId());
    }

    private ActivityComment getValidComment(Long commentId) {
        if (commentId == null) {
            throw new BusinessException("评论ID不能为空");
        }
        ActivityComment comment = socialMapper.getCommentById(commentId);
        if (comment == null || DeleteStatusConstant.DELETED.equals(comment.getIsDeleted())) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }

    private void validateActivityExists(Long activityId) {
        if (activityId == null) {
            throw new BusinessException("活动ID不能为空");
        }
        Activity activity = activityMapper.getActivityById(activityId);
        if (activity == null || DeleteStatusConstant.DELETED.equals(activity.getIsDeleted())) {
            throw new BusinessException("活动不存在");
        }
    }

    private Long getCurrentUserId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return currentUserId;
    }
}
