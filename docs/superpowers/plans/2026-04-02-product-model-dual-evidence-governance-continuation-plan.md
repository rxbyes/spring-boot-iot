# Product Model Dual Evidence Governance Continuation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to continue this work session-by-session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于已完成的“双证据 -> compare -> apply -> 正式模型”实现，在下一次会话中完成真实环境复验、必要补差和分支收尾。

**Architecture:** compare/apply 的后端接口、前端抽屉流程、对比表组件和正式模型写库链路已经在当前分支落地，下一阶段不再扩展新治理模型，而是围绕 `application-dev.yml` 的真实环境做闭环验收。正式模型仍只写入 `iot_product_model`，对象洞察台的设备属性快照继续只作为运行证据，不作为正式模型写入口。

**Tech Stack:** Spring Boot 4、Java 17、Vue 3、TypeScript、Maven、Vitest、真实 dev 环境

---

## Current Baseline

- 当前工作分支：`codex/product-model-dual-evidence-governance`
- 已完成提交：
  - `5bf2f96` `feat(device): add product model governance endpoints`
  - `c06d926` `feat(device): add product model governance compare flow`
  - `8b6757d` `feat(device): add product model governance apply flow`
  - `9d96a65` `feat(ui): add dual evidence product model governance flow`
  - `7536fbf` `docs: update dual evidence product model governance docs`
- 已通过验证：
  - `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest,ProductModelServiceImplTest test`
  - `cd spring-boot-iot-ui && npm exec -- vitest run src/__tests__/components/ProductModelDesignerDrawer.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  - `node scripts/run-quality-gates.mjs`
  - `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
- 当前未完成事项：
  - 真实环境后端 / 前端闭环验收
  - 如验收暴露偏差，做小步修补并补齐文档
  - 按 `finishing-a-development-branch` 决定分支去向

## File Map

- Check: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Check: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Check: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Check: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`
- Check: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Check: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Check: `spring-boot-iot-ui/src/api/product.ts`
- Check: `spring-boot-iot-ui/src/types/api.ts`
- Modify if real-environment acceptance changes behavior: `docs/02-业务功能与流程说明.md`
- Modify if real-environment acceptance changes behavior: `docs/03-接口规范与接口清单.md`
- Modify if real-environment acceptance changes behavior: `docs/04-数据库设计与初始化数据.md`
- Modify if real-environment acceptance changes behavior: `docs/06-前端开发与CSS规范.md`
- Modify if real-environment acceptance changes behavior: `docs/08-变更记录与技术债清单.md`
- Modify if real-environment acceptance changes behavior: `docs/15-前端优化与治理计划.md`
- Check if real-environment acceptance changes behavior: `README.md`
- Check if real-environment acceptance changes behavior: `AGENTS.md`

### Task 1: 恢复会话上下文并保护分支

**Files:**
- Check: `docs/superpowers/specs/2026-04-01-product-model-dual-evidence-governance-design.md`
- Check: `docs/superpowers/plans/2026-04-02-product-model-dual-evidence-governance-continuation-plan.md`

- [ ] **Step 1: 确认当前仍在隔离分支**

Run: `git branch --show-current`
Expected: 输出 `codex/product-model-dual-evidence-governance`

- [ ] **Step 2: 确认工作树干净**

Run: `git status --short`
Expected: 无输出

- [ ] **Step 3: 回看当前基线提交**

Run: `git log --oneline --decorate -5`
Expected: 顶部依次可见 `7536fbf`、`9d96a65`、`8b6757d`、`c06d926`、`5bf2f96`

### Task 2: 重建本地验证基线

**Files:**
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`

- [ ] **Step 1: 运行后端定向测试**

Run: `mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=ProductModelControllerTest,ProductModelServiceImplTest test`
Expected: `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 2: 运行前端双证据 Vitest**

Run: `cd spring-boot-iot-ui && npm exec -- vitest run src/__tests__/components/ProductModelDesignerDrawer.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
Expected: 输出 `Test Files  4 passed` 与 `Tests  6 passed`

- [ ] **Step 3: 运行整仓打包冒烟**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
Expected: 输出 `BUILD SUCCESS`

### Task 3: 执行真实环境闭环验收

**Files:**
- Check: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Check: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Check: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Check: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Check: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`

- [ ] **Step 1: 按真实环境配置启动后端**

Run: `mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
Expected: 使用 `dev` profile 启动，不回退 H2，不报缺失 compare/apply Bean 或 Mapper 错误

- [ ] **Step 2: 启动前端验收壳层**

Run: `pwsh -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`
Expected: 前端验收壳层可访问 `/products`

If `pwsh` is unavailable:
Run: `cd spring-boot-iot-ui && npm run dev -- --host 127.0.0.1`
Expected: 本地前端 dev 服务可访问，并在记录中明确说明为什么没有走 PowerShell 验收入口

- [ ] **Step 3: 在 `/products` 完成一次双证据治理闭环**

Acceptance checklist:
- 选择一个已有真实设备数据的产品，打开 `产品经营工作台 -> 物模型治理`
- 在手动证据区粘贴单设备样本 JSON，确认能生成候选
- 触发自动证据提炼，确认能加载运行期候选
- 在对比表中依次检查 `property / event / service` 切换、状态标签、建议动作和风险标记
- 至少完成一条 `create` 或 `update` 决策并执行 apply

Expected: 页面无前端报错、接口无 `500`、compare/apply 无字段契约错位

- [ ] **Step 4: 回看正式模型结果**

Run: 在同一产品的正式模型列表刷新数据
Expected: 已应用条目出现在正式模型中，且不会生成平行 draft/schema 表写入痕迹

- [ ] **Step 5: 如真实环境受阻，诚实记录阻塞点**

Run: 记录启动日志或接口报错摘要
Expected: 明确写出是环境、账号、数据还是网络阻塞，不得使用 H2 或废弃验收路径替代

### Task 4: 验收通过后的收尾

**Files:**
- Check: `docs/08-变更记录与技术债清单.md`
- Check: `README.md`
- Check: `AGENTS.md`

- [ ] **Step 1: 再跑一次本地质量门禁**

Run: `node scripts/run-quality-gates.mjs`
Expected: 输出 `All local minimum quality gates passed`

- [ ] **Step 2: 如果真实环境验收引发行为变化，原位补文档**

Run: 更新受影响文档后执行 `git diff -- docs README.md AGENTS.md`
Expected: 仅包含本次真实环境复验新增的行为、流程或阻塞说明

- [ ] **Step 3: 调用分支收尾流程**

Run: 按 `finishing-a-development-branch` 技能再次提供四个固定选项
Expected: 用户明确选择 `merge / PR / keep / discard` 之一后再执行后续动作

## Known Risks

- 真实环境验收依赖可用的 dev 数据、管理员账号与至少一个存在运行期证据的产品。
- 当前对象洞察台仍只提供设备属性快照证据，不应该在验收时把它当成正式模型写入口。
- 如果下一次会话只做验收而不改代码，不需要重复拆分新计划；直接按本文件顺序执行即可。
