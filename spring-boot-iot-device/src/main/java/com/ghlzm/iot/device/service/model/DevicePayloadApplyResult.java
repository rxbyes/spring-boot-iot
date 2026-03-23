package com.ghlzm.iot.device.service.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * payload 应用结果。
 */
@Data
public class DevicePayloadApplyResult {

    private String branch;
    private Map<String, Object> summary = new LinkedHashMap<>();
}
