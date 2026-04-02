package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

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

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() throws Exception {
        roleService = spy(new RoleServiceImpl(permissionService));
        Field field = findField(roleService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(roleService, roleMapper);
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
}
