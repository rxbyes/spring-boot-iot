package com.ghlzm.iot.report.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AutomationResultArchiveIndexServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void springBeanFactoryShouldResolveConfiguredResultsDirConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AutomationResultArchiveIndexServiceImpl.class);

            context.refresh();

            assertThat(context.getBean(AutomationResultArchiveIndexServiceImpl.class))
                    .isInstanceOf(AutomationResultArchiveIndexServiceImpl.class);
        }
    }

    @Test
    void shouldRefreshLatestIndexAndCollectFacets() throws Exception {
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        Files.writeString(
                logsDir.resolve("registry-run-20260425153000.json"),
                """
                        {
                          "runId": "20260425153000",
                          "options": {
                            "packageCode": "quality-factory-p0",
                            "environmentCode": "dev"
                          },
                          "summary": {
                            "total": 1,
                            "passed": 1,
                            "failed": 0
                          },
                          "results": [
                            {
                              "scenarioId": "quality-factory.login",
                              "runnerType": "browserPlan",
                              "status": "passed",
                              "blocking": "blocker",
                              "summary": "passed",
                              "evidenceFiles": [
                                "logs/acceptance/browser-plan-20260425153000.json"
                              ]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(
                logsDir.resolve("registry-run-20260425160000.json"),
                """
                        {
                          "runId": "20260425160000",
                          "options": {
                            "packageCode": "product-governance-p1",
                            "environmentCode": "sit"
                          },
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "product.publish",
                              "runnerType": "riskDrill",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "接口响应异常 500",
                              "details": {
                                "moduleCode": "product",
                                "moduleName": "产品治理",
                                "stepLabel": "提交产品发布",
                                "apiRef": "POST /device/product/release",
                                "pageAction": "点击发布产品"
                              }
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
        Files.writeString(logsDir.resolve("registry-run-20260425170000.json"), "{broken-json", StandardCharsets.UTF_8);
        Files.writeString(logsDir.resolve("browser-plan-20260425153000.json"), "{\"ok\":true}", StandardCharsets.UTF_8);

        AutomationResultArchiveIndexServiceImpl service = new AutomationResultArchiveIndexServiceImpl(
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        var refresh = service.refreshIndex();
        var index = service.loadArchiveIndex(false);
        var facets = service.listFacets();

        assertThat(refresh.getIndexedRuns()).isEqualTo(2);
        assertThat(refresh.getSkippedFiles()).isEqualTo(1);
        assertThat(index.getRuns()).hasSize(2);
        assertThat(index.getRuns().get(0).getRunId()).isEqualTo("20260425160000");
        assertThat(index.getRuns().get(0).getFailureSummary().getPrimaryCategory()).isEqualTo("接口");
        assertThat(index.getRuns().get(0).getFailedModules()).hasSize(1);
        assertThat(index.getRuns().get(0).getFailedModules().get(0).getDiagnosis().getCategory()).isEqualTo("接口");
        assertThat(index.getRuns().get(0).getFailedScenarios()).hasSize(1);
        assertThat(index.getRuns().get(0).getFailedScenarios().get(0).getDiagnosis().getCategory()).isEqualTo("接口");
        assertThat(index.getRuns().get(1).getEvidenceItems()).hasSize(2);
        assertThat(index.getSkippedFiles()).hasSize(1);
        assertThat(facets.getPackageCodes()).containsExactly("product-governance-p1", "quality-factory-p0");
        assertThat(facets.getEnvironmentCodes()).containsExactly("dev", "sit");
        assertThat(Files.exists(logsDir.resolve("automation-result-index.latest.json"))).isTrue();
    }
}
