package com.ghlzm.iot.report.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.report.service.BusinessAcceptanceService;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceLatestResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageModuleVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 业务验收配置与结果发现服务
 */
@Slf4j
@Service
public class BusinessAcceptanceServiceImpl implements BusinessAcceptanceService {

    private static final Pattern REGISTRY_RUN_FILE_PATTERN = Pattern.compile("^registry-run-(.+)\\.json$");
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path workspaceRoot;
    private final Path packagesConfigPath;
    private final Path acceptanceRegistryPath;
    private final Path resultsDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public BusinessAcceptanceServiceImpl(
            @Value("${iot.business-acceptance.workspace-root:.}") String workspaceRoot,
            @Value("${iot.business-acceptance.packages-config:config/automation/business-acceptance-packages.json}") String packagesConfigPath,
            @Value("${iot.automation.registry-path:config/automation/acceptance-registry.json}") String acceptanceRegistryPath,
            @Value("${iot.automation.results-dir:logs/acceptance}") String resultsDir
    ) {
        this(
                Paths.get(workspaceRoot),
                Paths.get(packagesConfigPath),
                Paths.get(acceptanceRegistryPath),
                Paths.get(resultsDir),
                JsonMapper.builder().findAndAddModules().build()
        );
    }

    BusinessAcceptanceServiceImpl(
            Path workspaceRoot,
            Path packagesConfigPath,
            Path acceptanceRegistryPath,
            Path resultsDir,
            ObjectMapper objectMapper
    ) {
        this.workspaceRoot = normalizePath(workspaceRoot, null);
        this.packagesConfigPath = normalizePath(packagesConfigPath, this.workspaceRoot);
        this.acceptanceRegistryPath = normalizePath(acceptanceRegistryPath, this.workspaceRoot);
        this.resultsDir = normalizePath(resultsDir, this.workspaceRoot);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<BusinessAcceptancePackageVO> listPackages() {
        LoadedDefinition definition = loadDefinition();
        return definition.getPackages().stream()
                .map(pkg -> toPackageVO(pkg, resolveLatestResult(pkg)))
                .toList();
    }

    @Override
    public List<BusinessAcceptanceAccountTemplateVO> listAccountTemplates() {
        LoadedDefinition definition = loadDefinition();
        return definition.getAccountTemplates().stream()
                .map(this::toAccountTemplateVO)
                .toList();
    }

    private BusinessAcceptancePackageVO toPackageVO(
            BusinessAcceptancePackageConfig pkg,
            BusinessAcceptanceLatestResultVO latestResult
    ) {
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

    private BusinessAcceptanceLatestResultVO resolveLatestResult(BusinessAcceptancePackageConfig pkg) {
        Optional<BusinessAcceptanceLedgerRun> latestRun = findLatestRun(pkg.getPackageCode());
        if (latestRun.isEmpty()) {
            BusinessAcceptanceLatestResultVO latestResult = new BusinessAcceptanceLatestResultVO();
            latestResult.setStatus("neverRun");
            latestResult.setPassedModuleCount(0);
            latestResult.setFailedModuleCount(0);
            latestResult.setFailedModuleNames(Collections.emptyList());
            return latestResult;
        }

        BusinessAcceptanceLedgerRun ledgerRun = latestRun.get();
        Set<String> selectedModuleCodes = resolveSelectedModuleCodes(
                ledgerRun.detail().getOptions(),
                pkg.getModules()
        );
        List<String> failedModuleNames = pkg.getModules().stream()
                .filter(module -> selectedModuleCodes.contains(module.getModuleCode()))
                .filter(module -> moduleFailed(module, ledgerRun.detail().getResults()))
                .map(BusinessAcceptanceModuleConfig::getModuleName)
                .toList();

        BusinessAcceptanceLatestResultVO latestResult = new BusinessAcceptanceLatestResultVO();
        latestResult.setRunId(resolveRunId(ledgerRun.detail(), ledgerRun.file()));
        latestResult.setStatus(resolveRunStatus(ledgerRun.detail().getSummary()));
        latestResult.setUpdatedAt(resolveUpdatedAt(ledgerRun.updatedAtEpochMillis()));
        latestResult.setFailedModuleNames(failedModuleNames);
        latestResult.setFailedModuleCount(failedModuleNames.size());
        latestResult.setPassedModuleCount(Math.max(0, selectedModuleCodes.size() - failedModuleNames.size()));
        return latestResult;
    }

    private boolean moduleFailed(
            BusinessAcceptanceModuleConfig module,
            List<AutomationResultRunResultVO> results
    ) {
        Set<String> failedScenarioIds = normalizeResults(results).stream()
                .filter(item -> !"passed".equalsIgnoreCase(defaultString(item.getStatus())))
                .map(AutomationResultRunResultVO::getScenarioId)
                .filter(StringUtils::hasText)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        return module.getScenarioRefs().stream().anyMatch(failedScenarioIds::contains);
    }

    private Optional<BusinessAcceptanceLedgerRun> findLatestRun(String packageCode) {
        if (!Files.isDirectory(resultsDir)) {
            return Optional.empty();
        }

        try (Stream<Path> stream = Files.list(resultsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isRegistryRunFile)
                    .map(this::safeReadLedgerRun)
                    .filter(Objects::nonNull)
                    .filter(item -> packageCode.equals(resolveOptionText(item.detail().getOptions(), "packageCode")))
                    .max(Comparator.comparingLong(BusinessAcceptanceLedgerRun::updatedAtEpochMillis));
        } catch (IOException e) {
            throw new BizException(500, "读取业务验收运行结果失败", e);
        }
    }

    private BusinessAcceptanceLedgerRun safeReadLedgerRun(Path file) {
        try {
            AutomationResultRunDetailVO detail = objectMapper.readValue(
                    Files.readString(file, StandardCharsets.UTF_8),
                    AutomationResultRunDetailVO.class
            );
            long updatedAt = Files.getLastModifiedTime(file).toMillis();
            return new BusinessAcceptanceLedgerRun(file, updatedAt, detail);
        } catch (IOException e) {
            log.warn("跳过无法解析的业务验收运行结果文件: {}", file, e);
            return null;
        }
    }

    private boolean isRegistryRunFile(Path file) {
        return REGISTRY_RUN_FILE_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private String resolveRunId(AutomationResultRunDetailVO detail, Path file) {
        if (StringUtils.hasText(detail.getRunId())) {
            return detail.getRunId().trim();
        }
        String fileName = file.getFileName().toString();
        return fileName.substring("registry-run-".length(), fileName.length() - ".json".length());
    }

    private String resolveUpdatedAt(long epochMillis) {
        return UPDATED_AT_FORMATTER.format(
                Instant.ofEpochMilli(epochMillis)
                        .atZone(ZoneId.systemDefault())
        );
    }

    private String resolveRunStatus(AutomationResultSummaryVO summary) {
        if (summary != null && defaultInteger(summary.getFailed(), 0) > 0) {
            return "failed";
        }
        return "passed";
    }

    private Set<String> resolveSelectedModuleCodes(
            Map<String, Object> options,
            List<BusinessAcceptanceModuleConfig> packageModules
    ) {
        LinkedHashSet<String> allowedModuleCodes = packageModules.stream()
                .map(BusinessAcceptanceModuleConfig::getModuleCode)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        LinkedHashSet<String> selected = new LinkedHashSet<>();

        Object rawSelectedModules = options == null ? null : options.get("selectedModules");
        if (rawSelectedModules instanceof String textValue) {
            for (String item : textValue.split(",")) {
                String normalized = normalizeText(item);
                if (allowedModuleCodes.contains(normalized)) {
                    selected.add(normalized);
                }
            }
        } else if (rawSelectedModules instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String normalized = normalizeText(item == null ? null : String.valueOf(item));
                if (allowedModuleCodes.contains(normalized)) {
                    selected.add(normalized);
                }
            }
        }

        if (selected.isEmpty()) {
            selected.addAll(allowedModuleCodes);
        }
        return selected;
    }

    private LoadedDefinition loadDefinition() {
        BusinessAcceptanceDefinitionDocument document = readPackagesDocument();
        Set<String> registryScenarioIds = readRegistryScenarioIds();

        LinkedHashMap<String, BusinessAcceptanceAccountTemplateConfig> accountTemplates = new LinkedHashMap<>();
        for (BusinessAcceptanceAccountTemplateConfig template : defaultList(document.getAccountTemplates())) {
            String templateCode = requireText(template.getTemplateCode(), "账号模板编码不能为空");
            if (accountTemplates.putIfAbsent(templateCode, template) != null) {
                throw new BizException(500, "业务验收账号模板编码重复: " + templateCode);
            }
            template.setTemplateCode(templateCode);
            template.setTemplateName(requireText(template.getTemplateName(), "账号模板名称不能为空"));
            template.setUsername(requireText(template.getUsername(), "账号模板用户名不能为空"));
            template.setRoleHint(normalizeText(template.getRoleHint()));
            template.setSupportedEnvironments(normalizeStringList(template.getSupportedEnvironments()));
        }

        LinkedHashMap<String, BusinessAcceptancePackageConfig> packages = new LinkedHashMap<>();
        for (BusinessAcceptancePackageConfig pkg : defaultList(document.getPackages())) {
            String packageCode = requireText(pkg.getPackageCode(), "业务验收包编码不能为空");
            if (packages.putIfAbsent(packageCode, pkg) != null) {
                throw new BizException(500, "业务验收包编码重复: " + packageCode);
            }

            pkg.setPackageCode(packageCode);
            pkg.setPackageName(requireText(pkg.getPackageName(), "业务验收包名称不能为空"));
            pkg.setDescription(normalizeText(pkg.getDescription()));
            pkg.setTargetRoles(normalizeStringList(pkg.getTargetRoles()));
            pkg.setSupportedEnvironments(normalizeStringList(pkg.getSupportedEnvironments()));
            pkg.setDefaultAccountTemplate(requireText(pkg.getDefaultAccountTemplate(), "默认账号模板不能为空"));

            if (!accountTemplates.containsKey(pkg.getDefaultAccountTemplate())) {
                throw new BizException(500, "业务验收包引用了未知账号模板: " + pkg.getDefaultAccountTemplate());
            }

            LinkedHashMap<String, BusinessAcceptanceModuleConfig> modules = new LinkedHashMap<>();
            for (BusinessAcceptanceModuleConfig module : defaultList(pkg.getModules())) {
                String moduleCode = requireText(module.getModuleCode(), "业务验收模块编码不能为空");
                if (modules.putIfAbsent(moduleCode, module) != null) {
                    throw new BizException(500, "业务验收包模块编码重复: " + moduleCode);
                }

                module.setModuleCode(moduleCode);
                module.setModuleName(requireText(module.getModuleName(), "业务验收模块名称不能为空"));
                module.setSuggestedDirection(normalizeText(module.getSuggestedDirection()));
                List<String> scenarioRefs = normalizeStringList(module.getScenarioRefs());
                if (scenarioRefs.isEmpty()) {
                    throw new BizException(500, "业务验收模块未配置场景引用: " + moduleCode);
                }
                for (String scenarioRef : scenarioRefs) {
                    if (!registryScenarioIds.contains(scenarioRef)) {
                        throw new BizException(500, "业务验收模块引用了未知场景: " + scenarioRef);
                    }
                }
                module.setScenarioRefs(scenarioRefs);
            }
            pkg.setModules(new ArrayList<>(modules.values()));
        }

        return new LoadedDefinition(
                new ArrayList<>(packages.values()),
                new ArrayList<>(accountTemplates.values())
        );
    }

    private BusinessAcceptanceDefinitionDocument readPackagesDocument() {
        if (!Files.isRegularFile(packagesConfigPath)) {
            throw new BizException(500, "未找到业务验收包配置: " + packagesConfigPath);
        }
        try {
            return objectMapper.readValue(
                    Files.readString(packagesConfigPath, StandardCharsets.UTF_8),
                    BusinessAcceptanceDefinitionDocument.class
            );
        } catch (IOException e) {
            throw new BizException(500, "读取业务验收包配置失败", e);
        }
    }

    private Set<String> readRegistryScenarioIds() {
        if (!Files.isRegularFile(acceptanceRegistryPath)) {
            throw new BizException(500, "未找到统一验收注册表: " + acceptanceRegistryPath);
        }
        try {
            JsonNode root = objectMapper.readTree(Files.readString(acceptanceRegistryPath, StandardCharsets.UTF_8));
            LinkedHashSet<String> scenarioIds = new LinkedHashSet<>();
            JsonNode scenarios = root.path("scenarios");
            if (!scenarios.isArray()) {
                return scenarioIds;
            }
            for (JsonNode scenario : scenarios) {
                String scenarioId = normalizeText(scenario.path("id").asText());
                if (!StringUtils.hasText(scenarioId)) {
                    throw new BizException(500, "统一验收注册表存在空场景编码");
                }
                scenarioIds.add(scenarioId);
            }
            return scenarioIds;
        } catch (IOException e) {
            throw new BizException(500, "读取统一验收注册表失败", e);
        }
    }

    private List<AutomationResultRunResultVO> normalizeResults(List<AutomationResultRunResultVO> results) {
        return results == null ? Collections.emptyList() : results;
    }

    private String resolveOptionText(Map<String, Object> options, String key) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return "";
        }
        return normalizeText(String.valueOf(options.get(key)));
    }

    private Path normalizePath(Path path, Path basePath) {
        Path resolved = path;
        if (basePath != null && !path.isAbsolute()) {
            resolved = basePath.resolve(path);
        }
        return resolved.toAbsolutePath().normalize();
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(500, message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .toList();
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private record LoadedDefinition(
            List<BusinessAcceptancePackageConfig> packages,
            List<BusinessAcceptanceAccountTemplateConfig> accountTemplates
    ) {
        List<BusinessAcceptancePackageConfig> getPackages() {
            return packages;
        }

        List<BusinessAcceptanceAccountTemplateConfig> getAccountTemplates() {
            return accountTemplates;
        }
    }

    private record BusinessAcceptanceLedgerRun(
            Path file,
            long updatedAtEpochMillis,
            AutomationResultRunDetailVO detail
    ) {
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
