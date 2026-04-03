package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserMapper userMapper;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() throws Exception {
        roleService = spy(new RoleServiceImpl(permissionService, userMapper));
        Field field = findField(roleService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(roleService, roleMapper);
    }

    @Test
    void shouldFilterRolePageToCurrentUserTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.ALL, false));

        Page<Role> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(roleService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class));

        invokeScopedPageRoles(99L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Role>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(roleService).page(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantRoleDetailAccess() throws Exception {
        Role role = new Role();
        role.setId(1L);
        role.setTenantId(2L);

        when(roleMapper.selectById(1L)).thenReturn(role);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedGetById(99L, 1L));
        assertEquals("角色不存在或无权访问", exception.getMessage());
    }

    @Test
    void shouldDeleteRoleViaLogicDeleteOperation() {
        Role existing = new Role();
        existing.setId(1L);
        existing.setDeleted(0);

        when(roleMapper.selectById(1L)).thenReturn(existing);
        doReturn(true).when(roleService).removeById(1L);
        doNothing().when(permissionService).deleteRoleRelations(1L);

        roleService.deleteRole(1L);

        verify(roleService).removeById(1L);
        verify(permissionService).deleteRoleRelations(1L);
        verify(roleService, never()).updateById(existing);
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Object invokeScopedPageRoles(Long currentUserId,
                                         String roleName,
                                         String roleCode,
                                         Integer status,
                                         Long pageNum,
                                         Long pageSize) throws Exception {
        try {
            Method method = RoleServiceImpl.class.getMethod(
                    "pageRoles",
                    Long.class,
                    String.class,
                    String.class,
                    Integer.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(roleService, currentUserId, roleName, roleCode, status, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageRoles overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Role invokeScopedGetById(Long currentUserId, Long roleId) throws Exception {
        try {
            Method method = RoleServiceImpl.class.getMethod("getById", Long.class, Long.class);
            return (Role) method.invoke(roleService, currentUserId, roleId);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped getById overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        if (exception.getTargetException() instanceof Exception target) {
            return target;
        }
        throw exception;
    }
}
