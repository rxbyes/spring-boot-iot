package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceRelationUpsertDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceRelation;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceRelationMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.model.DeviceRelationRule;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备关系服务实现。
 */
@Service
public class DeviceRelationServiceImpl implements DeviceRelationService {

    private static final Set<String> SUPPORTED_RELATION_TYPES = Set.of("COLLECTOR_CHILD", "GATEWAY_CHILD");
    private static final Set<String> SUPPORTED_CANONICALIZATION_STRATEGIES = Set.of("LEGACY", "LF_VALUE");
    private static final Set<String> SUPPORTED_STATUS_MIRROR_STRATEGIES = Set.of("NONE", "SENSOR_STATE");
    private static final Comparator<DeviceRelation> RELATION_ORDER = Comparator
            .comparing(DeviceRelation::getLogicalChannelCode, Comparator.nullsLast(String::compareTo))
            .thenComparing(DeviceRelation::getId, Comparator.nullsLast(Long::compareTo));

    private final DeviceRelationMapper deviceRelationMapper;
    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;

    public DeviceRelationServiceImpl(DeviceRelationMapper deviceRelationMapper,
                                     DeviceMapper deviceMapper,
                                     ProductMapper productMapper) {
        this.deviceRelationMapper = deviceRelationMapper;
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceRelationVO createRelation(Long currentUserId, DeviceRelationUpsertDTO dto) {
        Device parent = getRequiredDeviceByCode(normalizeRequired(dto == null ? null : dto.getParentDeviceCode(), "父设备编码"));
        Device child = getRequiredDeviceByCode(normalizeRequired(dto == null ? null : dto.getChildDeviceCode(), "子设备编码"));
        validateParentAndChild(parent, child);

        String logicalChannelCode = normalizeRequired(dto.getLogicalChannelCode(), "逻辑通道编码");
        ensureUnique(parent.getTenantId(), parent.getId(), logicalChannelCode, null);

        DeviceRelation relation = new DeviceRelation();
        fillRelation(relation, currentUserId, dto, parent, child, logicalChannelCode);
        if (deviceRelationMapper.insert(relation) <= 0) {
            throw new BizException("设备关系新增失败，请稍后重试");
        }
        return toVO(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceRelationVO updateRelation(Long currentUserId, Long relationId, DeviceRelationUpsertDTO dto) {
        DeviceRelation existing = getRequiredRelation(relationId);
        Device parent = getRequiredDeviceByCode(normalizeRequired(dto == null ? null : dto.getParentDeviceCode(), "父设备编码"));
        Device child = getRequiredDeviceByCode(normalizeRequired(dto == null ? null : dto.getChildDeviceCode(), "子设备编码"));
        validateParentAndChild(parent, child);

        String logicalChannelCode = normalizeRequired(dto.getLogicalChannelCode(), "逻辑通道编码");
        ensureUnique(parent.getTenantId(), parent.getId(), logicalChannelCode, relationId);

        fillRelation(existing, currentUserId, dto, parent, child, logicalChannelCode);
        if (deviceRelationMapper.updateById(existing) <= 0) {
            throw new BizException("设备关系更新失败，请稍后重试");
        }
        return toVO(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long currentUserId, Long relationId) {
        DeviceRelation relation = getRequiredRelation(relationId);
        relation.setUpdateBy(currentUserId);
        if (deviceRelationMapper.deleteById(relation.getId()) <= 0) {
            throw new BizException("设备关系删除失败，请稍后重试");
        }
    }

    @Override
    public List<DeviceRelationVO> listByParentDeviceCode(Long currentUserId, String parentDeviceCode) {
        String normalizedParentDeviceCode = normalizeRequired(parentDeviceCode, "父设备编码");
        return deduplicateRelations(deviceRelationMapper.selectList(new LambdaQueryWrapper<DeviceRelation>()
                        .eq(DeviceRelation::getParentDeviceCode, normalizedParentDeviceCode)
                        .eq(DeviceRelation::getDeleted, 0)
                        .orderByAsc(DeviceRelation::getLogicalChannelCode)
                        .orderByAsc(DeviceRelation::getId)))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<DeviceRelationRule> listEnabledRulesByParentDeviceCode(String parentDeviceCode) {
        String normalizedParentDeviceCode = normalizeOptional(parentDeviceCode);
        if (normalizedParentDeviceCode == null) {
            return List.of();
        }
        return deduplicateRelations(deviceRelationMapper.selectList(new LambdaQueryWrapper<DeviceRelation>()
                        .eq(DeviceRelation::getParentDeviceCode, normalizedParentDeviceCode)
                        .eq(DeviceRelation::getEnabled, 1)
                        .eq(DeviceRelation::getDeleted, 0)
                        .orderByAsc(DeviceRelation::getLogicalChannelCode)
                        .orderByAsc(DeviceRelation::getId)))
                .stream()
                .map(this::toRule)
                .toList();
    }

    /**
     * 历史库可能绕过唯一校验写入重复的逻辑通道映射，读侧统一保留同通道最新记录。
     */
    private List<DeviceRelation> deduplicateRelations(List<DeviceRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        Map<String, DeviceRelation> deduplicated = new LinkedHashMap<>();
        relations.stream()
                .sorted(RELATION_ORDER)
                .forEach(relation -> deduplicated.put(buildRelationDedupKey(relation), relation));
        return deduplicated.values().stream()
                .sorted(RELATION_ORDER)
                .toList();
    }

    private String buildRelationDedupKey(DeviceRelation relation) {
        String logicalChannelCode = normalizeOptional(relation == null ? null : relation.getLogicalChannelCode());
        if (logicalChannelCode == null) {
            return "__relation__" + (relation == null ? "null" : relation.getId());
        }
        return logicalChannelCode.toUpperCase(Locale.ROOT);
    }

    private void fillRelation(DeviceRelation relation,
                              Long currentUserId,
                              DeviceRelationUpsertDTO dto,
                              Device parent,
                              Device child,
                              String logicalChannelCode) {
        Product childProduct = child.getProductId() == null ? null : productMapper.selectById(child.getProductId());
        relation.setTenantId(parent.getTenantId());
        relation.setParentDeviceId(parent.getId());
        relation.setParentDeviceCode(parent.getDeviceCode());
        relation.setLogicalChannelCode(logicalChannelCode);
        relation.setChildDeviceId(child.getId());
        relation.setChildDeviceCode(child.getDeviceCode());
        relation.setChildProductId(child.getProductId());
        relation.setChildProductKey(childProduct == null ? null : childProduct.getProductKey());
        relation.setRelationType(normalizeRelationType(dto.getRelationType()));
        relation.setCanonicalizationStrategy(normalizeCanonicalizationStrategy(dto.getCanonicalizationStrategy()));
        relation.setStatusMirrorStrategy(normalizeStatusMirrorStrategy(dto.getStatusMirrorStrategy()));
        relation.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        relation.setRemark(normalizeOptional(dto.getRemark()));
        relation.setUpdateBy(currentUserId);
        if (relation.getCreateBy() == null) {
            relation.setCreateBy(currentUserId);
        }
    }

    private Device getRequiredDeviceByCode(String deviceCode) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode)
                .eq(Device::getDeleted, 0)
                .last("LIMIT 1"));
        if (device == null) {
            throw new BizException("设备不存在: " + deviceCode);
        }
        return device;
    }

    private DeviceRelation getRequiredRelation(Long relationId) {
        DeviceRelation relation = deviceRelationMapper.selectById(relationId);
        if (relation == null || Integer.valueOf(1).equals(relation.getDeleted())) {
            throw new BizException("设备关系不存在: " + relationId);
        }
        return relation;
    }

    private void validateParentAndChild(Device parent, Device child) {
        if (parent == null || child == null) {
            return;
        }
        if (parent.getId() != null && parent.getId().equals(child.getId())) {
            throw new BizException("父设备和子设备不能是同一台设备");
        }
        if (parent.getTenantId() != null && child.getTenantId() != null && !parent.getTenantId().equals(child.getTenantId())) {
            throw new BizException("父子设备必须属于同一租户");
        }
    }

    private void ensureUnique(Long tenantId, Long parentDeviceId, String logicalChannelCode, Long excludeRelationId) {
        DeviceRelation existing = deviceRelationMapper.selectOne(new LambdaQueryWrapper<DeviceRelation>()
                .eq(DeviceRelation::getTenantId, tenantId)
                .eq(DeviceRelation::getParentDeviceId, parentDeviceId)
                .eq(DeviceRelation::getLogicalChannelCode, logicalChannelCode)
                .eq(DeviceRelation::getDeleted, 0)
                .ne(excludeRelationId != null, DeviceRelation::getId, excludeRelationId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException("同一父设备下逻辑通道已存在: " + logicalChannelCode);
        }
    }

    private String normalizeRelationType(String relationType) {
        String normalized = normalizeRequired(relationType, "关系类型").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RELATION_TYPES.contains(normalized)) {
            throw new BizException("关系类型不支持: " + relationType);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeCanonicalizationStrategy(String strategy) {
        String normalized = normalizeRequired(strategy, "归一化策略").toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CANONICALIZATION_STRATEGIES.contains(normalized)) {
            throw new BizException("归一化策略不支持: " + strategy);
        }
        return normalized;
    }

    private String normalizeStatusMirrorStrategy(String strategy) {
        String normalized = normalizeOptional(strategy);
        if (normalized == null) {
            return "NONE";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATUS_MIRROR_STRATEGIES.contains(upper)) {
            throw new BizException("状态镜像策略不支持: " + strategy);
        }
        return upper;
    }

    private DeviceRelationVO toVO(DeviceRelation relation) {
        DeviceRelationVO vo = new DeviceRelationVO();
        vo.setId(relation.getId());
        vo.setParentDeviceCode(relation.getParentDeviceCode());
        vo.setLogicalChannelCode(relation.getLogicalChannelCode());
        vo.setChildDeviceCode(relation.getChildDeviceCode());
        vo.setChildProductId(relation.getChildProductId());
        vo.setChildProductKey(relation.getChildProductKey());
        vo.setRelationType(relation.getRelationType());
        vo.setCanonicalizationStrategy(relation.getCanonicalizationStrategy());
        vo.setStatusMirrorStrategy(relation.getStatusMirrorStrategy());
        vo.setEnabled(relation.getEnabled());
        vo.setRemark(relation.getRemark());
        vo.setCreateTime(relation.getCreateTime());
        vo.setUpdateTime(relation.getUpdateTime());
        return vo;
    }

    private DeviceRelationRule toRule(DeviceRelation relation) {
        DeviceRelationRule rule = new DeviceRelationRule();
        rule.setRelationId(relation.getId());
        rule.setParentDeviceCode(relation.getParentDeviceCode());
        rule.setLogicalChannelCode(relation.getLogicalChannelCode());
        rule.setChildDeviceCode(relation.getChildDeviceCode());
        rule.setChildProductId(relation.getChildProductId());
        rule.setChildProductKey(relation.getChildProductKey());
        rule.setRelationType(relation.getRelationType());
        rule.setCanonicalizationStrategy(relation.getCanonicalizationStrategy());
        rule.setStatusMirrorStrategy(relation.getStatusMirrorStrategy());
        return rule;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
