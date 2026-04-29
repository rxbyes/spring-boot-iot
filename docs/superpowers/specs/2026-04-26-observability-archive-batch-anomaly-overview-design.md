# G4 归档批次异常总览设计

## 1. 背景

G3 已经把 `iot_message_log_archive_batch` 的单批次证据链补齐到 `/system-log`：

1. `message-archive-batches/page` 可以分页查看批次真相。
2. `message-archive-batches/report-preview` 可以只读预览确认报告。
3. `message-archive-batches/compare` 可以对单个 `batchNo` 给出 `MATCHED / DRIFTED / PARTIAL / UNAVAILABLE` 结论。

但当前工作台仍有两个明显缺口：

1. **异常批次不够容易先浮出来。** 运维必须先点进抽屉，才能知道这批是不是 `DRIFTED` 或 `PARTIAL`。
2. **缺少最近异常的整体视角。** 页面能看单批次，却不能直接回答“最近一段时间有几批异常、偏差总量多少、最近异常是哪一批”。

G4 的目标不是再造一个治理平台，而是在现有 `/system-log -> 归档批次台账` 上补一层**异常总览能力**，让运维先在列表层看见问题，再决定是否进入抽屉深挖证据。

## 2. 目标与非目标

### 2.1 目标

1. 在现有 `message-archive-batches/page` 结果中补齐轻量 compare 投影，让列表层能直接看到异常状态与偏差。
2. 新增一个只读 `overview` 接口，回答最近一段时间批次治理的异常概况。
3. 在 `/system-log` 的 `归档批次台账` 区块新增异常摘要卡、`compareStatus` 筛选和异常行高亮。
4. 保持 G3 的 `批次对比 + 确认报告预览` 抽屉分层不变，只把“是否异常”的判断前移。

### 2.2 非目标

1. 本阶段不新增新的治理路由，不拆成独立“归档批次治理页”。
2. 本阶段不新增新的数据库表，不引入 compare 结果持久化缓存。
3. 本阶段不新增下载、重跑、补跑、重新 apply 或手工修复动作。
4. 本阶段不改写现有 `compare` 与 `report-preview` 的详细语义，只扩展列表层和顶部总览层。

## 3. 方案对比与推荐

### 3.1 方案 A：前端现拼列表异常

做法：

1. 继续使用现有 `page + compare + report-preview`。
2. 页面拿到分页结果后，逐条再请求 compare。
3. 前端本地计算异常摘要与筛选。

优点：

- 不需要新增后端读侧。

缺点：

- 会形成 `N+1` 请求。
- 列表筛选不是真正的服务端筛选。
- 批次数量增加后，列表抖动和加载时延都会明显变差。

### 3.2 方案 B：只扩展 page 接口

做法：

1. 在 `message-archive-batches/page` 中给每行补轻量 compare 投影。
2. 前端顶部摘要卡直接基于当前页 5 条数据做聚合。

优点：

- 列表层问题可以解决。
- 改动范围相对小。

缺点：

- 顶部摘要只反映“当前页”，不反映“最近整体”。
- 很容易出现“这一页没问题，看起来像整体都没问题”的误导。

### 3.3 方案 C：列表投影 + 独立 overview 接口

做法：

1. 扩展 `message-archive-batches/page`，为每行补轻量 compare 投影，并支持按 compare 维度筛选。
2. 新增 `message-archive-batches/overview`，专门聚合最近时间窗口内的异常批次概况。
3. 前端列表层消费 page，顶部摘要卡消费 overview，详情抽屉继续消费已有 `compare + report-preview`。

优点：

- 列表筛选、异常高亮和顶部总览职责清晰。
- 避免前端 `N+1` 计算。
- 保持 G3 抽屉深挖路径不变，只补“列表入口判断力”。

缺点：

- 需要新增一个读侧 VO/聚合逻辑。

### 3.4 推荐

采用 **方案 C**。

原因：

1. 它最符合 G4 的单点目标：先浮出异常，再给出最近总体异常视角。
2. 它不把 G3 的抽屉逻辑打散，仍保持“列表是入口，抽屉是深挖”。
3. 它避免前端把 compare 逻辑再次做成第二套计算器。

## 4. 后端设计

### 4.1 扩展分页接口

接口继续使用：

- `GET /api/system/observability/message-archive-batches/page`

在现有查询参数基础上新增：

- `compareStatus`
- `onlyAbnormal`

语义约束：

1. `compareStatus` 允许值：`MATCHED / DRIFTED / PARTIAL / UNAVAILABLE`
2. `onlyAbnormal=true` 等价于筛出 `DRIFTED + PARTIAL + UNAVAILABLE`
3. 当前端同时传 `compareStatus=MATCHED` 与 `onlyAbnormal=true` 时，后端以 `onlyAbnormal=true` 为准，避免互相打架

在现有返回项基础上新增轻量 compare 投影：

- `compareStatus`
- `compareStatusLabel`
- `deltaConfirmedVsDeleted`
- `deltaDryRunVsDeleted`
- `remainingExpiredRows`
- `previewAvailable`
- `previewReasonCode`

这些字段的职责是让列表层直接回答：

1. 这批是否异常
2. 异常属于偏差、部分可比还是不可比
3. 偏差有多大
4. 报告预览当前是否可用

### 4.2 新增 overview 接口

新增：

- `GET /api/system/observability/message-archive-batches/overview`

查询参数建议支持：

- `sourceTable`
- `dateFrom`
- `dateTo`

返回结构固定收口为：

- `totalBatches`
- `matchedBatches`
- `driftedBatches`
- `partialBatches`
- `unavailableBatches`
- `abnormalBatches`
- `totalDeltaConfirmedVsDeleted`
- `totalRemainingExpiredRows`
- `latestAbnormalBatch`
- `latestAbnormalOccurredAt`

其中：

1. `abnormalBatches = driftedBatches + partialBatches + unavailableBatches`
2. `totalDeltaConfirmedVsDeleted` 表示窗口内所有批次的确认量与删除量偏差合计
3. `totalRemainingExpiredRows` 表示窗口内所有批次执行后剩余过期量合计
4. `latestAbnormalBatch` 用于在摘要卡中直接给出最近异常批次号

### 4.3 状态与降级规则

G4 不引入新枚举，继续沿用 G3：

- `MATCHED`
- `DRIFTED`
- `PARTIAL`
- `UNAVAILABLE`

列表投影与 overview 聚合的状态判定规则如下：

#### `MATCHED`

表示：

1. 当前批次可完成完整 compare
2. `dry-run` 与 `apply` 结果一致
3. `remainingExpiredRows = 0`

#### `DRIFTED`

表示证据完整，但执行结果有偏差，例如：

1. `confirmedExpiredRows != applyDeletedRows`
2. `dryRunExpiredRows != applyDeletedRows`
3. `remainingExpiredRows > 0`
4. 归档型表出现 `applyArchivedRows != applyDeletedRows`

#### `PARTIAL`

表示：

1. 批次行存在
2. `dry-run` 报告可读
3. `apply` 报告缺失、路径非法或无法解析
4. 因而只能做半对比

#### `UNAVAILABLE`

表示：

1. 无法形成可信 compare 投影
2. 例如 `confirmReportPath` 缺失、越界、JSON 缺失或解析失败

### 4.4 计算边界

G4 不新增持久化 compare cache，因此服务端仍基于现有真相推导：

1. `iot_message_log_archive_batch`
2. `confirmReportPath`
3. `artifactsJson.reportJsonPath`

边界固定如下：

1. 文件读取仍然只允许仓库 `logs/observability/` 白名单目录
2. 不允许客户端传任意文件路径
3. 若某批次无法快速形成轻量投影，直接降级为 `UNAVAILABLE`
4. `overview` 只聚合当前窗口内已有批次，不做“自动发现最近 dry-run / apply 链”

## 5. 前端设计

### 5.1 页面位置

不新增新页，继续放在 `/system-log` 的 `归档批次台账` 区块里。

### 5.2 顶部摘要卡

在现有台账区域顶部新增一排轻量摘要卡，建议 4 张：

1. `异常批次数`
2. `执行偏差总量`
3. `剩余过期总量`
4. `最近异常批次`

职责是先回答：

1. 最近有没有异常
2. 异常规模大不大
3. 最近异常落在哪一批

这排卡只消费 `overview` 接口，不从当前页 5 条列表数据临时拼装。

### 5.3 列表筛选

在现有 `批次号 / 状态 / 创建日期` 基础上新增：

1. `对比结论` 下拉
2. `仅看异常` 开关

推荐交互：

1. `仅看异常` 打开后，优先使用 `onlyAbnormal=true`
2. 当 `仅看异常` 打开时，如果下拉仍停在 `MATCHED`，前端应自动重置为 `全部`

### 5.4 列表展示

列表中新增或前移以下信息：

1. `对比结论`
2. `确认差值`
3. `剩余过期`
4. `报告预览`

异常行高亮策略：

1. `MATCHED` 保持中性
2. `DRIFTED` 最强高亮
3. `PARTIAL` 次一级提示
4. `UNAVAILABLE` 明确提示不可忽略，但弱于 `DRIFTED`

G4 不在列表层展示完整 compare 细节；详细原因仍进入抽屉看。

### 5.5 抽屉保持 G3 分层

详情抽屉继续保持：

1. `批次对比`
2. `确认报告预览`

G4 不重做抽屉，只保证：

1. 列表层浮出的异常状态与抽屉 compare 结论一致
2. 行内“报告预览可用性”与抽屉中的 preview 降级状态一致

## 6. 数据与接口契约

### 6.1 `page` 返回新增字段

新增字段全部定位为只读投影：

- `compareStatus`
- `compareStatusLabel`
- `deltaConfirmedVsDeleted`
- `deltaDryRunVsDeleted`
- `remainingExpiredRows`
- `previewAvailable`
- `previewReasonCode`

前端不得把这些字段当作可写治理真相，只用于筛选、排序和展示。

### 6.2 `overview` 返回契约

`overview` 返回的聚合值全部是时间窗口内只读统计：

1. 不回传每个批次的完整 compare 细节
2. 不替代 `compare` 接口
3. 只用于顶部摘要卡和异常概览提示

## 7. 测试与验证

### 7.1 后端

至少补齐以下单测：

1. `page` 支持 `compareStatus` 筛选
2. `page` 支持 `onlyAbnormal=true`
3. `onlyAbnormal=true` 会正确收口为 `DRIFTED + PARTIAL + UNAVAILABLE`
4. `overview` 能正确聚合 `matched / drifted / partial / unavailable`
5. 不可投影批次会计入 `unavailableBatches`，不会误算成 `matched`

### 7.2 前端

至少补齐以下回归：

1. `/system-log` 展示顶部异常摘要卡
2. `对比结论` 筛选生效
3. `仅看异常` 开关生效
4. `DRIFTED / PARTIAL / UNAVAILABLE` 行高亮正确
5. 现有 G3 抽屉仍可正常打开 compare 与 report preview

### 7.3 验证命令

实施后至少定向验证：

1. `mvn -pl spring-boot-iot-system -DskipTests=false -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
2. `npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/api/observability.test.ts src/__tests__/views/AuditLogView.test.ts`
3. `git diff --check`

## 8. 文档影响

本阶段需要同步更新：

1. `README.md`
2. `AGENTS.md`
3. `docs/03-接口规范与接口清单.md`
4. `docs/08-变更记录与技术债清单.md`
5. `docs/11-可观测性、日志追踪与消息通知治理.md`

不新增平行文档，不拆第二份 README。

## 9. 风险与取舍

1. 由于 G4 不引入 compare cache，`page` 与 `overview` 仍需要基于现有批次真相和报告路径做轻量推导，因此必须严格控制只读边界与异常降级。
2. `overview` 解决的是“最近整体异常感知”，不是完整时间序列分析；如果后续需要趋势化治理看板，应另起后续阶段。
3. `onlyAbnormal` 与 `compareStatus` 的优先级必须在前后端都保持一致，否则会出现“筛选看起来开了，但结果不对”的体验偏差。
