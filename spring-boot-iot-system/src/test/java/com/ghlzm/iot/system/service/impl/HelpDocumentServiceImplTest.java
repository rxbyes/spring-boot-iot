package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.mapper.HelpDocumentMapper;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
import com.ghlzm.iot.system.vo.MenuTreeNodeVO;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpDocumentServiceImplTest {

    @Mock
    private HelpDocumentMapper helpDocumentMapper;
    @Mock
    private PermissionService permissionService;

    private HelpDocumentServiceImpl helpDocumentService;

    @BeforeEach
    void setUp() {
        helpDocumentService = new HelpDocumentServiceImpl(helpDocumentMapper, permissionService);
    }

    @Test
    void shouldFilterAccessibleDocumentsAndPrioritizeCurrentPathMatch() {
        Long userId = 2L;
        when(permissionService.getUserAuthContext(userId)).thenReturn(authContext(
                List.of("BUSINESS_STAFF"),
                List.of(menuNode("/alarm-center"), menuNode("/devices"))
        ));
        when(helpDocumentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                document(401L, "business", "告警处置指南", "BUSINESS_STAFF", "/alarm-center"),
                document(402L, "faq", "通用常见问题", null, null),
                document(403L, "technical", "通知渠道联调", "DEVELOPER_STAFF", "/channel"),
                document(404L, "business", "事件协同手册", "BUSINESS_STAFF", "/event-disposal")
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
                List.of("BUSINESS_STAFF"),
                List.of(menuNode("/alarm-center"))
        ));

        assertThrows(BizException.class, () -> helpDocumentService.getAccessibleDocument(userId, 501L, "/channel"));
    }

    private UserAuthContextVO authContext(List<String> roleCodes, List<MenuTreeNodeVO> menus) {
        UserAuthContextVO authContext = new UserAuthContextVO();
        authContext.setUserId(2L);
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
        HelpDocument document = new HelpDocument();
        document.setId(id);
        document.setTenantId(1L);
        document.setDocCategory(docCategory);
        document.setTitle(title);
        document.setSummary(title + "-摘要");
        document.setContent(title + "-正文");
        document.setVisibleRoleCodes(visibleRoleCodes);
        document.setRelatedPaths(relatedPaths);
        document.setStatus(1);
        document.setSortNo(0);
        return document;
    }
}
