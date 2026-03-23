package com.ghlzm.iot.framework.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObservabilityConsoleLoggingConfigurer {

    private final IotProperties iotProperties;

    @PostConstruct
    void initialize() {
        configure(LoggingSystem.get(getClass().getClassLoader()));
    }

    void configure(LoggingSystem loggingSystem) {
        IotProperties.Observability.Console console = iotProperties.getObservability().getConsole();
        loggingSystem.setLogLevel(
                MybatisLoggingConstants.SQL_LOGGER_NAME,
                Boolean.TRUE.equals(console.getMybatisSqlEnabled()) ? LogLevel.TRACE : LogLevel.OFF
        );
        loggingSystem.setLogLevel(
                MybatisLoggingConstants.SESSION_LOGGER_NAME,
                Boolean.TRUE.equals(console.getMybatisSessionEnabled()) ? LogLevel.DEBUG : LogLevel.OFF
        );
    }
}
