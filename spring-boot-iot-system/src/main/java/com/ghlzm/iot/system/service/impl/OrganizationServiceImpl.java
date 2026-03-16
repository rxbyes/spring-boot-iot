package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织机构 Service 实现类
 */
@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization>
            implements OrganizationService {

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Organization addOrganization(Organization organization) {
            // 验证组织编码唯一性
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getTenantId, organization.getTenantId())
                        .eq(Organization::getOrgCode, organization.getOrgCode())
                        .eq(Organization::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("组织编码已存在");
            }

            // 设置默认值
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
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getDeleted, 0);
            if (parentId != null && parentId > 0) {
                  queryWrapper.eq(Organization::getParentId, parentId);
            } else {
                  queryWrapper.eq(Organization::getParentId, 0);
            }
            queryWrapper.orderByAsc(Organization::getSortNo);
            return this.list(queryWrapper);
      }

      @Override
      public List<Organization> listOrganizationTree() {
            // 查询所有未删除的组织机构
            List<Organization> allOrganizations = this.list(new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getDeleted, 0)
                        .orderByAsc(Organization::getSortNo));

            // 构建树形结构
            List<Organization> tree = new ArrayList<>();
            for (Organization org : allOrganizations) {
                  if (org.getParentId() == 0 || org.getParentId() == null) {
                        org.setChildren(findChildren(org, allOrganizations));
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
      @Transactional(rollbackFor = Exception.class)
      public void updateOrganization(Organization organization) {
            // 验证组织编码唯一性
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
            // 验证是否存在子组织
            LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Organization::getParentId, id)
                        .eq(Organization::getDeleted, 0);
            if (this.count(queryWrapper) > 0) {
                  throw new BizException("存在子组织，无法删除");
            }

            this.removeById(id);
      }

      /**
       * 递归查找子组织
       */
      private List<Organization> findChildren(Organization parent, List<Organization> all) {
            List<Organization> children = new ArrayList<>();
            for (Organization org : all) {
                  if (parent.getId().equals(org.getParentId())) {
                        org.setChildren(findChildren(org, all));
                        children.add(org);
                  }
            }
            return children;
      }
}
