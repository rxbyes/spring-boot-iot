package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色 Service 实现类
 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

      @Autowired
      private RoleMapper roleMapper;

      @Override
      public Role addRole(Role role) {
            // 检查角色编码是否已存在
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode());
            long count = count(queryWrapper);
            if (count > 0) {
                  throw new BizException("角色编码已存在");
            }

            // 设置默认值
            if (role.getCreateBy() == null) {
                  role.setCreateBy(1L);
            }

            save(role);
            return role;
      }

      @Override
      public List<Role> listRoles(String roleName, String roleCode, Integer status) {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            if (roleName != null && !roleName.isEmpty()) {
                  queryWrapper.like(Role::getRoleName, roleName);
            }
            if (roleCode != null && !roleCode.isEmpty()) {
                  queryWrapper.eq(Role::getRoleCode, roleCode);
            }
            if (status != null) {
                  queryWrapper.eq(Role::getStatus, status);
            }
            queryWrapper.eq(Role::getDeleted, 0);
            queryWrapper.orderByAsc(Role::getCreateTime);
            return list(queryWrapper);
      }

      @Override
      public Role getById(Long id) {
            return super.getById(id);
      }

      @Override
      public void updateRole(Role role) {
            // 检查角色是否存在
            Role existingRole = getById(role.getId());
            if (existingRole == null) {
                  throw new BizException("角色不存在");
            }

            // 检查角色编码是否已存在
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode());
            queryWrapper.ne(Role::getId, role.getId());
            long count = count(queryWrapper);
            if (count > 0) {
                  throw new BizException("角色编码已存在");
            }

            updateById(role);
      }

      @Override
      public void deleteRole(Long id) {
            // 检查角色是否存在
            Role role = getById(id);
            if (role == null) {
                  throw new BizException("角色不存在");
            }

            // 逻辑删除
            role.setDeleted(1);
            updateById(role);
      }

      @Override
      public List<Role> listUserRoles(Long userId) {
            // TODO: 实现查询用户角色列表
            return null;
      }
}
