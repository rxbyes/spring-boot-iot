package com.ghlzm.iot.alarm.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class AlarmRecordServiceImplTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AlarmRecordServiceImpl.class);
    private final Level originalLevel = logger.getLevel();

    @AfterEach
    void tearDown() {
        logger.setLevel(originalLevel);
        logger.detachAndStopAllAppenders();
    }

    @Test
    void addAlarmShouldLogLifecycleSummary() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        AlarmRecordServiceImpl service = spy(new AlarmRecordServiceImpl());
        doReturn(true).when(service).save(any(AlarmRecord.class));

        AlarmRecord alarm = new AlarmRecord();
        alarm.setId(501L);
        alarm.setAlarmCode("AL-501");
        alarm.setDeviceCode("DEV-501");
        alarm.setAlarmLevel("high");

        AlarmRecord result = service.addAlarm(alarm);

        assertSame(alarm, result);
        assertEquals(0, alarm.getStatus());
        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("event=\"alarm_lifecycle\""));
        assertTrue(message.contains("action=\"create\""));
        assertTrue(message.contains("result=\"success\""));
        assertTrue(message.contains("alarmId=501"));
        assertTrue(message.contains("alarmCode=\"AL-501\""));
        assertTrue(message.contains("deviceCode=\"DEV-501\""));
    }
}
