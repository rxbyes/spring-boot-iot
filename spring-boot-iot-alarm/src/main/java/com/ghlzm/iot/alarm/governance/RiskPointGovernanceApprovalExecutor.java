package com.ghlzm.iot.alarm.governance;

import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionMetricDTO;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionRequest;
import com.ghlzm.iot.alarm.dto.RiskPointDeviceCapabilityBindingRequest;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointPendingPromotionService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionResultVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Executes risk-point governance approval actions inside the alarm domain.
 */
@Service
public class RiskPointGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

    public static final String ACTION_RISK_POINT_BIND_DEVICE = "RISK_POINT_BIND_DEVICE";
    public static final String ACTION_RISK_POINT_UNBIND_DEVICE = "RISK_POINT_UNBIND_DEVICE";
    public static final String ACTION_RISK_POINT_PENDING_PROMOTION = "RISK_POINT_PENDING_PROMOTION";
    public static final String BINDING_MODE_METRIC = "METRIC";
    public static final String BINDING_MODE_DEVICE_ONLY = "DEVICE_ONLY";

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final List<String> RISK_BINDING_AFFECTED_TYPES = List.of("RISK_POINT", "DEVICE", "RISK_BINDING");
    private static final List<String> RISK_PROMOTION_AFFECTED_TYPES = List.of("RISK_POINT", "RISK_METRIC", "RISK_BINDING");

    private final RiskPointBindingMaintenanceService bindingMaintenanceService;
    private final RiskPointPendingPromotionService pendingPromotionService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskPointGovernanceApprovalExecutor(RiskPointBindingMaintenanceService bindingMaintenanceService,
                                               RiskPointPendingPromotionService pendingPromotionService) {
        this.bindingMaintenanceService = bindingMaintenanceService;
        this.pendingPromotionService = pendingPromotionService;
    }

    @Override
    public boolean supports(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return false;
        }
        String normalized = actionCode.trim();
        return ACTION_RISK_POINT_BIND_DEVICE.equals(normalized)
                || ACTION_RISK_POINT_UNBIND_DEVICE.equals(normalized)
                || ACTION_RISK_POINT_PENDING_PROMOTION.equals(normalized);
    }

    @Override
    public GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        String actionCode = order.getActionCode().trim();
        return switch (actionCode) {
            case ACTION_RISK_POINT_BIND_DEVICE -> executeBind(order);
            case ACTION_RISK_POINT_UNBIND_DEVICE -> executeUnbind(order);
            case ACTION_RISK_POINT_PENDING_PROMOTION -> executePendingPromotion(order);
            default -> throw new BizException("审批动作不支持执行: " + actionCode);
        };
    }

    @Override
    public GovernanceSimulationResult simulate(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        String actionCode = order.getActionCode().trim();
        return switch (actionCode) {
            case ACTION_RISK_POINT_BIND_DEVICE -> simulateBind(order);
            case ACTION_RISK_POINT_UNBIND_DEVICE -> simulateUnbind(order);
            case ACTION_RISK_POINT_PENDING_PROMOTION -> simulatePendingPromotion(order);
            default -> throw new BizException("审批动作不支持预演: " + actionCode);
        };
    }

    public static String writeBindPayload(RiskPointDevice request) {
        if (request == null) {
            throw new BizException("风险点绑定审批载荷不能为空");
        }
        ObjectNode root = JsonMapper.builder().findAndAddModules().build().createObjectNode();
        ObjectNode requestNode = root.putObject("request");
        writeNullableText(requestNode, "bindingMode", BINDING_MODE_METRIC);
        writeNullableLong(requestNode, "riskPointId", request.getRiskPointId());
        writeNullableLong(requestNode, "deviceId", request.getDeviceId());
        writeNullableLong(requestNode, "riskMetricId", request.getRiskMetricId());
        writeNullableText(requestNode, "deviceCode", request.getDeviceCode());
        writeNullableText(requestNode, "deviceName", request.getDeviceName());
        writeNullableText(requestNode, "metricIdentifier", request.getMetricIdentifier());
        writeNullableText(requestNode, "metricName", request.getMetricName());
        return root.toString();
    }

    public static String writeBindPayload(RiskPointDeviceCapabilityBindingRequest request,
                                          String deviceCapabilityType,
                                          String extensionStatus) {
        if (request == null) {
            throw new BizException("风险点绑定审批载荷不能为空");
        }
        ObjectNode root = JsonMapper.builder().findAndAddModules().build().createObjectNode();
        ObjectNode requestNode = root.putObject("request");
        writeNullableText(requestNode, "bindingMode", BINDING_MODE_DEVICE_ONLY);
        writeNullableLong(requestNode, "riskPointId", request.getRiskPointId());
        writeNullableLong(requestNode, "deviceId", request.getDeviceId());
        writeNullableText(requestNode, "deviceCapabilityType", deviceCapabilityType);
        writeNullableText(requestNode, "extensionStatus", extensionStatus);
        return root.toString();
    }

    public static String writeUnbindPayload(Long riskPointId,
                                            Long deviceId,
                                            String deviceCode,
                                            String deviceName) {
        ObjectNode root = JsonMapper.builder().findAndAddModules().build().createObjectNode();
        ObjectNode requestNode = root.putObject("request");
        writeNullableLong(requestNode, "riskPointId", riskPointId);
        writeNullableLong(requestNode, "deviceId", deviceId);
        writeNullableText(requestNode, "deviceCode", deviceCode);
        writeNullableText(requestNode, "deviceName", deviceName);
        return root.toString();
    }

    public static String writePendingPromotionPayload(RiskPointDevicePendingBinding pending,
                                                      RiskPointPendingPromotionRequest request) {
        if (pending == null) {
            throw new BizException("待治理记录不存在");
        }
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode requestNode = root.putObject("request");
        writeNullableLong(requestNode, "pendingId", pending.getId());
        writeNullableLong(requestNode, "riskPointId", pending.getRiskPointId());
        writeNullableLong(requestNode, "deviceId", pending.getDeviceId());
        writeNullableText(requestNode, "riskPointCode", pending.getRiskPointCode());
        writeNullableText(requestNode, "riskPointName", pending.getRiskPointName());
        writeNullableText(requestNode, "deviceCode", pending.getDeviceCode());
        writeNullableText(requestNode, "deviceName", pending.getDeviceName());
        if (request != null) {
            requestNode.put("completePending", Boolean.TRUE.equals(request.getCompletePending()));
            writeNullableText(requestNode, "promotionNote", request.getPromotionNote());
            ArrayNode metricsNode = requestNode.putArray("metrics");
            if (request.getMetrics() != null) {
                for (RiskPointPendingPromotionMetricDTO metric : request.getMetrics()) {
                    if (metric == null) {
                        continue;
                    }
                    ObjectNode metricNode = metricsNode.addObject();
                    writeNullableLong(metricNode, "riskMetricId", metric.getRiskMetricId());
                    writeNullableText(metricNode, "metricIdentifier", metric.getMetricIdentifier());
                    writeNullableText(metricNode, "metricName", metric.getMetricName());
                }
            }
        }
        return root.toString();
    }

    private GovernanceApprovalActionExecutionResult executeBind(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "风险点绑定审批载荷缺少请求体");
        String bindingMode = readOptionalText(requestNode, "bindingMode");
        if (BINDING_MODE_DEVICE_ONLY.equalsIgnoreCase(bindingMode)) {
            RiskPointDeviceCapabilityBindingRequest request = new RiskPointDeviceCapabilityBindingRequest();
            request.setRiskPointId(readRequiredLong(requestNode, "riskPointId", "风险点绑定审批载荷缺少风险点 ID"));
            request.setDeviceId(readRequiredLong(requestNode, "deviceId", "风险点绑定审批载荷缺少设备 ID"));
            request.setDeviceCapabilityType(readOptionalText(requestNode, "deviceCapabilityType"));
            RiskPointDeviceCapabilityBinding saved = bindingMaintenanceService.bindDeviceCapability(request, order.getOperatorUserId());
            String payloadJson = appendExecutionResult(order.getPayloadJson(), buildDeviceOnlyBindExecutionResult(saved));
            String impactSnapshotJson = buildImpactSnapshot(
                    readOptionalLong(requestNode, "riskPointId"),
                    readOptionalLong(requestNode, "deviceId"),
                    null,
                    1,
                    true,
                    "可通过风险点解绑撤回本次设备级正式绑定"
            );
            String rollbackSnapshotJson = buildRollbackSnapshot(
                    readOptionalLong(requestNode, "riskPointId"),
                    readOptionalLong(requestNode, "deviceId"),
                    null,
                    true
            );
            return new GovernanceApprovalActionExecutionResult(payloadJson, impactSnapshotJson, rollbackSnapshotJson);
        }
        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(readRequiredLong(requestNode, "riskPointId", "风险点绑定审批载荷缺少风险点 ID"));
        request.setDeviceId(readRequiredLong(requestNode, "deviceId", "风险点绑定审批载荷缺少设备 ID"));
        request.setRiskMetricId(readOptionalLong(requestNode, "riskMetricId"));
        request.setDeviceCode(readOptionalText(requestNode, "deviceCode"));
        request.setDeviceName(readOptionalText(requestNode, "deviceName"));
        request.setMetricIdentifier(readRequiredText(requestNode, "metricIdentifier", "风险点绑定审批载荷缺少测点标识"));
        request.setMetricName(readOptionalText(requestNode, "metricName"));
        RiskPointDevice saved = bindingMaintenanceService.bindDevice(request, order.getOperatorUserId());
        String payloadJson = appendExecutionResult(order.getPayloadJson(), buildBindExecutionResult(saved));
        String impactSnapshotJson = buildImpactSnapshot(
                readOptionalLong(requestNode, "riskPointId"),
                readOptionalLong(requestNode, "deviceId"),
                readOptionalText(requestNode, "metricIdentifier"),
                1,
                true,
                "可通过风险点解绑撤回本次正式绑定"
        );
        String rollbackSnapshotJson = buildRollbackSnapshot(
                readOptionalLong(requestNode, "riskPointId"),
                readOptionalLong(requestNode, "deviceId"),
                readOptionalText(requestNode, "metricIdentifier"),
                true
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, impactSnapshotJson, rollbackSnapshotJson);
    }

    private GovernanceApprovalActionExecutionResult executeUnbind(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "风险点解绑审批载荷缺少请求体");
        Long riskPointId = readRequiredLong(requestNode, "riskPointId", "风险点解绑审批载荷缺少风险点 ID");
        Long deviceId = readRequiredLong(requestNode, "deviceId", "风险点解绑审批载荷缺少设备 ID");
        bindingMaintenanceService.unbindDevice(riskPointId, deviceId, order.getOperatorUserId());
        String payloadJson = appendExecutionResult(order.getPayloadJson(), buildUnbindExecutionResult(riskPointId, deviceId));
        String impactSnapshotJson = buildImpactSnapshot(
                riskPointId,
                deviceId,
                null,
                1,
                false,
                "解绑执行后需人工重新绑定恢复"
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, impactSnapshotJson, null);
    }

    private GovernanceApprovalActionExecutionResult executePendingPromotion(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "待治理转正审批载荷缺少请求体");
        Long pendingId = readRequiredLong(requestNode, "pendingId", "待治理转正审批载荷缺少 pending ID");
        RiskPointPendingPromotionRequest request = new RiskPointPendingPromotionRequest();
        if (requestNode.has("completePending")) {
            request.setCompletePending(requestNode.path("completePending").asBoolean(false));
        }
        request.setPromotionNote(readOptionalText(requestNode, "promotionNote"));
        if (requestNode.has("metrics") && requestNode.path("metrics").isArray()) {
            java.util.List<RiskPointPendingPromotionMetricDTO> metrics = new java.util.ArrayList<>();
            for (JsonNode metricNode : requestNode.path("metrics")) {
                RiskPointPendingPromotionMetricDTO metric = new RiskPointPendingPromotionMetricDTO();
                metric.setRiskMetricId(readOptionalLong(metricNode, "riskMetricId"));
                metric.setMetricIdentifier(readOptionalText(metricNode, "metricIdentifier"));
                metric.setMetricName(readOptionalText(metricNode, "metricName"));
                metrics.add(metric);
            }
            request.setMetrics(metrics);
        }
        RiskPointPendingPromotionResultVO result = pendingPromotionService.promote(pendingId, request, order.getOperatorUserId());
        int affectedCount = result == null || result.getItems() == null ? 0 : result.getItems().size();
        String payloadJson = appendExecutionResult(order.getPayloadJson(), buildPendingPromotionExecutionResult(result));
        String impactSnapshotJson = buildImpactSnapshot(
                readOptionalLong(requestNode, "riskPointId"),
                readOptionalLong(requestNode, "deviceId"),
                null,
                affectedCount,
                true,
                "可通过正式绑定维护撤回本次转正测点"
        );
        String rollbackSnapshotJson = buildRollbackSnapshot(
                readOptionalLong(requestNode, "riskPointId"),
                readOptionalLong(requestNode, "deviceId"),
                null,
                true
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, impactSnapshotJson, rollbackSnapshotJson);
    }

    private GovernanceSimulationResult simulateBind(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "风险点绑定审批载荷缺少请求体");
        readRequiredLong(requestNode, "riskPointId", "风险点绑定审批载荷缺少风险点 ID");
        readRequiredLong(requestNode, "deviceId", "风险点绑定审批载荷缺少设备 ID");
        String bindingMode = readOptionalText(requestNode, "bindingMode");
        if (!BINDING_MODE_DEVICE_ONLY.equalsIgnoreCase(bindingMode)) {
            readRequiredText(requestNode, "metricIdentifier", "风险点绑定审批载荷缺少测点标识");
        }
        GovernanceImpactSnapshot impact = buildImpactSnapshotObject(1L, RISK_BINDING_AFFECTED_TYPES, true, "可通过风险点解绑撤回本次正式绑定");
        GovernanceRollbackSnapshot rollback = buildRollbackSnapshotObject(true, "可通过风险点解绑撤回本次正式绑定");
        return buildSimulationResult(order, impact, rollback);
    }

    private GovernanceSimulationResult simulateUnbind(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "风险点解绑审批载荷缺少请求体");
        readRequiredLong(requestNode, "riskPointId", "风险点解绑审批载荷缺少风险点 ID");
        readRequiredLong(requestNode, "deviceId", "风险点解绑审批载荷缺少设备 ID");
        GovernanceImpactSnapshot impact = buildImpactSnapshotObject(1L, RISK_BINDING_AFFECTED_TYPES, false, "解绑执行后需人工重新绑定恢复");
        GovernanceRollbackSnapshot rollback = buildRollbackSnapshotObject(false, "解绑执行后需人工重新绑定恢复");
        return buildSimulationResult(order, impact, rollback);
    }

    private GovernanceSimulationResult simulatePendingPromotion(GovernanceApprovalOrder order) {
        JsonNode requestNode = readRequiredRequestNode(order.getPayloadJson(), "待治理转正审批载荷缺少请求体");
        readRequiredLong(requestNode, "pendingId", "待治理转正审批载荷缺少 pending ID");
        long affectedCount = resolvePromotionAffectedCount(requestNode);
        GovernanceImpactSnapshot impact = buildImpactSnapshotObject(
                affectedCount,
                RISK_PROMOTION_AFFECTED_TYPES,
                true,
                "可通过正式绑定维护撤回本次转正测点"
        );
        GovernanceRollbackSnapshot rollback = buildRollbackSnapshotObject(true, "可通过正式绑定维护撤回本次转正测点");
        return buildSimulationResult(order, impact, rollback);
    }

    private JsonNode readRequiredRequestNode(String payloadJson, String message) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode requestNode = root == null ? null : root.path("request");
            if (requestNode == null || requestNode.isMissingNode() || requestNode.isNull()) {
                throw new BizException(message);
            }
            return requestNode;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(message);
        }
    }

    private String appendExecutionResult(String payloadJson, ObjectNode resultNode) {
        try {
            ObjectNode root = payloadJson == null || payloadJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(payloadJson);
            ObjectNode executionNode = root.putObject("execution");
            executionNode.put("status", STATUS_SUCCESS);
            executionNode.set("result", resultNode);
            return root.toString();
        } catch (Exception ex) {
            throw new BizException("审批执行结果写回失败");
        }
    }

    private ObjectNode buildBindExecutionResult(RiskPointDevice saved) {
        ObjectNode result = objectMapper.createObjectNode();
        writeNullableLong(result, "bindingId", saved == null ? null : saved.getId());
        writeNullableLong(result, "riskPointId", saved == null ? null : saved.getRiskPointId());
        writeNullableLong(result, "deviceId", saved == null ? null : saved.getDeviceId());
        writeNullableLong(result, "riskMetricId", saved == null ? null : saved.getRiskMetricId());
        writeNullableText(result, "metricIdentifier", saved == null ? null : saved.getMetricIdentifier());
        writeNullableText(result, "metricName", saved == null ? null : saved.getMetricName());
        return result;
    }

    private ObjectNode buildDeviceOnlyBindExecutionResult(RiskPointDeviceCapabilityBinding saved) {
        ObjectNode result = objectMapper.createObjectNode();
        writeNullableLong(result, "bindingId", saved == null ? null : saved.getId());
        writeNullableLong(result, "riskPointId", saved == null ? null : saved.getRiskPointId());
        writeNullableLong(result, "deviceId", saved == null ? null : saved.getDeviceId());
        writeNullableText(result, "bindingMode", BINDING_MODE_DEVICE_ONLY);
        writeNullableText(result, "deviceCapabilityType", saved == null ? null : saved.getDeviceCapabilityType());
        writeNullableText(result, "extensionStatus", saved == null ? null : saved.getExtensionStatus());
        return result;
    }

    private ObjectNode buildUnbindExecutionResult(Long riskPointId, Long deviceId) {
        ObjectNode result = objectMapper.createObjectNode();
        writeNullableLong(result, "riskPointId", riskPointId);
        writeNullableLong(result, "deviceId", deviceId);
        result.put("unbindCompleted", true);
        return result;
    }

    private ObjectNode buildPendingPromotionExecutionResult(RiskPointPendingPromotionResultVO result) {
        ObjectNode node = objectMapper.createObjectNode();
        writeNullableLong(node, "pendingId", result == null ? null : result.getPendingId());
        writeNullableText(node, "pendingStatus", result == null ? null : result.getPendingStatus());
        ArrayNode itemsNode = node.putArray("items");
        if (result != null && result.getItems() != null) {
            result.getItems().forEach(item -> {
                ObjectNode itemNode = itemsNode.addObject();
                writeNullableText(itemNode, "metricIdentifier", item.getMetricIdentifier());
                writeNullableText(itemNode, "metricName", item.getMetricName());
                writeNullableText(itemNode, "promotionStatus", item.getPromotionStatus());
                writeNullableLong(itemNode, "bindingId", item.getBindingId());
            });
        }
        return node;
    }

    private GovernanceSimulationResult buildSimulationResult(GovernanceApprovalOrder order,
                                                             GovernanceImpactSnapshot impact,
                                                             GovernanceRollbackSnapshot rollback) {
        boolean rollbackable = rollback != null && Boolean.TRUE.equals(rollback.getRollbackable());
        String rollbackPlanSummary = impact != null && StringUtils.hasText(impact.getRollbackPlanSummary())
                ? impact.getRollbackPlanSummary()
                : rollback == null ? null : rollback.getRollbackPlanSummary();
        return new GovernanceSimulationResult(
                order == null ? null : order.getId(),
                order == null ? null : order.getWorkItemId(),
                order == null ? null : order.getActionCode(),
                true,
                impact == null ? null : impact.getAffectedCount(),
                impact == null || impact.getAffectedTypes() == null ? List.of() : impact.getAffectedTypes(),
                rollbackable,
                rollbackPlanSummary,
                null,
                impact,
                rollback,
                false,
                null
        );
    }

    private GovernanceImpactSnapshot buildImpactSnapshotObject(Long affectedCount,
                                                               List<String> affectedTypes,
                                                               boolean rollbackable,
                                                               String rollbackPlanSummary) {
        GovernanceImpactSnapshot snapshot = new GovernanceImpactSnapshot();
        snapshot.setAffectedCount(affectedCount);
        snapshot.setAffectedTypes(affectedTypes);
        snapshot.setRollbackable(rollbackable);
        snapshot.setRollbackPlanSummary(rollbackPlanSummary);
        return snapshot;
    }

    private GovernanceRollbackSnapshot buildRollbackSnapshotObject(boolean rollbackable,
                                                                   String rollbackPlanSummary) {
        GovernanceRollbackSnapshot snapshot = new GovernanceRollbackSnapshot();
        snapshot.setRollbackable(rollbackable);
        snapshot.setRollbackPlanSummary(rollbackPlanSummary);
        return snapshot;
    }

    private String buildImpactSnapshot(Long riskPointId,
                                       Long deviceId,
                                       String metricIdentifier,
                                       int affectedBindingCount,
                                       boolean rollbackable,
                                       String rollbackPlanSummary) {
        ObjectNode root = objectMapper.createObjectNode();
        writeNullableLong(root, "riskPointId", riskPointId);
        writeNullableLong(root, "deviceId", deviceId);
        writeNullableText(root, "metricIdentifier", metricIdentifier);
        root.put("affectedRiskPointCount", riskPointId == null ? 0 : 1);
        root.put("affectedDeviceCount", deviceId == null ? 0 : 1);
        root.put("affectedBindingCount", Math.max(affectedBindingCount, 0));
        root.put("rollbackable", rollbackable);
        writeNullableText(root, "rollbackPlanSummary", rollbackPlanSummary);
        return root.toString();
    }

    private String buildRollbackSnapshot(Long riskPointId,
                                         Long deviceId,
                                         String metricIdentifier,
                                         boolean rollbackable) {
        ObjectNode root = objectMapper.createObjectNode();
        writeNullableLong(root, "riskPointId", riskPointId);
        writeNullableLong(root, "deviceId", deviceId);
        writeNullableText(root, "metricIdentifier", metricIdentifier);
        root.put("rollbackable", rollbackable);
        return root.toString();
    }

    private Long readRequiredLong(JsonNode node, String fieldName, String message) {
        Long value = readOptionalLong(node, fieldName);
        if (value == null) {
            throw new BizException(message);
        }
        return value;
    }

    private Long readOptionalLong(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.canConvertToLong()) {
            return valueNode.longValue();
        }
        if (valueNode.isTextual()) {
            String text = valueNode.asText().trim();
            if (!text.isEmpty() && text.matches("^-?\\d+$")) {
                return Long.parseLong(text);
            }
        }
        return null;
    }

    private long resolvePromotionAffectedCount(JsonNode requestNode) {
        if (requestNode == null || !requestNode.has("metrics") || !requestNode.path("metrics").isArray()) {
            return 0L;
        }
        long count = 0L;
        for (JsonNode metricNode : requestNode.path("metrics")) {
            if (metricNode != null && !metricNode.isNull()) {
                count++;
            }
        }
        return count;
    }

    private String readRequiredText(JsonNode node, String fieldName, String message) {
        String value = readOptionalText(node, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new BizException(message);
        }
        return value;
    }

    private String readOptionalText(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.path(fieldName).isNull()) {
            return null;
        }
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static void writeNullableLong(ObjectNode node, String fieldName, Long value) {
        if (node != null && value != null) {
            node.put(fieldName, value);
        }
    }

    private static void writeNullableText(ObjectNode node, String fieldName, String value) {
        if (node != null && StringUtils.hasText(value)) {
            node.put(fieldName, value.trim());
        }
    }
}
