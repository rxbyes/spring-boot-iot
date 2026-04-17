package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleBatchStatusDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleReplayDTO;
import com.ghlzm.iot.device.dto.VendorMetricMappingRuleUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricMappingRule;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleMapper;
import com.ghlzm.iot.device.mapper.VendorMetricMappingRuleSnapshotMapper;
import com.ghlzm.iot.device.service.VendorMetricMappingRuleService;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleReplayVO;
import com.ghlzm.iot.device.vo.VendorMetricMappingRuleVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.framework.protocol.YamlProtocolSecurityDefinitionProvider;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class VendorMetricMappingRuleServiceImpl implements VendorMetricMappingRuleService {

    private static final String SCOPE_TYPE_PRODUCT = "PRODUCT";
    private static final String SCOPE_TYPE_DEVICE_FAMILY = "DEVICE_FAMILY";
    private static final String SCOPE_TYPE_SCENARIO = "SCENARIO";
    private static final String SCOPE_TYPE_PROTOCOL = "PROTOCOL";
    private static final String SCOPE_TYPE_TENANT_DEFAULT = "TENANT_DEFAULT";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String PROTOCOL_FAMILY_SELECTOR_PREFIX = "family:";
    private static final String HIT_SOURCE_MISS = "MISS";

    private final VendorMetricMappingRuleMapper mapper;
    private final VendorMetricMappingRuleSnapshotMapper snapshotMapper;
    private final ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider;
    private final ProductMapper productMapper;
    private final VendorMetricMappingRuntimeServiceImpl runtimeService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper) {
        this(mapper, null, (ProtocolSecurityDefinitionProvider) null, null, null);
    }

    public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                              VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                              IotProperties iotProperties) {
        this(mapper, snapshotMapper,
                iotProperties == null ? null : new YamlProtocolSecurityDefinitionProvider(iotProperties),
                null,
                null);
    }

    public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                              VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                              ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider) {
        this(mapper, snapshotMapper, protocolSecurityDefinitionProvider, null, null);
    }

    @Autowired
    public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                              VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                              ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
                                              ProductMapper productMapper,
                                              VendorMetricMappingRuntimeServiceImpl runtimeService) {
        this.mapper = mapper;
        this.snapshotMapper = snapshotMapper;
        this.protocolSecurityDefinitionProvider = protocolSecurityDefinitionProvider;
        this.productMapper = productMapper;
        this.runtimeService = runtimeService;
    }

    @Override
    public PageResult<VendorMetricMappingRuleVO> pageRules(Long productId, String status, Long pageNum, Long pageSize) {
        Page<VendorMetricMappingRule> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<VendorMetricMappingRule> result = mapper.selectPage(page, new LambdaQueryWrapper<VendorMetricMappingRule>()
                .eq(VendorMetricMappingRule::getDeleted, 0)
                .eq(productId != null, VendorMetricMappingRule::getProductId, productId)
                .eq(StringUtils.hasText(status), VendorMetricMappingRule::getStatus, normalizeUpper(status))
                .orderByDesc(VendorMetricMappingRule::getUpdateTime)
                .orderByDesc(VendorMetricMappingRule::getId));
        Map<Long, VendorMetricMappingRuleSnapshot> latestPublishedSnapshots = loadLatestPublishedSnapshots(result.getRecords());
        List<VendorMetricMappingRuleVO> records = result.getRecords().stream()
                .map(rule -> toVO(rule, latestPublishedSnapshots.get(rule.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(Long productId, Long operatorId, VendorMetricMappingRuleUpsertDTO dto) {
        VendorMetricMappingRule rule = new VendorMetricMappingRule();
        rule.setId(IdWorker.getId());
        applyEditableFields(rule, productId, dto);
        ensureNoConflictingRule(rule, null);
        rule.setStatus(normalizeStatus(dto == null ? null : dto.getStatus(), STATUS_DRAFT));
        rule.setVersionNo(1);
        rule.setCreateBy(normalizePositiveLong(operatorId));
        rule.setUpdateBy(normalizePositiveLong(operatorId));
        mapper.insert(rule);
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorMetricMappingRuleVO createAndGet(Long productId, Long operatorId, VendorMetricMappingRuleUpsertDTO dto) {
        Long ruleId = createRule(productId, operatorId, dto);
        return toVO(getRequiredRule(productId, ruleId), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VendorMetricMappingRuleVO updateAndGet(Long productId,
                                                  Long ruleId,
                                                  Long operatorId,
                                                  VendorMetricMappingRuleUpsertDTO dto) {
        VendorMetricMappingRule rule = getRequiredRule(productId, ruleId);
        applyEditableFields(rule, productId, dto);
        ensureNoConflictingRule(rule, ruleId);
        rule.setStatus(normalizeStatus(dto == null ? null : dto.getStatus(), rule.getStatus()));
        rule.setVersionNo(nextVersion(rule.getVersionNo()));
        rule.setUpdateBy(normalizePositiveLong(operatorId));
        if (mapper.updateById(rule) <= 0) {
            throw new BizException("厂商字段映射规则更新失败，请稍后重试");
        }
        return toVO(rule, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchStatus(Long productId, Long operatorId, VendorMetricMappingRuleBatchStatusDTO dto) {
        if (productId == null || productId <= 0) {
            throw new BizException("产品标识不能为空");
        }
        if (dto == null || dto.getRuleIds() == null || dto.getRuleIds().isEmpty()) {
            throw new BizException("ruleIds 不能为空");
        }
        String targetStatus = normalizeUpper(dto.getTargetStatus());
        if (!StringUtils.hasText(targetStatus)) {
            throw new BizException("targetStatus 不能为空");
        }

        Set<Long> selectedRuleIds = new LinkedHashSet<>();
        for (Long ruleId : dto.getRuleIds()) {
            if (ruleId != null && ruleId > 0) {
                selectedRuleIds.add(ruleId);
            }
        }
        if (selectedRuleIds.isEmpty()) {
            throw new BizException("ruleIds 不能为空");
        }

        List<VendorMetricMappingRule> rules = mapper.selectBatchIds(List.copyOf(selectedRuleIds));
        int matchedCount = 0;
        int changedCount = 0;
        for (VendorMetricMappingRule rule : rules == null ? List.<VendorMetricMappingRule>of() : rules) {
            if (rule == null || rule.getId() == null || Integer.valueOf(1).equals(rule.getDeleted())) {
                continue;
            }
            if (!productId.equals(rule.getProductId())) {
                continue;
            }
            matchedCount++;
            if (targetStatus.equals(normalizeUpper(rule.getStatus()))) {
                continue;
            }
            rule.setStatus(targetStatus);
            rule.setVersionNo(nextVersion(rule.getVersionNo()));
            rule.setUpdateBy(normalizePositiveLong(operatorId));
            mapper.updateById(rule);
            changedCount++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedCount", dto.getRuleIds().size());
        result.put("matchedCount", matchedCount);
        result.put("changedCount", changedCount);
        result.put("targetStatus", targetStatus);
        return result;
    }

    @Override
    public VendorMetricMappingRuleReplayVO replay(Long productId, VendorMetricMappingRuleReplayDTO dto) {
        if (productId == null || productId <= 0) {
            throw new BizException("产品标识不能为空");
        }
        if (dto == null || !StringUtils.hasText(dto.getRawIdentifier())) {
            throw new BizException("rawIdentifier 不能为空");
        }

        Product product = getRequiredProduct(productId);
        String rawIdentifier = normalizeRawIdentifier(dto.getRawIdentifier());
        String logicalChannelCode = normalizeUpper(dto.getLogicalChannelCode());
        String sampleValue = normalizeText(dto.getSampleValue());

        if (runtimeService == null) {
            return buildReplayMiss(rawIdentifier, logicalChannelCode, sampleValue);
        }

        VendorMetricMappingRuntimeServiceImpl.ReplayResolution resolution =
                runtimeService.replayForGovernance(product, rawIdentifier, logicalChannelCode);
        if (resolution == null) {
            return buildReplayMiss(rawIdentifier, logicalChannelCode, sampleValue);
        }

        String targetIdentifier = normalizeLowerIdentifier(resolution.targetNormativeIdentifier());
        VendorMetricMappingRuleReplayVO vo = new VendorMetricMappingRuleReplayVO();
        vo.setMatched(Boolean.TRUE.equals(resolution.matched()));
        vo.setHitSource(normalizeText(resolution.hitSource()));
        vo.setMatchedScopeType(normalizeUpper(resolution.matchedScopeType()));
        vo.setRuleId(resolution.ruleId());
        vo.setRawIdentifier(normalizeRawIdentifier(
                StringUtils.hasText(resolution.rawIdentifier()) ? resolution.rawIdentifier() : rawIdentifier
        ));
        vo.setLogicalChannelCode(normalizeUpper(
                StringUtils.hasText(resolution.logicalChannelCode()) ? resolution.logicalChannelCode() : logicalChannelCode
        ));
        vo.setTargetNormativeIdentifier(targetIdentifier);
        vo.setCanonicalIdentifier(targetIdentifier);
        vo.setSampleValue(sampleValue);
        return vo;
    }

    private VendorMetricMappingRule getRequiredRule(Long productId, Long ruleId) {
        VendorMetricMappingRule rule = mapper.selectById(ruleId);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted()) || !productId.equals(rule.getProductId())) {
            throw new BizException("厂商字段映射规则不存在: " + ruleId);
        }
        return rule;
    }

    private Product getRequiredProduct(Long productId) {
        if (productMapper == null) {
            throw new BizException("产品不存在: " + productId);
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("产品不存在: " + productId);
        }
        return product;
    }

    private VendorMetricMappingRuleReplayVO buildReplayMiss(String rawIdentifier,
                                                            String logicalChannelCode,
                                                            String sampleValue) {
        VendorMetricMappingRuleReplayVO vo = new VendorMetricMappingRuleReplayVO();
        vo.setMatched(Boolean.FALSE);
        vo.setHitSource(HIT_SOURCE_MISS);
        vo.setMatchedScopeType(null);
        vo.setRuleId(null);
        vo.setRawIdentifier(rawIdentifier);
        vo.setLogicalChannelCode(logicalChannelCode);
        vo.setTargetNormativeIdentifier(null);
        vo.setCanonicalIdentifier(null);
        vo.setSampleValue(sampleValue);
        return vo;
    }

    private void applyEditableFields(VendorMetricMappingRule rule,
                                     Long productId,
                                     VendorMetricMappingRuleUpsertDTO dto) {
        if (productId == null || productId <= 0) {
            throw new BizException("产品标识不能为空");
        }
        String scopeType = normalizeScopeType(dto == null ? null : dto.getScopeType());
        rule.setProductId(productId);
        rule.setScopeType(scopeType);
        rule.setProtocolCode(normalizeProtocolSelector(dto == null ? null : dto.getProtocolCode()));
        rule.setScenarioCode(normalizeLower(dto == null ? null : dto.getScenarioCode()));
        rule.setDeviceFamily(normalizeLower(dto == null ? null : dto.getDeviceFamily()));
        rule.setRawIdentifier(normalizeRawIdentifier(dto == null ? null : dto.getRawIdentifier()));
        rule.setLogicalChannelCode(normalizeText(dto == null ? null : dto.getLogicalChannelCode()));
        rule.setRelationConditionJson(normalizeJson(dto == null ? null : dto.getRelationConditionJson(), "relationConditionJson"));
        rule.setNormalizationRuleJson(normalizeJson(dto == null ? null : dto.getNormalizationRuleJson(), "normalizationRuleJson"));
        rule.setTargetNormativeIdentifier(normalizeLowerIdentifier(dto == null ? null : dto.getTargetNormativeIdentifier()));
        validateScopeFields(rule);
    }

    private VendorMetricMappingRuleVO toVO(VendorMetricMappingRule rule,
                                           VendorMetricMappingRuleSnapshot snapshot) {
        VendorMetricMappingRuleVO vo = new VendorMetricMappingRuleVO();
        vo.setId(rule.getId());
        vo.setProductId(rule.getProductId());
        vo.setScopeType(rule.getScopeType());
        vo.setProtocolCode(rule.getProtocolCode());
        vo.setScenarioCode(rule.getScenarioCode());
        vo.setDeviceFamily(rule.getDeviceFamily());
        vo.setRawIdentifier(rule.getRawIdentifier());
        vo.setLogicalChannelCode(rule.getLogicalChannelCode());
        vo.setRelationConditionJson(rule.getRelationConditionJson());
        vo.setNormalizationRuleJson(rule.getNormalizationRuleJson());
        vo.setTargetNormativeIdentifier(rule.getTargetNormativeIdentifier());
        vo.setStatus(rule.getStatus());
        vo.setVersionNo(rule.getVersionNo());
        vo.setPublishedStatus(snapshot == null ? null : snapshot.getLifecycleStatus());
        vo.setPublishedVersionNo(snapshot == null ? null : snapshot.getPublishedVersionNo());
        vo.setApprovalOrderId(snapshot == null ? rule.getApprovalOrderId() : snapshot.getApprovalOrderId());
        vo.setCreateBy(rule.getCreateBy());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateBy(rule.getUpdateBy());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    private Map<Long, VendorMetricMappingRuleSnapshot> loadLatestPublishedSnapshots(List<VendorMetricMappingRule> rules) {
        if (snapshotMapper == null || rules == null || rules.isEmpty()) {
            return Map.of();
        }
        Map<Long, VendorMetricMappingRuleSnapshot> latestSnapshots = new HashMap<>();
        rules.stream()
                .map(VendorMetricMappingRule::getProductId)
                .filter(productId -> productId != null && productId > 0)
                .distinct()
                .forEach(productId -> mergeLatestSnapshots(latestSnapshots, snapshotMapper.selectPublishedByProductId(productId)));
        return latestSnapshots;
    }

    private void mergeLatestSnapshots(Map<Long, VendorMetricMappingRuleSnapshot> target,
                                      List<VendorMetricMappingRuleSnapshot> snapshots) {
        if (target == null || snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (VendorMetricMappingRuleSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.getRuleId() == null) {
                continue;
            }
            target.merge(snapshot.getRuleId(), snapshot, this::preferHigherVersionSnapshot);
        }
    }

    private VendorMetricMappingRuleSnapshot preferHigherVersionSnapshot(VendorMetricMappingRuleSnapshot left,
                                                                        VendorMetricMappingRuleSnapshot right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Integer leftVersion = left.getPublishedVersionNo();
        Integer rightVersion = right.getPublishedVersionNo();
        if (leftVersion == null && rightVersion != null) {
            return right;
        }
        if (leftVersion != null && rightVersion == null) {
            return left;
        }
        if (leftVersion != null && rightVersion != null && !leftVersion.equals(rightVersion)) {
            return leftVersion > rightVersion ? left : right;
        }
        Long leftId = left.getId();
        Long rightId = right.getId();
        if (leftId == null) {
            return right;
        }
        if (rightId == null) {
            return left;
        }
        return leftId >= rightId ? left : right;
    }

    private String normalizeScopeType(String scopeType) {
        String normalized = normalizeUpper(scopeType);
        if (!StringUtils.hasText(normalized)) {
            return SCOPE_TYPE_PRODUCT;
        }
        return switch (normalized) {
            case SCOPE_TYPE_PRODUCT,
                 SCOPE_TYPE_DEVICE_FAMILY,
                 SCOPE_TYPE_SCENARIO,
                 SCOPE_TYPE_PROTOCOL,
                 SCOPE_TYPE_TENANT_DEFAULT -> normalized;
            default -> throw new BizException("scopeType 不合法: " + scopeType);
        };
    }

    private String normalizeStatus(String status, String defaultValue) {
        String normalized = normalizeUpper(status);
        return StringUtils.hasText(normalized) ? normalized : normalizeUpper(defaultValue);
    }

    private String normalizeRawIdentifier(String rawIdentifier) {
        String normalized = normalizeText(rawIdentifier);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeLowerIdentifier(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeJson(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String json = JsonPayloadUtils.normalizeJsonDocument(normalized);
        try {
            objectMapper.readTree(json);
            return json;
        } catch (Exception ex) {
            throw new BizException(fieldName + " 必须是合法 JSON");
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : null;
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

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private Long normalizePositiveLong(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private Integer nextVersion(Integer currentVersion) {
        return currentVersion == null || currentVersion <= 0 ? 1 : currentVersion + 1;
    }

    private void validateScopeFields(VendorMetricMappingRule rule) {
        if (rule == null) {
            return;
        }
        String scopeType = normalizeScopeType(rule.getScopeType());
        if (SCOPE_TYPE_DEVICE_FAMILY.equals(scopeType) && !StringUtils.hasText(rule.getDeviceFamily())) {
            throw new BizException("deviceFamily 不能为空");
        }
        if (SCOPE_TYPE_SCENARIO.equals(scopeType) && !StringUtils.hasText(rule.getScenarioCode())) {
            throw new BizException("scenarioCode 不能为空");
        }
        if (SCOPE_TYPE_PROTOCOL.equals(scopeType) && !StringUtils.hasText(rule.getProtocolCode())) {
            throw new BizException("protocolCode 不能为空");
        }
        if (SCOPE_TYPE_PROTOCOL.equals(scopeType)
                && isProtocolFamilySelector(rule.getProtocolCode())
                && !isConfiguredProtocolFamilySelector(rule.getProtocolCode())) {
            throw new BizException("protocolCode 对应的协议族不存在或未启用: " + rule.getProtocolCode());
        }
    }

    private boolean isProtocolFamilySelector(String protocolCode) {
        String normalizedSelector = normalizeProtocolSelector(protocolCode);
        return StringUtils.hasText(normalizedSelector)
                && normalizedSelector.startsWith(PROTOCOL_FAMILY_SELECTOR_PREFIX);
    }

    private boolean isConfiguredProtocolFamilySelector(String protocolCode) {
        String normalizedSelector = normalizeProtocolSelector(protocolCode);
        if (!StringUtils.hasText(normalizedSelector) || protocolSecurityDefinitionProvider == null) {
            return false;
        }
        String familyCode = normalizedSelector.substring(PROTOCOL_FAMILY_SELECTOR_PREFIX.length());
        IotProperties.Protocol.FamilyDefinition definition =
                protocolSecurityDefinitionProvider.getFamilyDefinition(familyCode);
        return definition != null && !Boolean.FALSE.equals(definition.getEnabled());
    }

    private void ensureNoConflictingRule(VendorMetricMappingRule incomingRule, Long currentRuleId) {
        if (incomingRule == null || incomingRule.getProductId() == null || !StringUtils.hasText(incomingRule.getRawIdentifier())) {
            return;
        }
        List<VendorMetricMappingRule> existingRules = mapper.selectList(new LambdaQueryWrapper<VendorMetricMappingRule>()
                .eq(VendorMetricMappingRule::getDeleted, 0)
                .eq(VendorMetricMappingRule::getProductId, incomingRule.getProductId())
                .eq(VendorMetricMappingRule::getRawIdentifier, incomingRule.getRawIdentifier()));
        if (existingRules == null || existingRules.isEmpty()) {
            return;
        }
        for (VendorMetricMappingRule existingRule : existingRules) {
            if (existingRule == null || Integer.valueOf(1).equals(existingRule.getDeleted())) {
                continue;
            }
            if (currentRuleId != null && currentRuleId.equals(existingRule.getId())) {
                continue;
            }
            if (!sameScopeSignature(existingRule, incomingRule)) {
                continue;
            }
            if (sameTarget(existingRule, incomingRule)) {
                continue;
            }
            throw new BizException("厂商字段映射规则存在冲突，请先清理同 scope 下的重复目标: " + incomingRule.getRawIdentifier());
        }
    }

    private boolean sameScopeSignature(VendorMetricMappingRule left, VendorMetricMappingRule right) {
        return equalsIgnoreCase(left.getScopeType(), right.getScopeType())
                && equalsIgnoreCase(left.getLogicalChannelCode(), right.getLogicalChannelCode())
                && equalsIgnoreCase(left.getProtocolCode(), right.getProtocolCode())
                && equalsIgnoreCase(left.getScenarioCode(), right.getScenarioCode())
                && equalsIgnoreCase(left.getDeviceFamily(), right.getDeviceFamily());
    }

    private boolean sameTarget(VendorMetricMappingRule left, VendorMetricMappingRule right) {
        return equalsIgnoreCase(left.getTargetNormativeIdentifier(), right.getTargetNormativeIdentifier());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        String normalizedLeft = normalizeText(left);
        String normalizedRight = normalizeText(right);
        if (normalizedLeft == null && normalizedRight == null) {
            return true;
        }
        if (normalizedLeft == null || normalizedRight == null) {
            return false;
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }
}
