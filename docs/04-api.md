# API 文档

本文档只记录当前 Phase 1 已实现并已联调验证的接口，不包含后续规则、告警、OTA、MQTT 真接入等能力。

## 统一返回格式
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 产品接口

### 新增产品
`POST /device/product/add`

请求体：
```json
{
  "productKey": "demo-product",
  "productName": "演示产品",
  "protocolCode": "mqtt-json",
  "nodeType": 1,
  "dataFormat": "JSON",
  "manufacturer": "Codex",
  "description": "演示产品"
}
```

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 2001,
    "productKey": "demo-product",
    "productName": "演示产品",
    "protocolCode": "mqtt-json"
  }
}
```

### 根据 ID 查询产品
`GET /device/product/{id}`

## 设备接口

### 新增设备
`POST /device/add`

请求体：
```json
{
  "productKey": "demo-product",
  "deviceName": "演示设备-01",
  "deviceCode": "demo-device-01",
  "deviceSecret": "123456",
  "clientId": "demo-device-01",
  "username": "demo-device-01",
  "password": "123456",
  "firmwareVersion": "1.0.0",
  "ipAddress": "127.0.0.1",
  "address": "lab-a"
}
```

### 根据 ID 查询设备
`GET /device/{id}`

### 根据 deviceCode 查询设备
`GET /device/code/{deviceCode}`

## 消息接入接口

### HTTP 模拟设备上报
`POST /message/http/report`

请求体：
```json
{
  "protocolCode": "mqtt-json",
  "productKey": "demo-product",
  "deviceCode": "demo-device-01",
  "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}",
  "topic": "/sys/demo-product/demo-device-01/thing/property/post",
  "clientId": "demo-device-01",
  "tenantId": "1"
}
```

说明：
- 当前 Phase 1 只打通 HTTP 模拟上报入口。
- `protocolCode` 目前联调验证通过的是 `mqtt-json`。
- `payload` 当前以字符串形式承载原始 JSON。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 设备属性与消息日志接口

### 查询设备最新属性
`GET /device/{deviceCode}/properties`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "identifier": "temperature",
      "propertyName": "temperature",
      "propertyValue": "26.5",
      "valueType": "double"
    },
    {
      "identifier": "humidity",
      "propertyName": "humidity",
      "propertyValue": "68",
      "valueType": "int"
    }
  ]
}
```

### 查询设备消息日志
`GET /device/{deviceCode}/message-logs`

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "messageType": "property",
      "topic": "/sys/demo-product/demo-device-01/thing/property/post",
      "payload": "{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}"
    }
  ]
}
```

## 典型错误返回

### 非法协议编码
```json
{
  "code": 500,
  "msg": "未找到协议适配器: bad-protocol"
}
```

### 设备不存在
```json
{
  "code": 500,
  "msg": "设备不存在: missing-device"
}
```
