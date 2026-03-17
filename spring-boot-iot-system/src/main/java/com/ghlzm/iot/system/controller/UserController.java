package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.system.dto.ChangePasswordDTO;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.service.UserService;
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

      public UserController(UserService userService) {
            this.userService = userService;
      }

      @PostMapping("/add")
      public R<Void> addUser(@RequestBody User user) {
            userService.addUser(user);
            return R.ok();
      }

      @GetMapping("/list")
      public R<List<User>> listUsers(@RequestParam(required = false) String username,
                                     @RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) Integer status) {
            return R.ok(userService.listUsers(username, phone, email, status));
      }

      @GetMapping("/page")
      public R<PageResult<User>> pageUsers(@RequestParam(required = false) String username,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Long pageNum,
                                           @RequestParam(defaultValue = "10") Long pageSize) {
            return R.ok(userService.pageUsers(username, phone, email, status, pageNum, pageSize));
      }

      @GetMapping("/{id}")
      public R<User> getById(@PathVariable Long id) {
            return R.ok(userService.getById(id));
      }

      @PutMapping("/update")
      public R<Void> updateUser(@RequestBody User user) {
            userService.updateUser(user);
            return R.ok();
      }

      @DeleteMapping("/{id}")
      public R<Void> deleteUser(@PathVariable Long id) {
            userService.deleteUser(id);
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
      public R<Void> resetPassword(@PathVariable Long userId) {
            userService.resetPassword(userId);
            return R.ok();
      }
}
