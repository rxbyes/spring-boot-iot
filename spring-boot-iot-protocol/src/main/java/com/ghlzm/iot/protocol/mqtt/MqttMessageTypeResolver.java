package com.ghlzm.iot.protocol.mqtt;

import org.springframework.stereotype.Component;

/**
 * MQTT 消息类型解析器。
 * 只负责根据 topic 中的领域段和动作段推断统一消息类型。
 */
@Component
public class MqttMessageTypeResolver {

    public String resolve(String domain, String action) {
        if ("legacy".equals(domain) && "dp".equals(action)) {
            return "property";
        }
        if ("property".equals(domain) && "post".equals(action)) {
            return "property";
        }
        if ("property".equals(domain) && "set".equals(action)) {
            return "property";
        }
        if ("event".equals(domain) && "post".equals(action)) {
            return "event";
        }
        if ("status".equals(domain) && "post".equals(action)) {
            return "status";
        }
        if ("property".equals(domain) && "reply".equals(action)) {
            return "reply";
        }
        if ("service".equals(domain) && "reply".equals(action)) {
            return "reply";
        }
        if ("service".equals(domain) && "invoke".equals(action)) {
            return "service";
        }
        return domain;
    }
}
