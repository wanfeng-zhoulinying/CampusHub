package com.campushub.service.admin;

import com.campushub.constant.DeleteStatusConstant;
import com.campushub.constant.VenueStatusConstant;
import com.campushub.dto.AdminVenueQueryDTO;
import com.campushub.dto.AdminVenueSaveDTO;
import com.campushub.entity.Venue;
import com.campushub.exception.BusinessException;
import com.campushub.mapper.VenueMapper;
import com.campushub.vo.AdminVenueListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminVenueServiceImpl implements AdminVenueService {

    private final VenueMapper venueMapper;

    /**
     * 后台场地列表。
     * 支持按名称、分类、状态筛选，方便管理端做维护操作。
     */
    @Override
    public List<AdminVenueListVO> listVenues(AdminVenueQueryDTO queryDTO) {
        return venueMapper.listAdminVenues(queryDTO.getName(), queryDTO.getCategory(), queryDTO.getStatus());
    }

    /**
     * 新增场地。
     * 后台创建场地时，默认以未删除记录写入数据库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVenue(AdminVenueSaveDTO saveDTO) {
        validateVenueSaveDTO(saveDTO);

        Venue venue = new Venue();
        venue.setName(saveDTO.getName());
        venue.setCategory(saveDTO.getCategory());
        venue.setLocation(saveDTO.getLocation());
        venue.setCapacity(saveDTO.getCapacity());
        venue.setCoverUrl(saveDTO.getCoverUrl());
        venue.setDescription(saveDTO.getDescription());
        venue.setLongitude(saveDTO.getLongitude());
        venue.setLatitude(saveDTO.getLatitude());
        venue.setStatus(saveDTO.getStatus() == null ? VenueStatusConstant.ENABLED : saveDTO.getStatus());
        venue.setIsDeleted(DeleteStatusConstant.NOT_DELETED);

        venueMapper.saveVenue(venue);
        return venue.getId();
    }

    /**
     * 修改场地。
     * 只允许修改存在且未删除的场地记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenue(Long venueId, AdminVenueSaveDTO saveDTO) {
        validateVenueSaveDTO(saveDTO);

        Venue existedVenue = venueMapper.getVenueById(venueId);
        if (existedVenue == null || DeleteStatusConstant.DELETED.equals(existedVenue.getIsDeleted())) {
            throw new BusinessException("场地不存在");
        }

        Venue venue = new Venue();
        venue.setId(venueId);
        venue.setName(saveDTO.getName());
        venue.setCategory(saveDTO.getCategory());
        venue.setLocation(saveDTO.getLocation());
        venue.setCapacity(saveDTO.getCapacity());
        venue.setCoverUrl(saveDTO.getCoverUrl());
        venue.setDescription(saveDTO.getDescription());
        venue.setLongitude(saveDTO.getLongitude());
        venue.setLatitude(saveDTO.getLatitude());
        venue.setStatus(saveDTO.getStatus() == null ? existedVenue.getStatus() : saveDTO.getStatus());

        int affectedRows = venueMapper.updateVenue(venue);
        if (affectedRows == 0) {
            throw new BusinessException("场地修改失败");
        }
    }

    /**
     * 修改场地状态。
     * 管理端可直接做启用和关闭操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenueStatus(Long venueId, Integer status) {
        if (status == null) {
            throw new BusinessException("场地状态不能为空");
        }

        int affectedRows = venueMapper.updateVenueStatus(venueId, status);
        if (affectedRows == 0) {
            throw new BusinessException("场地状态修改失败");
        }
    }

    private void validateVenueSaveDTO(AdminVenueSaveDTO saveDTO) {
        if (saveDTO.getName() == null || saveDTO.getName().isBlank()) {
            throw new BusinessException("场地名称不能为空");
        }
        if (saveDTO.getCategory() == null || saveDTO.getCategory().isBlank()) {
            throw new BusinessException("场地分类不能为空");
        }
        if (saveDTO.getLocation() == null || saveDTO.getLocation().isBlank()) {
            throw new BusinessException("场地位置不能为空");
        }
        if (saveDTO.getCapacity() == null || saveDTO.getCapacity() <= 0) {
            throw new BusinessException("场地容量必须大于0");
        }
    }
}
