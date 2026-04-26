package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.report.service.AutomationResultArchiveIndexService;
import com.ghlzm.iot.report.vo.AutomationFailureDiagnosisVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveFacetVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveIndexVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveRefreshVO;
import com.ghlzm.iot.report.vo.AutomationResultFailedModuleVO;
import com.ghlzm.iot.report.vo.AutomationResultFailedScenarioVO;
import com.ghlzm.iot.report.vo.AutomationResultFailureSummaryVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 自动化结果归档索引服务。
 */
@Slf4j
@Service
public class AutomationResultArchiveIndexServiceImpl implements AutomationResultArchiveIndexService {

    private static final Pattern REGISTRY_RUN_FILE_PATTERN = Pattern.compile("^registry-run-(.+)\\.json$");
    private static final String ACCEPTANCE_PREFIX = "logs/acceptance/";
    private static final String LATEST_INDEX_FILE_NAME = "automation-result-index.latest.json";
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<String> CATEGORY_PRIORITY = List.of("权限", "环境", "接口", "UI", "数据", "断言", "其他");
    private static final int MAX_EVIDENCE_SUMMARY_LENGTH = 160;

    private final Path resultsDir;
    private final ObjectMapper objectMapper;

    AutomationResultArchiveIndexServiceImpl(Path resultsDir, ObjectMapper objectMapper) {
        this.resultsDir = resultsDir.toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @Override
    public AutomationResultArchiveIndexVO loadArchiveIndex(boolean forceRefresh) {
        if (!Files.isDirectory(resultsDir)) {
            return emptyIndex();
        }
        if (forceRefresh || isLatestIndexMissingOrStale()) {
            return buildAndWriteLatestIndex();
        }
        try {
            AutomationResultArchiveIndexVO index = objectMapper.readValue(
                    Files.readString(resolveLatestIndexFile(), StandardCharsets.UTF_8),
                    AutomationResultArchiveIndexVO.class
            );
            return requiresDiagnosisBackfill(index) ? buildAndWriteLatestIndex() : index;
        } catch (IOException ex) {
            log.warn("自动化结果归档索引读取失败，改为重建 latest 索引。", ex);
            return buildAndWriteLatestIndex();
        }
    }

    @Override
    public AutomationResultArchiveFacetVO listFacets() {
        AutomationResultArchiveIndexVO index = loadArchiveIndex(false);
        return index.getFacets() == null ? createEmptyFacets() : index.getFacets();
    }

    @Override
    public AutomationResultArchiveRefreshVO refreshIndex() {
        AutomationResultArchiveIndexVO index = buildAndWriteLatestIndex();
        AutomationResultArchiveRefreshVO refresh = new AutomationResultArchiveRefreshVO();
        refresh.setGeneratedAt(index.getGeneratedAt());
        refresh.setLatestIndexPath(toDisplayPath(resolveLatestIndexFile()));
        refresh.setIndexedRuns(index.getSourceSummary() == null ? 0 : defaultInteger(index.getSourceSummary().getIndexedRuns(), 0));
        refresh.setSkippedFiles(index.getSourceSummary() == null ? 0 : defaultInteger(index.getSourceSummary().getSkippedFiles(), 0));
        return refresh;
    }

    private AutomationResultArchiveIndexVO buildAndWriteLatestIndex() {
        AutomationResultArchiveIndexVO index = buildIndex();
        try {
            Files.createDirectories(resultsDir);
            Files.writeString(
                    resolveLatestIndexFile(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(index),
                    StandardCharsets.UTF_8
            );
            return index;
        } catch (IOException ex) {
            throw new BizException(500, "写入自动化结果归档索引失败", ex);
        }
    }

    private AutomationResultArchiveIndexVO buildIndex() {
        if (!Files.isDirectory(resultsDir)) {
            return emptyIndex();
        }
        try (Stream<Path> stream = Files.list(resultsDir)) {
            List<Path> runFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isRegistryRunFile)
                    .sorted(java.util.Comparator.comparing(this::resolveSortKey))
                    .toList();

            List<AutomationResultArchiveIndexVO.RunRecord> runs = new ArrayList<>();
            List<AutomationResultArchiveIndexVO.SkippedFile> skippedFiles = new ArrayList<>();
            for (Path runFile : runFiles) {
                try {
                    runs.add(toRunRecord(readRunDetail(runFile)));
                } catch (RuntimeException ex) {
                    log.warn("跳过无法解析的自动化运行结果文件: {}", runFile, ex);
                    AutomationResultArchiveIndexVO.SkippedFile skippedFile = new AutomationResultArchiveIndexVO.SkippedFile();
                    skippedFile.setFileName(runFile.getFileName().toString());
                    skippedFile.setReason(ex.getCause() instanceof IOException ? "invalid-json" : "invalid-structure");
                    skippedFiles.add(skippedFile);
                }
            }

            runs.sort(java.util.Comparator.comparing(AutomationResultArchiveIndexVO.RunRecord::getRunId).reversed());

            AutomationResultArchiveIndexVO index = new AutomationResultArchiveIndexVO();
            index.setGeneratedAt(Instant.now().toString());
            index.setResultsDir(toDisplayPath(resultsDir));
            index.setSourceSummary(buildSourceSummary(runFiles.size(), runs.size(), skippedFiles.size()));
            index.setFacets(buildFacets(runs));
            index.setRuns(runs);
            index.setSkippedFiles(skippedFiles);
            return index;
        } catch (IOException ex) {
            throw new BizException(500, "读取自动化结果目录失败", ex);
        }
    }

    private AutomationResultArchiveIndexVO.SourceSummary buildSourceSummary(int registryRunFiles, int indexedRuns, int skippedFiles) {
        AutomationResultArchiveIndexVO.SourceSummary summary = new AutomationResultArchiveIndexVO.SourceSummary();
        summary.setRegistryRunFiles(registryRunFiles);
        summary.setIndexedRuns(indexedRuns);
        summary.setSkippedFiles(skippedFiles);
        return summary;
    }

    private AutomationResultArchiveFacetVO buildFacets(List<AutomationResultArchiveIndexVO.RunRecord> runs) {
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        LinkedHashSet<String> runnerTypes = new LinkedHashSet<>();
        LinkedHashSet<String> packageCodes = new LinkedHashSet<>();
        LinkedHashSet<String> environmentCodes = new LinkedHashSet<>();

        runs.forEach(run -> {
            if (StringUtils.hasText(run.getStatus())) {
                statuses.add(run.getStatus());
            }
            if (run.getRunnerTypes() != null) {
                run.getRunnerTypes().stream().filter(StringUtils::hasText).forEach(runnerTypes::add);
            }
            if (StringUtils.hasText(run.getPackageCode())) {
                packageCodes.add(run.getPackageCode());
            }
            if (StringUtils.hasText(run.getEnvironmentCode())) {
                environmentCodes.add(run.getEnvironmentCode());
            }
        });

        AutomationResultArchiveFacetVO facets = new AutomationResultArchiveFacetVO();
        facets.setStatuses(statuses.stream().sorted().toList());
        facets.setRunnerTypes(runnerTypes.stream().sorted().toList());
        facets.setPackageCodes(packageCodes.stream().sorted().toList());
        facets.setEnvironmentCodes(environmentCodes.stream().sorted().toList());
        return facets;
    }

    private AutomationResultArchiveIndexVO.RunRecord toRunRecord(AutomationResultRunDetailVO detail) {
        AutomationResultArchiveIndexVO.RunRecord run = new AutomationResultArchiveIndexVO.RunRecord();
        run.setRunId(detail.getRunId());
        run.setUpdatedAt(detail.getUpdatedAt());
        run.setReportPath(detail.getReportPath());
        run.setStatus(resolveRunStatus(detail.getSummary()));
        run.setSummary(detail.getSummary());
        run.setPackageCode(cleanText(detail.getOptions() == null ? null : detail.getOptions().get("packageCode")));
        run.setEnvironmentCode(cleanText(detail.getOptions() == null ? null : detail.getOptions().get("environmentCode")));
        run.setRunnerTypes(resolveRunnerTypes(detail.getResults()));
        run.setFailedScenarioIds(detail.getFailedScenarioIds());
        run.setRelatedEvidenceFiles(detail.getRelatedEvidenceFiles());
        run.setEvidenceItems(resolveEvidenceItems(detail));
        RunFailureDiagnosis diagnosis = buildRunFailureDiagnosis(detail.getResults());
        run.setFailureSummary(diagnosis.failureSummary());
        run.setFailedModules(diagnosis.failedModules());
        run.setFailedScenarios(diagnosis.failedScenarios());
        return run;
    }

    private List<AutomationResultArchiveIndexVO.EvidenceItem> resolveEvidenceItems(AutomationResultRunDetailVO detail) {
        return resolveAllowedEvidence(detail).entrySet().stream()
                .map(entry -> toEvidenceItem(entry.getKey(), entry.getValue(), detail.getReportPath()))
                .toList();
    }

    private AutomationResultArchiveIndexVO.EvidenceItem toEvidenceItem(String evidencePath, String source, String reportPath) {
        AutomationResultArchiveIndexVO.EvidenceItem item = new AutomationResultArchiveIndexVO.EvidenceItem();
        item.setPath(evidencePath);
        item.setFileName(Paths.get(evidencePath).getFileName().toString());
        item.setCategory(resolveEvidenceCategory(evidencePath, reportPath));
        item.setSource(source);
        return item;
    }

    private boolean requiresDiagnosisBackfill(AutomationResultArchiveIndexVO index) {
        if (index == null || index.getRuns() == null || index.getRuns().isEmpty()) {
            return false;
        }
        return index.getRuns().stream().anyMatch(run ->
                defaultInteger(run.getSummary() == null ? null : run.getSummary().getFailed(), 0) > 0
                        && (run.getFailureSummary() == null || run.getFailedScenarios() == null || run.getFailedModules() == null)
        );
    }

    private RunFailureDiagnosis buildRunFailureDiagnosis(List<AutomationResultRunResultVO> results) {
        Map<String, List<String>> scenarioEvidenceTexts = resolveScenarioEvidenceTexts(results);
        List<AutomationResultFailedScenarioVO> failedScenarios = normalizeResults(results).stream()
                .filter(item -> !"passed".equalsIgnoreCase(defaultString(item.getStatus())))
                .map(item -> diagnoseFailedScenario(item, scenarioEvidenceTexts.getOrDefault(cleanText(item.getScenarioId()), Collections.emptyList())))
                .toList();
        return new RunFailureDiagnosis(
                summarizeFailureCategories(failedScenarios),
                aggregateFailedModules(failedScenarios),
                failedScenarios
        );
    }

    private Map<String, List<String>> resolveScenarioEvidenceTexts(List<AutomationResultRunResultVO> results) {
        LinkedHashMap<String, List<String>> scenarioEvidenceTexts = new LinkedHashMap<>();
        for (AutomationResultRunResultVO result : normalizeResults(results)) {
            String scenarioId = cleanText(result.getScenarioId());
            if (!StringUtils.hasText(scenarioId)) {
                continue;
            }
            List<String> evidenceTexts = new ArrayList<>();
            for (String evidencePath : result.getEvidenceFiles()) {
                String rawText = readEvidenceText(evidencePath);
                if (StringUtils.hasText(rawText)) {
                    evidenceTexts.add(rawText);
                }
            }
            scenarioEvidenceTexts.put(scenarioId, evidenceTexts);
        }
        return scenarioEvidenceTexts;
    }

    private String readEvidenceText(String evidencePath) {
        try {
            Path resolvedPath = resolveEvidenceFile(evidencePath);
            if (!Files.isRegularFile(resolvedPath)) {
                return "";
            }
            return cleanText(Files.readString(resolvedPath, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ex) {
            return "";
        }
    }

    private AutomationResultFailedScenarioVO diagnoseFailedScenario(
            AutomationResultRunResultVO result,
            List<String> evidenceTexts
    ) {
        String signalText = collectSignalText(result, evidenceTexts);
        AutomationFailureDiagnosisVO diagnosis = inferDiagnosis(signalText);
        diagnosis.setEvidenceSummary(buildEvidenceSummary(result, evidenceTexts));

        AutomationResultFailedScenarioVO failedScenario = new AutomationResultFailedScenarioVO();
        failedScenario.setScenarioId(cleanText(result.getScenarioId()));
        failedScenario.setScenarioTitle(inferScenarioTitle(result));
        failedScenario.setModuleCode(inferModuleCode(result));
        failedScenario.setModuleName(inferModuleName(result));
        failedScenario.setRunnerType(cleanText(result.getRunnerType()));
        failedScenario.setStepLabel(readDetailText(result, "stepLabel"));
        failedScenario.setApiRef(readDetailText(result, "apiRef"));
        failedScenario.setPageAction(readDetailText(result, "pageAction"));
        failedScenario.setDiagnosis(diagnosis);
        return failedScenario;
    }

    private String collectSignalText(AutomationResultRunResultVO result, List<String> evidenceTexts) {
        List<String> parts = new ArrayList<>();
        parts.add(cleanText(result.getSummary()));
        parts.add(readDetailText(result, "stepLabel"));
        parts.add(readDetailText(result, "apiRef"));
        parts.add(readDetailText(result, "pageAction"));
        evidenceTexts.stream().map(this::cleanText).filter(StringUtils::hasText).forEach(parts::add);
        return String.join("\n", parts).toLowerCase(Locale.ROOT);
    }

    private AutomationFailureDiagnosisVO inferDiagnosis(String signalText) {
        if (containsAny(signalText, "401", "403", "unauthorized", "forbidden", "无权限", "未授权", "登录失效", "菜单不可见", "权限不足")) {
            return buildDiagnosis("权限", "命中 401/403 或未授权信号");
        }
        if (containsAny(signalText, "econnrefused", "connection refused", "timeout", "timed out", "etimedout", "dns", "服务未启动", "页面不可达", "依赖不可用")) {
            return buildDiagnosis("环境", "命中连接拒绝、超时或依赖不可用信号");
        }
        if (containsAny(signalText, "500", "502", "503", "504", "接口响应异常", "response missing", "响应缺字段", "contract mismatch")) {
            return buildDiagnosis("接口", "命中 5xx、响应异常或契约缺口信号");
        }
        if (containsAny(signalText, "selector not found", "element not found", "not clickable", "页面未渲染", "按钮不可点击", "对话框未出现")) {
            return buildDiagnosis("UI", "命中页面元素未渲染或不可交互信号");
        }
        if (containsAny(signalText, "数据不存在", "记录为空", "样本缺失", "前置数据缺失", "列表为空")) {
            return buildDiagnosis("数据", "命中前置数据缺失或结果为空信号");
        }
        if (containsAny(signalText, "asserttext", "asserturlincludes", "assertvariableequals", "assertion failed", "断言失败")) {
            return buildDiagnosis("断言", "流程可达但断言不成立");
        }
        return buildDiagnosis("其他", "未命中已知规则，建议查看原始证据");
    }

    private AutomationFailureDiagnosisVO buildDiagnosis(String category, String reason) {
        AutomationFailureDiagnosisVO diagnosis = new AutomationFailureDiagnosisVO();
        diagnosis.setCategory(category);
        diagnosis.setReason(reason);
        diagnosis.setEvidenceSummary("未记录证据摘要");
        return diagnosis;
    }

    private boolean containsAny(String signalText, String... needles) {
        for (String needle : needles) {
            if (signalText.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String buildEvidenceSummary(AutomationResultRunResultVO result, List<String> evidenceTexts) {
        List<String> parts = uniqueNonBlank(List.of(
                cleanText(result.getSummary()),
                readDetailText(result, "stepLabel"),
                readDetailText(result, "apiRef"),
                readDetailText(result, "pageAction"),
                evidenceTexts.isEmpty() ? "" : cleanText(evidenceTexts.get(0))
        ));
        String summary = String.join("；", parts.stream().limit(3).toList());
        return StringUtils.hasText(summary) ? truncateText(summary) : "未记录证据摘要";
    }

    private String inferScenarioTitle(AutomationResultRunResultVO result) {
        String scenarioTitle = readDetailText(result, "scenarioTitle");
        return StringUtils.hasText(scenarioTitle) ? scenarioTitle : cleanText(result.getScenarioId());
    }

    private String inferModuleCode(AutomationResultRunResultVO result) {
        String moduleCode = readDetailText(result, "moduleCode");
        if (StringUtils.hasText(moduleCode)) {
            return moduleCode;
        }
        String scenarioId = cleanText(result.getScenarioId());
        if (!StringUtils.hasText(scenarioId)) {
            return "unknown";
        }
        int dotIndex = scenarioId.indexOf('.');
        return dotIndex > 0 ? scenarioId.substring(0, dotIndex) : scenarioId;
    }

    private String inferModuleName(AutomationResultRunResultVO result) {
        String moduleName = readDetailText(result, "moduleName");
        return StringUtils.hasText(moduleName) ? moduleName : inferModuleCode(result);
    }

    private String readDetailText(AutomationResultRunResultVO result, String key) {
        Map<String, Object> details = result.getDetails();
        if (details == null) {
            return "";
        }
        return cleanText(details.get(key));
    }

    private AutomationResultFailureSummaryVO summarizeFailureCategories(List<AutomationResultFailedScenarioVO> failedScenarios) {
        LinkedHashMap<String, Integer> countsByCategory = new LinkedHashMap<>();
        for (AutomationResultFailedScenarioVO scenario : failedScenarios) {
            String category = cleanText(scenario.getDiagnosis() == null ? null : scenario.getDiagnosis().getCategory());
            if (!StringUtils.hasText(category)) {
                category = "其他";
            }
            countsByCategory.put(category, countsByCategory.getOrDefault(category, 0) + 1);
        }
        String primaryCategory = countsByCategory.keySet().stream()
                .sorted((left, right) -> {
                    int countDiff = countsByCategory.getOrDefault(right, 0) - countsByCategory.getOrDefault(left, 0);
                    return countDiff != 0 ? countDiff : compareCategoryPriority(left, right);
                })
                .findFirst()
                .orElse("其他");

        AutomationResultFailureSummaryVO summary = new AutomationResultFailureSummaryVO();
        summary.setPrimaryCategory(primaryCategory);
        summary.setCountsByCategory(countsByCategory);
        return summary;
    }

    private int compareCategoryPriority(String left, String right) {
        int leftIndex = CATEGORY_PRIORITY.indexOf(left);
        int rightIndex = CATEGORY_PRIORITY.indexOf(right);
        if (leftIndex < 0) {
            leftIndex = CATEGORY_PRIORITY.size();
        }
        if (rightIndex < 0) {
            rightIndex = CATEGORY_PRIORITY.size();
        }
        return Integer.compare(leftIndex, rightIndex);
    }

    private List<AutomationResultFailedModuleVO> aggregateFailedModules(List<AutomationResultFailedScenarioVO> failedScenarios) {
        LinkedHashMap<String, List<AutomationResultFailedScenarioVO>> moduleBuckets = new LinkedHashMap<>();
        for (AutomationResultFailedScenarioVO scenario : failedScenarios) {
            String moduleCode = cleanText(scenario.getModuleCode());
            if (!StringUtils.hasText(moduleCode)) {
                moduleCode = "unknown";
            }
            moduleBuckets.computeIfAbsent(moduleCode, ignored -> new ArrayList<>()).add(scenario);
        }

        List<AutomationResultFailedModuleVO> modules = new ArrayList<>();
        for (Map.Entry<String, List<AutomationResultFailedScenarioVO>> entry : moduleBuckets.entrySet()) {
            List<AutomationResultFailedScenarioVO> scenarios = entry.getValue();
            AutomationResultFailureSummaryVO summary = summarizeFailureCategories(scenarios);
            int primaryCount = summary.getCountsByCategory().getOrDefault(summary.getPrimaryCategory(), 0);
            List<String> extraCategories = summary.getCountsByCategory().entrySet().stream()
                    .filter(item -> !Objects.equals(item.getKey(), summary.getPrimaryCategory()))
                    .map(item -> item.getValue() + " 个 " + item.getKey() + " 问题")
                    .toList();
            String reason = extraCategories.isEmpty()
                    ? scenarios.size() + " 个失败场景中 " + primaryCount + " 个命中" + summary.getPrimaryCategory() + "问题"
                    : scenarios.size() + " 个失败场景中 " + primaryCount + " 个命中" + summary.getPrimaryCategory() + "问题，另有 " + String.join("、", extraCategories);
            AutomationFailureDiagnosisVO diagnosis = buildDiagnosis(summary.getPrimaryCategory(), reason);
            diagnosis.setEvidenceSummary(truncateText(String.join("；", uniqueNonBlank(
                    scenarios.stream()
                            .map(item -> item.getDiagnosis() == null ? null : item.getDiagnosis().getEvidenceSummary())
                            .limit(2)
                            .toList()
            ))));

            AutomationResultFailedModuleVO module = new AutomationResultFailedModuleVO();
            module.setModuleCode(entry.getKey());
            module.setModuleName(cleanText(scenarios.get(0).getModuleName()));
            module.setFailedScenarioCount(scenarios.size());
            module.setDiagnosis(diagnosis);
            modules.add(module);
        }
        return modules;
    }

    private List<String> uniqueNonBlank(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String text = cleanText(value);
            if (StringUtils.hasText(text)) {
                normalized.add(text);
            }
        }
        return List.copyOf(normalized);
    }

    private String truncateText(String value) {
        String text = cleanText(value);
        if (!StringUtils.hasText(text) || text.length() <= MAX_EVIDENCE_SUMMARY_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_EVIDENCE_SUMMARY_LENGTH - 1) + "...";
    }

    private boolean isLatestIndexMissingOrStale() {
        Path latestIndex = resolveLatestIndexFile();
        if (!Files.isRegularFile(latestIndex)) {
            return true;
        }
        try {
            long latestIndexMillis = Files.getLastModifiedTime(latestIndex).toMillis();
            try (Stream<Path> stream = Files.list(resultsDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(this::isRegistryRunFile)
                        .anyMatch(file -> latestRegistryRunMillis(file) > latestIndexMillis);
            }
        } catch (IOException ex) {
            return true;
        }
    }

    private long latestRegistryRunMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException ex) {
            return Long.MAX_VALUE;
        }
    }

    private Path resolveLatestIndexFile() {
        return resultsDir.resolve(LATEST_INDEX_FILE_NAME).normalize();
    }

    private boolean isRegistryRunFile(Path file) {
        return REGISTRY_RUN_FILE_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private String resolveSortKey(Path file) {
        return resolveRunIdFromFileName(file).orElse(file.getFileName().toString());
    }

    private AutomationResultRunDetailVO readRunDetail(Path file) {
        try {
            AutomationResultRunDetailVO detail = objectMapper.readValue(
                    Files.readString(file, StandardCharsets.UTF_8),
                    AutomationResultRunDetailVO.class
            );
            detail.setRunId(resolveRunId(detail, file));
            detail.setReportPath(toDisplayPath(file));
            detail.setUpdatedAt(resolveUpdatedAt(file));
            detail.setSummary(normalizeSummary(detail.getSummary(), detail.getResults()));
            detail.setResults(normalizeResults(detail.getResults()));
            detail.setFailedScenarioIds(resolveFailedScenarioIds(detail.getResults()));
            detail.setRelatedEvidenceFiles(resolveRelatedEvidenceFiles(detail.getResults()));
            return detail;
        } catch (IOException ex) {
            throw new BizException(500, "读取自动化运行结果失败: " + file.getFileName(), ex);
        }
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

    private String resolveUpdatedAt(Path file) throws IOException {
        return UPDATED_AT_FORMATTER.format(
                Instant.ofEpochMilli(Files.getLastModifiedTime(file).toMillis())
                        .atZone(ZoneId.systemDefault())
        );
    }

    private AutomationResultSummaryVO normalizeSummary(
            AutomationResultSummaryVO summary,
            List<AutomationResultRunResultVO> results
    ) {
        List<AutomationResultRunResultVO> normalizedResults = normalizeResults(results);
        int failed = (int) normalizedResults.stream()
                .filter(item -> !"passed".equalsIgnoreCase(defaultString(item.getStatus())))
                .count();
        int total = normalizedResults.size();

        AutomationResultSummaryVO normalized = summary == null ? new AutomationResultSummaryVO() : summary;
        normalized.setTotal(defaultInteger(normalized.getTotal(), total));
        normalized.setFailed(defaultInteger(normalized.getFailed(), failed));
        normalized.setPassed(defaultInteger(normalized.getPassed(), Math.max(0, normalized.getTotal() - normalized.getFailed())));
        return normalized;
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
                        .map(this::normalizeEvidencePath)
                        .filter(StringUtils::hasText)
                        .toList());
            }
        });
        return results;
    }

    private List<String> resolveFailedScenarioIds(List<AutomationResultRunResultVO> results) {
        return normalizeResults(results).stream()
                .filter(item -> !"passed".equalsIgnoreCase(defaultString(item.getStatus())))
                .map(AutomationResultRunResultVO::getScenarioId)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> resolveRelatedEvidenceFiles(List<AutomationResultRunResultVO> results) {
        LinkedHashSet<String> evidenceFiles = new LinkedHashSet<>();
        normalizeResults(results).stream()
                .map(AutomationResultRunResultVO::getEvidenceFiles)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(this::normalizeEvidencePath)
                .filter(StringUtils::hasText)
                .forEach(evidenceFiles::add);
        return List.copyOf(evidenceFiles);
    }

    private Map<String, String> resolveAllowedEvidence(AutomationResultRunDetailVO detail) {
        LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
        addEvidencePath(evidence, detail.getReportPath(), "report");
        detail.getRelatedEvidenceFiles().forEach(path -> addEvidencePath(evidence, path, "related"));
        detail.getResults().forEach(result -> result.getEvidenceFiles().forEach(path -> addEvidencePath(evidence, path, "scenario")));
        return evidence;
    }

    private void addEvidencePath(Map<String, String> evidence, String evidencePath, String source) {
        String normalizedPath = normalizeEvidencePath(evidencePath);
        if (!StringUtils.hasText(normalizedPath)) {
            return;
        }
        Path file = resolveEvidenceFile(normalizedPath);
        if (!Files.isRegularFile(file)) {
            return;
        }
        evidence.putIfAbsent(normalizedPath, source);
    }

    private String normalizeEvidencePath(String evidencePath) {
        if (!StringUtils.hasText(evidencePath)) {
            return "";
        }
        Path candidate = Paths.get(evidencePath);
        if (candidate.isAbsolute()) {
            return toDisplayPath(candidate);
        }
        return evidencePath.replace('\\', '/');
    }

    private Path resolveEvidenceFile(String evidencePath) {
        String normalizedPath = normalizeEvidencePath(evidencePath);
        Path candidate;
        if (normalizedPath.startsWith(ACCEPTANCE_PREFIX)) {
            candidate = resultsDir.resolve(normalizedPath.substring(ACCEPTANCE_PREFIX.length())).normalize();
        } else {
            Path rawCandidate = Paths.get(normalizedPath);
            candidate = rawCandidate.isAbsolute()
                    ? rawCandidate.toAbsolutePath().normalize()
                    : resultsDir.resolve(normalizedPath).normalize();
        }
        if (!candidate.startsWith(resultsDir)) {
            throw new BizException(400, "证据文件路径不合法: " + normalizedPath);
        }
        return candidate;
    }

    private String resolveEvidenceCategory(String evidencePath, String reportPath) {
        if (Objects.equals(evidencePath, reportPath)) {
            return "run-summary";
        }
        String fileName = Paths.get(evidencePath).getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".json")) {
            return "json";
        }
        if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            return "markdown";
        }
        if (fileName.endsWith(".txt") || fileName.endsWith(".log") || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml") || fileName.endsWith(".csv")) {
            return "text";
        }
        return "unknown";
    }

    private List<String> resolveRunnerTypes(List<AutomationResultRunResultVO> results) {
        LinkedHashSet<String> runnerTypes = new LinkedHashSet<>();
        normalizeResults(results).stream()
                .map(AutomationResultRunResultVO::getRunnerType)
                .filter(StringUtils::hasText)
                .forEach(runnerTypes::add);
        return List.copyOf(runnerTypes);
    }

    private String resolveRunStatus(AutomationResultSummaryVO summary) {
        return summary != null && defaultInteger(summary.getFailed(), 0) > 0 ? "failed" : "passed";
    }

    private String toDisplayPath(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        int nameCount = normalized.getNameCount();
        for (int index = 0; index < nameCount - 1; index++) {
            if ("logs".equalsIgnoreCase(normalized.getName(index).toString())
                    && "acceptance".equalsIgnoreCase(normalized.getName(index + 1).toString())) {
                return normalized.subpath(index, nameCount).toString().replace('\\', '/');
            }
        }
        return normalized.toString().replace('\\', '/');
    }

    private String cleanText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private AutomationResultArchiveFacetVO createEmptyFacets() {
        AutomationResultArchiveFacetVO facets = new AutomationResultArchiveFacetVO();
        facets.setStatuses(Collections.emptyList());
        facets.setRunnerTypes(Collections.emptyList());
        facets.setPackageCodes(Collections.emptyList());
        facets.setEnvironmentCodes(Collections.emptyList());
        return facets;
    }

    private AutomationResultArchiveIndexVO emptyIndex() {
        AutomationResultArchiveIndexVO index = new AutomationResultArchiveIndexVO();
        index.setGeneratedAt(Instant.now().toString());
        index.setResultsDir(toDisplayPath(resultsDir));
        index.setSourceSummary(buildSourceSummary(0, 0, 0));
        index.setFacets(createEmptyFacets());
        index.setRuns(Collections.emptyList());
        index.setSkippedFiles(Collections.emptyList());
        return index;
    }

    private record RunFailureDiagnosis(
            AutomationResultFailureSummaryVO failureSummary,
            List<AutomationResultFailedModuleVO> failedModules,
            List<AutomationResultFailedScenarioVO> failedScenarios
    ) {
    }
}
