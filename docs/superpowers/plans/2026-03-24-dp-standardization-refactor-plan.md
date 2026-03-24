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

1. `$dp` 复杂度过度集中在 [MqttJsonProtocolAdapter.java](E:/idea/ghatg/spring-boot-iot/spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java)，单类同时承担解密、帧解析、安全校验、消息类型推断、属性扁平化和子消息拆分。
2. 地灾家族识别逻辑是隐式散落的，`status/property` 推断、时间戳容器识别和属性拍平耦合在一起，新增厂商兼容时很容易继续堆分支。
3. `TELEMETRY_PERSIST` 成功并不等于 legacy stable 命中成功；当前 `NORMALIZED_FALLBACK_ONLY` 很容易掩盖映射缺口。
4. 遥测映射仍通过产品物模型 `specsJson.tdengineLegacy` 隐式表达，缺少统一校验入口、缺少治理维度，也不便统计“哪些家族长期只走 fallback”。
5. 当前 `message-flow` 虽然已经能看 10 阶段结果，但缺少 `familyCodes`、`vendor/appId`、`normalizationStrategy` 等协议治理核心维度。
6. 真实环境在持续接入，不能接受“大改完成后一次性上线再看结果”的切换方式，必须支持双轨观察与逐步放量。

## 目标架构

### 1. 保持不变的边界

- MQTT 接收入口继续由 [MqttMessageConsumer.java](E:/idea/ghatg/spring-boot-iot/spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java) 负责。
- 固定编排继续由 [UpMessageProcessingPipeline.java](E:/idea/ghatg/spring-boot-iot/spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java) 负责。
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

- [ ] **Step 3: 对每个样本记录以下字段**

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

### Task 1 执行快照（2026-03-24 21:06 CST）

- `GET /actuator/health/mqttConsumer` 已确认真实运行基线：`status=UP`、`consumerActive=true`、`connected=true`、`leadershipMode=LEADER`，订阅主题包含 `$dp`。
- `GET /api/device/message-flow/recent?size=30` 已确认最近样本全部来自真实 MQTT `$dp` 会话，可稳定提取 `traceId / deviceCode / dataFormatType / messageType / childMessageCount / TELEMETRY_PERSIST.branch / legacyStableCount / normalizedFallbackCount`。
- 当前 `message-flow/session/{sessionId}` 的 `PROTOCOL_DECODE.summary` 仍未暴露 `appId / familyCodes / normalizationStrategy / timestampSource`，因此 Step 3 只能部分完成；该缺口正是 Task 2 的直接输入，不再等待额外样本补齐后才推进。

### Task 1 样本摘录

| traceId | sessionId | deviceCode | productKey | dataFormatType | messageType | childMessageCount | TELEMETRY_PERSIST.branch | legacyStableCount | normalizedFallbackCount | 备注 |
|---|---|---|---|---|---|---:|---|---:|---:|---|
| `a94856d24eb64ed2bae321456c23c689` | `48ed7ac5dbd249628b216796d72ed092` | `SK00EA0D1307984` | `south_rtu` | `STANDARD_TYPE_2` | `property` | 0 | `NORMALIZED_FALLBACK_ONLY` | 0 | 3 | 当前无法从时间线直接看到 `appId / familyCodes` |
| `5dc58d0dfc2f41a6870f0c34aca42a71` | `0ec35e89aa2e4730a3bbeeeeba3c80b7` | `SK00EB0D1308296` | `south_deep_displacement` | `STANDARD_TYPE_2` | `status` | 0 | `NORMALIZED_FALLBACK_ONLY` | 0 | 17 | 深部位移样本，fallback 点位较多 |
| `7c4b1c23e8894662bc54fcc470de8b0f` | `c1d0002194364e3a9bf6e43fea434433` | `SJ11F2148734232A` | `south_gnss_monitor` | `STANDARD_TYPE_2` | `property` | 0 | `NORMALIZED_FALLBACK_ONLY` | 0 | 8 | GNSS 样本 |
| `cc98aa9ec7f4474f848ca66295354743` | `da4468c889f240bc80635f6f58b90bb1` | `SK11EB0D1308097AZ` | `south_multi_displacement` | `STANDARD_TYPE_2` | `status` | 0 | `NORMALIZED_FALLBACK_ONLY` | 0 | 14 | 多维位移样本 |

### Task 1 当前结论

- 当前 live `$dp` 真实链路稳定，Task 1 Step 1 / Step 2 已完成。
- 当前高风险信号已经明确：抽查样本全部命中 `NORMALIZED_FALLBACK_ONLY`，说明 legacy stable 映射治理仍未覆盖这些家族。
- Step 3 的 `appId / familyCodes` 台账记录被当前可观测缺口阻塞；继续补样本不会解锁该问题，因此下一步直接执行 Task 2，为 `$dp` 解码与 `message-flow` 补齐协议元数据。

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

### Task 2 执行结果（2026-03-24 21:16 CST）

- 已新增 `DeviceUpProtocolMetadata`，并挂载到 `DeviceUpMessage.protocolMetadata`。
- `$dp` 解码后当前会补齐：
  - `appId`
  - `familyCodes`
  - `normalizationStrategy=LEGACY_DP`
  - `timestampSource`
  - `childSplitApplied`
  - `routeType`
- `UpMessageProcessingPipeline` 当前会把上述元数据中的 `appId / familyCodes / normalizationStrategy / timestampSource / childSplitApplied` 回写到 `PROTOCOL_DECODE.summary`，不改变既有固定 10 阶段顺序和下游业务处理分支。
- 红灯测试先确认当前实现缺少 `getProtocolMetadata()` / `DeviceUpProtocolMetadata`；随后最小实现已拉绿以下验证：

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-message -am -DskipTests=false -Dtest=UpMessageProcessingPipelineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 说明：当前工作区缺少 `.mvn/settings.xml`，已按仓库规则回退到 plain `mvn`；消息模块验证使用 `-am`，以确保新引入的 protocol 模型类参与同次 Reactor 编译。

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

### Task 3 执行结果（2026-03-24 21:31 CST）

- 已新增 `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpEnvelopeDecoder.java`，统一承接：
  - 明文 JSON
  - 带控制字符前缀的明文 JSON
  - `header.appId + bodies.body` 加密外层包
- 当前 `LegacyDpEnvelopeDecoder` 已接管原先位于 `MqttJsonProtocolAdapter` 中的 `$dp` 外层解包、解密、二次帧解析和 `dataFormatType` 富化逻辑；adapter 只保留解码编排、家族元数据组装、属性拍平和子消息拆分。
- 红灯测试先通过 `LegacyDpEnvelopeDecoderTest` 明确确认 “类缺失”；随后最小迁移拉绿了以下协议侧验证：

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest,MqttPayloadDecryptorRegistryTest,LegacyDpEnvelopeDecoderTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 额外回归：

```bash
mvn -pl spring-boot-iot-message -am -DskipTests=false -Dtest=UpMessageProcessingPipelineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：协议模块 18 个定向测试全部通过，`UpMessageProcessingPipelineTest` 继续通过，说明本轮抽类没有改变固定 10 阶段主链路行为。

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

### Task 4 执行结果（2026-03-24 21:46 CST）

- 已新增：
  - `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpFamilyResolver.java`
  - `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java`
  - `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildMessageSplitter.java`
  - `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java`
- 当前 `MqttJsonProtocolAdapter` 已进一步收口为编排类：`$dp` 解码阶段改为复用“家族识别 -> 属性标准化 -> 子消息拆分”三段式组件，不再在主类内直接维护全部 legacy 细节。
- 红灯测试先确认了计划内类缺失；拉绿过程中又补齐了两项行为约束：
  - `LegacyDpFamilyResolver` 只从 payload 顶层和设备体识别家族码，避免把 `ext_power_volt` 之类属性名误判为家族
  - `LegacyDpChildMessageSplitter` 对 `iot.device.sub-device-mappings` 按 `logicalCode` 稳定排序，避免子消息顺序随配置 Map 实现波动
- 本轮定向回归：

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest test
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest,LegacyDpPropertyNormalizerTest,LegacyDpChildMessageSplitterTest,MqttTopicParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：15 个协议侧定向测试全部通过，核心家族属性键、时间戳选择、`messageType` 推断和深部位移子消息拆分行为均保持与既有链路一致。

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
mvn -s .mvn/settings.xml -pl spring-boot-iot-device "-DskipTests=false" "-Dtest=DevicePropertyMetadataServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=LegacyTdengineTelemetryWriterTest,LegacyTdengineTelemetryReaderTest,TdengineTelemetryFacadeTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- legacy/fallback 组合策略保持可回归

### Task 5 执行结果（2026-03-24 22:17 CST）

- 已新增：
  - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/DeviceTelemetryMappingService.java`
  - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceTelemetryMappingServiceImpl.java`
  - `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/TelemetryMetricMapping.java`
- 当前 legacy 映射读取已经统一收口到 `DeviceTelemetryMappingService`：
  - 继续兼容 `specsJson.tdengineLegacy`
  - 会显式标记 `MISSING_TDENGINE_LEGACY_MAPPING / INVALID_SPECS_JSON / DISABLED / MISSING_STABLE / INVALID_STABLE / MISSING_COLUMN / INVALID_COLUMN`
  - `DevicePropertyMetadataServiceImpl` 仅负责把显式映射结果回填给原有属性元数据模型，保持现有调用方兼容
- `LegacyTdengineTelemetryWriter / Reader` 已改为消费显式映射服务，`TdengineTelemetryFacade` 已补齐：
  - `legacyMappedMetricCount`
  - `legacyUnmappedMetricCount`
  - `fallbackMetricCount`
  - `fallbackReason`
- 当前命中 fallback 时，除了 `TELEMETRY_PERSIST.branch` 外，还会输出治理日志和时间线摘要，不再把 `NORMALIZED_FALLBACK_ONLY` 误读为“完全成功”。
- 本轮定向回归：

```bash
mvn -pl spring-boot-iot-device -DskipTests=false -Dtest=DeviceTelemetryMappingServiceImplTest,DevicePropertyMetadataServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-telemetry -am -DskipTests=false -Dtest=LegacyTdengineTelemetryWriterTest,LegacyTdengineTelemetryReaderTest,TdengineTelemetryFacadeTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-message -am -DskipTests=false -Dtest=UpMessageProcessingPipelineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：device 侧 3 个定向测试、telemetry 侧 11 个定向测试、message 侧 7 个定向测试全部通过；映射服务、legacy/fallback 组合策略和时间线透传均保持可回归。

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

### Task 6 执行结果（2026-03-24 22:30 CST）

- 已在 `IotProperties` 与 `application-dev.yml` 中补齐 3 个双轨治理开关：
  - `iot.protocol.legacy-dp.family-observability-enabled`
  - `iot.protocol.legacy-dp.normalizer-v2-enabled`
  - `iot.telemetry.legacy-mapping-validate-only`
- `MqttJsonProtocolAdapter` 当前会按开关控制 `$dp` 家族可观测字段暴露；关闭 `family-observability-enabled` 后，不再向 `protocolMetadata / PROTOCOL_DECODE.summary` 暴露 `familyCodes / timestampSource / childSplitApplied / normalizationStrategy`。
- `normalizer-v2-enabled=false` 时，当前只把 `normalizationStrategy` 回写为 `LEGACY_DP_COMPAT`，继续保持固定 Pipeline、控制器接口和鉴权口径不变。
- `TdengineTelemetryFacade` 当前已补齐 `legacy_mapping_validate_only` 日志路径：在 `validate-only` 模式下会输出 old/new 映射差异，但仍沿用当前落库结果，不改变 `TELEMETRY_PERSIST.branch`。
- 本轮新鲜验证已通过：

```bash
mvn -pl spring-boot-iot-protocol -am -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-telemetry -am -DskipTests=false -Dtest=TdengineTelemetryFacadeTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-message -am -DskipTests=false -Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest,MqttReportPublishServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：protocol 侧 11 个测试、telemetry 侧 5 个测试、message 侧 16 个测试全部通过，说明特性开关接入没有破坏 MQTT 入口、固定 10 阶段时间线和 TDengine 遥测双轨逻辑。

### Task 7: 真实环境分家族验收、文档更新和旧逻辑清理

**Files:**
- Modify: `docs/05-protocol.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Inspect: `README.md`
- Inspect: `AGENTS.md`

- [ ] **Step 1: 按家族执行真实环境验收**

Minimum checks per family:
- `message-flow` 中 10 阶段完整
- `PROTOCOL_DECODE.summary.familyCodes` 正确
- `PAYLOAD_APPLY` 属性数正确
- `TELEMETRY_PERSIST` 分支符合预期
- `/api/telemetry/latest` 可见最新值
- TDengine stable 或 fallback 表中可查

- [ ] **Step 2: 对每个家族都记录 legacy 命中率与 fallback 命中率**

Acceptance target:
- 已完成治理的家族，fallback 比例应显著下降
- fallback 仅保留未映射指标或临时兼容项

- [x] **Step 3: 原位更新权威文档**

Required updates:
- `05-protocol`：补齐 `$dp` 新的分段结构和家族标准化口径
- `11`：补齐 `familyCodes/appId/fallback` 观测口径
- `07`：补齐新开关配置说明
- `真实环境测试与验收手册`：补齐分家族验收步骤
- `08`：记录本轮治理边界和后续技术债

- [ ] **Step 4: 只有在真实环境稳定后才删除 adapter 中的旧私有方法**

Removal target:
- 零散的 legacy 推断私有方法
- 与新组件重复的旧 helper

- [ ] **Step 5: 运行最终验证命令**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-message "-DskipTests=false" "-Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryPersistStageHandlerTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 协议、消息、遥测三个模块关键回归通过

### Task 7 执行快照（2026-03-24 22:34 CST）

- 已确认共享环境入口可用：

```bash
curl -sS http://127.0.0.1:9999/actuator/health/mqttConsumer
curl -sS http://127.0.0.1:9999/actuator/health
```

- 结果：共享 `9999` 实例 `mqttConsumer.status=UP`、`connected=true`、订阅主题包含 `$dp`，且最近会话中持续有真实 `$dp` 流量。
- 已抽查真实会话：

```bash
# 先登录获取 Bearer token，再调用：
curl -sS 'http://127.0.0.1:9999/api/device/message-flow/recent?size=30' -H "Authorization: Bearer <token>"
curl -sS 'http://127.0.0.1:9999/api/device/message-flow/session/ac83a6967984491fb06745e09f65a1da' -H "Authorization: Bearer <token>"
```

- 观察：共享 `9999` 的真实 `$dp` `PROTOCOL_DECODE.summary` 仍只包含 `routeType / messageType / dataFormatType / childMessageCount / filePayload / correlationMatched`，没有本轮新增的 `appId / familyCodes / normalizationStrategy / timestampSource / childSplitApplied`。
- 同时，`/actuator/health` 中 `diskSpace.path` 指向另一套共享工作区，而不是当前 worktree，说明该实例并非本分支代码。
- `README.md` 与 `AGENTS.md` 已按规则复核；本轮没有新增交付边界、模块职责或启动命令分支，因此不需要同步改动这两份文件。
- 结论：Task 7 Step 1 / Step 2 当前被“共享验收入口未对齐当前分支实例”阻塞。由于真实 `$dp` 正在持续接入，不能通过再起一个并行消费同一共享 Broker 的 live consumer 来补验收，否则会带来重复入库和重复风控分发风险。
- 本轮已先完成 Step 3 文档更新；Step 4 旧逻辑清理与 Step 5 最终验证需等隔离验收实例、独立 Broker 或真实设备窗口对齐后再继续。

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
