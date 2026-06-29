package com.campushub.service.social;

import com.campushub.dto.ActivityCommentCreateDTO;
import com.campushub.vo.ActivityCommentVO;
import com.campushub.vo.ActivityFavoriteVO;

import java.util.List;

public interface SocialService {

    Long createActivityComment(ActivityCommentCreateDTO createDTO);

    List<ActivityCommentVO> listActivityComments(Long activityId);

    Boolean toggleCommentLike(Long commentId);

    Boolean toggleActivityFavorite(Long activityId);

    List<ActivityFavoriteVO> listMyFavorites();
}
