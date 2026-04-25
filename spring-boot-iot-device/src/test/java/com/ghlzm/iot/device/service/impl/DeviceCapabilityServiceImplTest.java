package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.DeviceCapabilityRegistry;
import com.ghlzm.iot.device.capability.ProductCapabilityMetadataParser;
import com.ghlzm.iot.device.dto.DeviceCapabilityExecuteDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceCapabilityCommandGateway;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandResult;
import com.ghlzm.iot.device.vo.CommandRecordPageItemVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityExecuteResultVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityOverviewVO;
import com.ghlzm.iot.device.vo.DeviceCapabilityVO;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCapabilityServiceImplTest {

    private static final class RecordingEvidenceRecorder implements ObservabilityEvidenceRecorder {
        private final AtomicReference<BusinessEventLogRecord> lastEvent = new AtomicReference<>();

        @Override
        public void recordBusinessEvent(BusinessEventLogRecord event) {
            lastEvent.set(event);
        }
    }

    @Mock
    private DeviceService deviceService;
    @Mock
    private ProductService productService;
    @Mock
    private DeviceCapabilityCommandGateway deviceCapabilityCommandGateway;
    @Mock
    private CommandRecordService commandRecordService;

    @Test
    void getCapabilitiesShouldExposeWarningBroadcastCapabilities() {
        Device device = device("warning-device-01", 1001L, 1, 1, 1);
        Product product = product("warning-product", ProductStatusEnum.ENABLED.getCode(), """
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);
        when(deviceService.getRequiredByCode(7L, "warning-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        DeviceCapabilityServiceImpl service = new DeviceCapabilityServiceImpl(
                deviceService,
                productService,
                new ProductCapabilityMetadataParser(),
                new DeviceCapabilityRegistry(),
                deviceCapabilityCommandGateway,
                commandRecordService
        );

        DeviceCapabilityOverviewVO overview = service.getCapabilities(7L, "warning-device-01");

        assertEquals("warning-device-01", overview.getDeviceCode());
        assertEquals("WARNING", overview.getProductCapabilityType());
        assertEquals("BROADCAST", overview.getSubType());
        assertTrue(overview.isOnlineExecutable());
        assertEquals(List.of("broadcast_play", "broadcast_stop", "broadcast_volume", "reboot"),
                overview.getCapabilities().stream().map(DeviceCapabilityVO::getCode).toList());
        assertTrue(overview.getCapabilities().stream().allMatch(DeviceCapabilityVO::isEnabled));
    }

    @Test
    void executeShouldDelegateToGatewayAndMapResult() {
        Device device = device("warning-device-01", 1001L, 1, 1, 1);
        Product product = product("warning-product", ProductStatusEnum.ENABLED.getCode(), """
                {"governance":{"productCapabilityType":"WARNING","warningDeviceKind":"BROADCAST"}}
                """);
        when(deviceService.getRequiredByCode(7L, "warning-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        DeviceCapabilityCommandResult commandResult = new DeviceCapabilityCommandResult();
        commandResult.setCommandId("1776999000000");
        commandResult.setDeviceCode("warning-device-01");
        commandResult.setCapabilityCode("broadcast_play");
        commandResult.setStatus("SENT");
        commandResult.setTopic("/iot/broadcast/warning-device-01");
        commandResult.setSentAt(LocalDateTime.parse("2026-04-24T10:50:00"));
        when(deviceCapabilityCommandGateway.execute(any(DeviceCapabilityCommandRequest.class))).thenReturn(commandResult);

        DeviceCapabilityServiceImpl service = new DeviceCapabilityServiceImpl(
                deviceService,
                productService,
                new ProductCapabilityMetadataParser(),
                new DeviceCapabilityRegistry(),
                deviceCapabilityCommandGateway,
                commandRecordService
        );
        RecordingEvidenceRecorder evidenceRecorder = new RecordingEvidenceRecorder();
        service.setObservabilityEvidenceRecorder(evidenceRecorder);

        DeviceCapabilityExecuteDTO dto = new DeviceCapabilityExecuteDTO();
        dto.setParams(Map.of("content", "road-work", "bNum", 1, "volume", 80));
        DeviceCapabilityExecuteResultVO result = service.execute(7L, "warning-device-01", "broadcast_play", dto);

        assertEquals("1776999000000", result.getCommandId());
        assertEquals("broadcast_play", result.getCapabilityCode());
        assertEquals("SENT", result.getStatus());
        assertEquals("/iot/broadcast/warning-device-01", result.getTopic());
        assertNotNull(result.getSentAt());

        ArgumentCaptor<DeviceCapabilityCommandRequest> requestCaptor = ArgumentCaptor.forClass(DeviceCapabilityCommandRequest.class);
        verify(deviceCapabilityCommandGateway).execute(requestCaptor.capture());
        assertEquals("warning-device-01", requestCaptor.getValue().getDevice().getDeviceCode());
        assertEquals("broadcast_play", requestCaptor.getValue().getCapability().code());
        assertEquals("road-work", requestCaptor.getValue().getParams().get("content"));

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertNotNull(event);
        assertEquals("device.command.issued", event.getEventCode());
        assertEquals("device_operation", event.getDomainCode());
        assertEquals("device", event.getObjectType());
        assertEquals("warning-device-01", event.getObjectId());
        assertEquals("1776999000000", event.getEvidenceId());
        assertEquals("warning-product", event.getMetadata().get("productKey"));
        assertEquals("broadcast_play", event.getMetadata().get("capabilityCode"));
    }

    @Test
    void pageCommandsShouldDelegateToCommandRecordService() {
        PageResult<CommandRecordPageItemVO> pageResult = PageResult.of(1L, 1L, 10L, List.of());
        when(commandRecordService.pageByDevice(7L, "warning-device-01", "broadcast_play", "SENT", 1L, 10L))
                .thenReturn(pageResult);

        DeviceCapabilityServiceImpl service = new DeviceCapabilityServiceImpl(
                deviceService,
                productService,
                new ProductCapabilityMetadataParser(),
                new DeviceCapabilityRegistry(),
                deviceCapabilityCommandGateway,
                commandRecordService
        );

        PageResult<CommandRecordPageItemVO> result = service.pageCommands(7L, "warning-device-01", "broadcast_play", "SENT", 1L, 10L);

        assertEquals(1L, result.getTotal());
    }

    private Device device(String code, Long productId, Integer onlineStatus, Integer activateStatus, Integer deviceStatus) {
        Device device = new Device();
        device.setId(2001L);
        device.setDeviceCode(code);
        device.setProductId(productId);
        device.setOnlineStatus(onlineStatus);
        device.setActivateStatus(activateStatus);
        device.setDeviceStatus(deviceStatus);
        return device;
    }

    private Product product(String productKey, Integer status, String metadataJson) {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey(productKey);
        product.setStatus(status);
        product.setMetadataJson(metadataJson);
        return product;
    }
}
