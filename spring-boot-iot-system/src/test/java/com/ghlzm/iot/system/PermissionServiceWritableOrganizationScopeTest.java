package com.ghlzm.iot.system;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.entity.Role;
import com.ghlzm.iot.system.entity.User;
import com.ghlzm.iot.system.mapper.MenuMapper;
import com.ghlzm.iot.system.mapper.OrganizationMapper;
import com.ghlzm.iot.system.mapper.RoleMapper;
import com.ghlzm.iot.system.mapper.RoleMenuMapper;
import com.ghlzm.iot.system.mapper.TenantMapper;
import com.ghlzm.iot.system.mapper.UserMapper;
import com.ghlzm.iot.system.mapper.UserRoleMapper;
import com.ghlzm.iot.system.service.impl.PermissionServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceWritableOrganizationScopeTest {

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
    void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Organization.class);
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
    void shouldAllowOrgScopeToWriteIntoCurrentOrganizationSubtree() {
        Long userId = 2001L;

        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5001L);
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3001L);
        role.setRoleCode("MANAGEMENT_STAFF");
        role.setDataScopeType("ORG");

        Organization root = new Organization();
        root.setId(5001L);
        root.setParentId(0L);
        root.setDeleted(0);

        Organization child = new Organization();
        child.setId(5002L);
        child.setParentId(5001L);
        child.setDeleted(0);

        Organization grandChild = new Organization();
        grandChild.setId(5003L);
        grandChild.setParentId(5002L);
        grandChild.setDeleted(0);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));
        when(organizationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child, grandChild));

        Set<Long> writableOrgIds = permissionService.listWritableOrganizationIds(userId);

        assertEquals(Set.of(5001L, 5002L, 5003L), writableOrgIds);
    }

    @Test
    void shouldKeepSelfScopeAccessRangeAtCurrentOrganizationOnly() {
        Long userId = 2002L;

        User user = new User();
        user.setId(userId);
        user.setTenantId(1L);
        user.setOrgId(5002L);
        user.setDeleted(0);

        Role role = new Role();
        role.setId(3002L);
        role.setRoleCode("BUSINESS_STAFF");
        role.setDataScopeType("SELF");

        when(userMapper.selectById(userId)).thenReturn(user);
        when(userRoleMapper.selectRoleIdsByUserId(userId)).thenReturn(List.of(role.getId()));
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(role));

        Set<Long> writableOrgIds = permissionService.listWritableOrganizationIds(userId);

        assertEquals(Set.of(5002L), writableOrgIds);
    }
}
