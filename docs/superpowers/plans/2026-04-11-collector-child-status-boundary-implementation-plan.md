# Collector Child Status Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce the formal parent/child modeling boundary for composite collectors so collector products keep only collector-owned fields, while collector pages gain a read-only aggregate view of child latest metrics and child formal status.

**Architecture:** Keep `spring-boot-iot-device` as the write-side authority. `ProductModelServiceImpl` must stop collector products from recognizing child monitoring metrics or mirrored child `sensor_state` during composite governance compare, while child products and single-device products continue to share the same formal identifiers. Add a separate collector child insight read API that assembles `iot_device_relation + child latest properties + device online state` into a collector overview payload, and render that payload in `DeviceInsightView` without writing anything back into product contracts or object-insight formal metrics.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, Vitest, JUnit 5, Maven

---

## File Structure

### Task 1 ownership: governance write-side boundary in `spring-boot-iot-device`

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

### Task 2 ownership: collector child aggregate read API in `spring-boot-iot-device`

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CollectorChildInsightService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightMetricVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightControllerTest.java`

### Task 3 ownership: collector boundary guidance in the product workbench

- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

### Task 4 ownership: collector aggregate panel in the device insight view

- Create: `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue`
- Modify: `spring-boot-iot-ui/src/api/iot.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

### Task 5 ownership: documentation and focused verification

- Review: `README.md`
- Review: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

## Task 1: Enforce collector write-side governance boundary

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Write the failing device governance tests**

```java
@Test
void compareGovernanceShouldIgnoreCompositeChildMetricsForCollectorProduct() {
    Product collector = product(6006L, "nf-monitor-collector-v1", "南方测绘 监测型 采集器");
    collector.setNodeType(2);
    when(productMapper.selectById(6006L)).thenReturn(collector);
    when(productModelMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("business");
    manualExtract.setDeviceStructure("composite");
    manualExtract.setParentDeviceCode("SK00EA0D1307988");
    manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
    manualExtract.setSamplePayload("""
            {"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6006L, dto);

    assertTrue(result.getCompareRows().isEmpty());
}

@Test
void compareGovernanceShouldKeepCollectorRuntimeStatusButDropChildSensorState() {
    Product collector = product(6006L, "nf-monitor-collector-v1", "南方测绘 监测型 采集器");
    collector.setNodeType(2);
    when(productMapper.selectById(6006L)).thenReturn(collector);
    when(productModelMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("status");
    manualExtract.setDeviceStructure("composite");
    manualExtract.setParentDeviceCode("SK00EA0D1307988");
    manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
    manualExtract.setSamplePayload("""
            {"SK00EA0D1307988":{"S1_ZT_1":{"2026-04-09T13:47:28.000Z":{"temp":20.31,"humidity":89.04,"signal_4g":-71,"sensor_state":{"L1_LF_1":0}}}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6006L, dto);

    List<String> identifiers = result.getCompareRows().stream()
            .map(ProductModelGovernanceCompareRowVO::getIdentifier)
            .sorted()
            .toList();
    assertEquals(List.of("humidity", "signal_4g", "temp"), identifiers);
    assertTrue(result.getCompareRows().stream().noneMatch(item -> "sensor_state".equals(item.getIdentifier())));
}
```

- [ ] **Step 2: Run the device governance test to verify it fails**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
```

Expected: FAIL because collector products still canonicalize composite child business/status data into formal compare rows.

- [ ] **Step 3: Add the minimal collector boundary policy and wire it into composite manual extraction**

```java
final class CollectorChildMetricBoundaryPolicy {

    private static final int COLLECTOR_NODE_TYPE = 2;
    private static final String SAMPLE_TYPE_STATUS = "status";
    private static final String PARENT_SENSOR_STATE_PREFIX = "S1_ZT_1.sensor_state.";

    boolean applies(Product product, String deviceStructure) {
        return product != null
                && Integer.valueOf(COLLECTOR_NODE_TYPE).equals(product.getNodeType())
                && "composite".equalsIgnoreCase(deviceStructure);
    }

    boolean shouldKeepRawLeaf(String sampleType, String rawIdentifier, List<String> logicalChannelCodes) {
        String normalized = normalize(rawIdentifier);
        if (normalized == null) {
            return false;
        }
        if (SAMPLE_TYPE_STATUS.equalsIgnoreCase(sampleType)) {
            if (logicalChannelCodes.stream().anyMatch(code -> normalized.equals(PARENT_SENSOR_STATE_PREFIX + code))) {
                return false;
            }
            return logicalChannelCodes.stream().noneMatch(code -> normalized.equals(code) || normalized.startsWith(code + "."));
        }
        return logicalChannelCodes.stream().noneMatch(code -> normalized.equals(code) || normalized.startsWith(code + "."));
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
```

```java
private final CollectorChildMetricBoundaryPolicy collectorChildMetricBoundaryPolicy =
        new CollectorChildMetricBoundaryPolicy();

private ManualSampleSnapshot applyCompositeManualSnapshot(Product product,
                                                          ProductModelGovernanceCompareDTO.ManualExtractInput input,
                                                          ManualSampleSnapshot rawSnapshot) {
    List<NormalizedRelationMapping> relationMappings = normalizeRelationMappings(input);
    List<String> logicalChannelCodes = relationMappings.stream()
            .map(NormalizedRelationMapping::logicalChannelCode)
            .toList();
    List<ManualLeafEvidence> sourceLeaves = rawSnapshot.leaves();
    if (collectorChildMetricBoundaryPolicy.applies(product, input.getDeviceStructure())) {
        sourceLeaves = sourceLeaves.stream()
                .filter(leaf -> collectorChildMetricBoundaryPolicy.shouldKeepRawLeaf(
                        rawSnapshot.sampleType(),
                        leaf.identifier(),
                        logicalChannelCodes
                ))
                .toList();
    }
    List<ManualLeafEvidence> canonicalLeaves = sourceLeaves.stream()
            .map(leaf -> canonicalizeCompositeLeaf(leaf, rawSnapshot.sampleType(), relationMappings))
            .filter(Objects::nonNull)
            .toList();
    return new ManualSampleSnapshot(
            rawSnapshot.deviceCode(),
            rawSnapshot.sampleType(),
            canonicalLeaves,
            rawSnapshot.ignoredFieldCount() + Math.max(0, sourceLeaves.size() - canonicalLeaves.size())
    );
}
```

- [ ] **Step 4: Re-run the same device test to verify it passes**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
```

Expected: PASS, and existing deep displacement / laser / crack composite child tests still stay green.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "feat(device): enforce collector child governance boundary"
```

## Task 2: Add collector child aggregate read API

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CollectorChildInsightService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightMetricVO.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImplTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightControllerTest.java`

- [ ] **Step 1: Write the failing service and controller tests**

```java
@Test
void getOverviewShouldAggregateChildLatestMetricsAndFormalSensorState() {
    when(deviceService.getDetailByCode(1001L, "SK00EA0D1307988")).thenReturn(parentDevice(1, "SK00EA0D1307988"));
    when(deviceRelationService.listByParentDeviceCode(1001L, "SK00EA0D1307988"))
            .thenReturn(List.of(relation("L1_LF_1", "202018108", "nf-monitor-laser-rangefinder-v1")));
    when(deviceService.getDetailByCode(1001L, "202018108")).thenReturn(childDevice("202018108", "1# 激光测点", 1));
    when(deviceService.listProperties(1001L, "202018108")).thenReturn(List.of(
            property("value", "激光测距值", "10.86", "mm", "2026-04-09 21:47:28"),
            property("sensor_state", "传感器状态", "0", null, "2026-04-09 21:47:28")
    ));

    CollectorChildInsightOverviewVO overview = service.getOverview(1001L, "SK00EA0D1307988");

    assertEquals(1, overview.getChildCount());
    assertEquals(1, overview.getReachableChildCount());
    assertEquals(1, overview.getSensorStateReportedCount());
    assertEquals("L1_LF_1", overview.getChildren().get(0).getLogicalChannelCode());
    assertEquals("reachable", overview.getChildren().get(0).getCollectorLinkState());
    assertEquals("0", overview.getChildren().get(0).getSensorStateValue());
    assertEquals("value", overview.getChildren().get(0).getMetrics().get(0).getIdentifier());
}

private DeviceDetailVO parentDevice(int onlineStatus, String deviceCode) {
    DeviceDetailVO vo = new DeviceDetailVO();
    vo.setDeviceCode(deviceCode);
    vo.setOnlineStatus(onlineStatus);
    return vo;
}

private DeviceDetailVO childDevice(String deviceCode, String deviceName, int onlineStatus) {
    DeviceDetailVO vo = new DeviceDetailVO();
    vo.setDeviceCode(deviceCode);
    vo.setDeviceName(deviceName);
    vo.setOnlineStatus(onlineStatus);
    vo.setLastReportTime("2026-04-09 21:47:28");
    return vo;
}

private DeviceRelationVO relation(String logicalChannelCode, String childDeviceCode, String childProductKey) {
    DeviceRelationVO vo = new DeviceRelationVO();
    vo.setLogicalChannelCode(logicalChannelCode);
    vo.setChildDeviceCode(childDeviceCode);
    vo.setChildProductKey(childProductKey);
    return vo;
}

private DeviceProperty property(String identifier, String name, String value, String unit, String reportTime) {
    DeviceProperty property = new DeviceProperty();
    property.setIdentifier(identifier);
    property.setPropertyName(name);
    property.setPropertyValue(value);
    property.setUnit(unit);
    property.setReportTime(reportTime);
    return property;
}
```

```java
@Test
void overviewEndpointShouldDelegateToCollectorInsightService() {
    CollectorChildInsightOverviewVO overview = new CollectorChildInsightOverviewVO();
    overview.setParentDeviceCode("SK00EA0D1307988");
    overview.setChildCount(1);
    when(collectorChildInsightService.getOverview(1001L, "SK00EA0D1307988")).thenReturn(overview);

    R<CollectorChildInsightOverviewVO> response = controller.getOverview("SK00EA0D1307988", authentication(1001L));

    assertEquals("SK00EA0D1307988", response.getData().getParentDeviceCode());
    assertEquals(1, response.getData().getChildCount());
    verify(collectorChildInsightService).getOverview(1001L, "SK00EA0D1307988");
}
```

- [ ] **Step 2: Run the collector insight tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=CollectorChildInsightServiceImplTest,DeviceCollectorInsightControllerTest" test
```

Expected: FAIL because the collector insight service, VO models, and controller do not exist yet.

- [ ] **Step 3: Implement the minimal read-side aggregate service and endpoint**

```java
public interface CollectorChildInsightService {

    CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode);
}
```

```java
@Service
public class CollectorChildInsightServiceImpl implements CollectorChildInsightService {

    @Override
    public CollectorChildInsightOverviewVO getOverview(Long currentUserId, String parentDeviceCode) {
        DeviceDetailVO parent = deviceService.getDetailByCode(currentUserId, parentDeviceCode);
        List<DeviceRelationVO> relations = deviceRelationService.listByParentDeviceCode(currentUserId, parentDeviceCode);
        List<CollectorChildInsightChildVO> children = relations.stream()
                .map(relation -> toChildOverview(currentUserId, parent, relation))
                .toList();

        CollectorChildInsightOverviewVO overview = new CollectorChildInsightOverviewVO();
        overview.setParentDeviceCode(parent.getDeviceCode());
        overview.setParentOnlineStatus(parent.getOnlineStatus());
        overview.setChildCount(children.size());
        overview.setReachableChildCount((int) children.stream()
                .filter(child -> "reachable".equals(child.getCollectorLinkState()))
                .count());
        overview.setSensorStateReportedCount((int) children.stream()
                .filter(child -> child.getSensorStateValue() != null && !child.getSensorStateValue().isBlank())
                .count());
        overview.setChildren(children);
        return overview;
    }

    private CollectorChildInsightChildVO toChildOverview(Long currentUserId,
                                                         DeviceDetailVO parent,
                                                         DeviceRelationVO relation) {
        DeviceDetailVO child = deviceService.getDetailByCode(currentUserId, relation.getChildDeviceCode());
        List<DeviceProperty> properties = deviceService.listProperties(currentUserId, relation.getChildDeviceCode());
        CollectorChildInsightChildVO item = new CollectorChildInsightChildVO();
        item.setLogicalChannelCode(relation.getLogicalChannelCode());
        item.setChildDeviceCode(relation.getChildDeviceCode());
        item.setChildDeviceName(child.getDeviceName());
        item.setChildProductKey(relation.getChildProductKey());
        item.setCollectorLinkState(resolveCollectorLinkState(parent, child));
        item.setSensorStateValue(resolvePropertyValue(properties, "sensor_state"));
        item.setMetrics(selectMonitoringMetrics(properties));
        item.setLastReportTime(child.getLastReportTime());
        return item;
    }
}
```

```java
@RestController
public class DeviceCollectorInsightController {

    @GetMapping("/api/device/{deviceCode}/collector-children/overview")
    public R<CollectorChildInsightOverviewVO> getOverview(@PathVariable String deviceCode,
                                                          Authentication authentication) {
        return R.ok(collectorChildInsightService.getOverview(requireCurrentUserId(authentication), deviceCode));
    }
}
```

- [ ] **Step 4: Re-run the collector insight tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=CollectorChildInsightServiceImplTest,DeviceCollectorInsightControllerTest" test
```

Expected: PASS, with the endpoint returning child latest metrics plus separate `collectorLinkState` and `sensorStateValue`.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/CollectorChildInsightService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightController.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightOverviewVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightChildVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/CollectorChildInsightMetricVO.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/CollectorChildInsightServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/DeviceCollectorInsightControllerTest.java
git commit -m "feat(device): add collector child insight overview api"
```

## Task 3: Add collector boundary guidance to the product workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Write the failing workbench tests**

```ts
it('shows collector boundary note in composite collector mode', async () => {
  const wrapper = mountWorkspace({
    product: {
      id: 6006,
      productKey: 'nf-monitor-collector-v1',
      productName: '南方测绘 监测型 采集器',
      protocolCode: 'mqtt-json',
      nodeType: 2
    }
  })

  await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')

  expect(wrapper.get('[data-testid="collector-boundary-note"]').text()).toContain('采集器产品只治理自身状态字段')
  expect(wrapper.get('[data-testid="collector-boundary-note"]').text()).toContain('子设备监测值和 sensor_state 请在对应子产品治理')
})

it('renders collector empty guidance when compare result is empty in composite collector mode', async () => {
  mockCompareProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      productId: 6006,
      summary: { propertyCount: 0, eventCount: 0, serviceCount: 0 },
      compareRows: []
    }
  })

  const wrapper = mountWorkspace({
    product: {
      id: 6006,
      productKey: 'nf-monitor-collector-v1',
      productName: '南方测绘 监测型 采集器',
      protocolCode: 'mqtt-json',
      nodeType: 2
    }
  })

  await flushPromises()
  await wrapper.get('[data-testid="device-structure-composite"]').trigger('click')
  await wrapper.get('[data-testid="composite-parent-device-code"]').setValue('SK00EA0D1307988')
  await wrapper.get('[data-testid="contract-field-sample-input"]').setValue(
    '{"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}'
  )
  await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')

  expect(wrapper.get('[data-testid="collector-boundary-empty"]').text()).toContain('请在对应子产品中治理子设备正式字段')
})
```

- [ ] **Step 2: Run the workbench test to verify it fails**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: FAIL because the workbench does not yet surface collector-only boundary guidance.

- [ ] **Step 3: Implement the minimal collector guidance UI**

```vue
<el-alert
  v-if="isCollectorCompositeMode"
  data-testid="collector-boundary-note"
  type="warning"
  :closable="false"
  title="采集器产品只治理自身状态字段；子设备监测值和 sensor_state 请在对应子产品治理。"
/>
```

```vue
<div v-else-if="isCollectorCompositeMode" class="product-model-designer__empty" data-testid="collector-boundary-empty">
  <strong>当前采集器没有可治理的子设备正式字段</strong>
  <p>采集器总览可以查看子设备最新值和状态，但正式字段请在对应子产品中治理。</p>
</div>
```

```ts
const isCollectorCompositeMode = computed(() =>
  Number(props.product?.nodeType) === 2 && deviceStructure.value === 'composite'
)
```

- [ ] **Step 4: Re-run the workbench test to verify it passes**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS, and the workbench explicitly teaches users that collector pages are read-side aggregate views, not collector-owned formal contract fields.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat(ui): add collector boundary guidance to product workbench"
```

## Task 4: Add collector aggregate panel to the device insight view

**Files:**
- Create: `spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue`
- Modify: `spring-boot-iot-ui/src/api/iot.ts`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: Write the failing device insight test**

```ts
it('renders collector child aggregate panel without merging child metrics into collector snapshot', async () => {
  vi.mocked(getCollectorChildInsightOverview).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      parentDeviceCode: 'SK00EA0D1307988',
      parentOnlineStatus: 1,
      childCount: 1,
      reachableChildCount: 1,
      sensorStateReportedCount: 1,
      children: [
        {
          logicalChannelCode: 'L1_LF_1',
          childDeviceCode: '202018108',
          childDeviceName: '1# 激光测点',
          childProductKey: 'nf-monitor-laser-rangefinder-v1',
          collectorLinkState: 'reachable',
          sensorStateValue: '0',
          lastReportTime: '2026-04-09 21:47:28',
          metrics: [
            {
              identifier: 'value',
              displayName: '激光测距值',
              propertyValue: '10.86',
              unit: 'mm',
              reportTime: '2026-04-09 21:47:28'
            }
          ]
        }
      ]
    }
  })

  mockRoute.query = { deviceCode: 'SK00EA0D1307988' }
  const wrapper = mountView()
  await flushPromises()

  expect(wrapper.text()).toContain('子设备总览')
  expect(wrapper.text()).toContain('L1_LF_1')
  expect(wrapper.text()).toContain('激光测距值')
  expect(wrapper.text()).toContain('链路可达')
  expect(wrapper.text()).not.toContain('把子设备指标写回采集器正式字段')
})
```

- [ ] **Step 2: Run the device insight test to verify it fails**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts
```

Expected: FAIL because the device insight page does not yet request or render collector child aggregate data.

- [ ] **Step 3: Implement the minimal collector aggregate API wiring and panel**

```ts
export interface CollectorChildInsightMetric {
  identifier: string
  displayName?: string | null
  propertyValue?: string | null
  unit?: string | null
  reportTime?: string | null
}

export interface CollectorChildInsightChild {
  logicalChannelCode: string
  childDeviceCode: string
  childDeviceName?: string | null
  childProductKey?: string | null
  collectorLinkState: string
  sensorStateValue?: string | null
  lastReportTime?: string | null
  metrics: CollectorChildInsightMetric[]
}

export interface CollectorChildInsightOverview {
  parentDeviceCode: string
  parentOnlineStatus?: number | null
  childCount: number
  reachableChildCount: number
  sensorStateReportedCount: number
  children: CollectorChildInsightChild[]
}
```

```ts
export function getCollectorChildInsightOverview(deviceCode: string): Promise<ApiEnvelope<CollectorChildInsightOverview>> {
  return request<CollectorChildInsightOverview>(`/api/device/${deviceCode}/collector-children/overview`)
}
```

```vue
<CollectorChildInsightPanel
  v-if="collectorOverview?.children?.length"
  :overview="collectorOverview"
/>
```

```ts
const collectorOverview = ref<CollectorChildInsightOverview | null>(null)

async function loadCollectorOverview(code: string) {
  const response = await getCollectorChildInsightOverview(code)
  collectorOverview.value = response.data ?? null
}
```

- [ ] **Step 4: Re-run the device insight test to verify it passes**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/DeviceInsightView.test.ts
```

Expected: PASS, with collector pages showing child latest metrics, child formal `sensor_state`, and separate collector-link reachability.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-ui/src/components/device/CollectorChildInsightPanel.vue spring-boot-iot-ui/src/api/iot.ts spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts
git commit -m "feat(ui): add collector child aggregate insight panel"
```

## Task 5: Sync docs and run focused verification

**Files:**
- Review: `README.md`
- Review: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the approved boundary and API statements to docs**

```md
- `2026-04-11` 起，复合采集器治理边界明确收口：
  - 采集器产品只治理采集器自身运行态/通讯态字段，不再承载子设备监测值或子设备正式状态 `sensor_state`
  - 激光测距、深部位移等子设备继续在子产品或单台产品中治理正式字段，保持 `value / sensor_state`、`dispsX / dispsY / sensor_state` 口径不变
  - 采集器页允许通过读侧聚合查看全部子设备最近监测值、最近正式状态和链路可达性，但这些聚合结果不会回写采集器合同字段
- 新增只读接口：`GET /api/device/{deviceCode}/collector-children/overview`
  - 返回采集器下子设备总数、链路可达数、已上报正式状态数，以及每个逻辑通道对应的子设备最近监测值和 `sensor_state`
  - `collectorLinkState` 与 `sensorStateValue` 必须分开展示，前者表示采集器链路可达性，后者表示子设备正式健康态
```

- [ ] **Step 2: Review `README.md` and `AGENTS.md`, then update if their current-state sections mention this capability slice**

Use these checks before editing:

```powershell
Select-String -Path README.md -Pattern '契约字段|对象洞察|采集器'
Select-String -Path AGENTS.md -Pattern '契约字段|对象洞察|采集器'
```

If `Select-String` returns hits, append one concise bullet to the existing recent-update or current-state list.
If `Select-String` returns no hits, leave `README.md` and `AGENTS.md` unchanged and record that decision in the task notes instead of creating a parallel section.

- [ ] **Step 3: Run the focused regression pack**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest,CollectorChildInsightServiceImplTest,DeviceCollectorInsightControllerTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/DeviceInsightView.test.ts
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
git diff --check
```

Expected:

- `ProductModelServiceImplTest` PASS
- `CollectorChildInsightServiceImplTest` PASS
- `DeviceCollectorInsightControllerTest` PASS
- `ProductModelDesignerWorkspace.test.ts` PASS
- `DeviceInsightView.test.ts` PASS
- `spring-boot-iot-admin` packaging succeeds
- `git diff --check` returns no whitespace or merge-marker errors

- [ ] **Step 4: Commit**

```powershell
git add README.md AGENTS.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document collector child status boundary"
```

## Self-Review

Spec coverage:

1. Collector products no longer absorb child monitoring metrics or child formal `sensor_state` through Task 1.
2. Single-device and child-device products keep the same formal model because Task 1 changes only collector composite handling and leaves existing deep displacement / laser child tests intact.
3. Collector pages can still show all child latest metrics and states through Task 2 and Task 4 without duplicating formal contract fields.
4. Product workbench user guidance is covered in Task 3 so the UI teaches the boundary instead of silently returning empty compare rows.
5. Required documentation and verification are covered in Task 5.

Placeholder scan:

1. No `TODO`, `TBD`, or “implement later” placeholders remain.
2. Each task lists concrete files, exact commands, and concrete code/test snippets.

Type consistency:

1. Child formal identifiers remain `value`, `dispsX`, `dispsY`, and `sensor_state`.
2. Collector read-side aggregation uses `collectorLinkState` for upstream reachability and `sensorStateValue` for child formal status so the two semantics do not collide.
3. The new backend endpoint and frontend API both use `/api/device/{deviceCode}/collector-children/overview`.
