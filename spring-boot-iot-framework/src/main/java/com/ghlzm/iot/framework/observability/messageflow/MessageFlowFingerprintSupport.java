package com.ghlzm.iot.framework.observability.messageflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * message-flow fingerprint 计算工具。
 */
public final class MessageFlowFingerprintSupport {

    private MessageFlowFingerprintSupport() {
    }

    public static String buildFingerprint(String topic, String deviceCode, byte[] payload) {
        String normalizedTopic = topic == null ? "" : topic.trim();
        String normalizedDeviceCode = deviceCode == null ? "" : deviceCode.trim();
        String payloadHex = toHex(payload);
        return sha256(normalizedTopic + "\n" + normalizedDeviceCode + "\n" + payloadHex);
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
