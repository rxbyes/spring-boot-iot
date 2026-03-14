package com.ghlzm.iot.message.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT 连接监听器。
 * 该类只负责连接与消费侧日志输出，避免把日志细节塞进消费者主流程。
 */
@Component
public class MqttConnectionListener {

    private static final Logger log = LoggerFactory.getLogger(MqttConnectionListener.class);

    public void onConnectComplete(boolean reconnect, String serverUri) {
        log.info("MQTT 客户端已连接, reconnect={}, serverUri={}", reconnect, serverUri);
    }

    public void onSubscribe(List<String> topics) {
        log.info("MQTT 客户端已订阅主题: {}", topics);
    }

    public void onConnectionLost(Throwable cause) {
        log.warn("MQTT 连接已断开: {}", cause == null ? "unknown" : cause.getMessage());
    }

    public void onStartupSkipped(String reason) {
        log.info("跳过 MQTT 客户端启动: {}", reason);
    }

    public void onMessageDispatchFailed(String topic, Throwable throwable) {
        log.error("MQTT 消息分发失败, topic={}", topic, throwable);
    }
}
