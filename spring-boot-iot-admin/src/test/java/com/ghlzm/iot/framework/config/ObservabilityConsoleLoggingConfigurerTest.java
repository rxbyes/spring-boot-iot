package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowLoggingConstants;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
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
        properties.getObservability().getDiagnostic().setSqlFileEnabled(false);
        properties.getObservability().getDiagnostic().setAccessFileEnabled(false);
        MessageFlowProperties messageFlowProperties = new MessageFlowProperties();
        messageFlowProperties.setEnabled(false);
        LoggingSystem loggingSystem = mock(LoggingSystem.class);

        new ObservabilityConsoleLoggingConfigurer(properties, messageFlowProperties).configure(loggingSystem);

        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SQL_LOGGER_NAME, LogLevel.OFF);
        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SESSION_LOGGER_NAME, LogLevel.OFF);
        verify(loggingSystem).setLogLevel(DiagnosticLoggingConstants.DIAGNOSTIC_SQL_LOGGER_NAME, LogLevel.OFF);
        verify(loggingSystem).setLogLevel(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME, LogLevel.OFF);
        verify(loggingSystem).setLogLevel(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME, LogLevel.OFF);
        verifyNoMoreInteractions(loggingSystem);
    }

    @Test
    void configureShouldEnableMybatisNoiseLoggersWithExpectedLevels() {
        IotProperties properties = new IotProperties();
        properties.getObservability().getConsole().setMybatisSqlEnabled(true);
        properties.getObservability().getConsole().setMybatisSessionEnabled(true);
        properties.getObservability().getDiagnostic().setSqlFileEnabled(true);
        properties.getObservability().getDiagnostic().setAccessFileEnabled(true);
        MessageFlowProperties messageFlowProperties = new MessageFlowProperties();
        messageFlowProperties.setEnabled(true);
        LoggingSystem loggingSystem = mock(LoggingSystem.class);

        new ObservabilityConsoleLoggingConfigurer(properties, messageFlowProperties).configure(loggingSystem);

        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SQL_LOGGER_NAME, LogLevel.TRACE);
        verify(loggingSystem).setLogLevel(MybatisLoggingConstants.SESSION_LOGGER_NAME, LogLevel.DEBUG);
        verify(loggingSystem).setLogLevel(DiagnosticLoggingConstants.DIAGNOSTIC_SQL_LOGGER_NAME, LogLevel.INFO);
        verify(loggingSystem).setLogLevel(DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME, LogLevel.INFO);
        verify(loggingSystem).setLogLevel(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME, LogLevel.INFO);
        verifyNoMoreInteractions(loggingSystem);
    }
}
