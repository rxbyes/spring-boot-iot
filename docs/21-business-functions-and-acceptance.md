# spring-boot-iot 业务功能清单与验收标准

更新时间：2026-03-16

本文档用于把 `spring-boot-iot` 当前系统能力，整理为一份可用于评审与交付的「业务功能清单」与「验收标准」。

依据范围（来自 `docs/`）：
1. `docs/00-overview.md`、`docs/01-architecture.md`、`docs/02-module-structure.md`
2. `docs/03-database.md`、`docs/04-api.md`、`docs/05-protocol.md`、`docs/07-message-flow.md`
3. `docs/test-scenarios.md`
4. Phase 4：以 `docs/19-phase4-progress.md` 为权威进度口径；`docs/18-phase4-risk-platform-roadmap.md` 提供验收目标与参考 API 口径

## 1. 系统范围与模块清单

系统定位：模块化单体 IoT 网关平台，统一打通设备接入、协议适配、消息处理、设备管理，并在此基础上扩展风险监测预警与处置能力。

硬约束（验收前置条件）：
1. 项目名：`spring-boot-iot`
2. Base package：`com.ghlzm.iot`
3. 唯一启动模块：`spring-boot-iot-admin`
4. 不破坏模块边界，不把业务逻辑写进 Controller，不把持久化逻辑写进协议适配层

模块（按 `docs/02-module-structure.md`）：
- spring-boot-iot-common：通用模型、异常、返回体、工具
- spring-boot-iot-framework：全局配置、安全、缓存、MyBatis 等
- spring-boot-iot-auth：认证
- spring-boot-iot-system：系统管理（用户、角色、组织、区域、字典、审计、通知渠道等）
- spring-boot-iot-device：产品、设备、属性、消息日志、驾驶舱数据
- spring-boot-iot-gateway：网关与子设备拓扑（能力闭环相关）
- spring-boot-iot-protocol：协议适配器、编解码、安全校验
- spring-boot-iot-message：接入入口与分发（HTTP/MQTT 上行，MQTT 下行发布）
- spring-boot-iot-telemetry：时序/历史查询抽象（按规划逐步落地）
- spring-boot-iot-rule：规则引擎（按规划逐步落地）
- spring-boot-iot-alarm：告警中心、事件处置、风险点、阈值规则、联动与预案
- spring-boot-iot-report：分析报表
- spring-boot-iot-ota：OTA（按规划逐步落地）
- spring-boot-iot-admin：应用启动与整合

## 2. 业务功能清单

### 2.1 IoT 基础域（产品、设备、上报、数据落库）

业务功能点：
1. 产品管理
2. 物模型管理（产品模型：属性、事件、服务）
3. 设备管理（设备注册、查询）
4. 设备上行数据接入
5. 协议解析与标准化
6. 原始消息日志持久化
7. 最新属性更新
8. 设备在线状态维护

对应核心表（参考 `docs/03-database.md`）：
1. `iot_product`
2. `iot_product_model`
3. `iot_device`
4. `iot_device_property`
5. `iot_message_log`

已明确的对外 API（参考 `docs/04-api.md`）：
1. `POST /device/product/add`，新增产品
2. `GET /device/product/{id}`，查询产品
3. `POST /device/add`，新增设备
4. `GET /device/{id}`，查询设备
5. `GET /device/code/{deviceCode}`，按编码查询设备
6. `POST /message/http/report`，HTTP 模拟上报入口
7. `GET /device/{deviceCode}/properties`，查询最新属性
8. `GET /device/{deviceCode}/message-logs`，查询消息日志

### 2.2 设备接入域（MQTT 上下行与 Topic 规范）

业务功能点：
1. MQTT 上行接入（直连设备标准 Topic）
2. MQTT 上行接入（历史兼容 `$dp`）
3. 网关代子设备 Topic 解析结构预留
4. MQTT 下行最小发布能力

Topic 规范（参考 `docs/05-protocol.md`、`docs/04-api.md`）：
1. 上行标准 Topic：`/sys/{productKey}/{deviceCode}/thing/{property|event|status}/post`
2. 上行兼容 Topic：`$dp`
3. 子设备上行预留：`/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/{property|event|status}/post`
4. 下行标准 Topic：`/sys/{productKey}/{deviceCode}/thing/property/set`、`/sys/{productKey}/{deviceCode}/thing/service/{serviceIdentifier}/invoke`
5. 子设备下行预留：`/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`、`/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/service/{serviceIdentifier}/invoke`

下行 API（参考 `docs/04-api.md`）：
1. `POST /message/mqtt/down/publish`

### 2.3 协议适配与安全校验（mqtt-json 与 `$dp` 兼容）

业务功能点（参考 `docs/05-protocol.md`）：
1. `ProtocolAdapter` 插件化适配（`getProtocolCode`、`decode`、`encode`）
2. mqtt-json 上行解码为统一 `DeviceUpMessage`
3. mqtt-json 下行编码统一 `DeviceDownMessage`
4. `$dp` 三类历史报文兼容
5. AES 解密与多商户 key 选择（`header.appId` -> `spring.cloud.aes.merchants`）
6. DES/3DES 扩展解密
7. 安全校验：签名、时间戳、nonce 防重放（缺失时兼容旧设备，不阻断主链路）
8. 二进制帧兼容：C.1/C.2/C.3/C.4（按文档定义）

调试辅助 API（参考 `docs/04-api.md`）：
1. `GET /device/{deviceCode}/file-snapshots`，查看 C.3 文件快照 Redis 最小持久化结果
2. `GET /device/{deviceCode}/firmware-aggregates`，查看 C.4 固件分包 Redis 聚合结果与 MD5 校验

### 2.4 平台闭环域（Phase 3 方向）

说明：`docs/16-phase3-roadmap.md`、`docs/17-command-closure-roadmap.md` 为路线与设计文档，验收口径以实际实现与对外接口为准；本节给出可复用的验收维度。

业务功能点：
1. 指令闭环（命令记录、状态流转、MQTT 回执接入、超时判定）
2. 网关与子设备闭环（静态拓扑、子设备上报与下发入链）
3. 规则引擎基础版（单条件单动作规则）

对应核心表（参考 `docs/03-database.md`）：
1. `iot_command_record`
2. `iot_gateway`、`iot_gateway_topology`
3. `iot_rule_chain`

### 2.5 风险监测预警与处置平台（Phase 4）

Phase 4 当前进度口径（参考 `docs/19-phase4-progress.md`）：
1. 已完成可用：告警中心、事件处置、风险点管理、阈值规则配置、联动规则与应急预案、分析报表、系统管理
2. 当前未计入已交付：风险监测实时监测与 GIS 地图已进入代码基线但待真实环境验收；首页驾驶舱增强、设备中心增强仍未交付
3. 待补齐：Phase 4 真实环境验收、文档一致性回写、消息日志表命名统一方案

告警中心（后端接口前缀：`/api/alarm`，以代码为准）：
1. 新增告警：`POST /api/alarm/add`
2. 告警列表：`GET /api/alarm/list`
3. 告警详情：`GET /api/alarm/{id}`
4. 告警确认：`POST /api/alarm/{id}/confirm`
5. 告警抑制：`POST /api/alarm/{id}/suppress`
6. 告警关闭：`POST /api/alarm/{id}/close`

事件处置（后端接口前缀：`/api/event`，以代码为准）：
1. 新增事件：`POST /api/event/add`
2. 事件列表：`GET /api/event/list`
3. 事件详情：`GET /api/event/{id}`
4. 工单派发：`POST /api/event/{id}/dispatch`
5. 事件关闭：`POST /api/event/{id}/close`
6. 现场反馈：`POST /api/event/{eventId}/feedback`
7. 工单列表：`GET /api/event/work-orders`
8. 工单接收：`POST /api/event/work-orders/{id}/receive`
9. 工单开始：`POST /api/event/work-orders/{id}/start`
10. 工单完成：`POST /api/event/work-orders/{id}/complete`

风险点管理（后端接口前缀：`/api/risk-point`，以代码为准）：
1. 新增风险点：`POST /api/risk-point/add`
2. 更新风险点：`POST /api/risk-point/update`
3. 删除风险点：`POST /api/risk-point/delete/{id}`
4. 风险点详情：`GET /api/risk-point/get/{id}`
5. 风险点列表：`GET /api/risk-point/list`
6. 绑定设备：`POST /api/risk-point/bind-device`
7. 解绑设备：`POST /api/risk-point/unbind-device`
8. 已绑定设备列表：`GET /api/risk-point/bound-devices/{riskPointId}`
9. 绑定弹窗设备选项：`GET /api/device/list`
10. 绑定弹窗测点选项：`GET /api/device/{deviceId}/metrics`

阈值规则配置（后端接口前缀：`/api/rule-definition`，以代码为准）：
1. 规则列表：`GET /api/rule-definition/list`
2. 规则详情：`GET /api/rule-definition/get/{id}`
3. 新增规则：`POST /api/rule-definition/add`
4. 更新规则：`POST /api/rule-definition/update`
5. 删除规则：`POST /api/rule-definition/delete/{id}`

联动规则（后端接口前缀：`/api/linkage-rule`，以代码为准）：
1. 规则列表：`GET /api/linkage-rule/list`
2. 规则详情：`GET /api/linkage-rule/get/{id}`
3. 新增规则：`POST /api/linkage-rule/add`
4. 更新规则：`POST /api/linkage-rule/update`
5. 删除规则：`POST /api/linkage-rule/delete/{id}`

应急预案（后端接口前缀：`/api/emergency-plan`，以代码为准）：
1. 预案列表：`GET /api/emergency-plan/list`
2. 预案详情：`GET /api/emergency-plan/get/{id}`
3. 新增预案：`POST /api/emergency-plan/add`
4. 更新预案：`POST /api/emergency-plan/update`
5. 删除预案：`POST /api/emergency-plan/delete/{id}`

分析报表（后端接口前缀：`/api/report`，以代码为准）：
1. 风险趋势：`GET /api/report/risk-trend`
2. 告警统计：`GET /api/report/alarm-statistics`
3. 事件闭环：`GET /api/report/event-closure`
4. 设备健康：`GET /api/report/device-health`

系统管理（后端接口前缀按各 Controller 定义，以代码为准）：
1. 组织机构：`/api/organization`
2. 用户管理：`/api/user`
3. 角色管理：`/api/role`
4. 区域管理：`/api/region`
5. 字典配置：`/api/dict`
6. 通知渠道：`/api/system/channel`
7. 审计日志：`/api/system/audit-log`

## 3. 验收标准

### 3.1 全局验收标准（所有阶段通用）

1. 构建通过：`mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
2. 启动通过：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
3. API 返回体统一：`{"code":200,"msg":"success","data":...}`（参考 `docs/04-api.md`）
4. 关键原则不破坏：模块边界、Controller 无业务逻辑、协议适配层不做持久化（参考 `docs/02-module-structure.md`、`docs/01-architecture.md`）
5. 原始报文保留，最新属性独立维护（参考 `docs/07-message-flow.md`）

### 3.2 Phase 1 验收（HTTP 主链路）

真实环境验收（参考 `docs/test-scenarios.md`）：
1. 使用 `application-dev.yml` 启动后端主服务
2. 同一套场景覆盖：产品新增、设备新增、HTTP 上报、属性查询、消息日志查询、非法协议、不存在设备

手工验收（参考 `docs/04-api.md`、`docs/test-scenarios.md`）：
1. 初始化数据库：执行 `sql/init.sql`、`sql/init-data.sql`
2. 启动应用（`dev` 真实环境）
3. 新增产品：调用 `POST /device/product/add`，返回 `code=200` 且返回产品主键
4. 新增设备：调用 `POST /device/add`，返回 `code=200`
5. 发送 HTTP 上报：调用 `POST /message/http/report`，返回 `code=200`
6. 查询属性：`GET /device/{deviceCode}/properties`，能返回上报的属性键值
7. 查询日志：`GET /device/{deviceCode}/message-logs`，能返回至少 1 条日志，topic 与 payload 正确
8. 数据库核对：`iot_message_log` 新增记录，`iot_device_property` 更新，`iot_device.online_status=1`，`last_online_time/last_report_time` 刷新

错误路径验收（参考 `docs/04-api.md`、`docs/test-scenarios.md`）：
1. protocolCode 非法时返回业务错误（示例：`bad-protocol`）
2. deviceCode 不存在时返回业务错误（示例：`missing-device`）

### 3.3 Phase 2 验收（MQTT 上下行最小闭环）

真实环境验收（参考 `docs/test-scenarios.md`）：
1. 基于 `application-dev.yml` 完成 MQTT 标准 Topic、`$dp`、下行发布联调
2. 协议侧安全与兼容回归通过：`MqttDeviceAesDataTests`、`MqttPayloadSecurityValidatorTest`、`MqttBinaryFormatParserTest`、`MqttJsonProtocolAdapterTest`、`MqttPayloadDecryptorRegistryTest`

手工验收（参考 `docs/test-scenarios.md`、`docs/05-protocol.md`）：
1. 启动并开启 MQTT（共享 dev 环境口径详见 `docs/test-scenarios.md`）
2. MQTT 上行：使用 MQTTX 向标准 Topic 发布属性上报，预期属性与日志可用，在线状态刷新
3. MQTT 下行：调用 `POST /message/mqtt/down/publish`，并订阅推荐下行 Topic，预期订阅端收到编码后的 JSON 下行消息
4. `$dp` 兼容：按文档给出的 3 类示例分别上报，预期都能进入统一主链路并产生属性更新与日志

### 3.4 协议与文件类报文验收（C.3/C.4）

1. C.3 文件类报文上报后，`GET /device/{deviceCode}/file-snapshots` 能查询到 Redis 最小快照结果
2. C.4 固件分包报文上报后，`GET /device/{deviceCode}/firmware-aggregates` 能查询到聚合状态，且 `md5Matched` 符合预期

### 3.5 Phase 4 验收（风险预警与处置平台）

进度口径：以 `docs/19-phase4-progress.md` 为准，当前阶段验收优先覆盖“已完成可用”的能力，并明确标注“未交付”的能力不纳入本轮收口。

Phase 4 全量收口目标（参考 `docs/18-phase4-risk-platform-roadmap.md` 的验收标准）：
1. `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 通过
2. 告警中心、事件处置、风险配置、报表分析、系统管理真实环境验收全部通过
3. Phase 4 对外 API 与文档完整更新

告警中心验收（对应表：`iot_alarm_record`）：
1. 列表可按设备、状态、告警等级查询
2. 详情可查询到完整字段
3. 确认、抑制、关闭操作会更新状态与操作人、操作时间字段
4. 核对数据库：状态流转与时间字段落库一致
5. 重点核对项：`docs/19-phase4-progress.md` 标记包含“通知记录”，但当前需以实际系统为准补充验收点

事件处置验收（对应表：`iot_event_record`、`iot_event_work_order`）：
1. 事件列表与详情可查询
2. 派发后生成或更新工单，并可在工单列表中查询
3. 工单接收、开始、完成动作可用，完成时可写入反馈与照片信息
4. 事件关闭后状态变更并记录关闭原因
5. 现场反馈接口可更新事件反馈内容

风险点管理验收（对应表：`risk_point`、`risk_point_device`）：
1. 风险点 CRUD 可用，编码唯一性校验符合预期
2. 绑定弹窗可加载设备选项与设备测点选项
3. 风险点与设备绑定与解绑可用
4. 可查询风险点下绑定设备列表

阈值规则配置验收（对应表：`rule_definition`）：
1. 规则 CRUD 可用
2. 列表可按测点标识、告警等级、状态过滤
3. 重点核对项：`docs/19-phase4-progress.md` 标记包含“规则测试”，但当前需以实际系统为准补充验收点

联动规则与应急预案验收（对应表：`linkage_rule`、`emergency_plan`）：
1. 联动规则 CRUD 可用
2. 应急预案 CRUD 可用
3. 重点核对项：执行联动的运行时编排接口若存在，需要补充验收点与回归用例

分析报表验收：
1. 4 个报表接口可用，返回结构稳定
2. 日期区间参数可选，格式固定为 `YYYY-MM-DD`
3. 风险趋势接口返回 `date`、`alarmCount`、`eventCount`
4. 告警统计接口返回 `total`、`critical`、`high`、`medium`、`low`
5. 事件闭环接口返回 `total`、`closed`、`unclosed`
6. 设备健康接口返回 `onlineRate`、`healthy`、`warning`、`critical`，并可解释为基于 `online_status` 与 `last_report_time` 的当前健康分层

系统管理验收（对应表：`sys_organization`、`sys_region`、`sys_dict`、`sys_audit_log` 及用户角色相关表）：
1. 组织机构支持树结构查询与维护
2. 用户管理支持新增、编辑、禁用、重置密码（若实现）
3. 角色管理支持角色 CRUD 与菜单授权（若实现）
4. 区域、字典可配置且可被业务模块引用
5. 通知渠道与审计日志可查询（接口存在时）

### 3.5.1 Phase 4 真实环境验收前置条件

1. 后端统一以 `application-dev.yml` 启动：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
2. 真实环境数据库除 `sql/init.sql` 外，还应补齐 Phase 4 升级脚本，详见 `docs/03-database.md`
3. 建议验收数据统一使用 `accept-`、`ACCEPT_` 前缀，避免污染历史演示数据
4. 页面验收优先按路由访问真实页面，再同步核对接口返回与数据库结果
5. 以 `docs/19-phase4-progress.md` 为“是否纳入交付范围”的权威口径；本节同时反映当前代码基线的实际可验能力

### 3.5.2 Phase 4 模块动作矩阵

| 模块 | 页面 / 路由 | 关键动作 | 关键接口 | 主要核对表 | 当前通过口径 |
|---|---|---|---|---|---|
| 告警中心 | `/alarm-center` | 列表筛选、详情查看、确认、抑制、关闭 | `/api/alarm/list`、`/api/alarm/{id}`、`/api/alarm/{id}/confirm`、`/api/alarm/{id}/suppress`、`/api/alarm/{id}/close` | `iot_alarm_record` | 状态流转、处理人、处理时间一致 |
| 事件处置 | `/event-disposal` | 列表筛选、详情查看、派发、反馈、关闭、工单接收/开始/完成 | `/api/event/list`、`/api/event/{id}`、`/api/event/{id}/dispatch`、`/api/event/{eventId}/feedback`、`/api/event/{id}/close`、`/api/event/work-orders/*` | `iot_event_record`、`iot_event_work_order` | 事件状态与工单状态联动一致 |
| 风险点管理 | `/risk-point` | 新增、编辑、删除、绑定设备、解绑设备、查看绑定设备 | `/api/risk-point/*`；`/api/device/list`；`/api/device/{deviceId}/metrics` | `risk_point`、`risk_point_device` | 风险点主数据与绑定关系一致，绑定弹窗可加载真实设备与测点选项 |
| 阈值规则 | `/rule-definition` | 新增、编辑、删除、筛选查询 | `/api/rule-definition/*` | `rule_definition` | 规则主数据可增删改查 |
| 联动规则 | `/linkage-rule` | 新增、编辑、删除、筛选查询 | `/api/linkage-rule/*` | `linkage_rule` | 规则配置可增删改查 |
| 应急预案 | `/emergency-plan` | 新增、编辑、删除、筛选查询 | `/api/emergency-plan/*` | `emergency_plan` | 预案主数据可增删改查 |
| 分析报表 | `/report-analysis` | 打开页面、切换时间范围、发起四类查询 | `/api/report/risk-trend`、`/api/report/alarm-statistics`、`/api/report/event-closure`、`/api/report/device-health` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 已按真实业务表聚合；切换时间范围后 KPI 与图表应同步刷新 |
| 组织机构 | `/organization` | 新增、编辑、删除、树查询 | `/api/organization`、`/api/organization/list`、`/api/organization/tree` | `sys_organization` | 树结构与主数据一致 |
| 用户管理 | `/user` | 新增、编辑、删除、重置密码 | `/api/user/*` | `sys_user` | 用户主数据与状态一致 |
| 角色权限 | `/role` | 新增、编辑、删除、按用户查角色 | `/api/role/*` | `sys_role`、`sys_user_role` | 角色主数据与用户角色关系一致 |
| 区域管理 | `/region` | 新增、编辑、删除、树查询 | `/api/region`、`/api/region/list`、`/api/region/tree` | `sys_region` | 区域树与主数据一致 |
| 字典配置 | `/dict` | 字典分类新增、编辑、删除、按编码查询 | `/api/dict/*` | `sys_dict` | 当前只验字典分类；字典项接口尚未补齐到当前后端 |
| 通知渠道 | `/channel` | 新增、编辑、删除、按编码查询 | `/api/system/channel/*` | `sys_notification_channel` | 渠道主数据可增删改查 |
| 审计日志 | `/audit-log` | 列表查询、分页查询、详情查看、删除 | `/api/system/audit-log/*` | `sys_audit_log` | 页面查询结果与表数据一致 |

### 3.5.3 Phase 4 真实环境 SQL 核对模板

说明：
1. 以下 SQL 使用示例编码，实际执行时替换为本次验收使用的编码或 ID。
2. 统一追加 `deleted = 0` 条件，避免被逻辑删除数据干扰。
3. 若真实环境存在历史脏数据，优先按 `create_time desc` 或唯一编码限定本次操作记录。

#### 告警中心

```sql
SELECT id, alarm_code, device_code, status, confirm_user, confirm_time, suppress_user, suppress_time, close_user, close_time, update_time
FROM iot_alarm_record
WHERE alarm_code = 'ACCEPT-ALARM-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 事件处置

```sql
SELECT id, event_code, alarm_code, status, dispatch_user, dispatch_time, close_user, close_reason, update_time
FROM iot_event_record
WHERE event_code = 'ACCEPT-EVENT-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;

SELECT id, event_id, work_order_code, status, receive_user, receive_time, start_time, complete_time, update_time
FROM iot_event_work_order
WHERE event_code = 'ACCEPT-EVENT-001' AND deleted = 0
ORDER BY id DESC;
```

#### 风险点管理

```sql
SELECT id, risk_point_code, risk_point_name, region_id, risk_level, status, update_time
FROM risk_point
WHERE risk_point_code = 'ACCEPT-RP-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;

SELECT id, risk_point_id, device_id, device_code, metric_identifier, metric_name, update_time
FROM risk_point_device
WHERE risk_point_id = 1 AND device_id = 1 AND deleted = 0
ORDER BY id DESC;
```

#### 阈值规则

```sql
SELECT id, rule_name, metric_identifier, duration, alarm_level, convert_to_event, status, update_time
FROM rule_definition
WHERE rule_name = 'ACCEPT-RULE-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 联动规则

```sql
SELECT id, rule_name, trigger_condition, action_list, status, update_time
FROM linkage_rule
WHERE rule_name = 'ACCEPT-LINK-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 应急预案

```sql
SELECT id, plan_name, risk_level, status, update_time
FROM emergency_plan
WHERE plan_name = 'ACCEPT-PLAN-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 组织机构

```sql
SELECT id, org_code, org_name, parent_id, status, sort_no, update_time
FROM sys_organization
WHERE org_code = 'ACCEPT-ORG-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 用户管理

```sql
SELECT id, username, real_name, phone, email, status, update_time
FROM sys_user
WHERE username = 'accept_user_001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 角色权限

```sql
SELECT id, role_code, role_name, status, update_time
FROM sys_role
WHERE role_code = 'ACCEPT_ROLE_001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;

SELECT id, user_id, role_id, create_time
FROM sys_user_role
WHERE user_id = 1 AND role_id = 1 AND deleted = 0
ORDER BY id DESC;
```

#### 区域管理

```sql
SELECT id, region_code, region_name, parent_id, region_type, status, update_time
FROM sys_region
WHERE region_code = 'ACCEPT-REG-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 字典配置

```sql
SELECT id, dict_code, dict_name, dict_type, status, update_time
FROM sys_dict
WHERE dict_code = 'ACCEPT_DICT_001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

当前限制：
1. 字典项表 `sys_dict_item` 已有数据模型，但后端字典项子接口未在当前基线补齐。
2. 因此本轮真实环境验收只覆盖字典分类，不把字典项 CRUD 作为通过项。

#### 通知渠道

```sql
SELECT id, channel_code, channel_name, status, is_default, update_time
FROM sys_notification_channel
WHERE channel_code = 'accept-webhook-001' AND deleted = 0
ORDER BY id DESC
LIMIT 1;
```

#### 审计日志

```sql
SELECT id, operation_module, operation_type, operation_uri, status, user_id, username, create_time
FROM sys_audit_log
WHERE operation_module IN ('organization', 'user', 'role', 'region', 'dict', 'channel')
ORDER BY id DESC
LIMIT 20;
```

#### 分析报表

```sql
SELECT COUNT(*) AS alarm_count, MIN(trigger_time) AS first_alarm_time, MAX(trigger_time) AS last_alarm_time
FROM iot_alarm_record
WHERE deleted = 0;

SELECT COUNT(*) AS event_count, MIN(trigger_time) AS first_event_time, MAX(trigger_time) AS last_event_time
FROM iot_event_record
WHERE deleted = 0;
```

当前限制：
1. 设备健康当前基于 `iot_device.online_status` 与 `last_report_time` 做分层，不等同于后续 Phase 5 可能扩展的遥测健康评分模型。
2. 若真实环境历史数据缺少可解析的 `triggerTime` 且 `createTime` 也不可用，对应记录不会进入趋势统计。

### 3.6 回归保护（必须不回归）

以下验收项必须长期保持可用（参考 `docs/04-api.md` 与 `docs/test-scenarios.md`）：
1. `POST /device/product/add`
2. `GET /device/product/{id}`
3. `POST /device/add`
4. `GET /device/{id}`
5. `GET /device/code/{deviceCode}`
6. `POST /message/http/report`
7. `GET /device/{deviceCode}/properties`
8. `GET /device/{deviceCode}/message-logs`

## 4. 业务验收矩阵

| 业务域 | 功能项 | 当前基线状态 | 主要验收标准 | 验证方式 |
|---|---|---|---|---|
| IoT 基础 | 产品管理 | 已交付 | 可新增、查询产品；协议编码与产品主数据可落库 | API + 数据库核对 |
| IoT 基础 | 设备管理 | 已交付 | 可新增、按 ID/编码查询设备；设备主数据完整 | API + 数据库核对 |
| 数据接入 | HTTP 模拟上报 | 已交付 | 上报成功后写入消息日志、更新属性、刷新在线状态 | 真实环境联调 + 数据库核对 |
| 数据接入 | MQTT 上行接入 | 已交付 | 标准 Topic 与 `$dp` 兼容 Topic 可进入统一主链路 | MQTT 联调 + 数据库核对 |
| 协议能力 | `mqtt-json` 适配与安全校验 | 已交付 | 解码结果统一、签名/时间戳/nonce 校验符合文档约束 | 单测 + 协议联调 |
| 平台闭环 | 指令闭环 | 已交付 | 指令记录、状态流转、回执接入可用 | 真实环境联调 + 日志核对 |
| 平台闭环 | 网关/子设备拓扑 | 已交付 | 静态拓扑可维护，子设备上报/下发链路可用 | 真实环境联调 + 主题联调 |
| 平台闭环 | 规则引擎基础版 | 已交付 | 单条件单动作规则可配置、触发、记录结果 | 真实环境联调 + 规则结果核对 |
| 风险平台 | 告警中心 | 已交付 | 列表、详情、确认、抑制、关闭可用；状态与操作人落库一致 | API + 数据库核对 |
| 风险平台 | 事件处置 | 已交付 | 列表、详情、派发、反馈、关闭、工单流转可用 | API + 数据库核对 |
| 风险平台 | 风险点管理 | 已交付 | 风险点 CRUD、设备绑定/解绑、绑定设备查询可用 | API + 数据库核对 |
| 风险平台 | 阈值规则配置 | 已交付 | 规则 CRUD 可用；筛选查询符合业务字段口径 | API + 业务回归 |
| 风险平台 | 联动规则与应急预案 | 已交付 | 联动规则 CRUD、应急预案 CRUD 可用 | API + 页面回归 |
| 风险平台 | 分析报表 | 已交付 | 风险趋势、告警统计、事件闭环、设备健康 4 类报表接口稳定 | API + 页面回归 |
| 系统管理 | 组织、用户、角色、区域、字典、通知渠道、审计日志 | 已交付 | 基础 CRUD/列表/树形/查询能力与代码映射一致 | API + 页面回归 |
| 风险平台增强 | 实时监测、GIS 地图、驾驶舱增强 | 风险监测代码完成待验收 | 实时监测与 GIS 页面、接口已进入代码基线，待真实环境验收后纳入交付；驾驶舱增强仍不纳入本轮验收 | 分阶段纳入 |

使用说明：
1. “当前基线状态”以 `docs/19-phase4-progress.md` 为准。
2. “已交付”表示当前版本应纳入本轮验收；“未交付”表示只能作为规划项跟踪，不能按已完成收口。
3. 验收结论建议按“通过 / 不通过 / 待确认”三态记录到项目周报或验收纪要。

## 5. 模块-页面-接口-数据表四维对照表

说明：
1. 本节按“当前代码基线”整理，口径不等同于“已交付范围”。
2. `docs/04-api.md` 主要收口当前纳入验收的接口；本节额外补充代码中已存在但未完全交付、未完全接线或仍为演示态的页面与接口。
3. “主要数据表/存储”列写的是当前代码直接依赖的核心持久化对象；若当前实现为静态演示数据或 Redis 临时存储，也会明确标注。

### 5.1 已接入真实后端或存储的页面

| 模块 | 页面（路由 / 视图） | 前端 API 文件 | 后端接口（主路径） | 主要数据表 / 存储 | 当前状态 |
|---|---|---|---|---|---|
| IoT 基础 | `/products` / `ProductWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /device/product/add`；`GET /device/product/{id}` | `iot_product`、`iot_product_model` | 已接真实后端，支撑产品新增与查询 |
| IoT 基础 | `/devices` / `DeviceWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /device/add`；`GET /device/{id}`；`GET /device/code/{deviceCode}` | `iot_device` | 已接真实后端，支撑设备建档与查询 |
| 数据接入 | `/reporting` / `ReportWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /message/http/report` | `iot_message_log`、`iot_device_property`、`iot_device` | 已接真实后端，走 HTTP 上报主链路 |
| 数据洞察 | `/insight` / `DeviceInsightView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `GET /device/code/{deviceCode}`；`GET /device/{deviceCode}/properties`；`GET /device/{deviceCode}/message-logs` | `iot_device`、`iot_device_property`、`iot_message_log` | 已接真实后端；当前仍是“设备洞察页”，不是风险监测独立页 |
| 文件调试 | `/file-debug` / `FilePayloadDebugView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `GET /device/{deviceCode}/file-snapshots`；`GET /device/{deviceCode}/firmware-aggregates` | `Redis(iot:device:file:*)`、`Redis(iot:device:firmware:*)` | 已接真实后端；当前文件快照与固件聚合先落 Redis |
| 告警中心 | `/alarm-center` / `AlarmCenterView.vue` | `spring-boot-iot-ui/src/api/alarm.ts` | `/api/alarm/list`；`/api/alarm/{id}`；`/api/alarm/{id}/confirm`；`/api/alarm/{id}/suppress`；`/api/alarm/{id}/close` | `iot_alarm_record` | 已接真实后端；前后端已兼容 JSON Body / Query 两种传参 |
| 事件处置 | `/event-disposal` / `EventDisposalView.vue` | `spring-boot-iot-ui/src/api/alarm.ts` | `/api/event/list`；`/api/event/{id}`；`/api/event/{id}/dispatch`；`/api/event/{id}/close`；`/api/event/{eventId}/feedback`；`/api/event/work-orders/*` | `iot_event_record`、`iot_event_work_order` | 已接真实后端；工单流转与反馈链路已接入 |
| 风险点管理 | `/risk-point` / `RiskPointView.vue` | `spring-boot-iot-ui/src/api/riskPoint.ts`；`spring-boot-iot-ui/src/api/iot.ts` | `/api/risk-point/list`；`/api/risk-point/get/{id}`；`/api/risk-point/add`；`/api/risk-point/update`；`/api/risk-point/delete/{id}`；`/api/risk-point/bind-device`；`/api/risk-point/unbind-device`；`/api/risk-point/bound-devices/{riskPointId}`；`/api/device/list`；`/api/device/{deviceId}/metrics` | `risk_point`、`risk_point_device`、`iot_device`、`iot_product_model`、`iot_device_property` | 已按后端真实路径对齐前端调用；绑定弹窗依赖设备选项与测点选项接口 |
| 阈值规则 | `/rule-definition` / `RuleDefinitionView.vue` | `spring-boot-iot-ui/src/api/ruleDefinition.ts` | `/api/rule-definition/list`；`/api/rule-definition/get/{id}`；`/api/rule-definition/add`；`/api/rule-definition/update`；`/api/rule-definition/delete/{id}` | `rule_definition` | 已按后端真实路径对齐前端调用 |
| 联动规则 | `/linkage-rule` / `LinkageRuleView.vue` | `spring-boot-iot-ui/src/api/linkageRule.ts` | `/api/linkage-rule/list`；`/api/linkage-rule/get/{id}`；`/api/linkage-rule/add`；`/api/linkage-rule/update`；`/api/linkage-rule/delete/{id}` | `linkage_rule` | 已按后端真实路径对齐前端调用 |
| 应急预案 | `/emergency-plan` / `EmergencyPlanView.vue` | `spring-boot-iot-ui/src/api/emergencyPlan.ts` | `/api/emergency-plan/list`；`/api/emergency-plan/get/{id}`；`/api/emergency-plan/add`；`/api/emergency-plan/update`；`/api/emergency-plan/delete/{id}` | `emergency_plan` | 已按后端真实路径对齐前端调用 |
| 组织管理 | `/organization` / `OrganizationView.vue` | `spring-boot-iot-ui/src/api/organization.ts` | `/api/organization`；`/api/organization/list`；`/api/organization/tree`；`/api/organization/{id}` | `sys_organization` | 已接真实后端；树形维护能力可用 |
| 用户管理 | `/user` / `UserView.vue` | `spring-boot-iot-ui/src/api/user.ts` | `/api/user/list`；`/api/user/{id}`；`/api/user/add`；`/api/user/update`；`/api/user/{id}`；`/api/user/reset-password/{userId}` | `sys_user` | 已按后端真实路径对齐前端新增/更新调用 |
| 角色管理 | `/role` / `RoleView.vue` | `spring-boot-iot-ui/src/api/role.ts` | `/api/role/add`；`/api/role/list`；`/api/role/{id}`；`/api/role/update`；`/api/role/user/{userId}` | `sys_role`、`sys_user_role` | 已接真实后端；角色与用户角色查询已接入 |
| 区域管理 | `/region` / `RegionView.vue` | `spring-boot-iot-ui/src/api/region.ts` | `/api/region`；`/api/region/list`；`/api/region/tree`；`/api/region/{id}` | `sys_region` | 已接真实后端；树形区域维护能力可用 |
| 字典配置 | `/dict` / `DictView.vue` | `spring-boot-iot-ui/src/api/dict.ts` | `/api/dict`；`/api/dict/list`；`/api/dict/tree`；`/api/dict/{id}`；`/api/dict/code/{dictCode}` | `sys_dict`、`sys_dict_item` | 主字典 CRUD 已接后端；字典项子接口前端已写、后端未见对应 Controller |
| 通知渠道 | `/channel` / `ChannelView.vue` | `spring-boot-iot-ui/src/api/channel.ts` | `/api/system/channel/list`；`/api/system/channel/getByCode/{channelCode}`；`/api/system/channel/add`；`/api/system/channel/update`；`/api/system/channel/delete/{id}` | `sys_notification_channel` | 已接真实后端 |
| 审计日志 | `/audit-log` / `AuditLogView.vue` | `spring-boot-iot-ui/src/api/auditLog.ts` | `/api/system/audit-log/list`；`/api/system/audit-log/page`；`/api/system/audit-log/get/{id}`；`/api/system/audit-log/delete/{id}` | `sys_audit_log` | 已接真实后端 |

### 5.2 已有页面或接口，但当前仍待对齐 / 待补完 / 演示态

| 模块 | 页面（路由 / 视图） | 前端 API 文件 | 后端接口（主路径） | 主要数据表 / 存储 | 当前状态 |
|---|---|---|---|---|---|
| 驾驶舱 | `/` / `CockpitView.vue` | `spring-boot-iot-ui/src/api/cockpit.ts` | `/api/cockpit/data`；`/api/cockpit/trend`；`/api/cockpit/distribution`；`/api/cockpit/warnings`；`/api/cockpit/activities` | 无固定表；当前由 `CockpitServiceImpl` 静态组装 | 页面与接口已接通，但当前是演示数据，不是基于业务表的统计结果 |
| 报表分析 | `/report-analysis` / `ReportAnalysisView.vue` | `spring-boot-iot-ui/src/api/report.ts` | `/api/report/risk-trend`；`/api/report/alarm-statistics`；`/api/report/event-closure`；`/api/report/device-health` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 前端已对齐真实接口；后端已按业务表完成聚合，日期筛选参数固定为 `YYYY-MM-DD` |
| ?????? | `/risk-monitoring` / `RealTimeMonitoringView.vue`?`/risk-monitoring-gis` / `RiskGisView.vue` | `spring-boot-iot-ui/src/api/riskMonitoring.ts` | `/api/risk-monitoring/realtime/list`?`/api/risk-monitoring/realtime/{bindingId}`?`/api/risk-monitoring/gis/points` | `risk_point`?`risk_point_device`?`iot_alarm_record`?`iot_event_record`?`iot_device`?`iot_device_property`?`iot_message_log`?`iot_product` | ???????????????????????2026-03-16 ???????????? `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql` ?????GIS ??? ECharts ????? |
| 规划展示 | `/future-lab` / `FutureLabView.vue` | 无 | 无 | 无 | 纯规划展示页，不连接后端接口与数据表 |

## 6. 验收输出物建议

1. 自动化：CI 或本地执行日志，至少包含构建结果与关键测试结果
2. 手工：HTTP 联调记录、MQTTX 上下行截图或录屏、关键表落库截图
3. 结论：按本清单逐项标注“通过/不通过/待确认”，并对差异项给出整改结论与负责人



## 认证与权限验收基线（2026-03-16）

### 验收范围
- 登录能力：`POST /api/auth/login`
- 会话识别：`GET /api/auth/me`
- 受保护接口拦截：设备管理与系统管理 API

### 验收标准
- 使用有效账号登录可获得 JWT token
- 未携带 token 访问受保护接口返回 `401`
- 携带 token 访问同一接口返回非 `401`
- 前端登录后自动携带 `Authorization: Bearer <token>`
- 前端收到 `401` 时自动清理本地登录态并要求重新登录
