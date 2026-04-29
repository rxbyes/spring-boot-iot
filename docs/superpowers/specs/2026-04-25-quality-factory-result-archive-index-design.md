# 质量工场结果归档索引设计

- 日期：`2026-04-25`
- 范围：`scripts/auto`、`spring-boot-iot-report`、`spring-boot-iot-ui`、`docs/*`
- 状态：`proposed`

## 1. 背景

质量工场当前已经完成两条主路径收口：

1. `/business-acceptance` 业务验收台负责轻量发起与业务结论。
2. `/automation-governance?tab=evidence` 自动化治理台负责历史台账、失败明细和证据预览。

同时，覆盖治理链也已经补齐：

1. `generate-acceptance-coverage.mjs` 生成覆盖矩阵。
2. `acceptance-coverage-policy.json` 提供 readiness policy。
3. `diff-acceptance-coverage.mjs` 提供覆盖趋势差异。
4. `run-acceptance-readiness.mjs` 提供封板或 CI 前的治理就绪报告。

但结果证据链仍然停留在“现扫文件”的第一阶段：

1. 后端 `AutomationResultQueryServiceImpl` 每次分页都会遍历 `logs/acceptance/registry-run-*.json`。
2. 最近运行、筛选和 runId 详情都直接依赖文件扫描，不存在稳定索引。
3. 自动化治理台虽然已经收入口径，但结果证据工作区仍缺少 `环境 / 验收包` 等治理维度。
4. 未来要做失败分类、flaky 治理、趋势统计或通知分发时，没有统一可复用的结果索引底座。

根据 `2026-04-25-quality-factory-automation-test-plan-design.md` 的第三阶段路线，下一步应优先补齐“集中式结果归档模型，保留文件证据但增加可查询索引”的最小切片。

## 2. 目标

本轮目标是把当前“文件即结果”的模式升级为“文件真相 + 索引消费”的模式：

1. 新增一个离线结果归档索引 CLI，扫描 `logs/acceptance` 生成标准索引文件。
2. 保持 `logs/acceptance` 与原始 `registry-run-*.json` 为真相源，不新增数据库表。
3. 后端台账查询改为“索引优先”，在索引缺失或过旧时自动补建。
4. 自动化治理台的“结果证据”工作区升级为支持 `关键词 / 状态 / 执行器 / 验收包 / 环境 / 日期` 的统一检索视图。
5. 保持 run 详情与证据内容读取链稳定，不重写现有证据预览模型。

## 3. 非目标

本轮明确不做以下事项：

1. 不新增数据库归档表，也不引入 MySQL / TDengine 结果存储。
2. 不把场景结果拆成单独 scenario 级索引，也不做 evidence 文件全文索引。
3. 不新增失败分类、flaky 识别、趋势图、通知分发或 CI 平台接入。
4. 不修改真实环境执行入口，不替代 `run-acceptance-registry.mjs`。
5. 不改动业务验收台的轻量发起边界。

## 4. 方案选型

### 4.1 真相源

采用“`logs/acceptance` + 索引文件”为真相源。

理由：

1. 与当前结果和证据留痕口径一致，不引入新持久化真相。
2. 便于在共享环境、本地工作区和离线封板场景下复用。
3. 后续如要落库，仍可由索引文件或同一 schema 继续演进。

### 4.2 索引粒度

首版只做 `run 级索引 + evidence 元数据`。

理由：

1. 已足够支撑结果台账、最近运行、包/环境筛选和证据入口。
2. 不会把首轮实现扩张成新的分析平台。
3. 为后续失败分类和 flaky 治理保留演进空间。

### 4.3 刷新策略

采用“查询时自动补建 + 页面手动刷新”。

理由：

1. 用户首次进入治理台时不需要先跑 CLI。
2. 后端可以在缺失或过旧时自动重建 `.latest` 索引。
3. 页面仍保留显式“刷新索引”动作，便于共享环境核对最新结果。

## 5. 索引产物

### 5.1 文件路径

索引输出固定落在 `logs/acceptance`：

1. `automation-result-index.latest.json`
2. `automation-result-index-<timestamp>.json`
3. `automation-result-index-<timestamp>.md`

其中：

1. `.latest.json` 供后端查询稳定读取。
2. 时间戳 JSON/Markdown 用于封板、复盘和审计留痕。

### 5.2 JSON 结构

建议结构：

```json
{
  "generatedAt": "2026-04-25T15:30:00.000Z",
  "resultsDir": "logs/acceptance",
  "sourceSummary": {
    "registryRunFiles": 12,
    "indexedRuns": 11,
    "skippedFiles": 1
  },
  "facets": {
    "statuses": ["passed", "failed"],
    "runnerTypes": ["browserPlan", "riskDrill"],
    "packageCodes": ["quality-factory-p0", "product-governance-p1"],
    "environmentCodes": ["dev", "sit"]
  },
  "runs": [
    {
      "runId": "20260425153000",
      "updatedAt": "2026-04-25T15:30:12+08:00",
      "reportPath": "logs/acceptance/registry-run-20260425153000.json",
      "status": "failed",
      "summary": {
        "total": 8,
        "passed": 6,
        "failed": 2
      },
      "packageCode": "quality-factory-p0",
      "environmentCode": "dev",
      "runnerTypes": ["browserPlan", "apiSmoke"],
      "failedScenarioIds": ["quality-factory-login"],
      "evidenceItems": [
        {
          "path": "logs/acceptance/registry-run-20260425153000.json",
          "fileName": "registry-run-20260425153000.json",
          "category": "run-summary",
          "source": "report"
        }
      ]
    }
  ],
  "skippedFiles": [
    {
      "fileName": "registry-run-20260425142000.json",
      "reason": "invalid-json"
    }
  ]
}
```

### 5.3 Markdown 结构

Markdown 报告用于人工留痕，至少包含：

1. `# Automation Result Archive Index`
2. 生成时间、目录、索引数量、跳过数量
3. Facets 摘要
4. 最近若干次运行的表格摘要
5. 跳过文件和原因
6. 下一步建议（如刷新索引、修复坏文件、进入治理台复盘）

## 6. run 级索引字段

首版每条运行记录收集以下字段：

1. `runId`
2. `updatedAt`
3. `reportPath`
4. `status`
5. `summary.total / passed / failed`
6. `packageCode`
7. `environmentCode`
8. `runnerTypes[]`
9. `failedScenarioIds[]`
10. `evidenceItems[]`

字段来源：

1. `status / summary / runnerTypes / failedScenarioIds / evidenceItems` 来自 `registry-run-*.json`。
2. `packageCode / environmentCode` 优先来自 `options.packageCode / options.environmentCode`。
3. `updatedAt` 来自结果文件修改时间，保持与当前结果查询口径一致。

如 `packageCode` 或 `environmentCode` 缺失，索引仍保留该运行，但 facet 不把空值计入集合。

## 7. 后端架构

### 7.1 新增服务

新增 `AutomationResultArchiveIndexService`，职责：

1. 扫描 `resultsDir`
2. 解析并校验 `registry-run-*.json`
3. 构建统一索引模型
4. 写出 `.latest.json` 与时间戳 JSON/Markdown
5. 判断现有索引是否缺失或过旧
6. 为查询服务提供最新索引读侧入口

### 7.2 查询读链

结果查询服务改为双层模型：

1. `page` 和 `recent` 接口优先读取归档索引。
2. 索引缺失、过旧或结构非法时，先自动补建，再继续查询。
3. `detail`、`evidence`、`evidence/content` 继续读取原始运行文件，避免本轮扩大重构面。

### 7.3 新增接口

在现有 `/api/report/automation-results` 下补齐：

1. `GET /facets`
   - 返回 `statuses / runnerTypes / packageCodes / environmentCodes`
2. `POST /refresh-index`
   - 手动重建索引
   - 返回最新索引摘要

现有 `GET /page` 与 `GET /recent` 新增可选筛选参数：

1. `packageCode`
2. `environmentCode`

## 8. 前端工作区改造

### 8.1 页面边界

保持入口不变：

1. `/automation-governance?tab=evidence`

不新增独立结果路由，不恢复旧的 `/automation-results` 一级入口。

### 8.2 结果证据工作区

“结果证据”工作区保留当前对称布局，但升级台账检索头：

1. 关键词
2. 状态
3. 执行器
4. 验收包
5. 环境
6. 日期范围
7. 刷新台账
8. 刷新索引

列表列建议补齐：

1. `packageCode`
2. `environmentCode`
3. `runnerTypes`
4. `summary`
5. `evidence count`

最近运行、当前判断、失败场景明细和证据预览仍沿用现有工作区，不单独新开页面。

## 9. 错误处理

### 9.1 索引构建

以下情况不应阻断整个结果台账：

1. 单个 `registry-run` 文件 JSON 非法
2. 单个文件结构缺字段
3. 某个 evidence 文件丢失

这些情况应：

1. 记录到 `skippedFiles`
2. 写入 Markdown 报告
3. 在刷新索引接口响应中回传摘要

以下情况应视为阻断：

1. `logs/acceptance` 目录不存在且无法读取
2. 无法写出 `.latest.json`
3. 索引结构整体非法且自动重建失败

### 9.2 查询侧

如索引读取失败且自动补建也失败：

1. `/page` 和 `/recent` 返回明确错误消息
2. 前端展示“结果台账加载失败，请检查索引或日志目录”

## 10. 测试策略

### 10.1 Node CLI

新增 Node 测试覆盖：

1. 最小目录可生成 `.latest.json` 与时间戳 JSON/Markdown
2. 非法 JSON 会进入 `skippedFiles`
3. `packageCode / environmentCode / runnerTypes` 会进入 facets
4. 证据条目会按 `report / related / scenario` 正确归类

### 10.2 Java

新增或扩展测试：

1. 索引服务可在缺失索引时构建并写出 `.latest.json`
2. 查询服务会在索引过旧时自动刷新
3. `page` 支持 `packageCode / environmentCode` 筛选
4. `recent` 从索引中返回最近运行
5. `facets` 接口返回稳定维度
6. `refresh-index` 接口返回最新索引摘要

### 10.3 前端

新增或扩展 Vitest：

1. 结果工作区会请求 facets 并渲染包/环境筛选项
2. 查询条件会正确带上 `packageCode / environmentCode`
3. “刷新索引”会调用新接口并刷新台账
4. 自动化治理台的 `runId` 深链行为保持不变

## 11. 文档影响

需要同步更新：

1. `README.md`
2. `AGENTS.md`
3. `docs/05-自动化测试与质量保障.md`
4. `docs/真实环境测试与验收手册.md`
5. `docs/21-业务功能清单与验收标准.md`
6. `docs/superpowers/README.md`

文档口径固定为：

1. 结果归档索引只提升查询和治理效率，不替代真实环境业务验收。
2. 结果真相仍来自 `logs/acceptance` 与原始 `registry-run` 文件。
3. 自动化治理台是研发、测试和管理员消费底层证据的唯一入口。

## 12. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Node CLI 与 Java 自动构建逻辑重复 | 后续维护分叉 | 固定统一 JSON schema 与字段口径，测试共同覆盖同一结构 |
| `.latest.json` 与真实文件不同步 | 查询结果过期 | 查询前比较最新 `registry-run` 修改时间，必要时自动重建 |
| 旧运行缺少 `packageCode / environmentCode` | 筛选维度不完整 | 允许空值，facets 只聚合存在的维度 |
| 坏文件较多影响索引可信度 | 台账判断失真 | 把 `skippedFiles` 公开进 JSON/Markdown 和刷新接口摘要 |
| 页面筛选项变多导致布局变重 | 用户体验下降 | 继续复用 `StandardListFilterHeader`，保持一行栅格和对称操作区 |

## 13. 成功标准

1. `logs/acceptance` 下可生成标准结果归档索引文件。
2. 后端台账查询从“现扫文件”升级为“索引优先”。
3. 自动化治理台结果证据工作区支持按包、环境、状态、执行器和日期检索。
4. 页面可手动刷新索引，后端也可在索引过旧时自动补建。
5. run 详情和证据预览链保持可用，不引入新的结果真相源。
