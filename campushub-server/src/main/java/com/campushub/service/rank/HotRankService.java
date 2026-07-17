package com.campushub.service.rank;

import com.campushub.constant.RedisKeyConstant;
import com.campushub.mapper.ActivityMapper;
import com.campushub.mapper.VenueMapper;
import com.campushub.vo.HotActivityVO;
import com.campushub.vo.HotVenueVO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotRankService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityMapper activityMapper;
    private final VenueMapper venueMapper;

    /**
     * 增加活动热度分。
     * 热度属于辅助展示数据，Redis 异常只记录日志，不影响主业务流程。
     */
    public void increaseActivityHeat(Long activityId, double score, String reason) {
        increaseHeat(RedisKeyConstant.HOT_ACTIVITY_RANK, activityId, score, reason, "activityId");
    }

    /**
     * 增加场地热度分。
     * 热度属于辅助展示数据，Redis 异常只记录日志，不影响主业务流程。
     */
    public void increaseVenueHeat(Long venueId, double score, String reason) {
        increaseHeat(RedisKeyConstant.HOT_VENUE_RANK, venueId, score, reason, "venueId");
    }

    /**
     * 查询热门活动排行榜。
     * 先从 Redis ZSet 取活动 ID 和分数，再回表补齐活动展示信息，最后按 Redis 分数顺序返回。
     */
    public List<HotActivityVO> listHotActivities(Integer limit) {
        List<RankItem> rankItems = listRankItems(RedisKeyConstant.HOT_ACTIVITY_RANK, limit);
        if (rankItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = rankItems.stream().map(RankItem::getId).toList();
        List<HotActivityVO> activities = activityMapper.listHotActivitiesByIds(ids);
        Map<Long, HotActivityVO> activityMap = activities.stream()
                .collect(Collectors.toMap(HotActivityVO::getId, Function.identity(), (left, right) -> left));

        return rankItems.stream()
                .map(item -> buildHotActivity(item, activityMap))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查询热门场地排行榜。
     * 先从 Redis ZSet 取场地 ID 和分数，再回表补齐场地展示信息，最后按 Redis 分数顺序返回。
     */
    public List<HotVenueVO> listHotVenues(Integer limit) {
        List<RankItem> rankItems = listRankItems(RedisKeyConstant.HOT_VENUE_RANK, limit);
        if (rankItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = rankItems.stream().map(RankItem::getId).toList();
        List<HotVenueVO> venues = venueMapper.listHotVenuesByIds(ids);
        Map<Long, HotVenueVO> venueMap = venues.stream()
                .collect(Collectors.toMap(HotVenueVO::getId, Function.identity(), (left, right) -> left));

        return rankItems.stream()
                .map(item -> buildHotVenue(item, venueMap))
                .filter(Objects::nonNull)
                .toList();
    }

    private void increaseHeat(String key, Long businessId, double score, String reason, String idName) {
        if (businessId == null || score <= 0) {
            return;
        }
        try {
            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(
                    key,
                    String.valueOf(businessId),
                    score
            );
            log.info("[HotRank] 热度增加 {}={}, score={}, reason={}, newScore={}",
                    idName, businessId, score, reason, newScore);
        } catch (RuntimeException e) {
            log.warn("[HotRank] 热度增加失败 {}={}, reason={}", idName, businessId, reason, e);
        }
    }

    private List<RankItem> listRankItems(String key, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, safeLimit - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankItem> rankItems = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long id = parseId(tuple.getValue(), key);
            if (id == null) {
                continue;
            }
            rankItems.add(new RankItem(id, tuple.getScore() == null ? 0D : tuple.getScore()));
        }
        rankItems.sort(Comparator.comparing(RankItem::getScore).reversed().thenComparing(RankItem::getId));
        return rankItems;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private Long parseId(String value, String key) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            stringRedisTemplate.opsForZSet().remove(key, value);
            log.warn("[HotRank] 删除非法排行榜成员 key={}, value={}", key, value);
            return null;
        }
    }

    private HotActivityVO buildHotActivity(RankItem item, Map<Long, HotActivityVO> activityMap) {
        HotActivityVO activity = activityMap.get(item.getId());
        if (activity == null) {
            return null;
        }
        activity.setHotScore(item.getScore());
        return activity;
    }

    private HotVenueVO buildHotVenue(RankItem item, Map<Long, HotVenueVO> venueMap) {
        HotVenueVO venue = venueMap.get(item.getId());
        if (venue == null) {
            return null;
        }
        venue.setHotScore(item.getScore());
        return venue;
    }

    @Getter
    private static final class RankItem {

        private final Long id;
        private final Double score;

        private RankItem(Long id, Double score) {
            this.id = id;
            this.score = score;
        }
    }
}
