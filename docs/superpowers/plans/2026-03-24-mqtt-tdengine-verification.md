# MQTT TDengine Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify whether MQTT-reported sensor data enters TDengine through the fixed upstream pipeline, and determine whether each sample lands in legacy stable, `iot_device_telemetry_point`, or both.

**Architecture:** Use three evidence layers in order: runtime baseline, fixed-pipeline trace evidence, and TDengine/latest-query evidence. Reuse a real completed MQTT session as the main positive sample, then separately validate failure-path behavior and regression coverage from focused tests.

**Tech Stack:** Spring Boot 4, Java 17, Paho MQTT, TDengine (TAOS-RS), MySQL, Redis, Maven, PowerShell, REST APIs

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

- [ ] **Step 1: Confirm MQTT subscription and telemetry mode in the real-environment config**

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

- [ ] **Step 2: Confirm the local `dev` instance is actually consuming MQTT**

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

- [ ] **Step 3: Record a hard blocker immediately if the consumer is not healthy**

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

- [ ] **Step 1: Login and fetch recent message-flow sessions**

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

- [ ] **Step 2: Pick one completed MQTT session with non-empty `deviceCode` as the primary sample**

Selection rule:
- Prefer the latest `$dp` or standard `/sys/...` completed session
- Prefer a session with `traceId` present
- Prefer `timelineAvailable = true`

- [ ] **Step 3: Fetch the full session timeline for the chosen sample**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/message-flow/session/<SESSION_ID>" -Headers $headers
```

Expected:
- `timeline.status = "COMPLETED"`
- exact ten stages present

- [ ] **Step 4: Verify the stage order exactly matches the fixed pipeline**

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

- [ ] **Step 5: Verify stage-specific evidence for the primary sample**

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

- [ ] **Step 1: Read the `TELEMETRY_PERSIST` step summary from the chosen session**

Expected fields:
- `persistedTargetCount`
- `persistedPointCount`
- `failedTargetCount`
- `legacyStableCount`
- `legacyColumnCount`
- `normalizedFallbackCount`
- `skippedMetricCount`
- `storageMode`

- [ ] **Step 2: Classify the actual landing branch**

Interpretation:
- `LEGACY_COMPATIBLE`: only legacy stable written
- `LEGACY_WITH_NORMALIZED_FALLBACK`: legacy stable + fallback table both written
- `NORMALIZED_FALLBACK_ONLY`: only `iot_device_telemetry_point` written
- `NON_BLOCKING_FAILURE`: telemetry write failed but pipeline continued

- [ ] **Step 3: Apply the stage-7 entry gate rules**

Expected from code:
- skip if `storage-type != tdengine`
- skip if `messageType = reply`
- skip if file payload exists
- skip if `properties` empty
- persist otherwise

- [ ] **Step 4: Mark the sample as valid TDengine evidence only if both conditions hold**

Required:
- `TELEMETRY_PERSIST.status = SUCCESS`
- `persistedPointCount > 0`

## Task 4: Cross-Check with `/api/telemetry/latest`

**Files:**
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/controller/TelemetryController.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Inspect: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java`

- [ ] **Step 1: Resolve `deviceId` for the chosen sample device**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/code/<DEVICE_CODE>" -Headers $headers
```

Expected:
- valid `data.id`

- [ ] **Step 2: Query latest telemetry for the same device**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/telemetry/latest?deviceId=<DEVICE_ID>" -Headers $headers
```

Expected:
- `storageType = "tdengine"`
- `properties` non-empty
- `traceId` equals the sample session trace, or can be explained by legacy trace compensation

- [ ] **Step 3: Compare API output to the message-flow sample**

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

- [ ] **Step 1: For `NORMALIZED_FALLBACK_ONLY`, query `iot_device_telemetry_point` directly**

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

- [ ] **Step 2: For `LEGACY_COMPATIBLE` or mixed branch, derive expected legacy mapping**

Inspect:
- `iot_product_model.specs_json.tdengineLegacy.stable`
- `iot_product_model.specs_json.tdengineLegacy.column`
- `iot_device.metadata_json.tdengineLegacy.deviceSn`
- `iot_device.metadata_json.tdengineLegacy.location`
- `iot_device.metadata_json.tdengineLegacy.subTables`

Expected:
- stable name and target columns are derivable

- [ ] **Step 3: Query the expected legacy stable when mapping exists**

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

- [ ] **Step 4: Record the final landing mode for the chosen sample**

Allowed outcomes:
- normalized fallback only
- legacy only
- legacy + normalized fallback

## Task 6: Validate Failure-Path Semantics

**Files:**
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Inspect: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceContractStageHandler.java`
- Inspect: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/mqtt/MqttTopicRouter.java`

- [ ] **Step 1: Pick one recent failed MQTT session from `message-flow/recent`**

Selection rule:
- prefer a failed session with `transportMode = "MQTT"`

- [ ] **Step 2: Fetch its session timeline**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:9999/api/device/message-flow/session/<FAILED_SESSION_ID>" -Headers $headers
```

Expected:
- one stage is `FAILED`

- [ ] **Step 3: Classify the failure stage**

Interpretation:
- `TOPIC_ROUTE`: unsupported topic or route parse failure
- `PROTOCOL_DECODE`: payload decode failure
- `DEVICE_CONTRACT`: device/product/protocol contract failure
- `TELEMETRY_PERSIST`: non-blocking TDengine failure if session still completed

- [ ] **Step 4: Verify non-blocking semantics only when failure is in stage 7**

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

- [ ] **Step 1: Run the focused protocol + pipeline + telemetry test suite**

Run:

```powershell
mvn --% -s .mvn/settings.xml -pl spring-boot-iot-protocol,spring-boot-iot-message,spring-boot-iot-device,spring-boot-iot-telemetry -am test -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MqttTopicParserTest,MqttJsonProtocolAdapterTest,MqttMessageConsumerTest,UpMessageProcessingPipelineTest,TelemetryPersistStageHandlerTest,TdengineTelemetryStorageServiceTest,LegacyTdengineTelemetryWriterTest,TdengineTelemetryFacadeTest
```

Expected:
- build success
- zero failures
- zero errors

- [ ] **Step 2: Map each passing test to one validation conclusion**

Required mapping:
- topic parsing
- payload decode
- fixed stage order
- telemetry stage gating
- normalized fallback write
- legacy stable write
- facade merge behavior

## Task 8: Produce the Final Verification Record

**Files:**
- Output: `docs/superpowers/plans/2026-03-24-mqtt-tdengine-verification.md`

- [ ] **Step 1: Write a one-page result matrix**

Required columns:
- stage
- class/method
- evidence source
- sample trace/session
- observed result
- pass/fail/blocker

- [ ] **Step 2: State the final answer in one sentence**

Required format:
- `MQTT sensor data entered TDengine: YES/NO`
- `landing path: legacy / fallback / mixed / blocked`

- [ ] **Step 3: Record unresolved items explicitly**

Examples:
- no legacy-mapped sample observed in live environment
- TDengine SQL console unavailable
- runtime health unstable
- only fallback table verified, legacy stable not verified

- [ ] **Step 4: If blocked, record the blocker exactly and stop**

Required:
- include exact API, stage, or datasource blocker
- do not replace with H2-based conclusions
