package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchStartAcceptanceDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchTemplateApplyDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseQueryDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseUpdateDTO;
import com.ghlzm.iot.device.entity.DeviceOnboardingCase;
import com.ghlzm.iot.device.entity.OnboardingTemplatePack;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceOnboardingCaseMapper;
import com.ghlzm.iot.device.mapper.OnboardingTemplatePackMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceOnboardingAcceptanceGateway;
import com.ghlzm.iot.device.service.DeviceOnboardingCaseService;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceLaunch;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingAcceptanceSummaryVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import java.time.LocalDateTime;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 无代码接入案例服务实现。
 */
@Service
public class DeviceOnboardingCaseServiceImpl implements DeviceOnboardingCaseService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private static final String STEP_PROTOCOL_GOVERNANCE = "PROTOCOL_GOVERNANCE";
    private static final String STEP_PRODUCT_GOVERNANCE = "PRODUCT_GOVERNANCE";
    private static final String STEP_CONTRACT_RELEASE = "CONTRACT_RELEASE";
    private static final String STEP_ACCEPTANCE = "ACCEPTANCE";

    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_READY = "READY";

    private static final String ACCEPTANCE_STATUS_RUNNING = "RUNNING";
    private static final String ACCEPTANCE_STATUS_PASSED = "PASSED";

    private static final String BLOCKER_PROTOCOL = "待补齐协议族/解密档案/协议模板";
    private static final String BLOCKER_PRODUCT = "待绑定产品并完成契约治理";
    private static final String BLOCKER_RELEASE = "待发布正式合同批次";
    private static final String BLOCKER_ACCEPTANCE_DEVICE = "待补齐验收设备编码";
    private static final String BLOCKER_ACCEPTANCE_RUNNING = "标准接入验收执行中";

    private final DeviceOnboardingCaseMapper mapper;
    private final ProductMapper productMapper;
    private final OnboardingTemplatePackMapper templatePackMapper;
    private final DeviceOnboardingAcceptanceGateway acceptanceGateway;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    @Autowired
    public DeviceOnboardingCaseServiceImpl(DeviceOnboardingCaseMapper mapper,
                                           ProductMapper productMapper,
                                           OnboardingTemplatePackMapper templatePackMapper,
                                           ObjectProvider<DeviceOnboardingAcceptanceGateway> acceptanceGatewayProvider) {
        this(
                mapper,
                productMapper,
                templatePackMapper,
                acceptanceGatewayProvider == null ? null : acceptanceGatewayProvider.getIfAvailable()
        );
    }

    DeviceOnboardingCaseServiceImpl(DeviceOnboardingCaseMapper mapper,
                                    ProductMapper productMapper,
                                    DeviceOnboardingAcceptanceGateway acceptanceGateway) {
        this(mapper, productMapper, null, acceptanceGateway);
    }

    DeviceOnboardingCaseServiceImpl(DeviceOnboardingCaseMapper mapper,
                                    ProductMapper productMapper,
                                    OnboardingTemplatePackMapper templatePackMapper,
                                    DeviceOnboardingAcceptanceGateway acceptanceGateway) {
        this.mapper = mapper;
        this.productMapper = productMapper;
        this.templatePackMapper = templatePackMapper;
        this.acceptanceGateway = acceptanceGateway;
    }

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
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
                    .like(DeviceOnboardingCase::getDeviceFamily, keyword)
                    .or()
                    .like(DeviceOnboardingCase::getDeviceCode, keyword));
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
        validateTemplatePackIfPresent(entity.getTemplatePackId());
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
        clearAcceptanceState(entity);
        ensureCaseCodeUnique(entity.getTenantId(), entity.getCaseCode(), caseId);
        validateTemplatePackIfPresent(entity.getTemplatePackId());
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public DeviceOnboardingCaseBatchResultVO batchCreateCases(DeviceOnboardingCaseBatchCreateDTO dto, Long operatorUserId) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BizException("请至少提供一条接入案例");
        }
        List<DeviceOnboardingCaseVO> successes = new ArrayList<>();
        List<DeviceOnboardingCaseBatchResultVO.FailureItem> failures = new ArrayList<>();
        for (DeviceOnboardingCaseCreateDTO item : dto.getItems()) {
            String caseCode = item == null ? null : normalizeOptional(item.getCaseCode());
            String caseName = item == null ? null : normalizeOptional(item.getCaseName());
            try {
                if (item == null) {
                    throw new BizException("接入案例不能为空");
                }
                successes.add(createCase(item, operatorUserId));
            } catch (Exception ex) {
                failures.add(buildFailureItem(null, caseCode, caseName, resolveFailureMessage(ex)));
            }
        }
        return buildBatchResult("BATCH_CREATE", successes, failures);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseBatchResultVO batchApplyTemplatePack(DeviceOnboardingCaseBatchTemplateApplyDTO dto,
                                                                    Long operatorUserId) {
        if (dto == null || dto.getCaseIds() == null || dto.getCaseIds().isEmpty()) {
            throw new BizException("请至少选择一个接入案例");
        }
        OnboardingTemplatePack templatePack = requireTemplatePack(dto.getTemplatePackId());
        List<DeviceOnboardingCaseVO> successes = new ArrayList<>();
        List<DeviceOnboardingCaseBatchResultVO.FailureItem> failures = new ArrayList<>();
        for (Long caseId : dto.getCaseIds()) {
            try {
                successes.add(applyTemplatePack(caseId, templatePack, operatorUserId));
            } catch (Exception ex) {
                failures.add(buildFailureItem(caseId, null, null, resolveFailureMessage(ex)));
            }
        }
        return buildBatchResult("BATCH_APPLY_TEMPLATE", successes, failures);
    }

    @Override
    public DeviceOnboardingCaseBatchResultVO batchStartAcceptance(DeviceOnboardingCaseBatchStartAcceptanceDTO dto,
                                                                  Long operatorUserId) {
        if (dto == null || dto.getCaseIds() == null || dto.getCaseIds().isEmpty()) {
            throw new BizException("请至少选择一个接入案例");
        }
        List<DeviceOnboardingCaseVO> successes = new ArrayList<>();
        List<DeviceOnboardingCaseBatchResultVO.FailureItem> failures = new ArrayList<>();
        for (Long caseId : dto.getCaseIds()) {
            DeviceOnboardingCase snapshot = mapper.selectById(caseId);
            try {
                successes.add(startAcceptance(caseId, operatorUserId));
            } catch (Exception ex) {
                failures.add(buildFailureItem(
                        caseId,
                        snapshot == null ? null : snapshot.getCaseCode(),
                        snapshot == null ? null : snapshot.getCaseName(),
                        resolveFailureMessage(ex)
                ));
            }
        }
        return buildBatchResult("BATCH_START_ACCEPTANCE", successes, failures);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseVO startAcceptance(Long caseId, Long operatorUserId) {
        DeviceOnboardingCase entity = getRequiredCase(caseId);
        validateProductIfPresent(entity.getProductId());
        DerivedStatus derivedStatus = deriveStatus(entity);
        entity.setCurrentStep(derivedStatus.currentStep());
        entity.setStatus(derivedStatus.status());
        entity.setBlockerSummaryJson(writeStringList(derivedStatus.blockers()));
        if (!STEP_ACCEPTANCE.equals(derivedStatus.currentStep()) || !STATUS_READY.equals(derivedStatus.status())) {
            clearAcceptanceState(entity);
            throw new BizException(derivedStatus.blockers().isEmpty()
                    ? "当前接入案例尚未具备触发验收条件"
                    : derivedStatus.blockers().get(0));
        }
        if (!StringUtils.hasText(entity.getDeviceCode())) {
            throw new BizException(BLOCKER_ACCEPTANCE_DEVICE);
        }
        if (acceptanceGateway == null) {
            throw new BizException("标准接入验收网关未启用");
        }
        DeviceOnboardingAcceptanceLaunch launch = acceptanceGateway.launch(toAcceptanceRequest(entity));
        entity.setAcceptanceJobId(normalizeOptional(launch == null ? null : launch.jobId()));
        entity.setAcceptanceRunId(null);
        applyAcceptanceProgress(entity, new DeviceOnboardingAcceptanceProgress(
                entity.getAcceptanceJobId(),
                null,
                ACCEPTANCE_STATUS_RUNNING,
                BLOCKER_ACCEPTANCE_RUNNING,
                List.of(),
                null
        ));
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        recordAcceptanceStartedEvent(entity, operatorUserId);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceOnboardingCaseVO refreshStatus(Long caseId, Long operatorUserId) {
        DeviceOnboardingCase entity = getRequiredCase(caseId);
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        syncAcceptanceProgressIfNeeded(entity);
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
        entity.setTemplatePackId(dto.getTemplatePackId());
        entity.setProductId(dto.getProductId());
        entity.setReleaseBatchId(dto.getReleaseBatchId());
        entity.setDeviceCode(normalizeOptional(dto.getDeviceCode()));
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
        entity.setTemplatePackId(dto.getTemplatePackId());
        entity.setProductId(dto.getProductId());
        entity.setReleaseBatchId(dto.getReleaseBatchId());
        entity.setDeviceCode(normalizeOptional(dto.getDeviceCode()));
        entity.setRemark(normalizeOptional(dto.getRemark()));
    }

    private void applyDerivedStatus(DeviceOnboardingCase entity) {
        DerivedStatus derivedStatus = deriveStatus(entity);
        entity.setCurrentStep(derivedStatus.currentStep());
        entity.setStatus(derivedStatus.status());
        entity.setBlockerSummaryJson(writeStringList(derivedStatus.blockers()));
        if (!STEP_ACCEPTANCE.equals(derivedStatus.currentStep()) || !STATUS_READY.equals(derivedStatus.status())) {
            clearAcceptanceState(entity);
        }
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
        if (!StringUtils.hasText(entity.getDeviceCode())) {
            blockers.add(BLOCKER_ACCEPTANCE_DEVICE);
            return new DerivedStatus(STEP_ACCEPTANCE, STATUS_BLOCKED, blockers);
        }
        return new DerivedStatus(STEP_ACCEPTANCE, STATUS_READY, List.of());
    }

    private DeviceOnboardingCaseVO applyTemplatePack(Long caseId,
                                                     OnboardingTemplatePack templatePack,
                                                     Long operatorUserId) {
        DeviceOnboardingCase entity = getRequiredCase(caseId);
        entity.setTemplatePackId(templatePack.getId());
        entity.setScenarioCode(normalizeOptional(templatePack.getScenarioCode()));
        entity.setDeviceFamily(normalizeOptional(templatePack.getDeviceFamily()));
        entity.setProtocolFamilyCode(normalizeOptional(templatePack.getProtocolFamilyCode()));
        entity.setDecryptProfileCode(normalizeOptional(templatePack.getDecryptProfileCode()));
        entity.setProtocolTemplateCode(normalizeOptional(templatePack.getProtocolTemplateCode()));
        clearAcceptanceState(entity);
        validateProductIfPresent(entity.getProductId());
        applyDerivedStatus(entity);
        entity.setUpdateBy(operatorUserId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    private OnboardingTemplatePack requireTemplatePack(Long templatePackId) {
        if (templatePackId == null) {
            throw new BizException("templatePackId 不能为空");
        }
        if (templatePackMapper == null) {
            throw new BizException("模板包服务未启用");
        }
        OnboardingTemplatePack templatePack = templatePackMapper.selectById(templatePackId);
        if (templatePack == null || templatePack.getDeleted() != null && templatePack.getDeleted() == 1) {
            throw new BizException("模板包不存在: " + templatePackId);
        }
        return templatePack;
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

    private void validateTemplatePackIfPresent(Long templatePackId) {
        if (templatePackId == null) {
            return;
        }
        if (templatePackMapper == null) {
            throw new BizException("模板包服务未启用");
        }
        OnboardingTemplatePack templatePack = templatePackMapper.selectById(templatePackId);
        if (templatePack == null || templatePack.getDeleted() != null && templatePack.getDeleted() == 1) {
            throw new BizException("模板包不存在: " + templatePackId);
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
        vo.setTemplatePackId(entity.getTemplatePackId());
        vo.setProductId(entity.getProductId());
        vo.setReleaseBatchId(entity.getReleaseBatchId());
        vo.setDeviceCode(entity.getDeviceCode());
        vo.setCurrentStep(entity.getCurrentStep());
        vo.setStatus(entity.getStatus());
        vo.setBlockers(readStringList(entity.getBlockerSummaryJson()));
        vo.setAcceptance(readAcceptanceSummary(entity.getEvidenceSummaryJson()));
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

    private void recordAcceptanceStartedEvent(DeviceOnboardingCase entity, Long operatorUserId) {
        if (entity == null) {
            return;
        }
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(defaultTenantId(entity.getTenantId()));
        event.setTraceId(TraceContextHolder.currentOrCreate());
        event.setEventCode("acceptance.onboarding_case.launched");
        event.setEventName("接入案例验收启动完成");
        event.setDomainCode("acceptance");
        event.setActionCode("launch_onboarding_case");
        event.setObjectType("onboarding_case");
        event.setObjectId(entity.getId() == null ? null : String.valueOf(entity.getId()));
        event.setObjectName(entity.getCaseName());
        event.setActorUserId(operatorUserId);
        event.setResultStatus(ObservabilityEvidenceStatus.SUCCESS);
        event.setSourceType("DEVICE_ONBOARDING");
        event.setEvidenceType("iot_device_onboarding_case");
        event.setEvidenceId(entity.getId() == null ? null : String.valueOf(entity.getId()));
        event.setOccurredAt(LocalDateTime.now());
        event.getMetadata().putAll(buildAcceptanceStartedMetadata(entity));
        evidenceRecorder.recordBusinessEvent(event);
    }

    private Map<String, Object> buildAcceptanceStartedMetadata(DeviceOnboardingCase entity) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("caseId", entity.getId());
        metadata.put("caseCode", entity.getCaseCode());
        metadata.put("caseName", entity.getCaseName());
        metadata.put("scenarioCode", entity.getScenarioCode());
        metadata.put("deviceFamily", entity.getDeviceFamily());
        metadata.put("productId", entity.getProductId());
        metadata.put("releaseBatchId", entity.getReleaseBatchId());
        metadata.put("deviceCode", entity.getDeviceCode());
        metadata.put("protocolFamilyCode", entity.getProtocolFamilyCode());
        metadata.put("decryptProfileCode", entity.getDecryptProfileCode());
        metadata.put("protocolTemplateCode", entity.getProtocolTemplateCode());
        metadata.put("acceptanceJobId", entity.getAcceptanceJobId());
        metadata.put("acceptanceRunId", entity.getAcceptanceRunId());
        metadata.put("currentStep", entity.getCurrentStep());
        metadata.put("status", entity.getStatus());
        return metadata;
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? DEFAULT_TENANT_ID : tenantId;
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

    private void syncAcceptanceProgressIfNeeded(DeviceOnboardingCase entity) {
        if (entity == null || acceptanceGateway == null || !STEP_ACCEPTANCE.equals(entity.getCurrentStep())) {
            return;
        }
        if (!StringUtils.hasText(entity.getAcceptanceJobId()) && !StringUtils.hasText(entity.getAcceptanceRunId())) {
            return;
        }
        DeviceOnboardingAcceptanceProgress progress = acceptanceGateway.getProgress(
                normalizeOptional(entity.getAcceptanceJobId()),
                normalizeOptional(entity.getAcceptanceRunId())
        );
        if (progress == null) {
            return;
        }
        applyAcceptanceProgress(entity, progress);
    }

    private void applyAcceptanceProgress(DeviceOnboardingCase entity, DeviceOnboardingAcceptanceProgress progress) {
        if (entity == null || progress == null) {
            return;
        }
        DeviceOnboardingAcceptanceSummaryVO acceptance = new DeviceOnboardingAcceptanceSummaryVO();
        acceptance.setJobId(normalizeOptional(firstNonBlank(progress.jobId(), entity.getAcceptanceJobId())));
        acceptance.setRunId(normalizeOptional(firstNonBlank(progress.runId(), entity.getAcceptanceRunId())));
        acceptance.setStatus(normalizeOptional(progress.status()));
        acceptance.setSummary(normalizeOptional(progress.summary()));
        acceptance.setFailedLayers(progress.failedLayers() == null ? List.of() : List.copyOf(progress.failedLayers()));
        acceptance.setJumpPath(normalizeOptional(progress.jumpPath()));
        entity.setAcceptanceJobId(acceptance.getJobId());
        entity.setAcceptanceRunId(acceptance.getRunId());
        entity.setEvidenceSummaryJson(writeAcceptanceSummary(acceptance));
        if (ACCEPTANCE_STATUS_RUNNING.equalsIgnoreCase(progress.status())) {
            entity.setCurrentStep(STEP_ACCEPTANCE);
            entity.setStatus(STATUS_IN_PROGRESS);
            entity.setBlockerSummaryJson(writeStringList(List.of(BLOCKER_ACCEPTANCE_RUNNING)));
            return;
        }
        if (ACCEPTANCE_STATUS_PASSED.equalsIgnoreCase(progress.status())) {
            entity.setCurrentStep(STEP_ACCEPTANCE);
            entity.setStatus(STATUS_READY);
            entity.setBlockerSummaryJson(writeStringList(List.of()));
            return;
        }
        entity.setCurrentStep(STEP_ACCEPTANCE);
        entity.setStatus(STATUS_BLOCKED);
        entity.setBlockerSummaryJson(writeStringList(List.of(firstNonBlank(progress.summary(), "标准接入验收未通过"))));
    }

    private DeviceOnboardingAcceptanceRequest toAcceptanceRequest(DeviceOnboardingCase entity) {
        return new DeviceOnboardingAcceptanceRequest(
                entity.getId(),
                entity.getTenantId(),
                entity.getCaseCode(),
                entity.getCaseName(),
                entity.getProductId(),
                entity.getReleaseBatchId(),
                entity.getDeviceCode(),
                entity.getProtocolFamilyCode(),
                entity.getDecryptProfileCode(),
                entity.getProtocolTemplateCode()
        );
    }

    private DeviceOnboardingAcceptanceSummaryVO readAcceptanceSummary(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DeviceOnboardingAcceptanceSummaryVO.class);
        } catch (Exception ex) {
            throw new BizException("接入案例验收摘要解析失败");
        }
    }

    private String writeAcceptanceSummary(DeviceOnboardingAcceptanceSummaryVO summary) {
        if (summary == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception ex) {
            throw new BizException("接入案例验收摘要序列化失败");
        }
    }

    private void clearAcceptanceState(DeviceOnboardingCase entity) {
        if (entity == null) {
            return;
        }
        entity.setAcceptanceJobId(null);
        entity.setAcceptanceRunId(null);
        entity.setEvidenceSummaryJson(null);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private DeviceOnboardingCaseBatchResultVO buildBatchResult(
            String action,
            List<DeviceOnboardingCaseVO> successes,
            List<DeviceOnboardingCaseBatchResultVO.FailureItem> failures) {
        DeviceOnboardingCaseBatchResultVO result = new DeviceOnboardingCaseBatchResultVO();
        result.setAction(action);
        result.setRequestedCount((successes == null ? 0 : successes.size()) + (failures == null ? 0 : failures.size()));
        result.setSuccessCount(successes == null ? 0 : successes.size());
        result.setFailedCount(failures == null ? 0 : failures.size());
        result.setSuccessItems(successes == null ? List.of() : successes.stream().map(this::toBatchSuccessItem).toList());
        result.setFailureItems(failures == null ? List.of() : List.copyOf(failures));
        result.setFailureGroups(groupFailureItems(failures));
        return result;
    }

    private DeviceOnboardingCaseBatchResultVO.SuccessItem toBatchSuccessItem(DeviceOnboardingCaseVO vo) {
        DeviceOnboardingCaseBatchResultVO.SuccessItem item = new DeviceOnboardingCaseBatchResultVO.SuccessItem();
        item.setCaseId(vo.getId());
        item.setCaseCode(vo.getCaseCode());
        item.setCaseName(vo.getCaseName());
        item.setCurrentStep(vo.getCurrentStep());
        item.setStatus(vo.getStatus());
        item.setDeviceCode(vo.getDeviceCode());
        item.setAcceptanceStatus(vo.getAcceptance() == null ? null : vo.getAcceptance().getStatus());
        item.setAcceptanceRunId(vo.getAcceptance() == null ? null : vo.getAcceptance().getRunId());
        return item;
    }

    private DeviceOnboardingCaseBatchResultVO.FailureItem buildFailureItem(Long caseId,
                                                                           String caseCode,
                                                                           String caseName,
                                                                           String message) {
        String summary = StringUtils.hasText(message) ? message.trim() : "批量操作失败";
        DeviceOnboardingCaseBatchResultVO.FailureItem item = new DeviceOnboardingCaseBatchResultVO.FailureItem();
        item.setCaseId(caseId);
        item.setCaseCode(caseCode);
        item.setCaseName(caseName);
        item.setFailureKey(summary);
        item.setMessage(summary);
        return item;
    }

    private List<DeviceOnboardingCaseBatchResultVO.FailureGroup> groupFailureItems(
            List<DeviceOnboardingCaseBatchResultVO.FailureItem> failures) {
        if (failures == null || failures.isEmpty()) {
            return List.of();
        }
        Map<String, DeviceOnboardingCaseBatchResultVO.FailureGroup> groups = new LinkedHashMap<>();
        for (DeviceOnboardingCaseBatchResultVO.FailureItem item : failures) {
            String key = StringUtils.hasText(item.getFailureKey()) ? item.getFailureKey().trim() : "批量操作失败";
            DeviceOnboardingCaseBatchResultVO.FailureGroup group = groups.computeIfAbsent(key, ignored -> {
                DeviceOnboardingCaseBatchResultVO.FailureGroup created = new DeviceOnboardingCaseBatchResultVO.FailureGroup();
                created.setFailureKey(key);
                created.setSummary(key);
                created.setCount(0);
                created.setCaseCodes(new ArrayList<>());
                return created;
            });
            group.setCount(group.getCount() == null ? 1 : group.getCount() + 1);
            if (StringUtils.hasText(item.getCaseCode())) {
                group.getCaseCodes().add(item.getCaseCode().trim());
            }
        }
        return List.copyOf(groups.values());
    }

    private String resolveFailureMessage(Exception ex) {
        if (ex instanceof BizException bizException && StringUtils.hasText(bizException.getMessage())) {
            return bizException.getMessage();
        }
        return ex == null ? "批量操作失败" : firstNonBlank(ex.getMessage(), "批量操作失败");
    }

    private record DerivedStatus(String currentStep, String status, List<String> blockers) {
    }
}
