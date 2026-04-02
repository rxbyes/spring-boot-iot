package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.dto.UserProfileUpdateDTO;
import com.ghlzm.iot.system.entity.User;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
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

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @BeforeEach
    void setUp() {
        userService = spy(new UserServiceImpl(userMapper, passwordEncoder, permissionService));
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

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User updated = userCaptor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals("新昵称", updated.getNickname());
        assertEquals("超级管理员", updated.getRealName());
        assertEquals("13800000000", updated.getPhone());
        assertEquals("admin@ghlzm.com", updated.getEmail());
        assertEquals("/avatars/admin.png", updated.getAvatar());
        assertNull(updated.getUsername());
        assertNull(updated.getStatus());
        assertNull(updated.getOrgId());
        verifyNoInteractions(permissionService);
    }

    @Test
    void shouldFilterUserPageToAccessibleOrganizationsForOrgChildrenScope() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.ORG_AND_CHILDREN, false));
        when(permissionService.listAccessibleOrganizationIds(1L)).thenReturn(java.util.Set.of(7101L, 7102L));

        Page<User> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(userService).page(any(Page.class), any(LambdaQueryWrapper.class));

        userService.pageUsers(1L, null, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("org_id"));
    }

    @Test
    void shouldFilterUserPageToCurrentUserForSelfScope() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.SELF, false));

        Page<User> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(userService).page(any(Page.class), any(LambdaQueryWrapper.class));

        userService.pageUsers(1L, null, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("id"));
    }

    @Test
    void shouldDeleteUserViaLogicDeleteOperation() {
        User existing = new User();
        existing.setId(1L);
        existing.setTenantId(1L);
        existing.setOrgId(7101L);
        existing.setDeleted(0);

        when(userMapper.selectById(1L)).thenReturn(existing);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.ALL, true));
        doReturn(true).when(userService).removeById(1L);
        doNothing().when(permissionService).deleteUserRoles(1L);

        userService.deleteUser(99L, 1L);

        verify(userService).removeById(1L);
        verify(permissionService).deleteUserRoles(1L);
        verify(userMapper, never()).updateById(any(User.class));
    }
}
