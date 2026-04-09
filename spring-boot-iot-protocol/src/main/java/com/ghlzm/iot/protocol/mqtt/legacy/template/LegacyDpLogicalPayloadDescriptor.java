package com.ghlzm.iot.protocol.mqtt.legacy.template;

import java.time.LocalDateTime;

record LegacyDpLogicalPayloadDescriptor(LocalDateTime timestamp,
                                        LegacyDpLogicalPayloadShape shape,
                                        Object latestValue,
                                        String rawPayload) {
}
