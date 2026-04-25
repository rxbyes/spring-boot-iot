# 质量工场覆盖历史与趋势对比设计

> 日期：2026-04-25  
> 范围：`scripts/auto`、`logs/acceptance`、质量工场覆盖治理文档  
> 设计来源：在覆盖矩阵与准入策略门禁已落地后，继续推进全能自动化测试平台第三阶段的趋势分析最小切片  

## 1. 背景

当前质量工场已经具备三层自动化治理能力：

1. `run-acceptance-registry.mjs` 统一执行 registry 场景并生成 `registry-run-<runId>.json/.md`。
2. `generate-acceptance-coverage.mjs` 从 `acceptance-registry.json` 与 `business-acceptance-packages.json` 生成覆盖矩阵。
3. `acceptance-coverage-policy.json` 可以把缺失引用、P1/P2 元数据、最低覆盖和执行器分布转为 readiness gate。

这些能力能回答“当前覆盖是否合格”，但还不能回答“覆盖相对上一次变好了还是变差了”。第三阶段平台化目标中的趋势分析、覆盖缺口演进和 CI readiness 追踪，都需要一个轻量的历史对比入口作为基础。

## 2. 目标

本轮新增离线覆盖历史对比能力，回答以下问题：

1. 两份覆盖矩阵之间新增或移除了哪些场景。
2. 两份覆盖矩阵之间新增或移除了哪些业务验收包。
3. P0/P1/P2、执行器类型和责任域覆盖数量如何变化。
4. 缺口数量和 policy error/warning 是否增加或减少。
5. 对比结果能沉淀为 JSON/Markdown 证据，供封板评审、周回归和后续 CI 使用。

## 3. 非目标

1. 不接数据库，不新增自动化结果归档表。
2. 不改 `/automation-results` 后端查询模型。
3. 不新增前端热力图或趋势页面。
4. 不新增 CI、定时任务或通知分发配置。
5. 不重新执行真实业务验收，只比较已经生成的 `acceptance-coverage-*.json`。

## 4. 方案

新增一个离线 CLI：

```bash
node scripts/auto/diff-acceptance-coverage.mjs
node scripts/auto/diff-acceptance-coverage.mjs --baseline-path=logs/acceptance/acceptance-coverage-a.json --current-path=logs/acceptance/acceptance-coverage-b.json
```

默认不传路径时，CLI 在 `logs/acceptance` 中按文件名时间戳选择最近两份 `acceptance-coverage-*.json`，较早一份作为 baseline，较新一份作为 current。显式传入路径时，按用户提供的文件对比，适合 CI 或封板复盘固定证据。

输出：

```text
logs/acceptance/acceptance-coverage-diff-<timestamp>.json
logs/acceptance/acceptance-coverage-diff-<timestamp>.md
```

该 CLI 只消费覆盖矩阵 JSON，不重新读取 registry/package，也不访问后端、前端、MQTT、数据库或真实环境。

## 5. 数据结构

JSON 报告结构：

```json
{
  "generatedAt": "2026-04-25T18:30:00.000Z",
  "baselinePath": "logs/acceptance/acceptance-coverage-20260425170000.json",
  "currentPath": "logs/acceptance/acceptance-coverage-20260425183000.json",
  "summary": {
    "scenarioDelta": 1,
    "packageDelta": 0,
    "missingScenarioRefsDelta": 0,
    "unreferencedScenariosDelta": -1,
    "metadataMissingScenariosDelta": 0,
    "policyErrorsDelta": 0,
    "policyWarningsDelta": -1,
    "status": "improved"
  },
  "changes": {
    "scenarios": {
      "added": ["object-insight.p1.browser-smoke"],
      "removed": []
    },
    "packages": {
      "added": [],
      "removed": []
    },
    "coverageByPriority": {
      "P1": { "baseline": 5, "current": 6, "delta": 1 }
    },
    "coverageByRunnerType": {
      "browserPlan": { "baseline": 8, "current": 9, "delta": 1 }
    },
    "coverageByOwnerDomain": {
      "object-insight": { "baseline": 0, "current": 1, "delta": 1 }
    }
  },
  "policyEvaluation": {
    "baseline": { "status": "passed", "errors": 0, "warnings": 1 },
    "current": { "status": "passed", "errors": 0, "warnings": 0 }
  }
}
```

`summary.status` 取值：

1. `regressed`：policy errors 增加、missing refs 增加、metadata gaps 增加，或 P0 场景减少。
2. `improved`：阻断缺口减少、warning 减少、覆盖数量增加且没有触发回退条件。
3. `unchanged`：关键指标无变化。
4. `mixed`：既有改善也有回退，但未达到 `regressed` 的阻断级别。

## 6. 对比算法

算法只依赖现有覆盖矩阵字段：

1. 从 `matrix.scenarios[].id` 提取场景集合，计算 added/removed。
2. 从 `matrix.packages[].packageCode` 提取验收包集合，计算 added/removed。
3. 从 `coverageByPriority / coverageByRunnerType / coverageByOwnerDomain` 提取 bucket `total`，计算 baseline/current/delta。
4. 从 `summary` 读取 `missingScenarioRefs / unreferencedScenarios / metadataMissingScenarios`，计算缺口变化。
5. 从可选的 `policyEvaluation.summary` 读取 `errors / warnings`，无 policy 时视为 `null`，报告中明确显示“未提供策略评估”。
6. 按第 5 节的规则生成趋势状态。

如果输入矩阵缺少必要字段，CLI 应报错并退出 `1`，避免用损坏证据生成误导性趋势。

## 7. Markdown 报告

Markdown 输出包含：

1. `# Acceptance Coverage Diff`
2. baseline/current 路径和生成时间。
3. Summary 表格，展示场景、包、缺口、policy errors/warnings 的 baseline/current/delta。
4. Added/Removed Scenarios。
5. Added/Removed Packages。
6. Coverage By Priority / Runner Type / Owner Domain 差异表。
7. Policy Evaluation 差异摘要。
8. Next Actions：
   - `regressed`：要求先处理回退项再进入封板或 CI readiness。
   - `mixed`：要求负责人确认回退项是否可接受。
   - `improved/unchanged`：保留报告作为覆盖治理证据。

## 8. 错误处理

1. `logs/acceptance` 中少于两份覆盖矩阵：退出 `1`，提示需要先运行 `generate-acceptance-coverage.mjs` 至少两次或显式传入路径。
2. 显式路径不存在：退出 `1`。
3. JSON 非法：退出 `1`。
4. 文件不是覆盖矩阵结构：退出 `1`。
5. baseline 与 current 指向同一个文件：退出 `1`。
6. 输出目录不存在：自动创建。

## 9. 测试策略

新增 Node 测试覆盖：

1. 对比两个内存矩阵能正确生成 added/removed 场景和包。
2. P0 场景减少时状态为 `regressed`。
3. warning 减少且无回退时状态为 `improved`。
4. 同时有非阻断改善和非阻断回退时状态为 `mixed`。
5. CLI 默认选择最近两份 `acceptance-coverage-*.json`。
6. CLI 显式传入 baseline/current 路径时生成 JSON/Markdown。
7. 少于两份覆盖矩阵、非法 JSON、同一路径输入会退出 `1`。

回归验证继续执行：

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/docs/check-topology.mjs
git diff --check
```

## 10. 文档影响

需要同步更新：

1. `README.md`：增加覆盖趋势对比命令入口。
2. `docs/05-自动化测试与质量保障.md`：补充趋势对比用途、命令、报告判读。
3. `docs/真实环境测试与验收手册.md`：补充封板或周回归时如何留存 diff 证据。
4. `docs/21-业务功能清单与验收标准.md`：说明第三阶段覆盖治理已支持历史对比，但仍不替代真实环境执行验收。
5. `AGENTS.md`：如质量工场当前状态需要记录趋势对比能力，则追加当前状态说明。

## 11. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 覆盖矩阵文件由不同分支生成 | 差异可能包含非同源变化 | 报告记录 baseline/current 路径，封板时固定显式路径 |
| 文件名时间戳不可靠 | 默认最近两份选错 | 显式 `--baseline-path / --current-path` 优先，默认选择只用于本地快速检查 |
| policyEvaluation 缺失 | 无法判断 policy 变化 | 报告显示 policy 未提供，不把缺失 policy 默认为通过 |
| 趋势状态过度简化 | mixed 场景需要人工判断 | Markdown Next Actions 明确要求负责人确认 |

## 12. 成功标准

1. CLI 能从最近两份覆盖矩阵自动生成 diff JSON/Markdown。
2. CLI 能按显式 baseline/current 路径生成 diff JSON/Markdown。
3. 报告能展示场景、包、优先级、执行器、责任域、gap 和 policy 的变化。
4. 回退场景能被标为 `regressed`，改善场景能被标为 `improved`。
5. 新增能力有 Node 测试和文档说明。
6. 能与现有覆盖矩阵、policy 门禁、真实环境验收文档保持同一事实源口径。
