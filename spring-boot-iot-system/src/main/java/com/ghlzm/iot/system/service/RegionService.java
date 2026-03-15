package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.Region;

import java.util.List;

/**
 * 区域管理 Service
 */
public interface RegionService extends IService<Region> {

      /**
       * 添加区域
       */
      Region addRegion(Region region);

      /**
       * 查询区域列表
       */
      List<Region> listRegions(Long parentId);

      /**
       * 查询区域树
       */
      List<Region> listRegionTree();

      /**
       * 更新区域
       */
      void updateRegion(Region region);

      /**
       * 删除区域
       */
      void deleteRegion(Long id);
}
