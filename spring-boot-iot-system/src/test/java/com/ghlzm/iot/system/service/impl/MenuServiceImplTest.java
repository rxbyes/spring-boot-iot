package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private MenuSchemaSupport menuSchemaSupport;
    @Mock
    private PermissionService permissionService;

    private MenuServiceImpl menuService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Menu.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        menuService = spy(instantiateService());
        Field field = findField(menuService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(menuService, menuMapper);
    }

    @Test
    void shouldFilterScopedMenuPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        Page<Menu> page = new Page<>(1L, 10L);
        page.setRecords(List.of());
        page.setTotal(0L);
        doReturn(page).when(menuService).page(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class));

        invokeScopedPageMenus(99L, null, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Menu>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(menuService).page(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantScopedMenuDetailAccess() throws Exception {
        Menu menu = new Menu();
        menu.setId(1L);
        setTenantId(menu, 2L);

        when(menuMapper.selectById(1L)).thenReturn(menu);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedGetById(99L, 1L));
        assertEquals("菜单不存在或无权访问", exception.getMessage());
    }

    private MenuServiceImpl instantiateService() throws Exception {
        for (Constructor<?> constructor : MenuServiceImpl.class.getConstructors()) {
            if (constructor.getParameterCount() == 4) {
                return (MenuServiceImpl) constructor.newInstance(roleMenuMapper, jdbcTemplate, menuSchemaSupport, permissionService);
            }
            if (constructor.getParameterCount() == 3) {
                return (MenuServiceImpl) constructor.newInstance(roleMenuMapper, jdbcTemplate, menuSchemaSupport);
            }
        }
        throw new AssertionError("MenuServiceImpl constructor not found");
    }

    private Object invokeScopedPageMenus(Long currentUserId,
                                         String menuName,
                                         String menuCode,
                                         Integer type,
                                         Integer status,
                                         Long pageNum,
                                         Long pageSize) throws Exception {
        try {
            Method method = MenuServiceImpl.class.getMethod(
                    "pageMenus",
                    Long.class,
                    String.class,
                    String.class,
                    Integer.class,
                    Integer.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(menuService, currentUserId, menuName, menuCode, type, status, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageMenus overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private Menu invokeScopedGetById(Long currentUserId, Long menuId) throws Exception {
        try {
            Method method = MenuServiceImpl.class.getMethod("getMenuById", Long.class, Long.class);
            return (Menu) method.invoke(menuService, currentUserId, menuId);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped getMenuById overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private void setTenantId(Menu menu, Long tenantId) throws Exception {
        try {
            Method method = Menu.class.getMethod("setTenantId", Long.class);
            method.invoke(menu, tenantId);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Menu tenantId field is missing", exception);
        }
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

    private Exception unwrap(InvocationTargetException exception) throws Exception {
        if (exception.getTargetException() instanceof Exception target) {
            return target;
        }
        throw exception;
    }
}
