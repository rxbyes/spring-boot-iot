package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Organization;

import java.util.List;

public interface OrganizationService extends IService<Organization> {

      Organization addOrganization(Organization organization);

      List<Organization> listOrganizations(Long parentId);

      List<Organization> listOrganizations(Long currentUserId, Long parentId);

      PageResult<Organization> pageOrganizations(String orgName, String orgCode, Integer status, Long pageNum, Long pageSize);

      PageResult<Organization> pageOrganizations(Long currentUserId, String orgName, String orgCode, Integer status, Long pageNum, Long pageSize);

      List<Organization> listOrganizationTree();

      List<Organization> listOrganizationTree(Long currentUserId);

      List<Organization> listWritableOrganizationTree(Long currentUserId);

      Organization getById(Long id);

      Organization getById(Long currentUserId, Long id);

      void updateOrganization(Organization organization);

      void deleteOrganization(Long id);
}
