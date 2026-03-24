package com.ghlzm.iot.protocol.mqtt.legacy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyDpPropertyNormalizerTest {

    private final LegacyDpPropertyNormalizer normalizer = new LegacyDpPropertyNormalizer();

    @Test
    void shouldNormalizeStatusPayloadForS1ZtFamily() {
        LegacyDpNormalizeResult result = normalizer.normalize(Map.of(
                "17165802", Map.of(
                        "S1_ZT_1", Map.of(
                                "2026-03-14T06:00:00.000Z", Map.of(
                                        "temp", 16.2,
                                        "humidity", 81.5,
                                        "sensor_state", "OK"
                                )
                        )
                )
        ), "17165802", List.of("S1_ZT_1"));

        assertEquals("status", result.messageType());
        assertEquals("PAYLOAD_LATEST_TIMESTAMP", result.timestampSource());
        assertEquals(LocalDateTime.of(2026, 3, 14, 14, 0), result.timestamp());
        assertEquals(16.2, result.properties().get("S1_ZT_1.temp"));
        assertEquals(81.5, result.properties().get("S1_ZT_1.humidity"));
        assertEquals("OK", result.properties().get("S1_ZT_1.sensor_state"));
    }

    @Test
    void shouldNormalizeGpsPayloadForL1GpFamily() {
        LegacyDpNormalizeResult result = normalizer.normalize(Map.of(
                "17165802", Map.of(
                        "L1_GP_1", Map.of(
                                "2026-03-14T06:00:00.000Z", Map.of(
                                        "gpsTotalZ", 3.2,
                                        "gpsTotalX", 9.9,
                                        "gpsTotalY", 0.5
                                )
                        )
                )
        ), "17165802", List.of("L1_GP_1"));

        assertEquals("property", result.messageType());
        assertEquals(LocalDateTime.of(2026, 3, 14, 14, 0), result.timestamp());
        assertEquals(3.2, result.properties().get("L1_GP_1.gpsTotalZ"));
        assertEquals(9.9, result.properties().get("L1_GP_1.gpsTotalX"));
        assertEquals(0.5, result.properties().get("L1_GP_1.gpsTotalY"));
    }

    @Test
    void shouldNormalizeInclinationPayloadForL1QjFamily() {
        LegacyDpNormalizeResult result = normalizer.normalize(Map.of(
                "QJ-001", Map.of(
                        "L1_QJ_1", Map.of(
                                "2026-03-15T01:02:03.000Z", Map.of(
                                        "angle", 1.2,
                                        "azi", 90.5,
                                        "x", 0.11,
                                        "y", 0.22,
                                        "z", 0.33
                                )
                        )
                )
        ), "QJ-001", List.of("L1_QJ_1"));

        assertEquals("property", result.messageType());
        assertEquals(LocalDateTime.of(2026, 3, 15, 9, 2, 3), result.timestamp());
        assertEquals(1.2, result.properties().get("L1_QJ_1.angle"));
        assertEquals(90.5, result.properties().get("L1_QJ_1.azi"));
        assertEquals(0.11, result.properties().get("L1_QJ_1.x"));
        assertEquals(0.22, result.properties().get("L1_QJ_1.y"));
        assertEquals(0.33, result.properties().get("L1_QJ_1.z"));
    }

    @Test
    void shouldNormalizeAccelerationPayloadForL1JsFamily() {
        LegacyDpNormalizeResult result = normalizer.normalize(Map.of(
                "JS-001", Map.of(
                        "L1_JS_1", Map.of(
                                "2026-03-15T01:02:03.000Z", Map.of(
                                        "gx", 0.1,
                                        "gy", 0.2,
                                        "gz", "0.3"
                                )
                        )
                )
        ), "JS-001", List.of("L1_JS_1"));

        assertEquals("property", result.messageType());
        assertEquals(LocalDateTime.of(2026, 3, 15, 9, 2, 3), result.timestamp());
        assertEquals(0.1, result.properties().get("L1_JS_1.gx"));
        assertEquals(0.2, result.properties().get("L1_JS_1.gy"));
        assertEquals("0.3", result.properties().get("L1_JS_1.gz"));
    }

    @Test
    void shouldCollapseScalarTimestampContainersForLegacyFamilies() {
        LegacyDpNormalizeResult result = normalizer.normalize(Map.of(
                "484021", Map.of(
                        "L1_LF_1", Map.of(
                                "2018-08-02T08:52:32.449Z", 11.2,
                                "2018-08-02T10:52:32.449Z", 10.9
                        )
                )
        ), "484021", List.of("L1_LF_1"));

        assertEquals("property", result.messageType());
        assertEquals(LocalDateTime.of(2018, 8, 2, 18, 52, 32, 449_000_000), result.timestamp());
        assertEquals("10.9", String.valueOf(result.properties().get("L1_LF_1")));
    }
}
