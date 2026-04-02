package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.dto.UserProfileUpdateDTO;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionService permissionService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, passwordEncoder, permissionService);
    }

    @Test
    void shouldUpdateCurrentUserProfileWithoutChangingRolesOrStatus() {
        User existing = new User();
        existing.setId(1L);
        existing.setTenantId(1L);
        existing.setOrgId(7101L);
        existing.setStatus(1);
        existing.setUsername("admin");
        existing.setNickname("旧昵称");

        when(userMapper.selectById(1L)).thenReturn(existing);

        userService.updateCurrentUserProfile(1L, new UserProfileUpdateDTO(
                "新昵称",
                "超级管理员",
                "13800000000",
                "admin@ghlzm.com",
                "/avatars/admin.png"
        ));

        verify(userMapper).updateById(argThat(user ->
                Objects.equals(user.getId(), 1L)
                        && Objects.equals(user.getNickname(), "新昵称")
                        && Objects.equals(user.getRealName(), "超级管理员")
                        && Objects.equals(user.getPhone(), "13800000000")
                        && Objects.equals(user.getEmail(), "admin@ghlzm.com")
                        && Objects.equals(user.getAvatar(), "/avatars/admin.png")
                        && user.getUsername() == null
                        && user.getStatus() == null
                        && user.getOrgId() == null
        ));
        verifyNoInteractions(permissionService);
    }
}
