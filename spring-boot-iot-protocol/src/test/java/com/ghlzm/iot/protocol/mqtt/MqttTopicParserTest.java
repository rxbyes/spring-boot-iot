package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttTopicParserTest {

    private final MqttTopicParser parser = new MqttTopicParser(new MqttMessageTypeResolver());

    @Test
    void shouldParseDirectDeviceTopic() {
        MqttTopicParser.ParsedTopic parsedTopic = parser.parse("/sys/demo-product/demo-device-01/thing/property/post");

        assertEquals("demo-product", parsedTopic.productKey());
        assertEquals("demo-device-01", parsedTopic.deviceCode());
        assertEquals(MqttTopicParser.ROUTE_TYPE_DIRECT, parsedTopic.routeType());
        assertEquals("property", parsedTopic.messageType());
    }

    @Test
    void shouldParseSubDeviceTopic() {
        MqttTopicParser.ParsedTopic parsedTopic = parser.parse("/sys/gw-product/gateway-01/sub/sub-device-01/thing/property/post");

        assertEquals("gw-product", parsedTopic.productKey());
        assertEquals("sub-device-01", parsedTopic.deviceCode());
        assertEquals("gateway-01", parsedTopic.gatewayDeviceCode());
        assertEquals("sub-device-01", parsedTopic.subDeviceCode());
        assertEquals(MqttTopicParser.ROUTE_TYPE_SUB_DEVICE, parsedTopic.routeType());
        assertEquals("property", parsedTopic.messageType());
    }

    @Test
    void shouldKeepLegacyDpTopic() {
        MqttTopicParser.ParsedTopic parsedTopic = parser.parse("$dp");

        assertEquals(MqttTopicParser.ROUTE_TYPE_LEGACY, parsedTopic.routeType());
        assertEquals("property", parsedTopic.messageType());
    }

    @Test
    void shouldRejectUnsupportedTopic() {
        BizException ex = assertThrows(BizException.class, () -> parser.parse("/bad/demo/topic"));

        assertEquals("不支持的 MQTT topic: /bad/demo/topic", ex.getMessage());
    }
}
