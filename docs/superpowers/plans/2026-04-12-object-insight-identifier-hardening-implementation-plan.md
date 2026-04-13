# Object Insight Identifier Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent object insight trend queries from going all-zero when saved product insight identifiers differ from runtime telemetry identifiers only by casing.

**Architecture:** Add a write-side normalization step in product metadata validation so saved `objectInsight.customMetrics[]` identifiers align to formal product model identifiers when the mismatch is case-only. Add a read-side fallback in telemetry history query resolution so direct callers still resolve identifiers against product metadata and current device properties before querying TDengine.

**Tech Stack:** Spring Boot, MyBatis-Plus, Jackson, Vitest, JUnit 5, Mockito

---

### Task 1: Lock the regression with tests

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- Modify: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`
- Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`

- [ ] Add a product-service test proving mixed-case `objectInsight.customMetrics[].identifier` values are normalized to formal product model identifiers before persistence.
- [ ] Add a telemetry-query test proving `/api/telemetry/history/batch` resolves incoming identifiers against current device property casing before querying raw history.
- [ ] Run targeted backend/frontend tests and confirm the new cases fail before implementation.

### Task 2: Normalize identifiers on product metadata writes

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`

- [ ] Inject the product-model read dependency needed to map case-insensitive identifiers back to the formal product model identifier for the same product.
- [ ] During metadata normalization, rewrite `objectInsight.customMetrics[]` identifiers to the canonical formal identifier when a case-insensitive match exists.
- [ ] Keep duplicate detection and group validation aligned with the rewritten identifiers.

### Task 3: Resolve identifiers defensively on telemetry history reads

**Files:**
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`

- [ ] Resolve requested identifiers against product metadata and current device properties with case-insensitive matching before building metadata and issuing TDengine queries.
- [ ] Ensure the response preserves the resolved runtime/formal identifier so bucket matching and display-name lookup stay consistent.
- [ ] Keep existing history routing and fallback semantics unchanged aside from identifier hardening.

### Task 4: Verify and document

**Files:**
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] Update the change log with the new write-side and read-side hardening scope.
- [ ] Run targeted verification commands for `ProductServiceImplTest`, `TelemetryQueryServiceImplTest`, and the related Vitest regression suite.
- [ ] Check `git diff --check` on touched files.
