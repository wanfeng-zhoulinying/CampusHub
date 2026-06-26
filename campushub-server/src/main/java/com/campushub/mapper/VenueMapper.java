package com.campushub.mapper;

import com.campushub.entity.Venue;
import com.campushub.vo.AdminVenueListVO;
import com.campushub.vo.VenueDetailVO;
import com.campushub.vo.VenueListVO;
import com.campushub.vo.VenueSlotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface VenueMapper {

    List<VenueListVO> listVenues(@Param("category") String category,
                                 @Param("keyword") String keyword,
                                 @Param("status") Integer status);

    VenueDetailVO getVenueDetailById(@Param("id") Long id);

    List<VenueSlotVO> listVenueSlots(@Param("venueId") Long venueId,
                                     @Param("slotDate") LocalDate slotDate);

    List<AdminVenueListVO> listAdminVenues(@Param("name") String name,
                                           @Param("category") String category,
                                           @Param("status") Integer status);

    Venue getVenueById(@Param("id") Long id);

    int saveVenue(Venue venue);

    int updateVenue(Venue venue);

    int updateVenueStatus(@Param("venueId") Long venueId, @Param("status") Integer status);
}
