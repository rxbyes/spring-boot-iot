package com.ghlzm.iot.message.dispatcher;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.springframework.stereotype.Component;
/**
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

    public void dispatch(RawDeviceMessage rawMessage) {
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(rawMessage.getProtocolCode());
        if (adapter == null) {
            throw new BizException("未找到协议适配器: " + rawMessage.getProtocolCode());
        }

        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(rawMessage.getTenantId());
        context.setProductKey(rawMessage.getProductKey());
        context.setDeviceCode(rawMessage.getDeviceCode());
        context.setTopic(rawMessage.getTopic());
        context.setClientId(rawMessage.getClientId());

        DeviceUpMessage upMessage = adapter.decode(rawMessage.getPayload(), context);
        upMessage.setProtocolCode(rawMessage.getProtocolCode());
        upMessage.setTopic(rawMessage.getTopic());

        deviceMessageService.handleUpMessage(upMessage);
    }
}
