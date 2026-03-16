# 数据库设计

## 数据库名
- 业务主库：`rm_iot`
- 时序库：`iot`（TDengine，按 `application-dev.yml` 当前配置）

## 命名说明
当前消息日志采用“双命名”口径：
- 物理表：`iot_device_message_log`
- 主命名：`iot_message_log`
- 兼容方式：`sql/init.sql` 直接创建视图 `iot_message_log`（历史库可继续使用 `sql/upgrade/20260316_iot_message_log_view.sql`）

原因：
- Phase 1-3 历史代码仍直接读写 `iot_device_message_log`
- 新文档和新增能力统一按 `iot_message_log` 进行业务表述
- 通过兼容视图避免在当前真实环境基线中强行改表名导致主链路回归

## 当前核心表
### 系统域
- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_menu`
- `sys_role_menu`
- `sys_tenant`
- `sys_organization`
- `sys_region`
- `sys_dict`
- `sys_dict_item`
- `sys_notification_channel`
- `sys_audit_log`

### IoT 基础域
- `iot_product`
- `iot_product_model`
- `iot_device`
- `iot_device_property`
- `iot_message_log`
- `iot_command_record`

### 风险平台域
- `iot_alarm_record`
- `iot_event_record`
- `iot_event_work_order`
- `risk_point`
- `risk_point_device`
- `rule_definition`
- `linkage_rule`
- `emergency_plan`

## 核心表说明
### `iot_product`
产品定义表，记录产品编码、名称、协议、节点类型、数据格式等。

### `iot_product_model`
产品物模型表，记录属性、事件、服务定义。

### `iot_device`
设备实例表，记录产品、认证信息、在线状态、最近上报时间等。

### `iot_device_property`
设备最新属性表，用于快速查询设备最新值。

### `iot_message_log`
原始报文日志表（或兼容视图），用于排障、审计和验收核对。

### `iot_alarm_record`
告警记录表，记录告警主数据、状态流转、处理人、处理时间等。

### `iot_event_record`
事件主表，记录告警转事件后的处置主流程数据。

### `iot_event_work_order`
事件工单表，记录派发、接收、开始、完成、反馈等工单状态。

### `risk_point`
风险点主数据表，记录风险点编码、区域、负责人、风险等级等。

### `rule_definition`
阈值规则表，记录测点、条件表达式、持续时间、告警等级等。

### `linkage_rule`
联动规则表，记录触发条件和动作编排配置。

### `emergency_plan`
应急预案表，记录风险级别、适用场景、处置步骤等。

### `sys_menu` / `sys_role_menu` / `sys_user_role`
系统菜单、角色菜单、用户角色关系表。

说明：
- `sys_menu.meta_json` 用于承载前端一级导航、二级导航、提示文案等 UI 元数据。
- `sys_menu.type = 2` 代表按钮权限，`menu_code` 作为前端 `v-permission` 与后端授权判断的统一权限码。
- `20260316_phase4_task10_dynamic_menu_auth.sql` 会初始化五类基础角色：`业务人员`、`管理人员`、`运维人员`、`开发人员`、`超级管理人员`。

## 初始化与升级顺序
### 初始化
1. 执行 [sql/init.sql](../sql/init.sql)
2. 如需样例数据，再执行 `sql/init-data.sql`（包含 IoT 主链路、风险平台、系统管理、动态菜单与超级管理员授权的真实联调基础数据）
3. 新库场景到此即可，不必再执行 `sql/upgrade/`。

### 历史库升级到当前真实环境验收基线
仅对历史库（已在跑的旧库）执行；按顺序执行 `sql/upgrade/`：
1. `20260316_phase4_task3_risk_monitoring_schema_sync.sql`
2. `20260316_phase4_real_env_schema_alignment.sql`
3. `20260316_phase4_task10_dynamic_menu_auth.sql`
4. `20260316_iot_message_log_view.sql`

说明：
- `sql/init.sql` 已整合当前代码基线所需核心表结构，`sql/upgrade/` 的定位是历史库增量兼容，不是新库初始化必需步骤。
- `20260315` 系列一次性建表脚本已从日常维护口径移除；对应能力已合并到 `sql/init.sql`，历史兼容由 `20260316_phase4_real_env_schema_alignment.sql` 负责。
- `20260316_phase4_task3_risk_monitoring_schema_sync.sql` 用于修复共享开发库的早期 Phase 4 半升级状态：补齐 `risk_point.create_by` / `update_by`，并补建 `risk_point_device`。
- `20260316_phase4_real_env_schema_alignment.sql` 用于补齐真实库历史缺列/缺表与旧版强约束差异（含 `sys_notification_channel`、`sys_audit_log` 兼容字段，以及 `rule_code` / `plan_code` 允许 `NULL` 的兼容改造）。
- `20260316_phase4_task10_dynamic_menu_auth.sql` 用于补齐 `sys_menu.meta_json`、动态菜单树、按钮权限码以及五类默认角色授权关系；脚本已内置 `sys_menu.type/menu_type` 双字段兼容处理，可避免历史库出现 `1364 - Field 'menu_type' doesn't have a default`。
- 第 4 个脚本不会替换物理表，只会补充 `iot_message_log` 兼容视图。
- 若真实环境已存在部分 Phase 4 表，请先核对表结构再执行，避免重复建表失败；风险监测联调前至少确认 `risk_point`、`risk_point_device` 两张表与当前脚本一致。

## 一期最小闭环涉及表
- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_tenant`
- `iot_product`
- `iot_product_model`
- `iot_device`
- `iot_device_property`
- `iot_message_log`

## Phase 4 真实环境验收重点表
- `iot_alarm_record`
- `iot_event_record`
- `iot_event_work_order`
- `risk_point`
- `risk_point_device`
- `rule_definition`
- `linkage_rule`
- `emergency_plan`
- `sys_organization`
- `sys_user`
- `sys_role`
- `sys_region`
- `sys_dict`
- `sys_notification_channel`
- `sys_audit_log`

具体 SQL 核对模板见 [docs/21-business-functions-and-acceptance.md](21-business-functions-and-acceptance.md)。
