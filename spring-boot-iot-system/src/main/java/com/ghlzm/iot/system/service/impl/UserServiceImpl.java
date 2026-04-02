package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.dto.UserProfileUpdateDTO;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.UserService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
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
import java.util.Set;
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
            return addUser(null, user);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public User addUser(Long currentUserId, User user) {
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, user.getUsername()));
            if (existingUser != null) {
                  throw new BizException("用户名已存在");
            }
            ensureWritableOrganization(currentUserId, user.getOrgId());

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
            return listUsers(null, username, phone, email, status);
      }

      @Override
      public List<User> listUsers(Long currentUserId, String username, String phone, String email, Integer status) {
            List<User> users = userMapper.selectList(buildUserQueryWrapper(currentUserId, username, phone, email, status));
            fillUserRoles(users);
            return users;
      }

      @Override
      public PageResult<User> pageUsers(String username, String phone, String email, Integer status, Long pageNum, Long pageSize) {
            return pageUsers(null, username, phone, email, status, pageNum, pageSize);
      }

      @Override
      public PageResult<User> pageUsers(Long currentUserId, String username, String phone, String email, Integer status, Long pageNum, Long pageSize) {
            Page<User> page = PageQueryUtils.buildPage(pageNum, pageSize);
            Page<User> result = page(page, buildUserQueryWrapper(currentUserId, username, phone, email, status));
            List<User> records = result.getRecords();
            fillUserRoles(records);
            return PageQueryUtils.toPageResult(result);
      }

      @Override
      public User getById(Long id) {
            return getById(null, id);
      }

      @Override
      public User getById(Long currentUserId, Long id) {
            User user = userMapper.selectById(id);
            if (user == null) {
                  return null;
            }
            ensureUserAccessible(currentUserId, user);
            fillUserRoles(List.of(user));
            user.setRoleIds(permissionService.listUserRoleIds(id));
            return user;
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateUser(User user) {
            updateUser(null, user);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateUser(Long currentUserId, User user) {
            User existingUser = userMapper.selectById(user.getId());
            if (existingUser == null) {
                  throw new BizException("用户不存在");
            }
            ensureUserAccessible(currentUserId, existingUser);
            ensureWritableOrganization(currentUserId, user.getOrgId());

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
            deleteUser(null, id);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void deleteUser(Long currentUserId, Long id) {
            User user = userMapper.selectById(id);
            if (user == null) {
                  throw new BizException("用户不存在");
            }
            ensureUserAccessible(currentUserId, user);

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
            resetPassword(null, userId);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void resetPassword(Long currentUserId, Long userId) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                  throw new BizException("用户不存在");
            }
            ensureUserAccessible(currentUserId, user);

            user.setPassword(passwordEncoder.encode("123456"));
            user.setUpdateTime(new Date());
            userMapper.updateById(user);
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public void updateCurrentUserProfile(Long userId, UserProfileUpdateDTO dto) {
            User existingUser = userMapper.selectById(userId);
            if (existingUser == null || Integer.valueOf(1).equals(existingUser.getDeleted())) {
                  throw new BizException("用户不存在或已失效");
            }

            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setNickname(normalizeNullable(dto.nickname()));
            updateUser.setRealName(normalizeNullable(dto.realName()));
            updateUser.setPhone(normalizeNullable(dto.phone()));
            updateUser.setEmail(normalizeNullable(dto.email()));
            updateUser.setAvatar(normalizeNullable(dto.avatar()));
            updateUser.setUpdateTime(new Date());
            userMapper.updateById(updateUser);
      }

      private LambdaQueryWrapper<User> buildUserQueryWrapper(Long currentUserId, String username, String phone, String email, Integer status) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            applyUserScope(wrapper, currentUserId);
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
            return wrapper;
      }

      private void applyUserScope(LambdaQueryWrapper<User> wrapper, Long currentUserId) {
            if (currentUserId == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            wrapper.eq(context.tenantId() != null, User::getTenantId, context.tenantId());
            if (context.superAdmin() || context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.SELF) {
                  wrapper.eq(User::getId, currentUserId);
                  return;
            }
            Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
            if (accessibleOrgIds.isEmpty()) {
                  wrapper.eq(User::getId, -1L);
                  return;
            }
            wrapper.in(User::getOrgId, accessibleOrgIds);
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

      private String normalizeNullable(String value) {
            if (value == null) {
                  return null;
            }
            return value.trim();
      }

      private void ensureWritableOrganization(Long currentUserId, Long orgId) {
            if (currentUserId == null || orgId == null || orgId <= 0) {
                  return;
            }
            if (!permissionService.listAccessibleOrganizationIds(currentUserId).contains(orgId)) {
                  throw new BizException("目标机构不在当前账号的数据范围内");
            }
      }

      private void ensureUserAccessible(Long currentUserId, User targetUser) {
            if (currentUserId == null || targetUser == null) {
                  return;
            }
            DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
            if (context.superAdmin()) {
                  return;
            }
            if (context.tenantId() != null && !context.tenantId().equals(targetUser.getTenantId())) {
                  throw new BizException("用户不存在或无权访问");
            }
            if (context.dataScopeType() == DataScopeType.ALL || context.dataScopeType() == DataScopeType.TENANT) {
                  return;
            }
            if (context.dataScopeType() == DataScopeType.SELF) {
                  if (!currentUserId.equals(targetUser.getId())) {
                        throw new BizException("用户不存在或无权访问");
                  }
                  return;
            }
            if (targetUser.getOrgId() == null || !permissionService.listAccessibleOrganizationIds(currentUserId).contains(targetUser.getOrgId())) {
                  throw new BizException("用户不存在或无权访问");
            }
      }
}
