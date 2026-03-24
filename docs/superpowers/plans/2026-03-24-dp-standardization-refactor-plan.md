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

- [x] **Step 5: 运行最终验证命令**

Run:

```powershell
mvn -pl spring-boot-iot-protocol "-DskipTests=false" "-Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl spring-boot-iot-message "-DskipTests=false" "-Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryPersistStageHandlerTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- 协议、消息、遥测三个模块关键回归通过

<<<<<<< HEAD
### Task 7 执行快照（2026-03-24 23:29 CST）

- 已完成共享运行态对齐：

```bash
curl -sS http://127.0.0.1:9999/actuator/health
curl -sS http://127.0.0.1:9999/actuator/health/mqttConsumer
```

- 当前 `9999` 实例已明确来自本 worktree：
  - `diskSpace.path=/Users/rxbyes/.codex/worktrees/2a61/spring-boot-iot/...`
  - `mqttConsumer.status=UP`
  - `connected=true`
  - `leadershipMode=LEADER`
  - `subscribeTopics` 包含 `$dp`
- 当前 live `$dp` `message-flow/session/{sessionId}` 已暴露本轮新增协议元数据，`PROTOCOL_DECODE.summary` 可稳定看到 `appId / familyCodes / normalizationStrategy / timestampSource / childSplitApplied`。

#### Task 7 Step 1 抽样验收实证

| 家族焦点 | sessionId | traceId | deviceCode | `familyCodes` | `TELEMETRY_PERSIST.branch` | `/api/telemetry/latest` | TDengine fallback 取证 |
|---|---|---|---|---|---|---|---|
| `L1_JS_1 + L1_LF_1` | `9fd82ce723c74806a53363a30a9d1cef` | `713a1476166a4cbb8ef3d5e6f683b0a8` | `SK11E80D1307259AZ` | `["L1_JS_1","L1_LF_1"]` | `NORMALIZED_FALLBACK_ONLY` | `traceId=713a1476166a4cbb8ef3d5e6f683b0a8`，`propertyCount=24` | `iot_device_telemetry_point` 按 `device_code + trace_id` 可查 `4` 条 |
| `L1_LF_2 + L1_LF_3` | `69a22aa38232494d82aff7dec52f0e75` | `220eeb9f6f824f05b53341f428bf7575` | `SK00EA0D1308009` | `["L1_LF_1","L1_LF_2","L1_LF_3"]` | `NORMALIZED_FALLBACK_ONLY` | 最新值已滚到 `traceId=0cba31ebac9a44d7b9b2cc086f57a246`，`propertyCount=22` | 按样本 `traceId` 可查 `3` 条 |
| `S1_ZT_1` | `fdd6ffa7a3474fa08853688e6cb44587` | `0be3e661123b4bfcb182f7eb7ed86223` | `SK00EA0D1308009` | `["S1_ZT_1"]` | `NORMALIZED_FALLBACK_ONLY` | 同设备最新值已滚到 `traceId=0cba31ebac9a44d7b9b2cc086f57a246`，`propertyCount=22` | 按样本 `traceId` 可查 `16` 条 |

- 上述三类代表家族已能在当前 worktree 实例上完成 `message-flow -> latest API -> TDengine fallback` 的闭环取证，但 `legacyStableCount` 仍然全部为 `0`，`fallbackReason` 均为 `MISSING_TDENGINE_LEGACY_MAPPING`。

#### 深部位移 split 家族现状

- 旧阻塞样本 `92d0d6b6cc214a718b0436eb0a055134 / 41ed04b0d68a4bc6ac413c33c9987aa9` 与 `d75b14a96d184f859aa63526a13c9550 / 420a14f3df444516a9a81bfeade1822a` 曾表现为：
  - `PROTOCOL_DECODE.childMessageCount=8`
  - `DEVICE_CONTRACT.childTargetCount=8`
  - `PAYLOAD_APPLY` 出现 `8` 条 `CHILD_PROPERTY`
  - `TELEMETRY_PERSIST.summary.persistedTargetCount=8`
  - `TELEMETRY_PERSIST.summary.persistedPointCount=16`
  - 但 TDengine fallback 表只剩最后一个 child `84330696` 的 `2` 条数据
- 根因已定位并修复：`iot_device_telemetry_point` 是共享单表，旧实现按“单个 target 内 `reported_at + pointIndex`”生成 `ts`，导致同一父报文下 8 个 child target 反复复用同一组毫秒时间戳；TDengine 只保留最后一次写入的那组行。修复后：
  - fallback 存储 `ts` 切为进程内全局单调存储时间戳，避免跨 target 覆盖
  - `reported_at` 继续保留真实设备上报时间
  - `/api/telemetry/latest` 查询顺序改为 `reported_at DESC, ts DESC`
- 修复后已用新后端实例复验真实 split 会话 `8aa1ca29bd6345a985eae76e8d8246fe / 3897836665d14d33b3f2f40881787821`：
  - `PROTOCOL_DECODE.childMessageCount=8`
  - `TELEMETRY_PERSIST.summary.persistedTargetCount=8`
  - `TELEMETRY_PERSIST.summary.persistedPointCount=16`
  - `traceId=3897836665d14d33b3f2f40881787821` 在 TDengine fallback 表中可直接查到 `8` 个 child 设备、每个设备 `2` 条
  - `8` 个 child 设备的 `/api/telemetry/latest` 都已追上同一条 split `traceId`

| childDeviceCode | latestTraceId | latestPropertyCount | `traceId=3897836665d14d33b3f2f40881787821` 的 TDengine 计数 |
|---|---|---:|---:|
| `84330701` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330695` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330697` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330699` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330686` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330687` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330691` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |
| `84330696` | `3897836665d14d33b3f2f40881787821` | 2 | 2 |

- 结论：深部位移 split 家族的物理落库不一致已解除，Task 7 Step 1 当前只剩“未在 live 窗口看到的目标家族样本”与 Step 2 的 legacy 命中率问题，不再被 split 落库错误阻塞。

#### Task 7 Step 2 live 窗口命中率

- 按最近 `60` 条 `message-flow/recent` 采样，筛出 `48` 条已完成真实 `$dp` 会话，当前观察到的代表家族命中率如下：

| familyCode | sessionCount | legacyHitSessions | fallbackHitSessions |
|---|---:|---:|---:|
| `S1_ZT_1` | 26 | 0 | 26 |
| `L1_JS_1` | 13 | 0 | 13 |
| `L1_QJ_1` | 13 | 0 | 13 |
| `L1_LF_1` | 8 | 0 | 8 |
| `L1_LF_2` | 5 | 0 | 5 |
| `L1_LF_3` | 5 | 0 | 5 |
| `L3_YL_1` | 2 | 0 | 2 |
| `L1_SW_1` | 2 | 0 | 2 |

- 同一窗口还额外观察到 `L1_LF_4` 至 `L1_LF_9`、`L1_SW_2` 至 `L1_SW_8`，命中情况也全部是 `legacy=0 / fallback>0`。
- 当前窗口未观察到的目标家族包括：`L1_GP_1`、`QN_QB_ZT_1`、`L4_NW_1`、`HY_BSD_ZT_1`、`HY_BSD_ACK_1`。
- `2026-03-24 23:45 CST` 已补做 MySQL + 运行态根因取证，结论已从“现象”收敛到“数据基线缺口”：
  - `south_rtu`（`product_id=202603192100560259`）、`south_multi_displacement`（`product_id=202603192100560251`）、`south_gnss_monitor`（`product_id=202603192100560246`）在 `iot_product_model` 中当前 `property` 行数均为 `0`，因此 `DeviceTelemetryMappingServiceImpl` 读取不到任何 `specs_json.tdengineLegacy`。
  - `south_deep_displacement`（`product_id=202603192100560250`）当前只有 `2` 条 `property` 物模型：`dispsX / dispsY`，且两条 `specs_json` 都只有 `{"unit":"mm"}`，没有 `tdengineLegacy`，因此 split child 的 `dispsX / dispsY` 也只能继续走 fallback。
  - 代表设备近 `24h` 实际写入的标准化属性已远超现有物模型承载范围，例如 `SJ11F2148734232A` 写入了 `27` 个属性，包含 `L1_JS_1.gX / L1_QJ_1.angle / S1_ZT_1.ext_power_volt`；`SK00EA0D1308009` 写入了 `22` 个属性；这些 live 指标当前没有对应的产品物模型映射可供 legacy writer 命中。
  - 运行态日志也与数据库结论一致：`traceId=b4f6ff37529840589eabc3696481e680` 处理 `south_rtu` live 报文时，`ProductModelMapper.selectList` 对 `product_id=202603192100560259` 的查询结果就是 `Total: 0`。
  - 代表设备的 `metadata_json.tdengineLegacy` 目前也全部为空，但 `LegacyTdengineDeviceMetadataResolver` 对 `deviceSn/location/subTable` 有兜底回退；因此当前 `fallbackReason=MISSING_TDENGINE_LEGACY_MAPPING` 的主因仍是产品物模型缺失，而不是设备 metadata 缺失。

| productKey | modelPropertyCount | `tdengineLegacy` count | liveIdentifierCount (24h) | 当前缺口样例 |
|---|---:|---:|---:|---|
| `south_rtu` | 0 | 0 | 35 | `L1_LF_1` 至 `L1_LF_9`、`S1_ZT_1.ext_power_volt`、`S1_ZT_1.sensor_state.L1_LF_1` |
| `south_multi_displacement` | 0 | 0 | 26 | `L1_JS_1.gX`、`L1_QJ_1.angle`、`L1_LF_1.value`、`S1_ZT_1.pa_state` |
| `south_gnss_monitor` | 0 | 0 | 31 | `L1_GP_1.gpsTotalX/Y/Z`、`L1_JS_1.gX`、`L1_QJ_1.angle` |
| `south_deep_displacement` | 2 | 0 | 23 | live 有 `L1_SW_1.dispsX / L1_SW_1.dispsY` 与多项 `S1_ZT_1.*`，但物模型只有裸 `dispsX / dispsY` |

- 结论：Task 7 Step 2 现阶段不能签收，而且当前阻塞已明确为真实环境数据治理问题，不是 `DeviceTelemetryMappingServiceImpl` 的解析 bug。要让 live 家族从 `fallback only` 回到 `legacy-compatible`，下一步必须先补齐 `iot_product_model.specs_json.tdengineLegacy` 与缺失的 property 基线，再复验命中率。

#### Task 7 Step 5 回归验证

- 已于 `2026-03-24 23:10 CST` 与 `2026-03-24 23:24 CST` 新鲜执行以下命令：

```bash
mvn -pl spring-boot-iot-protocol -DskipTests=false -Dtest=MqttJsonProtocolAdapterTest,MqttPayloadSecurityValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-message -DskipTests=false -Dtest=MqttMessageConsumerTest,UpMessageProcessingPipelineTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-telemetry -DskipTests=false -Dtest=TelemetryPersistStageHandlerTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl spring-boot-iot-telemetry -DskipTests=false -Dtest=TelemetryPersistStageHandlerTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest,TdengineTelemetryStorageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：
  - `spring-boot-iot-protocol`：`16` tests, `0` failures, `0` errors
  - `spring-boot-iot-message`：`8` tests, `0` failures, `0` errors
  - `spring-boot-iot-telemetry`：`9` tests, `0` failures, `0` errors
  - `spring-boot-iot-telemetry`（含 `TdengineTelemetryStorageServiceTest` 新回归）：`13` tests, `0` failures, `0` errors
- 说明：Task 7 Step 5 已完成；split 落库覆盖问题已通过“红灯测试 -> 绿灯测试 -> 真实环境会话复验”闭环确认。当前剩余阻塞只在 Step 2 的 legacy 命中率与未出现的目标家族样本，不再是 split 物理落库错误。

- `README.md` 与 `AGENTS.md` 已按规则复核；本轮仅更新计划执行快照，没有新增交付边界、模块职责或启动命令分支，因此仍不需要同步改动这两份文件。
- 当前真实环境阻塞点已经进一步收敛为一类问题：已观测家族的 legacy 命中率仍为 `0`。
- 在 Step 2 的 legacy/fallback 治理目标达成前，Task 7 Step 4 仍不应推进，旧 helper 逻辑也不应删除。
=======
## Task 7 Step 2 映射治理待办清单（codex/dev 合并版）

- Task 7 Step 2 当前先按“缺口台账 + 默认 dry-run 的 SQL 草案”收口，不直接产出面向真实环境的最终升级 SQL。
- 对应草案文件为 `sql/upgrade/20260324_phase5_tdengine_mapping_gap_draft.sql`，当前只固化 `iot_product_model` property 基线和 `tdengineLegacy` 占位结构。
- 当前草案严格遵守以下边界：
  - 默认 `@apply_changes = 0`，只预览范围，不直接改库。
  - `stable/column` 只保留在草案注释和 hint 字段中，不自动写入 `specs_json.tdengineLegacy`。
  - 只有 TDengine REST / SQL 认证恢复并完成 `DESCRIBE <stable>` 复核后，才允许把对应指标从 `PENDING_TDENGINE_VERIFICATION` 升级为正式映射。

| productKey | 当前草案关注点 | 代表性缺口 / 建议 identifier | 当前草案动作 | 当前阻塞 |
|---|---|---|---|---|
| `south_rtu` | 裂缝家族 + 状态类补点 | `L1_LF_1`、`L1_LF_2`、`L1_LF_3`、`S1_ZT_1.ext_power_volt` | 新增代表性 `property seed`，全部使用 `tdengineLegacy.enabled=false` 占位结构 | 还缺 live 全量 identifier 导出，`l1_lf_1 / s1_zt_1` 目标列名未核验 |
| `south_multi_displacement` | 多维位移代表性指标补点 | `L1_JS_1.gX`、`L1_QJ_1.angle`、`L1_LF_1.value`、`S1_ZT_1.pa_state` | 新增代表性 `property seed`，保留 stable/column hint，不回写正式映射 | 倾角 / 加速度 / 状态类列名仍需 TDengine 实库核验 |
| `south_gnss_monitor` | GNSS 位移 + 惯导指标补点 | `L1_GP_1.gpsTotalX`、`L1_GP_1.gpsTotalY`、`L1_GP_1.gpsTotalZ`、`L1_JS_1.gX` | 新增代表性 `property seed`，建议 identifier 先保持 live key | `l1_gp_1` 列命名口径未核验，当前不能直接切回 legacy |
| `south_deep_displacement` | 现有 `dispsX / dispsY` 仅补占位结构，父级状态类单独补点 | 现有 `dispsX / dispsY`，补充 `S1_ZT_1.ext_power_volt` | 对现有 property 只补 `tdengineLegacy` 占位结构；父级状态类以代表性 seed 入草案 | 还需区分 split child 指标与父级状态类的最终 stable 列映射 |

## Task 7 Step 2 下一步实现计划

1. 先执行 `sql/upgrade/20260324_phase5_tdengine_mapping_gap_draft.sql` 的 dry-run 预览，确认四类 `productKey` 在当前真实库里的 property 基线、已有 `tdengineLegacy` 和代表性缺口。
2. 再把 live 窗口中尚未收录的完整 identifier 导出追加到草案，尤其是 `south_rtu` 的剩余裂缝点位和 `south_deep_displacement` 父级 `S1_ZT_1.*` 指标。
3. 待 TDengine REST / SQL 认证恢复后，逐 stable 执行 `DESCRIBE`，把草案中的 stable/column hint 收敛成真实映射，再决定哪些指标允许切换到 `enabled=true`。
4. 在映射核验完成前，继续把 `NORMALIZED_FALLBACK_ONLY` 视为治理信号，而不是默认成功态；草案 SQL 也不得直接作为真实环境最终升级脚本执行。
5. 映射补齐后，重新执行 Task 7 的 live 验收，至少对 `south_rtu / south_multi_displacement / south_gnss_monitor / south_deep_displacement` 各补 1 个正样本，复核 `legacyStableCount` 是否恢复。
6. 若后续需要新的隔离验收环境，优先在仓库根目录 `.worktrees/` 下创建 worktree，不再继续复用 `~/.codex/worktrees/2a61/...` 这类临时路径。
>>>>>>> cb867fb (Document TDengine mapping gap draft workflow)

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
