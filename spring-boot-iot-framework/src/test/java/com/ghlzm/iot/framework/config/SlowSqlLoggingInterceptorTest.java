package com.ghlzm.iot.framework.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlowSqlLoggingInterceptorTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_SQL_LOGGER_NAME);
    private final Level originalLevel = logger.getLevel();

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLevel);
        logger.detachAndStopAllAppenders();
    }

    @Test
    void interceptShouldLogSummaryWhenThresholdExceeded() throws Throwable {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        IotProperties properties = new IotProperties();
        properties.getObservability().getPerformance().setSlowSqlThresholdMs(5L);
        SlowSqlLoggingInterceptor interceptor = new SlowSqlLoggingInterceptor(properties);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{
                buildMappedStatement("com.ghlzm.iot.system.mapper.UserMapper.selectList"),
                Map.of("username", "demo")
        });
        when(invocation.proceed()).thenAnswer(answer -> {
            LockSupport.parkNanos(20_000_000L);
            return List.of("a", "b");
        });

        Object result = interceptor.intercept(invocation);

        assertEquals(List.of("a", "b"), result);
        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("event=\"slow_sql\""));
        assertTrue(message.contains("statementId=\"com.ghlzm.iot.system.mapper.UserMapper.selectList\""));
        assertTrue(message.contains("rowCount=2"));
    }

    @Test
    void interceptShouldSkipSummaryWhenThresholdNotReached() throws Throwable {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        IotProperties properties = new IotProperties();
        properties.getObservability().getPerformance().setSlowSqlThresholdMs(1000L);
        SlowSqlLoggingInterceptor interceptor = new SlowSqlLoggingInterceptor(properties);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{
                buildMappedStatement("com.ghlzm.iot.system.mapper.UserMapper.selectList"),
                Map.of("username", "demo")
        });
        when(invocation.proceed()).thenReturn(List.of("a"));

        interceptor.intercept(invocation);

        assertEquals(0, appender.list.size());
    }

    private MappedStatement buildMappedStatement(String id) {
        Configuration configuration = new Configuration();
        StaticSqlSource sqlSource = new StaticSqlSource(configuration, "select * from sys_user where username = ?");
        return new MappedStatement.Builder(configuration, id, sqlSource, SqlCommandType.SELECT).build();
    }
}
