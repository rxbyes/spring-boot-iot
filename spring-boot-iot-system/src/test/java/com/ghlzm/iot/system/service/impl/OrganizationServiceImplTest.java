package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.enums.DataScopeType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private PermissionService permissionService;

    private OrganizationServiceImpl organizationService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Organization.class);
    }

    @BeforeEach
    void setUp() {
        organizationService = spy(new OrganizationServiceImpl(permissionService));
    }

    @Test
    void shouldFilterOrganizationPageToAccessibleSubtreeForOrgChildrenScope() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.ORG_AND_CHILDREN, false));
        when(permissionService.listAccessibleOrganizationIds(1L)).thenReturn(java.util.Set.of(7101L, 7102L));

        Page<Organization> page = new Page<>(1L, 10L);
        page.setRecords(java.util.List.of());
        page.setTotal(0L);
        doReturn(page).when(organizationService).page(any(Page.class), any(LambdaQueryWrapper.class));

        organizationService.pageOrganizations(1L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Organization>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("id"));
    }

    @Test
    void shouldFilterOrganizationTreeToCurrentNodeForSelfScope() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7102L, DataScopeType.SELF, false));
        doReturn(java.util.List.of()).when(organizationService).list(any(LambdaQueryWrapper.class));

        organizationService.listOrganizationTree(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Organization>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationService).list(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("id"));
    }

    @Test
    void shouldAnchorOrganizationListRootToCurrentOrganizationForSelfScope() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7102L, DataScopeType.SELF, false));
        doReturn(java.util.List.of()).when(organizationService).list(any(LambdaQueryWrapper.class));

        organizationService.listOrganizations(1L, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Organization>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(organizationService).list(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("id"));
        assertTrue(!sqlSegment.contains("parent_id"));
    }

    @Test
    void shouldReturnCurrentNodeAsTreeRootWhenParentOutsideScopedTree() {
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7102L, DataScopeType.SELF, false));

        Organization current = new Organization();
        current.setId(7102L);
        current.setParentId(7101L);
        current.setOrgName("告警处置组");

        doReturn(java.util.List.of(current)).when(organizationService).list(any(LambdaQueryWrapper.class));

        java.util.List<Organization> tree = organizationService.listOrganizationTree(1L);

        assertEquals(1, tree.size());
        assertEquals(7102L, tree.get(0).getId());
    }
}
