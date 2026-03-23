package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.device.vo.DeviceStatsBucketVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMessageServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceMessageLogMapper deviceMessageLogMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModelMapper productModelMapper;
    @Mock
    private CommandRecordService commandRecordService;
    @Mock
    private DeviceFileService deviceFileService;
    @Mock
    private DeviceOnlineSessionService deviceOnlineSessionService;
    @Mock
    private DeviceSessionService deviceSessionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private DeviceMessageServiceImpl deviceMessageService;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setActivateDefault(true);
        iotProperties.setDevice(deviceConfig);
        DeviceContractStageHandler deviceContractStageHandler =
                new DeviceContractStageHandler(deviceMapper, productMapper);
        DeviceMessageLogStageHandler deviceMessageLogStageHandler =
                new DeviceMessageLogStageHandler(deviceMessageLogMapper);
        DevicePropertyMetadataService devicePropertyMetadataService =
                new DevicePropertyMetadataServiceImpl(productModelMapper);
        DevicePayloadApplyStageHandler devicePayloadApplyStageHandler =
                new DevicePayloadApplyStageHandler(
                        devicePropertyMapper,
                        devicePropertyMetadataService,
                        commandRecordService,
                        deviceFileService
                );
        DeviceStateStageHandler deviceStateStageHandler =
                new DeviceStateStageHandler(
                        deviceMapper,
                        deviceOnlineSessionService,
                        deviceSessionService,
                        iotProperties
                );
        DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler =
                new DeviceRiskDispatchStageHandler(eventPublisher);
        deviceMessageService = new DeviceMessageServiceImpl(
                deviceMapper,
                deviceMessageLogMapper,
                devicePropertyMapper,
                productMapper,
                productModelMapper,
                commandRecordService,
                deviceFileService,
                deviceOnlineSessionService,
                jdbcTemplate,
                iotProperties,
                eventPublisher,
                deviceContractStageHandler,
                deviceMessageLogStageHandler,
                devicePayloadApplyStageHandler,
                deviceStateStageHandler,
                deviceRiskDispatchStageHandler
        );
    }

    @Test
    void handleUpMessageShouldPersistLogAndPropertyAndOnlineStatus() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel propertyModel = new ProductModel();
        propertyModel.setIdentifier("temperature");
        propertyModel.setModelName("temperature");
        propertyModel.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(propertyModel));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-01",
                Map.of("temperature", 26.5), "property", "/sys/demo-product/demo-device-01/thing/property/post");

        deviceMessageService.handleUpMessage(upMessage);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper).insert(logCaptor.capture());
        assertEquals("/sys/demo-product/demo-device-01/thing/property/post", logCaptor.getValue().getTopic());
        assertEquals("property", logCaptor.getValue().getMessageType());

        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).insert(propertyCaptor.capture());
        assertEquals("temperature", propertyCaptor.getValue().getIdentifier());
        assertEquals("temperature", propertyCaptor.getValue().getPropertyName());
        assertEquals("double", propertyCaptor.getValue().getValueType());
        assertEquals("26.5", propertyCaptor.getValue().getPropertyValue());
        verify(devicePropertyMapper, never()).updateById(any(DeviceProperty.class));
        verify(deviceOnlineSessionService).recordOnlineHeartbeat(any(Device.class), any(LocalDateTime.class));

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).updateById(deviceCaptor.capture());
        assertEquals(1, deviceCaptor.getValue().getOnlineStatus());
        assertEquals(1, deviceCaptor.getValue().getActivateStatus());

        ArgumentCaptor<DeviceRiskEvaluationEvent> eventCaptor = ArgumentCaptor.forClass(DeviceRiskEvaluationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("demo-device-01", eventCaptor.getValue().getDeviceCode());
        assertEquals("26.5", String.valueOf(eventCaptor.getValue().getProperties().get("temperature")));
    }

    @Test
    void handleUpMessageShouldUpdateExistingPropertyWhenModelMissing() {
        Device device = new Device();
        device.setId(2002L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-02");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        DeviceProperty existing = new DeviceProperty();
        existing.setId(1L);
        existing.setDeviceId(2002L);
        existing.setIdentifier("humidity");
        existing.setPropertyName("humidity");
        existing.setValueType("integer");
        existing.setCreateTime(LocalDateTime.now().minusDays(1));

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(devicePropertyMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("humidity", 68);
        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-02",
                properties, "property", "/sys/demo-product/demo-device-02/thing/property/post");

        deviceMessageService.handleUpMessage(upMessage);

        verify(devicePropertyMapper, never()).insert(any(DeviceProperty.class));
        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).updateById(propertyCaptor.capture());
        assertEquals("humidity", propertyCaptor.getValue().getPropertyName());
        assertEquals("int", propertyCaptor.getValue().getValueType());
        assertEquals("68", propertyCaptor.getValue().getPropertyValue());
    }

    @Test
    void handleUpMessageShouldThrowWhenDeviceMissing() {
        when(deviceMapper.selectOne(any())).thenReturn(null);
        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "missing-device",
                Map.of("temperature", 25), "property", "/sys/demo-product/missing-device/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备不存在: missing-device", ex.getMessage());
        verifyNoInteractions(productMapper, productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldThrowWhenProtocolMismatch() {
        Device device = new Device();
        device.setId(2003L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-03");
        device.setProtocolCode("tcp-hex");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("tcp-hex");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-03",
                Map.of("temperature", 25), "property", "/sys/demo-product/demo-device-03/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备协议不匹配: demo-device-03, expected=tcp-hex, actual=mqtt-json", ex.getMessage());
        verify(productMapper).selectById(1001L);
        verifyNoInteractions(productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldThrowWhenDeviceProductUnboundBeforeProtocolValidation() {
        Device device = new Device();
        device.setId(2006L);
        device.setTenantId(1L);
        device.setProductId(null);
        device.setDeviceCode("demo-device-06");
        device.setProtocolCode("");

        when(deviceMapper.selectOne(any())).thenReturn(device);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-06",
                Map.of("temperature", 25), "property", "$dp");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备未绑定产品: demo-device-06", ex.getMessage());
        verifyNoInteractions(productMapper, productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldFallbackToProductProtocolWhenDeviceProtocolBlank() {
        Device device = new Device();
        device.setId(2007L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-07");
        device.setProtocolCode("");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel propertyModel = new ProductModel();
        propertyModel.setIdentifier("temperature");
        propertyModel.setModelName("temperature");
        propertyModel.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(propertyModel));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-07",
                Map.of("temperature", 26.5), "property", "$dp");

        deviceMessageService.handleUpMessage(upMessage);

        verify(deviceMessageLogMapper).insert(any(DeviceMessageLog.class));
        verify(devicePropertyMapper).insert(any(DeviceProperty.class));
        verify(deviceOnlineSessionService).recordOnlineHeartbeat(any(Device.class), any(LocalDateTime.class));
    }

    @Test
    void recordDispatchFailureTraceShouldNormalizeTrailingJsonJunkBeforePersist() throws Exception {
        byte[] payload = """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}}
                """.getBytes(StandardCharsets.UTF_8);

        deviceMessageService.recordDispatchFailureTrace("$dp", payload, null);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper).insert(logCaptor.capture());
        String storedPayload = logCaptor.getValue().getPayload();
        assertEquals(
                "{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}",
                JsonMapper.builder().findAndAddModules().build().readTree(storedPayload).toString()
        );
    }

    @Test
    void handleUpMessageShouldThrowWhenProductDisabled() {
        Device device = new Device();
        device.setId(2005L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-05");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.DISABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-05",
                Map.of("temperature", 25), "property", "/sys/demo-product/demo-device-05/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("产品已停用，拒绝设备接入: demo-product", ex.getMessage());
        verifyNoInteractions(productModelMapper, deviceMessageLogMapper, devicePropertyMapper, deviceFileService);
    }

    @Test
    void handleUpMessageShouldFillCommandStatusWhenReplyArrives() {
        Device device = new Device();
        device.setId(2004L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-04");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(commandRecordService.markSuccessByCommandId(any(), any(), any())).thenReturn(true);

        DeviceUpMessage upMessage = buildMessage(
                "mqtt-json",
                "demo-product",
                "demo-device-04",
                Map.of(),
                "reply",
                "/sys/demo-product/demo-device-04/thing/property/reply"
        );
        upMessage.setRawPayload("{\"messageId\":\"cmd-001\",\"success\":true}");

        deviceMessageService.handleUpMessage(upMessage);

        verify(commandRecordService).markSuccessByCommandId(any(), any(), any());
        verify(devicePropertyMapper, never()).insert(any(DeviceProperty.class));
        verify(devicePropertyMapper, never()).updateById(any(DeviceProperty.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handleUpMessageShouldFanOutChildMessagesByDeviceCode() {
        Device baseDevice = new Device();
        baseDevice.setId(3001L);
        baseDevice.setTenantId(1L);
        baseDevice.setProductId(1001L);
        baseDevice.setDeviceCode("SK00FB0D1310195");
        baseDevice.setProtocolCode("mqtt-json");

        Device childDevice1 = new Device();
        childDevice1.setId(3002L);
        childDevice1.setTenantId(1L);
        childDevice1.setProductId(1001L);
        childDevice1.setDeviceCode("84330701");
        childDevice1.setProtocolCode("mqtt-json");

        Device childDevice2 = new Device();
        childDevice2.setId(3003L);
        childDevice2.setTenantId(1L);
        childDevice2.setProductId(1001L);
        childDevice2.setDeviceCode("84330695");
        childDevice2.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel dispsX = new ProductModel();
        dispsX.setIdentifier("dispsX");
        dispsX.setModelName("dispsX");
        dispsX.setDataType("double");

        ProductModel dispsY = new ProductModel();
        dispsY.setIdentifier("dispsY");
        dispsY.setModelName("dispsY");
        dispsY.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(baseDevice, childDevice1, childDevice2);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(dispsX, dispsY));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "SK00FB0D1310195",
                Map.of(), "property", "$dp");
        upMessage.setRawPayload("""
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0445,"dispsY":0.0293}},"L1_SW_2":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0293,"dispsY":0.0330}}}}
                """);

        DeviceUpMessage childMessage1 = new DeviceUpMessage();
        childMessage1.setDeviceCode("84330701");
        childMessage1.setTimestamp(upMessage.getTimestamp());
        childMessage1.setProperties(buildProperties(-0.0445, 0.0293));

        DeviceUpMessage childMessage2 = new DeviceUpMessage();
        childMessage2.setDeviceCode("84330695");
        childMessage2.setTimestamp(upMessage.getTimestamp());
        childMessage2.setProperties(buildProperties(-0.0293, 0.0330));

        upMessage.setChildMessages(List.of(childMessage1, childMessage2));

        deviceMessageService.handleUpMessage(upMessage);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper, times(3)).insert(logCaptor.capture());
        List<String> loggedDeviceCodes = new ArrayList<>();
        for (DeviceMessageLog logRecord : logCaptor.getAllValues()) {
            loggedDeviceCodes.add(logRecord.getDeviceCode());
        }
        assertEquals(List.of("SK00FB0D1310195", "84330701", "84330695"), loggedDeviceCodes);

        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper, times(4)).insert(propertyCaptor.capture());
        List<String> propertyKeys = new ArrayList<>();
        for (DeviceProperty property : propertyCaptor.getAllValues()) {
            propertyKeys.add(property.getDeviceId() + ":" + property.getIdentifier() + "=" + property.getPropertyValue());
        }
        assertEquals(List.of(
                "3002:dispsX=-0.0445",
                "3002:dispsY=0.0293",
                "3003:dispsX=-0.0293",
                "3003:dispsY=0.033"
        ), propertyKeys);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper, times(3)).updateById(deviceCaptor.capture());
        assertEquals(List.of(3001L, 3002L, 3003L),
                deviceCaptor.getAllValues().stream().map(Device::getId).toList());

        ArgumentCaptor<DeviceRiskEvaluationEvent> eventCaptor = ArgumentCaptor.forClass(DeviceRiskEvaluationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertEquals(List.of("84330701", "84330695"),
                eventCaptor.getAllValues().stream().map(DeviceRiskEvaluationEvent::getDeviceCode).toList());
    }

    @Test
    void getMessageTraceStatsShouldAggregateRecentSummary() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 11L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 5L;
                    }
                    if (sql.contains("message_type = ?")) {
                        return 3L;
                    }
                    if (sql.contains("INTERVAL 1 HOUR")) {
                        return 4L;
                    }
                    if (sql.contains("INTERVAL 24 HOUR")) {
                        return 18L;
                    }
                    return 22L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("message_type")) {
                        return List.of(new DeviceStatsBucketVO("property", "property", 12L));
                    }
                    if (sql.contains("product_key")) {
                        return List.of(new DeviceStatsBucketVO("demo-product", "demo-product", 10L));
                    }
                    if (sql.contains("device_code")) {
                        return List.of(new DeviceStatsBucketVO("demo-device-01", "demo-device-01", 8L));
                    }
                    return List.of(new DeviceStatsBucketVO(
                            "/sys/demo-product/demo-device-01/thing/property/post",
                            "/sys/demo-product/demo-device-01/thing/property/post",
                            7L
                    ));
                });

        DeviceMessageTraceStatsVO stats = deviceMessageService.getMessageTraceStats(new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery());

        assertEquals(22L, stats.getTotal());
        assertEquals(4L, stats.getRecentHourCount());
        assertEquals(18L, stats.getRecent24HourCount());
        assertEquals(11L, stats.getDistinctTraceCount());
        assertEquals(5L, stats.getDistinctDeviceCount());
        assertEquals(3L, stats.getDispatchFailureCount());
        assertEquals("property", stats.getTopMessageTypes().get(0).getValue());
        assertEquals("demo-product", stats.getTopProductKeys().get(0).getValue());
        assertEquals("demo-device-01", stats.getTopDeviceCodes().get(0).getValue());
        assertEquals("/sys/demo-product/demo-device-01/thing/property/post", stats.getTopTopics().get(0).getValue());
    }

    private DeviceUpMessage buildMessage(String protocolCode,
                                         String productKey,
                                         String deviceCode,
                                         Map<String, Object> properties,
                                         String messageType,
                                         String topic) {
        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode(protocolCode);
        upMessage.setProductKey(productKey);
        upMessage.setDeviceCode(deviceCode);
        upMessage.setProperties(properties);
        upMessage.setMessageType(messageType);
        upMessage.setTopic(topic);
        upMessage.setRawPayload("{\"properties\":{}}");
        upMessage.setTimestamp(LocalDateTime.now());
        return upMessage;
    }

    private Map<String, Object> buildProperties(double dispsX, double dispsY) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("dispsX", dispsX);
        properties.put("dispsY", dispsY);
        return properties;
    }
}
