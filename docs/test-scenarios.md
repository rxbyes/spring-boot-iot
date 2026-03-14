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

## MQTT 历史 `$dp` 主题兼容验证

前置条件：
- 启动应用时开启 MQTT：`IOT_MQTT_ENABLED=true`
- Broker 已订阅 `$dp`
- 数据库中已存在对应 `deviceCode` 的设备，协议为 `mqtt-json`

示例 1：倾角仪 / 加速度明文数据
```bash
mosquitto_pub -h mqtt.ghlqf.com -p 1883 -u emqx -P '1qaz2wsx' -t '$dp' -m '{"100054920":{"L1_QJ_1":{"2026-03-14T07:04:03.000Z":{"X":3.15,"Y":-5.14,"Z":83.97,"angle":-6.03,"trend":236.18,"AZI":236.18}},"L1_JS_1":{"2026-03-14T07:04:03.000Z":{"gX":-0.04,"gY":0.18,"gZ":-0.04}}}}'
```

示例 2：设备状态数据
```bash
mosquitto_pub -h mqtt.ghlqf.com -p 1883 -u emqx -P '1qaz2wsx' -t '$dp' -m $'\x10{"100054920":{"S1_ZT_1":{"ext_power_volt":3.540,"solar_volt":6.185,"battery_dump_energy":1,"temp":0.0,"humidity":0,"lon":103.482170,"lat":36.180176,"signal_4g":-51,"sw_version":"V1.0.3(Jul 19 2023 16:48:13)-15522832","sensor_state":{"L1_JS_1":0,"L1_QJ_1":0,"L1_LF_1":3}}}}'
```

验证重点：
- `iot_device_message_log` 新增 `$dp` 记录
- `iot_device_property` 中出现拍平后的属性，例如：
  - `L1_QJ_1.X`
  - `L1_JS_1.gY`
  - `S1_ZT_1.ext_power_volt`
  - `S1_ZT_1.sensor_state.L1_LF_1`
- `iot_device.online_status = 1`
- `iot_device.last_online_time`、`last_report_time` 已刷新

加密数据说明：
- 当前代码已经能识别 `header.appId + bodies.body` 包装格式
- 但未提供具体厂商解密算法与密钥时，不会伪造解密逻辑
- 这类报文当前会返回“未配置 appId 对应的解密器”错误，等待后续按厂商接入真实解密实现

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
