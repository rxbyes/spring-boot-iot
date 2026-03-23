package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowSessionVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowStepVO;
import com.ghlzm.iot.device.vo.messageflow.MessageFlowTimelineVO;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStep;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * message-flow 查询控制器。
 */
@RestController
public class DeviceMessageFlowController {

    private final MessageFlowTimelineStore messageFlowTimelineStore;

    public DeviceMessageFlowController(MessageFlowTimelineStore messageFlowTimelineStore) {
        this.messageFlowTimelineStore = messageFlowTimelineStore;
    }

    @GetMapping("/api/device/message-flow/session/{sessionId}")
    public R<MessageFlowSessionVO> getSession(@PathVariable String sessionId) {
        Optional<MessageFlowSession> session = messageFlowTimelineStore.getSession(sessionId);
        if (session.isEmpty()) {
            return R.ok(null);
        }
        MessageFlowSessionVO sessionVO = toSessionVO(session.get());
        if (session.get().getTraceId() != null && !session.get().getTraceId().isBlank()) {
            sessionVO.setTimeline(messageFlowTimelineStore.getTimeline(session.get().getTraceId())
                    .map(this::toTimelineVO)
                    .orElse(null));
        }
        return R.ok(sessionVO);
    }

    @GetMapping("/api/device/message-flow/trace/{traceId}")
    public R<MessageFlowTimelineVO> getTrace(@PathVariable String traceId) {
        return R.ok(messageFlowTimelineStore.getTimeline(traceId).map(this::toTimelineVO).orElse(null));
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
}
