package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.report.service.AutomationResultArchiveIndexService;
import com.ghlzm.iot.report.vo.AutomationResultArchiveFacetVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveIndexVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveRefreshVO;
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
            return objectMapper.readValue(
                    Files.readString(resolveLatestIndexFile(), StandardCharsets.UTF_8),
                    AutomationResultArchiveIndexVO.class
            );
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
}
