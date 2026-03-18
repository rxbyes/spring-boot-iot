package com.ghlzm.iot.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.mapper.UserRoleMapper;
import com.ghlzm.iot.system.service.impl.PermissionServiceImpl;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        permissionService = new PermissionServiceImpl(
                userMapper,
                roleMapper,
                menuMapper,
                userRoleMapper,
                roleMenuMapper,
                objectMapper
        );
    }

    @Test
    void shouldNotQueryRoleMenuWhenUserHasNoRoles() {
        Long userId = 1001L;
        User user = new User();
        user.setId(userId);
        user.setUsername("no-role-user");
        user.setDeleted(0);

        Menu menu = new Menu();
        menu.setId(2001L);
        menu.setParentId(0L);
        menu.setMenuName("Demo Menu");
        menu.setType(1);
        menu.setSort(1);
        menu.setPath("/demo");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of());
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu));

        UserAuthContextVO context = permissionService.getUserAuthContext(userId);

        assertNotNull(context);
        assertEquals(userId, context.getUserId());
        assertTrue(context.getRoles().isEmpty());
        assertTrue(context.getRoleCodes().isEmpty());
        assertTrue(context.getPermissions().isEmpty());
        assertTrue(context.getMenus().isEmpty());
        assertEquals("/", context.getHomePath());
        verify(roleMenuMapper, never()).selectMenuIdsByRoleIds(any());
    }
}
