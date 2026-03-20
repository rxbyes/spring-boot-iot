package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Organization;

import java.util.List;

public interface OrganizationService extends IService<Organization> {

      Organization addOrganization(Organization organization);

      List<Organization> listOrganizations(Long parentId);

      PageResult<Organization> pageOrganizations(String orgName, String orgCode, Integer status, Long pageNum, Long pageSize);

      List<Organization> listOrganizationTree();

      Organization getById(Long id);

      void updateOrganization(Organization organization);

      void deleteOrganization(Long id);
}
