# 协议规范

## 当前协议
- mqtt-json
- tcp-hex（预留）
- modbus-tcp（预留）
- modbus-rtu（预留）

## ProtocolAdapter 抽象
核心接口：
- getProtocolCode()
- decode(payload, context)
- encode(message, context)

## MQTT JSON 上报格式
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
- 所有协议最终都要转换为统一的 DeviceUpMessage / DeviceDownMessage
- 协议层不直接写业务库
- 新协议优先通过新增适配器实现，不改主链路
