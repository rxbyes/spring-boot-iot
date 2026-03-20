# 协议示例

## mqtt-json 属性上报
### Topic
/sys/demo-product/demo-device-01/thing/property/post

### Payload
```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68,
    "switch": true
  }
}
```

## 事件上报
### Topic
/sys/demo-product/demo-device-01/thing/event/post

### Payload
```json
{
  "messageType": "event",
  "events": {
    "overheat": {
      "level": 2,
      "value": 88.2
    }
  }
}
```

## 下行指令
```json
{
  "messageId": "cmd-001",
  "commandType": "service",
  "serviceIdentifier": "reboot",
  "params": {}
}
```
