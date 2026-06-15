package com.campushub.mapper;

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
}
