package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Region;
import com.ghlzm.iot.system.mapper.RegionMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.RegionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
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

      private final PermissionService permissionService;

      public RegionServiceImpl(PermissionService permissionService) {
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Region addRegion(Region region) {
            return addRegion(null, region);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Region addRegion(Long currentUserId, Region region) {
            Long tenantId = resolveTenantId(currentUserId, region.getTenantId());
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getTenantId, tenantId)
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
            region.setTenantId(tenantId == null ? 1L : tenantId);
            validateParentRegion(currentUserId, region.getTenantId(), region.getParentId(), null);

            this.save(region);
            return region;
      }

      @Override
      public List<Region> listRegions(Long parentId) {
            return listRegions(null, parentId);
      }

      @Override
      public List<Region> listRegions(Long currentUserId, Long parentId) {
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(Region::getDeleted, 0)
                    .eq(tenantId != null, Region::getTenantId, tenantId)
                    .eq(Region::getParentId, normalizeParentId(parentId))
                    .orderByAsc(Region::getSortNo)
                    .orderByAsc(Region::getId);
            List<Region> regions = this.list(queryWrapper);
            fillHasChildren(regions);
            return regions;
      }

      @Override
      public PageResult<Region> pageRegions(String regionName, String regionCode, String regionType, Long pageNum, Long pageSize) {
            return pageRegions(null, regionName, regionCode, regionType, pageNum, pageSize);
      }

      @Override
      public PageResult<Region> pageRegions(Long currentUserId,
                                            String regionName,
                                            String regionCode,
                                            String regionType,
                                            Long pageNum,
                                            Long pageSize) {
            Page<Region> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Region> result = page(page, buildPageQueryWrapper(currentUserId, regionName, regionCode, regionType));
            List<Region> records = result.getRecords();
            fillHasChildren(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public List<Region> listRegionTree() {
            return listRegionTree(null);
      }

      @Override
      public List<Region> listRegionTree(Long currentUserId) {
            Long tenantId = resolveTenantId(currentUserId, null);
            List<Region> allRegions = this.list(new LambdaQueryWrapper<Region>()
                    .eq(tenantId != null, Region::getTenantId, tenantId)
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
      public Region getById(Long currentUserId, Long id) {
            Region region = super.getById(id);
            if (region == null) {
                  return null;
            }
            ensureRegionAccessible(currentUserId, region);
            return region;
      }

      @Override
      public void updateRegion(Region region) {
            updateRegion(null, region);
      }

      @Override
      public void updateRegion(Long currentUserId, Region region) {
            Region existing = super.getById(region.getId());
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("区域不存在");
            }
            ensureRegionAccessible(currentUserId, existing);
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getTenantId, existing.getTenantId())
                    .eq(Region::getRegionCode, region.getRegionCode())
                    .ne(Region::getId, region.getId())
                    .eq(Region::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("区域编码已存在");
            }

            validateParentRegion(currentUserId, existing.getTenantId(), region.getParentId(), region.getId());
            region.setTenantId(existing.getTenantId());
            this.updateById(region);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRegion(Long id) {
            deleteRegion(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRegion(Long currentUserId, Long id) {
            Region existing = super.getById(id);
            if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
                  throw new BizException("区域不存在");
            }
            ensureRegionAccessible(currentUserId, existing);
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Region::getParentId, id)
                    .eq(Region::getTenantId, existing.getTenantId())
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

      private LambdaQueryWrapper<Region> buildPageQueryWrapper(Long currentUserId,
                                                               String regionName,
                                                               String regionCode,
                                                               String regionType) {
            boolean filterMode = StringUtils.hasText(regionName) || StringUtils.hasText(regionCode) || StringUtils.hasText(regionType);
            LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(tenantId != null, Region::getTenantId, tenantId);
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

            Set<Long> tenantIds = regions.stream()
                    .map(Region::getTenantId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            List<Region> children = this.list(new LambdaQueryWrapper<Region>()
                    .select(Region::getParentId)
                    .in(Region::getParentId, ids)
                    .in(!tenantIds.isEmpty(), Region::getTenantId, tenantIds)
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

      private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
            if (currentUserId == null) {
                  return fallbackTenantId;
            }
            return permissionService.getDataPermissionContext(currentUserId).tenantId();
      }

      private void ensureRegionAccessible(Long currentUserId, Region region) {
            if (currentUserId == null || region == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(region.getTenantId())) {
                  throw new BizException("区域不存在或无权访问");
            }
      }

      private void validateParentRegion(Long currentUserId, Long tenantId, Long parentId, Long currentId) {
            if (parentId == null || parentId <= 0) {
                  return;
            }
            if (currentId != null && parentId.equals(currentId)) {
                  throw new BizException("父级区域不能是自身");
            }
            Region parent = super.getById(parentId);
            if (parent == null || Integer.valueOf(1).equals(parent.getDeleted())) {
                  throw new BizException("父级区域不存在");
            }
            if (tenantId != null && parent.getTenantId() != null && !tenantId.equals(parent.getTenantId())) {
                  throw new BizException("父级区域不存在或无权访问");
            }
            ensureRegionAccessible(currentUserId, parent);
      }
}
