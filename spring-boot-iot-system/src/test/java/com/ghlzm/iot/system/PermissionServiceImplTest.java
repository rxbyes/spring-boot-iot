package com.ghlzm.iot.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghlzm.iot.system.entity.Menu;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.Tenant;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.mapper.TenantMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.mapper.UserRoleMapper;
import com.ghlzm.iot.system.service.impl.PermissionServiceImpl;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Menu.class);
        TableInfoHelper.initTableInfo(assistant, Organization.class);
        TableInfoHelper.initTableInfo(assistant, Role.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Tenant.class);
    }

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
        assertEquals("已填写实名信息", context.getAuthStatus());
        assertEquals(List.of("账号登录", "手机号登录"), context.getLoginMethods());
        assertEquals("ORG_AND_CHILDREN", context.getDataScopeType());
        assertEquals("本机构及下级", context.getDataScopeSummary());
        assertTrue(context.getDisplayName().contains("运营管理负责人"));
    }

    @Test
    void shouldResolveDataPermissionContextAndOrganizationSubtree() {
        Long userId = 1005L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5001L);
        user.setUsername("org-manager");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3005L);
        role.setRoleCode("MANAGEMENT_STAFF");
        role.setRoleName("管理人员");
        role.setDataScopeType("ORG_AND_CHILDREN");

        Organization root = new Organization();
        root.setId(5001L);
        root.setParentId(0L);
        root.setOrgName("平台治理中心");
        root.setDeleted(0);

        Organization child = new Organization();
        child.setId(5002L);
        child.setParentId(5001L);
        child.setOrgName("监测一部");
        child.setDeleted(0);

        Organization grandChild = new Organization();
        grandChild.setId(5003L);
        grandChild.setParentId(5002L);
        grandChild.setOrgName("监测一组");
        grandChild.setDeleted(0);

        Organization anotherRoot = new Organization();
        anotherRoot.setId(6001L);
        anotherRoot.setParentId(0L);
        anotherRoot.setOrgName("外部机构");
        anotherRoot.setDeleted(0);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(organizationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child, grandChild, anotherRoot));

        DataPermissionContext context = permissionService.getDataPermissionContext(userId);
        Set<Long> orgIds = permissionService.listAccessibleOrganizationIds(userId);

        assertEquals(userId, context.userId());
        assertEquals(1L, context.tenantId());
        assertEquals(5001L, context.orgId());
        assertEquals(DataScopeType.ORG_AND_CHILDREN, context.dataScopeType());
        assertEquals(Set.of(5001L, 5002L, 5003L), orgIds);
    }

    @Test
    void shouldFallbackToCurrentOrganizationForSelfScopeOrganizationAccess() {
        Long userId = 1006L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5002L);
        user.setUsername("self-user");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3006L);
        role.setRoleCode("BUSINESS_STAFF");
        role.setRoleName("业务人员");
        role.setDataScopeType("SELF");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        DataPermissionContext context = permissionService.getDataPermissionContext(userId);
        Set<Long> orgIds = permissionService.listAccessibleOrganizationIds(userId);

        assertEquals(DataScopeType.SELF, context.dataScopeType());
        assertEquals(Set.of(5002L), orgIds);
    }

    @Test
    void shouldExposeAllScopeForSuperAdminAuthContext() {
        Long userId = 1007L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5001L);
        user.setUsername("admin");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3007L);
        role.setRoleCode("SUPER_ADMIN");
        role.setRoleName("超级管理员");
        role.setDataScopeType("TENANT");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        UserAuthContextVO context = permissionService.getUserAuthContext(userId);

        assertTrue(context.isSuperAdmin());
        assertEquals(DataScopeType.ALL.name(), context.getDataScopeType());
        assertEquals(DataScopeType.ALL.getLabel(), context.getDataScopeSummary());
    }

    @Test
    void shouldFilterActiveMenusToCurrentUserTenantWhenBuildingAuthContext() {
        Long userId = 1009L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5001L);
        user.setUsername("tenant-user");
        user.setDeleted(0);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of());
        when(menuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        permissionService.getUserAuthContext(userId);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<Menu>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(menuMapper).selectList(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldListTenantOrganizationsForSuperAdminWithoutBoundOrg() {
        Long userId = 1008L;
        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(null);
        user.setUsername("admin-no-org");
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3008L);
        role.setRoleCode("SUPER_ADMIN");
        role.setRoleName("超级管理员");
        role.setDataScopeType("TENANT");

        Organization root = new Organization();
        root.setId(5001L);
        root.setParentId(0L);
        root.setDeleted(0);

        Organization child = new Organization();
        child.setId(5002L);
        child.setParentId(5001L);
        child.setDeleted(0);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(organizationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child));

        Set<Long> orgIds = permissionService.listAccessibleOrganizationIds(userId);

        assertEquals(Set.of(5001L, 5002L), orgIds);
    }
}
