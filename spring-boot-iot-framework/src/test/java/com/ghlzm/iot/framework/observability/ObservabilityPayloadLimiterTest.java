package com.ghlzm.iot.framework.observability;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityPayloadLimiterTest {

    @Test
    void toJsonShouldMaskSecretsAndTruncateLargeValues() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("apiKey", "secret-demo");
        payload.put("longValue", "x".repeat(400));

        String json = ObservabilityPayloadLimiter.toJson(payload);

        assertTrue(json.contains("\"apiKey\":\"***\""));
        assertTrue(json.contains("...(truncated)"));
        assertTrue(json.length() <= 2000);
    }

    @Test
    void toJsonShouldLimitNestedCollections() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", IntStream.range(0, 20).boxed().toList());

        String json = ObservabilityPayloadLimiter.toJson(payload);

        assertTrue(json.contains("...(truncated)"));
    }
}
