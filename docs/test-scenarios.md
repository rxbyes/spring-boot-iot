# 测试场景

## 自动化测试
- 单元测试命令：`mvn -pl spring-boot-iot-device test -DskipTests=false`
- 覆盖类：`DeviceMessageServiceImplTest`
- 覆盖点：上报成功链路、已有属性更新、设备不存在、协议不匹配

## 端到端集成测试
- 测试类：`DeviceHttpReportE2EIntegrationTest`
- 启动方式：`SpringBootTest + RANDOM_PORT`
- 独立用例：
- `shouldPersistReportOnSuccessPath`
- `shouldReturnBizErrorWhenProtocolIsInvalid`
- `shouldReturnBizErrorWhenDeviceDoesNotExist`
- 验证范围：产品新增、设备新增、HTTP 上报、设备查询、属性查询、消息日志查询、数据库落库结果、非法协议错误、不存在设备错误
- 运行前需要提供环境变量：`IOT_MYSQL_URL`、`IOT_MYSQL_USERNAME`、`IOT_MYSQL_PASSWORD`、`IOT_REDIS_HOST`、`IOT_REDIS_PORT`、`IOT_REDIS_PASSWORD`、`IOT_REDIS_DATABASE`、`IOT_MQTT_BROKER_URL`、`IOT_MQTT_USERNAME`、`IOT_MQTT_PASSWORD`
- 运行命令：`mvn -pl spring-boot-iot-admin test -DskipTests=false -Dtest=DeviceHttpReportE2EIntegrationTest`

## 场景 1：产品新增
- 创建 demo-product
- 校验 product_key 唯一性

## 场景 2：设备新增
- 创建设备 demo-device-01
- 校验 device_code 唯一性
- 校验 productKey 必须存在

## 场景 3：HTTP 属性上报
- 使用 docs/device-simulator.md 中请求体
- 预期新增 message_log
- 预期新增或更新 device_property
- 预期 device.online_status = 1
- 预期 topic 正确写入 message_log
- 预期若存在 `iot_product_model` 定义，则属性 `property_name/value_type` 按模型写入

## 场景 4：重复属性上报
- 再次上报 temperature
- 预期更新现有 property，不重复插入
- 预期 `update_time` 变化，`id` 不变

## 场景 5：非法协议编码
- protocolCode = bad-protocol
- 预期返回错误

## 场景 6：不存在设备
- deviceCode = missing-device
- 预期返回业务异常
