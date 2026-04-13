package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 厂商字段映射规则运行时解析服务实现。
 */
@Service
public class VendorMetricMappingRuntimeServiceImpl implements VendorMetricMappingRuntimeService {

    private static final String SCOPE_TYPE_PRODUCT = "PRODUCT";
    private static final String SCOPE_TYPE_DEVICE_FAMILY = "DEVICE_FAMILY";
    private static final String SCOPE_TYPE_SCENARIO = "SCENARIO";
    private static final String SCOPE_TYPE_PROTOCOL = "PROTOCOL";
    private static final String SCOPE_TYPE_TENANT_DEFAULT = "TENANT_DEFAULT";
    private static final Set<String> DISABLED_STATUSES = Set.of("INACTIVE", "DISABLED", "RETIRED");
    private static final String PROTOCOL_FAMILY_SELECTOR_PREFIX = "family:";

    private final VendorMetricMappingRuleMapper mapper;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final PublishedProductContractSnapshotService snapshotService;
    private final MetricIdentifierResolver metricIdentifierResolver;
    private final IotProperties iotProperties;
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();

    @Autowired
    public VendorMetricMappingRuntimeServiceImpl(VendorMetricMappingRuleMapper mapper,
                                                 NormativeMetricDefinitionService normativeMetricDefinitionService,
                                                 PublishedProductContractSnapshotService snapshotService,
                                                 MetricIdentifierResolver metricIdentifierResolver,
                                                 IotProperties iotProperties) {
        this.mapper = mapper;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
        this.snapshotService = snapshotService;
        this.metricIdentifierResolver = metricIdentifierResolver;
        this.iotProperties = iotProperties;
    }

    public VendorMetricMappingRuntimeServiceImpl(VendorMetricMappingRuleMapper mapper,
                                                 NormativeMetricDefinitionService normativeMetricDefinitionService,
                                                 PublishedProductContractSnapshotService snapshotService,
                                                 MetricIdentifierResolver metricIdentifierResolver) {
        this(mapper, normativeMetricDefinitionService, snapshotService, metricIdentifierResolver, null);
    }

    public VendorMetricMappingRuntimeServiceImpl(VendorMetricMappingRuleMapper mapper,
                                                 NormativeMetricDefinitionService normativeMetricDefinitionService) {
        this(mapper, normativeMetricDefinitionService, null, null, null);
    }

    @Override
    public MappingResolution resolveForGovernance(Product product, String rawIdentifier, String logicalChannelCode) {
        return resolveInternal(
                product,
                resolveProtocolSelectors(resolveProtocolCode(product), null),
                rawIdentifier,
                logicalChannelCode,
                true
        );
    }

    @Override
    public MappingResolution resolveForRuntime(Product product,
                                               DeviceUpMessage upMessage,
                                               String rawIdentifier,
                                               String logicalChannelCode) {
        MappingResolution snapshotResolution = resolvePublishedSnapshot(product, rawIdentifier, logicalChannelCode);
        if (snapshotResolution != null) {
            return snapshotResolution;
        }
        DeviceUpProtocolMetadata protocolMetadata = upMessage == null ? null : upMessage.getProtocolMetadata();
        return resolveInternal(
                product,
                resolveProtocolSelectors(
                        normalizeLower(upMessage == null ? null : upMessage.getProtocolCode()),
                        protocolMetadata == null ? null : protocolMetadata.getFamilyCodes()
                ),
                rawIdentifier,
                logicalChannelCode,
                false
        );
    }

    @Override
    public String normalizeApplyIdentifier(Product product, String identifier) {
        String sanitizedIdentifier = normalizeText(identifier);
        if (sanitizedIdentifier == null) {
            return null;
        }
        MappingResolution resolution = resolveInternal(
                product,
                resolveProtocolSelectors(resolveProtocolCode(product), null),
                sanitizedIdentifier,
                null,
                true
        );
        return resolution == null ? sanitizedIdentifier : resolution.targetNormativeIdentifier();
    }

    private MappingResolution resolvePublishedSnapshot(Product product,
                                                       String rawIdentifier,
                                                       String logicalChannelCode) {
        if (snapshotService == null || metricIdentifierResolver == null
                || product == null || product.getId() == null || !StringUtils.hasText(rawIdentifier)) {
            return null;
        }
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(product.getId());
        if (snapshot == null) {
            return null;
        }
        MetricIdentifierResolution resolution = metricIdentifierResolver.resolveForRuntime(snapshot, rawIdentifier);
        if (resolution == null || !StringUtils.hasText(resolution.canonicalIdentifier())) {
            return null;
        }
        if (MetricIdentifierResolution.SOURCE_RAW_IDENTIFIER.equals(resolution.source())) {
            return null;
        }
        return new MappingResolution(
                null,
                resolution.canonicalIdentifier(),
                normalizeText(rawIdentifier),
                normalizeUpper(logicalChannelCode)
        );
    }

    private MappingResolution resolveInternal(Product product,
                                              Set<String> protocolSelectors,
                                              String rawIdentifier,
                                              String logicalChannelCode,
                                              boolean strict) {
        Long productId = product == null ? null : product.getId();
        String normalizedRawIdentifier = normalizeLower(rawIdentifier);
        if (productId == null || normalizedRawIdentifier == null) {
            return null;
        }
        String scenarioCode = normalizeLower(normativeMatcher.resolveScenarioCode(product));
        String normalizedLogicalChannelCode = normalizeUpper(logicalChannelCode);
        Set<String> scenarioDeviceFamilies = resolveScenarioDeviceFamilies(scenarioCode);
        List<ResolvedRuleCandidate> candidates = mapper.selectList(new LambdaQueryWrapper<VendorMetricMappingRule>()
                        .eq(VendorMetricMappingRule::getDeleted, 0)
                        .and(wrapper -> wrapper.eq(VendorMetricMappingRule::getProductId, productId)
                                .or(scope -> scope.isNull(VendorMetricMappingRule::getProductId)
                                        .eq(VendorMetricMappingRule::getScopeType, SCOPE_TYPE_TENANT_DEFAULT)))
                        .eq(VendorMetricMappingRule::getRawIdentifier, normalizedRawIdentifier))
                .stream()
                .filter(this::isUsableRule)
                .filter(rule -> scopeMatches(rule, scenarioCode, protocolSelectors, scenarioDeviceFamilies))
                .filter(rule -> matchesOptional(rule.getScenarioCode(), scenarioCode))
                .filter(rule -> matchesOptionalProtocol(rule.getProtocolCode(), protocolSelectors))
                .filter(rule -> matchesOptionalUpper(rule.getLogicalChannelCode(), normalizedLogicalChannelCode))
                .filter(rule -> matchesOptional(rule.getDeviceFamily(), scenarioDeviceFamilies))
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
        String scopeType = normalizeUpper(rule.getScopeType());
        if (!Set.of(
                SCOPE_TYPE_PRODUCT,
                SCOPE_TYPE_DEVICE_FAMILY,
                SCOPE_TYPE_SCENARIO,
                SCOPE_TYPE_PROTOCOL,
                SCOPE_TYPE_TENANT_DEFAULT
        ).contains(scopeType)) {
            return false;
        }
        String status = normalizeUpper(rule.getStatus());
        return status == null || !DISABLED_STATUSES.contains(status);
    }

    private boolean scopeMatches(VendorMetricMappingRule rule,
                                 String scenarioCode,
                                 Set<String> protocolSelectors,
                                 Set<String> scenarioDeviceFamilies) {
        String scopeType = normalizeUpper(rule == null ? null : rule.getScopeType());
        if (scopeType == null) {
            return false;
        }
        return switch (scopeType) {
            case SCOPE_TYPE_PRODUCT -> true;
            case SCOPE_TYPE_DEVICE_FAMILY -> matchesRequired(rule.getDeviceFamily(), scenarioDeviceFamilies);
            case SCOPE_TYPE_SCENARIO -> matchesRequired(rule.getScenarioCode(), scenarioCode);
            case SCOPE_TYPE_PROTOCOL -> matchesRequiredProtocol(rule.getProtocolCode(), protocolSelectors);
            case SCOPE_TYPE_TENANT_DEFAULT -> true;
            default -> false;
        };
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

    private boolean matchesOptional(String expectedValue, Set<String> actualValues) {
        String normalizedExpectedValue = normalizeLower(expectedValue);
        if (normalizedExpectedValue == null) {
            return true;
        }
        return actualValues != null && actualValues.contains(normalizedExpectedValue);
    }

    private boolean matchesOptionalProtocol(String expectedValue, Set<String> protocolSelectors) {
        String normalizedExpectedValue = normalizeProtocolSelector(expectedValue);
        if (normalizedExpectedValue == null) {
            return true;
        }
        return protocolSelectors != null && protocolSelectors.contains(normalizedExpectedValue);
    }

    private boolean matchesRequired(String expectedValue, String actualValue) {
        String normalizedExpectedValue = normalizeLower(expectedValue);
        return normalizedExpectedValue != null && normalizedExpectedValue.equals(normalizeLower(actualValue));
    }

    private boolean matchesRequired(String expectedValue, Set<String> actualValues) {
        String normalizedExpectedValue = normalizeLower(expectedValue);
        return normalizedExpectedValue != null && actualValues != null && actualValues.contains(normalizedExpectedValue);
    }

    private boolean matchesRequiredProtocol(String expectedValue, Set<String> protocolSelectors) {
        String normalizedExpectedValue = normalizeProtocolSelector(expectedValue);
        return normalizedExpectedValue != null && protocolSelectors != null && protocolSelectors.contains(normalizedExpectedValue);
    }

    private int specificity(VendorMetricMappingRule rule) {
        int score = scopePriority(rule == null ? null : rule.getScopeType()) * 100;
        if (StringUtils.hasText(rule == null ? null : rule.getLogicalChannelCode())) {
            score += 10;
        }
        if (SCOPE_TYPE_PROTOCOL.equals(normalizeUpper(rule == null ? null : rule.getScopeType()))
                && isProtocolFamilySelector(rule == null ? null : rule.getProtocolCode())) {
            score += 1;
        }
        return score;
    }

    private int scopePriority(String scopeType) {
        String normalizedScopeType = normalizeUpper(scopeType);
        if (SCOPE_TYPE_PRODUCT.equals(normalizedScopeType)) {
            return 5;
        }
        if (SCOPE_TYPE_DEVICE_FAMILY.equals(normalizedScopeType)) {
            return 4;
        }
        if (SCOPE_TYPE_SCENARIO.equals(normalizedScopeType)) {
            return 3;
        }
        if (SCOPE_TYPE_PROTOCOL.equals(normalizedScopeType)) {
            return 2;
        }
        if (SCOPE_TYPE_TENANT_DEFAULT.equals(normalizedScopeType)) {
            return 1;
        }
        return 0;
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

    private Set<String> resolveProtocolSelectors(String protocolCode, List<String> familyCodes) {
        LinkedHashSet<String> selectors = new LinkedHashSet<>();
        String normalizedProtocolCode = normalizeLower(protocolCode);
        if (normalizedProtocolCode != null) {
            selectors.add(normalizedProtocolCode);
        }
        if (familyCodes == null || familyCodes.isEmpty() || iotProperties == null || iotProperties.getProtocol() == null) {
            return Set.copyOf(selectors);
        }
        for (String familyCode : familyCodes) {
            String normalizedFamilyCode = normalizeLower(familyCode);
            IotProperties.Protocol.FamilyDefinition familyDefinition = resolveProtocolFamilyDefinition(normalizedFamilyCode);
            if (normalizedFamilyCode == null || familyDefinition == null || Boolean.FALSE.equals(familyDefinition.getEnabled())) {
                continue;
            }
            String configuredProtocolCode = normalizeLower(familyDefinition.getProtocolCode());
            if (configuredProtocolCode != null && normalizedProtocolCode != null
                    && !configuredProtocolCode.equals(normalizedProtocolCode)) {
                continue;
            }
            selectors.add(PROTOCOL_FAMILY_SELECTOR_PREFIX + normalizedFamilyCode);
        }
        return Set.copyOf(selectors);
    }

    private IotProperties.Protocol.FamilyDefinition resolveProtocolFamilyDefinition(String familyCode) {
        if (!StringUtils.hasText(familyCode)
                || iotProperties == null
                || iotProperties.getProtocol() == null
                || iotProperties.getProtocol().getFamilyDefinitions() == null) {
            return null;
        }
        return iotProperties.getProtocol().getFamilyDefinitions().entrySet().stream()
                .filter(entry -> familyCode.equals(normalizeLower(entry.getKey()))
                        || familyCode.equals(normalizeLower(entry.getValue() == null ? null : entry.getValue().getFamilyCode())))
                .map(java.util.Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Set<String> resolveScenarioDeviceFamilies(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode) || normativeMetricDefinitionService == null) {
            return Set.of();
        }
        List<NormativeMetricDefinition> definitions = normativeMetricDefinitionService.listByScenario(scenarioCode);
        if (definitions == null || definitions.isEmpty()) {
            return Set.of();
        }
        return definitions.stream()
                .map(NormativeMetricDefinition::getDeviceFamily)
                .map(this::normalizeLower)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
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

    private String normalizeProtocolSelector(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String normalizedLower = normalized.toLowerCase(Locale.ROOT);
        if (!normalizedLower.startsWith(PROTOCOL_FAMILY_SELECTOR_PREFIX)) {
            return normalizedLower;
        }
        String familyCode = normalizeLower(normalizedLower.substring(PROTOCOL_FAMILY_SELECTOR_PREFIX.length()));
        if (!StringUtils.hasText(familyCode)) {
            return null;
        }
        return PROTOCOL_FAMILY_SELECTOR_PREFIX + familyCode;
    }

    private boolean isProtocolFamilySelector(String value) {
        String normalizedSelector = normalizeProtocolSelector(value);
        return StringUtils.hasText(normalizedSelector)
                && normalizedSelector.startsWith(PROTOCOL_FAMILY_SELECTOR_PREFIX);
    }

    private record ResolvedRuleCandidate(VendorMetricMappingRule rule,
                                         String targetNormativeIdentifier,
                                         int specificity) {
    }
}
