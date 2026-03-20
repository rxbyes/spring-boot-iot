package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.service.RoleService;
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

      public RoleController(RoleService roleService) {
            this.roleService = roleService;
      }

      @PostMapping("/add")
      public R<Void> addRole(@RequestBody Role role) {
            roleService.addRole(role);
            return R.ok();
      }

      @GetMapping("/list")
      public R<List<Role>> listRoles(@RequestParam(required = false) String roleName,
                                     @RequestParam(required = false) String roleCode,
                                     @RequestParam(required = false) Integer status) {
            return R.ok(roleService.listRoles(roleName, roleCode, status));
      }

      @GetMapping("/page")
      public R<PageResult<Role>> pageRoles(@RequestParam(required = false) String roleName,
                                           @RequestParam(required = false) String roleCode,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize) {
            return R.ok(roleService.pageRoles(roleName, roleCode, status, pageNum, pageSize));
      }

      @GetMapping("/{id}")
      public R<Role> getById(@PathVariable Long id) {
            return R.ok(roleService.getById(id));
      }

      @PutMapping("/update")
      public R<Void> updateRole(@RequestBody Role role) {
            roleService.updateRole(role);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteRole(@PathVariable Long id) {
            roleService.deleteRole(id);
            return R.ok();
      }

      @GetMapping("/user/{userId}")
      public R<List<Role>> listUserRoles(@PathVariable Long userId) {
            return R.ok(roleService.listUserRoles(userId));
      }
}
