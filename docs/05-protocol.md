# 协议规范

## 当前支持
- mqtt-json
- mqtt-json 兼容历史 `$dp` 主题明文 JSON 上报
- mqtt-json 兼容历史 `$dp` 主题 AES 加密 JSON 上报
- mqtt-json 兼容 DES / 3DES 等配置化对称解密扩展
- mqtt-json 兼容签名、时间戳、防重放安全校验

## 预留协议
- tcp-hex
- modbus-tcp
- modbus-rtu

## ProtocolAdapter 抽象
核心方法：
- getProtocolCode()
- decode(payload, context)
- encode(message, context)

## MQTT JSON 上报示例
```json
{
  "messageType": "property",
  "properties": {
    "temperature": 26.5,
    "humidity": 68
  }
}
```

## MQTT JSON 事件上报示例

Topic：
```text
/sys/demo-product/demo-device-01/thing/event/post
```

Payload：
```json
{
  "messageType": "event",
  "events": {
    "overheat": {
      "level": 2,
      "value": 88.2
    }
  }
}
```

## MQTT Topic 规范
- /sys/{productKey}/{deviceCode}/thing/property/post
- /sys/{productKey}/{deviceCode}/thing/event/post
- /sys/{productKey}/{deviceCode}/thing/property/reply
- /sys/{productKey}/{deviceCode}/thing/service/reply
- /sys/{productKey}/{deviceCode}/thing/status/post
- /sys/{productKey}/{deviceCode}/thing/property/set
- /sys/{productKey}/{deviceCode}/thing/service/{serviceIdentifier}/invoke
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/event/post
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/status/post
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set
- /sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/service/{serviceIdentifier}/invoke
- $dp（历史兼容主题）

## Phase 2 Topic 矩阵

### 上行
- 直连设备属性上报：
  - `/sys/{productKey}/{deviceCode}/thing/property/post`
- 直连设备事件上报：
  - `/sys/{productKey}/{deviceCode}/thing/event/post`
- 直连设备状态上报：
  - `/sys/{productKey}/{deviceCode}/thing/status/post`
- 历史兼容：
  - `$dp`
- 子设备预留上报：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/event/post`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/status/post`

### 下行
- 直连设备属性下发：
  - `/sys/{productKey}/{deviceCode}/thing/property/set`
- 直连设备服务调用：
  - `/sys/{productKey}/{deviceCode}/thing/service/{serviceIdentifier}/invoke`
- 子设备预留下发：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/service/{serviceIdentifier}/invoke`

## 网关代子设备 Topic 预留
- 子设备上报推荐 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/post`
- 子设备下发推荐 topic：
  - `/sys/{gatewayProductKey}/{gatewayDeviceCode}/sub/{subDeviceCode}/thing/property/set`
- 解析约定：
  - `productKey` 仍表示网关产品 `gatewayProductKey`
  - `gatewayDeviceCode` 表示网关设备编码
  - `subDeviceCode` 表示逻辑子设备编码
  - 当前 `deviceCode` 在子设备 topic 场景下按“有效设备编码”取 `subDeviceCode`
- 当前仅建立 topic 规范和解析扩展点，不代表已经实现子设备认证、拓扑和落库

## MQTT 下行编码说明
- 当前 `mqtt-json` 下行继续复用 `ProtocolAdapter.encode(message, context)`
- `DeviceDownMessage` 会被编码为 JSON 字节流后交给 MQTT 发布器
- 最小下行消息模型字段：
  - `messageId`
  - `commandType`
  - `serviceIdentifier`
  - `params`
- 属性下发示例：
```json
{
  "messageId": "1773507184482",
  "commandType": "property",
  "serviceIdentifier": null,
  "params": {
    "switch": 1,
    "targetTemperature": 23.0,
    "requestId": "task6-verify-001"
  }
}
```

- 服务调用示例：
```json
{
  "messageId": "cmd-001",
  "commandType": "service",
  "serviceIdentifier": "reboot",
  "params": {}
}
```

## 历史 `$dp` 兼容说明
- `$dp` 不包含标准 topic 里的 `productKey` / `deviceCode` / `messageType`
- 当前主链路兼容三类历史报文：
  - 直接明文 JSON，上层 key 为设备编码，例如 `{"100054920":{...}}`
  - 带控制字符前缀的明文 JSON，例如 `\u0010{"100054920":{...}}`
  - 加密 JSON 包装格式，例如 `{"header":{"appId":"62000001"},"bodies":{"body":"..."}}`
- 历史加密报文的真实处理顺序为：
  1. 先解析 MQTT 负载外层帧头
  2. 提取外层 JSON 中的 `header.appId` 与 `bodies.body`
  3. 根据 `appId` 从 `spring.cloud.aes.merchants` 中选择对应 AES 密钥配置
  4. 解密 `bodies.body`
  5. 对解密结果再次执行“类型字节 + 大端长度 + JSON 正文”的帧解析
  6. 将最终 JSON 标准化为统一 `DeviceUpMessage`
- 历史二进制帧格式遵循当前兼容规则：
  - Byte 1：数据格式类型
  - Byte 2：有效长度高位字节
  - Byte 3：有效长度低位字节
  - Byte 4 开始：JSON 字符串
  - 长度采用大端序
- 当前已落地的标准表：
  - 表 C.1：单值 / 多值属性 JSON
  - 表 C.2：带时间序列键的属性 JSON，协议层默认取最新时间点作为最新属性
  - 表 C.3：文件描述 JSON + 文件流二进制尾部
  - 表 C.4：固件升级分包二进制格式，当前在协议层完成拆包和字段标准化
- 对于明文嵌套 JSON：
  - 协议层会从最外层 key 提取 `deviceCode`
  - 设备状态、GNSS、倾角仪、加速度等嵌套数据会被拍平成属性
  - 属性标识形如 `L1_QJ_1.X`、`L1_JS_1.gY`、`S1_ZT_1.ext_power_volt`
  - 若配置了 `iot.device.sub-device-mappings.{baseStationDeviceCode}.{logicalCode}={childDeviceCode}`，协议层会把“基准站一包多测点”报文拆成多个 `childMessages`：
    - 父消息继续保留原始密文/明文日志与在线状态更新
    - 子消息按映射后的真实 `deviceCode` 分别进入 `device` 模块落库
    - 当逻辑测点值为 `时间戳 -> 对象` 结构时，子消息属性会写成对象内字段，例如 `dispsX`、`dispsY`
    - 当逻辑测点值为 `时间戳 -> 标量` 结构时，子消息属性会回落为该逻辑测点自身
- 对于加密 JSON：
  - 当前已实现 `MqttPayloadDecryptor` 扩展点和 `SpringCloudAesMqttPayloadDecryptor`
  - 默认按 `header.appId` 选择 `spring.cloud.aes.merchants` 中的厂商密钥
  - 当前已支持 `application-dev.yml` 中配置的 `62000001`
  - 若未配置对应解密器，会返回清晰业务异常

## `$dp` 主链路说明
`$dp` 主题收到消息后，当前运行时统一进入显式 `UpMessageProcessingPipeline`，主口径固定为：

1. `INGRESS`
2. `TOPIC_ROUTE`
3. `PROTOCOL_DECODE`
4. `DEVICE_CONTRACT`
5. `MESSAGE_LOG`
6. `PAYLOAD_APPLY`
7. `TELEMETRY_PERSIST`
8. `DEVICE_STATE`
9. `RISK_DISPATCH`
10. `COMPLETE`

说明：
- `message` 模块负责固定 Pipeline 编排；`MqttMessageConsumer` 和 `DeviceHttpController` 都统一进入同一条 Pipeline。
- `TOPIC_ROUTE` 继续由 `MqttTopicRouter` 负责；HTTP 入口在该阶段固定标记为 `SKIPPED/DIRECT_HTTP`。
- `PROTOCOL_DECODE` 继续由 `MqttJsonProtocolAdapter` 负责，不再顺带承担“流程展示”职责；阶段摘要固定输出 `routeType`、`messageType`、`dataFormatType`、`childMessageCount`、`filePayload`。
- `device` 模块负责 `DEVICE_CONTRACT / MESSAGE_LOG / PAYLOAD_APPLY / DEVICE_STATE / RISK_DISPATCH` 等显式 stage handler。
- `telemetry` 模块负责 `TELEMETRY_PERSIST`，按标准化 `properties` 写 TDengine；`reply` / 文件载荷 / 空属性消息会跳过该步骤。
- `UpMessageDispatcher` 当前仅保留为 legacy/compatibility 类和兼容性测试对象，不再作为 `$dp` 主链路说明对象。
- 表 C.3 / C.4 在协议层会进一步标准化为：
  - `DeviceUpMessage.filePayload`
  - `DeviceFilePayload`
  - `DeviceFirmwarePacket`
  后续 OTA 或文件存储能力可以直接基于这些统一模型扩展
 - 当前 `device` 模块已经会消费 `filePayload`：
   - C.3 文件消息会写入 Redis 文件快照
   - C.4 固件分包会写入 Redis 聚合状态并尝试重组
   - OTA 模块通过 `DeviceFilePayloadListener` 预留扩展点接入

## MQTT 下行主链路说明
推荐下行消息当前运行时主链路为：

1. `DeviceDownController`
2. `DownMessageService`
3. `MqttDownMessagePublisher`
4. `ProtocolAdapter.encode`
5. `MqttMessageConsumer.publish`
6. MQTT Broker

说明：
- `message` 模块负责发布入口和最小编排
- `protocol` 模块负责统一下行模型编码
- 当前不实现 ACK、重试、指令状态流转

## Topic 解析扩展点
- `MqttTopicParser` 当前会输出：
  - `routeType`
  - `gatewayDeviceCode`
  - `subDeviceCode`
- `routeType` 当前可取：
  - `direct`
  - `sub-device`
  - `legacy`
- `RawDeviceMessage` 与 `ProtocolContext` 已同步预留上述字段
- 默认订阅列表仍保持直连设备 topic，不主动开启子设备 topic 订阅，避免在未落地子设备业务前影响现有运行环境

## 模块归位建议
- `spring-boot-iot-protocol`
  - 负责加密、解密、报文完整性校验、数据格式类型识别、大小端长度解析
  - 负责签名、防重放、时间戳校验
  - 负责多厂商、多算法扩展点，例如 AES、DES、3DES 解密与 MD5 兼容签名
- `spring-boot-iot-message`
  - 只保留消费、桥接、分发
  - 不承载运行时协议解析和安全逻辑
- `spring-boot-iot-admin`
  - 只保留测试和配置验证
  - 不放运行时协议逻辑

## 安全校验规则
- 当前安全头支持从 `header` 中读取：
  - `signature` 或 `sign`
  - `timestamp` 或 `ts`
  - `nonce`
  - `signAlgorithm` / `signatureAlgorithm` / `algorithm`
- 若安全字段全部缺失，则兼容旧设备报文，不阻断主链路
- 若存在任一安全字段，则按完整安全流程校验：
  1. 校验时间戳格式与时间窗
  2. 校验 nonce 防重放
  3. 按算法校验签名
- 默认签名串格式：
  - `appId={appId}&timestamp={timestamp}&nonce={nonce}&body={body}`
- 当前支持算法：
  - `AES`
  - `MD5`
- `MD5` 规则说明：
  - MD5 不做可逆解密，只做带密钥摘要校验
  - 默认按 `body + signatureSecret` 生成摘要
  - 可通过 `iot.protocol.crypto.merchants.{appId}.signatureJoinMode` 调整为 `KEY_PREFIX` / `KEY_SUFFIX` / `CONTENT_ONLY`
- 多算法解密扩展：
  - `SpringCloudAesMqttPayloadDecryptor`：复用 `spring.cloud.aes.merchants`
  - `DesMqttPayloadDecryptor`：读取 `iot.protocol.crypto.merchants.{appId}` 中的 `DES` 配置
  - `TripleDesMqttPayloadDecryptor`：读取 `iot.protocol.crypto.merchants.{appId}` 中的 `DESede` 配置

## 数据格式类型
- 直接 JSON：历史兼容模式，不带标准帧头
- 类型 1：已按当前附件中的表 C.1 落地
  - Byte 1：数据格式类型
  - Byte 2：有效长度高位字节
  - Byte 3：有效长度低位字节
  - Byte 4 起：JSON 正文
  - 长度值采用大端序
- 类型 2：已按表 C.2 落地
  - 帧头与 C.1 一致
  - JSON 内部允许 `时间戳 -> 值` 结构
  - 协议层默认取最新时间点的值写入统一 `DeviceUpMessage.properties`
  - 深部位移设备当前已验证的报文示例中，解密后的字节前缀可能为 `[2, 1, 10, ...]`：
    - 第 1 字节 `2` 表示数据格式类型 2
    - 第 2~3 字节 `0x01 0x0A` 表示 JSON 正文长度 `266`
    - 长度值按大端序解释，从第 4 字节开始读取 JSON 正文
  - 对于南方测绘“基准站 + 8 个子设备”深部位移场景，当前已支持在类型 2 解密后按配置把 `L1_SW_1` ~ `L1_SW_8` 拆分映射为独立子设备上报
- 类型 3：已按表 C.3 落地
  - Byte 1：数据格式类型 3
  - Byte 2~3：文件描述 JSON 长度，大端序
  - Byte 4~n：文件描述 JSON
  - Byte n+1~n+2：文件数据流长度，大端序
  - Byte n+3~：文件数据流
  - 当前协议层会把文件描述和文件流标准化到 `DeviceUpMessage.filePayload`
  - `device` 模块会把文件快照写入 Redis，当前不会把文件类数据直接落入一期最新属性表
- 类型 4：当前作为表 C.4 固件升级分包格式单独解析
  - Byte 1~2：当前数据包索引，大端序
  - Byte 3~4：当前数据包大小，大端序
  - Byte 5~6：数据包总个数，大端序
  - Byte 7~n：二进制数据流
  - 最后一个数据包允许带 MD5 长度和固件 MD5 字符串
  - 当前仅在协议层拆包校验，并标准化到 `DeviceFilePayload.firmwarePacket`
  - `device` 模块会在 Redis 中聚合分包、重组固件流并校验 MD5
  - 后续 OTA 业务可直接消费该协议对象和聚合结果

## 约束
- 所有协议最终都要转换为统一 DeviceUpMessage / DeviceDownMessage
- 协议层不直接写业务库
- 新协议优先新增适配器，不改主链路
