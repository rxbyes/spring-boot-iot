# 质量工场失败归因增强设计

- 日期：`2026-04-26`
- 范围：`scripts/auto`、`spring-boot-iot-report`、`spring-boot-iot-ui`、`docs/*`
- 状态：`proposed`

## 1. 背景

质量工场当前已经形成三层基础能力：

1. `/quality-workbench` 只保留 `业务验收台 + 自动化治理台` 两条主路径。
2. `generate-acceptance-coverage.mjs`、`diff-acceptance-coverage.mjs`、`run-acceptance-readiness.mjs` 已补齐覆盖治理、趋势对比和 readiness 证据。
3. `generate-automation-result-archive-index.mjs` 已把 `logs/acceptance/registry-run-*.json` 收口成稳定的 run 级归档索引，并驱动治理台结果证据工作区按 `关键字 / 聚合状态 / 执行器 / 验收包 / 环境 / 日期范围` 检索。

但当前结果表达仍有明显缺口：

1. 业务验收结果页只能回答“哪个模块失败”，还不能回答“更像什么问题”。
2. 自动化治理台虽然能看到失败场景和证据，但仍主要依赖人工阅读原始 evidence，自行判断属于环境、接口、UI 还是断言问题。
3. 结果归档索引当前只沉淀 run 摘要、package、environment 和 evidence 元数据，没有失败判断切片。
4. 后续若要做 flaky 治理、重跑建议、失败趋势或 CI 分类告警，仍缺少统一的失败 taxonomy 底座。

因此，本轮应在不改变真实执行入口和门禁逻辑的前提下，把质量工场从“能查到失败”升级为“更会判断失败”。

## 2. 目标

本轮目标是为质量工场补齐首版只读失败归因能力：

1. 在结果归档索引生成阶段，产出模块级和场景级失败归因结果。
2. 业务验收结果页补充失败模块的 `分类 / 简短原因 / 命中证据摘要`。
3. 自动化治理台结果证据工作区补充运行级失败分类分布，以及失败场景的 `分类 / 简短原因 / 命中证据摘要`。
4. 归因只作为结果解释层，不改变现有 `passed / failed / blocked` 判定，也不参与 readiness gate。
5. 保持 `logs/acceptance`、`registry-run-*.json` 和原始 evidence 文件为唯一真相源。

## 3. 非目标

本轮明确不做以下事项：

1. 不改变统一验收注册表、业务验收台或 readiness 的通过/失败门禁。
2. 不新增数据库归档表，也不把失败归因写入 MySQL 或 TDengine。
3. 不要求当前 registry 先批量补齐 failure taxonomy 元数据。
4. 不做 flaky 判定、自动重跑、重跑建议、趋势图或 CI 分类告警。
5. 不做多标签归因、置信度评分或复杂规则解释面板。
6. 不恢复旧的 `研发工场 / 执行中心 / 结果与基线中心` 入口。

## 4. 方案选型

### 4.1 归因粒度

首版采用 `模块级 + 场景级` 归因。

理由：

1. 业务验收结果页只需要模块级解释，避免把业务视角拉进底层排障细节。
2. 自动化治理台需要场景级解释，便于研发和测试快速定位。
3. 该粒度已经足够支撑后续 flaky 治理和失败趋势的扩展。

### 4.2 归因来源

首版采用“规则归因为主”的路线，不要求先回填 registry taxonomy 元数据。

规则输入固定来自现有结果和证据：

1. `registry-run-*.json` 中的 run / module / scenario 失败结果。
2. `failureDetails`、`stepLabel`、`apiRef`、`pageAction` 等现有失败描述。
3. evidence 列表和可预览文本 evidence 的关键字。
4. 执行器类型，如 `browserPlan / apiSmoke / messageFlow / riskDrill`。

理由：

1. 可以直接建立在现有真相源上，不阻塞首轮交付。
2. 不需要先对历史 registry 做大规模元数据回填。
3. 后续仍可兼容“规则归因 + 人工声明优先归因”的升级路线。

### 4.3 归因生成时机

归因在生成 `automation-result-index` 时一并产出。

理由：

1. 前后端可以共享同一份失败解释，不会出现不同页面各自猜测的情况。
2. 归档索引已经是 run 级结果稳定读入口，顺势扩字段最自然。
3. 查询链保持轻量，避免每次打开结果页都重复扫描原始 evidence。

## 5. 失败分类口径

首版固定使用以下七类：

1. `环境`
2. `数据`
3. `权限`
4. `接口`
5. `UI`
6. `断言`
7. `其他`

分类口径说明：

1. `环境`：目标地址不可达、连接被拒绝、超时、依赖服务不可用、共享环境组件异常。
2. `数据`：前置数据缺失、样本不存在、模板或实体未准备好、列表为空且按场景语义应存在。
3. `权限`：登录态缺失、菜单不可见、接口 `401/403`、授权不足。
4. `接口`：服务 `5xx`、响应结构断裂、契约字段缺失、后端业务错误主导失败。
5. `UI`：元素未渲染、选择器未命中、控件不可点击、对话框或页面状态未出现。
6. `断言`：流程已走通，但断言本身不满足，如 `assertText`、`assertUrlIncludes`、`assertVariableEquals` 失败。
7. `其他`：未命中任何已知规则，或 evidence 不足以稳定判断。

## 6. 归因规则链

### 6.1 场景级主分类

每个失败场景只产出一个主分类，按固定优先级命中：

1. `权限`
2. `环境`
3. `接口`
4. `UI`
5. `数据`
6. `断言`
7. `其他`

优先级说明：

1. `权限` 与 `环境` 具有更强的阻断语义，优先从大量噪音中提出来。
2. `接口`、`UI`、`数据` 属于主要功能问题分类。
3. `断言` 只在流程可达且未命中更明确问题时作为主分类。
4. `其他` 作为兜底，避免为了“看起来智能”而强行猜测。

### 6.2 场景级命中信号

首版规则信号使用现有字符串和结构字段，不引入新数据库配置。

#### `权限`

命中任一信号即可：

1. 响应状态或 evidence 包含 `401`、`403`、`unauthorized`、`forbidden`
2. 失败描述包含 `无权限`、`未授权`、`登录失效`
3. 页面动作或 UI 证据包含 `菜单不可见`、`权限不足`

#### `环境`

命中任一信号即可：

1. evidence 包含 `ECONNREFUSED`、`timeout`、`ETIMEDOUT`、`DNS`
2. 失败描述包含 `服务未启动`、`页面不可达`、`依赖不可用`
3. message-flow / risk-drill 证据指向 Redis、MQTT、MySQL、TDengine 等依赖不可用

#### `接口`

命中任一信号即可：

1. 响应状态或 evidence 包含 `500`、`502`、`503`、`504`
2. 失败描述包含 `接口响应异常`、`contract mismatch`、`响应缺字段`
3. API smoke 或 browser plan 捕获到明确后端业务异常主导失败

#### `UI`

命中任一信号即可：

1. evidence 包含 `selector not found`、`element not found`、`not clickable`
2. 失败描述包含 `页面未渲染`、`按钮不可点击`、`对话框未出现`
3. 视觉相关断言或页面交互信号明确失败，且未命中更高优先级的接口/环境/权限问题

#### `数据`

命中任一信号即可：

1. evidence 或失败描述包含 `数据不存在`、`记录为空`、`样本缺失`
2. 前置数据或模板准备失败
3. 场景语义要求存在数据，但读侧结果为空且未命中权限/环境/UI/接口问题

#### `断言`

命中任一信号即可：

1. evidence 包含 `assertText`、`assertUrlIncludes`、`assertVariableEquals` 等断言失败关键字
2. 流程可达、接口可达、UI 元素可达，但断言不满足

#### `其他`

当以上规则全部未命中时：

1. 分类固定为 `其他`
2. 原因文案固定为“未命中已知规则，建议查看原始证据”

### 6.3 模块级主分类

模块级归因不独立猜测，只聚合失败场景结果：

1. 若模块下所有失败场景同类，则直接继承该类。
2. 若出现多类失败，则按“出现次数最多”决定主类。
3. 若次数打平，则按场景级主分类优先级取主类。
4. 模块级原因文案需明确多类混合情况，例如：
   - `3 个失败场景中 2 个命中接口问题，另有 1 个 UI 问题`

## 7. 索引结构扩展

### 7.1 run 级新增字段

在现有 `automation-result-index.latest.json` 的 `runs[]` 记录里新增：

1. `failureSummary`
2. `failedModules[]`
3. `failedScenarios[]`

建议结构：

```json
{
  "runId": "20260426103000",
  "status": "failed",
  "failureSummary": {
    "primaryCategory": "接口",
    "countsByCategory": {
      "接口": 2,
      "UI": 1
    }
  },
  "failedModules": [
    {
      "moduleCode": "product-governance",
      "moduleName": "产品治理",
      "failedScenarioCount": 2,
      "diagnosis": {
        "category": "接口",
        "reason": "2 个失败场景中接口问题占多数",
        "evidenceSummary": "compare 接口返回 500；版本台账接口响应缺字段"
      }
    }
  ],
  "failedScenarios": [
    {
      "scenarioId": "product.contract.compare",
      "scenarioTitle": "契约字段 compare",
      "moduleCode": "product-governance",
      "runnerType": "apiSmoke",
      "stepLabel": "调用 compare 接口",
      "apiRef": "POST /api/device/product/{id}/compare",
      "pageAction": "",
      "diagnosis": {
        "category": "接口",
        "reason": "命中 500 响应异常规则",
        "evidenceSummary": "接口返回 500，响应体含 compare failed"
      }
    }
  ]
}
```

### 7.2 不变字段

本轮保持以下字段语义不变：

1. `runId`
2. `updatedAt`
3. `reportPath`
4. `status`
5. `summary`
6. `packageCode`
7. `environmentCode`
8. `runnerTypes`
9. `evidenceItems`
10. `skippedFiles`

## 8. 后端读侧边界

### 8.1 结果查询服务

现有索引驱动查询继续保留，但 detail 读侧需要把新增归因字段返回给前端：

1. 运行级失败分类分布
2. 失败模块诊断摘要
3. 失败场景诊断摘要

### 8.2 业务验收结果页

业务验收结果读侧应优先消费索引中的模块级归因，而不是让前端自行推理：

1. 失败模块结果新增 `diagnosis.category`
2. 失败模块结果新增 `diagnosis.reason`
3. 失败模块结果新增 `diagnosis.evidenceSummary`

### 8.3 不改动的接口边界

本轮不改变以下接口的基础语义：

1. `GET /api/report/business-acceptance/results/{runId}` 仍回答业务结论和模块结果，只是补诊断字段。
2. `GET /api/report/automation-results/page`、`recent` 仍以归档索引为主，只是补可读归因字段。
3. `GET /api/report/automation-results/{runId}`、`evidence`、`evidence/content` 仍回到原始 `registry-run` 与 evidence 文件。

## 9. 前端表达

### 9.1 业务验收结果页

`/business-acceptance/results/:runId` 保持现有“先回答是否通过”的结构，不做页面重构。

仅在失败模块卡片新增：

1. `主分类`
2. `简短原因`
3. `证据摘要`

表达原则：

1. 业务页只展示模块级诊断，不展示场景级规则细节。
2. 不展示 matched rule id、原始关键字或长串 technical detail。
3. 若当前账号具备治理台权限，继续保留跳到 `/automation-governance?tab=evidence&runId=...` 的入口。

### 9.2 自动化治理台结果证据

`/automation-governance?tab=evidence` 保持现有结果台账 + 详情布局，但新增两层表达：

1. 运行级摘要区
   - `主要失败类型`
   - `失败分类分布`
2. 失败场景列表
   - `分类`
   - `简短原因`
   - `证据摘要`

表达原则：

1. 治理台优先服务研发和测试，可以比业务页多一层场景级视角。
2. 首版仍不展示复杂规则面板、置信度或多标签。
3. 归因命中 `其他` 时，页面要明确提示“建议查看原始证据”。

## 10. 测试策略

### 10.1 Node

为索引生成链补充失败归因测试，覆盖：

1. `权限 / 环境 / 接口 / UI / 数据 / 断言 / 其他` 七类场景命中
2. 模块级聚合归因
3. `其他` 兜底文案
4. 归因字段与原有 run 索引字段共存

### 10.2 Java

后端读侧测试覆盖：

1. 索引 detail 可返回 `failureSummary / failedModules / failedScenarios`
2. 业务验收结果可读到模块级诊断
3. 索引缺失或重建后，归因字段依然稳定

### 10.3 前端

Vitest 至少覆盖：

1. 业务验收结果页展示模块级 `分类 / 原因 / 证据摘要`
2. 自动化治理台结果证据展示运行级分布
3. 自动化治理台结果证据展示场景级 `分类 / 原因 / 证据摘要`
4. `其他` 分类时的兜底提示

## 11. 风险与约束

1. 首版规则归因基于字符串和现有 evidence 结构，不保证百分之百精确，但必须保持稳定、可解释。
2. 归因结果不是新的运行真相，只是统一解释层。
3. 不允许为了抬高“命中率”而做含糊猜测；命不中就落 `其他`。
4. 若后续发现某一类规则误判率明显偏高，应通过补规则或补元数据修复，而不是在页面硬编码特例。

## 12. 验收标准

1. 新生成的 `automation-result-index.latest.json` 必须包含：
   - `failureSummary`
   - `failedModules[]`
   - `failedScenarios[]`
2. 失败场景必须稳定落到 `环境 / 数据 / 权限 / 接口 / UI / 断言 / 其他` 之一。
3. 业务验收结果页对失败模块显示：
   - `分类`
   - `简短原因`
   - `命中证据摘要`
4. 自动化治理台结果证据工作区显示：
   - 运行级失败分类分布
   - 场景级 `分类 / 原因 / 证据摘要`
5. 规则未命中时，页面必须明确显示：
   - 分类为 `其他`
   - 原因说明为“未命中已知规则，建议查看原始证据”
6. 全链路不改变当前真实环境执行入口、`passed / failed / blocked` 判定和 readiness gate 结论。
