# Telemetry V2 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production-ready Telemetry V2 foundation: TDengine v2 raw primary writes, MySQL latest projection, async legacy mirror hooks, read-routing skeleton, and synced docs/config.

**Architecture:** Keep the fixed Pipeline unchanged and replace the current telemetry write core with a coordinator that writes V2 raw storage first, then dispatches async projection work for latest and legacy compatibility. Read paths gain explicit routing so V2 can be adopted gradually while legacy fallback remains configurable.

**Tech Stack:** Spring Boot 4, Java 17, TDengine via `JdbcTemplate`, MySQL, Redis `StringRedisTemplate`, JUnit 5, Mockito, Maven

---

## File Structure

### Existing files to modify

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\config\IotProperties.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-dev.yml`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-prod.yml`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-test.yml`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\handler\TelemetryPersistStageHandler.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TdengineTelemetryFacade.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\LegacyTdengineTelemetryWriter.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\LegacyTdengineTelemetryReader.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- `E:\idea\ghatg\spring-boot-iot\README.md`
- `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

### New files to create

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryV2Point.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryStreamKind.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryProjectionTask.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolver.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2SchemaSupport.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2TableNamingStrategy.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryRawBatchWriter.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinator.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryProjectionQueue.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\RedisTelemetryProjectionQueue.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjectionRepository.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjector.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLegacyMirrorProjector.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryReadRouter.java`
- `E:\idea\ghatg\spring-boot-iot\sql\upgrade\20260327_phase5_telemetry_v2_latest_projection.sql`

### Tests to create or expand

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolverTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2SchemaSupportTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryRawBatchWriterTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinatorTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\RedisTelemetryProjectionQueueTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjectorTest.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryReadRouterTest.java`
- expand:
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\handler\TelemetryPersistStageHandlerTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TdengineTelemetryFacadeTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImplTest.java`

## Implementation Notes

- Preserve the fixed Pipeline stage order. Only replace the internals of `TELEMETRY_PERSIST`.
- V2 raw write is the only success criterion for telemetry persistence in TDengine mode.
- Legacy write becomes async mirror work. Mirror failures must not flip the main stage into failure.
- `application-dev.yml` and `application-prod.yml` should default to V2-enabled, legacy-mirror-enabled. `application-test.yml` may keep conservative defaults but must include the new keys.
- Keep current legacy reader/writer logic reachable behind isolated adapters so disabling legacy only requires config changes.
- Use ASCII in new files unless a Chinese comment is necessary to explain non-obvious telemetry semantics.

### Task 1: Add V2 Configuration and Routing Skeleton

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\config\IotProperties.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-dev.yml`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-prod.yml`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-admin\src\main\resources\application-test.yml`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolver.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolverTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldResolveV2PrimaryWriteAndLegacyMirrorFlags() {
    IotProperties properties = new IotProperties();
    properties.getTelemetry().setStorageType("tdengine");
    properties.getTelemetry().setPrimaryStorage("tdengine-v2");
    properties.getTelemetry().getLegacyMirror().setEnabled(true);

    TelemetryStorageModeResolver resolver = new TelemetryStorageModeResolver(properties);

    assertTrue(resolver.isTdengineEnabled());
    assertTrue(resolver.isV2PrimaryEnabled());
    assertTrue(resolver.isLegacyMirrorEnabled());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryStorageModeResolverTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the resolver class and new config fields do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement:
- new `IotProperties.Telemetry` nested config for:
  - `primaryStorage`
  - `raw.retentionDays`
  - `latest.redisEnabled`
  - `latest.mysqlProjectionEnabled`
  - `aggregate.enabled/hourlyEnabled/dailyEnabled`
  - `legacyMirror.enabled/mode/retryEnabled`
  - `readRouting.latestSource/historySource/aggregateSource/legacyReadFallbackEnabled`
  - `tenantRouting.mode`
- `TelemetryStorageModeResolver` methods used by the write and read paths.
- YAML defaults in all three application profiles.

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryStorageModeResolverTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-framework/src/main/java/com/ghlzm/iot/framework/config/IotProperties.java spring-boot-iot-admin/src/main/resources/application-dev.yml spring-boot-iot-admin/src/main/resources/application-prod.yml spring-boot-iot-admin/src/main/resources/application-test.yml spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryStorageModeResolver.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryStorageModeResolverTest.java
git commit -m "feat: add telemetry v2 config routing skeleton"
```

### Task 2: Build TDengine V2 Raw Schema and Batch Writer

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryStreamKind.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryV2Point.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2TableNamingStrategy.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2SchemaSupport.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryRawBatchWriter.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryV2SchemaSupportTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryRawBatchWriterTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests that prove:
- table names resolve to `tenant + device + stream`
- `ensureTable` creates the three V2 stables
- a single property payload becomes batched V2 points without one `jdbcTemplate.update()` call per metric

```java
@Test
void shouldGroupPointsByTenantDeviceAndStreamKind() { ... }

@Test
void shouldCreateMeasureStatusAndEventStables() { ... }

@Test
void shouldBatchInsertGroupedV2Points() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryV2SchemaSupportTest,TelemetryRawBatchWriterTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the V2 schema and writer classes do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement:
- `TelemetryStreamKind` to map normalized metrics into `MEASURE`, `STATUS`, or `EVENT`.
- `TelemetryV2Point` as the intermediate write model.
- `TelemetryV2TableNamingStrategy` for `tb_m_<tenantId>_<deviceId>` / `tb_s_<tenantId>_<deviceId>` / `tb_e_<tenantId>_<deviceId>`.
- `TelemetryV2SchemaSupport` to create V2 stables with the agreed raw column set.
- `TelemetryRawBatchWriter` to transform `DeviceProcessingTarget` into grouped batches and issue batched TDengine inserts.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryV2SchemaSupportTest,TelemetryRawBatchWriterTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/model/TelemetryStreamKind.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/model/TelemetryV2Point.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryV2TableNamingStrategy.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryV2SchemaSupport.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryRawBatchWriter.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryV2SchemaSupportTest.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryRawBatchWriterTest.java
git commit -m "feat: add telemetry v2 raw writer foundation"
```

### Task 3: Add MySQL Latest Projection Storage

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\sql\upgrade\20260327_phase5_telemetry_v2_latest_projection.sql`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjectionRepository.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjector.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLatestProjectorTest.java`

- [ ] **Step 1: Write the failing test**

Add a projector test that proves:
- latest projection upserts by `(tenant_id, device_id, metric_id)`
- a newer `reported_at` overwrites an older one
- older replays do not clobber fresher values

```java
@Test
void shouldUpsertLatestSnapshotUsingTenantDeviceMetricKey() { ... }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryLatestProjectorTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the repository and projector do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement:
- SQL upgrade script to create `iot_device_metric_latest`.
- repository methods for bulk upsert and point lookup by device.
- projector logic that writes only the freshest value per metric.

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryLatestProjectorTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add sql/upgrade/20260327_phase5_telemetry_v2_latest_projection.sql spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryLatestProjectionRepository.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryLatestProjector.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryLatestProjectorTest.java
git commit -m "feat: add telemetry latest projection storage"
```

### Task 4: Wire V2 Write Coordination and Async Legacy Mirror

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryProjectionTask.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryProjectionQueue.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\RedisTelemetryProjectionQueue.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinator.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryLegacyMirrorProjector.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\handler\TelemetryPersistStageHandler.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TdengineTelemetryFacade.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\LegacyTdengineTelemetryWriter.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinatorTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\RedisTelemetryProjectionQueueTest.java`
- Modify test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\handler\TelemetryPersistStageHandlerTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests that prove:
- V2 TDengine mode writes raw first and returns success without waiting for legacy mirror completion.
- legacy mirror tasks are enqueued when enabled.
- `TelemetryPersistStageHandler` delegates to the coordinator instead of directly to the old facade writer path.

```java
@Test
void shouldWriteV2RawBeforePublishingLegacyMirrorTask() { ... }

@Test
void shouldNotFailMainWriteWhenLegacyMirrorQueuePublishFails() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryWriteCoordinatorTest,RedisTelemetryProjectionQueueTest,TelemetryPersistStageHandlerTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the coordinator and queue do not exist yet and the handler still delegates to the old path.

- [ ] **Step 3: Write minimal implementation**

Implement:
- queue abstraction backed by `StringRedisTemplate`
- coordinator flow:
  - resolve telemetry mode
  - V2 raw write
  - enqueue latest projection task
  - enqueue legacy mirror task if enabled
- legacy mirror projector that reuses the existing legacy writer
- updated stage handler wiring

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryWriteCoordinatorTest,RedisTelemetryProjectionQueueTest,TelemetryPersistStageHandlerTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/model/TelemetryProjectionTask.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryProjectionQueue.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/RedisTelemetryProjectionQueue.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryWriteCoordinator.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryLegacyMirrorProjector.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacade.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryWriter.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryWriteCoordinatorTest.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/RedisTelemetryProjectionQueueTest.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandlerTest.java
git commit -m "feat: coordinate telemetry v2 writes and legacy mirror"
```

### Task 5: Add V2 Read Routing for Latest Queries

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryReadRouter.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\LegacyTdengineTelemetryReader.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryReadRouterTest.java`
- Modify test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImplTest.java`
- Modify test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TdengineTelemetryFacadeTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests that prove:
- latest queries prefer V2 projected latest when `latest-source=v2`
- legacy fallback is still used when enabled and V2 latest has gaps
- MySQL storage mode still falls back to `iot_device_property`

```java
@Test
void shouldReadV2LatestBeforeLegacyFallback() { ... }

@Test
void shouldUseMysqlDevicePropertyWhenTdengineDisabled() { ... }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryReadRouterTest,TelemetryQueryServiceImplTest,TdengineTelemetryFacadeTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because V2 latest routing does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement:
- `TelemetryReadRouter` to resolve latest/history sources from config
- `TelemetryQueryServiceImpl` to use V2 latest repository first
- legacy fallback only when configured and V2 latest is incomplete

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry -am test "-Dtest=TelemetryReadRouterTest,TelemetryQueryServiceImplTest,TdengineTelemetryFacadeTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryReadRouter.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/LegacyTdengineTelemetryReader.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryReadRouterTest.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TdengineTelemetryFacadeTest.java
git commit -m "feat: route telemetry latest reads through v2 projection"
```

### Task 6: Sync Documentation, Schema, and Final Regression Coverage

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify if needed: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify if needed: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`

- [ ] **Step 1: Add or update regression tests first**

Expand or add tests covering:
- `TelemetryPersistStageHandler`
- `TelemetryWriteCoordinator`
- `TelemetryQueryServiceImpl`
- legacy mirror disabled path
- V2 latest fallback behavior

- [ ] **Step 2: Run the telemetry-focused regression suite**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry,spring-boot-iot-device,spring-boot-iot-message -am test -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false "-Dtest=TelemetryStorageModeResolverTest,TelemetryV2SchemaSupportTest,TelemetryRawBatchWriterTest,TelemetryLatestProjectorTest,TelemetryWriteCoordinatorTest,RedisTelemetryProjectionQueueTest,TelemetryReadRouterTest,TelemetryPersistStageHandlerTest,TelemetryQueryServiceImplTest,TdengineTelemetryFacadeTest,LegacyTdengineTelemetryWriterTest,LegacyTdengineTelemetryReaderTest"
```

Expected: PASS for all listed tests.

- [ ] **Step 3: Update docs and config references**

Document:
- V2 raw primary storage
- latest projection table
- legacy async mirror compatibility
- new YAML keys
- migration and fallback behavior

Confirm whether `README.md` and `AGENTS.md` need updates; if they do, update them in place instead of creating parallel docs.

- [ ] **Step 4: Run docs and quality checks**

Run:

```powershell
node scripts/docs/check-topology.mjs
node scripts/run-quality-gates.mjs
```

Expected:
- topology check: PASS
- quality gates: PASS, or explicit list of remaining environment blockers

- [ ] **Step 5: Commit**

```powershell
git add docs/01-系统概览与架构说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/07-部署运行与配置说明.md docs/08-变更记录与技术债清单.md README.md AGENTS.md
git commit -m "docs: document telemetry v2 foundation"
```

