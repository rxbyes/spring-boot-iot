package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
public class RoleController {

      @Autowired
      private RoleService roleService;

      /**
       * 添加角色
       */
      @PostMapping("/add")
      public R<Void> addRole(@RequestBody Role role) {
            roleService.addRole(role);
            return R.ok();
      }

      /**
       * 查询角色列表
       */
      @GetMapping("/list")
      public R<List<Role>> listRoles(
                  @RequestParam(required = false) String roleName,
                  @RequestParam(required = false) String roleCode,
                  @RequestParam(required = false) Integer status) {
            List<Role> roles = roleService.listRoles(roleName, roleCode, status);
            return R.ok(roles);
      }

      /**
       * 根据ID查询角色
       */
      @GetMapping("/{id}")
      public R<Role> getById(@PathVariable Long id) {
            Role role = roleService.getById(id);
            return R.ok(role);
      }

      /**
       * 更新角色
       */
      @PutMapping("/update")
      public R<Void> updateRole(@RequestBody Role role) {
            roleService.updateRole(role);
            return R.ok();
      }

      /**
       * 删除角色
       */
      @DeleteMapping("/{id}")
      public R<Void> deleteRole(@PathVariable Long id) {
            roleService.deleteRole(id);
            return R.ok();
      }

      /**
       * 查询用户角色列表
       */
      @GetMapping("/user/{userId}")
      public R<List<Role>> listUserRoles(@PathVariable Long userId) {
            List<Role> roles = roleService.listUserRoles(userId);
            return R.ok(roles);
      }
}
