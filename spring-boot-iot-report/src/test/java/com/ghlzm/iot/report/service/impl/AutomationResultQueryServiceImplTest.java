package com.ghlzm.iot.report.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
                            "scope": "delivery"
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
                .hasMessageContaining("证据文件不属于当前运行结果");
    }
}
