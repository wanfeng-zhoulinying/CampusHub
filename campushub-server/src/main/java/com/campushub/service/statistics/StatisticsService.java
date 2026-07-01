package com.campushub.service.statistics;

import com.campushub.vo.AdminBookingStatusStatVO;
import com.campushub.vo.AdminDashboardOverviewVO;
import com.campushub.vo.AdminHotActivityVO;
import com.campushub.vo.AdminHotVenueVO;

import java.util.List;

public interface StatisticsService {

    AdminDashboardOverviewVO getDashboardOverview();

    List<AdminBookingStatusStatVO> listBookingStatusStats();

    List<AdminHotVenueVO> listHotVenues(Integer limit);

    List<AdminHotActivityVO> listHotActivities(Integer limit);
}
