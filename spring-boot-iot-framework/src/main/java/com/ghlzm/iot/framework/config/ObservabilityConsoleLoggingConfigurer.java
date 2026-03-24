package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.messageflow.MessageFlowLoggingConstants;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObservabilityConsoleLoggingConfigurer {

    private final IotProperties iotProperties;
    private final MessageFlowProperties messageFlowProperties;

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
        IotProperties.Observability.Diagnostic diagnostic = iotProperties.getObservability().getDiagnostic();
        loggingSystem.setLogLevel(
                DiagnosticLoggingConstants.DIAGNOSTIC_SQL_LOGGER_NAME,
                diagnostic != null && Boolean.TRUE.equals(diagnostic.getSqlFileEnabled()) ? LogLevel.INFO : LogLevel.OFF
        );
        loggingSystem.setLogLevel(
                DiagnosticLoggingConstants.DIAGNOSTIC_ACCESS_LOGGER_NAME,
                diagnostic != null && Boolean.TRUE.equals(diagnostic.getAccessFileEnabled()) ? LogLevel.INFO : LogLevel.OFF
        );
        loggingSystem.setLogLevel(
                MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME,
                Boolean.TRUE.equals(messageFlowProperties.getEnabled()) ? LogLevel.INFO : LogLevel.OFF
        );
    }
}
