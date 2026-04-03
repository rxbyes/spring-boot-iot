package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.RoleService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

      private final PermissionService permissionService;
      private final UserMapper userMapper;

      public RoleServiceImpl(PermissionService permissionService,
                             UserMapper userMapper) {
            this.permissionService = permissionService;
            this.userMapper = userMapper;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Role addRole(Role role) {
            return addRole(null, role);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public Role addRole(Long currentUserId, Role role) {
            Long tenantId = resolveTenantId(currentUserId, role.getTenantId());
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode())
                    .eq(tenantId != null, Role::getTenantId, tenantId)
                    .eq(Role::getDeleted, 0);
            if (count(queryWrapper) > 0) {
                  throw new BizException("角色编码已存在");
            }

            role.setTenantId(tenantId == null ? 1L : tenantId);
            if (role.getCreateBy() == null) {
                  role.setCreateBy(currentUserId == null ? 1L : currentUserId);
            }
            if (role.getStatus() == null) {
                  role.setStatus(1);
            }

            save(role);
            if (role.getMenuIds() != null) {
                  permissionService.replaceRoleMenus(role.getId(), role.getMenuIds(), role.getCreateBy());
            }
            return getById(currentUserId, role.getId());
      }

      @Override
      public List<Role> listRoles(String roleName, String roleCode, Integer status) {
            return listRoles(null, roleName, roleCode, status);
      }

      @Override
      public List<Role> listRoles(Long currentUserId, String roleName, String roleCode, Integer status) {
            return list(buildRoleQueryWrapper(currentUserId, roleName, roleCode, status));
      }

      @Override
      public PageResult<Role> pageRoles(String roleName, String roleCode, Integer status, Long pageNum, Long pageSize) {
            return pageRoles(null, roleName, roleCode, status, pageNum, pageSize);
      }

      @Override
      public PageResult<Role> pageRoles(Long currentUserId, String roleName, String roleCode, Integer status, Long pageNum, Long pageSize) {
            Page<Role> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<Role> result = page(page, buildRoleQueryWrapper(currentUserId, roleName, roleCode, status));
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public Role getById(Long id) {
            return getById(null, id);
      }

      @Override
      public Role getById(Long currentUserId, Long id) {
            Role role = super.getById(id);
            if (role == null) {
                  return null;
            }
            ensureRoleAccessible(currentUserId, role);
            role.setMenuIds(permissionService.listRoleMenuIds(id));
            return role;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateRole(Role role) {
            updateRole(null, role);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateRole(Long currentUserId, Role role) {
            Role existingRole = super.getById(role.getId());
            if (existingRole == null) {
                  throw new BizException("角色不存在");
            }
            ensureRoleAccessible(currentUserId, existingRole);

            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Role::getRoleCode, role.getRoleCode())
                    .eq(Role::getTenantId, existingRole.getTenantId())
                    .eq(Role::getDeleted, 0)
                    .ne(Role::getId, role.getId());
            if (count(queryWrapper) > 0) {
                  throw new BizException("角色编码已存在");
            }

            role.setTenantId(existingRole.getTenantId());
            if (role.getUpdateBy() == null) {
                  role.setUpdateBy(currentUserId == null
                          ? (existingRole.getUpdateBy() == null ? 1L : existingRole.getUpdateBy())
                          : currentUserId);
            }
            updateById(role);

            if (role.getMenuIds() != null) {
                  permissionService.replaceRoleMenus(role.getId(), role.getMenuIds(), role.getUpdateBy());
            }
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRole(Long id) {
            deleteRole(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteRole(Long currentUserId, Long id) {
            Role role = super.getById(id);
            if (role == null) {
                  throw new BizException("角色不存在");
            }
            ensureRoleAccessible(currentUserId, role);

            if (!removeById(id)) {
                  throw new BizException("角色删除失败");
            }
            permissionService.deleteRoleRelations(id);
      }

      @Override
      public List<Role> listUserRoles(Long userId) {
            return listUserRoles(null, userId);
      }

      @Override
      public List<Role> listUserRoles(Long currentUserId, Long userId) {
            ensureTargetUserAccessible(currentUserId, userId);
            List<Long> roleIds = permissionService.listUserRoleIds(userId);
            if (roleIds.isEmpty()) {
                  return List.of();
            }

            Long tenantId = resolveTenantId(currentUserId, null);
            LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Role::getId, roleIds)
                    .eq(tenantId != null, Role::getTenantId, tenantId)
                    .orderByAsc(Role::getCreateTime)
                    .orderByAsc(Role::getId);
            return list(wrapper);
      }

      private LambdaQueryWrapper<Role> buildRoleQueryWrapper(Long currentUserId, String roleName, String roleCode, Integer status) {
            LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
            Long tenantId = resolveTenantId(currentUserId, null);
            queryWrapper.eq(tenantId != null, Role::getTenantId, tenantId);
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

      private Long resolveTenantId(Long currentUserId, Long fallbackTenantId) {
            if (currentUserId == null) {
                  return fallbackTenantId;
            }
            return permissionService.getDataPermissionContext(currentUserId).tenantId();
      }

      private void ensureRoleAccessible(Long currentUserId, Role role) {
            if (currentUserId == null || role == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(role.getTenantId())) {
                  throw new BizException("角色不存在或无权访问");
            }
      }

      private void ensureTargetUserAccessible(Long currentUserId, Long userId) {
            if (currentUserId == null || userId == null) {
                  return;
            }
            User targetUser = userMapper.selectById(userId);
            if (targetUser == null || Integer.valueOf(1).equals(targetUser.getDeleted())) {
                  throw new BizException("用户不存在或无权访问");
            }

            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(targetUser.getTenantId())) {
                  throw new BizException("用户不存在或无权访问");
            }
            if (context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.SELF) {
                  if (!currentUserId.equals(userId)) {
                        throw new BizException("用户不存在或无权访问");
                  }
                  return;
            }

            Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
            if (targetUser.getOrgId() == null
                    || CollectionUtils.isEmpty(accessibleOrgIds)
                    || !accessibleOrgIds.contains(targetUser.getOrgId())) {
                  throw new BizException("用户不存在或无权访问");
            }
      }
}
