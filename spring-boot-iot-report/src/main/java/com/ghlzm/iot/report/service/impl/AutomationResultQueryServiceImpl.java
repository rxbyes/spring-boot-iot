package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationResultRunDetailVO;
import com.ghlzm.iot.report.vo.AutomationResultRunResultVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import com.ghlzm.iot.report.vo.AutomationResultSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path resultsDir;
    private final ObjectMapper objectMapper;

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
        if (!StringUtils.hasText(runId)) {
            throw new BizException(400, "运行编号不能为空");
        }

        Path file = resultsDir.resolve("registry-run-" + runId.trim() + ".json").normalize();
        if (!Files.isRegularFile(file)) {
            throw new BizException(404, "未找到运行结果: " + runId);
        }

        return readRunDetail(file);
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

    private AutomationResultRunDetailVO readRunDetail(Path file) {
        try {
            AutomationResultRunDetailVO detail = objectMapper.readValue(
                    Files.readString(file),
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
        Set<String> evidenceFiles = new LinkedHashSet<>();
        normalizeResults(results).stream()
                .map(AutomationResultRunResultVO::getEvidenceFiles)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(this::normalizeEvidencePath)
                .filter(StringUtils::hasText)
                .forEach(evidenceFiles::add);
        return List.copyOf(evidenceFiles);
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
