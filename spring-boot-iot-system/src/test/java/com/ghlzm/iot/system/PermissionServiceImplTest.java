package com.ghlzm.iot.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.Tenant;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.mapper.TenantMapper;
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
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private OrganizationMapper organizationMapper;

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
                tenantMapper,
                organizationMapper,
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

    @Test
    void shouldReturnRoleWorkspaceHomeWhenPreferredMenuRootExists() {
        Long userId = 1002L;
        User user = new User();
        user.setId(userId);
        user.setUsername("business-user");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3001L);
        role.setRoleCode("BUSINESS_STAFF");
        role.setRoleName("业务人员");

        Menu riskRoot = new Menu();
        riskRoot.setId(93000002L);
        riskRoot.setParentId(0L);
        riskRoot.setMenuName("风险运营");
        riskRoot.setMenuCode("risk-ops");
        riskRoot.setType(0);
        riskRoot.setSort(20);

        Menu alarmMenu = new Menu();
        alarmMenu.setId(93002001L);
        alarmMenu.setParentId(93000002L);
        alarmMenu.setMenuName("告警运营台");
        alarmMenu.setMenuCode("risk:alarm");
        alarmMenu.setType(1);
        alarmMenu.setSort(21);
        alarmMenu.setPath("/alarm-center");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(riskRoot, alarmMenu));
        when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(role.getId()))).thenReturn(List.of(alarmMenu.getId()));

        UserAuthContextVO context = permissionService.getUserAuthContext(userId);

        assertEquals("/risk-disposal", context.getHomePath());
    }

    @Test
    void shouldFallbackToFirstAuthorizedPageWhenPreferredWorkspaceMissing() {
        Long userId = 1003L;
        User user = new User();
        user.setId(userId);
        user.setUsername("developer-user");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3002L);
        role.setRoleCode("DEVELOPER_STAFF");
        role.setRoleName("开发人员");

        Menu riskRoot = new Menu();
        riskRoot.setId(93000002L);
        riskRoot.setParentId(0L);
        riskRoot.setMenuName("风险运营");
        riskRoot.setMenuCode("risk-ops");
        riskRoot.setType(0);
        riskRoot.setSort(20);

        Menu alarmMenu = new Menu();
        alarmMenu.setId(93002001L);
        alarmMenu.setParentId(93000002L);
        alarmMenu.setMenuName("告警运营台");
        alarmMenu.setMenuCode("risk:alarm");
        alarmMenu.setType(1);
        alarmMenu.setSort(21);
        alarmMenu.setPath("/alarm-center");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(riskRoot, alarmMenu));
        when(roleMenuMapper.selectMenuIdsByRoleIds(List.of(role.getId()))).thenReturn(List.of(alarmMenu.getId()));

        UserAuthContextVO context = permissionService.getUserAuthContext(userId);

        assertEquals("/alarm-center", context.getHomePath());
    }

    @Test
    void shouldExposeTenantOrganizationAndScopeSummary() {
        Long userId = 1004L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5001L);
        user.setUsername("manager-demo");
        user.setNickname("运营管理负责人");
        user.setRealName("管理演示账号");
        user.setPhone("13800000002");
        user.setEmail("manager_demo@ghlzm.com");
        user.setLastLoginIp("10.10.10.8");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3003L);
        role.setRoleCode("MANAGEMENT_STAFF");
        role.setRoleName("管理人员");
        role.setDataScopeType("ORG_AND_CHILDREN");

        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantName("默认租户");

        Organization organization = new Organization();
        organization.setId(5001L);
        organization.setOrgName("平台治理中心");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(organizationMapper.selectById(5001L)).thenReturn(organization);

        UserAuthContextVO context = permissionService.getUserAuthContext(userId);

        assertEquals(1L, context.getTenantId());
        assertEquals("默认租户", context.getTenantName());
        assertEquals(5001L, context.getOrgId());
        assertEquals("平台治理中心", context.getOrgName());
        assertEquals("运营管理负责人", context.getNickname());
        assertEquals(List.of("账号登录", "手机号登录"), context.getLoginMethods());
        assertEquals("ORG_AND_CHILDREN", context.getDataScopeType());
        assertEquals("本机构及下级", context.getDataScopeSummary());
        assertTrue(context.getDisplayName().contains("运营管理负责人"));
    }
}
