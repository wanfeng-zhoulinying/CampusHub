package com.campushub.service.venue;

import com.campushub.dto.VenueQueryDTO;
import com.campushub.mapper.VenueMapper;
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

    @Override
    public List<VenueListVO> listVenues(VenueQueryDTO queryDTO) {
        // 列表查询默认只返回开放状态的场地，避免前端把关闭场地直接展示出来。
        Integer status = queryDTO.getStatus() == null ? 1 : queryDTO.getStatus();
        return venueMapper.listVenues(queryDTO.getCategory(), queryDTO.getKeyword(), status);
    }

    @Override
    public VenueDetailVO getVenueDetail(Long id) {
        // 详情查询当前只做单表读取，后续如果接入收藏、评分等信息，可在这里继续组装返回值。
        return venueMapper.getVenueDetailById(id);
    }

    @Override
    public List<VenueSlotVO> listVenueSlots(Long venueId, LocalDate slotDate) {
        // 时间段查询按场地ID和日期精确过滤，供预约页面展示当天可选时段。
        return venueMapper.listVenueSlots(venueId, slotDate);
    }
}
