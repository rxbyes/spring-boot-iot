package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.report.service.AutomationResultArchiveIndexService;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationFailureDiagnosisVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveFacetVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveIndexVO;
import com.ghlzm.iot.report.vo.AutomationResultArchiveRefreshVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceContentVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceItemVO;
import com.ghlzm.iot.report.vo.AutomationResultFailedScenarioVO;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动化运行结果文件查询服务。
 */
@Service
public class AutomationResultQueryServiceImpl implements AutomationResultQueryService {

    private static final Pattern REGISTRY_RUN_FILE_PATTERN = Pattern.compile("^registry-run-(.+)\\.json$");
    private static final String ACCEPTANCE_PREFIX = "logs/acceptance/";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PREVIEW_LENGTH = 20_000;
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Set<String> IMAGE_EVIDENCE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

    private final Path resultsDir;
    private final ObjectMapper objectMapper;
    private final AutomationResultArchiveIndexService archiveIndexService;

    @Autowired
    public AutomationResultQueryServiceImpl(
            @Value("${iot.automation.results-dir:logs/acceptance}") String resultsDir,
            AutomationResultArchiveIndexService archiveIndexService
    ) {
        this(Paths.get(resultsDir), JsonMapper.builder().findAndAddModules().build(), archiveIndexService);
    }

    AutomationResultQueryServiceImpl(Path resultsDir, ObjectMapper objectMapper) {
        this(resultsDir, objectMapper, null);
    }

    AutomationResultQueryServiceImpl(
            Path resultsDir,
            ObjectMapper objectMapper,
            AutomationResultArchiveIndexService archiveIndexService
    ) {
        this.resultsDir = resultsDir.toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        this.archiveIndexService = archiveIndexService == null
                ? new AutomationResultArchiveIndexServiceImpl(this.resultsDir, this.objectMapper)
                : archiveIndexService;
    }

    @Override
    public PageResult<AutomationResultRunSummaryVO> pageRuns(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            String status,
            String runnerType,
            String packageCode,
            String environmentCode,
            String dateFrom,
            String dateTo
    ) {
        int resolvedPageNum = normalizePageNum(pageNum);
        int resolvedPageSize = normalizePageSize(pageSize);
        Long startMillis = resolveDateStartMillis(dateFrom);
        Long endExclusiveMillis = resolveDateEndExclusiveMillis(dateTo);
        String normalizedKeyword = normalizeText(keyword);
        String normalizedStatus = normalizeText(status);
        String normalizedRunnerType = normalizeText(runnerType);
        String normalizedPackageCode = normalizeText(packageCode);
        String normalizedEnvironmentCode = normalizeText(environmentCode);

        List<AutomationResultArchiveIndexVO.RunRecord> matchedRuns = archiveIndexService.loadArchiveIndex(false)
                .getRuns()
                .stream()
                .filter(item -> matchesPageFilters(
                        item,
                        normalizedKeyword,
                        normalizedStatus,
                        normalizedRunnerType,
                        normalizedPackageCode,
                        normalizedEnvironmentCode,
                        startMillis,
                        endExclusiveMillis
                ))
                .toList();

        int fromIndex = Math.min((resolvedPageNum - 1) * resolvedPageSize, matchedRuns.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, matchedRuns.size());
        List<AutomationResultRunSummaryVO> records = matchedRuns.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();
        return PageResult.of(
                (long) matchedRuns.size(),
                (long) resolvedPageNum,
                (long) resolvedPageSize,
                records
        );
    }

    @Override
    public List<AutomationResultRunSummaryVO> listRecentRuns(Integer limit) {
        int resolvedLimit = normalizeLimit(limit);
        return archiveIndexService.loadArchiveIndex(false)
                .getRuns()
                .stream()
                .limit(resolvedLimit)
                .map(this::toSummary)
                .toList();
    }

    @Override
    public AutomationResultArchiveFacetVO listFacets() {
        return archiveIndexService.listFacets();
    }

    @Override
    public AutomationResultArchiveRefreshVO refreshIndex() {
        return archiveIndexService.refreshIndex();
    }

    @Override
    public AutomationResultRunDetailVO getRunDetail(String runId) {
        return attachIndexedDiagnosis(readRunDetail(resolveRunFile(runId)));
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
            String category = resolveEvidenceCategory(normalizedPath, detail.getReportPath());
            if ("image".equals(category)) {
                AutomationResultEvidenceContentVO content = new AutomationResultEvidenceContentVO();
                content.setPath(normalizedPath);
                content.setFileName(file.getFileName().toString());
                content.setCategory(category);
                content.setContent(resolveImageDataUrl(file));
                content.setTruncated(false);
                return content;
            }

            String rawContent = Files.readString(file, StandardCharsets.UTF_8);
            boolean truncated = rawContent.length() > MAX_PREVIEW_LENGTH;
            AutomationResultEvidenceContentVO content = new AutomationResultEvidenceContentVO();
            content.setPath(normalizedPath);
            content.setFileName(file.getFileName().toString());
            content.setCategory(category);
            content.setContent(truncated ? rawContent.substring(0, MAX_PREVIEW_LENGTH) : rawContent);
            content.setTruncated(truncated);
            return content;
        } catch (IOException e) {
            throw new BizException(500, "读取证据文件失败: " + normalizedPath, e);
        }
    }

    private boolean matchesPageFilters(
            AutomationResultArchiveIndexVO.RunRecord indexedRun,
            String keyword,
            String status,
            String runnerType,
            String packageCode,
            String environmentCode,
            Long startMillis,
            Long endExclusiveMillis
    ) {
        long updatedAtEpochMillis = parseUpdatedAtMillis(indexedRun.getUpdatedAt());
        if (startMillis != null && updatedAtEpochMillis < startMillis) {
            return false;
        }
        if (endExclusiveMillis != null && updatedAtEpochMillis >= endExclusiveMillis) {
            return false;
        }
        if (StringUtils.hasText(status) && !status.equals(normalizeText(indexedRun.getStatus()))) {
            return false;
        }
        if (StringUtils.hasText(runnerType) && indexedRun.getRunnerTypes().stream()
                .map(this::normalizeText)
                .noneMatch(runnerType::equals)) {
            return false;
        }
        if (StringUtils.hasText(packageCode) && !packageCode.equals(normalizeText(indexedRun.getPackageCode()))) {
            return false;
        }
        if (StringUtils.hasText(environmentCode) && !environmentCode.equals(normalizeText(indexedRun.getEnvironmentCode()))) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return matchesKeyword(indexedRun, keyword);
    }

    private boolean matchesKeyword(AutomationResultArchiveIndexVO.RunRecord run, String keyword) {
        if (containsIgnoreCase(run.getRunId(), keyword)
                || containsIgnoreCase(run.getReportPath(), keyword)
                || containsIgnoreCase(run.getPackageCode(), keyword)
                || containsIgnoreCase(run.getEnvironmentCode(), keyword)) {
            return true;
        }
        return run.getFailedScenarioIds().stream().anyMatch(item -> containsIgnoreCase(item, keyword))
                || run.getRunnerTypes().stream().anyMatch(item -> containsIgnoreCase(item, keyword));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && normalizeText(value).contains(keyword);
    }

    private AutomationResultRunSummaryVO toSummary(AutomationResultArchiveIndexVO.RunRecord record) {
        AutomationResultRunSummaryVO summary = new AutomationResultRunSummaryVO();
        summary.setRunId(record.getRunId());
        summary.setReportPath(record.getReportPath());
        summary.setUpdatedAt(record.getUpdatedAt());
        summary.setSummary(record.getSummary());
        summary.setFailedScenarioIds(record.getFailedScenarioIds());
        summary.setRelatedEvidenceFiles(record.getRelatedEvidenceFiles());
        summary.setStatus(record.getStatus());
        summary.setRunnerTypes(record.getRunnerTypes());
        summary.setPackageCode(record.getPackageCode());
        summary.setEnvironmentCode(record.getEnvironmentCode());
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

    private AutomationResultRunDetailVO attachIndexedDiagnosis(AutomationResultRunDetailVO detail) {
        AutomationResultArchiveIndexVO.RunRecord indexedRun = findIndexedRun(detail.getRunId());
        if (indexedRun == null) {
            return detail;
        }
        detail.setFailureSummary(indexedRun.getFailureSummary());
        detail.setFailedModules(indexedRun.getFailedModules());
        detail.setFailedScenarios(indexedRun.getFailedScenarios());

        Map<String, AutomationFailureDiagnosisVO> scenarioDiagnosisMap = new LinkedHashMap<>();
        List<AutomationResultFailedScenarioVO> failedScenarios = indexedRun.getFailedScenarios();
        if (failedScenarios != null) {
            failedScenarios.forEach(item -> {
                if (StringUtils.hasText(item.getScenarioId()) && item.getDiagnosis() != null) {
                    scenarioDiagnosisMap.putIfAbsent(item.getScenarioId(), item.getDiagnosis());
                }
            });
        }
        detail.getResults().forEach(result -> result.setDiagnosis(scenarioDiagnosisMap.get(result.getScenarioId())));
        return detail;
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
        normalized.setPassed(defaultInteger(
                normalized.getPassed(),
                Math.max(0, normalized.getTotal() - normalized.getFailed())
        ));
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
        if (Files.isDirectory(file)) {
            addImageEvidenceDirectory(evidence, file, source);
            return;
        }
        if (!Files.isRegularFile(file)) {
            return;
        }
        evidence.putIfAbsent(normalizedPath, source);
    }

    private void addImageEvidenceDirectory(Map<String, String> evidence, Path directory, String source) {
        try (var files = Files.list(directory)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(this::isImageEvidencePath)
                    .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                    .map(this::toDisplayPath)
                    .forEach(path -> evidence.putIfAbsent(path, source));
        } catch (IOException ignored) {
            // Ignore unreadable evidence directories; the registry report still records the original artifact path.
        }
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
        String fileName = Paths.get(path).getFileName().toString().toLowerCase(Locale.ROOT);
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
        if (isImageEvidencePath(fileName)) {
            return "image";
        }
        return "unknown";
    }

    private boolean isImageEvidencePath(Path path) {
        return isImageEvidencePath(path.getFileName().toString());
    }

    private boolean isImageEvidencePath(String fileName) {
        String lowerName = defaultString(fileName).toLowerCase(Locale.ROOT);
        return IMAGE_EVIDENCE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }

    private String resolveImageDataUrl(Path file) throws IOException {
        return resolveImageMimeType(file) + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file));
    }

    private String resolveImageMimeType(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "data:image/jpeg";
        }
        if (fileName.endsWith(".webp")) {
            return "data:image/webp";
        }
        if (fileName.endsWith(".gif")) {
            return "data:image/gif";
        }
        return "data:image/png";
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

    private int normalizePageNum(Integer pageNum) {
        if (pageNum == null || pageNum <= 0) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private Long resolveDateStartMillis(String dateFrom) {
        if (!StringUtils.hasText(dateFrom)) {
            return null;
        }
        try {
            return LocalDate.parse(dateFrom.trim())
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ex) {
            throw new BizException(400, "开始日期格式不合法: " + dateFrom, ex);
        }
    }

    private Long resolveDateEndExclusiveMillis(String dateTo) {
        if (!StringUtils.hasText(dateTo)) {
            return null;
        }
        try {
            return LocalDate.parse(dateTo.trim())
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ex) {
            throw new BizException(400, "结束日期格式不合法: " + dateTo, ex);
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private long parseUpdatedAtMillis(String updatedAt) {
        if (!StringUtils.hasText(updatedAt)) {
            return 0L;
        }
        try {
            return OffsetDateTime.parse(updatedAt).toInstant().toEpochMilli();
        } catch (DateTimeParseException ex) {
            return 0L;
        }
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
