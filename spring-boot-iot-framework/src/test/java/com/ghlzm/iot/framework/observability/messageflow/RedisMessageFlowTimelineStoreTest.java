package com.ghlzm.iot.framework.observability.messageflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RedisMessageFlowTimelineStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private MessageFlowProperties messageFlowProperties;
    private RedisMessageFlowTimelineStore store;

    @BeforeEach
    void setUp() {
        messageFlowProperties = new MessageFlowProperties();
        messageFlowProperties.setEnabled(true);
        messageFlowProperties.setTtlHours(24);
        messageFlowProperties.setSessionMatchWindowSeconds(120);
        messageFlowProperties.setRecentSessionLimit(500);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        store = new RedisMessageFlowTimelineStore(stringRedisTemplate, messageFlowProperties);
    }

    @Test
    void saveSessionShouldWriteExpectedRedisKeyAndTtl() throws Exception {
        MessageFlowSession session = new MessageFlowSession();
        session.setSessionId("session-001");
        session.setTransportMode("MQTT");
        session.setStatus(MessageFlowStatuses.SESSION_PUBLISHED);
        session.setSubmittedAt(LocalDateTime.of(2026, 3, 23, 10, 30));

        store.saveSession(session);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());
        assertEquals("iot:message-flow:session:session-001", keyCaptor.getValue());
        assertEquals(Duration.ofHours(24), ttlCaptor.getValue());
        MessageFlowSession stored = objectMapper.readValue(jsonCaptor.getValue(), MessageFlowSession.class);
        assertEquals("session-001", stored.getSessionId());
        assertEquals("MQTT", stored.getTransportMode());
        verify(zSetOperations).add("iot:message-flow:recent:sessions", "session-001", session.getSubmittedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Test
    void saveTimelineShouldWriteExpectedRedisKeyAndTtl() throws Exception {
        MessageFlowTimeline timeline = new MessageFlowTimeline();
        timeline.setTraceId("trace-001");
        timeline.setSessionId("session-001");
        timeline.setStatus(MessageFlowStatuses.SESSION_COMPLETED);

        store.saveTimeline(timeline);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), jsonCaptor.capture(), ttlCaptor.capture());
        assertEquals("iot:message-flow:trace:trace-001", keyCaptor.getValue());
        assertEquals(Duration.ofHours(24), ttlCaptor.getValue());
        MessageFlowTimeline stored = objectMapper.readValue(jsonCaptor.getValue(), MessageFlowTimeline.class);
        assertEquals("trace-001", stored.getTraceId());
        assertEquals("session-001", stored.getSessionId());
    }

    @Test
    void getSessionAndTraceShouldReturnEmptyWhenRedisEntryExpired() {
        when(valueOperations.get("iot:message-flow:session:session-expired")).thenReturn(null);
        when(valueOperations.get("iot:message-flow:trace:trace-expired")).thenReturn("   ");

        Optional<MessageFlowSession> session = store.getSession("session-expired");
        Optional<MessageFlowTimeline> timeline = store.getTimeline("trace-expired");

        assertTrue(session.isEmpty());
        assertTrue(timeline.isEmpty());
    }

    @Test
    void bindFingerprintAndReadShouldUseShortWindowTtl() {
        store.bindFingerprint("fingerprint-001", "session-001");
        verify(valueOperations).set(
                "iot:message-flow:fingerprint:fingerprint-001",
                "session-001",
                Duration.ofSeconds(120)
        );

        when(valueOperations.get("iot:message-flow:fingerprint:fingerprint-002")).thenReturn(" session-002 ");
        Optional<String> sessionId = store.getSessionIdByFingerprint("fingerprint-002");
        assertTrue(sessionId.isPresent());
        assertEquals("session-002", sessionId.get());

        when(valueOperations.get("iot:message-flow:fingerprint:fingerprint-missing")).thenReturn(null);
        assertFalse(store.getSessionIdByFingerprint("fingerprint-missing").isPresent());
    }

    @Test
    void getRecentSessionIdsShouldUseReverseRangeAndAllowPruningExpiredMembers() {
        when(zSetOperations.reverseRange("iot:message-flow:recent:sessions", 0, 2)).thenReturn(Set.of("session-003", "session-002", "session-001"));

        List<String> sessionIds = store.getRecentSessionIds(3);

        assertEquals(3, sessionIds.size());
        assertTrue(sessionIds.contains("session-001"));
        store.removeRecentSession("session-002");
        verify(zSetOperations).remove("iot:message-flow:recent:sessions", "session-002");
    }
}
