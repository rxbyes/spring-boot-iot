package com.ghlzm.iot.message.service.capability;

public record CapabilityFeedback(
        boolean valid,
        String cmd,
        String result,
        String msgid,
        String message,
        String rawPayload,
        String invalidReason
) {

    public static CapabilityFeedback invalid(String rawPayload, String invalidReason) {
        return new CapabilityFeedback(false, null, null, null, null, rawPayload, invalidReason);
    }

    public static CapabilityFeedback valid(String cmd, String result, String msgid, String message, String rawPayload) {
        return new CapabilityFeedback(true, cmd, result, msgid, message, rawPayload, null);
    }
}
