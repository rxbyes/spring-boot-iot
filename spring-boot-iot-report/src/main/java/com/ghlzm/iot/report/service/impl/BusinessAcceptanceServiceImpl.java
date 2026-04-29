package com.ghlzm.iot.report.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.report.service.AutomationResultArchiveIndexService;
import com.ghlzm.iot.report.service.BusinessAcceptanceService;
import com.ghlzm.iot.report.vo.AutomationFailureDiagnosisVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveIndexVO;
import com.ghlzm.iot.report.vo.AutomationResultFailedScenarioVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceFailureDetailVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceLatestResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceModuleResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageModuleVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunLaunchVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunRequest;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunStatusVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class BusinessAcceptanceServiceImpl implements BusinessAcceptanceService {

    private static final Pattern REGISTRY_RUN_FILE_PATTERN = Pattern.compile("^registry-run-(.+)\\.json$");
    private static final Pattern RUN_ID_OUTPUT_PATTERN = Pattern.compile("\"runId\"\\s*:\\s*\"([^\"]+)\"");
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter RUN_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_DIAGNOSIS_EVIDENCE_LENGTH = 160;
    private static final List<String> ENVIRONMENT_BLOCK_KEYWORDS = List.of("econnrefused", "connection refused", "timed out", "timeout", "fetch failed", "login", "health", "unavailable");
    private static final List<String> DIAGNOSIS_CATEGORY_PRIORITY = List.of("权限", "环境", "接口", "UI", "数据", "断言", "其他");

    private final Path workspaceRoot;
    private final Path packagesConfigPath;
    private final Path acceptanceRegistryPath;
    private final Path resultsDir;
    private final Path registryRunnerScriptPath;
    private final String nodeCommand;
    private final ObjectMapper objectMapper;
    private final AutomationResultArchiveIndexService archiveIndexService;
    private final Map<String, JobState> jobStates = new ConcurrentHashMap<>();
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    @Autowired
    public BusinessAcceptanceServiceImpl(
            @Value("${iot.business-acceptance.workspace-root:.}") String workspaceRoot,
            @Value("${iot.business-acceptance.packages-config:config/automation/business-acceptance-packages.json}") String packagesConfigPath,
            @Value("${iot.automation.registry-path:config/automation/acceptance-registry.json}") String acceptanceRegistryPath,
            @Value("${iot.automation.results-dir:logs/acceptance}") String resultsDir,
            @Value("${iot.automation.node-command:node}") String nodeCommand,
            @Value("${iot.automation.registry-runner-script:scripts/auto/run-acceptance-registry.mjs}") String registryRunnerScriptPath
    ) {
        this(Paths.get(workspaceRoot), Paths.get(packagesConfigPath), Paths.get(acceptanceRegistryPath), Paths.get(resultsDir), nodeCommand, Paths.get(registryRunnerScriptPath), JsonMapper.builder().findAndAddModules().build());
    }

    BusinessAcceptanceServiceImpl(Path workspaceRoot, Path packagesConfigPath, Path acceptanceRegistryPath, Path resultsDir, ObjectMapper objectMapper) {
        this(workspaceRoot, packagesConfigPath, acceptanceRegistryPath, resultsDir, "node", Paths.get("scripts/auto/run-acceptance-registry.mjs"), objectMapper);
    }

    BusinessAcceptanceServiceImpl(Path workspaceRoot, Path packagesConfigPath, Path acceptanceRegistryPath, Path resultsDir, String nodeCommand, Path registryRunnerScriptPath, ObjectMapper objectMapper) {
        Path normalizedWorkspaceRoot = normalizePath(workspaceRoot, null);
        this.workspaceRoot = resolveWorkspaceRoot(normalizedWorkspaceRoot, packagesConfigPath, acceptanceRegistryPath);
        this.packagesConfigPath = normalizePath(packagesConfigPath, this.workspaceRoot);
        this.acceptanceRegistryPath = normalizePath(acceptanceRegistryPath, this.workspaceRoot);
        this.resultsDir = normalizePath(resultsDir, this.workspaceRoot);
        this.registryRunnerScriptPath = normalizePath(registryRunnerScriptPath, this.workspaceRoot);
        this.nodeCommand = normalizeText(nodeCommand);
        this.objectMapper = objectMapper;
        this.archiveIndexService = new AutomationResultArchiveIndexServiceImpl(this.resultsDir, this.objectMapper);
    }

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    @Override
    public List<BusinessAcceptancePackageVO> listPackages() {
        LoadedDefinition definition = loadDefinition();
        return definition.packages().stream().map(pkg -> toPackageVO(pkg, resolveLatestResult(pkg, definition))).toList();
    }

    @Override
    public List<BusinessAcceptanceAccountTemplateVO> listAccountTemplates() {
        return loadDefinition().accountTemplates().stream().map(this::toAccountTemplateVO).toList();
    }

    @Override
    public BusinessAcceptanceRunLaunchVO launchRun(BusinessAcceptanceRunRequest request) {
        LoadedDefinition definition = loadDefinition();
        BusinessAcceptancePackageConfig pkg = resolvePackage(definition, request == null ? null : request.getPackageCode());
        BusinessAcceptanceAccountTemplateConfig template = resolveAccountTemplate(definition, request == null ? null : request.getAccountTemplateCode());
        String environmentCode = requireText(request == null ? null : request.getEnvironmentCode(), "运行环境不能为空");
        validateEnvironment(pkg, template, environmentCode);
        LinkedHashSet<String> moduleCodes = resolveRequestedModuleCodes(request == null ? null : request.getModuleCodes(), pkg);

        String jobId = "ba-" + UUID.randomUUID();
        Path derivedRegistryPath = writeDerivedRegistry(jobId, pkg, moduleCodes, definition.registry());
        JobState state = new JobState();
        state.setJobId(jobId);
        state.setStatus("running");
        state.setStartedAt(currentTimestamp());
        jobStates.put(jobId, state);

        submitLaunch(jobId, derivedRegistryPath, buildLaunchCommand(derivedRegistryPath, pkg.getPackageCode(), environmentCode, template.getTemplateCode(), moduleCodes));
        recordLaunchBusinessEvent(jobId, pkg, template, environmentCode, moduleCodes);

        BusinessAcceptanceRunLaunchVO launchVO = new BusinessAcceptanceRunLaunchVO();
        launchVO.setJobId(jobId);
        launchVO.setStatus(state.getStatus());
        launchVO.setStartedAt(state.getStartedAt());
        return launchVO;
    }

    @Override
    public BusinessAcceptanceRunStatusVO getRunStatus(String jobId) {
        JobState state = jobStates.get(normalizeText(jobId));
        if (state == null) {
            throw new BizException(404, "未找到业务验收任务: " + jobId);
        }
        BusinessAcceptanceRunStatusVO statusVO = new BusinessAcceptanceRunStatusVO();
        statusVO.setJobId(state.getJobId());
        statusVO.setStatus(state.getStatus());
        statusVO.setRunId(state.getRunId());
        statusVO.setStartedAt(state.getStartedAt());
        statusVO.setFinishedAt(state.getFinishedAt());
        statusVO.setErrorMessage(state.getErrorMessage());
        return statusVO;
    }

    @Override
    public BusinessAcceptanceResultVO getRunResult(String packageCode, String runId) {
        LoadedDefinition definition = loadDefinition();
        BusinessAcceptancePackageConfig pkg = resolvePackage(definition, packageCode);
        LedgerRun ledgerRun = readLedgerRun(runId);
        return buildBusinessResult(pkg, ledgerRun, definition, findIndexedRun(resolveRunId(ledgerRun.detail(), ledgerRun.file())));
    }

    protected void submitLaunch(String jobId, Path derivedRegistryPath, List<String> command) {
        CompletableFuture.runAsync(() -> executeLaunch(jobId, derivedRegistryPath, command));
    }

    private void recordLaunchBusinessEvent(String jobId,
                                           BusinessAcceptancePackageConfig pkg,
                                           BusinessAcceptanceAccountTemplateConfig template,
                                           String environmentCode,
                                           Collection<String> moduleCodes) {
        BusinessEventLogRecord event = new BusinessEventLogRecord();
        event.setTenantId(1L);
        event.setTraceId(TraceContextHolder.currentOrCreate());
        event.setEventCode("acceptance.business_run.launched");
        event.setEventName("业务验收运行启动完成");
        event.setDomainCode("acceptance");
        event.setActionCode("launch_business_run");
        event.setObjectType("business_acceptance_run");
        event.setObjectId(jobId);
        event.setObjectName(pkg == null ? null : pkg.getPackageName());
        event.setResultStatus(ObservabilityEvidenceStatus.SUCCESS);
        event.setSourceType("BUSINESS_ACCEPTANCE");
        event.setEvidenceType("business_acceptance_job");
        event.setEvidenceId(jobId);
        event.setOccurredAt(LocalDateTime.now());
        event.getMetadata().putAll(buildLaunchMetadata(jobId, pkg, template, environmentCode, moduleCodes));
        evidenceRecorder.recordBusinessEvent(event);
    }

    private Map<String, Object> buildLaunchMetadata(String jobId,
                                                    BusinessAcceptancePackageConfig pkg,
                                                    BusinessAcceptanceAccountTemplateConfig template,
                                                    String environmentCode,
                                                    Collection<String> moduleCodes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", jobId);
        metadata.put("packageCode", pkg == null ? null : pkg.getPackageCode());
        metadata.put("packageName", pkg == null ? null : pkg.getPackageName());
        metadata.put("environmentCode", normalizeText(environmentCode));
        metadata.put("accountTemplateCode", template == null ? null : template.getTemplateCode());
        metadata.put("accountTemplateName", template == null ? null : template.getTemplateName());
        metadata.put("username", template == null ? null : template.getUsername());
        metadata.put("moduleCodes", moduleCodes == null ? List.of() : List.copyOf(moduleCodes));
        return metadata;
    }

    private void executeLaunch(String jobId, Path derivedRegistryPath, List<String> command) {
        JobState state = jobStates.get(jobId);
        if (state == null) {
            return;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workspaceRoot.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output;
            try (var inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            process.waitFor();
            state.setRunId(extractRunId(output));
            state.setFinishedAt(currentTimestamp());
            if (StringUtils.hasText(state.getRunId())) {
                state.setStatus("completed");
            } else {
                state.setStatus(classifyFailureStatus(output));
                state.setErrorMessage(trimErrorMessage(output));
            }
        } catch (Exception ex) {
            state.setFinishedAt(currentTimestamp());
            state.setStatus(classifyFailureStatus(ex.getMessage()));
            state.setErrorMessage(trimErrorMessage(ex.getMessage()));
            log.warn("业务验收任务执行失败: {}", jobId, ex);
        }
    }

    private BusinessAcceptancePackageVO toPackageVO(BusinessAcceptancePackageConfig pkg, BusinessAcceptanceLatestResultVO latestResult) {
        BusinessAcceptancePackageVO vo = new BusinessAcceptancePackageVO();
        vo.setPackageCode(pkg.getPackageCode());
        vo.setPackageName(pkg.getPackageName());
        vo.setDescription(pkg.getDescription());
        vo.setTargetRoles(normalizeStringList(pkg.getTargetRoles()));
        vo.setSupportedEnvironments(normalizeStringList(pkg.getSupportedEnvironments()));
        vo.setDefaultAccountTemplate(normalizeText(pkg.getDefaultAccountTemplate()));
        vo.setModules(pkg.getModules().stream().map(this::toModuleVO).toList());
        vo.setLatestResult(latestResult);
        return vo;
    }

    private BusinessAcceptancePackageModuleVO toModuleVO(BusinessAcceptanceModuleConfig module) {
        BusinessAcceptancePackageModuleVO vo = new BusinessAcceptancePackageModuleVO();
        vo.setModuleCode(module.getModuleCode());
        vo.setModuleName(module.getModuleName());
        vo.setSuggestedDirection(module.getSuggestedDirection());
        vo.setScenarioRefs(normalizeStringList(module.getScenarioRefs()));
        return vo;
    }

    private BusinessAcceptanceAccountTemplateVO toAccountTemplateVO(BusinessAcceptanceAccountTemplateConfig template) {
        BusinessAcceptanceAccountTemplateVO vo = new BusinessAcceptanceAccountTemplateVO();
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setUsername(template.getUsername());
        vo.setRoleHint(template.getRoleHint());
        vo.setSupportedEnvironments(normalizeStringList(template.getSupportedEnvironments()));
        return vo;
    }

    private LoadedDefinition loadDefinition() {
        RegistryDocument registry = readRegistryDocument();
        BusinessAcceptanceDefinitionDocument document = readDefinitionDocument();
        LinkedHashMap<String, BusinessAcceptanceAccountTemplateConfig> templateMap = new LinkedHashMap<>();
        for (BusinessAcceptanceAccountTemplateConfig template : defaultList(document.getAccountTemplates())) {
            String code = requireText(template.getTemplateCode(), "账号模板编码不能为空");
            if (templateMap.putIfAbsent(code, template) != null) {
                throw new BizException(500, "业务验收账号模板编码重复: " + code);
            }
            template.setTemplateCode(code);
            template.setTemplateName(requireText(template.getTemplateName(), "账号模板名称不能为空"));
            template.setUsername(requireText(template.getUsername(), "账号模板用户名不能为空"));
            template.setRoleHint(normalizeText(template.getRoleHint()));
            template.setSupportedEnvironments(normalizeStringList(template.getSupportedEnvironments()));
        }

        LinkedHashMap<String, BusinessAcceptancePackageConfig> packageMap = new LinkedHashMap<>();
        for (BusinessAcceptancePackageConfig pkg : defaultList(document.getPackages())) {
            String packageCode = requireText(pkg.getPackageCode(), "业务验收包编码不能为空");
            if (packageMap.putIfAbsent(packageCode, pkg) != null) {
                throw new BizException(500, "业务验收包编码重复: " + packageCode);
            }
            pkg.setPackageCode(packageCode);
            pkg.setPackageName(requireText(pkg.getPackageName(), "业务验收包名称不能为空"));
            pkg.setDescription(normalizeText(pkg.getDescription()));
            pkg.setTargetRoles(normalizeStringList(pkg.getTargetRoles()));
            pkg.setSupportedEnvironments(normalizeStringList(pkg.getSupportedEnvironments()));
            pkg.setDefaultAccountTemplate(requireText(pkg.getDefaultAccountTemplate(), "默认账号模板不能为空"));
            if (!templateMap.containsKey(pkg.getDefaultAccountTemplate())) {
                throw new BizException(500, "业务验收包引用了未知账号模板: " + pkg.getDefaultAccountTemplate());
            }

            LinkedHashMap<String, BusinessAcceptanceModuleConfig> moduleMap = new LinkedHashMap<>();
            for (BusinessAcceptanceModuleConfig module : defaultList(pkg.getModules())) {
                String moduleCode = requireText(module.getModuleCode(), "业务验收模块编码不能为空");
                if (moduleMap.putIfAbsent(moduleCode, module) != null) {
                    throw new BizException(500, "业务验收包模块编码重复: " + moduleCode);
                }
                module.setModuleCode(moduleCode);
                module.setModuleName(requireText(module.getModuleName(), "业务验收模块名称不能为空"));
                module.setSuggestedDirection(normalizeText(module.getSuggestedDirection()));
                module.setScenarioRefs(normalizeStringList(module.getScenarioRefs()));
                if (module.getScenarioRefs().isEmpty()) {
                    throw new BizException(500, "业务验收模块未配置场景引用: " + moduleCode);
                }
                module.getScenarioRefs().forEach(ref -> {
                    if (!registry.scenarios().containsKey(ref)) {
                        throw new BizException(500, "业务验收模块引用了未知场景: " + ref);
                    }
                });
            }
            pkg.setModules(new ArrayList<>(moduleMap.values()));
        }
        return new LoadedDefinition(new ArrayList<>(packageMap.values()), packageMap, new ArrayList<>(templateMap.values()), templateMap, registry);
    }

    private BusinessAcceptanceLatestResultVO resolveLatestResult(BusinessAcceptancePackageConfig pkg, LoadedDefinition definition) {
        Optional<LedgerRun> latestRun = findLatestRun(pkg.getPackageCode());
        if (latestRun.isEmpty()) {
            BusinessAcceptanceLatestResultVO latestResult = new BusinessAcceptanceLatestResultVO();
            latestResult.setStatus("neverRun");
            latestResult.setPassedModuleCount(0);
            latestResult.setFailedModuleCount(0);
            latestResult.setFailedModuleNames(Collections.emptyList());
            return latestResult;
        }
        LedgerRun ledgerRun = latestRun.get();
        BusinessAcceptanceResultVO result = buildBusinessResult(
                pkg,
                ledgerRun,
                definition,
                findIndexedRun(resolveRunId(ledgerRun.detail(), ledgerRun.file()))
        );
        BusinessAcceptanceLatestResultVO latestResult = new BusinessAcceptanceLatestResultVO();
        latestResult.setRunId(result.getRunId());
        latestResult.setStatus(result.getStatus());
        latestResult.setUpdatedAt(resolveUpdatedAt(latestRun.get().updatedAtEpochMillis()));
        latestResult.setPassedModuleCount(result.getPassedModuleCount());
        latestResult.setFailedModuleCount(result.getFailedModuleCount());
        latestResult.setFailedModuleNames(result.getFailedModuleNames());
        return latestResult;
    }

    private BusinessAcceptanceResultVO buildBusinessResult(
            BusinessAcceptancePackageConfig pkg,
            LedgerRun ledgerRun,
            LoadedDefinition definition,
            AutomationResultArchiveIndexVO.RunRecord indexedRun
    ) {
        Set<String> selectedModuleCodes = resolveSelectedModuleCodes(ledgerRun.detail().getOptions(), pkg.getModules());
        List<BusinessAcceptanceModuleResultVO> modules = pkg.getModules().stream()
                .filter(module -> selectedModuleCodes.contains(module.getModuleCode()))
                .map(module -> buildModuleResult(module, ledgerRun.detail(), definition, indexedRun))
                .toList();

        BusinessAcceptanceResultVO result = new BusinessAcceptanceResultVO();
        result.setRunId(resolveRunId(ledgerRun.detail(), ledgerRun.file()));
        result.setStatus(modules.stream().anyMatch(item -> "blocked".equals(item.getStatus())) ? "blocked"
                : modules.stream().anyMatch(item -> "failed".equals(item.getStatus())) ? "failed" : "passed");
        result.setPassedModuleCount((int) modules.stream().filter(item -> "passed".equals(item.getStatus())).count());
        result.setFailedModuleCount((int) modules.stream().filter(item -> !"passed".equals(item.getStatus())).count());
        result.setFailedModuleNames(modules.stream().filter(item -> !"passed".equals(item.getStatus())).map(BusinessAcceptanceModuleResultVO::getModuleName).toList());
        result.setDurationText(resolveDurationText(result.getRunId(), ledgerRun.updatedAtEpochMillis()));
        result.setJumpToAutomationResultsPath("/automation-governance?tab=evidence&runId=" + result.getRunId());
        result.setModules(modules);
        return result;
    }

    private BusinessAcceptanceModuleResultVO buildModuleResult(
            BusinessAcceptanceModuleConfig module,
            AutomationResultRunDetailVO detail,
            LoadedDefinition definition,
            AutomationResultArchiveIndexVO.RunRecord indexedRun
    ) {
        List<AutomationResultRunResultVO> relatedResults = normalizeResults(detail.getResults()).stream()
                .filter(item -> module.getScenarioRefs().contains(normalizeText(item.getScenarioId())))
                .toList();
        List<AutomationResultRunResultVO> failedResults = relatedResults.stream()
                .filter(item -> !"passed".equalsIgnoreCase(normalizeText(item.getStatus())))
                .toList();
        List<AutomationResultFailedScenarioVO> failedDiagnoses = resolveFailedScenarioDiagnoses(module, indexedRun);
        boolean blocked = relatedResults.isEmpty() || failedResults.stream().anyMatch(this::environmentFailureDetected);
        String status = blocked ? "blocked" : failedResults.isEmpty() ? "passed" : "failed";

        BusinessAcceptanceModuleResultVO moduleResult = new BusinessAcceptanceModuleResultVO();
        moduleResult.setModuleCode(module.getModuleCode());
        moduleResult.setModuleName(module.getModuleName());
        moduleResult.setStatus(status);
        moduleResult.setSuggestedDirection("blocked".equals(status) ? "environment" : StringUtils.hasText(module.getSuggestedDirection()) ? module.getSuggestedDirection() : "needsReview");
        moduleResult.setFailedScenarioTitles(failedResults.isEmpty() && !"passed".equals(status)
                ? module.getScenarioRefs().stream().map(scenarioId -> resolveScenarioTitle(definition, scenarioId)).toList()
                : failedResults.stream().map(item -> resolveScenarioTitle(definition, item.getScenarioId())).toList());
        moduleResult.setFailedScenarioCount("passed".equals(status) ? 0 : Math.max(1, moduleResult.getFailedScenarioTitles().size()));
        moduleResult.setDiagnosis(resolveModuleDiagnosis(module, failedResults, failedDiagnoses, status));
        moduleResult.setFailureDetails("passed".equals(status) ? Collections.emptyList() : buildFailureDetails(module, failedResults, definition, status));
        return moduleResult;
    }

    private List<BusinessAcceptanceFailureDetailVO> buildFailureDetails(BusinessAcceptanceModuleConfig module, List<AutomationResultRunResultVO> failedResults, LoadedDefinition definition, String moduleStatus) {
        if (failedResults.isEmpty()) {
            String scenarioId = module.getScenarioRefs().isEmpty() ? "" : module.getScenarioRefs().get(0);
            return List.of(buildFallbackFailure(module, scenarioId, definition, moduleStatus));
        }
        return failedResults.stream().map(item -> {
            BusinessAcceptanceFailureDetailVO failure = buildFallbackFailure(module, item.getScenarioId(), definition, moduleStatus);
            failure.setSummary(StringUtils.hasText(item.getSummary()) ? normalizeText(item.getSummary()) : failure.getSummary());
            if (item.getDetails() != null) {
                failure.setStepLabel(resolveDetailValue(item.getDetails(), "stepLabel", failure.getStepLabel()));
                failure.setApiRef(resolveDetailValue(item.getDetails(), "apiRef", failure.getApiRef()));
                failure.setPageAction(resolveDetailValue(item.getDetails(), "pageAction", failure.getPageAction()));
            }
            return failure;
        }).toList();
    }

    private List<AutomationResultFailedScenarioVO> resolveFailedScenarioDiagnoses(
            BusinessAcceptanceModuleConfig module,
            AutomationResultArchiveIndexVO.RunRecord indexedRun
    ) {
        if (indexedRun == null || indexedRun.getFailedScenarios() == null) {
            return Collections.emptyList();
        }
        return indexedRun.getFailedScenarios().stream()
                .filter(item -> module.getScenarioRefs().contains(normalizeText(item.getScenarioId())))
                .toList();
    }

    private AutomationFailureDiagnosisVO resolveModuleDiagnosis(
            BusinessAcceptanceModuleConfig module,
            List<AutomationResultRunResultVO> failedResults,
            List<AutomationResultFailedScenarioVO> failedDiagnoses,
            String moduleStatus
    ) {
        if ("passed".equals(moduleStatus)) {
            return null;
        }
        if (!failedDiagnoses.isEmpty()) {
            return aggregateModuleDiagnosis(failedDiagnoses);
        }

        AutomationFailureDiagnosisVO diagnosis = new AutomationFailureDiagnosisVO();
        if ("blocked".equals(moduleStatus)) {
            diagnosis.setCategory("环境");
            diagnosis.setReason("未读取到有效运行结果，疑似环境阻塞");
        } else {
            diagnosis.setCategory("其他");
            diagnosis.setReason("未命中已知规则，建议查看原始证据");
        }
        String evidenceSummary = failedResults.stream()
                .map(AutomationResultRunResultVO::getSummary)
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> module.getFallbackFailure() == null ? "" : normalizeText(module.getFallbackFailure().getSummary()));
        diagnosis.setEvidenceSummary(StringUtils.hasText(evidenceSummary) ? truncateDiagnosisText(evidenceSummary) : "未记录证据摘要");
        return diagnosis;
    }

    private AutomationFailureDiagnosisVO aggregateModuleDiagnosis(List<AutomationResultFailedScenarioVO> failedDiagnoses) {
        LinkedHashMap<String, Integer> countsByCategory = new LinkedHashMap<>();
        failedDiagnoses.forEach(item -> {
            String category = normalizeText(item.getDiagnosis() == null ? null : item.getDiagnosis().getCategory());
            String resolvedCategory = StringUtils.hasText(category) ? category : "其他";
            countsByCategory.put(resolvedCategory, countsByCategory.getOrDefault(resolvedCategory, 0) + 1);
        });
        String primaryCategory = countsByCategory.keySet().stream()
                .sorted((left, right) -> {
                    int countDiff = countsByCategory.getOrDefault(right, 0) - countsByCategory.getOrDefault(left, 0);
                    return countDiff != 0 ? countDiff : compareDiagnosisCategoryPriority(left, right);
                })
                .findFirst()
                .orElse("其他");
        int primaryCount = countsByCategory.getOrDefault(primaryCategory, 0);
        List<String> extraCategories = countsByCategory.entrySet().stream()
                .filter(item -> !item.getKey().equals(primaryCategory))
                .map(item -> item.getValue() + " 个 " + item.getKey() + " 问题")
                .toList();

        AutomationFailureDiagnosisVO diagnosis = new AutomationFailureDiagnosisVO();
        diagnosis.setCategory(primaryCategory);
        diagnosis.setReason(extraCategories.isEmpty()
                ? failedDiagnoses.size() + " 个失败场景中 " + primaryCount + " 个命中" + primaryCategory + "问题"
                : failedDiagnoses.size() + " 个失败场景中 " + primaryCount + " 个命中" + primaryCategory + "问题，另有 " + String.join("、", extraCategories));
        diagnosis.setEvidenceSummary(truncateDiagnosisText(String.join("；", uniqueDiagnosisParts(
                failedDiagnoses.stream()
                        .map(item -> item.getDiagnosis() == null ? null : item.getDiagnosis().getEvidenceSummary())
                        .limit(2)
                        .toList()
        ))));
        if (!StringUtils.hasText(diagnosis.getEvidenceSummary())) {
            diagnosis.setEvidenceSummary("未记录证据摘要");
        }
        return diagnosis;
    }

    private BusinessAcceptanceFailureDetailVO buildFallbackFailure(BusinessAcceptanceModuleConfig module, String scenarioId, LoadedDefinition definition, String moduleStatus) {
        BusinessAcceptanceFailureDetailVO failure = new BusinessAcceptanceFailureDetailVO();
        failure.setScenarioId(normalizeText(scenarioId));
        failure.setScenarioTitle(resolveScenarioTitle(definition, scenarioId));
        failure.setStepLabel(module.getFallbackFailure() == null ? "" : normalizeText(module.getFallbackFailure().getStepLabel()));
        failure.setApiRef(module.getFallbackFailure() == null ? "" : normalizeText(module.getFallbackFailure().getApiRef()));
        failure.setPageAction(module.getFallbackFailure() == null ? "" : normalizeText(module.getFallbackFailure().getPageAction()));
        String summary = module.getFallbackFailure() == null ? "" : normalizeText(module.getFallbackFailure().getSummary());
        failure.setSummary(StringUtils.hasText(summary) ? summary : ("blocked".equals(moduleStatus) ? "环境阻塞，未读取到有效运行结果。" : "业务验收未通过，待研发或测试复核。"));
        return failure;
    }

    private String resolveScenarioTitle(LoadedDefinition definition, String scenarioId) {
        RegistryScenario scenario = definition.registry().scenarios().get(normalizeText(scenarioId));
        return scenario == null ? normalizeText(scenarioId) : scenario.title();
    }

    private String resolveDetailValue(Map<String, Object> details, String key, String fallback) {
        Object value = details.get(key);
        return value == null ? fallback : normalizeText(String.valueOf(value));
    }

    private boolean environmentFailureDetected(AutomationResultRunResultVO result) {
        String haystack = (normalizeText(result.getSummary()) + " " + String.valueOf(result.getDetails())).toLowerCase(Locale.ROOT);
        return ENVIRONMENT_BLOCK_KEYWORDS.stream().anyMatch(haystack::contains);
    }

    private BusinessAcceptancePackageConfig resolvePackage(LoadedDefinition definition, String packageCode) {
        String normalized = requireText(packageCode, "业务验收包不能为空");
        BusinessAcceptancePackageConfig pkg = definition.packageMap().get(normalized);
        if (pkg == null) {
            throw new BizException(404, "未找到业务验收包: " + packageCode);
        }
        return pkg;
    }

    private BusinessAcceptanceAccountTemplateConfig resolveAccountTemplate(LoadedDefinition definition, String accountTemplateCode) {
        String normalized = requireText(accountTemplateCode, "账号模板不能为空");
        BusinessAcceptanceAccountTemplateConfig template = definition.accountTemplateMap().get(normalized);
        if (template == null) {
            throw new BizException(404, "未找到账号模板: " + accountTemplateCode);
        }
        return template;
    }

    private void validateEnvironment(BusinessAcceptancePackageConfig pkg, BusinessAcceptanceAccountTemplateConfig template, String environmentCode) {
        if (!pkg.getSupportedEnvironments().contains(environmentCode)) {
            throw new BizException(400, "业务验收包不支持当前环境: " + environmentCode);
        }
        if (!template.getSupportedEnvironments().contains(environmentCode)) {
            throw new BizException(400, "账号模板不支持当前环境: " + environmentCode);
        }
    }

    private LinkedHashSet<String> resolveRequestedModuleCodes(List<String> requestedModuleCodes, BusinessAcceptancePackageConfig pkg) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (requestedModuleCodes == null || requestedModuleCodes.isEmpty()) {
            pkg.getModules().forEach(module -> selected.add(module.getModuleCode()));
            return selected;
        }
        Set<String> allowed = pkg.getModules().stream().map(BusinessAcceptanceModuleConfig::getModuleCode).collect(LinkedHashSet::new, Set::add, Set::addAll);
        for (String moduleCode : requestedModuleCodes) {
            String normalized = normalizeText(moduleCode);
            if (!allowed.contains(normalized)) {
                throw new BizException(400, "业务验收模块不属于当前验收包: " + moduleCode);
            }
            selected.add(normalized);
        }
        return selected;
    }

    private Set<String> resolveSelectedModuleCodes(Map<String, Object> options, List<BusinessAcceptanceModuleConfig> packageModules) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        Object rawSelectedModules = options == null ? null : options.get("selectedModules");
        if (rawSelectedModules instanceof String textValue) {
            for (String item : textValue.split(",")) {
                String normalized = normalizeText(item);
                if (StringUtils.hasText(normalized)) {
                    selected.add(normalized);
                }
            }
        } else if (rawSelectedModules instanceof Collection<?> collection) {
            for (Object item : collection) {
                String normalized = normalizeText(item == null ? null : String.valueOf(item));
                if (StringUtils.hasText(normalized)) {
                    selected.add(normalized);
                }
            }
        }
        if (selected.isEmpty()) {
            packageModules.forEach(module -> selected.add(module.getModuleCode()));
        }
        return selected;
    }

    private Path writeDerivedRegistry(String jobId, BusinessAcceptancePackageConfig pkg, LinkedHashSet<String> moduleCodes, RegistryDocument registry) {
        try {
            Path derivedDir = Files.createDirectories(resultsDir.resolve("derived-registries"));
            Path derivedRegistryPath = derivedDir.resolve("business-acceptance-" + jobId + ".json").normalize();
            LinkedHashMap<String, RegistryScenario> orderedScenarios = new LinkedHashMap<>();
            LinkedHashSet<String> visiting = new LinkedHashSet<>();
            pkg.getModules().stream()
                    .filter(module -> moduleCodes.contains(module.getModuleCode()))
                    .forEach(module -> module.getScenarioRefs().forEach(scenarioId ->
                            appendScenario(registry, scenarioId, orderedScenarios, visiting)
                    ));

            ObjectNode root = objectMapper.createObjectNode();
            root.put("version", StringUtils.hasText(registry.version()) ? registry.version() : "1.0.0");
            root.put("generatedAt", currentTimestamp());
            root.set("defaultTarget", registry.defaultTarget() == null ? objectMapper.createObjectNode() : registry.defaultTarget().deepCopy());

            ArrayNode scenariosNode = objectMapper.createArrayNode();
            orderedScenarios.values().forEach(item -> scenariosNode.add(item.source().deepCopy()));
            root.set("scenarios", scenariosNode);

            Files.writeString(
                    derivedRegistryPath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                    StandardCharsets.UTF_8
            );
            return derivedRegistryPath;
        } catch (IOException ex) {
            throw new BizException(500, "Failed to write derived registry", ex);
        }
    }

    private void appendScenario(
            RegistryDocument registry,
            String scenarioId,
            Map<String, RegistryScenario> orderedScenarios,
            Set<String> visiting
    ) {
        String normalized = normalizeText(scenarioId);
        if (orderedScenarios.containsKey(normalized)) {
            return;
        }
        if (!visiting.add(normalized)) {
            throw new BizException(500, "Registry dependency cycle detected: " + normalized);
        }
        RegistryScenario scenario = registry.scenarios().get(normalized);
        if (scenario == null) {
            throw new BizException(500, "Registry scenario not found: " + normalized);
        }
        for (String dependencyId : scenario.dependsOn()) {
            appendScenario(registry, dependencyId, orderedScenarios, visiting);
        }
        visiting.remove(normalized);
        orderedScenarios.put(normalized, scenario);
    }

    private List<String> buildLaunchCommand(
            Path derivedRegistryPath,
            String packageCode,
            String environmentCode,
            String accountTemplateCode,
            LinkedHashSet<String> moduleCodes
    ) {
        List<String> command = new ArrayList<>();
        command.add(StringUtils.hasText(nodeCommand) ? nodeCommand : "node");
        command.add(registryRunnerScriptPath.toString());
        command.add("--registry-path=" + derivedRegistryPath);
        command.add("--package-code=" + packageCode);
        command.add("--environment-code=" + environmentCode);
        command.add("--account-template=" + accountTemplateCode);
        command.add("--selected-modules=" + String.join(",", moduleCodes));
        return command;
    }

    private RegistryDocument readRegistryDocument() {
        try {
            JsonNode root = objectMapper.readTree(Files.readString(acceptanceRegistryPath, StandardCharsets.UTF_8));
            if (root == null || !root.isObject()) {
                throw new BizException(500, "Invalid acceptance registry document");
            }
            LinkedHashMap<String, RegistryScenario> scenarios = new LinkedHashMap<>();
            JsonNode scenarioArray = root.get("scenarios");
            if (scenarioArray != null && scenarioArray.isArray()) {
                for (JsonNode scenarioNode : scenarioArray) {
                    String scenarioId = requireText(readText(scenarioNode.get("id")), "Registry scenario id cannot be empty");
                    if (scenarios.containsKey(scenarioId)) {
                        throw new BizException(500, "Duplicate registry scenario id: " + scenarioId);
                    }
                    List<String> dependsOn = new ArrayList<>();
                    JsonNode dependsOnNode = scenarioNode.get("dependsOn");
                    if (dependsOnNode != null && dependsOnNode.isArray()) {
                        dependsOnNode.forEach(depNode -> {
                            String depId = normalizeText(readText(depNode));
                            if (StringUtils.hasText(depId)) {
                                dependsOn.add(depId);
                            }
                        });
                    }
                    scenarios.put(
                            scenarioId,
                            new RegistryScenario(
                                    scenarioId,
                                    normalizeText(readText(scenarioNode.get("title"))),
                                    scenarioNode.deepCopy(),
                                    List.copyOf(dependsOn)
                            )
                    );
                }
            }
            return new RegistryDocument(
                    normalizeText(readText(root.get("version"))),
                    normalizeText(readText(root.get("generatedAt"))),
                    root.get("defaultTarget") == null ? objectMapper.createObjectNode() : root.get("defaultTarget").deepCopy(),
                    scenarios
            );
        } catch (IOException ex) {
            throw new BizException(500, "Failed to read acceptance registry", ex);
        }
    }

    private BusinessAcceptanceDefinitionDocument readDefinitionDocument() {
        try {
            return objectMapper.readValue(
                    Files.readString(packagesConfigPath, StandardCharsets.UTF_8),
                    BusinessAcceptanceDefinitionDocument.class
            );
        } catch (IOException ex) {
            throw new BizException(500, "Failed to read business acceptance config", ex);
        }
    }

    private AutomationResultArchiveIndexVO.RunRecord findIndexedRun(String runId) {
        if (!StringUtils.hasText(runId)) {
            return null;
        }
        AutomationResultArchiveIndexVO.RunRecord indexedRun = archiveIndexService.loadArchiveIndex(false)
                .getRuns()
                .stream()
                .filter(item -> runId.equals(item.getRunId()))
                .findFirst()
                .orElse(null);
        if (indexedRun != null && indexedRun.getFailureSummary() != null) {
            return indexedRun;
        }
        return archiveIndexService.loadArchiveIndex(true)
                .getRuns()
                .stream()
                .filter(item -> runId.equals(item.getRunId()))
                .findFirst()
                .orElse(indexedRun);
    }

    private Optional<LedgerRun> findLatestRun(String packageCode) {
        if (!Files.isDirectory(resultsDir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(resultsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isRegistryRunFile)
                    .map(this::safeReadLedgerRun)
                    .filter(java.util.Objects::nonNull)
                    .filter(item -> packageCode.equals(normalizeText(String.valueOf(item.detail().getOptions().get("packageCode")))))
                    .sorted(Comparator.comparingLong(LedgerRun::updatedAtEpochMillis).reversed())
                    .findFirst();
        } catch (IOException ex) {
            throw new BizException(500, "Failed to scan acceptance results", ex);
        }
    }

    private LedgerRun safeReadLedgerRun(Path file) {
        try {
            return readLedgerRun(file);
        } catch (RuntimeException ex) {
            log.warn("Skip unreadable acceptance ledger file: {}", file, ex);
            return null;
        }
    }

    private LedgerRun readLedgerRun(String runId) {
        String normalizedRunId = requireText(runId, "runId cannot be empty");
        Path file = resultsDir.resolve("registry-run-" + normalizedRunId + ".json").normalize();
        if (!file.startsWith(resultsDir) || !Files.isRegularFile(file)) {
            throw new BizException(404, "Acceptance run not found: " + normalizedRunId);
        }
        return readLedgerRun(file);
    }

    private LedgerRun readLedgerRun(Path file) {
        try {
            AutomationResultRunDetailVO detail = objectMapper.readValue(
                    Files.readString(file, StandardCharsets.UTF_8),
                    AutomationResultRunDetailVO.class
            );
            detail.setRunId(resolveRunId(detail, file));
            detail.setOptions(detail.getOptions() == null ? Collections.emptyMap() : detail.getOptions());
            detail.setResults(normalizeResults(detail.getResults()));
            return new LedgerRun(file, Files.getLastModifiedTime(file).toMillis(), detail);
        } catch (IOException ex) {
            throw new BizException(500, "Failed to read acceptance run: " + file.getFileName(), ex);
        }
    }

    private String resolveUpdatedAt(long updatedAtEpochMillis) {
        return UPDATED_AT_FORMATTER.format(Instant.ofEpochMilli(updatedAtEpochMillis).atZone(ZoneId.systemDefault()));
    }

    private String resolveRunId(AutomationResultRunDetailVO detail, Path file) {
        if (StringUtils.hasText(detail.getRunId())) {
            return detail.getRunId().trim();
        }
        return resolveRunIdFromFileName(file).orElse(file.getFileName().toString());
    }

    private Optional<String> resolveRunIdFromFileName(Path file) {
        Matcher matcher = REGISTRY_RUN_FILE_PATTERN.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private String resolveDurationText(String runId, long updatedAtEpochMillis) {
        if (!StringUtils.hasText(runId)) {
            return "unknown";
        }
        try {
            long startedAtMillis = LocalDateTime.parse(runId.trim(), RUN_ID_FORMATTER)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            long durationSeconds = Math.max(0L, (updatedAtEpochMillis - startedAtMillis) / 1000L);
            long hours = durationSeconds / 3600L;
            long minutes = (durationSeconds % 3600L) / 60L;
            long seconds = durationSeconds % 60L;
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            }
            if (minutes > 0) {
                return minutes + "m " + seconds + "s";
            }
            return seconds + "s";
        } catch (DateTimeParseException ex) {
            return "unknown";
        }
    }

    private String currentTimestamp() {
        return UPDATED_AT_FORMATTER.format(Instant.now().atZone(ZoneId.systemDefault()));
    }

    private String extractRunId(String output) {
        if (!StringUtils.hasText(output)) {
            return "";
        }
        Matcher matcher = RUN_ID_OUTPUT_PATTERN.matcher(output);
        if (matcher.find()) {
            return normalizeText(matcher.group(1));
        }
        return "";
    }

    private String classifyFailureStatus(String message) {
        String normalized = normalizeText(message).toLowerCase(Locale.ROOT);
        return ENVIRONMENT_BLOCK_KEYWORDS.stream().anyMatch(normalized::contains) ? "blocked" : "failed";
    }

    private String trimErrorMessage(String message) {
        String normalized = normalizeText(message).replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH - 3) + "...";
    }

    private List<AutomationResultRunResultVO> normalizeResults(List<AutomationResultRunResultVO> results) {
        if (results == null) {
            return Collections.emptyList();
        }
        results.forEach(item -> {
            if (item.getEvidenceFiles() == null) {
                item.setEvidenceFiles(Collections.emptyList());
            } else {
                item.setEvidenceFiles(item.getEvidenceFiles().stream()
                        .map(this::normalizeText)
                        .filter(StringUtils::hasText)
                        .toList());
            }
        });
        return results;
    }

    private boolean isRegistryRunFile(Path file) {
        return REGISTRY_RUN_FILE_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private String readText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText();
    }

    private Path normalizePath(Path path, Path basePath) {
        Path resolved = path;
        if (basePath != null && !path.isAbsolute()) {
            resolved = basePath.resolve(path);
        }
        return resolved.toAbsolutePath().normalize();
    }

    private Path resolveWorkspaceRoot(Path configuredWorkspaceRoot, Path packagesConfigPath, Path acceptanceRegistryPath) {
        Path candidate = configuredWorkspaceRoot;
        while (candidate != null) {
            if (matchesWorkspacePath(candidate, packagesConfigPath) && matchesWorkspacePath(candidate, acceptanceRegistryPath)) {
                if (!candidate.equals(configuredWorkspaceRoot)) {
                    log.info("Adjusted business acceptance workspace root from {} to {}", configuredWorkspaceRoot, candidate);
                }
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return configuredWorkspaceRoot;
    }

    private boolean matchesWorkspacePath(Path workspaceRoot, Path path) {
        if (path == null || path.isAbsolute()) {
            return true;
        }
        return Files.exists(workspaceRoot.resolve(path));
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private int compareDiagnosisCategoryPriority(String left, String right) {
        int leftIndex = DIAGNOSIS_CATEGORY_PRIORITY.indexOf(left);
        int rightIndex = DIAGNOSIS_CATEGORY_PRIORITY.indexOf(right);
        if (leftIndex < 0) {
            leftIndex = DIAGNOSIS_CATEGORY_PRIORITY.size();
        }
        if (rightIndex < 0) {
            rightIndex = DIAGNOSIS_CATEGORY_PRIORITY.size();
        }
        return Integer.compare(leftIndex, rightIndex);
    }

    private List<String> uniqueDiagnosisParts(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream().map(this::normalizeText).filter(StringUtils::hasText).forEach(normalized::add);
        return List.copyOf(normalized);
    }

    private String truncateDiagnosisText(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized) || normalized.length() <= MAX_DIAGNOSIS_EVIDENCE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_DIAGNOSIS_EVIDENCE_LENGTH - 1) + "...";
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().map(this::normalizeText).filter(StringUtils::hasText).toList();
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(500, message);
        }
        return normalized;
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private record LoadedDefinition(
            List<BusinessAcceptancePackageConfig> packages,
            Map<String, BusinessAcceptancePackageConfig> packageMap,
            List<BusinessAcceptanceAccountTemplateConfig> accountTemplates,
            Map<String, BusinessAcceptanceAccountTemplateConfig> accountTemplateMap,
            RegistryDocument registry
    ) {
    }

    private record RegistryDocument(String version, String generatedAt, JsonNode defaultTarget, Map<String, RegistryScenario> scenarios) {
    }

    private record RegistryScenario(String id, String title, JsonNode source, List<String> dependsOn) {
    }

    private record LedgerRun(Path file, long updatedAtEpochMillis, AutomationResultRunDetailVO detail) {
    }

    @Data
    private static class JobState {
        private String jobId;
        private String status;
        private String runId;
        private String startedAt;
        private String finishedAt;
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BusinessAcceptanceDefinitionDocument {
        private List<BusinessAcceptancePackageConfig> packages;
        private List<BusinessAcceptanceAccountTemplateConfig> accountTemplates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BusinessAcceptancePackageConfig {
        private String packageCode;
        private String packageName;
        private String description;
        private List<String> targetRoles;
        private List<String> supportedEnvironments;
        private String defaultAccountTemplate;
        private List<BusinessAcceptanceModuleConfig> modules;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BusinessAcceptanceModuleConfig {
        private String moduleCode;
        private String moduleName;
        private List<String> scenarioRefs;
        private String suggestedDirection;
        private BusinessAcceptanceFallbackFailureConfig fallbackFailure;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BusinessAcceptanceFallbackFailureConfig {
        private String stepLabel;
        private String apiRef;
        private String pageAction;
        private String summary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BusinessAcceptanceAccountTemplateConfig {
        private String templateCode;
        private String templateName;
        private String username;
        private String roleHint;
        private List<String> supportedEnvironments;
    }
}
