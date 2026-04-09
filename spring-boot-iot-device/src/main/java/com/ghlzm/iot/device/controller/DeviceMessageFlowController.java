package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.MessageFlowRecentQuery;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowCorrelationCountVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowLookupCountVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowOpsOverviewVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowRecentSessionVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowSessionVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowSessionCountVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowStageMetricVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowStepVO;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowTimelineVO;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStep;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * message-flow 查询控制器。
 */
@RestController
public class DeviceMessageFlowController {

    private final MessageFlowTimelineStore messageFlowTimelineStore;
    private final MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    private final MessageFlowProperties messageFlowProperties;
    private final DeviceMessageService deviceMessageService;

    public DeviceMessageFlowController(MessageFlowTimelineStore messageFlowTimelineStore,
                                       MessageFlowMetricsRecorder messageFlowMetricsRecorder,
                                       MessageFlowProperties messageFlowProperties,
                                       DeviceMessageService deviceMessageService) {
        this.messageFlowTimelineStore = messageFlowTimelineStore;
        this.messageFlowMetricsRecorder = messageFlowMetricsRecorder;
        this.messageFlowProperties = messageFlowProperties;
        this.deviceMessageService = deviceMessageService;
    }

    @GetMapping("/api/device/message-flow/session/{sessionId}")
    public R<MessageFlowSessionVO> getSession(@PathVariable String sessionId) {
        try {
            Optional<MessageFlowSession> session = messageFlowTimelineStore.getSession(sessionId);
            if (session.isEmpty()) {
                messageFlowMetricsRecorder.recordLookup(
                        MessageFlowMetricsRecorder.LOOKUP_TARGET_SESSION,
                        MessageFlowMetricsRecorder.LOOKUP_RESULT_MISS
                );
                return R.ok(null);
            }
            MessageFlowSession resolvedSession = session.get();
            recordCorrelationMissIfTimedOut(resolvedSession);
            MessageFlowSessionVO sessionVO = toSessionVO(resolvedSession);
            if (hasText(resolvedSession.getTraceId())) {
                sessionVO.setTimeline(messageFlowTimelineStore.getTimeline(resolvedSession.getTraceId())
                        .map(this::toTimelineVO)
                        .orElse(null));
            }
            messageFlowMetricsRecorder.recordLookup(
                    MessageFlowMetricsRecorder.LOOKUP_TARGET_SESSION,
                    MessageFlowMetricsRecorder.LOOKUP_RESULT_HIT
            );
            return R.ok(sessionVO);
        } catch (RuntimeException ex) {
            messageFlowMetricsRecorder.recordLookup(
                    MessageFlowMetricsRecorder.LOOKUP_TARGET_SESSION,
                    MessageFlowMetricsRecorder.LOOKUP_RESULT_ERROR
            );
            throw ex;
        }
    }

    @GetMapping("/api/device/message-flow/trace/{traceId}")
    public R<MessageFlowTimelineVO> getTrace(@PathVariable String traceId) {
        try {
            Optional<MessageFlowTimeline> timeline = messageFlowTimelineStore.getTimeline(traceId);
            messageFlowMetricsRecorder.recordLookup(
                    MessageFlowMetricsRecorder.LOOKUP_TARGET_TRACE,
                    timeline.isPresent()
                            ? MessageFlowMetricsRecorder.LOOKUP_RESULT_HIT
                            : MessageFlowMetricsRecorder.LOOKUP_RESULT_MISS
            );
            return R.ok(timeline.map(this::toTimelineVO).orElse(null));
        } catch (RuntimeException ex) {
            messageFlowMetricsRecorder.recordLookup(
                    MessageFlowMetricsRecorder.LOOKUP_TARGET_TRACE,
                    MessageFlowMetricsRecorder.LOOKUP_RESULT_ERROR
            );
            throw ex;
        }
    }

    @GetMapping("/api/device/message-flow/detail/{id}")
    public R<MessageTraceDetailVO> getTraceDetail(@PathVariable Long id, Authentication authentication) {
        MessageTraceDetailVO detail = deviceMessageService.getMessageTraceDetail(requireCurrentUserId(authentication), id);
        if (detail != null && hasText(detail.getTraceId())) {
            try {
                Optional<MessageFlowTimeline> timeline = messageFlowTimelineStore.getTimeline(detail.getTraceId());
                messageFlowMetricsRecorder.recordLookup(
                        MessageFlowMetricsRecorder.LOOKUP_TARGET_TRACE,
                        timeline.isPresent()
                                ? MessageFlowMetricsRecorder.LOOKUP_RESULT_HIT
                                : MessageFlowMetricsRecorder.LOOKUP_RESULT_MISS
                );
                detail.setTimeline(timeline.map(this::toTimelineVO).orElse(null));
                detail.setTimelineLookupError(Boolean.FALSE);
            } catch (RuntimeException ex) {
                messageFlowMetricsRecorder.recordLookup(
                        MessageFlowMetricsRecorder.LOOKUP_TARGET_TRACE,
                        MessageFlowMetricsRecorder.LOOKUP_RESULT_ERROR
                );
                detail.setTimeline(null);
                detail.setTimelineLookupError(Boolean.TRUE);
            }
        }
        return R.ok(detail);
    }

    @GetMapping("/api/device/message-flow/ops/overview")
    public R<MessageFlowOpsOverviewVO> getOpsOverview() {
        return R.ok(toOpsOverviewVO(messageFlowMetricsRecorder.snapshot()));
    }

    @GetMapping("/api/device/message-flow/recent")
    public R<List<MessageFlowRecentSessionVO>> getRecentSessions(MessageFlowRecentQuery query,
                                                                 @RequestParam(defaultValue = "10") Integer size) {
        List<MessageFlowRecentSessionVO> result = new ArrayList<>();
        for (String sessionId : messageFlowTimelineStore.getRecentSessionIds(resolveRecentRequestSize(size))) {
            Optional<MessageFlowSession> session = messageFlowTimelineStore.getSession(sessionId);
            if (session.isEmpty()) {
                messageFlowTimelineStore.removeRecentSession(sessionId);
                continue;
            }
            MessageFlowSession resolvedSession = session.get();
            recordCorrelationMissIfTimedOut(resolvedSession);
            if (!matchesRecentQuery(resolvedSession, query)) {
                continue;
            }
            result.add(toRecentSessionVO(resolvedSession));
        }
        return R.ok(result);
    }

    private MessageFlowSessionVO toSessionVO(MessageFlowSession session) {
        MessageFlowSessionVO sessionVO = new MessageFlowSessionVO();
        sessionVO.setSessionId(session.getSessionId());
        sessionVO.setTransportMode(session.getTransportMode());
        sessionVO.setStatus(session.getStatus());
        sessionVO.setSubmittedAt(session.getSubmittedAt());
        sessionVO.setTraceId(session.getTraceId());
        sessionVO.setDeviceCode(session.getDeviceCode());
        sessionVO.setTopic(session.getTopic());
        sessionVO.setCorrelationPending(session.getCorrelationPending());
        return sessionVO;
    }

    private MessageFlowRecentSessionVO toRecentSessionVO(MessageFlowSession session) {
        MessageFlowRecentSessionVO result = new MessageFlowRecentSessionVO();
        result.setSessionId(session.getSessionId());
        result.setTraceId(session.getTraceId());
        result.setTransportMode(session.getTransportMode());
        result.setStatus(session.getStatus());
        result.setSubmittedAt(session.getSubmittedAt());
        result.setDeviceCode(session.getDeviceCode());
        result.setTopic(session.getTopic());
        result.setCorrelationPending(session.getCorrelationPending());
        result.setTimelineAvailable(hasText(session.getTraceId())
                && messageFlowTimelineStore.getTimeline(session.getTraceId()).isPresent());
        return result;
    }

    private MessageFlowOpsOverviewVO toOpsOverviewVO(MessageFlowMetricsRecorder.OverviewSnapshot snapshot) {
        MessageFlowOpsOverviewVO result = new MessageFlowOpsOverviewVO();
        result.setRuntimeStartedAt(snapshot.runtimeStartedAt());
        for (MessageFlowMetricsRecorder.SessionCountSnapshot item : snapshot.sessionCounts()) {
            MessageFlowSessionCountVO countVO = new MessageFlowSessionCountVO();
            countVO.setTransportMode(item.transportMode());
            countVO.setStatus(item.status());
            countVO.setCount(item.count());
            result.getSessionCounts().add(countVO);
        }
        for (MessageFlowMetricsRecorder.CorrelationCountSnapshot item : snapshot.correlationCounts()) {
            MessageFlowCorrelationCountVO countVO = new MessageFlowCorrelationCountVO();
            countVO.setResult(item.result());
            countVO.setCount(item.count());
            result.getCorrelationCounts().add(countVO);
        }
        for (MessageFlowMetricsRecorder.LookupCountSnapshot item : snapshot.lookupCounts()) {
            MessageFlowLookupCountVO countVO = new MessageFlowLookupCountVO();
            countVO.setTarget(item.target());
            countVO.setResult(item.result());
            countVO.setCount(item.count());
            result.getLookupCounts().add(countVO);
        }
        for (MessageFlowMetricsRecorder.StageMetricSnapshot item : snapshot.stageMetrics()) {
            MessageFlowStageMetricVO metricVO = new MessageFlowStageMetricVO();
            metricVO.setStage(item.stage());
            metricVO.setCount(item.count());
            metricVO.setFailureCount(item.failureCount());
            metricVO.setSkippedCount(item.skippedCount());
            metricVO.setAvgCostMs(item.avgCostMs());
            metricVO.setP95CostMs(item.p95CostMs());
            metricVO.setMaxCostMs(item.maxCostMs());
            result.getStageMetrics().add(metricVO);
        }
        return result;
    }

    private MessageFlowTimelineVO toTimelineVO(MessageFlowTimeline timeline) {
        MessageFlowTimelineVO timelineVO = new MessageFlowTimelineVO();
        timelineVO.setTraceId(timeline.getTraceId());
        timelineVO.setSessionId(timeline.getSessionId());
        timelineVO.setFlowType(timeline.getFlowType());
        timelineVO.setStatus(timeline.getStatus());
        timelineVO.setDeviceCode(timeline.getDeviceCode());
        timelineVO.setProductKey(timeline.getProductKey());
        timelineVO.setTopic(timeline.getTopic());
        timelineVO.setProtocolCode(timeline.getProtocolCode());
        timelineVO.setMessageType(timeline.getMessageType());
        timelineVO.setStartedAt(timeline.getStartedAt());
        timelineVO.setFinishedAt(timeline.getFinishedAt());
        timelineVO.setTotalCostMs(timeline.getTotalCostMs());
        if (timeline.getSteps() != null) {
            for (MessageFlowStep step : timeline.getSteps()) {
                timelineVO.getSteps().add(toStepVO(step));
            }
        }
        return timelineVO;
    }

    private MessageFlowStepVO toStepVO(MessageFlowStep step) {
        MessageFlowStepVO stepVO = new MessageFlowStepVO();
        stepVO.setStage(step.getStage());
        stepVO.setHandlerClass(step.getHandlerClass());
        stepVO.setHandlerMethod(step.getHandlerMethod());
        stepVO.setStatus(step.getStatus());
        stepVO.setCostMs(step.getCostMs());
        stepVO.setStartedAt(step.getStartedAt());
        stepVO.setFinishedAt(step.getFinishedAt());
        stepVO.setSummary(step.getSummary());
        stepVO.setErrorClass(step.getErrorClass());
        stepVO.setErrorMessage(step.getErrorMessage());
        stepVO.setBranch(step.getBranch());
        return stepVO;
    }

    private boolean matchesRecentQuery(MessageFlowSession session, MessageFlowRecentQuery query) {
        if (query == null) {
            return true;
        }
        return containsIgnoreCase(session.getDeviceCode(), query.getDeviceCode())
                && containsIgnoreCase(session.getTopic(), query.getTopic())
                && equalsIgnoreCase(session.getTransportMode(), query.getTransportMode())
                && equalsIgnoreCase(session.getStatus(), query.getStatus());
    }

    private void recordCorrelationMissIfTimedOut(MessageFlowSession session) {
        if (session == null || !Boolean.TRUE.equals(session.getCorrelationPending())) {
            return;
        }
        if (hasText(session.getTraceId()) || session.getSubmittedAt() == null) {
            return;
        }
        LocalDateTime timeoutPoint = session.getSubmittedAt()
                .plusSeconds(resolveSessionMatchWindowSeconds());
        if (LocalDateTime.now().isBefore(timeoutPoint)) {
            return;
        }
        messageFlowMetricsRecorder.recordCorrelationMissOnce(
                session.getSessionId(),
                session.getTransportMode(),
                session.getDeviceCode(),
                session.getTopic(),
                session.getSubmittedAt()
        );
    }

    private int resolveRecentRequestSize(Integer requestedSize) {
        int defaultSize = 10;
        int safeSize = requestedSize == null || requestedSize < 1 ? defaultSize : requestedSize;
        Integer configuredLimit = messageFlowProperties.getRecentSessionLimit();
        int maxSize = configuredLimit == null || configuredLimit < 1 ? 500 : configuredLimit;
        return Math.min(safeSize, maxSize);
    }

    private int resolveSessionMatchWindowSeconds() {
        Integer seconds = messageFlowProperties.getSessionMatchWindowSeconds();
        return seconds == null || seconds < 1 ? 120 : seconds;
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        if (!hasText(expected)) {
            return true;
        }
        return hasText(actual) && actual.trim().toLowerCase().contains(expected.trim().toLowerCase());
    }

    private boolean equalsIgnoreCase(String actual, String expected) {
        if (!hasText(expected)) {
            return true;
        }
        return hasText(actual) && actual.trim().equalsIgnoreCase(expected.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long requireCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new BizException(401, "未认证，请先登录");
        }
        return principal.userId();
    }
}
