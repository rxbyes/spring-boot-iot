package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户 Service 实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

      @Autowired
      private UserMapper userMapper;

      @Autowired
      private PasswordEncoder passwordEncoder;

      @Override
      @Transactional(rollbackFor = Exception.class)
      public User addUser(User user) {
            // 检查用户名是否已存在
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername()));
            if (existingUser != null) {
                  throw new BizException("用户名已存在");
            }

            // 加密密码
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                  user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            user.setCreateTime(new Date());
            user.setCreateBy(user.getCreateBy());
            user.setDeleted(0);
            user.setStatus(1);

            userMapper.insert(user);
            return user;
      }

      @Override
      public List<User> listUsers(String username, String phone, String email, Integer status) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (username != null && !username.isEmpty()) {
                  wrapper.like(User::getUsername, username);
            }
            if (phone != null && !phone.isEmpty()) {
                  wrapper.like(User::getPhone, phone);
            }
            if (email != null && !email.isEmpty()) {
                  wrapper.like(User::getEmail, email);
            }
            if (status != null) {
                  wrapper.eq(User::getStatus, status);
            }
            wrapper.eq(User::getDeleted, 0);
            wrapper.orderByDesc(User::getCreateTime);
            return userMapper.selectList(wrapper);
      }

      @Override
      public User getById(Long id) {
            return userMapper.selectById(id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateUser(User user) {
            User existingUser = userMapper.selectById(user.getId());
            if (existingUser == null) {
                  throw new BizException("用户不存在");
            }

            // 检查用户名是否被其他用户使用
            User sameUsernameUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername())
                        .ne(User::getId, user.getId()));
            if (sameUsernameUser != null) {
                  throw new BizException("用户名已存在");
            }

            user.setUpdateTime(new Date());
            user.setUpdateBy(user.getUpdateBy());

            userMapper.updateById(user);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteUser(Long id) {
            User user = userMapper.selectById(id);
            if (user == null) {
                  throw new BizException("用户不存在");
            }

            user.setDeleted(1);
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
      }

      @Override
      public User getByUsername(String username) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0));
      }

      @Override
      public User getByPhone(String phone) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .eq(User::getDeleted, 0));
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void changePassword(Long userId, String oldPassword, String newPassword) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                  throw new BizException("用户不存在");
            }

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                  throw new BizException("原密码错误");
            }

            // 加密新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void resetPassword(Long userId) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                  throw new BizException("用户不存在");
            }

            // 重置为默认密码 123456
            user.setPassword(passwordEncoder.encode("123456"));
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
      }
}
