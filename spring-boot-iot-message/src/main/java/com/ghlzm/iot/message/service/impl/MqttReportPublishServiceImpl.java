package com.ghlzm.iot.message.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowFingerprintSupport;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.message.mqtt.MqttDownMessagePublisher;
import com.ghlzm.iot.message.mqtt.MqttMessageConsumer;
import com.ghlzm.iot.message.service.MqttReportPublishService;
import com.ghlzm.iot.message.service.model.MqttReportPublishCommand;
import com.ghlzm.iot.message.support.MessagePayloadEncodingSupport;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    private final MessageFlowProperties messageFlowProperties;
    private final MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    private final MessageFlowTimelineStore messageFlowTimelineStore;

    public MqttReportPublishServiceImpl(DeviceService deviceService,
                                        ProductService productService,
                                        MqttMessageConsumer mqttMessageConsumer,
                                        MqttDownMessagePublisher mqttDownMessagePublisher,
                                        IotProperties iotProperties,
                                        MessageFlowProperties messageFlowProperties,
                                        MessageFlowMetricsRecorder messageFlowMetricsRecorder,
                                        MessageFlowTimelineStore messageFlowTimelineStore) {
        this.deviceService = deviceService;
        this.productService = productService;
        this.mqttMessageConsumer = mqttMessageConsumer;
        this.mqttDownMessagePublisher = mqttDownMessagePublisher;
        this.iotProperties = iotProperties;
        this.messageFlowProperties = messageFlowProperties;
        this.messageFlowMetricsRecorder = messageFlowMetricsRecorder;
        this.messageFlowTimelineStore = messageFlowTimelineStore;
    }

    @Override
    public MessageFlowSubmitResult publish(MqttReportPublishCommand command) {
        if (command == null) {
            throw new BizException("MQTT 模拟上报参数不能为空");
        }
        if (!mqttMessageConsumer.isConnected()) {
            throw new BizException("MQTT 客户端未连接，无法执行模拟上报");
        }

        Device device = deviceService.getRequiredByCode(command.getDeviceCode());
        Product product = resolveRequiredProduct(device, command.getDeviceCode());
        ensureProductMatched(command.getProductKey(), product, command.getDeviceCode());
        ensureProtocolMatched(command.getProtocolCode(), device, product, command.getDeviceCode());

        int actualQos = command.getQos() == null ? iotProperties.getMqtt().getQos() : command.getQos();
        boolean retained = Boolean.TRUE.equals(command.getRetained());
        byte[] payloadBytes = MessagePayloadEncodingSupport.resolvePayloadBytes(
                command.getPayload(),
                command.getPayloadEncoding()
        );
        MessageFlowSubmitResult submitResult = buildSubmitResult(command, payloadBytes);
        mqttDownMessagePublisher.publishRaw(command.getTopic(), payloadBytes, actualQos, retained);
        return submitResult;
    }

    private MessageFlowSubmitResult buildSubmitResult(MqttReportPublishCommand command, byte[] payloadBytes) {
        MessageFlowSubmitResult submitResult = new MessageFlowSubmitResult();
        String sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        submitResult.setSessionId(sessionId);
        submitResult.setStatus(MessageFlowStatuses.SESSION_PUBLISHED);
        submitResult.setTimelineAvailable(Boolean.FALSE);
        submitResult.setCorrelationPending(Boolean.TRUE);
        messageFlowMetricsRecorder.recordSession("MQTT", MessageFlowStatuses.SESSION_PUBLISHED);
        messageFlowMetricsRecorder.recordCorrelation(MessageFlowMetricsRecorder.CORRELATION_RESULT_PUBLISHED);

        if (Boolean.TRUE.equals(messageFlowProperties.getEnabled())) {
            MessageFlowSession session = new MessageFlowSession();
            session.setSessionId(sessionId);
            session.setTransportMode("MQTT");
            session.setStatus(MessageFlowStatuses.SESSION_PUBLISHED);
            session.setSubmittedAt(LocalDateTime.now());
            session.setDeviceCode(command.getDeviceCode());
            session.setTopic(command.getTopic());
            session.setCorrelationPending(Boolean.TRUE);
            messageFlowTimelineStore.saveSession(session);

            String fingerprint = MessageFlowFingerprintSupport.buildFingerprint(
                    command.getTopic(),
                    command.getDeviceCode(),
                    payloadBytes
            );
            messageFlowTimelineStore.bindFingerprint(fingerprint, sessionId);
        }
        return submitResult;
    }

    private void ensureProductMatched(String productKey, Product product, String deviceCode) {
        if (product == null || product.getProductKey() == null) {
            throw new BizException("设备所属产品不存在: " + deviceCode);
        }
        if (!product.getProductKey().equalsIgnoreCase(productKey)) {
            throw new BizException("模拟上报 productKey 与设备所属产品不匹配: " + deviceCode);
        }
    }

    private Product resolveRequiredProduct(Device device, String deviceCode) {
        if (device == null || device.getProductId() == null || device.getProductId() <= 0) {
            throw new BizException("设备所属产品不存在: " + deviceCode);
        }
        try {
            return productService.getRequiredById(device.getProductId());
        } catch (BizException ex) {
            throw new BizException("设备所属产品不存在: " + deviceCode);
        }
    }

    private void ensureProtocolMatched(String protocolCode, Device device, Product product, String deviceCode) {
        String deviceProtocolCode = normalizeText(device == null ? null : device.getProtocolCode());
        String productProtocolCode = normalizeText(product == null ? null : product.getProtocolCode());
        if (hasText(deviceProtocolCode) && hasText(productProtocolCode)
                && !deviceProtocolCode.equalsIgnoreCase(productProtocolCode)) {
            throw new BizException("模拟上报目标设备协议配置异常: " + deviceCode
                    + ", deviceProtocol=" + deviceProtocolCode
                    + ", productProtocol=" + productProtocolCode);
        }
        String expectedProtocolCode = hasText(deviceProtocolCode) ? deviceProtocolCode : productProtocolCode;
        if (!hasText(expectedProtocolCode)) {
            throw new BizException("模拟上报目标设备未配置协议: " + deviceCode
                    + ", deviceProtocol=" + displayText(deviceProtocolCode)
                    + ", productProtocol=" + displayText(productProtocolCode));
        }
        if (!expectedProtocolCode.equalsIgnoreCase(protocolCode)) {
            throw new BizException("模拟上报 protocolCode 与设备协议不匹配: " + deviceCode
                    + ", expected=" + expectedProtocolCode
                    + ", actual=" + displayText(protocolCode));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String displayText(String value) {
        return hasText(value) ? value.trim() : "<empty>";
    }
}
