# 协议规范

## 当前支持
- mqtt-json

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

## 约束
- 所有协议最终都要转换为统一 DeviceUpMessage / DeviceDownMessage
- 协议层不直接写业务库
- 新协议优先新增适配器，不改主链路
