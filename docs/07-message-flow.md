# 消息流说明

## 上行链路
设备
-> 接入层
-> 协议适配器
-> DeviceMessageService
-> iot_device_message_log
-> iot_device_property
-> iot_device 状态更新

## 下行链路
平台
-> DownMessageDispatcher
-> ProtocolAdapter.encode
-> 设备

## 当前一期实现
- HTTP 入口：
  - `DeviceHttpController -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- MQTT 入口：
  - `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl`
- `DeviceMessageServiceImpl` 负责消息日志落库、最新属性更新、设备在线状态更新
- `DeviceSessionService` 负责 MQTT 会话在线态和最近活跃时间维护

## 关键原则
- 原始报文必须保留
- 最新属性独立维护
- 接入方式可以变化，但业务主链路保持稳定
