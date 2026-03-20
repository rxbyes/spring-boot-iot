package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

      Role addRole(Role role);

      List<Role> listRoles(String roleName, String roleCode, Integer status);

      PageResult<Role> pageRoles(String roleName, String roleCode, Integer status, Long pageNum, Long pageSize);

      Role getById(Long id);

      void updateRole(Role role);

      void deleteRole(Long id);

      List<Role> listUserRoles(Long userId);
}
