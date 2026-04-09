# 协议族定义与解密配置解析对象化设计

> 日期：2026-04-09
> 状态：已完成首轮设计确认，待用户复核后进入 implementation plan
> 适用范围：`spring-boot-iot-protocol`、`spring-boot-iot-framework`、`spring-boot-iot-admin`
> 目标：在不引入数据库表、不新增前台维护页、不破坏现有 `application-dev.yml` 验收基线的前提下，把现有 MQTT 协议安全配置从“按 `appId` 直连解密器”升级为“YAML/IotProperties 驱动的统一协议族对象 + 解密配置对象 + Resolver/SPI”。

## 1. 背景

截至 2026-04-09，仓库已经具备以下稳定事实：

1. `spring-boot-iot-protocol` 已存在 `MqttPayloadDecryptor`、`MqttPayloadDecryptorRegistry`、`SpringCloudAesMqttPayloadDecryptor`、`DesMqttPayloadDecryptor` 等解密扩展点。
2. `spring-boot-iot-framework` 的 `IotProperties.Protocol.Crypto.Merchant` 已承接 `iot.protocol.crypto.merchants` 配置，当前主要服务 DES / 3DES 等 JCE 算法。
3. `spring.cloud.aes.merchants` 已承接 AES 厂商密钥配置，当前由 `SpringCloudAesMqttPayloadDecryptor` 直接消费。
4. `MqttJsonProtocolAdapter` 已能在协议元数据里识别 `appId`、`familyCodes`、`protocolCode` 和 `decryptedPayloadPreview`。

但当前协议安全配置仍有三类结构性问题：

1. 解密路由主要依赖 `decryptor.supports(appId)`，运行时只表达“某个 `appId` 有没有对应实现”，没有正式表达“该设备/报文属于哪个协议族、应落到哪套解密配置”。
2. AES merchant 配置和 `iot.protocol.crypto.merchants` 配置分属两套配置树，运行时没有统一的“解密配置对象”。
3. 当前扩展点仍以“算法实现类”为中心，而不是以“协议族对象 + 解密配置对象”为中心，后续要引入协议族级治理、数据库化配置或审批发布时，缺少稳定中间层。

因此，本轮优先做一刀“对象层先行、配置真相不变”的渐进升级。

## 2. 已确认约束

本轮设计必须严格遵守以下约束：

1. 不新增数据库表，不把协议族和解密 profile 直接落库。
2. 不新增前台维护页，不把这轮工作扩展成控制面 CRUD。
3. 继续复用 `spring.cloud.aes.merchants` 与 `iot.protocol.crypto.merchants`，不制造第二份密钥真相。
4. 真实环境验收基线仍是 `spring-boot-iot-admin/src/main/resources/application-dev.yml`。
5. 不改动现有 MQTT 报文业务语义；本轮只调整“如何选解密配置”和“如何路由到解密器”。
6. 兼容旧配置：未声明协议族配置时，仍允许按 `appId` 回退，不得导致现有可用厂商链路回归。

## 3. 目标与非目标

### 3.1 本轮目标

1. 把“协议族是什么”正式收口为 `ProtocolFamilyDefinition`。
2. 把“解密配置是什么”正式收口为 `ProtocolDecryptProfile`。
3. 把“运行时如何根据上下文选择解密配置”正式收口为 `ProtocolDecryptProfileResolver`。
4. 让 `MqttPayloadDecryptorRegistry` 从“直接按 `appId` 找实现”升级为“先解析 profile，再按算法路由实现”。
5. 保留后续接数据库对象、审批发布和控制面治理的演进接口。

### 3.2 本轮非目标

1. 不做协议族数据库表或审批发布。
2. 不做前端配置页。
3. 不做归一规则、风险指标生成规则的正式对象化。
4. 不重构整个 `MqttJsonProtocolAdapter` 或所有协议适配器。
5. 不改变现有 AES / DES / 3DES 算法执行语义。

## 4. 推荐方案

本轮采用“统一对象层 + YAML 配置投影 + Resolver/SPI 路由”的轻量方案。

核心原则如下：

1. YAML 仍是唯一配置真相源。
2. 运行时优先先识别“协议族/解密 profile”，再决定使用哪个解密器。
3. 解密器只负责“算法执行”，不再同时承担“配置路由”。
4. `familyCodes` 一旦显式声明 `decryptProfileCode`，优先级高于旧的 `appId` 直连路由。
5. 缺少协议族定义时，继续保留 `appId` 回退，保证旧链路不回归。

## 5. 对象模型

### 5.1 `ProtocolFamilyDefinition`

定位：表达“某个协议族是什么、该走哪套协议安全配置”。

建议字段：

1. `familyCode`
2. `protocolCode`
3. `displayName`
4. `decryptProfileCode`
5. `signAlgorithm`
6. `normalizationStrategy`
7. `enabled`

职责：

1. 承接协议族的正式命名和显示语义。
2. 将 `familyCodes` 与解密配置对象建立稳定映射。
3. 为后续归一策略、风险指标生成规则继续保留扩展挂点。

说明：

- `normalizationStrategy` 本轮只作为占位字段透传，不在运行时引入额外行为。
- 本轮不要求每个报文都必须命中 family；缺 family 时可回退旧逻辑。

### 5.2 `ProtocolDecryptProfile`

定位：表达“一个可执行的解密配置对象”。

建议字段：

1. `profileCode`
2. `algorithm`
3. `merchantSource`
4. `merchantKey`
5. `transformation`
6. `signatureSecret`
7. `enabled`

其中：

- `merchantSource` 本轮先固定两类：`SPRING_CLOUD_AES`、`IOT_PROTOCOL_CRYPTO`
- `merchantKey` 对应现有 merchant key，本轮实际就是 `appId`

职责：

1. 把 AES merchant 与 `iot.protocol.crypto.merchants` 统一投影成同一种运行时对象。
2. 让解密器执行时只依赖 profile，而不是直接横切读取全局配置树。
3. 为后续数据库化配置保留稳定契约。

### 5.3 `ProtocolDecryptResolveContext`

定位：运行时解密配置选择上下文。

建议字段：

1. `appId`
2. `protocolCode`
3. `familyCodes`

职责：

1. 统一承接从 MQTT 报文和协议元数据里提取出来的解密选择条件。
2. 作为 `ProtocolDecryptProfileResolver` 的唯一输入模型。

### 5.4 `ProtocolDecryptProfileResolver`

定位：统一负责“根据上下文找出最终解密配置对象”。

职责：

1. 优先根据 `familyCodes -> ProtocolFamilyDefinition.decryptProfileCode` 解析 profile。
2. 当 family 未命中时，回退到 `appId -> ProtocolDecryptProfile`。
3. 返回最终匹配的 `ProtocolDecryptProfile` 或明确失败原因。

解析优先级：

1. `familyCodes` 命中的启用中 `ProtocolFamilyDefinition`
2. `appId` 映射到的启用中 `ProtocolDecryptProfile`
3. 都不存在时抛出明确业务错误

### 5.5 `ProtocolDecryptExecutor` SPI

定位：把现有“解密器”收口为“按算法执行解密”的 SPI。

建议接口语义：

1. `supports(String algorithm)`
2. `decryptBytes(ProtocolDecryptProfile profile, String encryptedBody)`

职责：

1. 根据 profile 执行 AES / DES / 3DES 等具体算法。
2. 不再负责“该不该处理这个 appId”的路由判断。

说明：

- 现有 `MqttPayloadDecryptor` 可以演进为兼容层，或由新的 executor SPI 替换。
- 本轮实现时应优先减少改动面，允许保留一个兼容适配层承接旧接口。

## 6. 配置模型

本轮只在 `iot.protocol` 下新增两组配置，不改动现有 merchant 配置结构。

### 6.1 新增 `iot.protocol.family-definitions`

作用：定义“协议族 -> 解密 profile / 签名算法”。

建议示例：

```yaml
iot:
  protocol:
    family-definitions:
      legacy-dp-crack:
        family-code: legacy-dp-crack
        protocol-code: mqtt-json
        display-name: 裂缝 legacy dp
        decrypt-profile-code: aes-62000000
        sign-algorithm: AES
        normalization-strategy: legacy-dp-v1
        enabled: true
      legacy-dp-gnss:
        family-code: legacy-dp-gnss
        protocol-code: mqtt-json
        display-name: GNSS legacy dp
        decrypt-profile-code: des-62000001
        sign-algorithm: MD5
        normalization-strategy: legacy-dp-gnss-v1
        enabled: true
```

### 6.2 新增 `iot.protocol.decrypt-profiles`

作用：定义统一的解密配置对象，但底层仍引用已有 merchant 真相。

建议示例：

```yaml
iot:
  protocol:
    decrypt-profiles:
      aes-62000000:
        profile-code: aes-62000000
        algorithm: AES
        merchant-source: SPRING_CLOUD_AES
        merchant-key: 62000000
        enabled: true
      des-62000001:
        profile-code: des-62000001
        algorithm: DES
        merchant-source: IOT_PROTOCOL_CRYPTO
        merchant-key: 62000001
        transformation: DES/CBC/PKCS5Padding
        enabled: true
```

### 6.3 保留现有 merchant 配置

本轮继续保留：

1. `spring.cloud.aes.merchants.*`
2. `iot.protocol.crypto.merchants.*`

新加的 `decrypt-profiles` 只引用它们，不复制密钥，不形成第二份密钥真相。

## 7. 运行时调用链

本轮推荐把解密调用链调整为以下顺序：

1. `MqttJsonProtocolAdapter` 从报文中提取 `appId`、`protocolCode`、`familyCodes`。
2. 当检测到 body 需要解密时，构造 `ProtocolDecryptResolveContext`。
3. `ProtocolDecryptProfileResolver` 按优先级解析最终 `ProtocolDecryptProfile`。
4. `MqttPayloadDecryptorRegistry` 或其兼容替代层，依据 `profile.algorithm` 选择实际解密执行器。
5. 具体执行器依据 `merchantSource + merchantKey` 读取底层 merchant 真相并完成算法解密。

### 7.1 优先级规则

1. 若 `familyCodes` 显式命中启用中的 family，优先使用 family 绑定的 `decryptProfileCode`。
2. 若 family 未命中，则回退按 `appId` 查找默认 profile。
3. 若 profile 未启用、merchant 丢失或引用无效，应抛出明确业务错误。

### 7.2 兼容规则

1. 未配置 `family-definitions` 时，旧 `appId` 路由必须继续可用。
2. 旧 AES / DES / 3DES 执行逻辑不变，只变更配置选择入口。
3. `MqttJsonProtocolAdapter` 输出的协议元数据字段保持兼容，不额外破坏现有 trace / observability 字段。

## 8. 启动期校验

为避免配置漂移，本轮建议新增最小校验：

1. `family-definitions.decrypt-profile-code` 必须能找到对应 profile。
2. `decrypt-profiles.merchant-source + merchant-key` 必须能找到实际 merchant。
3. `enabled=false` 的 profile 不允许被 family 引用。
4. `algorithm` 必须能被已注册的解密执行器支持。
5. 同一个 `familyCode + protocolCode` 不允许重复定义多个启用态 family。

校验失败口径：

1. 启动期尽早失败，报清楚缺的配置项。
2. 不允许等到真实 MQTT 报文进来后才发现 profile 配错。

## 9. 模块边界

### 9.1 `spring-boot-iot-framework`

职责：

1. 扩展 `IotProperties`，承接 `family-definitions` 和 `decrypt-profiles`。
2. 提供启动期配置绑定和基础校验。

### 9.2 `spring-boot-iot-protocol`

职责：

1. 定义 `ProtocolFamilyDefinition`、`ProtocolDecryptProfile`、`ProtocolDecryptResolveContext`、`ProtocolDecryptProfileResolver` 等对象和 SPI。
2. 调整 `MqttPayloadDecryptorRegistry` 与具体解密器，使其从“按 appId 路由”升级为“按 profile 路由”。
3. 在 `MqttJsonProtocolAdapter` 中接入新的 resolver 调用。

### 9.3 `spring-boot-iot-admin`

职责：

1. 在 `application-dev.yml`、`application-prod.yml`、`application-test.yml` 中提供最小样例配置。
2. 保持真实环境启动口径不变。

## 10. 风险与权衡

### 10.1 主要收益

1. 后续数据库化协议族/解密 profile 时，不必再直接改业务适配器。
2. 解密路由从“算法实现类中心”升级到“配置对象中心”，便于治理和审计。
3. 未来归一规则、风险指标生成规则也能沿同一路径继续对象化。

### 10.2 当前代价

1. 仍然以 YAML 为真相源，本轮不具备控制面治理能力。
2. 需要在启动期增加一层配置投影和校验逻辑。
3. 需要为旧接口保留兼容层，避免一次性重构过深。

## 11. 验收口径

本轮设计完成后的实现，至少应满足以下结果：

1. 协议族定义和解密配置具备正式代码对象，不再只散落在 `supports(appId)` 和 merchant 配置里。
2. 解密配置解析具备统一 resolver，优先按 family，兼容回退 appId。
3. AES / DES / 3DES 解密执行语义不变。
4. 现有 `application-dev.yml` 真实环境基线可继续启动和联调。
5. 新旧配置兼容，不因为未补 family 配置而直接破坏历史可用链路。

## 12. 后续演进接口

本轮完成后，后续推荐按以下顺序继续：

1. 把 `normalizationStrategy` 收口为独立归一策略对象/SPI。
2. 把风险指标生成规则收口为独立对象/SPI。
3. 再评估是否把 `ProtocolFamilyDefinition` 与 `ProtocolDecryptProfile` 升级为数据库控制面对象。
