package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunLaunchVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessAcceptanceServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadLatestPackageSummaryFromRunLedger() throws Exception {
        Path automationDir = Files.createDirectories(tempDir.resolve("config").resolve("automation"));
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));

        Files.writeString(
                automationDir.resolve("acceptance-registry.json"),
                """
                        {
                          "version": "1.0.0",
                          "scenarios": [
                            {
                              "id": "auth.browser-smoke",
                              "title": "登录与产品设备浏览器冒烟",
                              "module": "device",
                              "runnerType": "browserPlan",
                              "scope": "delivery",
                              "blocking": "blocker",
                              "dependsOn": [],
                              "runner": {}
                            },
                            {
                              "id": "system.api-smoke",
                              "title": "系统与业务 API 冒烟",
                              "module": "system",
                              "runnerType": "apiSmoke",
                              "scope": "delivery",
                              "blocking": "warning",
                              "dependsOn": [],
                              "runner": {}
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        Files.writeString(
                automationDir.resolve("business-acceptance-packages.json"),
                """
                        {
                          "version": "1.0.0",
                          "packages": [
                            {
                              "packageCode": "product-device",
                              "packageName": "产品与设备",
                              "description": "覆盖产品与设备交付验收。",
                              "targetRoles": ["acceptance", "product", "manager"],
                              "supportedEnvironments": ["dev", "test"],
                              "defaultAccountTemplate": "acceptance-default",
                              "modules": [
                                {
                                  "moduleCode": "product-create",
                                  "moduleName": "产品新增",
                                  "scenarioRefs": ["auth.browser-smoke"],
                                  "suggestedDirection": "needsReview",
                                  "fallbackFailure": {
                                    "stepLabel": "提交产品新增表单",
                                    "apiRef": "POST /device/product/add",
                                    "pageAction": "点击新增产品并提交",
                                    "summary": "产品新增链路需要复核。"
                                  }
                                }
                              ]
                            }
                          ],
                          "accountTemplates": [
                            {
                              "templateCode": "acceptance-default",
                              "templateName": "验收账号模板",
                              "username": "biz_demo",
                              "roleHint": "业务验收",
                              "supportedEnvironments": ["dev", "test"]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        Files.writeString(
                logsDir.resolve("registry-run-20260404153000.json"),
                """
                        {
                          "runId": "20260404153000",
                          "options": {
                            "packageCode": "product-device",
                            "environmentCode": "dev",
                            "accountTemplate": "acceptance-default",
                            "selectedModules": "product-create"
                          },
                          "summary": {
                            "total": 1,
                            "passed": 0,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "auth.browser-smoke",
                              "runnerType": "browserPlan",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "browser smoke failed",
                              "evidenceFiles": []
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        BusinessAcceptanceServiceImpl service = new BusinessAcceptanceServiceImpl(
                tempDir,
                automationDir.resolve("business-acceptance-packages.json"),
                automationDir.resolve("acceptance-registry.json"),
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        BusinessAcceptancePackageVO pkg = service.listPackages().get(0);

        assertThat(pkg.getPackageCode()).isEqualTo("product-device");
        assertThat(pkg.getLatestResult()).isNotNull();
        assertThat(pkg.getLatestResult().getStatus()).isEqualTo("failed");
        assertThat(pkg.getLatestResult().getRunId()).isEqualTo("20260404153000");
        assertThat(service.listAccountTemplates()).hasSize(1);
        assertThat(service.listAccountTemplates().get(0).getTemplateCode()).isEqualTo("acceptance-default");
    }

    @Test
    void shouldLaunchBusinessAcceptanceRunAndKeepRunningStatusBeforeWorkerCallback() throws Exception {
        Path automationDir = Files.createDirectories(tempDir.resolve("config").resolve("automation"));
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        writeRegistryConfig(automationDir);
        writePackageConfig(automationDir);

        CapturingBusinessAcceptanceService service = new CapturingBusinessAcceptanceService(
                tempDir,
                automationDir.resolve("business-acceptance-packages.json"),
                automationDir.resolve("acceptance-registry.json"),
                logsDir
        );

        var request = new com.ghlzm.iot.report.vo.BusinessAcceptanceRunRequest();
        request.setPackageCode("product-device");
        request.setEnvironmentCode("dev");
        request.setAccountTemplateCode("acceptance-default");
        request.setModuleCodes(List.of("product-create"));

        BusinessAcceptanceRunLaunchVO launch = service.launchRun(request);

        assertThat(launch.getJobId()).isNotBlank();
        assertThat(launch.getStatus()).isEqualTo("running");
        assertThat(service.getRunStatus(launch.getJobId()).getStatus()).isEqualTo("running");
        assertThat(service.capturedCommand).isNotNull();
        assertThat(service.capturedCommand).anyMatch(item -> item.contains("--package-code=product-device"));
        assertThat(service.capturedCommand).anyMatch(item -> item.contains("--environment-code=dev"));
        assertThat(service.capturedCommand).anyMatch(item -> item.contains("--account-template=acceptance-default"));
        assertThat(service.capturedCommand).anyMatch(item -> item.contains("--selected-modules=product-create"));
        assertThat(service.capturedRegistryPath).isNotNull();
        assertThat(Files.readString(service.capturedRegistryPath, StandardCharsets.UTF_8)).contains("auth.browser-smoke");
    }

    @Test
    void shouldAggregateRunResultToBusinessModules() throws Exception {
        Path automationDir = Files.createDirectories(tempDir.resolve("config").resolve("automation"));
        Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
        writeRegistryConfig(automationDir);
        writePackageConfig(automationDir);

        Files.writeString(
                logsDir.resolve("registry-run-20260404153000.json"),
                """
                        {
                          "runId": "20260404153000",
                          "options": {
                            "packageCode": "product-device",
                            "environmentCode": "dev",
                            "accountTemplate": "acceptance-default",
                            "selectedModules": "product-create,product-query"
                          },
                          "summary": {
                            "total": 2,
                            "passed": 1,
                            "failed": 1
                          },
                          "results": [
                            {
                              "scenarioId": "auth.browser-smoke",
                              "runnerType": "browserPlan",
                              "status": "failed",
                              "blocking": "blocker",
                              "summary": "browser smoke failed",
                              "evidenceFiles": []
                            },
                            {
                              "scenarioId": "system.api-smoke",
                              "runnerType": "apiSmoke",
                              "status": "passed",
                              "blocking": "warning",
                              "summary": "api smoke passed",
                              "evidenceFiles": []
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        BusinessAcceptanceServiceImpl service = new BusinessAcceptanceServiceImpl(
                tempDir,
                automationDir.resolve("business-acceptance-packages.json"),
                automationDir.resolve("acceptance-registry.json"),
                logsDir,
                JsonMapper.builder().findAndAddModules().build()
        );

        BusinessAcceptanceResultVO result = service.getRunResult("product-device", "20260404153000");

        assertThat(result.getStatus()).isEqualTo("failed");
        assertThat(result.getPassedModuleCount()).isEqualTo(1);
        assertThat(result.getFailedModuleNames()).containsExactly("产品新增");
        assertThat(result.getJumpToAutomationResultsPath()).isEqualTo("/automation-results?runId=20260404153000");
        var failedModule = result.getModules().stream()
                .filter(item -> "product-create".equals(item.getModuleCode()))
                .findFirst()
                .orElseThrow();
        assertThat(failedModule.getFailureDetails()).hasSize(1);
        assertThat(failedModule.getFailureDetails().get(0).getStepLabel()).isEqualTo("提交产品新增表单");
        assertThat(failedModule.getFailureDetails().get(0).getApiRef()).isEqualTo("POST /device/product/add");
        assertThat(failedModule.getFailureDetails().get(0).getPageAction()).isEqualTo("点击新增产品并提交");
    }

    private void writeRegistryConfig(Path automationDir) throws Exception {
        Files.writeString(
                automationDir.resolve("acceptance-registry.json"),
                """
                        {
                          "version": "1.0.0",
                          "scenarios": [
                            {
                              "id": "auth.browser-smoke",
                              "title": "登录与产品设备浏览器冒烟",
                              "module": "device",
                              "runnerType": "browserPlan",
                              "scope": "delivery",
                              "blocking": "blocker",
                              "dependsOn": [],
                              "runner": {}
                            },
                            {
                              "id": "system.api-smoke",
                              "title": "系统与业务 API 冒烟",
                              "module": "system",
                              "runnerType": "apiSmoke",
                              "scope": "delivery",
                              "blocking": "warning",
                              "dependsOn": [],
                              "runner": {}
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }

    private void writePackageConfig(Path automationDir) throws Exception {
        Files.writeString(
                automationDir.resolve("business-acceptance-packages.json"),
                """
                        {
                          "version": "1.0.0",
                          "packages": [
                            {
                              "packageCode": "product-device",
                              "packageName": "产品与设备",
                              "description": "覆盖产品与设备交付验收。",
                              "targetRoles": ["acceptance", "product", "manager"],
                              "supportedEnvironments": ["dev", "test"],
                              "defaultAccountTemplate": "acceptance-default",
                              "modules": [
                                {
                                  "moduleCode": "product-create",
                                  "moduleName": "产品新增",
                                  "scenarioRefs": ["auth.browser-smoke"],
                                  "suggestedDirection": "needsReview",
                                  "fallbackFailure": {
                                    "stepLabel": "提交产品新增表单",
                                    "apiRef": "POST /device/product/add",
                                    "pageAction": "点击新增产品并提交",
                                    "summary": "产品新增链路需要复核。"
                                  }
                                },
                                {
                                  "moduleCode": "product-query",
                                  "moduleName": "产品查询",
                                  "scenarioRefs": ["system.api-smoke"],
                                  "suggestedDirection": "needsReview",
                                  "fallbackFailure": {
                                    "stepLabel": "打开产品列表并检索产品",
                                    "apiRef": "GET /device/product/{id}",
                                    "pageAction": "输入产品关键字并查询",
                                    "summary": "产品查询链路需要复核。"
                                  }
                                }
                              ]
                            }
                          ],
                          "accountTemplates": [
                            {
                              "templateCode": "acceptance-default",
                              "templateName": "验收账号模板",
                              "username": "biz_demo",
                              "roleHint": "业务验收",
                              "supportedEnvironments": ["dev", "test"]
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }

    private static final class CapturingBusinessAcceptanceService extends BusinessAcceptanceServiceImpl {

        private List<String> capturedCommand;
        private Path capturedRegistryPath;

        private CapturingBusinessAcceptanceService(
                Path workspaceRoot,
                Path packagesConfigPath,
                Path acceptanceRegistryPath,
                Path resultsDir
        ) {
            super(
                    workspaceRoot,
                    packagesConfigPath,
                    acceptanceRegistryPath,
                    resultsDir,
                    JsonMapper.builder().findAndAddModules().build()
            );
        }

        @Override
        protected void submitLaunch(String jobId, Path derivedRegistryPath, List<String> command) {
            this.capturedRegistryPath = derivedRegistryPath;
            this.capturedCommand = command;
        }
    }
}
