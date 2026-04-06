# GNSS Semantic Contract Wave Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved `phase2-gnss` semantic-contract slice and preserve `riskMetricId` through the threshold-rule stack without breaking the real `application-dev.yml` baseline.

**Architecture:** This wave copies the verified crack-governance method into a second formal scenario: `phase2-gnss`. The device domain owns scenario recognition, compare/apply decoration, evidence capture, and release seeding; the alarm domain owns risk-metric publication and `rule_definition` validation; the UI only upgrades existing pages to retain and submit `riskMetricId` rather than inventing a second workflow.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Vue 3, Element Plus, Vitest, JUnit 5, Mockito

---

## File Structure

### Task 1 ownership: GNSS semantic-contract governance in `spring-boot-iot-device`

- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

### Task 2 ownership: risk catalog / device metric / threshold-rule backend

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceMetricOptionVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`

### Task 3 ownership: threshold-rule UI and API typing

- Modify: `spring-boot-iot-ui/src/api/ruleDefinition.ts`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RuleDefinitionView.test.ts`

### Task 4 ownership: docs sync

- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `AGENTS.md`

## Task 1: Add `phase2-gnss` scenario recognition and compare/apply behavior

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Write the failing GNSS compare/apply tests**

```java
@Test
void compareGovernanceShouldDecorateGnssRowsWithNormativeMetadata() {
    when(productMapper.selectById(3003L)).thenReturn(product(3003L, "gnss-monitor-v1", "GNSS位移监测仪"));
    when(productModelMapper.selectList(any())).thenReturn(List.of());
    when(normativeMetricDefinitionService.listByScenario("phase2-gnss")).thenReturn(List.of(
            normativeDefinition("gpsInitial", "GNSS 原始观测基础数据", 0),
            normativeDefinition("gpsTotalX", "GNSS 累计位移 X", 1),
            normativeDefinition("gpsTotalY", "GNSS 累计位移 Y", 1),
            normativeDefinition("gpsTotalZ", "GNSS 累计位移 Z", 1)
    ));

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("business");
    manualExtract.setDeviceStructure("single");
    manualExtract.setSamplePayload("""
            {"device-gnss-01":{"gpsTotalX":{"2026-04-06T08:00:00.000Z":12.6},"gpsTotalY":{"2026-04-06T08:00:00.000Z":3.2}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(3003L, dto);

    assertEquals(List.of("gpsTotalX", "gpsTotalY"),
            result.getCompareRows().stream().map(ProductModelGovernanceCompareRowVO::getIdentifier).toList());
    assertEquals("GNSS 累计位移 X", result.getCompareRows().get(0).getNormativeName());
    assertTrue(Boolean.TRUE.equals(result.getCompareRows().get(0).getRiskReady()));
}

@Test
void applyGovernanceShouldCreateGnssReleaseBatch() {
    when(productMapper.selectById(3003L)).thenReturn(product(3003L, "gnss-monitor-v1", "GNSS位移监测仪"));
    when(productModelMapper.selectOne(any())).thenReturn(null);
    when(productContractReleaseService.createBatch(3003L, "phase2-gnss", "manual_compare_apply", 1, 0L))
            .thenReturn(22345L);

    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(applyItem("create", null, "property", "gpsTotalX", "GNSS 累计位移 X")));

    ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(3003L, dto);

    assertEquals(22345L, result.getReleaseBatchId());
}
```

- [ ] **Step 2: Run the targeted device tests and confirm RED**

Run:

```bash
mvn -pl spring-boot-iot-device -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest test
```

Expected: `BUILD FAILURE`, with failures showing missing `phase2-gnss` scenario resolution or missing normative decoration.

- [ ] **Step 3: Implement the minimal GNSS scenario support**

```sql
INSERT INTO iot_normative_metric_definition (
    id, tenant_id, scenario_code, device_family, identifier, display_name, unit, precision_digits,
    monitor_content_code, monitor_type_code, risk_enabled, trend_enabled, metadata_json
) VALUES
    (920011, 1, 'phase2-gnss', 'GNSS', 'gpsInitial', 'GNSS 原始观测基础数据', NULL, 0, 'L1', 'GP', 0, 0, JSON_OBJECT('usage', 'raw_observation')),
    (920012, 1, 'phase2-gnss', 'GNSS', 'gpsTotalX', 'GNSS 累计位移 X', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920013, 1, 'phase2-gnss', 'GNSS', 'gpsTotalY', 'GNSS 累计位移 Y', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920014, 1, 'phase2-gnss', 'GNSS', 'gpsTotalZ', 'GNSS 累计位移 Z', 'mm', 4, 'L1', 'GP', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920015, 1, 'phase2-gnss', 'GNSS', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0, JSON_OBJECT('usage', 'health_state'));
```

```java
static final String SCENARIO_PHASE2_GNSS = "phase2-gnss";

String resolveScenarioCode(Product product) {
    if (product == null) {
        return null;
    }
    if (matchesGnss(product.getProductKey())
            || matchesGnss(product.getProductName())
            || matchesGnss(product.getManufacturer())
            || matchesGnss(product.getDescription())) {
        return SCENARIO_PHASE2_GNSS;
    }
    if (matchesCrack(product.getProductKey())
            || matchesCrack(product.getProductName())
            || matchesCrack(product.getManufacturer())
            || matchesCrack(product.getDescription())) {
        return SCENARIO_PHASE1_CRACK;
    }
    return null;
}

private boolean matchesGnss(String value) {
    if (!StringUtils.hasText(value)) {
        return false;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.contains("gnss") || value.contains("北斗") || value.contains("卫星");
}
```

- [ ] **Step 4: Re-run the same device tests and confirm GREEN**

Run:

```bash
mvn -pl spring-boot-iot-device -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest test
```

Expected: `BUILD SUCCESS`

## Task 2: Publish GNSS risk metrics and keep `riskMetricId` in backend responses

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/DeviceMetricOptionVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RuleDefinitionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`

- [ ] **Step 1: Add failing backend tests**

```java
@Test
void publishFromReleasedContractsShouldIgnoreGpsInitialButPublishGnssTotals() {
    RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(riskMetricCatalogMapper);

    ProductModel gpsInitial = new ProductModel();
    gpsInitial.setId(3201L);
    gpsInitial.setProductId(3003L);
    gpsInitial.setIdentifier("gpsInitial");
    gpsInitial.setModelName("GNSS 原始观测基础数据");

    ProductModel gpsTotalX = new ProductModel();
    gpsTotalX.setId(3202L);
    gpsTotalX.setProductId(3003L);
    gpsTotalX.setIdentifier("gpsTotalX");
    gpsTotalX.setModelName("GNSS 累计位移 X");

    service.publishFromReleasedContracts(3003L, List.of(gpsInitial, gpsTotalX), Set.of("gpsTotalX"));

    verify(riskMetricCatalogMapper).insert(argThat(row -> "gpsTotalX".equals(row.getContractIdentifier())));
    verify(riskMetricCatalogMapper, never()).insert(argThat(row -> "gpsInitial".equals(row.getContractIdentifier())));
}

@Test
void addRuleShouldKeepRiskMetricIdWhenExpressionIsExecutable() {
    RuleDefinitionServiceImpl service = spy(new RuleDefinitionServiceImpl());
    RuleDefinition rule = new RuleDefinition();
    rule.setRuleName("GNSS 红色策略");
    rule.setRiskMetricId(6102L);
    rule.setMetricIdentifier("gpsTotalX");
    rule.setExpression("value >= 12");
    rule.setAlarmLevel("critical");
    rule.setStatus(0);

    doReturn(true).when(service).save(any(RuleDefinition.class));

    service.addRule(rule);

    assertEquals(6102L, rule.getRiskMetricId());
    verify(service).save(rule);
}
```

```java
@Test
void listMetricOptionsShouldExposeRiskMetricIdForPublishedProductModel() {
    Device device = tenantDevice(4001L, 8L, 3003L);
    doReturn(device).when(deviceService).getRequiredById(4001L);
    when(permissionService.getDataPermissionContext(99L))
            .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
    when(productModelMapper.selectList(any())).thenReturn(List.of(productModel(3003L, "gpsTotalX", "GNSS 累计位移 X")));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of());
    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(riskMetric(6102L, 3003L, "gpsTotalX")));

    List<DeviceMetricOptionVO> options = deviceService.listMetricOptions(99L, 4001L);

    assertEquals(6102L, options.get(0).getRiskMetricId());
}
```

- [ ] **Step 2: Run backend targeted tests and confirm RED**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DeviceServiceImplTest,RuleDefinitionServiceImplTest,RiskMetricCatalogServiceImplTest test
```

Expected: `BUILD FAILURE`, with failures for missing `riskMetricId` response field or rule persistence assertions.

- [ ] **Step 3: Implement minimal backend support**

```java
@Data
public class DeviceMetricOptionVO {
    private String identifier;
    private String name;
    private String dataType;
    private Long riskMetricId;
}
```

```java
Map<String, Long> riskMetricIds = riskMetricCatalogService.listEnabledByProduct(device.getProductId()).stream()
        .collect(Collectors.toMap(RiskMetricCatalog::getContractIdentifier, RiskMetricCatalog::getId, (left, right) -> left, LinkedHashMap::new));

option.setRiskMetricId(riskMetricIds.get(option.getIdentifier()));
```

```java
private void validateExecutableRule(RuleDefinition rule) {
    if (rule == null) {
        throw new BizException("阈值策略不能为空");
    }
    if (rule.getRiskMetricId() == null && !StringUtils.hasText(rule.getMetricIdentifier())) {
        throw new BizException("阈值策略必须绑定目录指标或测点标识符");
    }
    if (Integer.valueOf(0).equals(rule.getStatus()) && !StringUtils.hasText(rule.getExpression())) {
        throw new BizException("启用中的阈值策略必须提供可执行表达式");
    }
    if (StringUtils.hasText(rule.getExpression()) && !RiskPolicyResolver.isExecutableExpression(rule.getExpression())) {
        throw new BizException("阈值策略表达式格式无效，仅支持 value >= 12 这类写法");
    }
}
```

- [ ] **Step 4: Re-run backend targeted tests and confirm GREEN**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=DeviceServiceImplTest,RuleDefinitionServiceImplTest,RiskMetricCatalogServiceImplTest test
```

Expected: `BUILD SUCCESS`

## Task 3: Keep `riskMetricId` in `/rule-definition` UI flows

**Files:**
- Modify: `spring-boot-iot-ui/src/api/ruleDefinition.ts`
- Modify: `spring-boot-iot-ui/src/views/RuleDefinitionView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RuleDefinitionView.test.ts`

- [ ] **Step 1: Add failing UI tests**

```ts
it('preserves riskMetricId when editing and submitting an existing rule', async () => {
  mockPageRuleList.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [{ ...createRuleRow(), riskMetricId: 6102 }]
    }
  });
  mockUpdateRule.mockResolvedValue({ code: 200, msg: 'success', data: { ...createRuleRow(), riskMetricId: 6102 } });

  const wrapper = mountView();
  await flushPromises();

  await (wrapper.vm as any).handleEdit({ ...createRuleRow(), riskMetricId: 6102 });
  await (wrapper.vm as any).handleSubmit();

  expect(mockUpdateRule).toHaveBeenCalledWith(expect.objectContaining({
    riskMetricId: 6102,
    metricIdentifier: 'displacementX'
  }));
});
```

- [ ] **Step 2: Run the RuleDefinition view tests and confirm RED**

Run:

```bash
cd spring-boot-iot-ui && NODE_OPTIONS='--require=/tmp/localhost-shim.cjs' npx vitest run src/__tests__/views/RuleDefinitionView.test.ts
```

Expected: `FAIL`, because the current form model/API typings do not carry `riskMetricId`.

- [ ] **Step 3: Implement the minimal UI support**

```ts
export interface RuleDefinition {
  id: IdType;
  riskMetricId?: IdType | null;
  metricIdentifier: string;
  metricName: string;
  // ...
}
```

```ts
const form = reactive({
  id: undefined as number | undefined,
  riskMetricId: undefined as number | undefined,
  ruleName: '',
  metricIdentifier: '',
  metricName: '',
  // ...
});

const handleEdit = (row: RuleDefinition) => {
  form.id = row.id;
  form.riskMetricId = row.riskMetricId == null ? undefined : Number(row.riskMetricId);
  form.metricIdentifier = row.metricIdentifier;
  form.metricName = row.metricName;
  // ...
};
```

- [ ] **Step 4: Re-run the same UI test and confirm GREEN**

Run:

```bash
cd spring-boot-iot-ui && NODE_OPTIONS='--require=/tmp/localhost-shim.cjs' npx vitest run src/__tests__/views/RuleDefinitionView.test.ts
```

Expected: `PASS`

## Task 4: Sync docs to the approved GNSS wave boundary

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Add the approved GNSS capability statements**

```md
- `2026-04-06` 起，产品语义契约第二波已补齐 `phase2-gnss`：
  - compare 可返回 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ` 的规范元信息
  - apply 会写入正式合同、厂商字段证据和发布批次
  - 仅 `gpsTotalX / gpsTotalY / gpsTotalZ` 进入 `risk_metric_catalog`
  - 阈值策略页开始保留并提交 `riskMetricId`
```

- [ ] **Step 2: Run doc hygiene verification**

Run:

```bash
git diff --check
python3 -m py_compile scripts/run-real-env-schema-sync.py
```

Expected: no whitespace errors; schema sync script still compiles.

- [ ] **Step 3: Run the final targeted regression pack**

Run:

```bash
mvn -pl spring-boot-iot-protocol,spring-boot-iot-device,spring-boot-iot-alarm -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest,DeviceServiceImplTest,RuleDefinitionServiceImplTest,RiskMetricCatalogServiceImplTest test
cd spring-boot-iot-ui && NODE_OPTIONS='--require=/tmp/localhost-shim.cjs' npx vitest run src/__tests__/views/RuleDefinitionView.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts
```

Expected: both commands succeed.

## Self-Review

Spec coverage:

1. `phase2-gnss` 场景识别、规范字段集合、compare/apply 与发布行为由 Task 1 覆盖。
2. `gpsInitial` 不进入风险目录、设备测点选项补齐 `riskMetricId`、规则后端保存 `riskMetricId` 由 Task 2 覆盖。
3. `/rule-definition` 前台保留并提交 `riskMetricId` 由 Task 3 覆盖。
4. 文档和验收口径同步由 Task 4 覆盖。

Placeholder scan:

1. 无 `TODO / TBD / later` 占位。
2. 每个任务都给出明确文件、命令和期望。

Type consistency:

1. 统一使用 `phase2-gnss` 作为场景编码。
2. 统一使用 `gpsInitial / gpsTotalX / gpsTotalY / gpsTotalZ` 作为 GNSS 规范字段。
3. 统一使用 `riskMetricId` 作为目录主键字段名。
