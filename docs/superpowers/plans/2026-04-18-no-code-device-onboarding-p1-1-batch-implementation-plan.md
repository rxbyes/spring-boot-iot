# No-Code Device Onboarding P1-1 Batch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/device-onboarding` 补齐 `P1-1` 最小批量能力，包括批量创建接入案例、批量套用模板包、批量触发验收，以及按失败原因查看批量结果分组。

**Architecture:** 继续复用 `iot_device_onboarding_case` 与 `iot_onboarding_template_pack` 作为唯一编排与模板真相，不新增平行业务表。后端以批量接口聚合单案例能力并返回统一批量结果摘要；前端仍留在 `/device-onboarding` 同页，通过选中案例、批量录入和失败分组面板承接 `P1-1` 最小闭环。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, JUnit 5

---

## Scope Check

本计划只覆盖 `P1-1 批量治理与批量验收` 最小切片，包含：

1. 批量创建接入案例
2. 批量套用模板包
3. 批量触发验收
4. 批量结果摘要与失败分组

本计划明确不做：

1. 批量发布审批
2. 批量复杂回滚
3. 批量自动修复
4. AI 自动生成并自动发布规则
5. 新增第二套路由或第二套接入真相

## File Structure

### Device Module

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchTemplateApplyDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchStartAcceptanceDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseBatchResultVO.java`

### Frontend

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`

### Tests

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

### Docs

- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

## Task 1: Add batch DTOs, service contracts and result model

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchTemplateApplyDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseBatchStartAcceptanceDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseBatchResultVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\DeviceOnboardingCaseService.java`

- [ ] **Step 1: 定义批量创建、批量套模板、批量触发验收 DTO**
- [ ] **Step 2: 定义统一批量结果 VO，承载 success/failure/grouped failures**
- [ ] **Step 3: 扩展 `DeviceOnboardingCaseService` 批量接口签名**

## Task 2: Implement batch orchestration in onboarding case service

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`

- [ ] **Step 1: 为批量创建、批量套模板、批量触发验收补 failing tests**
- [ ] **Step 2: 运行聚焦 device service tests，确认 RED**
- [ ] **Step 3: 实现批量创建，逐条复用现有 case 创建规则并聚合失败原因**
- [ ] **Step 4: 实现批量套模板，统一覆盖模板管理字段并重算步骤状态**
- [ ] **Step 5: 实现批量触发验收，逐条复用现有验收入口并聚合失败分组**
- [ ] **Step 6: 重新运行聚焦 device service tests，确认 GREEN**

## Task 3: Expose batch endpoints in controller

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseController.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\DeviceOnboardingCaseControllerTest.java`

- [ ] **Step 1: 为 batch-create / batch-apply-template / batch-start-acceptance 补 controller tests**
- [ ] **Step 2: 运行 controller tests，确认 RED**
- [ ] **Step 3: 增加三个批量接口并透传当前用户 ID**
- [ ] **Step 4: 重新运行 controller tests，确认 GREEN**

## Task 4: Surface batch create, batch apply and grouped results in `/device-onboarding`

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: 为批量建档、选中案例、批量套模板、批量触发验收和失败分组展示补 failing UI tests**
- [ ] **Step 2: 运行聚焦 Vitest，确认 RED**
- [ ] **Step 3: 扩展 API/types 并在接入台增加批量操作区、勾选态和结果分组卡片**
- [ ] **Step 4: 重新运行聚焦 Vitest，确认 GREEN**

## Task 5: Sync docs and verify

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: 同步 `P0-2/P0-3` 已实现事实与 `P1-1` 新增批量能力**
- [ ] **Step 2: 运行聚焦 Maven tests、聚焦 Vitest、前端 build**
- [ ] **Step 3: 记录仍未进入范围的非目标项**

## Self-Review

### Spec coverage

本计划覆盖：

1. 批量创建接入案例
2. 批量套用模板包
3. 批量触发验收
4. 批量结果失败分组

本计划未覆盖但明确留待后续：

1. 批量审批发布
2. 批量复杂回滚
3. 批量自动修复
4. AI 自动生成并自动发布治理规则

### Placeholder scan

已检查，无 `TODO` / `TBD` / “后续再补”式空步骤。

### Type consistency

批量结果对象统一命名为：

1. `DeviceOnboardingCaseBatchResultVO`
2. `DeviceOnboardingCaseBatchCreateDTO`
3. `DeviceOnboardingCaseBatchTemplateApplyDTO`
4. `DeviceOnboardingCaseBatchStartAcceptanceDTO`

## Execution Handoff

Plan complete and saved to `E:\idea\ghatg\spring-boot-iot\docs\superpowers\plans\2026-04-18-no-code-device-onboarding-p1-1-batch-implementation-plan.md`.

本会话内用户已经明确要求继续执行，因此下一步直接按计划进入 TDD 实施。
