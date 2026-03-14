# 测试场景

## 场景 1：产品新增
- 创建 demo-product
- 校验 product_key 唯一性

## 场景 2：设备新增
- 创建设备 demo-device-01
- 校验 device_code 唯一性

## 场景 3：HTTP 属性上报
- 使用 docs/device-simulator.md 中请求体
- 预期新增 message_log
- 预期新增或更新 device_property
- 预期 device.online_status = 1

## 场景 4：重复属性上报
- 再次上报 temperature
- 预期更新现有 property，不重复插入

## 场景 5：非法协议编码
- protocolCode = bad-protocol
- 预期返回错误

## 场景 6：不存在设备
- deviceCode = missing-device
- 预期返回业务异常
