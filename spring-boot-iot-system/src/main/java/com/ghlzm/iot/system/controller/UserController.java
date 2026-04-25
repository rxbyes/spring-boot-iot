package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.dto.ChangePasswordDTO;
import com.ghlzm.iot.system.dto.UserProfileUpdateDTO;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
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
@RequestMapping("/api/user")
public class UserController {

      private final UserService userService;
      private final GovernancePermissionGuard permissionGuard;

      public UserController(UserService userService) {
            this(userService, null);
      }

      @Autowired
      public UserController(UserService userService, GovernancePermissionGuard permissionGuard) {
            this.userService = userService;
            this.permissionGuard = permissionGuard;
      }

      @PostMapping("/add")
      public R<Void> addUser(@RequestBody User user, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "新增用户", GovernancePermissionCodes.USER_ADD);
            userService.addUser(currentUserId, user);
            return R.ok();
      }

      @GetMapping("/list")
      public R<List<User>> listUsers(@RequestParam(required = false) String username,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) Integer status,
                                     Authentication authentication) {
            return R.ok(userService.listUsers(requireCurrentUserId(authentication), username, phone, email, status));
      }

      @GetMapping("/page")
      public R<PageResult<User>> pageUsers(@RequestParam(required = false) String username,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize,
                                           Authentication authentication) {
            return R.ok(userService.pageUsers(requireCurrentUserId(authentication), username, phone, email, status, pageNum, pageSize));
      }

      @GetMapping("/{id}")
      public R<User> getById(@PathVariable Long id, Authentication authentication) {
            return R.ok(userService.getById(requireCurrentUserId(authentication), id));
      }

      @PutMapping("/update")
      public R<Void> updateUser(@RequestBody User user, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "编辑用户", GovernancePermissionCodes.USER_UPDATE);
            userService.updateUser(currentUserId, user);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "删除用户", GovernancePermissionCodes.USER_DELETE);
            userService.deleteUser(currentUserId, id);
            return R.ok();
      }

      @GetMapping("/username/{username}")
      public R<User> getByUsername(@PathVariable String username) {
            return R.ok(userService.getByUsername(username));
      }

      @PostMapping("/change-password")
      public R<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
            userService.changePassword(dto.getId(), dto.getOldPassword(), dto.getNewPassword());
            return R.ok();
      }

      @PostMapping("/reset-password/{userId}")
      public R<Void> resetPassword(@PathVariable Long userId, Authentication authentication) {
            Long currentUserId = requireCurrentUserId(authentication);
            requirePermission(currentUserId, "重置用户密码", GovernancePermissionCodes.USER_RESET_PASSWORD);
            userService.resetPassword(currentUserId, userId);
            return R.ok();
      }

      @PutMapping("/profile")
      public R<Void> updateCurrentUserProfile(@RequestBody UserProfileUpdateDTO dto,
                                             Authentication authentication) {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            userService.updateCurrentUserProfile(principal.userId(), dto);
            return R.ok();
      }

      private Long requireCurrentUserId(Authentication authentication) {
            if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
                  throw new com.ghlzm.iot.common.exception.BizException(401, "未认证，请先登录");
            }
            return principal.userId();
      }

      private void requirePermission(Long currentUserId, String actionName, String permissionCode) {
            if (permissionGuard != null) {
                  permissionGuard.requireAnyPermission(currentUserId, actionName, permissionCode);
            }
      }
}
