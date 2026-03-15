package com.ghlzm.iot.message.dispatcher;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 上行消息分发器。
 * 该层只负责协议适配器路由和统一消息转换，不承担业务落库职责。
 *
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:34
 */
@Component
public class UpMessageDispatcher {

    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final DeviceMessageService deviceMessageService;

    public UpMessageDispatcher(ProtocolAdapterRegistry protocolAdapterRegistry,
                               DeviceMessageService deviceMessageService) {
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.deviceMessageService = deviceMessageService;
    }

    public DeviceUpMessage dispatch(RawDeviceMessage rawMessage) {
        // 先按协议编码选择适配器，message 模块只做接入与分发。
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(rawMessage.getProtocolCode());
        if (adapter == null) {
            throw new BizException("未找到协议适配器: " + rawMessage.getProtocolCode());
        }

        // 构造协议上下文，把接入层已知的信息统一传给协议层。
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

        DeviceUpMessage upMessage = adapter.decode(rawMessage.getPayload(), context);
        if (upMessage == null) {
            throw new BizException("协议解析结果为空");
        }

        // 一期主链路要求上下游字段完整，这里对适配器未显式回填的字段做最小兜底。
        if (upMessage.getTenantId() == null || upMessage.getTenantId().isBlank()) {
            upMessage.setTenantId(rawMessage.getTenantId());
        }
        if (upMessage.getProductKey() == null || upMessage.getProductKey().isBlank()) {
            upMessage.setProductKey(rawMessage.getProductKey());
        }
        if (upMessage.getDeviceCode() == null || upMessage.getDeviceCode().isBlank()) {
            upMessage.setDeviceCode(rawMessage.getDeviceCode());
        }
        if (upMessage.getMessageType() == null || upMessage.getMessageType().isBlank()) {
            upMessage.setMessageType(rawMessage.getMessageType());
        }
        if (upMessage.getRawPayload() == null || upMessage.getRawPayload().isBlank()) {
            upMessage.setRawPayload(new String(rawMessage.getPayload(), StandardCharsets.UTF_8));
        }
        if (upMessage.getTimestamp() == null) {
            upMessage.setTimestamp(LocalDateTime.now());
        }
        upMessage.setProtocolCode(rawMessage.getProtocolCode());
        upMessage.setTopic(rawMessage.getTopic());

        // 分发完成后交给 device 模块处理落库、属性更新和在线状态刷新。
        deviceMessageService.handleUpMessage(upMessage);
        return upMessage;
    }

    private Map<String, Object> buildMetadata(RawDeviceMessage rawMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("messageType", rawMessage.getMessageType());
        metadata.put("topic", rawMessage.getTopic());
        metadata.put("clientId", rawMessage.getClientId());
        metadata.put("topicRouteType", rawMessage.getTopicRouteType());
        metadata.put("gatewayDeviceCode", rawMessage.getGatewayDeviceCode());
        metadata.put("subDeviceCode", rawMessage.getSubDeviceCode());
        return metadata;
    }
}
