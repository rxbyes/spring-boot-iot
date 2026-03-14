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
 * MQTT JSON 协议适配器。
 * 一期通过该适配器把 HTTP 模拟上报中的 JSON payload 转换成统一上行消息。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Component
public class MqttJsonProtocolAdapter implements ProtocolAdapter {

    /**
     * 当前阶段协议层只需要最小 JSON 解析能力，直接在适配器内部维护 ObjectMapper，
     * 避免联调测试依赖额外的 Jackson Bean 注入条件。
     */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public String getProtocolCode() {
        return "mqtt-json";
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeviceUpMessage decode(byte[] payload, ProtocolContext context) {
        try {
            // 当前阶段约定 payload 是标准 JSON 字符串，按 map 形式解析即可满足主链路。
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
