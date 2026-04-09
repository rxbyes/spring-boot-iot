# Product Contract Normative Evidence Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/products` 的物模型治理抽屉内，把现有“手动提炼 + 自动提炼”升级为“规范证据 + 报文证据 + 正式基线”的分批产品契约治理方法，首批落地倾角 / 加速度 / 裂缝一体机的 `property` 正式模型治理。

**Architecture:** 后端继续以 `spring-boot-iot-device` 为编排中心，不新增平行治理系统，而是在现有 `model-governance/compare` 与 `model-governance/apply` 上增加规范预设、运行证据映射和 compare 语义增强。前端继续复用 `/products`、`ProductModelDesignerWorkspace`、`ProductModelDesignerDrawer` 和 `ProductModelGovernanceCompareTable`，把“手动证据入口”调整为“规范证据优先”，并保留样本 JSON 作为辅助核对工具。

**Tech Stack:** Spring Boot 4、Java 17、MyBatis-Plus、Vue 3、TypeScript、Vitest、JUnit 5、Mockito、npm

---

## File Map

### Backend

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java`
  - 维护首批规范预设、规范字段元数据、原始字段别名映射和一体机默认字段集。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
  - 增加 `governanceMode`、`normativePresetCode`、`selectedNormativeIdentifiers`，保留 `manualExtract` 和 `manualDraftItems` 作为辅助证据入口。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java`
  - 增加 `evidenceOrigin`、`unit`、`normativeSource`、`rawIdentifiers`、`monitorContentCode`、`monitorTypeCode`、`sensorCode`。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
  - 根据 compare 请求模式组装规范证据、映射运行证据并继续复用现有 compare/apply 主流程。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`
  - 保持三栏结构不变，但把 `manualCandidate` 语义升级为“规范证据优先 + 样本辅助”。
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
  - 增加规范模式 compare 行为测试。
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java`
  - 单独校验预设字段骨架和别名映射。

### Frontend

- Create: `spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts`
  - UI 侧规范预设元数据、首批预设标题、副文案和默认字段标签。
- Modify: `spring-boot-iot-ui/src/types/api.ts`
  - 增加 compare 新请求字段和证据快照字段。
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
  - 保持声明同步。
- Modify: `spring-boot-iot-ui/src/api/product.ts`
  - compare 请求体支持规范模式。
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  - 工作台文案收口为“规范证据优先、报文验证、显式 apply”。
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  - 增加治理模式、规范预设卡、字段勾选和 compare 请求组装。
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  - 三栏标题改为 `规范证据 / 报文证据 / 正式模型`，展示单位、规范出处、原始字段别名。
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
  - 校验工作台 copy 收口。
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
  - 校验规范模式默认 compare payload 和 apply 闭环。
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  - 校验 compare 表三栏语义和规范字段元数据渲染。

### Docs

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`
- Check: `docs/21-业务功能清单与验收标准.md`
- Check: `README.md`
- Check: `AGENTS.md`

### Verification

- Backend preset + service tests:
  - `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test`
- Frontend targeted tests:
  - `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Package smoke:
  - `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
- Quality gates:
  - `node scripts/run-quality-gates.mjs`

### Task 1: Add Normative Preset Contract And Registry

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java`

- [ ] **Step 1: Write the failing preset test**

```java
@Test
void integratedPresetShouldExposeNormativePropertyDefinitionsAndAliasMappings() {
    ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

    List<ProductModelGovernanceEvidenceVO> definitions =
            registry.buildPropertyPreset("landslide-integrated-tilt-accel-crack-v1", List.of("L1_QJ_1.X", "S1_ZT_1.signal_4g"));

    assertEquals(2, definitions.size());
    assertEquals("normative", definitions.get(0).getEvidenceOrigin());
    assertEquals("L1_QJ_1.X", definitions.get(0).getIdentifier());
    assertEquals("°", definitions.get(0).getUnit());
    assertEquals("表 B.1", definitions.get(0).getNormativeSource());
    assertTrue(registry.findNormativeIdentifier("landslide-integrated-tilt-accel-crack-v1", "signal_4g").isPresent());
}
```

- [ ] **Step 2: Run the new preset test to confirm missing registry/fields**

Run: `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelNormativePresetRegistryTest test`
Expected: FAIL with `cannot find symbol` for `ProductModelNormativePresetRegistry`, `getEvidenceOrigin`, `getUnit`, or `getNormativeSource`

- [ ] **Step 3: Implement the registry and compare contract fields**

```java
@Data
public class ProductModelGovernanceCompareDTO {

    private String governanceMode;

    private String normativePresetCode;

    private List<String> selectedNormativeIdentifiers;

    private ManualExtractInput manualExtract;

    private List<ManualDraftItem> manualDraftItems;

    private Boolean includeRuntimeCandidates;
}
```

```java
@Data
public class ProductModelGovernanceEvidenceVO {

    private String evidenceOrigin;

    private String unit;

    private String normativeSource;

    private List<String> rawIdentifiers;

    private String monitorContentCode;

    private String monitorTypeCode;

    private String sensorCode;
}
```

```java
public class ProductModelNormativePresetRegistry {

    public static final String GOVERNANCE_MODE_NORMATIVE = "normative";
    public static final String PRESET_INTEGRATED = "landslide-integrated-tilt-accel-crack-v1";

    public List<ProductModelGovernanceEvidenceVO> buildPropertyPreset(String presetCode, Collection<String> selectedIdentifiers) {
        if (!PRESET_INTEGRATED.equals(presetCode)) {
            throw new BizException("规范预设不支持: " + presetCode);
        }
        Set<String> filter = selectedIdentifiers == null ? Set.of() : new LinkedHashSet<>(selectedIdentifiers);
        return integratedDefinitions().stream()
                .filter(item -> filter.isEmpty() || filter.contains(item.getIdentifier()))
                .toList();
    }

    public Optional<String> findNormativeIdentifier(String presetCode, String rawIdentifier) {
        return integratedAliasMap().entrySet().stream()
                .filter(entry -> entry.getValue().contains(rawIdentifier))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
```

- [ ] **Step 4: Run the preset test to verify the contract passes**

Run: `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelNormativePresetRegistryTest test`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit the preset contract slice**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductModelGovernanceCompareDTO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceEvidenceVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java
git commit -m "feat(device): add normative product governance preset registry"
```

### Task 2: Wire Normative Compare Assembly Into Backend Governance

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Add failing service tests for normative compare semantics**

```java
@Test
void compareGovernanceShouldBuildNormativeRowsForIntegratedPreset() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L, "south-survey-multi-detector-v1", "南方测绘多维检测仪"));
    when(productModelMapper.selectList(any())).thenReturn(List.of(existingModel(2001L, "L1_QJ_1.AZI", 50)));
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
            property(3001L, "signal_4g", "4G 信号强度", "int", LocalDateTime.of(2026, 4, 4, 9, 0)),
            property(3001L, "X", "倾角 X", "double", LocalDateTime.of(2026, 4, 4, 9, 1))
    ));
    when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
    when(commandRecordMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    dto.setGovernanceMode("normative");
    dto.setNormativePresetCode("landslide-integrated-tilt-accel-crack-v1");
    dto.setSelectedNormativeIdentifiers(List.of("L1_QJ_1.X", "S1_ZT_1.signal_4g", "L1_QJ_1.AZI"));
    dto.setIncludeRuntimeCandidates(true);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

    assertEquals("double_aligned", compareRow(result, "property", "L1_QJ_1.X").getCompareStatus());
    assertEquals("double_aligned", compareRow(result, "property", "S1_ZT_1.signal_4g").getCompareStatus());
    assertEquals("formal_exists", compareRow(result, "property", "L1_QJ_1.AZI").getCompareStatus());
}

@Test
void compareGovernanceShouldKeepUnmappedRuntimeFieldAsRuntimeOnly() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));
    when(productModelMapper.selectList(any())).thenReturn(List.of());
    when(deviceMapper.selectList(any())).thenReturn(List.of(device(3001L)));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
            property(3001L, "mysteryField", "未知字段", "double", LocalDateTime.of(2026, 4, 4, 9, 2))
    ));
    when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
    when(commandRecordMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    dto.setGovernanceMode("normative");
    dto.setNormativePresetCode("landslide-integrated-tilt-accel-crack-v1");
    dto.setIncludeRuntimeCandidates(true);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(1001L, dto);

    assertEquals("runtime_only", compareRow(result, "property", "mysteryField").getCompareStatus());
    assertTrue(compareRow(result, "property", "mysteryField").getRiskFlags().contains("manual_missing"));
}
```

- [ ] **Step 2: Run the service tests to verify normative mode is not wired yet**

Run: `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelServiceImplTest test`
Expected: FAIL because `governanceMode` is ignored, runtime evidence is not remapped, and `compareRow(..., "property", "L1_QJ_1.X")` cannot be found

- [ ] **Step 3: Implement normative compare assembly and remap runtime evidence**

```java
public ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
    Product product = getRequiredProduct(productId);
    List<ProductModel> existingModels = listActiveModels(productId);
    ProductModelCandidateResultVO manualResult = isNormativeMode(dto)
            ? buildNormativeGovernanceCandidates(productId, existingModels.size(), dto)
            : buildManualGovernanceCandidates(productId, existingModels.size(), dto);
    ProductModelCandidateResultVO runtimeResult = shouldLoadRuntimeCandidates(dto)
            ? buildRuntimeGovernanceCandidates(productId, product, existingModels.size(), dto)
            : emptyCandidateResult(productId, existingModels.size(), EXTRACTION_MODE_RUNTIME);
    return governanceComparator.compare(productId, existingModels, manualResult, runtimeResult);
}
```

```java
private ProductModelCandidateResultVO buildNormativeGovernanceCandidates(Long productId,
                                                                         int existingModelCount,
                                                                         ProductModelGovernanceCompareDTO dto) {
    List<ProductModelGovernanceEvidenceVO> definitions = normativePresetRegistry.buildPropertyPreset(
            dto.getNormativePresetCode(),
            dto.getSelectedNormativeIdentifiers()
    );
    List<ProductModelCandidateVO> propertyCandidates = definitions.stream()
            .map(this::toNormativeCandidate)
            .toList();
    return buildCandidateResult(productId, existingModelCount,
            new PropertyEvidenceBundle(propertyCandidates, propertyCandidates.size(), null),
            new EventEvidenceBundle(List.of(), 0, "规范模式首批不治理事件"),
            new ServiceEvidenceBundle(List.of(), 0, "规范模式首批不治理服务"),
            EXTRACTION_MODE_MANUAL, "normative", null, 0);
}
```

```java
private List<ProductModelCandidateVO> remapRuntimePropertiesByPreset(List<ProductModelCandidateVO> runtimeCandidates,
                                                                     ProductModelGovernanceCompareDTO dto) {
    if (!isNormativeMode(dto)) {
        return runtimeCandidates;
    }
    return runtimeCandidates.stream()
            .map(candidate -> normativePresetRegistry.findNormativeIdentifier(dto.getNormativePresetCode(), candidate.getIdentifier())
                    .map(identifier -> applyNormativeIdentifier(candidate, identifier, dto.getNormativePresetCode()))
                    .orElse(candidate))
            .toList();
}
```

```java
private List<String> resolveRiskFlags(ProductModelGovernanceEvidenceVO manual,
                                      ProductModelGovernanceEvidenceVO runtime,
                                      ProductModelGovernanceEvidenceVO formal,
                                      String compareStatus,
                                      List<String> suspectedMatches) {
    List<String> riskFlags = new ArrayList<>();
    if ("suspected_conflict".equals(compareStatus)) {
        riskFlags.add("definition_mismatch");
    }
    if (manual == null) {
        riskFlags.add("manual_missing");
    }
    if (runtime == null) {
        riskFlags.add("runtime_missing");
    }
    if (formal != null) {
        riskFlags.add("formal_baseline");
    }
    if (suspectedMatches != null && !suspectedMatches.isEmpty()) {
        riskFlags.add("suspected_match");
    }
    return riskFlags;
}
```

- [ ] **Step 4: Run backend preset + service tests and confirm compare semantics**

Run: `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test`
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit the backend normative compare slice**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelGovernanceComparator.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "feat(device): add normative product governance compare flow"
```

### Task 3: Update Frontend Compare Types And Table Semantics

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`

- [ ] **Step 1: Write the failing compare table test for normative labels**

```ts
it('renders normative evidence labels and raw identifiers in the compare row', async () => {
  const wrapper = mount(ProductModelGovernanceCompareTable, {
    props: {
      rows: [
        {
          modelType: 'property',
          identifier: 'L1_QJ_1.X',
          compareStatus: 'double_aligned',
          suggestedAction: '纳入新增',
          riskFlags: [],
          suspectedMatches: [],
          manualCandidate: {
            modelType: 'property',
            identifier: 'L1_QJ_1.X',
            modelName: '倾角测点 X 轴倾角',
            evidenceOrigin: 'normative',
            unit: '°',
            normativeSource: '表 B.1',
            rawIdentifiers: ['X', 'angleX']
          },
          runtimeCandidate: {
            modelType: 'property',
            identifier: 'L1_QJ_1.X',
            modelName: '倾角测点 X 轴倾角',
            evidenceOrigin: 'runtime',
            sourceTables: ['iot_device_property']
          }
        }
      ]
    }
  })

  expect(wrapper.text()).toContain('规范证据')
  expect(wrapper.text()).toContain('表 B.1')
  expect(wrapper.text()).toContain('X / angleX')
})
```

- [ ] **Step 2: Run the targeted compare table test and confirm missing labels/fields**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
Expected: FAIL because `evidenceOrigin` / `normativeSource` / `rawIdentifiers` are not defined in UI types and the table still renders `手动证据`

- [ ] **Step 3: Implement the frontend type changes and table copy**

```ts
export interface ProductModelGovernanceEvidence {
  modelId?: IdType | null;
  modelType: ProductModelType;
  identifier: string;
  modelName: string;
  evidenceOrigin?: 'normative' | 'sample_json' | 'manual_draft' | 'runtime' | 'formal' | null;
  unit?: string | null;
  normativeSource?: string | null;
  rawIdentifiers?: string[] | null;
  monitorContentCode?: string | null;
  monitorTypeCode?: string | null;
  sensorCode?: string | null;
}
```

```vue
<section class="product-model-governance-compare-table__evidence-card">
  <span>规范证据</span>
  <strong>{{ evidenceTitle(row.manualCandidate) }}</strong>
  <p>{{ evidenceSummary(row.manualCandidate) }}</p>
  <small v-if="row.manualCandidate?.normativeSource">{{ row.manualCandidate.normativeSource }}</small>
  <small v-if="row.manualCandidate?.rawIdentifiers?.length">
    {{ row.manualCandidate.rawIdentifiers.join(' / ') }}
  </small>
</section>
```

```ts
function evidenceSummary(evidence?: ProductModelGovernanceEvidence | null) {
  if (!evidence) {
    return '当前侧暂无可用证据。'
  }
  const typePart = evidence.dataType || evidence.eventType || formatServiceSummary(evidence)
  const meta = [evidence.unit, evidence.monitorTypeCode, evidence.sourceTables?.join(' / ')].filter(Boolean)
  return [typePart, ...meta].filter(Boolean).join(' · ')
}
```

- [ ] **Step 4: Re-run the targeted compare table test**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
Expected: `✓ src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`

- [ ] **Step 5: Commit the frontend compare table slice**

```bash
git add spring-boot-iot-ui/src/types/api.ts \
        spring-boot-iot-ui/src/types/api.d.ts \
        spring-boot-iot-ui/src/api/product.ts \
        spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts
git commit -m "feat(ui): show normative evidence in product governance compare table"
```

### Task 4: Build The Normative-First Drawer And Workspace Flow

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: Write the failing workspace/drawer tests for normative mode**

```ts
it('defaults the drawer to normative governance for the integrated preset', async () => {
  const wrapper = mountDrawer()
  await flushPromises()
  await nextTick()

  expect(wrapper.text()).toContain('规范证据优先')
  expect(wrapper.text()).toContain('倾角 / 加速度 / 裂缝一体机')

  await wrapper.get('[data-testid="governance-compare-submit"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
    governanceMode: 'normative',
    normativePresetCode: 'landslide-integrated-tilt-accel-crack-v1',
    selectedNormativeIdentifiers: ['L1_QJ_1.X', 'L1_QJ_1.Y', 'L1_QJ_1.Z', 'L1_QJ_1.angle', 'L1_QJ_1.AZI'],
    manualExtract: undefined,
    manualDraftItems: [],
    includeRuntimeCandidates: true
  })
})
```

- [ ] **Step 2: Run the workspace/drawer tests and confirm payload mismatch**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: FAIL because the component still sends `manualExtract`-only payloads and does not render preset copy

- [ ] **Step 3: Implement normative preset metadata and drawer request building**

```ts
export const INTEGRATED_PRESET = {
  code: 'landslide-integrated-tilt-accel-crack-v1',
  title: '倾角 / 加速度 / 裂缝一体机',
  description: '首批只治理 L1 变形监测与设备状态参数，正式模型采用规范化 identifier。',
  defaultIdentifiers: [
    'L1_QJ_1.X',
    'L1_QJ_1.Y',
    'L1_QJ_1.Z',
    'L1_QJ_1.angle',
    'L1_QJ_1.AZI',
    'L1_JS_1.gX',
    'L1_JS_1.gY',
    'L1_JS_1.gZ',
    'L1_LF_1.value',
    'S1_ZT_1.signal_4g'
  ]
} as const
```

```ts
const governanceMode = ref<'normative' | 'generic'>('normative')
const normativePresetCode = ref(INTEGRATED_PRESET.code)
const selectedNormativeIdentifiers = ref([...INTEGRATED_PRESET.defaultIdentifiers])
```

```ts
const response = await productApi.compareProductModelGovernance(props.product.id, {
  governanceMode: governanceMode.value,
  normativePresetCode: governanceMode.value === 'normative' ? normativePresetCode.value : undefined,
  selectedNormativeIdentifiers: governanceMode.value === 'normative' ? selectedNormativeIdentifiers.value : undefined,
  manualExtract: governanceMode.value === 'generic' && manualSamplePayload.value.trim()
    ? {
        sampleType: manualSampleType.value,
        samplePayload: manualSamplePayload.value.trim()
      }
    : undefined,
  manualDraftItems: normalizedManualDraftItems(),
  includeRuntimeCandidates: includeRuntimeCandidates.value
})
```

```vue
<strong>规范证据 + 报文证据</strong>
<p>首批按一体机规范字段骨架执行 compare，样本 JSON 继续保留为辅助核对工具。</p>
```

- [ ] **Step 4: Re-run the workspace/drawer tests**

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: `✓` for both files

- [ ] **Step 5: Commit the normative-first drawer slice**

```bash
git add spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts \
        spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue \
        spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
        spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
git commit -m "feat(ui): add normative-first product governance flow"
```

### Task 5: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/appendix/iot-field-governance-and-sop.md`
- Check: `docs/21-业务功能清单与验收标准.md`
- Check: `README.md`
- Check: `AGENTS.md`

- [ ] **Step 1: Write the doc updates inline**

```md
## docs/03-接口规范与接口清单.md
- `POST /api/device/product/{productId}/model-governance/compare` 当前除 `manualExtract`、`manualDraftItems[]`、`includeRuntimeCandidates` 外，新增 `governanceMode`、`normativePresetCode`、`selectedNormativeIdentifiers[]`。
- 当 `governanceMode=normative` 时，compare 主语义升级为“规范证据 + 报文证据 + 正式基线”；`manualCandidate` 继续复用原字段名，但首批默认承载规范证据。

## docs/04-数据库设计与初始化数据.md
- `/products` 抽屉内首批可切换到“规范证据优先”治理模式，当前一体机预设固定落 `property`，最终仍只把显式 apply 的 `create / update` 写入 `iot_product_model`。
- 规范出处、单位、监测内容编码、监测类型编码优先进入 `specsJson` 与 compare 证据快照，不新增平行表。

## docs/appendix/iot-field-governance-and-sop.md
- 新增一体机规范预设编码 `landslide-integrated-tilt-accel-crack-v1` 和默认字段集，明确它是 `/products` 首批规范治理样板，而不是行业总模板。
```

- [ ] **Step 2: Run targeted tests and packaging**

Run: `mvn -pl spring-boot-iot-device -am -DskipTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test`
Expected: `BUILD SUCCESS`

Run: `npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
Expected: all targeted product governance tests pass

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Run quality gates**

Run: `node scripts/run-quality-gates.mjs`
Expected: script exits `0`

- [ ] **Step 4: Check whether top-level docs need changes**

```md
- `docs/21-业务功能清单与验收标准.md`：只有当本轮把“规范证据优先治理”纳入正式交付边界或验收标准时才修改；若仍属于设备中心下一阶段增强，则保持原文不变。
- `README.md`：只有当项目总入口需要新增这套治理模式摘要时才修改；若不影响最小阅读集和启动口径，则保持不变。
- `AGENTS.md`：只有当这次实现改变了产品治理的项目级工作规则时才修改；若只是设备中心下一阶段能力增强，则保持不变。
```

- [ ] **Step 5: Commit docs and verification-ready state**

```bash
git add docs/02-业务功能与流程说明.md \
        docs/03-接口规范与接口清单.md \
        docs/04-数据库设计与初始化数据.md \
        docs/08-变更记录与技术债清单.md \
        docs/appendix/iot-field-governance-and-sop.md
git commit -m "docs: document normative product governance flow"
```
