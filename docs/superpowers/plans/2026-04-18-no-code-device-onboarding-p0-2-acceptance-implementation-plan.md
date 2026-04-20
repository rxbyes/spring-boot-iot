# No-Code Device Onboarding P0-2 Acceptance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/device-onboarding` 接入案例补齐标准接入验收触发、运行回写、结果摘要与 `/automation-results` 深链，形成 P0-2 最小闭环。

**Architecture:** 继续复用 report 模块已有的自动化结果中心与运行台账格式，不新增平行结果模型。设备域只保存接入案例的验收引用和摘要；report 模块负责异步执行 8 个固定检查项并写入 `logs/acceptance/registry-run-<runId>.json`，前端只做触发、摘要展示和跳转。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, JUnit 5, Maven, Schema Registry

---

## Scope Check

本计划只覆盖 `P0-2 标准接入验收链` 的最小切片，包含：

1. 接入案例增加验收设备编码、验收任务引用与结果摘要
2. `POST /api/device/onboarding/cases/{caseId}/start-acceptance`
3. `refresh-status` 自动轮询验收运行态并回写 `runId`
4. report 模块异步执行 8 个固定检查项并产出统一运行结果
5. `/device-onboarding` 页面展示验收摘要并跳转 `/automation-results?runId=...`

本计划明确不做：

1. 模板包对象与模板管理页
2. 批量创建 / 批量触发验收
3. 接入案例自动创建设备或自动发布协议 / 合同
4. 业务验收台 `/business-acceptance` 的新入口页改造

## File Structure

### Schema And Docs

- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\sql\init.sql`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\schema\generated\mysql-schema-sync.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

### Device Module

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingAcceptanceGateway.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\DeviceOnboardingAcceptanceLaunch.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\DeviceOnboardingAcceptanceProgress.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingAcceptanceSummaryVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`

### Report Module

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\pom.xml`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\DeviceOnboardingAcceptanceService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceGatewayImpl.java`

### Frontend

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`

### Tests

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\test\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\test\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceGatewayImplTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

## Task 1: Extend onboarding cases with acceptance references and state

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingAcceptanceSummaryVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`

- [ ] **Step 1: Write failing service tests for acceptance-ready derivation**
- [ ] **Step 2: Run device service tests and verify RED**
- [ ] **Step 3: Add schema fields `device_code`, `acceptance_job_id`, `acceptance_run_id` and regenerate artifacts**
- [ ] **Step 4: Implement minimal entity / DTO / VO fields and acceptance summary parsing**
- [ ] **Step 5: Re-run device service tests and verify GREEN**

## Task 2: Add backend acceptance orchestration in device and report modules

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingAcceptanceGateway.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\DeviceOnboardingAcceptanceLaunch.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\DeviceOnboardingAcceptanceProgress.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\pom.xml`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\DeviceOnboardingAcceptanceService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\main\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceGatewayImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\test\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-report\src\test\java\com\ghlzm\iot\report\service\impl\DeviceOnboardingAcceptanceGatewayImplTest.java`

- [ ] **Step 1: Write failing tests for `startAcceptance` and report-side result aggregation**
- [ ] **Step 2: Run focused device/report tests and verify RED**
- [ ] **Step 3: Implement 8 固定检查项的最小执行与统一结果汇总**
- [ ] **Step 4: Implement `start-acceptance` / `refresh-status` orchestration and controller endpoint**
- [ ] **Step 5: Re-run focused device/report tests and verify GREEN**

## Task 3: Surface acceptance actions and summaries in `/device-onboarding`

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: Write failing UI tests for trigger/run summary/jump actions**
- [ ] **Step 2: Run the focused UI test and verify RED**
- [ ] **Step 3: Implement `触发验收`、验收摘要展示和 `/automation-results` 跳转**
- [ ] **Step 4: Re-run the focused UI test and verify GREEN**

## Task 4: Update docs and verify end-to-end

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Review: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: 更新行为/API/schema/验收口径文档**
- [ ] **Step 2: 执行 schema render/check、device/report 测试、前端测试**
- [ ] **Step 3: 记录仍未完成的 `P0-3/P1-1` 边界**

## Self-Review

### Spec coverage

本计划覆盖：

1. 接入案例触发标准接入验收
2. 回写 jobId / runId / 摘要结果
3. `/automation-results` 深链
4. 8 个固定检查项的最小闭环

本计划未覆盖但明确留待后续：

1. 模板包管理
2. 批量接入案例与批量验收

### Placeholder scan

已检查，无 `TODO` / `TBD` / “后续补充实现” 式空步骤。

### Type consistency

统一使用：

1. `DeviceOnboardingAcceptanceGateway`
2. `DeviceOnboardingAcceptanceLaunch`
3. `DeviceOnboardingAcceptanceProgress`
4. `DeviceOnboardingAcceptanceSummaryVO`

## Execution Handoff

Plan complete and saved to `E:\idea\ghatg\spring-boot-iot\docs\superpowers\plans\2026-04-18-no-code-device-onboarding-p0-2-acceptance-implementation-plan.md`.

本会话已由用户明确要求继续执行，因此下一步直接按计划进入 TDD 实施。
