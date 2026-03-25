package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * `$dp` 标准化与子消息拆分的中间结果。
 */
@Data
public class LegacyDpNormalizeResult {

    private Map<String, Object> properties = new LinkedHashMap<>();
    private LocalDateTime timestamp;
    private String timestampSource;
    private String messageType;
    private List<String> familyCodes = List.of();
    private List<DeviceUpMessage> childMessages = List.of();
    private Boolean childSplitApplied = Boolean.FALSE;
}
