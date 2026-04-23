package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.RuntimeMetricDisplayRuleUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.RuntimeMetricDisplayRule;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.RuntimeMetricDisplayRuleMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.RuntimeMetricDisplayRuleService;
import com.ghlzm.iot.device.vo.RuntimeMetricDisplayRuleVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RuntimeMetricDisplayRuleServiceImpl implements RuntimeMetricDisplayRuleService {

    private static final String SCOPE_TYPE_PRODUCT = "PRODUCT";
    private static final String SCOPE_TYPE_DEVICE_FAMILY = "DEVICE_FAMILY";
    private static final String SCOPE_TYPE_SCENARIO = "SCENARIO";
    private static final String SCOPE_TYPE_PROTOCOL = "PROTOCOL";
    private static final String SCOPE_TYPE_TENANT_DEFAULT = "TENANT_DEFAULT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_ACTIVE, STATUS_DISABLED);
    private static final Set<String> DISABLED_STATUSES = Set.of(STATUS_DISABLED, "INACTIVE", "RETIRED");

    private final RuntimeMetricDisplayRuleMapper mapper;
    private final ProductMapper productMapper;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();

    public RuntimeMetricDisplayRuleServiceImpl(RuntimeMetricDisplayRuleMapper mapper,
                                               NormativeMetricDefinitionService normativeMetricDefinitionService) {
        this(mapper, null, normativeMetricDefinitionService);
    }

    @Autowired
    public RuntimeMetricDisplayRuleServiceImpl(RuntimeMetricDisplayRuleMapper mapper,
                                               ProductMapper productMapper,
                                               NormativeMetricDefinitionService normativeMetricDefinitionService) {
        this.mapper = mapper;
        this.productMapper = productMapper;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
    }

    @Override
    public PageResult<RuntimeMetricDisplayRuleVO> pageRules(Long productId, String status, Long pageNum, Long pageSize) {
        Page<RuntimeMetricDisplayRule> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<RuntimeMetricDisplayRule> result = mapper.selectPage(page, new LambdaQueryWrapper<RuntimeMetricDisplayRule>()
                .eq(RuntimeMetricDisplayRule::getDeleted, 0)
                .eq(productId != null, RuntimeMetricDisplayRule::getProductId, productId)
                .eq(StringUtils.hasText(status), RuntimeMetricDisplayRule::getStatus, normalizeStatus(status, STATUS_ACTIVE))
                .orderByDesc(RuntimeMetricDisplayRule::getUpdateTime)
                .orderByDesc(RuntimeMetricDisplayRule::getId));
        return PageResult.of(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(this::toVO).toList()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuntimeMetricDisplayRuleVO createAndGet(Long productId, Long operatorId, RuntimeMetricDisplayRuleUpsertDTO dto) {
        RuntimeMetricDisplayRule rule = new RuntimeMetricDisplayRule();
        applyEditableFields(rule, productId, dto);
        ensureProductExists(productId);
        ensureNoConflictingRule(rule, null);
        rule.setStatus(normalizeStatus(dto == null ? null : dto.getStatus(), STATUS_ACTIVE));
        rule.setVersionNo(1);
        rule.setCreateBy(normalizePositiveLong(operatorId));
        rule.setUpdateBy(normalizePositiveLong(operatorId));
        mapper.insert(rule);
        return toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RuntimeMetricDisplayRuleVO updateAndGet(Long productId,
                                                   Long ruleId,
                                                   Long operatorId,
                                                   RuntimeMetricDisplayRuleUpsertDTO dto) {
        RuntimeMetricDisplayRule rule = getRequiredRule(productId, ruleId);
        applyEditableFields(rule, productId, dto);
        ensureNoConflictingRule(rule, ruleId);
        rule.setStatus(normalizeStatus(dto == null ? null : dto.getStatus(), rule.getStatus()));
        rule.setVersionNo(nextVersion(rule.getVersionNo()));
        rule.setUpdateBy(normalizePositiveLong(operatorId));
        if (mapper.updateById(rule) <= 0) {
            throw new BizException("运行态字段显示规则更新失败，请稍后重试");
        }
        return toVO(rule);
    }

    @Override
    public DisplayResolution resolveForDisplay(Product product, String rawIdentifier) {
        Long productId = product == null ? null : product.getId();
        String normalizedRawIdentifier = normalizeLower(rawIdentifier);
        if (productId == null || normalizedRawIdentifier == null) {
            return null;
        }
        String scenarioCode = normalizeLower(normativeMatcher.resolveScenarioCode(product));
        Set<String> scenarioDeviceFamilies = resolveScenarioDeviceFamilies(scenarioCode);
        String protocolCode = normalizeProtocolCode(product == null ? null : product.getProtocolCode());
        List<RuntimeMetricDisplayRule> rules = mapper.selectList(new LambdaQueryWrapper<RuntimeMetricDisplayRule>()
                .eq(RuntimeMetricDisplayRule::getDeleted, 0)
                .eq(RuntimeMetricDisplayRule::getRawIdentifier, normalizedRawIdentifier)
                .and(wrapper -> wrapper.eq(RuntimeMetricDisplayRule::getProductId, productId)
                        .or(scope -> scope.isNull(RuntimeMetricDisplayRule::getProductId)
                                .eq(RuntimeMetricDisplayRule::getScopeType, SCOPE_TYPE_TENANT_DEFAULT))));
        return rules.stream()
                .filter(this::isUsableRule)
                .filter(rule -> normalizedRawIdentifier.equals(normalizeLower(rule.getRawIdentifier())))
                .filter(rule -> scopeMatches(rule, productId, scenarioCode, scenarioDeviceFamilies, protocolCode))
                .sorted(Comparator
                        .comparingInt((RuntimeMetricDisplayRule rule) -> specificity(rule, scenarioCode, scenarioDeviceFamilies, protocolCode))
                        .reversed()
                        .thenComparing(RuntimeMetricDisplayRule::getId, Comparator.nullsLast(Long::compareTo)))
                .map(rule -> new DisplayResolution(
                        rule.getId(),
                        normalizeScopeType(rule.getScopeType()),
                        normalizeText(rule.getDisplayName()),
                        normalizeText(rule.getUnit())
                ))
                .findFirst()
                .orElse(null);
    }

    private RuntimeMetricDisplayRule getRequiredRule(Long productId, Long ruleId) {
        RuntimeMetricDisplayRule rule = mapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted()) || !Objects.equals(productId, rule.getProductId())) {
            throw new BizException("运行态字段显示规则不存在: " + ruleId);
        }
        return rule;
    }

    private void applyEditableFields(RuntimeMetricDisplayRule rule,
                                     Long productId,
                                     RuntimeMetricDisplayRuleUpsertDTO dto) {
        if (productId == null || productId <= 0) {
            throw new BizException("产品标识不能为空");
        }
        rule.setProductId(productId);
        rule.setScopeType(normalizeScopeType(dto == null ? null : dto.getScopeType()));
        rule.setProtocolCode(normalizeProtocolCode(dto == null ? null : dto.getProtocolCode()));
        rule.setScenarioCode(normalizeLower(dto == null ? null : dto.getScenarioCode()));
        rule.setDeviceFamily(normalizeLower(dto == null ? null : dto.getDeviceFamily()));
        rule.setRawIdentifier(normalizeLower(dto == null ? null : dto.getRawIdentifier()));
        rule.setDisplayName(normalizeText(dto == null ? null : dto.getDisplayName()));
        rule.setUnit(normalizeText(dto == null ? null : dto.getUnit()));
        validateScopeFields(rule);
    }

    private void ensureProductExists(Long productId) {
        if (productId == null || productId <= 0) {
            throw new BizException("产品标识不能为空");
        }
        if (productMapper == null) {
            return;
        }
        if (productMapper.selectById(productId) == null) {
            throw new BizException("产品不存在: " + productId);
        }
    }

    private void ensureNoConflictingRule(RuntimeMetricDisplayRule target, Long currentRuleId) {
        List<RuntimeMetricDisplayRule> conflicts = mapper.selectList(new LambdaQueryWrapper<RuntimeMetricDisplayRule>()
                .eq(RuntimeMetricDisplayRule::getDeleted, 0)
                .eq(RuntimeMetricDisplayRule::getProductId, target.getProductId())
                .eq(RuntimeMetricDisplayRule::getScopeType, target.getScopeType())
                .eq(RuntimeMetricDisplayRule::getRawIdentifier, target.getRawIdentifier())
                .eq(StringUtils.hasText(target.getProtocolCode()), RuntimeMetricDisplayRule::getProtocolCode, target.getProtocolCode())
                .eq(StringUtils.hasText(target.getScenarioCode()), RuntimeMetricDisplayRule::getScenarioCode, target.getScenarioCode())
                .eq(StringUtils.hasText(target.getDeviceFamily()), RuntimeMetricDisplayRule::getDeviceFamily, target.getDeviceFamily()));
        boolean conflicted = conflicts.stream()
                .filter(Objects::nonNull)
                .filter(rule -> rule.getId() != null)
                .anyMatch(rule -> !Objects.equals(rule.getId(), currentRuleId));
        if (conflicted) {
            throw new BizException("同一作用域下已存在运行态字段显示规则: " + target.getRawIdentifier());
        }
    }

    private void validateScopeFields(RuntimeMetricDisplayRule rule) {
        if (!StringUtils.hasText(rule.getRawIdentifier())) {
            throw new BizException("rawIdentifier 不能为空");
        }
        if (!StringUtils.hasText(rule.getDisplayName())) {
            throw new BizException("displayName 不能为空");
        }
        switch (rule.getScopeType()) {
            case SCOPE_TYPE_PRODUCT -> {
                rule.setProtocolCode(null);
                rule.setScenarioCode(null);
                rule.setDeviceFamily(null);
            }
            case SCOPE_TYPE_DEVICE_FAMILY -> {
                if (!StringUtils.hasText(rule.getScenarioCode())) {
                    throw new BizException("DEVICE_FAMILY 作用域必须填写 scenarioCode");
                }
                if (!StringUtils.hasText(rule.getDeviceFamily())) {
                    throw new BizException("DEVICE_FAMILY 作用域必须填写 deviceFamily");
                }
                rule.setProtocolCode(null);
            }
            case SCOPE_TYPE_SCENARIO -> {
                if (!StringUtils.hasText(rule.getScenarioCode())) {
                    throw new BizException("SCENARIO 作用域必须填写 scenarioCode");
                }
                rule.setProtocolCode(null);
                rule.setDeviceFamily(null);
            }
            case SCOPE_TYPE_PROTOCOL -> {
                if (!StringUtils.hasText(rule.getProtocolCode())) {
                    throw new BizException("PROTOCOL 作用域必须填写 protocolCode");
                }
                rule.setScenarioCode(null);
                rule.setDeviceFamily(null);
            }
            case SCOPE_TYPE_TENANT_DEFAULT -> {
                rule.setProtocolCode(null);
                rule.setScenarioCode(null);
                rule.setDeviceFamily(null);
            }
            default -> throw new BizException("scopeType 不支持: " + rule.getScopeType());
        }
    }

    private boolean isUsableRule(RuntimeMetricDisplayRule rule) {
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            return false;
        }
        if (!StringUtils.hasText(rule.getDisplayName())) {
            return false;
        }
        return !DISABLED_STATUSES.contains(normalizeStatus(rule.getStatus(), STATUS_ACTIVE));
    }

    private boolean scopeMatches(RuntimeMetricDisplayRule rule,
                                 Long productId,
                                 String scenarioCode,
                                 Set<String> deviceFamilies,
                                 String protocolCode) {
        if (rule == null) {
            return false;
        }
        return switch (normalizeScopeType(rule.getScopeType())) {
            case SCOPE_TYPE_PRODUCT -> Objects.equals(rule.getProductId(), productId);
            case SCOPE_TYPE_DEVICE_FAMILY ->
                    matchesOptional(rule.getScenarioCode(), scenarioCode) && matchesOptional(rule.getDeviceFamily(), deviceFamilies);
            case SCOPE_TYPE_SCENARIO -> matchesOptional(rule.getScenarioCode(), scenarioCode);
            case SCOPE_TYPE_PROTOCOL -> matchesOptional(rule.getProtocolCode(), protocolCode);
            case SCOPE_TYPE_TENANT_DEFAULT -> true;
            default -> false;
        };
    }

    private int specificity(RuntimeMetricDisplayRule rule,
                            String scenarioCode,
                            Set<String> deviceFamilies,
                            String protocolCode) {
        int base = switch (normalizeScopeType(rule.getScopeType())) {
            case SCOPE_TYPE_PRODUCT -> 500;
            case SCOPE_TYPE_DEVICE_FAMILY -> 400;
            case SCOPE_TYPE_SCENARIO -> 300;
            case SCOPE_TYPE_PROTOCOL -> 200;
            case SCOPE_TYPE_TENANT_DEFAULT -> 100;
            default -> 0;
        };
        if (matchesOptional(rule.getScenarioCode(), scenarioCode)) {
            base += 20;
        }
        if (matchesOptional(rule.getDeviceFamily(), deviceFamilies)) {
            base += 10;
        }
        if (matchesOptional(rule.getProtocolCode(), protocolCode)) {
            base += 5;
        }
        return base;
    }

    private Set<String> resolveScenarioDeviceFamilies(String scenarioCode) {
        if (!StringUtils.hasText(scenarioCode) || normativeMetricDefinitionService == null) {
            return Set.of();
        }
        return normativeMetricDefinitionService.listByScenario(scenarioCode).stream()
                .map(item -> item == null ? null : item.getDeviceFamily())
                .map(this::normalizeLower)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private RuntimeMetricDisplayRuleVO toVO(RuntimeMetricDisplayRule rule) {
        RuntimeMetricDisplayRuleVO vo = new RuntimeMetricDisplayRuleVO();
        vo.setId(rule.getId());
        vo.setProductId(rule.getProductId());
        vo.setScopeType(rule.getScopeType());
        vo.setProtocolCode(rule.getProtocolCode());
        vo.setScenarioCode(rule.getScenarioCode());
        vo.setDeviceFamily(rule.getDeviceFamily());
        vo.setRawIdentifier(rule.getRawIdentifier());
        vo.setDisplayName(rule.getDisplayName());
        vo.setUnit(rule.getUnit());
        vo.setStatus(rule.getStatus());
        vo.setVersionNo(rule.getVersionNo());
        vo.setCreateBy(rule.getCreateBy());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateBy(rule.getUpdateBy());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    private String normalizeScopeType(String value) {
        String normalized = normalizeUpper(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException("scopeType 不能为空");
        }
        return normalized;
    }

    private String normalizeStatus(String value, String defaultValue) {
        String normalized = normalizeUpper(value);
        if (!StringUtils.hasText(normalized)) {
            return defaultValue;
        }
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BizException("status 不支持: " + value);
        }
        return normalized;
    }

    private Integer nextVersion(Integer currentVersion) {
        return currentVersion == null || currentVersion < 1 ? 1 : currentVersion + 1;
    }

    private Long normalizePositiveLong(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private boolean matchesOptional(String expected, String actual) {
        String normalizedExpected = normalizeLower(expected);
        if (!StringUtils.hasText(normalizedExpected)) {
            return true;
        }
        return normalizedExpected.equals(normalizeLower(actual));
    }

    private boolean matchesOptional(String expected, Set<String> actualValues) {
        String normalizedExpected = normalizeLower(expected);
        if (!StringUtils.hasText(normalizedExpected)) {
            return true;
        }
        if (actualValues == null || actualValues.isEmpty()) {
            return false;
        }
        return actualValues.stream().map(this::normalizeLower).anyMatch(normalizedExpected::equals);
    }

    private String normalizeProtocolCode(String value) {
        return normalizeLower(value);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
