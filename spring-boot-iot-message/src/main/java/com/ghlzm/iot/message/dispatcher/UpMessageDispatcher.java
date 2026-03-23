package com.ghlzm.iot.message.dispatcher;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上行消息分发器。
 */
@Component
public class UpMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpMessageDispatcher.class);

    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final DeviceMessageService deviceMessageService;

    public UpMessageDispatcher(ProtocolAdapterRegistry protocolAdapterRegistry,
                               DeviceMessageService deviceMessageService) {
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.deviceMessageService = deviceMessageService;
    }

    public DeviceUpMessage dispatch(RawDeviceMessage rawMessage) {
        long startNs = System.nanoTime();
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(rawMessage.getProtocolCode());
        if (adapter == null) {
            log.warn(ObservabilityEventLogSupport.summary(
                    "protocol_decode",
                    "failure",
                    elapsedMillis(startNs),
                    buildFailureDetails(rawMessage, "adapter_not_found", null)
            ));
            throw new BizException("未找到协议适配器: " + rawMessage.getProtocolCode());
        }

        String traceId = hasText(rawMessage.getTraceId())
                ? rawMessage.getTraceId()
                : TraceContextHolder.currentOrCreate();
        rawMessage.setTraceId(traceId);

        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(rawMessage.getTenantId());
        context.setProductKey(rawMessage.getProductKey());
        context.setDeviceCode(rawMessage.getDeviceCode());
        context.setGatewayDeviceCode(rawMessage.getGatewayDeviceCode());
        context.setSubDeviceCode(rawMessage.getSubDeviceCode());
        context.setTopicRouteType(rawMessage.getTopicRouteType());
        context.setMessageType(rawMessage.getMessageType());
        context.setTopic(rawMessage.getTopic());
        context.setClientId(rawMessage.getClientId());
        context.setMetadata(buildMetadata(rawMessage));

        try {
            DeviceUpMessage upMessage = adapter.decode(rawMessage.getPayload(), context);
            if (upMessage == null) {
                throw new BizException("协议解析结果为空");
            }

            if (!hasText(upMessage.getTenantId())) {
                upMessage.setTenantId(rawMessage.getTenantId());
            }
            if (!hasText(upMessage.getProductKey())) {
                upMessage.setProductKey(rawMessage.getProductKey());
            }
            if (!hasText(upMessage.getDeviceCode())) {
                upMessage.setDeviceCode(rawMessage.getDeviceCode());
            }
            if (!hasText(upMessage.getMessageType())) {
                upMessage.setMessageType(rawMessage.getMessageType());
            }
            if (!hasText(upMessage.getRawPayload())) {
                upMessage.setRawPayload(new String(rawMessage.getPayload(), StandardCharsets.UTF_8));
            }
            if (upMessage.getTimestamp() == null) {
                upMessage.setTimestamp(LocalDateTime.now());
            }
            if (!hasText(upMessage.getTraceId())) {
                upMessage.setTraceId(traceId);
            }
            upMessage.setProtocolCode(rawMessage.getProtocolCode());
            upMessage.setTopic(rawMessage.getTopic());
            enrichRawMessage(rawMessage, upMessage);

            deviceMessageService.handleUpMessage(upMessage);
            return upMessage;
        } catch (RuntimeException ex) {
            log.warn(ObservabilityEventLogSupport.summary(
                    resolveFailureEvent(ex),
                    "failure",
                    elapsedMillis(startNs),
                    buildFailureDetails(rawMessage, resolveFailureReason(ex), ex)
            ), ex);
            throw ex;
        }
    }

    private void enrichRawMessage(RawDeviceMessage rawMessage, DeviceUpMessage upMessage) {
        if (rawMessage == null || upMessage == null) {
            return;
        }
        if (hasText(upMessage.getDeviceCode())) {
            rawMessage.setDeviceCode(upMessage.getDeviceCode());
        }
        if (hasText(upMessage.getProductKey())) {
            rawMessage.setProductKey(upMessage.getProductKey());
        }
        if (hasText(upMessage.getMessageType())) {
            rawMessage.setMessageType(upMessage.getMessageType());
        }
        if (!hasText(rawMessage.getClientId()) && hasText(upMessage.getDeviceCode())) {
            rawMessage.setClientId(upMessage.getDeviceCode());
        }
        if (hasText(upMessage.getProtocolCode())) {
            rawMessage.setProtocolCode(upMessage.getProtocolCode());
        }
    }

    private Map<String, Object> buildMetadata(RawDeviceMessage rawMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("traceId", rawMessage.getTraceId());
        metadata.put("messageType", rawMessage.getMessageType());
        metadata.put("topic", rawMessage.getTopic());
        metadata.put("clientId", rawMessage.getClientId());
        metadata.put("productKey", rawMessage.getProductKey());
        metadata.put("deviceCode", rawMessage.getDeviceCode());
        metadata.put("topicRouteType", rawMessage.getTopicRouteType());
        metadata.put("gatewayDeviceCode", rawMessage.getGatewayDeviceCode());
        metadata.put("subDeviceCode", rawMessage.getSubDeviceCode());
        return metadata;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> buildFailureDetails(RawDeviceMessage rawMessage, String reason, Throwable throwable) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", rawMessage == null ? TraceContextHolder.getTraceId() : rawMessage.getTraceId());
        details.put("protocolCode", rawMessage == null ? null : rawMessage.getProtocolCode());
        details.put("topic", rawMessage == null ? null : rawMessage.getTopic());
        details.put("deviceCode", rawMessage == null ? null : rawMessage.getDeviceCode());
        details.put("productKey", rawMessage == null ? null : rawMessage.getProductKey());
        details.put("clientId", rawMessage == null ? null : rawMessage.getClientId());
        details.put("messageType", rawMessage == null ? null : rawMessage.getMessageType());
        details.put("reason", reason);
        if (throwable != null) {
            details.put("errorClass", throwable.getClass().getSimpleName());
        }
        return details;
    }

    private String resolveFailureEvent(RuntimeException ex) {
        String message = ex == null || ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("协议解析") || message.contains("decode")) {
            return "protocol_decode";
        }
        if (message.contains("设备不存在")
                || message.contains("协议不匹配")
                || message.contains("协议未配置")
                || message.contains("协议配置异常")
                || message.contains("产品不匹配")
                || message.contains("产品不存在")
                || message.contains("未绑定产品")) {
            return "device_contract_validation";
        }
        return "message_dispatch";
    }

    private String resolveFailureReason(RuntimeException ex) {
        String message = ex == null ? null : ex.getMessage();
        if (!hasText(message)) {
            return ex == null ? null : ex.getClass().getSimpleName();
        }
        return message;
    }

    private long elapsedMillis(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }
}
