# Documentation Deep Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate the Markdown documentation system into clear authority docs, a current work summary, a process-evidence index, and archive-only historical UI planning docs.

**Architecture:** Keep the existing `docs/` topology and authority matrix intact. Add one process-evidence index under `docs/superpowers`, strengthen the current-stage summary inside existing authority docs, archive stale UI subproject planning docs, and verify all links through the existing topology checker.

**Tech Stack:** Markdown, Git, existing docs topology checker (`node scripts/docs/check-topology.mjs`), ripgrep.

---

## File Structure

- Create: `docs/superpowers/README.md`
  - Owns the index for `docs/superpowers/specs` and `docs/superpowers/plans`.
  - Groups process documents by topic and labels each theme with status and authority-doc landing points.
- Modify: `docs/16-阶段规划与迭代路线图.md`
  - Adds a compact full-project current work summary covering stable baseline, completed work, partial/open work, and next lines.
- Modify: `docs/README.md`
  - Adds `docs/superpowers/README.md` to the collaboration/process-evidence layer and points current work summary readers to `docs/16`.
- Modify: `README.md`
  - Keeps the root entry concise and adds the process-evidence index to related entries without making it a default reading dependency.
- Modify: `AGENTS.md`
  - Keeps assistant-facing rules aligned with the new process-evidence index and default reading path.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Records the documentation consolidation result and the archive status of UI planning docs.
- Modify: `docs/archive/README.md`
  - Adds archived UI planning docs to the archive index.
- Move: `spring-boot-iot-ui/docs/24-product-definition-evolution-plan.md` -> `docs/archive/ui-product-definition-evolution-plan-20260424.md`
- Move: `spring-boot-iot-ui/docs/25-product-detail-active-metrics.md` -> `docs/archive/ui-product-detail-active-metrics-20260424.md`
- Move: `spring-boot-iot-ui/docs/26-product-definition-implement-status.md` -> `docs/archive/ui-product-definition-implement-status-20260424.md`
- Move: `spring-boot-iot-ui/docs/27-pending-implementation-details.md` -> `docs/archive/ui-product-definition-pending-details-20260424.md`
- Modify: `spring-boot-iot-ui/README.md`
  - Removes stale product-roadmap language and fixes the archive debug-console link.

## Task 1: Create Process Evidence Index

**Files:**
- Create: `docs/superpowers/README.md`
- Modify: `docs/README.md`

- [ ] **Step 1: Write the process-evidence index**

Create `docs/superpowers/README.md` with this structure:

```markdown
# Superpowers 过程证据索引

> 文档定位：`docs/superpowers/specs` 与 `docs/superpowers/plans` 的过程证据索引。
> 适用角色：项目负责人、研发、测试、交付负责人、AI 协作助手。
> 权威级别：过程证据索引；不替代 `docs/` 根目录权威文档。
> 上游来源：`docs/superpowers/specs/*.md`、`docs/superpowers/plans/*.md`、`docs/08-变更记录与技术债清单.md`。
> 下游消费：任务追溯、方案复盘、下一轮拆解。
> 更新时间：2026-04-24

本目录保存设计与实施过程证据。日常编码默认不需要逐篇阅读本目录；需要追溯某一轮设计、执行计划或取舍时，先读本索引，再进入对应 `specs` 或 `plans` 文件。

## 1. 状态口径

| 状态 | 含义 |
|---|---|
| 已落地 | 对应能力已进入当前代码或权威文档基线 |
| 部分落地 | 主链路已完成，但仍有复验、扩面或长期治理项 |
| 计划中 | 已有设计或计划，尚未完成主实现 |
| 已被主文档吸收 | 结论已回写权威文档，原文件仅作过程追溯 |
| 历史参考 | 已退出当前执行路径，只保留背景价值 |

## 2. 主题索引

| 主题 | 当前状态 | 代表过程文档 | 对应权威文档 | 当前结论 |
|---|---|---|---|---|
| 产品定义与契约治理 | 部分落地 | `specs/2026-04-19-product-governance-reset-and-products-ia-design.md`、`plans/2026-04-20-product-workbench-polish.md` | `../02-业务功能与流程说明.md`、`../21-业务功能清单与验收标准.md` | `/products` 已拆为主列表和五段详情子路由，契约、映射与版本治理继续在产品定义中心内收口 |
| 对象洞察与 telemetry | 部分落地 | `specs/2026-04-23-object-insight-trend-legacy-identifier-graceful-cleanup-design.md`、`specs/2026-04-23-l3-l4-auto-canonicalization-design.md` | `../02-业务功能与流程说明.md`、`../04-数据库设计与初始化数据.md` | telemetry v2、历史窗口查询和运行态字段归一已进入基线，拓扑角色化洞察仍按后续计划推进 |
| 无代码接入与协议治理 | 部分落地 | `specs/2026-04-18-no-code-device-onboarding-p0-p1-design.md`、`specs/2026-04-18-protocol-governance-browser-expansion-design.md` | `../02-业务功能与流程说明.md`、`../03-接口规范与接口清单.md` | `/device-onboarding` 已形成 intake、模板包和批量编排最小闭环，不代表任意设备已全自动零代码接入 |
| 设备资产与能力操作 | 计划中 | `specs/2026-04-24-device-capability-design.md`、`specs/2026-04-24-device-asset-operation-simplification-design.md` | `../02-业务功能与流程说明.md`、`../21-业务功能清单与验收标准.md` | 设备详情与设备操作职责拆分，能力命令台账进入下一轮实施重点 |
| 风险闭环与治理控制面 | 部分落地 | `specs/2026-04-10-governance-control-plane-domain-execution-design.md`、`specs/2026-04-13-governance-task-view-selector-design.md` | `../19-第四阶段交付边界与复验进展.md`、`../08-变更记录与技术债清单.md` | 控制面承担调度、审计和 replay 收口，领域真相仍由产品、风险与系统域执行 |
| 质量工场与真实环境验收 | 部分落地 | `specs/2026-04-04-business-acceptance-workbench-design.md`、`plans/2026-04-05-role-automation-acceptance-and-dev-guide-implementation-plan.md` | `../05-自动化测试与质量保障.md`、`../真实环境测试与验收手册.md` | 业务验收台和结果与基线中心已可用，全角色完全自治与统一 CI 仍未完成 |
| 前端治理与页面结构 | 部分落地 | `specs/2026-03-31-global-list-workbench-template-rollout-design.md`、`specs/2026-04-20-product-detail-tab-refresh-design.md` | `../06-前端开发与CSS规范.md`、`../15-前端优化与治理计划.md` | 共享列表、抽屉、分页和接入智维页面结构持续收口，页面私有样式仍需长期治理 |
| 数据库 schema 与治理 registry | 已落地 | `specs/2026-04-14-database-schema-governance-design.md`、`specs/2026-04-15-schema-governance-framework-design.md` | `../04-数据库设计与初始化数据.md`、`../08-变更记录与技术债清单.md` | 结构真相源固定为 `schema/`，治理真相源固定为 `schema-governance/` |
| 智能助手协作与文档治理 | 部分落地 | `plans/2026-03-31-iot-business-governance-docs-consolidation-plan.md`、`specs/2026-04-24-documentation-deep-consolidation-design.md` | `../README.md`、`../08-变更记录与技术债清单.md` | 文档默认阅读路径和过程证据层继续分离，新增或迁移文档必须更新索引 |

## 3. 使用规则

1. 默认接手项目时先读 `../README.md` 与 `../README.md` 指向的权威主文档，不把本目录作为最小阅读集。
2. 查某项能力为什么这样设计时，按主题进入 `specs`。
3. 查某轮任务如何拆解和验证时，按主题进入 `plans`.
4. 若过程文档结论已经回写主文档，后续维护以主文档为准。
5. 新增 `specs` 或 `plans` 时，同步更新本索引的主题、状态或代表文档。
```

- [ ] **Step 2: Fix the duplicated README wording in the new index**

In the `使用规则` section, replace the first item with:

```markdown
1. 默认接手项目时先读 `../../README.md` 与 `../README.md` 指向的权威主文档，不把本目录作为最小阅读集。
```

- [ ] **Step 3: Add the process-evidence layer to `docs/README.md`**

Add `docs/superpowers/README.md` under the collaboration tooling layer and state that `specs/plans` are not default reading dependencies.

- [ ] **Step 4: Verify links and topology**

Run: `node scripts/docs/check-topology.mjs`

Expected: `Document topology check passed.`

- [ ] **Step 5: Commit Task 1**

```bash
git add docs/superpowers/README.md docs/README.md
git commit -m "docs: index superpowers process evidence"
```

## Task 2: Add Current Work Full Summary

**Files:**
- Modify: `docs/16-阶段规划与迭代路线图.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Insert a full summary section in `docs/16`**

Add a new section after `## 1. 当前阶段定位`:

```markdown
## 1.1 当前工作全盘总结（2026-04-24）

### 稳定基线

- Phase 1~3 设备接入主链路已长期稳定，HTTP / MQTT 上报统一进入固定 Pipeline。
- `spring-boot-iot-admin` 仍是唯一启动模块，父 `pom.xml` 当前活跃 `12` 个模块。
- 真实环境验收继续固定使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 或其环境变量覆盖。
- telemetry v2 已进入默认基线，dev / prod 默认写 TDengine v2 raw，并保留 MySQL latest 投影与 legacy fallback。
- 数据库结构真相源固定为 `schema/`，对象退场与 seed 治理真相源固定为 `schema-governance/`。

### 已完成并进入当前基线

- 产品定义中心已拆为 `/products` 主列表和 `/products/:productId/overview`、`devices`、`contracts`、`mapping-rules`、`releases` 五段详情子路由。
- 契约字段、映射规则、发布批次、风险指标目录、对象洞察重点指标和运行态显示规则已形成最小治理闭环。
- 风险监测、GIS 态势图、告警、事件、风险策略、报表分析、系统治理和系统内容治理已纳入 Phase 4 真实环境基线。
- 质量工场已形成业务验收台、研发工场、执行中心、结果与基线中心的可用链路。
- `/device-onboarding` 已具备统一接入案例、模板包、验收触发和批量编排的第一阶段能力。
- 治理控制面已具备治理任务台、治理运维台、治理审批台、权限与密钥治理页的最小闭环。

### 部分完成或仍需推进

- 零代码接入仍是阶段性底座能力，不代表任意设备已完成全自动接入。
- 对象洞察按设备拓扑角色分场景展示仍处于推进中，后续需继续收口父采集器、子设备、单台设备的快照和趋势差异。
- 设备资产中心的能力操作台账和命令执行链路已完成设计，仍需按产品能力类型继续实施和复验。
- 统一 CI / 契约测试流水线仍未成为正式基线，当前仍以本地质量门禁和共享真实环境验收为主。
- 风险指标桥层、协议治理和映射规则治理仍需扩大设备族覆盖，并继续沉淀运行时自动生成与恢复编排能力。

### 明确未完成或不纳入当前承诺

- GIS SDK、第三方地图底图、热力图和驾驶舱内嵌风险监测子模块不属于当前已交付范围。
- 任意设备厂商的无人值守零代码接入不属于当前已交付范围。
- 首页完整驾驶舱不等同于所有角色视角真实环境验收完成。
- 多机构成员模型、角色授权快照、统一数据权限全域推广仍属于后续演进。

### 下一轮建议主线

1. 设备资产能力操作：按产品类型展示可执行能力、参数模板、常用命令和命令台账。
2. 对象洞察拓扑角色化：优先完成读侧止血，再推进多 profile 配置治理和存量巡检。
3. 零代码接入扩面：沿协议治理、映射规则、模板包和正式合同发布继续推进，不新增平行真相源。
4. 质量门禁工程化：把当前本地脚本和真实环境验收逐步固化为可复用的团队级流水线。
5. 文档减负：入口文档保持摘要化，过程文档进入 `docs/superpowers/README.md` 索引，不再进入最小阅读集。
```

- [ ] **Step 2: Update `docs/08` documentation consolidation summary**

Add a dated bullet under `### 1.5 文档体系重塑`:

```markdown
- `2026-04-24`：文档深度整合进入执行口径。当前固定采用“权威入口 + 当前总结 + 过程证据索引 + 历史归档”四层模型：入口文档只保留摘要和导航，当前全盘总结收口到 `docs/16`，`docs/superpowers/specs` 与 `docs/superpowers/plans` 通过 `docs/superpowers/README.md` 作为过程证据层索引，旧 UI 子项目产品定义规划转入 archive 或历史参考，不再作为当前产品定义中心事实来源。
```

- [ ] **Step 3: Verify the edited docs do not introduce unmanaged pending markers**

Run: `rg -n 'DOC-Q-[0-9]+' docs/16-阶段规划与迭代路线图.md docs/08-变更记录与技术债清单.md`

Expected: either no output, or only existing governed entries in `docs/08-变更记录与技术债清单.md`.

- [ ] **Step 4: Commit Task 2**

```bash
git add docs/16-阶段规划与迭代路线图.md docs/08-变更记录与技术债清单.md
git commit -m "docs: summarize current project work"
```

## Task 3: Archive Stale UI Subproject Planning Docs

**Files:**
- Move: `spring-boot-iot-ui/docs/24-product-definition-evolution-plan.md`
- Move: `spring-boot-iot-ui/docs/25-product-detail-active-metrics.md`
- Move: `spring-boot-iot-ui/docs/26-product-definition-implement-status.md`
- Move: `spring-boot-iot-ui/docs/27-pending-implementation-details.md`
- Modify: `docs/archive/README.md`
- Modify: `spring-boot-iot-ui/README.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Move the stale UI planning docs into archive**

Run:

```bash
git mv spring-boot-iot-ui/docs/24-product-definition-evolution-plan.md docs/archive/ui-product-definition-evolution-plan-20260424.md
git mv spring-boot-iot-ui/docs/25-product-detail-active-metrics.md docs/archive/ui-product-detail-active-metrics-20260424.md
git mv spring-boot-iot-ui/docs/26-product-definition-implement-status.md docs/archive/ui-product-definition-implement-status-20260424.md
git mv spring-boot-iot-ui/docs/27-pending-implementation-details.md docs/archive/ui-product-definition-pending-details-20260424.md
```

- [ ] **Step 2: Add archive index entries**

Add these rows under `docs/archive/README.md` section `### 4. 过程记录 / 排查资料`:

```markdown
| `ui-product-definition-evolution-plan-20260424.md` | UI 子项目旧规划 | 需要追溯早期产品定义中心进化规划时 | `../02-业务功能与流程说明.md`、`../15-前端优化与治理计划.md` |
| `ui-product-detail-active-metrics-20260424.md` | UI 子项目旧需求 | 需要追溯早期产品详情活跃度统计设想时 | `../02-业务功能与流程说明.md`、`../21-业务功能清单与验收标准.md` |
| `ui-product-definition-implement-status-20260424.md` | UI 子项目旧状态 | 需要追溯早期产品定义中心实现状态记录时 | `../08-变更记录与技术债清单.md` |
| `ui-product-definition-pending-details-20260424.md` | UI 子项目旧规划 | 需要追溯早期待实现功能清单时 | `../16-阶段规划与迭代路线图.md` |
```

- [ ] **Step 3: Update `spring-boot-iot-ui/README.md`**

Replace the stale product-page list with a concise statement:

```markdown
## 当前定位

`spring-boot-iot-ui` 是主仓库的 Vue 3 前端工作区。当前页面结构、产品定义中心路由、接入智维页面约束和前端治理规则，以主仓库 `docs/02-业务功能与流程说明.md`、`docs/06-前端开发与CSS规范.md`、`docs/15-前端优化与治理计划.md` 为准。
```

Replace the stale reference:

```markdown
- `docs/13-frontend-debug-console.md`
```

with:

```markdown
- `docs/archive/13-frontend-debug-console.md`
```

- [ ] **Step 4: Add a front-end governance note**

Add to `docs/15-前端优化与治理计划.md` under historical materials:

```markdown
- `2026-04-24`：`spring-boot-iot-ui/docs/24~27` 早期产品定义中心规划已迁入 `docs/archive/`，当前产品定义中心事实以 `docs/02`、`docs/21` 和本文件为准；后续不得在 UI 子项目下重新维护平行产品路线图。
```

- [ ] **Step 5: Verify moved-file references**

Run:

```bash
rg -n '24-product-definition-evolution-plan|25-product-detail-active-metrics|26-product-definition-implement-status|27-pending-implementation-details|13-frontend-debug-console' README.md AGENTS.md docs spring-boot-iot-ui -g '*.md'
```

Expected: only archive references, the updated UI README archive link, or no output for the moved `24~27` names.

- [ ] **Step 6: Commit Task 3**

```bash
git add docs/archive spring-boot-iot-ui/README.md docs/15-前端优化与治理计划.md
git commit -m "docs: archive stale ui planning docs"
```

## Task 4: Align Root Entry Rules and Documentation Navigation

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/README.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Add process-evidence index to root related entries**

Add this related entry in `README.md`:

```markdown
- 过程设计与实施计划索引：`docs/superpowers/README.md`
```

Keep it out of the default minimal reading list.

- [ ] **Step 2: Align `AGENTS.md` assistant collaboration entry**

Add one sentence under `智能助手协作入口规则`:

```markdown
- 如果需要追溯历史设计或实施计划，先查阅 `docs/superpowers/README.md`；`docs/superpowers/specs` 与 `docs/superpowers/plans` 不进入默认编码前最小阅读集。
```

- [ ] **Step 3: Strengthen `docs/README.md` process evidence wording**

Ensure `docs/README.md` says that `docs/superpowers/README.md` is process evidence, not an authority replacement.

- [ ] **Step 4: Add final archive conclusion in `docs/08`**

Add one row to `## 5. 文档归档处理结论`:

```markdown
| `spring-boot-iot-ui/docs/24~27` | 已归档 | 早期产品定义中心规划与当前产品定义中心路由、契约治理和对象洞察事实不再一致，正文已迁入 `docs/archive/ui-product-*-20260424.md`，当前事实以 `docs/02 / docs/15 / docs/21` 为准 |
```

- [ ] **Step 5: Run topology check**

Run: `node scripts/docs/check-topology.mjs`

Expected: `Document topology check passed.`

- [ ] **Step 6: Commit Task 4**

```bash
git add README.md AGENTS.md docs/README.md docs/08-变更记录与技术债清单.md
git commit -m "docs: align documentation navigation"
```

## Task 5: Final Verification

**Files:**
- Check only.

- [ ] **Step 1: Check branch and working tree**

Run:

```bash
git branch --show-current
git status --short
```

Expected: branch is `codex/dev`; status is clean after final commit.

- [ ] **Step 2: Run topology check**

Run: `node scripts/docs/check-topology.mjs`

Expected: `Document topology check passed.`

- [ ] **Step 3: Search for stale UI doc references**

Run:

```bash
rg -n 'spring-boot-iot-ui/docs/(24|25|26|27)-|docs/13-frontend-debug-console.md|DOC-Q-[0-9]+' README.md AGENTS.md docs spring-boot-iot-ui -g '*.md'
```

Expected: no stale `spring-boot-iot-ui/docs/24~27` links, no `docs/13-frontend-debug-console.md` active link, and any `DOC-Q-*` output only appears in the existing governed question registry.

- [ ] **Step 4: Review final diff against design**

Run:

```bash
git log --oneline -n 6
git diff HEAD~4..HEAD --stat
```

Expected: commits cover process index, current summary, UI archive, navigation alignment, and no code/schema files changed.
