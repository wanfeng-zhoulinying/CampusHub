package com.campushub.service.venue;

import com.campushub.constant.VenueStatusConstant;
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
        Integer status = queryDTO.getStatus() == null ? VenueStatusConstant.ENABLED : queryDTO.getStatus();
        return venueMapper.listVenues(queryDTO.getCategory(), queryDTO.getKeyword(), status);
    }

    @Override
    public VenueDetailVO getVenueDetail(Long venueId) {
        return venueMapper.getVenueDetailById(venueId);
    }

    @Override
    public List<VenueSlotVO> listVenueSlots(Long venueId, LocalDate slotDate) {
        return venueMapper.listVenueSlots(venueId, slotDate);
    }
}
