package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.InAppMessageBridgeQueryService;
import com.ghlzm.iot.system.service.InAppMessageService;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeStatsVO;
import com.ghlzm.iot.system.vo.InAppMessageUnreadStatsVO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InAppMessageControllerTest {

    @Mock
    private InAppMessageService inAppMessageService;

    @Mock
    private InAppMessageBridgeQueryService inAppMessageBridgeQueryService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InAppMessageController(
                        inAppMessageService,
                        inAppMessageBridgeQueryService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken(new JwtUserPrincipal(1L, "admin"), null);
    }

    @Test
    void shouldResolveAdminPageBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageService.pageMessages(null, null, null, null, null, null, 1L, 10L))
                .thenReturn(PageResult.empty(1L, 10L));

        mockMvc.perform(get("/api/system/in-app-message/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageService).pageMessages(null, null, null, null, null, null, 1L, 10L);
        verify(inAppMessageService, never()).getById(anyLong());
    }

    @Test
    void shouldResolveMyPageBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageService.pageMyMessages(1L, null, null, 1L, 5L))
                .thenReturn(PageResult.empty(1L, 5L));

        mockMvc.perform(get("/api/system/in-app-message/my/page")
                        .principal(authentication)
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageService).pageMyMessages(1L, null, null, 1L, 5L);
        verify(inAppMessageService, never()).getMyMessageDetail(anyLong(), anyLong());
    }

    @Test
    void shouldResolveUnreadCountBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageService.getMyUnreadStats(1L)).thenReturn(new InAppMessageUnreadStatsVO());

        mockMvc.perform(get("/api/system/in-app-message/my/unread-count")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageService).getMyUnreadStats(1L);
        verify(inAppMessageService, never()).getMyMessageDetail(anyLong(), anyLong());
    }

    @Test
    void shouldResolveBridgeStatsBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageBridgeQueryService.getBridgeStats(null, null, null, null, null, null, null))
                .thenReturn(new InAppMessageBridgeStatsVO());

        mockMvc.perform(get("/api/system/in-app-message/bridge/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageBridgeQueryService).getBridgeStats(null, null, null, null, null, null, null);
        verify(inAppMessageService, never()).getById(anyLong());
    }

    @Test
    void shouldResolveBridgePageBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageBridgeQueryService.pageBridgeLogs(null, null, null, null, null, null, null, 1L, 10L))
                .thenReturn(PageResult.empty(1L, 10L));

        mockMvc.perform(get("/api/system/in-app-message/bridge/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageBridgeQueryService).pageBridgeLogs(null, null, null, null, null, null, null, 1L, 10L);
        verify(inAppMessageService, never()).getById(anyLong());
    }

    @Test
    void shouldResolveBridgeAttemptsBeforeNumericDetailRoute() throws Exception {
        when(inAppMessageBridgeQueryService.listBridgeAttempts(1L)).thenReturn(List.of(new InAppMessageBridgeAttemptVO()));

        mockMvc.perform(get("/api/system/in-app-message/bridge/1/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inAppMessageBridgeQueryService).listBridgeAttempts(1L);
        verify(inAppMessageService, never()).getById(anyLong());
    }
}
