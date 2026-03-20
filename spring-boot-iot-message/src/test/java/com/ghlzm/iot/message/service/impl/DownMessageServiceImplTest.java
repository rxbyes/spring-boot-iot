package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.service.model.DownMessagePublishCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownMessageServiceImplTest {

    @Mock
    private MqttDownMessagePublisher mqttDownMessagePublisher;
    @Mock
    private CommandRecordService commandRecordService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private ProductService productService;

    @Test
    void publishShouldRejectWhenProductDisabled() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.DISABLED.getCode());

        when(deviceService.getRequiredByCode("demo-device-01")).thenReturn(device);
        when(productService.getRequiredById(1001L)).thenReturn(product);

        DownMessageServiceImpl downMessageService = new DownMessageServiceImpl(
                mqttDownMessagePublisher,
                commandRecordService,
                deviceService,
                productService,
                buildIotProperties()
        );

        DownMessagePublishCommand command = new DownMessagePublishCommand();
        command.setDeviceCode("demo-device-01");
        command.setCommandType("property");

        BizException ex = assertThrows(BizException.class, () -> downMessageService.publish(command));
        assertEquals("产品已停用，拒绝设备下发: demo-product", ex.getMessage());
        verify(deviceService).getRequiredByCode("demo-device-01");
        verify(productService).getRequiredById(1001L);
        verifyNoInteractions(mqttDownMessagePublisher);
        verify(commandRecordService, never()).create(any(CommandRecord.class));
    }

    private IotProperties buildIotProperties() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Mqtt mqtt = new IotProperties.Mqtt();
        mqtt.setQos(1);
        iotProperties.setMqtt(mqtt);
        return iotProperties;
    }
}
