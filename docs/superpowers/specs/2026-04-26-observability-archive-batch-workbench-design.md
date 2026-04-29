# G1 可观测归档批次运营入口设计

## 1. 背景

F3 已经补齐了 `GET /api/system/observability/message-archive-batches/page`，共享 `dev` 环境里也已经存在真实批次数据，例如 `iot_message_log-20260426000119`，可用于核对 `confirmedExpiredRows / archivedRows / deletedRows`。

但当前这条证据链仍然偏后端视角：

1. 运维要么翻治理脚本报告，要么直接查 `iot_message_log_archive_batch`。
2. `/system-log` 虽然已经能查看 `调度任务台账`、`性能慢点 Top` 和 `TraceId 证据包`，但还看不到消息热表的归档治理结果。

G1 的目标，就是把“消息日志冷归档的治理批次”接到现有异常观测工作台，让排障和治理核账留在同一个运营入口里完成。

## 2. 目标与非目标

### 2.1 目标

1. 在 `/system-log` 增加 `归档批次台账` 面板，默认展示最近批次。
2. 让运维能直接查看批次号、确认行数、归档行数、删除行数、确认报告路径和失败原因。
3. 保持当前 `/system-log` 的工作台语法，不新增第二个治理页，不打断已有 `调度任务台账 / 性能慢点 / 证据抽屉` 的节奏。

### 2.2 非目标

1. 本阶段不新增后端接口，继续复用 F3 已落地的 `message-archive-batches/page`。
2. 本阶段不直接展示 `iot_message_log_archive` 原始消息内容，不把冷归档数据接进 `/message-trace`。
3. 本阶段不把 `/automation-governance` 改造成日志治理主入口。

## 3. 推荐方案

采用“**在 `/system-log` 内新增归档批次台账区块 + 同页详情抽屉**”方案。

### 3.1 落点选择

优先落在 `/system-log`，不先落到 `/automation-governance`，原因是：

1. `/system-log` 已经承接后台异常核对和证据包复盘，是一线排障入口。
2. 调度任务、慢点热点、归档批次本质上都属于“可检索的系统证据面板”，放在同一页更符合运维判断路径。
3. `/automation-governance` 当前更偏测试资产、执行配置和结果编排，不适合作为日志治理的第一接触面。

### 3.2 交互形态

新增第三块系统模式面板：

- 标题：`归档批次台账`
- 数据来源：`pageObservabilityMessageArchiveBatches({ sourceTable: 'iot_message_log', pageNum: 1, pageSize: 5 })`
- 单卡展示：
  - `batchNo`
  - `status`
  - `sourceTable`
  - `retentionDays`
  - `cutoffAt`
  - `confirmedExpiredRows / candidateRows / archivedRows / deletedRows`
  - `confirmReportPath` 或 `failedReason`
- 行内动作：`详情`

点击 `详情` 后，在同页打开 `StandardDetailDrawer`，展示：

1. 批次摘要卡：批次号、状态、确认行数、删除行数。
2. 批次结果：来源表、治理模式、保留期、候选/归档/删除统计。
3. 确认报告：报告路径、报告生成时间、截止时间、失败原因。
4. 附加产物：从 `artifactsJson` 中解析的报告路径或其它批次产物。

## 4. 设计约束

1. 继续沿用 `AuditLogView.vue` 现有结构，不新增路由。
2. 继续用 `StandardDetailDrawer`，不另造专用抽屉组件。
3. 刷新时机与现有系统面板保持一致：挂载、系统模式切换、系统筛选联动和点击 `刷新列表` 时一并刷新。
4. 批次详情只读取分页结果已有字段，不新增“点详情再发第二个接口”的路径。

## 5. 验证策略

1. `AuditLogView.test.ts` 补齐：
   - 归档批次台账默认请求断言
   - 面板渲染断言
   - 详情抽屉展示确认报告与附加产物断言
2. 运行：
   - `spring-boot-iot-ui/node_modules/.bin/vitest --run src/__tests__/views/AuditLogView.test.ts`
3. 若需要，再补一轮页面源码守卫，确认仍使用共享 `StandardDetailDrawer` 和列表骨架。

## 6. 文档影响

G1 完成后需同步更新：

1. `README.md`
2. `AGENTS.md`
3. `docs/03-接口规范与接口清单.md`
4. `docs/08-变更记录与技术债清单.md`
5. `docs/11-可观测性、日志追踪与消息通知治理.md`

核心变更点：

- `/system-log` 新增 `归档批次台账`
- 支持同页查看 `confirmReportPath / archivedRows / deletedRows / failedReason / artifactsJson`
- 归档批次台账正式成为异常观测工作台的一部分
