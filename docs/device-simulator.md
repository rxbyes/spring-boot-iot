# 设备模拟器说明

## HTTP 上报接口
POST /message/http/report

## 示例请求
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

## curl 示例
```bash
curl -X POST http://localhost:9999/message/http/report \
  -H "Content-Type: application/json" \
  -d '{
    "protocolCode":"mqtt-json",
    "productKey":"demo-product",
    "deviceCode":"demo-device-01",
    "payload":"{\"messageType\":\"property\",\"properties\":{\"temperature\":26.5,\"humidity\":68}}",
    "topic":"/sys/demo-product/demo-device-01/thing/property/post",
    "clientId":"demo-device-01",
    "tenantId":"1"
  }'
```

## 验证接口
- GET /device/demo-device-01/properties
- GET /device/demo-device-01/message-logs
