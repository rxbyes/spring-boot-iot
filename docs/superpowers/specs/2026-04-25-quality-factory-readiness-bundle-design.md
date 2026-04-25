# 质量工场验收就绪包设计

> 日期：2026-04-25  
> 范围：`scripts/auto`、`logs/acceptance`、质量工场覆盖治理文档  
> 设计来源：在覆盖矩阵、覆盖准入策略和覆盖趋势对比已落地后，继续推进全能自动化测试平台第三阶段的 CI/readiness 最小切片  

## 1. 背景

当前质量工场已经形成三类离线治理证据：

1. `generate-acceptance-coverage.mjs` 从统一验收注册表与业务验收包生成覆盖矩阵。
2. `acceptance-coverage-policy.json` 将缺失引用、P1/P2 元数据、最低覆盖和执行器分布转为准入策略。
3. `diff-acceptance-coverage.mjs` 对比两份覆盖矩阵，输出覆盖变化趋势。

这些能力分别回答“当前覆盖是什么”“当前覆盖是否满足策略”“覆盖相对上次是否变化”。但封板、周回归或后续 CI 入口仍需要一个更高层的统一报告，直接回答：

1. 本次自动化资产是否达到验收准备状态。
2. 当前覆盖策略是否通过。
3. 覆盖趋势是否出现回退。
4. 需要保留哪些 JSON/Markdown 证据。
5. 失败或告警后下一步应该处理什么。

本轮新增验收就绪包 CLI，把已有能力组合成一个可被本地、共享环境脚本、托管 CI 或定时任务复用的最小入口。

## 2. 目标

1. 新增一个离线 CLI，生成 `acceptance-readiness-<timestamp>.json/.md`。
2. 默认执行覆盖矩阵生成和覆盖策略评估。
3. 在存在可对比覆盖矩阵时执行趋势对比，或允许显式指定 baseline/current。
4. 用统一状态 `passed / warning / failed` 回答本次是否具备验收就绪性。
5. 在报告中聚合覆盖矩阵路径、策略结果、趋势结果和可行动建议。
6. 为后续 CI 或定时任务提供稳定命令，但本轮不绑定任何具体 CI 平台。

## 3. 非目标

1. 不执行真实业务验收场景，不替代 `run-acceptance-registry.mjs`。
2. 不访问后端、前端、MQTT、MySQL、TDengine 或浏览器。
3. 不新增数据库归档表，不改 `/automation-results` 后端查询模型。
4. 不新增前端页面、热力图、通知分发或 flaky 归因。
5. 不引入 GitHub Actions、Jenkins、Cron 或外部调度配置。
6. 不改变现有覆盖矩阵、策略门禁和趋势差异 CLI 的独立使用方式。

## 4. 方案

新增 CLI：

```bash
node scripts/auto/run-acceptance-readiness.mjs
node scripts/auto/run-acceptance-readiness.mjs --coverage-policy-path=config/automation/acceptance-coverage-policy.json
node scripts/auto/run-acceptance-readiness.mjs --baseline-coverage-path=logs/acceptance/acceptance-coverage-a.json --current-coverage-path=logs/acceptance/acceptance-coverage-b.json
node scripts/auto/run-acceptance-readiness.mjs --skip-diff
```

行为模式：

1. 默认模式：生成一份新的当前覆盖矩阵，并默认使用 `config/automation/acceptance-coverage-policy.json` 评估它。
2. baseline-only 模式：显式传入 `--baseline-coverage-path` 时，仍生成新的当前覆盖矩阵，再用显式 baseline 与本次 current 做趋势对比。
3. pinned-pair 模式：同时传入 `--baseline-coverage-path` 与 `--current-coverage-path` 时，不重新生成当前覆盖矩阵，而是读取显式 current 矩阵并用当前策略文件重新评估它，再对比显式 baseline/current。
4. no-history 模式：未传 baseline 时，在本次新生成矩阵之外查找最近一份历史覆盖矩阵作为 baseline；存在时执行 diff，不存在时记录 `diff.skippedReason`。
5. 所有模式都会输出统一就绪包：
   - `logs/acceptance/acceptance-readiness-<timestamp>.json`
   - `logs/acceptance/acceptance-readiness-<timestamp>.md`

该 CLI 是组合器，不复制覆盖矩阵或 diff 的核心算法。实现上应复用现有库函数，避免形成第二套判定逻辑。

## 5. 状态判定

就绪包状态固定为三档：

| 状态 | 含义 | CLI 退出码 |
|---|---|---|
| `passed` | 策略无 error，趋势未回退，或趋势被显式跳过 | `0` |
| `warning` | 策略只有 warning，或趋势为 `mixed`，需要人工确认 | `0` |
| `failed` | 策略存在 error，趋势为 `regressed`，或输入证据非法 | `1` |

判定优先级：

1. 输入参数、策略文件、覆盖矩阵文件或 JSON 结构非法时直接 `failed`。
2. 覆盖策略存在 `errors > 0` 时为 `failed`。
3. 覆盖趋势 `summary.status=regressed` 时为 `failed`。
4. 覆盖策略存在 `warnings > 0` 或趋势为 `mixed` 时为 `warning`。
5. 其余情况为 `passed`。

`--skip-diff` 只跳过趋势判断，不跳过覆盖策略。跳过时报告必须明确写出原因，避免 CI 使用者误以为已完成趋势对比。

## 6. 报告结构

JSON 报告结构：

```json
{
  "generatedAt": "2026-04-25T20:30:00.000Z",
  "status": "passed",
  "exitCode": 0,
  "inputs": {
    "coveragePolicyPath": "config/automation/acceptance-coverage-policy.json",
    "baselineCoveragePath": "logs/acceptance/acceptance-coverage-20260425190000.json",
    "currentCoveragePath": "logs/acceptance/acceptance-coverage-20260425203000.json",
    "skipDiff": false
  },
  "coverage": {
    "jsonPath": "logs/acceptance/acceptance-coverage-20260425203000.json",
    "markdownPath": "logs/acceptance/acceptance-coverage-20260425203000.md",
    "summary": {
      "totalScenarios": 16,
      "totalPackages": 13,
      "missingScenarioRefs": 0,
      "metadataMissingScenarios": 0,
      "hasGaps": false
    },
    "policyEvaluation": {
      "status": "passed",
      "summary": {
        "errors": 0,
        "warnings": 0
      }
    }
  },
  "diff": {
    "jsonPath": "logs/acceptance/acceptance-coverage-diff-20260425203000.json",
    "markdownPath": "logs/acceptance/acceptance-coverage-diff-20260425203000.md",
    "summary": {
      "status": "unchanged",
      "scenarioDelta": 0,
      "packageDelta": 0,
      "policyErrorsDelta": 0,
      "policyWarningsDelta": 0
    }
  },
  "nextActions": [
    "Coverage policy passed and no coverage regression was detected."
  ]
}
```

当趋势被跳过时：

```json
{
  "diff": {
    "skipped": true,
    "skippedReason": "No previous acceptance coverage matrix was found."
  }
}
```

## 7. Markdown 报告

Markdown 输出包含：

1. `# Acceptance Readiness`
2. 总状态、生成时间和建议退出码。
3. Coverage Summary：覆盖矩阵路径、场景数、包数、缺口数。
4. Policy Summary：策略名称、版本、errors、warnings、失败规则。
5. Coverage Diff：baseline/current 路径、趋势状态、关键 delta；跳过时展示跳过原因。
6. Evidence Artifacts：列出覆盖矩阵、diff、readiness 自身的 JSON/Markdown 路径。
7. Next Actions：
   - `failed`：列出必须先处理的策略 error 或趋势回退。
   - `warning`：列出需要人工确认的 warning 或 mixed 变化。
   - `passed`：说明可保留报告作为 readiness evidence，但仍需执行真实环境验收。

Markdown 必须避免写成“业务验收已通过”。正确口径是“自动化资产已具备验收准备状态”。

## 8. CLI 参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `--coverage-policy-path` | `config/automation/acceptance-coverage-policy.json` | 覆盖准入策略文件 |
| `--baseline-coverage-path` | 空 | 显式指定趋势 baseline |
| `--current-coverage-path` | 空 | 显式指定趋势 current；必须和 baseline 一起传入 |
| `--skip-diff` | `false` | 跳过趋势对比 |
| `--output-dir` | `logs/acceptance` | 就绪包与派生报告输出目录 |
| `--timestamp` | 当前时间 | 测试和固定证据使用 |

参数规则：

1. `--skip-diff` 与 `--baseline-coverage-path` 或 `--current-coverage-path` 同时传入时视为非法参数。
2. 只传 `--baseline-coverage-path` 是合法用法，表示 baseline 固定、current 使用本次新生成矩阵。
3. 只传 `--current-coverage-path` 时视为非法参数，避免 current 证据没有明确 baseline。
4. baseline 与 current 同时传入时，不重新生成 current，而是读取显式 current 并重新执行策略评估。
5. baseline 未显式传入时，默认查找本次生成矩阵之前最近的一份覆盖矩阵。
6. `--output-dir` 不存在时自动创建。

## 9. 错误处理

1. 策略文件不存在、JSON 非法或规则非法：退出 `1`，不生成误导性 `passed` 报告。
2. 显式 baseline/current 路径不存在或指向同一个文件：退出 `1`。
3. 显式覆盖矩阵结构非法：退出 `1`。
4. 覆盖矩阵生成失败：退出 `1`。
5. diff 执行失败：退出 `1`，除非用户显式传入 `--skip-diff`。
6. 输出目录创建失败或写入失败：退出 `1`。

失败时如果已经能确定部分证据，仍可写入 `acceptance-readiness-*.json/.md`，但状态必须是 `failed`，并在 `nextActions` 中说明阻断点。

## 10. 测试策略

新增 Node 测试覆盖：

1. 策略通过且趋势 `unchanged` 时，readiness 状态为 `passed`。
2. 策略 error 时，readiness 状态为 `failed` 且退出码为 `1`。
3. 策略 warning 时，readiness 状态为 `warning` 且退出码为 `0`。
4. 趋势 `regressed` 时，readiness 状态为 `failed`。
5. 趋势 `mixed` 时，readiness 状态为 `warning`。
6. `--skip-diff` 会记录跳过原因，不影响策略判定。
7. 默认模式能用本次新生成矩阵和上一份历史矩阵生成 diff。
8. 显式 baseline/current 能稳定生成 readiness、coverage diff 和证据路径。
9. 非法参数、缺失文件、同一路径输入会退出 `1`。

回归验证继续执行：

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/auto/acceptance-coverage-diff.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/auto/diff-acceptance-coverage.mjs
node scripts/docs/check-topology.mjs
git diff --check
```

## 11. 文档影响

需要同步更新：

1. `README.md`：增加 readiness 命令和定位。
2. `AGENTS.md`：补充质量工场当前状态中的就绪包入口。
3. `docs/05-自动化测试与质量保障.md`：说明 readiness 与真实环境验收、覆盖策略、趋势 diff 的关系。
4. `docs/真实环境测试与验收手册.md`：补充封板或周回归前如何生成并留存 readiness evidence。
5. `docs/21-业务功能清单与验收标准.md`：更新质量工场阶段 3 能力说明。

文档口径固定为：readiness 只证明自动化资产治理状态，不证明业务链路已经在真实环境跑通；真实验收仍必须使用 `application-dev.yml` 或覆盖该文件的环境变量。

## 12. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| 团队误把 readiness 当成业务验收通过 | 交付判断失真 | Markdown 和文档统一写明“仍需真实环境验收” |
| 自动查找 baseline 选错文件 | 趋势判断失真 | 报告记录 baseline/current 路径；封板建议显式传参 |
| 组合器复制底层算法 | 后续维护分叉 | 实现复用 coverage/diff 库函数，只新增状态聚合 |
| warning 退出码为 0 被忽略 | 告警长期积压 | Markdown Next Actions 和 JSON `status=warning` 明确暴露 |
| logs 目录历史文件不足 | 首次运行无法对比 | 默认记录 `diff.skippedReason`，允许 `--skip-diff` |

## 13. 成功标准

1. `run-acceptance-readiness.mjs` 可生成 JSON/Markdown 就绪包。
2. 默认命令能完成覆盖矩阵、策略评估和可用时的趋势对比。
3. 显式 baseline/current 可生成确定性的趋势证据。
4. 策略 error 或趋势回退会让 CLI 返回非零。
5. warning 不阻断 CLI，但报告能明确提示人工确认项。
6. 新能力有 Node 测试覆盖，并与现有覆盖矩阵、策略、diff 测试一同通过。
7. 文档同步说明 readiness 与真实环境业务验收的边界。
