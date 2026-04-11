package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.governance.RiskPointGovernanceApprovalExecutor;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import com.ghlzm.iot.system.service.model.GovernanceWorkItemCommand;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 风险点绑定维护服务实现。
 */
@Service
public class RiskPointBindingMaintenanceServiceImpl implements RiskPointBindingMaintenanceService {

    private static final String STATUS_PENDING_METRIC_GOVERNANCE = "PENDING_METRIC_GOVERNANCE";
    private static final String STATUS_PARTIALLY_PROMOTED = "PARTIALLY_PROMOTED";
    private static final String STATUS_PROMOTION_SUCCESS = "SUCCESS";
    private static final String SOURCE_PENDING_PROMOTION = "PENDING_PROMOTION";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String WORK_ITEM_CODE_RISK_BINDING = "PENDING_RISK_BINDING";
    private static final String TASK_CATEGORY_RISK_BINDING = "RISK_BINDING";
    private static final String DOMAIN_CODE_ALARM = "ALARM";
    private static final String EXECUTION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String EXECUTION_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String COMMENT_DIRECT_APPLIED = "DIRECT_APPLIED";

    private final RiskPointService riskPointService;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RiskPointDevicePendingBindingMapper pendingBindingMapper;
    private final RiskPointDevicePendingPromotionMapper pendingPromotionMapper;
    private final GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver;
    private final GovernanceApprovalService governanceApprovalService;
    private final GovernanceWorkItemService governanceWorkItemService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskPointBindingMaintenanceServiceImpl(RiskPointService riskPointService,
                                                  RiskPointDeviceMapper riskPointDeviceMapper,
                                                  RiskPointDevicePendingBindingMapper pendingBindingMapper,
                                                  RiskPointDevicePendingPromotionMapper pendingPromotionMapper) {
        this(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null
        );
    }

    @Autowired
    public RiskPointBindingMaintenanceServiceImpl(RiskPointService riskPointService,
                                                  RiskPointDeviceMapper riskPointDeviceMapper,
                                                  RiskPointDevicePendingBindingMapper pendingBindingMapper,
                                                  RiskPointDevicePendingPromotionMapper pendingPromotionMapper,
                                                  GovernanceApprovalPolicyResolver governanceApprovalPolicyResolver,
                                                  @Lazy GovernanceApprovalService governanceApprovalService,
                                                  GovernanceWorkItemService governanceWorkItemService) {
        this.riskPointService = riskPointService;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.pendingBindingMapper = pendingBindingMapper;
        this.pendingPromotionMapper = pendingPromotionMapper;
        this.governanceApprovalPolicyResolver = governanceApprovalPolicyResolver;
        this.governanceApprovalService = governanceApprovalService;
        this.governanceWorkItemService = governanceWorkItemService;
    }

    @Override
    public List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId) {
        if (riskPointIds == null || riskPointIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedRiskPointIds = riskPointIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedRiskPointIds.isEmpty()) {
            return List.of();
        }
        for (Long riskPointId : normalizedRiskPointIds) {
            riskPointService.getById(riskPointId, currentUserId);
        }
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .in(RiskPointDevice::getRiskPointId, normalizedRiskPointIds));
        Map<Long, Set<Long>> distinctDeviceIdsByRiskPointId = new LinkedHashMap<>();
        Map<Long, Integer> metricCountByRiskPointId = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            Long riskPointId = binding.getRiskPointId();
            distinctDeviceIdsByRiskPointId.computeIfAbsent(riskPointId, key -> new LinkedHashSet<>());
            if (binding.getDeviceId() != null) {
                distinctDeviceIdsByRiskPointId.get(riskPointId).add(binding.getDeviceId());
            }
            metricCountByRiskPointId.put(riskPointId, metricCountByRiskPointId.getOrDefault(riskPointId, 0) + 1);
        }

        List<RiskPointDevicePendingBinding> pendingRows = pendingBindingMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingBinding>()
                .eq(RiskPointDevicePendingBinding::getDeleted, 0)
                .in(RiskPointDevicePendingBinding::getRiskPointId, normalizedRiskPointIds)
                .in(RiskPointDevicePendingBinding::getResolutionStatus, List.of(STATUS_PENDING_METRIC_GOVERNANCE, STATUS_PARTIALLY_PROMOTED)));
        Map<Long, Integer> pendingCountByRiskPointId = new LinkedHashMap<>();
        for (RiskPointDevicePendingBinding pending : pendingRows) {
            if (!STATUS_PENDING_METRIC_GOVERNANCE.equals(pending.getResolutionStatus())
                    && !STATUS_PARTIALLY_PROMOTED.equals(pending.getResolutionStatus())) {
                continue;
            }
            Long riskPointId = pending.getRiskPointId();
            pendingCountByRiskPointId.put(riskPointId, pendingCountByRiskPointId.getOrDefault(riskPointId, 0) + 1);
        }

        List<RiskPointBindingSummaryVO> result = new ArrayList<>(normalizedRiskPointIds.size());
        for (Long riskPointId : normalizedRiskPointIds) {
            RiskPointBindingSummaryVO summary = new RiskPointBindingSummaryVO();
            summary.setRiskPointId(riskPointId);
            summary.setBoundDeviceCount(distinctDeviceIdsByRiskPointId.getOrDefault(riskPointId, Set.of()).size());
            summary.setBoundMetricCount(metricCountByRiskPointId.getOrDefault(riskPointId, 0));
            summary.setPendingBindingCount(pendingCountByRiskPointId.getOrDefault(riskPointId, 0));
            result.add(summary);
        }
        return result;
    }

    @Override
    public List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId) {
        riskPointService.getById(riskPointId, currentUserId);
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .eq(RiskPointDevice::getRiskPointId, riskPointId)
                .orderByAsc(RiskPointDevice::getDeviceCode)
                .orderByAsc(RiskPointDevice::getMetricIdentifier));
        if (bindings.isEmpty()) {
            return List.of();
        }
        bindings = new ArrayList<>(bindings);
        bindings.sort(Comparator
                .comparing(RiskPointDevice::getDeviceCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(RiskPointDevice::getMetricIdentifier, Comparator.nullsLast(String::compareTo)));

        Set<Long> bindingIds = bindings.stream()
                .map(RiskPointDevice::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> promotedBindingIds = new LinkedHashSet<>();
        if (!bindingIds.isEmpty()) {
            List<RiskPointDevicePendingPromotion> promotionRows = pendingPromotionMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingPromotion>()
                    .eq(RiskPointDevicePendingPromotion::getDeleted, 0)
                    .in(RiskPointDevicePendingPromotion::getRiskPointDeviceId, bindingIds));
            for (RiskPointDevicePendingPromotion promotion : promotionRows) {
                if (promotion.getRiskPointDeviceId() != null
                        && STATUS_PROMOTION_SUCCESS.equals(promotion.getPromotionStatus())) {
                    promotedBindingIds.add(promotion.getRiskPointDeviceId());
                }
            }
        }

        Map<Long, RiskPointBindingDeviceGroupVO> groups = new LinkedHashMap<>();
        for (RiskPointDevice binding : bindings) {
            Long deviceId = binding.getDeviceId();
            RiskPointBindingDeviceGroupVO group = groups.computeIfAbsent(deviceId, key -> {
                RiskPointBindingDeviceGroupVO value = new RiskPointBindingDeviceGroupVO();
                value.setDeviceId(binding.getDeviceId());
                value.setDeviceCode(binding.getDeviceCode());
                value.setDeviceName(binding.getDeviceName());
                value.setMetrics(new ArrayList<>());
                return value;
            });

            RiskPointBindingMetricVO metric = new RiskPointBindingMetricVO();
            metric.setBindingId(binding.getId());
            metric.setRiskMetricId(binding.getRiskMetricId());
            metric.setMetricIdentifier(binding.getMetricIdentifier());
            metric.setMetricName(binding.getMetricName());
            metric.setBindingSource(promotedBindingIds.contains(binding.getId()) ? SOURCE_PENDING_PROMOTION : SOURCE_MANUAL);
            metric.setCreateTime(binding.getCreateTime());
            group.getMetrics().add(metric);
        }

        List<RiskPointBindingDeviceGroupVO> result = new ArrayList<>(groups.values());
        for (RiskPointBindingDeviceGroupVO group : result) {
            group.setMetricCount(group.getMetrics() == null ? 0 : group.getMetrics().size());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovernanceSubmissionResultVO submitBindDevice(RiskPointDevice riskPointDevice, Long currentUserId) {
        if (riskPointDevice == null) {
            throw new BizException("风险点绑定请求不能为空");
        }
        Long subjectId = IdWorker.getId();
        String snapshotJson = writeBindSnapshot(riskPointDevice);
        Long approverUserId = resolveOptionalApproverUserId(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                currentUserId
        );
        if (approverUserId == null) {
            Long workItemId = openWorkItem(
                    RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                    subjectId,
                    riskPointDevice.getDeviceCode(),
                    riskPointDevice.getRiskMetricId(),
                    snapshotJson,
                    EXECUTION_STATUS_IN_PROGRESS,
                    currentUserId
            );
            bindDevice(riskPointDevice, currentUserId);
            resolveWorkItem(RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE, subjectId, currentUserId);
            return GovernanceSubmissionResultVO.directApplied(workItemId);
        }
        Long workItemId = openWorkItem(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                subjectId,
                riskPointDevice.getDeviceCode(),
                riskPointDevice.getRiskMetricId(),
                snapshotJson,
                EXECUTION_STATUS_PENDING_APPROVAL,
                currentUserId
        );
        Long approvalOrderId = requireGovernanceApprovalService().submitAction(new GovernanceApprovalActionCommand(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                "risk point bind device",
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                subjectId,
                workItemId,
                currentUserId,
                approverUserId,
                RiskPointGovernanceApprovalExecutor.writeBindPayload(riskPointDevice),
                null
        ));
        return GovernanceSubmissionResultVO.pendingApproval(workItemId, approvalOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GovernanceSubmissionResultVO submitUnbindDevice(Long riskPointId, Long deviceId, Long currentUserId) {
        Long subjectId = IdWorker.getId();
        String snapshotJson = writeUnbindSnapshot(riskPointId, deviceId);
        Long approverUserId = resolveOptionalApproverUserId(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE,
                currentUserId
        );
        if (approverUserId == null) {
            Long workItemId = openWorkItem(
                    RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE,
                    subjectId,
                    null,
                    null,
                    snapshotJson,
                    EXECUTION_STATUS_IN_PROGRESS,
                    currentUserId
            );
            unbindDevice(riskPointId, deviceId, currentUserId);
            resolveWorkItem(RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE, subjectId, currentUserId);
            return GovernanceSubmissionResultVO.directApplied(workItemId);
        }
        Long workItemId = openWorkItem(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE,
                subjectId,
                null,
                null,
                snapshotJson,
                EXECUTION_STATUS_PENDING_APPROVAL,
                currentUserId
        );
        Long approvalOrderId = requireGovernanceApprovalService().submitAction(new GovernanceApprovalActionCommand(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE,
                "risk point unbind device",
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_UNBIND_DEVICE,
                subjectId,
                workItemId,
                currentUserId,
                approverUserId,
                RiskPointGovernanceApprovalExecutor.writeUnbindPayload(riskPointId, deviceId, null, null),
                null
        ));
        return GovernanceSubmissionResultVO.pendingApproval(workItemId, approvalOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskPointDevice bindDevice(RiskPointDevice riskPointDevice, Long currentUserId) {
        return riskPointService.bindDeviceAndReturn(riskPointDevice, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindDevice(Long riskPointId, Long deviceId, Long currentUserId) {
        riskPointService.unbindDevice(riskPointId, deviceId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBinding(Long bindingId, Long currentUserId) {
        RiskPointDevice binding = requireBinding(bindingId);
        riskPointService.getById(binding.getRiskPointId(), currentUserId);
        int deletedRows = riskPointDeviceMapper.deleteById(bindingId);
        if (deletedRows <= 0) {
            throw new BizException("删除绑定失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskPointBindingMetricVO replaceBindingMetric(Long bindingId,
                                                         RiskPointBindingReplaceRequest request,
                                                         Long currentUserId) {
        RiskPointDevice oldBinding = requireBinding(bindingId);
        riskPointService.getById(oldBinding.getRiskPointId(), currentUserId);
        String newMetricIdentifier = normalizeRequiredMetricIdentifier(request == null ? null : request.getMetricIdentifier());
        String oldMetricIdentifier = normalizeMetricIdentifier(oldBinding.getMetricIdentifier());
        if (Objects.equals(newMetricIdentifier, oldMetricIdentifier)) {
            throw new BizException("替换测点不能与原测点相同");
        }

        RiskPointDevice duplicate = riskPointDeviceMapper.selectOne(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getRiskPointId, oldBinding.getRiskPointId())
                .eq(RiskPointDevice::getDeviceId, oldBinding.getDeviceId())
                .eq(RiskPointDevice::getMetricIdentifier, newMetricIdentifier)
                .eq(RiskPointDevice::getDeleted, 0)
                .ne(RiskPointDevice::getId, oldBinding.getId()));
        if (duplicate != null) {
            throw new BizException("目标测点已存在绑定记录");
        }

        RiskPointDevice replacement = new RiskPointDevice();
        replacement.setRiskPointId(oldBinding.getRiskPointId());
        replacement.setDeviceId(oldBinding.getDeviceId());
        replacement.setDeviceCode(oldBinding.getDeviceCode());
        replacement.setDeviceName(oldBinding.getDeviceName());
        replacement.setRiskMetricId(request == null ? null : request.getRiskMetricId());
        replacement.setMetricIdentifier(newMetricIdentifier);
        replacement.setMetricName(resolveReplacementMetricName(
                request == null ? null : request.getMetricName(),
                newMetricIdentifier
        ));
        RiskPointDevice saved = riskPointService.bindDeviceAndReturn(replacement, currentUserId);

        int deletedRows = riskPointDeviceMapper.deleteById(bindingId);
        if (deletedRows <= 0) {
            throw new BizException("旧绑定删除失败，替换终止");
        }
        return toMetric(saved, SOURCE_MANUAL);
    }

    private RiskPointDevice requireBinding(Long bindingId) {
        if (bindingId == null) {
            throw new BizException("绑定ID不能为空");
        }
        RiskPointDevice binding = riskPointDeviceMapper.selectById(bindingId);
        if (binding == null || isDeleted(binding.getDeleted())) {
            throw new BizException("绑定记录不存在");
        }
        if (binding.getRiskPointId() == null) {
            throw new BizException("绑定记录缺少风险点ID");
        }
        if (binding.getDeviceId() == null) {
            throw new BizException("绑定记录缺少设备ID");
        }
        return binding;
    }

    private String normalizeRequiredMetricIdentifier(String metricIdentifier) {
        String normalized = normalizeMetricIdentifier(metricIdentifier);
        if (normalized == null) {
            throw new BizException("测点标识不能为空");
        }
        return normalized;
    }

    private String normalizeMetricIdentifier(String metricIdentifier) {
        return StringUtils.hasText(metricIdentifier) ? metricIdentifier.trim() : null;
    }

    private String normalizeMetricName(String metricName) {
        return StringUtils.hasText(metricName) ? metricName.trim() : null;
    }

    private String resolveReplacementMetricName(String metricName, String metricIdentifier) {
        String normalizedMetricName = normalizeMetricName(metricName);
        return normalizedMetricName == null ? metricIdentifier : normalizedMetricName;
    }

    private Long openWorkItem(String actionCode,
                              Long subjectId,
                              String deviceCode,
                              Long riskMetricId,
                              String snapshotJson,
                              String executionStatus,
                              Long currentUserId) {
        if (governanceWorkItemService == null) {
            return null;
        }
        return governanceWorkItemService.openOrRefreshAndGetId(new GovernanceWorkItemCommand(
                WORK_ITEM_CODE_RISK_BINDING,
                actionCode,
                subjectId,
                null,
                riskMetricId,
                null,
                null,
                null,
                normalize(deviceCode),
                null,
                null,
                "RISK_POINT",
                null,
                snapshotJson,
                TASK_CATEGORY_RISK_BINDING,
                DOMAIN_CODE_ALARM,
                actionCode,
                executionStatus,
                null,
                null,
                null,
                null,
                "P2",
                currentUserId
        ));
    }

    private void resolveWorkItem(String actionCode, Long subjectId, Long currentUserId) {
        if (governanceWorkItemService == null) {
            return;
        }
        governanceWorkItemService.resolve(
                WORK_ITEM_CODE_RISK_BINDING,
                actionCode,
                subjectId,
                currentUserId,
                COMMENT_DIRECT_APPLIED
        );
    }

    private Long resolveOptionalApproverUserId(String actionCode, Long currentUserId) {
        if (governanceApprovalPolicyResolver == null) {
            return null;
        }
        return governanceApprovalPolicyResolver.resolveOptionalApproverUserId(actionCode, currentUserId);
    }

    private GovernanceApprovalService requireGovernanceApprovalService() {
        if (governanceApprovalService == null) {
            throw new BizException("治理审批服务未配置");
        }
        return governanceApprovalService;
    }

    private String writeBindSnapshot(RiskPointDevice riskPointDevice) {
        ObjectNode root = objectMapper.createObjectNode();
        writeNullableLong(root, "riskPointId", riskPointDevice == null ? null : riskPointDevice.getRiskPointId());
        writeNullableLong(root, "deviceId", riskPointDevice == null ? null : riskPointDevice.getDeviceId());
        writeNullableLong(root, "riskMetricId", riskPointDevice == null ? null : riskPointDevice.getRiskMetricId());
        writeNullableText(root, "deviceCode", riskPointDevice == null ? null : riskPointDevice.getDeviceCode());
        writeNullableText(root, "deviceName", riskPointDevice == null ? null : riskPointDevice.getDeviceName());
        writeNullableText(root, "metricIdentifier", riskPointDevice == null ? null : riskPointDevice.getMetricIdentifier());
        writeNullableText(root, "metricName", riskPointDevice == null ? null : riskPointDevice.getMetricName());
        return root.toString();
    }

    private String writeUnbindSnapshot(Long riskPointId, Long deviceId) {
        ObjectNode root = objectMapper.createObjectNode();
        writeNullableLong(root, "riskPointId", riskPointId);
        writeNullableLong(root, "deviceId", deviceId);
        return root.toString();
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted != 0;
    }

    private RiskPointBindingMetricVO toMetric(RiskPointDevice binding, String bindingSource) {
        if (binding == null) {
            throw new BizException("绑定保存失败");
        }
        RiskPointBindingMetricVO metric = new RiskPointBindingMetricVO();
        metric.setBindingId(binding.getId());
        metric.setRiskMetricId(binding.getRiskMetricId());
        metric.setMetricIdentifier(binding.getMetricIdentifier());
        metric.setMetricName(binding.getMetricName());
        metric.setBindingSource(bindingSource);
        metric.setCreateTime(binding.getCreateTime());
        return metric;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void writeNullableLong(ObjectNode node, String fieldName, Long value) {
        if (node != null && value != null) {
            node.put(fieldName, value);
        }
    }

    private void writeNullableText(ObjectNode node, String fieldName, String value) {
        if (node != null && StringUtils.hasText(value)) {
            node.put(fieldName, value.trim());
        }
    }
}
