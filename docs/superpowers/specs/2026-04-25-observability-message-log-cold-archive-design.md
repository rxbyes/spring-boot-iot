# F2 `iot_message_log` 冷归档设计

## 1. 背景

`iot_message_log` 已经完成“唯一物理真表”收口，并成为链路追踪、验收回放和接入排障的主证据表。`2026-04-25` 的可观测日志治理 F1 已补齐 `dry-run/apply` 入口，但当前对 `iot_message_log` 的治理仍是“直接删除过期行”。最新真实环境 `dry-run` 显示：

- `sys_observability_span_log`：`expiredRows=0`
- `sys_business_event_log`：`expiredRows=0`
- `iot_message_log`：`expiredRows=16098`

这意味着当前容量压力几乎全部集中在消息日志热表，而它又是接入排障最常用的原始证据源。F2 的目标不是改变 `iot_message_log` 的热表定位，而是在保留证据链的前提下，把过期热数据安全迁入冷归档，再执行物理删除。

## 2. 目标与非目标

### 2.1 目标

1. 为 `iot_message_log` 建立“先归档、后删除”的治理链路。
2. 为每次归档/删除动作建立可追溯的批次台账。
3. 让 `dry-run`、`apply`、归档结果和删除结果形成同一条证据链。
4. 在真实环境验证时继续以 `application-dev.yml` / 环境变量为唯一基线。

### 2.2 非目标

1. 本阶段不新增前端页面，不把冷归档数据接到 `/message-trace` 或 `/system-log`。
2. 本阶段不改变 `sys_observability_span_log`、`sys_business_event_log` 的治理方式，它们仍维持现有 delete-only 口径。
3. 本阶段不把 `iot_message_log` 从主运行链路中降级为 archived/pending_delete 对象；它仍是 active 热表。
4. 本阶段不引入对象存储、压缩包归档或跨库迁移，冷归档先固定在当前 MySQL 真相域内。

## 3. 推荐方案

采用“**MySQL 冷归档真表 + 批次台账 + 幂等 apply**”方案。

### 3.1 方案摘要

1. 新增 `iot_message_log_archive` 保存冷归档后的原始消息行。
2. 新增 `iot_message_log_archive_batch` 保存每次治理批次的确认信息、归档/删除统计和证据路径。
3. `scripts/govern-observability-logs.py` 在处理 `iot_message_log` 的 `apply` 时改为：
   - 先生成批次；
   - 再按批次分块把热表过期行复制到归档表；
   - 仅删除“已成功归档”的那部分热表行；
   - 最后回写批次状态和统计。
4. `node scripts/auto/run-observability-log-governance.mjs` 继续作为统一入口，不改变人工确认门禁，只扩展返回的批次证据。

### 3.2 选择理由

相比“只导出文件再删热表”，这个方案的优点是：

- 归档结果仍保留在受 schema 和索引约束的真相表内，后续导出、核对和审计都更稳定。
- 每次 `apply` 都有批次台账，可以回答“删了哪一批、删前 dry-run 是哪份、归档了多少、删了多少、失败在哪一步”。
- 通过 `original_log_id` 唯一约束和“只删除已归档行”的规则，可以把失败重试做成幂等。

## 4. 数据模型设计

### 4.1 新增 `iot_message_log_archive`

定位：`iot_message_log` 的冷归档真表，只承接超过热表保留期的历史消息证据。

建议字段：

- 继承原热表核心字段：
  - `tenant_id`
  - `device_id`
  - `product_id`
  - `message_type`
  - `topic`
  - `payload`
  - `report_time`
  - `trace_id`
  - `device_code`
  - `product_key`
  - `create_time`
- 新增归档治理字段：
  - `id`：归档表主键
  - `original_log_id`：原热表 `iot_message_log.id`
  - `archive_batch_id`：归档批次主键
  - `archived_at`：归档入表时间

关键约束和索引：

1. `uk_original_log_id (original_log_id)`：保证单条热表行只会归档一次。
2. `idx_archive_batch_id (archive_batch_id)`：按批次追踪。
3. `idx_trace_id_report_time (trace_id, report_time)`：支持后续按 TraceId 导出或核查。
4. `idx_device_code_report_time (device_code, report_time)`：支持按设备补查。

生命周期口径：

- `schema/` 中定义为 `active` 对象。
- 不进入业务主链路读写，但属于治理运行真相的一部分，因此继续纳入 `includedInInit=true` 与 `includedInSchemaSync=true`。

### 4.2 新增 `iot_message_log_archive_batch`

定位：每次冷归档治理的批次台账。

建议字段：

- `id`
- `batch_no`
- `source_table`：固定为 `iot_message_log`
- `governance_mode`：F2 当前固定写 `APPLY`，字段保留给后续扩展
- `status`：`RUNNING` / `SUCCEEDED` / `FAILED`
- `retention_days`
- `cutoff_at`
- `confirm_report_path`
- `confirm_report_generated_at`
- `confirmed_expired_rows`
- `candidate_rows`
- `archived_rows`
- `deleted_rows`
- `failed_reason`
- `artifacts_json`：报告路径、批次摘要等
- `create_time`
- `update_time`

关键约束和索引：

1. `uk_batch_no (batch_no)`：批次号唯一。
2. `idx_source_status_time (source_table, status, create_time)`：按来源和状态检索。

### 4.3 热表本身不改名、不降级

`iot_message_log` 仍然是设备消息日志唯一热表真相。F2 只增加“超出保留期后的去向”和“删除证据”，不改变业务代码对热表的当前依赖。

## 5. Schema Governance 设计

### 5.1 新增 `schema-governance/device-domain.json`

当前 `device` 域还没有治理 registry 文件。F2 新增该文件，并先登记一个治理对象：

- `iot_message_log`
- `governanceStage=freeze_candidate`
- `realEnvAuditProfile=mysql_hot_table_with_cold_archive`

采用 `freeze_candidate` 而不是 `archived` 的原因：

- 该对象仍是 active 热表；
- 当前只是进入容量治理与删除前置条件治理，而不是对象退场；
- 需要在域级治理台账中明确它已经进入“保留期 + 冷归档 + 删除门禁”的受控状态。

### 5.2 治理前置条件

建议登记以下删除前置条件：

1. `archive_table_ready`
2. `archive_batch_recorded`
3. `confirmed_report_matches_apply`
4. `docs_and_registry_updated`

### 5.3 治理脚本扩展

现有 `scripts/governance/run_domain_audit.py` 只支持 `mysql_archived_object_with_seed`。F2 需要扩展新的 profile：

- `mysql_hot_table_with_cold_archive`

该 profile 至少要审计：

1. 热表是否存在。
2. 冷归档表和批次台账表是否存在。
3. 热表总量、过期量、租户分布。
4. 最近批次状态、归档行数、删除行数。
5. 是否存在“批次显示已归档，但热表对应行未删”或“批次显示已删，但归档表缺失原行”的异常。

## 6. 执行链路设计

### 6.1 `dry-run`

`dry-run` 不写归档表、不写批次表，只输出计划信息。

对 `iot_message_log`，报告中新增：

- `archiveEnabled=true`
- `archiveTable=iot_message_log_archive`
- `archiveBatchTable=iot_message_log_archive_batch`
- `candidateRows`：当前过期候选数

`dry-run` 仍然只作为“计划和确认材料”，不做实际数据移动，也不写 `iot_message_log_archive_batch`。后续 `apply` 会把 `confirm_report_path`、`confirm_report_generated_at` 和 `confirmed_expired_rows` 写入批次台账，把 `dry-run` 报告和真实删除动作串起来。

### 6.2 `apply`

`apply` 只在现有人工确认门禁通过后执行。对 `iot_message_log` 的执行顺序固定为：

1. 创建批次台账，状态为 `RUNNING`。
2. 按 `deleteBatchSize` 分块查询候选热表行，固定按 `report_time ASC, id ASC` 处理。
3. 将当前块复制到 `iot_message_log_archive`。
4. 仅删除本块中“已经成功落到归档表”的那些 `original_log_id` 对应热表行。
5. 累加 `archived_rows`、`deleted_rows`，持续回写批次统计。
6. 全部完成后把批次改为 `SUCCEEDED`；如任一块失败，则把批次改为 `FAILED` 并保留已完成统计。

### 6.3 幂等与失败语义

F2 采用“**分块事务 + 唯一约束幂等**”而不是“一次大事务”。

原因：

- 需要在失败时保留批次证据，而不是整批回滚后只留下 stderr。
- 大表下单次大事务风险更高，不利于共享环境执行。

幂等保证：

1. `iot_message_log_archive.uk_original_log_id` 保证重复归档不会产生第二份冷数据。
2. 删除动作只针对“本轮已确认存在于归档表”的 `original_log_id`。
3. 失败重试时，已归档未删除的行可被下一次 `apply` 补删，不会丢失原始证据。

## 7. 脚本与接口边界

### 7.1 `scripts/govern-observability-logs.py`

需要扩展的行为：

1. 识别 `iot_message_log` 的 `archive` 配置。
2. 支持创建归档批次和归档表写入。
3. 在报告 JSON/Markdown 中输出批次结果。
4. 保持 `sys_observability_span_log`、`sys_business_event_log` 的当前删除逻辑不变。

### 7.2 `config/automation/observability-log-governance-policy.json`

为 `iot_message_log` 增加专属归档配置，例如：

- `archiveTable`
- `archiveBatchTable`
- `archiveEnabled`
- `archiveChunkSize`：未显式配置时回退到现有 `deleteBatchSize`

### 7.3 `node scripts/auto/run-observability-log-governance.mjs`

保持入口语义不变：

- 默认 `dry-run`
- `apply` 仍必须带 `--confirm-report` 与 `--confirm-expired-rows`

允许增强的输出：

- 当底层返回归档批次结果时，把 `archiveBatch` 摘要透传到 CLI 输出 JSON。

## 8. 文档与真相源更新范围

必须同步更新：

1. `schema/mysql/device-domain.json`
2. `schema-governance/device-domain.json`
3. `docs/04-数据库设计与初始化数据.md`
4. `docs/07-部署运行与配置说明.md`
5. `docs/08-变更记录与技术债清单.md`
6. `docs/11-可观测性、日志追踪与消息通知治理.md`
7. `README.md`
8. `AGENTS.md`

必须执行的生成/校验链：

1. `python scripts/schema/render_artifacts.py --write`
2. `python scripts/schema/check_schema_registry.py`
3. `python scripts/governance/render_governance_docs.py --write`
4. `python scripts/governance/check_governance_registry.py`
5. `python scripts/governance/run_domain_audit.py --domain device`

## 9. 测试与验证策略

### 9.1 自动化测试

至少补齐：

1. `scripts/tests/test_govern_observability_logs.py`
   - 验证 `iot_message_log` 在 `apply` 时会先归档后删除。
   - 验证已归档行不会重复归档。
   - 验证部分失败时批次状态为 `FAILED`。
2. `scripts/auto/observability-log-governance.test.mjs`
   - 验证 wrapper 能正确透传归档批次摘要。
3. `scripts/tests/test_governance_registry.py`
   - 验证 `device` 域治理对象登记生效。
4. `scripts/tests/test_governance_tools.py`
   - 验证新的治理 profile 能出现在治理附录与域台账中。

### 9.2 真实环境验证

本阶段只要求真实环境 `dry-run`，不直接执行 `apply`。

最小验证命令：

1. `node --test scripts/auto/observability-log-governance.test.mjs`
2. `python3 scripts/tests/test_govern_observability_logs.py`
3. `python3 scripts/tests/test_governance_registry.py`
4. `python3 scripts/tests/test_governance_tools.py`
5. `python3 scripts/schema/check_schema_registry.py`
6. `python3 scripts/governance/check_governance_registry.py`
7. `node scripts/auto/run-observability-log-governance.mjs`
8. `python3 scripts/governance/run_domain_audit.py --domain device`

## 10. 风险与回退

### 10.1 主要风险

1. 归档表和热表字段不一致，导致 `INSERT ... SELECT` 失败。
2. 批次统计与实际删除不一致，破坏证据链可信度。
3. 共享环境执行 `apply` 时，如果先删后归档，会造成证据不可恢复。

### 10.2 对策

1. 冷归档表字段结构尽量贴近热表，只增加最小治理字段。
2. 删除逻辑固定绑定“已成功归档”的原始主键集合。
3. 首轮真实环境只做 `dry-run`，不做 `apply`。

### 10.3 回退策略

若实现过程中发现批次台账或归档链复杂度显著超出预期，可保守回退到：

- 保留 `iot_message_log_archive`；
- 延后 `schema-governance` profile 扩展；
- 但仍不退回“直接删热表”的方案。

## 11. 实施顺序建议

1. 先补 `schema/` 与 `schema-governance/` 真相源。
2. 再补 Python 治理脚本和治理审计脚本。
3. 最后补 Node wrapper 和文档。

这样可以先把“数据去哪里、治理如何登记、真实库如何审计”三件事锁死，再做执行入口，避免入口先行而真相源滞后。
