# MQTT 真实联调运行手册

本文档记录 Phase 2 的一次真实 MQTT 联调过程，并沉淀为可复用运行手册。当前重点覆盖：
- MQTT 上行真实联调
- MQTT 下行真实联调
- 子设备 topic 预留口径

## 1. 联调目标

验证真实 MQTT 上下行能力已经可用，并明确子设备场景的当前边界。

上行目标：
- `iot_device_message_log` 写入消息日志
- `iot_device_property` 更新最新属性
- `iot_device.online_status` 刷新为在线
- `last_online_time`、`last_report_time` 正常更新

下行目标：
- 平台可向指定 MQTT topic 发布消息
- 可通过 MQTT 客户端直接订阅到平台下行消息

子设备预留目标：
- 明确网关代子设备 topic 规范
- 明确当前仅支持解析结构预留，不支持完整业务处理

本次验证基于共享 `dev` 环境完成，不是代码路径推演，也不是单元测试替代。

## 2. 启动命令

启动 `spring-boot-iot-admin`：

```bash
IOT_MQTT_ENABLED=true \
IOT_MQTT_CLIENT_ID=codex-mqtt-verify-001 \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，应至少看到以下日志：
- `MQTT 客户端已连接`
- `MQTT 客户端已订阅主题`

## 3. 环境变量

本次真实联调实际使用的环境变量：
- `IOT_MQTT_ENABLED=true`
- `IOT_MQTT_CLIENT_ID=codex-mqtt-verify-001`

其余连接配置直接使用 [application-dev.yml](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-admin/src/main/resources/application-dev.yml)：
- MySQL
- Redis
- MQTT Broker

说明：
- 使用唯一 `clientId` 是为了避免共享 Broker 上的消费端互踢
- 本次未额外覆盖 `IOT_MQTT_BROKER_URL`、`IOT_MQTT_USERNAME`、`IOT_MQTT_PASSWORD`

## 4. 测试产品与设备准备方式

本次使用一套干净测试数据，避免被共享环境中的历史数据干扰。

### 4.1 创建产品

请求：

```bash
curl -X POST http://localhost:9999/device/product/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"codex-mqtt-product-01",
    "productName":"Codex MQTT Product 01",
    "protocolCode":"mqtt-json",
    "nodeType":1,
    "dataFormat":"JSON",
    "manufacturer":"Codex",
    "description":"Real MQTT verification product"
  }'
```

### 4.2 创建设备

请求：

```bash
curl -X POST http://localhost:9999/device/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"codex-mqtt-product-01",
    "deviceName":"Codex MQTT Device 01",
    "deviceCode":"codex-mqtt-device-01",
    "deviceSecret":"123456",
    "clientId":"codex-mqtt-device-01",
    "username":"codex-mqtt-device-01",
    "password":"123456",
    "firmwareVersion":"1.0.0",
    "ipAddress":"127.0.0.1",
    "address":"codex-lab"
  }'
```

### 4.3 设备准备完成判定

查询：

```bash
curl http://localhost:9999/device/code/codex-mqtt-device-01
```

关键期望：
- `deviceCode = codex-mqtt-device-01`
- `protocolCode = mqtt-json`
- `onlineStatus = 0`

## 5. 实际使用的 Topic

本次真实联调成功使用的标准 MQTT topic：

```text
/sys/codex-mqtt-product-01/codex-mqtt-device-01/thing/property/post
```

## 6. 实际使用的 Payload

本次真实联调成功使用的 payload：

```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

## 7. 上行验证接口与验证结果

### 7.1 查询属性

请求：

```bash
curl http://localhost:9999/device/codex-mqtt-device-01/properties
```

本次验证结果：
- `temperature = 26.5`
- `humidity = 68`

说明：
- 证明 `iot_device_property` 已更新

### 7.2 查询消息日志

请求：

```bash
curl http://localhost:9999/device/codex-mqtt-device-01/message-logs
```

本次验证结果：
- `messageType = property`
- `topic = /sys/codex-mqtt-product-01/codex-mqtt-device-01/thing/property/post`
- `payload` 为本次 MQTT 上报内容

说明：
- 证明 `iot_device_message_log` 已写入

### 7.3 查询设备状态

请求：

```bash
curl http://localhost:9999/device/code/codex-mqtt-device-01
```

本次验证结果：
- `onlineStatus = 1`
- `lastOnlineTime` 已更新
- `lastReportTime` 已更新

说明：
- 证明 `iot_device` 在线状态和最近上报时间已刷新

## 8. 下行联调最小步骤

### 8.1 创建下行测试数据

```bash
curl -X POST http://localhost:9999/device/product/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"codex-down-product-02",
    "productName":"Codex 下行产品-02",
    "protocolCode":"mqtt-json",
    "nodeType":1,
    "dataFormat":"JSON"
  }'
```

```bash
curl -X POST http://localhost:9999/device/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"codex-down-product-02",
    "deviceName":"Codex 下行设备-02",
    "deviceCode":"codex-down-device-02",
    "deviceSecret":"123456",
    "clientId":"codex-down-device-02",
    "username":"codex-down-device-02",
    "password":"123456"
  }'
```

### 8.2 调用下行发布接口

```bash
curl -X POST http://localhost:9999/message/mqtt/down/publish \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"codex-down-product-02",
    "deviceCode":"codex-down-device-02",
    "qos":1,
    "commandType":"property",
    "params":{
      "switch":1,
      "targetTemperature":23.0,
      "requestId":"task6-verify-001"
    }
  }'
```

### 8.3 订阅验证

- 订阅 topic：
```text
/sys/codex-down-product-02/codex-down-device-02/thing/property/set
```

- 预期 payload：
```json
{
  "messageId": "1773507184482",
  "commandType": "property",
  "serviceIdentifier": null,
  "params": {
    "switch": 1,
    "targetTemperature": 23.0,
    "requestId": "task6-verify-001"
  }
}
```

说明：
- `messageId` 为运行时生成值，每次发布都会变化
- 当前下行联调只验证“平台成功发布到 Broker”，不要求设备 ACK

## 9. 子设备 Topic 预留口径

- 子设备上报：
```text
/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post
```

- 子设备下发：
```text
/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set
```

当前说明：
- 已完成 parser 和统一上下文结构预留
- 默认订阅范围仍保持直连设备 topic
- 当前不实现子设备拓扑、认证和完整业务流转

## 10. 常见失败原因与排查建议

### 10.1 `iot.mqtt.enabled=false`

现象：
- 控制台出现 `跳过 MQTT 客户端启动: iot.mqtt.enabled=false`

处理：
- 确认启动命令里显式带了 `IOT_MQTT_ENABLED=true`

### 10.2 MQTT 客户端无日志

现象：
- MQTTX 能自收自发
- 后端没有 `MQTT 收到上行消息`

处理：
- 检查后端是否已连接并订阅成功
- 检查 MQTT Broker 地址、用户名、密码
- 检查 `clientId` 是否与其他实例冲突，建议使用唯一值

### 10.3 `设备所属产品不匹配`

现象：
- 控制台出现 `设备所属产品不匹配: {deviceCode}`

原因：
- topic 中的 `productKey` 与设备实际绑定产品不一致

处理：
- 先查 `deviceCode -> productKey` 映射，再按真实产品编码发 topic

### 10.4 `设备不存在`

现象：
- 控制台出现 `设备不存在: {deviceCode}`

原因：
- 上报中的 `deviceCode` 在当前库中不存在
- 共享 Broker 上可能混入其他设备报文

处理：
- 使用一套独立的测试产品和设备
- 优先验证标准 `/sys/...` topic，不要先用共享噪声较多的 `$dp`

### 10.5 共享环境 Redis 不可达

现象：
- 应用启动时 Redisson 初始化失败

处理：
- 这通常是运行环境网络限制，不是业务代码问题
- 需要在能访问共享 Redis 的环境中启动 `spring-boot-iot-admin`

### 10.6 产品和设备并行创建导致设备创建失败

现象：
- 刚创建完产品就马上创建设备，设备返回 `产品不存在`

原因：
- 两个请求并行发送时，设备创建可能先于产品提交完成

处理：
- 先创建产品，成功后再顺序创建设备

## 11. 本次联调结论

本次 Phase 2 真实联调已完成，结论如下：
- 真实 MQTT 消息已成功发布到共享 Broker
- `MqttMessageConsumer -> UpMessageDispatcher -> MqttJsonProtocolAdapter -> DeviceMessageServiceImpl` 已在真实环境跑通
- `iot_device_message_log` 已写入
- `iot_device_property` 已更新
- `iot_device.online_status`、`last_online_time`、`last_report_time` 已刷新
- `POST /message/mqtt/down/publish` 已在真实环境完成发布验证
- 子设备场景已完成 topic 规范和解析扩展点预留

因此：
- Phase 2 MQTT 能力已达到可演示状态
- 当前仓库已具备进入下一阶段前的交接条件
