package com.ghlzm.iot.message.pipeline;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.service.model.DevicePayloadApplyResult;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.device.service.model.DeviceStateRefreshResult;
import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowFingerprintSupport;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowLoggingConstants;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowMetricsRecorder;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowProperties;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSession;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStageResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStages;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStatuses;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowStep;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowSubmitResult;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimeline;
import com.ghlzm.iot.framework.observability.messageflow.MessageFlowTimelineStore;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import com.ghlzm.iot.message.mqtt.MqttTopicRouter;
import com.ghlzm.iot.message.service.capability.CapabilityFeedback;
import com.ghlzm.iot.message.service.capability.CapabilityFeedbackHandler;
import com.ghlzm.iot.message.service.capability.CapabilityFeedbackTopicMatcher;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.model.ProtocolTemplateEvidence;
import com.ghlzm.iot.protocol.core.model.ProtocolTemplateExecutionEvidence;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import com.ghlzm.iot.telemetry.service.handler.TelemetryPersistStageHandler;
import com.ghlzm.iot.telemetry.service.model.TelemetryPersistResult;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private static final String CAPABILITY_FEEDBACK_STAGE = "CAPABILITY_FEEDBACK";

    private static final Logger messageFlowLogger =
            LoggerFactory.getLogger(MessageFlowLoggingConstants.MESSAGE_FLOW_LOGGER_NAME);

    private final MessageFlowProperties messageFlowProperties;
    private final MessageFlowMetricsRecorder messageFlowMetricsRecorder;
    private final MessageFlowTimelineStore messageFlowTimelineStore;
    private final MessagePipelineTransactionExecutor transactionExecutor;
    private final MqttTopicRouter mqttTopicRouter;
    private final ProtocolAdapterRegistry protocolAdapterRegistry;
    private final DeviceContractStageHandler deviceContractStageHandler;
    private final DeviceMessageLogStageHandler deviceMessageLogStageHandler;
    private final DevicePayloadApplyStageHandler devicePayloadApplyStageHandler;
    private final TelemetryPersistStageHandler telemetryPersistStageHandler;
    private final DeviceStateStageHandler deviceStateStageHandler;
    private final DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler;
    private final CapabilityFeedbackTopicMatcher capabilityFeedbackTopicMatcher;
    private final CapabilityFeedbackHandler capabilityFeedbackHandler;
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    public UpMessageProcessingPipeline(MessageFlowProperties messageFlowProperties,
                                       MessageFlowMetricsRecorder messageFlowMetricsRecorder,
                                       MessageFlowTimelineStore messageFlowTimelineStore,
                                       MessagePipelineTransactionExecutor transactionExecutor,
                                       MqttTopicRouter mqttTopicRouter,
                                       ProtocolAdapterRegistry protocolAdapterRegistry,
                                       DeviceContractStageHandler deviceContractStageHandler,
                                       DeviceMessageLogStageHandler deviceMessageLogStageHandler,
                                       DevicePayloadApplyStageHandler devicePayloadApplyStageHandler,
                                       TelemetryPersistStageHandler telemetryPersistStageHandler,
                                       DeviceStateStageHandler deviceStateStageHandler,
                                       DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler,
                                       CapabilityFeedbackTopicMatcher capabilityFeedbackTopicMatcher,
                                       CapabilityFeedbackHandler capabilityFeedbackHandler) {
        this.messageFlowProperties = messageFlowProperties;
        this.messageFlowMetricsRecorder = messageFlowMetricsRecorder;
        this.messageFlowTimelineStore = messageFlowTimelineStore;
        this.transactionExecutor = transactionExecutor;
        this.mqttTopicRouter = mqttTopicRouter;
        this.protocolAdapterRegistry = protocolAdapterRegistry;
        this.deviceContractStageHandler = deviceContractStageHandler;
        this.deviceMessageLogStageHandler = deviceMessageLogStageHandler;
        this.devicePayloadApplyStageHandler = devicePayloadApplyStageHandler;
        this.telemetryPersistStageHandler = telemetryPersistStageHandler;
        this.deviceStateStageHandler = deviceStateStageHandler;
        this.deviceRiskDispatchStageHandler = deviceRiskDispatchStageHandler;
        this.capabilityFeedbackTopicMatcher = capabilityFeedbackTopicMatcher;
        this.capabilityFeedbackHandler = capabilityFeedbackHandler;
    }

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    public MessageFlowExecutionResult process(UpMessageProcessingRequest request) {
        String previousTraceId = TraceContextHolder.getTraceId();
        ProcessingContext context = new ProcessingContext(request);
        try {
            executeStage(context, MessageFlowStages.INGRESS, getClass().getSimpleName(), "ingress", () -> ingress(context));
            if (isCapabilityFeedback(context)) {
                executeStage(context, CAPABILITY_FEEDBACK_STAGE, CapabilityFeedbackHandler.class.getSimpleName(), "handle", () -> capabilityFeedback(context));
                executeStage(context, MessageFlowStages.COMPLETE, getClass().getSimpleName(), "complete", () -> complete(context));
                finalizeTimeline(context, null);
                return buildExecutionResult(context);
            }
            executeStage(context, MessageFlowStages.TOPIC_ROUTE, MqttTopicRouter.class.getSimpleName(), "route", () -> topicRoute(context));
            executeStage(context, MessageFlowStages.PROTOCOL_DECODE, resolveProtocolHandlerClass(context), "decode", () -> protocolDecode(context));
            executeStage(context, MessageFlowStages.DEVICE_CONTRACT, DeviceContractStageHandler.class.getSimpleName(), "resolve", () -> deviceContract(context));
            executeWithinTransaction(() -> {
                executeStage(context, MessageFlowStages.MESSAGE_LOG, DeviceMessageLogStageHandler.class.getSimpleName(), "save", () -> messageLog(context));
                executeStage(context, MessageFlowStages.PAYLOAD_APPLY, DevicePayloadApplyStageHandler.class.getSimpleName(), "apply", () -> payloadApply(context));
            });
            executeStage(context, MessageFlowStages.TELEMETRY_PERSIST, TelemetryPersistStageHandler.class.getSimpleName(), "persist", () -> telemetryPersist(context));
            executeWithinTransaction(() -> {
                executeStage(context, MessageFlowStages.DEVICE_STATE, DeviceStateStageHandler.class.getSimpleName(), "refresh", () -> deviceState(context));
                executeStage(context, MessageFlowStages.RISK_DISPATCH, DeviceRiskDispatchStageHandler.class.getSimpleName(), "dispatch", () -> riskDispatch(context));
            });
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
        messageFlowMetricsRecorder.recordSession(context.request.getTransportMode(), MessageFlowStatuses.SESSION_PROCESSING);

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
                rebindTimelineSessionId(context, matchedSessionId.get());
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
                messageFlowMetricsRecorder.recordCorrelation(MessageFlowMetricsRecorder.CORRELATION_RESULT_MATCHED);
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
        DeviceUpProtocolMetadata protocolMetadata = upMessage.getProtocolMetadata();
        if (protocolMetadata != null) {
            if (hasText(protocolMetadata.getRouteType())) {
                result.getSummary().put("routeType", protocolMetadata.getRouteType());
            }
            if (hasText(protocolMetadata.getAppId())) {
                result.getSummary().put("appId", protocolMetadata.getAppId());
            }
            if (protocolMetadata.getFamilyCodes() != null && !protocolMetadata.getFamilyCodes().isEmpty()) {
                result.getSummary().put("familyCodes", protocolMetadata.getFamilyCodes());
            }
            if (hasText(protocolMetadata.getNormalizationStrategy())) {
                result.getSummary().put("normalizationStrategy", protocolMetadata.getNormalizationStrategy());
            }
            if (hasText(protocolMetadata.getTimestampSource())) {
                result.getSummary().put("timestampSource", protocolMetadata.getTimestampSource());
            }
            if (protocolMetadata.getChildSplitApplied() != null) {
                result.getSummary().put("childSplitApplied", protocolMetadata.getChildSplitApplied());
            }
            ProtocolTemplateEvidence templateEvidence = protocolMetadata.getTemplateEvidence();
            if (templateEvidence != null) {
                if (templateEvidence.getTemplateCodes() != null && !templateEvidence.getTemplateCodes().isEmpty()) {
                    result.getSummary().put("templateCodes", templateEvidence.getTemplateCodes());
                }
                if (templateEvidence.getExecutions() != null && !templateEvidence.getExecutions().isEmpty()) {
                    result.getSummary().put("templateExecutionCount", templateEvidence.getExecutions().size());
                    List<String> logicalCodes = templateEvidence.getExecutions().stream()
                            .map(ProtocolTemplateExecutionEvidence::getLogicalChannelCode)
                            .filter(this::hasText)
                            .toList();
                    if (!logicalCodes.isEmpty()) {
                        result.getSummary().put("templateLogicalCodes", logicalCodes);
                    }
                }
            }
        }
        ProtocolDecodeSummarySupport.append(result, upMessage);
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
        int primaryLatestPropertyCount = latestPropertyCount(parentResult);
        int childLatestPropertyCount = 0;
        int childLatestWriteTargetCount = 0;

        for (int index = 1; index < context.targets.size(); index++) {
            DeviceProcessingTarget childTarget = context.targets.get(index);
            DevicePayloadApplyResult childResult = devicePayloadApplyStageHandler.apply(childTarget);
            int latestPropertyCount = latestPropertyCount(childResult);
            childLatestPropertyCount += latestPropertyCount;
            if (latestPropertyCount > 0) {
                childLatestWriteTargetCount++;
            }
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
        result.getSummary().put("primaryLatestPropertyCount", primaryLatestPropertyCount);
        result.getSummary().put("childLatestPropertyCount", childLatestPropertyCount);
        result.getSummary().put("childLatestWriteTargetCount", childLatestWriteTargetCount);
        return result;
    }

    private MessageFlowStageResult deviceState(ProcessingContext context) {
        int primaryLinkStateRefreshCount = 0;
        int childLinkStateRefreshCount = 0;
        int sensorStateTouchedCount = 0;
        for (DeviceProcessingTarget target : context.targets) {
            DeviceStateRefreshResult refreshResult = deviceStateStageHandler.refresh(target);
            if (Boolean.TRUE.equals(target.getChildTarget())) {
                childLinkStateRefreshCount++;
            } else {
                primaryLinkStateRefreshCount++;
            }
            if (refreshResult != null
                    && refreshResult.getSummary() != null
                    && isTrue(refreshResult.getSummary().get("sensorStateTouched"))) {
                sensorStateTouchedCount++;
            }
        }
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("refreshedTargetCount", context.targets.size());
        result.getSummary().put("primaryLinkStateRefreshCount", primaryLinkStateRefreshCount);
        result.getSummary().put("childLinkStateRefreshCount", childLinkStateRefreshCount);
        result.getSummary().put("sensorStateTouchedCount", sensorStateTouchedCount);
        return result;
    }

    private MessageFlowStageResult telemetryPersist(ProcessingContext context) {
        MessageFlowStageResult result = new MessageFlowStageResult();
        if (context.targets.isEmpty()) {
            result.setStatus(MessageFlowStatuses.STEP_SKIPPED);
            result.setBranch("NO_TARGETS");
            result.getSummary().put("persistedTargetCount", 0);
            result.getSummary().put("persistedPointCount", 0);
            result.getSummary().put("skippedTargetCount", 0);
            result.getSummary().put("failedTargetCount", 0);
            return result;
        }

        int persistedTargetCount = 0;
        int persistedPointCount = 0;
        int skippedTargetCount = 0;
        int failedTargetCount = 0;
        int primaryPersistedTargetCount = 0;
        int childPersistedTargetCount = 0;
        int primaryPersistedPointCount = 0;
        int childPersistedPointCount = 0;
        int legacyStableCount = 0;
        int legacyColumnCount = 0;
        int normalizedFallbackCount = 0;
        int legacyMappedMetricCount = 0;
        int legacyUnmappedMetricCount = 0;
        int fallbackMetricCount = 0;
        int skippedMetricCount = 0;
        List<String> fallbackReasons = new ArrayList<>();
        String branch = null;
        String storageMode = null;
        String errorClass = null;
        String errorMessage = null;

        for (DeviceProcessingTarget target : context.targets) {
            boolean childTarget = Boolean.TRUE.equals(target.getChildTarget());
            try {
                TelemetryPersistResult persistResult = telemetryPersistStageHandler.persist(target);
                if (persistResult == null || persistResult.isSkipped()) {
                    skippedTargetCount++;
                    if (branch == null && persistResult != null && hasText(persistResult.getBranch())) {
                        branch = persistResult.getBranch();
                    }
                    if (storageMode == null && persistResult != null && hasText(persistResult.getStorageMode())) {
                        storageMode = persistResult.getStorageMode();
                    }
                    if (persistResult != null) {
                        legacyMappedMetricCount += nullSafeInt(persistResult.getLegacyMappedMetricCount());
                        legacyUnmappedMetricCount += nullSafeInt(persistResult.getLegacyUnmappedMetricCount());
                        fallbackMetricCount += nullSafeInt(persistResult.getFallbackMetricCount());
                        skippedMetricCount += nullSafeInt(persistResult.getSkippedMetricCount());
                        mergeFallbackReasons(fallbackReasons, persistResult.getFallbackReasons());
                    }
                    continue;
                }
                persistedTargetCount++;
                int pointCount = nullSafeInt(persistResult.getPointCount());
                persistedPointCount += pointCount;
                if (childTarget) {
                    childPersistedTargetCount++;
                    childPersistedPointCount += pointCount;
                } else {
                    primaryPersistedTargetCount++;
                    primaryPersistedPointCount += pointCount;
                }
                legacyStableCount += nullSafeInt(persistResult.getLegacyStableCount());
                legacyColumnCount += nullSafeInt(persistResult.getLegacyColumnCount());
                normalizedFallbackCount += nullSafeInt(persistResult.getNormalizedFallbackCount());
                legacyMappedMetricCount += nullSafeInt(persistResult.getLegacyMappedMetricCount());
                legacyUnmappedMetricCount += nullSafeInt(persistResult.getLegacyUnmappedMetricCount());
                fallbackMetricCount += nullSafeInt(persistResult.getFallbackMetricCount());
                skippedMetricCount += nullSafeInt(persistResult.getSkippedMetricCount());
                mergeFallbackReasons(fallbackReasons, persistResult.getFallbackReasons());
                if (branch == null && hasText(persistResult.getBranch())) {
                    branch = persistResult.getBranch();
                }
                if (storageMode == null && hasText(persistResult.getStorageMode())) {
                    storageMode = persistResult.getStorageMode();
                }
            } catch (Exception ex) {
                failedTargetCount++;
                if (errorClass == null) {
                    errorClass = ex.getClass().getSimpleName();
                    errorMessage = ex.getMessage();
                }
                logTelemetryPersistFailure(target, ex);
            }
        }

        if (failedTargetCount > 0) {
            result.setStatus(MessageFlowStatuses.STEP_FAILED);
            result.setErrorClass(errorClass);
            result.setErrorMessage(errorMessage);
            result.setBranch("NON_BLOCKING_FAILURE");
        } else if (persistedPointCount == 0) {
            result.setStatus(MessageFlowStatuses.STEP_SKIPPED);
            result.setBranch(hasText(branch) ? branch : "NO_PROPERTIES");
        } else {
            result.setBranch(hasText(branch) ? branch : "TDENGINE");
        }

        result.getSummary().put("persistedTargetCount", persistedTargetCount);
        result.getSummary().put("persistedPointCount", persistedPointCount);
        result.getSummary().put("skippedTargetCount", skippedTargetCount);
        result.getSummary().put("failedTargetCount", failedTargetCount);
        result.getSummary().put("primaryPersistedTargetCount", primaryPersistedTargetCount);
        result.getSummary().put("childPersistedTargetCount", childPersistedTargetCount);
        result.getSummary().put("primaryPersistedPointCount", primaryPersistedPointCount);
        result.getSummary().put("childPersistedPointCount", childPersistedPointCount);
        result.getSummary().put("legacyStableCount", legacyStableCount);
        result.getSummary().put("legacyColumnCount", legacyColumnCount);
        result.getSummary().put("normalizedFallbackCount", normalizedFallbackCount);
        result.getSummary().put("legacyMappedMetricCount", legacyMappedMetricCount);
        result.getSummary().put("legacyUnmappedMetricCount", legacyUnmappedMetricCount);
        result.getSummary().put("fallbackMetricCount", fallbackMetricCount);
        result.getSummary().put("skippedMetricCount", skippedMetricCount);
        if (!fallbackReasons.isEmpty()) {
            result.getSummary().put("fallbackReasons", fallbackReasons);
        }
        if (hasText(storageMode)) {
            result.getSummary().put("storageMode", storageMode);
        }
        return result;
    }

    private void mergeFallbackReasons(List<String> target, List<String> incoming) {
        if (target == null || incoming == null || incoming.isEmpty()) {
            return;
        }
        for (String reason : incoming) {
            if (!hasText(reason) || target.contains(reason)) {
                continue;
            }
            target.add(reason);
        }
    }

    private int latestPropertyCount(DevicePayloadApplyResult result) {
        if (result == null || result.getSummary() == null) {
            return 0;
        }
        Object latestPropertyCount = result.getSummary().get("latestPropertyCount");
        if (latestPropertyCount != null) {
            return nullSafeInt(latestPropertyCount);
        }
        return nullSafeInt(result.getSummary().get("propertyCount"));
    }

    private int nullSafeInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text.trim()) || "1".equals(text.trim());
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
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

    private MessageFlowStageResult capabilityFeedback(ProcessingContext context) {
        String rawPayload = context.request.getPayload() == null
                ? null
                : new String(context.request.getPayload(), StandardCharsets.UTF_8);
        CapabilityFeedback feedback = capabilityFeedbackHandler.handle(context.request.getTopic(), rawPayload);
        MessageFlowStageResult result = new MessageFlowStageResult();
        result.getSummary().put("feedback", Boolean.TRUE);
        result.getSummary().put("topic", context.request.getTopic());
        result.getSummary().put("valid", feedback.valid());
        result.getSummary().put("msgid", feedback.msgid());
        result.getSummary().put("result", feedback.result());
        if (!feedback.valid()) {
            result.setBranch("INVALID");
            result.getSummary().put("invalidReason", feedback.invalidReason());
        } else {
            result.setBranch("CAPABILITY_FEEDBACK");
        }
        return result;
    }

    private boolean isCapabilityFeedback(ProcessingContext context) {
        return "MQTT".equalsIgnoreCase(context.request.getTransportMode())
                && capabilityFeedbackTopicMatcher.matches(context.request.getTopic());
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

        messageFlowMetricsRecorder.recordSession(
                context.request.getTransportMode(),
                throwable == null ? MessageFlowStatuses.SESSION_COMPLETED : MessageFlowStatuses.SESSION_FAILED
        );
        saveSessionIfEnabled(session);
        saveTimelineIfEnabled(context.timeline);
        logSummary(context, throwable);
        recordPersistentEvidence(context, throwable);
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
            messageFlowMetricsRecorder.recordStageDuration(
                    stage,
                    context.request.getTransportMode(),
                    step.getStatus(),
                    step.getCostMs()
            );
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
            messageFlowMetricsRecorder.recordStageDuration(
                    stage,
                    context.request.getTransportMode(),
                    step.getStatus(),
                    step.getCostMs()
            );
            MessagePipelineFailureMetadata.attach(ex, stage, context.traceId, context.rawDeviceMessage, context.upMessage);
            throw ex;
        }
    }

    private void executeWithinTransaction(Runnable action) {
        transactionExecutor.execute(action);
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

    private void rebindTimelineSessionId(ProcessingContext context, String reboundSessionId) {
        if (!hasText(reboundSessionId)) {
            return;
        }
        String normalizedSessionId = reboundSessionId.trim();
        String previousSessionId = context.sessionId;
        context.sessionId = normalizedSessionId;
        context.timeline.setSessionId(normalizedSessionId);
        for (MessageFlowStep step : context.timeline.getSteps()) {
            if (step == null || step.getSummary() == null || !step.getSummary().containsKey("sessionId")) {
                continue;
            }
            Object summarySessionId = step.getSummary().get("sessionId");
            if (summarySessionId == null
                    || !hasText(String.valueOf(summarySessionId))
                    || previousSessionId == null
                    || previousSessionId.equals(String.valueOf(summarySessionId))) {
                step.getSummary().put("sessionId", normalizedSessionId);
            }
        }
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

    private void logTelemetryPersistFailure(DeviceProcessingTarget target, Exception ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("traceId", target == null || target.getMessage() == null ? null : target.getMessage().getTraceId());
        details.put("deviceCode", target == null || target.getDevice() == null ? null : target.getDevice().getDeviceCode());
        details.put("productKey", target == null || target.getMessage() == null ? null : target.getMessage().getProductKey());
        details.put("messageType", target == null || target.getMessage() == null ? null : target.getMessage().getMessageType());
        details.put("topic", target == null || target.getMessage() == null ? null : target.getMessage().getTopic());
        messageFlowLogger.warn(
                ObservabilityEventLogSupport.summary("telemetry_persist", "failure", null, details),
                ex
        );
    }

    private void recordPersistentEvidence(ProcessingContext context, Throwable throwable) {
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTenantId(1L);
        span.setTraceId(context.traceId);
        span.setSpanType(ObservabilitySpanTypes.MESSAGE_FLOW);
        span.setSpanName("message-flow." + normalizeTagValue(context.request.getTransportMode(), "unknown"));
        span.setDomainCode("message");
        span.setEventCode("iot.message.flow");
        span.setObjectType("device");
        span.setObjectId(resolveDeviceCode(context));
        span.setTransportType(context.request.getTransportMode());
        span.setStatus(throwable == null ? ObservabilityEvidenceStatus.SUCCESS : ObservabilityEvidenceStatus.FAILURE);
        span.setDurationMs(context.timeline.getTotalCostMs());
        span.setStartedAt(context.timeline.getStartedAt());
        span.setFinishedAt(context.timeline.getFinishedAt());
        if (throwable != null) {
            span.setErrorClass(throwable.getClass().getName());
            span.setErrorMessage(throwable.getMessage());
        }
        span.getTags().putAll(buildMessageFlowEvidenceTags(context, throwable));
        evidenceRecorder.recordSpan(span);

        if (throwable != null) {
            recordMessageFlowFailureEvent(context, throwable);
        }
    }

    private void recordMessageFlowFailureEvent(ProcessingContext context, Throwable throwable) {
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(1L);
        event.setTraceId(context.traceId);
        event.setEventCode("iot.message.failure");
        event.setEventName("设备上行处理失败");
        event.setDomainCode("message");
        event.setActionCode("up_message_failure");
        event.setObjectType("device");
        event.setObjectId(resolveDeviceCode(context));
        event.setResultStatus(ObservabilityEvidenceStatus.FAILURE);
        event.setSourceType(normalizeTagValue(context.request.getTransportMode(), "MESSAGE_FLOW"));
        event.setEvidenceType("message_flow");
        event.setEvidenceId(context.sessionId);
        event.setDurationMs(context.timeline.getTotalCostMs());
        event.setErrorMessage(throwable.getMessage());
        event.setOccurredAt(LocalDateTime.now());
        event.getMetadata().putAll(buildMessageFlowEvidenceTags(context, throwable));
        evidenceRecorder.recordBusinessEvent(event);
    }

    private Map<String, Object> buildMessageFlowEvidenceTags(ProcessingContext context, Throwable throwable) {
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("sessionId", context.sessionId);
        tags.put("traceId", context.traceId);
        tags.put("transportMode", context.request.getTransportMode());
        tags.put("status", context.timeline.getStatus());
        tags.put("topic", resolveTopic(context));
        tags.put("deviceCode", resolveDeviceCode(context));
        tags.put("productKey", resolveProductKey(context));
        tags.put("messageType", context.upMessage == null ? null : context.upMessage.getMessageType());
        MessageFlowStep slowestStep = context.timeline.getSteps().stream()
                .max(java.util.Comparator.comparingLong(MessageFlowStep::getCostMs))
                .orElse(null);
        if (slowestStep != null) {
            tags.put("slowestStage", slowestStep.getStage());
            tags.put("slowestStageCostMs", slowestStep.getCostMs());
        }
        if (throwable != null) {
            tags.put("errorClass", throwable.getClass().getName());
        }
        List<Map<String, Object>> stages = new ArrayList<>();
        for (MessageFlowStep step : context.timeline.getSteps()) {
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("stage", step.getStage());
            stage.put("status", step.getStatus());
            stage.put("costMs", step.getCostMs());
            stage.put("handlerClass", step.getHandlerClass());
            if (step.getErrorClass() != null) {
                stage.put("errorClass", step.getErrorClass());
            }
            stages.add(stage);
        }
        tags.put("stages", stages);
        return tags;
    }

    private String resolveTopic(ProcessingContext context) {
        if (context.rawDeviceMessage != null && hasText(context.rawDeviceMessage.getTopic())) {
            return context.rawDeviceMessage.getTopic();
        }
        return context.upMessage == null ? null : context.upMessage.getTopic();
    }

    private String resolveDeviceCode(ProcessingContext context) {
        if (context.upMessage != null && hasText(context.upMessage.getDeviceCode())) {
            return context.upMessage.getDeviceCode();
        }
        return context.rawDeviceMessage == null ? null : context.rawDeviceMessage.getDeviceCode();
    }

    private String resolveProductKey(ProcessingContext context) {
        if (context.upMessage != null && hasText(context.upMessage.getProductKey())) {
            return context.upMessage.getProductKey();
        }
        return context.rawDeviceMessage == null ? null : context.rawDeviceMessage.getProductKey();
    }

    private String normalizeTagValue(String value, String fallback) {
        return hasText(value) ? value : fallback;
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
