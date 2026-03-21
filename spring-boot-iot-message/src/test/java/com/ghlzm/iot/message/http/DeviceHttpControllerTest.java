package com.ghlzm.iot.message.http;

import com.ghlzm.iot.message.dispatcher.UpMessageDispatcher;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeviceHttpControllerTest {

    @Test
    void shouldEncodePayloadWithUtf8ByDefault() {
        UpMessageDispatcher dispatcher = mock(UpMessageDispatcher.class);
        DeviceHttpController controller = new DeviceHttpController(dispatcher);
        DeviceReportRequest request = buildRequest("{\"message\":\"中文\"}");

        controller.report(request);

        ArgumentCaptor<RawDeviceMessage> captor = ArgumentCaptor.forClass(RawDeviceMessage.class);
        verify(dispatcher).dispatch(captor.capture());
        assertArrayEquals(request.getPayload().getBytes(StandardCharsets.UTF_8), captor.getValue().getPayload());
    }

    @Test
    void shouldPreserveFrameBytesWhenLatin1EncodingIsSpecified() {
        UpMessageDispatcher dispatcher = mock(UpMessageDispatcher.class);
        DeviceHttpController controller = new DeviceHttpController(dispatcher);
        byte[] rawPacket = new byte[]{0x02, 0x01, (byte) 0xC8, 0x7B, 0x7D};

        DeviceReportRequest request = buildRequest(new String(rawPacket, StandardCharsets.ISO_8859_1));
        request.setPayloadEncoding("ISO-8859-1");

        controller.report(request);

        ArgumentCaptor<RawDeviceMessage> captor = ArgumentCaptor.forClass(RawDeviceMessage.class);
        verify(dispatcher).dispatch(captor.capture());
        assertArrayEquals(rawPacket, captor.getValue().getPayload());
    }

    private DeviceReportRequest buildRequest(String payload) {
        DeviceReportRequest request = new DeviceReportRequest();
        request.setProtocolCode("mqtt-json");
        request.setProductKey("demo-product");
        request.setDeviceCode("demo-device-01");
        request.setPayload(payload);
        request.setTenantId("1");
        return request;
    }
}
