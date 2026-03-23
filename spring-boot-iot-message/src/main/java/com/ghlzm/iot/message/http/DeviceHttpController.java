package com.ghlzm.iot.message.http;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.message.http.vo.MessageFlowSubmitResultVO;
import com.ghlzm.iot.message.pipeline.MessageFlowExecutionResult;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingPipeline;
import com.ghlzm.iot.message.pipeline.UpMessageProcessingRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * HTTP 设备接入控制器。
 */
@RestController
public class DeviceHttpController {

    private final UpMessageProcessingPipeline upMessageProcessingPipeline;

    public DeviceHttpController(UpMessageProcessingPipeline upMessageProcessingPipeline) {
        this.upMessageProcessingPipeline = upMessageProcessingPipeline;
    }

    @PostMapping("/api/message/http/report")
    public R<MessageFlowSubmitResultVO> report(@RequestBody @Valid DeviceReportRequest request) {
        UpMessageProcessingRequest pipelineRequest = new UpMessageProcessingRequest();
        pipelineRequest.setTransportMode("HTTP");
        pipelineRequest.setProtocolCode(request.getProtocolCode());
        pipelineRequest.setProductKey(request.getProductKey());
        pipelineRequest.setDeviceCode(request.getDeviceCode());
        pipelineRequest.setTopic(request.getTopic());
        pipelineRequest.setClientId(request.getClientId());
        pipelineRequest.setTenantId(request.getTenantId());
        pipelineRequest.setPayload(resolvePayloadBytes(request));

        MessageFlowExecutionResult result = upMessageProcessingPipeline.process(pipelineRequest);
        return R.ok(toSubmitResultVO(result.getSubmitResult()));
    }

    private byte[] resolvePayloadBytes(DeviceReportRequest request) {
        Charset charset = resolveCharset(request.getPayloadEncoding());
        return request.getPayload().getBytes(charset);
    }

    private Charset resolveCharset(String payloadEncoding) {
        if (payloadEncoding == null || payloadEncoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        String normalized = payloadEncoding.trim().toLowerCase(Locale.ROOT);
        if ("iso-8859-1".equals(normalized) || "latin1".equals(normalized) || "latin-1".equals(normalized)) {
            return StandardCharsets.ISO_8859_1;
        }
        return StandardCharsets.UTF_8;
    }

    private MessageFlowSubmitResultVO toSubmitResultVO(MessageFlowSubmitResult submitResult) {
        MessageFlowSubmitResultVO resultVO = new MessageFlowSubmitResultVO();
        resultVO.setSessionId(submitResult.getSessionId());
        resultVO.setTraceId(submitResult.getTraceId());
        resultVO.setStatus(submitResult.getStatus());
        resultVO.setTimelineAvailable(submitResult.getTimelineAvailable());
        resultVO.setCorrelationPending(submitResult.getCorrelationPending());
        return resultVO;
    }
}
