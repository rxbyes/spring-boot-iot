package com.ghlzm.iot.message.support;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 统一处理模拟上报 payload 的字节编码口径。
 */
public final class MessagePayloadEncodingSupport {

    private MessagePayloadEncodingSupport() {
    }

    public static byte[] resolvePayloadBytes(String payload, String payloadEncoding) {
        Charset charset = resolveCharset(payloadEncoding);
        return payload.getBytes(charset);
    }

    public static Charset resolveCharset(String payloadEncoding) {
        if (payloadEncoding == null || payloadEncoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        String normalized = payloadEncoding.trim().toLowerCase(Locale.ROOT);
        if ("iso-8859-1".equals(normalized) || "latin1".equals(normalized) || "latin-1".equals(normalized)) {
            return StandardCharsets.ISO_8859_1;
        }
        return StandardCharsets.UTF_8;
    }
}
