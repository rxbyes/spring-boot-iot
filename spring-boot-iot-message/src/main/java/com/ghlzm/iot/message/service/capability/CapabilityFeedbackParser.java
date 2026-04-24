package com.ghlzm.iot.message.service.capability;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CapabilityFeedbackParser {

    public CapabilityFeedback parse(String rawPayload) {
        if (!StringUtils.hasText(rawPayload)) {
            return CapabilityFeedback.invalid(rawPayload, "反馈报文为空");
        }
        Map<String, String> values = new LinkedHashMap<>();
        String[] pairs = rawPayload.trim().split("&");
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = normalizeKey(pair.substring(0, index));
            String value = decode(pair.substring(index + 1));
            values.put(key, value);
        }

        String cmd = values.get("cmd");
        String result = values.get("result");
        String msgid = values.get("msgid");
        String message = values.get("message");
        if (!StringUtils.hasText(cmd) || !StringUtils.hasText(result) || !StringUtils.hasText(msgid)) {
            return CapabilityFeedback.invalid(rawPayload, "反馈报文缺少 cmd/result/msgid");
        }
        return CapabilityFeedback.valid(cmd, result, msgid, message, rawPayload);
    }

    private String normalizeKey(String key) {
        String value = key == null ? "" : key.trim();
        if (value.startsWith("$")) {
            value = value.substring(1);
        }
        return value.toLowerCase();
    }

    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
