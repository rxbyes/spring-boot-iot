# SQL Initialization Consolidation Design

**Date:** 2026-03-31
**Status:** Approved in chat, pending written-spec review
**Scope:** `sql/init.sql`、`sql/init-data.sql` 与 `sql/upgrade/*` 的基线收口治理

## 1. 背景

当前仓库中的 SQL 入口已经出现明显分裂：

1. `[sql/init.sql](../../../sql/init.sql)` 与 `[sql/init-data.sql](../../../sql/init-data.sql)` 并未完整覆盖当前真实环境默认交付基线。
2. 多个已经成为正式基线的结构和初始化数据仍散落在 `sql/upgrade/*.sql` 中，例如：
   - `iot_message_log` 兼容视图
   - `iot_device_online_session`
   - `sys_in_app_message` / `sys_help_document`
   - `sys_in_app_message_bridge_log` / `sys_in_app_message_bridge_attempt_log`
   - `iot_device_access_error_log`
   - `iot_device_invalid_report_state`
   - `iot_device_metric_latest`
   - 系统治理分页索引和产品统计索引
3. 菜单、按钮权限、站内消息、帮助文档等初始化数据同样经历了多轮增量补齐，但并未完全回收到全量初始化脚本。
4. 结果是“新环境初始化”和“历史库升级”边界不清：
   - 新环境如果只执行 `init.sql + init-data.sql`，不一定能得到完整的当前基线。
   - 老环境运维又不得不从大量升级脚本里辨认哪些是已进入正式基线、哪些只是历史修补。

本轮治理的目标不是消灭所有升级脚本，而是把**已经进入当前默认交付基线**的结构与种子统一回收到两个主入口文件里。

## 2. 目标

本轮设计目标如下：

1. 让 `[sql/init.sql](../../../sql/init.sql)` 成为当前正式 MySQL 结构的唯一全量初始化入口。
2. 让 `[sql/init-data.sql](../../../sql/init-data.sql)` 成为当前正式 MySQL 演示数据与权限种子的唯一全量初始化入口。
3. 把已经成为默认交付基线的 DDL、索引、视图和初始化数据并回 `init/init-data`。
4. 保留 `sql/upgrade` 作为“历史库迁移 + 环境专项 + 设备专项”的补充目录，而不是当前默认初始化主入口。
5. 同步更新数据库文档，明确哪些脚本已并回基线，哪些脚本必须继续保留为专项或历史迁移。

## 3. 非目标

本轮不做以下事情：

1. 不把所有 `sql/upgrade/*.sql` 一刀切删除。
2. 不把历史扩展区域种子 `[sql/upgrade/sys_region.sql](../../../sql/upgrade/sys_region.sql)` 强行并入 `init-data.sql`。
3. 不把针对单个真实设备场景的专项数据脚本并入全量初始化，例如深部位移子设备 bootstrap。
4. 不把 TDengine legacy stable DDL 写回 `sql/init.sql`。
5. 不改变应用运行时“TDengine 兼容表由 `spring-boot-iot-telemetry` 启动时自动补齐”的现有口径。
6. 不借本轮整理顺带修改业务行为、菜单语义或真实环境验收边界。

## 4. 设计原则

### 4.1 基线优先

- 只要某张表、某个索引、某个兼容视图、某组菜单/帮助文档样例已经成为当前默认交付基线，就必须直接体现在 `init.sql` 或 `init-data.sql` 中。

### 4.2 历史迁移与全量初始化分离

- `init.sql` / `init-data.sql` 负责“从零开始得到当前基线”。
- `sql/upgrade` 负责“把旧库补到当前基线”或“执行特定环境/特定设备专项脚本”。

### 4.3 环境专项不混入通用初始化

- 不能把依赖扩展字段、真实设备存在性或人工核验前置条件的脚本无条件塞进全量初始化。

### 4.4 文档与代码口径一致

- 数据库文档必须同步反映新的初始化入口，不再把“当前默认初始化仍要手工拼一串 upgrade 脚本”写成隐性前提。

## 5. 收口边界

### 5.1 并回 `sql/init.sql` 的结构范围

以下内容已经进入当前正式基线，应直接出现在 `[sql/init.sql](../../../sql/init.sql)`：

1. `iot_message_log` 兼容视图。
2. `iot_device_online_session` 表及其索引。
3. `sys_in_app_message`、`sys_in_app_message_read`、`sys_help_document` 表及相关索引。
4. `sys_in_app_message_bridge_log`、`sys_in_app_message_bridge_attempt_log` 表。
5. `iot_device_access_error_log` 表及 `contract_snapshot` 列。
6. `iot_device_invalid_report_state` 表及其唯一键、检索索引。
7. `iot_device_metric_latest` 表及其唯一键、查询索引。
8. 已成为分页/统计基线的索引，例如：
   - `sys_user.idx_user_deleted_status_create_time`
   - `sys_role.idx_role_deleted_status_create_time`
   - `sys_menu.idx_menu_deleted_parent_sort`
   - `sys_menu.idx_menu_deleted_status_sort`
   - `sys_organization.idx_org_deleted_parent_sort`
   - `sys_organization.idx_org_deleted_status_sort`
   - `sys_region.idx_region_deleted_parent_sort`
   - `sys_region.idx_region_deleted_type_sort`
   - `sys_dict.idx_dict_deleted_sort`
   - `sys_dict.idx_dict_deleted_type_sort`
   - `sys_notification_channel.idx_channel_deleted_sort`
   - `sys_notification_channel.idx_channel_deleted_type_sort`
   - `sys_audit_log.idx_trace_id`
   - `sys_audit_log.idx_device_code`
   - `iot_device.idx_device_deleted_product_stats`

### 5.2 并回 `sql/init-data.sql` 的数据范围

以下内容已经进入当前正式基线，应直接出现在 `[sql/init-data.sql](../../../sql/init-data.sql)`：

1. 五大工作台菜单基线：
   - `接入智维`
   - `风险运营`
   - `风险策略`
   - `平台治理`
   - `质量工场`
2. 产品定义中心、设备资产中心、系统内容治理等页面菜单和按钮权限。
3. 与当前正式菜单结构对应的角色菜单授权。
4. `sys_in_app_message` 的首批站内消息样例，且数据本身已经是当前标准来源口径与 `dedup_key`。
5. `sys_help_document` 的首批帮助中心样例。
6. `sys_notification_channel` 中已进入默认基线的 `webhook-default` 场景配置。
7. 其它已进入正式初始化基线的演示账号、角色、产品、设备、风险平台样例数据。

### 5.3 继续保留在 `sql/upgrade` 的脚本范围

以下脚本继续保留在 `sql/upgrade`，不并入通用初始化：

1. 历史库迁移脚本：
   - `20260316_phase4_real_env_schema_alignment.sql`
   - `20260316_phase4_task3_risk_monitoring_schema_sync.sql`
   - `20260316_phase4_task10_dynamic_menu_auth.sql`
   - `20260317_phase4_menu_button_permission_backfill.sql`
   - 以及其它针对旧菜单结构、旧字段或旧环境的幂等补丁脚本
2. 环境专项脚本：
   - `[sql/upgrade/sys_region.sql](../../../sql/upgrade/sys_region.sql)`
3. 设备专项脚本：
   - `20260320_phase4_deep_displacement_sub_devices_bootstrap.sql`
4. TDengine / 映射治理专项脚本：
   - `20260324_phase5_tdengine_mapping_gap_draft.sql`

保留原因：

- 这些脚本要么用于历史库修复，
- 要么依赖扩展 schema，
- 要么依赖真实设备存在性或人工核验前置条件，
- 因此不适合成为“新环境一键初始化”的默认组成部分。

## 6. TDengine 口径

本轮对 TDengine 相关文件的处理原则如下：

1. **并回 `init.sql` 的只包括 MySQL 侧投影或治理结构**
   - 例如 `iot_device_metric_latest`
2. **不把 TDengine legacy stable DDL 写回 `init.sql`**
   - 当前真实环境仍按环境治理口径维护 stable
3. **不改变 `iot_device_telemetry_point` 的初始化方式**
   - 继续由 `spring-boot-iot-telemetry` 在 TDengine 数据源启动时执行 `CREATE TABLE IF NOT EXISTS`
4. **继续保留映射草案脚本**
   - `20260324_phase5_tdengine_mapping_gap_draft.sql` 仍是治理草案，不属于全量初始化

## 7. 实施方案

### 7.1 `sql/init.sql`

实施时按以下方式调整：

1. 把新增正式基线表、索引、视图直接写入 `init.sql`。
2. 对于已在 `init.sql` 中存在但字段/索引尚不完整的表，直接在基线 DDL 中补齐，而不是再依赖 upgrade 里补列。
3. 保持 `DROP VIEW / DROP TABLE / CREATE TABLE / CREATE VIEW` 的全量初始化语义不变。

### 7.2 `sql/init-data.sql`

实施时按以下方式调整：

1. 直接保留当前最新菜单树和按钮权限，不再要求新环境额外执行多个菜单补丁脚本。
2. 样例数据本身要直接是最终形态，不再依赖后续“去重键回填”“来源值修复”“乱码修复”。
3. 对已进入正式基线的帮助中心和站内消息样例，直接在 `init-data.sql` 中给出最终版本。

### 7.3 `sql/upgrade`

实施时按以下方式调整：

1. 不删除历史迁移与专项脚本。
2. 文档中明确这些脚本的职责已经从“默认初始化步骤”降级为“历史库迁移/专项用途”。
3. 如确有完全失效且无历史价值的脚本，再单独评估是否归档，但不作为本轮必要动作。

## 8. 风险与控制

### 8.1 风险：基线并回后与历史迁移脚本重复

控制：

- 允许重复存在，但文档必须声明：
  - `init.sql + init-data.sql` 是新环境入口
  - `upgrade` 是旧环境迁移与专项脚本

### 8.2 风险：把专项脚本误并入全量初始化

控制：

- 明确排除 `sys_region.sql`、深部位移 bootstrap、TDengine 映射草案。

### 8.3 风险：文档仍沿用旧初始化口径

控制：

- 同步更新 `[docs/04-数据库设计与初始化数据.md](../../04-数据库设计与初始化数据.md)`。
- 必要时同步检查 `[README.md](../../../README.md)` 是否需要提及新的“初始化已收口”事实。

## 9. 验收标准

本轮治理完成后，应满足：

1. 新环境只执行 `[sql/init.sql](../../../sql/init.sql)` 与 `[sql/init-data.sql](../../../sql/init-data.sql)`，即可得到当前 MySQL 正式基线。
2. 数据库文档中能明确区分：
   - 默认初始化入口
   - 历史迁移脚本
   - 环境/设备/TDengine 专项脚本
3. `sql/upgrade` 不再承担“新环境必须顺带执行的一串默认补丁脚本”的角色。
4. 不会把 `sys_region.sql`、深部位移专项脚本或 TDengine 映射草案误写进通用初始化。
