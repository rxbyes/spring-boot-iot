package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

      User addUser(User user);

      List<User> listUsers(String username, String phone, String email, Integer status);

      PageResult<User> pageUsers(String username, String phone, String email, Integer status, Long pageNum, Long pageSize);

      User getById(Long id);

      void updateUser(User user);

      void deleteUser(Long id);

      User getByUsername(String username);

      User getByPhone(String phone);

      void changePassword(Long userId, String oldPassword, String newPassword);

      void resetPassword(Long userId);
}
