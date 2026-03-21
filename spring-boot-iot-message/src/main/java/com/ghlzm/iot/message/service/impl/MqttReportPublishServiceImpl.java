package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlzm.iot.message.service.MqttReportPublishService;
import com.ghlzm.iot.message.service.model.MqttReportPublishCommand;
import com.ghlzm.iot.message.support.MessagePayloadEncodingSupport;
import org.springframework.stereotype.Service;

/**
 * MQTT 原始上行模拟发布服务实现。
 */
@Service
public class MqttReportPublishServiceImpl implements MqttReportPublishService {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final MqttMessageConsumer mqttMessageConsumer;
    private final MqttDownMessagePublisher mqttDownMessagePublisher;
    private final IotProperties iotProperties;

    public MqttReportPublishServiceImpl(DeviceService deviceService,
                                        ProductService productService,
                                        MqttMessageConsumer mqttMessageConsumer,
                                        MqttDownMessagePublisher mqttDownMessagePublisher,
                                        IotProperties iotProperties) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.mqttMessageConsumer = mqttMessageConsumer;
        this.mqttDownMessagePublisher = mqttDownMessagePublisher;
        this.iotProperties = iotProperties;
    }

    @Override
    public void publish(MqttReportPublishCommand command) {
        if (command == null) {
            throw new BizException("MQTT 模拟上报参数不能为空");
        }
        if (!mqttMessageConsumer.isConnected()) {
            throw new BizException("MQTT 客户端未连接，无法执行模拟上报");
        }

        Device device = deviceService.getRequiredByCode(command.getDeviceCode());
        Product product = productService.getRequiredById(device.getProductId());
        ensureProductMatched(command.getProductKey(), product, command.getDeviceCode());
        ensureProtocolMatched(command.getProtocolCode(), device, command.getDeviceCode());

        int actualQos = command.getQos() == null ? iotProperties.getMqtt().getQos() : command.getQos();
        boolean retained = Boolean.TRUE.equals(command.getRetained());
        byte[] payloadBytes = MessagePayloadEncodingSupport.resolvePayloadBytes(
                command.getPayload(),
                command.getPayloadEncoding()
        );
        mqttDownMessagePublisher.publishRaw(command.getTopic(), payloadBytes, actualQos, retained);
    }

    private void ensureProductMatched(String productKey, Product product, String deviceCode) {
        if (product == null || product.getProductKey() == null) {
            throw new BizException("设备所属产品不存在: " + deviceCode);
        }
        if (!product.getProductKey().equalsIgnoreCase(productKey)) {
            throw new BizException("模拟上报 productKey 与设备所属产品不匹配: " + deviceCode);
        }
    }

    private void ensureProtocolMatched(String protocolCode, Device device, String deviceCode) {
        String actualProtocolCode = device == null ? null : device.getProtocolCode();
        if (actualProtocolCode == null || actualProtocolCode.isBlank()) {
            return;
        }
        if (!actualProtocolCode.equalsIgnoreCase(protocolCode)) {
            throw new BizException("模拟上报 protocolCode 与设备协议不匹配: " + deviceCode);
        }
    }
}
