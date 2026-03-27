# MQTT TDengine Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify whether MQTT-reported sensor data enters TDengine through the fixed upstream pipeline, and determine whether each sample lands in legacy stable, `iot_device_telemetry_point`, or both.

**Architecture:** Use three evidence layers in order: runtime baseline, fixed-pipeline trace evidence, and TDengine/latest-query evidence. Reuse a real completed MQTT session as the main positive sample, then separately validate failure-path behavior and regression coverage from focused tests.

**Tech Stack:** Spring Boot 4, Java 17, Paho MQTT, TDengine (TAOS-RS), MySQL, Redis, Maven, PowerShell, REST APIs

**2026-03-25 回填说明：** Task 1~6 的勾选根据本页 Task 8 结果矩阵中已记录的 live 证据回填；本会话又 fresh 重跑了 protocol / message / telemetry 定向回归，用于确认当前仓库仍能复现历史绿灯的测试基线。

---

## File Map

- `spring-boot-iot-admin/src/main/resources/application-dev.yml`
  Responsibility: real-environment MQTT subscription baseline, TDengine datasource, telemetry storage mode.
- `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
  Responsibility: effective defaults for MQTT, protocol, telemetry, tenant, and sub-device mapping.
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
  Responsibility: MQTT entrypoint, lifecycle, health, and handoff into pipeline.
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttTopicRouter.java`
  Responsibility: convert MQTT topic + payload into `RawDeviceMessage`.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttTopicParser.java`
  Responsibility: parse `/sys/...`, sub-device topic, and `$dp` route type.
- `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapter.java`
  Responsibility: decode MQTT payload into `DeviceUpMessage`, flatten properties, split child messages.
- `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
  Responsibility: enforce `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE`.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceContractStageHandler.java`
  Responsibility: device/product/protocol contract validation.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceMessageLogStageHandler.java`
  Responsibility: MySQL message log persistence before telemetry persistence.
- `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java`
  Responsibility: latest-property update in MySQL and reply/file branching.
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java`
  Responsibility: telemetry persistence gatekeeper for stage 7.
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`
  Responsibility: choose legacy-compatible vs normalized fallback persistence and latest query merge.
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriter.java`
  Responsibility: legacy stable write path.
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryStorageService.java`
  Responsibility: normalized fallback write path into `iot_device_telemetry_point`.
- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
  Responsibility: `/api/telemetry/latest` query response and `storageType=tdengine` evidence.
- `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`
  Responsibility: fixed stage order, non-blocking telemetry failure, MQTT correlation coverage.
- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandlerTest.java`
  Responsibility: stage 7 skip/delegate behavior.
- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryStorageServiceTest.java`
  Responsibility: normalized fallback row persistence behavior.
- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriterTest.java`
  Responsibility: legacy stable write behavior.
- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacadeTest.java`
  Responsibility: legacy + fallback merge behavior and trace compensation.

## Task 1: Freeze Real-Environment Baseline

**Files:**
- Inspect: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Inspect: `spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java`
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`

- [x] **Step 1: Confirm MQTT subscription and telemetry mode in the real-environment config**

Run:

```powershell
Get-Content 'spring-boot-iot-admin/src/main/resources/application-dev.yml' -Encoding UTF8 |
  Select-String 'broker-url|default-subscribe-topics|storage-type|tdengine-mode|legacy-normalized-fallback-enabled|slave_1'
```

Expected:
- MQTT subscribes to `$dp` and `/sys/+/+/thing/...`
- telemetry uses `storage-type: tdengine`
- telemetry uses `tdengine-mode: legacy-compatible`
- TDengine datasource is `slave_1`

- [x] **Step 2: Confirm the local `dev` instance is actually consuming MQTT**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:9999/actuator/health/mqttConsumer'
```

Expected:
- `status = "UP"`
- `details.running = true`
- `details.consumerActive = true`
- `details.connected = true`
- `details.subscribeTopics` includes `$dp`

- [x] **Step 3: Record a hard blocker immediately if the consumer is not healthy**

Stop criteria:
- `consumerActive = false`
- `connected = false`
- `$dp` missing from `subscribeTopics`
- runtime not reachable on `127.0.0.1:9999`

Required note:
- Mark as environment blocker
- Do not fall back to H2 or synthetic-only conclusions

## Task 2: Capture One Positive MQTT Sample

**Files:**
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumer.java`
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Inspect: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/MqttTopicParser.java`

- [x] **Step 1: Login and fetch recent message-flow sessions**

Run:

```powershell
$body = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9999/api/auth/login' -ContentType 'application/json' -Body $body
$token = $login.data.token
$headers = @{ Authorization = 'Bearer ' + $token }
Invoke-RestMethod -Method Get -Uri 'http://127.0.0.1:9999/api/device/message-flow/recent?size=10' -Headers $headers
```

Expected:
- At least one `transportMode = "MQTT"` and `status = "COMPLETED"` session

- [x] **Step 2: Pick one completed MQTT session with non-empty `deviceCode` as the primary sample**

Selection rule:
- Prefer the latest `$dp` or standard `/sys/...` completed session
- Prefer a session with `traceId` present
- Prefer `timelineAvailable = true`

- [x] **Step 3: Fetch the full session timeline for the chosen sample**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/message-flow/session/<SESSION_ID>" -Headers $headers
```

Expected:
- `timeline.status = "COMPLETED"`
- exact ten stages present

- [x] **Step 4: Verify the stage order exactly matches the fixed pipeline**

Expected stage order:

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

- [x] **Step 5: Verify stage-specific evidence for the primary sample**

Check:
- `TOPIC_ROUTE.summary.routeType`
- `PROTOCOL_DECODE.summary.messageType`
- `PROTOCOL_DECODE.summary.dataFormatType`
- `PAYLOAD_APPLY.summary.propertyCount > 0`
- `TELEMETRY_PERSIST.summary.persistedPointCount > 0`
- `DEVICE_STATE.summary.refreshedTargetCount > 0`

## Task 3: Prove Stage 7 Reaches TDengine

**Files:**
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriter.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryStorageService.java`

- [x] **Step 1: Read the `TELEMETRY_PERSIST` step summary from the chosen session**

Expected fields:
- `persistedTargetCount`
- `persistedPointCount`
- `failedTargetCount`
- `legacyStableCount`
- `legacyColumnCount`
- `normalizedFallbackCount`
- `skippedMetricCount`
- `storageMode`

- [x] **Step 2: Classify the actual landing branch**

Interpretation:
- `LEGACY_COMPATIBLE`: only legacy stable written
- `LEGACY_WITH_NORMALIZED_FALLBACK`: legacy stable + fallback table both written
- `NORMALIZED_FALLBACK_ONLY`: only `iot_device_telemetry_point` written
- `NON_BLOCKING_FAILURE`: telemetry write failed but pipeline continued

- [x] **Step 3: Apply the stage-7 entry gate rules**

Expected from code:
- skip if `storage-type != tdengine`
- skip if `messageType = reply`
- skip if file payload exists
- skip if `properties` empty
- persist otherwise

- [x] **Step 4: Mark the sample as valid TDengine evidence only if both conditions hold**

Required:
- `TELEMETRY_PERSIST.status = SUCCESS`
- `persistedPointCount > 0`

## Task 4: Cross-Check with `/api/telemetry/latest`

**Files:**
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/controller/TelemetryController.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`

- [x] **Step 1: Resolve `deviceId` for the chosen sample device**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/code/<DEVICE_CODE>" -Headers $headers
```

Expected:
- valid `data.id`

- [x] **Step 2: Query latest telemetry for the same device**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/telemetry/latest?deviceId=<DEVICE_ID>" -Headers $headers
```

Expected:
- `storageType = "tdengine"`
- `properties` non-empty
- `traceId` equals the sample session trace, or can be explained by legacy trace compensation

- [x] **Step 3: Compare API output to the message-flow sample**

Check:
- same `deviceCode`
- same `productKey`
- `reportTime` is consistent with the decoded payload timestamp
- returned properties are consistent with the sample branch and property count

## Task 5: Verify the Physical TDengine Landing Table

**Files:**
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DevicePropertyMetadataServiceImpl.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineDeviceMetadataResolver.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineSchemaInspector.java`

- [x] **Step 1: For `NORMALIZED_FALLBACK_ONLY`, query `iot_device_telemetry_point` directly**

Run in TDengine SQL console:

```sql
SELECT ts, reported_at, device_code, metric_code, trace_id, value_text
FROM iot_device_telemetry_point
WHERE device_code = '<DEVICE_CODE>'
  AND trace_id = '<TRACE_ID>'
ORDER BY ts DESC;
```

Expected:
- at least one row
- same `trace_id` as the chosen session
- one row per persisted metric

- [x] **Step 2: For `LEGACY_COMPATIBLE` or mixed branch, derive expected legacy mapping**

Inspect:
- `iot_product_model.specs_json.tdengineLegacy.stable`
- `iot_product_model.specs_json.tdengineLegacy.column`
- `iot_device.metadata_json.tdengineLegacy.deviceSn`
- `iot_device.metadata_json.tdengineLegacy.location`
- `iot_device.metadata_json.tdengineLegacy.subTables`

Expected:
- stable name and target columns are derivable

- [x] **Step 3: Query the expected legacy stable when mapping exists**

Run in TDengine SQL console:

```sql
SELECT ts, rd, *
FROM <LEGACY_STABLE_OR_SUBTABLE>
WHERE device_sn = '<DEVICE_SN>'
ORDER BY ts DESC
LIMIT 5;
```

Expected:
- latest row timestamp aligns with the sample report time
- mapped columns contain the reported metric values

- [x] **Step 4: Record the final landing mode for the chosen sample**

Allowed outcomes:
- normalized fallback only
- legacy only
- legacy + normalized fallback

## Task 6: Validate Failure-Path Semantics

**Files:**
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceContractStageHandler.java`
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttTopicRouter.java`

- [x] **Step 1: Pick one recent failed MQTT session from `message-flow/recent`**

Selection rule:
- prefer a failed session with `transportMode = "MQTT"`

- [x] **Step 2: Fetch its session timeline**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/message-flow/session/<FAILED_SESSION_ID>" -Headers $headers
```

Expected:
- one stage is `FAILED`

- [x] **Step 3: Classify the failure stage**

Interpretation:
- `TOPIC_ROUTE`: unsupported topic or route parse failure
- `PROTOCOL_DECODE`: payload decode failure
- `DEVICE_CONTRACT`: device/product/protocol contract failure
- `TELEMETRY_PERSIST`: non-blocking TDengine failure if session still completed

- [x] **Step 4: Verify non-blocking semantics only when failure is in stage 7**

Expected:
- `timeline.status = COMPLETED`
- `TELEMETRY_PERSIST.status = FAILED`
- `TELEMETRY_PERSIST.branch = NON_BLOCKING_FAILURE`
- `DEVICE_STATE` and `RISK_DISPATCH` still execute afterward

## Task 7: Re-run Focused Regression Tests

**Files:**
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttTopicParserTest.java`
- Test: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/MqttJsonProtocolAdapterTest.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/mqtt/MqttMessageConsumerTest.java`
- Test: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandlerTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryStorageServiceTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriterTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacadeTest.java`

- [x] **Step 1: Run the focused protocol + pipeline + telemetry test suite**

Run:

```powershell
mvn --% -s .mvn/settings.xml -pl spring-boot-iot-protocol,spring-boot-iot-message,spring-boot-iot-device,spring-boot-iot-telemetry -am test -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MqttTopicParserTest,MqttJsonProtocolAdapterTest,MqttMessageConsumerTest,UpMessageProcessingPipelineTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest
```

Expected:
- build success
- zero failures
- zero errors

- [x] **Step 2: Map each passing test to one validation conclusion**

Required mapping:
- topic parsing
- payload decode
- fixed stage order
- telemetry stage gating
- normalized fallback write
- legacy stable write
- facade merge behavior

2026-03-25 fresh rerun note:
- `MqttTopicParserTest`、`MqttJsonProtocolAdapterTest`、`LegacyDpEnvelopeDecoderTest`、`LegacyDpPropertyNormalizerTest`、`LegacyDpChildMessageSplitterTest`、`MqttPayloadSecurityValidatorTest`、`MqttPayloadDecryptorRegistryTest` 共 27 个协议相关测试重新执行通过，覆盖 topic parsing、payload decode、legacy/compat 策略与子设备拆分。
- `UpMessageProcessingPipelineTest`、`MqttMessageConsumerTest`、`MqttReportPublishServiceImplTest` 共 16 个 message 相关测试重新执行通过，覆盖固定阶段顺序、MQTT consumer 入口和 stage 7 non-blocking 语义。
- `TelemetryPersistStageHandlerTest`、`LegacyTdengineTelemetryWriterTest`、`TdengineTelemetryFacadeTest` 共 9 个 telemetry 相关测试重新执行通过，覆盖 telemetry stage gating、normalized fallback write、legacy stable write 与 facade merge behavior。

## Task 8: Produce the Final Verification Record

**Files:**
- Output: `docs/superpowers/plans/2026-03-24-mqtt-tdengine-verification.md`

- [x] **Step 1: Write a one-page result matrix**

Required columns:
- stage
- class/method
- evidence source
- sample trace/session
- observed result
- pass/fail/blocker

### Verification Matrix

Verified on `2026-03-24`, with `2026-03-27` live supplemental retest, using `application-dev.yml`, live `127.0.0.1:9999` runtime, live MQTT `message-flow`, TDengine REST SQL, and focused regression tests.

| stage | class/method | evidence source | sample trace/session | observed result | pass/fail/blocker |
|---|---|---|---|---|---|
| runtime baseline | `MqttMessageConsumer` / `/actuator/health/mqttConsumer` | live health API + config inspection | `2026-03-24 20:13` startup window | `status=UP`，`consumerActive=true`，`connected=true`，订阅包含 `$dp`；`application-dev.yml` 为 `storage-type=tdengine`、`tdengine-mode=legacy-compatible` | PASS |
| fixed pipeline | `UpMessageProcessingPipeline#process` | live `GET /api/device/message-flow/session/{sessionId}` | `traceId=d4137f67b9e649daab586ffa7563ec90` / `sessionId=0c0f2284adb2430bb595d12b9a3940ab` | 阶段顺序为 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE` | PASS |
| protocol decode evidence | `MqttJsonProtocolAdapter#decode` | same live session timeline | same as above | `routeType=legacy`，`messageType=status`，`dataFormatType=STANDARD_TYPE_2`，`childMessageCount=0` | PASS |
| single-device deep displacement alias retest | `LegacyDpChildMessageSplitter` + `MqttJsonProtocolAdapter#decode` | live `POST /api/message/mqtt/report/publish` + `GET /api/device/message-flow/trace/{traceId}` + `GET /api/device/{deviceCode}/properties` + `GET /api/telemetry/latest` | `traceId=c15a78d53f0443c0a47ad20154ab8a3b` / `sessionId=bb342847b1b14fefa8c2d238f4c68bdc` | `$dp` type-2 明文帧成功闭环，`PROTOCOL_DECODE.summary.familyCodes=["L1_SW_1"]`、`normalizationStrategy=LEGACY_DP_COMPAT`、`childSplitApplied=false`；`iot_device_property` 与 latest telemetry 新值均写入 `dispsX=-0.0445`、`dispsY=0.0293`，历史 `L1_SW_1.*` 未被继续刷新 | PASS |
| stage-7 entry gate | `TelemetryPersistStageHandler#persist` | code inspection + focused tests | `TelemetryPersistStageHandlerTest` | 代码明确对 `storage-type != tdengine`、`reply`、文件载荷、空属性做跳过；其他属性消息继续持久化 | PASS |
| TDengine landing branch | `TdengineTelemetryFacade#persist` | same live session timeline | same as above | `TELEMETRY_PERSIST.status=SUCCESS`，`branch=NORMALIZED_FALLBACK_ONLY`，`persistedPointCount=17`，`legacyStableCount=0`，`normalizedFallbackCount=17`，`storageMode=legacy-compatible` | PASS |
| latest query cross-check | `TelemetryQueryServiceImpl#getLatest` | live `GET /api/device/code/{deviceCode}` + `GET /api/telemetry/latest` | `deviceCode=SK00EB0D1308289` / `deviceId=1925443622023450625` | `storageType=tdengine`，`reportTime=2026-03-24T20:14:37`，`traceId=d4137f67b9e649daab586ffa7563ec90`，属性集非空且与 session 样本一致 | PASS |
| physical TDengine table | `TdengineTelemetryStorageService` | TDengine REST SQL (`http://8.130.107.120:6041/rest/sql`) | same trace/device | `iot_device_telemetry_point` 中可查到 `device_id=1925443622023450625`、`trace_id=d4137f67b9e649daab586ffa7563ec90` 的多条指标行 | PASS |
| failure-path classification | `MqttJsonProtocolAdapter#decode` | live failed session timeline | `traceId=5d34666269af4273867dfc7b8cfdbf43` / `sessionId=d270bed4eca647d3a2f454c9040211b3` | 最近失败样本停在 `PROTOCOL_DECODE`，错误为 `mqtt-json-decrypted MQTT 负载不能为空`，不是 stage 7 失败 | PASS |
| non-blocking stage-7 semantics | `UpMessageProcessingPipeline#telemetryPersist` | focused regression tests | `UpMessageProcessingPipelineTest` + `TelemetryPersistStageHandlerTest` + `TdengineTelemetryFacadeTest` | 测试证明 stage 7 发生 TDengine 异常时，时间线可记为 `FAILED/NON_BLOCKING_FAILURE`，但 session 继续 `COMPLETED` | PASS |
| regression coverage | protocol/message/telemetry focused suite | fresh Maven run on `2026-03-24` | `MqttTopicParserTest,MqttJsonProtocolAdapterTest,MqttMessageConsumerTest,UpMessageProcessingPipelineTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest` | 命令退出码 `0`，无失败 | PASS |

- [x] **Step 2: State the final answer in one sentence**

Required format:
- `MQTT sensor data entered TDengine: YES/NO`
- `landing path: legacy / fallback / mixed / blocked`

Result:

`MQTT sensor data entered TDengine: YES; landing path: fallback`

- [x] **Step 3: Record unresolved items explicitly**

Examples:
- no legacy-mapped sample observed in live environment
- TDengine SQL console unavailable
- runtime health unstable
- only fallback table verified, legacy stable not verified

Unresolved items:

- 在 `2026-03-24 20:13-20:18` 这轮 live window 内，没有观察到 `LEGACY_COMPATIBLE` 或 `LEGACY_WITH_NORMALIZED_FALLBACK` 的正样本；本轮主正样本为 `NORMALIZED_FALLBACK_ONLY`。
- `2026-03-24 23:45 CST` 已追加 MySQL 基线核验，确认当前 live 家族迟迟没有 legacy 命中并不是 reader/writer 代码漏读，而是源数据本身缺口：
  - `south_rtu`、`south_multi_displacement`、`south_gnss_monitor` 这三类 live 产品在 `iot_product_model` 中当前 `property` 行数都是 `0`。
  - `south_deep_displacement` 当前只有 `dispsX / dispsY` 两条 `property`，且 `specs_json` 没有 `tdengineLegacy`。
  - 代表设备近 `24h` 已真实写入 `17-27` 个标准化属性，但当前产品物模型无法提供对应的 legacy stable/column 映射。
- 本机 `taos` CLI 在直接查询时崩溃为 `crash signal is 11`；物理表核验改为 TDengine REST SQL，未使用 H2 或伪造链路替代。
- `2026-03-25` 已补做 TDengine REST `DESCRIBE` 复核，确认 `s1_zt_1 / l1_lf_1 / l1_js_1 / l1_qj_1 / l1_gp_1 / l1_sw_1` 可直接查询；后续阻塞已从“SQL 控制台不可用”收敛为 `iot_product_model.specs_json.tdengineLegacy` 尚未按这些真实列名完成映射。
- `2026-03-27` 已补做单设备深部位移 live 复验：`traceId=c15a78d53f0443c0a47ad20154ab8a3b` 证明协议 alias 修复已真正进入主链路；当前剩余问题不再是“是否继续生成新的 `L1_SW_1.*` 前缀写入”，而是历史残留数据与 `tdengineLegacy` canonical mapping 治理。
- 最近 live failed MQTT session 没有出现 `TELEMETRY_PERSIST = NON_BLOCKING_FAILURE` 样本；stage 7 非阻塞语义由聚焦单测证明，而非 live failure session 证明。
- `2026-03-27` 当前 worktree 对应实例的 live `message-flow` 已可直接看到 `familyCodes / normalizationStrategy / timestampSource / childSplitApplied`；但这轮 plaintext type-2 样本没有 `appId`，因此“加密 `$dp` live 样本中的 `appId` 展示”仍需单独补一个带 `header.appId` 的真实回放或专用模拟器样本。

- [x] **Step 4: If blocked, record the blocker exactly and stop**

Required:
- include exact API, stage, or datasource blocker
- do not replace with H2-based conclusions

Blocker status:

- 本轮主结论未被阻塞；初始阻塞仅为本地 `http://127.0.0.1:9999` 未启动，随后已按 `application-dev.yml` 拉起 `dev` 实例并解除。
- 若后续要把本页结论从 `landing path: fallback` 推进到 `legacy` 或 `mixed`，当前精确阻塞点已经明确为真实环境数据源：`iot_product_model.specs_json.tdengineLegacy` 与相关 `property` 基线尚未覆盖 live 家族；`DESCRIBE` 已证明目标 stable/column 可查，下一步应直接治理 canonical mapping，而不是继续等待 TDengine 认证恢复。
- 本轮没有使用 H2、旧 H2 profile、独立 H2 schema 脚本或 synthetic-only 结论替代真实环境验证。
