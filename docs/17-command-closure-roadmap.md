# 指令闭环能力设计

本文档用于设计 Phase 3 第一优先方向“指令闭环能力”的最小 MVP 范围、状态流转、模块影响范围、数据表建议、ACK 处理方式和任务拆解。

当前文档只做设计收口，不代表这些能力已经实现。

## 1. 指令闭环能力的目标

在当前已具备 MQTT 下行最小发布能力的基础上，补出“平台发命令后，能够追踪结果”的最小闭环。

本方向的目标是：
- 平台每次下发命令都能形成可追踪记录
- 平台可以识别设备回执或 ACK，并更新命令状态
- 平台可以判断“成功、失败、超时”这三类最小结果
- 先围绕直连设备 MQTT 打通完整闭环

核心不是做复杂调度，而是先把“发出去的命令有没有结果”这件事跑通。

## 2. 为什么它适合作为 Phase 3 第一优先

它适合作为第一优先，原因有 4 点：

### 2.1 它直接建立在 Phase 2 成果上
- 当前已经有：
  - MQTT 下行发布入口
  - MQTT 上行接入链路
  - topic 解析能力
  - 设备在线状态基础

也就是说，命令闭环不需要重新开辟大范围基础设施，可以直接在当前能力之上补最小追踪。

### 2.2 它最容易形成业务闭环
相比规则引擎和网关 / 子设备闭环，指令闭环的路径最短：

平台下发  
-> 设备接收  
-> 设备回执  
-> 平台更新状态

这条链路一旦跑通，平台从“会发消息”升级为“知道消息结果”。

### 2.3 它对后续两条路线都有支撑作用
- 规则引擎后续要触发平台下行动作
- 网关 / 子设备后续也要继承命令生命周期能力

因此先做命令闭环，后续两条路线的落地会更稳。

### 2.4 它更适合保持最小可运行实现
- 先不做重试
- 先不做批量命令
- 先不做复杂状态机
- 先不做任务调度平台

这样可以用最小成本换到最大的交付价值。

## 3. 当前项目已具备的基础

### 3.1 已有下行能力
- 已存在 `POST /message/mqtt/down/publish`
- 已存在 `MqttDownMessagePublisher`
- 已复用 `ProtocolAdapter.encode(...)`
- 已在真实环境验证平台可以成功向 Broker 发布消息

### 3.2 已有上行能力
- 已存在 MQTT 上行消费链路
- 已支持：
  - `property/post`
  - `event/post`
  - `status/post`
  - `property/reply`
  - `service/reply`
- 已存在统一主链路：
  - `RawDeviceMessage -> UpMessageDispatcher -> ProtocolAdapter -> DeviceMessageService`

### 3.3 已有可复用的数据与上下文
- `iot_device_message_log`
- `iot_device_property`
- `iot_device`
- Redis 设备会话
- topic 解析结果

### 3.4 当前还缺的核心点
- 没有命令记录表
- 没有命令生命周期状态
- 没有 ACK / reply 与已发送命令的关联规则
- 没有超时判定和失败回填

## 4. 最小 MVP 范围

建议把 MVP 范围严格控制在以下边界内：

### 4.1 覆盖范围
- 只覆盖直连设备 MQTT
- 只覆盖单次命令发送
- 只覆盖平台主动下发产生的命令记录
- 只覆盖 MQTT `property/reply` 和 `service/reply`

### 4.2 MVP 需要具备的结果
- 下发前创建命令记录
- 发布成功后更新为“已发送”
- 收到 ACK / reply 后更新为“成功”或“失败”
- 在规定时间内未收到回执时，更新为“超时”

### 4.3 MVP 不做的内容
- 不做重试
- 不做补发
- 不做批量命令
- 不做优先级队列
- 不做复杂状态机
- 不做网关 / 子设备命令闭环
- 不做规则自动触发命令

## 5. 指令生命周期状态设计

建议先采用最小状态集合：

### 5.1 推荐状态
- `CREATED`
- `SENT`
- `SUCCESS`
- `FAILED`
- `TIMEOUT`

### 5.2 状态含义
- `CREATED`
  - 命令记录已创建，但尚未真正发送到 Broker
- `SENT`
  - 平台已经成功调用 MQTT 发布器完成发送
- `SUCCESS`
  - 平台收到设备回执，且回执表示成功
- `FAILED`
  - 平台收到设备回执，且回执明确表示失败
- `TIMEOUT`
  - 命令在约定时间内未收到有效回执

### 5.3 推荐状态流转

```text
CREATED -> SENT -> SUCCESS
CREATED -> SENT -> FAILED
CREATED -> SENT -> TIMEOUT
CREATED -> FAILED
```

说明：
- `CREATED -> FAILED` 用于“尚未发出就失败”的场景，例如参数校验失败或编码失败
- 不建议 MVP 阶段引入更多中间态

## 6. 建议新增或扩展的数据表

建议新增：
- `iot_command_record`

### 6.1 推荐字段
- `id`
- `command_id`
- `device_id`
- `device_code`
- `product_key`
- `gateway_device_code`
- `sub_device_code`
- `topic`
- `command_type`
- `service_identifier`
- `request_payload`
- `reply_payload`
- `qos`
- `retained`
- `status`
- `send_time`
- `ack_time`
- `timeout_time`
- `error_message`
- `tenant_id`
- `remark`
- `create_time`
- `update_time`

### 6.2 字段设计建议
- `command_id`
  - 作为业务主键，用于和设备回执关联
- `request_payload`
  - 保留原始下发内容，便于排障
- `reply_payload`
  - 保留回执内容，便于审计和定位
- `gateway_device_code` / `sub_device_code`
  - 先预留，MVP 阶段可以为空
- `timeout_time`
  - 用于表达最小超时判定点

### 6.3 是否需要改现有表
- 不建议在 MVP 阶段强行改 `iot_device_message_log`
- 命令生命周期应优先独立建表
- 后续若需要审计联查，可通过 `command_id` 或 topic 做关联

## 7. 影响的模块

### 7.1 直接影响模块
- `spring-boot-iot-message`
- `spring-boot-iot-protocol`
- `spring-boot-iot-device`
- `spring-boot-iot-admin`

### 7.2 每个模块建议职责

`spring-boot-iot-message`
- 负责下行发布入口
- 负责桥接 ACK / reply 上行消息
- 不负责命令持久化细节

`spring-boot-iot-protocol`
- 负责 reply / ack 解析
- 负责统一协议模型映射
- 不负责状态流转和数据库写入

`spring-boot-iot-device`
- 负责命令记录创建、状态更新、超时处理
- 负责最小命令查询能力

`spring-boot-iot-admin`
- 负责启动和最小联调配置

## 8. ACK / reply 的处理建议

### 8.1 建议优先复用现有 topic
- `property/reply`
- `service/reply`

原因：
- 当前 topic 解析已经支持
- 不需要额外定义新的接入协议
- 更贴近已有 MQTT 直连设备链路

### 8.2 ACK 关联建议
建议优先使用以下顺序关联命令：

1. reply payload 中显式携带 `messageId` / `commandId`
2. 若协议中没有显式命令标识，再结合：
   - `deviceCode`
   - `topic`
   - `commandType`
   - 最近一条 `SENT` 命令

说明：
- MVP 阶段优先依赖显式 `messageId`
- 若没有显式标识，兜底关联要非常保守，避免串单

### 8.3 reply 结果建议
建议先只识别两类语义：
- 成功
- 失败

可优先兼容以下字段：
- `success`
- `code`
- `msg`
- `message`

推荐判定策略：
- `success = true` 或 `code = 0/200` 视为成功
- 明确错误码或 `success = false` 视为失败
- 无法判定的回执先记日志，不强行更新为成功

## 9. 超时与失败处理建议

### 9.1 超时处理建议
- 每条命令在 `SENT` 时就计算一个 `timeout_time`
- MVP 阶段可采用固定超时时间，例如 30 秒或 60 秒
- 到达超时时间仍未收到有效 reply，则更新为 `TIMEOUT`

### 9.2 超时实现建议
MVP 阶段有两种可选方案：

方案 A：简单定时扫描
- 定时扫描 `status = SENT and timeout_time < now()`
- 批量更新为 `TIMEOUT`

方案 B：请求后延迟检查
- 简单但不稳
- 不推荐长期使用

建议优先方案 A，因为：
- 实现简单
- 对后续重试扩展更友好

### 9.3 失败处理建议
失败分为两类：

发送失败：
- 编码失败
- MQTT 发布失败
- 设备不存在 / 协议不匹配

设备回执失败：
- 设备明确返回失败
- 回执内容解析出错误码或错误消息

建议都统一落到 `FAILED`，并写入 `error_message`

## 10. 推荐拆分成哪些 Task

### Task 1：命令记录模型与表设计
- 定义 `iot_command_record`
- 定义最小状态枚举
- 明确 `command_id` 生成规则

### Task 2：下行发布时写入命令记录
- 发布前创建 `CREATED`
- 发布成功更新为 `SENT`
- 发布失败更新为 `FAILED`

### Task 3：reply / ack 解析与命令关联
- 接入 `property/reply`
- 接入 `service/reply`
- 通过 `messageId` / `commandId` 关联命令

### Task 4：命令状态回填
- 收到成功回执更新为 `SUCCESS`
- 收到失败回执更新为 `FAILED`
- 保留 `reply_payload`

### Task 5：超时扫描与状态更新
- 扫描超时命令
- 更新为 `TIMEOUT`
- 保留最小超时日志

### Task 6：最小查询与联调验证
- 提供命令记录查询接口或最小调试入口
- 完成一次真实命令闭环联调

## 11. 哪些能力先做最小实现，哪些后续增强

### 11.1 先做最小实现的能力
- 命令记录表
- 5 个最小状态
- 基于 `messageId` 的 ACK 关联
- 固定超时策略
- 单设备单命令场景
- 直连设备 MQTT reply 闭环

### 11.2 后续增强的能力
- 命令重试
- 批量命令
- 命令优先级
- 多协议 ACK 统一抽象
- 网关 / 子设备命令闭环
- 规则引擎自动触发命令
- 更细粒度状态机

## 12. 建议先做哪几个 Task

我建议先做下面 3 个 Task：

1. `Task 1：命令记录模型与表设计`
2. `Task 2：下行发布时写入命令记录`
3. `Task 3：reply / ack 解析与命令关联`

原因：
- 这 3 个 Task 一起能把“记录命令 -> 发出去 -> 找到回执”这条最关键主链路先立住
- 它们是后续 `SUCCESS / FAILED / TIMEOUT` 全部能力的前提
- 如果没有先把命令主键、状态模型和关联规则设计清楚，后面会反复返工

在这 3 个 Task 稳定后，再继续：

4. `Task 4：命令状态回填`
5. `Task 5：超时扫描与状态更新`
6. `Task 6：最小查询与联调验证`

## 13. 结论

指令闭环能力非常适合作为 Phase 3 的第一优先方向。

建议路线是：
- 先把命令记录模型和 ACK 关联机制做清楚
- 再补成功 / 失败 / 超时状态回填
- 最后做最小联调与查询验证

这样既能保持最小可运行实现，也能为后续网关 / 子设备闭环和规则引擎能力打下稳定基础。
