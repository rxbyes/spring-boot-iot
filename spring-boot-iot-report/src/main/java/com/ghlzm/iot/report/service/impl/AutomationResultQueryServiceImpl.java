package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceContentVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceItemVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 自动化运行结果文件查询服务
 */
@Slf4j
@Service
public class AutomationResultQueryServiceImpl implements AutomationResultQueryService {

    private static final Pattern REGISTRY_RUN_FILE_PATTERN = Pattern.compile("^registry-run-(.+)\\.json$");
    private static final String ACCEPTANCE_PREFIX = "logs/acceptance/";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_PREVIEW_LENGTH = 20_000;
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path resultsDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public AutomationResultQueryServiceImpl(
            @Value("${iot.automation.results-dir:logs/acceptance}") String resultsDir
    ) {
        this(Paths.get(resultsDir), JsonMapper.builder().findAndAddModules().build());
    }

    AutomationResultQueryServiceImpl(Path resultsDir, ObjectMapper objectMapper) {
        this.resultsDir = resultsDir.toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AutomationResultRunSummaryVO> listRecentRuns(Integer limit) {
        if (!Files.isDirectory(resultsDir)) {
            return Collections.emptyList();
        }

        int resolvedLimit = normalizeLimit(limit);
        try (Stream<Path> stream = Files.list(resultsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isRegistryRunFile)
                    .sorted(Comparator.comparing(this::resolveSortKey).reversed())
                    .limit(resolvedLimit)
                    .map(this::safeReadRunSummary)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new BizException(500, "读取自动化运行结果目录失败", e);
        }
    }

    @Override
    public AutomationResultRunDetailVO getRunDetail(String runId) {
        return readRunDetail(resolveRunFile(runId));
    }

    @Override
    public List<AutomationResultEvidenceItemVO> listRunEvidence(String runId) {
        AutomationResultRunDetailVO detail = getRunDetail(runId);
        return resolveAllowedEvidence(detail).entrySet().stream()
                .map(entry -> toEvidenceItem(entry.getKey(), entry.getValue(), detail.getReportPath()))
                .toList();
    }

    @Override
    public AutomationResultEvidenceContentVO getEvidenceContent(String runId, String path) {
        AutomationResultRunDetailVO detail = getRunDetail(runId);
        Map<String, String> allowedEvidence = resolveAllowedEvidence(detail);
        String normalizedPath = normalizeEvidencePath(path);
        if (!allowedEvidence.containsKey(normalizedPath)) {
            throw new BizException(400, "证据文件不属于当前运行结果: " + normalizedPath);
        }

        Path file = resolveEvidenceFile(normalizedPath);
        if (!Files.isRegularFile(file)) {
            throw new BizException(404, "未找到证据文件: " + normalizedPath);
        }

        try {
            String rawContent = Files.readString(file, StandardCharsets.UTF_8);
            boolean truncated = rawContent.length() > MAX_PREVIEW_LENGTH;
            AutomationResultEvidenceContentVO content = new AutomationResultEvidenceContentVO();
            content.setPath(normalizedPath);
            content.setFileName(file.getFileName().toString());
            content.setCategory(resolveEvidenceCategory(normalizedPath, detail.getReportPath()));
            content.setContent(truncated ? rawContent.substring(0, MAX_PREVIEW_LENGTH) : rawContent);
            content.setTruncated(truncated);
            return content;
        } catch (IOException e) {
            throw new BizException(500, "读取证据文件失败: " + normalizedPath, e);
        }
    }

    private boolean isRegistryRunFile(Path file) {
        return REGISTRY_RUN_FILE_PATTERN.matcher(file.getFileName().toString()).matches();
    }

    private String resolveSortKey(Path file) {
        return resolveRunIdFromFileName(file).orElse(file.getFileName().toString());
    }

    private AutomationResultRunSummaryVO safeReadRunSummary(Path file) {
        try {
            return toSummary(readRunDetail(file));
        } catch (RuntimeException ex) {
            log.warn("跳过无法解析的自动化运行结果文件: {}", file, ex);
            return null;
        }
    }

    private AutomationResultRunSummaryVO toSummary(AutomationResultRunDetailVO detail) {
        AutomationResultRunSummaryVO summary = new AutomationResultRunSummaryVO();
        summary.setRunId(detail.getRunId());
        summary.setReportPath(detail.getReportPath());
        summary.setUpdatedAt(detail.getUpdatedAt());
        summary.setSummary(detail.getSummary());
        summary.setFailedScenarioIds(detail.getFailedScenarioIds());
        summary.setRelatedEvidenceFiles(detail.getRelatedEvidenceFiles());
        return summary;
    }

    private Path resolveRunFile(String runId) {
        if (!StringUtils.hasText(runId)) {
            throw new BizException(400, "运行编号不能为空");
        }

        Path file = resultsDir.resolve("registry-run-" + runId.trim() + ".json").normalize();
        if (!file.startsWith(resultsDir) || !Files.isRegularFile(file)) {
            throw new BizException(404, "未找到运行结果: " + runId);
        }
        return file;
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
        } catch (IOException e) {
            throw new BizException(500, "读取自动化运行结果失败: " + file.getFileName(), e);
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

    private void addEvidencePath(Map<String, String> evidence, String path, String source) {
        String normalizedPath = normalizeEvidencePath(path);
        if (!StringUtils.hasText(normalizedPath)) {
            return;
        }
        Path file = resolveEvidenceFile(normalizedPath);
        if (!Files.isRegularFile(file)) {
            return;
        }
        evidence.putIfAbsent(normalizedPath, source);
    }

    private AutomationResultEvidenceItemVO toEvidenceItem(String path, String source, String reportPath) {
        AutomationResultEvidenceItemVO item = new AutomationResultEvidenceItemVO();
        item.setPath(path);
        item.setFileName(Paths.get(path).getFileName().toString());
        item.setCategory(resolveEvidenceCategory(path, reportPath));
        item.setSource(source);
        return item;
    }

    private String normalizeEvidencePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        Path candidate = Paths.get(path);
        if (candidate.isAbsolute()) {
            return toDisplayPath(candidate);
        }
        return path.replace('\\', '/');
    }

    private Path resolveEvidenceFile(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BizException(400, "证据文件路径不能为空");
        }

        String normalizedPath = normalizeEvidencePath(path);
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

    private String resolveEvidenceCategory(String path, String reportPath) {
        if (Objects.equals(path, reportPath)) {
            return "run-summary";
        }
        String fileName = Paths.get(path).getFileName().toString().toLowerCase();
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

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
