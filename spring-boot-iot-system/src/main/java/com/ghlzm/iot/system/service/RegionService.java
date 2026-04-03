package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Region;

import java.util.List;

public interface RegionService extends IService<Region> {

      Region addRegion(Region region);
      Region addRegion(Long currentUserId, Region region);

      List<Region> listRegions(Long parentId);
      List<Region> listRegions(Long currentUserId, Long parentId);

      PageResult<Region> pageRegions(String regionName, String regionCode, String regionType, Long pageNum, Long pageSize);
      PageResult<Region> pageRegions(Long currentUserId,
                                     String regionName,
                                     String regionCode,
                                     String regionType,
                                     Long pageNum,
                                     Long pageSize);

      List<Region> listRegionTree();
      List<Region> listRegionTree(Long currentUserId);

      Region getById(Long currentUserId, Long id);

      void updateRegion(Region region);
      void updateRegion(Long currentUserId, Region region);

      void deleteRegion(Long id);
      void deleteRegion(Long currentUserId, Long id);
}
