package com.ghlzm.iot.message.service.capability;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.capability.DeviceCapabilityCode;
import com.ghlzm.iot.device.capability.DeviceCapabilityDefinition;
import com.ghlzm.iot.device.capability.WarningDeviceKind;
import com.ghlzm.iot.device.service.model.DeviceCapabilityCommandRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class WarningCapabilityPayloadBuilder {

    public CapabilityCommandPayload build(DeviceCapabilityCommandRequest request, String commandId) {
        if (request == null || request.getDevice() == null || request.getProduct() == null || request.getCapability() == null) {
            throw new BizException("预警能力请求参数不能为空");
        }
        WarningDeviceKind kind = request.getMetadata() == null ? WarningDeviceKind.UNKNOWN : request.getMetadata().warningDeviceKind();
        String topic = resolveTopic(request.getDevice().getDeviceCode(), kind);
        String payloadText = resolvePayload(request, commandId);
        return new CapabilityCommandPayload(topic, payloadText, "capability", request.getCapability().code(), commandId);
    }

    private String resolveTopic(String deviceCode, WarningDeviceKind kind) {
        if (!StringUtils.hasText(deviceCode)) {
            throw new BizException("设备编码不能为空");
        }
        if (kind == WarningDeviceKind.LED) {
            return "/iot/led/" + deviceCode;
        }
        if (kind == WarningDeviceKind.FLASH) {
            return "/iot/flash/" + deviceCode;
        }
        return "/iot/broadcast/" + deviceCode;
    }

    private String resolvePayload(DeviceCapabilityCommandRequest request, String commandId) {
        String capabilityCode = request.getCapability().code();
        Map<String, Object> params = request.getParams();
        if (DeviceCapabilityCode.BROADCAST_PLAY.equals(capabilityCode)) {
            String content = stringParam(params, "content");
            int contentLength = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
            return queryPayload("broadcast")
                    .appendParam("b_num", integerText(params, "bNum", "1"))
                    .appendParam("b_size", String.valueOf(contentLength))
                    .appendParam("b_content", content)
                    .appendParam("volume", integerText(params, "volume", "80"))
                    .appendParam("msgid", commandId)
                    .build();
        }
        if (DeviceCapabilityCode.BROADCAST_STOP.equals(capabilityCode)) {
            return queryPayload("stop").appendParam("msgid", commandId).build();
        }
        if (DeviceCapabilityCode.BROADCAST_VOLUME.equals(capabilityCode)) {
            return queryPayload("play")
                    .appendParam("volume", integerText(params, "volume", null))
                    .appendParam("msgid", commandId)
                    .build();
        }
        if (DeviceCapabilityCode.LED_PROGRAM.equals(capabilityCode)) {
            return queryPayload("led")
                    .appendParam("type", integerText(params, "type", null))
                    .appendParam("brigh", integerText(params, "brigh", null))
                    .appendParam("freq", integerText(params, "freq", null))
                    .appendParam("msgid", commandId)
                    .build();
        }
        if (DeviceCapabilityCode.LED_STOP.equals(capabilityCode)) {
            return queryPayload("stop").appendParam("msgid", commandId).build();
        }
        if (DeviceCapabilityCode.FLASH_CONTROL.equals(capabilityCode)) {
            return queryPayload("flash")
                    .appendParam("type", integerText(params, "type", null))
                    .appendParam("brigh", integerText(params, "brigh", null))
                    .appendParam("freq", integerText(params, "freq", null))
                    .appendParam("msgid", commandId)
                    .build();
        }
        if (DeviceCapabilityCode.FLASH_STOP.equals(capabilityCode)) {
            return queryPayload("stop").appendParam("msgid", commandId).build();
        }
        if (DeviceCapabilityCode.REBOOT.equals(capabilityCode)) {
            return queryPayload("reboot").appendParam("msgid", commandId).build();
        }
        throw new BizException("当前预警能力未定义下发协议: " + capabilityCode);
    }

    private QueryPayloadBuilder queryPayload(String cmd) {
        return new QueryPayloadBuilder(cmd);
    }

    private String stringParam(Map<String, Object> params, String key) {
        Object value = params == null ? null : params.get(key);
        if (value == null) {
            throw new BizException("缺少能力参数: " + key);
        }
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            throw new BizException("能力参数不能为空: " + key);
        }
        return text;
    }

    private String integerText(Map<String, Object> params, String key, String defaultValue) {
        Object value = params == null ? null : params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return String.valueOf(number.intValue());
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private static final class QueryPayloadBuilder {

        private final StringBuilder builder;

        private QueryPayloadBuilder(String cmd) {
            this.builder = new StringBuilder("$cmd=").append(encode(cmd));
        }

        private QueryPayloadBuilder appendParam(String key, String value) {
            if (value == null) {
                return this;
            }
            builder.append('&').append(key).append('=').append(encode(value));
            return this;
        }

        private String build() {
            return builder.toString();
        }

        private static String encode(String value) {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
        }
    }
}
