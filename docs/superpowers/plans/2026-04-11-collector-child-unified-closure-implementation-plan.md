# Collector-Child Unified Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first complete collector-child governance closure so product governance, runtime truth sources, shared dev data baseline, and risk/control-plane semantics all follow one parent-child contract.

**Architecture:** Freeze `iot_device_relation` as the only parent-child truth source, keep collector write-side ownership limited to collector-owned runtime fields, and treat child metrics plus child `sensor_state` as child-owned formal fields across compare, apply, latest, telemetry, state refresh, insight, and risk governance. This phase must be completed as one whole stage made of four work packages; do not stop after a single local fix.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Vitest, JUnit 5, Maven, MySQL init-data, TDengine, Python schema sync

---

## Stage Freeze

This phase is one integrated delivery body named:

`采集器-子设备一体化治理闭环一期`

Only these four work packages belong to this phase:

1. Governance boundary closure
2. Runtime truth-source closure
3. Shared dev data baseline closure
4. Risk and control-plane closure

Rules:

- Do not split this phase into endless “fix one more local edge case” loops.
- Do not open a fifth main line during execution.
- Every task must keep laser rangefinder, deep displacement, collector, and single-device deep displacement in view.
- Every task must record “next functional points”, but those points are notes for the next phase, not reasons to expand the current phase.

## Phase Completion Definition

This phase is complete only when all four work packages are complete together:

1. `/products` formal governance boundary is stable for collector vs child products.
2. `PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE` follow the same parent-child ownership boundary.
3. Shared `dev` environment can reproduce collector, laser, and deep displacement scenarios without manual patching.
4. Risk binding, threshold policy, governance task, and governance ops can correctly interpret collector-child semantics.
5. Docs are updated in-place in `docs/02`、`docs/03`、`docs/04`、`docs/08`、`docs/21`, and `README.md` / `AGENTS.md` are reviewed.

## File Structure

### Task 1 ownership: governance boundary closure

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`

### Task 2 ownership: runtime truth-source closure

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceStateStageHandler.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandlerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DeviceStateStageHandlerTest.java`
- Modify: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandlerTest.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImplTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java`

### Task 3 ownership: shared dev data baseline closure

- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

### Task 4 ownership: risk and control-plane closure

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributor.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributorTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RuleDefinitionView.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

## Task 1: Close the governance boundary as a stable stage rule

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Add apply-side defensive tests so collector products cannot publish child-owned fields even with crafted payloads**

Test targets to add:

```java
@Test
void applyGovernanceShouldRejectCollectorPayloadContainingChildMetricValue() { }

@Test
void applyGovernanceShouldRejectCollectorPayloadContainingChildSensorState() { }

@Test
void applyGovernanceShouldAllowCollectorOwnedRuntimeStatusFields() { }
```

Expected rule:

- collector product rejects `value`
- collector product rejects `sensor_state`
- collector product rejects `dispsX / dispsY`
- collector product accepts `temp / humidity / signal_4g`

- [ ] **Step 2: Run the focused governance tests and verify red before implementation**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
```

Expected: FAIL on the new apply-side guard tests.

- [ ] **Step 3: Implement one shared collector ownership guard used by both compare and apply**

Implementation target:

- keep the existing compare-side boundary
- add one shared ownership predicate used during `applyGovernance`
- fail fast with `BizException` when a collector product attempts to publish child-owned identifiers

Implementation notes:

```java
private void validateCollectorOwnedIdentifiers(Product product, List<ProductModelGovernanceApplyItemDTO> items) {
    if (!isCollectorProduct(product)) {
        return;
    }
    List<String> invalidIdentifiers = items.stream()
            .map(ProductModelGovernanceApplyItemDTO::getIdentifier)
            .filter(this::isChildOwnedFormalIdentifier)
            .toList();
    if (!invalidIdentifiers.isEmpty()) {
        throw new BizException("采集器产品不能发布子设备正式字段: " + String.join(",", invalidIdentifiers));
    }
}
```

- [ ] **Step 4: Keep the product workbench guidance aligned with the stricter server rule**

UI acceptance:

- composite collector mode keeps the existing warning note
- apply button path must not imply child fields can be published from collector product
- empty-state copy must explicitly say “子设备字段请到子产品治理”

- [ ] **Step 5: Run the green pack**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected:

- all collector compare/apply boundary tests pass
- existing laser and deep displacement governance tests stay green

- [ ] **Step 6: Record the completion definition for Task 1**

Task 1 is complete only when:

- collector compare and apply both enforce the same ownership rule
- laser and deep displacement child products still publish their own formal fields
- single-device deep displacement is not regressed

**Next Functional Points after Task 1:**

- add operator-facing “边界诊断原因码” for contract compare/apply rejections
- make collector boundary rules configurable per relation family only if a new vendor proves the current rule insufficient
- add backend audit detail for “why this identifier is child-owned”

## Task 2: Close the runtime truth source

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceStateStageHandler.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandler.java`
- Modify: `spring-boot-iot-message/src/main/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipeline.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandlerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DeviceStateStageHandlerTest.java`
- Modify: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/handler/TelemetryPersistStageHandlerTest.java`
- Modify: `spring-boot-iot-message/src/test/java/com/ghlzm/iot/message/pipeline/UpMessageProcessingPipelineTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceMessageServiceImplTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpChildMessageSplitterTest.java`

- [ ] **Step 1: Write runtime red tests for collector-child separation**

Add tests proving:

```java
@Test
void payloadApplyShouldNotWriteChildMetricsIntoCollectorLatest() { }

@Test
void telemetryPersistShouldPersistCollectorStatusForParentAndMonitoringMetricsForChild() { }

@Test
void deviceStateShouldRefreshParentLinkStateWithoutOverwritingChildSensorState() { }
```

Use two fixtures:

- laser collector payload with `value + sensor_state + temp/humidity/signal_4g`
- deep displacement collector payload with `dispsX + dispsY + sensor_state + temp/humidity`

- [ ] **Step 2: Run the runtime red pack**

Run:

```powershell
mvn -pl spring-boot-iot-device,spring-boot-iot-message,spring-boot-iot-telemetry,spring-boot-iot-protocol -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest,DeviceStateStageHandlerTest,TelemetryPersistStageHandlerTest,UpMessageProcessingPipelineTest,DeviceMessageServiceImplTest,LegacyDpChildMessageSplitterTest" test
```

Expected: FAIL because current runtime path still needs explicit assertions and, if missing, boundary fixes.

- [ ] **Step 3: Make runtime ownership explicit in three places**

Implementation target:

1. `DevicePayloadApplyStageHandler`
   - parent only applies collector-owned runtime fields
   - child targets apply child-owned monitoring metrics and child `sensor_state`
2. `TelemetryPersistStageHandler`
   - persist per target device only
   - no parent telemetry row for child-owned monitoring identifiers
3. `DeviceStateStageHandler`
   - parent state tracks collector/device online semantics
   - child `sensor_state` stays a property/latest fact, not overwritten by parent link reachability

Implementation note:

```java
// Parent target
properties = keepCollectorOwnedRuntimeFields(properties);

// Child target
properties = keepChildOwnedFormalFields(properties);
```

- [ ] **Step 4: Keep pipeline evidence readable**

Required pipeline result:

- `PAYLOAD_APPLY` summary must answer whether data applied to parent or child
- `TELEMETRY_PERSIST` summary must answer persisted property count per target
- `DEVICE_STATE` summary must not claim child sensor health was refreshed from parent link state

- [ ] **Step 5: Run the runtime green pack**

Run:

```powershell
mvn -pl spring-boot-iot-device,spring-boot-iot-message,spring-boot-iot-telemetry,spring-boot-iot-protocol -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest,DeviceStateStageHandlerTest,TelemetryPersistStageHandlerTest,UpMessageProcessingPipelineTest,DeviceMessageServiceImplTest,LegacyDpChildMessageSplitterTest" test
```

Expected:

- parent latest contains only collector-owned runtime fields
- child latest/telemetry contain `value` or `dispsX / dispsY`, plus child `sensor_state`
- no cross-write regression in the message pipeline

- [ ] **Step 6: Record the completion definition for Task 2**

Task 2 is complete only when:

- compare/apply boundary and runtime boundary match
- insight read model can explain where parent status and child status each come from
- there is no duplicate truth source between parent latest and child latest

**Next Functional Points after Task 2:**

- expose separate child status history query if the business later wants historical `sensor_state` curves in parent aggregate pages
- add automated drift audit comparing compare/apply ownership rules vs runtime ownership rules
- consider a parent-child latest projection view only if reporting demands it, never as a new write-side truth source

## Task 3: Close the shared dev data baseline

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-admin/src/main/resources/application-dev.yml`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write down the exact baseline dataset to make reproducible**

This task must baseline:

- collector product: `nf-monitor-collector-v1`
- laser product: `nf-monitor-laser-rangefinder-v1`
- deep displacement product: `nf-monitor-deep-displacement-v1`
- laser object insight config
- deep displacement object insight config
- collector-child `iot_device_relation` rows for laser and deep displacement examples
- sample devices and, where needed, risk demo bindings

- [ ] **Step 2: Add schema-sync and seed red tests/checks**

Run these checks before implementation:

```powershell
python -m unittest scripts.tests.test_run_real_env_schema_sync
Select-String -Path sql/init-data.sql -Pattern "nf-monitor-laser-rangefinder-v1|nf-monitor-deep-displacement-v1|nf-monitor-collector-v1"
Select-String -Path spring-boot-iot-admin/src/main/resources/application-dev.yml -Pattern "SK00EA0D1307988|SK00FB0D1310195"
```

Expected: identify what is already seeded and what is still missing from a fresh dev environment.

- [ ] **Step 3: Seed the missing baseline**

Implementation scope:

- `sql/init-data.sql`
  - ensure collector and child products exist together
  - ensure formal models for laser and deep displacement are seeded with the agreed identifiers
  - ensure object-insight config is aligned with the formal fields
- `application-dev.yml`
  - ensure the sample relation/mapping fixtures still cover both laser and deep displacement collectors
- `run-real-env-schema-sync.py`
  - backfill missing products, relations, and fixed governance seeds for older shared environments

Required deep displacement baseline:

```text
single-device product fields: dispsX / dispsY / sensor_state
collector-child relation strategy: LEGACY + SENSOR_STATE
collector product fields: temp / humidity / signal_4g / ...
```

- [ ] **Step 4: Run baseline verification**

Run:

```powershell
python -m unittest scripts.tests.test_run_real_env_schema_sync
Select-String -Path sql/init-data.sql -Pattern "nf-monitor-laser-rangefinder-v1|nf-monitor-deep-displacement-v1|nf-monitor-collector-v1"
Select-String -Path sql/init-data.sql -Pattern "sensor_state|dispsX|dispsY"
Select-String -Path spring-boot-iot-admin/src/main/resources/application-dev.yml -Pattern "SK00EA0D1307988|SK00FB0D1310195|84330701"
```

Expected:

- schema sync tests pass
- seed file contains the complete product baseline
- shared dev config still contains the canonical collector-child mapping examples

- [ ] **Step 5: Record the completion definition for Task 3**

Task 3 is complete only when:

- a fresh environment and a historical environment can both reach the same baseline with script + init-data
- laser and deep displacement no longer rely on manual database patching to demonstrate the agreed architecture
- docs match the new baseline

**Next Functional Points after Task 3:**

- package sample datasets as “厂商场景样板包”
- add a one-shot baseline verifier script for product + relation + insight config + risk seeds
- add synthetic MQTT payload replay fixtures for browser-side acceptance

## Task 4: Close risk and control-plane semantics

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributor.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImpl.java`
- Modify: `spring-boot-iot-ui/src/views/GovernanceTaskView.vue`
- Modify: `spring-boot-iot-ui/src/views/GovernanceOpsWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceWorkItemContributorTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceOpsServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RuleDefinitionView.test.ts`

- [ ] **Step 1: Freeze the risk semantics instead of leaving them implicit**

This phase adopts the following minimum semantics:

- laser rangefinder publishes `value` to risk catalog; `sensor_state` stays governance-only
- deep displacement publishes `dispsX` and `dispsY` to risk catalog; `sensor_state` stays governance-only
- collector product does not publish child-owned metrics to risk catalog

Required matcher result:

```text
phase1-crack -> value
phase2-gnss -> gpsTotalX / gpsTotalY / gpsTotalZ
phase3-deep-displacement -> dispsX / dispsY
```

- [ ] **Step 2: Add red tests for publish rule, work items, and ops alerts**

Add tests proving:

```java
@Test
void publishRuleShouldAllowDeepDisplacementMonitoringMetrics() { }

@Test
void workItemContributorShouldExplainCollectorChildBoundaryGapInSnapshot() { }

@Test
void opsServiceShouldEmitBoundaryAwareMissingMetricAlertForDeepDisplacement() { }
```

UI red tests:

- governance task can route boundary-related product governance gaps back to `/products`
- risk point and rule-definition pages can interpret child-owned metric context without treating collector as the monitoring subject

- [ ] **Step 3: Implement the minimum control-plane semantics without spawning a fifth subsystem**

Implementation rule:

- reuse existing work item and ops alert objects
- enrich `snapshotJson`, `blockingReason`, `dimensionKey`, and route context
- do not invent a separate new control-plane application

Expected behavior:

- missing relation or missing child formal state guidance returns to `/products`
- missing risk binding still returns to `/risk-point`
- missing threshold policy still returns to `/rule-definition`
- control plane payload clearly says whether the subject is collector-owned or child-owned

- [ ] **Step 4: Run the control-plane green pack**

Run:

```powershell
mvn -pl spring-boot-iot-alarm,spring-boot-iot-device,spring-boot-iot-system -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DefaultRiskMetricCatalogPublishRuleTest,RiskGovernanceWorkItemContributorTest,RiskGovernanceOpsServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/GovernanceControlPlaneViews.test.ts src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RuleDefinitionView.test.ts
```

Expected:

- deep displacement metrics become risk-ready under the frozen phase rule
- work items and ops alerts can explain collector-child gaps
- risk point and threshold policy pages receive the correct child-owned metric context

- [ ] **Step 5: Record the completion definition for Task 4**

Task 4 is complete only when:

- risk catalog knows which deep displacement metrics are publishable
- control plane can distinguish contract boundary issues from binding/policy issues
- downstream pages route operators to the correct domain workbench without turning collector into the formal monitoring subject

**Next Functional Points after Task 4:**

- add boundary-specific ops alert filters once operators prove they need them
- add product-level replay templates for collector-child scenarios
- consider richer deep displacement risk semantics such as primary-axis recommendation only after the two-axis baseline is stable

## Cross-Cutting Delivery Gate

These checks are mandatory after Tasks 1-4 are all complete:

- [ ] **Step 1: Update docs in place**

Must review and update as needed:

- `docs/02-业务功能与流程说明.md`
- `docs/03-接口规范与接口清单.md`
- `docs/04-数据库设计与初始化数据.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`
- review `README.md`
- review `AGENTS.md`

- [ ] **Step 2: Run the full focused verification pack**

Run:

```powershell
mvn -pl spring-boot-iot-device,spring-boot-iot-message,spring-boot-iot-telemetry,spring-boot-iot-protocol,spring-boot-iot-alarm,spring-boot-iot-system -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest,DevicePayloadApplyStageHandlerTest,DeviceStateStageHandlerTest,TelemetryPersistStageHandlerTest,UpMessageProcessingPipelineTest,DeviceMessageServiceImplTest,LegacyDpChildMessageSplitterTest,DefaultRiskMetricCatalogPublishRuleTest,RiskGovernanceWorkItemContributorTest,RiskGovernanceOpsServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/DeviceInsightView.test.ts src/__tests__/views/GovernanceControlPlaneViews.test.ts src/__tests__/views/RiskPointView.test.ts src/__tests__/views/RuleDefinitionView.test.ts
python -m unittest scripts.tests.test_run_real_env_schema_sync
git diff --check
```

Expected:

- all backend targeted suites pass
- all frontend targeted suites pass
- schema sync test passes
- `git diff --check` is clean

- [ ] **Step 3: Commit by work package**

Suggested commits:

```powershell
git commit -m "feat(device): harden collector child governance boundary"
git commit -m "feat(runtime): align collector child truth sources"
git commit -m "chore(data): seed collector child baseline"
git commit -m "feat(governance): close collector child risk semantics"
```

## Self-Review

Spec coverage:

1. The phase body is frozen as one integrated stage instead of a single endlessly expanding local fix.
2. All four work packages from the agreed stage body are preserved: governance boundary, runtime truth source, shared dev baseline, risk/control-plane closure.
3. Laser rangefinder, deep displacement, collector, and single-device deep displacement all remain in scope.
4. Each work package includes completion definition plus next functional points, so key follow-up ideas are preserved without being pulled into the current stage.

Placeholder scan:

1. No `TODO` or “implement later” placeholders are used.
2. Each task names the concrete modules and verification commands needed to execute the package.

Type consistency:

1. Collector-owned write-side fields remain runtime/status-only.
2. Child-owned formal fields remain `value / sensor_state` or `dispsX / dispsY / sensor_state`.
3. Deep displacement risk semantics are explicitly frozen in this phase as `dispsX / dispsY` publishable, `sensor_state` governance-only.
