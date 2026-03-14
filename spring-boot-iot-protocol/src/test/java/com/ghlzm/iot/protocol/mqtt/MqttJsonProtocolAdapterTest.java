package com.ghlzm.iot.protocol.mqtt;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttJsonProtocolAdapterTest {

    private final MqttJsonProtocolAdapter adapter = new MqttJsonProtocolAdapter(
            new MqttPayloadDecryptorRegistry(List.of()),
            new MqttPayloadFrameParser()
    );

    @Test
    void shouldDecodeLegacyNestedPlaintextPayload() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        DeviceUpMessage message = adapter.decode("""
                {"17165802":{"L1_GP_1":{"2026-03-14T06:00:00.000Z":{"gpsTotalZ":3.2,"gpsTotalX":9.9,"gpsTotalY":0.5}}}}
                """.getBytes(StandardCharsets.UTF_8), context);

        assertEquals("17165802", message.getDeviceCode());
        assertEquals("property", message.getMessageType());
        assertEquals(3.2, message.getProperties().get("L1_GP_1.gpsTotalZ"));
        assertEquals(9.9, message.getProperties().get("L1_GP_1.gpsTotalX"));
    }

    @Test
    void shouldRejectEncryptedPayloadWhenDecryptorIsMissing() {
        ProtocolContext context = new ProtocolContext();
        context.setTopic("$dp");
        context.setMessageType("property");

        BizException ex = assertThrows(BizException.class, () -> adapter.decode("""
                {"header":{"appId":"62000001"},"bodies":{"body":"PTOLy04o/stDufUYFo5s3g=="}} 
                """.getBytes(StandardCharsets.UTF_8), context));

        assertEquals("检测到加密 MQTT 报文，但未配置 appId 对应的解密器: 62000001", ex.getMessage());
    }
}
