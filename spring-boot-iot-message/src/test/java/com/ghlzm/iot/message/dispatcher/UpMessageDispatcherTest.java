package com.ghlzm.iot.message.dispatcher;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.model.DeviceDownMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpMessageDispatcherTest {

    @Mock
    private ProtocolAdapterRegistry protocolAdapterRegistry;
    @Mock
    private DeviceMessageService deviceMessageService;

    @Test
    void dispatchShouldEnrichRawMessageWithDecodedIdentityBeforeDeviceValidation() {
        UpMessageDispatcher dispatcher = new UpMessageDispatcher(protocolAdapterRegistry, deviceMessageService);
        RawDeviceMessage rawMessage = new RawDeviceMessage();
        rawMessage.setProtocolCode("mqtt-json");
        rawMessage.setTopic("$dp");
        rawMessage.setPayload("{\"body\":\"encrypted\"}".getBytes(StandardCharsets.UTF_8));

        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(new StubProtocolAdapter());
        doThrow(new BizException("设备未绑定产品: SK11EB0D1308096AZ"))
                .when(deviceMessageService).handleUpMessage(any(DeviceUpMessage.class));

        BizException ex = assertThrows(BizException.class, () -> dispatcher.dispatch(rawMessage));

        assertEquals("设备未绑定产品: SK11EB0D1308096AZ", ex.getMessage());
        assertEquals("SK11EB0D1308096AZ", rawMessage.getDeviceCode());
        assertEquals("south_multi_displacement", rawMessage.getProductKey());
        assertEquals("mqtt-json", rawMessage.getProtocolCode());
        assertEquals("property", rawMessage.getMessageType());
        assertEquals("SK11EB0D1308096AZ", rawMessage.getClientId());
    }

    private static class StubProtocolAdapter implements ProtocolAdapter {

        @Override
        public String getProtocolCode() {
            return "mqtt-json";
        }

        @Override
        public DeviceUpMessage decode(byte[] payload, com.ghlzm.iot.protocol.core.context.ProtocolContext context) {
            DeviceUpMessage upMessage = new DeviceUpMessage();
            upMessage.setDeviceCode("SK11EB0D1308096AZ");
            upMessage.setProductKey("south_multi_displacement");
            upMessage.setProtocolCode("mqtt-json");
            upMessage.setMessageType("property");
            upMessage.setTimestamp(LocalDateTime.now());
            upMessage.setRawPayload("{\"decoded\":true}");
            return upMessage;
        }

        @Override
        public byte[] encode(DeviceDownMessage message, com.ghlzm.iot.protocol.core.context.ProtocolContext context) {
            return new byte[0];
        }
    }
}
