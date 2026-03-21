package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.entity.HelpDocument;
import com.ghlzm.iot.system.service.HelpDocumentService;
import com.ghlzm.iot.system.vo.HelpDocumentAccessVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HelpDocumentControllerTest {

    @Mock
    private HelpDocumentService helpDocumentService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HelpDocumentController(helpDocumentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken(new JwtUserPrincipal(1L, "admin"), null);
    }

    @Test
    void shouldResolveAdminPageBeforeNumericDetailRoute() throws Exception {
        when(helpDocumentService.pageDocuments(null, null, null, 1L, 10L))
                .thenReturn(PageResult.empty(1L, 10L));

        mockMvc.perform(get("/api/system/help-doc/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(helpDocumentService).pageDocuments(null, null, null, 1L, 10L);
        verify(helpDocumentService, never()).getById(anyLong());
    }

    @Test
    void shouldResolveAccessPageBeforeNumericDetailRoute() throws Exception {
        when(helpDocumentService.pageAccessibleDocuments(1L, null, null, "/system-management", 1L, 5L))
                .thenReturn(PageResult.of(1L, 1L, 5L, List.of(new HelpDocumentAccessVO())));

        mockMvc.perform(get("/api/system/help-doc/access/page")
                        .principal(authentication)
                        .param("currentPath", "/system-management")
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(helpDocumentService).pageAccessibleDocuments(1L, null, null, "/system-management", 1L, 5L);
        verify(helpDocumentService, never()).getAccessibleDocument(anyLong(), anyLong(), anyString());
    }
}
