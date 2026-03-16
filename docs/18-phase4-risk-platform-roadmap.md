# Phase 4 - IoT智能风险监测预警处置平台研发计划

本文档用于规划 Phase 4 的开发方向、优先级、模块影响范围、数据表扩展建议和任务拆解。

## 1. Phase 4 总目标

在 Phase 1/2/3 已完成的设备管理、协议解析、消息接入、指令闭环、网关拓扑、规则引擎基础上，进入"风险监测预警处置"阶段。

Phase 4 的总体目标是：
- 构建完整的风险监测预警处置平台
- 实现告警管理、事件处置、风险配置、分析报表等核心功能
- 支持智慧园区、智慧水利、智慧仓储、城市安全监测等场景
- 保持当前模块化单体结构，继续以 `spring-boot-iot-admin` 为唯一启动模块

## 2. 当前项目已具备的能力基础

### 2.1 Phase 1 已完成基础
- 产品管理
- 设备管理
- HTTP 模拟设备上报
- 协议解析
- 消息日志持久化
- 最新属性更新
- 设备在线状态更新

### 2.2 Phase 2 已完成基础
- MQTT 接入骨架
- MQTT topic 解析
- 设备认证基础版
- 设备会话与在线状态基础版
- MQTT 上行真实联调
- MQTT 下行最小发布能力
- 子设备 topic 规范与解析扩展点预留

### 2.3 Phase 3 已完成基础
- 指令闭环能力（命令记录与状态模型、MQTT 回执接入）
- 网关/子设备业务闭环（静态拓扑、子设备上报/下发）
- 规则引擎基础版（单条件单动作规则）

### 2.4 对 Phase 4 最有价值的现有积累
- 已有统一主链路：`RawDeviceMessage -> UpMessageDispatcher -> ProtocolAdapter -> DeviceMessageService`
- 已有 MQTT 下行发布骨架
- 已有 topic 解析扩展点
- 已有设备在线状态与 Redis 会话基础
- 已有消息日志表和最新属性表，可作为规则和闭环能力的基础输入
- 已有规则引擎基础框架

## 3. Phase 4 核心功能模块

### 3.1 风险监测驾驶舱（升级）
**目标**：提供风险态势总览、区域风险地图、重点风险点看板

**核心组件**：
- KPI指标卡片（设备总数、在线设备数、今日告警数、未关闭事件数）
- 风险趋势折线图
- 告警等级分布饼图
- GIS风险点分布地图
- 最新告警列表
- 待处理事件列表

### 3.2 风险监测模块（新增）
**目标**：提供实时监测、监测详情、历史趋势分析、区域监测看板

**核心功能**：
- 实时监测列表（设备编码、设备名称、风险点、测点名称、当前值、单位、状态、最新上报时间、风险等级、是否告警）
- 监测详情页面（设备信息、当前监测数据、最近24小时趋势、最近告警记录）

### 3.3 告警中心模块（新增）
**目标**：提供实时告警列表、告警详情、告警确认、告警抑制、通知记录

**核心功能**：
- 告警列表（告警编号、告警标题、告警类型、告警等级、区域、风险点、设备名称、测点名称、当前值、阈值、状态、触发时间）
- 告警详情（规则来源、触发记录、通知记录、处理意见）
- 告警确认
- 告警抑制
- 通知记录

### 3.4 事件处置模块（新增）
**目标**：提供事件列表、事件详情、工单派发、现场反馈、事件复盘

**核心流程**：
- 告警触发 → 生成事件 → 工单派发 → 现场处理 → 事件关闭 → 复盘分析

**核心功能**：
- 事件列表（事件编号、事件标题、风险等级、责任人、紧急程度、到场时限、完成时限、状态）
- 事件详情
- 工单派发
- 现场反馈
- 事件复盘

### 3.5 设备中心模块（升级）
**目标**：提供产品管理、产品物模型、设备管理、设备详情、在线记录、运维记录

**核心功能**：
- 设备管理（设备编码、设备名称、产品名称、风险点、安装位置、在线状态、运行状态、最近上报时间）
- 产品物模型（属性编码、属性名称、数据类型、单位、是否风险监测项、默认阈值）
- 设备详情
- 在线记录
- 运维记录

### 3.6 风险配置模块（新增）
**目标**：提供风险点管理、风险分级配置、阈值规则、联动规则、应急预案

**核心功能**：
- 风险点管理（风险点编号、风险点名称、区域、负责人、风险等级）
- 风险分级配置
- 阈值规则（规则名称、测点、表达式、持续时间、告警等级、通知方式、是否转事件）
- 联动规则
- 应急预案

### 3.7 分析报表模块（新增）
**目标**：提供风险趋势分析、告警统计报表、事件闭环分析、设备健康分析

**核心功能**：
- 风险趋势分析
- 告警统计报表
- 事件闭环分析
- 设备健康分析

### 3.8 系统管理模块（新增）
**目标**：提供组织机构、用户管理、角色权限、区域管理、字典配置、通知渠道、审计日志

**核心功能**：
- 组织机构
- 用户管理
- 角色权限
- 区域管理
- 字典配置
- 通知渠道
- 审计日志

## 4. 推荐优先级

推荐优先级如下：

1. **告警中心基础能力**（P1）
2. **事件处置基础能力**（P1）
3. **风险点管理**（P2）
4. **阈值规则配置**（P2）
5. **联动规则与应急预案**（P3）
6. **分析报表**（P3）
7. **系统管理**（P3）
8. **实时监测**（P4）
9. **GIS地图集成**（P4）
10. **通知与提醒**（P4）

## 5. 推荐的 Phase 4 执行顺序

### 第一阶段：告警中心与事件处置（2-3周）
**原因**：
- 告警和事件是风险监测的核心业务
- 告警中心为事件处置提供输入
- 两者形成闭环，可快速验证业务价值
- 对现有模块冲击最小

### 第二阶段：风险配置（1-2周）
**原因**：
- 风险点和规则是告警触发的基础
- 在告警中心稳定后推进
- 为后续联动规则和应急预案打基础

### 第三阶段：分析报表与系统管理（2-3周）
**原因**：
- 分析报表依赖告警和事件数据
- 系统管理为平台提供基础支撑
- 在核心业务稳定后推进

### 第四阶段：增强功能（1-2周）
**原因**：
- 实时监测、GIS地图、通知提醒为增强功能
- 在核心功能稳定后推进
- 提升平台完整性和用户体验

## 6. Task 拆解

### Task 1: 告警中心基础能力
**目标**：实现告警列表、告警详情、告警确认、告警抑制、通知记录

**影响模块**：
- `spring-boot-iot-alarm`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**新增数据表**：
- `iot_alarm_record`

**建议字段**：
- `id`
- `alarm_code` - 告警编号
- `alarm_title` - 告警标题
- `alarm_type` - 告警类型
- `alarm_level` - 告警等级
- `region_id` - 区域ID
- `region_name` - 区域名称
- `risk_point_id` - 风险点ID
- `risk_point_name` - 风险点名称
- `device_id` - 设备ID
- `device_code` - 设备编码
- `device_name` - 设备名称
- `metric_name` - 测点名称
- `current_value` - 当前值
- `threshold_value` - 阈值
- `status` - 状态（0-未确认，1-已确认，2-已抑制，3-已关闭）
- `trigger_time` - 触发时间
- `confirm_time` - 确认时间
- `confirm_user` - 确认用户
- `suppress_time` - 抑制时间
- `suppress_user` - 抑制用户
- `close_time` - 关闭时间
- `close_user` - 关闭用户
- `rule_id` - 规则ID
- `rule_name` - 规则名称
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

**任务清单**：
- [ ] 创建 `iot_alarm_record` 表
- [ ] 创建 AlarmRecord 实体类
- [ ] 创建 AlarmRecordMapper
- [ ] 创建 AlarmRecordService
- [ ] 创建 AlarmRecordController
- [ ] 实现告警列表API
- [ ] 实现告警详情API
- [ ] 实现告警确认API
- [ ] 实现告警抑制API
- [ ] 实现通知记录API
- [ ] 前端告警中心页面
- [ ] 端到端测试

### Task 2: 事件处置基础能力
**目标**：实现事件列表、事件详情、工单派发、事件关闭、事件复盘

**影响模块**：
- `spring-boot-iot-event` (新增)
- `spring-boot-iot-alarm`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**新增数据表**：
- `iot_event_record`
- `iot_event_work_order`

**建议字段**：

`iot_event_record`
- `id`
- `event_code` - 事件编号
- `event_title` - 事件标题
- `alarm_id` - 告警ID
- `alarm_code` - 告警编号
- `alarm_level` - 告警等级
- `risk_level` - 风险等级
- `region_id` - 区域ID
- `region_name` - 区域名称
- `risk_point_id` - 风险点ID
- `risk_point_name` - 风险点名称
- `device_id` - 设备ID
- `device_code` - 设备编码
- `device_name` - 设备名称
- `metric_name` - 测点名称
- `current_value` - 当前值
- `status` - 状态（0-待派发，1-已派发，2-处理中，3-待验收，4-已关闭，5-已取消）
- `responsible_user` - 责任人
- `urgency_level` - 紧急程度
- `arrival_time_limit` - 到场时限（分钟）
- `completion_time_limit` - 完成时限（分钟）
- `trigger_time` - 触发时间
- `dispatch_time` - 派发时间
- `dispatch_user` - 派发用户
- `start_time` - 处理开始时间
- `complete_time` - 处理完成时间
- `close_time` - 关闭时间
- `close_user` - 关闭用户
- `close_reason` - 关闭原因
- `review_notes` - 复盘记录
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

`iot_event_work_order`
- `id`
- `event_id` - 事件ID
- `event_code` - 事件编号
- `work_order_code` - 工单编号
- `work_order_type` - 工单类型
- `assign_user` - 派发用户
- `receive_user` - 接收用户
- `receive_time` - 接收时间
- `start_time` - 开始时间
- `complete_time` - 完成时间
- `status` - 状态（0-待接收，1-已接收，2-处理中，3-已完成，4-已取消）
- `feedback` - 现场反馈
- `photos` - 照片URL（JSON数组）
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

**任务清单**：
- [ ] 创建 `iot_event_record` 和 `iot_event_work_order` 表
- [ ] 创建 EventRecord 和 EventWorkOrder 实体类
- [ ] 创建 EventRecordMapper 和 EventWorkOrderMapper
- [ ] 创建 EventRecordService 和 EventWorkOrderService
- [ ] 创建 EventRecordController
- [ ] 实现事件列表API
- [ ] 实现事件详情API
- [ ] 实现工单派发API
- [ ] 实现事件关闭API
- [ ] 实现场景反馈API
- [ ] 实现事件复盘API
- [ ] 前端事件处置页面
- [ ] 端到端测试

### Task 3: 风险点管理
**目标**：实现风险点CRUD、风险点与设备绑定

**影响模块**：
- `spring-boot-iot-risk` (新增)
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**新增数据表**：
- `risk_point`

**建议字段**：
- `id`
- `risk_point_code` - 风险点编号
- `risk_point_name` - 风险点名称
- `region_id` - 区域ID
- `region_name` - 区域名称
- `responsible_user` - 负责人
- `responsible_phone` - 负责人电话
- `risk_level` - 风险等级
- `description` - 描述
- `status` - 状态（0-启用，1-停用）
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

**任务清单**：
- [ ] 创建 `risk_point` 表
- [ ] 创建 RiskPoint 实体类
- [ ] 创建 RiskPointMapper
- [ ] 创建 RiskPointService
- [ ] 创建 RiskPointController
- [ ] 实现风险点CRUD API
- [ ] 实现风险点与设备绑定API
- [ ] 前端风险点管理页面
- [ ] 端到端测试

### Task 4: 阈值规则配置
**目标**：实现规则CRUD、规则测试

**影响模块**：
- `spring-boot-iot-risk`
- `spring-boot-iot-rule`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**新增数据表**：
- `rule_definition`

**建议字段**：
- `id`
- `rule_code` - 规则编码
- `rule_name` - 规则名称
- `risk_point_id` - 风险点ID
- `metric_identifier` - 测点标识符
- `condition_expression` - 表达式
- `duration` - 持续时间（秒）
- `alarm_level` - 告警等级
- `notify_methods` - 通知方式（JSON数组）
- `convert_to_event` - 是否转事件
- `status` - 状态（0-启用，1-停用）
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

**任务清单**：
- [ ] 创建 `rule_definition` 表
- [ ] 创建 RuleDefinition 实体类
- [ ] 创建 RuleDefinitionMapper
- [ ] 创建 RuleDefinitionService
- [ ] 创建 RuleDefinitionController
- [ ] 实现规则CRUD API
- [ ] 实现规则测试API
- [ ] 前端规则配置页面
- [ ] 端到端测试

### Task 5: 联动规则与应急预案
**目标**：实现联动规则CRUD、应急预案CRUD、联动执行

**影响模块**：
- `spring-boot-iot-risk`
- `spring-boot-iot-rule`
- `spring-boot-iot-message`
- `spring-boot-iot-admin`

**新增数据表**：
- `linkage_rule`
- `emergency_plan`

**建议字段**：

`linkage_rule`
- `id`
- `rule_code` - 规则编码
- `rule_name` - 规则名称
- `trigger_condition` - 触发条件
- `action_list` - 执行动作（JSON数组）
- `execution_order` - 执行顺序
- `timeout` - 执行超时（秒）
- `retry_strategy` - 重试策略
- `status` - 状态（0-启用，1-停用）
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

`emergency_plan`
- `id`
- `plan_code` - 预案编码
- `plan_name` - 预案名称
- `risk_level` - 风险等级
- `applicable_scenario` - 适用场景
- `disposal_steps` - 处置步骤（JSON数组）
- `responsible_user` - 责任人
- `resource_requirements` - 资源需求
- `status` - 状态（0-启用，1-停用）
- `tenant_id` - 租户ID
- `create_time` - 创建时间
- `update_time` - 更新时间

**任务清单**：
- [ ] 创建 `linkage_rule` 和 `emergency_plan` 表
- [ ] 创建 LinkageRule 和 EmergencyPlan 实体类
- [ ] 创建 LinkageRuleMapper 和 EmergencyPlanMapper
- [ ] 创建 LinkageRuleService 和 EmergencyPlanService
- [ ] 创建 LinkageRuleController 和 EmergencyPlanController
- [ ] 实现联动规则CRUD API
- [ ] 实现应急预案CRUD API
- [ ] 实现联动执行API
- [ ] 前端联动规则页面
- [ ] 前端应急预案页面
- [ ] 端到端测试

### Task 6: 分析报表
**目标**：实现风险趋势分析、告警统计、事件闭环分析、设备健康分析

**影响模块**：
- `spring-boot-iot-report` (新增)
- `spring-boot-iot-alarm`
- `spring-boot-iot-event`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**任务清单**：
- [ ] 创建 ReportService
- [ ] 实现风险趋势分析API
- [ ] 实现告警统计API
- [ ] 实现事件闭环分析API
- [ ] 实现设备健康分析API
- [ ] 前端报表页面
- [ ] 端到端测试

### Task 7: 系统管理
**目标**：实现组织机构、用户管理、角色权限、区域管理、字典配置、通知渠道、审计日志

**影响模块**：
- `spring-boot-iot-system` (扩展)
- `spring-boot-iot-admin`

**新增数据表**：
- `sys_organization`
- `sys_region`
- `sys_dict`
- `sys_audit_log`

**任务清单**：
- [ ] 创建 `sys_organization`、`sys_region`、`sys_dict`、`sys_audit_log` 表
- [ ] 创建对应实体类和Mapper
- [ ] 实现组织机构管理API
- [ ] 实现用户管理API
- [ ] 实现区域管理API
- [ ] 实现字典配置API
- [ ] 实现通知渠道API
- [ ] 实现审计日志API
- [ ] 前端系统管理页面
- [ ] 端到端测试

### Task 8: 实时监测
**目标**：实现实时监测列表、监测详情

**影响模块**：
- `spring-boot-iot-risk`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

**任务清单**：
- [ ] 实现实时监测列表API
- [ ] 实现监测详情API
- [ ] 前端实时监测页面
- [ ] 端到端测试

### Task 9: GIS地图集成
**目标**：实现风险点地图、区域风险热力图

**影响模块**：
- `spring-boot-iot-risk`
- `spring-boot-iot-admin`

**任务清单**：
- [ ] 实现风险点地图API
- [ ] 实现区域风险热力图API
- [ ] 前端GIS地图页面
- [ ] 端到端测试

### Task 10: 通知与提醒
**目标**：实现通知记录、通知发送、通知模板管理

**影响模块**：
- `spring-boot-iot-alarm`
- `spring-boot-iot-message`
- `spring-boot-iot-admin`

**新增数据表**：
- `iot_notification_record`

**任务清单**：
- [ ] 创建 `iot_notification_record` 表
- [ ] 创建 NotificationRecord 实体类
- [ ] 创建 NotificationRecordMapper
- [ ] 实现通知记录API
- [ ] 实现通知发送API
- [ ] 实现通知模板管理API
- [ ] 前端通知中心页面
- [ ] 端到端测试

## 7. 哪些能力继续保持"最小可运行实现"

### 建议继续最小实现的能力
- 告警中心
  - 先只支持单设备、单告警、单确认
- 事件处置
  - 先只支持静态派发和基础反馈
- 联动规则
  - 先只支持单条件、单动作
- 应急预案
  - 先只支持静态步骤和基础执行

### 适合后续增强的能力
- 告警聚合与去重
- 事件自动派单
- 联动规则可视化编排
- 应急预案动态调整
- 报表导出与分享
- 多租户隔离

## 8. 推荐先走的路线

我推荐先走"告警中心与事件处置"这条路线。

原因：
- 它直接建立在 Phase 3 已完成的规则引擎能力之上
- 影响模块较少，风险最可控
- 最容易形成可演示的业务价值
- 告警和事件形成闭环，能直接服务风险监测场景
- 若先做系统管理或GIS地图，整体面会更大，且会反过来依赖告警和事件能力

## 9. Phase 4 规划结论

Phase 4 不建议一开始并行推进多个大方向。

更稳的方式是：
1. 先补告警中心与事件处置
2. 再做风险配置
3. 最后做分析报表与系统管理

这样可以在保持当前 Phase 1/2/3 稳定成果的前提下，逐步把平台推向真正可运营的风险监测预警处置平台。

## 10. 技术架构

### 后端模块
```
spring-boot-iot
├── spring-boot-iot-alarm (扩展)
│   └── 告警中心
├── spring-boot-iot-event (新增)
│   └── 事件处置
├── spring-boot-iot-risk (新增)
│   └── 风险配置
├── spring-boot-iot-report (新增)
│   └── 分析报表
├── spring-boot-iot-system (扩展)
│   └── 系统管理
└── spring-boot-iot-admin (扩展)
    └── 应用启动
```

### 前端页面
```
spring-boot-iot-ui
├── CockpitView.vue (升级)
│   └── 风险监测驾驶舱
├── AlarmCenterView.vue (新增)
│   └── 告警中心
├── EventDisposalView.vue (新增)
│   └── 事件处置
├── RiskConfigurationView.vue (新增)
│   └── 风险配置
├── ReportAnalysisView.vue (新增)
│   └── 分析报表
├── SystemManagementView.vue (新增)
│   └── 系统管理
└── RealTimeMonitoringView.vue (新增)
    └── 实时监测
```

### 数据库
```
rm_iot
├── iot_alarm_record (新增)
├── iot_event_record (新增)
├── iot_event_work_order (新增)
├── risk_point (新增)
├── rule_definition (新增)
├── linkage_rule (新增)
├── emergency_plan (新增)
├── sys_organization (新增)
├── sys_region (新增)
├── sys_dict (新增)
├── sys_audit_log (新增)
└── iot_notification_record (新增)
```

## 11. 实施原则

1. **渐进式实施**：分阶段推进，不影响现有功能
2. **向后兼容**：不破坏现有API和页面
3. **文档驱动**：每个功能都有对应文档
4. **测试覆盖**：关键功能有单元测试和真实环境验收
5. **模块化设计**：保持现有模块边界清晰
6. **最小可运行**：每个阶段都有可演示成果

## 12. 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 需求变更 | 高 | 中 | 需求评审、分阶段确认 |
| 技术复杂度 | 中 | 中 | 技术预研、原型验证 |
| 时间延期 | 中 | 低 | 进度跟踪、及时调整 |
| 质量问题 | 高 | 低 | 代码评审、测试覆盖 |

## 13. 验收标准

### Phase 4 完成定义
Phase 4 应该满足以下条件：
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
- 告警中心真实环境验收通过
- 事件处置真实环境验收通过
- 风险配置真实环境验收通过
- 报表分析真实环境验收通过
- 以下API可用：
  - `POST /alarm/add` - 新增告警
  - `GET /alarm/list` - 告警列表
  - `GET /alarm/{id}` - 告警详情
  - `POST /alarm/{id}/confirm` - 告警确认
  - `POST /alarm/{id}/suppress` - 告警抑制
  - `POST /event/add` - 新增事件
  - `GET /event/list` - 事件列表
  - `GET /event/{id}` - 事件详情
  - `POST /event/{id}/dispatch` - 工单派发
  - `POST /event/{id}/close` - 事件关闭
  - `POST /risk-point/add` - 新增风险点
  - `GET /risk-point/list` - 风险点列表
  - `POST /rule/add` - 新增规则
  - `GET /rule/list` - 规则列表
  - `GET /report/risk-trend` - 风险趋势分析
  - `GET /report/alarm-statistics` - 告警统计
  - `GET /report/event-closure` - 事件闭环分析
  - `GET /report/device-health` - 设备健康分析
- 文档完整更新

## 15. 已完成任务（2026-03-15）

### Task 1: 告警中心基础能力（部分完成）
**状态**：后端已完成，前端页面已完成

**已完成**：
- [x] 创建 `iot_alarm_record` 表（已收敛至 `sql/init.sql`，历史库兼容见 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql`）
- [x] 创建 AlarmRecord 实体类
- [x] 创建 AlarmRecordMapper
- [x] 创建 AlarmRecordService
- [x] 创建 AlarmRecordController
- [x] 实现告警列表API
- [x] 实现告警详情API
- [x] 实现告警确认API
- [x] 实现告警抑制API
- [x] 前端告警中心页面（AlarmCenterView.vue）
- [x] 前端告警中心 API（alarm.ts）
- [x] 前端告警中心路由配置
- [x] 后端代码编译通过

**待完成**：
- [ ] 端到端测试
- [ ] 通知记录API

### Task 2: 事件处置基础能力（部分完成）
**状态**：后端已完成，前端页面已完成

**已完成**：
- [x] 创建 `iot_event_record` 和 `iot_event_work_order` 表
- [x] 创建 EventRecord 和 EventWorkOrder 实体类
- [x] 创建 EventRecordMapper 和 EventWorkOrderMapper
- [x] 创建 EventRecordService 和 EventWorkOrderService
- [x] 创建 EventRecordController
- [x] 实现事件列表API
- [x] 实现事件详情API
- [x] 实现工单派发API
- [x] 实现事件关闭API
- [x] 前端事件处置页面（EventDisposalView.vue）
- [x] 前端告警中心 API（alarm.ts）
- [x] 前端事件处置路由配置
- [x] 后端代码编译通过

**待完成**：
- [ ] 端到端测试
- [ ] 实现场景反馈API
- [ ] 实现事件复盘API

### Task 3: 风险点管理（未开始）
**状态**：待开始

**任务清单**：
- [ ] 创建 `risk_point` 表
- [ ] 创建 RiskPoint 实体类
- [ ] 创建 RiskPointMapper
- [ ] 创建 RiskPointService
- [ ] 创建 RiskPointController
- [ ] 实现风险点CRUD API
- [ ] 实现风险点与设备绑定API
- [ ] 前端风险点管理页面
- [ ] 端到端测试

### Task 4: 阈值规则配置（未开始）
**状态**：待开始

**任务清单**：
- [ ] 创建 `rule_definition` 表
- [ ] 创建 RuleDefinition 实体类
- [ ] 创建 RuleDefinitionMapper
- [ ] 创建 RuleDefinitionService
- [ ] 创建 RuleDefinitionController
- [ ] 实现规则CRUD API
- [ ] 实现规则测试API
- [ ] 前端规则配置页面
- [ ] 端到端测试

### Task 5: 联动规则与应急预案（已完成）
**状态**：已完成

**已完成**：
- [x] 创建 `linkage_rule` 和 `emergency_plan` 表（已收敛至 `sql/init.sql`，历史库兼容见 `sql/upgrade/20260316_phase4_real_env_schema_alignment.sql`）
- [x] 创建 LinkageRule 和 EmergencyPlan 实体类
- [x] 创建 LinkageRuleMapper 和 EmergencyPlanMapper
- [x] 创建 LinkageRuleService 和 EmergencyPlanService
- [x] 创建 LinkageRuleController 和 EmergencyPlanController
- [x] 实现联动规则CRUD API
- [x] 实现应急预案CRUD API
- [x] 前端联动规则页面（LinkageRuleView.vue）
- [x] 前端应急预案页面（EmergencyPlanView.vue）
- [x] 前端联动规则 API（linkageRule.ts）
- [x] 前端应急预案 API（emergencyPlan.ts）
- [x] 路由配置（router/index.ts）
- [x] 后端代码编译通过

**待完成**：
- [ ] 端到端测试
- [ ] 实现联动执行API
- [ ] 前端联动规则路由配置（已添加）
- [ ] 前端应急预案路由配置（已添加）

### Task 6: 分析报表（未开始）
**状态**：待开始

**任务清单**：
- [ ] 创建 ReportService
- [ ] 实现风险趋势分析API
- [ ] 实现告警统计API
- [ ] 实现事件闭环分析API
- [ ] 实现设备健康分析API
- [ ] 前端报表页面
- [ ] 端到端测试

### Task 7: 系统管理（未开始）
**状态**：待开始

**任务清单**：
- [ ] 创建 `sys_organization`、`sys_region`、`sys_dict`、`sys_audit_log` 表
- [ ] 创建对应实体类和Mapper
- [ ] 实现组织机构管理API
- [ ] 实现用户管理API
- [ ] 实现区域管理API
- [ ] 实现字典配置API
- [ ] 实现通知渠道API
- [ ] 实现审计日志API
- [ ] 前端系统管理页面
- [ ] 端到端测试

### Task 8: 实时监测（未开始）
**状态**：待开始

**任务清单**：
- [ ] 实现实时监测列表API
- [ ] 实现监测详情API
- [ ] 前端实时监测页面
- [ ] 端到端测试

### Task 9: GIS地图集成（未开始）
**状态**：待开始

**任务清单**：
- [ ] 实现风险点地图API
- [ ] 实现区域风险热力图API
- [ ] 前端GIS地图页面
- [ ] 端到端测试

### Task 10: 通知与提醒（未开始）
**状态**：待开始

**新增数据表**：
- `iot_notification_record`

**任务清单**：
- [ ] 创建 `iot_notification_record` 表
- [ ] 创建 NotificationRecord 实体类
- [ ] 创建 NotificationRecordMapper
- [ ] 实现通知记录API
- [ ] 实现通知发送API
- [ ] 实现通知模板管理API
- [ ] 前端通知中心页面
- [ ] 端到端测试

## 14. 后续演进方向

### Phase 5 - AI智能研判
- 风险预测模型
- 异常检测算法
- 智能派单
- 自动处置建议

### Phase 6 - 数字孪生
- 3D可视化
- 设备拓扑图
- 场景模拟
- 虚实联动

### Phase 7 - 移动端
- 移动App
- 推送通知
- 移动审批
- 现场反馈
