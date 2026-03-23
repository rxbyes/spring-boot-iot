package com.ghlzm.iot.framework.observability.messageflow;

import java.util.Optional;

/**
 * message-flow 时间线存储。
 */
public interface MessageFlowTimelineStore {

    void saveSession(MessageFlowSession session);

    Optional<MessageFlowSession> getSession(String sessionId);

    void saveTimeline(MessageFlowTimeline timeline);

    Optional<MessageFlowTimeline> getTimeline(String traceId);

    void bindFingerprint(String fingerprint, String sessionId);

    Optional<String> getSessionIdByFingerprint(String fingerprint);
}
