package com.ghlzm.iot.message.service.capability;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.capability.DeviceCapabilityCode;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VideoCapabilityPayloadBuilder {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public CapabilityCommandPayload build(DeviceCapabilityCommandRequest request, String commandId) {
        if (request == null || request.getDevice() == null || request.getCapability() == null) {
            throw new BizException("视频能力请求参数不能为空");
        }
        String topic = resolveTopic(request.getDevice().getDeviceCode());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgid", commandId);
        payload.put("commandType", "capability");
        payload.put("capabilityCode", request.getCapability().code());
        payload.put("deviceCode", request.getDevice().getDeviceCode());
        payload.put("productKey", request.getProduct() == null ? null : request.getProduct().getProductKey());
        payload.put("params", request.getParams());
        return new CapabilityCommandPayload(topic, serialize(payload), "capability", request.getCapability().code(), commandId);
    }

    private String resolveTopic(String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            throw new BizException("设备编码不能为空");
        }
        return "/iot/video/" + deviceCode;
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new BizException("视频能力载荷序列化失败: " + ex.getMessage());
        }
    }
}
