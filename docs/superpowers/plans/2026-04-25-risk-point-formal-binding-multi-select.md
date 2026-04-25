# Risk Point Formal Binding Multi-Select Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade risk point formal binding so one device can submit multiple formal metrics in one request, one approval order, and multiple `risk_point_device` rows.

**Architecture:** Keep `GET /api/risk-point/devices/{deviceId}/formal-metrics` as the only source of selectable metrics, but replace the single-metric bind request with a batch request carrying `metrics[]`. Backend validation, approval snapshot serialization, and direct-write execution all operate on the same batch model; both frontend entry points switch their metric selector from single value to multi-select while preserving existing empty-state messaging and device search.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Element Plus, JUnit 5, Vitest.

---

## File Structure

### Backend

- Create `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindMetricDTO.java`
  - Batch metric item for formal binding requests and approval payloads.
- Create `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBatchBindDeviceRequest.java`
  - Batch formal binding request carrying `riskPointId`, `deviceId`, optional device display fields, and `metrics[]`.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
  - Accept the new batch request at `POST /api/risk-point/bind-device`.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
  - Replace single-metric formal bind entry points with batch request signatures.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
  - Validate and normalize `metrics[]`, reject duplicates, create one approval snapshot, and write multiple `RiskPointDevice` rows when approved or direct-applied.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java`
  - Serialize and deserialize batch formal binding payloads, including approval execution and simulation.
- Test `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
  - Cover direct batch bind, approval batch bind, duplicate rejection, and invalid catalog rejection.
- Test `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`
  - Assert controller request binding for batch formal bind.

### Frontend

- Modify `spring-boot-iot-ui/src/api/riskPoint.ts`
  - Add batch formal bind request typings and update `bindDevice(...)` to accept the new request.
- Modify `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
  - Switch add-form metric selection to `metricIdentifiers: string[]`, enable `multiple`, build `metrics[]`, and update success feedback count.
- Modify `spring-boot-iot-ui/src/views/RiskPointView.vue`
  - Switch the legacy bind drawer to the same multi-select request model and count-based success messaging.
- Modify `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
  - Cover multi-select interaction, payload assembly, and filtered metric options.
- Modify `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`
  - Cover legacy drawer multi-select submission, empty validation, and count-aware success flow.

### Docs

- Modify `docs/03-接口规范与接口清单.md`
  - Document `POST /api/risk-point/bind-device` as a batch formal binding request.
- Modify `docs/08-变更记录与技术债清单.md`
  - Record the multi-select formal binding upgrade and related environment caveats.
- Modify `docs/15-前端优化与治理计划.md`
  - Record the new rule that both formal binding entry points must keep selector behavior aligned.
- Check `README.md` and `AGENTS.md`
  - Update only if the batch formal binding behavior changes the stated project baseline or operating rules.

---

### Task 1: Backend Batch Formal Binding Contract

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindMetricDTO.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBatchBindDeviceRequest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`

- [ ] **Step 1: Write or update controller contract test**

```java
@Test
void bindDeviceShouldAcceptBatchMetricsRequest() throws Exception {
    RiskPointBatchBindDeviceRequest request = new RiskPointBatchBindDeviceRequest();
    request.setRiskPointId(1L);
    request.setDeviceId(2L);
    request.setMetrics(List.of(metric(11L, "dispsX", "位移X"), metric(12L, "dispsY", "位移Y")));

    when(bindingMaintenanceService.submitBindDevice(any(RiskPointBatchBindDeviceRequest.class), eq(9L)))
            .thenReturn(new GovernanceSubmissionResultVO("DIRECT_SUCCESS", "已新增 2 个正式测点绑定"));

    mockMvc.perform(post("/api/risk-point/bind-device")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
                    .with(authentication(authentication())))
            .andExpect(status().isOk());

    verify(bindingMaintenanceService).submitBindDevice(any(RiskPointBatchBindDeviceRequest.class), eq(9L));
}
```

- [ ] **Step 2: Run the focused controller test and confirm it fails before the contract exists**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointControllerTest#bindDeviceShouldAcceptBatchMetricsRequest test
```

Expected: compile failure or request-binding failure because the batch DTO and service signature do not exist yet.

- [ ] **Step 3: Add the batch DTOs and controller/service signature**

```java
public class RiskPointBatchBindDeviceRequest {
    private Long riskPointId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private List<RiskPointBindMetricDTO> metrics = new ArrayList<>();
}
```

```java
public class RiskPointBindMetricDTO {
    private Long riskMetricId;
    private String metricIdentifier;
    private String metricName;
}
```

```java
@PostMapping("/bind-device")
public R<GovernanceSubmissionResultVO> bindDevice(@RequestBody RiskPointBatchBindDeviceRequest request,
                                                  Authentication authentication) {
    return R.ok(bindingMaintenanceService.submitBindDevice(request, requireCurrentUserId(authentication)));
}
```

- [ ] **Step 4: Re-run the focused controller test**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointControllerTest#bindDeviceShouldAcceptBatchMetricsRequest test
```

Expected: PASS.

- [ ] **Step 5: Commit the contract-only slice**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBindMetricDTO.java \
        spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/dto/RiskPointBatchBindDeviceRequest.java \
        spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java \
        spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskPointBindingMaintenanceService.java \
        spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "feat: add batch risk point binding contract"
```

### Task 2: Backend Batch Validation, Approval, and Direct Write

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`

- [ ] **Step 1: Add failing service tests for batch success and failure cases**

```java
@Test
void submitBindDeviceShouldDirectlyApplyAllMetricsWhenApprovalPolicyMissing() {
    RiskPointBatchBindDeviceRequest request = batchRequest(100L, 200L,
            metric(301L, "dispsX", "位移X"),
            metric(302L, "dispsY", "位移Y"));

    GovernanceSubmissionResultVO result = service.submitBindDevice(request, 9L);

    assertThat(result.getStatus()).isEqualTo("DIRECT_SUCCESS");
    assertThat(riskPointDeviceMapper.insert(any())).isEqualTo(1);
    verify(riskPointDeviceMapper, times(2)).insert(any(RiskPointDevice.class));
}
```

```java
@Test
void submitBindDeviceShouldRejectDuplicateMetricIdentifiersWithinBatch() {
    RiskPointBatchBindDeviceRequest request = batchRequest(100L, 200L,
            metric(301L, "dispsX", "位移X"),
            metric(301L, "dispsX", "位移X"));

    assertThatThrownBy(() -> service.submitBindDevice(request, 9L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("测点");
}
```

- [ ] **Step 2: Run the focused service test class and confirm it fails**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointBindingMaintenanceServiceImplTest test
```

Expected: FAIL because service methods, payload handling, and assertions still assume one metric.

- [ ] **Step 3: Implement batch normalization and atomic execution**

```java
private List<RiskPointBindMetricDTO> normalizeFormalBindingSelections(RiskPointBatchBindDeviceRequest request, Long userId) {
    if (CollectionUtils.isEmpty(request.getMetrics())) {
        throw new BizException("请至少选择一个测点");
    }
    List<DeviceMetricOptionVO> options = listFormalBindingMetricOptions(request.getDeviceId(), userId);
    Map<String, DeviceMetricOptionVO> optionMap = options.stream()
            .collect(Collectors.toMap(DeviceMetricOptionVO::getMetricIdentifier, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    LinkedHashSet<String> seenIdentifiers = new LinkedHashSet<>();
    List<RiskPointBindMetricDTO> normalized = new ArrayList<>();
    for (RiskPointBindMetricDTO metric : request.getMetrics()) {
        String identifier = StringUtils.trimWhitespace(metric.getMetricIdentifier());
        if (!StringUtils.hasText(identifier) || !seenIdentifiers.add(identifier)) {
            throw new BizException("请勿重复选择测点");
        }
        DeviceMetricOptionVO option = optionMap.get(identifier);
        if (option == null) {
            throw new BizException("所选测点不在当前设备可绑定的正式目录中");
        }
        normalized.add(new RiskPointBindMetricDTO(option.getRiskMetricId(), option.getMetricIdentifier(), option.getMetricName()));
    }
    return normalized;
}
```

```java
for (RiskPointBindMetricDTO metric : normalizedMetrics) {
    RiskPointDevice binding = buildRiskPointDevice(request, metric);
    bindDevice(binding, currentUserId);
}
```

- [ ] **Step 4: Update approval payload serialization and simulation to use `metrics[]`**

```java
payload.put("metrics", request.getMetrics());
```

```java
List<RiskPointBindMetricDTO> metrics = objectMapper.convertValue(
        payload.get("metrics"),
        new TypeReference<List<RiskPointBindMetricDTO>>() {}
);
request.setMetrics(metrics);
```

- [ ] **Step 5: Re-run focused backend tests**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit the backend batch execution slice**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java \
        spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/governance/RiskPointGovernanceApprovalExecutor.java \
        spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java \
        spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "feat: support batch formal risk point binding"
```

### Task 3: Frontend Multi-Select Entry Points

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Update frontend tests to expect `metrics[]` and multi-select state**

```ts
expect(mockBindDevice).toHaveBeenCalledWith({
  riskPointId: 1,
  deviceId: 10,
  deviceCode: 'CXH15522812',
  deviceName: '多维检测仪',
  metrics: [
    { riskMetricId: 101, metricIdentifier: 'dispsX', metricName: '位移X' },
    { riskMetricId: 102, metricIdentifier: 'dispsY', metricName: '位移Y' }
  ]
})
```

- [ ] **Step 2: Run the focused Vitest files and confirm they fail**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts \
  src/__tests__/views/RiskPointView.test.ts
```

Expected: FAIL because the form state and request payload are still single-select.

- [ ] **Step 3: Implement batch request typings and multi-select forms**

```ts
export interface RiskPointBindMetric {
  riskMetricId?: IdType | null;
  metricIdentifier: string;
  metricName: string;
}

export interface RiskPointBatchBindDeviceRequest {
  riskPointId: IdType;
  deviceId: IdType;
  deviceCode?: string;
  deviceName?: string;
  metrics: RiskPointBindMetric[];
}
```

```vue
<el-select
  v-model="addForm.metricIdentifiers"
  multiple
  collapse-tags
  collapse-tags-tooltip
  :disabled="!addForm.deviceId || bindMetricOptions.length === 0"
>
```

```ts
const selectedMetrics = addForm.metricIdentifiers
  .map((identifier) => getSelectedMetricOption(addForm.deviceId, identifier))
  .filter((metric): metric is DeviceMetricOption => Boolean(metric))
await bindDevice({
  riskPointId: props.riskPointId,
  deviceId: addForm.deviceId,
  deviceCode: selectedDevice?.deviceCode,
  deviceName: selectedDevice?.deviceName,
  metrics: selectedMetrics.map((metric) => ({
    riskMetricId: metric.riskMetricId ?? null,
    metricIdentifier: metric.metricIdentifier,
    metricName: metric.metricName,
  })),
})
```

- [ ] **Step 4: Re-run focused frontend tests**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts \
  src/__tests__/views/RiskPointView.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit the frontend multi-select slice**

```bash
git add spring-boot-iot-ui/src/api/riskPoint.ts \
        spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue \
        spring-boot-iot-ui/src/views/RiskPointView.vue \
        spring-boot-iot-ui/src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts \
        spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: add multi-select risk point binding forms"
```

### Task 4: Docs and Regression Verification

**Files:**
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Check: `README.md`
- Check: `AGENTS.md`

- [ ] **Step 1: Update docs to match the new batch binding behavior**

```md
- `POST /api/risk-point/bind-device`：风险点正式绑定批量提交接口，请求体为 `riskPointId + deviceId + metrics[]`，一次提交共享一张审批单。
- 风险绑定工作台与列表页旧抽屉的“新增正式绑定”统一支持多测点选择，不允许一处保留单选语义。
```

- [ ] **Step 2: Run backend regression commands**

Run:

```bash
mvn -pl spring-boot-iot-alarm -Dtest=RiskPointBindingMaintenanceServiceImplTest,RiskPointControllerTest test
```

Expected: PASS.

- [ ] **Step 3: Run frontend regression commands**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/riskPoint/RiskPointBindingMaintenanceDrawer.test.ts \
  src/__tests__/views/RiskPointView.test.ts \
  src/__tests__/components/riskPoint/RiskPointDetailDrawer.test.ts
```

Expected: PASS.

- [ ] **Step 4: Review README and AGENTS impact**

```md
若本次仅改变风险绑定正式绑定交互与接口契约，而未改变项目定位、模块边界、运行方式或验收基线，可记录为“已检查，无需更新”。
```

- [ ] **Step 5: Commit docs and verification updates**

```bash
git add docs/03-接口规范与接口清单.md \
        docs/08-变更记录与技术债清单.md \
        docs/15-前端优化与治理计划.md \
        docs/superpowers/plans/2026-04-25-risk-point-formal-binding-multi-select.md
git commit -m "docs: record batch risk point binding behavior"
```
