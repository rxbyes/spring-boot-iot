package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Component;

/**
 * MQTT Topic 解析器。
 * protocol 模块只负责纯解析逻辑，不参与接入层分发和业务处理。
 */
@Component
public class MqttTopicParser {

    private final MqttMessageTypeResolver mqttMessageTypeResolver;

    public MqttTopicParser(MqttMessageTypeResolver mqttMessageTypeResolver) {
        this.mqttMessageTypeResolver = mqttMessageTypeResolver;
    }

    /**
     * 解析标准直连设备 topic。
     * 当前只覆盖 /sys/{productKey}/{deviceCode}/thing/{domain}/{action} 结构。
     */
    public ParsedTopic parse(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new BizException("MQTT topic 不能为空");
        }

        String[] segments = topic.split("/");
        if (segments.length < 7 || !"sys".equals(segments[1]) || !"thing".equals(segments[4])) {
            throw new BizException("不支持的 MQTT topic: " + topic);
        }

        String productKey = segments[2];
        String deviceCode = segments[3];
        String domain = segments[5];
        String action = segments[6];
        String messageType = mqttMessageTypeResolver.resolve(domain, action);
        return new ParsedTopic(productKey, deviceCode, domain, action, messageType, topic);
    }

    /**
     * MQTT Topic 解析结果。
     */
    public record ParsedTopic(String productKey,
                              String deviceCode,
                              String domain,
                              String action,
                              String messageType,
                              String originalTopic) {
    }
}
