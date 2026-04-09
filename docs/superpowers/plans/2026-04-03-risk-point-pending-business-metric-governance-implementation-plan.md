# Risk Point Pending Business Metric Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restrict `/api/risk-point/pending-bindings/*` to business-meaningful monitoring metrics defined by the spec, normalize vendor-prefixed fields to canonical metric identifiers, and prevent status parameters from being promoted into `risk_point_device`.

**Architecture:** Add a pending-governance-specific rules layer inside `spring-boot-iot-alarm` that classifies candidate fields by device context, filters out device-status telemetry, canonicalizes vendor aliases like `L1_SW_1.dispsX -> dispsX`, and reuses the same rules in both recommendation and promotion paths. Keep the existing frontend drawer structure and surface alias evidence through `reasonSummary`.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Jackson, Maven, Vue 3, TypeScript

---

## File Map

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingMetricGovernanceRules.java`
  - Encapsulate spec-driven filtering, alias normalization, and reason-summary helpers.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
  - Apply rules after collecting evidence and before sorting final candidates.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
  - Normalize submitted identifiers and block non-business metrics.
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
  - Add failing tests for status-parameter filtering and alias canonicalization.
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
  - Add failing test proving status parameters are rejected at promote time.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

## Task 1: Lock the Behavior with Tests

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`

- [ ] **Step 1: Add a failing recommendation test for deep-displacement status filtering**

Add a test that feeds a fixed inclinometer device with both `L1_SW_1.dispsX / L1_SW_1.dispsY` and status fields like `battery_dump_energy`, `signal_4g`, `temp`, `humidity`, `lat`, `lon`, `sw_version`, and assert only `dispsX / dispsY` survive.

- [ ] **Step 2: Run the recommendation test to verify it fails**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest#* -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test`

Expected: FAIL because current recommendation logic still leaves status parameters in the candidate list and does not canonicalize `L1_SW_1.dispsX`.

- [ ] **Step 3: Add a failing promotion test for status-parameter rejection**

Add a test that submits `battery_dump_energy` through `RiskPointPendingPromotionServiceImpl#promote` for a fixed inclinometer pending row and assert:
- no formal binding insert happens
- result item is `INVALID_METRIC`

- [ ] **Step 4: Run the promotion test to verify it fails**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingPromotionServiceImplTest#* -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test`

Expected: FAIL because current promote logic accepts any candidate that survives recommendation building.

## Task 2: Implement Spec-Driven Candidate Governance

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingMetricGovernanceRules.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`

- [ ] **Step 1: Create a dedicated rules helper**

Implement a small helper that:
- recognizes device-status fields from Table F.1
- recognizes canonical business metrics from the approved monitoring spec
- normalizes prefixed raw fields like `L1_SW_1.dispsX` to canonical identifiers
- builds explanation text like `深部位移规范测点；原始证据字段：L1_SW_1.dispsX`

- [ ] **Step 2: Apply the helper to final candidate assembly**

Update recommendation service so final candidates are produced by:
1. collecting evidence as today
2. normalizing aliases
3. removing disallowed status parameters
4. only keeping spec-valid business metrics for the detected device context

- [ ] **Step 3: Keep canonical metric identifier but preserve alias evidence**

When multiple raw identifiers map to the same canonical metric:
- merge them into one candidate
- keep canonical `metricIdentifier`
- prefer business-friendly `metricName`
- append raw aliases to `reasonSummary`

- [ ] **Step 4: Run the recommendation test suite**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test`

Expected: PASS

## Task 3: Reuse the Same Rules in Promote Path

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`

- [ ] **Step 1: Normalize submitted identifiers before candidate matching**

Apply the same rules helper so promote requests using raw identifiers like `L1_SW_1.dispsX` still resolve to canonical `dispsX`.

- [ ] **Step 2: Reject non-business metrics even if requested manually**

If a submitted metric is outside the allowed spec-driven set for the current device context, return `INVALID_METRIC` and skip formal binding writes.

- [ ] **Step 3: Ensure formal binding stores canonical metric identifiers**

When a metric is promoted successfully, write the canonical identifier to `risk_point_device.metric_identifier`, not the vendor-prefixed raw identifier.

- [ ] **Step 4: Run the promotion test suite**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingPromotionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test`

Expected: PASS

## Task 4: Update Documentation

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Document the new candidate-governance rule**

Explain that pending candidates now follow the monitoring-spec whitelist and no longer treat device-status parameters as formal risk-point metrics by default.

- [ ] **Step 2: Document alias normalization**

Explain that prefixed raw fields such as `L1_SW_1.dispsX` are shown as evidence but promoted as canonical metrics such as `dispsX`.

- [ ] **Step 3: Record the write-side guardrail**

Document that `/promote` rejects status parameters and other non-spec metrics with `INVALID_METRIC`.

## Task 5: Verify the Change End-to-End

**Files:**
- Modify: none

- [ ] **Step 1: Run focused backend verification**

Run: `mvn -pl spring-boot-iot-alarm -am -Dtest=RiskPointPendingRecommendationServiceImplTest,RiskPointPendingPromotionServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test`

Expected: PASS

- [ ] **Step 2: Run the packaging build**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 3: Real-environment smoke check**

Using `application-dev.yml`, verify at least one fixed inclinometer pending record shows only business displacement metrics in candidates and rejects a device-status metric if submitted manually.

- [ ] **Step 4: Summarize remaining manual-governance work**

Report which pending families still need human confirmation because the runtime evidence does not yet expose enough spec-valid business metrics.
