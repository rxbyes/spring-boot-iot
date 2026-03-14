# 协议规范

## 当前支持
- mqtt-json
- mqtt-json 兼容历史 `$dp` 主题明文 JSON 上报

## 预留协议
- tcp-hex
- modbus-tcp
- modbus-rtu

## ProtocolAdapter 抽象
核心方法：
- getProtocolCode()
- decode(payload, context)
- encode(message, context)

## MQTT JSON 上报示例
```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

## MQTT Topic 规范
- /sys/{productKey}/{deviceCode}/thing/property/post
- /sys/{productKey}/{deviceCode}/thing/event/post
- /sys/{productKey}/{deviceCode}/thing/property/reply
- /sys/{productKey}/{deviceCode}/thing/service/reply
- /sys/{productKey}/{deviceCode}/thing/status/post
- $dp（历史兼容主题）

## 历史 `$dp` 兼容说明
- `$dp` 不包含标准 topic 里的 `productKey` / `deviceCode` / `messageType`
- 当前主链路兼容两类 JSON：
  - 直接明文 JSON，上层 key 为设备编码，例如 `{"100054920":{...}}`
  - 加密 JSON 包装格式，例如 `{"header":{"appId":"62000001"},"bodies":{"body":"..."}}`
- 对于明文嵌套 JSON：
  - 协议层会从最外层 key 提取 `deviceCode`
  - 设备状态、GNSS、倾角仪、加速度等嵌套数据会被拍平成属性
  - 属性标识形如 `L1_QJ_1.X`、`L1_JS_1.gY`、`S1_ZT_1.ext_power_volt`
- 对于加密 JSON：
  - 当前已预留 `MqttPayloadDecryptor` 扩展点
  - 后续可按 `header.appId` 对接不同厂商密钥与解密算法
  - 若未配置对应解密器，会返回清晰业务异常

## 约束
- 所有协议最终都要转换为统一 DeviceUpMessage / DeviceDownMessage
- 协议层不直接写业务库
- 新协议优先新增适配器，不改主链路
