package com.ghlzm.iot.message.service.capability;

import java.nio.charset.StandardCharsets;

public record CapabilityCommandPayload(
        String topic,
        String payloadText,
        String commandType,
        String serviceIdentifier,
        String commandId
) {

    public byte[] payloadBytes() {
        return payloadText == null ? new byte[0] : payloadText.getBytes(StandardCharsets.UTF_8);
    }
}
