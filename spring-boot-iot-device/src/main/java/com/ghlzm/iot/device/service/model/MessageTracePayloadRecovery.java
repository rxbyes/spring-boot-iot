package com.ghlzm.iot.device.service.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 链路追踪详情中的 Payload 恢复结果。
 */
@Data
public class MessageTracePayloadRecovery {

    private String rawPayload;
    private String decryptedPayload;
    private Map<String, Object> decodedPayload = new LinkedHashMap<>();
}
