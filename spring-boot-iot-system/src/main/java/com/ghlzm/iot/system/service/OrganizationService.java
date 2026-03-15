package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.Organization;

import java.util.List;

/**
 * 组织机构 Service
 */
public interface OrganizationService extends IService<Organization> {

      /**
       * 添加组织机构
       */
      Organization addOrganization(Organization organization);

      /**
       * 查询组织机构列表
       */
      List<Organization> listOrganizations(Long parentId);

      /**
       * 查询组织机构树
       */
      List<Organization> listOrganizationTree();

      /**
       * 根据ID查询组织机构
       */
      Organization getById(Long id);

      /**
       * 更新组织机构
       */
      void updateOrganization(Organization organization);

      /**
       * 删除组织机构
       */
      void deleteOrganization(Long id);
}
