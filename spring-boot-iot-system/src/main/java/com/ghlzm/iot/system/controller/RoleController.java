package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {

      private final RoleService roleService;
      private final GovernancePermissionGuard permissionGuard;

      public RoleController(RoleService roleService) {
            this(roleService, null);
      }

      @Autowired
      public RoleController(RoleService roleService, GovernancePermissionGuard permissionGuard) {
            this.roleService = roleService;
            this.permissionGuard = permissionGuard;
      }

      @PostMapping("/add")
      public R<Void> addRole(@RequestBody Role role, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "新增角色", GovernancePermissionCodes.ROLE_ADD);
            roleService.addRole(currentUserId, role);
            return R.ok();
      }

      @GetMapping("/list")
      public R<List<Role>> listRoles(@RequestParam(required = false) String roleName,
                                     @RequestParam(required = false) String roleCode,
                                     @RequestParam(required = false) Integer status,
                                     Authentication authentication) {
            return R.ok(roleService.listRoles(requireCurrentUserId(authentication), roleName, roleCode, status));
      }

      @GetMapping("/page")
      public R<PageResult<Role>> pageRoles(@RequestParam(required = false) String roleName,
                                           @RequestParam(required = false) String roleCode,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize,
                                           Authentication authentication) {
            return R.ok(roleService.pageRoles(requireCurrentUserId(authentication), roleName, roleCode, status, pageNum, pageSize));
      }

      @GetMapping("/{id}")
      public R<Role> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(roleService.getById(requireCurrentUserId(authentication), id));
      }

      @PutMapping("/update")
      public R<Void> updateRole(@RequestBody Role role, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "编辑角色", GovernancePermissionCodes.ROLE_UPDATE);
            roleService.updateRole(currentUserId, role);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteRole(@PathVariable Long id, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "删除角色", GovernancePermissionCodes.ROLE_DELETE);
            roleService.deleteRole(currentUserId, id);
            return R.ok();
      }

      @GetMapping("/user/{userId}")
      public R<List<Role>> listUserRoles(@PathVariable Long userId, Authentication authentication) {
            return R.ok(roleService.listUserRoles(requireCurrentUserId(authentication), userId));
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new IllegalStateException("当前请求缺少有效登录上下文");
            }
            return principal.userId();
      }

      private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
            if (permissionGuard != null) {
                  permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
            }
      }
}
