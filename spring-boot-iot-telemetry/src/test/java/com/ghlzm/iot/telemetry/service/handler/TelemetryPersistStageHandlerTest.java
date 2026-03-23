package com.ghlzm.iot.telemetry.service.handler;

import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceFilePayload;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.telemetry.service.impl.TdengineTelemetryFacade;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryPersistStageHandlerTest {

    @Mock
    private TdengineTelemetryFacade tdengineTelemetryFacade;

    private IotProperties iotProperties;
    private TelemetryPersistStageHandler telemetryPersistStageHandler;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        telemetryPersistStageHandler = new TelemetryPersistStageHandler(iotProperties, tdengineTelemetryFacade);
    }

    @Test
    void persistShouldSkipWhenStorageTypeIsNotTdengine() {
        iotProperties.getTelemetry().setStorageType("mysql");

        TelemetryPersistResult result = telemetryPersistStageHandler.persist(buildTarget("property", Map.of("temperature", 26.5), false));

        assertTrue(result.isSkipped());
        assertEquals("STORAGE_TYPE_MYSQL", result.getBranch());
        verifyNoInteractions(tdengineTelemetryFacade);
    }

    @Test
    void persistShouldDelegateToTdengineForPropertyPayload() {
        iotProperties.getTelemetry().setStorageType("tdengine");
        iotProperties.getTelemetry().setTdengineMode("legacy-compatible");
        DeviceProcessingTarget target = buildTarget("property", Map.of("temperature", 26.5, "humidity", 68), false);
        when(tdengineTelemetryFacade.persist(target)).thenReturn(
                TelemetryPersistResult.persisted("LEGACY_COMPATIBLE", "legacy-compatible", 2, 1, 2, 0, 0)
        );

        TelemetryPersistResult result = telemetryPersistStageHandler.persist(target);

        assertFalse(result.isSkipped());
        assertEquals(2, result.getPointCount());
        assertEquals("legacy-compatible", result.getStorageMode());
        verify(tdengineTelemetryFacade).persist(target);
    }

    @Test
    void persistShouldSkipReplyFileAndEmptyProperties() {
        iotProperties.getTelemetry().setStorageType("tdengine");

        TelemetryPersistResult replyResult = telemetryPersistStageHandler.persist(buildTarget("reply", Map.of("status", "ok"), false));
        TelemetryPersistResult fileResult = telemetryPersistStageHandler.persist(buildTarget("property", Map.of("temperature", 26.5), true));
        TelemetryPersistResult emptyResult = telemetryPersistStageHandler.persist(buildTarget("property", Map.of(), false));

        assertEquals("MESSAGE_TYPE_REPLY", replyResult.getBranch());
        assertEquals("FILE_PAYLOAD", fileResult.getBranch());
        assertEquals("EMPTY_PROPERTIES", emptyResult.getBranch());
        verifyNoInteractions(tdengineTelemetryFacade);
    }

    private DeviceProcessingTarget buildTarget(String messageType, Map<String, Object> properties, boolean filePayload) {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage message = new DeviceUpMessage();
        message.setProductKey("demo-product");
        message.setDeviceCode("demo-device-01");
        message.setProtocolCode("mqtt-json");
        message.setTraceId("trace-001");
        message.setMessageType(messageType);
        message.setTopic("/sys/demo-product/demo-device-01/thing/property/post");
        message.setProperties(properties);
        if (filePayload) {
            message.setFilePayload(new DeviceFilePayload());
        }

        DeviceProcessingTarget target = new DeviceProcessingTarget();
        target.setDevice(device);
        target.setProduct(product);
        target.setMessage(message);
        return target;
    }
}
