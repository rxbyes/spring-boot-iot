# MQTTX 真实环境联调手册

本手册用于在真实环境下通过 MQTTX 验证 MQTT 上报主链路。

## 1. 前提
- 后端使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 或对应环境变量覆盖后的真实环境配置。
- MySQL、Redis、MQTT Broker 可连通。
- MQTT 客户端使用 MQTTX。
- 不再使用 H2 或旧端到端验收链路作为兜底。

## 2. 启动后端

```bash
IOT_MQTT_ENABLED=true \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

关键日志应包含：
- MQTT 客户端连接成功
- 默认订阅 topic 成功
- 无 Broker 认证失败或重连异常

## 3. 准备测试数据
先创建产品和设备，确保产品协议为 `mqtt-json`。

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

## 4. MQTTX 连接参数
- Host：按 `application-dev.yml` 中 `iot.mqtt.broker-url`
- Port：按 Broker 配置
- Username：按 `IOT_MQTT_USERNAME`
- Password：按 `IOT_MQTT_PASSWORD`
- Client ID：建议唯一，例如 `mqttx-debug-001`

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
- 插入 1 条 `iot_message_log`
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
- 插入 1 条 topic 为 `$dp` 的 `iot_message_log`
- 更新属性值
- 刷新设备在线状态

## 7. HTTP / 数据验证

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
- `properties` 中可见最新属性值
- `message-logs` 中可见标准 topic 与 `$dp` 记录
- `onlineStatus = 1`
- `lastOnlineTime`、`lastReportTime` 已更新

## 8. 可选数据库验证
如需直接核对数据库，优先检查：
- `iot_device`
- `iot_device_property`
- `iot_message_log`

建议条件：
- `device_code = 'demo-device-01'`
- `topic in ('$dp', '/sys/demo-product/demo-device-01/thing/property/post')`

## 9. 可选 Redis 验证
如需直接核对 Redis，优先检查：

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
- MQTTX 已发送但数据库无变化：先看后端是否收到 MQTT 上行日志，再检查 topic、设备编码、Broker 权限。
- 后端收到消息但未进入主链路：重点排查设备不存在、设备协议不匹配、产品与设备绑定不一致。
- 标准 topic 无效：检查 `productKey`、`deviceCode` 是否与数据库一致。
- `$dp` 无效：检查 payload 中的 `deviceCode` 是否存在，且设备协议是否为 `mqtt-json`。
- 若切换到其他真实环境，只覆盖 `IOT_MQTT_*`、`IOT_MYSQL_*`、`IOT_REDIS_*` 环境变量，并同步核对 AES 商户密钥配置。
