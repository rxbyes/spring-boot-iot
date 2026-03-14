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

## 设备新增
POST /device/add

### 请求体
```json
{
  "productKey": "demo-product",
  "deviceName": "演示设备-02",
  "deviceCode": "demo-device-02",
  "deviceSecret": "123456",
  "clientId": "demo-device-02"
}
```

## 查询设备
GET /device/{id}

## 按编码查询设备
GET /device/code/{deviceCode}

## HTTP 模拟设备上报
POST /message/http/report

### 请求体
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

## 查询设备最新属性
GET /device/{deviceCode}/properties

## 查询设备消息日志
GET /device/{deviceCode}/message-logs

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
