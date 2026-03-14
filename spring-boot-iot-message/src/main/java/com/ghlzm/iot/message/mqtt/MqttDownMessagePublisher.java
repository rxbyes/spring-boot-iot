package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.springframework.stereotype.Component;

/**
 * MQTT 下行发布骨架。
 * Phase 2 Task 1 只建立最小发布入口，为后续下行任务预留组件位置。
 */
@Component
public class MqttDownMessagePublisher {

    private final MqttMessageConsumer mqttMessageConsumer;
    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final IotProperties iotProperties;

    public MqttDownMessagePublisher(MqttMessageConsumer mqttMessageConsumer,
                                    ProtocolAdapterRegistry protocolAdapterRegistry,
                                    IotProperties iotProperties) {
        this.mqttMessageConsumer = mqttMessageConsumer;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.iotProperties = iotProperties;
    }

    /**
     * 按协议编码发布统一下行消息。
     * 当前先保留最小入口，真正的命令任务编排留待后续阶段实现。
     */
    public void publish(String protocolCode, String topic, DeviceDownMessage message, ProtocolContext context) {
        String actualProtocolCode = (protocolCode == null || protocolCode.isBlank())
                ? iotProperties.getProtocol().getDefaultCode()
                : protocolCode;
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(actualProtocolCode);
        if (adapter == null) {
            throw new BizException("未找到协议适配器: " + actualProtocolCode);
        }
        byte[] payload = adapter.encode(message, context);
        publishRaw(topic, payload, iotProperties.getMqtt().getQos(), false);
    }

    /**
     * 发布原始二进制消息。
     */
    public void publishRaw(String topic, byte[] payload, int qos, boolean retained) {
        mqttMessageConsumer.publish(topic, payload, qos, retained);
    }
}
