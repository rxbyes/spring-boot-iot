# MQTTX 现场联调手册

本文档用于共享 `dev` 环境的快速联调。

目标：
- 用 MQTTX 向共享 Broker 发 1 条标准 `/sys/...` 消息
- 用 MQTTX 向共享 Broker 发 1 条历史 `$dp` 消息
- 验证消息已经进入现有业务主链路

## 1. 前提

- 后端使用 [spring-boot-iot-admin/src/main/resources/application-dev.yml](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-admin/src/main/resources/application-dev.yml)
- MySQL、Redis、MQTT 都直接使用该文件中的共享环境
- MQTT 客户端使用 MQTTX
- 不需要本地安装 Broker
- 不需要安装 `mosquitto_pub`

## 2. 启动后端

```bash
IOT_MQTT_ENABLED=true \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，确认日志中没有 MQTT 连接异常。

关键日志：
- `MQTT 客户端已连接`
- `MQTT 客户端已订阅主题`

## 3. 准备测试设备

先创建 1 个产品和 1 个设备。

产品示例：
```json
{
  "productKey": "demo-product",
  "productName": "演示产品",
  "protocolCode": "mqtt-json",
  "nodeType": 1,
  "dataFormat": "JSON"
}
```

设备示例：
```json
{
  "productKey": "demo-product",
  "deviceName": "演示设备-01",
  "deviceCode": "demo-device-01",
  "deviceSecret": "123456",
  "clientId": "demo-device-01",
  "username": "demo-device-01",
  "password": "123456"
}
```

如果前端调试台已启动，也可以直接在前端页面创建。

## 4. MQTTX 连接参数

- Host：`mqtt.ghlqf.com`
- Port：`1883`
- Username：`emqx`
- Password：按 `application-dev.yml` 中 `iot.mqtt.password`
- Client ID：任意唯一值，例如 `mqttx-debug-001`

连接成功后再发送消息。

## 5. 标准 Topic 联调

Topic：
```text
/sys/demo-product/demo-device-01/thing/property/post
```

Payload：
```json
{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}
```

发送后预期：
- 插入 1 条 `iot_device_message_log`
- 更新 `temperature`、`humidity`
- 刷新设备在线状态

## 6. 历史 `$dp` 联调

Topic：
```text
$dp
```

Payload：
```json
{"deviceCode":"demo-device-01","temperature":25.1,"humidity":61}
```

发送后预期：
- 插入 1 条 topic 为 `$dp` 的消息日志
- 更新 `temperature`、`humidity`
- 再次刷新设备在线状态

## 7. HTTP 验证

查询属性：
```bash
curl http://localhost:9999/device/demo-device-01/properties
```

查询消息日志：
```bash
curl http://localhost:9999/device/demo-device-01/message-logs
```

查询设备状态：
```bash
curl http://localhost:9999/device/code/demo-device-01
```

通过标准：
- `properties` 中能看到最新属性值
- `message-logs` 中能看到标准 topic 和 `$dp` 的记录
- 设备 `onlineStatus = 1`
- `lastOnlineTime`、`lastReportTime` 已更新

## 8. 可选数据库验证

如果你需要直接查共享 MySQL，可重点看 3 张表：
- `iot_device`
- `iot_device_property`
- `iot_device_message_log`

建议查询条件：
- `device_code = 'demo-device-01'`
- `topic in ('$dp', '/sys/demo-product/demo-device-01/thing/property/post')`

## 9. 可选 Redis 验证

如果你需要直接查共享 Redis，重点看：

```text
iot:device:session:demo-device-01
```

预期字段：
- `deviceCode`
- `clientId`
- `topic`
- `connected`
- `connectTime`
- `lastSeenTime`

## 10. 常见排查

- MQTTX 已发送但数据库没变化：
  - 先看是否出现 `MQTT 收到上行消息`
  - 如果没有，说明消息没有到达当前消费者，优先检查 Broker 连接、topic、客户端权限
- 出现 `MQTT 收到上行消息` 但没有 `MQTT 上行消息进入主链路成功`：
  - 继续看 `MQTT 消息分发失败`
  - 常见原因是 `设备不存在`、`设备协议不匹配`、`设备所属产品不匹配`、`不支持的 MQTT topic`
- 标准 topic 无效：检查 `productKey`、`deviceCode` 是否与数据库一致
- `$dp` 无效：检查 payload 里的 `deviceCode` 是否存在且协议为 `mqtt-json`
- 属性没更新但消息日志有记录：重点排查 payload 格式是否为合法 JSON

## 11. 最小回退验证

如果共享 MQTT 暂时不可用，先运行：

```bash
mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=DeviceMqttReportE2EIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

这个测试会直接走 `MqttMessageConsumer -> MqttTopicRouter -> RawDeviceMessage -> UpMessageDispatcher -> DeviceMessageServiceImpl`。
