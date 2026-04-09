# `$dp` 协议标准化与 TDengine 映射治理重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不破坏现有 MQTT 固定 Pipeline、真实 `$dp` 订阅稳定性和现网厂商兼容性的前提下，完成 `$dp` 多厂商地灾报文的协议标准化治理、家族级可观测增强，以及 TDengine legacy/fallback 映射的可维护化重构。

**Architecture:** 保持 `MqttMessageConsumer -> UpMessageProcessingPipeline` 的 10 阶段主链路不变，把当前集中在 `MqttJsonProtocolAdapter` 内的 `$dp` 复杂逻辑拆为“外层解包/解密 -> 家族识别 -> 属性标准化 -> 子消息拆分 -> 遥测映射”五段，并引入显式的遥测映射服务层。重构采用“先冻结现状、再增强观测、后双轨切换”的方式，避免一次性替换导致真实环境回归失控。

**Tech Stack:** Spring Boot 4, Java 17, Paho MQTT, Jackson, MySQL, Redis, TDengine, Maven, PowerShell

---

## 背景与边界

- 当前真实环境中的 `$dp` 已由后台常驻进程持续订阅，不能以“停止接入、离线重放”的方式重构。
- 当前固定主链路已稳定，顺序必须保持为：

```text
INGRESS
TOPIC_ROUTE
PROTOCOL_DECODE
DEVICE_CONTRACT
MESSAGE_LOG
PAYLOAD_APPLY
TELEMETRY_PERSIST
DEVICE_STATE
RISK_DISPATCH
COMPLETE
```

- 当前 `$dp` 的主要复杂度不在 MQTT 入口，而在 `$dp` 历史兼容协议和 TDengine 落库映射：
  - 多厂商
  - 多加密/帧格式
  - 多设备家族
  - 一包多测点 / 基站转子设备
  - legacy stable 与 `iot_device_telemetry_point` 双落库模式
- 本轮重构不做的事：
  - 不改 `MqttMessageConsumer` 的职责边界
  - 不改固定 Pipeline 顺序
  - 不把厂商协议逻辑移到 `message` 模块或 `telemetry` 模块
  - 不重新设计设备管理 UI
  - 不引入 H2 或离线替代验收链路

## 家族基线

以下家族是当前 `$dp` 重构的核心治理对象，后续所有标准化、映射和验收都按“家族”而不是“厂商 JSON 形状”推进：

| 家族码 | 目标 stable | 典型设备类型 |
|---|---|---|
| `S1_ZT_1` | `s1_zt_1` | GNSS、倾角、加速度、裂缝、激光测距、深部位移、雨量计、泥位计等状态类 |
| `L1_GP_1` | `l1_gp_1` | GNSS 位移 |
| `QN_QB_ZT_1` | `qn_qb_zt_1` | 情报板状态 |
| `L4_NW_1` | `l4_nw_1` | 泥位计 |
| `HY_BSD_ZT_1` | `hy_bsd_zt_1` | 爆闪灯状态 |
| `HY_BSD_ACK_1` | `hy_bsd_ack_1` | 爆闪灯指令确认 |
| `L3_YL_1` | `l3_yl_1` | 雨量计 |
| `L1_LF_1` | `l1_lf_1` | 裂缝 / 激光测距 |
| `L1_QJ_1` | `l1_qj_1` | 倾角 |
| `L1_JS_1` | `l1_js_1` | 加速度 |
| `L1_SW_1` | `l1_sw_1` | 深部位移 |

## 当前问题清单

1. `$dp` 复杂度过度集中在 [MqttJsonProtocolAdapter.java](../../../spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java)，单类同时承担解密、帧解析、安全校验、消息类型推断、属性扁平化和子消息拆分。
2. 地灾家族识别逻辑是隐式散落的，`status/property` 推断、时间戳容器识别和属性拍平耦合在一起，新增厂商兼容时很容易继续堆分支。
3. `TELEMETRY_PERSIST` 成功并不等于 legacy stable 命中成功；当前 `NORMALIZED_FALLBACK_ONLY` 很容易掩盖映射缺口。
4. 遥测映射仍通过产品物模型 `specsJson.tdengineLegacy` 隐式表达，缺少统一校验入口、缺少治理维度，也不便统计“哪些家族长期只走 fallback”。
5. 当前 `message-flow` 虽然已经能看 10 阶段结果，但缺少 `familyCodes`、`vendor/appId`、`normalizationStrategy` 等协议治理核心维度。
6. 真实环境在持续接入，不能接受“大改完成后一次性上线再看结果”的切换方式，必须支持双轨观察与逐步放量。

## 目标架构

### 1. 保持不变的边界

- MQTT 接收入口继续由 [MqttMessageConsumer.java](../../../spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java) 负责。
- 固定编排继续由 [UpMessageProcessingPipeline.java](../../../spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java) 负责。
- `DEVICE_CONTRACT / MESSAGE_LOG / PAYLOAD_APPLY / DEVICE_STATE / RISK_DISPATCH` 的模块归位不变。
- `TELEMETRY_PERSIST` 仍然只消费标准化后的 `properties`，不反向理解厂商原始 payload。

### 2. `$dp` 协议层目标分段

建议把当前 `$dp` 解码链路拆成以下 5 段：

1. `LegacyDpEnvelopeDecoder`
   - 负责控制字符前缀、二进制帧长度、加密包外层 JSON、`appId`、解密、安全校验。
2. `LegacyDpFamilyResolver`
   - 负责识别当前 payload 内包含哪些地灾家族。
3. `LegacyDpPropertyNormalizer`
   - 负责按家族规则输出稳定的标准化属性、时间戳、消息类型。
4. `LegacyDpChildMessageSplitter`
   - 负责基站一包多测点拆成多个 `childMessages`。
5. `DeviceTelemetryMappingService`
   - 负责把标准化属性映射到 legacy stable / column / fallback 策略，不让 `telemetry` 直接解析产品物模型原文。

### 3. 标准化原则

- 以“家族码”作为协议标准化主键，不以厂商 payload 层级作为主键。
- 在没有显式迁移计划前，现有属性标识尽量保持稳定，避免前端、规则、报表和风险逻辑被动回归。
- `iot_device_telemetry_point` 是安全网，不是长期默认归宿。长期目标应是：
  - 已治理家族优先命中 legacy stable
  - fallback 只承接新指标、临时兼容或确实不宜入宽表的字段
- 协议治理必须优先增强观测，再做行为替换。

## File Map

### 当前核心文件

- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
  Responsibility: MQTT 入口、运行态、接入 handoff。
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
  Responsibility: 固定 10 阶段编排与 `message-flow` 时间线。
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttTopicRouter.java`
  Responsibility: `$dp` / 标准 Topic 路由上下文。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
  Responsibility: 当前 `$dp` 主要复杂逻辑承载点。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpMessage.java`
  Responsibility: 协议层到业务层的统一上行消息模型。
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DevicePropertyMetadataServiceImpl.java`
  Responsibility: 现有产品物模型属性元数据与 `tdengineLegacy` 解析。
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java`
  Responsibility: 时序落库 gatekeeper。
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`
  Responsibility: legacy-compatible 与 fallback 组合策略。
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriter.java`
  Responsibility: legacy stable 写入。
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryReader.java`
  Responsibility: legacy stable 最新值读取。

### 建议新增文件

- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java`
  Responsibility: 承载 `$dp` 协议元数据，如 `appId`、`familyCodes`、`normalizationStrategy`、`childSplitApplied`。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`
  Responsibility: `$dp` 外层解包、解密、安全校验、数据格式解析。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java`
  Responsibility: 识别 payload 中包含的地灾家族。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java`
  Responsibility: 按家族输出标准化属性和时间戳。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java`
  Responsibility: 基站一包多测点拆分。
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java`
  Responsibility: 协议层内部中间态，供 adapter 组装 `DeviceUpMessage`。
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTelemetryMappingService.java`
  Responsibility: 显式遥测映射读取与校验服务接口。
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTelemetryMappingServiceImpl.java`
  Responsibility: 从产品物模型及后续扩展来源解析映射。
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/TelemetryMetricMapping.java`
  Responsibility: stable / column / enabled / source 等映射模型。

## 实施阶段

### Task 1: 冻结真实 `$dp` 基线与家族样本库

**Files:**
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Inspect: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`
- Inspect: `spring-boot-iot-admin/src/main/resources/application-dev.yml`

- [x] **Step 1: 确认真实环境 `$dp` 运行基线**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:9999/actuator/health/mqttConsumer'
```

Expected:
- `status = "UP"`
- `details.connected = true`
- `details.consumerActive = true`
- `details.subscribeTopics` 包含 `$dp`

- [x] **Step 2: 拉取最近真实 `$dp` 会话并按家族做样本台账**

Run:

```powershell
$body = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9999/api/auth/login' -ContentType 'application/json' -Body $body
$headers = @{ Authorization = 'Bearer ' + $login.data.token }
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:9999/api/device/message-flow/recent?size=30' -Headers $headers
```

Selection rule:
- 优先选择真实 `$dp` topic
- 每个家族至少保留 3 条正样本
- 每个厂商 `appId` 至少保留 1 条样本

- [x] **Step 3: 对每个样本记录以下字段**

Required fields:
- `traceId`
- `deviceCode`
- `appId`
- `familyCodes`
- `dataFormatType`
- `messageType`
- `childMessageCount`
- `TELEMETRY_PERSIST.branch`
- `legacyStableCount`
- `normalizedFallbackCount`

- [x] **Step 4: 立即标注当前高风险家族**

Risk rule:
- 长期只走 `NORMALIZED_FALLBACK_ONLY`
- 同一家族不同厂商输出字段不一致
- 同一设备族需要大量 `childMessages`
- 同一家族存在多种时间戳结构

Evidence:
- `docs/superpowers/plans/2026-03-24-dp-baseline-freeze.md`

### Task 2: 先补家族级协议元数据与可观测，不改业务行为

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpProtocolMetadata.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/DeviceUpMessage.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`

- [x] **Step 1: 先写失败测试，要求 `$dp` 解码后带出协议元数据**

Test assertions:
- `$dp` 报文能输出 `appId`
- `familyCodes` 非空
- `normalizationStrategy = LEGACY_DP`
- `childSplitApplied` 与实际子消息拆分一致

- [x] **Step 2: 运行单测，确认当前实现尚不满足这些元数据要求**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 新增测试失败

- [x] **Step 3: 给 `DeviceUpMessage` 增加显式协议元数据挂载点**

Required fields:
- `appId`
- `familyCodes`
- `normalizationStrategy`
- `timestampSource`
- `childSplitApplied`
- `routeType`

- [x] **Step 4: 把协议元数据回写到 `PROTOCOL_DECODE.summary`**

Required summary keys:
- `familyCodes`
- `appId`
- `normalizationStrategy`
- `timestampSource`

- [x] **Step 5: 重新运行协议与 message-flow 单测**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-message "-DskipTests=false" "-Dtest=UpMessageProcessingPipelineTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 新增元数据测试通过
- 10 阶段顺序测试继续通过

### Task 3: 把 `$dp` 外层解包/解密从 `MqttJsonProtocolAdapter` 中拆出

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpEnvelopeDecoderTest.java`

- [x] **Step 1: 为三类 `$dp` 包型补齐失败测试**

Coverage:
- 明文 JSON
- 带控制字符前缀的明文 JSON
- 带 `header.appId + bodies.body` 的加密包

- [x] **Step 2: 把当前 `decodePayload()` 与 `enrichByDataFormat()` 搬到新 decoder 中**

Constraint:
- 只做职责迁移，不改变输出结构

- [x] **Step 3: 保持 `MqttJsonProtocolAdapter.decode()` 只负责编排，不再直接处理外层加解密细节**

Expected end state:
- adapter 看起来像 orchestration class
- envelope decoder 看起来像 pure protocol decoder

- [x] **Step 4: 运行协议模块全部相关测试**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest,MqttPayloadDecryptorRegistryTest,LegacyDpEnvelopeDecoderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 明文、加密、帧解析回归通过

### Task 4: 把家族识别、属性标准化、子消息拆分拆成独立组件

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java`
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpPropertyNormalizerTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java`

- [x] **Step 1: 为核心家族补齐标准化失败测试**

Minimum family coverage:
- `S1_ZT_1`
- `L1_GP_1`
- `L1_QJ_1`
- `L1_JS_1`
- `L1_SW_1`

Assertion goals:
- 输出属性键稳定
- 时间戳取值一致
- `messageType` 推断稳定
- 基站一包多测点的子消息拆分与当前行为一致

- [x] **Step 2: 实现 `LegacyDpFamilyResolver`，显式识别家族码**

Rules:
- 从 payload 顶层和设备体内识别家族 key
- 一条报文允许命中多个家族
- 不允许继续靠零散 `contains("_ZT_")` 弱推断承担全部职责

- [x] **Step 3: 实现 `LegacyDpPropertyNormalizer`，按家族做标准化**

Rules:
- 时间序列容器统一取最新点
- 标量与对象容器统一输出
- 当前已被前端/规则/报表消费的属性名，若未明确迁移，不得随意改名

- [x] **Step 4: 实现 `LegacyDpChildMessageSplitter`，把子设备拆分从 adapter 主类里移出**

Rules:
- 继续复用 `iot.device.sub-device-mappings`
- 父消息原始日志和在线态保留
- 子消息只承接标准化属性

- [x] **Step 5: 运行协议模块回归测试**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest,MqttTopicParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 旧行为样本与新拆分类实现一致

### Task 5: 引入显式遥测映射服务，降低 fallback 的隐性默认化

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTelemetryMappingService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTelemetryMappingServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/TelemetryMetricMapping.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DevicePropertyMetadataServiceImpl.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriter.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryReader.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DevicePropertyMetadataServiceImplTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriterTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryReaderTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacadeTest.java`

- [x] **Step 1: 先写失败测试，要求映射读取与校验统一从 service 层走**

Expected:
- legacy writer/reader 不再自己理解散落的映射来源
- 缺失 stable/column 的映射会被显式标记

- [x] **Step 2: 抽出 `DeviceTelemetryMappingService`，先兼容现有 `specsJson.tdengineLegacy` 来源**

Constraint:
- 第一阶段不强行引入新表
- 先做服务抽象，保持行为兼容

- [x] **Step 3: 在 `TdengineTelemetryFacade` 中引入“映射命中率”观测**

Required metrics/summary fields:
- `legacyMappedMetricCount`
- `legacyUnmappedMetricCount`
- `fallbackMetricCount`

- [x] **Step 4: 把 `NORMALIZED_FALLBACK_ONLY` 视为治理信号，而不是默认成功态**

Required action:
- 日志和时间线中必须能看出“命中 fallback 的原因”

- [x] **Step 5: 运行 device + telemetry 相关测试**

Run:

```powershell
mvn -pl spring-boot-iot-device -am "-DskipTests=false" "-Dtest=DeviceTelemetryMappingServiceImplTest,DevicePropertyMetadataServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl spring-boot-iot-telemetry -am "-DskipTests=false" "-Dtest=LegacyTdengineTelemetryWriterTest,LegacyTdengineTelemetryReaderTest,TdengineTelemetryFacadeTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl spring-boot-iot-message -am "-DskipTests=false" "-Dtest=UpMessageProcessingPipelineTest,MqttJsonProtocolAdapterTest,LegacyDpEnvelopeDecoderTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest,MqttTopicParserTest,MqttPayloadSecurityValidatorTest,MqttPayloadDecryptorRegistryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- legacy/fallback 组合策略保持可回归

### Task 6: 增加特性开关与双轨切换能力

**Files:**
- Modify: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`

- [x] **Step 1: 增加 `$dp` 重构过程专用开关**

Recommended flags:
- `iot.protocol.legacy-dp.family-observability-enabled`
- `iot.protocol.legacy-dp.normalizer-v2-enabled`
- `iot.telemetry.legacy-mapping-validate-only`

- [x] **Step 2: 在 `validate-only` 模式下输出 old/new 差异，但继续沿用旧结果**

Purpose:
- 先看真实环境差异，再决定放量

- [x] **Step 3: 在 `normalizer-v2-enabled` 打开后，仅切换协议内部实现，不改 Pipeline 和控制器接口**

Success rule:
- 外部 API 路径、鉴权、session/timeline 结构不变

- [x] **Step 4: 运行 message 模块回归测试**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-message "-DskipTests=false" "-Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest,MqttReportPublishServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- MQTT 入口和时间线行为不回归

Verification evidence:
- `mvn -pl spring-boot-iot-protocol -am "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,LegacyDpEnvelopeDecoderTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest,MqttTopicParserTest,MqttPayloadSecurityValidatorTest,MqttPayloadDecryptorRegistryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl spring-boot-iot-message -am "-DskipTests=false" "-Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest,MqttReportPublishServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

### Task 7: 真实环境分家族验收、文档更新和旧逻辑清理

**Files:**
- Modify: `docs/05-protocol.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Inspect: `README.md`
- Inspect: `AGENTS.md`

- [x] **Step 1: 按家族执行真实环境验收**

Minimum checks per family:
- `message-flow` 中 10 阶段完整
- `PROTOCOL_DECODE.summary.familyCodes` 正确
- `PAYLOAD_APPLY` 属性数正确
- `TELEMETRY_PERSIST` 分支符合预期
- `/api/telemetry/latest` 可见最新值
- TDengine stable 或 fallback 表中可查

- [x] **Step 2: 对每个家族都记录 legacy 命中率与 fallback 命中率**

Acceptance target:
- 已完成治理的家族，fallback 比例应显著下降
- fallback 仅保留未映射指标或临时兼容项

Verification evidence:
- `SK00E60D1306931`：`status` 会话 `cd4b8ca569f34cebbb52946bafc1fc68` / trace `c4bfcfa4ba9349b68ab0eac87287e25f` 命中 `familyCodes=S1_ZT_1`、`normalizedFallbackCount=17`；`property` 会话 `37dcd60f5fc64eb7a8b90b0fde8d7b63` / trace `d74178cd34f64c0197a913bbf82af152` 命中 `familyCodes=L3_YL_1`、`normalizedFallbackCount=2`。两条 trace 在 TDengine `reported_at=2026-03-24T07:18:03.000Z` 下同时存在。
- `SK00EA0D1307992`：`status` 会话 `8f1bfcc4bdcc43df878a6b33e7b39a90` / trace `6544c3bbb7504ea38bd8c7a34136b400` 命中 `familyCodes=S1_ZT_1`、`normalizedFallbackCount=19`；`property` 会话 `9c673a06d6e741b6b0b2af323eb96f8b` / trace `90bfb1b8b3f74be0b16d4a772db81b0e` 命中 `familyCodes=L1_LF_1,L1_LF_2,L1_LF_3`、`normalizedFallbackCount=3`。两条 trace 在 TDengine `reported_at=2026-03-24T07:17:38.000Z` 下同时存在。
- 当前样本仍以 `NORMALIZED_FALLBACK_ONLY` 为主，说明家族识别与 fallback 原因观测已补齐，但 legacy stable 映射治理尚未完成，后续应继续降低 fallback 比例。

- [x] **Step 3: 原位更新权威文档**

Required updates:
- `05-protocol`：补齐 `$dp` 新的分段结构和家族标准化口径
- `11`：补齐 `familyCodes/appId/fallback` 观测口径
- `07`：补齐新开关配置说明
- `真实环境测试与验收手册`：补齐分家族验收步骤
- `08`：记录本轮治理边界和后续技术债

Inspection result:
- `README.md`：本次 bugfix 不涉及新增入口、命令或协作约束，无需更新。
- `AGENTS.md`：本次 bugfix 不改变开发规则、模块边界或验收基线，无需更新。

- [ ] **Step 4: 只有在真实环境稳定后才删除 adapter 中的旧私有方法**

Removal target:
- 零散的 legacy 推断私有方法
- 与新组件重复的旧 helper

- [x] **Step 5: 运行最终验证命令**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-message "-DskipTests=false" "-Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryPersistStageHandlerTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 协议、消息、遥测三个模块关键回归通过

## Task 7 Step 2 映射治理待办清单（codex/dev 合并版）

- Task 7 Step 2 当前先按“缺口台账 + 默认 dry-run 的 SQL 草案”收口，不直接产出面向真实环境的最终升级 SQL。
- 对应草案文件为 `sql/upgrade/20260324_phase5_tdengine_mapping_gap_draft.sql`，当前只固化 `iot_product_model` property 基线和 `tdengineLegacy` 占位结构。
- 当前草案严格遵守以下边界：
  - 默认 `@apply_changes = 0`，只预览范围，不直接改库。
  - `stable/column` 只保留在草案注释和 hint 字段中，不自动写入 `specs_json.tdengineLegacy`。
  - 只有 TDengine REST / SQL 认证恢复并完成 `DESCRIBE <stable>` 复核后，才允许把对应指标从 `PENDING_TDENGINE_VERIFICATION` 升级为正式映射。

| productKey | 当前草案关注点 | 代表性缺口 / 建议 identifier | 当前草案动作 | 当前阻塞 |
|---|---|---|---|---|
| `south_rtu` | 裂缝家族 + 状态类补点 | `L1_LF_1` ~ `L1_LF_9`、父级 `S1_ZT_1.*`、`sensor_state.L1_LF_*` | 已把 RTU 当前 live gap 全量补到 35 条 draft seed，全部使用 `tdengineLegacy.enabled=false` 占位结构 | `DESCRIBE` 已确认 `l1_lf_1.lf` 与 `s1_zt_1.signal_nb / signal_db` 存在；当前阻塞转为 identifier canonicalization（`L1_LF_1.value`、`singal_*` 拼写漂移）与 mixed type 收敛 |
| `south_multi_displacement` | 多维位移指标与状态类补点 | `L1_JS_1.*`、`L1_QJ_1.*`、`L1_LF_1 / L1_LF_1.value`、`S1_ZT_1.*` | 已把当前 26 个 live identifier 全量补到 draft seed，并把 `pa_state` 修正为 `bool` 占位 | `DESCRIBE` 已确认 `l1_js_1(gx,gy,gz)`、`l1_qj_1(angle,azi,x,y,z)`、`l1_lf_1.lf`、`s1_zt_1.*`；当前阻塞转为 `gX/gY/gZ`、`X/Y/Z/azimuth` 到真实列名的 canonicalization 与 `gz`/状态类 mixed type 收敛 |
| `south_gnss_monitor` | GNSS 位移、惯导、倾角与状态类补点 | `L1_GP_1.*`、`L1_JS_1.*`、`L1_QJ_1.*`、`S1_ZT_1.*` | 已把当前 31 个 live identifier 全量补到 draft seed，建议 identifier 继续保持 live key | `DESCRIBE` 已确认 `l1_gp_1(gps_total_z,gps_total_x,gps_total_y)`、`l1_js_1(gx,gy,gz)`、`l1_qj_1(angle,azi,x,y,z)`、`s1_zt_1.*`；当前阻塞转为 live identifier 到真实列名的映射收敛与 mixed type 处置 |
| `south_deep_displacement` | 现有 `dispsX / dispsY` 仅补占位结构，父级状态类单独补点 | 现有 `dispsX / dispsY`，补充 17 个 `S1_ZT_1.*` 父级状态 seed | 对现有 property 只补 `tdengineLegacy` 占位结构；父级状态类已扩到一整组 draft seed | `DESCRIBE` 已确认 `l1_sw_1.disps_x / disps_y` 与 `s1_zt_1.*` 存在；单设备 `L1_SW_1.* -> dispsX/dispsY` 协议 alias 已补齐，当前重点转为历史残留 `L1_SW_*`、临时验证指标与列名下划线映射收敛 |

### 2026-03-25 dry-run 结果补记

- 已在当前真实库执行 `sql/upgrade/20260324_phase5_tdengine_mapping_gap_draft.sql` 的 dry-run 预览；为兼容 `rm_iot` 当前库级 `utf8mb4_general_ci` 与业务表字段 `utf8mb4_0900_ai_ci` 的混用，脚本已补充临时表显式 collation，避免预览阶段 join 直接报错。
- 当前 property 基线：
  - `south_rtu`：`iot_product_model` 中 `property_count=0`，`tdengineLegacy=0`。
  - `south_multi_displacement`：`property_count=0`，`tdengineLegacy=0`。
  - `south_gnss_monitor`：`property_count=0`，`tdengineLegacy=0`。
  - `south_deep_displacement`：`property_count=2`（仅 `dispsX / dispsY`），`tdengineLegacy=0`。
- 代表性 seed 覆盖情况：
  - `south_rtu / south_multi_displacement / south_gnss_monitor` 的 12 个代表性 seed 当前全部 `MISSING_MODEL`。
  - `south_deep_displacement` 中 `dispsX / dispsY` 已存在，但仍是 `MISSING_TDENGINE_LEGACY`；`S1_ZT_1.ext_power_volt` 仍为 `MISSING_MODEL`。
- live gap 汇总：
  - `south_rtu`：`DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=31`。当前已确认未追踪的裂缝点位至少包含 `L1_LF_4` ~ `L1_LF_9`。
  - `south_multi_displacement`：`DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=22`。
  - `south_gnss_monitor`：`DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=27`。
  - `south_deep_displacement`：`DRAFT_PATCH=2`，`DRAFT_SEED=1`，`UNTRACKED_LIVE_GAP=36`；父级 `S1_ZT_1.*` 已确认还缺 `battery_dump_energy`、`battery_volt`、`consume_power`、`humidity`、`humidity_out`、`lat`、`lon`、`sensor_state.L1_SW_1`、`signal_4g`、`singal_db`、`singal_NB`、`solar_volt`、`supply_power`、`sw_version`、`temp`、`temp_out`。
- 为避免下一轮继续靠人工比对，草案 SQL 末尾已新增 live gap 导出查询，会直接输出 `MISSING_MODEL / MISSING_TDENGINE_LEGACY` 与 `DRAFT_SEED / DRAFT_PATCH / UNTRACKED_LIVE_GAP`。

### 2026-03-25 第二轮 seed 扩充 dry-run 补记

- 已把 `south_rtu` 的 `L1_LF_4` ~ `L1_LF_9` 追加到 `tmp_tdengine_mapping_gap_seed`，并把 `south_deep_displacement` 父级 `S1_ZT_1.*` 缺口一次性扩充到 17 条 draft seed。
- 当前 seed 行数：
  - `south_rtu=10`
  - `south_multi_displacement=4`
  - `south_gnss_monitor=4`
  - `south_deep_displacement=17`
- 第二轮 dry-run 后的 live gap 汇总：
  - `south_rtu`：`DRAFT_SEED=10`，`UNTRACKED_LIVE_GAP=25`。
  - `south_multi_displacement`：仍为 `DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=22`。
  - `south_gnss_monitor`：仍为 `DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=27`。
  - `south_deep_displacement`：`DRAFT_PATCH=2`，`DRAFT_SEED=17`，`UNTRACKED_LIVE_GAP=20`。
- 本轮 dry-run 额外发现：
  - `south_rtu` 的 `L1_LF_4` 在真实库中同时出现 `int / double` 两种 `value_type`，草案先统一按裂缝数值 `double` 占位，后续要结合 live 样本再确认是否需要类型兼容。
  - `south_rtu` 的 `S1_ZT_1.signal_4g`、`S1_ZT_1.singal_NB` 当前也存在 `int / string` 混型，下一轮不宜直接按单一数值类型落正式映射，优先补 normalizer 类型兼容或在草案里保留更保守的占位口径。
  - `south_rtu` 当前剩余未追踪缺口，已集中收敛到父级状态类与传感器状态指标：`S1_ZT_1.battery_dump_energy`、`battery_volt`、`consume_power`、`humidity`、`humidity_out`、`lat`、`lon`、`signal_4g`、`singal_bd`、`singal_db`、`singal_NB`、`solar_volt`、`supply_power`、`sw_version`、`temp`、`temp_out`，以及 `S1_ZT_1.sensor_state.L1_LF_1` ~ `S1_ZT_1.sensor_state.L1_LF_9`。
  - `south_deep_displacement` 当前剩余未追踪缺口，已主要集中在 split child 指标 `L1_SW_1` ~ `L1_SW_8` 的 `dispsX / dispsY`，以及临时验证残留 `codex_verify_humidity_20260324`、`codex_verify_temp_20260324`、`humidity`、`temperature`；其中 `L1_SW_*.(dispsX|dispsY)` 当前全部为 `double`，更适合作为下一轮批量 seed / alias 决策对象。

### 2026-03-25 第三轮 seed 扩充 dry-run 补记

- 已把 `south_rtu` 父级状态类与 `sensor_state.L1_LF_*` 一次性补齐到草案：
  - 父级状态字段：`battery_dump_energy`、`battery_volt`、`consume_power`、`humidity`、`humidity_out`、`lat`、`lon`、`signal_4g`、`singal_bd`、`singal_db`、`singal_NB`、`solar_volt`、`supply_power`、`sw_version`、`temp`、`temp_out`
  - 传感器状态字段：`S1_ZT_1.sensor_state.L1_LF_1` ~ `S1_ZT_1.sensor_state.L1_LF_9`
- 当前 seed 行数：
  - `south_rtu=35`
  - `south_multi_displacement=4`
  - `south_gnss_monitor=4`
  - `south_deep_displacement=17`
- 第三轮 dry-run 后的 live gap 汇总：
  - `south_rtu`：`DRAFT_SEED=35`，已无 `UNTRACKED_LIVE_GAP`。
  - `south_multi_displacement`：仍为 `DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=22`。
  - `south_gnss_monitor`：仍为 `DRAFT_SEED=4`，`UNTRACKED_LIVE_GAP=27`。
  - `south_deep_displacement`：`DRAFT_PATCH=2`，`DRAFT_SEED=17`，`UNTRACKED_LIVE_GAP=20`。
- 本轮 dry-run 额外发现：
  - `south_rtu` 当前的主要问题已从“seed 缺失”转为“字段类型与 legacy 列定义尚未收敛”，其中 `L1_LF_3 / L1_LF_4` 仍有 `int / double` 混型，`signal_4g / singal_NB` 仍有 `int / string` 混型。
  - 草案因此对 `signal_4g`、`singal_NB` 先采用 `string` 占位；这只是治理占位，不代表最终产品物模型应直接固化为字符串。
  - 当前唯一仍未追踪的真实环境 gap 已集中到 `south_deep_displacement`：`L1_SW_1` ~ `L1_SW_8` 的 `dispsX / dispsY`，以及临时验证残留 `codex_verify_humidity_20260324`、`codex_verify_temp_20260324`、`humidity`、`temperature`。

### 2026-03-25 深部位移单设备 alias 根因补记

- 继续排查 `south_deep_displacement` 的 `UNTRACKED_LIVE_GAP=20` 后，已确认其中最新写入的 `L1_SW_1.dispsX / L1_SW_1.dispsY` 并非“基站缺少子设备映射”，而是单设备深部位移终端 `SK00EB0D1308310 / SK00EB0D1308314` 在无 `sub-device-mappings` 场景下仍把唯一逻辑测点保留为 `L1_SW_1.*` 前缀。
- 代码对照结论：
  - 基准站 `SK00FB0D1310195` 的“基站 + 8 子设备”路径仍应走 `LegacyDpChildMessageSplitter` 的子消息拆分，并写入子设备 `dispsX / dispsY`。
  - 单设备深部位移场景不应继续向 `iot_device_property` 写入 `L1_SW_1.dispsX` 这类前缀属性，而应折叠为当前设备自身的 `dispsX / dispsY`，与现有产品物模型保持一致。
- 已补协议层修复：
  - `LegacyDpChildMessageSplitter` 在“无子设备映射 + family code 仅包含 `S1_ZT_1` 与单个 `L1_SW_*` 逻辑测点”场景下，会把逻辑前缀属性折叠为当前设备属性，并保留混合传感器报文的原始 `L1_SW_*` 前缀。
  - 新增 `LegacyDpChildMessageSplitterTest.shouldCollapseSingleDeepDisplacementLogicalCodeWhenNoSubDeviceMappingExists` 回归测试，覆盖 `S1_ZT_1` 状态字段与 `L1_SW_1.dispsX / dispsY` 共存场景。
  - 新增 `MqttJsonProtocolAdapterTest.shouldCollapseSingleDeepDisplacementLogicalPropertiesWithoutSubDeviceMappings` 适配器层回归测试，并补充 `LegacyDpChildMessageSplitterTest.shouldKeepLogicalPrefixWhenLegacyPayloadContainsOtherSensorFamilies` 反例，锁住“混合传感器报文不折叠”边界。
  - 协议回归命令 `mvn -pl spring-boot-iot-protocol -am "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=MqttJsonProtocolAdapterTest,LegacyDpEnvelopeDecoderTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest,MqttTopicParserTest,MqttPayloadSecurityValidatorTest,MqttPayloadDecryptorRegistryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 已通过，合计 30 条测试全部通过。
- `2026-03-27` 已在当前 worktree 对应的 `dev` 实例补做真实环境复验：
  - 通过 `POST /api/message/mqtt/report/publish` 向 `$dp` 发送 `STANDARD_TYPE_2` 明文帧，样本设备 `SK00EB0D1308310`，`sessionId=bb342847b1b14fefa8c2d238f4c68bdc`，`traceId=c15a78d53f0443c0a47ad20154ab8a3b`。
  - `GET /api/device/message-flow/trace/c15a78d53f0443c0a47ad20154ab8a3b` 已确认阶段完整闭环，`PROTOCOL_DECODE.summary.familyCodes=["L1_SW_1"]`、`normalizationStrategy=LEGACY_DP_COMPAT`、`childSplitApplied=false`，`PAYLOAD_APPLY.summary.propertyCount=2`，`TELEMETRY_PERSIST.summary.normalizedFallbackCount=2`。
  - `GET /api/device/SK00EB0D1308310/properties` 已确认本轮新增写入的是当前设备 `dispsX=-0.0445`、`dispsY=0.0293`，`updateTime=2026-03-27T09:08:35`；历史残留 `L1_SW_1.dispsX / L1_SW_1.dispsY` 没有被继续刷新。
  - `GET /api/telemetry/latest?deviceId=1925451072155930626` 已确认最新 `traceId=c15a78d53f0443c0a47ad20154ab8a3b` 只把本轮新值写入 canonical `dispsX / dispsY`，而旧 `L1_SW_1.*` 仅作为历史残留继续可见。
- 注意：当前真实库里的 `UNTRACKED_LIVE_GAP=20` 仍是历史数据快照，未随代码修改自动清零；但 `2026-03-27` 的新样本已确认单设备场景不会继续生成新的 `L1_SW_1.*` 前缀属性。

### 2026-03-25 第四轮 seed 扩充 dry-run 补记

- 已把 `south_multi_displacement` 的剩余 22 个 live identifier 全量补到 draft seed，并修正 `S1_ZT_1.pa_state` 为 `bool` 占位。
- 已把 `south_gnss_monitor` 的剩余 27 个 live identifier 全量补到 draft seed，覆盖 `L1_JS_1.*`、`L1_QJ_1.*` 与父级 `S1_ZT_1.*` 状态类。
- 当前 seed 行数：
  - `south_rtu=35`
  - `south_multi_displacement=26`
  - `south_gnss_monitor=31`
  - `south_deep_displacement=17`
- 第四轮 dry-run 后的 live gap 汇总：
  - `south_rtu`：`DRAFT_SEED=35`。
  - `south_multi_displacement`：`DRAFT_SEED=26`，已无 `UNTRACKED_LIVE_GAP`。
  - `south_gnss_monitor`：`DRAFT_SEED=31`，已无 `UNTRACKED_LIVE_GAP`。
  - `south_deep_displacement`：`DRAFT_PATCH=2`，`DRAFT_SEED=17`，`UNTRACKED_LIVE_GAP=20`。
- 本轮 dry-run 额外发现：
  - `south_multi_displacement` 当前的主要问题已从“seed 缺失”转为“字段类型与 legacy 列定义尚未收敛”，其中 `humidity / solar_volt / temp` 存在 `double / int` 混型，`lat / lon` 存在 `string / double` 混型，`pa_state` 的真实类型为 `bool`。
  - `south_gnss_monitor` 当前也已从“seed 缺失”转为“类型与列定义收敛”问题，其中 `L1_JS_1.gY / gZ`、`L1_QJ_1.angle / Y / Z`、`S1_ZT_1.ext_power_volt` 仍存在 `double / int` 混型；此外 `S1_ZT_1.sensor_state.L1_JZ_1` 说明 live 场景还带有额外传感器状态分支，后续要结合 TDengine `DESCRIBE` 决定是否长期纳管。
  - 经过本轮补齐后，映射草案的唯一未追踪 live gap 已稳定收敛到 `south_deep_displacement` 的历史残留 `L1_SW_*.(dispsX|dispsY)` 与临时验证指标。

### 2026-03-25 TDengine DESCRIBE 复核补记

- 已通过 `curl -u root:*** http://8.130.107.120:6041/rest/sql/iot -d "DESCRIBE <stable>"` 复核 TDengine REST 鉴权与 SQL 查询链路，`PENDING_TDENGINE_VERIFICATION` 的“认证未恢复”阻塞已解除。
- 关键 stable 当前真实列定义如下：
  - `s1_zt_1`：`ext_power_volt`、`solar_volt`、`battery_dump_energy`、`signal_nb`、`signal_db`、`temp`、`humidity`、`lon`、`lat`、`signal_4g`、`sw_version`、`pa_state`、`sound_state`、`sensor_state`
  - `l1_lf_1`：`lf`
  - `l1_js_1`：`gx`、`gy`、`gz`
  - `l1_qj_1`：`angle`、`azi`、`x`、`y`、`z`
  - `l1_gp_1`：`gps_total_z`、`gps_total_x`、`gps_total_y`
  - `l1_sw_1`：`disps_x`、`disps_y`
- 复核后结论：
  - 当前阻塞不再是“TDengine 连不上 / 查不到 stable”，而是 live identifier 到真实列名的 canonicalization、下划线风格收敛，以及 mixed type 与历史脏数据清理。
  - `l1_js_1.gz` 当前真实类型为 `NCHAR`，与 live 侧 `gZ` 的数值预期存在冲突，后续需要先确认是否沿用字符串列、改写映射策略，还是把该指标继续保留为 `enabled=false` 占位。
  - `s1_zt_1` 已确认采用 `signal_nb / signal_db` 命名，不应继续把 `singal_NB / singal_db` 这类拼写漂移当作正式映射候选。

## Task 7 Step 2 下一步实现计划

1. 先执行 `sql/upgrade/20260324_phase5_tdengine_mapping_gap_draft.sql` 的 dry-run 预览，确认四类 `productKey` 在当前真实库里的 property 基线、已有 `tdengineLegacy` 和代表性缺口。
2. `south_deep_displacement` 不再继续盲补 `L1_SW_*.(dispsX|dispsY)` 到草案；`2026-03-27` 真实环境补采样已确认单设备场景回到 `dispsX / dispsY`。下一步只清理历史残留 `L1_SW_*` 与临时验证指标，并为现有 `dispsX / dispsY` 收敛 `tdengineLegacy -> l1_sw_1.disps_x / disps_y` 的 canonical mapping；`south_multi_displacement / south_gnss_monitor` 当前仅剩类型收敛与 TDengine 列核验。
3. 基于已确认的 `DESCRIBE` 结果，逐 stable 把草案里的 hint 收敛为真实列名建议：
   - `L1_GP_1.gpsTotalZ/X/Y -> l1_gp_1.gps_total_z/x/y`
   - `L1_JS_1.gX/gY/gZ -> l1_js_1.gx/gy/gz`
   - `L1_QJ_1.azimuth/X/Y/Z -> l1_qj_1.azi/x/y/z`
   - `dispsX/dispsY -> l1_sw_1.disps_x/disps_y`
   - `signal_NB/signal_db/singal_* -> s1_zt_1.signal_nb/signal_db`
4. 在 canonicalization 与 mixed type 策略未定前，继续把 `NORMALIZED_FALLBACK_ONLY` 视为治理信号，而不是默认成功态；草案 SQL 也不得直接作为真实环境最终升级脚本执行。
5. 映射 hint 收敛后，重新执行 Task 7 的 live 验收，至少对 `south_rtu / south_multi_displacement / south_gnss_monitor / south_deep_displacement` 各补 1 个正样本，复核 `legacyStableCount` 是否恢复，并重点观察 `l1_sw_1.disps_x / disps_y` 是否开始命中 legacy stable。
6. 若后续需要新的隔离验收环境，优先在仓库根目录 `.worktrees/` 下创建 worktree，不再继续复用 `~/.codex/worktrees/2a61/...` 这类临时路径。

## 推进顺序建议

1. 先做 Task 1，冻结真实 `$dp` 基线和家族样本。
2. 再做 Task 2，只补观测，不改行为。
3. 然后做 Task 3 和 Task 4，把 `MqttJsonProtocolAdapter` 拆薄。
4. 再做 Task 5，治理 TDengine 映射服务层。
5. 最后做 Task 6 和 Task 7，双轨切换、分家族验收、清旧逻辑。

## 风险与止损点

- 若 Task 2 做完后仍无法稳定识别家族，说明现网厂商 payload 差异超出预期，需要先补样本，不应继续深拆。
- 若 Task 4 使现有属性名大面积变化，应立即停止，改走“属性别名兼容”设计，而不是强推统一命名。
- 若 Task 5 后 `NORMALIZED_FALLBACK_ONLY` 比例不降反升，说明映射抽象有问题，应先回查 mapping service 输出，而不是继续改 writer。
- 若真实环境出现 `PROTOCOL_DECODE` 失败放大、`DEVICE_CONTRACT` 命中率下降或 `TELEMETRY_PERSIST.failedTargetCount` 上升，应立刻回退到旧 normalizer 路径。

## 完成定义

- `$dp` 相关复杂逻辑不再集中堆叠在单个 adapter 类中。
- `message-flow` 能直接看见 `familyCodes / appId / normalizationStrategy`。
- 主要地灾家族都具备真实环境正样本与回归测试。
- 已治理家族的 TDengine legacy stable 命中率提升，fallback 退回到安全网角色。
- 文档、配置、真实环境验收手册与代码事实同步。
