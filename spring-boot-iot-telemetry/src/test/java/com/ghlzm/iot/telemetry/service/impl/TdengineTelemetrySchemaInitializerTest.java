package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TdengineTelemetrySchemaInitializerTest {

    @Mock
    private TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport;
    @Mock
    private TelemetryV2SchemaSupport telemetryV2SchemaSupport;

    private IotProperties iotProperties;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        iotProperties.getTelemetry().setStorageType("tdengine");
    }

    @Test
    void shouldInitializeLegacyAndV2TelemetrySchemaWhenTdengineEnabled() throws Exception {
        TdengineTelemetrySchemaInitializer initializer = new TdengineTelemetrySchemaInitializer(
                iotProperties,
                tdengineTelemetrySchemaSupport,
                telemetryV2SchemaSupport
        );

        initializer.run(null);

        verify(tdengineTelemetrySchemaSupport).ensureTable();
        verify(telemetryV2SchemaSupport).ensureTables();
    }

    @Test
    void shouldSkipSchemaInitializationWhenTdengineDisabled() throws Exception {
        iotProperties.getTelemetry().setStorageType("mysql");
        TdengineTelemetrySchemaInitializer initializer = new TdengineTelemetrySchemaInitializer(
                iotProperties,
                tdengineTelemetrySchemaSupport,
                telemetryV2SchemaSupport
        );

        initializer.run(null);

        verifyNoInteractions(tdengineTelemetrySchemaSupport, telemetryV2SchemaSupport);
    }
}
