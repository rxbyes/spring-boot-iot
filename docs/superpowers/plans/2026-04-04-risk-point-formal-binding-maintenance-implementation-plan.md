# Risk Point Formal Binding Maintenance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add formal-binding summaries and a dedicated binding-maintenance drawer to `/risk-point` so operators can inspect, add, remove, and replace risk-point device metrics without confusing them with pending governance.

**Architecture:** Keep the existing pending-governance endpoints untouched, add a focused backend binding-maintenance service for summary reads and single-binding mutations, and move the new UI into a dedicated `riskPoint` drawer component so `RiskPointView.vue` only owns page-level orchestration. Use a batch summary endpoint for the list page to avoid N+1 requests, and keep “整机解绑” distinct from “单测点删除” all the way through controller, service, and UI copy.

**Tech Stack:** Spring Boot 4 / Java 17, MyBatis-Plus, Vue 3 + TypeScript, Element Plus, Vitest, JUnit 5, existing `StandardWorkbenchPanel` / `StandardFormDrawer` patterns.

---

## File Structure

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindingReplaceRequest.java`
  Responsibility: request model for replacing a single formal metric binding.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
  Responsibility: formal-binding summary, grouped-detail, remove, and replace contracts.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
  Responsibility: aggregate formal bindings, resolve binding source, remove one binding, and replace one metric within one risk point/device pair.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingSummaryVO.java`
  Responsibility: list-page summary payload for formal-device count, formal-metric count, and unresolved pending count.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingMetricVO.java`
  Responsibility: one formal binding row returned to the UI, including `bindingId`, metric identity, source, and create time.
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingDeviceGroupVO.java`
  Responsibility: group one device with all of its formal metric bindings.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
  Responsibility: expose summary, grouped-detail, single-binding remove, and single-binding replace endpoints with current-user extraction.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskPointDeviceMapper.java`
  Responsibility: stay as the single mapper for `risk_point_device`; if implementation remains wrapper-based, no custom SQL is needed.
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
  Responsibility: keep existing `bindDeviceAndReturn()` and `unbindDevice()` behavior available for the new maintenance service.
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
  Responsibility: lock summary/group/remove/replace behavior and ensure “单测点删除” never falls back to whole-device unbind.
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`
  Responsibility: prove the new controller endpoints extract the current user and delegate correctly.

- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
  Responsibility: expose summary, grouped-binding, remove-binding, and replace-binding contracts to Vue code.
- Create: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
  Responsibility: render the formal-binding maintenance drawer, including top-level add-binding flow, per-device cards, per-metric actions, and governance reminder copy.
- Create: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
  Responsibility: verify drawer rendering and action flows for add metric, whole-device unbind, remove metric, and replace metric.
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
  Responsibility: load batch summaries for the list page, rename the row action to “维护绑定”, and host the new drawer component.
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
  Responsibility: verify summary rendering, row-action rename, and drawer-open wiring.

- Modify: `docs/02-业务功能与流程说明.md`
  Responsibility: describe formal binding maintenance and the split from pending governance.
- Modify: `docs/03-接口规范与接口清单.md`
  Responsibility: document the new summary/group/remove/replace risk-point APIs.
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: record the formal-binding maintenance enhancement and the decision not to edit device master data in the risk-point page.
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: record the new shared drawer pattern and guard against pushing formal-binding details back into the list table.
- Modify: `docs/21-业务功能清单与验收标准.md`
  Responsibility: update acceptance criteria for summary visibility, formal-binding detail maintenance, and single-binding deletion semantics.

### Task 1: Add Backend Read Models For Binding Summaries And Grouped Formal Bindings

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingSummaryVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingMetricVO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingDeviceGroupVO.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`

- [ ] **Step 1: Write the failing read-side tests**

Create the new service test and controller test with these assertions before adding implementation:

```java
@Test
void listBindingSummariesShouldAggregateFormalBindingsAndPendingCounts() {
    Fixture fixture = new Fixture();
    when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(fixture.riskPoint(12L, "RP-OPS-001"));
    when(fixture.riskPointService.getById(13L, 1001L)).thenReturn(fixture.riskPoint(13L, "RP-OPS-002"));
    when(fixture.riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
            fixture.binding(801L, 12L, 2001L, "DEV-01", "angle", "倾角"),
            fixture.binding(802L, 12L, 2001L, "DEV-01", "AZI", "方位角"),
            fixture.binding(803L, 12L, 2002L, "DEV-02", "gX", "加速度X"),
            fixture.binding(804L, 13L, 2003L, "DEV-03", "value", "雨量")
    ));
    when(fixture.pendingBindingMapper.selectList(any())).thenReturn(List.of(
            fixture.pending(901L, 12L, "PENDING_METRIC_GOVERNANCE"),
            fixture.pending(902L, 12L, "PARTIALLY_PROMOTED")
    ));

    List<RiskPointBindingSummaryVO> result = fixture.service.listBindingSummaries(List.of(12L, 13L), 1001L);

    assertEquals(2, result.size());
    assertEquals(2, result.get(0).getBoundDeviceCount());
    assertEquals(3, result.get(0).getBoundMetricCount());
    assertEquals(2, result.get(0).getPendingBindingCount());
    assertEquals(1, result.get(1).getBoundDeviceCount());
    assertEquals(1, result.get(1).getBoundMetricCount());
    assertEquals(0, result.get(1).getPendingBindingCount());
}

@Test
void listBindingGroupsShouldGroupMetricsByDeviceAndMarkPromotionSource() {
    Fixture fixture = new Fixture();
    when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(fixture.riskPoint(12L, "RP-OPS-001"));
    when(fixture.riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
            fixture.binding(801L, 12L, 2001L, "DEV-01", "angle", "倾角"),
            fixture.binding(802L, 12L, 2001L, "DEV-01", "AZI", "方位角"),
            fixture.binding(803L, 12L, 2002L, "DEV-02", "gX", "加速度X")
    ));
    when(fixture.promotionMapper.selectList(any())).thenReturn(List.of(
            fixture.promotion(7001L, 802L),
            fixture.promotion(7002L, 803L)
    ));

    List<RiskPointBindingDeviceGroupVO> groups = fixture.service.listBindingGroups(12L, 1001L);

    assertEquals(2, groups.size());
    assertEquals("DEV-01", groups.get(0).getDeviceCode());
    assertEquals(2, groups.get(0).getMetricCount());
    assertEquals("MANUAL", groups.get(0).getMetrics().get(0).getBindingSource());
    assertEquals("PENDING_PROMOTION", groups.get(0).getMetrics().get(1).getBindingSource());
}
```

```java
@Test
void listBindingSummariesShouldExtractCurrentUserAndDelegate() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointBindingMaintenanceService bindingMaintenanceService = mock(RiskPointBindingMaintenanceService.class);
    RiskPointController controller = new RiskPointController(riskPointService, bindingMaintenanceService);
    when(bindingMaintenanceService.listBindingSummaries(List.of(12L, 13L), 1001L)).thenReturn(List.of());

    R<List<RiskPointBindingSummaryVO>> response = controller.listBindingSummaries(List.of(12L, 13L), authentication(1001L));

    assertEquals(200, response.getCode());
    verify(bindingMaintenanceService).listBindingSummaries(List.of(12L, 13L), 1001L);
}

@Test
void listBindingGroupsShouldExtractCurrentUserAndDelegate() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointBindingMaintenanceService bindingMaintenanceService = mock(RiskPointBindingMaintenanceService.class);
    RiskPointController controller = new RiskPointController(riskPointService, bindingMaintenanceService);
    when(bindingMaintenanceService.listBindingGroups(12L, 1001L)).thenReturn(List.of());

    R<List<RiskPointBindingDeviceGroupVO>> response = controller.listBindingGroups(12L, authentication(1001L));

    assertEquals(200, response.getCode());
    verify(bindingMaintenanceService).listBindingGroups(12L, 1001L);
}
```

- [ ] **Step 2: Run the backend read-side tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: FAIL because the new service, VO classes, controller constructor overload, and read endpoints do not exist yet.

- [ ] **Step 3: Add the summary/group VO classes and maintenance-service interface**

Create the simple read models and contract:

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingSummaryVO.java
@Data
public class RiskPointBindingSummaryVO {

    private Long riskPointId;

    private Integer boundDeviceCount;

    private Integer boundMetricCount;

    private Integer pendingBindingCount;
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingMetricVO.java
@Data
public class RiskPointBindingMetricVO {

    private Long bindingId;

    private String metricIdentifier;

    private String metricName;

    private String bindingSource;

    private Date createTime;
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingDeviceGroupVO.java
@Data
public class RiskPointBindingDeviceGroupVO {

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Integer metricCount;

    private List<RiskPointBindingMetricVO> metrics;
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java
public interface RiskPointBindingMaintenanceService {

    List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId);

    List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId);
}
```

- [ ] **Step 4: Implement the read-side maintenance service and controller endpoints**

Implement wrapper-based aggregation so no XML mapper is needed:

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java
@Service
public class RiskPointBindingMaintenanceServiceImpl implements RiskPointBindingMaintenanceService {

    private static final Set<String> UNRESOLVED_PENDING_STATUSES = Set.of("PENDING_METRIC_GOVERNANCE", "PARTIALLY_PROMOTED");
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_PENDING_PROMOTION = "PENDING_PROMOTION";

    @Override
    public List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId) {
        List<Long> normalizedIds = (riskPointIds == null ? List.<Long>of() : riskPointIds).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        normalizedIds.forEach(riskPointId -> riskPointService.getById(riskPointId, currentUserId));

        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .in(RiskPointDevice::getRiskPointId, normalizedIds)
                .eq(RiskPointDevice::getDeleted, 0));
        List<RiskPointDevicePendingBinding> pendingRows = pendingBindingMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingBinding>()
                .in(RiskPointDevicePendingBinding::getRiskPointId, normalizedIds)
                .in(RiskPointDevicePendingBinding::getResolutionStatus, UNRESOLVED_PENDING_STATUSES)
                .eq(RiskPointDevicePendingBinding::getDeleted, 0));

        Map<Long, Set<Long>> deviceIdsByRiskPoint = new HashMap<>();
        Map<Long, Integer> metricCountByRiskPoint = new HashMap<>();
        for (RiskPointDevice binding : bindings) {
            deviceIdsByRiskPoint.computeIfAbsent(binding.getRiskPointId(), key -> new LinkedHashSet<>()).add(binding.getDeviceId());
            metricCountByRiskPoint.merge(binding.getRiskPointId(), 1, Integer::sum);
        }
        Map<Long, Integer> pendingCountByRiskPoint = new HashMap<>();
        for (RiskPointDevicePendingBinding pending : pendingRows) {
            pendingCountByRiskPoint.merge(pending.getRiskPointId(), 1, Integer::sum);
        }

        return normalizedIds.stream().map(riskPointId -> {
            RiskPointBindingSummaryVO item = new RiskPointBindingSummaryVO();
            item.setRiskPointId(riskPointId);
            item.setBoundDeviceCount(deviceIdsByRiskPoint.getOrDefault(riskPointId, Set.of()).size());
            item.setBoundMetricCount(metricCountByRiskPoint.getOrDefault(riskPointId, 0));
            item.setPendingBindingCount(pendingCountByRiskPoint.getOrDefault(riskPointId, 0));
            return item;
        }).toList();
    }

    @Override
    public List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId) {
        riskPointService.getById(riskPointId, currentUserId);
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getRiskPointId, riskPointId)
                .eq(RiskPointDevice::getDeleted, 0)
                .orderByAsc(RiskPointDevice::getDeviceCode)
                .orderByAsc(RiskPointDevice::getMetricIdentifier));
        if (bindings.isEmpty()) {
            return List.of();
        }

        Set<Long> bindingIds = bindings.stream().map(RiskPointDevice::getId).collect(Collectors.toSet());
        Set<Long> promotedBindingIds = promotionMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingPromotion>()
                        .in(RiskPointDevicePendingPromotion::getRiskPointDeviceId, bindingIds)
                        .eq(RiskPointDevicePendingPromotion::getDeleted, 0))
                .stream()
                .map(RiskPointDevicePendingPromotion::getRiskPointDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<RiskPointDevice>> bindingsByDevice = bindings.stream()
                .collect(Collectors.groupingBy(RiskPointDevice::getDeviceId, LinkedHashMap::new, Collectors.toList()));
        List<RiskPointBindingDeviceGroupVO> groups = new ArrayList<>();
        for (List<RiskPointDevice> sameDeviceBindings : bindingsByDevice.values()) {
            RiskPointDevice first = sameDeviceBindings.get(0);
            RiskPointBindingDeviceGroupVO group = new RiskPointBindingDeviceGroupVO();
            group.setDeviceId(first.getDeviceId());
            group.setDeviceCode(first.getDeviceCode());
            group.setDeviceName(first.getDeviceName());
            group.setMetricCount(sameDeviceBindings.size());
            group.setMetrics(sameDeviceBindings.stream().map(binding -> {
                RiskPointBindingMetricVO metric = new RiskPointBindingMetricVO();
                metric.setBindingId(binding.getId());
                metric.setMetricIdentifier(binding.getMetricIdentifier());
                metric.setMetricName(binding.getMetricName());
                metric.setBindingSource(promotedBindingIds.contains(binding.getId()) ? SOURCE_PENDING_PROMOTION : SOURCE_MANUAL);
                metric.setCreateTime(binding.getCreateTime());
                return metric;
            }).toList());
            groups.add(group);
        }
        return groups;
    }
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java
private final RiskPointBindingMaintenanceService bindingMaintenanceService;

public RiskPointController(RiskPointService riskPointService,
                           RiskPointBindingMaintenanceService bindingMaintenanceService) {
    this.riskPointService = riskPointService;
    this.bindingMaintenanceService = bindingMaintenanceService;
}

@GetMapping("/binding-summaries")
public R<List<RiskPointBindingSummaryVO>> listBindingSummaries(@RequestParam List<Long> riskPointIds,
                                                               Authentication authentication) {
    return R.ok(bindingMaintenanceService.listBindingSummaries(riskPointIds, requireCurrentUserId(authentication)));
}

@GetMapping("/binding-groups/{riskPointId}")
public R<List<RiskPointBindingDeviceGroupVO>> listBindingGroups(@PathVariable Long riskPointId,
                                                                Authentication authentication) {
    return R.ok(bindingMaintenanceService.listBindingGroups(riskPointId, requireCurrentUserId(authentication)));
}
```

- [ ] **Step 5: Run the backend read-side tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: PASS with the summary aggregation and grouped-binding endpoint assertions green.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingSummaryVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingMetricVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskPointBindingDeviceGroupVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "feat: add risk point binding summary APIs"
```

### Task 2: Add Backend Single-Binding Remove And Replace Actions

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindingReplaceRequest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`

- [ ] **Step 1: Write the failing mutation tests**

Add these cases to the same test files:

```java
@Test
void removeBindingShouldDeleteOnlyTargetMetricBinding() {
    Fixture fixture = new Fixture();
    RiskPointDevice binding = fixture.binding(801L, 12L, 2001L, "DEV-01", "angle", "倾角");
    when(fixture.riskPointDeviceMapper.selectById(801L)).thenReturn(binding);
    when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(fixture.riskPoint(12L, "RP-OPS-001"));

    fixture.service.removeBinding(801L, 1001L);

    verify(fixture.riskPointDeviceMapper).deleteById(801L);
    verify(fixture.riskPointService, never()).unbindDevice(12L, 2001L, 1001L);
}

@Test
void replaceBindingMetricShouldCreateNewBindingAndDeleteOldBinding() {
    Fixture fixture = new Fixture();
    RiskPointDevice oldBinding = fixture.binding(801L, 12L, 2001L, "DEV-01", "angle", "倾角");
    when(fixture.riskPointDeviceMapper.selectById(801L)).thenReturn(oldBinding);
    when(fixture.riskPointService.getById(12L, 1001L)).thenReturn(fixture.riskPoint(12L, "RP-OPS-001"));
    when(fixture.riskPointDeviceMapper.selectOne(any())).thenReturn(null);
    when(fixture.riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), eq(1001L))).thenAnswer(invocation -> {
        RiskPointDevice saved = invocation.getArgument(0);
        saved.setId(9501L);
        return saved;
    });

    RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
    request.setMetricIdentifier("AZI");
    request.setMetricName("方位角");

    RiskPointBindingMetricVO result = fixture.service.replaceBindingMetric(801L, request, 1001L);

    assertEquals(9501L, result.getBindingId());
    assertEquals("AZI", result.getMetricIdentifier());
    verify(fixture.riskPointDeviceMapper).deleteById(801L);
}
```

```java
@Test
void removeBindingShouldExtractCurrentUserAndDelegate() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointBindingMaintenanceService bindingMaintenanceService = mock(RiskPointBindingMaintenanceService.class);
    RiskPointController controller = new RiskPointController(riskPointService, bindingMaintenanceService);

    R<Void> response = controller.removeBinding(801L, authentication(1001L));

    assertEquals(200, response.getCode());
    verify(bindingMaintenanceService).removeBinding(801L, 1001L);
}

@Test
void replaceBindingMetricShouldExtractCurrentUserAndDelegate() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointBindingMaintenanceService bindingMaintenanceService = mock(RiskPointBindingMaintenanceService.class);
    RiskPointController controller = new RiskPointController(riskPointService, bindingMaintenanceService);
    RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
    request.setMetricIdentifier("AZI");
    request.setMetricName("方位角");
    RiskPointBindingMetricVO result = new RiskPointBindingMetricVO();
    result.setBindingId(9501L);
    when(bindingMaintenanceService.replaceBindingMetric(801L, request, 1001L)).thenReturn(result);

    R<RiskPointBindingMetricVO> response = controller.replaceBindingMetric(801L, request, authentication(1001L));

    assertEquals(9501L, response.getData().getBindingId());
    verify(bindingMaintenanceService).replaceBindingMetric(801L, request, 1001L);
}
```

- [ ] **Step 2: Run the backend mutation tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: FAIL because remove/replace methods, request DTO, and controller endpoints do not exist yet.

- [ ] **Step 3: Add the replace-request DTO and service mutation logic**

Create the request type and keep replace semantics transactional:

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindingReplaceRequest.java
@Data
public class RiskPointBindingReplaceRequest {

    private String metricIdentifier;

    private String metricName;
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java
public interface RiskPointBindingMaintenanceService {

    List<RiskPointBindingSummaryVO> listBindingSummaries(List<Long> riskPointIds, Long currentUserId);

    List<RiskPointBindingDeviceGroupVO> listBindingGroups(Long riskPointId, Long currentUserId);

    void removeBinding(Long bindingId, Long currentUserId);

    RiskPointBindingMetricVO replaceBindingMetric(Long bindingId, RiskPointBindingReplaceRequest request, Long currentUserId);
}
```

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java
@Override
@Transactional(rollbackFor = Exception.class)
public void removeBinding(Long bindingId, Long currentUserId) {
    RiskPointDevice binding = requireBinding(bindingId);
    riskPointService.getById(binding.getRiskPointId(), currentUserId);
    if (riskPointDeviceMapper.deleteById(bindingId) != 1) {
        throw new BizException("正式绑定删除失败");
    }
}

@Override
@Transactional(rollbackFor = Exception.class)
public RiskPointBindingMetricVO replaceBindingMetric(Long bindingId, RiskPointBindingReplaceRequest request, Long currentUserId) {
    RiskPointDevice oldBinding = requireBinding(bindingId);
    riskPointService.getById(oldBinding.getRiskPointId(), currentUserId);
    String nextMetricIdentifier = normalizeRequiredMetricIdentifier(request);
    if (nextMetricIdentifier.equals(oldBinding.getMetricIdentifier())) {
        throw new BizException("请选择新的测点");
    }
    RiskPointDevice duplicate = riskPointDeviceMapper.selectOne(new LambdaQueryWrapper<RiskPointDevice>()
            .eq(RiskPointDevice::getRiskPointId, oldBinding.getRiskPointId())
            .eq(RiskPointDevice::getDeviceId, oldBinding.getDeviceId())
            .eq(RiskPointDevice::getMetricIdentifier, nextMetricIdentifier)
            .eq(RiskPointDevice::getDeleted, 0));
    if (duplicate != null) {
        throw new BizException("测点已绑定到该风险点设备");
    }

    RiskPointDevice nextBinding = new RiskPointDevice();
    nextBinding.setRiskPointId(oldBinding.getRiskPointId());
    nextBinding.setDeviceId(oldBinding.getDeviceId());
    nextBinding.setMetricIdentifier(nextMetricIdentifier);
    nextBinding.setMetricName(StringUtils.hasText(request.getMetricName()) ? request.getMetricName().trim() : nextMetricIdentifier);
    RiskPointDevice saved = riskPointService.bindDeviceAndReturn(nextBinding, currentUserId);
    if (riskPointDeviceMapper.deleteById(bindingId) != 1) {
        throw new BizException("旧正式绑定删除失败");
    }
    return toMetric(saved, SOURCE_MANUAL);
}

private RiskPointDevice requireBinding(Long bindingId) {
    RiskPointDevice binding = riskPointDeviceMapper.selectById(bindingId);
    if (binding == null || Integer.valueOf(1).equals(binding.getDeleted())) {
        throw new BizException("正式绑定不存在");
    }
    return binding;
}

private String normalizeRequiredMetricIdentifier(RiskPointBindingReplaceRequest request) {
    String value = request == null ? null : request.getMetricIdentifier();
    if (!StringUtils.hasText(value)) {
        throw new BizException("请选择新的测点");
    }
    return value.trim();
}

private RiskPointBindingMetricVO toMetric(RiskPointDevice binding, String bindingSource) {
    RiskPointBindingMetricVO item = new RiskPointBindingMetricVO();
    item.setBindingId(binding.getId());
    item.setMetricIdentifier(binding.getMetricIdentifier());
    item.setMetricName(binding.getMetricName());
    item.setBindingSource(bindingSource);
    item.setCreateTime(binding.getCreateTime());
    return item;
}
```

- [ ] **Step 4: Wire the new controller endpoints**

Add the single-binding mutation routes to the existing controller:

```java
// spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java
@PostMapping("/bindings/{bindingId}/remove")
public R<Void> removeBinding(@PathVariable Long bindingId, Authentication authentication) {
    bindingMaintenanceService.removeBinding(bindingId, requireCurrentUserId(authentication));
    return R.ok();
}

@PostMapping("/bindings/{bindingId}/replace")
public R<RiskPointBindingMetricVO> replaceBindingMetric(@PathVariable Long bindingId,
                                                        @RequestBody RiskPointBindingReplaceRequest request,
                                                        Authentication authentication) {
    return R.ok(bindingMaintenanceService.replaceBindingMetric(bindingId, request, requireCurrentUserId(authentication)));
}
```

- [ ] **Step 5: Run the backend mutation tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: PASS with green assertions for single-binding delete and transactional metric replacement.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindingReplaceRequest.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "feat: add risk point binding mutation APIs"
```

### Task 3: Show Binding Summaries In The Risk-Point List And Open A Dedicated Maintenance Drawer

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Create: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write the failing list-summary and drawer-entry tests**

Extend the view test to assert summary text and the renamed row action:

```ts
it('loads binding summaries for the current page and renders the summary column', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [createRiskPointRow()]
    }
  })
  mockListBindingSummaries.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        riskPointId: 1,
        boundDeviceCount: 2,
        boundMetricCount: 5,
        pendingBindingCount: 1
      }
    ]
  })

  const wrapper = mountView()
  await flushPromises()

  expect(mockListBindingSummaries).toHaveBeenCalledWith([1])
  expect(wrapper.text()).toContain('2 台设备')
  expect(wrapper.text()).toContain('5 个测点')
  expect(wrapper.text()).toContain('待治理 1 条')
  expect((wrapper.vm as any).getRiskPointRowActions().map((item: { label: string }) => item.label)).toContain('维护绑定')
})

it('opens the binding maintenance drawer instead of the legacy bind drawer entry', async () => {
  mockPageRiskPointList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [createRiskPointRow()]
    }
  })
  mockListBindingSummaries.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [{ riskPointId: 1, boundDeviceCount: 0, boundMetricCount: 0, pendingBindingCount: 0 }]
  })

  const wrapper = mountView()
  await flushPromises()
  ;(wrapper.vm as any).handleRiskPointRowAction('maintain-binding', createRiskPointRow())
  await flushPromises()

  expect(wrapper.text()).toContain('维护绑定')
  expect((wrapper.vm as any).bindingMaintenanceVisible).toBe(true)
  expect((wrapper.vm as any).bindingMaintenanceRiskPoint?.riskPointName).toBe('示例风险点')
})
```

- [ ] **Step 2: Run the view test to verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/views/RiskPointView.test.ts --run
```

Expected: FAIL because the summary API mock, summary column, `maintain-binding` action, and drawer state do not exist yet.

- [ ] **Step 3: Extend the risk-point API module with summary and grouped-binding contracts**

Add the types and read functions first:

```ts
// spring-boot-iot-ui/src/api/riskPoint.ts
export interface RiskPointBindingSummary {
  riskPointId: IdType;
  boundDeviceCount: number;
  boundMetricCount: number;
  pendingBindingCount: number;
}

export interface RiskPointBindingMetric {
  bindingId: IdType;
  metricIdentifier: string;
  metricName?: string | null;
  bindingSource: 'MANUAL' | 'PENDING_PROMOTION' | 'UNKNOWN';
  createTime?: string | null;
}

export interface RiskPointBindingDeviceGroup {
  deviceId: IdType;
  deviceCode: string;
  deviceName: string;
  metricCount: number;
  metrics: RiskPointBindingMetric[];
}

export const listBindingSummaries = (riskPointIds: Array<IdType>): Promise<ApiEnvelope<RiskPointBindingSummary[]>> => {
  const queryString = buildQueryString({ riskPointIds });
  return request<RiskPointBindingSummary[]>(`/api/risk-point/binding-summaries?${queryString}`, { method: 'GET' });
};

export const listBindingGroups = (riskPointId: IdType): Promise<ApiEnvelope<RiskPointBindingDeviceGroup[]>> => {
  return request<RiskPointBindingDeviceGroup[]>(`/api/risk-point/binding-groups/${riskPointId}`, { method: 'GET' });
};
```

- [ ] **Step 4: Replace the row action and add the summary column plus drawer shell**

Create a minimal drawer shell and wire it into the list page:

```vue
<!-- spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue -->
<template>
  <StandardFormDrawer
    v-model="visible"
    title="维护绑定"
    subtitle="查看正式绑定摘要，并继续维护设备与测点关系。"
    size="48rem"
  >
    <div class="ops-drawer-stack">
      <div class="ops-drawer-note">
        <strong>维护提示</strong>
        <span>这里维护的是风险点正式绑定关系，不是设备主档信息。</span>
      </div>
      <EmptyState
        v-if="!riskPointId"
        title="未选择风险点"
        description="请从风险点列表选择一条记录后再维护绑定。"
      />
    </div>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'

const props = defineProps<{
  modelValue: boolean
  riskPointId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>
```

```ts
// spring-boot-iot-ui/src/views/RiskPointView.vue
type RiskPointRowActionCommand = 'edit' | 'maintain-binding' | 'pending-promotion' | 'delete';

const bindingSummaryMap = ref<Record<number, RiskPointBindingSummary>>({})
const bindingMaintenanceVisible = ref(false)
const bindingMaintenanceRiskPoint = ref<RiskPoint | null>(null)

const getRiskPointRowActions = () => [
  { command: 'edit' as const, label: '编辑' },
  { command: 'maintain-binding' as const, label: '维护绑定' },
  { command: 'pending-promotion' as const, label: '待治理转正' },
  { command: 'delete' as const, label: '删除' }
]

const loadBindingSummaries = async () => {
  const ids = riskPointList.value.map((item) => Number(item.id)).filter(Boolean)
  if (ids.length === 0) {
    bindingSummaryMap.value = {}
    return
  }
  const res = await listBindingSummaries(ids)
  if (res.code === 200) {
    bindingSummaryMap.value = Object.fromEntries((res.data || []).map((item) => [Number(item.riskPointId), item]))
  }
}

const openBindingMaintenance = (row: RiskPoint) => {
  bindingMaintenanceRiskPoint.value = row
  bindingMaintenanceVisible.value = true
}
```

```vue
<!-- spring-boot-iot-ui/src/views/RiskPointView.vue -->
<el-table-column label="绑定概览" min-width="220">
  <template #default="{ row }">
    <div class="risk-point-binding-summary">
      <span>{{ bindingSummaryMap[Number(row.id)]?.boundDeviceCount ?? 0 }} 台设备</span>
      <span>{{ bindingSummaryMap[Number(row.id)]?.boundMetricCount ?? 0 }} 个测点</span>
      <span>待治理 {{ bindingSummaryMap[Number(row.id)]?.pendingBindingCount ?? 0 }} 条</span>
    </div>
  </template>
</el-table-column>

<RiskPointBindingMaintenanceDrawer
  v-model="bindingMaintenanceVisible"
  :risk-point-id="bindingMaintenanceRiskPoint ? Number(bindingMaintenanceRiskPoint.id) : null"
/>
```

- [ ] **Step 5: Run the view test to verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/views/RiskPointView.test.ts --run
```

Expected: PASS with the summary column and “维护绑定” row action assertions green.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: show risk point binding summaries"
```

### Task 4: Implement The Formal-Binding Maintenance Drawer Flows

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Write the failing drawer tests for add, whole-device unbind, remove metric, and replace metric**

Create the new component test with explicit API expectations:

```ts
it('renders grouped bindings and shows source badges', async () => {
  mockListBindingGroups.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        deviceId: 2001,
        deviceCode: 'DEV-01',
        deviceName: '北坡倾角仪',
        metricCount: 2,
        metrics: [
          { bindingId: 801, metricIdentifier: 'angle', metricName: '倾角', bindingSource: 'MANUAL' },
          { bindingId: 802, metricIdentifier: 'AZI', metricName: '方位角', bindingSource: 'PENDING_PROMOTION' }
        ]
      }
    ]
  })

  const wrapper = mountDrawer()
  await flushPromises()

  expect(wrapper.text()).toContain('DEV-01')
  expect(wrapper.text()).toContain('倾角')
  expect(wrapper.text()).toContain('PENDING_PROMOTION')
})

it('adds a formal metric binding through the drawer add form', async () => {
  mockListBindingGroups.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: []
  })
  mockListBindableDevices.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        id: 2001,
        productId: 1001,
        deviceCode: 'DEV-01',
        deviceName: '北坡倾角仪',
        orgId: 7101,
        orgName: '平台运维中心'
      }
    ]
  })
  mockGetDeviceMetricOptions.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      { identifier: 'angle', name: '倾角' },
      { identifier: 'AZI', name: '方位角' }
    ]
  })
  mockBindDevice.mockResolvedValueOnce({ code: 200, msg: 'success', data: undefined })

  const wrapper = mountDrawer()
  await flushPromises()
  ;(wrapper.vm as any).addForm.deviceId = 2001
  await (wrapper.vm as any).loadAddMetricOptions(2001)
  ;(wrapper.vm as any).addForm.metricIdentifier = 'angle'

  await (wrapper.vm as any).handleAddMetricSubmit()

  expect(mockBindDevice).toHaveBeenCalledWith(
    expect.objectContaining({
      riskPointId: 1,
      deviceId: 2001,
      metricIdentifier: 'angle'
    })
  )
})

it('removes a single binding without calling whole-device unbind', async () => {
  mockListBindingGroups.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        deviceId: 2001,
        deviceCode: 'DEV-01',
        deviceName: '北坡倾角仪',
        metricCount: 2,
        metrics: [
          { bindingId: 801, metricIdentifier: 'angle', metricName: '倾角', bindingSource: 'MANUAL' },
          { bindingId: 802, metricIdentifier: 'AZI', metricName: '方位角', bindingSource: 'PENDING_PROMOTION' }
        ]
      }
    ]
  })
  mockRemoveBinding.mockResolvedValueOnce({ code: 200, msg: 'success', data: undefined })

  const wrapper = mountDrawer()
  await flushPromises()
  await (wrapper.vm as any).handleRemoveMetric({ bindingId: 801, metricIdentifier: 'angle', metricName: '倾角' })

  expect(mockRemoveBinding).toHaveBeenCalledWith(801)
  expect(mockUnbindDevice).not.toHaveBeenCalled()
})

it('replaces a metric binding with the selected target metric', async () => {
  mockListBindingGroups.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        deviceId: 2001,
        deviceCode: 'DEV-01',
        deviceName: '北坡倾角仪',
        metricCount: 2,
        metrics: [
          { bindingId: 801, metricIdentifier: 'angle', metricName: '倾角', bindingSource: 'MANUAL' },
          { bindingId: 802, metricIdentifier: 'AZI', metricName: '方位角', bindingSource: 'PENDING_PROMOTION' }
        ]
      }
    ]
  })
  mockGetDeviceMetricOptions.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      { identifier: 'AZI', name: '方位角' },
      { identifier: 'angle', name: '倾角' }
    ]
  })
  mockReplaceBinding.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: { bindingId: 9501, metricIdentifier: 'AZI', metricName: '方位角', bindingSource: 'MANUAL' }
  })

  const wrapper = mountDrawer()
  await flushPromises()
  ;(wrapper.vm as any).replaceForm.bindingId = 801
  ;(wrapper.vm as any).replaceForm.deviceId = 2001
  ;(wrapper.vm as any).replaceForm.metricIdentifier = 'AZI'
  ;(wrapper.vm as any).replaceForm.metricName = '方位角'

  await (wrapper.vm as any).handleReplaceMetricSubmit()

  expect(mockReplaceBinding).toHaveBeenCalledWith(801, {
    metricIdentifier: 'AZI',
    metricName: '方位角'
  })
})
```

- [ ] **Step 2: Run the drawer-focused UI tests to verify they fail**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/views/RiskPointView.test.ts --run
```

Expected: FAIL because grouped-binding loading, remove/replace APIs, top-level state, and drawer actions are not implemented yet.

- [ ] **Step 3: Add the grouped-binding, remove, and replace APIs plus full drawer behavior**

Finish the API surface first:

```ts
// spring-boot-iot-ui/src/api/riskPoint.ts
export interface RiskPointBindingReplaceRequest {
  metricIdentifier: string;
  metricName: string;
}

export const removeBinding = (bindingId: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/risk-point/bindings/${bindingId}/remove`, { method: 'POST' });
};

export const replaceBinding = (bindingId: IdType, body: RiskPointBindingReplaceRequest): Promise<ApiEnvelope<RiskPointBindingMetric>> => {
  return request<RiskPointBindingMetric>(`/api/risk-point/bindings/${bindingId}/replace`, { method: 'POST', body });
};
```

Then make the drawer own the maintenance flow:

```vue
<!-- spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from '@/utils/message'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import { bindDevice, listBindingGroups, listBindableDevices, removeBinding, replaceBinding, unbindDevice } from '@/api/riskPoint'
import { getDeviceMetricOptions } from '@/api/iot'

const props = defineProps<{
  modelValue: boolean
  riskPointId?: number | null
  riskPointName?: string | null
  riskPointCode?: string | null
  orgName?: string | null
  pendingBindingCount?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  updated: []
}>()

const groups = ref<RiskPointBindingDeviceGroup[]>([])
const loading = ref(false)
const actionLoading = ref(false)
const deviceOptions = ref<DeviceOption[]>([])
const addForm = reactive({ deviceId: 0, metricIdentifier: '', metricName: '' })
const addMetricOptions = ref<DeviceMetricOption[]>([])
const replaceForm = reactive({ bindingId: 0, deviceId: 0, metricIdentifier: '', metricName: '' })
const replaceMetricOptions = ref<DeviceMetricOption[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const loadGroups = async () => {
  if (!props.riskPointId) {
    groups.value = []
    return
  }
  loading.value = true
  try {
    const [groupRes, deviceRes] = await Promise.all([
      listBindingGroups(props.riskPointId),
      listBindableDevices(props.riskPointId)
    ])
    groups.value = groupRes.code === 200 ? (groupRes.data || []) : []
    deviceOptions.value = deviceRes.code === 200 ? (deviceRes.data || []) : []
  } finally {
    loading.value = false
  }
}

const handleWholeDeviceUnbind = async (group: RiskPointBindingDeviceGroup) => {
  if (!props.riskPointId) return
  try {
    await confirmAction({
      title: '整机解绑',
      message: `将删除设备 ${group.deviceCode} 在当前风险点下的全部正式测点绑定，是否继续？`,
      confirmButtonText: '确认解绑'
    })
    actionLoading.value = true
    const res = await unbindDevice(props.riskPointId, group.deviceId)
    if (res.code === 200) {
      ElMessage.success('整机解绑成功')
      await loadGroups()
      emit('updated')
    }
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      ElMessage.error(error instanceof Error ? error.message : '整机解绑失败')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleRemoveMetric = async (metric: RiskPointBindingMetric) => {
  try {
    await confirmAction({
      title: '删除测点',
      message: `将删除正式绑定测点 ${metric.metricName || metric.metricIdentifier}，是否继续？`,
      confirmButtonText: '确认删除'
    })
    actionLoading.value = true
    const res = await removeBinding(metric.bindingId)
    if (res.code === 200) {
      ElMessage.success('测点删除成功')
      await loadGroups()
      emit('updated')
    }
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      ElMessage.error(error instanceof Error ? error.message : '测点删除失败')
    }
  } finally {
    actionLoading.value = false
  }
}
</script>
```

- [ ] **Step 4: Finish add-metric and replace-metric flows, then refresh the list summaries after drawer updates**

Wire the add/replace submits and make the page reload summaries when the drawer emits `updated`:

```ts
// spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue
const loadAddMetricOptions = async (deviceId: number) => {
  const res = await getDeviceMetricOptions(deviceId)
  addMetricOptions.value = res.code === 200 ? (res.data || []) : []
}

const loadReplaceMetricOptions = async (deviceId: number) => {
  const res = await getDeviceMetricOptions(deviceId)
  replaceMetricOptions.value = res.code === 200 ? (res.data || []) : []
}

const handleAddMetricSubmit = async () => {
  if (!props.riskPointId || !addForm.deviceId || !addForm.metricIdentifier) {
    ElMessage.warning('请选择设备和测点')
    return
  }
  actionLoading.value = true
  try {
    const selectedDevice = deviceOptions.value.find((item) => String(item.id) === String(addForm.deviceId))
    const selectedMetric = addMetricOptions.value.find((item) => item.identifier === addForm.metricIdentifier)
    const res = await bindDevice({
      riskPointId: props.riskPointId,
      deviceId: addForm.deviceId,
      deviceCode: selectedDevice?.deviceCode,
      deviceName: selectedDevice?.deviceName,
      metricIdentifier: selectedMetric?.identifier,
      metricName: selectedMetric?.name
    })
    if (res.code === 200) {
      ElMessage.success('正式绑定新增成功')
      await loadGroups()
      emit('updated')
    }
  } finally {
    actionLoading.value = false
  }
}

const handleReplaceMetricSubmit = async () => {
  if (!replaceForm.bindingId || !replaceForm.metricIdentifier) {
    ElMessage.warning('请选择新的测点')
    return
  }
  actionLoading.value = true
  try {
    const selectedMetric = replaceMetricOptions.value.find((item) => item.identifier === replaceForm.metricIdentifier)
    const res = await replaceBinding(replaceForm.bindingId, {
      metricIdentifier: replaceForm.metricIdentifier,
      metricName: selectedMetric?.name || replaceForm.metricName
    })
    if (res.code === 200) {
      ElMessage.success('测点替换成功')
      await loadGroups()
      emit('updated')
    }
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => props.modelValue,
  async (value) => {
    if (value) {
      await loadGroups()
    }
  },
  { immediate: true }
)
```

```ts
// spring-boot-iot-ui/src/views/RiskPointView.vue
const handleBindingMaintenanceUpdated = async () => {
  await Promise.all([loadRiskPointList(), loadBindingSummaries()])
}
```

```vue
<!-- spring-boot-iot-ui/src/views/RiskPointView.vue -->
<RiskPointBindingMaintenanceDrawer
  v-model="bindingMaintenanceVisible"
  :risk-point-id="bindingMaintenanceRiskPoint ? Number(bindingMaintenanceRiskPoint.id) : null"
  :risk-point-name="bindingMaintenanceRiskPoint?.riskPointName || ''"
  :risk-point-code="bindingMaintenanceRiskPoint?.riskPointCode || ''"
  :org-name="bindingMaintenanceRiskPoint?.orgName || ''"
  :pending-binding-count="bindingSummaryMap[Number(bindingMaintenanceRiskPoint?.id || 0)]?.pendingBindingCount ?? 0"
  @updated="handleBindingMaintenanceUpdated"
/>
```

- [ ] **Step 5: Run the focused frontend tests to verify they pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts src/__tests__/views/RiskPointView.test.ts --run
```

Expected: PASS with grouped-binding rendering, single-binding delete, replace, and refresh wiring covered.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue \
  spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: add risk point binding maintenance drawer"
```

### Task 5: Update Documentation And Run Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update the business-flow and API docs**

Record the new UI behavior and endpoints explicitly:

```md
<!-- docs/02-业务功能与流程说明.md -->
- 风险对象中心当前区分“正式绑定维护”和“待治理转正”两类入口：
  - `维护绑定`：查看当前正式绑定设备与测点，并支持新增正式绑定、整机解绑、删除单个测点、替换单个测点。
  - `待治理转正`：查看 pending 记录、推荐候选和历史留痕，并将候选测点转成正式绑定。
- 风险对象中心不直接编辑设备主档；设备名称、组织、产品等主数据仍在设备资产中心维护。
```

```md
<!-- docs/03-接口规范与接口清单.md -->
- `GET /api/risk-point/binding-summaries`
  - 说明：按风险点批量返回正式绑定摘要。
- `GET /api/risk-point/binding-groups/{riskPointId}`
  - 说明：按设备分组返回当前风险点的正式绑定测点。
- `POST /api/risk-point/bindings/{bindingId}/remove`
  - 说明：删除单个正式测点绑定，不影响同设备其他测点。
- `POST /api/risk-point/bindings/{bindingId}/replace`
  - 说明：将单个正式测点绑定替换为新的测点绑定。
```

- [ ] **Step 2: Update the change log, front-end governance doc, and acceptance doc**

Capture the maintenance semantics and regression guardrails:

```md
<!-- docs/08-变更记录与技术债清单.md -->
- 2026-04-04：风险对象中心新增正式绑定摘要与维护抽屉，明确拆分“整机解绑”和“单测点删除”，并约束风险点页不直接编辑设备主档。
```

```md
<!-- docs/15-前端优化与治理计划.md -->
- `/risk-point` 的正式绑定详情继续下沉到共享抽屉，不把设备卡片和测点卡片重新塞回主列表。
- “维护绑定”和“待治理转正”必须保持两个独立入口，避免正式绑定维护与 pending 治理回流为同一面板。
```

```md
<!-- docs/21-业务功能清单与验收标准.md -->
- 风险对象中心验收新增：
  - 列表可见正式绑定设备数、正式绑定测点数、待治理记录数。
  - 可按设备分组查看正式绑定测点。
  - 整机解绑删除该设备在当前风险点下的全部正式测点绑定。
  - 单测点删除仅删除当前绑定项，不影响同设备其他测点。
```

- [ ] **Step 3: Run the backend verification suite**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false -Dtest=RiskPointServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest,RiskPointPendingControllerTest test
```

Expected: PASS with all risk-point service/controller tests green.

- [ ] **Step 4: Run the frontend verification suite**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/views/RiskPointView.test.ts src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts --run
```

Expected: PASS with risk-point list and drawer interaction tests green.

- [ ] **Step 5: Run the local quality gates**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS with repository quality gates reporting success for the touched backend/frontend/doc files.

- [ ] **Step 6: Run the backend package build**

Run:

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Run the frontend production build**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected: Vite build completes successfully and emits the production bundle without TypeScript errors.

- [ ] **Step 8: Commit**

```bash
git add \
  docs/02-业务功能与流程说明.md \
  docs/03-接口规范与接口清单.md \
  docs/08-变更记录与技术债清单.md \
  docs/15-前端优化与治理计划.md \
  docs/21-业务功能清单与验收标准.md
git commit -m "docs: record risk point binding maintenance flow"
```

## Real-Environment Acceptance Checklist

After Task 5 passes locally, validate the shipped behavior in the shared dev baseline:

1. Start the backend against the real dev profile.

```bash
mvn -pl spring-boot-iot-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: the admin app boots with the real MySQL / Redis / telemetry baseline from `application-dev.yml`.

2. Start the frontend.

```bash
npm --prefix spring-boot-iot-ui run dev -- --host 0.0.0.0
```

Expected: Vite serves the updated `/risk-point` page.

3. Manual browser checks:
   - Open `/risk-point` and verify the list shows `X 台设备 / Y 个测点 / 待治理 Z 条`.
   - Open one row that already has formal bindings and verify the maintenance drawer shows device-grouped metric cards.
   - Trigger one “删除测点” action and verify only the target metric disappears.
   - Trigger one “整机解绑” action and verify the selected device group disappears entirely.
   - Add one metric back through the maintenance drawer and verify the summary column updates after refresh.
   - Open the same row’s `待治理转正` drawer and verify it still behaves independently from the formal-binding drawer.

4. If the shared environment blocks access or a required table is missing, stop and report the real-environment blocker instead of switching to H2 or another deprecated fallback.
