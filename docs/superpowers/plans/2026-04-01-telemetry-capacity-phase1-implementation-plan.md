# Telemetry Capacity Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce the first long-term telemetry capacity architecture seam by separating write fanout from raw persistence and wiring aggregate/cold-archive extension points without changing current business behavior.

**Architecture:** Keep the existing `TelemetryWriteCoordinator -> TelemetryRawBatchWriter` raw write path intact, but move projection dispatch into a dedicated fanout service so latest projection, legacy mirror, future aggregate, and future cold archive become independent downstream actions. Add new projection task types plus no-op downstream implementations that are gated by configuration, so subsequent phases can add real aggregate and archive storage without reworking the write coordinator again.

**Tech Stack:** Spring Boot 4, Java 17, TDengine, MySQL, JdbcTemplate, JUnit 5, Mockito, Maven

---

## File Structure

### Existing files to modify

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\config\IotProperties.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolver.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinator.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryProjectionTask.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinatorTest.java`

### New files to create

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryAggregateProjector.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryColdArchiveWriter.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteFanoutService.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteFanoutServiceTest.java`

## Task 1: Lock the New Fanout Contract in Tests

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteFanoutServiceTest.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinatorTest.java`

- [ ] Write a failing test asserting the new fanout service dispatches `LATEST`, `LEGACY_MIRROR`, `AGGREGATE`, and `COLD_ARCHIVE` only when their corresponding switches are enabled.
- [ ] Run `mvn -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=TelemetryWriteFanoutServiceTest,TelemetryWriteCoordinatorTest" test` and verify it fails because the fanout service and new projection types do not exist yet.
- [ ] Update the coordinator test so `TelemetryWriteCoordinator` depends on the fanout service instead of directly invoking latest and legacy projectors.
- [ ] Re-run the same Maven command and verify the failure is now isolated to missing implementation.

## Task 2: Add Projection Types and Configuration Gates

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-framework\src\main\java\com\ghlzm\iot\framework\config\IotProperties.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryStorageModeResolver.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\model\TelemetryProjectionTask.java`

- [ ] Add failing assertions in the tests for `AGGREGATE` and `COLD_ARCHIVE` projection types and for the new storage mode switches.
- [ ] Run the same targeted Maven command and verify it fails on missing enum values / missing resolver methods.
- [ ] Add aggregate and cold-archive config blocks plus resolver methods that expose whether those downstream actions are enabled.
- [ ] Extend `TelemetryProjectionTask.ProjectionType` with `AGGREGATE` and `COLD_ARCHIVE`.
- [ ] Re-run the targeted Maven command and verify the failure moves to missing fanout implementation.

## Task 3: Implement Fanout Service and No-Op Downstream Extensions

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryAggregateProjector.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryColdArchiveWriter.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteFanoutService.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryWriteCoordinator.java`

- [ ] Implement `TelemetryAggregateProjector` as a minimal no-op projector that accepts a `TelemetryProjectionTask` and returns immediately when aggregation is not yet materialized.
- [ ] Implement `TelemetryColdArchiveWriter` as a minimal no-op archive hook that accepts a `TelemetryProjectionTask` and returns immediately when cold storage is not yet materialized.
- [ ] Implement `TelemetryWriteFanoutService` to publish and asynchronously dispatch latest, legacy mirror, aggregate, and cold-archive tasks according to `TelemetryStorageModeResolver`.
- [ ] Refactor `TelemetryWriteCoordinator` so it only resolves points, performs raw write, then delegates all downstream dispatch to the fanout service.
- [ ] Re-run `mvn -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dmaven.test.skip=false" "-Dtest=TelemetryWriteFanoutServiceTest,TelemetryWriteCoordinatorTest" test` and verify the targeted tests pass.

## Task 4: Run Focused Module Verification

**Files:**
- Modify only if verification exposes a real defect in the files above.

- [ ] Run `mvn -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dmaven.test.skip=false" test` and verify the telemetry module test suite passes.
- [ ] Run `mvn -pl spring-boot-iot-telemetry -am -DskipTests compile` and verify compilation passes in reactor context.
- [ ] Review `git diff` to confirm this phase only changes telemetry capacity architecture scaffolding and related tests.
