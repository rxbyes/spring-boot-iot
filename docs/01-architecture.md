# 总体架构

## 架构分层
1. 接入层：MQTT、TCP、HTTP、WebSocket
2. 协议层：ProtocolAdapter、编解码、协议注册中心
3. 业务层：auth、system、device、gateway、rule、alarm、ota
4. 数据层：MySQL / TDengine、Redis、时序库
5. 运维层：日志、监控、链路追踪

## 设计原则
- 接入层与业务层解耦
- 协议采用插件化扩展
- 原始报文与最新属性分离存储
- 一期先模块化单体，后续可演进微服务
- spring-boot-iot-admin 为统一启动入口

## 当前推荐形态
- Spring Boot 4
- Java 17
- MyBatis Plus
- MySQL
- Redis
- HTTP 模拟设备接入
- 后续接入 MQTT / TCP

## 核心主链路
设备上报
-> DeviceHttpController
-> UpMessageDispatcher
-> ProtocolAdapterRegistry
-> MqttJsonProtocolAdapter
-> DeviceMessageServiceImpl
-> iot_message_log
-> iot_device_property
-> iot_device 在线状态更新
