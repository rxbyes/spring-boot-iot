package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
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
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    private DeviceMessageServiceImpl deviceMessageService;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setActivateDefault(true);
        iotProperties.setDevice(deviceConfig);
        deviceMessageService = new DeviceMessageServiceImpl(
                deviceMapper,
                deviceMessageLogMapper,
                devicePropertyMapper,
                productMapper,
                productModelMapper,
                iotProperties
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

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).updateById(deviceCaptor.capture());
        assertEquals(1, deviceCaptor.getValue().getOnlineStatus());
        assertEquals(1, deviceCaptor.getValue().getActivateStatus());
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
        assertEquals("integer", propertyCaptor.getValue().getValueType());
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
        when(deviceMapper.selectOne(any())).thenReturn(device);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-03",
                Map.of("temperature", 25), "property", "/sys/demo-product/demo-device-03/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备协议不匹配: demo-device-03", ex.getMessage());
        verifyNoInteractions(productMapper, productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
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
}
