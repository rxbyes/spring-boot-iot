# 风险点 Pending 批量治理脚本设计

> 日期：2026-04-04
> 主题：把 `/risk-point` 待治理积压从人工逐条点击升级为“清单驱动 + 默认 dry-run + 显式 apply”的批量治理工具

## 1. 背景

`risk_point_device_pending_binding` 当前已经具备：

- 待治理分页查询
- 推荐候选测点查询
- 单条转正 `promote`
- 单条忽略 `ignore`

但真实环境积压已经达到数百条，且当前运营判断已经形成较明确的四桶口径：

1. 可按重点测点进入人工确认后转正
2. 需要人工复核设备语义或测点语义
3. 需要继续等待运行态证据
4. 明显属于声光/爆闪/广播/视频/联动类，应进入排除池

如果继续依赖页面逐条勾选，将导致：

- 批次执行成本过高
- 操作结果难复盘、难重试
- 无法稳定复用“重点测点优先”的治理口径

## 2. 目标

- 新增一个仓库内脚本，把待治理批次收口为可复用的运维动作。
- 脚本支持基于真实 dev 环境生成治理清单 manifest。
- 脚本支持根据 manifest 输出批次摘要。
- 脚本支持对 manifest 中的 `promote_candidates` 与 `exclude_candidates` 执行批量 `promote` / `ignore`。
- 所有写操作默认 `dry-run`，只有显式 `--apply` 才调用真实接口。
- 每次执行都落盘结果 JSON，便于留痕、复盘和重试。

## 3. 非目标

- 本轮不新增后端批量治理接口。
- 本轮不改变前端“待治理转正”抽屉交互。
- 本轮不自动处理 `manual_review` 与 `need_runtime_evidence` 两桶，只输出清单和摘要。
- 本轮不替代已有 `promote` 规则；写侧仍以现有后端接口校验为准。

## 4. 已确认决策

1. 批量工具落在 `scripts/`，不引入新的后端模块。
2. 仍以 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 为真实环境基线。
3. `promote` 与 `ignore` 默认只打印计划，不落库。
4. 对重点监测家族，脚本只提交重点测点，不因为候选里出现附加字段就扩大转正口径。

## 5. 方案对比

### 5.1 方案 A：只输出待执行 SQL / curl 文本

优点：

- 实现最轻。

缺点：

- 仍需人工拷贝执行。
- 无法稳定收集执行结果。
- 不便做重试和局部筛选。

### 5.2 方案 B：脚本内置 `build-manifest / plan / promote / ignore`

优点：

- 与现有单条接口完全兼容。
- 可先生成 manifest，再基于同一份清单执行。
- 默认 `dry-run` 风险低。
- 结果可直接落 JSON 复盘。

缺点：

- 需要补一层运维脚本抽象与最小测试。

### 5.3 方案 C：后端新增批量治理接口

优点：

- 最终体验更一致。

缺点：

- 超出本轮“继续执行”的最小闭环。
- 改动面更大，还要补接口权限、事务和前端配套。

## 6. 选型

本轮采用方案 B。

## 7. 脚本设计

### 7.1 命令结构

新增脚本：

- `scripts/manage-risk-point-pending-governance.py`

支持子命令：

- `build-manifest`
  - 读取真实环境 pending 记录
  - 调用候选接口获取当前推荐测点
  - 生成 `promote_candidates / manual_review / need_runtime_evidence / exclude_candidates`
- `plan`
  - 读取 manifest 并输出批次摘要
- `promote`
  - 读取 manifest 中的 `promote_candidates`
  - 默认 dry-run，仅打印即将调用的 payload
  - `--apply` 时真实调用 `/api/risk-point/pending-bindings/{pendingId}/promote`
- `ignore`
  - 读取 manifest 中的 `exclude_candidates`
  - 默认 dry-run
  - `--apply` 时真实调用 `/api/risk-point/pending-bindings/{pendingId}/ignore`

### 7.2 manifest 结构

manifest 至少包含：

- `summary`
- `promote_candidates`
- `manual_review`
- `need_runtime_evidence`
- `exclude_candidates`

每条 `promote_candidates` 记录至少保留：

- `pendingId`
- `deviceName`
- `deviceCode`
- `riskPointName`
- `productName`
- `recommendedMetrics`
- `reason`

### 7.3 分类规则

首批规则直接复用当前已验证口径：

- 设备名称命中 `声光 / 爆闪 / 广播 / 摄像 / 监控 / 情报板 / 音柱 / 联动 / 控制器` 的 pending，进入 `exclude_candidates`
- `多维位移监测仪` 且候选稳定为 `gX / gY / gZ`，进入 `promote_candidates`
- `GNSS位移监测仪` 只要候选出现 `gpsTotalX / gpsTotalY / gpsTotalZ`，进入 `promote_candidates`，但正式推荐只保留这三项
- `中海达 监测型 倾角仪` 仅当候选稳定命中 `AZI / X / Y / Z / angle` 时进入 `promote_candidates`
- `雨量计` 命中 `temp / totalValue / value` 时，只推荐 `value / totalValue`
- `GNSS` 或 `倾角` 家族若只出现 `gX / gY / gZ`，进入 `manual_review`
- 其它空候选或未命中重点规范测点的记录进入 `need_runtime_evidence`

### 7.4 写侧安全

- `promote` 与 `ignore` 默认 dry-run
- 必须显式传 `--apply`
- 支持 `--limit`、`--pending-ids`
- 每次执行输出结果 JSON
- 后端返回失败或 `INVALID_METRIC` 时，脚本只记录结果，不自行重试或绕过校验

## 8. 验收标准

1. 可以通过脚本从真实 dev 环境生成 manifest。
2. `plan` 能输出四桶摘要，不依赖人工读原始 JSON。
3. `promote --dry-run` 能正确打印推荐测点 payload。
4. `ignore --dry-run` 能正确打印忽略 payload。
5. 针对雨量计，脚本不会把 `temp` 自动纳入正式转正请求。
6. 执行结果会落 JSON 文件，后续可复盘。
