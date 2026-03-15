# API 文档

本文档记录当前已实现并已验证的 HTTP 调试接口，同时补充 Phase 2 已交付的 MQTT 上下行能力。

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
- HTTP 入口主要用于本地调试和回归。
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

### MQTT 上行接入

MQTT 上行不提供额外 HTTP API，设备消息直接通过 Broker 进入：

- 直连设备标准 topic：
  - `/sys/{productKey}/{deviceCode}/thing/property/post`
  - `/sys/{productKey}/{deviceCode}/thing/event/post`
  - `/sys/{productKey}/{deviceCode}/thing/status/post`
- 历史兼容 topic：`$dp`
- 子设备预留 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/event/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/status/post`

标准 topic payload 示例：
```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

`$dp` payload 示例：
```json
{
  "deviceCode": "demo-device-01",
  "temperature": 25.1,
  "humidity": 61
}
```

说明：
- MQTT 消息进入后仍走统一主链路：`RawDeviceMessage -> UpMessageDispatcher -> ProtocolAdapter -> DeviceMessageService`
- 当前已能识别直连设备 topic、历史 `$dp` 和子设备预留 topic
- 子设备 topic 目前只完成解析结构预留，不进入完整子设备业务处理
- 查询验证仍复用已有 HTTP 接口：
  - `GET /device/code/{deviceCode}`
  - `GET /device/{deviceCode}/properties`
  - `GET /device/{deviceCode}/message-logs`

### MQTT 下行发布
`POST /message/mqtt/down/publish`

请求体：
```json
{
  "productKey": "codex-down-product-02",
  "deviceCode": "codex-down-device-02",
  "qos": 1,
  "commandType": "property",
  "params": {
    "switch": 1,
    "targetTemperature": 23.0,
    "requestId": "task6-verify-001"
  }
}
```

说明：
- 当前由 `message` 模块负责下行发布，`protocol` 模块负责 `DeviceDownMessage` 编码。
- 若未显式传入 `topic`，系统按推荐规范自动拼接：
  - 属性下发：`/sys/{productKey}/{deviceCode}/thing/property/set`
  - 服务调用：`/sys/{productKey}/{deviceCode}/thing/service/{serviceIdentifier}/invoke`
- 子设备下行预留 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/service/{serviceIdentifier}/invoke`
- 若未显式传入 `protocolCode`，默认继承设备绑定协议。
- 当前只建立最小发布能力，不实现 ACK、重试、状态机。
- 当前下行发布入口只面向直连设备；子设备下行 topic 仅做规范预留。

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "protocolCode": "mqtt-json",
    "topic": "/sys/codex-down-product-02/codex-down-device-02/thing/property/set",
    "qos": 1,
    "retained": false,
    "deviceCode": "codex-down-device-02",
    "productKey": "codex-down-product-02",
    "commandType": "property"
  }
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

## 文件调试接口

### 查询设备文件快照
`GET /device/{deviceCode}/file-snapshots`

说明：
- 用于查看表 C.3 文件类消息在 Redis 中的最小持久化结果
- 当前返回文件描述、文件长度、Base64 文件流和更新时间

### 查询设备固件聚合结果
`GET /device/{deviceCode}/firmware-aggregates`

说明：
- 用于查看表 C.4 固件分包在 Redis 中的聚合状态
- 当前返回分包数量、已接收分包索引、重组结果、MD5 校验结果

成功响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "transferId": "0-2-1024",
      "deviceCode": "demo-device-01",
      "dataSetId": "ota-firmware",
      "fileType": "bin",
      "receivedPacketCount": 2,
      "totalPackets": 2,
      "md5Matched": true
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
