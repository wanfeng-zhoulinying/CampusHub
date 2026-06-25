package com.campushub.service.venue;

import com.campushub.dto.VenueQueryDTO;
import com.campushub.vo.VenueDetailVO;
import com.campushub.vo.VenueListVO;
import com.campushub.vo.VenueSlotVO;

import java.time.LocalDate;
import java.util.List;

public interface VenueService {

    List<VenueListVO> listVenues(VenueQueryDTO queryDTO);

    VenueDetailVO getVenueDetail(Long venueId);

    List<VenueSlotVO> listVenueSlots(Long venueId, LocalDate slotDate);
}
