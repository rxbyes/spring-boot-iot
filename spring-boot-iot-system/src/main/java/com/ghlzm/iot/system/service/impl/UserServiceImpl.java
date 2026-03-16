package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.vo.RoleSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

      private final UserMapper userMapper;
      private final PasswordEncoder passwordEncoder;
      private final PermissionService permissionService;

      public UserServiceImpl(UserMapper userMapper,
                             PasswordEncoder passwordEncoder,
                             PermissionService permissionService) {
            this.userMapper = userMapper;
            this.passwordEncoder = passwordEncoder;
            this.permissionService = permissionService;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public User addUser(User user) {
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername()));
            if (existingUser != null) {
                  throw new BizException("用户名已存在");
            }

            if (StringUtils.hasText(user.getPassword())) {
                  user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            if (user.getTenantId() == null) {
                  user.setTenantId(1L);
            }
            if (user.getStatus() == null) {
                  user.setStatus(1);
            }
            user.setCreateTime(new Date());
            user.setDeleted(0);

            userMapper.insert(user);
            if (user.getRoleIds() != null) {
                  permissionService.replaceUserRoles(user.getId(), user.getRoleIds(), user.getCreateBy());
            }
            return getById(user.getId());
      }

      @Override
      public List<User> listUsers(String username, String phone, String email, Integer status) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(username)) {
                  wrapper.like(User::getUsername, username.trim());
            }
            if (StringUtils.hasText(phone)) {
                  wrapper.like(User::getPhone, phone.trim());
            }
            if (StringUtils.hasText(email)) {
                  wrapper.like(User::getEmail, email.trim());
            }
            if (status != null) {
                  wrapper.eq(User::getStatus, status);
            }
            wrapper.orderByDesc(User::getCreateTime).orderByDesc(User::getId);

            List<User> users = userMapper.selectList(wrapper);
            fillUserRoles(users);
            return users;
      }

      @Override
      public User getById(Long id) {
            User user = userMapper.selectById(id);
            if (user == null) {
                  return null;
            }
            fillUserRoles(List.of(user));
            user.setRoleIds(permissionService.listUserRoleIds(id));
            return user;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateUser(User user) {
            User existingUser = userMapper.selectById(user.getId());
            if (existingUser == null) {
                  throw new BizException("用户不存在");
            }

            User sameUsernameUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, user.getUsername())
                        .ne(User::getId, user.getId()));
            if (sameUsernameUser != null) {
                  throw new BizException("用户名已存在");
            }

            user.setTenantId(existingUser.getTenantId());
            user.setUpdateTime(new Date());
            userMapper.updateById(user);

            if (user.getRoleIds() != null) {
                  permissionService.replaceUserRoles(user.getId(), user.getRoleIds(), user.getUpdateBy());
            }
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
            permissionService.deleteUserRoles(id);
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

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                  throw new BizException("原密码错误");
            }

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

            user.setPassword(passwordEncoder.encode("123456"));
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
      }

      private void fillUserRoles(List<User> users) {
            if (CollectionUtils.isEmpty(users)) {
                  return;
            }

            Map<Long, List<RoleSummaryVO>> roleMap = permissionService.listUserRolesByUserIds(
                    users.stream().map(User::getId).toList()
            );
            for (User user : users) {
                  List<RoleSummaryVO> roles = roleMap.getOrDefault(user.getId(), List.of());
                  user.setRoleNames(roles.stream().map(RoleSummaryVO::getRoleName).collect(Collectors.toList()));
            }
      }
}
