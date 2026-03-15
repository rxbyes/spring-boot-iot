package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.mapper.RegionMapper;
import com.ghlzm.iot.system.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 区域管理 Service 实现类
 */
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region>
            implements RegionService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Region addRegion(Region region) {
            // 验证区域编码唯一性
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getTenantId, region.getTenantId())
                        .eq(Region::getRegionCode, region.getRegionCode())
                        .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("区域编码已存在");
            }

            // 设置默认值
            if (region.getSortNo() == null) {
                  region.setSortNo(0);
            }
            if (region.getStatus() == null) {
                  region.setStatus(1);
            }

            this.save(region);
            return region;
      }

      @Override
      public List<Region> listRegions(Long parentId) {
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getDeleted, 0);
            if (parentId != null && parentId > 0) {
                  queryWrapper.eq(Region::getParentId, parentId);
            } else {
                  queryWrapper.eq(Region::getParentId, 0);
            }
            queryWrapper.orderByAsc(Region::getSortNo);
            return this.list(queryWrapper);
      }

      @Override
      public List<Region> listRegionTree() {
            // 查询所有未删除的区域
            List<Region> allRegions = this.list(new LambdaQueryWrapper<Region>()
                        .eq(Region::getDeleted, 0)
                        .orderByAsc(Region::getSortNo));

            // 构建树形结构
            List<Region> tree = new ArrayList<>();
            for (Region region : allRegions) {
                  if (region.getParentId() == 0 || region.getParentId() == null) {
                        region.setChildren(findChildren(region, allRegions));
                        tree.add(region);
                  }
            }
            return tree;
      }

      public Region getById(Long id) {
            return this.getById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateRegion(Region region) {
            // 验证区域编码唯一性
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getTenantId, region.getTenantId())
                        .eq(Region::getRegionCode, region.getRegionCode())
                        .ne(Region::getId, region.getId())
                        .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("区域编码已存在");
            }

            this.updateById(region);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRegion(Long id) {
            // 验证是否存在子区域
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getParentId, id)
                        .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("存在子区域，无法删除");
            }

            this.removeById(id);
      }

      /**
       * 递归查找子区域
       */
      private List<Region> findChildren(Region parent, List<Region> all) {
            List<Region> children = new ArrayList<>();
            for (Region region : all) {
                  if (parent.getId().equals(region.getParentId())) {
                        region.setChildren(findChildren(region, all));
                        children.add(region);
                  }
            }
            return children;
      }
}
