package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingBatchActivateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingActivationServiceImplTest {

    @Mock
    private DeviceService deviceService;

    private DeviceOnboardingActivationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceOnboardingActivationServiceImpl(deviceService);
    }

    @Test
    void activateShouldRejectWhenSuggestionNotConfirmed() {
        DeviceOnboardingBatchActivateDTO dto = new DeviceOnboardingBatchActivateDTO();
        dto.setTraceIds(List.of("trace-unregistered-001"));
        dto.setConfirmed(Boolean.FALSE);

        BizException error = assertThrows(BizException.class, () -> service.activate(10001L, dto));

        assertEquals("请先确认接入建议", error.getMessage());
        verify(deviceService, never()).getOnboardingSuggestion(eq(10001L), any(DeviceOnboardingSuggestionQuery.class));
        verify(deviceService, never()).addDevice(eq(10001L), any(DeviceAddDTO.class));
    }

    @Test
    void activateShouldCreateReadySuggestionsAndCollectRejectedRows() {
        DeviceOnboardingBatchActivateDTO dto = new DeviceOnboardingBatchActivateDTO();
        dto.setTraceIds(List.of("trace-unregistered-001", "trace-unregistered-002"));
        dto.setConfirmed(Boolean.TRUE);

        when(deviceService.getOnboardingSuggestion(eq(10001L), any(DeviceOnboardingSuggestionQuery.class)))
                .thenAnswer(invocation -> {
                    DeviceOnboardingSuggestionQuery query = invocation.getArgument(1, DeviceOnboardingSuggestionQuery.class);
                    if ("trace-unregistered-001".equals(query.getTraceId())) {
                        DeviceOnboardingSuggestionVO ready = new DeviceOnboardingSuggestionVO();
                        ready.setTraceId("trace-unregistered-001");
                        ready.setDeviceCode("shadow-device-01");
                        ready.setDeviceName("裂缝计-01");
                        ready.setRecommendedProductKey("south_rtu");
                        ready.setSuggestionStatus("READY");
                        ready.setRuleGaps(List.of());
                        return ready;
                    }
                    DeviceOnboardingSuggestionVO partial = new DeviceOnboardingSuggestionVO();
                    partial.setTraceId("trace-unregistered-002");
                    partial.setDeviceCode("shadow-device-02");
                    partial.setRecommendedProductKey("south_rtu");
                    partial.setSuggestionStatus("PARTIAL");
                    partial.setRuleGaps(List.of("推荐产品尚未形成正式合同发布，转正前需先发布契约字段。"));
                    return partial;
                });

        DeviceDetailVO created = new DeviceDetailVO();
        created.setId(9101L);
        created.setDeviceCode("shadow-device-01");
        created.setDeviceName("裂缝计-01");
        when(deviceService.addDevice(eq(10001L), any(DeviceAddDTO.class))).thenReturn(created);

        DeviceOnboardingBatchResultVO result = service.activate(10001L, dto);

        assertEquals(2, result.getRequestedCount());
        assertEquals(1, result.getActivatedCount());
        assertEquals(1, result.getRejectedCount());
        assertEquals(List.of("trace-unregistered-001"), result.getActivatedTraceIds());
        assertEquals(List.of("shadow-device-01"), result.getActivatedDeviceCodes());
        assertEquals(1, result.getErrors().size());
        assertEquals("trace-unregistered-002", result.getErrors().get(0).getTraceId());
        assertEquals("shadow-device-02", result.getErrors().get(0).getDeviceCode());
        assertEquals("推荐产品尚未形成正式合同发布，转正前需先发布契约字段。", result.getErrors().get(0).getMessage());

        ArgumentCaptor<DeviceAddDTO> payloadCaptor = ArgumentCaptor.forClass(DeviceAddDTO.class);
        verify(deviceService).addDevice(eq(10001L), payloadCaptor.capture());
        assertEquals("south_rtu", payloadCaptor.getValue().getProductKey());
        assertEquals("裂缝计-01", payloadCaptor.getValue().getDeviceName());
        assertEquals("shadow-device-01", payloadCaptor.getValue().getDeviceCode());
        assertEquals(1, payloadCaptor.getValue().getActivateStatus());
        assertEquals(1, payloadCaptor.getValue().getDeviceStatus());
    }
}
