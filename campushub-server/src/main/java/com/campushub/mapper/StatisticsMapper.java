package com.campushub.mapper;

import com.campushub.vo.AdminBookingStatusStatVO;
import com.campushub.vo.AdminDashboardOverviewVO;
import com.campushub.vo.AdminHotActivityVO;
import com.campushub.vo.AdminHotVenueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatisticsMapper {

    AdminDashboardOverviewVO getDashboardOverview();

    List<AdminBookingStatusStatVO> listBookingStatusStats();

    List<AdminHotVenueVO> listHotVenues(@Param("limit") Integer limit);

    List<AdminHotActivityVO> listHotActivities(@Param("limit") Integer limit);
}
