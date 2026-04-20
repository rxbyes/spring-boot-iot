package com.ghlzm.iot.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.ProductMetricResolverSnapshot;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceRequest;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.protocol.ProtocolSecurityDefinitionProvider;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateReplayDTO;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateReplayService;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateReplayVO;
import com.ghlzm.iot.report.service.DeviceOnboardingAcceptanceService;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchSeries;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class DeviceOnboardingAcceptanceServiceImpl implements DeviceOnboardingAcceptanceService {

    private static final String WORKFLOW_CODE = "device-onboarding";
    private static final String RUNNER_TYPE = "deviceOnboardingCheck";
    private static final DateTimeFormatter RUN_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Path workspaceRoot;
    private final Path resultsDir;
    private final ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider;
    private final ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper;
    private final ProtocolTemplateReplayService protocolTemplateReplayService;
    private final PublishedProductContractSnapshotService publishedProductContractSnapshotService;
    private final ProductContractReleaseBatchMapper productContractReleaseBatchMapper;
    private final ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper;
    private final ProductMetricResolverSnapshotMapper productMetricResolverSnapshotMapper;
    private final DeviceService deviceService;
    private final DeviceMessageService deviceMessageService;
    private final TelemetryQueryService telemetryQueryService;
    private final CollectorChildInsightService collectorChildInsightService;
    private final RiskMetricCatalogService riskMetricCatalogService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeviceOnboardingAcceptanceServiceImpl(
            @Value("${iot.device-onboarding.workspace-root:.}") String workspaceRoot,
            ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
            ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper,
            ProtocolTemplateReplayService protocolTemplateReplayService,
            PublishedProductContractSnapshotService publishedProductContractSnapshotService,
            ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
            ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper,
            ProductMetricResolverSnapshotMapper productMetricResolverSnapshotMapper,
            DeviceService deviceService,
            DeviceMessageService deviceMessageService,
            TelemetryQueryService telemetryQueryService,
            CollectorChildInsightService collectorChildInsightService,
            RiskMetricCatalogService riskMetricCatalogService
    ) {
        this(
                Paths.get(workspaceRoot),
                protocolSecurityDefinitionProvider,
                protocolTemplateDefinitionSnapshotMapper,
                protocolTemplateReplayService,
                publishedProductContractSnapshotService,
                productContractReleaseBatchMapper,
                productContractReleaseSnapshotMapper,
                productMetricResolverSnapshotMapper,
                deviceService,
                deviceMessageService,
                telemetryQueryService,
                collectorChildInsightService,
                riskMetricCatalogService,
                JsonMapper.builder().findAndAddModules().build()
        );
    }

    DeviceOnboardingAcceptanceServiceImpl(
            Path workspaceRoot,
            ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
            ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper,
            ProtocolTemplateReplayService protocolTemplateReplayService,
            PublishedProductContractSnapshotService publishedProductContractSnapshotService,
            ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
            ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper,
            ProductMetricResolverSnapshotMapper productMetricResolverSnapshotMapper,
            DeviceService deviceService,
            DeviceMessageService deviceMessageService,
            TelemetryQueryService telemetryQueryService,
            CollectorChildInsightService collectorChildInsightService,
            RiskMetricCatalogService riskMetricCatalogService
    ) {
        this(
                workspaceRoot,
                protocolSecurityDefinitionProvider,
                protocolTemplateDefinitionSnapshotMapper,
                protocolTemplateReplayService,
                publishedProductContractSnapshotService,
                productContractReleaseBatchMapper,
                productContractReleaseSnapshotMapper,
                productMetricResolverSnapshotMapper,
                deviceService,
                deviceMessageService,
                telemetryQueryService,
                collectorChildInsightService,
                riskMetricCatalogService,
                JsonMapper.builder().findAndAddModules().build()
        );
    }

    DeviceOnboardingAcceptanceServiceImpl(
            Path workspaceRoot,
            ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
            ProtocolTemplateDefinitionSnapshotMapper protocolTemplateDefinitionSnapshotMapper,
            ProtocolTemplateReplayService protocolTemplateReplayService,
            PublishedProductContractSnapshotService publishedProductContractSnapshotService,
            ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
            ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper,
            ProductMetricResolverSnapshotMapper productMetricResolverSnapshotMapper,
            DeviceService deviceService,
            DeviceMessageService deviceMessageService,
            TelemetryQueryService telemetryQueryService,
            CollectorChildInsightService collectorChildInsightService,
            RiskMetricCatalogService riskMetricCatalogService,
            ObjectMapper objectMapper
    ) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.resultsDir = this.workspaceRoot.resolve("logs").resolve("acceptance").normalize();
        this.protocolSecurityDefinitionProvider = protocolSecurityDefinitionProvider;
        this.protocolTemplateDefinitionSnapshotMapper = protocolTemplateDefinitionSnapshotMapper;
        this.protocolTemplateReplayService = protocolTemplateReplayService;
        this.publishedProductContractSnapshotService = publishedProductContractSnapshotService;
        this.productContractReleaseBatchMapper = productContractReleaseBatchMapper;
        this.productContractReleaseSnapshotMapper = productContractReleaseSnapshotMapper;
        this.productMetricResolverSnapshotMapper = productMetricResolverSnapshotMapper;
        this.deviceService = deviceService;
        this.deviceMessageService = deviceMessageService;
        this.telemetryQueryService = telemetryQueryService;
        this.collectorChildInsightService = collectorChildInsightService;
        this.riskMetricCatalogService = riskMetricCatalogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceOnboardingAcceptanceProgress run(DeviceOnboardingAcceptanceRequest request) {
        validateRequest(request);
        String runId = nextRunId();
        String markdownPath = "logs/acceptance/registry-run-" + runId + ".md";
        List<AutomationResultRunResultVO> results = buildResults(request, markdownPath);
        AutomationResultRunDetailVO detail = buildRunDetail(runId, request, results);
        writeRunArtifacts(runId, detail);
        return toProgress(runId, detail.getSummary(), detail.getResults());
    }

    @Override
    public DeviceOnboardingAcceptanceProgress loadProgress(String runId) {
        String normalizedRunId = requireText(runId, "runId 不能为空");
        Path runFile = resultsDir.resolve("registry-run-" + normalizedRunId + ".json").normalize();
        if (!runFile.startsWith(resultsDir) || !Files.isRegularFile(runFile)) {
            throw new BizException("未找到接入验收运行结果: " + normalizedRunId);
        }
        try {
            AutomationResultRunDetailVO detail = objectMapper.readValue(
                    Files.readString(runFile, StandardCharsets.UTF_8),
                    AutomationResultRunDetailVO.class
            );
            detail.setRunId(normalizedRunId);
            detail.setSummary(normalizeSummary(detail.getSummary(), detail.getResults()));
            detail.setResults(normalizeResults(detail.getResults()));
            return toProgress(normalizedRunId, detail.getSummary(), detail.getResults());
        } catch (IOException ex) {
            throw new BizException("读取接入验收结果失败: " + normalizedRunId);
        }
    }

    private List<AutomationResultRunResultVO> buildResults(DeviceOnboardingAcceptanceRequest request, String markdownPath) {
        return List.of(
                runCheck("device-onboarding.protocol-family", "协议层", markdownPath, () -> checkProtocolFamily(request)),
                runCheck("device-onboarding.decrypt-profile", "解密层", markdownPath, () -> checkDecryptProfile(request)),
                runCheck("device-onboarding.template-replay", "模板层", markdownPath, () -> checkTemplateReplay(request)),
                runCheck("device-onboarding.compare", "映射层", markdownPath, () -> checkCompareSnapshot(request)),
                runCheck("device-onboarding.mapping", "映射层", markdownPath, () -> checkMetricResolverSnapshot(request)),
                runCheck("device-onboarding.contract-release", "合同层", markdownPath, () -> checkContractRelease(request)),
                runCheck("device-onboarding.read-side", "读侧层", markdownPath, () -> checkReadSide(request)),
                runCheck("device-onboarding.risk-boundary", "风险层", markdownPath, () -> checkRiskBoundary(request))
        );
    }

    private AutomationResultRunResultVO runCheck(String scenarioId,
                                                 String layer,
                                                 String markdownPath,
                                                 CheckExecutor executor) {
        CheckOutcome outcome;
        try {
            outcome = executor.execute();
        } catch (Exception ex) {
            outcome = CheckOutcome.failed(
                    StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : layer + "检查失败",
                    detailMap()
            );
        }
        AutomationResultRunResultVO result = new AutomationResultRunResultVO();
        result.setScenarioId(scenarioId);
        result.setRunnerType(RUNNER_TYPE);
        result.setStatus(outcome.status());
        result.setBlocking("blocker");
        result.setSummary(outcome.summary());
        result.setEvidenceFiles(List.of(markdownPath));

        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("layer", layer);
        details.putAll(outcome.details());
        result.setDetails(details);
        return result;
    }

    private CheckOutcome checkProtocolFamily(DeviceOnboardingAcceptanceRequest request) {
        IotProperties.Protocol.FamilyDefinition family =
                protocolSecurityDefinitionProvider.getFamilyDefinition(request.protocolFamilyCode());
        if (family == null) {
            return CheckOutcome.failed("未命中已发布协议族定义", detailMap("familyCode", request.protocolFamilyCode()));
        }
        String boundDecryptProfile = normalizeText(family.getDecryptProfileCode());
        String expectedDecryptProfile = normalizeText(request.decryptProfileCode());
        if (StringUtils.hasText(boundDecryptProfile)
                && StringUtils.hasText(expectedDecryptProfile)
                && !Objects.equals(boundDecryptProfile, expectedDecryptProfile)) {
            return CheckOutcome.failed(
                    "协议族绑定的解密档案与接入案例不一致",
                    detailMap("familyCode", request.protocolFamilyCode(), "boundDecryptProfileCode", boundDecryptProfile)
            );
        }
        return CheckOutcome.passed("协议族命中", detailMap("familyCode", request.protocolFamilyCode()));
    }

    private CheckOutcome checkDecryptProfile(DeviceOnboardingAcceptanceRequest request) {
        IotProperties.Protocol.DecryptProfile profile =
                protocolSecurityDefinitionProvider.getDecryptProfile(request.decryptProfileCode());
        if (profile == null) {
            return CheckOutcome.failed("未命中已发布解密档案", detailMap("decryptProfileCode", request.decryptProfileCode()));
        }
        return CheckOutcome.passed("解密档案命中", detailMap("decryptProfileCode", request.decryptProfileCode()));
    }

    private CheckOutcome checkTemplateReplay(DeviceOnboardingAcceptanceRequest request) {
        List<ProtocolTemplateDefinitionSnapshot> snapshots = protocolTemplateDefinitionSnapshotMapper.selectList(
                new LambdaQueryWrapper<ProtocolTemplateDefinitionSnapshot>()
                        .eq(ProtocolTemplateDefinitionSnapshot::getTemplateCode, request.protocolTemplateCode())
                        .eq(ProtocolTemplateDefinitionSnapshot::getLifecycleStatus, "PUBLISHED")
                        .eq(ProtocolTemplateDefinitionSnapshot::getDeleted, 0)
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getPublishedVersionNo)
                        .orderByDesc(ProtocolTemplateDefinitionSnapshot::getId)
        );
        if (snapshots == null || snapshots.isEmpty()) {
            return CheckOutcome.failed("未命中已发布协议模板快照", detailMap("templateCode", request.protocolTemplateCode()));
        }
        List<DeviceMessageLog> messageLogs = deviceMessageService.listMessageLogs(request.deviceCode());
        if (messageLogs == null || messageLogs.isEmpty()) {
            return CheckOutcome.failed("未找到设备最近上报报文，无法执行模板回放", detailMap("deviceCode", request.deviceCode()));
        }
        String payloadJson = normalizeText(messageLogs.get(0).getPayload());
        if (!StringUtils.hasText(payloadJson)) {
            return CheckOutcome.failed("最近上报报文为空，无法执行模板回放", detailMap("deviceCode", request.deviceCode()));
        }
        ProtocolTemplateReplayDTO dto = new ProtocolTemplateReplayDTO();
        dto.setTemplateCode(request.protocolTemplateCode());
        dto.setPayloadJson(payloadJson);
        ProtocolTemplateReplayVO replayVO = protocolTemplateReplayService.replay(dto);
        if (replayVO == null || !Boolean.TRUE.equals(replayVO.getMatched())) {
            String summary = replayVO == null ? "模板回放未命中" : firstNonBlank(replayVO.getSummary(), "模板回放未命中");
            return CheckOutcome.failed(summary, detailMap("templateCode", request.protocolTemplateCode()));
        }
        return CheckOutcome.passed(
                firstNonBlank(replayVO.getSummary(), "模板回放成功"),
                detailMap("templateCode", request.protocolTemplateCode())
        );
    }

    private CheckOutcome checkCompareSnapshot(DeviceOnboardingAcceptanceRequest request) {
        Long count = productContractReleaseSnapshotMapper.selectCount(
                new LambdaQueryWrapper<ProductContractReleaseSnapshot>()
                        .eq(ProductContractReleaseSnapshot::getProductId, request.productId())
                        .eq(ProductContractReleaseSnapshot::getBatchId, request.releaseBatchId())
                        .eq(ProductContractReleaseSnapshot::getDeleted, 0)
        );
        if (count == null || count <= 0L) {
            return CheckOutcome.failed("未找到 compare/apply 生成的正式合同快照", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        return CheckOutcome.passed("正式合同快照已发布", detailMap("releaseBatchId", request.releaseBatchId()));
    }

    private CheckOutcome checkMetricResolverSnapshot(DeviceOnboardingAcceptanceRequest request) {
        Long count = productMetricResolverSnapshotMapper.selectCount(
                new LambdaQueryWrapper<ProductMetricResolverSnapshot>()
                        .eq(ProductMetricResolverSnapshot::getProductId, request.productId())
                        .eq(ProductMetricResolverSnapshot::getReleaseBatchId, request.releaseBatchId())
                        .eq(ProductMetricResolverSnapshot::getDeleted, 0)
        );
        if (count == null || count <= 0L) {
            return CheckOutcome.failed("未找到正式指标解析快照", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        return CheckOutcome.passed("正式指标解析快照已发布", detailMap("releaseBatchId", request.releaseBatchId()));
    }

    private CheckOutcome checkContractRelease(DeviceOnboardingAcceptanceRequest request) {
        ProductContractReleaseBatch batch = productContractReleaseBatchMapper.selectById(request.releaseBatchId());
        if (batch == null || batch.getDeleted() != null && batch.getDeleted() == 1) {
            return CheckOutcome.failed("未找到正式合同发布批次", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        if (!Objects.equals(batch.getProductId(), request.productId())) {
            return CheckOutcome.failed("正式合同批次与接入案例产品不匹配", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        String releaseStatus = normalizeText(batch.getReleaseStatus()).toUpperCase(Locale.ROOT);
        if (!"PUBLISHED".equals(releaseStatus) && !"RELEASED".equals(releaseStatus)) {
            return CheckOutcome.failed("正式合同批次尚未发布", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        PublishedProductContractSnapshot snapshot = publishedProductContractSnapshotService.getRequiredSnapshot(request.productId());
        if (snapshot == null || snapshot.releaseBatchId() == null) {
            return CheckOutcome.failed("已发布合同快照不存在", detailMap("productId", request.productId()));
        }
        if (!Objects.equals(snapshot.releaseBatchId(), request.releaseBatchId())) {
            return CheckOutcome.failed("运行态正式合同快照未切换到当前批次", detailMap("releaseBatchId", request.releaseBatchId()));
        }
        return CheckOutcome.passed("正式合同已发布", detailMap("releaseBatchId", request.releaseBatchId()));
    }

    private CheckOutcome checkReadSide(DeviceOnboardingAcceptanceRequest request) {
        Device device = deviceService.getRequiredByCode(request.deviceCode());
        List<String> issues = new ArrayList<>();
        if (!Objects.equals(device.getProductId(), request.productId())) {
            issues.add("设备产品与接入案例不一致");
        }
        List<?> properties = deviceService.listProperties(request.deviceCode());
        if (properties == null || properties.isEmpty()) {
            issues.add("latest 属性为空");
        }
        Map<String, Object> latest = telemetryQueryService.getLatest(device.getId());
        if (latest == null || latest.isEmpty()) {
            issues.add("latest 时序快照为空");
        }
        List<String> recommendedMetrics = collectorChildInsightService.listRecommendedMetrics(request.productId());
        if (recommendedMetrics == null || recommendedMetrics.isEmpty()) {
            issues.add("对象洞察重点指标为空");
            recommendedMetrics = List.of();
        }
        TelemetryHistoryBatchRequest historyRequest = new TelemetryHistoryBatchRequest();
        historyRequest.setDeviceId(device.getId());
        historyRequest.setIdentifiers(recommendedMetrics);
        historyRequest.setRangeCode("1d");
        historyRequest.setFillPolicy("ZERO");
        TelemetryHistoryBatchResponse historyResponse = telemetryQueryService.getHistoryBatch(historyRequest);
        if (!hasHistoryPoints(historyResponse)) {
            issues.add("history 时序点为空");
        }
        if (!issues.isEmpty()) {
            return CheckOutcome.failed(
                    String.join("；", issues),
                    detailMap("deviceCode", request.deviceCode(), "deviceId", device.getId())
            );
        }
        return CheckOutcome.passed("latest/history/insight 读侧可用", detailMap("deviceCode", request.deviceCode(), "deviceId", device.getId()));
    }

    private CheckOutcome checkRiskBoundary(DeviceOnboardingAcceptanceRequest request) {
        PublishedProductContractSnapshot snapshot = publishedProductContractSnapshotService.getRequiredSnapshot(request.productId());
        List<RiskMetricCatalog> catalogs = riskMetricCatalogService.listEnabledByProduct(request.productId());
        if (catalogs == null || catalogs.isEmpty()) {
            return CheckOutcome.passed("风险目录边界符合正式合同", detailMap("productId", request.productId()));
        }
        List<String> outOfBoundaryIdentifiers = catalogs.stream()
                .map(RiskMetricCatalog::getContractIdentifier)
                .filter(StringUtils::hasText)
                .filter(identifier -> !snapshot.containsPublishedIdentifier(identifier))
                .distinct()
                .toList();
        if (!outOfBoundaryIdentifiers.isEmpty()) {
            return CheckOutcome.failed(
                    "风险目录存在超出正式合同边界的指标",
                    detailMap("identifiers", outOfBoundaryIdentifiers)
            );
        }
        return CheckOutcome.passed("风险目录边界符合正式合同", detailMap("productId", request.productId()));
    }

    private boolean hasHistoryPoints(TelemetryHistoryBatchResponse historyResponse) {
        if (historyResponse == null || historyResponse.getPoints() == null || historyResponse.getPoints().isEmpty()) {
            return false;
        }
        return historyResponse.getPoints().stream()
                .filter(Objects::nonNull)
                .map(TelemetryHistoryBatchSeries::getBuckets)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .anyMatch(bucket -> bucket != null && bucket.getValue() != null);
    }

    private AutomationResultRunDetailVO buildRunDetail(String runId,
                                                       DeviceOnboardingAcceptanceRequest request,
                                                       List<AutomationResultRunResultVO> results) {
        AutomationResultRunDetailVO detail = new AutomationResultRunDetailVO();
        detail.setRunId(runId);
        LinkedHashMap<String, Object> options = new LinkedHashMap<>();
        options.put("caseId", request.caseId());
        options.put("caseCode", request.caseCode());
        options.put("workflow", WORKFLOW_CODE);
        options.put("productId", request.productId());
        options.put("releaseBatchId", request.releaseBatchId());
        options.put("deviceCode", request.deviceCode());
        detail.setOptions(options);
        detail.setSummary(normalizeSummary(null, results));
        detail.setResults(results);
        return detail;
    }

    private void writeRunArtifacts(String runId, AutomationResultRunDetailVO detail) {
        try {
            Files.createDirectories(resultsDir);
            Path jsonFile = resultsDir.resolve("registry-run-" + runId + ".json");
            Path markdownFile = resultsDir.resolve("registry-run-" + runId + ".md");
            Files.writeString(
                    jsonFile,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detail),
                    StandardCharsets.UTF_8
            );
            Files.writeString(markdownFile, toMarkdown(detail), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BizException("写入接入验收结果失败: " + runId);
        }
    }

    private String toMarkdown(AutomationResultRunDetailVO detail) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 无代码接入标准验收\n\n");
        builder.append("- runId: ").append(detail.getRunId()).append('\n');
        builder.append("- workflow: ").append(String.valueOf(detail.getOptions().get("workflow"))).append('\n');
        builder.append("- caseCode: ").append(String.valueOf(detail.getOptions().get("caseCode"))).append('\n');
        builder.append("- summary: ")
                .append(detail.getSummary().getPassed())
                .append('/')
                .append(detail.getSummary().getTotal())
                .append(" checks passed\n\n");
        for (AutomationResultRunResultVO result : detail.getResults()) {
            builder.append("## ").append(result.getScenarioId()).append('\n');
            builder.append("- status: ").append(result.getStatus()).append('\n');
            builder.append("- layer: ").append(String.valueOf(result.getDetails().get("layer"))).append('\n');
            builder.append("- summary: ").append(result.getSummary()).append("\n\n");
        }
        return builder.toString();
    }

    private DeviceOnboardingAcceptanceProgress toProgress(String runId,
                                                          AutomationResultSummaryVO summary,
                                                          List<AutomationResultRunResultVO> results) {
        List<AutomationResultRunResultVO> normalizedResults = normalizeResults(results);
        LinkedHashSet<String> failedLayerSet = normalizedResults.stream()
                .filter(item -> !"passed".equalsIgnoreCase(normalizeText(item.getStatus())))
                .map(AutomationResultRunResultVO::getDetails)
                .filter(Objects::nonNull)
                .map(detail -> detail.get("layer"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> failedLayers = List.copyOf(failedLayerSet);
        return new DeviceOnboardingAcceptanceProgress(
                null,
                runId,
                resolveProgressStatus(normalizedResults),
                buildSummaryText(summary, failedLayers),
                failedLayers,
                "/automation-results?runId=" + runId
        );
    }

    private String resolveProgressStatus(List<AutomationResultRunResultVO> results) {
        boolean hasFailed = results.stream().anyMatch(item -> "failed".equalsIgnoreCase(normalizeText(item.getStatus())));
        if (hasFailed) {
            return "FAILED";
        }
        boolean hasBlocked = results.stream().anyMatch(item -> "blocked".equalsIgnoreCase(normalizeText(item.getStatus())));
        if (hasBlocked) {
            return "BLOCKED";
        }
        return "PASSED";
    }

    private String buildSummaryText(AutomationResultSummaryVO summary, List<String> failedLayers) {
        AutomationResultSummaryVO normalizedSummary = normalizeSummary(summary, List.<AutomationResultRunResultVO>of());
        StringBuilder builder = new StringBuilder();
        builder.append(normalizedSummary.getPassed()).append('/').append(normalizedSummary.getTotal()).append(" 检查项通过");
        if (failedLayers != null && !failedLayers.isEmpty()) {
            builder.append("，未通过层级: ").append(String.join("、", failedLayers));
        }
        return builder.toString();
    }

    private AutomationResultSummaryVO normalizeSummary(AutomationResultSummaryVO summary,
                                                       List<AutomationResultRunResultVO> results) {
        List<AutomationResultRunResultVO> normalizedResults = normalizeResults(results);
        int total = normalizedResults.size();
        int failed = (int) normalizedResults.stream()
                .filter(item -> !"passed".equalsIgnoreCase(normalizeText(item.getStatus())))
                .count();
        AutomationResultSummaryVO normalized = summary == null ? new AutomationResultSummaryVO() : summary;
        normalized.setTotal(normalized.getTotal() == null ? total : normalized.getTotal());
        normalized.setFailed(normalized.getFailed() == null ? failed : normalized.getFailed());
        normalized.setPassed(normalized.getPassed() == null
                ? Math.max(0, normalized.getTotal() - normalized.getFailed())
                : normalized.getPassed());
        return normalized;
    }

    private List<AutomationResultRunResultVO> normalizeResults(List<AutomationResultRunResultVO> results) {
        return results == null ? List.of() : results;
    }

    private String nextRunId() {
        String runId = RUN_ID_FORMATTER.format(LocalDateTime.now());
        Path existing = resultsDir.resolve("registry-run-" + runId + ".json");
        if (!Files.exists(existing)) {
            return runId;
        }
        return runId + System.currentTimeMillis() % 1000;
    }

    private void validateRequest(DeviceOnboardingAcceptanceRequest request) {
        if (request == null) {
            throw new BizException("接入验收请求不能为空");
        }
        requireText(request.caseCode(), "案例编码不能为空");
        requireText(request.deviceCode(), "验收设备编码不能为空");
        requireText(request.protocolFamilyCode(), "协议族编码不能为空");
        requireText(request.decryptProfileCode(), "解密档案编码不能为空");
        requireText(request.protocolTemplateCode(), "协议模板编码不能为空");
        if (request.productId() == null) {
            throw new BizException("产品不能为空");
        }
        if (request.releaseBatchId() == null) {
            throw new BizException("正式合同批次不能为空");
        }
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private LinkedHashMap<String, Object> detailMap(Object... keyValues) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        if (keyValues == null) {
            return details;
        }
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            Object value = keyValues[index + 1];
            if (key != null && value != null) {
                details.put(String.valueOf(key), value);
            }
        }
        return details;
    }

    @FunctionalInterface
    private interface CheckExecutor {

        CheckOutcome execute();
    }

    private record CheckOutcome(String status, String summary, LinkedHashMap<String, Object> details) {

        private static CheckOutcome passed(String summary, LinkedHashMap<String, Object> details) {
            return new CheckOutcome("passed", summary, details == null ? new LinkedHashMap<>() : details);
        }

        private static CheckOutcome failed(String summary, LinkedHashMap<String, Object> details) {
            return new CheckOutcome("failed", summary, details == null ? new LinkedHashMap<>() : details);
        }
    }
}
