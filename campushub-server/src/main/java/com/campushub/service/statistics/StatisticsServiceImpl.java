package com.campushub.service.statistics;

import com.campushub.exception.BusinessException;
import com.campushub.mapper.StatisticsMapper;
import com.campushub.vo.AdminBookingStatusStatVO;
import com.campushub.vo.AdminDashboardOverviewVO;
import com.campushub.vo.AdminHotActivityVO;
import com.campushub.vo.AdminHotVenueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final int DEFAULT_TOP_LIMIT = 5;

    private final StatisticsMapper statisticsMapper;

    /**
     * 查询后台看板总览数据。
     */
    @Override
    public AdminDashboardOverviewVO getDashboardOverview() {
        return statisticsMapper.getDashboardOverview();
    }

    /**
     * 查询预约状态分布。
     */
    @Override
    public List<AdminBookingStatusStatVO> listBookingStatusStats() {
        return statisticsMapper.listBookingStatusStats();
    }

    /**
     * 查询热门场地排行。
     */
    @Override
    public List<AdminHotVenueVO> listHotVenues(Integer limit) {
        return statisticsMapper.listHotVenues(normalizeLimit(limit));
    }

    /**
     * 查询热门活动排行。
     */
    @Override
    public List<AdminHotActivityVO> listHotActivities(Integer limit) {
        return statisticsMapper.listHotActivities(normalizeLimit(limit));
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_LIMIT;
        }
        if (limit <= 0) {
            throw new BusinessException("排行数量必须大于0");
        }
        return limit;
    }
}
