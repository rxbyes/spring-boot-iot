package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 厂商字段映射规则运行时解析服务实现。
 */
@Service
public class VendorMetricMappingRuntimeServiceImpl implements VendorMetricMappingRuntimeService {

    private static final String SCOPE_TYPE_PRODUCT = "PRODUCT";
    private static final Set<String> DISABLED_STATUSES = Set.of("INACTIVE", "DISABLED", "RETIRED");

    private final VendorMetricMappingRuleMapper mapper;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();

    public VendorMetricMappingRuntimeServiceImpl(VendorMetricMappingRuleMapper mapper,
                                                 NormativeMetricDefinitionService normativeMetricDefinitionService) {
        this.mapper = mapper;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
    }

    @Override
    public MappingResolution resolveForGovernance(Product product, String rawIdentifier, String logicalChannelCode) {
        return resolveInternal(product, resolveProtocolCode(product), rawIdentifier, logicalChannelCode, true);
    }

    @Override
    public MappingResolution resolveForRuntime(Product product,
                                               DeviceUpMessage upMessage,
                                               String rawIdentifier,
                                               String logicalChannelCode) {
        return resolveInternal(
                product,
                normalizeLower(upMessage == null ? null : upMessage.getProtocolCode()),
                rawIdentifier,
                logicalChannelCode,
                false
        );
    }

    @Override
    public String normalizeApplyIdentifier(Product product, String identifier) {
        String normalizedIdentifier = normalizeLower(identifier);
        if (normalizedIdentifier == null) {
            return null;
        }
        MappingResolution resolution = resolveInternal(product, resolveProtocolCode(product), normalizedIdentifier, null, true);
        return resolution == null ? normalizedIdentifier : resolution.targetNormativeIdentifier();
    }

    private MappingResolution resolveInternal(Product product,
                                              String protocolCode,
                                              String rawIdentifier,
                                              String logicalChannelCode,
                                              boolean strict) {
        Long productId = product == null ? null : product.getId();
        String normalizedRawIdentifier = normalizeLower(rawIdentifier);
        if (productId == null || normalizedRawIdentifier == null) {
            return null;
        }
        String scenarioCode = normalizeLower(normativeMatcher.resolveScenarioCode(product));
        String deviceFamily = normalizeLower(product == null ? null : product.getManufacturer());
        String normalizedProtocolCode = normalizeLower(protocolCode);
        String normalizedLogicalChannelCode = normalizeUpper(logicalChannelCode);
        List<ResolvedRuleCandidate> candidates = mapper.selectList(new LambdaQueryWrapper<VendorMetricMappingRule>()
                        .eq(VendorMetricMappingRule::getDeleted, 0)
                        .eq(VendorMetricMappingRule::getProductId, productId)
                        .eq(VendorMetricMappingRule::getScopeType, SCOPE_TYPE_PRODUCT)
                        .eq(VendorMetricMappingRule::getRawIdentifier, normalizedRawIdentifier))
                .stream()
                .filter(this::isUsableRule)
                .filter(rule -> matchesOptional(rule.getScenarioCode(), scenarioCode))
                .filter(rule -> matchesOptional(rule.getProtocolCode(), normalizedProtocolCode))
                .filter(rule -> matchesOptionalUpper(rule.getLogicalChannelCode(), normalizedLogicalChannelCode))
                .filter(rule -> matchesOptional(rule.getDeviceFamily(), deviceFamily))
                .map(rule -> new ResolvedRuleCandidate(
                        rule,
                        canonicalizeTargetIdentifier(product, rule.getTargetNormativeIdentifier()),
                        specificity(rule)
                ))
                .filter(candidate -> StringUtils.hasText(candidate.targetNormativeIdentifier()))
                .sorted(Comparator
                        .comparingInt(ResolvedRuleCandidate::specificity)
                        .reversed()
                        .thenComparing(candidate -> candidate.rule().getId(), Comparator.nullsLast(Long::compareTo)))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        int bestSpecificity = candidates.get(0).specificity();
        List<ResolvedRuleCandidate> bestCandidates = candidates.stream()
                .filter(candidate -> candidate.specificity() == bestSpecificity)
                .toList();
        long distinctTargetCount = bestCandidates.stream()
                .map(candidate -> normalizeLower(candidate.targetNormativeIdentifier()))
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (distinctTargetCount > 1) {
            if (strict) {
                throw new BizException("厂商字段映射规则命中多个目标规范字段，请先清理冲突规则: " + normalizedRawIdentifier);
            }
            return null;
        }
        ResolvedRuleCandidate winner = bestCandidates.get(0);
        return new MappingResolution(
                winner.rule().getId(),
                winner.targetNormativeIdentifier(),
                normalizedRawIdentifier,
                normalizeUpper(winner.rule().getLogicalChannelCode())
        );
    }

    private boolean isUsableRule(VendorMetricMappingRule rule) {
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            return false;
        }
        if (!SCOPE_TYPE_PRODUCT.equalsIgnoreCase(normalizeText(rule.getScopeType()))) {
            return false;
        }
        String status = normalizeUpper(rule.getStatus());
        return status == null || !DISABLED_STATUSES.contains(status);
    }

    private boolean matchesOptional(String expectedValue, String actualValue) {
        String normalizedExpectedValue = normalizeLower(expectedValue);
        if (normalizedExpectedValue == null) {
            return true;
        }
        return normalizedExpectedValue.equals(normalizeLower(actualValue));
    }

    private boolean matchesOptionalUpper(String expectedValue, String actualValue) {
        String normalizedExpectedValue = normalizeUpper(expectedValue);
        if (normalizedExpectedValue == null) {
            return true;
        }
        return normalizedExpectedValue.equals(normalizeUpper(actualValue));
    }

    private int specificity(VendorMetricMappingRule rule) {
        int score = 0;
        if (StringUtils.hasText(rule.getScenarioCode())) {
            score++;
        }
        if (StringUtils.hasText(rule.getProtocolCode())) {
            score++;
        }
        if (StringUtils.hasText(rule.getLogicalChannelCode())) {
            score++;
        }
        if (StringUtils.hasText(rule.getDeviceFamily())) {
            score++;
        }
        return score;
    }

    private String canonicalizeTargetIdentifier(Product product, String targetIdentifier) {
        String normalizedTargetIdentifier = normalizeText(targetIdentifier);
        if (normalizedTargetIdentifier == null) {
            return null;
        }
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode) || normativeMetricDefinitionService == null) {
            return normalizedTargetIdentifier;
        }
        List<NormativeMetricDefinition> definitions = normativeMetricDefinitionService.listByScenario(scenarioCode);
        if (definitions == null || definitions.isEmpty()) {
            return normalizedTargetIdentifier;
        }
        return definitions.stream()
                .map(NormativeMetricDefinition::getIdentifier)
                .filter(StringUtils::hasText)
                .filter(identifier -> identifier.equalsIgnoreCase(normalizedTargetIdentifier))
                .findFirst()
                .orElse(normalizedTargetIdentifier);
    }

    private String resolveProtocolCode(Product product) {
        return normalizeLower(product == null ? null : product.getProtocolCode());
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private record ResolvedRuleCandidate(VendorMetricMappingRule rule,
                                         String targetNormativeIdentifier,
                                         int specificity) {
    }
}
