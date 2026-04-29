# G3 归档批次 dry-run / apply 对比设计

## 1. 背景

`/system-log` 当前已经具备两层能力：

1. `归档批次台账` 可以分页查看 `iot_message_log_archive_batch` 的批次真相。
2. `确认报告预览` 可以按 `batchNo` 读取仓库 `logs/observability/` 下的 dry-run 报告摘要与 Markdown 预览。

但运维仍然缺一层最关键的判断力：**这次 apply 是否真的按 dry-run 确认结果落地。**

当前工作台能分别看到：

1. 批次行里的 `confirmedExpiredRows / archivedRows / deletedRows`
2. `confirmReportPath` 指向的 dry-run 报告
3. `artifactsJson.reportJsonPath` 指向的 apply 报告

可它们还没有被统一计算成一个明确结论。运维需要自己在脑中拼接“确认多少、归档多少、删了多少、有没有剩余过期量”，这会让台账从“能看”停留在“难判断”。

G3 的目标不是新建治理平台，而是在现有 `/system-log -> 归档批次详情` 抽屉内补一层 **compare 结论视图**，让工作台直接回答：

- 这批消息是否已按确认结果执行
- 如果没有，偏差在哪
- 是证据不全，还是执行结果真的漂了

## 2. 目标与非目标

### 2.1 目标

1. 新增一个只读 compare 接口，按 `batchNo` 把 dry-run 报告、apply 报告和批次真相拼成统一结论。
2. 在 `/system-log` 的 `归档批次详情` 抽屉中新增 `批次对比` 区块，优先展示“是否匹配”的总览结论。
3. 固化四类状态：`MATCHED / DRIFTED / PARTIAL / UNAVAILABLE`，避免运维自行解释不同异常场景。
4. 保持现有 `确认报告预览` 区块作为原始证据查看入口，不与 compare 结果混淆。

### 2.2 非目标

1. 本阶段不新增独立治理路由，不拆分新的“日志治理工作台”。
2. 本阶段不新增下载、重跑、补跑、重新 apply 或手工修复动作。
3. 本阶段不修改 `iot_message_log_archive_batch`、`iot_message_log_archive` 或治理脚本的持久化结构。
4. 本阶段不做“跨多个批次的时间序列对比”，只解决单个 `batchNo` 的 dry-run / apply 对比。

## 3. 方案对比与推荐

### 3.1 方案 A：前端现算对比

做法：

1. 只基于当前批次行的 `confirmedExpiredRows / archivedRows / deletedRows` 在前端直接计算。
2. 不新增 compare 接口。
3. 仍由前端分别请求预览接口和分页接口，再拼出对比结论。

优点：

- 改动快。
- 不需要新增后端 VO。

缺点：

- 无法可靠区分“apply 报告缺失”和“执行结果有偏差”。
- 只能看批次行聚合，不能做分表结论。
- 让前端承担证据拼接逻辑，边界不稳。

### 3.2 方案 B：后端统一 compare 读侧，前端只消费结果

做法：

1. 新增 `GET /api/system/observability/message-archive-batches/compare?batchNo=...`。
2. 后端按 `batchNo` 查询批次行，再读取：
   - `confirmReportPath` 指向的 dry-run JSON
   - `artifactsJson.reportJsonPath` 指向的 apply JSON
3. 服务端返回已计算好的总览结论、分表对比和降级原因。
4. 前端仅负责展示。

优点：

- 证据拼接逻辑集中在后端，语义稳定。
- 可以自然表达 `PARTIAL / UNAVAILABLE`。
- 可直接复用 G2 已有的白名单文件读取边界。

缺点：

- 需要新增一组 compare VO 和解析逻辑。

### 3.3 方案 C：批次链路总览页

做法：

1. 不只看一个批次，而是同时展示最近一次 dry-run、对应 apply 和后续复验 dry-run。
2. 形成连续治理链路看板。

优点：

- 后续扩展空间最大。

缺点：

- 范围明显超出 G3。
- 会把当前详情抽屉拉成小型子系统。

### 3.4 推荐

采用 **方案 B**。

原因：

1. 它最符合 G3 的单点目标：让工作台回答“有没有按确认结果落地”。
2. 它延续了 G2 已经建立的后端白名单文件读取边界。
3. 它让前端保持“展示 compare 结果 + 查看原始证据”的清晰分工，而不是变成第二个计算器。

## 4. 用户体验设计

### 4.1 页面位置

`批次对比` 不新开页，直接放在现有 `/system-log` 的 `归档批次详情` 抽屉中。

抽屉结构调整为：

1. 批次摘要卡片
2. 批次结果键值区
3. **批次对比**
4. 确认报告预览
5. 附加产物

这样顺序上先回答“执行结果有没有对上”，再往下看 dry-run 原始摘要和 Markdown 文本。

### 4.2 视图形态

采用用户已确认的 **A：总览优先** 布局。

`批次对比` 区块固定分成两层：

1. **总览卡片**
   - 结论状态
   - dry-run 确认量
   - apply 归档/删除量
   - 偏差值

2. **分表对比**
   - 表名
   - dry-run 过期量
   - apply 归档量
   - apply 删除量
   - 剩余过期量
   - 行级结论

正常情况让用户一眼看到：

- `MATCHED`
- 确认 16098
- 删除 16098
- 偏差 0

异常情况才把注意力引到分表。

### 4.3 结论状态语义

G3 固定使用四类状态：

#### `MATCHED`

表示：

1. dry-run 报告可读
2. apply 报告可读
3. 总量无偏差
4. 所有关键表 `remainingExpiredRows = 0`
5. 归档型表（当前主要是 `iot_message_log`）同时满足 `applyArchivedRows = applyDeletedRows = dryRunExpiredRows`

UI 语义：

- 正向状态
- 用安静但明确的成功色
- 标题文案为“已按确认结果落地”

#### `DRIFTED`

表示证据完整，但结果存在偏差，例如：

1. `confirmedExpiredRows != applyDeletedRows`
2. `dryRunExpiredRows != applyDeletedRows`
3. `applyRemainingExpiredRows > 0`
4. `iot_message_log` 这种归档型表出现 `applyArchivedRows != applyDeletedRows`

UI 语义：

- 风险状态
- 顶部卡片直接显示偏差值
- 分表清单中高亮异常行
- 标题文案为“执行结果与确认结果存在偏差”

#### `PARTIAL`

表示部分证据可用，但无法完成完整 compare。

典型场景：

1. dry-run 报告存在
2. 批次真相存在
3. apply 报告 JSON 缺失、路径非法或无法解析
4. 因而只能用批次行聚合字段做半对比

UI 语义：

- 警示但不等价于失败
- 顶部卡片说明“已完成部分比对”
- 分表区域可为空，或仅展示批次级聚合对比

#### `UNAVAILABLE`

表示无法形成可信 compare。

典型场景：

1. 批次号不存在
2. `confirmReportPath` 缺失或越界
3. dry-run JSON 缺失或解析失败

UI 语义：

- 空态/失败态
- 说明无法形成对比，不误导用户去解读无根据的差值

### 4.4 降级规则

Compare 区块与 `确认报告预览` 区块解耦：

1. compare 不可用时，报告预览仍可单独可用。
2. 报告预览只有 JSON 摘要时，compare 仍可成立。
3. compare 成功时，不要求 Markdown 必须存在。

这样可以避免“少一个 Markdown 文件，整个对比都瘫掉”。

## 5. 后端设计

### 5.1 新增 compare 接口

新增：

- `GET /api/system/observability/message-archive-batches/compare?batchNo=<batchNo>`

接口语义：

1. 输入只有 `batchNo`
2. 不接受客户端传文件路径
3. 服务端只允许从批次真相表衍生证据文件路径

### 5.2 证据来源

当前 compare 固定依赖 3 个证据源：

1. `iot_message_log_archive_batch`
   - `confirmedExpiredRows`
   - `archivedRows`
   - `deletedRows`
   - `confirmReportPath`
   - `artifactsJson`

2. dry-run JSON
   - 路径来源：`confirmReportPath`
   - 重点字段：`summary`、`tables`

3. apply JSON
   - 路径来源：`artifactsJson.reportJsonPath`
   - 重点字段：`summary`、`tables`

如果 `artifactsJson.reportJsonPath` 不存在，但批次行本身存在 `archivedRows / deletedRows`，可以退化到 `PARTIAL`。

### 5.3 返回模型

建议新增：

- `ObservabilityMessageArchiveBatchCompareVO`
- `ObservabilityMessageArchiveBatchCompareSummaryVO`
- `ObservabilityMessageArchiveBatchCompareTableVO`
- `ObservabilityMessageArchiveBatchCompareSourceVO`

返回字段建议如下：

#### 顶层

1. `batchNo`
2. `sourceTable`
3. `status`
4. `compareStatus`
5. `compareMessage`
6. `sources`
7. `summaryCompare`
8. `tableComparisons`

#### `sources`

1. `confirmReportPath`
2. `resolvedDryRunJsonPath`
3. `resolvedApplyJsonPath`
4. `dryRunAvailable`
5. `applyAvailable`

#### `summaryCompare`

1. `confirmedExpiredRows`
2. `dryRunExpiredRows`
3. `applyArchivedRows`
4. `applyDeletedRows`
5. `remainingExpiredRows`
6. `deltaConfirmedVsDeleted`
7. `deltaDryRunVsDeleted`
8. `matched`

#### `tableComparisons[]`

1. `tableName`
2. `label`
3. `dryRunExpiredRows`
4. `applyArchivedRows`
5. `applyDeletedRows`
6. `applyRemainingExpiredRows`
7. `deltaDryRunVsDeleted`
8. `matched`
9. `reason`

### 5.4 对比算法

#### 总量算法

优先级：

1. `dryRunExpiredRows` 取 dry-run 报告顶层 `summary.expiredRows`
2. `applyArchivedRows` 取 apply 报告顶层 `summary.archivedRows`
3. `applyDeletedRows` 取 apply 报告顶层 `summary.deletedRows`
4. `remainingExpiredRows` 汇总 apply 报告各表 `remainingExpiredRows`

若 apply 报告不可用，则退化为：

1. `applyArchivedRows = batch.archivedRows`
2. `applyDeletedRows = batch.deletedRows`
3. `remainingExpiredRows = null`

#### `MATCHED` 判定

总量层要求：

1. `confirmedExpiredRows == applyDeletedRows`
2. `dryRunExpiredRows == applyDeletedRows`
3. `remainingExpiredRows == 0`

若 apply 报告存在且 `sourceTable = iot_message_log`，再额外要求：

4. `applyArchivedRows == applyDeletedRows`

#### `DRIFTED` 判定

在 dry-run、apply 都可用的前提下，只要以下任一成立即为 `DRIFTED`：

1. `confirmedExpiredRows != applyDeletedRows`
2. `dryRunExpiredRows != applyDeletedRows`
3. `remainingExpiredRows > 0`
4. 任一关键表 `matched=false`

#### `PARTIAL` 判定

满足：

1. dry-run 可用
2. 批次真相可用
3. apply 报告不可用

此时 compare 仍返回批次级摘要，但 `tableComparisons` 可为空或仅返回“无 apply 分表证据”说明。

#### `UNAVAILABLE` 判定

满足任一：

1. `batchNo` 查无批次
2. dry-run 报告路径缺失
3. dry-run 路径越界
4. dry-run JSON 缺失或解析失败

### 5.5 文件读取边界

G3 延续 G2 的白名单目录规则：

- `<repo>/logs/observability`

服务端行为：

1. 规范化 `confirmReportPath`
2. 从 `artifactsJson` 中读取 `reportJsonPath`
3. `normalize()` 后必须仍位于 `logs/observability` 内
4. 不允许前端传入路径

若 apply 路径非法或缺失：

- compare 返回 `PARTIAL`
- 不把整个请求打成 500

## 6. 前端设计

### 6.1 API client

在 `spring-boot-iot-ui/src/api/observability.ts` 新增：

- `getObservabilityMessageArchiveBatchCompare(batchNo: string)`
- compare 对应的 typed interfaces

### 6.2 `/system-log` 详情抽屉改造

在 `AuditLogView.vue` 中新增三块状态：

1. `messageArchiveBatchCompareLoading`
2. `messageArchiveBatchCompareErrorMessage`
3. `activeMessageArchiveBatchCompare`

打开批次详情时，并行加载：

1. 现有 `report-preview`
2. 新的 `compare`

展示顺序：

1. 批次摘要
2. 批次结果
3. **批次对比**
4. 确认报告预览
5. 附加产物

### 6.3 Compare 区块渲染规则

#### 总览卡片

固定展示：

1. 结论状态
2. dry-run 确认量
3. apply 归档/删除量
4. 偏差值

#### 分表区域

展示每张命中表的对比行。

视觉策略：

1. `matched=true` 的行保持中性
2. `matched=false` 的行做轻量高亮
3. 不做夸张告警色铺满整块区域

#### 降级态

1. `UNAVAILABLE`：显示原因文案，不展示对比表
2. `PARTIAL`：显示“已完成部分比对”，保留总量卡片，分表表格可为空
3. `DRIFTED`：显示偏差值和异常表

### 6.4 与现有报告预览关系

compare 和 preview 必须同时存在，但职责不同：

1. compare 回答“有没有对上”
2. preview 回答“原始证据长什么样”

前端不能把 compare 结果塞进现有 `确认报告预览` 区块里，否则语义会混。

## 7. 测试与验收

### 7.1 后端

补齐单测覆盖：

1. `MATCHED`
2. `DRIFTED`
3. `PARTIAL`
4. `UNAVAILABLE`
5. apply 路径越界时降级为 `PARTIAL`
6. dry-run 路径越界或缺失时为 `UNAVAILABLE`

重点测试文件：

- `ObservabilityEvidenceControllerTest`
- `ObservabilityEvidenceQueryServiceImplTest`

### 7.2 前端

补齐：

1. compare API query builder 测试
2. `/system-log` 抽屉中 compare 总览渲染
3. `DRIFTED` 高亮行渲染
4. `PARTIAL` 降级态渲染
5. `UNAVAILABLE` 原因文案渲染

重点文件：

- `spring-boot-iot-ui/src/__tests__/api/observability.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

### 7.3 文档

同步更新：

1. `docs/03-接口规范与接口清单.md`
2. `docs/08-变更记录与技术债清单.md`
3. `docs/11-可观测性、日志追踪与消息通知治理.md`
4. `README.md`
5. `AGENTS.md`

## 8. 风险与约束

1. 当前 apply 报告路径来自 `artifactsJson.reportJsonPath`，如果历史批次未带这个字段，compare 只能退化到 `PARTIAL`，不能假装有完整 apply 证据。
2. 本阶段不会回填历史批次缺失的 apply 报告，也不会新增离线补数脚本。
3. compare 的可信度建立在治理报告 JSON 结构稳定的前提下；若后续治理脚本调整 JSON 契约，必须同步调整 compare 解析逻辑和文档。

## 9. 结论

G3 采用“**后端统一 compare，前端总览优先展示**”的方式，在不引入新路由、不新增新真相表的前提下，把 `归档批次台账` 从“能看批次和报告”推进到“能直接判断执行是否对上确认结果”。

这一步完成后，`/system-log` 中的归档治理证据链会具备三层分工：

1. `台账`：这批做了什么
2. `对比`：这批有没有按确认结果落地
3. `预览`：原始 dry-run/apply 报告长什么样
