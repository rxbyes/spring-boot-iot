package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowFingerprintSupport;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowLoggingConstants;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStageResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStages;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStep;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.message.mqtt.MqttTopicRouter;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 上行消息固定 pipeline。
 */
@Component
public class UpMessageProcessingPipeline {

    private static final Logger messageFlowLogger =
            LoggerFactory.getLogger(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME);

    private final MessageFlowProperties messageFlowProperties;
    private final MessageFlowTimelineStore messageFlowTimelineStore;
    private final MqttTopicRouter mqttTopicRouter;
    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final DeviceContractStageHandler deviceContractStageHandler;
    private final DeviceMessageLogStageHandler deviceMessageLogStageHandler;
    private final DevicePayloadApplyStageHandler devicePayloadApplyStageHandler;
    private final DeviceStateStageHandler deviceStateStageHandler;
    private final DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler;

    public UpMessageProcessingPipeline(MessageFlowProperties messageFlowProperties,
                                       MessageFlowTimelineStore messageFlowTimelineStore,
                                       MqttTopicRouter mqttTopicRouter,
                                       ProtocolAdapterRegistry protocolAdapterRegistry,
                                       DeviceContractStageHandler deviceContractStageHandler,
                                       DeviceMessageLogStageHandler deviceMessageLogStageHandler,
                                       DevicePayloadApplyStageHandler devicePayloadApplyStageHandler,
                                       DeviceStateStageHandler deviceStateStageHandler,
                                       DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler) {
        this.messageFlowProperties = messageFlowProperties;
        this.messageFlowTimelineStore = messageFlowTimelineStore;
        this.mqttTopicRouter = mqttTopicRouter;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.deviceContractStageHandler = deviceContractStageHandler;
        this.deviceMessageLogStageHandler = deviceMessageLogStageHandler;
        this.devicePayloadApplyStageHandler = devicePayloadApplyStageHandler;
        this.deviceStateStageHandler = deviceStateStageHandler;
        this.deviceRiskDispatchStageHandler = deviceRiskDispatchStageHandler;
    }

    @Transactional(rollbackFor = Exception.class)
    public MessageFlowExecutionResult process(UpMessageProcessingRequest request) {
        String previousTraceId = TraceContextHolder.getTraceId();
        ProcessingContext context = new ProcessingContext(request);
        try {
            executeStage(context, MessageFlowStages.INGRESS, getClass().getSimpleName(), "ingress", () -> ingress(context));
            executeStage(context, MessageFlowStages.TOPIC_ROUTE, MqttTopicRouter.class.getSimpleName(), "route", () -> topicRoute(context));
            executeStage(context, MessageFlowStages.PROTOCOL_DECODE, resolveProtocolHandlerClass(context), "decode", () -> protocolDecode(context));
            executeStage(context, MessageFlowStages.DEVICE_CONTRACT, DeviceContractStageHandler.class.getSimpleName(), "resolve", () -> deviceContract(context));
            executeStage(context, MessageFlowStages.MESSAGE_LOG, DeviceMessageLogStageHandler.class.getSimpleName(), "save", () -> messageLog(context));
            executeStage(context, MessageFlowStages.PAYLOAD_APPLY, DevicePayloadApplyStageHandler.class.getSimpleName(), "apply", () -> payloadApply(context));
            executeStage(context, MessageFlowStages.DEVICE_STATE, DeviceStateStageHandler.class.getSimpleName(), "refresh", () -> deviceState(context));
            executeStage(context, MessageFlowStages.RISK_DISPATCH, DeviceRiskDispatchStageHandler.class.getSimpleName(), "dispatch", () -> riskDispatch(context));
            executeStage(context, MessageFlowStages.COMPLETE, getClass().getSimpleName(), "complete", () -> complete(context));
            finalizeTimeline(context, null);
            return buildExecutionResult(context);
        } catch (RuntimeException ex) {
            finalizeTimeline(context, ex);
            throw ex;
        } finally {
            restoreTrace(previousTraceId);
        }
    }

    private MessageFlowStageResult ingress(ProcessingContext context) {
        context.traceId = TraceContextHolder.currentOrCreate();
        context.sessionId = hasText(context.request.getSessionId())
                ? context.request.getSessionId().trim()
                : TraceContextHolder.generateTraceId();
        context.timeline.setTraceId(context.traceId);
        context.timeline.setSessionId(context.sessionId);
        context.timeline.setFlowType(context.request.getTransportMode());
        context.timeline.setStatus(MessageFlowStatuses.SESSION_PROCESSING);
        context.timeline.setStartedAt(context.startedAt);
        context.timeline.setTopic(context.request.getTopic());
        context.timeline.setDeviceCode(context.request.getDeviceCode());
        context.timeline.setProductKey(context.request.getProductKey());
        context.timeline.setProtocolCode(context.request.getProtocolCode());

        if (isHttp(context)) {
            context.session = buildOrLoadSession(context.sessionId);
            context.session.setTransportMode(context.request.getTransportMode());
            context.session.setStatus(MessageFlowStatuses.SESSION_PROCESSING);
            context.session.setSubmittedAt(context.startedAt);
            context.session.setTraceId(context.traceId);
            context.session.setDeviceCode(context.request.getDeviceCode());
            context.session.setTopic(context.request.getTopic());
            context.session.setCorrelationPending(Boolean.FALSE);
            saveSessionIfEnabled(context.session);
        }

        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("transportMode", context.request.getTransportMode());
        result.getSummary().put("sessionId", context.sessionId);
        result.getSummary().put("traceId", context.traceId);
        return result;
    }

    private MessageFlowStageResult topicRoute(ProcessingContext context) {
        MessageFlowStageResult result = new MessageFlowStageResult();
        if (isHttp(context)) {
            RawDeviceMessage rawMessage = new RawDeviceMessage();
            rawMessage.setProtocolCode(context.request.getProtocolCode());
            rawMessage.setProductKey(context.request.getProductKey());
            rawMessage.setTraceId(context.traceId);
            rawMessage.setDeviceCode(context.request.getDeviceCode());
            rawMessage.setTopic(context.request.getTopic());
            rawMessage.setClientId(context.request.getClientId());
            rawMessage.setTenantId(context.request.getTenantId());
            rawMessage.setPayload(context.request.getPayload());
            context.rawDeviceMessage = rawMessage;

            result.setStatus(MessageFlowStatuses.STEP_SKIPPED);
            result.setBranch("DIRECT_HTTP");
            result.getSummary().put("transportMode", context.request.getTransportMode());
            result.getSummary().put("topic", context.request.getTopic());
            return result;
        }

        RawDeviceMessage rawMessage = mqttTopicRouter.toRawMessage(
                context.request.getTopic(),
                new MqttMessage(context.request.getPayload())
        );
        rawMessage.setTraceId(context.traceId);
        context.rawDeviceMessage = rawMessage;
        context.timeline.setTopic(rawMessage.getTopic());
        context.timeline.setProtocolCode(rawMessage.getProtocolCode());

        result.getSummary().put("routeType", rawMessage.getTopicRouteType());
        result.getSummary().put("topic", rawMessage.getTopic());
        result.getSummary().put("deviceCode", rawMessage.getDeviceCode());
        result.getSummary().put("productKey", rawMessage.getProductKey());
        result.getSummary().put("messageType", rawMessage.getMessageType());
        return result;
    }

    private MessageFlowStageResult protocolDecode(ProcessingContext context) {
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(context.rawDeviceMessage.getProtocolCode());
        if (adapter == null) {
            throw new BizException("未找到协议适配器: " + context.rawDeviceMessage.getProtocolCode());
        }

        ProtocolContext protocolContext = buildProtocolContext(context.rawDeviceMessage);
        DeviceUpMessage upMessage = adapter.decode(context.rawDeviceMessage.getPayload(), protocolContext);
        if (upMessage == null) {
            throw new BizException("协议解析结果为空");
        }
        enrichUpMessage(context.rawDeviceMessage, upMessage, context.traceId);
        enrichRawMessage(context.rawDeviceMessage, upMessage);

        context.upMessage = upMessage;
        context.timeline.setDeviceCode(upMessage.getDeviceCode());
        context.timeline.setProductKey(upMessage.getProductKey());
        context.timeline.setProtocolCode(upMessage.getProtocolCode());
        context.timeline.setMessageType(upMessage.getMessageType());
        context.timeline.setTopic(upMessage.getTopic());

        if (isMqtt(context)) {
            String fingerprint = MessageFlowFingerprintSupport.buildFingerprint(
                    context.request.getTopic(),
                    upMessage.getDeviceCode(),
                    context.request.getPayload()
            );
            context.fingerprint = fingerprint;
            Optional<String> matchedSessionId = getMatchedSessionId(fingerprint);
            if (matchedSessionId.isPresent()) {
                context.sessionId = matchedSessionId.get();
                context.timeline.setSessionId(context.sessionId);
                context.session = buildOrLoadSession(context.sessionId);
                context.session.setSessionId(context.sessionId);
                context.session.setTransportMode(context.request.getTransportMode());
                if (context.session.getSubmittedAt() == null) {
                    context.session.setSubmittedAt(context.startedAt);
                }
                context.session.setStatus(MessageFlowStatuses.SESSION_PROCESSING);
                context.session.setTraceId(context.traceId);
                context.session.setDeviceCode(upMessage.getDeviceCode());
                context.session.setTopic(context.rawDeviceMessage.getTopic());
                context.session.setCorrelationPending(Boolean.FALSE);
                saveSessionIfEnabled(context.session);
                context.correlationMatched = true;
            }
        } else if (context.session != null) {
            context.session.setDeviceCode(upMessage.getDeviceCode());
            context.session.setTopic(upMessage.getTopic());
            saveSessionIfEnabled(context.session);
        }

        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("routeType", context.rawDeviceMessage.getTopicRouteType());
        result.getSummary().put("messageType", upMessage.getMessageType());
        result.getSummary().put("dataFormatType", upMessage.getDataFormatType());
        result.getSummary().put("childMessageCount", childMessageCount(upMessage));
        result.getSummary().put("filePayload", upMessage.getFilePayload() != null);
        result.getSummary().put("correlationMatched", context.correlationMatched);
        return result;
    }

    private MessageFlowStageResult deviceContract(ProcessingContext context) {
        context.targets.clear();
        List<DeviceUpMessage> messages = new ArrayList<>();
        messages.add(context.upMessage);
        if (context.upMessage.getChildMessages() != null) {
            for (DeviceUpMessage childMessage : context.upMessage.getChildMessages()) {
                if (childMessage == null || !hasText(childMessage.getDeviceCode())) {
                    continue;
                }
                normalizeChildMessage(context.upMessage, childMessage);
                messages.add(childMessage);
            }
        }

        int childCount = 0;
        for (int index = 0; index < messages.size(); index++) {
            DeviceProcessingTarget target = deviceContractStageHandler.resolve(messages.get(index));
            if (index > 0) {
                target.setChildTarget(Boolean.TRUE);
                childCount++;
            }
            context.targets.add(target);
        }

        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("targetCount", context.targets.size());
        result.getSummary().put("childTargetCount", childCount);
        result.getSummary().put("deviceCode", context.upMessage.getDeviceCode());
        result.getSummary().put("productKey", context.upMessage.getProductKey());
        return result;
    }

    private MessageFlowStageResult messageLog(ProcessingContext context) {
        for (DeviceProcessingTarget target : context.targets) {
            deviceMessageLogStageHandler.save(target);
        }
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("loggedTargetCount", context.targets.size());
        return result;
    }

    private MessageFlowStageResult payloadApply(ProcessingContext context) {
        MessageFlowStageResult result = new MessageFlowStageResult();
        if (context.targets.isEmpty()) {
            return result;
        }

        DevicePayloadApplyResult parentResult = devicePayloadApplyStageHandler.apply(context.targets.get(0));
        result.setBranch(parentResult.getBranch());
        result.getSummary().putAll(parentResult.getSummary());
        result.getSummary().put("targetCount", context.targets.size());

        for (int index = 1; index < context.targets.size(); index++) {
            DeviceProcessingTarget childTarget = context.targets.get(index);
            DevicePayloadApplyResult childResult = devicePayloadApplyStageHandler.apply(childTarget);
            MessageFlowStep childStep = new MessageFlowStep();
            childStep.setStage(MessageFlowStages.PAYLOAD_APPLY);
            childStep.setHandlerClass(DevicePayloadApplyStageHandler.class.getSimpleName());
            childStep.setHandlerMethod("applyChild");
            childStep.setStatus(MessageFlowStatuses.STEP_SUCCESS);
            childStep.setStartedAt(LocalDateTime.now());
            childStep.setFinishedAt(childStep.getStartedAt());
            childStep.setCostMs(0L);
            childStep.setBranch("CHILD_" + normalizeBranch(childResult.getBranch()));
            childStep.getSummary().put("childDeviceCode", childTarget.getDevice().getDeviceCode());
            childStep.getSummary().put("metricCount", childTarget.getMessage().getProperties() == null
                    ? 0
                    : childTarget.getMessage().getProperties().size());
            childStep.getSummary().putAll(childResult.getSummary());
            result.getAdditionalSteps().add(childStep);
        }
        return result;
    }

    private MessageFlowStageResult deviceState(ProcessingContext context) {
        for (DeviceProcessingTarget target : context.targets) {
            deviceStateStageHandler.refresh(target);
        }
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("refreshedTargetCount", context.targets.size());
        return result;
    }

    private MessageFlowStageResult riskDispatch(ProcessingContext context) {
        int publishedCount = 0;
        for (DeviceProcessingTarget target : context.targets) {
            if (deviceRiskDispatchStageHandler.dispatch(target)) {
                publishedCount++;
            }
        }
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("publishedTargetCount", publishedCount);
        return result;
    }

    private MessageFlowStageResult complete(ProcessingContext context) {
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("sessionId", context.sessionId);
        result.getSummary().put("traceId", context.traceId);
        result.getSummary().put("status", MessageFlowStatuses.SESSION_COMPLETED);
        result.getSummary().put("targetCount", context.targets.size());
        return result;
    }

    private void finalizeTimeline(ProcessingContext context, RuntimeException throwable) {
        context.timeline.setTraceId(context.traceId);
        context.timeline.setSessionId(context.sessionId);
        context.timeline.setFlowType(context.request.getTransportMode());
        context.timeline.setFinishedAt(LocalDateTime.now());
        context.timeline.setTotalCostMs(elapsedMs(context.startedNs));
        context.timeline.setStatus(throwable == null ? MessageFlowStatuses.SESSION_COMPLETED : MessageFlowStatuses.SESSION_FAILED);
        if (context.upMessage != null) {
            context.timeline.setDeviceCode(context.upMessage.getDeviceCode());
            context.timeline.setProductKey(context.upMessage.getProductKey());
            context.timeline.setProtocolCode(context.upMessage.getProtocolCode());
            context.timeline.setMessageType(context.upMessage.getMessageType());
            context.timeline.setTopic(context.upMessage.getTopic());
        }

        MessageFlowSession session = context.session == null ? buildOrLoadSession(context.sessionId) : context.session;
        session.setSessionId(context.sessionId);
        session.setTransportMode(context.request.getTransportMode());
        if (session.getSubmittedAt() == null) {
            session.setSubmittedAt(context.startedAt);
        }
        session.setStatus(throwable == null ? MessageFlowStatuses.SESSION_COMPLETED : MessageFlowStatuses.SESSION_FAILED);
        session.setTraceId(context.traceId);
        session.setDeviceCode(context.timeline.getDeviceCode());
        session.setTopic(context.timeline.getTopic());
        session.setCorrelationPending(Boolean.FALSE);
        context.session = session;

        saveSessionIfEnabled(session);
        saveTimelineIfEnabled(context.timeline);
        logSummary(context, throwable);
    }

    private MessageFlowExecutionResult buildExecutionResult(ProcessingContext context) {
        MessageFlowExecutionResult result = new MessageFlowExecutionResult();
        MessageFlowSubmitResult submitResult = new MessageFlowSubmitResult();
        submitResult.setSessionId(context.sessionId);
        submitResult.setTraceId(context.traceId);
        submitResult.setStatus(context.timeline.getStatus());
        submitResult.setTimelineAvailable(Boolean.TRUE.equals(messageFlowProperties.getEnabled()));
        submitResult.setCorrelationPending(Boolean.FALSE);
        result.setSubmitResult(submitResult);
        result.setTimeline(context.timeline);
        result.setRawDeviceMessage(context.rawDeviceMessage);
        result.setUpMessage(context.upMessage);
        return result;
    }

    private void executeStage(ProcessingContext context,
                              String stage,
                              String handlerClass,
                              String handlerMethod,
                              StageAction action) {
        MessageFlowStep step = new MessageFlowStep();
        step.setStage(stage);
        step.setHandlerClass(handlerClass);
        step.setHandlerMethod(handlerMethod);
        step.setStartedAt(LocalDateTime.now());
        try {
            MessageFlowStageResult stageResult = action.run();
            applyStageResult(step, stageResult);
            step.setFinishedAt(LocalDateTime.now());
            step.setCostMs(elapsedMs(step.getStartedAt(), step.getFinishedAt()));
            context.timeline.getSteps().add(step);
            if (stageResult != null && stageResult.getAdditionalSteps() != null) {
                context.timeline.getSteps().addAll(stageResult.getAdditionalSteps());
            }
        } catch (RuntimeException ex) {
            step.setStatus(MessageFlowStatuses.STEP_FAILED);
            step.setErrorClass(ex.getClass().getSimpleName());
            step.setErrorMessage(ex.getMessage());
            step.setFinishedAt(LocalDateTime.now());
            step.setCostMs(elapsedMs(step.getStartedAt(), step.getFinishedAt()));
            context.timeline.getSteps().add(step);
            throw ex;
        }
    }

    private void applyStageResult(MessageFlowStep step, MessageFlowStageResult stageResult) {
        if (stageResult == null) {
            step.setStatus(MessageFlowStatuses.STEP_SUCCESS);
            return;
        }
        step.setStatus(hasText(stageResult.getStatus()) ? stageResult.getStatus() : MessageFlowStatuses.STEP_SUCCESS);
        step.setBranch(stageResult.getBranch());
        step.setErrorClass(stageResult.getErrorClass());
        step.setErrorMessage(stageResult.getErrorMessage());
        if (stageResult.getSummary() != null) {
            step.getSummary().putAll(stageResult.getSummary());
        }
    }

    private ProtocolContext buildProtocolContext(RawDeviceMessage rawMessage) {
        ProtocolContext context = new ProtocolContext();
        context.setTenantCode(rawMessage.getTenantId());
        context.setProductKey(rawMessage.getProductKey());
        context.setDeviceCode(rawMessage.getDeviceCode());
        context.setGatewayDeviceCode(rawMessage.getGatewayDeviceCode());
        context.setSubDeviceCode(rawMessage.getSubDeviceCode());
        context.setTopicRouteType(rawMessage.getTopicRouteType());
        context.setMessageType(rawMessage.getMessageType());
        context.setTopic(rawMessage.getTopic());
        context.setClientId(rawMessage.getClientId());
        context.setMetadata(buildMetadata(rawMessage));
        return context;
    }

    private Map<String, Object> buildMetadata(RawDeviceMessage rawMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("traceId", rawMessage.getTraceId());
        metadata.put("messageType", rawMessage.getMessageType());
        metadata.put("topic", rawMessage.getTopic());
        metadata.put("clientId", rawMessage.getClientId());
        metadata.put("productKey", rawMessage.getProductKey());
        metadata.put("deviceCode", rawMessage.getDeviceCode());
        metadata.put("topicRouteType", rawMessage.getTopicRouteType());
        metadata.put("gatewayDeviceCode", rawMessage.getGatewayDeviceCode());
        metadata.put("subDeviceCode", rawMessage.getSubDeviceCode());
        return metadata;
    }

    private void enrichUpMessage(RawDeviceMessage rawMessage, DeviceUpMessage upMessage, String traceId) {
        if (!hasText(upMessage.getTenantId())) {
            upMessage.setTenantId(rawMessage.getTenantId());
        }
        if (!hasText(upMessage.getProductKey())) {
            upMessage.setProductKey(rawMessage.getProductKey());
        }
        if (!hasText(upMessage.getDeviceCode())) {
            upMessage.setDeviceCode(rawMessage.getDeviceCode());
        }
        if (!hasText(upMessage.getMessageType())) {
            upMessage.setMessageType(rawMessage.getMessageType());
        }
        if (!hasText(upMessage.getRawPayload())) {
            upMessage.setRawPayload(new String(rawMessage.getPayload(), StandardCharsets.UTF_8));
        }
        if (upMessage.getTimestamp() == null) {
            upMessage.setTimestamp(LocalDateTime.now());
        }
        if (!hasText(upMessage.getTraceId())) {
            upMessage.setTraceId(traceId);
        }
        upMessage.setProtocolCode(rawMessage.getProtocolCode());
        upMessage.setTopic(rawMessage.getTopic());
    }

    private void enrichRawMessage(RawDeviceMessage rawMessage, DeviceUpMessage upMessage) {
        if (hasText(upMessage.getDeviceCode())) {
            rawMessage.setDeviceCode(upMessage.getDeviceCode());
        }
        if (hasText(upMessage.getProductKey())) {
            rawMessage.setProductKey(upMessage.getProductKey());
        }
        if (hasText(upMessage.getMessageType())) {
            rawMessage.setMessageType(upMessage.getMessageType());
        }
        if (!hasText(rawMessage.getClientId()) && hasText(upMessage.getDeviceCode())) {
            rawMessage.setClientId(upMessage.getDeviceCode());
        }
        if (hasText(upMessage.getProtocolCode())) {
            rawMessage.setProtocolCode(upMessage.getProtocolCode());
        }
    }

    private void normalizeChildMessage(DeviceUpMessage parentMessage, DeviceUpMessage childMessage) {
        if (!hasText(childMessage.getTenantId())) {
            childMessage.setTenantId(parentMessage.getTenantId());
        }
        if (!hasText(childMessage.getProductKey())) {
            childMessage.setProductKey(parentMessage.getProductKey());
        }
        if (!hasText(childMessage.getProtocolCode())) {
            childMessage.setProtocolCode(parentMessage.getProtocolCode());
        }
        if (!hasText(childMessage.getTraceId())) {
            childMessage.setTraceId(parentMessage.getTraceId());
        }
        if (!hasText(childMessage.getMessageType())) {
            childMessage.setMessageType(parentMessage.getMessageType());
        }
        if (!hasText(childMessage.getTopic())) {
            childMessage.setTopic(parentMessage.getTopic());
        }
        if (childMessage.getTimestamp() == null) {
            childMessage.setTimestamp(parentMessage.getTimestamp());
        }
        if (!hasText(childMessage.getRawPayload())) {
            childMessage.setRawPayload(parentMessage.getRawPayload());
        }
    }

    private Optional<String> getMatchedSessionId(String fingerprint) {
        if (!Boolean.TRUE.equals(messageFlowProperties.getEnabled())) {
            return Optional.empty();
        }
        return messageFlowTimelineStore.getSessionIdByFingerprint(fingerprint);
    }

    private MessageFlowSession buildOrLoadSession(String sessionId) {
        if (Boolean.TRUE.equals(messageFlowProperties.getEnabled()) && hasText(sessionId)) {
            Optional<MessageFlowSession> session = messageFlowTimelineStore.getSession(sessionId);
            if (session.isPresent()) {
                return session.get();
            }
        }
        MessageFlowSession session = new MessageFlowSession();
        session.setSessionId(sessionId);
        return session;
    }

    private void saveSessionIfEnabled(MessageFlowSession session) {
        if (Boolean.TRUE.equals(messageFlowProperties.getEnabled())) {
            messageFlowTimelineStore.saveSession(session);
        }
    }

    private void saveTimelineIfEnabled(MessageFlowTimeline timeline) {
        if (Boolean.TRUE.equals(messageFlowProperties.getEnabled())) {
            messageFlowTimelineStore.saveTimeline(timeline);
        }
    }

    private void logSummary(ProcessingContext context, RuntimeException throwable) {
        if (!messageFlowLogger.isInfoEnabled()) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", context.traceId);
        details.put("sessionId", context.sessionId);
        details.put("transportMode", context.request.getTransportMode());
        details.put("deviceCode", context.timeline.getDeviceCode());
        details.put("productKey", context.timeline.getProductKey());
        details.put("topic", context.timeline.getTopic());
        details.put("messageType", context.timeline.getMessageType());
        details.put("stepCount", context.timeline.getSteps().size());
        if (throwable != null) {
            details.put("errorClass", throwable.getClass().getSimpleName());
        }
        messageFlowLogger.info(ObservabilityEventLogSupport.summary(
                "message_flow_summary",
                throwable == null ? "success" : "failure",
                context.timeline.getTotalCostMs(),
                details
        ));
    }

    private boolean isHttp(ProcessingContext context) {
        return "HTTP".equalsIgnoreCase(context.request.getTransportMode());
    }

    private boolean isMqtt(ProcessingContext context) {
        return "MQTT".equalsIgnoreCase(context.request.getTransportMode());
    }

    private String resolveProtocolHandlerClass(ProcessingContext context) {
        if (context.rawDeviceMessage == null || !hasText(context.rawDeviceMessage.getProtocolCode())) {
            return ProtocolAdapter.class.getSimpleName();
        }
        ProtocolAdapter adapter = protocolAdapterRegistry.getAdapter(context.rawDeviceMessage.getProtocolCode());
        return adapter == null ? ProtocolAdapter.class.getSimpleName() : adapter.getClass().getSimpleName();
    }

    private void restoreTrace(String previousTraceId) {
        if (hasText(previousTraceId)) {
            TraceContextHolder.bindTraceId(previousTraceId);
            return;
        }
        TraceContextHolder.clear();
    }

    private long elapsedMs(long startedNs) {
        return (System.nanoTime() - startedNs) / 1_000_000L;
    }

    private long elapsedMs(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return 0L;
        }
        return java.time.Duration.between(startedAt, finishedAt).toMillis();
    }

    private int childMessageCount(DeviceUpMessage upMessage) {
        return upMessage == null || upMessage.getChildMessages() == null ? 0 : upMessage.getChildMessages().size();
    }

    private String normalizeBranch(String branch) {
        return hasText(branch) ? branch.trim().toUpperCase() : "PROPERTY";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @FunctionalInterface
    private interface StageAction {

        MessageFlowStageResult run();
    }

    private static final class ProcessingContext {

        private final UpMessageProcessingRequest request;
        private final LocalDateTime startedAt = LocalDateTime.now();
        private final long startedNs = System.nanoTime();
        private final MessageFlowTimeline timeline = new MessageFlowTimeline();
        private final List<DeviceProcessingTarget> targets = new ArrayList<>();
        private String traceId;
        private String sessionId;
        private String fingerprint;
        private boolean correlationMatched;
        private MessageFlowSession session;
        private RawDeviceMessage rawDeviceMessage;
        private DeviceUpMessage upMessage;

        private ProcessingContext(UpMessageProcessingRequest request) {
            this.request = request;
        }
    }
}
