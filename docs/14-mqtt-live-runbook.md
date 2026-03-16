# MQTT 真实环境运行手册

本手册用于真实环境下的 MQTT 上下行联调与主链路验证。

## 1. 目标
- 验证 MQTT 上行消息可进入统一业务主链路
- 验证 MQTT 下行最小发布能力可用
- 验证网关 / 子设备 topic 解析扩展点未回归

## 2. 启动方式

```bash
IOT_MQTT_ENABLED=true \
IOT_MQTT_CLIENT_ID=codex-mqtt-verify-001 \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

其余连接配置直接使用 `application-dev.yml` 或对应环境变量覆盖。

## 3. 上行验证重点
- 标准 `/sys/{productKey}/{deviceCode}/thing/property/post` topic
- 历史 `$dp` topic
- AES 包裹报文解析与解密
- 统一落库到 `iot_message_log`
- 最新属性更新到 `iot_device_property`
- 设备在线状态刷新到 `iot_device`

## 4. 标准上行验收样例
Topic：

```text
/sys/codex-mqtt-product-01/codex-mqtt-device-01/thing/property/post
```

Payload：

```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

通过标准：
- `iot_message_log` 写入消息日志
- `iot_device_property` 写入最新属性
- `iot_device.online_status = 1`
- `last_online_time`、`last_report_time` 更新

## 5. `$dp` 验收样例
Topic：

```text
$dp
```

Payload：

```json
{
  "deviceCode": "codex-mqtt-device-01",
  "temperature": 25.1,
  "humidity": 61
}
```

通过标准：
- `iot_message_log` 写入 `$dp` 日志
- 属性落入 `iot_device_property`
- 设备在线状态刷新

## 6. 下行发布样例

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

通过标准：
- 返回 `code = 200`
- Broker 能收到编码后的下行 JSON
- 当前不要求设备 ACK

## 7. 网关 / 子设备扩展点
保留验证的 topic：
- `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
- `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`

当前要求：
- `MqttTopicParser` 能正确解析 `routeType`、`gatewayDeviceCode`、`subDeviceCode`
- 不要求本轮完成完整子设备业务验收

## 8. 验收证据
- 后端启动日志
- MQTTX 发布或订阅截图
- 关键接口请求 / 响应
- `iot_message_log` / `iot_device_property` / `iot_device` 核对截图
- Redis 设备会话截图（如启用）
