package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.Role;

import java.util.List;

/**
 * 角色 Service
 */
public interface RoleService extends IService<Role> {

      /**
       * 添加角色
       */
      Role addRole(Role role);

      /**
       * 查询角色列表
       */
      List<Role> listRoles(String roleName, String roleCode, Integer status);

      /**
       * 根据ID查询角色
       */
      Role getById(Long id);

      /**
       * 更新角色
       */
      void updateRole(Role role);

      /**
       * 删除角色
       */
      void deleteRole(Long id);

      /**
       * 查询用户角色列表
       */
      List<Role> listUserRoles(Long userId);
}
