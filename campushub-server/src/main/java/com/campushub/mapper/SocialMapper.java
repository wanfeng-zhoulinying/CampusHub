package com.campushub.mapper;

import com.campushub.entity.ActivityComment;
import com.campushub.entity.ActivityCommentLike;
import com.campushub.entity.ActivityFavorite;
import com.campushub.vo.ActivityCommentVO;
import com.campushub.vo.ActivityFavoriteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SocialMapper {

    int saveComment(ActivityComment comment);

    ActivityComment getCommentById(@Param("commentId") Long commentId);

    List<ActivityCommentVO> listActivityComments(@Param("activityId") Long activityId,
                                                 @Param("currentUserId") Long currentUserId);

    ActivityCommentLike getCommentLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int saveCommentLike(ActivityCommentLike like);

    int updateCommentLikeStatus(@Param("commentId") Long commentId,
                                @Param("userId") Long userId,
                                @Param("status") Integer status);

    int increaseCommentLikeCount(@Param("commentId") Long commentId);

    int decreaseCommentLikeCount(@Param("commentId") Long commentId);

    ActivityFavorite getActivityFavorite(@Param("activityId") Long activityId, @Param("userId") Long userId);

    int saveActivityFavorite(ActivityFavorite favorite);

    int updateActivityFavoriteStatus(@Param("activityId") Long activityId,
                                     @Param("userId") Long userId,
                                     @Param("status") Integer status);

    List<ActivityFavoriteVO> listUserFavorites(@Param("userId") Long userId);
}
