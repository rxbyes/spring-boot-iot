package com.ghlzm.iot.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ObservabilityConsoleLoggingConfigurerTest {

    @Test
    void configureShouldDisableMybatisNoiseLoggersWhenConsoleSwitchesAreOff() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getConsole().setMybatisSqlEnabled(false);
        properties.getObservability().getConsole().setMybatisSessionEnabled(false);
        LoggingSystem loggingSystem = mock(LoggingSystem.class);

        new ObservabilityConsoleLoggingConfigurer(properties).configure(loggingSystem);

        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SQL_LOGGER_NAME, LogLevel.OFF);
        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SESSION_LOGGER_NAME, LogLevel.OFF);
        verifyNoMoreInteractions(loggingSystem);
    }

    @Test
    void configureShouldEnableMybatisNoiseLoggersWithExpectedLevels() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getConsole().setMybatisSqlEnabled(true);
        properties.getObservability().getConsole().setMybatisSessionEnabled(true);
        LoggingSystem loggingSystem = mock(LoggingSystem.class);

        new ObservabilityConsoleLoggingConfigurer(properties).configure(loggingSystem);

        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SQL_LOGGER_NAME, LogLevel.TRACE);
        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SESSION_LOGGER_NAME, LogLevel.DEBUG);
        verifyNoMoreInteractions(loggingSystem);
    }
}
