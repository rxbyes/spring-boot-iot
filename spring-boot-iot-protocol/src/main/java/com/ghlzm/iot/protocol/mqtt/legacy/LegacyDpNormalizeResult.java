package com.ghlzm.iot.protocol.mqtt.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record LegacyDpNormalizeResult(List<String> familyCodes,
                                      Map<String, Object> properties,
                                      String messageType,
                                      LocalDateTime timestamp,
                                      String timestampSource) {
}
