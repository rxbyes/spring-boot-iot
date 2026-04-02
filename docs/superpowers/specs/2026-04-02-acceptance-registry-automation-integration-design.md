# 验收矩阵与自动化工程统一编排设计

## 背景

当前仓库已经同时存在以下几类测试与验收资产：

1. 文档侧验收矩阵：
   - `docs/21-业务功能清单与验收标准.md`
   - `docs/真实环境测试与验收手册.md`
   - `docs/05-自动化测试与质量保障.md`
2. 浏览器自动化侧：
   - `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
   - `spring-boot-iot-ui/src/utils/automationPlan.ts`
   - `config/automation/sample-web-smoke-plan.json`
   - `scripts/auto/run-browser-acceptance.mjs`
3. 真实环境脚本侧：
   - `scripts/run-business-function-smoke.ps1`
   - `scripts/run-business-function-browser.ps1`
   - `scripts/run-message-flow-acceptance.py`
4. 真实环境风险闭环演练侧：
   - 已有可执行的人工与脚本化验证口径，但尚未纳入统一自动化编排入口。

当前问题不是“没有测试资产”，而是这些资产缺少统一事实源和统一执行入口，导致：

1. `docs/21` 的验收矩阵无法直接驱动自动化执行；
2. 自动化工场只擅长生成浏览器计划，无法统筹 API 冒烟、消息流验收与风险闭环演练；
3. 真实环境脚本之间彼此独立，报告与阻断级别不统一；
4. 后续要接入集成测试、共享环境回归或 CI 时，没有单一入口可以复用。

本设计的目标不是推翻现有脚本，而是在不破坏已验证基线的前提下，建立“验收矩阵 -> 自动化计划 -> 执行器 -> 汇总报告”的统一链路。

## 目标

1. 建立一份机器可读的验收场景注册表，作为自动化编排层的统一事实源。
2. 保留双入口：
   - 命令行为统一执行入口；
   - 自动化工场为可视化计划与场景管理入口。
3. 统一调度现有浏览器自动化、API 冒烟、消息流验收与风险闭环演练。
4. 统一输出汇总级运行报告和场景级证据路径，便于提测、回归、集成测试和真实环境留痕。
5. 第一批以“风险闭环主链路”作为样板接入统一编排，证明从设备上报到告警/事件/工单/风险监测的链路可以被重复演练。

## 范围与非目标

### 本阶段范围

1. 新增验收场景注册表文件。
2. 新增统一 CLI 编排入口。
3. 为以下执行类型补齐统一适配层：
   - `browserPlan`
   - `apiSmoke`
   - `messageFlow`
   - `riskDrill`
4. 将现有自动化工场升级为“注册表 + 浏览器计划 + 结果导入回看”工作台。
5. 为统一编排、注册表解析和风险闭环样板补齐最小测试。
6. 原位更新测试与验收相关文档。

### 明确不做

1. 不把所有测试场景强行改写成同一种浏览器 step 模型。
2. 不在第一期新增后端“日志浏览 / 测试结果查询”服务接口。
3. 不在第一期把 `docs/21` 完整自动生成为注册表文件。
4. 不把已有 PowerShell/Python 脚本全部推翻重写。
5. 不把自动化工场改造成直接在浏览器内执行真实环境演练的前端终端。
6. 不用 H2、旧验收 profile 或历史前端自动化链路替代真实环境基线。

## 设计选择

本轮采用单一方案：增加“验收注册表”编排层，并继续复用现有执行器。

原因：

1. 直接以文档驱动脚本只能缓解口径漂移，无法形成稳定的工程入口。
2. 直接以浏览器计划作为唯一事实源，会把 API 冒烟和风险闭环这类真实业务演练塞进错误的模型。
3. 在现有脚本之上增加注册表和编排层，可以最小改造现有资产，同时保留后续继续扩面的空间。

## 总体架构

统一方案分为四层：

1. 验收矩阵层：
   - 权威描述仍以 `docs/21-业务功能清单与验收标准.md` 为准；
   - 自动化工程新增机器可读注册表，作为执行目录。
2. 执行计划层：
   - 浏览器场景继续使用 `config/automation/*.json`；
   - API 冒烟、消息流、风险演练类场景使用注册表参数块表达；
   - 不强行统一为单一 step DSL。
3. 执行器层：
   - 复用现有浏览器执行器；
   - 复用现有 PowerShell/Python 脚本；
   - 新增风险闭环演练执行器；
   - 所有执行器输出统一结果结构。
4. 编排与报告层：
   - 新增统一 CLI；
   - 负责筛选、依赖解析、执行分派、汇总报告与退出码。

## 数据模型

### 注册表文件

- 路径：`config/automation/acceptance-registry.json`
- 角色：统一验收目录，不替代 `docs/21`，而是承接其“可执行能力点”。

### 顶层结构

```json
{
  "version": "1.0.0",
  "generatedAt": "2026-04-02T00:00:00+08:00",
  "defaultTarget": {
    "frontendBaseUrl": "http://127.0.0.1:5174",
    "backendBaseUrl": "http://127.0.0.1:9999",
    "profile": "dev"
  },
  "scenarios": []
}
```

### 场景字段

每个场景至少包含：

1. `id`
   - 唯一编码，例如 `risk.full-drill.red-chain`
2. `title`
   - 人类可读标题
3. `module`
   - 归属模块，例如 `device`、`alarm`、`system`
4. `docRef`
   - 对应 `docs/21` 或 `docs/真实环境测试与验收手册.md` 的能力点标识
5. `runnerType`
   - 固定为 `browserPlan`、`apiSmoke`、`messageFlow`、`riskDrill`
6. `scope`
   - `delivery`、`baseline`、`regression`
7. `blocking`
   - `blocker`、`warning`、`info`
8. `dependsOn`
   - 依赖场景数组
9. `inputs`
   - 执行参数模板，例如设备编码前缀、端口、用户名、风险点编码模板
10. `evidence`
    - 期望证据类型，例如 `json`、`md`、`screenshot`、`apiSummary`
11. `timeouts`
    - 超时与重试参数
12. `runner`
    - 执行器专属参数块

### runner 专属参数

#### `browserPlan`

```json
{
  "planRef": "config/automation/sample-web-smoke-plan.json",
  "scenarioScopes": ["delivery", "baseline"],
  "failScopes": ["delivery"]
}
```

#### `apiSmoke`

```json
{
  "entryScript": "scripts/run-business-function-smoke.ps1",
  "pointFilters": ["IOT-PRODUCT", "IOT-DEVICE", "ALARM", "EVENT"]
}
```

#### `messageFlow`

```json
{
  "entryScript": "scripts/run-message-flow-acceptance.py",
  "requiresExpiredTraceId": true
}
```

#### `riskDrill`

```json
{
  "drillTemplate": "fullRiskClosure",
  "deviceCodePrefix": "CDXACC",
  "riskPointCodePrefix": "cdx-risk",
  "levels": [
    { "name": "blue", "value": 0.0014 },
    { "name": "yellow", "value": 6.2 },
    { "name": "orange", "value": 12.8 },
    { "name": "red", "value": 21.6 }
  ]
}
```

## 场景分类与边界

### `browserPlan`

适用范围：

1. 登录与会话初始化；
2. 页面可达与首屏接口校验；
3. 列表查询、表单提交、详情抽屉；
4. 视觉回归与页面结构稳定性检查。

规则：

1. 继续由自动化工场生成和维护 `config/automation/*.json`。
2. 命令行通过统一编排层转调 `scripts/auto/run-browser-acceptance.mjs`。
3. 不承担真实业务闭环的核心判定。

### `apiSmoke`

适用范围：

1. 受保护接口与鉴权冒烟；
2. 基础 CRUD；
3. 系统治理页对应的最小后端能力探活；
4. 已存在的 `run-business-function-smoke.ps1` 能覆盖的业务点。

规则：

1. 第一阶段保留现有 PowerShell 脚本。
2. 该脚本需要补充“按点位 / 按模块过滤”的参数，不再默认只能全量跑。
3. 统一由编排层收集其产物路径和失败摘要。

### `messageFlow`

适用范围：

1. 设备上报主链路；
2. TraceId 回查；
3. 过期链路验证；
4. 协议与消息时序取证。

规则：

1. 继续复用 Python 执行器；
2. 编排层只负责输入参数装配和产物归档；
3. 不改现有协议口径。

### `riskDrill`

适用范围：

1. 设备上报；
2. 风险对象绑定；
3. 阈值策略判级；
4. 联动规则命中；
5. 应急预案命中；
6. 告警、事件、工单生成；
7. 风险监测与实时结果回看。

规则：

1. 单独实现 Node 风险闭环演练执行器，不复用浏览器 step 模型。
2. 真实环境下生成唯一演练标识，避免污染共享数据。
3. 优先复用当前已验证的 `10099` 风险链路口径。
4. 结果必须输出结构化 JSON 和 Markdown 证据，作为后续集成测试样板。

## 统一 CLI

### 命令入口

- 路径：`scripts/auto/run-acceptance-registry.mjs`

### 核心职责

1. 读取注册表；
2. 解析运行目标；
3. 处理依赖关系；
4. 生成统一 `runId`；
5. 分派到对应执行器；
6. 汇总结果、证据路径和失败摘要；
7. 输出统一报告和退出码。

### 推荐命令

```bash
node scripts/auto/run-acceptance-registry.mjs --list
node scripts/auto/run-acceptance-registry.mjs --scope=delivery
node scripts/auto/run-acceptance-registry.mjs --module=alarm --include-deps
node scripts/auto/run-acceptance-registry.mjs --id=risk.full-drill.red-chain
```

### 参数约定

1. `--list`
   - 只列出可执行场景
2. `--id=<scenarioId>`
   - 只执行单场景
3. `--module=<module>`
   - 按模块筛选
4. `--scope=<scope>`
   - 按 `delivery / baseline / regression` 筛选
5. `--include-deps`
   - 执行依赖链
6. `--frontend-base-url=...`
   - 覆盖前端地址
7. `--backend-base-url=...`
   - 覆盖后端地址
8. `--output-prefix=...`
   - 覆盖输出前缀

### 退出码规则

1. 任一 `blocking=blocker` 场景失败，CLI 返回非零退出码。
2. 只有 `warning / info` 场景失败时，CLI 返回 `0`，但汇总报告必须高亮。
3. 参数错误、注册表冲突、依赖环路属于编排失败，直接返回非零退出码。

## 执行器适配层

### 浏览器执行器适配

统一编排层通过子进程调用：

```bash
node scripts/auto/run-browser-acceptance.mjs --plan=<planRef>
```

编排层需要解析其：

1. 退出码；
2. 汇总 JSON；
3. Markdown 报告；
4. 截图与视觉回归产物；
5. 场景级失败摘要。

### API 冒烟执行器适配

`scripts/run-business-function-smoke.ps1` 需要新增：

1. `-PointFilter` 或 `-ModuleFilter` 参数；
2. 返回统一的摘要结构；
3. 继续输出 `REPORT_JSON`、`REPORT_SUMMARY`、`REPORT_MD`；
4. 对于编排层调用，允许只运行某一部分功能点。

### 消息流执行器适配

统一编排层以子进程调用 Python 脚本，传入：

1. `--expired-trace-id`
2. 环境地址
3. 输出前缀

编排层只负责把其结果收口到统一报告。

### 风险闭环执行器

新增 `scripts/auto/run-risk-closure-drill.mjs`，负责：

1. 创建或复用演练设备；
2. 创建或复用风险点；
3. 确保绑定、阈值策略、联动规则、应急预案已准备好；
4. 按蓝、黄、橙、红四个等级发送真实数据；
5. 查询风险监测、告警、事件、工单接口；
6. 生成：
   - `logs/acceptance/risk-drill-<runId>.json`
   - `logs/acceptance/risk-drill-<runId>.md`

### 执行器统一结果结构

每个执行器最终都要向编排层返回：

```json
{
  "scenarioId": "risk.full-drill.red-chain",
  "runnerType": "riskDrill",
  "status": "passed",
  "blocking": "blocker",
  "startedAt": "2026-04-02T12:00:00+08:00",
  "finishedAt": "2026-04-02T12:03:00+08:00",
  "durationMs": 180000,
  "summary": "风险闭环红色链路通过",
  "evidenceFiles": [
    "logs/acceptance/risk-drill-20260402120000.json",
    "logs/acceptance/risk-drill-20260402120000.md"
  ],
  "details": {}
}
```

## 风险闭环样板场景

第一批样板选择“风险闭环主链路”，原因如下：

1. 这是当前第四阶段最需要收口的真实业务链路；
2. 它天然跨越设备接入、风险治理、告警运营、事件处置和风险监测；
3. 它最能证明“系统不是各功能孤岛，而是一个联动平台”；
4. 当前共享环境已经具备一次真实演练证据，可以作为固化基线。

### 样板步骤

1. 准备唯一设备编码、风险点编码和运行 token；
2. 检查风险点绑定关系；
3. 检查阈值策略、联动规则、应急预案是否可命中；
4. 发送蓝色样本，断言风险点为 `blue/NORMAL`，且不生成事件；
5. 发送黄色样本，断言至少生成告警；
6. 发送橙色样本，断言累计出现事件和工单；
7. 发送红色样本，断言最新风险监测结果为 `red/ALARM`；
8. 查询告警、事件、工单与风险监测接口，汇总最终证据。

### 最小断言口径

1. `riskLevel`
2. `monitorStatus`
3. `alarmCount`
4. `eventCount`
5. `workOrderCount`
6. 告警/事件留痕中包含命中的联动规则或应急预案文本

## 自动化工场升级

### 保留定位

自动化工场仍然是“编排与查看工具”，不是新的执行服务。

### 第一阶段新增 3 个视图

1. 注册表视图
   - 展示场景目录、执行器类型、阻断级别、依赖关系、文档映射关系
2. 计划关联视图
   - `browserPlan` 类型继续可编辑和导出 JSON
   - 非浏览器场景展示参数模板、证据要求和最近导入结果摘要
3. 结果回看视图
   - 支持导入统一 CLI 生成的汇总 JSON
   - 支持按场景查看状态、摘要、证据文件路径和失败分级

### 明确约束

1. 第一阶段不新增后端“读取本地 logs 文件”接口。
2. 结果回看采用“导入统一汇总 JSON”模式。
3. 自动化工场与命令行之间通过同一注册表和同一结果结构对齐，而不是共享运行时。

## 报告与产物

### 场景级产物

各执行器继续保留自己的原始产物：

1. 浏览器自动化：
   - summary JSON
   - results JSON
   - report Markdown
   - screenshot / visual diff
2. API 冒烟：
   - smoke JSON
   - summary JSON
   - report Markdown
3. 消息流：
   - message-flow JSON
4. 风险闭环：
   - risk-drill JSON
   - risk-drill Markdown

### 汇总级产物

统一 CLI 新增：

1. `logs/acceptance/registry-run-<timestamp>.json`
2. `logs/acceptance/registry-run-<timestamp>.md`

汇总报告至少包含：

1. `runId`
2. 环境地址
3. 执行参数摘要
4. 场景执行顺序
5. 每个场景的状态、阻断级别、耗时、失败摘要
6. 证据文件路径
7. 最终退出码判定

## 测试与验证

### Node 层最小测试

1. 注册表解析测试
   - 校验字段完整性
   - 校验重复 `id`
   - 校验无效 `runnerType`
2. 依赖解析测试
   - 校验排序正确
   - 校验环依赖报错
3. 执行器分派测试
   - 校验不同 `runnerType` 调用不同适配器
4. 汇总报告测试
   - 校验阻断级别与退出码规则

### 浏览器计划相关测试

1. 继续保留现有 `run-browser-acceptance.test.mjs`
2. 补充注册表指向浏览器计划的最小一致性测试

### 风险闭环样板测试

1. 在允许访问真实 `dev` 环境时，执行最小风险闭环演练
2. 断言：
   - 蓝色不生成事件
   - 黄橙红链路状态按预期推进
   - 最终 `riskLevel / monitorStatus` 与预期一致

### 真实环境规则

1. 所有最终验收继续以 `application-dev.yml` 对应环境为基线
2. 如环境不可达，明确记录为环境阻塞
3. 不得回退到 H2、旧前端自动化链路或伪造数据宣布通过

## 实施阶段

### Phase 1：统一骨架

1. 新增注册表文件
2. 新增统一 CLI
3. 接入 `browserPlan` 与 `apiSmoke`
4. 统一汇总报告

### Phase 2：风险闭环样板

1. 新增 `riskDrill` 执行器
2. 固化第一条风险闭环演练场景
3. 补齐最小测试和真实环境验证

### Phase 3：自动化工场升级

1. 展示注册表
2. 支持导入汇总结果
3. 展示最近一次导入结果摘要

### Phase 4：扩面

1. 逐步将 `docs/21` 更多能力点接入注册表
2. 按模块增加浏览器计划、消息流场景和风险演练模板

## 文档同步要求

实施时必须同步更新：

1. `docs/05-自动化测试与质量保障.md`
2. `docs/真实环境测试与验收手册.md`
3. `docs/21-业务功能清单与验收标准.md`
4. `docs/08-变更记录与技术债清单.md`
5. 必要时更新 `README.md`
6. 必要时更新 `AGENTS.md`

## 完成定义

本设计进入实现阶段的最低完成定义为：

1. 存在机器可读验收注册表；
2. 存在统一 CLI，可筛选、执行并汇总报告；
3. 浏览器计划与 API 冒烟已能通过统一 CLI 编排；
4. 风险闭环样板场景可在真实 `dev` 环境重复执行；
5. 自动化工场能展示注册表并导入统一汇总结果；
6. 相关测试与文档完成同步。
