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

- 测试类：`DeviceMqttReportE2EIntegrationTest`
- 启动方式：`SpringBootTest`
- 测试数据源：H2 内存数据库，由 `application-e2e.yml` + `schema-e2e.sql` 自动初始化
- AES 商户配置：`application-e2e.yml` 中已内置最小 `spring.cloud.aes.merchants.62000001`
- 覆盖范围：
  - 标准 `/sys/...` MQTT 上报
  - `$dp` 简单明文上报
  - `$dp` 嵌套遥测上报
  - `$dp` 状态报文上报
  - `$dp` 加密包裹 + 内层二进制帧上报
- 运行命令：`mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=DeviceMqttReportE2EIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`

- 测试类：`MqttDeviceAesDataTests`
- 启动方式：`SpringBootTest(classes = TestApplication.class)`
- 覆盖范围：
  - `spring.cloud.aes` 配置绑定
  - `aesEncryptors` 命名 Bean 注入
  - 外层 MQTT 帧头解析
  - `header.appId` / `bodies.body` 拆分
  - 按 `appId` 选择解密器
  - 解密后再次执行帧解析
  - 明文 JSON 完整性校验
  - AES 签名校验
- 运行命令：`mvn -pl spring-boot-iot-admin -am test -DskipTests=false -Dtest=MqttDeviceAesDataTests -Dsurefire.failIfNoSpecifiedTests=false`

- 测试类：`MqttPayloadSecurityValidatorTest`
- 启动方式：协议层单元测试
- 覆盖范围：
  - 时间戳时间窗校验
  - nonce 防重放校验
  - MD5 带密钥摘要签名
  - 字节流签名
- 运行命令：`mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttPayloadSecurityValidatorTest -Dsurefire.failIfNoSpecifiedTests=false`

- 测试类：`MqttBinaryFormatParserTest`
- 启动方式：协议层单元测试
- 覆盖范围：
  - 表 C.3 文件描述 + 文件流解析
  - 表 C.4 固件升级分包解析
- 运行命令：`mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttBinaryFormatParserTest -Dsurefire.failIfNoSpecifiedTests=false`

- 测试类：`MqttJsonProtocolAdapterTest`
- 启动方式：协议层单元测试
- 覆盖范围：
  - 历史嵌套明文属性解析
  - 表 C.2 时间序列值解析
  - 表 C.3 文件消息标准化到 `DeviceUpMessage.filePayload`
  - 表 C.4 固件分包标准化到 `DeviceFilePayload.firmwarePacket`
- 运行命令：`mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest -Dsurefire.failIfNoSpecifiedTests=false`

- 测试类：`MqttPayloadDecryptorRegistryTest`
- 启动方式：协议层单元测试
- 覆盖范围：
  - DES 解密
  - 3DES 解密
- 运行命令：`mvn -pl spring-boot-iot-protocol test -DskipTests=false -Dtest=MqttPayloadDecryptorRegistryTest -Dsurefire.failIfNoSpecifiedTests=false`

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

## MQTT 标准 Topic 手工联调步骤

### 步骤 1：启动应用
- 使用 `application-dev.yml` 中提供的共享联调环境
- 启动时开启 MQTT，并激活 `dev` 配置：
```bash
IOT_MQTT_ENABLED=true \
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

共享环境关键连接信息：
- MySQL：`8.130.107.120:3306/rm_iot`
- Redis：`8.130.107.120:6379`，database `8`
- MQTT Broker：`mqtt.ghlqf.com:1883`
- MQTT 用户名：`emqx`

### 步骤 2：创建产品和设备
- 先按前文 HTTP 联调步骤创建产品和设备
- 确保产品协议为 `mqtt-json`

### 步骤 3：使用 MQTTX 发布标准 Topic 上报
- 在 MQTTX 中新建连接：
  - Host：`mqtt.ghlqf.com`
  - Port：`1883`
  - Username：`emqx`
  - Password：使用 `application-dev.yml` 中的 `iot.mqtt.password`
- 发布 Topic：`/sys/demo-product/demo-device-01/thing/property/post`
- 发布 Payload：
```json
{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}
```

### 步骤 4：查询验证结果
```bash
curl http://localhost:9999/device/demo-device-01/properties
```

```bash
curl http://localhost:9999/device/demo-device-01/message-logs
```

```bash
curl http://localhost:9999/device/code/demo-device-01
```

### 步骤 5：校验通过标准
- `iot_device_message_log` 至少新增 1 条 topic 为 `/sys/demo-product/demo-device-01/thing/property/post` 的记录
- `iot_device_property` 中应包含 `temperature=26.5` 和 `humidity=68`
- `iot_device.online_status = 1`
- `iot_device.last_online_time`、`last_report_time` 已刷新
- 若 Redis 可用，`iot:device:session:demo-device-01` 中应存在 `connected=true`、`lastSeenTime` 已更新

## MQTT 历史 `$dp` 主题兼容验证

前置条件：
- 启动应用时开启 MQTT，并使用 `dev` 配置
- 使用 MQTTX 连接 `mqtt.ghlqf.com:1883`
- 数据库中已存在对应 `deviceCode` 的设备，协议为 `mqtt-json`
- 若验证加密报文，需确保 `spring.cloud.aes.merchants.{appId}` 已配置对应密钥

示例 1：倾角仪 / 加速度明文数据
- Topic：`$dp`
- Payload：
```json
{"100054920":{"L1_QJ_1":{"2026-03-14T07:04:03.000Z":{"X":3.15,"Y":-5.14,"Z":83.97,"angle":-6.03,"trend":236.18,"AZI":236.18}},"L1_JS_1":{"2026-03-14T07:04:03.000Z":{"gX":-0.04,"gY":0.18,"gZ":-0.04}}}}
```

示例 2：设备状态数据
- Topic：`$dp`
- Payload 以 MQTTX 原始字符串发送，内容如下：
```text
\u0010{"100054920":{"S1_ZT_1":{"ext_power_volt":3.540,"solar_volt":6.185,"battery_dump_energy":1,"temp":0.0,"humidity":0,"lon":103.482170,"lat":36.180176,"signal_4g":-51,"sw_version":"V1.0.3(Jul 19 2023 16:48:13)-15522832","sensor_state":{"L1_JS_1":0,"L1_QJ_1":0,"L1_LF_1":3}}}}
```

示例 3：加密包裹数据
```json
{
  "header": {
    "appId": "62000001"
  },
  "bodies": {
    "body": "PTOLy04o/stDufUYFo5s3trFmwXQj9R85OUt6LnCbQzJrvVKuDIkMkXxiauKs4AIPcpopHQuf4ZjJ8ScsF4zA6gtDisacA9yJjreDh2+KO+YHFnVE5CueOHigyX1LtOfutbpbF0KdQMXJRkt8EA7WnefdlHJx00J84FVq8NlEAE4TgRCqOtJqAcw3J0/qn0JCDX1roDvjNVXglW9DLklYmiVgLSPd+gDgiBFxFcVARpIv4aGyrFBBcOEqC4CgCMiHR40hrTDgqhle9CMjlbBtGt+4cL36tH1TxTgD7xTsqnICvcQ/uiAZaezk4xjOeul"
  }
}
```

说明：
- 外层 MQTT 负载允许是“类型字节 + 大端长度 + JSON 包裹”
- 解密后的明文仍允许是“类型字节 + 大端长度 + JSON 正文”
- 当前代码已经按上述流程处理，不需要在 message 层手工拆解

验证通过标准：
- `iot_device_message_log` 新增 `$dp` 记录
- 明文和加密两类报文都能进入现有主链路
- `iot_device_property` 中出现预期属性值
- `iot_device.online_status = 1`
- `iot_device.last_online_time`、`last_report_time` 已刷新

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
- 当前代码已经支持根据 `appId` 选择 `spring.cloud.aes.merchants` 中配置的 AES 密钥
- 当前代码已经支持根据 `iot.protocol.crypto.merchants.{appId}` 扩展 DES / 3DES 等对称解密算法
- 当前已验证 `62000001` 的最小 AES 测试链路
- 若未配置对应 `appId` 的解密器，仍会返回清晰业务异常

标准格式说明：
- 表 C.2 报文会取最新时间点的值进入最新属性表
- 表 C.3 报文当前会进入消息日志，并刷新在线状态；文件描述和文件流会标准化到 `DeviceUpMessage.filePayload`
- 表 C.4 固件分包当前会标准化到 `DeviceFilePayload.firmwarePacket`，但仍不进入 OTA 业务流程
- `device` 模块当前会把 C.3 文件快照和 C.4 固件聚合状态写入 Redis：
  - 文件快照 Key：`iot:device:file:{deviceCode}:{transferId}`
  - 固件聚合 Key：`iot:device:firmware:{deviceCode}:{transferId}`
- 固件聚合完成后会额外保存：
  - `assembledBase64`
  - `assembledLength`
  - `calculatedMd5`
  - `md5Matched`

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
- `DeviceMqttReportE2EIntegrationTest` 当前已通过，可作为 MQTT 主链路回归入口
- `MqttDeviceAesDataTests` 当前已通过，可作为 AES 拆包与解密回归入口
- `MqttBinaryFormatParserTest` 当前已通过，可作为 C.3 / C.4 标准格式回归入口
- `MqttJsonProtocolAdapterTest` 当前已通过，可作为 C.2 / C.3 / C.4 统一协议模型回归入口
- `MqttPayloadDecryptorRegistryTest` 当前已通过，可作为 DES / 3DES 解密回归入口
- 当前文件快照 / 固件聚合逻辑已接入运行时代码，建议继续通过 `mvn -pl spring-boot-iot-admin -am package -DskipTests` 保持主链路可编译
- `DeviceMessageServiceImplTest` 在当前 JDK 17 环境下仍受 Mockito inline mock maker 自附加 agent 限制影响，不作为本轮收口阻塞项
- 若共享 MQTT 环境暂时不可达，可先运行 `DeviceMqttReportE2EIntegrationTest` 完成最小规避验证
