package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.mapper.HelpDocumentMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpDocumentServiceImplTest {

    @Mock
    private HelpDocumentMapper helpDocumentMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private SystemContentSchemaSupport systemContentSchemaSupport;
    @Mock
    private SystemDictValueSupport systemDictValueSupport;

    private HelpDocumentServiceImpl helpDocumentService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, HelpDocument.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        helpDocumentService = spy(new HelpDocumentServiceImpl(
                helpDocumentMapper,
                permissionService,
                systemContentSchemaSupport,
                systemDictValueSupport
        ));
        Field field = findField(helpDocumentService.getClass(), "baseMapper");
        field.setAccessible(true);
        field.set(helpDocumentService, helpDocumentMapper);
    }

    @Test
    void shouldFilterAccessibleDocumentsAndPrioritizeCurrentPathMatch() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(
                1L,
                List.of("BUSINESS_STAFF"),
                List.of(menuNode("/alarm-center"), menuNode("/devices"))
        ));
        when(helpDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                document(401L, 1L, "business", "告警处置指南", "BUSINESS_STAFF", "/alarm-center"),
                document(402L, "faq", "通用常见问题", null, null),
                document(403L, 2L, "business", "跨租户告警指南", "BUSINESS_STAFF", "/alarm-center"),
                document(404L, "technical", "通知渠道联调", "DEVELOPER_STAFF", "/channel"),
                document(405L, "business", "事件协同手册", "BUSINESS_STAFF", "/event-disposal")
        ));

        List<HelpDocumentAccessVO> result = helpDocumentService.listAccessibleDocuments(userId, null, null, "/alarm-center", 10);

        assertEquals(2, result.size());
        assertEquals(401L, result.get(0).getId());
        assertTrue(result.get(0).isCurrentPathMatched());
        assertEquals(402L, result.get(1).getId());
    }

    @Test
    void shouldRejectDocumentWhenCurrentUserHasNoAuthorizedRelatedPath() {
        Long userId = 2L;
        HelpDocument document = document(501L, "business", "通知编排说明", "BUSINESS_STAFF", "/channel");

        when(helpDocumentMapper.selectById(501L)).thenReturn(document);
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(
                1L,
                List.of("BUSINESS_STAFF"),
                List.of(menuNode("/alarm-center"))
        ));

        assertThrows(BizException.class, () -> helpDocumentService.getAccessibleDocument(userId, 501L, "/channel"));
    }

    @Test
    void shouldThrowSchemaHintWhenHelpDocumentTableMissing() {
        doThrow(new BizException(SystemContentSchemaSupport.SCHEMA_HINT))
                .when(systemContentSchemaSupport)
                .ensureHelpDocumentReady();

        BizException exception = assertThrows(BizException.class,
                () -> helpDocumentService.listAccessibleDocuments(2L, null, null, "/alarm-center", 10));

        assertEquals(SystemContentSchemaSupport.SCHEMA_HINT, exception.getMessage());
    }

    @Test
    void shouldPageAccessibleDocumentsWithKeywordAndCurrentPathPriority() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(
                1L,
                List.of("BUSINESS_STAFF"),
                List.of(menuNode("/alarm-center"), menuNode("/devices"), menuNode("/report-analysis"))
        ));
        when(helpDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                document(601L, "business", "告警处置指南", "BUSINESS_STAFF", "/alarm-center", "告警,处置"),
                document(602L, "business", "设备巡检记录", "BUSINESS_STAFF", "/devices", "设备,巡检"),
                document(603L, "business", "告警复盘口径", null, "/report-analysis", "告警,复盘"),
                document(604L, "technical", "通知联调", "DEVELOPER_STAFF", "/channel", "通知")
        ));

        PageResult<HelpDocumentAccessVO> result = helpDocumentService.pageAccessibleDocuments(
                userId,
                "business",
                "告警",
                "/alarm-center",
                1L,
                1L
        );

        assertEquals(2L, result.getTotal());
        assertEquals(1L, result.getPageNum());
        assertEquals(1L, result.getPageSize());
        assertEquals(1, result.getRecords().size());
        assertEquals(601L, result.getRecords().get(0).getId());
        assertTrue(result.getRecords().get(0).isCurrentPathMatched());
    }

    @Test
    void shouldFilterScopedHelpDocumentAdminPageToCurrentTenant() throws Exception {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));
        when(helpDocumentMapper.selectPage(org.mockito.ArgumentMatchers.any(Page.class),
                org.mockito.ArgumentMatchers.any(LambdaQueryWrapper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invokeScopedPageDocuments(99L, null, null, null, 1L, 10L);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<LambdaQueryWrapper<HelpDocument>> wrapperCaptor =
                org.mockito.ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(helpDocumentMapper).selectPage(org.mockito.ArgumentMatchers.any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void shouldRejectCrossTenantHelpDocumentDetailAccess() throws Exception {
        HelpDocument document = document(801L, 2L, "business", "跨租户帮助文档", null, null);
        when(helpDocumentMapper.selectById(801L)).thenReturn(document);
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class, () -> invokeScopedAdminGetById(99L, 801L));
        assertEquals("帮助文档不存在或无权访问", exception.getMessage());
    }

    @Test
    void shouldDeleteHelpDocumentViaLogicDeleteOperation() {
        HelpDocument existing = document(701L, "business", "帮助文档删除", null, null);
        when(helpDocumentMapper.selectById(701L)).thenReturn(existing);
        when(helpDocumentMapper.deleteById(701L)).thenReturn(1);
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.TENANT, false));

        helpDocumentService.deleteDocument(701L, 1L);

        verify(helpDocumentService).removeById(701L);
        verify(helpDocumentMapper, never()).updateById(any(HelpDocument.class));
    }

    @Test
    void shouldThrowWhenHelpDocumentLogicDeleteFails() {
        HelpDocument existing = document(702L, "business", "帮助文档删除失败", null, null);
        when(helpDocumentMapper.selectById(702L)).thenReturn(existing);
        when(helpDocumentMapper.deleteById(702L)).thenReturn(0);
        when(permissionService.getDataPermissionContext(1L))
                .thenReturn(new DataPermissionContext(1L, 1L, 7101L, DataScopeType.TENANT, false));

        BizException exception = assertThrows(BizException.class,
                () -> helpDocumentService.deleteDocument(702L, 1L));

        assertEquals("帮助文档删除失败", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownHelpDocumentCategory() {
        HelpDocument document = new HelpDocument();
        document.setTitle("帮助文档");
        document.setContent("正文");
        document.setDocCategory("whitepaper");

        when(systemDictValueSupport.normalizeRequiredLowerCase(
                eq(null),
                eq("help_doc_category"),
                eq("whitepaper"),
                eq("文档分类"),
                eq(Set.of("business", "technical", "faq"))
        )).thenThrow(new BizException("文档分类不合法"));

        BizException exception = assertThrows(BizException.class, () -> helpDocumentService.addDocument(document, null));
        assertEquals("文档分类不合法", exception.getMessage());
    }

    private UserAuthContextVO authContext(Long tenantId, List<String> roleCodes, List<MenuTreeNodeVO> menus) {
        UserAuthContextVO authContext = new UserAuthContextVO();
        authContext.setUserId(2L);
        authContext.setTenantId(tenantId);
        authContext.setRoleCodes(roleCodes);
        authContext.setMenus(menus);
        return authContext;
    }

    private MenuTreeNodeVO menuNode(String path) {
        MenuTreeNodeVO menu = new MenuTreeNodeVO();
        menu.setPath(path);
        return menu;
    }

    private HelpDocument document(Long id,
                                  String docCategory,
                                  String title,
                                  String visibleRoleCodes,
                                  String relatedPaths) {
        return document(id, 1L, docCategory, title, visibleRoleCodes, relatedPaths, null);
    }

    private HelpDocument document(Long id,
                                  Long tenantId,
                                  String docCategory,
                                  String title,
                                  String visibleRoleCodes,
                                  String relatedPaths) {
        return document(id, tenantId, docCategory, title, visibleRoleCodes, relatedPaths, null);
    }

    private HelpDocument document(Long id,
                                  String docCategory,
                                  String title,
                                  String visibleRoleCodes,
                                  String relatedPaths,
                                  String keywords) {
        return document(id, 1L, docCategory, title, visibleRoleCodes, relatedPaths, keywords);
    }

    private HelpDocument document(Long id,
                                  Long tenantId,
                                  String docCategory,
                                  String title,
                                  String visibleRoleCodes,
                                  String relatedPaths,
                                  String keywords) {
        HelpDocument document = new HelpDocument();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setDocCategory(docCategory);
        document.setTitle(title);
        document.setSummary(title + "-摘要");
        document.setContent(title + "-正文");
        document.setKeywords(keywords);
        document.setVisibleRoleCodes(visibleRoleCodes);
        document.setRelatedPaths(relatedPaths);
        document.setStatus(1);
        document.setSortNo(0);
        return document;
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

    private Object invokeScopedPageDocuments(Long currentUserId,
                                             String title,
                                             String docCategory,
                                             Integer status,
                                             Long pageNum,
                                             Long pageSize) throws Exception {
        try {
            Method method = HelpDocumentServiceImpl.class.getMethod(
                    "pageDocuments",
                    Long.class,
                    String.class,
                    String.class,
                    Integer.class,
                    Long.class,
                    Long.class
            );
            return method.invoke(helpDocumentService, currentUserId, title, docCategory, status, pageNum, pageSize);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("scoped pageDocuments overload is missing", exception);
        } catch (InvocationTargetException exception) {
            throw unwrap(exception);
        }
    }

    private HelpDocument invokeScopedAdminGetById(Long currentUserId, Long id) throws Exception {
        try {
            Method method = HelpDocumentServiceImpl.class.getMethod("getById", Long.class, Long.class);
            return (HelpDocument) method.invoke(helpDocumentService, currentUserId, id);
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
