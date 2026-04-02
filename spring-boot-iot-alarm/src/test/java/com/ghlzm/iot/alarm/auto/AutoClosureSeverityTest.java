package com.ghlzm.iot.alarm.auto;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoClosureSeverityTest {

    private final IotProperties.Alarm.AutoClosure config = new IotProperties.Alarm.AutoClosure();

    @Test
    void classifyShouldMatchThresholdBoundaries() {
        assertEquals(AutoClosureSeverity.BLUE, AutoClosureSeverity.classify(new BigDecimal("4.999"), config));
        assertEquals(AutoClosureSeverity.YELLOW, AutoClosureSeverity.classify(new BigDecimal("5"), config));
        assertEquals(AutoClosureSeverity.YELLOW, AutoClosureSeverity.classify(new BigDecimal("9.999"), config));
        assertEquals(AutoClosureSeverity.ORANGE, AutoClosureSeverity.classify(new BigDecimal("10"), config));
        assertEquals(AutoClosureSeverity.ORANGE, AutoClosureSeverity.classify(new BigDecimal("19.999"), config));
        assertEquals(AutoClosureSeverity.RED, AutoClosureSeverity.classify(new BigDecimal("20"), config));
    }

    @Test
    void riskPointLevelsShouldUseFourColorCodes() {
        assertEquals("blue", AutoClosureSeverity.BLUE.getRiskPointLevel());
        assertEquals("yellow", AutoClosureSeverity.YELLOW.getRiskPointLevel());
        assertEquals("orange", AutoClosureSeverity.ORANGE.getRiskPointLevel());
        assertEquals("red", AutoClosureSeverity.RED.getRiskPointLevel());
    }
}
