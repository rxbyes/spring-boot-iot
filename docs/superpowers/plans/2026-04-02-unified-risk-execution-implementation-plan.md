# Unified Risk Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current partial deep-displacement auto-closure chain into a reusable risk execution baseline that connects device reporting, risk-object binding, threshold strategy, linkage planning, emergency plans, alarm/event generation, and risk monitoring read models.

**Architecture:** Keep `DeviceRiskDispatchStageHandler` as the only pipeline entry from the device-reporting flow, then refactor the downstream alarm-side logic into explicit policy resolution, evaluation, and projection units. Preserve the current deep-displacement compatibility path, but move threshold evaluation from hardcoded `application-*.yml` values to runtime policy resolution backed by `rule_definition`, with YAML retained only as a fallback baseline.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, JUnit 5, Mockito, existing `application-dev.yml` real-environment baseline, existing backend acceptance scripts.

---

## Scope Split

This request spans three connected but separately shippable slices. Implement them in order:

1. **Backend runtime unification**
   Make `risk_point_device + rule_definition + linkage_rule + emergency_plan` participate in the same runtime chain.
2. **Read-side and governance visibility**
   Make risk-monitoring, insight, and access-side “missing governance” views consume the same runtime result instead of static archive fields.
3. **UI handoff and shared acceptance**
   Add cross-entry navigation and regression coverage so `接入智维 / 风险策略 / 风险运营` are one operable flow instead of isolated pages.

The first slice is the critical path. Do not start page-level handoff work until the backend runtime contract is stable.

## File Map

### New backend files

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyDecision.java`
  Carries the resolved threshold source, threshold values, alarm level, risk color, duration, and event-conversion flags for one metric evaluation.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolver.java`
  Resolves runtime policy in priority order: `risk_point_device` binding override -> enabled `rule_definition` -> `iot.alarm.auto-closure` fallback.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskRuntimeLevelResolver.java`
  Centralizes how read-side services derive displayed risk level from active alarms, recent event state, and fallback archive fields.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskGovernanceGapQuery.java`
  Query object for missing-binding / missing-policy governance lists.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskGovernanceGapItemVO.java`
  Read-side view object for governance gaps.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskGovernanceService.java`
  Service contract for governance-gap discovery.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
  Implementation that bridges `iot_device`, latest properties, risk bindings, and rule definitions.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskGovernanceController.java`
  New API surface for “待绑定对象 / 待配策略 / 待补联动” governance views.

### Modified backend files

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java`
  Replace direct YAML severity classification with `RiskPolicyResolver`; keep linkage-rule and emergency-plan matching in the same transaction.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
  Add runtime-safe validation so enabled threshold rules cannot be saved with invalid or non-executable expressions.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMonitoringServiceImpl.java`
  Stop using `risk_point.riskLevel` as the primary display source; derive runtime risk level through `RiskRuntimeLevelResolver`.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DeviceRiskDispatchStageHandler.java`
  Keep the current publish point, but ensure the runtime event payload carries the fields required by the new policy resolver and governance projections.

### New / modified test files

- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolverTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureServiceTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMonitoringServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DeviceRiskDispatchStageHandlerTest.java`

### Read-side / UI files for slice 3

- Modify: `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- Create: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

### Docs and acceptance files

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/15-前端优化与治理计划.md`

## Task 1: Make Threshold Strategy Executable

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyDecision.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolver.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/RiskPolicyResolverTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`

- [ ] **Step 1: Write the failing resolver test for rule-definition precedence**

```java
@Test
void resolveShouldPreferEnabledRuleDefinitionOverYamlFallback() {
    RuleDefinition rule = new RuleDefinition();
    rule.setMetricIdentifier("dispsX");
    rule.setExpression("value >= 12");
    rule.setAlarmLevel("critical");
    rule.setConvertToEvent(1);
    rule.setStatus(0);
    rule.setDeleted(0);

    when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

    RiskPolicyDecision decision = resolver.resolve(tenantId, binding, new BigDecimal("12.8"));

    assertEquals("RULE_DEFINITION", decision.getSource());
    assertEquals("critical", decision.getAlarmLevel());
    assertEquals("red", decision.getRiskColor());
    assertTrue(decision.shouldCreateEvent());
}
```

- [ ] **Step 2: Run the resolver test and verify it fails for the missing runtime resolver**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPolicyResolverTest test`

Expected: FAIL because `RiskPolicyResolver` and `RiskPolicyDecision` do not exist yet.

- [ ] **Step 3: Implement minimal runtime policy resolution**

```java
public RiskPolicyDecision resolve(Long tenantId, RiskPointDevice binding, BigDecimal absoluteValue) {
    Optional<RuleDefinition> matchedRule = listEnabledRules(tenantId, binding.getMetricIdentifier()).stream()
            .filter(rule -> expressionEvaluator.matches(rule.getExpression(), absoluteValue))
            .findFirst();
    if (matchedRule.isPresent()) {
        return RiskPolicyDecision.fromRule(matchedRule.get(), absoluteValue);
    }
    return RiskPolicyDecision.fromAutoClosure(binding, absoluteValue, iotProperties.getAlarm().getAutoClosure());
}
```

- [ ] **Step 4: Add save-time validation for executable threshold rules**

```java
private void validateExecutableRule(RuleDefinition rule) {
    if (rule.getStatus() != null && rule.getStatus() == 0 && !StringUtils.hasText(rule.getExpression())) {
        throw new BizException("启用中的阈值策略必须提供可执行表达式");
    }
}
```

- [ ] **Step 5: Re-run the targeted alarm-module test suite**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPolicyResolverTest,RuleDefinitionServiceImplTest test`

Expected: PASS for the new resolver tests; if `RuleDefinitionServiceImplTest` does not exist yet, create it before rerunning.

## Task 2: Refactor Auto-Closure to Consume Runtime Policy

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureService.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/auto/DeepDisplacementAutoClosureServiceTest.java`

- [ ] **Step 1: Write the failing auto-closure test for rule-driven severity**

```java
@Test
void processShouldUseResolvedRulePolicyInsteadOfStaticYamlThreshold() {
    when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(enabledRule("dispsX", "value >= 12", "critical")));
    mockCommonLookups(riskPoint, binding, buildProperty(binding, "12.8"));

    service.process(buildEvent("84330701", Map.of("dispsX", 12.8)));

    verify(alarmRecordService).addAlarm(alarmCaptor.capture());
    assertEquals("critical", alarmCaptor.getValue().getAlarmLevel());
    assertTrue(alarmCaptor.getValue().getRemark().contains("\"policySource\":\"RULE_DEFINITION\""));
}
```

- [ ] **Step 2: Run the auto-closure test and verify it fails**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DeepDisplacementAutoClosureServiceTest test`

Expected: FAIL because `DeepDisplacementAutoClosureService` still calls `AutoClosureSeverity.classify(...)` directly.

- [ ] **Step 3: Replace direct severity classification with policy resolution**

```java
RiskPolicyDecision decision = riskPolicyResolver.resolve(event.getTenantId(), binding, absoluteValue);
if (!decision.shouldCreateAlarm()) {
    return;
}
List<Map<String, Object>> matchedLinkageRules = matchLinkageRules(...);
EmergencyPlan matchedPlan = matchEmergencyPlan(...);
AlarmRecord alarmRecord = buildAlarmRecord(event, riskPoint, binding, absoluteValue, decision, matchedLinkageRules, matchedPlan);
```

- [ ] **Step 4: Persist policy source and resolved threshold into runtime evidence**

```java
remark.put("policySource", decision.getSource());
remark.put("threshold", decision.getThresholdText());
notes.put("policySource", decision.getSource());
notes.put("threshold", decision.getThresholdText());
```

- [ ] **Step 5: Re-run the targeted auto-closure suite**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=AutoClosureSeverityTest,DeepDisplacementAutoClosureServiceTest test`

Expected: PASS with existing red/orange/yellow compatibility preserved where no enabled rule definition exists.

## Task 3: Unify Risk Monitoring Display with Runtime State

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/auto/RiskRuntimeLevelResolver.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMonitoringServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMonitoringServiceImpl.java`

- [ ] **Step 1: Write the failing monitoring test for runtime risk-level projection**

```java
@Test
void listRealtimeItemsShouldPreferActiveAlarmLevelOverArchivedRiskPointLevel() {
    riskPoint.setRiskLevel("blue");
    when(alarmRecordMapper.selectList(any())).thenReturn(List.of(activeAlarm("critical")));

    PageResult<RiskMonitoringListItemVO> page = service.listRealtimeItems(new RiskMonitoringListQuery());

    assertEquals("red", page.getRows().get(0).getRiskLevel());
}
```

- [ ] **Step 2: Run the monitoring test and verify it fails**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMonitoringServiceImplTest test`

Expected: FAIL because `RiskMonitoringServiceImpl` still returns `riskPoint.getRiskLevel()`.

- [ ] **Step 3: Introduce a centralized runtime-level resolver**

```java
String runtimeRiskLevel = riskRuntimeLevelResolver.resolve(
        riskPoint,
        latestActiveAlarm,
        latestRecentEvent
);
item.setRiskLevel(runtimeRiskLevel);
point.setRiskLevel(runtimeRiskLevel);
detail.setRiskLevel(runtimeRiskLevel);
```

- [ ] **Step 4: Keep archive fields as fallback, not primary source**

```java
if (latestActiveAlarm != null) {
    return mapAlarmLevelToColor(latestActiveAlarm.getAlarmLevel());
}
if (latestRecentEvent != null && StringUtils.hasText(latestRecentEvent.getRiskLevel())) {
    return latestRecentEvent.getRiskLevel();
}
return riskPoint == null ? null : riskPoint.getRiskLevel();
```

- [ ] **Step 5: Re-run monitoring and auto-closure tests together**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DeepDisplacementAutoClosureServiceTest,RiskMonitoringServiceImplTest test`

Expected: PASS with runtime display now following the same risk-evaluation chain.

## Task 4: Expose Governance Gaps Between Access and Risk Modules

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskGovernanceGapQuery.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskGovernanceGapItemVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskGovernanceService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskGovernanceController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`

- [ ] **Step 1: Write failing service tests for governance-gap discovery**

```java
@Test
void listMissingBindingsShouldReturnDevicesWithTelemetryButNoRiskPointBinding() {
    when(deviceMapper.selectList(any())).thenReturn(List.of(device("demo-device-01")));
    when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());

    List<RiskGovernanceGapItemVO> items = service.listMissingBindings(query);

    assertEquals(1, items.size());
    assertEquals("demo-device-01", items.get(0).getDeviceCode());
}
```

- [ ] **Step 2: Run the gap-service tests and verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskGovernanceServiceImplTest,RiskGovernanceControllerTest test`

Expected: FAIL because the service/controller do not exist yet.

- [ ] **Step 3: Implement minimal governance-gap APIs**

```java
@GetMapping("/missing-bindings")
public R<PageResult<RiskGovernanceGapItemVO>> listMissingBindings(RiskGovernanceGapQuery query) {
    return R.ok(riskGovernanceService.listMissingBindings(query));
}
```

- [ ] **Step 4: Add a second endpoint for missing threshold policies**

```java
@GetMapping("/missing-policies")
public R<PageResult<RiskGovernanceGapItemVO>> listMissingPolicies(RiskGovernanceGapQuery query) {
    return R.ok(riskGovernanceService.listMissingPolicies(query));
}
```

- [ ] **Step 5: Re-run the new governance-gap tests**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskGovernanceServiceImplTest,RiskGovernanceControllerTest test`

Expected: PASS, with page items pointing operators from `接入智维` telemetry inventory into `风险策略` maintenance work.

## Task 5: Wire Shared UI Handoffs

**Files:**
- Create: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/api/riskMonitoring.ts`
- Modify: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`

- [ ] **Step 1: Add the missing-governance API client**

```ts
export function getMissingRiskBindings(params: RiskGovernanceGapQuery) {
  return request<PageResult<RiskGovernanceGapItem>>(`/api/risk-governance/missing-bindings?${toQuery(params)}`, {
    method: 'GET'
  });
}
```

- [ ] **Step 2: Add failing component-level tests or minimal page-state assertions**

Run: `node scripts/run-quality-gates.mjs`

Expected: FAIL or lint/type errors until the new API and page states are wired.

- [ ] **Step 3: Add cross-entry handoff blocks without introducing page-private layout patterns**

```ts
const governanceActions = [
  { key: 'bind-risk-point', label: '纳入风险对象', route: '/risk-point', query: { deviceCode } },
  { key: 'configure-policy', label: '补阈值策略', route: '/rule-definition', query: { metricIdentifier } }
];
```

- [ ] **Step 4: Keep pages on the shared `StandardPageShell + StandardWorkbenchPanel` contract**

```vue
<StandardWorkbenchPanel
  :toolbar-meta-items="toolbarMetaItems"
  :loading="loading"
>
  <template #notices>
    <RiskGovernanceNoticeCard v-if="missingGovernanceCount > 0" />
  </template>
</StandardWorkbenchPanel>
```

- [ ] **Step 5: Re-run frontend quality gates**

Run: `node scripts/run-quality-gates.mjs`

Expected: PASS with no UTF-8 corruption and no regression to retired KPI / Hero layout structures.

## Task 6: Real-Environment Acceptance and Doc Sync

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Run focused backend tests after each slice**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskPolicyResolverTest,DeepDisplacementAutoClosureServiceTest,RiskMonitoringServiceImplTest,RiskGovernanceServiceImplTest,RiskGovernanceControllerTest test`

Expected: PASS.

- [ ] **Step 2: Run cross-module package verification**

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run shared-dev backend acceptance**

Run: `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`

Expected: the shared `dev` profile boots and the risk-monitoring / risk-governance APIs are reachable without falling back to retired H2 acceptance paths.

- [ ] **Step 4: Run business smoke for the linked chain**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://127.0.0.1:9999`

Expected: smoke output confirms:
`设备上报 -> 风险分发 -> 风险对象绑定 -> 阈值策略命中 -> 联动/预案留痕 -> 风险监测读侧可见`.

- [ ] **Step 5: Sync docs to actual implemented behavior**

Update the docs so they state:
- `rule_definition` is now runtime policy input, not just CRUD master data.
- `risk-monitoring` runtime risk level comes from the unified runtime chain, with archive fields only as fallback.
- `StandardPageShell + StandardWorkbenchPanel` remains the list interaction baseline, fixing the stale wording at `docs/21-业务功能清单与验收标准.md:290`.

## Self-Review

- Coverage check: backend runtime, read-side projection, governance gap visibility, UI handoff, acceptance, and documentation are all mapped to explicit tasks.
- Placeholder scan: no `TODO` / `TBD` placeholders are left in the plan.
- Consistency check: runtime policy terminology is unified around `RiskPolicyResolver`, `RiskPolicyDecision`, and `RiskRuntimeLevelResolver`.

## Recommended Execution Order

1. Execute **Task 1** and **Task 2** first. This is the real business-core unification.
2. Execute **Task 3** immediately after. Without it, risk operations will still show stale archive levels.
3. Execute **Task 4** and **Task 5** only after the backend runtime contract is stable.
4. Run **Task 6** before calling the work complete.
