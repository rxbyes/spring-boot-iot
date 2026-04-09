# 治理对象模型与控制面升级设计

> 日期：2026-04-08
> 状态：2026-04-09 已补充字段级对象边界、模块归属、服务边界、状态机与事件清单，待用户评审 spec 后进入实施计划
> 适用范围：`spring-boot-iot-device`、`spring-boot-iot-alarm`、`spring-boot-iot-system`、`spring-boot-iot-protocol`、`spring-boot-iot-admin`、`spring-boot-iot-ui`
> 目标：在不破坏 `spring-boot-iot` 现有模块化单体、真实环境验收与 `iot_product_model` 合同真相表基线的前提下，把“语义契约治理域 + 风险运营闭环域”进一步收口为可持续演进的对象模型、桥层模型与控制面模型。

## 1. 背景

仓库截至 2026-04-08 已经具备以下稳定事实：

1. 设备接入主链路固定为 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE`。
2. `iot_product_model` 继续作为正式合同真相表，同时服务运行链路和 `/products` 设计器。
3. `iot_normative_metric_definition`、`iot_vendor_metric_evidence`、`iot_product_contract_release_batch`、`iot_product_contract_release_snapshot` 已经形成语义治理旁路支撑表。
4. `risk_metric_catalog` 已经形成“正式合同 -> 风险闭环”之间的最小桥层。
5. `sys_governance_approval_order`、`sys_governance_approval_transition`、`sys_audit_log` 已经形成审批与审计的控制面基础。
6. 首页管理视角已开始展示六类治理任务与覆盖率，但多数任务仍停留在聚合提示，不是正式任务对象。

当前最核心的问题不再是“缺少一个页面”或“缺少一个接口”，而是对象边界仍然不够清晰：

1. `iot_vendor_metric_evidence` 同时在承担证据、建议和半规则化职责，但没有正式“映射规则对象”。
2. `risk_metric_catalog` 已经存在，但下游覆盖率仍有部分依赖语义匹配或兼容字段，而不是显式绑定。
3. 控制面已经有审批、回滚、影响分析、复盘接口，但缺少统一任务对象和统一运维对象。
4. 扩展模型已经有局部 SPI，但还没有统一的协议族、解密、归一和指标生成对象。

## 2. 已确认设计约束

本轮设计必须遵守以下约束：

1. 项目继续保持模块化单体，唯一启动模块仍为 `spring-boot-iot-admin`。
2. 不引入 H2 替代链路，不回退真实环境验收口径。
3. `iot_product_model` 继续作为正式合同真相表，不新增平行合同主表。
4. 风险闭环后续默认消费 `risk_metric_catalog`，不再直接把 `iot_product_model` 当作风险入口。
5. 当前最小治理场景仍以裂缝和 GNSS 为主，但对象设计必须为后续扩展留出边界。
6. 控制面与桥层对象优先做成平台对象，不允许继续散落在页面提示、服务内部条件分支和临时聚合逻辑里。

## 3. 推荐方案

本轮采用“保留现有正式合同真相表，补齐桥层对象、控制面对象和扩展对象”的渐进升级方案。

该方案的核心原则如下：

1. 正式合同真相只在 `iot_product_model`。
2. 风险闭环入口只在 `risk_metric_catalog`。
3. 字段证据对象与映射规则对象必须分离。
4. 审批、回滚、任务、复盘必须进入控制面对象，不再散落在 UI 文案和服务内部状态机里。
5. 覆盖率、驾驶舱 KPI、卡点分布尽量继续实时聚合，不额外引入统计真相表。

## 4. 总体对象模型

### 4.1 语义契约治理域

语义契约治理域由四类对象构成：

1. 规范字段对象
2. 厂商字段证据对象
3. 厂商映射规则对象
4. 正式合同与合同发布对象

### 4.2 风险运营闭环域

风险运营闭环域由四类对象构成：

1. 风险指标对象
2. 风险点绑定对象
3. 阈值策略对象
4. 联动编排与应急预案对象

### 4.3 控制面

控制面由三类对象构成：

1. 审批与审计对象
2. 治理任务对象
3. 复盘与运维对象

### 4.4 运行底座与扩展模型

运行底座继续承接接入、解密、解码、归一、证据沉淀和 trace；扩展模型负责把协议特例、关系归一和风险指标生成从 if/else 中抽离。

## 5. 对象级定义

### 5.1 规范字段对象

定位：平台对“标准语义”的最高真相源。

表归属：

- 继续复用 `iot_normative_metric_definition`

对象职责：

1. 统一定义规范字段标识、展示名和场景归属。
2. 定义单位、量纲、方向性、阈值类型和默认语义能力。
3. 回答该字段是否允许进入风险闭环、趋势分析、GIS、对象洞察和运营分析。

建议字段边界：

1. 保留现有：`scenario_code`、`device_family`、`identifier`、`display_name`、`unit`、`precision_digits`、`monitor_content_code`、`monitor_type_code`、`risk_enabled`、`trend_enabled`、`metadata_json`
2. 增量补强：`metric_dimension`、`threshold_type`、`semantic_direction`、`gis_enabled`、`insight_enabled`、`analytics_enabled`、`status`、`version_no`

设计说明：

- `metadata_json` 只保留不稳定、低频查询的扩展语义。
- 一旦规范字段进入正式治理，不允许直接通过厂商字段名替代其 `identifier`。

### 5.2 厂商字段证据对象

定位：统一沉淀“真实世界里厂商到底报了什么”。

表归属：

- 继续复用 `iot_vendor_metric_evidence`

对象职责：

1. 记录手动样本治理与真实上报中出现过的原始字段。
2. 记录该字段出现的逻辑通道、父子设备上下文和样例值。
3. 给治理人员提供规范建议，但不直接决定正式映射规则。

建议字段边界：

1. 保留现有：`product_id`、`parent_device_code`、`child_device_code`、`raw_identifier`、`canonical_identifier`、`logical_channel_code`、`evidence_origin`、`sample_value`、`value_type`、`evidence_count`、`last_seen_time`、`metadata_json`
2. 增量补强：`source_trace_id`、`source_message_id`、`confidence_score`

设计说明：

- 该对象是证据对象，不是规则对象。
- 不允许把“是否正式映射”只记在 `canonical_identifier` 上而不形成可复用规则。

### 5.3 厂商映射规则对象

定位：把证据层观察到的厂商字段稳定映射为规范字段的正式规则对象。

表归属：

- 新增 `iot_vendor_metric_mapping_rule`

对象职责：

1. 定义在某个产品、协议族或场景下，原始字段如何映射到规范字段。
2. 保存逻辑通道条件、父子关系条件和清洗/转换规则。
3. 承接审批、版本、启停和回滚能力。

建议字段：

1. `id`、`tenant_id`
2. `scope_type`：`PRODUCT / PROTOCOL / SCENARIO`
3. `product_id`
4. `protocol_code`
5. `scenario_code`
6. `device_family`
7. `raw_identifier`
8. `logical_channel_code`
9. `relation_condition_json`
10. `normalization_rule_json`
11. `target_normative_identifier`
12. `status`：`DRAFT / ACTIVE / INACTIVE`
13. `version_no`
14. `approval_order_id`
15. 标准审计字段

设计说明：

- 后续接入新厂商时，优先补映射规则对象，而不是继续在 `LegacyDp*` 或业务服务里加判断。

### 5.4 正式合同对象

定位：产品级正式合同的当前生效真相。

表归属：

- 继续复用 `iot_product_model`

对象职责：

1. 存放正式生效的合同字段。
2. 为设备合同匹配、latest、telemetry、消息消费提供稳定入口。
3. 保持与 `/products` 设计器的现有工作模式兼容。

设计说明：

- `iot_product_model` 不再承载规范字段库和厂商证据仓职责。
- 风险平台不再默认直接消费全部合同字段。

### 5.5 合同发布对象

定位：正式合同的版本头与快照对象。

表归属：

- 继续复用 `iot_product_contract_release_batch`
- 继续复用 `iot_product_contract_release_snapshot`

对象职责：

1. 表达一次正式发布。
2. 承接审批后执行、回滚、影响分析和版本对比。
3. 作为风险指标目录的来源批次。

建议字段边界：

1. `iot_product_contract_release_batch` 保留现有：`product_id`、`scenario_code`、`release_source`、`released_field_count`、`create_by`、`create_time`、`rollback_by`、`rollback_time`
2. 增量补强：`approval_order_id`、`release_reason`、`release_status`
3. `iot_product_contract_release_snapshot` 继续保持 `BEFORE_APPLY / AFTER_APPLY` 快照口径

### 5.6 风险指标对象

定位：正式合同与风险闭环之间的桥层真相对象。

表归属：

- 继续复用 `risk_metric_catalog`

对象职责：

1. 只发布允许进入风险闭环的正式合同字段。
2. 承接单位、量纲、阈值类型、方向性和展示能力。
3. 作为风险点绑定、阈值策略、联动预案、监测、洞察和运营分析的统一入口。

建议字段边界：

1. 保留现有：`product_id`、`product_model_id`、`contract_identifier`、`risk_metric_code`、`risk_metric_name`、`source_scenario_code`、`metric_unit`、`metric_dimension`、`threshold_type`、`semantic_direction`、`threshold_direction`、`trend_enabled`、`gis_enabled`、`insight_enabled`、`analytics_enabled`、`enabled`
2. 增量补强：`release_batch_id`、`normative_identifier`、`risk_category`、`metric_role`、`lifecycle_status`

设计说明：

- 风险域后续必须优先通过 `risk_metric_id` 连接。
- 不建议在该表长期保存 `binding_status / policy_status / linkage_status / plan_status` 等覆盖率结果，避免形成第二份统计真相。

### 5.7 风险点绑定对象

定位：风险指标在具体风险点和设备上的正式绑定对象。

表归属：

- 继续复用 `risk_point_device`
- 继续复用 `risk_point_device_pending_binding`
- 继续复用 `risk_point_device_pending_promotion`

对象职责：

1. 以 `risk_metric_id` 为主关联键形成正式绑定。
2. 保留 pending 表承接待治理与转正审计。
3. 禁止在正式表写入占位测点。

设计说明：

- `metric_identifier` 继续保留兼容，但后续优先级降为回退口径。

### 5.8 阈值策略对象

定位：风险指标的阈值决策对象。

表归属：

- 继续复用 `rule_definition`

对象职责：

1. 通过 `risk_metric_id` 优先绑定正式风险指标。
2. 定义表达式、持续时间、告警等级和事件转换规则。

设计说明：

- 该对象已经基本具备桥层适配能力，本轮只做口径收口，不做大规模拆表。

### 5.9 联动编排对象与应急预案对象

定位：风险指标的处置编排和资源对象。

表归属：

- 继续复用 `linkage_rule`
- 继续复用 `emergency_plan`
- 新增 `risk_metric_linkage_binding`
- 新增 `risk_metric_emergency_plan_binding`

对象职责：

1. 用显式绑定替代当前部分依赖语义匹配的覆盖率计算。
2. 让“联动已覆盖”“预案已覆盖”成为正式关系，而不是聚合猜测。

设计说明：

- 如果继续只依赖 `trigger_condition` 或文本语义推断，后续经营驾驶舱和影响分析会越来越不稳定。

### 5.10 审批与审计对象

定位：控制面的基础工作流对象。

表归属：

- 继续复用 `sys_governance_approval_order`
- 继续复用 `sys_governance_approval_transition`
- 继续复用 `sys_audit_log`

对象职责：

1. 承接关键写操作审批。
2. 记录状态迁移、执行结果和失败原因。
3. 为发布、回滚、密钥治理、策略变更提供统一审计入口。

### 5.11 治理工作项对象

定位：把“工作提示”升级为正式任务对象。

表归属：

- 新增 `iot_governance_work_item`

对象职责：

1. 承接六类固定任务：`待治理产品 / 待发布合同 / 待绑定风险点 / 待补阈值策略 / 待补联动预案 / 待运营复盘`
2. 记录任务状态、责任人、阻塞原因、上下文快照和关闭时间。
3. 作为首页驾驶舱、对象工作台和控制面任务中心的统一数据源。

设计说明：

- `missing-bindings`、`missing-policies`、`dashboard-overview` 等接口后续可以继续保留读侧兼容，但底层应逐步切换到正式任务对象。
- 该对象是控制面工作项，不等同于站内信、审批单或事件工单；审批单只负责关键写动作复核，工作项负责持续跟踪“还有什么没收口”。

### 5.12 复盘与运维对象

定位：控制面中的运维可操作对象。

表归属：

- 新增 `iot_governance_ops_alert`
- 当前阶段 `replay` 继续使用读侧聚合 API
- 如后续需要复盘派单和结案，可新增 `iot_governance_replay_case`

对象职责：

1. 承接字段漂移、合同差异、风险指标缺失等治理告警。
2. 为告警提供认领、抑制、升级、关闭和恢复生命周期。
3. 承接基于 `traceId / deviceCode / productKey / releaseBatchId` 的复盘入口。

设计说明：

- 告警本身需要正式生命周期，否则越配置化越难运维；因此运维告警对象优先级提升为与治理工作项同级。
- `replay` 先保持按条件实时聚合，不在本轮强制新增复盘案件主表。

## 6. 数据模型增量方案

### 6.1 建议新增表

1. `iot_vendor_metric_mapping_rule`
2. `risk_metric_linkage_binding`
3. `risk_metric_emergency_plan_binding`
4. `iot_governance_work_item`
5. `iot_governance_ops_alert`

### 6.2 建议补强现表

1. `iot_normative_metric_definition`
2. `iot_vendor_metric_evidence`
3. `iot_product_contract_release_batch`
4. `risk_metric_catalog`

### 6.3 暂不新增统计真相表

以下信息继续通过聚合查询得到：

1. 合同覆盖率
2. 风险绑定覆盖率
3. 阈值覆盖率
4. 联动覆盖率
5. 预案覆盖率
6. 管理驾驶舱 KPI
7. 卡点分布

## 7. 最小 DDL 草案

### 7.1 规范字段对象补强

```sql
ALTER TABLE iot_normative_metric_definition
    ADD COLUMN metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '量纲',
    ADD COLUMN threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
    ADD COLUMN semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
    ADD COLUMN gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持GIS',
    ADD COLUMN insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持对象洞察',
    ADD COLUMN analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持运营分析',
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '版本号';
```

### 7.2 厂商映射规则对象

```sql
CREATE TABLE iot_vendor_metric_mapping_rule (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    scope_type VARCHAR(32) NOT NULL COMMENT 'PRODUCT/PROTOCOL/SCENARIO',
    product_id BIGINT DEFAULT NULL,
    protocol_code VARCHAR(64) DEFAULT NULL,
    scenario_code VARCHAR(64) DEFAULT NULL,
    device_family VARCHAR(64) DEFAULT NULL,
    raw_identifier VARCHAR(128) NOT NULL,
    logical_channel_code VARCHAR(64) DEFAULT NULL,
    relation_condition_json JSON DEFAULT NULL,
    normalization_rule_json JSON DEFAULT NULL,
    target_normative_identifier VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    version_no INT NOT NULL DEFAULT 1,
    approval_order_id BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段映射规则表';
```

### 7.3 合同发布对象与风险指标对象补强

```sql
ALTER TABLE iot_product_contract_release_batch
    ADD COLUMN approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
    ADD COLUMN release_reason VARCHAR(500) DEFAULT NULL COMMENT '发布说明',
    ADD COLUMN release_status VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT 'RELEASED/ROLLED_BACK';

ALTER TABLE risk_metric_catalog
    ADD COLUMN release_batch_id BIGINT DEFAULT NULL COMMENT '来源发布批次',
    ADD COLUMN normative_identifier VARCHAR(64) DEFAULT NULL COMMENT '来源规范字段标识',
    ADD COLUMN risk_category VARCHAR(64) DEFAULT NULL COMMENT '风险指标类别',
    ADD COLUMN metric_role VARCHAR(32) DEFAULT NULL COMMENT 'PRIMARY/DERIVED/STATE',
    ADD COLUMN lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RETIRED';
```

### 7.4 联动与预案显式绑定对象

```sql
CREATE TABLE risk_metric_linkage_binding (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    risk_metric_id BIGINT NOT NULL,
    linkage_rule_id BIGINT NOT NULL,
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与联动规则绑定表';

CREATE TABLE risk_metric_emergency_plan_binding (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    risk_metric_id BIGINT NOT NULL,
    emergency_plan_id BIGINT NOT NULL,
    binding_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标与应急预案绑定表';
```

### 7.5 治理工作项对象

```sql
CREATE TABLE iot_governance_work_item (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    work_item_code VARCHAR(64) NOT NULL COMMENT 'PENDING_PRODUCT_GOVERNANCE等',
    subject_type VARCHAR(64) NOT NULL COMMENT 'PRODUCT/RISK_METRIC/RELEASE_BATCH/REPLAY_CASE',
    subject_id BIGINT NOT NULL,
    product_id BIGINT DEFAULT NULL,
    risk_metric_id BIGINT DEFAULT NULL,
    release_batch_id BIGINT DEFAULT NULL,
    approval_order_id BIGINT DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    device_code VARCHAR(64) DEFAULT NULL,
    product_key VARCHAR(64) DEFAULT NULL,
    work_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKED/BLOCKED/RESOLVED/CLOSED/CANCELLED',
    priority_level VARCHAR(16) NOT NULL DEFAULT 'P2' COMMENT 'P1/P2/P3',
    assignee_user_id BIGINT DEFAULT NULL,
    source_stage VARCHAR(64) DEFAULT NULL,
    blocking_reason VARCHAR(255) DEFAULT NULL,
    snapshot_json JSON DEFAULT NULL,
    due_time DATETIME DEFAULT NULL,
    resolved_time DATETIME DEFAULT NULL,
    closed_time DATETIME DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理与运营工作项表';
```

### 7.6 运维告警对象

```sql
CREATE TABLE iot_governance_ops_alert (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    alert_type VARCHAR(64) NOT NULL COMMENT 'FIELD_DRIFT/CONTRACT_DIFF/MISSING_RISK_METRIC等',
    alert_code VARCHAR(128) NOT NULL COMMENT '去重键或业务编码',
    subject_type VARCHAR(64) NOT NULL COMMENT 'PRODUCT/RISK_METRIC/RELEASE_BATCH/DEVICE',
    subject_id BIGINT DEFAULT NULL,
    product_id BIGINT DEFAULT NULL,
    risk_metric_id BIGINT DEFAULT NULL,
    release_batch_id BIGINT DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    device_code VARCHAR(64) DEFAULT NULL,
    product_key VARCHAR(64) DEFAULT NULL,
    alert_status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKED/SUPPRESSED/RESOLVED/CLOSED',
    severity_level VARCHAR(16) NOT NULL DEFAULT 'WARN' COMMENT 'INFO/WARN/CRITICAL',
    affected_count BIGINT NOT NULL DEFAULT 0,
    alert_title VARCHAR(255) NOT NULL,
    alert_message VARCHAR(1000) DEFAULT NULL,
    dimension_key VARCHAR(128) DEFAULT NULL,
    dimension_label VARCHAR(255) DEFAULT NULL,
    source_stage VARCHAR(64) DEFAULT NULL,
    snapshot_json JSON DEFAULT NULL,
    assignee_user_id BIGINT DEFAULT NULL,
    first_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_time DATETIME DEFAULT NULL,
    closed_time DATETIME DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='治理运维告警表';
```

## 8. API 归口建议

### 8.1 合同治理

目标归口：

- `/api/governance/contracts/**`

兼容原则：

- 现有 `/api/device/product/{productId}/model-governance/**` 继续保留兼容入口
- 读写文档逐步切换到“合同治理”语义，而不是“设计器 apply”的页面语义

### 8.2 审批

目标归口：

- `/api/governance/approvals/**`

兼容原则：

- 现有 `/api/system/governance-approval/**` 保留兼容

### 8.3 风险指标

目标归口：

- `/api/risk-metrics/**`

兼容原则：

- 现有 `/api/risk-governance/metric-catalogs*` 保留只读兼容

### 8.4 治理任务

目标归口：

- `/api/governance/tasks/**`

兼容原则：

- 现有 `missing-bindings`、`missing-policies`、`dashboard-overview` 逐步切换到底层任务对象

### 8.5 运维告警与复盘

目标归口：

- `/api/governance/alerts/**`
- `/api/governance/replays/**`

兼容原则：

- 现有 `/api/risk-governance/ops-alerts`
- 现有 `/api/risk-governance/replay`

### 8.6 前端工作台

最终统一收口四个入口：

1. 合同治理台
2. 审批台
3. 治理任务台
4. 运维复盘台

其中审批台当前已有页面基础，但需补独立前端路由和可达入口。

## 9. 模块归属与服务边界

### 9.1 模块归属

| 对象 / 能力 | 归属模块 | 说明 |
| --- | --- | --- |
| 规范字段、厂商证据、厂商映射规则、正式合同、合同发布批次 | `spring-boot-iot-device` | 语义契约治理域主归口 |
| 风险指标目录、风险点绑定、阈值策略、联动/预案显式绑定、运营 replay 读侧 | `spring-boot-iot-alarm` | 风险运营闭环域与桥层读写归口 |
| 审批单、审批流转、审计日志、治理工作项、运维告警生命周期、权限守卫 | `spring-boot-iot-system` | 控制面主归口 |
| 协议族、解密、编解码、主题路由 | `spring-boot-iot-protocol` + `spring-boot-iot-message` | 运行底座入口 |
| 路由、工作台、驾驶舱与控制台 UI | `spring-boot-iot-ui` | 只负责展示与交互编排 |

### 9.2 服务边界

1. `ProductModelService`  
   - 负责 compare / apply / 正式合同落库 / 生成发布批次  
   - **不直接编排风险闭环对象**
2. `ProductContractReleaseService`  
   - 负责发布批次、快照、影响分析、回滚  
   - 发布成功后只发领域事件，不直接回写风险对象
3. `RiskMetricCatalogService`  
   - 负责把正式合同发布为桥层目录  
   - 负责目录退役、目录读侧查询
4. `RiskPointService` / `RuleDefinitionService` / `LinkageRuleService` / `EmergencyPlanService`  
   - 负责闭环域正式关系  
   - 统一优先消费 `risk_metric_id`
5. `GovernanceWorkItemService`（新增）  
   - 负责六类治理工作项的创建、关闭、阻塞、认领
6. `GovernanceOpsAlertService`（新增）  
   - 负责治理运维告警的落表、去重、恢复、关闭
7. `GovernanceApprovalService`  
   - 只负责关键写操作复核  
   - 不直接承担长期待办职责

### 9.3 关键边界规则

1. `device -> alarm` 不走“服务硬调用”主路径，优先改为领域事件通知
2. `alarm -> system` 对工作项、运维告警的更新也优先走事件
3. 驾驶舱、产品工作台、运维工作台后续优先读取控制面对象，聚合接口保留兼容但不再作为唯一真相

## 10. 状态机建议

### 10.1 合同发布批次

`RELEASED -> ROLLED_BACK`

说明：

- 审批中的状态继续留在 `sys_governance_approval_order`
- `iot_product_contract_release_batch` 只表示已执行成功的发布批次及其后续回滚结果

### 10.2 风险指标目录

`ACTIVE -> RETIRED`

说明：

- 同一产品下旧合同字段退役时，目录项进入 `RETIRED`
- 下游闭环新建关系默认只允许绑定 `ACTIVE`

### 10.3 治理工作项

`OPEN -> ACKED -> RESOLVED -> CLOSED`

辅助分支：

- `OPEN -> BLOCKED`
- `BLOCKED -> OPEN`
- `OPEN/ACKED/BLOCKED -> CANCELLED`

说明：

- `ACKED` 表示已认领开始处理
- `RESOLVED` 表示业务条件已满足，等待自动或人工关单
- `CLOSED` 表示确认收口

### 10.4 运维告警

`OPEN -> ACKED -> RESOLVED -> CLOSED`

辅助分支：

- `OPEN/ACKED -> SUPPRESSED`
- `SUPPRESSED -> OPEN`

说明：

- `RESOLVED` 表示告警条件已恢复
- `CLOSED` 表示运维确认已处理完成

## 11. 事件清单与跨域流转

### 11.1 事件清单

1. `ProductContractReleasedEvent`
   - 生产者：`spring-boot-iot-device`
   - 载荷：`productId / releaseBatchId / scenarioCode / releasedIdentifiers / operatorId / approvalOrderId`
   - 消费者：`RiskMetricCatalogService`、`GovernanceWorkItemService`
2. `ProductContractRolledBackEvent`
   - 生产者：`spring-boot-iot-device`
   - 消费者：`RiskMetricCatalogService`、`GovernanceWorkItemService`
3. `RiskMetricCatalogPublishedEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 载荷：`productId / releaseBatchId / publishedRiskMetricIds / retiredRiskMetricIds`
   - 消费者：`GovernanceWorkItemService`
4. `RiskPointBindingChangedEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 消费者：`GovernanceWorkItemService`
5. `RuleDefinitionChangedEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 消费者：`GovernanceWorkItemService`
6. `RiskMetricCoverageChangedEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 消费者：`GovernanceWorkItemService`
7. `GovernanceOpsAlertRaisedEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 消费者：`GovernanceOpsAlertService`
8. `GovernanceOpsAlertRecoveredEvent`
   - 生产者：`spring-boot-iot-alarm`
   - 消费者：`GovernanceOpsAlertService`

### 11.2 最小闭环流

1. 产品合同 apply 审批通过
2. `device` 落正式合同 + 发布批次 + 快照
3. 发出 `ProductContractReleasedEvent`
4. `alarm` 发布 `risk_metric_catalog`，并带上同一个 `releaseBatchId`
5. 发出 `RiskMetricCatalogPublishedEvent`
6. `system` 更新治理工作项：
   - 关闭“待发布合同”
   - 判断是否开启“待发布风险指标目录 / 待绑定风险点 / 待补阈值策略 / 待补联动预案”
7. 运维侧根据证据与目录差异发出 `GovernanceOpsAlertRaisedEvent`
8. 控制面统一展示审批、工作项、运维告警、replay

## 12. 实施顺序

### 12.1 第一阶段：冻结对象边界与文档口径

目标：

1. 把“正式合同”“风险指标”“治理任务”“控制面”四类对象正式写入架构文档。
2. 升级 `docs/01`、`docs/02`、`docs/03`、`docs/04`、`docs/08`、`docs/19`、`docs/21`，并检查 `README.md` 与 `AGENTS.md`。

### 12.2 第二阶段：先补桥层，不动大前台

目标：

1. 补强 `iot_normative_metric_definition`
2. 新增 `iot_vendor_metric_mapping_rule`
3. 补强 `risk_metric_catalog`
4. 明确“风险闭环必须经过风险指标目录”

### 12.3 第三阶段：补显式处置绑定

目标：

1. 新增 `risk_metric_linkage_binding`
2. 新增 `risk_metric_emergency_plan_binding`
3. 把联动/预案覆盖率从语义猜测切换成显式关系

### 12.4 第四阶段：补控制面对象

目标：

1. 新增 `iot_governance_work_item`
2. 新增 `iot_governance_ops_alert`
3. 补审批台独立路由
4. 新增治理任务工作台
5. 新增运维复盘工作台

### 12.5 第五阶段：补扩展模型

建议后续继续新增如下对象：

1. `iot_protocol_family_profile`
2. `iot_decrypt_profile`
3. `iot_relation_normalization_rule`
4. `iot_metric_generation_rule`

目标：

1. 让协议族、解密、父子关系归一和风险指标生成有正式扩展对象
2. 逐步把 `LegacyDp*` 与散落在 service 里的厂商特例迁出

## 13. 验收口径

本轮设计落地后，至少应达到以下口径：

1. 文档能够明确回答：
   - 什么是正式合同
   - 什么是风险指标
   - 什么是控制面任务
2. 风险域默认通过 `risk_metric_id` 消费桥层对象。
3. 联动和预案覆盖率具备显式绑定关系，不再只依赖语义推断。
4. 首页和对象页的治理待办可回收到正式任务对象，而不是只靠聚合提示。
5. 真实环境验收时，可以按 `releaseBatchId / traceId / deviceCode / productKey` 回答治理与风险链路断点。

## 14. 非目标

本轮明确不做以下事项：

1. 不把项目拆为微服务。
2. 不引入外部 BPM 或工作流引擎。
3. 不一次性重写所有现有控制器路径。
4. 不把覆盖率和驾驶舱 KPI 单独落成统计真相表。
5. 不把全部厂商/全部协议一次性纳入零代码接入。

## 15. 风险与后续关注点

1. 若不尽快把映射规则对象独立出来，`iot_vendor_metric_evidence` 会继续膨胀成“证据 + 规则 + 决策”混合体。
2. 若不尽快把联动/预案覆盖关系显式化，驾驶舱和影响分析会长期依赖语义推测，难以稳定。
3. 若不把六类待办落成正式任务对象，前台仍会停留在“提示式工作台”，难以升级成真正的任务系统。
4. 若不把桥层对象继续扩成长期真相，风险域仍可能被 `iot_product_model` 的运行态细节反向牵制。
5. 若继续让 `device` 服务直接调用 `alarm` 服务完成目录发布，会逐步破坏模块边界，后续回滚、补偿和审计都会变得更脆弱。
