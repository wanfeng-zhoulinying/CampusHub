package com.campushub.service.admin;

import com.campushub.dto.AdminVenueQueryDTO;
import com.campushub.dto.AdminVenueSaveDTO;
import com.campushub.vo.AdminVenueListVO;

import java.util.List;

public interface AdminVenueService {

    List<AdminVenueListVO> listVenues(AdminVenueQueryDTO queryDTO);

    Long createVenue(AdminVenueSaveDTO saveDTO);

    void updateVenue(Long venueId, AdminVenueSaveDTO saveDTO);

    void updateVenueStatus(Long venueId, Integer status);
}
