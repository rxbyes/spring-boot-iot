package com.ghlzm.iot.message.mqtt;

import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.mqtt.MqttTopicParser;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT Topic 路由器。
 * 负责把 MQTT Topic 解析成现有主链路可消费的原始消息上下文。
 */
@Component
public class MqttTopicRouter {

    private static final List<String> DEFAULT_TOPICS = List.of(
//            "$dp",
            "/sys/+/+/thing/property/post",
            "/sys/+/+/thing/event/post",
            "/sys/+/+/thing/property/reply",
            "/sys/+/+/thing/service/reply",
            "/sys/+/+/thing/status/post"
    );

    private final IotProperties iotProperties;
    private final MqttTopicParser mqttTopicParser;

    public MqttTopicRouter(IotProperties iotProperties, MqttTopicParser mqttTopicParser) {
        this.iotProperties = iotProperties;
        this.mqttTopicParser = mqttTopicParser;
    }

    /**
     * 解析 MQTT topic。
     * 当前会区分直连设备 topic 与网关代子设备 topic，但默认订阅仍保持直连设备范围，
     * 避免在未接入子设备业务前影响现有运行环境。
     */
    public RoutedTopic route(String topic) {
        MqttTopicParser.ParsedTopic parsedTopic = mqttTopicParser.parse(topic);
        return new RoutedTopic(
                parsedTopic.productKey(),
                parsedTopic.deviceCode(),
                parsedTopic.gatewayDeviceCode(),
                parsedTopic.subDeviceCode(),
                parsedTopic.routeType(),
                parsedTopic.domain(),
                parsedTopic.action(),
                parsedTopic.messageType(),
                parsedTopic.originalTopic()
        );
    }

    /**
     * 把 MQTT 消息转换成统一 RawDeviceMessage，后续直接进入一期现有分发链路。
     */
    public RawDeviceMessage toRawMessage(String topic, MqttMessage mqttMessage) {
        RoutedTopic routedTopic = route(topic);

        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setProtocolCode(iotProperties.getProtocol().getDefaultCode());
        rawDeviceMessage.setProductKey(routedTopic.productKey());
        rawDeviceMessage.setDeviceCode(routedTopic.deviceCode());
        rawDeviceMessage.setGatewayDeviceCode(routedTopic.gatewayDeviceCode());
        rawDeviceMessage.setSubDeviceCode(routedTopic.subDeviceCode());
        rawDeviceMessage.setTopicRouteType(routedTopic.routeType());
        rawDeviceMessage.setMessageType(routedTopic.messageType());
        rawDeviceMessage.setTopic(routedTopic.originalTopic());
        // 当前阶段先把 deviceCode 作为 MQTT 设备侧 clientId 的默认映射。
        rawDeviceMessage.setClientId(routedTopic.deviceCode());
        rawDeviceMessage.setPayload(mqttMessage.getPayload());
        if (iotProperties.getTenant() != null && iotProperties.getTenant().getDefaultTenantId() != null) {
            rawDeviceMessage.setTenantId(String.valueOf(iotProperties.getTenant().getDefaultTenantId()));
        }
        return rawDeviceMessage;
    }

    public List<String> resolveSubscribeTopics() {
        List<String> configuredTopics = iotProperties.getMqtt().getDefaultSubscribeTopics();
        return configuredTopics == null || configuredTopics.isEmpty() ? DEFAULT_TOPICS : configuredTopics;
    }

    /**
     * Topic 解析结果。
     */
    public record RoutedTopic(String productKey,
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
