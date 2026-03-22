# Phase 2 总结

本文档用于收口 Phase 2 已交付的 MQTT 能力，方便交接、演示和后续继续扩展。

## 1. Phase 2 已完成能力

### 1.1 MQTT 接入骨架
- 应用可连接共享 MQTT Broker
- 已建立 `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage -> UpMessageDispatcher` 桥接链路
- 保持 HTTP 上报主链路不变

### 1.2 MQTT Topic 解析
- 已支持直连设备标准 topic：
  - `/sys/{productKey}/{deviceCode}/thing/property/post`
  - `/sys/{productKey}/{deviceCode}/thing/event/post`
  - `/sys/{productKey}/{deviceCode}/thing/status/post`
  - `/sys/{productKey}/{deviceCode}/thing/property/reply`
  - `/sys/{productKey}/{deviceCode}/thing/service/reply`
- 已兼容历史 `$dp` topic
- 已为网关代子设备 topic 预留解析结构：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`

### 1.3 设备认证与会话基础
- 已具备最小设备认证服务骨架
- 已具备 MQTT 会话在线态维护能力
- Redis 会话 Key：
  - `iot:device:session:{deviceCode}`

### 1.4 MQTT 上行能力
- 真实 MQTT 上报可进入现有统一主链路
- 已验证：
  - `iot_message_log` 写入
  - `iot_device_property` 更新
  - `iot_device.online_status` 刷新
  - `last_online_time`、`last_report_time` 刷新

### 1.5 MQTT 下行能力
- 已新增 `POST /message/mqtt/down/publish`
- 已支持最小 QoS 和 retained 参数
- 已复用 `ProtocolAdapter.encode(...)` 编码 `DeviceDownMessage`
- 已完成共享 `dev` 环境下的真实下行发布验证

## 2. 当前推荐演示路径

### 2.1 上行演示
1. 启动 `dev` 环境应用并开启 MQTT
2. 使用 MQTTX 向 `/sys/{productKey}/{deviceCode}/thing/property/post` 发布属性消息
3. 使用现有 HTTP 接口验证设备属性、消息日志和在线状态

### 2.2 下行演示
1. 创建测试产品和设备
2. 调用 `POST /message/mqtt/down/publish`
3. 使用 MQTTX 订阅 `/sys/{productKey}/{deviceCode}/thing/property/set`
4. 验证收到编码后的 JSON 下行消息

## 3. 当前仍未实现的能力

- MQTT ACK 闭环
- 下行重试
- 指令状态机
- 子设备数据库建模
- 网关拓扑管理
- 子设备认证闭环
- 规则引擎
- 告警中心
- OTA 业务闭环
- TCP 接入
- Phase 3 相关能力

## 4. 交接建议

- 现场演示优先使用 [14-mqtt-live-runbook.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/14-mqtt-live-runbook.md)
- 协议和 topic 规范优先查看 [05-protocol.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/05-protocol.md)
- API 调试入口优先查看 [04-api.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/04-api.md)
- 测试与样例优先查看 [真实环境测试与验收手册.md](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/真实环境测试与验收手册.md)

## 5. 当前状态结论

当前仓库已经达到“Phase 2 可交接、可演示、可继续扩展”状态。

说明：
- MQTT 上下行主链路已建立并完成真实联调
- 子设备场景已完成 topic 与解析扩展点预留
- 后续可以在不破坏当前直连设备能力的前提下继续进入网关、子设备和平台化能力扩展
