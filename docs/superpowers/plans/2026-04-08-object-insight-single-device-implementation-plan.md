# 对象洞察台单设备分析重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/insight` 重构为基于 TDengine 的单设备对象洞察台，支持从设备资产中心带 `deviceCode` 跳转、按固定时间维度展示折线趋势、按设备能力差异化展示客户可读的基础档案与核心指标。

**Architecture:** 后端继续保持模块边界：`spring-boot-iot-telemetry` 仅新增通用批量历史查询接口，按设备和指标列表返回补零后的时序桶；风险上下文仍沿用 `risk-monitoring`，设备主档与快照继续沿用 `device` 现有接口。前端采用“能力注册表 + 页面编排”方式，在 `DeviceInsightView` 里统一编排基础档案、核心指标、监测数据趋势、状态数据趋势和图文分析，所有客户可见文案统一使用中文业务名称。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, TDengine, Vue 3, TypeScript, Element Plus, ECharts, Vitest, Maven

---

## 背景约束

- 当前分支必须保持在 `codex/dev`，不得切到 `master` 开发。
- 当前仓库工作区存在其他未收敛改动，本计划执行时必须精确暂存本次文件，避免误带无关改动。
- 趋势图主数据必须来自 TDengine，不能再用消息日志拼趋势。
- `/insight` 直开时搜索框必须为空，且不自动请求默认设备。
- 从设备资产中心进入时，必须带 `deviceCode` 并自动加载。
- 时间维度固定为 `近一天 / 近一周 / 近一月 / 近一季度 / 近一年`，默认 `近一周`。
- 缺失桶必须补 `0`，不允许折线中断。
- 对客户可见的核心指标、趋势标题、分析文案不得直接暴露 `L4_NW_1`、`S1_ZT_1.sensor_state.L4_NW_1` 等内部标识。

## File Map

### Backend

- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/controller/TelemetryController.java`
  - 增加 `/api/telemetry/history/batch` 控制器入口。
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/TelemetryQueryService.java`
  - 暴露批量历史查询服务方法。
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
  - 实现范围映射、桶对齐、补零、seriesType 分类。
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchRequest.java`
  - 定义 deviceId、identifiers、rangeCode、fillPolicy。
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchResponse.java`
  - 返回 `deviceId/rangeCode/bucket/points`。
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchSeries.java`
  - 返回单指标序列、系列类别、显示名与 buckets。
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBucketPoint.java`
  - 返回 `time/value/filled` 单桶数据。
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/controller/TelemetryControllerTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

### Frontend

- Create: `spring-boot-iot-ui/src/api/telemetry.ts`
  - 定义批量历史查询 API 和前端响应类型。
- Create: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
  - 定义能力注册表、时间档位、中文展示名、系统自定义参数位。
- Modify: `spring-boot-iot-ui/src/utils/deviceInsight.ts`
  - 统一对象类型、分析文案、核心卡片文案辅助方法。
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
  - 重构页面语义、路由加载规则、请求并发控制和美观布局。
- Modify: `spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue`
  - 改为双分组折线图组件，支持监测数据 / 状态数据、补零提示、中文图例。
- Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

### Docs

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

---

### Task 1: 实现 TDengine 批量历史查询接口

**Files:**
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchRequest.java`
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchResponse.java`
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchSeries.java`
- Create: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBucketPoint.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/controller/TelemetryController.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/TelemetryQueryService.java`
- Modify: `spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/controller/TelemetryControllerTest.java`
- Test: `spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java`

- [ ] **Step 1: 先写失败测试，锁定控制器委托与补零行为**

```java
@Test
void historyBatchShouldDelegateToQueryService() {
    TelemetryQueryService telemetryQueryService = mock(TelemetryQueryService.class);
    TelemetryHistoryMigrationService migrationService = mock(TelemetryHistoryMigrationService.class);
    TelemetryController controller = new TelemetryController(telemetryQueryService, migrationService);

    TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
    request.setDeviceId(2001L);
    request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
    request.setRangeCode("7d");
    request.setFillPolicy("ZERO");

    TelemetryHistoryBatchResponse payload = new TelemetryHistoryBatchResponse();
    payload.setDeviceId(2001L);
    payload.setRangeCode("7d");
    when(telemetryQueryService.getHistoryBatch(request)).thenReturn(payload);

    R<TelemetryHistoryBatchResponse> response = controller.historyBatch(request);

    assertEquals(payload, response.getData());
    verify(telemetryQueryService).getHistoryBatch(request);
}

@Test
void getHistoryBatchShouldZeroFillRequestedBuckets() {
    Device device = buildDevice();
    Product product = buildProduct();
    when(deviceMapper.selectOne(any())).thenReturn(device);
    when(productMapper.selectById(1001L)).thenReturn(product);
    when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
    when(telemetryReadRouter.latestSource()).thenReturn("v2");
    when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
    when(normalizedTelemetryHistoryReader.listHistory(eq(device), eq(product), anyMap(), anyInt())).thenReturn(List.of(
        point("L4_NW_1", 2.6D, LocalDateTime.of(2026, 4, 7, 0, 0)),
        point("S1_ZT_1.sensor_state.L4_NW_1", 1L, LocalDateTime.of(2026, 4, 7, 0, 0))
    ));

    TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
    request.setDeviceId(2001L);
    request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
    request.setRangeCode("7d");
    request.setFillPolicy("ZERO");

    TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

    assertEquals(2, result.getPoints().size());
    assertEquals(7, result.getPoints().get(0).getBuckets().size());
    assertEquals(0D, result.getPoints().get(0).getBuckets().get(0).getValue());
    assertEquals("measure", result.getPoints().get(0).getSeriesType());
    assertEquals("status", result.getPoints().get(1).getSeriesType());
}
```

- [ ] **Step 2: 运行后端单测，确认当前接口尚不存在而失败**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryControllerTest,TelemetryQueryServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- `TelemetryControllerTest` 因 `historyBatch` 方法不存在而失败。
- `TelemetryQueryServiceImplTest` 因 `getHistoryBatch`、DTO 或依赖不存在而失败。

- [ ] **Step 3: 写最小实现，让接口能按范围生成桶并补零**

```java
@PostMapping("/api/telemetry/history/batch")
public R<TelemetryHistoryBatchResponse> historyBatch(@RequestBody TelemetryHistoryBatchRequest request) {
    return R.ok(telemetryQueryService.getHistoryBatch(request));
}

public interface TelemetryQueryService {
    Map<String, Object> getLatest(Long deviceId);
    TelemetryHistoryBatchResponse getHistoryBatch(TelemetryHistoryBatchRequest request);
}

@Override
public TelemetryHistoryBatchResponse getHistoryBatch(TelemetryHistoryBatchRequest request) {
    Device device = requireDevice(request.getDeviceId());
    Product product = device.getProductId() == null ? null : productMapper.selectById(device.getProductId());
    List<TelemetryV2Point> history = readTdengineHistory(device, product);
    List<BucketSlot> slots = buildSlots(request.getRangeCode(), LocalDateTime.now());
    return assembleBatchResponse(device, request, history, slots);
}
```

Implementation notes:
- `TelemetryHistoryBatchRequest` 只接受 `deviceId`、`identifiers`、`rangeCode`、`fillPolicy`，`bucket` 由后端根据 `rangeCode` 固化生成，避免前后端桶粒度跑偏。
- `rangeCode -> bucket` 固定映射：`1d -> hour`、`7d -> day`、`30d -> day`、`90d -> week`、`365d -> month`。
- 先走 `NormalizedTelemetryHistoryReader.listHistory(...)`；若 `latestSource=v2` 且允许 legacy fallback，再用 `LegacyTelemetryHistoryReader.listHistory(...)` 补齐缺指标。
- 只过滤当前请求的 `identifiers`，每桶保留该桶最后一个值；空桶统一输出 `0D` 且标记 `filled=true`。
- `seriesType` 统一输出 `measure/status`，根据 `TelemetryStreamKind.resolve(...)` 或 identifier 关键字判断。
- 非 TDengine 模式直接抛 `BizException("当前环境未启用 TDengine 历史查询")`，不回退消息日志。

- [ ] **Step 4: 重新运行后端单测，确认控制器和服务用例转绿**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryControllerTest,TelemetryQueryServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected:
- `BUILD SUCCESS`
- 两个测试类通过，且断言覆盖设备不存在、空指标、补零、seriesType 分类。

- [ ] **Step 5: 提交后端批量查询基础改动**

```powershell
git add spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/controller/TelemetryController.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/TelemetryQueryService.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImpl.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchRequest.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchResponse.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBatchSeries.java `
        spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/dto/TelemetryHistoryBucketPoint.java `
        spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/controller/TelemetryControllerTest.java `
        spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java
git commit -m "feat: add telemetry history batch query"
```

### Task 2: 建立对象洞察能力注册表与前端批量历史查询客户端

**Files:**
- Create: `spring-boot-iot-ui/src/api/telemetry.ts`
- Create: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Modify: `spring-boot-iot-ui/src/utils/deviceInsight.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts`

- [ ] **Step 1: 先写失败测试，锁定样板设备模板和中文展示名**

```ts
import { describe, expect, it } from 'vitest';
import {
  DEFAULT_INSIGHT_RANGE,
  buildInsightHistoryRequest,
  getInsightCapabilityProfile
} from '@/utils/deviceInsightCapability';

describe('deviceInsightCapability', () => {
  it('matches muddy-water sample device with chinese metrics only', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'SK00EB0D1308313',
      productName: '宏观现象监测设备泥水位'
    });

    expect(profile.heroMetrics.map((item) => item.displayName)).toEqual([
      '泥水位高程',
      '传感器在线状态',
      '剩余电量'
    ]);
    expect(profile.trendGroups.map((item) => item.title)).toEqual(['监测数据', '状态数据']);
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('相对湿度');
  });

  it('builds telemetry batch request with default weekly range and zero fill', () => {
    const profile = getInsightCapabilityProfile({ deviceCode: 'SK00EB0D1308313' });
    const request = buildInsightHistoryRequest(2001, profile, DEFAULT_INSIGHT_RANGE);

    expect(request.rangeCode).toBe('7d');
    expect(request.fillPolicy).toBe('ZERO');
    expect(request.identifiers).toContain('L4_NW_1');
    expect(request.identifiers).toContain('S1_ZT_1.battery_dump_energy');
  });
});
```

- [ ] **Step 2: 运行前端工具测试，确认当前能力注册表不存在而失败**

Run:

```powershell
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/utils/deviceInsight.test.ts
```

Expected:
- `deviceInsightCapability.ts` 导入失败或断言失败。
- `deviceInsight.test.ts` 仍停留在旧对象分类逻辑。

- [ ] **Step 3: 写最小实现，固化时间档位、模板匹配和 API 客户端**

```ts
export const INSIGHT_RANGE_OPTIONS = [
  { label: '近一天', value: '1d' },
  { label: '近一周', value: '7d' },
  { label: '近一月', value: '30d' },
  { label: '近一季度', value: '90d' },
  { label: '近一年', value: '365d' }
] as const;

export const DEFAULT_INSIGHT_RANGE = '7d';

export function getInsightCapabilityProfile(source: { deviceCode?: string | null; productName?: string | null }) {
  if (source.deviceCode === 'SK00EB0D1308313') {
    return muddyWaterLevelProfile;
  }
  return genericMonitoringProfile;
}

export function buildInsightHistoryRequest(deviceId: number | string, profile: InsightCapabilityProfile, rangeCode: InsightRangeCode) {
  return {
    deviceId: Number(deviceId),
    identifiers: profile.historyIdentifiers,
    rangeCode,
    fillPolicy: 'ZERO'
  };
}

export function getTelemetryHistoryBatch(payload: TelemetryHistoryBatchRequest) {
  return request<TelemetryHistoryBatchResponse>('/api/telemetry/history/batch', {
    method: 'POST',
    body: payload
  });
}
```

Implementation notes:
- `heroMetrics` 使用 `{ identifier, displayName, group }` 结构，避免页面层手写中文映射。
- `trendGroups` 明确分为 `measure` / `status` 两类，后续不同设备只补配置不改视图判断。
- `extensionParameters` 名称固定使用“系统自定义参数”。
- `deviceInsight.ts` 保留对象类型、风险级别、分析摘要等纯展示函数，不再承担模板定义。

- [ ] **Step 4: 重新运行前端工具测试，确认模板与默认档位转绿**

Run:

```powershell
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/utils/deviceInsight.test.ts
```

Expected:
- 两个工具测试文件全部通过。
- 样板设备命中中文模板，默认范围固定为 `近一周`。

- [ ] **Step 5: 提交前端能力注册表基础改动**

```powershell
git add spring-boot-iot-ui/src/api/telemetry.ts `
        spring-boot-iot-ui/src/utils/deviceInsightCapability.ts `
        spring-boot-iot-ui/src/utils/deviceInsight.ts `
        spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts `
        spring-boot-iot-ui/src/__tests__/utils/deviceInsight.test.ts
git commit -m "feat: add device insight capability registry"
```

### Task 3: 重构对象洞察台页面与双分组折线图

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 先写失败测试，锁定空态、跳转自动加载、中文指标与双分组折线图**

```ts
it('keeps direct-open insight idle until user inputs device code', async () => {
  mockRoute.query = {};
  const wrapper = mountView();
  await flushPromises();

  expect(getDeviceByCode).not.toHaveBeenCalled();
  expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
  expect(wrapper.text()).toContain('请输入设备编码后开始综合分析');
});

it('auto-loads device insight when device workbench passes deviceCode', async () => {
  mockRoute.query = { deviceCode: 'SK00EB0D1308313' };
  const wrapper = mountView();
  await flushPromises();

  expect(getDeviceByCode).toHaveBeenCalledWith('SK00EB0D1308313');
  expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
    rangeCode: '7d',
    fillPolicy: 'ZERO'
  }));
  expect(wrapper.text()).toContain('基础档案信息');
  expect(wrapper.text()).toContain('泥水位高程');
  expect(wrapper.text()).not.toContain('L4_NW_1');
});

it('renders measure and status line groups without exposing english identifiers', () => {
  const wrapper = mountTrend({
    title: '监测趋势',
    groups: [
      {
        title: '监测数据',
        series: [{ displayName: '泥水位高程', buckets: [{ time: '2026-04-07 00:00:00', value: 2.1, filled: false }] }]
      },
      {
        title: '状态数据',
        series: [{ displayName: '传感器在线状态', buckets: [{ time: '2026-04-07 00:00:00', value: 1, filled: true }] }]
      }
    ]
  });

  expect(wrapper.text()).toContain('监测数据');
  expect(wrapper.text()).toContain('状态数据');
  expect(wrapper.text()).not.toContain('S1_ZT_1.sensor_state.L4_NW_1');
});
```

- [ ] **Step 2: 运行对象洞察相关前端测试，确认旧页面合同已不满足**

Run:

```powershell
cd spring-boot-iot-ui
npm test -- src/__tests__/views/DeviceInsightView.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts
```

Expected:
- 直开空态测试失败，因为当前页面会自动带 `demo-device-01`。
- 组件测试失败，因为当前趋势仍依赖 `logs/detail` 且直接显示内部 identifier。

- [ ] **Step 3: 写最小实现，完成页面语义重构和折线图区改造**

```ts
const deviceCode = ref(resolveRouteDeviceCode(route.query.deviceCode));
const selectedRange = ref(DEFAULT_INSIGHT_RANGE);
const requestVersion = ref(0);

async function loadInsight(source: 'direct' | 'device-workbench' | 'route-change' | 'range-change') {
  const normalizedCode = deviceCode.value.trim();
  if (!normalizedCode) {
    resetInsightState();
    return;
  }

  const currentVersion = ++requestVersion.value;
  const deviceResponse = await getDeviceByCode(normalizedCode);
  if (currentVersion !== requestVersion.value) return;

  const capability = getInsightCapabilityProfile(deviceResponse.data);
  const historyResponse = await getTelemetryHistoryBatch(
    buildInsightHistoryRequest(deviceResponse.data.id!, capability, selectedRange.value)
  );
  if (currentVersion !== requestVersion.value) return;

  insightTrendGroups.value = buildTrendGroups(historyResponse.data, capability);
}
```

```vue
<RiskInsightTrendPanel
  :range-code="selectedRange"
  :groups="insightTrendGroups"
  :summary="trendSummary"
  :empty-message="trendEmptyMessage"
/>
```

Implementation notes:
- `resolveRouteDeviceCode()` 只在 query 中有有效 `deviceCode` 时返回值，否则返回空字符串，不再注入 `demo-device-01`。
- 顶部筛选区新增时间档位选择器，默认 `DEFAULT_INSIGHT_RANGE`；切换仅重拉 TDengine 批量历史。
- 页面分区固定为：设备画像区、基础档案信息（设备基础档案 / 风险上下文档案）、核心指标、双分组折线图、图文分析、属性快照。
- 核心指标卡和趋势标题都消费能力注册表里的 `displayName`，不直接显示 identifier；证据表格可以保留 identifier 列。
- `RiskInsightTrendPanel.vue` 只消费聚合后的 `groups`，不再接收 `logs` 作为趋势源。
- 图表 `series.smooth = false`，tooltip 对补零桶显示“补零补齐”。
- 当风险上下文为空时，风险档案卡片保留，但显示“当前未纳入风险监测”。
- 视觉上复用 `StandardPageShell / StandardWorkbenchPanel / PanelCard / MetricCard`，避免再造私有壳层。

- [ ] **Step 4: 重新运行对象洞察相关前端测试，确认新页面合同转绿**

Run:

```powershell
cd spring-boot-iot-ui
npm test -- src/__tests__/views/DeviceInsightView.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts
```

Expected:
- 三个测试文件通过。
- 断言覆盖：空态不自动请求、带 `deviceCode` 自动加载、时间切换触发批量查询、中文业务名称替代英文标识、趋势分组为监测数据与状态数据。

- [ ] **Step 5: 提交对象洞察台前端重构改动**

```powershell
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue `
        spring-boot-iot-ui/src/components/RiskInsightTrendPanel.vue `
        spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts `
        spring-boot-iot-ui/src/__tests__/components/RiskInsightTrendPanel.test.ts `
        spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: rebuild device insight workbench"
```

### Task 4: 原位更新文档并完成最终验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 更新业务、接口、前端治理和验收文档**

```md
- `docs/02-业务功能与流程说明.md`
  - 把对象洞察台描述改为“单设备对象洞察台”，补充两种进入方式和基础档案区。
- `docs/03-接口规范与接口清单.md`
  - 新增 `POST /api/telemetry/history/batch` 请求/响应示例、rangeCode 与补零说明。
- `docs/06-前端开发与CSS规范.md`
  - 记录对象洞察页继续复用共享壳层和双列趋势区表达，不新增私有壳层。
- `docs/08-变更记录与技术债清单.md`
  - 记录本轮已移除“消息日志拼趋势”的做法，后续技术债转为模板后台化。
- `docs/15-前端优化与治理计划.md`
  - 记录对象洞察页的客户化布局、中文指标展示、时间维度折线图区。
- `docs/21-业务功能清单与验收标准.md`
  - 补充 `/insight` 直开空态、资产中心跳转、TDengine 趋势、补零不断线的验收点。
```

- [ ] **Step 2: 跑后端与前端目标验证命令**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-telemetry "-DskipTests=false" "-Dtest=TelemetryControllerTest,TelemetryQueryServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
cd spring-boot-iot-ui
npm test -- src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/utils/deviceInsight.test.ts src/__tests__/components/RiskInsightTrendPanel.test.ts src/__tests__/views/DeviceInsightView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts
```

Expected:
- 后端测试通过。
- 前端对象洞察相关测试通过。

- [ ] **Step 3: 如环境允许，再做一次真实环境最小回归**

Run:

```powershell
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Manual checks:
- 设备资产中心点击“更多 -> 洞察”后，`/insight?deviceCode=SK00EB0D1308313` 自动打开并加载。
- 直开 `/insight` 搜索框为空，无默认设备。
- 默认时间档位是“近一周”。
- 趋势图分为“监测数据 / 状态数据”两类，且无断线。
- 核心指标仅显示“泥水位高程 / 传感器在线状态 / 剩余电量”等中文名称。

- [ ] **Step 4: 提交文档与最终收口改动**

```powershell
git add docs/02-业务功能与流程说明.md `
        docs/03-接口规范与接口清单.md `
        docs/06-前端开发与CSS规范.md `
        docs/08-变更记录与技术债清单.md `
        docs/15-前端优化与治理计划.md `
        docs/21-业务功能清单与验收标准.md
git commit -m "docs: update device insight workbench docs"
```

## 风险与止损点

- 如果 `TelemetryQueryServiceImpl` 无法稳定读取历史点位，不要回退到消息日志拼趋势；应显式返回“TDengine 趋势查询失败”。
- 如果 `rangeCode` 对应桶粒度在真实数据密度下表现不佳，只允许在服务端统一映射调整，不允许前端自行拼时间桶。
- 如果页面快速切换设备时出现串页，必须优先补 `requestVersion` 防覆盖，不能靠延时规避。
- 如果中文展示名缺失，页面优先回退到“监测值 / 状态值 / 系统自定义参数”这类中文兜底名，不直接把 identifier 暴露给客户。

## 完成定义

- `/insight` 已明确成为单设备对象洞察台，不再默认加载 demo 设备。
- 设备资产中心跳转能自动带入 `deviceCode`。
- 趋势数据来自 TDengine 批量历史接口，并按固定时间维度补零展示。
- 页面稳定展示设备基础档案、风险上下文档案、核心指标、监测数据趋势、状态数据趋势和图文分析。
- 样板设备 `SK00EB0D1308313` 的三项重点指标全部以中文业务名称展示。
- 相关文档、接口说明和验收标准已同步更新。
