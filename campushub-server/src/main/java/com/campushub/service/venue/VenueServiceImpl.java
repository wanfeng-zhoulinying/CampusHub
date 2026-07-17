package com.campushub.service.venue;

import com.campushub.constant.HotRankScoreConstant;
import com.campushub.constant.VenueStatusConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.campushub.dto.VenueQueryDTO;
import com.campushub.mapper.VenueMapper;
import com.campushub.service.cache.RedisCacheService;
import com.campushub.service.rank.HotRankService;
import com.campushub.vo.HotVenueVO;
import com.campushub.vo.VenueDetailVO;
import com.campushub.vo.VenueListVO;
import com.campushub.vo.VenueSlotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueMapper venueMapper;
    private final RedisCacheService redisCacheService;
    private final HotRankService hotRankService;

    @Override
    public List<VenueListVO> listVenues(VenueQueryDTO queryDTO) {
        Integer status = queryDTO.getStatus() == null ? VenueStatusConstant.ENABLED : queryDTO.getStatus();
        return venueMapper.listVenues(queryDTO.getCategory(), queryDTO.getKeyword(), status);
    }

    @Override
    public List<HotVenueVO> listHotVenues(Integer limit) {
        return hotRankService.listHotVenues(limit);
    }

    @Override
    public VenueDetailVO getVenueDetail(Long venueId) {
        String cacheKey = buildVenueDetailKey(venueId);
        // 场地详情使用 Cache Aside，命中空值缓存时不再重复查询数据库。
        VenueDetailVO detail = redisCacheService.queryWithPassThrough(
                cacheKey,
                VenueDetailVO.class,
                RedisTtlConstant.VENUE_DETAIL_MINUTES,
                () -> venueMapper.getVenueDetailById(venueId)
        );
        if (detail != null) {
            hotRankService.increaseVenueHeat(venueId, HotRankScoreConstant.VENUE_DETAIL_VIEW, "查看场地详情");
        }
        return detail;
    }

    @Override
    public List<VenueSlotVO> listVenueSlots(Long venueId, LocalDate slotDate) {
        List<VenueSlotVO> slots = venueMapper.listVenueSlots(venueId, slotDate);
        if (!slots.isEmpty()) {
            hotRankService.increaseVenueHeat(venueId, HotRankScoreConstant.VENUE_SLOT_VIEW, "查看场地时间段");
        }
        return slots;
    }

    /**
     * 构建场地详情缓存 key。
     */
    private String buildVenueDetailKey(Long venueId) {
        return RedisKeyConstant.VENUE_DETAIL + venueId;
    }
}
