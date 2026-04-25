package com.ghlzm.iot.device.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportDTO;
import com.ghlzm.iot.device.dto.NormativeMetricDefinitionImportItemDTO;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.vo.NormativeMetricDefinitionImportResultVO;
import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionCodes;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NormativeMetricDefinitionControllerTest {

    @Mock
    private NormativeMetricDefinitionService service;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private NormativeMetricDefinitionController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new NormativeMetricDefinitionController(service, permissionGuard);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void previewImportRouteShouldDeserializeRequestAndReturnSummary() throws Exception {
        NormativeMetricDefinitionImportResultVO result = result(1, 1, 0, 0);
        when(service.previewImport(any(NormativeMetricDefinitionImportDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/device/normative-metrics/import/preview")
                        .principal(authentication(1003L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "scenarioCode": "phase4-mud-level",
                                      "deviceFamily": "NF_MONITOR",
                                      "identifier": "value",
                                      "displayName": "泥水位",
                                      "unit": "m",
                                      "monitorContentCode": "L4",
                                      "monitorTypeCode": "NW"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.readyCount").value(1))
                .andExpect(jsonPath("$.data.conflictCount").value(0));

        ArgumentCaptor<NormativeMetricDefinitionImportDTO> captor =
                ArgumentCaptor.forClass(NormativeMetricDefinitionImportDTO.class);
        verify(service).previewImport(captor.capture());
        assertEquals("phase4-mud-level", captor.getValue().getItems().get(0).getScenarioCode());
        assertEquals("NW", captor.getValue().getItems().get(0).getMonitorTypeCode());
        verify(permissionGuard).requireAnyPermission(
                1003L,
                "规范字段库导入预检",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
    }

    @Test
    void applyImportRouteShouldDeserializeRequestAndReturnAppliedCount() throws Exception {
        NormativeMetricDefinitionImportResultVO result = result(1, 1, 0, 1);
        when(service.applyImport(any(NormativeMetricDefinitionImportDTO.class))).thenReturn(result);

        mockMvc.perform(post("/api/device/normative-metrics/import/apply")
                        .principal(authentication(1004L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "scenarioCode": "phase4-surface-flow-speed",
                                      "deviceFamily": "NF_MONITOR",
                                      "identifier": "value",
                                      "displayName": "表面流速",
                                      "unit": "m/s",
                                      "monitorContentCode": "L4",
                                      "monitorTypeCode": "BMLS"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.appliedCount").value(1));

        ArgumentCaptor<NormativeMetricDefinitionImportDTO> captor =
                ArgumentCaptor.forClass(NormativeMetricDefinitionImportDTO.class);
        verify(service).applyImport(captor.capture());
        assertEquals("phase4-surface-flow-speed", captor.getValue().getItems().get(0).getScenarioCode());
        assertEquals("BMLS", captor.getValue().getItems().get(0).getMonitorTypeCode());
        verify(permissionGuard).requireAnyPermission(
                1004L,
                "规范字段库导入落库",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
    }

    @Test
    void importRouteShouldReturnBizErrorWhenAuthenticationMissing() throws Exception {
        mockMvc.perform(post("/api/device/normative-metrics/import/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("未登录或登录状态已失效"));

        verifyNoInteractions(permissionGuard, service);
    }

    @Test
    void previewImportShouldRequireGovernancePermissionAndDelegate() {
        NormativeMetricDefinitionImportDTO request = importRequest();
        NormativeMetricDefinitionImportResultVO result = result(1, 1, 0, 0);
        when(service.previewImport(request)).thenReturn(result);

        R<NormativeMetricDefinitionImportResultVO> response = controller.previewImport(request, authentication(1001L));

        assertEquals(R.SUCCESS, response.getCode());
        assertSame(result, response.getData());
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "规范字段库导入预检",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(service).previewImport(request);
    }

    @Test
    void applyImportShouldRequireGovernancePermissionAndDelegate() {
        NormativeMetricDefinitionImportDTO request = importRequest();
        NormativeMetricDefinitionImportResultVO result = result(1, 1, 0, 1);
        when(service.applyImport(request)).thenReturn(result);

        R<NormativeMetricDefinitionImportResultVO> response = controller.applyImport(request, authentication(1002L));

        assertEquals(R.SUCCESS, response.getCode());
        assertSame(result, response.getData());
        verify(permissionGuard).requireAnyPermission(
                1002L,
                "规范字段库导入落库",
                GovernancePermissionCodes.PRODUCT_CONTRACT_GOVERN
        );
        verify(service).applyImport(request);
    }

    @Test
    void previewImportShouldRejectAnonymousAuthentication() {
        NormativeMetricDefinitionImportDTO request = importRequest();

        BizException exception = assertThrows(BizException.class, () -> controller.previewImport(request, null));

        assertEquals("未登录或登录状态已失效", exception.getMessage());
        verifyNoInteractions(permissionGuard, service);
    }

    @Test
    void applyImportShouldRejectNonJwtPrincipal() {
        NormativeMetricDefinitionImportDTO request = importRequest();
        Authentication authentication = new UsernamePasswordAuthenticationToken("anonymous", null, List.of());

        BizException exception = assertThrows(BizException.class, () -> controller.applyImport(request, authentication));

        assertEquals("未登录或登录状态已失效", exception.getMessage());
        verifyNoInteractions(permissionGuard, service);
    }

    private static NormativeMetricDefinitionImportDTO importRequest() {
        NormativeMetricDefinitionImportItemDTO item = new NormativeMetricDefinitionImportItemDTO();
        item.setScenarioCode("phase4-mud-level");
        item.setDeviceFamily("NF_MONITOR");
        item.setIdentifier("value");
        item.setDisplayName("泥水位");
        item.setUnit("m");
        item.setMonitorContentCode("L4");
        item.setMonitorTypeCode("NW");

        NormativeMetricDefinitionImportDTO request = new NormativeMetricDefinitionImportDTO();
        request.setItems(List.of(item));
        return request;
    }

    private static NormativeMetricDefinitionImportResultVO result(int total, int ready, int conflict, int applied) {
        NormativeMetricDefinitionImportResultVO result = new NormativeMetricDefinitionImportResultVO();
        result.setTotalCount(total);
        result.setReadyCount(ready);
        result.setConflictCount(conflict);
        result.setAppliedCount(applied);
        result.setRows(List.of());
        return result;
    }

    private static Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
