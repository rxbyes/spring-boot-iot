# No-Code Device Onboarding P0-3 Template Pack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `/device-onboarding` 补齐模板包真相、最小 CRUD 和“新建接入案例一键预填”能力，作为后续批量接入的复用资产底座。

**Architecture:** 继续把 `/device-onboarding` 作为统一接入编排入口，不新增第二套路由。模板包使用单表 `iot_onboarding_template_pack` 承载已发布治理资产的组合引用；接入案例只保存 `templatePackId` 关联与被应用后的显式字段，不把模板包当作运行时真相。前后端先交付最小读写和预填，不在本轮引入审批、发布快照或批量能力。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, JUnit 5, Schema Registry

---

## Scope Check

本计划只覆盖 `P0-3 接入模板包` 最小闭环，包含：

1. schema registry 新增 `iot_onboarding_template_pack`
2. 模板包后端分页 / 新建 / 编辑
3. `/device-onboarding` 页面内展示模板包列表和维护表单
4. 接入案例支持选择模板包并一键预填 `scenarioCode / deviceFamily / protocolFamilyCode / decryptProfileCode / protocolTemplateCode`
5. 接入案例保存 `templatePackId`

本计划明确不做：

1. 模板包审批、发布、回滚
2. 模板包批量导入导出
3. 模板包自动回写产品、合同批次或设备编码
4. `P1-1` 批量创建案例 / 批量触发验收 / 失败分组

## File Structure

### Schema And Docs

- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\sql\init.sql`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\schema\generated\mysql-schema-sync.json`
- Regenerate: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\resources\schema\runtime-bootstrap\mysql-active-schema.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

### Device Module

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\OnboardingTemplatePack.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\OnboardingTemplatePackMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackPageQueryDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackUpdateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\OnboardingTemplatePackService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\OnboardingTemplatePackServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\OnboardingTemplatePackController.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\OnboardingTemplatePackVO.java`

### Frontend

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`

### Tests

- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\OnboardingTemplatePackServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\OnboardingTemplatePackControllerTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

## Task 1: Add template pack schema and backend CRUD

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\schema\mysql\device-domain.json`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\OnboardingTemplatePack.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\OnboardingTemplatePackMapper.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackCreateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackPageQueryDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\OnboardingTemplatePackUpdateDTO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\OnboardingTemplatePackService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\OnboardingTemplatePackServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\controller\OnboardingTemplatePackController.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\OnboardingTemplatePackVO.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\OnboardingTemplatePackServiceImplTest.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\controller\OnboardingTemplatePackControllerTest.java`

- [ ] **Step 1: Write failing tests for template pack page/create/update behavior**
- [ ] **Step 2: Run focused device tests and verify RED**
- [ ] **Step 3: Add `iot_onboarding_template_pack` to schema registry and regenerate artifacts**
- [ ] **Step 4: Implement entity/mapper/service/controller and make tests pass**
- [ ] **Step 5: Re-run focused tests and verify GREEN**

## Task 2: Extend onboarding cases with `templatePackId` and prefill support

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\DeviceOnboardingCase.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseCreateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\dto\DeviceOnboardingCaseUpdateDTO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\vo\DeviceOnboardingCaseVO.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DeviceOnboardingCaseServiceImplTest.java`

- [ ] **Step 1: Write failing tests for case `templatePackId` persistence and invalid template validation**
- [ ] **Step 2: Run focused case service tests and verify RED**
- [ ] **Step 3: Implement `templatePackId` fields, validation and VO projection**
- [ ] **Step 4: Re-run focused case tests and verify GREEN**

## Task 3: Surface template pack management and one-click prefill in `/device-onboarding`

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\types\api.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\api\deviceOnboarding.ts`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\views\DeviceOnboardingWorkbenchView.vue`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-ui\src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: Write failing UI tests for template pack loading, apply-to-form and template CRUD submit**
- [ ] **Step 2: Run focused Vitest and verify RED**
- [ ] **Step 3: Implement template pack list/form and apply-to-case prefill**
- [ ] **Step 4: Re-run focused Vitest and verify GREEN**

## Task 4: Sync docs and verify

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

- [ ] **Step 1: 更新 P0-2 验收链与 P0-3 模板包行为/API/schema/UI 文档**
- [ ] **Step 2: 执行 schema render/check、focused Maven tests、focused Vitest、前端 build**
- [ ] **Step 3: 记录仍未完成的 P1-1 边界**

## Self-Review

### Spec coverage

本计划覆盖：

1. 模板包真相对象
2. 模板包最小 CRUD
3. 接入案例模板预填
4. P0-2 文档收口

本计划未覆盖但明确留待后续：

1. 模板包审批/发布
2. 模板包批量应用
3. 批量接入案例与批量验收

### Placeholder scan

已检查，无 `TODO` / `TBD` / 空白实现步骤。

### Type consistency

模板包对象统一命名为：

1. `OnboardingTemplatePack`
2. `OnboardingTemplatePackService`
3. `OnboardingTemplatePackVO`
4. `templatePackId`

## Execution Handoff

Plan complete and saved to `E:\idea\ghatg\spring-boot-iot\docs\superpowers\plans\2026-04-18-no-code-device-onboarding-p0-3-template-pack-implementation-plan.md`.

本会话内用户已明确要求持续执行，因此下一步直接按计划进入 TDD 实施。
