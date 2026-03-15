package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Component;

/**
 * MQTT Topic 解析器。
 * protocol 模块只负责纯解析逻辑，不参与接入层分发和业务处理。
 */
@Component
public class MqttTopicParser {

    public static final String ROUTE_TYPE_DIRECT = "direct";
    public static final String ROUTE_TYPE_SUB_DEVICE = "sub-device";
    public static final String ROUTE_TYPE_LEGACY = "legacy";

    private final MqttMessageTypeResolver mqttMessageTypeResolver;

    public MqttTopicParser(MqttMessageTypeResolver mqttMessageTypeResolver) {
        this.mqttMessageTypeResolver = mqttMessageTypeResolver;
    }

    /**
     * 解析 MQTT topic。
     * 当前支持：
     * - 直连设备：/sys/{productKey}/{deviceCode}/thing/{domain}/{action}
     * - 网关代子设备：/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/{domain}/{action}
     */
    public ParsedTopic parse(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new BizException("MQTT topic 不能为空");
        }

        // 兼容历史系统的 $dp 主题。该主题不包含标准设备上下文，需要后续结合 payload 补齐。
        if ("$dp".equals(topic)) {
            return new ParsedTopic(
                    null,
                    null,
                    null,
                    null,
                    ROUTE_TYPE_LEGACY,
                    "legacy",
                    "dp",
                    mqttMessageTypeResolver.resolve("legacy", "dp"),
                    topic
            );
        }

        String[] segments = topic.split("/");
        if (segments.length >= 9
                && "sys".equals(segments[1])
                && "sub".equals(segments[4])
                && "thing".equals(segments[6])) {
            String productKey = segments[2];
            String gatewayDeviceCode = segments[3];
            String subDeviceCode = segments[5];
            String domain = segments[7];
            String action = segments[8];
            String messageType = mqttMessageTypeResolver.resolve(domain, action);
            return new ParsedTopic(
                    productKey,
                    subDeviceCode,
                    gatewayDeviceCode,
                    subDeviceCode,
                    ROUTE_TYPE_SUB_DEVICE,
                    domain,
                    action,
                    messageType,
                    topic
            );
        }

        if (segments.length >= 7 && "sys".equals(segments[1]) && "thing".equals(segments[4])) {
            String productKey = segments[2];
            String deviceCode = segments[3];
            String domain = segments[5];
            String action = segments[6];
            String messageType = mqttMessageTypeResolver.resolve(domain, action);
            return new ParsedTopic(
                    productKey,
                    deviceCode,
                    null,
                    null,
                    ROUTE_TYPE_DIRECT,
                    domain,
                    action,
                    messageType,
                    topic
            );
        }

        if (segments.length < 7 || !"sys".equals(segments[1])) {
            throw new BizException("不支持的 MQTT topic: " + topic);
        }
        throw new BizException("不支持的 MQTT topic: " + topic);
    }

    /**
     * MQTT Topic 解析结果。
     */
    public record ParsedTopic(String productKey,
                              String deviceCode,
                              String gatewayDeviceCode,
                              String subDeviceCode,
                              String routeType,
                              String domain,
                              String action,
                              String messageType,
                              String originalTopic) {
    }
}
