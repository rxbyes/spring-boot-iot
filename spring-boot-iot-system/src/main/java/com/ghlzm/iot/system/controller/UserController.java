package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

      @Autowired
      private UserService userService;

      /**
       * 添加用户
       */
      @PostMapping("/add")
      public R<Void> addUser(@RequestBody User user) {
            userService.addUser(user);
            return R.ok();
      }

      /**
       * 查询用户列表
       */
      @GetMapping("/list")
      public R<List<User>> listUsers(
                  @RequestParam(required = false) String username,
                  @RequestParam(required = false) String phone,
                  @RequestParam(required = false) String email,
                  @RequestParam(required = false) Integer status) {
            List<User> users = userService.listUsers(username, phone, email, status);
            return R.ok(users);
      }

      /**
       * 根据ID查询用户
       */
      @GetMapping("/{id}")
      public R<User> getById(@PathVariable Long id) {
            User user = userService.getById(id);
            return R.ok(user);
      }

      /**
       * 更新用户
       */
      @PutMapping("/update")
      public R<Void> updateUser(@RequestBody User user) {
            userService.updateUser(user);
            return R.ok();
      }

      /**
       * 删除用户
       */
      @DeleteMapping("/{id}")
      public R<Void> deleteUser(@PathVariable Long id) {
            userService.deleteUser(id);
            return R.ok();
      }

      /**
       * 根据用户名查询用户
       */
      @GetMapping("/username/{username}")
      public R<User> getByUsername(@PathVariable String username) {
            User user = userService.getByUsername(username);
            return R.ok(user);
      }

      /**
       * 修改密码
       */
      @PostMapping("/change-password")
      public R<Void> changePassword(@RequestBody User user) {
            userService.changePassword(user.getId(), user.getPassword(), user.getPassword());
            return R.ok();
      }

      /**
       * 重置密码
       */
      @PostMapping("/reset-password/{userId}")
      public R<Void> resetPassword(@PathVariable Long userId) {
            userService.resetPassword(userId);
            return R.ok();
      }
}
