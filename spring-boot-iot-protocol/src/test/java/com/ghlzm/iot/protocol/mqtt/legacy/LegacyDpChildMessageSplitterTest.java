package com.ghlzm.iot.protocol.mqtt.legacy;

import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDpChildMessageSplitterTest {

    private final LegacyDpChildMessageSplitter splitter = new LegacyDpChildMessageSplitter();

    @Test
    void shouldSplitConfiguredDeepDisplacementChildren() {
        Map<String, String> subDeviceMappings = new LinkedHashMap<>();
        subDeviceMappings.put("L1_SW_1", "84330701");
        subDeviceMappings.put("L1_SW_2", "84330695");

        LegacyDpChildMessageSplitter.SplitResult splitResult = splitter.split(
                Map.of(
                        "SK00FB0D1310195", Map.of(
                                "L1_SW_1", Map.of(
                                        "2026-03-20T06:24:02.000Z", Map.of("dispsX", -0.0445, "dispsY", 0.0293)
                                ),
                                "L1_SW_2", Map.of(
                                        "2026-03-20T06:24:02.000Z", Map.of("dispsX", -0.0293, "dispsY", 0.0330)
                                )
                        )
                ),
                "SK00FB0D1310195",
                "1",
                "demo-product",
                "property",
                "$dp",
                LocalDateTime.of(2026, 3, 20, 14, 24, 2),
                subDeviceMappings
        );

        assertEquals(2, splitResult.messages().size());
        assertEquals(2, splitResult.logicalCodes().size());
        assertEquals("L1_SW_1", splitResult.logicalCodes().get(0));
        assertEquals("L1_SW_2", splitResult.logicalCodes().get(1));

        DeviceUpMessage childMessage1 = splitResult.messages().get(0);
        assertEquals("84330701", childMessage1.getDeviceCode());
        assertEquals("property", childMessage1.getMessageType());
        assertEquals(LocalDateTime.of(2026, 3, 20, 14, 24, 2), childMessage1.getTimestamp());
        assertEquals(-0.0445, childMessage1.getProperties().get("dispsX"));
        assertEquals(0.0293, childMessage1.getProperties().get("dispsY"));

        DeviceUpMessage childMessage2 = splitResult.messages().get(1);
        assertEquals("84330695", childMessage2.getDeviceCode());
        assertEquals(-0.0293, childMessage2.getProperties().get("dispsX"));
        assertEquals(0.0330, childMessage2.getProperties().get("dispsY"));
        assertNotNull(childMessage2.getRawPayload());
        assertTrue(childMessage2.getRawPayload().contains("L1_SW_2"));
    }
}
