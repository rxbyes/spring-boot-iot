package com.ghlzm.iot.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.system.entity.User;

import java.util.List;

/**
 * 用户 Service
 */
public interface UserService extends IService<User> {

      /**
       * 添加用户
       */
      User addUser(User user);

      /**
       * 查询用户列表
       */
      List<User> listUsers(String username, String phone, String email, Integer status);

      /**
       * 根据ID查询用户
       */
      User getById(Long id);

      /**
       * 更新用户
       */
      void updateUser(User user);

      /**
       * 删除用户
       */
      void deleteUser(Long id);

      /**
       * 根据用户名查询用户
       */
      User getByUsername(String username);

      /**
       * 根据手机号查询用户
       */
      User getByPhone(String phone);

      /**
       * 修改密码
       */
      void changePassword(Long userId, String oldPassword, String newPassword);

      /**
       * 重置密码
       */
      void resetPassword(Long userId);
}
