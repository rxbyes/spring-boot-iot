# F3 可观测归档批次治理与 schema sync 漂移收口设计

## 1. 背景

F2 已经把 `iot_message_log` 升级为唯一热表，并补齐了 `iot_message_log_archive` 与 `iot_message_log_archive_batch` 两张治理真表。共享 `dev` 环境也已经完成一次真实 `dry-run -> apply`，说明“先归档、后删除”的主链路是通的。

但当前还剩两处影响可持续运营的缺口：

1. `python3 scripts/run-real-env-schema-sync.py` 在共享环境会卡在 `sys_dict.uk_dict_code_tenant` 的历史索引漂移上。对象已经对齐，但脚本末尾仍会以 fatal 结束，不利于后续持续执行。
2. `iot_message_log_archive_batch` 目前只有脚本报告和数据库真表，还没有接入现有 `/api/system/observability/**` 读侧，运维无法直接按批次检索“哪次归档确认了什么、归档了多少、删了多少、失败在哪”。

F3 的目标，就是把这两处补齐到“可持续运维”的程度。

## 2. 目标与非目标

### 2.1 目标

1. 收口 `sys_dict.uk_dict_code_tenant` 这类“索引列顺序一致、唯一性漂移”的 schema sync 历史阻塞点。
2. 为 `iot_message_log_archive_batch` 提供最小可检索读侧，沿用现有可观测证据链 API 风格。
3. 保持真实环境 `application-dev.yml` 为唯一验收基线，不回退任何 H2 或旁路环境。

### 2.2 非目标

1. 本阶段不新增完整前端页面，只补后端分页 API 与前端 typed API client。
2. 本阶段不扩展 `iot_message_log_archive` 的原始消息查询读侧，不把冷归档数据直接接入 `/message-trace`。
3. 本阶段不做通用“任意索引自动修复器”，只收口当前已验证的、可安全判定的唯一性漂移场景。

## 3. 推荐方案

采用“**schema sync 有条件自修复 + observability 批次台账分页读侧**”方案。

### 3.1 schema sync 收口策略

对 `sys_dict.uk_dict_code_tenant` 这类历史漂移，按以下规则处理：

1. 只有当现有索引与期望索引的列序完全一致、差异仅体现在 `UNIQUE / INDEX` 唯一性时，才允许进入自动修复。
2. 如果目标是 `UNIQUE`，必须先检查对应列组合是否存在重复行；有重复则继续 fail fast，不自动猜测清洗。
3. 无重复时，脚本可安全执行 `DROP INDEX + ADD UNIQUE INDEX` 重建。
4. 如果差异不止唯一性，例如列顺序不同、列集合不同，仍维持当前 fatal 行为，不做激进修复。

这样能把共享环境当前的 `sys_dict` 历史索引漂移收掉，同时避免把“结构不一致”的高风险场景静默吞掉。

### 3.2 归档批次读侧策略

在现有 `ObservabilityEvidenceController` 下新增：

- `GET /api/system/observability/message-archive-batches/page`

查询对象固定为 `iot_message_log_archive_batch`，支持最小检索条件：

- `batchNo`
- `sourceTable`
- `status`
- `dateFrom`
- `dateTo`
- `pageNum`
- `pageSize`

返回字段以批次核账为中心，至少包括：

- `id`
- `batchNo`
- `sourceTable`
- `governanceMode`
- `status`
- `retentionDays`
- `cutoffAt`
- `confirmReportPath`
- `confirmReportGeneratedAt`
- `confirmedExpiredRows`
- `candidateRows`
- `archivedRows`
- `deletedRows`
- `failedReason`
- `artifactsJson`
- `createTime`
- `updateTime`

该接口继续要求登录态，但当前 `iot_message_log_archive_batch` 真相表本身不含 `tenant_id`，因此本阶段只作为全局治理批次台账开放，不做租户维度截断；它只承担读侧核账，不承载真实删除动作。

## 4. 设计细节

### 4.1 schema sync 自动修复边界

新增一个轻量“可修复索引漂移”判断：

1. 仅允许命中白名单对象，例如当前先覆盖 `("sys_dict", "uk_dict_code_tenant")`。
2. 仅允许“same columns, unique-kind drift”。
3. 自动修复前后都打印结构化日志，明确是：
   - `repairable drift detected`
   - `duplicate rows found, abort`
   - `index rebuilt as unique`

这样真实环境执行时能直接在脚本输出里看到到底是“自动收口成功”还是“因为重复数据仍然需要人工处理”。

### 4.2 observability 读侧落点

沿用当前证据链实现风格：

1. `controller` 新增分页入口。
2. `service interface` 新增 `pageMessageArchiveBatches(...)`。
3. `service impl` 用 `JdbcTemplate` 直接查 `iot_message_log_archive_batch`。
4. `spring-boot-iot-ui/src/api/observability.ts` 新增 query type、VO type 和 API 调用方法。

本阶段不新增页面，是因为 `/system-log` 已经够重，先把后端真相和前端 typed client 打通，更适合后续按需要接到治理台或日志台。

## 5. 验证策略

### 5.1 本地测试

1. `python3 -m unittest scripts.tests.test_run_real_env_schema_sync`
2. `mvn -pl spring-boot-iot-system -Dtest=ObservabilityEvidenceControllerTest,ObservabilityEvidenceQueryServiceImplTest test`
3. `cd spring-boot-iot-ui && npm test -- --run src/__tests__/api/observability.test.ts`

### 5.2 真实环境验证

1. 重新执行 `python3 scripts/run-real-env-schema-sync.py`，确认不再因 `sys_dict.uk_dict_code_tenant` 唯一性漂移 fatal。
2. 如共享环境存在归档批次数据，再补一次最小读侧验证，确认分页 SQL 能查到既有批次台账。

## 6. 文档影响

F3 完成后需要同步更新：

1. `README.md`
2. `AGENTS.md`
3. `docs/03-接口规范与接口清单.md`
4. `docs/08-变更记录与技术债清单.md`
5. `docs/11-可观测性、日志追踪与消息通知治理.md`

核心更新点是：

- 证据链 API 新增 `message-archive-batches/page`
- schema sync 会自动收口特定的唯一性漂移
- 运维现在可以通过后端读侧检索 `iot_message_log_archive_batch`
