package com.ghlzm.iot.protocol.mqtt.legacy.template.replay;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateReplayDTO;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;
import com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpRelationRule;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplate;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateContext;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutionResult;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateExecutor;
import com.ghlzm.iot.protocol.mqtt.legacy.template.LegacyDpChildTemplateRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class LegacyDpTemplateReplayServiceImpl implements LegacyDpTemplateReplayService {

    private static final String BUILTIN_CRACK_TEMPLATE_CODE = "crack_child_template";
    private static final String BUILTIN_DEEP_DISPLACEMENT_TEMPLATE_CODE = "deep_displacement_child_template";

    private final LegacyDpChildTemplateRegistry registry;
    private final LegacyDpChildTemplateExecutor executor;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public LegacyDpTemplateReplayServiceImpl() {
        this(new LegacyDpChildTemplateRegistry(), new LegacyDpChildTemplateExecutor());
    }

    public LegacyDpTemplateReplayServiceImpl(LegacyDpChildTemplateRegistry registry,
                                             LegacyDpChildTemplateExecutor executor) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public ProtocolTemplateReplayVO replay(ProtocolTemplateReplayDTO dto) {
        String requestedTemplateCode = normalizeText(dto == null ? null : dto.getTemplateCode());
        if (!StringUtils.hasText(requestedTemplateCode)) {
            throw new BizException("templateCode 不能为空");
        }
        String payloadJson = normalizeText(dto == null ? null : dto.getPayloadJson());
        if (!StringUtils.hasText(payloadJson)) {
            throw new BizException("payloadJson 不能为空");
        }

        Map<String, Object> payload = parsePayload(payloadJson);
        LegacyDpReplayProfile profile = resolveProfile(requestedTemplateCode);
        LegacyDpChildTemplate template = resolveBuiltinTemplate(profile.builtinTemplateCode());
        if (template == null) {
            return buildMiss(requestedTemplateCode, profile.builtinTemplateCode());
        }

        List<ProtocolTemplateReplayVO.ExtractedChild> extractedChildren = new ArrayList<>();
        Map<String, Object> parentProperties = resolveParentProperties(payload);
        for (Map.Entry<String, Object> entry : sortEntries(payload)) {
            String logicalCode = entry.getKey();
            if (!profile.matchesLogicalCode(logicalCode)) {
                continue;
            }
            LegacyDpChildTemplateContext context = new LegacyDpChildTemplateContext(
                    new LegacyDpRelationRule(
                            logicalCode,
                            "replay-child-" + logicalCode.toLowerCase(Locale.ROOT),
                            profile.canonicalizationStrategy(),
                            profile.statusMirrorStrategy()
                    ),
                    logicalCode,
                    entry.getValue(),
                    parentProperties
            );
            if (!template.matches(context)) {
                continue;
            }
            LegacyDpChildTemplateExecutionResult execution = executor.execute(template, context);
            extractedChildren.add(toExtractedChild(execution, logicalCode));
        }

        ProtocolTemplateReplayVO result = new ProtocolTemplateReplayVO();
        result.setTemplateCode(requestedTemplateCode);
        result.setResolvedTemplateCode(profile.builtinTemplateCode());
        result.setMatched(!extractedChildren.isEmpty());
        result.setSummary(extractedChildren.isEmpty() ? "未命中模板" : "命中 " + extractedChildren.size() + " 个逻辑通道");
        result.setExtractedChildren(extractedChildren);
        return result;
    }

    private ProtocolTemplateReplayVO buildMiss(String requestedTemplateCode, String resolvedTemplateCode) {
        ProtocolTemplateReplayVO result = new ProtocolTemplateReplayVO();
        result.setTemplateCode(requestedTemplateCode);
        result.setResolvedTemplateCode(resolvedTemplateCode);
        result.setMatched(Boolean.FALSE);
        result.setSummary("未命中模板");
        result.setExtractedChildren(List.of());
        return result;
    }

    private ProtocolTemplateReplayVO.ExtractedChild toExtractedChild(LegacyDpChildTemplateExecutionResult execution,
                                                                     String logicalCode) {
        ProtocolTemplateReplayVO.ExtractedChild child = new ProtocolTemplateReplayVO.ExtractedChild();
        child.setLogicalChannelCode(logicalCode);
        child.setChildProperties(execution.childProperties());
        child.setCanonicalizationStrategy(execution.canonicalizationStrategy());
        child.setStatusMirrorApplied(execution.statusMirrorApplied());
        child.setRawPayload(execution.rawPayload());
        return child;
    }

    private LegacyDpChildTemplate resolveBuiltinTemplate(String templateCode) {
        return registry.listTemplates().stream()
                .filter(template -> template != null && templateCode.equals(template.getTemplateCode()))
                .findFirst()
                .orElse(null);
    }

    private LegacyDpReplayProfile resolveProfile(String templateCode) {
        String normalized = normalizeLower(templateCode);
        if (normalized.contains("deep")) {
            return new LegacyDpReplayProfile(
                    BUILTIN_DEEP_DISPLACEMENT_TEMPLATE_CODE,
                    "^L1_SW_\\d+$",
                    "LEGACY",
                    "NONE"
            );
        }
        return new LegacyDpReplayProfile(
                BUILTIN_CRACK_TEMPLATE_CODE,
                "^L1_LF_\\d+$",
                "LF_VALUE",
                "SENSOR_STATE"
        );
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
            return payload == null ? Map.of() : payload;
        } catch (Exception ex) {
            throw new BizException("payloadJson 必须是合法 JSON");
        }
    }

    private List<Map.Entry<String, Object>> sortEntries(Map<String, Object> payload) {
        List<Map.Entry<String, Object>> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getKey() != null) {
                entries.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }
        entries.sort(Map.Entry.comparingByKey());
        return entries;
    }

    private Map<String, Object> resolveParentProperties(Map<String, Object> payload) {
        Object parent = payload.get("S1_ZT_1");
        if (!(parent instanceof Map<?, ?> parentMap)) {
            return Map.of();
        }
        Object sensorState = parentMap.get("sensor_state");
        if (!(sensorState instanceof Map<?, ?> sensorMap)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : sensorMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put("S1_ZT_1.sensor_state." + key, entry.getValue());
            }
        }
        return result;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : "";
    }

    private record LegacyDpReplayProfile(String builtinTemplateCode,
                                         String logicalCodeRegex,
                                         String canonicalizationStrategy,
                                         String statusMirrorStrategy) {

        boolean matchesLogicalCode(String logicalCode) {
            return StringUtils.hasText(logicalCode) && logicalCode.matches(logicalCodeRegex);
        }
    }
}
