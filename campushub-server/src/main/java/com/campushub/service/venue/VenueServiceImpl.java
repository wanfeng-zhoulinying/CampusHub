package com.campushub.service.venue;

import com.campushub.constant.VenueStatusConstant;
import com.campushub.constant.RedisKeyConstant;
import com.campushub.constant.RedisTtlConstant;
import com.campushub.dto.VenueQueryDTO;
import com.campushub.mapper.VenueMapper;
import com.campushub.service.cache.RedisCacheService;
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

    @Override
    public List<VenueListVO> listVenues(VenueQueryDTO queryDTO) {
        Integer status = queryDTO.getStatus() == null ? VenueStatusConstant.ENABLED : queryDTO.getStatus();
        return venueMapper.listVenues(queryDTO.getCategory(), queryDTO.getKeyword(), status);
    }

    @Override
    public VenueDetailVO getVenueDetail(Long venueId) {
        String cacheKey = buildVenueDetailKey(venueId);
        // 场地详情使用 Cache Aside，命中空值缓存时不再重复查询数据库。
        return redisCacheService.queryWithPassThrough(
                cacheKey,
                VenueDetailVO.class,
                RedisTtlConstant.VENUE_DETAIL_MINUTES,
                () -> venueMapper.getVenueDetailById(venueId)
        );
    }

    @Override
    public List<VenueSlotVO> listVenueSlots(Long venueId, LocalDate slotDate) {
        return venueMapper.listVenueSlots(venueId, slotDate);
    }

    /**
     * 构建场地详情缓存 key。
     */
    private String buildVenueDetailKey(Long venueId) {
        return RedisKeyConstant.VENUE_DETAIL + venueId;
    }
}
