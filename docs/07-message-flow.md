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
- 入口为 HTTP 模拟上报
- 通过 UpMessageDispatcher 分发
- 由 MqttJsonProtocolAdapter 做 decode
- DeviceMessageServiceImpl 负责落库与状态更新

## 关键原则
- 原始报文必须保留
- 最新属性独立维护
- 接入方式可以变化，但业务主链路保持稳定
