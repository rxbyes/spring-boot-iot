# 质量工场全流程业务自动化测试计划设计

> 日期：2026-04-25
> 范围：质量工场、业务验收台、研发工场、执行中心、结果与基线中心、统一验收注册表、真实环境验收文档
> 主题：基于当前产品说明书与质量工场现状，建立“立即可执行 + 平台化演进”的全流程业务自动化测试计划
> 状态：设计已确认，待用户审阅后进入实施计划

## 1. 背景

当前系统已经形成可用的质量工场底座：

1. `/quality-workbench` 作为质量工场总览。
2. `/business-acceptance` 面向验收人员、产品和项目经理，只暴露 `环境 / 账号模板 / 模块范围` 三类轻配置。
3. `/rd-workbench` 与 `/rd-automation-inventory`、`/rd-automation-templates`、`/rd-automation-plans`、`/rd-automation-handoff` 承接研发视角的自动化资产维护。
4. `/automation-execution` 承接执行中心，负责目标环境、统一注册表、阻断范围和命令预览。
5. `/automation-results` 承接结果与基线中心，按 `runId` 查询历史运行、失败场景和证据。
6. `config/automation/acceptance-registry.json` 已作为统一自动化场景事实源。
7. `config/automation/business-acceptance-packages.json` 已作为业务验收包事实源。
8. `scripts/auto/run-acceptance-registry.mjs` 已统一调度 `browserPlan / apiSmoke / messageFlow / riskDrill`。
9. 后端 `BusinessAcceptanceServiceImpl` 会按业务验收包派生临时 registry，并异步调用统一注册表 CLI。
10. 后端 `AutomationResultQueryServiceImpl` 会只读扫描 `logs/acceptance/registry-run-*.json`，并限制证据预览范围。

现有能力已满足“预置包一键执行、模块级结论、runId 证据追溯”的第一层可用性，但仍存在明显平台化缺口：

1. 业务验收包仍较少，未完整覆盖当前产品说明书中的所有关键业务能力。
2. 部分模块共享同一个底层场景，失败归因粒度偏粗。
3. 结果中心主要依赖 `logs/acceptance` 文件扫描，尚不是集中式 CI 证据中心。
4. 跨环境汇聚、趋势分析、flaky 治理、覆盖率热力图和自动归因仍未形成正式能力。
5. 托管式 CI 或定时调度未固化，自动化执行仍以本地或共享环境手工触发为主。

因此，本计划采用“双轨三阶段”路线：先把全流程业务验收跑起来，再把质量工场逐步做成全能自动化测试平台。

## 2. 目标

### 2.1 立即可执行目标

1. 按当前产品说明书和 `docs/21-业务功能清单与验收标准.md` 建立全流程业务自动化测试矩阵。
2. 将业务能力按 `P0 / P1 / P2` 分级，避免一次性全量阻断导致共享环境不可用。
3. 第一阶段优先补齐 P0 阻断链路，确保每次交付前能回答“系统主业务链路是否可交付”。
4. 每次执行都必须生成 `registry-run-<runId>.json`、`registry-run-<runId>.md` 和专项证据。
5. 业务验收台首屏必须回答：
   - 是否通过
   - 哪些模块未通过
   - 总耗时或运行状态
6. 失败模块必须能下钻到：
   - 失败场景
   - 失败步骤
   - 关联接口
   - 页面动作
   - 底层证据路径

### 2.2 平台化演进目标

1. 把质量工场从“可用验收入口”升级为“自动化测试平台”。
2. 建立覆盖率治理、失败归因、证据归档、视觉基线、跨环境汇聚和趋势分析能力。
3. 支持研发、测试、业务、产品、项目经理按不同入口消费同一套自动化事实源。
4. 后续可接入托管 CI 或定时任务，但不改变当前真实环境基线。

## 3. 非目标

1. 不重建第二套自动化执行引擎。
2. 不用 H2、旧验收 profile、旧浏览器自动化路径替代真实 `dev` 环境。
3. 不要求业务人员自行维护 registry、浏览器计划或低层断言。
4. 不在第一阶段引入数据库级自动化结果归档表。
5. 不把所有 P1/P2 场景纳入第一批阻断门禁。
6. 不承诺任意设备厂商已经实现零代码全自动接入。

## 4. 总体方案

采用推荐方案：**双轨三阶段**。

双轨：

1. **业务验收轨**：面向业务、产品、项目经理，重点是预置包、一键执行、模块结论和失败下钻。
2. **研发治理轨**：面向研发和测试，重点是 registry、执行器、场景模板、证据、覆盖率、CI 和失败归因。

三阶段：

1. **第一阶段：P0 可执行闭环**
   - 扩充 P0 业务验收包。
   - 保证主业务链路可一键执行。
   - 保证结果中心能按 `runId` 追溯证据。
2. **第二阶段：P1 覆盖扩面**
   - 扩充第四阶段治理能力和高级业务链路。
   - 拆细模块级断言，降低失败归因粗粒度。
3. **第三阶段：全能测试平台**
   - 建设 CI、跨环境归档、趋势分析、flaky 治理、覆盖率热力图和智能归因。

## 5. 优先级矩阵

### 5.1 P0 阻断链路

P0 是每次交付前必须执行的阻断链路。任一 blocker 场景失败，都不能宣称全流程业务自动化验收通过。

| 业务域 | 模块 | 自动化重点 | 推荐执行器 | 当前基线 |
|---|---|---|---|---|
| 登录与权限 | 登录、菜单、用户上下文 | 登录成功、菜单可达、受保护接口可访问 | browserPlan / apiSmoke | `auth.browser-smoke` |
| 产品与设备 | 产品新增、查询、设备新增、查询 | 产品设备基础 CRUD 与列表可达 | browserPlan / apiSmoke | `product-device` 包已覆盖粗粒度 |
| 上报链路 | HTTP 上报、MQTT 回溯、消息日志、latest 属性、在线状态 | 固定 Pipeline 与 trace 证据 | messageFlow / browserPlan | `message.expired-trace` |
| 遥测 | latest、history、补零窗口 | latest 查询、history 窗口、长历史补零 | apiSmoke / messageFlow | 需扩充 registry |
| 风险运营 | 告警、事件、工单、闭环复盘 | 红链路触发、告警事件生成、闭环处理 | riskDrill / apiSmoke | `risk.full-drill.red-chain` |
| 风险策略 | 风险点绑定、阈值、联动、预案 | 风险闭环依赖配置可用 | riskDrill / apiSmoke | 已有风险演练底座 |
| 平台治理 | 组织、用户、角色、治理任务、治理运维、治理审批 | 控制面页面和核心接口可达 | browserPlan / apiSmoke | `system.api-smoke`、`governance.control-plane.browser-smoke` |
| 质量工场 | 业务验收台、结果页、结果中心 | 启动任务、轮询状态、结果页、runId 跳转 | browserPlan / apiSmoke | 前后端定向测试已覆盖 |

P0 验收包建议扩为：

1. `platform-p0-full-flow`：登录、产品设备、上报链路、风险运营、平台治理、质量工场自验。
2. `iot-access-p0`：产品、设备、HTTP/MQTT、链路追踪、异常观测。
3. `risk-p0`：风险点、阈值、告警、事件、联动、预案。
4. `governance-p0`：组织用户角色、治理任务、治理运维、治理审批。

### 5.2 P1 扩展链路

P1 是每周、版本封版前或相关模块有改动时执行的扩展链路。

| 业务域 | 模块 | 自动化重点 | 推荐执行器 |
|---|---|---|---|
| 产品合同治理 | compare、apply、发布批次、回滚试算、审批回执 | 字段治理与审批链 | apiSmoke / browserPlan |
| 映射规则治理 | 草稿、台账、试命中、发布/回滚审批 | rawIdentifier 到正式字段治理 | apiSmoke / browserPlan |
| 协议治理 | 协议族、解密档案、协议模板、模板回放 | 协议运行态真相 | browserPlan / apiSmoke |
| 无代码接入 | 案例、模板包、状态派生、验收触发 | 接入卡点与结果回链 | apiSmoke / browserPlan |
| 对象洞察 | 属性快照、趋势单位、history 补零、子设备总览 | 读侧治理一致性 | apiSmoke / browserPlan |
| 风险指标目录 | 发布目录、推荐绑定、目录候选 | 风险语义与绑定候选 | apiSmoke |
| 设备操作 | 操作抽屉、能力参数、常用命令、能力反馈 | 设备命令链路 | apiSmoke / browserPlan |
| 结果中心 | 证据清单、原文预览、历史筛选、手工导入 | 复盘与证据治理 | apiSmoke / browserPlan |

P1 验收包建议新增：

1. `product-governance-p1`
2. `protocol-governance-p1`
3. `device-onboarding-p1`
4. `object-insight-p1`
5. `automation-results-p1`

### 5.3 P2 平台成熟度链路

P2 不作为日常阻断，但作为质量工场长期演进能力。

| 能力 | 目标 | 结果形态 |
|---|---|---|
| 多租户与数据权限矩阵 | 验证 tenant、org、dataScopeType 边界 | 权限矩阵验收包 |
| 视觉回归 | 固化关键页面视觉基线 | screenshot baseline / diff |
| 跨环境结果汇聚 | dev/test/prod-like 多环境对比 | 环境维度趋势 |
| flaky 治理 | 识别偶发失败与稳定失败 | flaky 标签与重跑策略 |
| 覆盖率热力图 | 显示业务域、页面、接口、执行器覆盖 | coverage dashboard |
| 失败自动归因 | 将失败归为环境、数据、权限、接口、UI、断言 | failure taxonomy |
| CI 归档 | 自动执行、保留产物、通知结果 | CI run + runId |
| 验收包草案生成 | 从文档和历史运行推荐新包 | 人工确认后的包配置 |

## 6. 执行架构

### 6.1 事实源

1. `docs/21-业务功能清单与验收标准.md`
   - 交付能力矩阵和验收判定标准。
2. `docs/05-自动化测试与质量保障.md`
   - 测试策略、质量门禁、自动化资产说明。
3. `docs/真实环境测试与验收手册.md`
   - 真实环境执行步骤、留痕和阻塞记录。
4. `config/automation/acceptance-registry.json`
   - 自动化场景执行事实源。
5. `config/automation/business-acceptance-packages.json`
   - 业务验收包事实源。

### 6.2 执行入口

| 入口 | 面向角色 | 用途 |
|---|---|---|
| `/business-acceptance` | 验收、产品、项目经理 | 轻配置、一键执行、看结论 |
| `/automation-execution` | 研发、测试 | 核对 registry、命令、阻断范围 |
| `/automation-results` | 全角色按权限 | 查看历史运行、失败、证据 |
| CLI `run-acceptance-registry.mjs` | 研发、CI | 统一执行自动化场景 |
| `run-quality-gates.mjs` | 研发 | 本地质量门禁，不替代真实验收 |

### 6.3 执行器边界

| 执行器 | 适用场景 | 不适用场景 |
|---|---|---|
| `browserPlan` | 登录、页面可达、表单、列表、抽屉、视觉证据 | 深业务状态最终判定 |
| `apiSmoke` | CRUD、治理接口、读侧接口、权限接口 | 复杂 UI 行为 |
| `messageFlow` | HTTP/MQTT 上报、trace、消息处理 Pipeline | 页面视觉验证 |
| `riskDrill` | 风险闭环、阈值、告警、事件、联动、预案 | 普通 CRUD |

### 6.4 结果与证据

每次执行必须形成以下最小证据：

1. `logs/acceptance/registry-run-<runId>.json`
2. `logs/acceptance/registry-run-<runId>.md`
3. 执行器专项证据：
   - browser summary / results / report / screenshots
   - message-flow JSON
   - risk-drill JSON / Markdown
   - apiSmoke summary
4. 业务验收台结果：
   - `status`
   - `passedModuleCount`
   - `failedModuleCount`
   - `failedModuleNames`
   - `failureDetails`
5. 结果中心证据：
   - run detail
   - evidence list
   - evidence content preview

## 7. 阶段实施计划

### 7.1 第一阶段：P0 可执行闭环

目标：交付前可以一键执行 P0 全流程，并按 `runId` 追溯证据。

工作项：

1. 梳理 `docs/21` 中 P0 业务功能。
2. 扩充 `business-acceptance-packages.json`，新增 P0 全流程包。
3. 扩充 `acceptance-registry.json`，让每个 P0 模块至少绑定一个可执行场景。
4. 对已有粗场景补充 `details.stepLabel / apiRef / pageAction`，减少 fallback 文案。
5. 增加质量工场自验场景：
   - 加载业务验收包
   - 发起运行
   - 轮询任务
   - 打开结果页
   - 跳转 `/automation-results?runId=...`
6. 在 `docs/05` 和真实环境手册中补齐 P0 执行命令和判断口径。

验收标准：

1. P0 包可以通过业务验收台启动。
2. 成功或失败都必须生成 `runId`。
3. 结果页能展示是否通过和失败模块。
4. 失败模块能展示失败步骤、接口和页面动作。
5. 结果中心能按同一 `runId` 预选运行并展示证据。
6. 环境失败必须标为 `blocked`，不得写成业务失败。

### 7.2 第二阶段：P1 覆盖扩面

目标：把第四阶段治理能力和高级业务能力纳入自动化验收。

工作项：

1. 新增产品合同治理 P1 包。
2. 新增协议治理 P1 包。
3. 新增无代码接入 P1 包。
4. 新增对象洞察 P1 包。
5. 新增自动化结果中心 P1 包。
6. 将共用粗场景拆成更细场景：
   - 产品新增与产品查询拆分
   - 设备新增与设备查询拆分
   - 告警闭环与事件处置拆分
   - 治理任务与治理运维拆分
7. 增强 registry 场景元数据：
   - `ownerDomain`
   - `priority`
   - `failureCategory`
   - `dataSetup`
   - `cleanupPolicy`

验收标准：

1. P1 包可通过 CLI 和业务验收台执行。
2. 失败模块不再主要依赖 fallback 文案。
3. 每个 P1 场景都有明确数据准备和清理策略。
4. 结果中心能区分 P0/P1 执行范围。

### 7.3 第三阶段：全能测试平台

目标：把质量工场升级为长期可运营的自动化测试平台。

工作项：

1. 引入 CI 或定时调度入口。
2. 建立集中式结果归档模型，保留文件证据但增加可查询索引。
3. 增加失败分类：
   - 环境阻塞
   - 数据未准备
   - 权限不足
   - 接口失败
   - UI 选择器失败
   - 业务断言失败
   - 视觉差异
4. 增加覆盖率视图：
   - 业务域覆盖
   - 页面覆盖
   - 接口覆盖
   - 执行器覆盖
   - P0/P1/P2 覆盖
5. 增加趋势分析：
   - 最近运行通过率
   - 高频失败模块
   - flaky 场景
   - 平均耗时
6. 增加通知分发：
   - 提测结果
   - 阻断失败
   - 环境阻塞
   - 长期未覆盖模块
7. 增加验收包治理：
   - 变更审阅
   - 版本台账
   - 启停用
   - 适用角色
   - 适用环境

验收标准：

1. CI 或定时任务能生成标准 `runId`。
2. 结果中心能按环境、包、状态、执行器、时间检索。
3. 平台能展示 P0/P1/P2 覆盖缺口。
4. 失败有明确分类和下一步建议。
5. 业务角色仍只消费轻量结果，不暴露低层编排细节。

## 8. 数据准备与清理

1. 真实环境验收数据统一使用 `accept-`、`ACCEPT_`、`cdx-accept-` 等前缀。
2. 默认使用 `tenant_id=1 / tenant_code=default`。
3. 涉及租户或组织边界时，必须单独标记数据权限场景。
4. 风险演练生成的风险点、规则、联动、预案应优先自动清理。
5. 消息日志、告警、事件等链路留痕默认允许保留，但必须能按前缀检索。
6. 任何需要 schema 或 seed 对齐的场景，必须在执行记录中写明前置对齐状态。

## 9. 环境阻塞判定

以下情况应判定为 `blocked`，不得写成业务失败：

1. 后端健康检查不可达。
2. 前端代理目标错误。
3. 登录接口不可达或演示账号缺失。
4. MQTT leader 不匹配导致 MQTT 消费不可用。
5. TDengine / MySQL / Redis 基础依赖不可达。
6. schema 未按 registry 对齐。
7. 共享环境端口冲突导致服务未启动。
8. 运行实例未重启，仍执行旧代码。

业务失败与环境阻塞必须在结果页、Markdown 报告和交付说明中分开。

## 10. 文档同步策略

后续实施时至少需要同步更新：

1. `docs/05-自动化测试与质量保障.md`
   - 自动化策略、执行命令、P0/P1/P2 准入说明。
2. `docs/真实环境测试与验收手册.md`
   - 真实环境启动、执行、证据留痕和阻塞记录。
3. `docs/21-业务功能清单与验收标准.md`
   - 业务能力对应验收包和判定标准。
4. `docs/03-接口规范与接口清单.md`
   - 若新增或调整业务验收接口。
5. `README.md`
   - 若质量工场入口、执行方式或交付口径变化。
6. `AGENTS.md`
   - 若真实环境规则、质量工场定位或协作规则变化。

不得新增平行替代文档。

## 11. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 一次性全量执行时间过长 | 共享环境压力大、反馈慢 | P0/P1/P2 分层，P0 阻断，P1 定期，P2 平台治理 |
| 模块共用粗场景 | 失败归因不准 | 第二阶段拆细场景并补充 details |
| 结果文件过多 | 查询慢、证据难找 | 第三阶段建立集中式结果索引 |
| 环境失败被误判为业务失败 | 交付结论失真 | 明确 blocked 分类和关键词判定 |
| 自动化数据污染共享环境 | 影响后续验收 | 统一前缀、清理策略和保留说明 |
| 业务验收包维护成本上升 | 业务入口变脆 | 研发治理轨负责 registry 和包版本治理 |
| 视觉基线误覆盖 | 掩盖真实 UI 回归 | 视觉刷新必须人工确认，不作为默认执行动作 |

## 12. 成功标准

第一阶段成功标准：

1. P0 全流程包可在 `/business-acceptance` 发起。
2. P0 全流程包可通过 CLI 执行。
3. 每次执行都有 `runId` 与标准证据。
4. 结果页能回答是否通过、哪些模块没过。
5. `/automation-results?runId=...` 能预选同一次运行。
6. 文档同步记录 P0 执行方式和判定口径。

第二阶段成功标准：

1. P1 治理能力包完成并可执行。
2. 主要模块不再依赖同一个粗场景给出结论。
3. 失败详情覆盖步骤、接口、页面动作和底层摘要。
4. 数据准备和清理策略明确。

第三阶段成功标准：

1. CI 或定时调度可生成标准运行结果。
2. 结果中心支持跨环境、跨包、跨时间检索。
3. 平台展示覆盖缺口、趋势、flaky 和失败分类。
4. 质量工场成为研发、测试、业务、产品、项目经理共同使用的自动化测试平台。

## 13. 下一步

用户审阅并确认本设计后，进入实施计划阶段。实施计划应把第一阶段拆成可执行任务，优先覆盖：

1. P0 验收矩阵落表。
2. `business-acceptance-packages.json` 扩充。
3. `acceptance-registry.json` 扩充。
4. 质量工场自验场景。
5. 前后端定向测试。
6. 真实环境 P0 执行命令与文档同步。

