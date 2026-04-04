package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final RiskPointService riskPointService;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RiskPointDevicePendingBindingMapper pendingBindingMapper;
    private final RiskPointDevicePendingPromotionMapper pendingPromotionMapper;

    public RiskPointBindingMaintenanceServiceImpl(RiskPointService riskPointService,
                                                  RiskPointDeviceMapper riskPointDeviceMapper,
                                                  RiskPointDevicePendingBindingMapper pendingBindingMapper,
                                                  RiskPointDevicePendingPromotionMapper pendingPromotionMapper) {
        this.riskPointService = riskPointService;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.pendingBindingMapper = pendingBindingMapper;
        this.pendingPromotionMapper = pendingPromotionMapper;
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

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted != 0;
    }

    private RiskPointBindingMetricVO toMetric(RiskPointDevice binding, String bindingSource) {
        if (binding == null) {
            throw new BizException("绑定保存失败");
        }
        RiskPointBindingMetricVO metric = new RiskPointBindingMetricVO();
        metric.setBindingId(binding.getId());
        metric.setMetricIdentifier(binding.getMetricIdentifier());
        metric.setMetricName(binding.getMetricName());
        metric.setBindingSource(bindingSource);
        metric.setCreateTime(binding.getCreateTime());
        return metric;
    }
}
