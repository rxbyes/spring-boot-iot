# Risk Point Formal Binding Metric Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `/risk-point -> 风险绑定工作台 -> 维护绑定` support fast device-code lookup and only allow formal bindings against metrics that are already published in the active risk metric catalog.

**Architecture:** Keep the generic device metric API unchanged. Add an alarm-domain read endpoint dedicated to formal risk bindings, implemented by reusing scoped device metric options and filtering to entries with `riskMetricId`. Enforce the same directory-only rule on the formal bind and replace write paths inside `RiskPointBindingMaintenanceServiceImpl`, while leaving pending-promotion logic untouched.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Element Plus, Vitest, JUnit 5, Mockito

---

## File Structure

### Task 1 ownership: formal-binding metric read API in `spring-boot-iot-alarm`

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`

### Task 2 ownership: formal bind / replace write-side guard in `spring-boot-iot-alarm`

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`

### Task 3 ownership: maintenance drawer API swap and device-code search in `spring-boot-iot-ui`

- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`

### Task 4 ownership: docs sync and focused verification

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

## Task 1: Add the formal-binding metric read API

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`

- [ ] **Step 1: Write the failing backend tests**

```java
@Test
void listFormalBindingMetricOptionsShouldExtractCurrentUserAndDelegate() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
    RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);

    DeviceMetricOptionVO option = new DeviceMetricOptionVO();
    option.setIdentifier("value");
    option.setName("激光测距值");
    option.setDataType("double");
    option.setRiskMetricId(6101L);
    when(maintenanceService.listFormalBindingMetricOptions(3001L, 1001L)).thenReturn(List.of(option));

    R<List<DeviceMetricOptionVO>> response =
            controller.listFormalBindingMetricOptions(3001L, authentication(1001L));

    assertEquals(1, response.getData().size());
    assertEquals("value", response.getData().get(0).getIdentifier());
    assertEquals(6101L, response.getData().get(0).getRiskMetricId());
    verify(maintenanceService).listFormalBindingMetricOptions(3001L, 1001L);
}

@Test
void listFormalBindingMetricOptionsShouldReturnOnlyPublishedCatalogMetrics() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
    RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
    RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
    DeviceService deviceService = mock(DeviceService.class);
    RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
            riskPointService,
            riskPointDeviceMapper,
            pendingBindingMapper,
            pendingPromotionMapper,
            null,
            null,
            null,
            deviceService
    );

    DeviceMetricOptionVO formalMetric = new DeviceMetricOptionVO();
    formalMetric.setIdentifier("value");
    formalMetric.setName("激光测距值");
    formalMetric.setDataType("double");
    formalMetric.setRiskMetricId(6101L);

    DeviceMetricOptionVO governanceOnlyMetric = new DeviceMetricOptionVO();
    governanceOnlyMetric.setIdentifier("sensor_state");
    governanceOnlyMetric.setName("传感器状态");
    governanceOnlyMetric.setDataType("int");
    governanceOnlyMetric.setRiskMetricId(null);

    when(deviceService.listMetricOptions(1001L, 3001L)).thenReturn(List.of(formalMetric, governanceOnlyMetric));

    List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3001L, 1001L);

    assertEquals(List.of("value"), result.stream().map(DeviceMetricOptionVO::getIdentifier).toList());
    assertEquals(List.of(6101L), result.stream().map(DeviceMetricOptionVO::getRiskMetricId).toList());
}
```

- [ ] **Step 2: Run the targeted alarm tests and verify RED**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointControllerTest,RiskPointBindingMaintenanceServiceImplTest" test
```

Expected: `BUILD FAILURE`, with compilation or assertion failures because `listFormalBindingMetricOptions(...)` does not exist yet.

- [ ] **Step 3: Implement the minimal controller/service API**

```java
// RiskPointBindingMaintenanceService.java
List<DeviceMetricOptionVO> listFormalBindingMetricOptions(Long deviceId, Long currentUserId);
```

```java
// RiskPointController.java
@GetMapping("/devices/{deviceId}/formal-metrics")
public R<List<DeviceMetricOptionVO>> listFormalBindingMetricOptions(@PathVariable Long deviceId,
                                                                    Authentication authentication) {
    return R.ok(bindingMaintenanceService.listFormalBindingMetricOptions(
            deviceId,
            requireCurrentUserId(authentication)
    ));
}
```

```java
// RiskPointBindingMaintenanceServiceImpl.java
@Override
public List<DeviceMetricOptionVO> listFormalBindingMetricOptions(Long deviceId, Long currentUserId) {
    if (deviceService == null) {
        return List.of();
    }
    return deviceService.listMetricOptions(currentUserId, deviceId).stream()
            .filter(option -> option.getRiskMetricId() != null)
            .sorted(Comparator.comparing(DeviceMetricOptionVO::getIdentifier, Comparator.nullsLast(String::compareTo)))
            .toList();
}
```

- [ ] **Step 4: Re-run the same alarm tests and verify GREEN**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointControllerTest,RiskPointBindingMaintenanceServiceImplTest" test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java
git commit -m "feat: add formal risk binding metric endpoint"
```

## Task 2: Enforce the directory-only rule on formal bind and replace

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`

- [ ] **Step 1: Write the failing write-guard tests**

```java
@Test
void submitBindDeviceShouldRejectMetricOutsideFormalCatalog() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
    RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
    RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
    DeviceService deviceService = mock(DeviceService.class);
    GovernanceWorkItemService workItemService = mock(GovernanceWorkItemService.class);
    RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
            riskPointService,
            riskPointDeviceMapper,
            pendingBindingMapper,
            pendingPromotionMapper,
            null,
            null,
            workItemService,
            deviceService
    );

    RiskPointDevice request = binding(null, 11L, 201L, "DEV-201", "一号设备", "sensor_state", "传感器状态", null);
    when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("value", "激光测距值", 6101L)));

    BizException error = assertThrows(BizException.class, () -> service.submitBindDevice(request, 1001L));

    assertEquals("当前测点未发布到风险指标目录，不能用于正式绑定", error.getMessage());
    verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
    verify(workItemService, never()).openOrRefreshAndGetId(any());
}

@Test
void replaceBindingMetricShouldRejectMetricOutsideFormalCatalog() {
    RiskPointService riskPointService = mock(RiskPointService.class);
    RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
    RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
    RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
    DeviceService deviceService = mock(DeviceService.class);
    RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
            riskPointService,
            riskPointDeviceMapper,
            pendingBindingMapper,
            pendingPromotionMapper,
            null,
            null,
            null,
            deviceService
    );

    RiskPointDevice existing = binding(3001L, 11L, 201L, "DEV-201", "一号设备", "value", "激光测距值", new Date());
    when(riskPointDeviceMapper.selectById(3001L)).thenReturn(existing);
    when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("value", "激光测距值", 6101L)));
    when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());

    RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
    request.setMetricIdentifier("sensor_state");
    request.setMetricName("传感器状态");

    BizException error = assertThrows(BizException.class, () -> service.replaceBindingMetric(3001L, request, 1001L));

    assertEquals("当前测点未发布到风险指标目录，不能用于正式绑定", error.getMessage());
    verify(riskPointDeviceMapper, never()).deleteById(any());
}
```

- [ ] **Step 2: Run the targeted service tests and verify RED**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointBindingMaintenanceServiceImplTest" test
```

Expected: `BUILD FAILURE`, with failures showing that non-directory metrics still pass through `submitBindDevice` or `replaceBindingMetric`.

- [ ] **Step 3: Implement the write-side guard without touching pending promotion**

```java
private DeviceMetricOptionVO requireFormalBindingMetricOption(Long currentUserId,
                                                             Long deviceId,
                                                             Long riskMetricId,
                                                             String metricIdentifier) {
    List<DeviceMetricOptionVO> options = listFormalBindingMetricOptions(deviceId, currentUserId);
    String normalizedIdentifier = StringUtils.hasText(metricIdentifier) ? metricIdentifier.trim() : null;
    return options.stream()
            .filter(option -> Objects.equals(option.getRiskMetricId(), riskMetricId)
                    || (normalizedIdentifier != null && normalizedIdentifier.equals(option.getIdentifier())))
            .findFirst()
            .orElseThrow(() -> new BizException("当前测点未发布到风险指标目录，不能用于正式绑定"));
}

private void normalizeFormalBindingSelection(RiskPointDevice request, Long currentUserId) {
    DeviceMetricOptionVO option = requireFormalBindingMetricOption(
            currentUserId,
            request.getDeviceId(),
            request.getRiskMetricId(),
            request.getMetricIdentifier()
    );
    request.setRiskMetricId(option.getRiskMetricId());
    request.setMetricIdentifier(option.getIdentifier());
    request.setMetricName(StringUtils.hasText(option.getName()) ? option.getName().trim() : option.getIdentifier());
}
```

```java
// submitBindDevice(...)
normalizeFormalBindingSelection(riskPointDevice, currentUserId);
```

```java
// replaceBindingMetric(...)
DeviceMetricOptionVO option = requireFormalBindingMetricOption(
        currentUserId,
        oldBinding.getDeviceId(),
        request == null ? null : request.getRiskMetricId(),
        request == null ? null : request.getMetricIdentifier()
);
replacement.setRiskMetricId(option.getRiskMetricId());
replacement.setMetricIdentifier(option.getIdentifier());
replacement.setMetricName(StringUtils.hasText(option.getName()) ? option.getName().trim() : option.getIdentifier());
```

Keep this guard in `RiskPointBindingMaintenanceServiceImpl`; do not tighten `RiskPointServiceImpl.bindDeviceAndReturn(...)`, because pending-promotion still relies on the broader governance path.

- [ ] **Step 4: Re-run the same service tests and verify GREEN**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointBindingMaintenanceServiceImplTest" test
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java
git commit -m "feat: guard formal risk bindings by metric catalog"
```

## Task 3: Switch the maintenance drawer to the new API and enable device-code search

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`

- [ ] **Step 1: Write the failing UI tests**

```ts
it('loads formal metric options from the risk-point API and keeps only published metrics', async () => {
  mockListFormalBindingMetricOptions.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      { identifier: 'value', name: '激光测距值', dataType: 'double', riskMetricId: 6101 },
      { identifier: 'dispsX', name: 'X轴位移', dataType: 'double', riskMetricId: 6102 }
    ]
  })

  const wrapper = mountDrawer()
  await flushPromises()

  await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
  await flushPromises()

  expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith('2002')
  const optionTexts = wrapper
    .get('[data-testid="binding-add-metric"]')
    .findAll('option')
    .map((node) => node.text())

  expect(optionTexts).toContain('激光测距值')
  expect(optionTexts).toContain('X轴位移')
})

it('marks the add-device selector as filterable for device-code lookup', async () => {
  const wrapper = mountDrawer()
  await flushPromises()

  expect(wrapper.get('[data-testid="binding-add-device"]').attributes('data-filterable')).toBe('true')
})

it('shows an explicit empty hint when the selected device has no formal catalog metrics', async () => {
  mockListFormalBindingMetricOptions.mockResolvedValueOnce({ code: 200, msg: 'success', data: [] })

  const wrapper = mountDrawer()
  await flushPromises()

  await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
  await flushPromises()

  expect(wrapper.text()).toContain('当前设备所属产品暂无可用于风险绑定的正式目录字段')
})
```

- [ ] **Step 2: Run the targeted Vitest suite and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts
```

Expected: `FAIL`, because the drawer still imports `getDeviceMetricOptions`, the new API helper is missing, and the device selector is not marked filterable.

- [ ] **Step 3: Implement the minimal API/component change**

```ts
// src/api/riskPoint.ts
import type { ApiEnvelope, DeviceOption, DeviceMetricOption, GovernanceSubmissionResult, IdType, PageResult } from '../types/api'

export const listFormalBindingMetricOptions = (deviceId: IdType): Promise<ApiEnvelope<DeviceMetricOption[]>> => {
  return request<DeviceMetricOption[]>(`/api/risk-point/devices/${deviceId}/formal-metrics`, { method: 'GET' })
}
```

```vue
<!-- RiskPointBindingMaintenanceDrawer.vue -->
<el-select
  v-model="addForm.deviceId"
  data-testid="binding-add-device"
  filterable
  placeholder="请输入设备编号或选择设备"
  :disabled="bindableDevices.length === 0 || addSubmitting"
  @change="handleAddDeviceChange"
>
```

```ts
// RiskPointBindingMaintenanceDrawer.vue script
import { listFormalBindingMetricOptions, ... } from '@/api/riskPoint'

const getMetricOptions = async (deviceId: IdType) => {
  const deviceIdKey = getIdKey(deviceId)
  if (!deviceIdKey) {
    return []
  }
  if (metricOptionCache[deviceIdKey]?.length) {
    return metricOptionCache[deviceIdKey]
  }
  const res = await listFormalBindingMetricOptions(deviceIdKey)
  metricOptionCache[deviceIdKey] = res.code === 200 ? (res.data || []) : []
  return metricOptionCache[deviceIdKey]
}
```

```vue
<p
  v-if="addForm.deviceId && !addSubmitting && addMetricOptions.length === 0"
  class="risk-point-binding-maintenance-drawer__detail-tip"
>
  当前设备所属产品暂无可用于风险绑定的正式目录字段。
</p>
```

Also update the local `ElSelectStub` in the test file so it accepts the `filterable` prop and exposes it via `data-filterable`.

- [ ] **Step 4: Re-run the same Vitest suite and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts
```

Expected: `PASS`

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/api/riskPoint.ts spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts
git commit -m "feat: filter formal risk binding metrics in drawer"
```

## Task 4: Sync docs and run focused verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the business and API docs**

```md
<!-- docs/02-业务功能与流程说明.md -->
- `风险绑定工作台 -> 维护绑定` 当前已支持按设备编号快速定位设备；新增正式绑定与更换测点只允许选择已发布到启用中 `risk_metric_catalog` 的正式字段，不再回流设备全量属性或纯治理语义字段。
```

```md
<!-- docs/03-接口规范与接口清单.md -->
- `GET /api/risk-point/devices/{deviceId}/formal-metrics` 当前用于风险对象中心正式绑定维护，只返回当前设备所属产品已发布到启用中风险指标目录的正式字段，响应字段为 `identifier / name / dataType / riskMetricId`。
- `POST /api/risk-point/bind-device` 与 `POST /api/risk-point/bindings/{bindingId}/replace` 当前会拒绝未进入风险指标目录的测点，返回业务错误，避免绕过前端把非目录字段写入正式 `risk_point_device`。
```

- [ ] **Step 2: Update the front-end governance rule and change log**

```md
<!-- docs/15-前端优化与治理计划.md -->
- 风险对象中心的正式绑定测点选择必须优先消费风险指标目录真相，不得回退到设备全量属性下拉；设备较多时优先通过设备编号输入过滤收口选择成本。
```

```md
<!-- docs/08-变更记录与技术债清单.md -->
- 2026-04-13：`/risk-point -> 风险绑定工作台 -> 维护绑定` 已补齐设备编号快速定位，并新增 formal-metrics 专用接口；新增正式绑定与更换测点当前只允许选择已发布到启用中风险指标目录的正式字段，写侧也会拒绝非目录字段。
```

- [ ] **Step 3: Run focused backend/frontend verification**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskPointControllerTest,RiskPointBindingMaintenanceServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts
git diff --check
```

Expected:

1. Maven test command ends with `BUILD SUCCESS`
2. Vitest command ends with `PASS`
3. `git diff --check` prints no whitespace or merge-marker errors

- [ ] **Step 4: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: document formal risk binding metric filter"
```

## Self-Review

### Spec coverage check

Spec requirements and matching tasks:

1. Formal-binding dedicated read API: covered by Task 1.
2. Directory-only bind/replace guard without tightening pending promotion: covered by Task 2.
3. Device-code lookup and maintenance drawer API swap: covered by Task 3.
4. Business/API/front-end governance doc sync and targeted verification: covered by Task 4.

No spec sections are currently uncovered.

### Placeholder scan

Checked for:

- `TODO`
- `TBD`
- “implement later”
- “add tests” without actual tests
- vague “handle edge cases”

No placeholders remain.

### Type consistency check

Plan-level type usage is consistent:

1. Backend read API uses `DeviceMetricOptionVO`.
2. Frontend API uses existing `DeviceMetricOption`.
3. Formal bind/replace guard stays in `RiskPointBindingMaintenanceServiceImpl`, not `RiskPointServiceImpl`, so pending promotion keeps its broader path.

