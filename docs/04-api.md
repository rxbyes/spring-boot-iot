# API 文档

## 统一返回格式
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 产品新增
POST /device/product/add

### 请求体
```json
{
  "productKey": "demo-product",
  "productName": "演示产品",
  "protocolCode": "mqtt-json",
  "nodeType": 1,
  "dataFormat": "JSON"
}
```

## 查询产品
GET /device/product/{id}

## HTTP 模拟设备上报
POST /message/http/report

### 请求体
```json
{
  "protocolCode": "mqtt-json",
  "productKey": "demo-product",
  "deviceCode": "demo-device-01",
  "payload": "{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}",
  "topic": "/sys/demo-product/demo-device-01/thing/property/post",
  "clientId": "demo-device-01",
  "tenantId": "1"
}
```

## 查询设备最新属性
GET /device/{deviceCode}/properties

## 查询设备消息日志
GET /device/{deviceCode}/message-logs
