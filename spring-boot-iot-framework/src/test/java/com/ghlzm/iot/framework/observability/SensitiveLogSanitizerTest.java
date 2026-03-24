package com.ghlzm.iot.framework.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveLogSanitizerTest {

    @Test
    void sanitizeShouldMaskJsonKvAndAuthorizationHeader() {
        String raw = """
                {"password":"123456","token":"abc","nested":"{\\"clientSecret\\":\\"xyz\\"}"} authorization: Bearer jwt-token-demo
                refreshToken=refresh-demo secret=my-secret
                """;

        String sanitized = SensitiveLogSanitizer.sanitize(raw);

        assertTrue(sanitized.contains("\"password\":\"***\""));
        assertTrue(sanitized.contains("\"token\":\"***\""));
        assertTrue(sanitized.contains("\\\"clientSecret\\\":\\\"***\\\""));
        assertTrue(sanitized.contains("refreshToken=***"));
        assertTrue(sanitized.contains("secret=***"));
        assertTrue(sanitized.contains("authorization: Bearer ***"));
        assertFalse(sanitized.contains("123456"));
        assertFalse(sanitized.contains("jwt-token-demo"));
    }
}
