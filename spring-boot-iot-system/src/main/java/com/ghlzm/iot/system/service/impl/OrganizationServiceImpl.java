package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
        implements OrganizationService {

      private final PermissionService permissionService;

      public OrganizationServiceImpl(PermissionService permissionService) {
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Organization addOrganization(Organization organization) {
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getTenantId, organization.getTenantId())
                    .eq(Organization::getOrgCode, organization.getOrgCode())
                    .eq(Organization::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("组织编码已存在");
            }

            if (organization.getParentId() == null) {
                  organization.setParentId(0L);
            }
            if (organization.getSortNo() == null) {
                  organization.setSortNo(0);
            }
            if (organization.getStatus() == null) {
                  organization.setStatus(1);
            }

            this.save(organization);
            return organization;
      }

      @Override
      public List<Organization> listOrganizations(Long parentId) {
            return listOrganizations(null, parentId);
      }

      @Override
      public List<Organization> listOrganizations(Long currentUserId, Long parentId) {
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getDeleted, 0);
            applyOrganizationRootFilter(queryWrapper, currentUserId, parentId);
            queryWrapper.orderByAsc(Organization::getSortNo)
                    .orderByAsc(Organization::getId);
            applyOrganizationScope(queryWrapper, currentUserId, false);
            List<Organization> organizations = this.list(queryWrapper);
            fillHasChildren(organizations);
            return organizations;
      }

      @Override
      public PageResult<Organization> pageOrganizations(String orgName, String orgCode, Integer status, Long pageNum, Long pageSize) {
            return pageOrganizations(null, orgName, orgCode, status, pageNum, pageSize);
      }

      @Override
      public PageResult<Organization> pageOrganizations(Long currentUserId, String orgName, String orgCode, Integer status, Long pageNum, Long pageSize) {
            Page<Organization> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Organization> result = page(page, buildPageQueryWrapper(currentUserId, orgName, orgCode, status));
            List<Organization> records = result.getRecords();
            fillHasChildren(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public List<Organization> listOrganizationTree() {
            return listOrganizationTree(null);
      }

      @Override
      public List<Organization> listOrganizationTree(Long currentUserId) {
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<Organization>()
                    .eq(Organization::getDeleted, 0)
                    .orderByAsc(Organization::getSortNo)
                    .orderByAsc(Organization::getId);
            applyOrganizationScope(queryWrapper, currentUserId, true);
            List<Organization> allOrganizations = this.list(queryWrapper);
            Set<Long> scopedIds = allOrganizations.stream()
                    .map(Organization::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());

            List<Organization> tree = new ArrayList<>();
            for (Organization org : allOrganizations) {
                  if (org.getParentId() == null
                          || org.getParentId() == 0
                          || !scopedIds.contains(org.getParentId())) {
                        List<Organization> children = findChildren(org, allOrganizations);
                        org.setChildren(children);
                        org.setHasChildren(!children.isEmpty());
                        tree.add(org);
                  }
            }
            return tree;
      }

      @Override
      public Organization getById(Long id) {
            return super.getById(id);
      }

      @Override
      public Organization getById(Long currentUserId, Long id) {
            Organization organization = super.getById(id);
            if (organization == null || Integer.valueOf(1).equals(organization.getDeleted())) {
                  return null;
            }
            ensureOrganizationAccessible(currentUserId, organization);
            return organization;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateOrganization(Organization organization) {
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getTenantId, organization.getTenantId())
                    .eq(Organization::getOrgCode, organization.getOrgCode())
                    .ne(Organization::getId, organization.getId())
                    .eq(Organization::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("组织编码已存在");
            }

            this.updateById(organization);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteOrganization(Long id) {
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getParentId, id)
                    .eq(Organization::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("存在子组织，无法删除");
            }

            this.removeById(id);
      }

      private LambdaQueryWrapper<Organization> buildPageQueryWrapper(Long currentUserId, String orgName, String orgCode, Integer status) {
            boolean filterMode = StringUtils.hasText(orgName) || StringUtils.hasText(orgCode) || status != null;
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getDeleted, 0);
            if (!filterMode) {
                  Long scopedRootOrgId = resolveScopedRootOrgId(currentUserId);
                  if (scopedRootOrgId != null) {
                        queryWrapper.eq(Organization::getId, scopedRootOrgId);
                  } else {
                        queryWrapper.eq(Organization::getParentId, 0L);
                  }
            }
            if (StringUtils.hasText(orgName)) {
                  queryWrapper.like(Organization::getOrgName, orgName.trim());
            }
            if (StringUtils.hasText(orgCode)) {
                  queryWrapper.like(Organization::getOrgCode, orgCode.trim());
            }
            if (status != null) {
                  queryWrapper.eq(Organization::getStatus, status);
            }
            applyOrganizationScope(queryWrapper, currentUserId, false);
            queryWrapper.orderByAsc(Organization::getSortNo).orderByAsc(Organization::getId);
            return queryWrapper;
      }

      private void applyOrganizationRootFilter(LambdaQueryWrapper<Organization> queryWrapper, Long currentUserId, Long parentId) {
            if (parentId != null && parentId > 0) {
                  queryWrapper.eq(Organization::getParentId, parentId);
                  return;
            }
            Long scopedRootOrgId = resolveScopedRootOrgId(currentUserId);
            if (scopedRootOrgId != null) {
                  queryWrapper.eq(Organization::getId, scopedRootOrgId);
                  return;
            }
            queryWrapper.eq(Organization::getParentId, 0L);
      }

      private void applyOrganizationScope(LambdaQueryWrapper<Organization> queryWrapper, Long currentUserId, boolean treeMode) {
            if (currentUserId == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            queryWrapper.eq(context.tenantId() != null, Organization::getTenantId, context.tenantId());
            if (context.superAdmin()) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.ORG || context.dataScopeType() == DataScopeType.SELF) {
                  queryWrapper.eq(Organization::getId, context.orgId());
                  return;
            }
            Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
            if (CollectionUtils.isEmpty(accessibleOrgIds)) {
                  queryWrapper.eq(Organization::getId, -1L);
                  return;
            }
            if (treeMode) {
                  queryWrapper.in(Organization::getId, accessibleOrgIds);
                  return;
            }
            queryWrapper.in(Organization::getId, accessibleOrgIds);
      }

      private Long resolveScopedRootOrgId(Long currentUserId) {
            if (currentUserId == null) {
                  return null;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin() || context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return null;
            }
            return context.orgId();
      }

      private void ensureOrganizationAccessible(Long currentUserId, Organization organization) {
            if (currentUserId == null || organization == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(organization.getTenantId())) {
                  throw new BizException("组织不存在或无权访问");
            }
            if (context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (!permissionService.listAccessibleOrganizationIds(currentUserId).contains(organization.getId())) {
                  throw new BizException("组织不存在或无权访问");
            }
      }

      private void fillHasChildren(List<Organization> organizations) {
            if (CollectionUtils.isEmpty(organizations)) {
                  return;
            }
            Set<Long> ids = organizations.stream()
                    .map(Organization::getId)
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toSet());
            if (ids.isEmpty()) {
                  organizations.forEach(item -> item.setHasChildren(Boolean.FALSE));
                  return;
            }

            List<Organization> children = this.list(new LambdaQueryWrapper<Organization>()
                    .select(Organization::getParentId)
                    .in(Organization::getParentId, ids)
                    .eq(Organization::getDeleted, 0));
            Set<Long> parentIds = children.stream()
                    .map(Organization::getParentId)
                    .filter(parentId -> parentId != null && parentId > 0)
                    .collect(Collectors.toSet());
            organizations.forEach(item -> item.setHasChildren(parentIds.contains(item.getId())));
      }

      private Long normalizeParentId(Long parentId) {
            return parentId != null && parentId > 0 ? parentId : 0L;
      }

      private List<Organization> findChildren(Organization parent, List<Organization> allOrganizations) {
            List<Organization> children = new ArrayList<>();
            for (Organization organization : allOrganizations) {
                  if (parent.getId().equals(organization.getParentId())) {
                        List<Organization> grandChildren = findChildren(organization, allOrganizations);
                        organization.setChildren(grandChildren);
                        organization.setHasChildren(!grandChildren.isEmpty());
                        children.add(organization);
                  }
            }
            return children;
      }
}
