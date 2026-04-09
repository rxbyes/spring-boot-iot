# 双核心域桥层与控制面升级 Program Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把当前 `spring-boot-iot` 从“语义治理最小切片 + 风险闭环最小切片”升级为“语义契约治理域 + 风险运营闭环域 + 风险指标桥层 + 治理控制面 + 运行底座”的可并行落地方案，并通过多工作树并行推进，最终统一回收至 `codex/dev`。

**Architecture:** 先由工作流 A 收口桥层真相、控制面对象与发布/回滚骨架，再由工作流 C 与 D 分别补齐可运维性、扩展模型和权限治理，最后由工作流 B 对接真实聚合指标与待办数据，完成经营驾驶舱和任务化工作台。所有工作流都必须以真实环境 `application-dev.yml` 为验收基线，不得回退到废弃 H2 验收路径。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Element Plus, TDengine, MySQL, Redis, Maven

---

## 分流矩阵

### 工作流 A：语义桥层与控制面基础

**分支 / 工作树：**

- Branch: `codex/semantic-bridge-control-plane`
- Worktree: `.worktrees/semantic-bridge-control-plane`

**受影响模块：**

- `spring-boot-iot-device`
- `spring-boot-iot-alarm`
- `spring-boot-iot-system`
- `spring-boot-iot-admin`

**核心目标：**

- 扩展 `risk_metric_catalog` 为桥层真相
- 给正式合同发布增加版本、影响分析、回滚和覆盖率视图
- 提供控制面后端 API 与最小审计闭环

### 工作流 B：治理经营驾驶舱与任务化工作台

**分支 / 工作树：**

- Branch: `codex/governance-task-workbench`
- Worktree: `.worktrees/governance-task-workbench`

**受影响模块：**

- `spring-boot-iot-ui`
- `spring-boot-iot-device`
- `spring-boot-iot-alarm`
- `spring-boot-iot-admin`

**核心目标：**

- 把驾驶舱从静态预设迁移为真实治理经营指标
- 把产品治理、风险对象、策略页升级为任务导向工作台

### 工作流 C：可运维性与链路复盘

**分支 / 工作树：**

- Branch: `codex/operability-drift-replay`
- Worktree: `.worktrees/operability-drift-replay`

**受影响模块：**

- `spring-boot-iot-message`
- `spring-boot-iot-device`
- `spring-boot-iot-alarm`
- `spring-boot-iot-system`
- `spring-boot-iot-admin`
- `scripts`

**核心目标：**

- 补齐字段漂移、合同差异、风险指标缺失告警
- 增加按发布批次、产品、设备、trace 的统一回放与复盘

### 工作流 D：扩展模型与安全治理

**分支 / 工作树：**

- Branch: `codex/extensibility-security-governance`
- Worktree: `.worktrees/extensibility-security-governance`

**受影响模块：**

- `spring-boot-iot-protocol`
- `spring-boot-iot-device`
- `spring-boot-iot-alarm`
- `spring-boot-iot-system`
- `spring-boot-iot-admin`

**核心目标：**

- 拆出协议/归一/语义映射/风险生成扩展点
- 收紧规范库、合同发布、风险指标、策略治理和密钥配置权限

## Task 1: 固化设计与并行边界

**Files:**
- Create: `docs/superpowers/specs/2026-04-06-dual-core-bridge-control-plane-design.md`
- Create: `docs/superpowers/plans/2026-04-06-dual-core-bridge-control-plane-program-implementation-plan.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: 把已确认设计沉淀为权威 spec**

输出内容必须覆盖五层架构、七条提升方向、四条工作流、合并顺序与非目标。

- [ ] **Step 2: 在技术债文档登记“当前最缺的是控制面、桥层扩展与运维治理”**

Run: `rg -n "控制面|风险指标目录|漂移告警|回滚" docs/08-变更记录与技术债清单.md`
Expected: 能定位新增条目，避免后续工作流重复定义问题边界。

- [ ] **Step 3: 仅暂存新 spec/plan 与技术债文档**

Run: `git add docs/superpowers/specs/2026-04-06-dual-core-bridge-control-plane-design.md docs/superpowers/plans/2026-04-06-dual-core-bridge-control-plane-program-implementation-plan.md docs/08-变更记录与技术债清单.md`
Expected: `git diff --cached --name-only` 只包含本任务文件。

## Task 2: 创建四个隔离工作树

**Files:**
- Create: `.worktrees/semantic-bridge-control-plane`
- Create: `.worktrees/governance-task-workbench`
- Create: `.worktrees/operability-drift-replay`
- Create: `.worktrees/extensibility-security-governance`

- [ ] **Step 1: 基于 `.worktrees` 创建工作树**

Run: `git worktree add .worktrees/semantic-bridge-control-plane -b codex/semantic-bridge-control-plane`
Expected: 输出包含新工作树路径与新分支名。

- [ ] **Step 2: 依次创建其余三个工作树**

Run: `git worktree add .worktrees/governance-task-workbench -b codex/governance-task-workbench`
Run: `git worktree add .worktrees/operability-drift-replay -b codex/operability-drift-replay`
Run: `git worktree add .worktrees/extensibility-security-governance -b codex/extensibility-security-governance`
Expected: 四个工作树都指向从 `codex/dev` 派生的新分支。

- [ ] **Step 3: 记录工作树清单**

Run: `git worktree list`
Expected: 返回四个新增工作树路径，便于多窗口打开与回收。

## Task 3: 工作流 A 实施任务包

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`

- [ ] **Step 1: 明确桥层 schema 与发布批次扩展口径**

交付内容必须包括：

- `risk_metric_catalog` 扩展字段
- 合同发布批次与风险指标发布批次关系
- 回滚状态与影响分析来源
- 覆盖率统计字段或统计口径

- [ ] **Step 2: 提供控制面最小后端 API**

API 至少包括：

- 合同发布批次列表/详情
- 风险指标目录列表/详情
- 影响分析查询
- 回滚试算/执行
- 覆盖率概览

- [ ] **Step 3: 用真实构建命令验证后端编译**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
Expected: BUILD SUCCESS

## Task 4: 工作流 C 实施任务包

**Files:**
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-message/...`
- Modify: `spring-boot-iot-device/...`
- Modify: `spring-boot-iot-alarm/...`
- Modify: `scripts/replay-mqtt-trace.py`
- Modify: `docs/07-部署运行与配置说明.md`

- [ ] **Step 1: 新增三类治理型运维告警**

告警最小集合：

- 字段漂移告警
- 厂商报文与正式合同差异告警
- 风险指标缺失告警

- [ ] **Step 2: 扩展统一回放入口**

回放维度至少包括：

- `traceId`
- `deviceCode`
- `productKey`
- `releaseBatchId`

- [ ] **Step 3: 补齐部署与运行文档**

Run: `rg -n "漂移|差异告警|releaseBatchId|回放" docs/07-部署运行与配置说明.md`
Expected: 能定位新增配置与运维说明。

## Task 5: 工作流 D 实施任务包

**Files:**
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadDecryptor.java`
- Modify: `spring-boot-iot-device/.../ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-alarm/.../RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-system/src/main/java/com/ghlzm/iot/system/service/impl/PermissionServiceImpl.java`
- Modify: `docs/01-系统概览与架构说明.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 抽取可替换规则边界**

本任务至少要从现有硬编码中抽出四类边界：

- 协议解密
- 字段映射
- 父子归一
- 风险指标生成

- [ ] **Step 2: 补齐控制面与桥层权限矩阵**

最小权限集：

- 规范库维护
- 合同 compare/apply/发布/回滚
- 风险指标发布/停用
- 策略与预案维护
- 控制面查看/导出

- [ ] **Step 3: 密钥治理先完成配置隔离与审计收口**

Run: `rg -n "iot:products|risk:rule-definition|system:audit|aes.merchants" spring-boot-iot-system spring-boot-iot-admin/src/main/resources/application-dev.yml docs/21-业务功能清单与验收标准.md`
Expected: 能看到新增权限码与配置治理说明。

## Task 6: 工作流 B 实施任务包

**Files:**
- Modify: `spring-boot-iot-ui/src/views/CockpitView.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/api/*.ts`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 用真实 API 替换驾驶舱静态治理经营指标**

最小指标集：

- 治理完成率
- 风险指标覆盖率
- 策略/预案覆盖率
- 厂商滞留与待处理积压

- [ ] **Step 2: 把关键工作台升级为任务导向**

至少补齐以下任务清单：

- 待治理产品
- 待发布合同
- 待绑定风险点
- 待补阈值策略
- 待补联动/预案
- 待复盘事项

- [ ] **Step 3: 前端自检共享模式与乱码风险**

Run: `node scripts/run-quality-gates.mjs`
Expected: 质量门禁通过，且新增前端文案不存在乱码。

## Task 7: 回收与统一合并到 codex/dev

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: 按固定顺序逐个回收工作流**

顺序：

1. `codex/semantic-bridge-control-plane`
2. `codex/operability-drift-replay`
3. `codex/extensibility-security-governance`
4. `codex/governance-task-workbench`

- [ ] **Step 2: 每次回收前执行最小真实环境验证**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: 最终统一更新顶层文档**

Run: `git diff --name-only codex/dev...HEAD`
Expected: 变更集包含对应实现文件与文档，且无平行替代文档。
