package com.ghlzm.iot.report.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutomationResultQueryServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldListRecentRegistryRunsFromAcceptanceLogs() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-20260402144931.json"),
                """
                        {
                          "runId": "20260402144931",
                          "summary": {
                            "total": 1,
                            "passed": 1,
                            "failed": 0
                          },
                          "results": [
                            {
                              "scenarioId": "auth.browser-smoke",
                              "runnerType": "browserPlan",
                              "status": "passed",
                              "blocking": "blocker",
                              "summary": "browser smoke passed",
                              "evidenceFiles": [
                                "logs/acceptance/browser-plan-20260402144931.json"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(
                logsDir.resolve("registry-run-20260402155432.json"),
                """
                        {
                          "runId": "20260402155432",
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "risk.full-drill.red-chain",
                              "runnerType": "riskDrill",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "simulated failure",
                              "evidenceFiles": [
                                "logs/acceptance/risk-drill-1775116282733.json"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(
                logsDir.resolve("risk-drill-1775116282733.json"),
                """
                        {
                          "scenarioId": "risk.full-drill.red-chain"
                        }
                        """,
                StandardCharsets.UTF_8
        );

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var recentRuns = service.listRecentRuns(10);

        assertThat(recentRuns).hasSize(2);
        assertThat(recentRuns.get(0).getRunId()).isEqualTo("20260402155432");
        assertThat(recentRuns.get(0).getSummary().getFailed()).isEqualTo(1);
        assertThat(recentRuns.get(0).getFailedScenarioIds()).containsExactly("risk.full-drill.red-chain");
        assertThat(recentRuns.get(0).getRelatedEvidenceFiles())
                .containsExactly("logs/acceptance/risk-drill-1775116282733.json");
    }

    @Test
    void shouldLoadRegistryRunDetailByRunId() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-20260402155432.json"),
                """
                        {
                          "runId": "20260402155432",
                          "options": {
                            "scope": "delivery",
                            "packageCode": "product-device",
                            "environmentCode": "dev",
                            "accountTemplate": "acceptance-default",
                            "selectedModules": "product-create,product-query"
                          },
                          "registryVersion": "1.0.0",
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "risk.full-drill.red-chain",
                              "runnerType": "riskDrill",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "simulated failure",
                              "evidenceFiles": [
                                "logs/acceptance/risk-drill-1775116282733.json"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var detail = service.getRunDetail("20260402155432");

        assertThat(detail.getRunId()).isEqualTo("20260402155432");
        assertThat(detail.getReportPath()).isEqualTo("logs/acceptance/registry-run-20260402155432.json");
        assertThat(detail.getOptions()).containsEntry("packageCode", "product-device");
        assertThat(detail.getOptions()).containsEntry("environmentCode", "dev");
        assertThat(detail.getOptions()).containsEntry("accountTemplate", "acceptance-default");
        assertThat(detail.getOptions()).containsEntry("selectedModules", "product-create,product-query");
        assertThat(detail.getSummary().getFailed()).isEqualTo(1);
        assertThat(detail.getResults()).hasSize(1);
        assertThat(detail.getResults().get(0).getScenarioId()).isEqualTo("risk.full-drill.red-chain");
    }

    @Test
    void shouldListEvidenceForRunIncludingRunSummaryAndScenarioArtifacts() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-20260402155432.json"),
                """
                        {
                          "runId": "20260402155432",
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "risk.full-drill.red-chain",
                              "runnerType": "riskDrill",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "simulated failure",
                              "evidenceFiles": [
                                "logs/acceptance/risk-drill-1775116282733.md",
                                "logs/acceptance/risk-drill-1775116282733.json"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(logsDir.resolve("risk-drill-1775116282733.md"), "# Risk Drill", StandardCharsets.UTF_8);
        Files.writeString(logsDir.resolve("risk-drill-1775116282733.json"), "{\"result\":true}", StandardCharsets.UTF_8);

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var evidenceItems = service.listRunEvidence("20260402155432");

        assertThat(evidenceItems).hasSize(3);
        assertThat(evidenceItems.get(0).getPath()).isEqualTo("logs/acceptance/registry-run-20260402155432.json");
        assertThat(evidenceItems.get(0).getCategory()).isEqualTo("run-summary");
        assertThat(evidenceItems.get(1).getPath()).isEqualTo("logs/acceptance/risk-drill-1775116282733.md");
    }

    @Test
    void shouldPreviewAllowedEvidenceContentAndRejectUnrelatedFiles() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-20260402155432.json"),
                """
                        {
                          "runId": "20260402155432",
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "risk.full-drill.red-chain",
                              "status": "failed",
                              "blocking": "blocker",
                              "evidenceFiles": [
                                "logs/acceptance/risk-drill-1775116282733.md"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(logsDir.resolve("risk-drill-1775116282733.md"), "# Risk Drill\n\n- status: failed", StandardCharsets.UTF_8);
        Files.writeString(logsDir.resolve("other-note.txt"), "not linked", StandardCharsets.UTF_8);

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var preview = service.getEvidenceContent("20260402155432", "logs/acceptance/risk-drill-1775116282733.md");

        assertThat(preview.getPath()).isEqualTo("logs/acceptance/risk-drill-1775116282733.md");
        assertThat(preview.getCategory()).isEqualTo("markdown");
        assertThat(preview.getContent()).contains("# Risk Drill");

        assertThatThrownBy(() -> service.getEvidenceContent("20260402155432", "logs/acceptance/other-note.txt"))
                .hasMessageContaining("other-note.txt");
    }

    @Test
    void shouldPageRegistryRunsWithKeywordStatusRunnerAndDateFilters() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        writeRegistryRun(
                logsDir,
                "20260401100000",
                "2026-04-01T10:00:00Z",
                "browserPlan",
                "passed",
                "auth.browser-smoke",
                "browser smoke passed"
        );
        writeRegistryRun(
                logsDir,
                "20260402120000",
                "2026-04-02T12:00:00Z",
                "riskDrill",
                "failed",
                "risk.mid-drill.orange-chain",
                "orange drill failed"
        );
        writeRegistryRun(
                logsDir,
                "20260403130000",
                "2026-04-03T13:00:00Z",
                "riskDrill",
                "failed",
                "risk.full-drill.red-chain",
                "red drill failed"
        );

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var page = service.pageRuns(1, 1, "red-chain", "failed", "riskDrill", "2026-04-02", "2026-04-03");

        assertThat(page.getTotal()).isEqualTo(1L);
        assertThat(page.getPageNum()).isEqualTo(1L);
        assertThat(page.getPageSize()).isEqualTo(1L);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getRunId()).isEqualTo("20260403130000");
        assertThat(page.getRecords().get(0).getStatus()).isEqualTo("failed");
        assertThat(page.getRecords().get(0).getRunnerTypes()).containsExactly("riskDrill");
    }

    @Test
    void shouldSkipBrokenRegistryRunFilesWhenPaging() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        writeRegistryRun(
                logsDir,
                "20260402120000",
                "2026-04-02T12:00:00Z",
                "riskDrill",
                "failed",
                "risk.full-drill.red-chain",
                "red drill failed"
        );
        Path brokenFile = logsDir.resolve("registry-run-20260403130000.json");
        Files.writeString(brokenFile, "{ invalid json", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(brokenFile, FileTime.from(Instant.parse("2026-04-03T13:00:00Z")));

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var page = service.pageRuns(1, 10, null, null, null, null, null);

        assertThat(page.getTotal()).isEqualTo(1L);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getRunId()).isEqualTo("20260402120000");
    }

    @Test
    void shouldReturnEmptyPageWhenResultsDirectoryDoesNotExist() {
        Path logsDir = tempDir.resolve("missing").resolve("acceptance");

        AutomationResultQueryServiceImpl service = new AutomationResultQueryServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var page = service.pageRuns(2, 20, null, null, null, null, null);

        assertThat(page.getTotal()).isEqualTo(0L);
        assertThat(page.getPageNum()).isEqualTo(2L);
        assertThat(page.getPageSize()).isEqualTo(20L);
        assertThat(page.getRecords()).isEmpty();
    }

    private void writeRegistryRun(
            Path logsDir,
            String runId,
            String updatedAt,
            String runnerType,
            String status,
            String scenarioId,
            String summary
    ) throws Exception {
        Path file = logsDir.resolve("registry-run-" + runId + ".json");
        Files.writeString(
                file,
                """
                        {
                          "runId": "%s",
                          "summary": {
                            "total": 1,
                            "passed": %s,
                            "failed": %s
                          },
                          "results": [
                            {
                              "scenarioId": "%s",
                              "runnerType": "%s",
                              "status": "%s",
                              "blocking": "blocker",
                              "summary": "%s",
                              "evidenceFiles": [
                                "logs/acceptance/%s.json"
                              ]
                            }
                          ]
                        }
                        """.formatted(
                        runId,
                        "passed".equals(status) ? "1" : "0",
                        "failed".equals(status) ? "1" : "0",
                        scenarioId,
                        runnerType,
                        status,
                        summary,
                        runnerType + "-" + runId
                ),
                StandardCharsets.UTF_8
        );
        Files.writeString(
                logsDir.resolve(runnerType + "-" + runId + ".json"),
                """
                        {
                          "scenarioId": "%s"
                        }
                        """.formatted(scenarioId),
                StandardCharsets.UTF_8
        );
        Files.setLastModifiedTime(file, FileTime.from(Instant.parse(updatedAt)));
    }
}
