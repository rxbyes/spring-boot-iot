# 测试场景

## 自动化测试
- 单元测试命令：`mvn -pl spring-boot-iot-device test -DskipTests=false`
- 覆盖类：`DeviceMessageServiceImplTest`
- 覆盖点：上报成功链路、已有属性更新、设备不存在、协议不匹配

## 端到端集成测试
- 测试类：`DeviceHttpReportE2EIntegrationTest`
- 启动方式：`SpringBootTest + WebApplicationContext + MockMvc`
- 测试数据源：H2 内存数据库，由 `application-e2e.yml` + `schema-e2e.sql` 自动初始化
- Redis 相关自动配置在 E2E 场景中已关闭，不依赖外部 Redis
- 独立用例：
- `shouldPersistReportOnSuccessPath`
- `shouldReturnBizErrorWhenProtocolIsInvalid`
- `shouldReturnBizErrorWhenDeviceDoesNotExist`
- 验证范围：产品新增、设备新增、HTTP 上报、设备查询、属性查询、消息日志查询、数据库落库结果、非法协议错误、不存在设备错误
- 运行命令：`mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=DeviceHttpReportE2EIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`

## HTTP 上报主链路手工联调步骤

### 步骤 1：初始化数据库
- 使用 [sql/init.sql](/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/sql/init.sql) 初始化本地数据库
- 确保当前应用连接的数据库中包含 `iot_product`、`iot_device`、`iot_device_property`、`iot_device_message_log` 等一期表

### 步骤 2：启动应用
- 启动命令：`mvn -pl spring-boot-iot-admin spring-boot:run`
- 如需切换环境，可通过 `--spring.profiles.active=dev|test|prod` 指定

### 步骤 3：创建产品
```bash
curl -X POST http://localhost:9999/device/product/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"demo-product",
    "productName":"演示产品",
    "protocolCode":"mqtt-json",
    "nodeType":1,
    "dataFormat":"JSON"
  }'
```

### 步骤 4：创建设备
```bash
curl -X POST http://localhost:9999/device/add \
  -H "Content-Type: application/json" \
  -d '{
    "productKey":"demo-product",
    "deviceName":"演示设备-01",
    "deviceCode":"demo-device-01",
    "deviceSecret":"123456",
    "clientId":"demo-device-01",
    "username":"demo-device-01",
    "password":"123456"
  }'
```

### 步骤 5：发送 HTTP 上报
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

### 步骤 6：查询属性与消息日志
```bash
curl http://localhost:9999/device/demo-device-01/properties
```

```bash
curl http://localhost:9999/device/demo-device-01/message-logs
```

### 步骤 7：校验数据库结果
- `iot_device_message_log` 至少新增 1 条上报记录
- `iot_device_property` 中应包含 `temperature` 和 `humidity`
- `iot_device.online_status = 1`
- `iot_device.last_online_time` 与 `last_report_time` 已更新

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

## 环境说明
- `DeviceHttpReportE2EIntegrationTest` 当前已通过，可作为主链路回归入口
- `DeviceMessageServiceImplTest` 在当前 JDK 17 环境下仍受 Mockito inline mock maker 自附加 agent 限制影响，不作为本轮收口阻塞项
