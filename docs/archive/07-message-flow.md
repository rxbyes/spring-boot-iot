# 消息流说明

## 上行链路
设备
-> 接入层
-> 协议适配器
-> DeviceMessageService
-> iot_message_log
-> iot_device_property
-> iot_device 状态更新

## 统一 Trace 链路
- `TraceContextFilter` 会对 `/api/**`、`/message/**` 入口生成或透传 `X-Trace-Id`。
- HTTP 上报链路会直接复用当前请求 `traceId`。
- MQTT 上报链路会在消费端为每条消息生成并绑定 `traceId`，随后贯穿 `RawDeviceMessage -> DeviceUpMessage -> DeviceMessageServiceImpl`。
- `DeviceMessageServiceImpl` 会把 `traceId`、`deviceCode`、`productKey` 写入 `iot_device_message_log`。
- 后台异常审计会把同一 `traceId` 写入 `sys_audit_log` 的 `system_error` 记录，便于通过“系统日志 / 消息追踪”双页联动排障。

## Phase 2 MQTT 上行链路
- 直连设备：
  - `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- 历史 `$dp`：
  - `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage(legacy) -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- 子设备 topic 预留：
  - `MqttMessageConsumer -> MqttTopicRouter(识别 sub-device) -> RawDeviceMessage(gatewayDeviceCode/subDeviceCode) -> UpMessageDispatcher -> ProtocolAdapter`

## 下行链路
平台
-> DeviceDownController
-> DownMessageService
-> MqttDownMessagePublisher
-> ProtocolAdapter.encode
-> MqttMessageConsumer.publish
-> MQTT Broker
-> 设备

## Phase 2 MQTT 下行链路
- 直连设备：
  - `DeviceDownController -> DownMessageServiceImpl -> MqttDownMessagePublisher -> ProtocolAdapter.encode -> MqttMessageConsumer.publish`
- 子设备下行预留：
  - 当前仅定义 topic 规范和解析方向，未进入业务发布编排

## 当前一期实现
- HTTP 入口：
  - `DeviceHttpController -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- MQTT 入口：
  - `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- MQTT 下行入口：
  - `DeviceDownController -> DownMessageServiceImpl -> MqttDownMessagePublisher -> MqttJsonProtocolAdapter.encode -> MqttMessageConsumer.publish`
- `DeviceMessageServiceImpl` 负责消息日志落库、最新属性更新、设备在线状态更新
- `DeviceSessionService` 负责 MQTT 会话在线态和最近活跃时间维护
- `AuditLogBackendExceptionRecorder` 负责把 MQTT / 后台异步异常写入 `sys_audit_log`

## 关键原则
- 原始报文必须保留
- 最新属性独立维护
- 接入方式可以变化，但业务主链路保持稳定
- 网关 / 子设备场景优先在 topic 解析层预留结构，不提前侵入设备业务层
- Phase 2 以“真实 MQTT 上下行可运行”为边界，不提前实现 ACK、拓扑和规则联动

## 失败链路补充（2026-03-18）
- 当 MQTT 上行消息在 DeviceMessageServiceImpl.handleUpMessage 的前置校验阶段失败（如设备不存在、产品不匹配）时，除写入 sys_audit_log 的 system_error 外，还会补写一条 iot_device_message_log 记录。
- 该补写记录的 messageType=dispatch_failed，用于保障 /system-log 到 /message-trace 的 trace 串联排障。
- 对于设备不存在场景，记录采用占位 device_id=0，并保留 traceId/deviceCode/productKey/topic/payload 供检索。
