package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.dto.RiskPointPendingIgnoreRequest;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionMetricDTO;
import com.ghlzm.iot.alarm.dto.RiskPointPendingPromotionRequest;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointPendingPromotionService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionItemVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionResultVO;
import com.ghlzm.iot.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险点待治理转正写侧实现。
 */
@Service
public class RiskPointPendingPromotionServiceImpl implements RiskPointPendingPromotionService {

    private static final String STATUS_PENDING_METRIC_GOVERNANCE = "PENDING_METRIC_GOVERNANCE";
    private static final String STATUS_PARTIALLY_PROMOTED = "PARTIALLY_PROMOTED";
    private static final String STATUS_PROMOTED = "PROMOTED";
    private static final String STATUS_IGNORED = "IGNORED";
    private static final String ITEM_SUCCESS = "SUCCESS";
    private static final String ITEM_DUPLICATE_SKIPPED = "DUPLICATE_SKIPPED";
    private static final String ITEM_INVALID_METRIC = "INVALID_METRIC";

    private final RiskPointDevicePendingBindingMapper pendingBindingMapper;
    private final RiskPointDevicePendingPromotionMapper promotionMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final RiskPointPendingRecommendationService recommendationService;
    private final RiskPointService riskPointService;
    private final RiskPointPendingMetricGovernanceRules metricGovernanceRules = new RiskPointPendingMetricGovernanceRules();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskPointPendingPromotionServiceImpl(RiskPointDevicePendingBindingMapper pendingBindingMapper,
                                                RiskPointDevicePendingPromotionMapper promotionMapper,
                                                RiskPointDeviceMapper riskPointDeviceMapper,
                                                RiskPointPendingRecommendationService recommendationService,
                                                RiskPointService riskPointService) {
        this.pendingBindingMapper = pendingBindingMapper;
        this.promotionMapper = promotionMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.recommendationService = recommendationService;
        this.riskPointService = riskPointService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskPointPendingPromotionResultVO promote(Long pendingId, RiskPointPendingPromotionRequest request, Long currentUserId) {
        RiskPointDevicePendingBinding pending = requirePromotablePending(pendingId);
        ensureRequestHasMetrics(request);
        riskPointService.getById(pending.getRiskPointId(), currentUserId);
        String promotionNote = normalize(request.getPromotionNote());

        RiskPointPendingCandidateBundleVO candidateBundle = recommendationService.getCandidates(pendingId, currentUserId);
        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = buildCandidateMap(candidateBundle);
        List<RiskPointPendingPromotionItemVO> items = new ArrayList<>();
        Long latestBindingId = pending.getPromotedBindingId();
        Date latestPromotionTime = pending.getPromotedTime();

        for (RiskPointPendingPromotionMetricDTO metric : request.getMetrics()) {
            String requestMetricIdentifier = normalize(metric == null ? null : metric.getMetricIdentifier());
            String metricIdentifier = metricGovernanceRules.normalizePromotableMetricIdentifier(
                    pending,
                    candidateBundle == null ? null : candidateBundle.getDeviceName(),
                    candidateBundle == null ? null : candidateBundle.getCandidates(),
                    requestMetricIdentifier
            );
            String metricName = normalize(metric == null ? null : metric.getMetricName());
            RiskPointPendingMetricCandidateVO candidate = metricIdentifier == null ? null : candidateMap.get(metricIdentifier);
            if (candidate == null) {
                items.add(insertHistoryAndBuildItem(
                        pending,
                        requestMetricIdentifier,
                        metricName,
                        null,
                        ITEM_INVALID_METRIC,
                        null,
                        currentUserId,
                        promotionNote
                ));
                continue;
            }

            RiskPointDevice existing = riskPointDeviceMapper.selectOne(new LambdaQueryWrapper<RiskPointDevice>()
                    .eq(RiskPointDevice::getRiskPointId, pending.getRiskPointId())
                    .eq(RiskPointDevice::getDeviceId, pending.getDeviceId())
                    .eq(RiskPointDevice::getMetricIdentifier, metricIdentifier)
                    .eq(RiskPointDevice::getDeleted, 0));
            if (existing != null) {
                items.add(insertHistoryAndBuildItem(
                        pending,
                        metricIdentifier,
                        resolveMetricName(metricName, candidate),
                        candidate,
                        ITEM_DUPLICATE_SKIPPED,
                        existing.getId(),
                        currentUserId,
                        promotionNote
                ));
                latestBindingId = existing.getId();
                latestPromotionTime = new Date();
                continue;
            }

            RiskPointDevice binding = new RiskPointDevice();
            binding.setRiskPointId(pending.getRiskPointId());
            binding.setDeviceId(pending.getDeviceId());
            binding.setDeviceCode(pending.getDeviceCode());
            binding.setDeviceName(pending.getDeviceName());
            binding.setMetricIdentifier(metricIdentifier);
            binding.setMetricName(resolveMetricName(metricName, candidate));
            RiskPointDevice saved = riskPointService.bindDeviceAndReturn(binding, currentUserId);
            items.add(insertHistoryAndBuildItem(
                    pending,
                    metricIdentifier,
                    binding.getMetricName(),
                    candidate,
                    ITEM_SUCCESS,
                    saved.getId(),
                    currentUserId,
                    promotionNote
            ));
            latestBindingId = saved.getId();
            latestPromotionTime = new Date();
        }

        boolean hasSuccessLikeItem = items.stream().anyMatch(item ->
                ITEM_SUCCESS.equals(item.getPromotionStatus()) || ITEM_DUPLICATE_SKIPPED.equals(item.getPromotionStatus())
        );
        String nextStatus = resolveNextPendingStatus(pending.getResolutionStatus(), request.getCompletePending(), hasSuccessLikeItem);
        pending.setResolutionStatus(nextStatus);
        pending.setResolutionNote(promotionNote);
        pending.setPromotedBindingId(latestBindingId);
        pending.setPromotedTime(latestPromotionTime);
        pending.setUpdateBy(currentUserId);
        pending.setUpdateTime(new Date());
        pendingBindingMapper.updateById(pending);

        RiskPointPendingPromotionResultVO result = new RiskPointPendingPromotionResultVO();
        result.setPendingId(pending.getId());
        result.setPendingStatus(nextStatus);
        result.setItems(items);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignore(Long pendingId, RiskPointPendingIgnoreRequest request, Long currentUserId) {
        RiskPointDevicePendingBinding pending = requireIgnorablePending(pendingId);
        riskPointService.getById(pending.getRiskPointId(), currentUserId);
        pending.setResolutionStatus(STATUS_IGNORED);
        pending.setResolutionNote(request == null ? null : normalize(request.getIgnoreNote()));
        pending.setUpdateBy(currentUserId);
        pending.setUpdateTime(new Date());
        pendingBindingMapper.updateById(pending);
    }

    private RiskPointDevicePendingBinding requirePromotablePending(Long pendingId) {
        RiskPointDevicePendingBinding pending = pendingBindingMapper.selectByIdForUpdate(pendingId);
        if (pending == null || isDeleted(pending.getDeleted())) {
            throw new BizException("待治理记录不存在");
        }
        if (!STATUS_PENDING_METRIC_GOVERNANCE.equals(pending.getResolutionStatus())
                && !STATUS_PARTIALLY_PROMOTED.equals(pending.getResolutionStatus())) {
            throw new BizException("当前治理状态不允许转正");
        }
        if (pending.getRiskPointId() == null || pending.getDeviceId() == null) {
            throw new BizException("待治理记录缺少风险点或设备，禁止转正");
        }
        return pending;
    }

    private RiskPointDevicePendingBinding requireIgnorablePending(Long pendingId) {
        RiskPointDevicePendingBinding pending = pendingBindingMapper.selectByIdForUpdate(pendingId);
        if (pending == null || isDeleted(pending.getDeleted())) {
            throw new BizException("待治理记录不存在");
        }
        if (!STATUS_PENDING_METRIC_GOVERNANCE.equals(pending.getResolutionStatus())
                && !STATUS_PARTIALLY_PROMOTED.equals(pending.getResolutionStatus())) {
            throw new BizException("当前治理状态不允许忽略");
        }
        if (pending.getRiskPointId() == null) {
            throw new BizException("待治理记录缺少风险点");
        }
        return pending;
    }

    private void ensureRequestHasMetrics(RiskPointPendingPromotionRequest request) {
        if (request == null || request.getMetrics() == null || request.getMetrics().isEmpty()) {
            throw new BizException("至少选择一个测点");
        }
    }

    private Map<String, RiskPointPendingMetricCandidateVO> buildCandidateMap(RiskPointPendingCandidateBundleVO bundle) {
        List<RiskPointPendingMetricCandidateVO> candidates = bundle == null || bundle.getCandidates() == null
                ? Collections.emptyList()
                : bundle.getCandidates();
        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = new LinkedHashMap<>();
        for (RiskPointPendingMetricCandidateVO candidate : candidates) {
            String identifier = normalize(candidate == null ? null : candidate.getMetricIdentifier());
            if (identifier != null) {
                candidateMap.put(identifier, candidate);
            }
        }
        return candidateMap;
    }

    private RiskPointPendingPromotionItemVO insertHistoryAndBuildItem(RiskPointDevicePendingBinding pending,
                                                                      String metricIdentifier,
                                                                      String metricName,
                                                                      RiskPointPendingMetricCandidateVO candidate,
                                                                      String status,
                                                                      Long bindingId,
                                                                      Long currentUserId,
                                                                      String promotionNote) {
        RiskPointDevicePendingPromotion row = new RiskPointDevicePendingPromotion();
        row.setPendingBindingId(pending.getId());
        row.setRiskPointDeviceId(bindingId);
        row.setRiskPointId(pending.getRiskPointId());
        row.setDeviceId(pending.getDeviceId());
        row.setDeviceCode(pending.getDeviceCode());
        row.setDeviceName(pending.getDeviceName());
        row.setMetricIdentifier(metricIdentifier);
        row.setMetricName(metricName);
        row.setPromotionStatus(status);
        row.setRecommendationLevel(candidate == null ? null : candidate.getRecommendationLevel());
        row.setRecommendationScore(candidate == null ? null : candidate.getRecommendationScore());
        row.setEvidenceSnapshotJson(writeEvidenceSnapshot(candidate));
        row.setPromotionNote(promotionNote);
        row.setOperatorId(currentUserId);
        row.setTenantId(pending.getTenantId());
        row.setCreateBy(currentUserId);
        row.setUpdateBy(currentUserId);
        row.setDeleted(0);
        promotionMapper.insert(row);

        RiskPointPendingPromotionItemVO item = new RiskPointPendingPromotionItemVO();
        item.setMetricIdentifier(metricIdentifier);
        item.setMetricName(metricName);
        item.setPromotionStatus(status);
        item.setBindingId(bindingId);
        return item;
    }

    private String writeEvidenceSnapshot(RiskPointPendingMetricCandidateVO candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("metricIdentifier", candidate.getMetricIdentifier());
            root.put("metricName", candidate.getMetricName());
            root.put("recommendationLevel", candidate.getRecommendationLevel());
            if (candidate.getRecommendationScore() != null) {
                root.put("recommendationScore", candidate.getRecommendationScore());
            }
            if (candidate.getLastSeenTime() != null) {
                root.put("lastSeenTime", candidate.getLastSeenTime().toString());
            }
            if (candidate.getSampleValue() != null) {
                root.put("sampleValue", candidate.getSampleValue());
            }
            if (candidate.getSeenCount() != null) {
                root.put("seenCount", candidate.getSeenCount());
            }
            ArrayNode sources = root.putArray("evidenceSources");
            if (candidate.getEvidenceSources() != null) {
                for (String source : candidate.getEvidenceSources()) {
                    sources.add(source);
                }
            }
            return root.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveNextPendingStatus(String currentStatus, Boolean completePending, boolean hasSuccessLikeItem) {
        if (!hasSuccessLikeItem) {
            return currentStatus;
        }
        return Boolean.TRUE.equals(completePending) ? STATUS_PROMOTED : STATUS_PARTIALLY_PROMOTED;
    }

    private String resolveMetricName(String requestMetricName, RiskPointPendingMetricCandidateVO candidate) {
        String normalizedRequestName = normalize(requestMetricName);
        if (normalizedRequestName != null) {
            return normalizedRequestName;
        }
        return candidate == null ? null : normalize(candidate.getMetricName());
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted != 0;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
