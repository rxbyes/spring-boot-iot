package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

      private final PermissionService permissionService;

      public RoleServiceImpl(PermissionService permissionService) {
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Role addRole(Role role) {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode());
            if (count(queryWrapper) > 0) {
                  throw new BizException("角色编码已存在");
            }

            if (role.getTenantId() == null) {
                  role.setTenantId(1L);
            }
            if (role.getCreateBy() == null) {
                  role.setCreateBy(1L);
            }
            if (role.getStatus() == null) {
                  role.setStatus(1);
            }

            save(role);
            if (role.getMenuIds() != null) {
                  permissionService.replaceRoleMenus(role.getId(), role.getMenuIds(), role.getCreateBy());
            }
            return getById(role.getId());
      }

      @Override
      public List<Role> listRoles(String roleName, String roleCode, Integer status) {
            return list(buildRoleQueryWrapper(roleName, roleCode, status));
      }

      @Override
      public PageResult<Role> pageRoles(String roleName, String roleCode, Integer status, Long pageNum, Long pageSize) {
            Page<Role> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Role> result = page(page, buildRoleQueryWrapper(roleName, roleCode, status));
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public Role getById(Long id) {
            Role role = super.getById(id);
            if (role == null) {
                  return null;
            }
            role.setMenuIds(permissionService.listRoleMenuIds(id));
            return role;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateRole(Role role) {
            Role existingRole = super.getById(role.getId());
            if (existingRole == null) {
                  throw new BizException("角色不存在");
            }

            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode())
                    .ne(Role::getId, role.getId());
            if (count(queryWrapper) > 0) {
                  throw new BizException("角色编码已存在");
            }

            if (role.getTenantId() == null) {
                  role.setTenantId(existingRole.getTenantId());
            }
            if (role.getUpdateBy() == null) {
                  role.setUpdateBy(existingRole.getUpdateBy() == null ? 1L : existingRole.getUpdateBy());
            }
            updateById(role);

            if (role.getMenuIds() != null) {
                  permissionService.replaceRoleMenus(role.getId(), role.getMenuIds(), role.getUpdateBy());
            }
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRole(Long id) {
            Role role = super.getById(id);
            if (role == null) {
                  throw new BizException("角色不存在");
            }

            if (!removeById(id)) {
                  throw new BizException("角色删除失败");
            }
            permissionService.deleteRoleRelations(id);
      }

      @Override
      public List<Role> listUserRoles(Long userId) {
            List<Long> roleIds = permissionService.listUserRoleIds(userId);
            if (roleIds.isEmpty()) {
                  return List.of();
            }

            LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Role::getId, roleIds)
                    .orderByAsc(Role::getCreateTime)
                    .orderByAsc(Role::getId);
            return list(wrapper);
      }

      private LambdaQueryWrapper<Role> buildRoleQueryWrapper(String roleName, String roleCode, Integer status) {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(roleName)) {
                  queryWrapper.like(Role::getRoleName, roleName.trim());
            }
            if (StringUtils.hasText(roleCode)) {
                  queryWrapper.eq(Role::getRoleCode, roleCode.trim());
            }
            if (status != null) {
                  queryWrapper.eq(Role::getStatus, status);
            }
            queryWrapper.orderByAsc(Role::getCreateTime).orderByAsc(Role::getId);
            return queryWrapper;
      }
}
