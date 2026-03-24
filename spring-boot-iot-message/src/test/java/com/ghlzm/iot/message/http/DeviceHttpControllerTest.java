package com.ghlzm.iot.message.http;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.message.pipeline.MessageFlowExecutionResult;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingPipeline;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceHttpControllerTest {

    @Test
    void shouldEncodePayloadWithUtf8ByDefault() {
        UpMessageProcessingPipeline pipeline = mock(UpMessageProcessingPipeline.class);
        when(pipeline.process(org.mockito.ArgumentMatchers.any())).thenReturn(buildExecutionResult());
        DeviceHttpController controller = new DeviceHttpController(pipeline);
        DeviceReportRequest request = buildRequest("{\"message\":\"中文\"}");

        controller.report(request);

        ArgumentCaptor<UpMessageProcessingRequest> captor = ArgumentCaptor.forClass(UpMessageProcessingRequest.class);
        verify(pipeline).process(captor.capture());
        assertArrayEquals(request.getPayload().getBytes(StandardCharsets.UTF_8), captor.getValue().getPayload());
        assertEquals("HTTP", captor.getValue().getTransportMode());
    }

    @Test
    void shouldPreserveFrameBytesWhenLatin1EncodingIsSpecified() {
        UpMessageProcessingPipeline pipeline = mock(UpMessageProcessingPipeline.class);
        when(pipeline.process(org.mockito.ArgumentMatchers.any())).thenReturn(buildExecutionResult());
        DeviceHttpController controller = new DeviceHttpController(pipeline);
        byte[] rawPacket = new byte[]{0x02, 0x01, (byte) 0xC8, 0x7B, 0x7D};

        DeviceReportRequest request = buildRequest(new String(rawPacket, StandardCharsets.ISO_8859_1));
        request.setPayloadEncoding("ISO-8859-1");

        controller.report(request);

        ArgumentCaptor<UpMessageProcessingRequest> captor = ArgumentCaptor.forClass(UpMessageProcessingRequest.class);
        verify(pipeline).process(captor.capture());
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

    private MessageFlowExecutionResult buildExecutionResult() {
        MessageFlowSubmitResult submitResult = new MessageFlowSubmitResult();
        submitResult.setSessionId("session-demo-001");
        submitResult.setTraceId("trace-demo-001");
        submitResult.setStatus("COMPLETED");
        submitResult.setTimelineAvailable(true);
        submitResult.setCorrelationPending(false);
        MessageFlowExecutionResult executionResult = new MessageFlowExecutionResult();
        executionResult.setSubmitResult(submitResult);
        return executionResult;
    }
}
