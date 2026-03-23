package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * TDengine 时序表启动初始化。
 */
@Component
public class TdengineTelemetrySchemaInitializer implements ApplicationRunner {

    private final IotProperties iotProperties;
    private final TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport;

    public TdengineTelemetrySchemaInitializer(IotProperties iotProperties,
                                              TdengineTelemetrySchemaSupport tdengineTelemetrySchemaSupport) {
        this.iotProperties = iotProperties;
        this.tdengineTelemetrySchemaSupport = tdengineTelemetrySchemaSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (iotProperties.getTelemetry() == null
                || iotProperties.getTelemetry().getStorageType() == null
                || !"tdengine".equalsIgnoreCase(iotProperties.getTelemetry().getStorageType())) {
            return;
        }
        tdengineTelemetrySchemaSupport.ensureTable();
    }
}
