# Device Insight Telemetry History Window Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `/api/telemetry/history/batch` 在对象洞察台返回全 0 趋势的问题，让 v2 raw、normalized fallback、legacy fallback 三条历史读链都按请求时间窗口读取 TDengine 历史数据。

**Architecture:** 在 `TelemetryQueryServiceImpl` 内新增统一的历史查询窗口定义，并把窗口显式传给三个历史读取器。各读取器在 SQL 层按窗口过滤最近数据，再沿用现有的入库时间优先聚桶和 `ZERO` 补零协议，避免“先读旧数据再补 0”。

**Tech Stack:** Java 17, Spring Boot, MyBatis, TDengine JDBC, JUnit 5, Mockito

---

### Task 1: 先用失败测试锁定窗口过滤缺失

**Files:**
- Modify: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

- [ ] **Step 1: 写失败测试，证明最近窗口被旧历史点挤掉时会错误补零**

```java
@Test
void getHistoryBatchShouldIgnoreRawPointsOutsideRequestedWindow() {
    Device device = buildDevice();
    Product product = buildProduct();
    DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
    LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
    LocalDateTime stalePointTime = now.minusDays(40);
    LocalDateTime recentPointTime = now.minusDays(1);

    when(deviceMapper.selectOne(any())).thenReturn(device);
    when(productMapper.selectById(1001L)).thenReturn(product);
    when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
    when(telemetryReadRouter.historySource()).thenReturn("v2");
    when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
    when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
            "L4_NW_1", measureMetadata
    ));
    when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
    when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
    when(telemetryRawHistoryReader.listHistory(
            eq(device),
            eq(product),
            anyMap(),
            eq(List.of("L4_NW_1")),
            any(),
            any(),
            anyInt()
    )).thenReturn(List.of(
            historyPoint("L4_NW_1", "泥水位高程", 9.9D, stalePointTime),
            historyPoint("L4_NW_1", "泥水位高程", 2.6D, recentPointTime)
    ));

    TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
    request.setDeviceId(2001L);
    request.setIdentifiers(List.of("L4_NW_1"));
    request.setRangeCode("7d");
    request.setFillPolicy("ZERO");

    TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

    assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 2.6D));
    assertEquals(false, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 9.9D));
}
```

- [ ] **Step 2: 再写两个失败测试，要求 fallback 读取器也收到窗口参数**

```java
@Test
void getHistoryBatchShouldPassWindowToNormalizedFallbackReader() {
    Device device = buildDevice();
    Product product = buildProduct();
    DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");

    when(deviceMapper.selectOne(any())).thenReturn(device);
    when(productMapper.selectById(1001L)).thenReturn(product);
    when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
    when(telemetryReadRouter.historySource()).thenReturn("v2");
    when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
    when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of("L4_NW_1", measureMetadata));
    when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
    when(telemetryRawHistoryReader.listHistory(eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1")), any(), any(), anyInt()))
            .thenReturn(List.of());
    when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(true);
    when(normalizedTelemetryHistoryReader.listHistory(eq(device), eq(product), anyMap(), any(), any(), anyInt()))
            .thenReturn(List.of(historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.now().minusDays(1))));

    TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
    request.setDeviceId(2001L);
    request.setIdentifiers(List.of("L4_NW_1"));
    request.setRangeCode("7d");
    request.setFillPolicy("ZERO");

    telemetryQueryService.getHistoryBatch(request);

    verify(normalizedTelemetryHistoryReader).listHistory(eq(device), eq(product), anyMap(), any(), any(), anyInt());
}

@Test
void getHistoryBatchShouldPassWindowToLegacyFallbackReader() {
    Device device = buildDevice();
    Product product = buildProduct();
    DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");

    when(deviceMapper.selectOne(any())).thenReturn(device);
    when(productMapper.selectById(1001L)).thenReturn(product);
    when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
    when(telemetryReadRouter.historySource()).thenReturn("legacy");
    when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
    when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of("L4_NW_1", measureMetadata));
    when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
    when(legacyTelemetryHistoryReader.listHistory(eq(device), eq(product), anyMap(), anyMap(), any(), any(), anyInt()))
            .thenReturn(List.of(historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.now().minusDays(1))));

    TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
    request.setDeviceId(2001L);
    request.setIdentifiers(List.of("L4_NW_1"));
    request.setRangeCode("7d");
    request.setFillPolicy("ZERO");

    telemetryQueryService.getHistoryBatch(request);

    verify(legacyTelemetryHistoryReader).listHistory(eq(device), eq(product), anyMap(), anyMap(), any(), any(), anyInt());
}
```

- [ ] **Step 3: 跑单测确认当前实现无法通过新断言**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
FAIL
```

- [ ] **Step 4: 提交前不写生产代码，先确认测试失败点就是窗口参数缺失**

Expected failure focus:

```text
listHistory(...) 参数签名不匹配
or
新增断言失败，最近窗口没有真实点
```

### Task 2: 在查询服务中引入统一历史窗口定义

**Files:**
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

- [ ] **Step 1: 在 `TelemetryQueryServiceImpl` 中扩展窗口定义，加入起止时间**

```java
private record HistoryQueryWindow(
        String rangeCode,
        String bucketCode,
        ChronoUnit unit,
        int slotCount,
        LocalDateTime windowStart,
        LocalDateTime windowEnd
) {
}
```

- [ ] **Step 2: 把 `resolveRangeDefinition` 和 `buildBucketSlots` 改为基于统一窗口对象工作**

```java
private HistoryQueryWindow resolveHistoryQueryWindow(String rangeCode, LocalDateTime now) {
    String normalizedRangeCode = normalizeRangeCode(rangeCode);
    HistoryWindowTemplate template = switch (normalizedRangeCode) {
        case "1d" -> new HistoryWindowTemplate("1d", "hour", ChronoUnit.HOURS, 24);
        case "7d" -> new HistoryWindowTemplate("7d", "day", ChronoUnit.DAYS, 7);
        case "30d" -> new HistoryWindowTemplate("30d", "day", ChronoUnit.DAYS, 30);
        case "365d" -> new HistoryWindowTemplate("365d", "month", ChronoUnit.MONTHS, 12);
        default -> throw new BizException("不支持的时间范围: " + rangeCode);
    };
    LocalDateTime anchor = alignToBucket(now, template.unit());
    LocalDateTime windowStart = anchor.minus(template.slotCount() - 1L, template.unit());
    LocalDateTime windowEnd = anchor.plus(1, template.unit());
    return new HistoryQueryWindow(
            template.rangeCode(),
            template.bucketCode(),
            template.unit(),
            template.slotCount(),
            windowStart,
            windowEnd
    );
}
```

- [ ] **Step 3: 在 `getHistoryBatch` 中用窗口对象替代原 `RangeDefinition`**

```java
HistoryQueryWindow queryWindow = resolveHistoryQueryWindow(request.getRangeCode(), LocalDateTime.now());
List<BucketSlot> slots = buildBucketSlots(queryWindow, LocalDateTime.now());
List<TelemetryV2Point> historyPoints = readHistoryPoints(
        device,
        product,
        metadataMap,
        mappingMap,
        identifiers,
        queryWindow
);
return buildHistoryBatchResponse(
        device.getId(),
        identifiers,
        request.getRangeCode(),
        queryWindow,
        slots,
        historyPoints,
        metadataMap
);
```

- [ ] **Step 4: 更新私有方法签名，把窗口继续下传到三条读链**

```java
private List<TelemetryV2Point> readHistoryPoints(..., List<String> identifiers, HistoryQueryWindow queryWindow)
private List<TelemetryV2Point> readV2History(..., List<String> identifiers, HistoryQueryWindow queryWindow)
private List<TelemetryV2Point> readLegacyHistory(..., HistoryQueryWindow queryWindow)
```

- [ ] **Step 5: 运行查询服务测试，确保签名变更已让测试进入下一轮失败点**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
FAIL
```

### Task 3: 给 v2 raw 读取器加窗口过滤

**Files:**
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryRawHistoryReader.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

- [ ] **Step 1: 扩展 `TelemetryRawHistoryReader.listHistory(...)` 签名，接收窗口起止时间**

```java
public List<TelemetryV2Point> listHistory(Device device,
                                          Product product,
                                          Map<String, DevicePropertyMetadata> metadataMap,
                                          List<String> identifiers,
                                          LocalDateTime windowStart,
                                          LocalDateTime windowEnd,
                                          int batchSize)
```

- [ ] **Step 2: 把 raw SQL 改成只查窗口内数据**

```java
private String buildSelectSql(String childTable, List<String> identifiers, int batchSize) {
    String placeholders = String.join(", ", identifiers.stream().map(item -> "?").toList());
    return SELECT_COLUMNS
            + childTable
            + " WHERE metric_id IN (" + placeholders + ")"
            + " AND COALESCE(ingested_at, ts, reported_at) >= ?"
            + " AND COALESCE(ingested_at, ts, reported_at) < ?"
            + " ORDER BY ts ASC"
            + " LIMIT " + Math.max(batchSize, 1);
}
```

- [ ] **Step 3: 传入时间参数并保持现有点模型转换不变**

```java
return jdbcTemplate.query(sql, rs -> {
    ...
}, Stream.concat(
        identifiers.stream().map(item -> (Object) item),
        Stream.of(Timestamp.valueOf(windowStart), Timestamp.valueOf(windowEnd))
).toArray());
```

- [ ] **Step 4: 在 `TelemetryQueryServiceImpl.readV2History(...)` 中把窗口传给 raw 读取器**

```java
List<TelemetryV2Point> rawHistory = telemetryRawHistoryReader.listHistory(
        device,
        product,
        metadataMap,
        identifiers,
        queryWindow.windowStart(),
        queryWindow.windowEnd(),
        HISTORY_BATCH_SIZE
);
```

- [ ] **Step 5: 运行测试，确认 raw 主链相关新增测试转绿**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
仍有 FAIL，但 raw 主链断言已通过
```

### Task 4: 给 normalized fallback 和 legacy fallback 加同一窗口过滤

**Files:**
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/NormalizedTelemetryHistoryReader.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTelemetryHistoryReader.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

- [ ] **Step 1: 扩展 normalized 读取器签名，并在 SQL 中按 `COALESCE(reported_at, ts)` 过滤**

```java
public List<TelemetryV2Point> listHistory(Device device,
                                          Product product,
                                          Map<String, DevicePropertyMetadata> metadataMap,
                                          LocalDateTime windowStart,
                                          LocalDateTime windowEnd,
                                          int batchSize)
```

```java
private static final String SELECT_SQL = """
        SELECT
            ts,
            reported_at,
            trace_id,
            message_type,
            metric_code,
            metric_name,
            value_type,
            value_text,
            value_long,
            value_double,
            value_bool
        FROM iot_device_telemetry_point
        WHERE device_id = ?
          AND COALESCE(reported_at, ts) >= ?
          AND COALESCE(reported_at, ts) < ?
        ORDER BY reported_at ASC, ts ASC
        LIMIT ?
        """;
```

- [ ] **Step 2: 扩展 legacy 读取器签名，并在每个 subtable SQL 上按 `COALESCE(rd, ts)` 过滤**

```java
public List<TelemetryV2Point> listHistory(Device device,
                                          Product product,
                                          Map<String, DevicePropertyMetadata> metadataMap,
                                          Map<String, TelemetryMetricMapping> mappingMap,
                                          LocalDateTime windowStart,
                                          LocalDateTime windowEnd,
                                          int batchSize)
```

```java
sql.append(" FROM ").append(subTable)
   .append(" WHERE COALESCE(")
   .append(schema.hasColumn("rd") ? "rd" : "ts")
   .append(", ts) >= ? AND COALESCE(")
   .append(schema.hasColumn("rd") ? "rd" : "ts")
   .append(", ts) < ? ORDER BY ts ASC LIMIT ")
   .append(Math.max(batchSize, 1));
```

- [ ] **Step 3: 在 `TelemetryQueryServiceImpl` 中把统一窗口传给 normalized / legacy 读取器**

```java
List<TelemetryV2Point> normalizedHistory = normalizedTelemetryHistoryReader.listHistory(
        device,
        product,
        metadataMap,
        queryWindow.windowStart(),
        queryWindow.windowEnd(),
        HISTORY_BATCH_SIZE
);

return legacyTelemetryHistoryReader.listHistory(
        device,
        product,
        metadataMap,
        mappingMap,
        queryWindow.windowStart(),
        queryWindow.windowEnd(),
        HISTORY_BATCH_SIZE
);
```

- [ ] **Step 4: 跑查询服务测试，确认三条历史读链窗口参数都已生效**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
PASS
```

### Task 5: 补文档并做模块级验证

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: 在接口文档中补充 `history/batch` 统一按请求窗口过滤**

```md
- `POST /api/telemetry/history/batch` 当前会先按 `rangeCode` 计算查询窗口，再分别从 telemetry v2 raw、`iot_device_telemetry_point` 兼容表与 legacy fallback 链路中只读取窗口内历史点；不再采用“全量历史 + LIMIT 后再由服务端丢弃旧点”的旧口径。
```

- [ ] **Step 2: 在变更记录中登记本次对象洞察趋势全 0 根因与修复**

```md
- 2026-04-12：修复 `/api/telemetry/history/batch` 在对象洞察台场景下因历史读取缺少时间窗口过滤而出现“趋势全为 0”的问题。`TelemetryQueryServiceImpl` 现已把统一窗口下传到 v2 raw、normalized fallback 与 legacy fallback 三条读链，服务端补零仅用于窗口内缺口，不再掩盖旧历史点误命中的情况。
```

- [ ] **Step 3: 在 README 和 AGENTS 中同步一句历史查询按窗口过滤**

```md
对象洞察等历史趋势查询当前会先按请求 `rangeCode` 限定 TDengine 历史窗口，再执行聚桶与补零，避免长历史设备被早期数据挤占后出现整段 0 趋势。
```

- [ ] **Step 4: 运行 telemetry 模块测试**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: 运行后端最小质量验证**

Run:

```powershell
mvn -pl spring-boot-iot-admin -am test "-Dtest=TelemetryQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected:

```text
BUILD SUCCESS
```
