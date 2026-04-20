package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackCreateDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackPageQueryDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackUpdateDTO;
import com.ghlzm.iot.device.entity.OnboardingTemplatePack;
import com.ghlzm.iot.device.mapper.OnboardingTemplatePackMapper;
import com.ghlzm.iot.device.service.OnboardingTemplatePackService;
import com.ghlzm.iot.device.vo.OnboardingTemplatePackVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 无代码接入模板包服务实现。
 */
@Service
public class OnboardingTemplatePackServiceImpl implements OnboardingTemplatePackService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final OnboardingTemplatePackMapper mapper;

    public OnboardingTemplatePackServiceImpl(OnboardingTemplatePackMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<OnboardingTemplatePackVO> pagePacks(OnboardingTemplatePackPageQueryDTO query) {
        OnboardingTemplatePackPageQueryDTO safeQuery = query == null ? new OnboardingTemplatePackPageQueryDTO() : query;
        Page<OnboardingTemplatePack> page = PageQueryUtils.buildPage(safeQuery.getPageNum(), safeQuery.getPageSize());
        LambdaQueryWrapper<OnboardingTemplatePack> wrapper = new LambdaQueryWrapper<OnboardingTemplatePack>()
                .eq(OnboardingTemplatePack::getDeleted, 0)
                .eq(safeQuery.getTenantId() != null, OnboardingTemplatePack::getTenantId, safeQuery.getTenantId())
                .eq(StringUtils.hasText(safeQuery.getStatus()), OnboardingTemplatePack::getStatus, normalizeOptional(safeQuery.getStatus()))
                .eq(StringUtils.hasText(safeQuery.getScenarioCode()), OnboardingTemplatePack::getScenarioCode, normalizeOptional(safeQuery.getScenarioCode()))
                .eq(StringUtils.hasText(safeQuery.getDeviceFamily()), OnboardingTemplatePack::getDeviceFamily, normalizeOptional(safeQuery.getDeviceFamily()))
                .orderByDesc(OnboardingTemplatePack::getUpdateTime)
                .orderByDesc(OnboardingTemplatePack::getId);
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(condition -> condition.like(OnboardingTemplatePack::getPackCode, keyword)
                    .or()
                    .like(OnboardingTemplatePack::getPackName, keyword)
                    .or()
                    .like(OnboardingTemplatePack::getScenarioCode, keyword)
                    .or()
                    .like(OnboardingTemplatePack::getDeviceFamily, keyword));
        }
        Page<OnboardingTemplatePack> result = mapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(this::toVO).toList()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingTemplatePackVO createPack(OnboardingTemplatePackCreateDTO dto, Long operatorUserId) {
        OnboardingTemplatePack entity = new OnboardingTemplatePack();
        entity.setTenantId(resolveTenantId(dto.getTenantId()));
        applyEditableFields(entity, dto);
        entity.setStatus(resolveStatus(entity.getStatus()));
        entity.setVersionNo(1);
        ensurePackCodeUnique(entity.getTenantId(), entity.getPackCode(), null);
        entity.setCreateBy(operatorUserId);
        entity.setUpdateBy(operatorUserId);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingTemplatePackVO updatePack(Long packId, OnboardingTemplatePackUpdateDTO dto, Long operatorUserId) {
        OnboardingTemplatePack entity = getRequiredPack(packId);
        entity.setTenantId(resolveTenantId(dto.getTenantId() == null ? entity.getTenantId() : dto.getTenantId()));
        applyEditableFields(entity, dto);
        entity.setStatus(resolveStatus(entity.getStatus()));
        entity.setVersionNo(firstPositive(entity.getVersionNo()) + 1);
        ensurePackCodeUnique(entity.getTenantId(), entity.getPackCode(), packId);
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    private void applyEditableFields(OnboardingTemplatePack entity, OnboardingTemplatePackCreateDTO dto) {
        entity.setPackCode(normalizeRequired(dto.getPackCode(), "模板包编码"));
        entity.setPackName(normalizeRequired(dto.getPackName(), "模板包名称"));
        entity.setScenarioCode(normalizeOptional(dto.getScenarioCode()));
        entity.setDeviceFamily(normalizeOptional(dto.getDeviceFamily()));
        entity.setStatus(normalizeOptional(dto.getStatus()));
        entity.setProtocolFamilyCode(normalizeOptional(dto.getProtocolFamilyCode()));
        entity.setDecryptProfileCode(normalizeOptional(dto.getDecryptProfileCode()));
        entity.setProtocolTemplateCode(normalizeOptional(dto.getProtocolTemplateCode()));
        entity.setDefaultGovernanceConfigJson(normalizeOptional(dto.getDefaultGovernanceConfigJson()));
        entity.setDefaultInsightConfigJson(normalizeOptional(dto.getDefaultInsightConfigJson()));
        entity.setDefaultAcceptanceProfileJson(normalizeOptional(dto.getDefaultAcceptanceProfileJson()));
        entity.setDescription(normalizeOptional(dto.getDescription()));
        entity.setRemark(normalizeOptional(dto.getRemark()));
    }

    private void applyEditableFields(OnboardingTemplatePack entity, OnboardingTemplatePackUpdateDTO dto) {
        entity.setPackCode(normalizeRequired(dto.getPackCode(), "模板包编码"));
        entity.setPackName(normalizeRequired(dto.getPackName(), "模板包名称"));
        entity.setScenarioCode(normalizeOptional(dto.getScenarioCode()));
        entity.setDeviceFamily(normalizeOptional(dto.getDeviceFamily()));
        entity.setStatus(normalizeOptional(dto.getStatus()));
        entity.setProtocolFamilyCode(normalizeOptional(dto.getProtocolFamilyCode()));
        entity.setDecryptProfileCode(normalizeOptional(dto.getDecryptProfileCode()));
        entity.setProtocolTemplateCode(normalizeOptional(dto.getProtocolTemplateCode()));
        entity.setDefaultGovernanceConfigJson(normalizeOptional(dto.getDefaultGovernanceConfigJson()));
        entity.setDefaultInsightConfigJson(normalizeOptional(dto.getDefaultInsightConfigJson()));
        entity.setDefaultAcceptanceProfileJson(normalizeOptional(dto.getDefaultAcceptanceProfileJson()));
        entity.setDescription(normalizeOptional(dto.getDescription()));
        entity.setRemark(normalizeOptional(dto.getRemark()));
    }

    private void ensurePackCodeUnique(Long tenantId, String packCode, Long excludeId) {
        LambdaQueryWrapper<OnboardingTemplatePack> wrapper = new LambdaQueryWrapper<OnboardingTemplatePack>()
                .eq(OnboardingTemplatePack::getTenantId, tenantId)
                .eq(OnboardingTemplatePack::getPackCode, packCode)
                .eq(OnboardingTemplatePack::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(OnboardingTemplatePack::getId, excludeId);
        }
        if (mapper.selectOne(wrapper) != null) {
            throw new BizException("模板包编码已存在: " + packCode);
        }
    }

    private OnboardingTemplatePack getRequiredPack(Long packId) {
        OnboardingTemplatePack entity = mapper.selectById(packId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BizException("模板包不存在: " + packId);
        }
        return entity;
    }

    private OnboardingTemplatePackVO toVO(OnboardingTemplatePack entity) {
        OnboardingTemplatePackVO vo = new OnboardingTemplatePackVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setPackCode(entity.getPackCode());
        vo.setPackName(entity.getPackName());
        vo.setScenarioCode(entity.getScenarioCode());
        vo.setDeviceFamily(entity.getDeviceFamily());
        vo.setStatus(entity.getStatus());
        vo.setVersionNo(entity.getVersionNo());
        vo.setProtocolFamilyCode(entity.getProtocolFamilyCode());
        vo.setDecryptProfileCode(entity.getDecryptProfileCode());
        vo.setProtocolTemplateCode(entity.getProtocolTemplateCode());
        vo.setDefaultGovernanceConfigJson(entity.getDefaultGovernanceConfigJson());
        vo.setDefaultInsightConfigJson(entity.getDefaultInsightConfigJson());
        vo.setDefaultAcceptanceProfileJson(entity.getDefaultAcceptanceProfileJson());
        vo.setDescription(entity.getDescription());
        vo.setRemark(entity.getRemark());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private Long resolveTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveStatus(String status) {
        String normalized = normalizeOptional(status);
        return StringUtils.hasText(normalized) ? normalized : STATUS_ACTIVE;
    }

    private int firstPositive(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }
}
