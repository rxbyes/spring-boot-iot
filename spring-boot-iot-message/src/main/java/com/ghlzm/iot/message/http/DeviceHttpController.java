package com.ghlzm.iot.message.http;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.message.dispatcher.UpMessageDispatcher;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
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

    private final UpMessageDispatcher upMessageDispatcher;

    public DeviceHttpController(UpMessageDispatcher upMessageDispatcher) {
        this.upMessageDispatcher = upMessageDispatcher;
    }

    @PostMapping("/api/message/http/report")
    public R<?> report(@RequestBody @Valid DeviceReportRequest request) {
        RawDeviceMessage raw = new RawDeviceMessage();
        raw.setProtocolCode(request.getProtocolCode());
        raw.setProductKey(request.getProductKey());
        raw.setTraceId(TraceContextHolder.currentOrCreate());
        raw.setDeviceCode(request.getDeviceCode());
        raw.setTopic(request.getTopic());
        raw.setClientId(request.getClientId());
        raw.setTenantId(request.getTenantId());
        raw.setPayload(resolvePayloadBytes(request));

        upMessageDispatcher.dispatch(raw);
        return R.ok();
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
}
