package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.schema.SchemaManifestLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TdengineSchemaManifestSupportTest {

    private static final String MYSQL_MANIFEST_LOCATION =
            "classpath:schema/runtime-bootstrap/mysql-active-schema.json";
    private static final String TDENGINE_MANIFEST_LOCATION =
            "classpath:schema/runtime-bootstrap/tdengine-active-schema.json";

    @Test
    void autoBootstrapObjectsShouldExcludeHourlyAggregateStable() {
        TdengineSchemaManifestSupport support = new TdengineSchemaManifestSupport(
                SchemaManifestLoader.forClasspath(
                        MYSQL_MANIFEST_LOCATION,
                        TDENGINE_MANIFEST_LOCATION
                )
        );

        List<String> autoObjects = support.autoBootstrapObjects().stream()
                .map(SchemaManifestLoader.TdengineSchemaObject::name)
                .sorted()
                .toList();

        assertEquals(
                List.of(
                        "iot_device_telemetry_point",
                        "iot_raw_event_point",
                        "iot_raw_measure_point",
                        "iot_raw_status_point"
                ),
                autoObjects
        );
        assertEquals(
                "manual_bootstrap_required",
                support.requireObject("iot_agg_measure_hour").runtimeBootstrapMode()
        );
    }
}
