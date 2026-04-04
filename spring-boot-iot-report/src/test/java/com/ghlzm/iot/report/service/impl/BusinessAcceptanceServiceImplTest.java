package com.ghlzm.iot.report.service.impl;

import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
