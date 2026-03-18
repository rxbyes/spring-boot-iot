# spring-boot-iot 业务功能清单与验收标准

更新时间：2026-03-18

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
- spring-boot-iot-system：系统管理（用户、角色、菜单、组织、区域、字典、审计、通知渠道等）
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
1. `POST /api/device/product/add`，新增产品
2. `GET /api/device/product/{id}`，查询产品
3. `POST /api/device/add`，新增设备
4. `GET /api/device/{id}`，查询设备
5. `GET /api/device/code/{deviceCode}`，按编码查询设备
6. `POST /message/http/report`，HTTP 模拟上报入口
7. `GET /api/device/{deviceCode}/properties`，查询最新属性
8. `GET /api/device/{deviceCode}/message-logs`，查询消息日志

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
1. `GET /api/device/{deviceCode}/file-snapshots`，查看 C.3 文件快照 Redis 最小持久化结果
2. `GET /api/device/{deviceCode}/firmware-aggregates`，查看 C.4 固件分包 Redis 聚合结果与 MD5 校验

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
2. 当前未计入已交付：风险监测实时监测与 GIS 地图已进入代码基线但待真实环境验收；首页已完成商业化重构并进一步升级为“数据看板驾驶舱 + 事务工作台入口”双层结构，支持一线/运维/管理/研发四类角色视角，但当前仍采用静态能力编排与本地 activity 记录，不纳入真实环境验收；设备中心增强仍未交付
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
6. 风险点分页列表：`GET /api/risk-point/page`
7. 绑定设备：`POST /api/risk-point/bind-device`
8. 解绑设备：`POST /api/risk-point/unbind-device`
9. 已绑定设备列表：`GET /api/risk-point/bound-devices/{riskPointId}`
10. 绑定弹窗设备选项：`GET /api/device/list`
11. 绑定弹窗测点选项：`GET /api/device/{deviceId}/metrics`

阈值规则配置（后端接口前缀：`/api/rule-definition`，以代码为准）：
1. 规则列表：`GET /api/rule-definition/list`
2. 规则分页列表：`GET /api/rule-definition/page`
3. 规则详情：`GET /api/rule-definition/get/{id}`
4. 新增规则：`POST /api/rule-definition/add`
5. 更新规则：`POST /api/rule-definition/update`
6. 删除规则：`POST /api/rule-definition/delete/{id}`

联动规则（后端接口前缀：`/api/linkage-rule`，以代码为准）：
1. 规则列表：`GET /api/linkage-rule/list`
2. 规则分页列表：`GET /api/linkage-rule/page`
3. 规则详情：`GET /api/linkage-rule/get/{id}`
4. 新增规则：`POST /api/linkage-rule/add`
5. 更新规则：`POST /api/linkage-rule/update`
6. 删除规则：`POST /api/linkage-rule/delete/{id}`

应急预案（后端接口前缀：`/api/emergency-plan`，以代码为准）：
1. 预案列表：`GET /api/emergency-plan/list`
2. 预案分页列表：`GET /api/emergency-plan/page`
3. 预案详情：`GET /api/emergency-plan/get/{id}`
4. 新增预案：`POST /api/emergency-plan/add`
5. 更新预案：`POST /api/emergency-plan/update`
6. 删除预案：`POST /api/emergency-plan/delete/{id}`

分析报表（后端接口前缀：`/api/report`，以代码为准）：
1. 风险趋势：`GET /api/report/risk-trend`
2. 告警统计：`GET /api/report/alarm-statistics`
3. 事件闭环：`GET /api/report/event-closure`
4. 设备健康：`GET /api/report/device-health`

系统管理（后端接口前缀按各 Controller 定义，以代码为准）：
1. 组织机构：`/api/organization`
2. 用户管理：`/api/user`
3. 角色管理：`/api/role`
4. 菜单管理：`/api/menu`
5. 区域管理：`/api/region`
6. 字典配置：`/api/dict`
7. 通知渠道：`/api/system/channel`
8. 业务日志：`/api/system/audit-log`（前端业务日志页默认排除 `system_error`）
9. 系统日志：设备接入分区复用 `/api/system/audit-log`，固定查看 `operation_type=system_error`
10. 消息追踪：`/api/device/message-trace/page`，支持按 `TraceId`、设备编码、产品标识、消息类型、Topic 分页检索接入消息日志
11. 前端列表交互基线：组织、用户、角色、区域、字典、通知渠道、菜单、业务日志，以及告警中心、事件处置、风险点管理、阈值规则、联动规则、应急预案、实时监测、GIS 风险态势统一采用“KPI 概览卡 + 筛选卡 + 列表卡 / 资源卡”工作台结构；其中列表页支持“已选项计数 / 清空选中 / 刷新列表”等操作栏，系统治理列表默认对超长单元格内容执行单行省略并在悬停时展示完整值，不再在列表内自动换行，GIS 未定位对象使用资源卡收拢展示
12. 前端列表导出基线：上述页面均支持“导出选中”与“导出当前结果”CSV（本地导出，不依赖新增后端接口），并包含中文列头与关键状态文案映射
13. 前端导出设置基线：支持“导出列设置”（列勾选 + 顺序调整），导出配置按页面本地持久化
14. 前端导出模板基线：支持默认/运维/管理导出模板，快速切换导出列配置
15. 前端自定义导出模板基线：支持保存模板与删除模板，模板按页面本地持久化
16. 前端模板共享基线：支持模板 JSON 导入/导出，实现跨账号或跨环境复用
17. 前端模板管理增强基线：支持模板重命名与分组标签筛选
18. 前端模板效率增强基线：支持模板搜索、最近使用置顶，并展示最近使用时间与最后修改时间
19. 前端模板导入预览明细基线：支持在导入确认前查看新增/覆盖/重命名/跳过完整列表，并支持在明细抽屉直接确认导入
20. 前端详情交互基线：实时监测 / GIS、告警中心、事件处置、业务日志、系统日志统一使用右侧详情抽屉；抽屉需支持统一标题区、状态标签、分组信息展示，以及加载 / 空态 / 错误态反馈；其中业务日志、系统日志、消息追踪详情默认采用“概览卡片 + 链路信息 + 深色报文块 + 结果提示卡”的日志型展示结构，告警中心、事件处置、风险监测详情默认采用“概览卡片 + 业务分区 + 说明提示卡”的业务型展示结构，且概览卡、字段卡、提示卡、最近记录卡统一保持浅色控制台式轻卡片风格
21. 前端表单交互基线：用户、角色、菜单、组织、区域、字典、通知渠道，以及风险点、阈值规则、联动规则、应急预案的新增 / 编辑统一使用右侧表单抽屉；风险点绑定设备、事件处置的派发 / 关闭、字典项管理及其新增 / 编辑也统一使用抽屉；自动化测试中心的导入计划 / 新增自定义页面、公共导出列设置及其模板命名 / 导入冲突处理、账号中心 / 修改密码等工具型交互也统一收口为抽屉；抽屉需保留现有表单校验、提交按钮与关闭重置逻辑，其中风险点、阈值规则、联动规则、应急预案的抽屉内部统一采用“提示卡 + 分区卡 + 栅格字段”的表单内容结构
22. 前端分页与配色基线：2026-03-18 起告警中心、事件处置、风险点管理、阈值规则、联动规则、应急预案，以及组织、用户、角色、菜单、区域、字典、通知渠道、业务日志、系统日志、消息追踪、实时监测统一使用 `StandardPagination` + `useServerPagination` 分页契约（其中告警中心、事件处置当前沿用后端 `/list` + 前端本地切片分页）；同时 `tokens.css` 与 `element-overrides.css` 的主色统一收敛到 `--brand`，壳层导航与头部高亮由 token 变量驱动

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
3. 新增产品：调用 `POST /api/device/product/add`，返回 `code=200` 且返回产品主键
4. 新增设备：调用 `POST /api/device/add`，返回 `code=200`
5. 发送 HTTP 上报：调用 `POST /message/http/report`，返回 `code=200`
6. 查询属性：`GET /api/device/{deviceCode}/properties`，能返回上报的属性键值
7. 查询日志：`GET /api/device/{deviceCode}/message-logs`，能返回至少 1 条日志，topic 与 payload 正确
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

1. C.3 文件类报文上报后，`GET /api/device/{deviceCode}/file-snapshots` 能查询到 Redis 最小快照结果
2. C.4 固件分包报文上报后，`GET /api/device/{deviceCode}/firmware-aggregates` 能查询到聚合状态，且 `md5Matched` 符合预期

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
2. 用户管理支持新增、编辑、禁用、重置密码、用户角色分配
3. 角色管理支持角色 CRUD、菜单授权、按钮权限授权
4. 菜单管理支持菜单树查看、菜单 CRUD、父子关系维护
5. 区域、字典可配置且可被业务模块引用
6. 通知渠道可查询，业务日志支持 HTTP 自动采集，系统日志支持 MQTT 等后台异常事件入库并可查询
7. 日志详情可查看请求参数与响应摘要，敏感字段需自动脱敏，超长内容应有截断标记；列表与详情链路中的雪花 `id` 应保持字符串语义，前端请求层需在 `JSON.parse` 前兜底保留 `id/*Id` 超大整数，避免因精度丢失打开空详情或误报“记录不存在”
8. 系统日志支持按 `TraceId`、设备编码、产品标识、异常编码、异常类型排查，并可直接跳转消息追踪页
9. 消息追踪页支持按 `TraceId` / Topic / 设备编码检索 `iot_message_log`，并可回跳系统日志页完成双向联查
10. 业务日志与系统日志均支持按 `operationResult`（成功/失败）筛选，并在页面展示统计概览：业务侧聚合总量/今日/成功失败/活跃用户，系统侧聚合异常总量/今日/MQTT/链路数/高频模块

### 3.5.0 系统管理动态权限基线

权限模型：
1. 菜单与按钮权限统一来自 MySQL：`sys_menu`、`sys_role_menu`、`sys_user_role`
2. `sys_menu.type = 2` 代表按钮权限，`menu_code` 作为前端与后端共享的操作权限码
3. 登录接口与 `/api/auth/me` 返回 `authContext`，前端据此恢复角色信息、账号资料与按钮权限；公共壳层导航优先由 `authContext.menus` 动态构建

默认角色分权基线：

| 角色名称 | 角色编码 | 默认授权范围 | 说明 |
|---|---|---|---|
| 业务人员 | `BUSINESS_STAFF` | 告警中心、事件处置、风险点管理、分析报表 | 面向日常业务监测、处置与复盘 |
| 管理人员 | `MANAGEMENT_STAFF` | 预警处置全链路 + 系统治理主菜单 + 用户/角色部分按钮权限 | 面向业务统筹、规则审批、组织治理 |
| 运维人员 | `OPS_STAFF` | 设备接入、设备运维、接入回放、系统日志、数据校验、告警/事件/风险点 | 面向设备接入与运行维护 |
| 开发人员 | `DEVELOPER_STAFF` | 设备接入全链路（含系统日志）+ 规则/预案 + 风险监测增强页 | 面向联调、验证、缺陷定位 |
| 超级管理人员 | `SUPER_ADMIN` | 全部菜单、全部按钮权限 | 拥有系统最高权限 |

菜单信息表（`sys_menu`，页面级菜单）：

| 菜单ID | 父ID | 菜单名称 | 菜单编码 | 路由 |
|---|---|---|---|---|
| 93000001 | 0 | 设备接入 | `iot-access` | - |
| 93000002 | 0 | 预警处置 | `risk-ops` | - |
| 93000003 | 0 | 系统治理 | `system-governance` | - |
| 93001001 | 93000001 | 产品模板中心 | `iot:products` | `/products` |
| 93001002 | 93000001 | 设备运维中心 | `iot:devices` | `/devices` |
| 93001003 | 93000001 | 接入回放台 | `iot:reporting` | `/reporting` |
| 93001004 | 93000001 | 风险点工作台 | `iot:insight` | `/insight` |
| 93001005 | 93000001 | 文件与固件校验 | `iot:file-debug` | `/file-debug` |
| 93001006 | 93000001 | 系统日志 | `iot:system-log` | `/system-log` |
| 93001007 | 93000001 | 消息追踪 | `iot:message-trace` | `/message-trace` |
| 93002001 | 93000002 | 告警中心 | `risk:alarm` | `/alarm-center` |
| 93002002 | 93000002 | 事件处置 | `risk:event` | `/event-disposal` |
| 93002003 | 93000002 | 风险点管理 | `risk:point` | `/risk-point` |
| 93002004 | 93000002 | 阈值规则配置 | `risk:rule-definition` | `/rule-definition` |
| 93002005 | 93000002 | 联动规则 | `risk:linkage-rule` | `/linkage-rule` |
| 93002006 | 93000002 | 应急预案 | `risk:emergency-plan` | `/emergency-plan` |
| 93002007 | 93000002 | 分析报表 | `risk:report` | `/report-analysis` |
| 93002008 | 93000002 | 实时监测 | `risk:monitoring` | `/risk-monitoring` |
| 93002009 | 93000002 | GIS 风险态势 | `risk:monitoring-gis` | `/risk-monitoring-gis` |
| 93003001 | 93000003 | 组织机构 | `system:organization` | `/organization` |
| 93003002 | 93000003 | 用户管理 | `system:user` | `/user` |
| 93003003 | 93000003 | 角色管理 | `system:role` | `/role` |
| 93003004 | 93000003 | 区域管理 | `system:region` | `/region` |
| 93003005 | 93000003 | 字典配置 | `system:dict` | `/dict` |
| 93003006 | 93000003 | 通知渠道 | `system:channel` | `/channel` |
| 93003007 | 93000003 | 业务日志 | `system:audit` | `/audit-log` |
| 93003008 | 93000003 | 菜单管理 | `system:menu` | `/menu` |
| 93003009 | 93000003 | 自动化测试 | `system:automation-test` | `/automation-test` |

说明：
1. 按钮权限同样落在 `sys_menu`，类型为 `type=2`（如 `system:user:add`、`system:role:update`、`system:menu:delete`），通过 `sys_role_menu` 绑定到角色。
2. 前端一级/二级导航优先使用登录返回的 `authContext.menus` 动态渲染；仅在菜单树为空时启用临时静态兜底。
3. 用户管理、角色管理、菜单管理页的新增 / 编辑 / 删除 / 重置密码按钮统一通过 `v-permission` 按 `authContext.permissions` 收口；菜单页“前往角色授权”入口额外要求 `system:role:update`。

### 3.5.1 Phase 4 真实环境验收前置条件

1. 后端统一以 `application-dev.yml` 启动：`mvn -s .mvn/settings.xml -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev`
2. 真实环境数据库除 `sql/init.sql` 外，还应补齐 Phase 4 升级脚本，详见 `docs/03-database.md`
3. 建议验收数据统一使用 `accept-`、`ACCEPT_` 前缀，避免污染历史演示数据
4. 页面验收优先按路由访问真实页面，再同步核对接口返回与数据库结果
5. 以 `docs/19-phase4-progress.md` 为“是否纳入交付范围”的权威口径；本节同时反映当前代码基线的实际可验能力

### 3.5.2 Phase 4 模块动作矩阵

| 模块 | 页面 / 路由 | 关键动作 | 关键接口 | 主要核对表 | 当前通过口径 |
|---|---|---|---|---|---|
| 告警中心 | `/alarm-center` | 列表筛选、详情查看、确认、抑制、关闭 | `/api/alarm/list`、`/api/alarm/{id}`、`/api/alarm/{id}/confirm`、`/api/alarm/{id}/suppress`、`/api/alarm/{id}/close` | `iot_alarm_record` | 状态流转、处理人、处理时间一致；详情统一走右侧抽屉 |
| 事件处置 | `/event-disposal` | 列表筛选、详情查看、派发、反馈、关闭、工单接收/开始/完成 | `/api/event/list`、`/api/event/{id}`、`/api/event/{id}/dispatch`、`/api/event/{eventId}/feedback`、`/api/event/{id}/close`、`/api/event/work-orders/*` | `iot_event_record`、`iot_event_work_order` | 事件状态与工单状态联动一致；详情、派发、关闭统一走右侧抽屉 |
| 风险点管理 | `/risk-point` | 新增、编辑、删除、绑定设备、解绑设备、查看绑定设备 | `/api/risk-point/*`；`/api/device/list`；`/api/device/{deviceId}/metrics` | `risk_point`、`risk_point_device` | 风险点主数据与绑定关系一致，绑定弹窗可加载真实设备与测点选项 |
| 阈值规则 | `/rule-definition` | 新增、编辑、删除、筛选查询 | `/api/rule-definition/*` | `rule_definition` | 规则主数据可增删改查 |
| 联动规则 | `/linkage-rule` | 新增、编辑、删除、筛选查询 | `/api/linkage-rule/*` | `linkage_rule` | 规则配置可增删改查 |
| 应急预案 | `/emergency-plan` | 新增、编辑、删除、筛选查询 | `/api/emergency-plan/*` | `emergency_plan` | 预案主数据可增删改查 |
| 分析报表 | `/report-analysis` | 打开页面、切换时间范围、发起四类查询 | `/api/report/risk-trend`、`/api/report/alarm-statistics`、`/api/report/event-closure`、`/api/report/device-health` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 已按真实业务表聚合；切换时间范围后 KPI 与图表应同步刷新 |
| 组织机构 | `/organization` | 新增、编辑、删除、树查询 | `/api/organization`、`/api/organization/list`、`/api/organization/tree` | `sys_organization` | 树结构与主数据一致 |
| 用户管理 | `/user` | 新增、编辑、删除、重置密码、分配角色 | `/api/user/*` | `sys_user`、`sys_user_role` | 用户主数据、状态与角色关系一致 |
| 角色权限 | `/role` | 新增、编辑、删除、菜单授权、按用户查角色 | `/api/role/*`、`/api/menu/tree` | `sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu` | 角色主数据、菜单关系与用户角色关系一致；角色页可直接勾选页面/按钮权限并刷新菜单树 |
| 菜单管理 | `/menu` | 菜单树查看、新增、编辑、删除 | `/api/menu/tree`、`/api/menu/list`、`/api/menu/{id}`、`/api/menu/add`、`/api/menu/update`、`/api/menu/{id}` | `sys_menu`、`sys_role_menu` | 菜单主数据、父子层级与角色引用约束一致；菜单页提供跳转角色授权入口 |
| 区域管理 | `/region` | 新增、编辑、删除、树查询 | `/api/region`、`/api/region/list`、`/api/region/tree` | `sys_region` | 区域树与主数据一致 |
| 字典配置 | `/dict` | 字典分类新增、编辑、删除、按编码查询 | `/api/dict/*` | `sys_dict` | 前端字典分类与字典项管理统一走右侧抽屉；真实环境当前仍只验字典分类，字典项接口是否可用以当前后端为准 |
| 通知渠道 | `/channel` | 新增、编辑、删除、按编码查询、测试通知 | `/api/system/channel/*`（含 `POST /api/system/channel/test/{channelCode}`） | `sys_notification_channel` | 渠道主数据可增删改查；支持 `system_error` 场景通知配置与测试通知 |
| 业务日志 | `/audit-log` | 自动采集接口访问审计、列表查询、分页查询、详情查看、删除、统计概览 | `/api/system/audit-log/*`（其中业务日志页默认传 `excludeSystemError=true`）；`GET /api/system/audit-log/business/stats` | `sys_audit_log` | 执行系统 API 后应新增业务操作审计，页面默认不混入 `system_error` 后台异常；支持按 `operationResult` 筛选；详情统一走右侧抽屉 |
| 系统日志 | `/system-log` | 后台异常事件查询、列表查询、分页查询、详情查看、删除、跳转消息追踪、统计概览 | `/api/system/audit-log/*`（其中系统日志页固定传 `operationType=system_error`）；`GET /api/system/audit-log/system-error/stats` | `sys_audit_log` | MQTT / 后台异步异常时应新增 `system_error` 记录，页面查询结果与表数据一致；支持按 `TraceId`、设备编码、产品标识、异常编码、异常类型与 `operationResult` 排查 |
| 消息追踪 | `/message-trace` / `MessageTraceView.vue` | 按 `TraceId`、设备编码、产品标识、消息类型、Topic 分页查询，查看 payload 详情，回跳系统日志 | `GET /api/device/message-trace/page` | `iot_message_log` | 已接真实后端；支持把系统日志中的异常记录与原始消息日志串联排障 |

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

说明：
- `webhook`、`wechat`、`feishu`、`dingtalk` 渠道支持 `POST /api/system/channel/test/{channelCode}` 手工测试通知。
- 自动系统异常通知要求渠道 `config` JSON 中存在 `url`，且 `scenes` 或 `scene` 包含 `system_error`。

#### 审计日志

```sql
SELECT id, operation_module, operation_type, request_url, request_method, user_name, operation_result, result_message, operation_time
FROM sys_audit_log
WHERE operation_module IN ('organization', 'user', 'role', 'region', 'dict', 'channel', 'message.mqtt')
ORDER BY id DESC
LIMIT 20;
```

说明：
- HTTP 业务失败即使返回 `HTTP 200`，只要统一响应体中的 `code != 200`，也应在审计日志中体现为失败。
- MQTT 启动失败、订阅失败、连接断开、消息分发失败等异步异常应以 `operation_type=system_error`、`request_method=MQTT`、`user_name=SYSTEM` 写入 `sys_audit_log`。
- 历史共享库若尚未补齐 `sys_audit_log.trace_id/device_code/product_key/error_code/exception_class`，执行角色管理、菜单管理、用户管理等系统治理新增/更新时，也不应再因审计表缺列导致主业务失败；业务日志页与系统日志页至少要能完成列表、分页、详情与删除。
- 若目标库仍使用旧字段 `log_type` / `operation_uri`，后端查询应自动兼容；如需完整的 Trace 检索与异常定位能力，仍需执行 `20260316_phase4_real_env_schema_alignment.sql`。
- 当 `iot.observability.system-error-notify-enabled=true` 且存在匹配 `system_error` 场景的通知渠道时，上述后台异常还应触发通知发送；通知失败只记录应用日志，不反向生成新的审计日志。

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
1. `POST /api/device/product/add`
2. `GET /api/device/product/{id}`
3. `POST /api/device/add`
4. `GET /api/device/{id}`
5. `GET /api/device/code/{deviceCode}`
6. `POST /message/http/report`
7. `GET /api/device/{deviceCode}/properties`
8. `GET /api/device/{deviceCode}/message-logs`

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
| 系统管理 | 组织、用户、角色、菜单、区域、字典、通知渠道、业务日志 | 已交付 | 基础 CRUD/列表/树形/查询能力与代码映射一致 | API + 页面回归 |
| 风险平台增强 | 实时监测、GIS 地图、首页商业化重构 | 风险监测代码完成待验收 | 实时监测与 GIS 页面、接口已进入代码基线，待真实环境验收后纳入交付；首页商业化重构已完成，但当前不以真实业务表聚合统计作为验收项 | 分阶段纳入 |

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
| IoT 基础 | `/products` / `ProductWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /api/device/product/add`；`GET /api/device/product/{id}` | `iot_product`、`iot_product_model` | 已接真实后端，支撑产品新增与查询 |
| IoT 基础 | `/devices` / `DeviceWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /api/device/add`；`GET /api/device/{id}`；`GET /api/device/code/{deviceCode}` | `iot_device` | 已接真实后端，支撑设备建档与查询 |
| 数据接入 | `/reporting` / `ReportWorkbenchView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `POST /message/http/report` | `iot_message_log`、`iot_device_property`、`iot_device` | 已接真实后端，当前以前端“接入验证中心”方式承接 HTTP 上报验证主链路 |
| 数据洞察 | `/insight` / `DeviceInsightView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `GET /api/device/code/{deviceCode}`；`GET /api/device/{deviceCode}/properties`；`GET /api/device/{deviceCode}/message-logs` | `iot_device`、`iot_device_property`、`iot_message_log` | 已接真实后端；当前以“监测对象工作台”方式组织属性、日志与研判线索，不是风险监测独立页 |
| 文件调试 | `/file-debug` / `FilePayloadDebugView.vue` | `spring-boot-iot-ui/src/api/iot.ts` | `GET /api/device/{deviceCode}/file-snapshots`；`GET /api/device/{deviceCode}/firmware-aggregates` | `Redis(iot:device:file:*)`、`Redis(iot:device:firmware:*)` | 已接真实后端；当前以前端“数据完整性校验”方式展示文件快照与固件聚合结果 |
| 告警中心 | `/alarm-center` / `AlarmCenterView.vue` | `spring-boot-iot-ui/src/api/alarm.ts` | `/api/alarm/list`；`/api/alarm/{id}`；`/api/alarm/{id}/confirm`；`/api/alarm/{id}/suppress`；`/api/alarm/{id}/close` | `iot_alarm_record` | 已接真实后端；告警操作参数按后端 `@RequestParam` 使用 query 传参（`confirmUser`/`suppressUser`/`closeUser`）；2026-03-18 起列表分页统一为 `StandardPagination + useServerPagination`（当前接口仍为 `/list`，前端按本地切片分页） |
| 事件处置 | `/event-disposal` / `EventDisposalView.vue` | `spring-boot-iot-ui/src/api/alarm.ts` | `/api/event/list`；`/api/event/{id}`；`/api/event/{id}/dispatch`；`/api/event/{id}/close`；`/api/event/{eventId}/feedback`；`/api/event/work-orders/*` | `iot_event_record`、`iot_event_work_order` | 已接真实后端；关闭事件、现场反馈、工单流转参数按 `@RequestParam` 使用 query 传参；2026-03-18 起列表分页统一为 `StandardPagination + useServerPagination`（当前接口仍为 `/list`，前端按本地切片分页） |
| 风险点管理 | `/risk-point` / `RiskPointView.vue` | `spring-boot-iot-ui/src/api/riskPoint.ts`；`spring-boot-iot-ui/src/api/iot.ts` | `/api/risk-point/list`；`/api/risk-point/page`；`/api/risk-point/get/{id}`；`/api/risk-point/add`；`/api/risk-point/update`；`/api/risk-point/delete/{id}`；`/api/risk-point/bind-device`；`/api/risk-point/unbind-device`；`/api/risk-point/bound-devices/{riskPointId}`；`/api/device/list`；`/api/device/{deviceId}/metrics` | `risk_point`、`risk_point_device`、`iot_device`、`iot_product_model`、`iot_device_property` | 已按后端真实路径对齐前端调用；2026-03-17 起列表默认走 `/page` 服务端分页，2026-03-18 起页面统一为“KPI 概览卡 + 筛选卡 + 列表卡 + 抽屉表单”工作台结构，抽屉内部统一为“提示卡 + 分区卡 + 栅格字段”录入布局，并统一使用 `StandardPagination` |
| 阈值规则 | `/rule-definition` / `RuleDefinitionView.vue` | `spring-boot-iot-ui/src/api/ruleDefinition.ts` | `/api/rule-definition/list`；`/api/rule-definition/page`；`/api/rule-definition/get/{id}`；`/api/rule-definition/add`；`/api/rule-definition/update`；`/api/rule-definition/delete/{id}` | `rule_definition` | 已按后端真实路径对齐前端调用；2026-03-17 起列表默认走 `/page` 服务端分页，2026-03-18 起页面统一为“KPI 概览卡 + 筛选卡 + 列表卡 + 抽屉表单”工作台结构，抽屉内部统一为“提示卡 + 分区卡 + 栅格字段”录入布局，并统一使用 `StandardPagination` |
| 联动规则 | `/linkage-rule` / `LinkageRuleView.vue` | `spring-boot-iot-ui/src/api/linkageRule.ts` | `/api/linkage-rule/list`；`/api/linkage-rule/page`；`/api/linkage-rule/get/{id}`；`/api/linkage-rule/add`；`/api/linkage-rule/update`；`/api/linkage-rule/delete/{id}` | `linkage_rule` | 已按后端真实路径对齐前端调用；2026-03-17 起列表默认走 `/page` 服务端分页，2026-03-18 起页面统一为“KPI 概览卡 + 筛选卡 + 列表卡 + 抽屉表单”工作台结构，抽屉内部统一为“提示卡 + 分区卡 + 栅格字段”录入布局，并统一使用 `StandardPagination` |
| 应急预案 | `/emergency-plan` / `EmergencyPlanView.vue` | `spring-boot-iot-ui/src/api/emergencyPlan.ts` | `/api/emergency-plan/list`；`/api/emergency-plan/page`；`/api/emergency-plan/get/{id}`；`/api/emergency-plan/add`；`/api/emergency-plan/update`；`/api/emergency-plan/delete/{id}` | `emergency_plan` | 已按后端真实路径对齐前端调用；2026-03-17 起列表默认走 `/page` 服务端分页，2026-03-18 起页面统一为“KPI 概览卡 + 筛选卡 + 列表卡 + 抽屉表单”工作台结构，抽屉内部统一为“提示卡 + 分区卡 + 栅格字段”录入布局，并统一使用 `StandardPagination` |
| 组织管理 | `/organization` / `OrganizationView.vue` | `spring-boot-iot-ui/src/api/organization.ts` | `/api/organization`；`/api/organization/list`；`/api/organization/tree`；`/api/organization/{id}` | `sys_organization` | 已接真实后端；树形维护能力可用 |
| 用户管理 | `/user` / `UserView.vue` | `spring-boot-iot-ui/src/api/user.ts` | `/api/user/list`；`/api/user/{id}`；`/api/user/add`；`/api/user/update`；`/api/user/change-password`；`/api/user/reset-password/{userId}` | `sys_user`、`sys_user_role` | 已按真实后端对齐；新增/编辑已支持角色分配，列表展示角色名称；右上角头像菜单可直接触发当前登录账号改密码 |
| 角色管理 | `/role` / `RoleView.vue` | `spring-boot-iot-ui/src/api/role.ts`；`spring-boot-iot-ui/src/api/menu.ts` | `/api/role/add`；`/api/role/list`；`/api/role/{id}`；`/api/role/update`；`/api/role/user/{userId}`；`/api/menu/tree` | `sys_role`、`sys_user_role`、`sys_menu`、`sys_role_menu` | 已接真实后端；角色页已补齐菜单树授权、筛选、全选/清空，且新增/编辑/删除按钮按 `system:role:*` 收口 |
| 菜单管理 | `/menu` / `MenuView.vue` | `spring-boot-iot-ui/src/api/menu.ts` | `/api/menu/tree`；`/api/menu/list`；`/api/menu/{id}`；`/api/menu/add`；`/api/menu/update`；`DELETE /api/menu/{id}` | `sys_menu`、`sys_role_menu` | 已接真实后端；支持菜单树查看与菜单 CRUD，删除时校验子菜单与角色引用，且新增/编辑/删除按钮按 `system:menu:*` 收口，跳转角色授权入口按 `system:role:update` 收口 |
| 自动化测试 | `/automation-test` / `AutomationTestCenterView.vue` | 无（前端本地编排，导出 JSON 计划） | 无（由 `scripts/auto/run-browser-acceptance.mjs --plan=...` 读取计划执行） | `localStorage`、`config/automation/*.json`、`config/automation/baselines/*`、`logs/acceptance/*` | 已交付配置驱动骨架；支持模板编排、变量捕获、报告导出、测试建议生成、页面盘点、覆盖分析、一键脚手架生成，以及插件式复杂步骤（勾选、上传、表格行、弹窗）；2026-03-17 起已补齐截图基线、视觉回归、diff 图片索引页、失败截图明细页与基线治理命令，并支持 `baselineDir`、`assertScreenshot`、`--update-baseline`、`npm run acceptance:browser:baseline:manage` |
| 区域管理 | `/region` / `RegionView.vue` | `spring-boot-iot-ui/src/api/region.ts` | `/api/region`；`/api/region/list`；`/api/region/tree`；`/api/region/{id}` | `sys_region` | 已接真实后端；树形区域维护能力可用 |
| 字典配置 | `/dict` / `DictView.vue` | `spring-boot-iot-ui/src/api/dict.ts` | `/api/dict`；`/api/dict/list`；`/api/dict/tree`；`/api/dict/{id}`；`/api/dict/code/{dictCode}` | `sys_dict`、`sys_dict_item` | 主字典 CRUD 已接后端；字典项子接口前端已写、后端未见对应 Controller |
| 通知渠道 | `/channel` / `ChannelView.vue` | `spring-boot-iot-ui/src/api/channel.ts` | `/api/system/channel/list`；`/api/system/channel/getByCode/{channelCode}`；`/api/system/channel/add`；`/api/system/channel/update`；`/api/system/channel/delete/{id}`；`/api/system/channel/test/{channelCode}` | `sys_notification_channel` | 已接真实后端；支持 `Webhook` 配置提示、测试通知与 `system_error` 场景配置 |
| 业务日志 | `/audit-log` / `AuditLogView.vue` | `spring-boot-iot-ui/src/api/auditLog.ts` | `/api/system/audit-log/list`；`/api/system/audit-log/page`；`/api/system/audit-log/get/{id}`；`/api/system/audit-log/delete/{id}`；`/api/system/audit-log/business/stats` | `sys_audit_log` | 已接真实后端；默认排除 `system_error`，支持按 `operationResult` 筛选并展示审计统计概览，详情支持请求/响应摘要展示（脱敏+截断） |
| 系统日志 | `/system-log` / `AuditLogView.vue` | `spring-boot-iot-ui/src/api/auditLog.ts` | `/api/system/audit-log/list`；`/api/system/audit-log/page`；`/api/system/audit-log/get/{id}`；`/api/system/audit-log/delete/{id}`；`/api/system/audit-log/system-error/stats` | `sys_audit_log` | 已接真实后端；固定查看 `system_error` 后台异常，支持按模块、通道、目标 / URL 与 `operationResult` 排查，并展示异常统计概览 |

### 5.1.1 系统治理分页基线补充（2026-03-17）

适用页面：
- `/organization`
- `/user`
- `/role`
- `/menu`
- `/region`
- `/dict`
- `/channel`
- `/audit-log`
- `/system-log`
- `/message-trace`

基线要求：
1. 分页组件切换到第 1 页、每页 10 条时，表格实际展示记录数不得超过 10 条。
2. 用户、角色、字典、通知渠道、业务日志、系统日志统一通过后端 `/page` 接口获取当前页数据，禁止前端基于全量数据二次 `slice` 假分页。
3. 组织、区域、菜单在无筛选条件时仅分页查询根节点，展开节点时再通过 `/list?parentId=...` 懒加载子节点。
4. 组织、区域、菜单在筛选模式下返回扁平分页结果，避免为搜索再次回退到整树全量查询。
5. `/api/menu/tree` 仅继续用于角色授权树等完整树场景；菜单管理页主列表不再依赖该接口做全量展示。
6. 审计日志分页必须基于数据库分页查询，不允许先查全量再在服务层 `subList`。

建议验收动作：
1. 分别在用户、角色、字典、通知渠道页将 `pageSize` 设为 `10`，确认表格实际展示 10 条以内且翻页后请求新的 `/page` 数据。
2. 在组织、区域、菜单页清空筛选条件后进入列表，确认首屏仅加载根节点；展开任一节点时才触发子节点请求。
3. 在组织、区域、菜单页输入筛选条件后查询，确认返回扁平分页结果，不再触发整树全量加载。
4. 在业务日志 / 系统日志页切换分页，确认接口请求参数包含 `pageNum`、`pageSize`，且结果总数与当前页记录数一致。

### 5.1.2 顶部账号与导航收口补充（2026-03-17）

适用页面：
- 全部采用 `AppShell` 壳层的登录后页面

基线要求：
1. 页面标题区不再提供显式 `退出登录` 按钮，个人操作统一收口到右上角头像菜单。
2. 顶部头像菜单需至少提供：账号中心、实名认证说明、登录方式、修改密码、退出登录。
3. `系统管理` 分组页面默认不展示最近访问标签，避免与左侧导航重复表达当前定位。
4. 页面头部不再展示 `接入设置` 展开区，避免与业务内容竞争视觉焦点。
5. 左侧分组简介卡仅在各一级分组首页展示，进入二级详情页后仅保留菜单列表与当前页标题；二级导航以菜单名称为主，说明通过悬浮提示提供，不再在左栏重复展开；内容区标题栏采用轻量层级展示，不再使用厚卡片式页头。
6. `GET /api/auth/me` 返回的 `authContext` 需包含当前用户手机号、邮箱、账号类型、实名状态和可用登录方式，供头像菜单与账号抽屉直接渲染。
7. 一级导航点击后应优先进入分组首页（如 `/device-access`、`/risk-disposal`、`/system-management`），再从概览页进入具体功能页面。
8. 告警中心、事件处置、实时监测、GIS 风险态势的列表操作条、提示条、空态卡与资源卡需统一采用浅色轻卡片风格，不再使用厚重工具条或强营销式装饰。

### 5.2 已有页面或接口，但当前仍待对齐 / 待补完 / 演示态

| 模块 | 页面（路由 / 视图） | 前端 API 文件 | 后端接口（主路径） | 主要数据表 / 存储 | 当前状态 |
|---|---|---|---|---|---|
| 驾驶舱 | `/` / `CockpitView.vue` | `spring-boot-iot-ui/src/api/report.ts` + `spring-boot-iot-ui/src/stores/activity.ts` | `/api/report/risk-trend`；`/api/report/alarm-statistics`；`/api/report/event-closure`；`/api/report/device-health`；旧 `/api/cockpit/*` 演示接口仍保留 | 报表聚合数据 + activity store 本地记录；旧 `CockpitServiceImpl` 演示数据 | 页面已升级为“数据看板驾驶舱 + 事务工作台入口”双层结构，支持一线/运维/管理/研发四类角色视角切换；KPI 优先使用真实报表聚合接口，接口不可用时自动回退稳定口径 |
| 分组首页 | `/device-access`、`/risk-disposal`、`/system-management`、`/risk-enhance` / `SectionLandingView.vue` | 无（前端静态编排 + 权限过滤） | 无（依赖 `GET /api/auth/me` 返回的 `authContext.menus` 与账号资料） | `localStorage(auth-context)` + `activity store` 本地痕迹 | 作为一级导航统一落地页，展示分组概览、常用入口、最近使用、推荐操作与全部能力；不单独引入后端接口 |
| 报表分析 | `/report-analysis` / `ReportAnalysisView.vue` | `spring-boot-iot-ui/src/api/report.ts` | `/api/report/risk-trend`；`/api/report/alarm-statistics`；`/api/report/event-closure`；`/api/report/device-health` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 前端已对齐真实接口；后端已按业务表完成聚合，日期筛选参数固定为 `YYYY-MM-DD` |
| 风险监测增强 | `/risk-monitoring` / `RealTimeMonitoringView.vue`；`/risk-monitoring-gis` / `RiskGisView.vue` | `spring-boot-iot-ui/src/api/riskMonitoring.ts` | `/api/risk-monitoring/realtime/list`；`/api/risk-monitoring/realtime/{bindingId}`；`/api/risk-monitoring/gis/points` | `risk_point`、`risk_point_device`、`iot_alarm_record`、`iot_event_record`、`iot_device`、`iot_device_property`、`iot_message_log`、`iot_product` | 代码已完成并接入真实 API；实时监测 / GIS 页面已统一为“KPI 概览卡 + 筛选卡 + 列表卡 / 资源卡 + 详情抽屉”结构，2026-03-16 已确认共享开发库需先执行 `sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql` 后再复验 |
| 规划展示 | `/future-lab` / `FutureLabView.vue` | 无 | 无 | 无 | 纯规划展示页，不连接后端接口与数据表 |

## 6. 验收输出物建议

1. 自动化：CI 或本地执行日志，至少包含构建结果与关键测试结果
2. 手工：HTTP 联调记录、MQTTX 上下行截图或录屏、关键表落库截图
3. 结论：按本清单逐项标注“通过/不通过/待确认”，并对差异项给出整改结论与负责人
4. 基线固化：执行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-acceptance-baseline.ps1`，输出 `logs/acceptance/acceptance-baseline-summary-<timestamp>.json` 与 `logs/acceptance/acceptance-baseline-report-<timestamp>.md`，并回写 `docs/22-automation-test-issues-20260316.md`；门禁同时校验执行成功、summary 是否为本轮 fresh、以及 `19/21` 指标阈值。



## 认证与权限验收基线（2026-03-16）

### 验收范围
- 平台首页：`/`
- 登录页面：`/login`
- 登录能力：`POST /api/auth/login`
- 会话识别：`GET /api/auth/me`
- 菜单树：`GET /api/menu/tree`
- 受保护接口拦截：设备管理与系统管理 API

### 验收标准
- 未登录可直接访问 `/` 首页
- 登录页具备微信扫码区、账号密码登录、手机号登录三种入口
- 使用有效账号登录可获得 JWT token
- 使用有效手机号和系统密码登录可获得 JWT token
- 登录成功后返回当前用户 `authContext`，包括角色、菜单树、按钮权限
- 刷新页面时可通过 `GET /api/auth/me` 恢复当前用户菜单与权限上下文
- 若共享开发环境出现 `authContext.menus=[]`，前端壳层仅启用文档预设的临时静态菜单兜底以保障可导航性；数据库动态菜单仍是正式口径
- 未携带 token 访问受保护页面会跳转到 `/login`，访问受保护接口返回 `401`
- 携带 token 访问同一接口返回非 `401`
- 前端登录后自动携带 `Authorization: Bearer <token>`
- 前端收到 `401` 时自动清理本地登录态并要求重新登录
- 角色管理页可加载完整菜单树，支持直接勾选页面/按钮权限；用户/角色/菜单页面操作按钮受 `menu_code` 权限控制
- 若真实环境菜单页仍看不到超管的新增/编辑/删除按钮，优先核查 `SUPER_ADMIN` 的真实角色 ID 是否与历史脚本预设值不一致；当前仓库已提供 `sql/upgrade/20260317_phase4_menu_button_permission_backfill.sql` 做定向回填。
- 当前共享环境下，微信扫码入口只验页面结构与接入提示，不验真实开放平台回调

### 执行与回归注意事项（2026-03-17）

1. 目标库存在历史结构差异：`sys_role_menu`、`sys_user_role` 可能仅保留 `id/tenant_id/role_id/menu_id/create_time` 等简化字段，执行 SQL 必须按现网字段兼容，避免直接写入不存在列（如 `create_by/update_by/deleted`）。
2. `SUPER_ADMIN` 角色 ID 在不同环境可能不同。执行授权时必须先按 `role_code='SUPER_ADMIN'` 反查真实 `id`，不要硬编码为 `92000005`。
3. 执行前建议备份 `sys_role`、`sys_menu`、`sys_role_menu`、`sys_user_role` 四张表，避免历史授权被误覆盖。
4. 涉及角色菜单重建时（如 `DELETE FROM sys_role_menu WHERE role_id IN (...)`）应在低峰窗口执行，避免短时权限抖动导致页面菜单闪断。
5. 菜单初始化与授权脚本建议保持幂等（`INSERT ... ON DUPLICATE KEY UPDATE` 或“存在即更新，不存在则插入”），避免重复执行后出现脏数据。
6. 菜单权限调整后，前端需重新登录以刷新 `authContext.menus`；仅刷新页面不一定触发新会话菜单树拉取。
7. 回归 `401` 问题时优先检查三点：是否携带 `Authorization: Bearer <token>`、token 是否过期、角色-菜单关系是否完整，再判断前端渲染问题。
8. 回归账号（如 `accept_*`）仅用于验收，验收结束后应禁用或删除，避免长期保留测试入口。
9. 角色菜单范围调整统一通过 `sys_role_menu` 维护，不再通过前端硬编码路由控制可见性。
10. 菜单新增、改名、改路由后，需同步更新升级 SQL 与本文件“菜单信息表”章节，确保文档与数据库口径一致。

### 上线检查清单（菜单权限）

- [ ] 已备份 `sys_role`、`sys_menu`、`sys_role_menu`、`sys_user_role` 四张表。
- [ ] 已确认目标环境 `sys_role_menu`、`sys_user_role` 表结构与脚本字段兼容。
- [ ] 已通过 `role_code='SUPER_ADMIN'` 查询并确认超级管理员真实角色 ID。
- [ ] 菜单与角色授权 SQL 已在低峰窗口执行，执行日志已留档。
- [ ] `sys_menu` 中已存在 `system:menu`（`/menu`），且状态为可用。
- [ ] `MANAGEMENT_STAFF` 已授权 `/menu`，`SUPER_ADMIN` 已授权 `/menu`。
- [ ] 已按账号回归：`admin`、`accept_management` 可见 `/menu`；`accept_business`、`accept_ops`、`accept_developer` 不可见 `/menu`。
- [ ] `POST /api/auth/login` 与 `GET /api/auth/me` 回归通过，`authContext.menus` 与角色授权一致。
- [ ] 未登录访问受保护接口返回 `401`，登录后携带 `Authorization: Bearer <token>` 返回非 `401`。
- [ ] 前端已执行重新登录验证，确认菜单树来自后端动态授权而非硬编码。
- [ ] 回归测试账号已完成收口策略（禁用或删除），避免长期保留测试入口。

### 执行记录模板（菜单权限）

| 序号 | 检查项 | 结果（通过/不通过） | 执行人 | 执行时间 | 证据（日志/截图/SQL） | 备注 |
|---|---|---|---|---|---|---|
| 1 | 已备份 `sys_role`、`sys_menu`、`sys_role_menu`、`sys_user_role` 四张表 |  |  |  |  |  |
| 2 | 已确认目标环境 `sys_role_menu`、`sys_user_role` 表结构与脚本字段兼容 |  |  |  |  |  |
| 3 | 已通过 `role_code='SUPER_ADMIN'` 查询并确认超级管理员真实角色 ID |  |  |  |  |  |
| 4 | 菜单与角色授权 SQL 已在低峰窗口执行，执行日志已留档 |  |  |  |  |  |
| 5 | `sys_menu` 中已存在 `system:menu`（`/menu`），且状态为可用 |  |  |  |  |  |
| 6 | `MANAGEMENT_STAFF` 已授权 `/menu`，`SUPER_ADMIN` 已授权 `/menu` |  |  |  |  |  |
| 7 | 已按账号回归：`admin`、`accept_management` 可见 `/menu`；`accept_business`、`accept_ops`、`accept_developer` 不可见 `/menu` |  |  |  |  |  |
| 8 | `POST /api/auth/login` 与 `GET /api/auth/me` 回归通过，`authContext.menus` 与角色授权一致 |  |  |  |  |  |
| 9 | 未登录访问受保护接口返回 `401`，登录后携带 `Authorization: Bearer <token>` 返回非 `401` |  |  |  |  |  |
| 10 | 前端已执行重新登录验证，确认菜单树来自后端动态授权而非硬编码 |  |  |  |  |  |
| 11 | 回归测试账号已完成收口策略（禁用或删除），避免长期保留测试入口 |  |  |  |  |  |

## 6. 真实环境全链路打勾清单（2026-03-16）

执行方式：
1. 后端以 `dev` 配置启动（示例端口 `10099`）。
2. 执行脚本：`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://127.0.0.1:10099`
3. 本次报告：
   - `logs/acceptance/business-function-report-20260316173045.md`
   - `logs/acceptance/business-function-summary-20260316173045.json`

### 6.1 四维实测对照（模块-页面-接口-数据表）

| 勾选 | 模块 | 页面/路由 | 关键接口 | 关键数据表 | 实测结论 |
|---|---|---|---|---|---|
| [x] | 环境与鉴权 | 全局 | `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/device/list` | `sys_user` | 登录、token 校验、受保护接口访问通过 |
| [x] | 产品管理 | `/products` | `POST /api/device/product/add`、`GET /api/device/product/{id}` | `iot_product` | 通过 |
| [x] | 设备管理 | `/devices` | `POST /api/device/add`、`GET /api/device/{id}`、`GET /api/device/code/{deviceCode}` | `iot_device` | 通过 |
| [x] | 上报调试 | `/reporting` | `POST /message/http/report`、`GET /api/device/{deviceCode}/properties`、`GET /api/device/{deviceCode}/message-logs` | `iot_message_log`、`iot_device_property` | 通过 |
| [ ] | MQTT 下发 | `/reporting` | `POST /message/mqtt/down/publish` | `iot_command_record` | 失败（500） |
| [ ] | 告警中心 | `/alarm-center` | `/api/alarm/*` | `iot_alarm_record` | 失败（500） |
| [ ] | 事件处置 | `/event-disposal` | `/api/event/*` | `iot_event_record`、`iot_event_work_order` | 失败（500） |
| [ ] | 风险点管理 | `/risk-point` | `/api/risk-point/*` | `risk_point`、`risk_point_device` | 部分通过，绑定链路失败 |
| [ ] | 阈值规则 | `/rule-definition` | `/api/rule-definition/*` | `rule_definition` | 失败（500） |
| [ ] | 联动规则 | `/linkage-rule` | `/api/linkage-rule/*` | `linkage_rule` | 失败（500） |
| [ ] | 应急预案 | `/emergency-plan` | `/api/emergency-plan/*` | `emergency_plan` | 失败（500） |
| [ ] | 分析报表 | `/report-analysis` | `/api/report/*` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 仅 1/4 通过 |
| [ ] | 组织机构 | `/organization` | `/api/organization/*` | `sys_organization` | 失败（500） |
| [x] | 用户管理 | `/user` | `/api/user/*` | `sys_user` | 通过 |
| [ ] | 角色管理 | `/role` | `/api/role/*` | `sys_role`、`sys_user_role` | 失败（500） |
| [x] | 区域管理 | `/region` | `/api/region/*` | `sys_region` | 通过 |
| [ ] | 字典配置 | `/dict` | `/api/dict/*` | `sys_dict`、`sys_dict_item` | 部分通过 |
| [ ] | 通知渠道 | `/channel` | `/api/system/channel/*` | `sys_notification_channel` | 失败（500） |
| [ ] | 业务日志 | `/audit-log` | `/api/system/audit-log/*` | `sys_audit_log` | 失败（500） |

### 6.2 本轮通过率

- 功能点总数：`19`
- 通过：`6`
- 未通过：`13`
- 通过功能点：`ENV`、`IOT-PRODUCT`、`IOT-DEVICE`、`INGEST-HTTP`、`SYS-USER`、`SYS-REGION`

### 6.3 主要阻塞（来自后端异常日志）

1. 缺列：`create_by`、`remark`、`org_type`、`metric_name`、`description`、`user_name`、`command_id`
2. 缺表：`rm_iot.risk_point_device`、`rm_iot.sys_notification_channel`
3. 约束问题：`Field 'dict_value' doesn't have a default value`

结论：
1. 鉴权链路已闭环，真实环境 token 可用于受保护接口访问。
2. 当前主要阻塞来自真实库结构与当前代码模型不一致，需先完成数据库 schema 对齐后再进行第二轮全链路验收。

## 7. 2026-03-16 第二轮真实环境复验结果（有效结论）

### 7.1 执行方式
1. 启动后端：`dev` 真实环境配置（`application-dev.yml`，端口 `9999`）
2. 执行 schema 对齐：`PYTHONPATH=.codex-runtime/pydeps python scripts/run-real-env-schema-sync.py`
3. 执行业务冒烟：`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://localhost:9999`

本轮产物：
- `logs/acceptance/business-function-smoke-20260316191059.json`
- `logs/acceptance/business-function-summary-20260316191059.json`
- `logs/acceptance/business-function-report-20260316191059.md`

### 7.2 四维实测结论（模块-页面-接口-数据表）
| 模块 | 页面/路由 | 关键接口 | 关键数据表 | 结论 |
|---|---|---|---|---|
| 环境与鉴权 | 全局 | `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/device/list` | `sys_user` | 通过 |
| 产品管理 | `/products` | `POST /api/device/product/add`、`GET /api/device/product/{id}` | `iot_product` | 通过 |
| 设备管理 | `/devices` | `POST /api/device/add`、`GET /api/device/{id}`、`GET /api/device/code/{deviceCode}` | `iot_device` | 通过 |
| 上报调试 | `/reporting` | `POST /message/http/report`、`GET /api/device/{deviceCode}/properties`、`GET /api/device/{deviceCode}/message-logs` | `iot_message_log`、`iot_device_property` | 通过 |
| MQTT 下发 | `/reporting` | `POST /message/mqtt/down/publish` | `iot_command_record` | 通过 |
| 告警中心 | `/alarm-center` | `/api/alarm/*` | `iot_alarm_record` | 通过 |
| 事件处置 | `/event-disposal` | `/api/event/*` | `iot_event_record`、`iot_event_work_order` | 通过 |
| 风险点管理 | `/risk-point` | `/api/risk-point/*` | `risk_point`、`risk_point_device` | 通过 |
| 阈值规则 | `/rule-definition` | `/api/rule-definition/*` | `rule_definition` | 通过 |
| 联动规则 | `/linkage-rule` | `/api/linkage-rule/*` | `linkage_rule` | 通过 |
| 应急预案 | `/emergency-plan` | `/api/emergency-plan/*` | `emergency_plan` | 通过 |
| 分析报表 | `/report-analysis` | `/api/report/*` | `iot_alarm_record`、`iot_event_record`、`iot_device` | 通过 |
| 组织机构 | `/organization` | `/api/organization/*` | `sys_organization` | 通过 |
| 用户管理 | `/user` | `/api/user/*` | `sys_user` | 通过 |
| 角色管理 | `/role` | `/api/role/*` | `sys_role`、`sys_user_role` | 通过 |
| 区域管理 | `/region` | `/api/region/*` | `sys_region` | 通过 |
| 字典配置 | `/dict` | `/api/dict/*` | `sys_dict`、`sys_dict_item` | 通过 |
| 通知渠道 | `/channel` | `/api/system/channel/*` | `sys_notification_channel` | 通过 |
| 业务日志 | `/audit-log` | `/api/system/audit-log/*` | `sys_audit_log` | 通过 |

### 7.3 通过率
- 功能点总数：`19`
- 通过：`19`
- 未通过：`0`

### 7.4 本轮关键修复
1. 补齐共享库缺表/缺列与默认值兼容（`risk_point_device`、`sys_notification_channel`、`sys_dict`、`sys_audit_log` 等）。
2. 调整旧唯一索引相关字段兼容：`rule_definition.rule_code`、`linkage_rule.rule_code`、`emergency_plan.plan_code` 允许 `NULL`，规避空串默认值导致的重复冲突。
3. 真实环境当前验收结论以上述第二轮结果为准；第一轮失败记录仅保留为问题追踪历史。
## 2026-03-18 Frontend UI Unification Progress

- Pagination baseline: view-level list pages have completed migration to shared `StandardPagination` + `useServerPagination`.
- Color baseline: shared shell/components/pages now avoid raw brand/accent literals and use theme tokens (`var(--brand|--accent)`) with `color-mix(...)`.
- Validation snapshot:
  - `rg -n "<el-pagination" spring-boot-iot-ui/src/views` => no matches.
  - `rg -n "#1677ff|#4096ff|#ff6a00|#ff8833|rgba\\(22, 119, 255|rgba\\(255, 106, 0" spring-boot-iot-ui/src` => only `src/styles/tokens.css` token source definitions remain.

## 日志链路补充（2026-03-18）
- 系统日志（system_error）与消息追踪链路新增最小兜底：当 MQTT 消息在分发前置校验失败时，iot_device_message_log 会补写 messageType=dispatch_failed 记录。
- 验收时应核对同一 traceId 在 sys_audit_log 与 /api/device/message-trace/page 可对应命中；设备不存在场景允许 device_id=0 占位。
