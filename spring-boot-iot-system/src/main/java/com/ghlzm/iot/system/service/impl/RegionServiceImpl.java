package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.mapper.RegionMapper;
import com.ghlzm.iot.system.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Region addRegion(Region region) {
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getTenantId, region.getTenantId())
                    .eq(Region::getRegionCode, region.getRegionCode())
                    .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("区域编码已存在");
            }

            if (region.getParentId() == null) {
                  region.setParentId(0L);
            }
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
            queryWrapper.eq(Region::getDeleted, 0)
                    .eq(Region::getParentId, normalizeParentId(parentId))
                    .orderByAsc(Region::getSortNo)
                    .orderByAsc(Region::getId);
            List<Region> regions = this.list(queryWrapper);
            fillHasChildren(regions);
            return regions;
      }

      @Override
      public PageResult<Region> pageRegions(String regionName, String regionCode, String regionType, Long pageNum, Long pageSize) {
            Page<Region> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Region> result = page(page, buildPageQueryWrapper(regionName, regionCode, regionType));
            List<Region> records = result.getRecords();
            fillHasChildren(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public List<Region> listRegionTree() {
            List<Region> allRegions = this.list(new LambdaQueryWrapper<Region>()
                    .eq(Region::getDeleted, 0)
                    .orderByAsc(Region::getSortNo)
                    .orderByAsc(Region::getId));

            List<Region> tree = new ArrayList<>();
            for (Region region : allRegions) {
                  if (region.getParentId() == null || region.getParentId() == 0) {
                        List<Region> children = findChildren(region, allRegions);
                        region.setChildren(children);
                        region.setHasChildren(!children.isEmpty());
                        tree.add(region);
                  }
            }
            return tree;
      }

      @Override
      public void updateRegion(Region region) {
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
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getParentId, id)
                    .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("存在子区域，无法删除");
            }

            this.removeById(id);
      }

      @Override
      public Region getById(Serializable id) {
            return super.getById(id);
      }

      private LambdaQueryWrapper<Region> buildPageQueryWrapper(String regionName, String regionCode, String regionType) {
            boolean filterMode = StringUtils.hasText(regionName) || StringUtils.hasText(regionCode) || StringUtils.hasText(regionType);
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getDeleted, 0);
            if (!filterMode) {
                  queryWrapper.eq(Region::getParentId, 0L);
            }
            if (StringUtils.hasText(regionName)) {
                  queryWrapper.like(Region::getRegionName, regionName.trim());
            }
            if (StringUtils.hasText(regionCode)) {
                  queryWrapper.like(Region::getRegionCode, regionCode.trim());
            }
            if (StringUtils.hasText(regionType)) {
                  queryWrapper.eq(Region::getRegionType, regionType.trim());
            }
            queryWrapper.orderByAsc(Region::getSortNo).orderByAsc(Region::getId);
            return queryWrapper;
      }

      private void fillHasChildren(List<Region> regions) {
            if (CollectionUtils.isEmpty(regions)) {
                  return;
            }
            Set<Long> ids = regions.stream()
                    .map(Region::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            if (ids.isEmpty()) {
                  regions.forEach(item -> item.setHasChildren(Boolean.FALSE));
                  return;
            }

            List<Region> children = this.list(new LambdaQueryWrapper<Region>()
                    .select(Region::getParentId)
                    .in(Region::getParentId, ids)
                    .eq(Region::getDeleted, 0));
            Set<Long> parentIds = children.stream()
                    .map(Region::getParentId)
                    .filter(parentId -> parentId != null && parentId > 0)
                    .collect(Collectors.toSet());
            regions.forEach(item -> item.setHasChildren(parentIds.contains(item.getId())));
      }

      private Long normalizeParentId(Long parentId) {
            return parentId != null && parentId > 0 ? parentId : 0L;
      }

      private List<Region> findChildren(Region parent, List<Region> allRegions) {
            List<Region> children = new ArrayList<>();
            for (Region region : allRegions) {
                  if (parent.getId().equals(region.getParentId())) {
                        List<Region> grandChildren = findChildren(region, allRegions);
                        region.setChildren(grandChildren);
                        region.setHasChildren(!grandChildren.isEmpty());
                        children.add(region);
                  }
            }
            return children;
      }
}
