package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseQueryDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseUpdateDTO;
import com.ghlzm.iot.device.entity.DeviceOnboardingCase;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceOnboardingCaseMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceOnboardingCaseService;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 无代码接入案例服务实现。
 */
@Service
public class DeviceOnboardingCaseServiceImpl implements DeviceOnboardingCaseService {

    private static final String STEP_PROTOCOL_GOVERNANCE = "PROTOCOL_GOVERNANCE";
    private static final String STEP_PRODUCT_GOVERNANCE = "PRODUCT_GOVERNANCE";
    private static final String STEP_CONTRACT_RELEASE = "CONTRACT_RELEASE";
    private static final String STEP_ACCEPTANCE = "ACCEPTANCE";

    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_READY = "READY";

    private static final String BLOCKER_PROTOCOL = "待补齐协议族/解密档案/协议模板";
    private static final String BLOCKER_PRODUCT = "待绑定产品并完成契约治理";
    private static final String BLOCKER_RELEASE = "待发布正式合同批次";

    private final DeviceOnboardingCaseMapper mapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceOnboardingCaseServiceImpl(DeviceOnboardingCaseMapper mapper, ProductMapper productMapper) {
        this.mapper = mapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResult<DeviceOnboardingCaseVO> pageCases(DeviceOnboardingCaseQueryDTO query) {
        DeviceOnboardingCaseQueryDTO safeQuery = query == null ? new DeviceOnboardingCaseQueryDTO() : query;
        Page<DeviceOnboardingCase> page = PageQueryUtils.buildPage(safeQuery.getPageNum(), safeQuery.getPageSize());
        LambdaQueryWrapper<DeviceOnboardingCase> wrapper = new LambdaQueryWrapper<DeviceOnboardingCase>()
                .eq(DeviceOnboardingCase::getDeleted, 0)
                .orderByDesc(DeviceOnboardingCase::getUpdateTime)
                .orderByDesc(DeviceOnboardingCase::getId);
        if (safeQuery.getTenantId() != null) {
            wrapper.eq(DeviceOnboardingCase::getTenantId, safeQuery.getTenantId());
        }
        if (StringUtils.hasText(safeQuery.getStatus())) {
            wrapper.eq(DeviceOnboardingCase::getStatus, safeQuery.getStatus().trim());
        }
        if (StringUtils.hasText(safeQuery.getCurrentStep())) {
            wrapper.eq(DeviceOnboardingCase::getCurrentStep, safeQuery.getCurrentStep().trim());
        }
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            String keyword = safeQuery.getKeyword().trim();
            wrapper.and(condition -> condition.like(DeviceOnboardingCase::getCaseCode, keyword)
                    .or()
                    .like(DeviceOnboardingCase::getCaseName, keyword)
                    .or()
                    .like(DeviceOnboardingCase::getScenarioCode, keyword)
                    .or()
                    .like(DeviceOnboardingCase::getDeviceFamily, keyword));
        }

        Page<DeviceOnboardingCase> result = mapper.selectPage(page, wrapper);
        List<DeviceOnboardingCaseVO> rows = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseVO createCase(DeviceOnboardingCaseCreateDTO dto, Long operatorUserId) {
        DeviceOnboardingCase entity = new DeviceOnboardingCase();
        entity.setTenantId(resolveTenantId(dto.getTenantId()));
        applyEditableFields(entity, dto);
        ensureCaseCodeUnique(entity.getTenantId(), entity.getCaseCode(), null);
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        entity.setCreateBy(operatorUserId);
        entity.setUpdateBy(operatorUserId);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public DeviceOnboardingCaseVO getCase(Long caseId) {
        return toVO(getRequiredCase(caseId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseVO updateCase(Long caseId, DeviceOnboardingCaseUpdateDTO dto, Long operatorUserId) {
        DeviceOnboardingCase entity = getRequiredCase(caseId);
        entity.setTenantId(resolveTenantId(dto.getTenantId() == null ? entity.getTenantId() : dto.getTenantId()));
        applyEditableFields(entity, dto);
        ensureCaseCodeUnique(entity.getTenantId(), entity.getCaseCode(), caseId);
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseVO refreshStatus(Long caseId, Long operatorUserId) {
        DeviceOnboardingCase entity = getRequiredCase(caseId);
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    private void applyEditableFields(DeviceOnboardingCase entity, DeviceOnboardingCaseCreateDTO dto) {
        entity.setCaseCode(normalizeRequired(dto.getCaseCode(), "案例编码"));
        entity.setCaseName(normalizeRequired(dto.getCaseName(), "案例名称"));
        entity.setScenarioCode(normalizeOptional(dto.getScenarioCode()));
        entity.setDeviceFamily(normalizeOptional(dto.getDeviceFamily()));
        entity.setProtocolFamilyCode(normalizeOptional(dto.getProtocolFamilyCode()));
        entity.setDecryptProfileCode(normalizeOptional(dto.getDecryptProfileCode()));
        entity.setProtocolTemplateCode(normalizeOptional(dto.getProtocolTemplateCode()));
        entity.setProductId(dto.getProductId());
        entity.setReleaseBatchId(dto.getReleaseBatchId());
        entity.setRemark(normalizeOptional(dto.getRemark()));
    }

    private void applyEditableFields(DeviceOnboardingCase entity, DeviceOnboardingCaseUpdateDTO dto) {
        entity.setCaseCode(normalizeRequired(dto.getCaseCode(), "案例编码"));
        entity.setCaseName(normalizeRequired(dto.getCaseName(), "案例名称"));
        entity.setScenarioCode(normalizeOptional(dto.getScenarioCode()));
        entity.setDeviceFamily(normalizeOptional(dto.getDeviceFamily()));
        entity.setProtocolFamilyCode(normalizeOptional(dto.getProtocolFamilyCode()));
        entity.setDecryptProfileCode(normalizeOptional(dto.getDecryptProfileCode()));
        entity.setProtocolTemplateCode(normalizeOptional(dto.getProtocolTemplateCode()));
        entity.setProductId(dto.getProductId());
        entity.setReleaseBatchId(dto.getReleaseBatchId());
        entity.setRemark(normalizeOptional(dto.getRemark()));
    }

    private void applyDerivedStatus(DeviceOnboardingCase entity) {
        DerivedStatus derivedStatus = deriveStatus(entity);
        entity.setCurrentStep(derivedStatus.currentStep());
        entity.setStatus(derivedStatus.status());
        entity.setBlockerSummaryJson(writeStringList(derivedStatus.blockers()));
    }

    private DerivedStatus deriveStatus(DeviceOnboardingCase entity) {
        List<String> blockers = new ArrayList<>();
        if (!StringUtils.hasText(entity.getProtocolFamilyCode())
                || !StringUtils.hasText(entity.getDecryptProfileCode())
                || !StringUtils.hasText(entity.getProtocolTemplateCode())) {
            blockers.add(BLOCKER_PROTOCOL);
            return new DerivedStatus(STEP_PROTOCOL_GOVERNANCE, STATUS_BLOCKED, blockers);
        }
        if (entity.getProductId() == null) {
            blockers.add(BLOCKER_PRODUCT);
            return new DerivedStatus(STEP_PRODUCT_GOVERNANCE, STATUS_BLOCKED, blockers);
        }
        if (entity.getReleaseBatchId() == null) {
            blockers.add(BLOCKER_RELEASE);
            return new DerivedStatus(STEP_CONTRACT_RELEASE, STATUS_IN_PROGRESS, blockers);
        }
        return new DerivedStatus(STEP_ACCEPTANCE, STATUS_READY, List.of());
    }

    private void ensureCaseCodeUnique(Long tenantId, String caseCode, Long excludeId) {
        LambdaQueryWrapper<DeviceOnboardingCase> wrapper = new LambdaQueryWrapper<DeviceOnboardingCase>()
                .eq(DeviceOnboardingCase::getTenantId, tenantId)
                .eq(DeviceOnboardingCase::getCaseCode, caseCode)
                .eq(DeviceOnboardingCase::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(DeviceOnboardingCase::getId, excludeId);
        }
        DeviceOnboardingCase existing = mapper.selectOne(wrapper);
        if (existing != null) {
            throw new BizException("接入案例编码已存在: " + caseCode);
        }
    }

    private void validateProductIfPresent(Long productId) {
        if (productId == null) {
            return;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() != null && product.getDeleted() == 1) {
            throw new BizException("产品不存在: " + productId);
        }
    }

    private DeviceOnboardingCase getRequiredCase(Long caseId) {
        DeviceOnboardingCase entity = mapper.selectById(caseId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BizException("接入案例不存在: " + caseId);
        }
        return entity;
    }

    private DeviceOnboardingCaseVO toVO(DeviceOnboardingCase entity) {
        DeviceOnboardingCaseVO vo = new DeviceOnboardingCaseVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setCaseCode(entity.getCaseCode());
        vo.setCaseName(entity.getCaseName());
        vo.setScenarioCode(entity.getScenarioCode());
        vo.setDeviceFamily(entity.getDeviceFamily());
        vo.setProtocolFamilyCode(entity.getProtocolFamilyCode());
        vo.setDecryptProfileCode(entity.getDecryptProfileCode());
        vo.setProtocolTemplateCode(entity.getProtocolTemplateCode());
        vo.setProductId(entity.getProductId());
        vo.setReleaseBatchId(entity.getReleaseBatchId());
        vo.setCurrentStep(entity.getCurrentStep());
        vo.setStatus(entity.getStatus());
        vo.setBlockers(readStringList(entity.getBlockerSummaryJson()));
        vo.setRemark(entity.getRemark());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long resolveTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ex) {
            throw new BizException("接入案例阻塞摘要序列化失败");
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            throw new BizException("接入案例阻塞摘要解析失败");
        }
    }

    private record DerivedStatus(String currentStep, String status, List<String> blockers) {
    }
}
