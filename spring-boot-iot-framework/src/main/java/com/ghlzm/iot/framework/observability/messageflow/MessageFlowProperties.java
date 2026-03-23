package com.ghlzm.iot.framework.observability.messageflow;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息链路时间线配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "iot.observability.message-flow")
public class MessageFlowProperties {

    /**
     * 是否启用消息链路时间线。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * trace/session 时间线保留时长，单位小时。
     */
    private Integer ttlHours = 24;

    /**
     * 模拟发布和真实消费回流的 fingerprint 匹配窗口，单位秒。
     */
    private Integer sessionMatchWindowSeconds = 120;
}
