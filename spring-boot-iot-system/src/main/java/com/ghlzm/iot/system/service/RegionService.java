package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Region;

import java.util.List;

public interface RegionService extends IService<Region> {

      Region addRegion(Region region);

      List<Region> listRegions(Long parentId);

      PageResult<Region> pageRegions(String regionName, String regionCode, String regionType, Long pageNum, Long pageSize);

      List<Region> listRegionTree();

      void updateRegion(Region region);

      void deleteRegion(Long id);
}
