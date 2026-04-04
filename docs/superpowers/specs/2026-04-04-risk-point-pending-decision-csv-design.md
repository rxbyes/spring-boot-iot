# 风险点 Pending 业务决策 CSV 执行设计

> 日期：2026-04-04
> 主题：把 `manual_review` 的业务确认结果从“人工看表 + 人工点页面”升级为“复用确认 CSV + 默认 dry-run + 显式 apply”的批量执行链路

## 1. 背景

截至 `2026-04-04`，`risk_point_device_pending_binding` 在首轮批量治理后已经形成三类对象：

1. 已按重点测点直接批量 `promote`
2. 已按非监测对象直接批量 `ignore`
3. 剩余 `17` 条 `manual_review`，当前都只出现 `gX / gY / gZ`

这 `17` 条已经具备：

1. 残余 manifest
2. 人工复核 CSV / JSON / Markdown 模板
3. 分组化 `resolution_note`
4. 业务复核口径文档

但当前还缺一段把“业务在 CSV 里确认的结论”真正执行到系统中的批量闭环。

如果继续靠人工读 CSV 后逐条打开页面点击，会导致：

1. 业务确认和系统执行分离
2. 批次结果难复盘
3. 容易把 `KEEP_PENDING`、`PROMOTE`、`IGNORE` 混做
4. 不利于重复治理同类设备

## 2. 目标

在现有 `scripts/manage-risk-point-pending-governance.py` 基础上继续补一段“决策 CSV 执行”能力，使业务可以直接复用已导出的 CSV 填写确认结果，并由脚本统一完成：

1. 读取 CSV 决策
2. 校验是否允许执行
3. 默认 dry-run 预览
4. 显式 `--apply` 时真正执行 `promote / ignore / keep pending`
5. 输出统一结果 JSON 作为留痕

## 3. 非目标

本轮不做以下事情：

1. 不新增后端批量执行接口
2. 不改变前端页面交互
3. 不让脚本自动猜测 `PROMOTE` 的正式测点
4. 不把未经业务确认的行直接推进到正式表
5. 不处理 `manual_review` 之外的 bucket

## 4. 已确认决策

1. 直接复用现有导出的 CSV 作为业务确认载体，不新造第二份文件格式。
2. 业务确认结果最少只支持三类：
   - `PROMOTE`
   - `IGNORE`
   - `KEEP_PENDING`
3. `PROMOTE` 必须显式填写 `canonical_metrics`，脚本不允许自动猜。
4. `KEEP_PENDING` 只允许更新 `resolution_note`，不改变 `resolution_status`。
5. 所有写操作默认 dry-run，只有显式 `--apply` 才落真实环境。

## 5. 方案对比

### 5.1 方案 A：业务填 CSV，脚本直接自动推断是否转正

优点：

1. 业务填写最少。

缺点：

1. 容易把本应继续等待的对象误转正。
2. 当前 `manual_review` 的核心问题就是语义不清，自动猜测会破坏治理边界。

### 5.2 方案 B：业务填 CSV，脚本只按显式决策执行

优点：

1. 业务确认和系统执行使用同一份台账。
2. `PROMOTE`、`IGNORE`、`KEEP_PENDING` 三类边界清晰。
3. 最符合当前已有 manifest + CSV + note 的治理链路。

缺点：

1. 需要业务多填一列 `canonical_metrics`。

### 5.3 方案 C：改成 JSON 决策文件

优点：

1. 程序读取最简单。

缺点：

1. 业务不方便直接维护。
2. 会和现有 CSV 模板形成双轨。

## 6. 选型

本轮采用方案 B。

## 7. 决策 CSV 设计

### 7.1 复用现有 CSV

继续复用：

- `risk-point-pending-manual-review-automation-20260404.csv`

后续脚本导出的 CSV 也统一沿用这一结构。

### 7.2 关键列

继续保留现有业务阅读列：

1. `pending_id`
2. `group`
3. `risk_point_name`
4. `device_name`
5. `device_code`
6. `product_name`
7. `candidate_metric_ids`
8. `candidate_metric_names`
9. `reason`
10. `suggested_action`
11. `system_recommendation`
12. `todo`

新增或固定用于执行的列：

1. `business_decision`
   - 允许值：`PROMOTE`、`IGNORE`、`KEEP_PENDING`
2. `canonical_metrics`
   - 仅 `PROMOTE` 必填
   - 多个测点用英文逗号分隔，例如：`gpsTotalX,gpsTotalY,gpsTotalZ`
3. `owner`
4. `due_date`
5. `notes`

### 7.3 填写规则

1. `PROMOTE`
   - 必须填写 `canonical_metrics`
   - 只能填写规范测点名，不允许填 `gX / gY / gZ` 这类当前待治理原始候选作为正式目标，除非业务已明确接受并在规范中认可
2. `IGNORE`
   - `canonical_metrics` 留空
   - `notes` 建议写明排除原因
3. `KEEP_PENDING`
   - `canonical_metrics` 留空
   - `notes` 建议写明继续等待的原因，如“等待产品归属纠正”或“等待 gpsTotalX/Y/Z 证据”

## 8. 脚本设计

### 8.1 新增子命令

在 `scripts/manage-risk-point-pending-governance.py` 中新增：

- `apply-manual-review-decisions`

### 8.2 输入参数

最小参数：

- `--csv <path>`

可选参数：

- `--manifest <path>`
  - 用于交叉校验该 CSV 是否来自当前 `manual_review` 清单
- `--pending-ids`
  - 只执行指定行
- `--limit`
  - 只执行前 N 行
- `--apply`
  - 显式写入真实环境
- `--result-output`
  - 写出 JSON 结果文件
- `--base-url` / `--login-username` / `--login-password`
  - 复用现有 promote/ignore 登录能力
- `--jdbc-url` / `--user` / `--password`
  - 复用现有 `KEEP_PENDING` 更新备注所需连接能力

### 8.3 执行规则

#### `PROMOTE`

1. 解析 `canonical_metrics`
2. 生成与现有 `promote` 相同的请求结构
3. 调用现有 `POST /api/risk-point/pending-bindings/{pendingId}/promote`
4. `completePending`
   - 默认 `true`
5. `promotionNote`
   - 固定带上“业务确认 CSV 批量执行”字样

#### `IGNORE`

1. 调用现有 `POST /api/risk-point/pending-bindings/{pendingId}/ignore`
2. `ignoreNote`
   - 优先取 `notes`
   - 没有则使用默认说明

#### `KEEP_PENDING`

1. 不调用后端写侧接口
2. 只更新 `risk_point_device_pending_binding.resolution_note`
3. 保持 `resolution_status` 原值不变
4. 备注中写入“业务确认继续保留 pending”及 `notes`

### 8.4 校验规则

脚本执行前必须校验：

1. `pending_id` 必须存在
2. `business_decision` 为空的行直接跳过
3. `business_decision` 不在允许集合时直接报错
4. `PROMOTE` 缺 `canonical_metrics` 直接报错
5. `IGNORE / KEEP_PENDING` 不得填写 `canonical_metrics`
6. 如果提供了 `--manifest`，则 `pending_id` 必须属于 manifest 的 `manual_review`

### 8.5 dry-run / apply

默认行为：

1. 不写库
2. 不调用 promote/ignore 接口
3. 只打印每条记录将如何执行

显式 `--apply` 时：

1. `PROMOTE` 调用真实 promote API
2. `IGNORE` 调用真实 ignore API
3. `KEEP_PENDING` 更新真实 `resolution_note`

## 9. 结果留痕

执行结果 JSON 至少包含：

1. `generatedAt`
2. `apply`
3. `summary`
4. `results[]`

每条结果至少包含：

1. `pendingId`
2. `businessDecision`
3. `dryRun`
4. `status`
5. `requestBody` 或 `resolutionNotePreview`
6. `error`

主状态建议：

1. `DRY_RUN`
2. `APPLIED`
3. `SKIPPED`
4. `ERROR`

## 10. 验收标准

1. 可以读取填写后的业务确认 CSV。
2. `PROMOTE / IGNORE / KEEP_PENDING` 三类都支持 dry-run。
3. `PROMOTE` 若未填 `canonical_metrics` 会被阻断。
4. `KEEP_PENDING` apply 后只更新 `resolution_note`，不改变 `resolution_status`。
5. 结果会落 JSON，便于和业务确认表对应追踪。

## 11. 风险与护栏

1. 业务填写错误风险
   - 通过严格校验和默认 dry-run 降低
2. `PROMOTE` 错把原始候选写成正式测点风险
   - 通过显式 `canonical_metrics` 限制降低
3. CSV 与 manifest 不一致风险
   - 通过可选 `--manifest` 交叉校验降低
4. 批量 apply 后难回溯风险
   - 通过结果 JSON 和现有 `risk_point_device_pending_promotion` 留痕降低

## 12. 下一步

按本设计继续进入 implementation plan，随后以 TDD 方式落地：

1. CSV 读取与校验
2. 决策执行路由
3. dry-run / apply
4. 单元测试
5. 真实环境小批量验证
