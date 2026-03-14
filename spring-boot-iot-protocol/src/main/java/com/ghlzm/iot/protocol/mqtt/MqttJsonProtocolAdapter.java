package com.ghlzm.iot.protocol.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Component
public class MqttJsonProtocolAdapter implements ProtocolAdapter {

    private final ObjectMapper objectMapper;

    public MqttJsonProtocolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProtocolCode() {
        return "mqtt-json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceUpMessage decode(byte[] payload, ProtocolContext context) {
        try {
            Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {
            });
            DeviceUpMessage message = new DeviceUpMessage();
            message.setTenantId(context.getTenantCode());
            message.setProductKey(context.getProductKey());
            message.setDeviceCode(context.getDeviceCode());
            message.setMessageType(String.valueOf(map.getOrDefault("messageType", "property")));
            message.setTopic(context.getTopic());

            Object props = map.get("properties");
            if (props instanceof Map<?, ?>) {
                message.setProperties((Map<String, Object>) props);
            }

            Object events = map.get("events");
            if (events instanceof Map<?, ?>) {
                message.setEvents((Map<String, Object>) events);
            }

            message.setTimestamp(LocalDateTime.now());
            message.setRawPayload(new String(payload, StandardCharsets.UTF_8));
            return message;
        } catch (Exception e) {
            throw new BizException("MQTT JSON 协议解析失败");
        }
    }

    @Override
    public byte[] encode(DeviceDownMessage message, ProtocolContext context) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (Exception e) {
            throw new BizException("MQTT JSON 协议编码失败");
        }
    }
}
