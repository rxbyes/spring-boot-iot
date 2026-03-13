package com.ghlzm.iot.protocol.core.model;
import lombok.Data;

import java.util.Map;
/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:06
 */
@Data
public class DeviceDownMessage {

    private String messageId;
    private String commandType;
    private String serviceIdentifier;
    private Map<String, Object> params;
}

