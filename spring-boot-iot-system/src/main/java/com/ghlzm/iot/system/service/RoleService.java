package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

      Role addRole(Role role);

      Role addRole(Long currentUserId, Role role);

      List<Role> listRoles(String roleName, String roleCode, Integer status);

      List<Role> listRoles(Long currentUserId, String roleName, String roleCode, Integer status);

      PageResult<Role> pageRoles(String roleName, String roleCode, Integer status, Long pageNum, Long pageSize);

      PageResult<Role> pageRoles(Long currentUserId, String roleName, String roleCode, Integer status, Long pageNum, Long pageSize);

      Role getById(Long id);

      Role getById(Long currentUserId, Long id);

      void updateRole(Role role);

      void updateRole(Long currentUserId, Role role);

      void deleteRole(Long id);

      void deleteRole(Long currentUserId, Long id);

      List<Role> listUserRoles(Long userId);

      List<Role> listUserRoles(Long currentUserId, Long userId);
}
