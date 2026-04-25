# 质量工场覆盖准入策略设计

> 日期：2026-04-25  
> 适用范围：`config/automation`、`scripts/auto`、质量工场验收文档  
> 设计来源：在覆盖矩阵生成器已落地的基础上，补齐可配置的 readiness gate  

## 1. 目标

当前 `scripts/auto/generate-acceptance-coverage.mjs` 已能从统一验收注册表与业务验收包生成覆盖矩阵，回答“有哪些场景、被哪些包引用、缺哪些元数据”。下一步需要把这份描述性报告升级为可配置的准入判断，让团队可以在封板、周回归或 CI 中明确回答：

1. 当前覆盖矩阵是否满足本轮准入策略。
2. 哪些缺口是阻断项，哪些只是待治理提醒。
3. 策略判断结果能否沉淀到同一份 JSON/Markdown 证据中。

本轮只做离线策略评估，不接数据库、不接前端热力图、不新增 CI 调度。

## 2. 边界

### 2.1 纳入范围

- 新增 `config/automation/acceptance-coverage-policy.json` 作为默认策略样例和团队基线。
- 扩展覆盖矩阵库，新增策略评估函数。
- 扩展 CLI，支持显式传入 `--policy-path=...`。
- JSON/Markdown 覆盖报告中追加策略评估结果。
- 更新质量工场相关文档，说明策略文件、退出码和使用场景。

### 2.2 不纳入范围

- 不改变 `node scripts/auto/generate-acceptance-coverage.mjs` 无参数命令的默认行为。
- 不把策略评估结果写入数据库或结果中心后端。
- 不新增前端页面、热力图或趋势分析。
- 不新增 CI/Jenkins/GitHub Actions 配置。
- 不改变 `run-acceptance-registry.mjs` 的真实执行语义。

## 3. 方案选择

采用“**显式策略文件 + 报告内嵌评估结果**”方案。

对比三个方案：

1. 继续只用 `--fail-on-gaps` 粗粒度失败判断。实现最少，但无法表达“baseline 可暂缓、P1 元数据必须齐、runner 分布只告警”这类真实准入规则。
2. 直接把策略写死到覆盖矩阵生成器。接入快，但后续 P2/P3、不同发布窗口和团队口径都会变成改代码。
3. 使用 JSON 策略文件。实现稍多，但能把准入规则变成可审查、可版本化、可在 CI 中复用的配置。

本轮选第 3 种。推荐原因是它沿用现有 JSON 配置治理方式，不增加运行时依赖，也方便后续把策略文件交给质量负责人维护。

## 4. 策略文件设计

默认策略文件：

```text
config/automation/acceptance-coverage-policy.json
```

建议结构：

```json
{
  "version": "1.0.0",
  "policyName": "quality-factory-default-readiness",
  "rules": {
    "missingScenarioRefs": {
      "severity": "error",
      "allowScenarioRefs": []
    },
    "unreferencedScenarios": {
      "severity": "warning",
      "allowScenarioIds": [],
      "allowScopes": ["baseline"]
    },
    "metadata": {
      "severity": "error",
      "requiredPriorities": ["P1", "P2"]
    },
    "minimumScenarioCountByPriority": {
      "severity": "error",
      "minimums": {
        "P0": 1,
        "P1": 1
      }
    },
    "requiredRunnerTypes": {
      "severity": "warning",
      "runnerTypes": ["browserPlan", "apiSmoke"]
    }
  }
}
```

字段含义：

1. `missingScenarioRefs`：业务验收包引用不存在的 registry 场景。默认阻断。
2. `unreferencedScenarios`：registry 场景未被任何业务验收包消费。默认告警，允许 `baseline` 场景暂缓。
3. `metadata`：指定优先级场景必须具备治理元数据。默认 P1 / P2 阻断。
4. `minimumScenarioCountByPriority`：指定优先级至少要有若干场景。用于防止 P0/P1 包被误删或清空。
5. `requiredRunnerTypes`：指定执行器类型至少要出现一次。默认告警，用于提醒覆盖结构失衡。

`severity` 仅支持 `error / warning`。未知规则或未知 severity 应被视为策略配置错误并让 CLI 返回非零，避免策略文件拼错后被静默忽略。

## 5. CLI 设计

新增参数：

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

行为规则：

1. 不传 `--policy-path` 时，保持现有行为：只生成覆盖矩阵，退出码仍由 `--fail-on-gaps` 决定。
2. 传入 `--policy-path` 时，CLI 读取策略文件、评估矩阵，并把 `policyEvaluation` 写入 JSON。
3. 只要策略评估中存在 `severity=error` 且 `status=failed` 的规则，CLI 退出码为 `1`。
4. `severity=warning` 的失败规则只进入报告，不影响退出码。
5. 若同时传入 `--fail-on-gaps`，则粗粒度 gap 和策略 error 任一失败都会让退出码为 `1`。

## 6. 评估结果结构

JSON 报告追加：

```json
{
  "policyEvaluation": {
    "policyName": "quality-factory-default-readiness",
    "policyVersion": "1.0.0",
    "status": "passed",
    "summary": {
      "totalRules": 5,
      "passed": 5,
      "failed": 0,
      "warnings": 0,
      "errors": 0
    },
    "results": [
      {
        "ruleId": "missingScenarioRefs",
        "severity": "error",
        "status": "passed",
        "message": "No missing scenario references.",
        "details": []
      }
    ]
  }
}
```

Markdown 报告新增 `## Policy Evaluation`：

- 首行展示策略名称、版本、状态。
- 表格展示每条规则的 severity、status、message。
- 对失败规则列出可行动对象，例如缺失的 `scenarioRef`、缺元数据的 `scenarioId`。

## 7. 错误处理

1. 策略文件不存在：CLI 报错并退出 `1`。
2. 策略 JSON 非法：CLI 报错并退出 `1`。
3. 策略字段类型非法：CLI 报错并退出 `1`。
4. 未知规则：CLI 报错并退出 `1`。
5. 未知 severity：CLI 报错并退出 `1`。
6. warning 规则失败：报告记录 warning，CLI 仍可退出 `0`。

## 8. 测试设计

新增或扩展 Node 测试：

1. `evaluateCoveragePolicy` 对缺失 `scenarioRef` 返回 error failed。
2. `metadata` 规则能识别 P1/P2 场景缺少 `ownerDomain / failureCategory / dataSetup.strategy / cleanupPolicy.strategy`。
3. `unreferencedScenarios` 能按 `allowScopes=["baseline"]` 放行 baseline 场景，但仍对 delivery 场景告警。
4. `minimumScenarioCountByPriority` 能在 P0 场景数不足时失败。
5. CLI 传入 `--policy-path` 后，JSON/Markdown 均包含策略评估结果。
6. CLI 在 policy error failed 时退出 `1`，只有 warning failed 时退出 `0`。

回归验证继续包含：

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/docs/check-topology.mjs
git diff --check
```

## 9. 文档影响

需要同步更新：

- `README.md`
- `AGENTS.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/真实环境测试与验收手册.md`
- `docs/21-业务功能清单与验收标准.md`

文档口径固定为：覆盖准入策略只评估自动化资产治理质量，不替代真实环境执行验收；策略通过表示覆盖配置满足当前 readiness gate，不表示业务链路已经真实跑通。

## 10. 后续扩展

本轮策略评估完成后，后续可继续逐步增加：

1. 历史覆盖矩阵对比，输出覆盖变化趋势。
2. CI 读取同一策略文件作为发布门禁。
3. 结果中心读取 `acceptance-coverage-*.json` 做只读覆盖热力图。
4. 按 ownerDomain 汇总质量责任人和待治理队列。
