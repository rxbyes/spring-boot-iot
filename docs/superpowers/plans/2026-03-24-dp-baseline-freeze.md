# `$dp` 真实环境基线冻结记录

> 记录时间：2026-03-24 12:04-12:10（Asia/Shanghai）  
> 执行工作区：`E:\idea\ghatg\spring-boot-iot\.worktrees\dp-standardization-refactor`  
> 目标：冻结 `$dp` 当前真实运行基线，为后续协议标准化与 TDengine 映射治理重构提供对照样本。

## 1. 运行基线

### 1.1 MQTT consumer 健康状态

`GET /actuator/health/mqttConsumer` 返回：

- `status = UP`
- `running = true`
- `consumerActive = true`
- `connected = true`
- `leadershipMode = LEADER`
- `subscribeTopics` 包含：
  - `$dp`
  - `/sys/+/+/thing/property/post`
  - `/sys/+/+/thing/event/post`
  - `/sys/+/+/thing/property/reply`
  - `/sys/+/+/thing/service/reply`
  - `/sys/+/+/thing/status/post`

结论：
- 当前本地 `dev` 后端正在真实消费 `$dp`
- 当前真实环境允许直接基于运行中的 consumer 做基线冻结，不存在“本地未接入、只能靠模拟”的阻塞

## 2. 抽样策略

从 `GET /api/device/message-flow/recent?size=50` 中抽取 6 个代表样本：

- 4 个成功样本
- 1 个带设备码失败样本
- 1 个无设备码失败样本

优先覆盖：

- 不同设备编码形态
- 不同产品
- `status` / `property` 两类消息
- `DEVICE_CONTRACT` 与 `PROTOCOL_DECODE` 两类失败

## 3. 样本台账

| 样本 | sessionId | traceId | deviceCode | productKey | status | failureStage | appId | messageType | dataFormatType | familyCodes | telemetryBranch | persistedPointCount | legacyStableCount | normalizedFallbackCount |
|---|---|---|---|---|---|---|---|---|---|---|---|---:|---:|---:|
| S1 | `cedec2c1073c4900970f3d750d873afa` | `2ebdbe5d774d49a484fe6b4b7f82fd5e` | `SK00EB0D1308289` | `south_deep_displacement` | `COMPLETED` |  | `62000001` | `status` | `STANDARD_TYPE_2` | `S1_ZT_1` | `NORMALIZED_FALLBACK_ONLY` | 17 | 0 | 17 |
| S2 | `9a5cf5683d3c4215b60d0b5b09ee837d` | `5df813927f0041e2b942a207b7247878` | `SJ11F2148734249A` | `south_gnss_monitor` | `COMPLETED` |  | `62000001` | `property` | `STANDARD_TYPE_2` | 待补核 | `NORMALIZED_FALLBACK_ONLY` | 8 | 0 | 8 |
| S3 | `9cb5af58cfc84a6d9fc544077967274c` | `f410a58d25dc438b927b3026726d2071` | `SK00E90D1307894` | `south_rain_gauge` | `COMPLETED` |  | `62000001` | `property` | `STANDARD_TYPE_2` | `L3_YL_1`、`S1_ZT_1` | `NORMALIZED_FALLBACK_ONLY` | 2 | 0 | 2 |
| S4 | `cc4448565d3740049a900e5a62f7e453` | `7581883623654824accdf973f95d9c18` | `SK00EA0D1308009` | `south_rtu` | `COMPLETED` |  | `62000001` | `property` | `STANDARD_TYPE_2` | `L1_LF_1`、`L1_LF_2`、`L1_LF_3`、`S1_ZT_1` | `NORMALIZED_FALLBACK_ONLY` | 3 | 0 | 3 |
| F1 | `ec8f96a838724e319aa719cb34a7b5b4` | `700a2a0e5c384c17b4475e6c465b19ab` | `100054920` |  | `FAILED` | `DEVICE_CONTRACT` |  | `property` | `STANDARD_TYPE_2` |  |  |  |  |  |
| F2 | `47d04dcab1a7413bb5684e88bc9fb019` | `392c7520abbd45a082b44ff601b84711` |  |  | `FAILED` | `PROTOCOL_DECODE` |  |  |  |  |  |  |  |  |

## 4. 关键证据

### 4.1 成功样本的共同特征

- 成功样本全部来自 `$dp`
- `PROTOCOL_DECODE.summary.dataFormatType` 均为 `STANDARD_TYPE_2`
- 当前抽样到的 4 个成功样本全部走：
  - `TELEMETRY_PERSIST.branch = NORMALIZED_FALLBACK_ONLY`
  - `legacyStableCount = 0`
- 这说明当前真实 `$dp` 主链路“能写入 TDengine”，但当前抽样家族没有命中 legacy stable，全部落到了 `iot_device_telemetry_point`

### 4.2 TDengine trace 查询结果

针对成功样本的 traceId 直接查询 `iot_device_telemetry_point`，确认：

- `2ebdbe5d774d49a484fe6b4b7f82fd5e`
  - 命中 `S1_ZT_1.*` 系列 17 个指标
- `f410a58d25dc438b927b3026726d2071`
  - 命中 `L3_YL_1.totalValue`
  - 命中 `L3_YL_1.value`
- `7581883623654824accdf973f95d9c18`
  - 命中 `L1_LF_1`
  - 命中 `L1_LF_2`
  - 命中 `L1_LF_3`

结论：
- 当前 `$dp` 的成功样本里，至少 `S1_ZT_1`、`L3_YL_1`、`L1_LF_*` 已真实写入 TDengine fallback 表
- 当前已验证“写入存在”，但同时也确认“legacy stable 未命中”

### 4.3 失败样本的首轮定位

`F1`：

- `failureStage = DEVICE_CONTRACT`
- `errorClass = BizException`
- `errorMessage = 设备不存在: 100054920`

结论：
- 当前存在真实 `$dp` 设备仍在上报，但平台内无对应设备建档的情况
- 这是协议层之外的契约治理问题，后续不能误判成解码器问题

`F2`：

- `failureStage = PROTOCOL_DECODE`
- `errorClass = BizException`
- `errorMessage = mqtt-json-decrypted MQTT 负载不能为空`

结论：
- 当前存在“解密后有效载荷为空”或外层帧不完整的 `$dp` 样本
- 这类问题正好对应后续 `LegacyDpEnvelopeDecoder` 拆分的优先治理点

## 5. 当前高风险项

### 5.1 fallback 默认化风险

当前抽样到的成功 `$dp` 会话全部是 `NORMALIZED_FALLBACK_ONLY`，说明：

- 现网 `$dp` 家族虽然能落库
- 但 legacy stable 映射没有被主路径命中
- 如果继续沿用现在的隐式映射治理，后续新增厂商只会继续把更多指标推向 fallback 表

### 5.2 家族命名漂移风险

`south_rtu` 样本里出现：

- `L1_LF_1`
- `L1_LF_2`
- `L1_LF_3`

说明当前同一设备体内可能同时存在“多逻辑测点变体”，这会让：

- family resolver
- metric mapping
- 报表与规则消费

都面临“家族码与逻辑测点码混用”的风险。

### 5.3 GNSS 样本 trace 对账异常

样本 `S2`：

- `message-flow` 中 `TELEMETRY_PERSIST.summary.persistedPointCount = 8`
- `branch = NORMALIZED_FALLBACK_ONLY`
- 但按 traceId 直接查 `iot_device_telemetry_point` 未命中记录
- `/api/telemetry/latest` 当前 trace 也未与该样本 trace 对齐

结论：
- 当前至少存在 1 类样本，时间线摘要与 trace 级时序查询之间存在不一致
- 这需要在 Task 2/Task 5 中额外补“trace 级写入一致性”观测，不宜直接假设所有 `persistedPointCount > 0` 都能从 trace 查询到

### 5.4 契约缺失与解码失败并存

当前失败样本至少分成两类：

- 设备未建档：`DEVICE_CONTRACT`
- 解密后载荷为空：`PROTOCOL_DECODE`

结论：
- 后续重构必须把“契约失败”和“协议失败”严格分开观察
- 不能因为 `$dp` 失败量大，就统一把问题归到 `MqttJsonProtocolAdapter`

## 6. 当前结论

1. 当前 `$dp` 真实消费正常，consumer 健康，无环境阻塞。
2. 当前 `$dp` 成功样本能够进入固定 Pipeline 并写入 TDengine。
3. 当前抽样到的成功家族全部走 fallback，而不是 legacy stable。
4. 当前 `$dp` 失败同时包含契约缺失和协议解码问题，不能混为一类。
5. 当前已经具备进入 Task 2 的条件：优先补协议元数据和家族级可观测，不先改行为。

## 7. 下一步

建议直接进入 Task 2：

- 给 `DeviceUpMessage` 增加协议元数据挂载点
- 在 `PROTOCOL_DECODE.summary` 中补 `appId / familyCodes / normalizationStrategy / timestampSource`
- 先增强 `message-flow` 可观测，不改 `$dp` 行为

