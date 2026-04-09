# MQTT Invalid Report Governance Design

**Date:** 2026-03-27
**Status:** Draft for review
**Scope:** MQTT 无效上报治理、未登记设备最新态保留、异常观测计数分流、Broker 准入前移预留

## 1. 背景

当前共享环境中的异常观测台已经出现明显的“重复无效上报放大”问题：

1. 大量重复异常会持续进入 MQTT 消费主链路，占用线程、协议解析、设备校验、异常归档与系统异常通知资源。
2. 当前异常样本中约 `90%` 集中在两类原因：
   - `设备不存在`
   - `mqtt-json-decrypted MQTT 负载不能为空`
3. 对于“未登记但可能真实存在”的设备，当前系统会不断重复写入失败轨迹和失败归档，但业务上真正需要的往往只是“最近一条最新记录 + 最近仍在上报”的只读视图。
4. 当前 `failure-stage-spike` 运维告警仍依赖失败归档行数统计；若简单把失败归档改成“只保留一条”，会直接破坏现有异常统计与告警语义。

仓库内当前相关事实如下：

1. MQTT 上行消息入口在 [`spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java)。
2. 分发失败后会继续写入失败轨迹、失败归档和 `system_error`，链路在 [`spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttConnectionListener.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttConnectionListener.java)。
3. `dispatch_failed` 失败轨迹当前每次失败都会插入一条消息日志，写入逻辑在 [`spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImpl.java)。
4. `iot_device_access_error_log` 当前每次失败都会插入一条失败归档，写入逻辑在 [`spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImpl.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAccessErrorLogServiceImpl.java)。
5. `$dp` 解密后空载荷异常来源于 [`spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadFrameParser.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttPayloadFrameParser.java) 与 [`spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java)。
6. 设备不存在异常来源于 [`spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceContractStageHandler.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceContractStageHandler.java)。
7. 项目里已经存在设备 MQTT 认证服务 [`spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAuthServiceImpl.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceAuthServiceImpl.java)，但当前尚未真正接管 Broker 认证流程。

## 2. 目标

本轮设计目标如下：

1. 让高频无效 MQTT 上报在更早阶段被拒绝或熔断，减少主链路线程、异常归档和系统异常通知开销。
2. 把 `设备不存在` 与 `$dp` 解密后空负载两类高频原因作为首批治理对象，先解决主要噪音来源。
3. 对未登记设备保留“最近一条最新态 + 命中次数 + 最近上报时间”，不再按时间持续堆积重复失败明细。
4. 保持 `/devices` 未登记设备名单和异常观测排障能力可用，不牺牲“最近一次原始报文 / 最近一次 TraceId / 最近一次 Topic”的查看能力。
5. 保持 `failure-stage-spike`、失败归档统计摘要和最近 1h / 24h 口径仍可反映真实频次，不依赖“每条重复坏报文都插一条详情行”。
6. 对已补录建档的设备提供自动解封能力，避免治理状态阻碍后续正常接入。

## 3. 非目标

1. 本轮不重构 HTTP 上报主链路。
2. 本轮不引入永久人工维护的“全局静态黑名单页面”。
3. 本轮不新增前端独立一级路由；若需要展示治理状态，优先复用现有异常观测台和设备资产中心。
4. 本轮不把外部 Broker 认证、ACL、踢线控制强行做成仓库内假实现；仓库内只预留对接点与文档口径。
5. 本轮不扩大治理范围到所有业务异常；首批仅处理高频无效上报场景。

## 4. 方案比较

### 4.1 方案 A：只改失败归档为“最新覆盖”

优点：

1. 改动面最小。
2. 能减少失败归档表膨胀。

问题：

1. 消息仍会进入主处理链路、协议解析、设备校验与异常通知流程，线程和 CPU 开销仍然存在。
2. 会直接影响现有失败归档列表、统计和 `failure-stage-spike` 的语义。
3. 无法区分“最新态展示”与“频次统计”两类不同用途。

### 4.2 方案 B：平台内入口熔断 + 最新态保留 + 计数分流

优点：

1. 仓库内可直接落地。
2. 能显著减少主链路重复处理、异常归档和通知噪音。
3. 能把未登记设备只读名单和失败频次统计拆成两条清晰口径。

问题：

1. 对于依赖 payload 才能识别设备的 legacy `$dp`，无法在“完全不解码”的情况下就识别全部 `设备不存在`。
2. 仍然会有少量首次命中或冷却期后重采样事件进入应用层。

### 4.3 方案 C：Broker 准入前置 + 平台内熔断双层治理

优点：

1. 对 `设备不存在` 最省资源，理想情况下在连接阶段就拒绝。
2. 与平台内熔断组合后，能同时覆盖“未知设备连接”和“已连接但持续发坏包”的两类问题。

问题：

1. 需要外部 Broker 配合，属于仓库外环境协同项。
2. 不能替代平台内对空负载、解密空包、重复坏包的治理。

### 4.4 推荐结论

推荐采用 **方案 C 的双层形态**，但按交付边界拆为两段：

1. **本轮仓库内必须落地：**
   - 平台内入口熔断
   - 未登记设备最新态保留
   - 失败频次计数分流
   - 补录建档后的自动解封
2. **本轮同步输出但不宣称仓库内已闭环：**
   - Broker 认证 / ACL 前移接入方案
   - 对现有 `DeviceAuthService` 的 Broker 集成说明

## 5. 设计结论

### 5.1 总体架构

采用“四层治理”：

1. **Broker 准入层**
   - 外部 Broker 侧集成现有 `DeviceAuthService`
   - 优先拒绝未知 `clientId/deviceCode`
   - 对标准 Topic 进一步约束“设备只能发自己的 Topic”
2. **MQTT 入口熔断层**
   - 在 `messageArrived` 入口与 `TOPIC_ROUTE` 后增加轻量治理判断
   - 对首批无效原因执行快速丢弃 / 采样 / 冷却
3. **最新态持久化层**
   - 把未登记设备与高频坏包收敛为“最新一条状态”
   - 供 `/devices` 与异常观测台查看最近状态
4. **频次统计层**
   - 单独累计 failure stage 和 reason code 的时间窗计数
   - 供 `failure-stage-spike` 与失败统计摘要消费

### 5.2 首批治理原因

本轮首批只治理两类原因：

1. `EMPTY_DECRYPTED_PAYLOAD`
   - 表示 `$dp` 报文经解密后 payload 为空，或原始 MQTT payload 为空
   - 对应现有错误文案 `mqtt-json-decrypted MQTT 负载不能为空`
2. `DEVICE_NOT_FOUND`
   - 表示设备契约阶段无法找到设备主档
   - 对应现有错误文案 `设备不存在: {deviceCode}`

后续可扩展但本轮不一并纳入：

1. `UNSUPPORTED_TOPIC`
2. `PROTOCOL_MISMATCH`
3. `PRODUCT_NOT_FOUND`
4. `INVALID_ENCRYPTED_ENVELOPE`

### 5.3 新增运行时治理服务

新增一个运行时治理服务族，建议命名为 `MqttInvalidReportGovernanceService`，放在 `spring-boot-iot-message`，并由 `spring-boot-iot-framework` 提供 Redis 存储与配置绑定支持。

核心职责：

1. 接收入口侧可识别的最小上下文：
   - transportMode
   - topic
   - topicRouteType
   - productKey
   - deviceCode
   - protocolCode
   - clientId
   - payload hash / payload size
   - reasonCode
   - failureStage
2. 返回治理决策：
   - `ALLOW`
   - `ALLOW_AND_SAMPLE`
   - `DROP_SUPPRESSED`
3. 维护冷却窗口与命中次数。
4. 触发最新态 upsert。
5. 触发频次计数累计。

### 5.4 治理决策位置

#### 5.4.1 原始入口快速丢弃

在 `MqttMessageConsumer#messageArrived` 最前面增加原始报文快速检查：

1. `payload == null` 或 `payload.length == 0`
   - 不进入固定 pipeline
   - 直接按 `EMPTY_DECRYPTED_PAYLOAD` 的原始空包分支处理
2. 若当前 `topic` + `payloadFingerprint` 已处于空包冷却期
   - 直接丢弃
   - 不写失败轨迹
   - 不写失败归档
   - 不写 `system_error`

#### 5.4.2 Topic 路由后早丢弃

对标准 `/sys/{productKey}/{deviceCode}/...` Topic：

1. `TOPIC_ROUTE` 后即可获得 `productKey/deviceCode`
2. 若运行时抑制表中已存在该设备的 `DEVICE_NOT_FOUND` 冷却态
   - 直接在 `PROTOCOL_DECODE` 前丢弃
   - 不再进入协议解析和设备契约校验

对 legacy `$dp`：

1. 仍需经过最小解码才能拿到设备身份
2. 因此无法对全部 `DEVICE_NOT_FOUND` 在 decode 前就丢弃
3. 但解码后若已识别到目标设备且命中冷却态，可在设备契约失败重采样前直接进入抑制分支

#### 5.4.3 失败回调后的重采样抑制

在 `MqttConnectionListener#onMessageDispatchFailed` 中加入治理决策：

1. 首次命中或冷却期已过：
   - 允许采样
   - 写一条失败归档样本
   - 更新最新态
   - 写频次计数
2. 冷却期内重复命中：
   - 只更新最新态与频次计数
   - 跳过失败轨迹 `dispatch_failed`
   - 跳过失败归档插入
   - 跳过 `system_error` 审计与站内消息/外部通知

### 5.5 运行时冷却策略

本轮默认建议如下，均通过 `application-dev.yml` / 环境变量可调：

1. `EMPTY_DECRYPTED_PAYLOAD`
   - 统计窗口：`60` 秒
   - 进入抑制阈值：`3` 次
   - 冷却时长：`15` 分钟
2. `DEVICE_NOT_FOUND`
   - 统计窗口：`60` 秒
   - 进入抑制阈值：`2` 次
   - 冷却时长：`30` 分钟

冷却行为：

1. 冷却期不是永久黑名单。
2. 冷却期结束后允许重新采样一次。
3. 补录建档成功后会主动清理对应设备的抑制 key。

## 6. 数据模型

### 6.1 保留现有样本归档表

继续保留 `iot_device_access_error_log`，但用途调整为：

1. **失败样本归档**
2. **排障详情查看**
3. **最近失败样本列表**

不再要求它承担“每一次重复坏报文都落一条明细”的职责。

### 6.2 新增最新态表

新增一张最新态表，建议命名为 `iot_device_invalid_report_state`。

职责：

1. 每个治理键只保留一条最新记录
2. 承接未登记设备列表的主来源
3. 承接高频坏包最近状态展示

建议字段：

1. `id`
2. `tenant_id`
3. `governance_key`
4. `reason_code`
5. `request_method`
6. `failure_stage`
7. `device_code`
8. `product_key`
9. `protocol_code`
10. `topic_route_type`
11. `topic`
12. `client_id`
13. `payload_size`
14. `payload_encoding`
15. `last_payload`
16. `last_trace_id`
17. `sample_error_message`
18. `sample_exception_class`
19. `first_seen_time`
20. `last_seen_time`
21. `hit_count`
22. `sampled_count`
23. `suppressed_count`
24. `suppressed_until`
25. `resolved`
26. `resolved_time`
27. `deleted`

唯一键建议：

1. `uk_invalid_report_state_governance_key (governance_key)`

治理键建议：

1. `DEVICE_NOT_FOUND`：
   - `tenantId + productKey + deviceCode + reasonCode`
2. `EMPTY_DECRYPTED_PAYLOAD`：
   - `tenantId + topic + productKey + deviceCode + reasonCode + payloadFingerprint`

### 6.3 频次计数不再依赖详情行数

新增 Redis 分钟桶计数，建议键口径：

1. `iot:invalid-report:bucket:{yyyyMMddHHmm}:failure-stage:{stage}`
2. `iot:invalid-report:bucket:{yyyyMMddHHmm}:reason:{reasonCode}`

作用：

1. 统计最近 1h / 24h 失败量
2. 支撑 `failure-stage-spike`
3. 支撑失败归档统计摘要中的 `topFailureStages`

这样即使详情归档被采样，统计仍可反映真实频次。

## 7. 现有页面与接口口径调整

### 7.1 `/devices` 未登记名单

当前未登记名单主要依赖 `iot_device_access_error_log` 最近一条失败记录。调整后：

1. 主来源切换为 `iot_device_invalid_report_state`
2. 仅展示 `reasonCode=DEVICE_NOT_FOUND` 且 `resolved=0` 的最新态
3. 继续保持只读，不变更“补档与排障”定位

页面可继续展示：

1. 最近一次设备编码
2. 最近一次产品标识
3. 最近一次失败阶段
4. 最近一次 Topic
5. 最近一次 TraceId
6. 最近一次原始载荷
7. 最近命中次数
8. 最近上报时间

### 7.2 异常观测台

异常观测台保持两种数据语义：

1. **样本详情**
   - 继续查看 `iot_device_access_error_log`
   - 用于排障详情与最近失败样本
2. **频次统计**
   - 改为读取 Redis 计数桶
   - 反映真实最近 1h / 24h 失败量和高频 failure stage

### 7.3 失败详情接口

现有 `GET /api/device/access-error/{id}` 继续保留，用于查看样本归档详情。

新增口径但不一定新增单独路由：

1. 允许设备资产中心按 `sourceRecordId` 读取最新态详情
2. 若前端复用现有详情抽屉，需明确区分：
   - `sample_archive`
   - `invalid_report_state`

## 8. 告警与统计兼容

### 8.1 `failure-stage-spike`

当前规则在 [`spring-boot-iot-admin/src/main/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingService.java`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-admin/src/main/java/com/ghlzm/iot/admin/observability/alerting/ObservabilityAlertingService.java) 中依赖 `DeviceAccessErrorLogService#listFailureStageCountsSince`。

调整后：

1. `listFailureStageCountsSince` 的实现从“扫失败归档详情行”切换为“聚合 Redis 分钟桶”
2. 这样即使详情被采样，`failure-stage-spike` 仍能按真实失败次数触发

### 8.2 失败归档统计摘要

`GET /api/device/access-error/stats` 调整为双源：

1. 最近 1h / 24h 总量、`topFailureStages`
   - 走 Redis 聚合计数
2. `topErrorCodes / topExceptionClasses / topProtocolCodes / topTopics`
   - 短期内继续从样本归档读取
   - 若后续样本采样影响明显，再升级为专门聚合口径

## 9. 补录建档后的自动解封

以下成功动作后，需要清理 `DEVICE_NOT_FOUND` 抑制态与最新态的 `resolved` 标记：

1. `POST /api/device/add`
2. `POST /api/device/batch-add`
3. `POST /api/device/{id}/replace`

处理规则：

1. 成功建档后立即清 Redis 抑制 key
2. 将 `iot_device_invalid_report_state.resolved=1`
3. 保留历史最新态记录，但从默认未登记列表中过滤掉

## 10. Broker 外部协同口径

本轮仓库内不实现 Broker 本体，但要明确协同方案：

1. 若共享 Broker 支持认证插件 / HTTP Auth / Webhook Auth：
   - 使用现有 `DeviceAuthService#validateSimpleCredential`
   - 以 `clientId = deviceCode`、`username = deviceCode`、`password = deviceSecret` 为基础校验
2. 若 Broker 支持 ACL：
   - 标准 Topic 设备只能发布自己的 `productKey/deviceCode`
3. 若 Broker 支持踢线或动态封禁：
   - 可在高频 `EMPTY_DECRYPTED_PAYLOAD` 场景下进一步外部封禁客户端

该部分本轮只更新文档与预留接口说明，不把外部环境能力写成仓库内已交付闭环。

## 11. 实施分期

### 11.1 第一期：仓库内止血闭环

1. 入口快速空包丢弃
2. 运行时冷却与抑制
3. 最新态表 `iot_device_invalid_report_state`
4. 未登记名单切换到最新态
5. 失败归档改为采样写入
6. 统计与 `failure-stage-spike` 改为聚合计数
7. 补录建档自动解封

### 11.2 第二期：Broker 外部准入联动

1. Broker 认证接入 `DeviceAuthService`
2. Topic ACL
3. 持续坏客户端外部封禁策略

## 12. 测试与验收

### 12.1 单元测试

至少覆盖：

1. 空 payload 第一次命中会采样，后续命中在冷却期内被抑制
2. 标准 Topic 未登记设备第一次命中采样，后续命中在 `PROTOCOL_DECODE` 前被丢弃
3. legacy `$dp` 未登记设备在首次 decode 后进入冷却，后续重复样本不会再写失败轨迹和 `system_error`
4. 最新态 upsert 会正确更新 `lastSeenTime / hitCount / suppressedCount`
5. 补录设备后会清理抑制态并把最新态标记为 `resolved`
6. `listFailureStageCountsSince` 能在样本采样后仍返回真实窗口计数

### 12.2 集成测试

至少覆盖：

1. `MqttMessageConsumer`
2. `MqttConnectionListener`
3. `DeviceAccessErrorLogServiceImpl`
4. `UnregisteredDeviceRosterServiceImpl`
5. `ObservabilityAlertingService`

### 12.3 真实环境验收

使用 `application-dev.yml` 基线，验证：

1. 连续发送空 payload 的 `$dp` 报文
   - 主链路不再持续刷失败样本
   - 异常观测统计仍能看到频次上升
2. 连续发送不存在设备的标准 Topic 报文
   - 首次失败后进入抑制
   - 后续重复报文不再持续刷 `dispatch_failed`
3. 未登记设备在 `/devices` 中只保留一条最新态
4. 设备补录后，下一条正常上报可恢复主链路

## 13. 文档影响

若按本设计实施，必须同步更新：

1. [`README.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/README.md)
2. [`docs/03-接口规范与接口清单.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/03-接口规范与接口清单.md)
3. [`docs/04-数据库设计与初始化数据.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/04-数据库设计与初始化数据.md)
4. [`docs/07-部署运行与配置说明.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/07-部署运行与配置说明.md)
5. [`docs/11-可观测性、日志追踪与消息通知治理.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/11-可观测性、日志追踪与消息通知治理.md)
6. [`docs/21-业务功能清单与验收标准.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/21-业务功能清单与验收标准.md)
7. [`docs/08-变更记录与技术债清单.md`](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/08-变更记录与技术债清单.md)

## 14. 风险与取舍

1. 对 legacy `$dp` 的 `DEVICE_NOT_FOUND` 无法全部在 decode 前阻断，这是协议事实限制，不是治理缺陷。
2. 统计改为 Redis 分钟桶后，若 Redis 不可用，本轮频次统计与相关告警会受阻；需继续按真实环境阻塞处理，不回退到 H2 或伪统计。
3. 样本归档减少后，少数长尾错误的样本数会低于旧口径，但这属于有意的治理结果；统计看频次，详情看样本，两者语义必须分开。
4. 外部 Broker 准入前移属于最优方案的一部分，但不应在本轮仓库内被宣称为已闭环交付。
