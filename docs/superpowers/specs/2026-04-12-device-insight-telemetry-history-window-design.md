# Device Insight Telemetry History Window Design

**Date:** 2026-04-12  
**Status:** Draft for review  
**Scope:** `spring-boot-iot-telemetry` 历史趋势查询窗口修复，覆盖对象洞察台 TDengine 主读链与回退链

## 1. 背景

对象洞察台 `/insight` 的趋势区通过 `POST /api/telemetry/history/batch` 读取 TDengine 历史数据。当前真实环境里，设备属性快照存在真实值，但趋势图大量展示为 `0`，与设备最新属性和 TDengine 时序存量不符。

现状链路如下：

1. 前端 `DeviceInsightView.vue` 只负责组织请求和消费 `points[].buckets[]`。
2. 后端 `TelemetryQueryServiceImpl` 负责：
   - 解析 `rangeCode`
   - 读取 v2 raw / normalized fallback / legacy fallback 历史点
   - 按时间桶聚合
   - 对缺口执行 `ZERO` 补零
3. dev / prod 默认读路由为：
   - `iot.telemetry.read-routing.history-source=v2`
   - `iot.telemetry.read-routing.legacy-read-fallback-enabled=true`

排查结果表明，当前问题不在前端，而在后端历史读取阶段缺少统一的“查询时间窗口”约束。

## 2. 根因

### 2.1 直接根因

`TelemetryRawHistoryReader` 当前执行的 TDengine SQL 仅按：

- `metric_id IN (...)`
- `ORDER BY ts ASC`
- `LIMIT 10000`

读取历史点，但**没有按当前请求的 `rangeCode` 过滤时间窗口**。

当设备历史数据很多时，查询返回的是最早的一批点，而不是“近一天 / 近一周 / 近一月 / 近一年”窗口内的点。后续 `TelemetryQueryServiceImpl` 再按最近窗口构建桶时，这些旧点全部落不到有效桶内，最终被 `ZERO` 补零逻辑覆盖为整段 `0` 曲线。

### 2.2 同类风险

当前不仅 v2 raw 主链缺少统一窗口过滤，两个回退读取器也存在同类口径风险：

1. `NormalizedTelemetryHistoryReader`
2. `LegacyTelemetryHistoryReader`

如果只修主链而不修回退链，历史查询在 fallback 生效时仍可能出现：

- 趋势全部补零
- 最近窗口数据被旧数据挤掉
- 主链与回退链返回口径不一致

## 3. 目标

本轮修复目标如下：

1. `POST /api/telemetry/history/batch` 必须只读取请求窗口内的历史点。
2. v2 raw、normalized fallback、legacy fallback 三条链路统一按同一时间窗口执行。
3. 服务端继续保留 `ZERO` 补零能力，但仅用于窗口内缺口补齐，不再掩盖错误时间范围读取。
4. 前端对象洞察台接口与展示协议保持不变。
5. 补齐自动化测试，避免后续窗口过滤再次回退为“全量 + LIMIT”模式。

## 4. 非目标

1. 本轮不调整对象洞察台 UI 结构、文案或图表组件。
2. 本轮不引入新的 TDengine 聚合查询模型。
3. 本轮不把 `history/batch` 从补零模式改成稀疏点模式。
4. 本轮不修改 `latest` 查询链路。

## 5. 设计原则

### 5.1 窗口先于补零

正确顺序必须是：

1. 先根据 `rangeCode` 计算窗口起止时间。
2. 各历史读取器只读取窗口内点位。
3. 再对窗口内时间桶进行聚合与补零。

不能继续采用“先读一批历史点，再靠桶过滤”的模式。

### 5.2 查询窗口统一下沉到读取器

窗口逻辑应作为后端历史查询的正式约束，不能只停留在 `TelemetryQueryServiceImpl` 内部的聚桶阶段。否则读取器仍会继续：

- 扫旧数据
- 受 `LIMIT` 干扰
- 在不同读链路之间返回不一致结果

### 5.3 继续沿用现有时间轴语义

历史趋势主时间轴保持现有规则：

- telemetry v2 raw：优先按 `ingested_at`
- 若 `ingested_at` 缺失：回退到 `reported_at`
- normalized / legacy：沿用各自现有有效时间列

本轮只修“窗口过滤缺失”，不改变当前“对象洞察趋势优先按入库时间聚桶”的基线。

## 6. 方案设计

## 6.1 在查询服务内显式生成窗口边界

`TelemetryQueryServiceImpl` 当前已有：

- `RangeDefinition`
- `buildBucketSlots(...)`
- `alignToBucket(...)`

本轮新增统一窗口定义，至少包含：

- `windowStart`
- `windowEnd`
- `bucketUnit`
- `slotCount`

窗口规则与当前桶定义保持一致：

- `1d`：最近 24 个小时桶
- `7d`：最近 7 个天桶
- `30d`：最近 30 个天桶
- `365d`：最近 12 个月桶

读取器必须基于该窗口执行查询，而不是由上层在内存中丢弃旧数据。

## 6.2 v2 raw 历史读取器增加时间过滤

`TelemetryRawHistoryReader` 调整为接收窗口参数，并在 SQL 中加入时间过滤。

过滤原则：

1. 以 `ingested_at` 为主过滤字段。
2. 对缺失 `ingested_at` 的旧点，允许回退到 `ts` 或 `reported_at` 参与窗口判断。
3. 结果继续按 `ts ASC` 排序，便于与现有聚桶逻辑兼容。

目标效果：

- 近一周请求只返回近一周内的 raw 点
- 历史很长的设备不会再被最早 10000 条旧点“占满”

## 6.3 normalized fallback 读取器增加时间过滤

`NormalizedTelemetryHistoryReader` 当前是从 `iot_device_telemetry_point` 读取兼容数据。该读取器也要接收窗口参数，并在 SQL 中按其有效时间列过滤。

过滤原则：

1. 优先使用 `reported_at`
2. 若 `reported_at` 为空，则允许使用 `ts`

这样可以保证：

- normalized fallback 与 raw 主链在相同窗口内返回可比结果
- fallback 不再因为历史过长而把最近窗口全部挤空

## 6.4 legacy fallback 读取器增加时间过滤

`LegacyTelemetryHistoryReader` 当前仍承担 legacy stable 回退职责。该读取器必须按 legacy 可用时间列增加窗口过滤，并与主链保持同一窗口定义。

要求：

1. 不能继续全量扫 legacy 历史后再交给上层丢弃
2. 当前 legacy 读取器实际使用 `rd`，缺失时回退 `ts`；本轮窗口过滤必须与该现状保持一致

## 6.5 保持补零协议不变

`TelemetryHistoryBatchResponse` 结构不变：

- `deviceId`
- `rangeCode`
- `bucket`
- `points[].identifier`
- `points[].displayName`
- `points[].seriesType`
- `points[].buckets[].time/value/filled`

补零逻辑保留：

- 有真实点：`filled=false`
- 无真实点：`value=0`，`filled=true`

前端继续依赖 `filled` 区分真实点和补零点，不需要同步改协议。

## 7. 涉及文件

### 7.1 后端核心实现

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
  - 新增统一窗口定义
  - 把窗口参数传入三条历史读取链

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryRawHistoryReader.java`
  - 增加窗口过滤参数
  - 调整 TDengine raw 查询 SQL

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/NormalizedTelemetryHistoryReader.java`
  - 增加窗口过滤参数
  - 调整兼容表查询 SQL

- `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTelemetryHistoryReader.java`
  - 增加窗口过滤参数
  - 调整 legacy fallback 查询口径

### 7.2 测试

- `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`
  - 补充窗口过滤相关单测

- 如需拆分读取器级测试，再新增：
  - `TelemetryRawHistoryReaderTest`
  - `NormalizedTelemetryHistoryReaderTest`
  - `LegacyTelemetryHistoryReaderTest`

### 7.3 文档

- `docs/03-接口规范与接口清单.md`
  - 明确 `history/batch` 按请求窗口读取，而不是历史全量截断

- `docs/08-变更记录与技术债清单.md`
  - 记录本次修复与根因

- 如接口说明已在 README / AGENTS 中有显式历史口径描述，再同步补一句“按窗口过滤”

## 8. 验证策略

### 8.1 单测

至少覆盖以下场景：

1. **raw 主链窗口过滤**
   - 旧点在窗口外、最近点在窗口内
   - 返回结果必须命中最近点，而不是全 0

2. **normalized fallback 窗口过滤**
   - fallback 生效时也只返回窗口内点

3. **legacy fallback 窗口过滤**
   - legacy 路径也不允许旧点污染最近窗口

4. **补零仍然有效**
   - 窗口内有真实点的桶保留真实值
   - 窗口内无真实点的桶继续 `filled=true`

### 8.2 回归验证

至少执行 telemetry 模块相关测试，重点确认：

1. `history/batch` 未破坏既有入库时间聚桶规则
2. latest 查询不受影响
3. fallback 异常处理语义不变

### 8.3 真实环境抽查

若共享 dev 环境可达，优先抽查一个“属性快照有值、趋势原先全 0”的设备：

1. 读取 `GET /device/{deviceCode}/properties`
2. 调用 `POST /api/telemetry/history/batch`
3. 确认最近窗口存在 `filled=false` 的真实桶，且趋势不再整段为 0

## 9. 风险与兼容性

### 9.1 风险

1. legacy 读取器时间列语义若与 raw 不同，可能需要针对字段来源做最小兼容处理。
2. 某些历史点如果 `ingested_at` 与 `reported_at` 均缺失，仍会被排除在窗口外。
3. 若某设备最近窗口内确实无数据，修复后仍会看到补零曲线；这属于真实结果，不是故障。

### 9.2 兼容性结论

本轮为后端查询口径修复：

- 不改 API 路径
- 不改返回结构
- 不改前端入参
- 不改对象洞察台 UI 结构

属于兼容性修复，不要求前端联动改造。

## 10. 结论

本问题的本质不是对象洞察台绘图错误，而是 `history/batch` 的后端历史读取缺少统一查询窗口，导致“旧数据先被 LIMIT 命中，最近窗口再被补零”。正确修复方式是把时间窗口下沉到 v2 raw、normalized fallback、legacy fallback 三条历史读取链中统一执行，再保留现有补零协议与前端消费方式。
