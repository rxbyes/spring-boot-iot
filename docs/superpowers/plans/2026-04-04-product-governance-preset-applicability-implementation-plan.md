# Product Governance Preset Applicability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/products -> 物模型双证据治理` choose normative presets by product applicability so `zhd-warning-sound-light-alarm-v1` and other warning products no longer show the monitoring-only `倾角 / 加速度 / 裂缝一体机` preset, while still falling back cleanly to generic dual-evidence governance.

**Architecture:** Keep the existing compare/apply pipeline intact and add one small applicability layer on both sides. The backend registry becomes the authority for “is this preset valid for this product?” and rejects mismatched normative compare requests; the frontend resolves the same static rule set locally to render either the preset card or a “暂无适用规范预设” empty state and automatically degrade compare requests to `generic`.

**Tech Stack:** Spring Boot 4 / Java 17, Vue 3 + TypeScript, Vitest, JUnit 5, existing `ProductModelDesignerDrawer` and `ProductModelNormativePresetRegistry` patterns.

---

## File Structure

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java`
  Responsibility: define the first static “which products can use which preset” rule set and expose resolve / validate helpers.
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
  Responsibility: reject normative compare requests when the requested preset does not apply to the current product.
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java`
  Responsibility: lock the registry contract for monitoring-product match and warning-product miss behavior.
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
  Responsibility: lock the compare guard so warning products cannot get integrated-monitoring normative rows from the backend.

- Modify: `spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts`
  Responsibility: carry frontend-side applicability metadata and expose a small resolver for the current product.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  Responsibility: render either the applicable preset card or the normative empty state, provide the explicit “切换到通用双证据” action, and auto-fallback compare requests to `generic`.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
  Responsibility: prove monitoring products still see the integrated preset, warning products see the empty state, and compare falls back to `generic`.

- Modify: `README.md`
  Responsibility: update the repo-level product governance baseline sentence.
- Modify: `AGENTS.md`
  Responsibility: align the project acceptance baseline with the new preset applicability rule.
- Modify: `docs/02-业务功能与流程说明.md`
  Responsibility: document that normative preset display is product-aware and warning products can legitimately have no applicable preset.
- Modify: `docs/03-接口规范与接口清单.md`
  Responsibility: document that normative compare requests are only valid when the product matches the preset, otherwise the UI falls back to `generic`.
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: record the shipped behavior change and the “empty state + generic fallback” decision.

### Task 1: Guard Normative Preset Applicability in the Backend

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Write the failing registry and service tests**

Add these tests before changing production code:

```java
@Test
void resolveApplicablePresetShouldReturnIntegratedPresetForMonitoringProduct() {
    ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

    assertEquals(
            ProductModelNormativePresetRegistry.PRESET_INTEGRATED,
            registry.resolveApplicablePreset("south-survey-multi-detector-v1", "南方测绘多维检测仪").orElseThrow()
    );
}

@Test
void resolveApplicablePresetShouldReturnEmptyForWarningSoundLightProduct() {
    ProductModelNormativePresetRegistry registry = new ProductModelNormativePresetRegistry();

    assertTrue(registry.resolveApplicablePreset(
            "zhd-warning-sound-light-alarm-v1",
            "中海达 预警型 声光报警器"
    ).isEmpty());
}
```

```java
@Test
void compareGovernanceShouldRejectInapplicableNormativePresetForWarningProduct() {
    when(productMapper.selectById(1001L)).thenReturn(
            product(1001L, "zhd-warning-sound-light-alarm-v1", "中海达 预警型 声光报警器")
    );
    when(productModelMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    dto.setGovernanceMode("normative");
    dto.setNormativePresetCode(ProductModelNormativePresetRegistry.PRESET_INTEGRATED);
    dto.setSelectedNormativeIdentifiers(List.of("L1_QJ_1.X"));
    dto.setIncludeRuntimeCandidates(false);

    BizException ex = assertThrows(BizException.class, () -> productModelService.compareGovernance(1001L, dto));

    assertEquals(
            "当前产品不适用规范预设: " + ProductModelNormativePresetRegistry.PRESET_INTEGRATED,
            ex.getMessage()
    );
}
```

- [ ] **Step 2: Run the device tests to verify they fail**

Run:

```bash
mvn -pl spring-boot-iot-device -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test
```

Expected: FAIL because `resolveApplicablePreset(...)` does not exist yet and warning products are not rejected in `compareGovernance(...)`.

- [ ] **Step 3: Add preset resolve / validate helpers to the registry**

Extend the registry with explicit applicability metadata and helpers:

```java
private static final Set<String> INTEGRATED_PRODUCT_KEYS = Set.of(
        "south-survey-multi-detector-v1"
);

private static final List<String> INTEGRATED_PRODUCT_KEY_KEYWORDS = List.of(
        "multi-detector",
        "tilt-accel-crack"
);

private static final List<String> INTEGRATED_PRODUCT_NAME_KEYWORDS = List.of(
        "多维检测仪",
        "倾角 / 加速度 / 裂缝一体机",
        "一体机"
);

private static final List<String> WARNING_EXCLUDED_KEYWORDS = List.of(
        "warning",
        "预警",
        "声光报警",
        "广播",
        "爆闪灯",
        "情报板"
);

public Optional<String> resolveApplicablePreset(String productKey, String productName) {
    String normalizedKey = normalizeText(productKey);
    String normalizedName = normalizeText(productName);
    if (containsAny(normalizedKey, WARNING_EXCLUDED_KEYWORDS)
            || containsAny(normalizedName, WARNING_EXCLUDED_KEYWORDS)) {
        return Optional.empty();
    }
    if (INTEGRATED_PRODUCT_KEYS.contains(normalizedKey)
            || containsAny(normalizedKey, INTEGRATED_PRODUCT_KEY_KEYWORDS)
            || containsAny(normalizedName, INTEGRATED_PRODUCT_NAME_KEYWORDS)) {
        return Optional.of(PRESET_INTEGRATED);
    }
    return Optional.empty();
}

public boolean isPresetApplicable(String presetCode, String productKey, String productName) {
    validatePresetCode(presetCode);
    return resolveApplicablePreset(productKey, productName)
            .map(presetCode::equals)
            .orElse(false);
}

private boolean containsAny(String source, List<String> keywords) {
    if (source == null || source.isBlank()) {
        return false;
    }
    return keywords.stream().map(this::normalizeText).anyMatch(source::contains);
}

private String normalizeText(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
}
```

Also add `import java.util.Locale;` at the top of the file.

- [ ] **Step 4: Reject mismatched normative compare requests in the service**

Update the compare entry so backend normative mode is product-aware:

```java
@Override
public ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
    Product product = getRequiredProduct(productId);
    List<ProductModel> existingModels = listActiveModels(productId);
    ProductModelCandidateResultVO manualResult = isNormativeMode(dto)
            ? buildNormativeGovernanceCandidates(productId, product, existingModels.size(), dto)
            : buildManualGovernanceCandidates(productId, existingModels.size(), dto);
    ProductModelCandidateResultVO runtimeResult = shouldLoadRuntimeCandidates(dto)
            ? buildRuntimeGovernanceCandidates(productId, product, existingModels.size(), dto)
            : emptyCandidateResult(productId, existingModels.size(), EXTRACTION_MODE_RUNTIME);
    return governanceComparator.compare(productId, existingModels, manualResult, runtimeResult);
}
```

```java
private ProductModelCandidateResultVO buildNormativeGovernanceCandidates(Long productId,
                                                                         Product product,
                                                                         int existingModelCount,
                                                                         ProductModelGovernanceCompareDTO dto) {
    String presetCode = normalizeRequired(dto == null ? null : dto.getNormativePresetCode(), "规范预设编码");
    if (!normativePresetRegistry.isPresetApplicable(presetCode, product.getProductKey(), product.getProductName())) {
        throw new BizException("当前产品不适用规范预设: " + presetCode);
    }
    List<ProductModelGovernanceEvidenceVO> definitions = normativePresetRegistry.buildPropertyPreset(
            presetCode,
            dto == null ? null : dto.getSelectedNormativeIdentifiers()
    );
    List<ProductModelCandidateVO> propertyCandidates = definitions.stream()
            .map(this::toNormativeCandidate)
            .toList();
    ProductModelCandidateResultVO result = buildCandidateResult(
            productId,
            existingModelCount,
            new PropertyEvidenceBundle(propertyCandidates, propertyCandidates.size(), countNeedsReview(propertyCandidates)),
            new EventEvidenceBundle(List.of(), 0, "规范模式首批不治理事件"),
            new ServiceEvidenceBundle(List.of(), 0, "规范模式首批不治理服务"),
            EXTRACTION_MODE_MANUAL,
            ProductModelNormativePresetRegistry.GOVERNANCE_MODE_NORMATIVE,
            null,
            0
    );
    mergeManualDraftItems(result, dto == null ? List.of() : dto.getManualDraftItems());
    refreshCandidateSummary(result, existingModelCount);
    return result;
}
```

- [ ] **Step 5: Run the device tests to verify they pass**

Run:

```bash
mvn -pl spring-boot-iot-device -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test
```

Expected: PASS with the new applicability resolver and the warning-product rejection path covered.

- [ ] **Step 6: Commit**

```bash
git add \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistry.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelNormativePresetRegistryTest.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "fix(device): guard normative preset applicability"
```

### Task 2: Render Normative Empty State and Generic Fallback in the Drawer

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`

- [ ] **Step 1: Write the failing drawer tests**

First extend the hoisted message mock so `warning` assertions are observable:

```ts
const {
  mockListProductModels,
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance,
  mockWarningMessage
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn(),
  mockWarningMessage: vi.fn()
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: mockWarningMessage
  }
}))
```

Then make `mountDrawer()` accept product overrides and add these tests:

```ts
function mountDrawer(productOverrides?: Partial<{ id: number; productKey: string; productName: string; protocolCode: string; nodeType: number }>) {
  return mount(ProductModelDesignerDrawer, {
    props: {
      modelValue: true,
      product: {
        id: 1001,
        productKey: 'south-survey-multi-detector-v1',
        productName: '南方测绘多维检测仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        ...productOverrides
      }
    },
    global: {
      stubs: {
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardActionLink: StandardActionLinkStub,
        StandardRowActions: StandardRowActionsStub,
        ElTag: ElTagStub,
        ElCheckbox: ElCheckboxStub,
        ElInput: ElInputStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElSelect: true,
        ElOption: true,
        ElSwitch: true,
        ElInputNumber: true
      }
    }
  })
}
```

Also reset the new warning mock in `beforeEach()`:

```ts
beforeEach(() => {
  mockListProductModels.mockReset()
  mockCompareProductModelGovernance.mockReset()
  mockApplyProductModelGovernance.mockReset()
  mockWarningMessage.mockReset()
  mockListProductModels.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
  mockCompareProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: compareResult()
  })
  mockApplyProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      createdCount: 1,
      updatedCount: 0,
      skippedCount: 0,
      conflictCount: 0,
      lastAppliedAt: '2026-03-31T12:05:00'
    }
  })
})
```

```ts
it('shows an empty normative preset state for warning products and can switch to generic governance', async () => {
    const wrapper = mountDrawer({
      productKey: 'zhd-warning-sound-light-alarm-v1',
      productName: '中海达 预警型 声光报警器'
    })
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('当前产品暂无适用规范预设')
    expect(wrapper.text()).not.toContain('倾角 X 轴')

    await wrapper.find('[data-testid="governance-switch-generic"]').trigger('click')

    expect(wrapper.text()).toContain('通用双证据')
})
```

```ts
it('falls back to generic compare when the current product has no applicable normative preset', async () => {
    const wrapper = mountDrawer({
      productKey: 'zhd-warning-sound-light-alarm-v1',
      productName: '中海达 预警型 声光报警器'
    })
    await flushPromises()
    await nextTick()

    await wrapper.find('[data-testid="governance-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockWarningMessage).toHaveBeenCalledWith('当前产品暂无适用规范预设，已改按通用双证据生成对比结果')
    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      governanceMode: 'generic',
      normativePresetCode: undefined,
      selectedNormativeIdentifiers: undefined,
      manualExtract: undefined,
      manualDraftItems: [],
      includeRuntimeCandidates: true
    })
})
```

- [ ] **Step 2: Run the drawer test to verify it fails**

Run:

```bash
CI=1 npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
```

Expected: FAIL because there is no preset applicability resolver, no empty-state card, no fallback button, and `handleCompare()` still sends `normative` for every product.

- [ ] **Step 3: Add frontend-side preset applicability metadata and resolver**

Extend the preset file with matcher metadata and a focused resolver:

```ts
import type { Product } from '@/types/api'

export interface ProductModelGovernanceNormativePresetMatcher {
  productKeys?: string[]
  productKeyKeywords?: string[]
  productNameKeywords?: string[]
  excludedKeywords?: string[]
}

export interface ProductModelGovernanceNormativePreset {
  code: string
  title: string
  description: string
  helperText: string
  defaultIdentifiers: string[]
  availableIdentifiers: ProductModelGovernanceNormativePresetItem[]
  matcher: ProductModelGovernanceNormativePresetMatcher
}

export const INTEGRATED_NORMATIVE_PRESET: ProductModelGovernanceNormativePreset = {
  code: 'landslide-integrated-tilt-accel-crack-v1',
  title: '倾角 / 加速度 / 裂缝一体机',
  description: '首批只治理 L1 变形监测与设备状态参数，正式模型统一采用规范化 identifier。',
  helperText: '默认先勾选倾角核心字段，样本 JSON 继续保留为辅助核对工具，可逐步扩展到加速度、裂缝与设备状态参数。',
  defaultIdentifiers: ['L1_QJ_1.X', 'L1_QJ_1.Y', 'L1_QJ_1.Z', 'L1_QJ_1.angle', 'L1_QJ_1.AZI'],
  availableIdentifiers: [
    { identifier: 'L1_QJ_1.X', label: '倾角 X 轴' },
    { identifier: 'L1_QJ_1.Y', label: '倾角 Y 轴' },
    { identifier: 'L1_QJ_1.Z', label: '倾角 Z 轴' },
    { identifier: 'L1_QJ_1.angle', label: '倾角平面夹角' },
    { identifier: 'L1_QJ_1.AZI', label: '倾角方位角' },
    { identifier: 'L1_JS_1.gX', label: '加速度 X 轴' },
    { identifier: 'L1_JS_1.gY', label: '加速度 Y 轴' },
    { identifier: 'L1_JS_1.gZ', label: '加速度 Z 轴' },
    { identifier: 'L1_LF_1.value', label: '裂缝张开度' },
    { identifier: 'S1_ZT_1.signal_4g', label: '4G 信号强度' }
  ],
  matcher: {
    productKeys: ['south-survey-multi-detector-v1'],
    productKeyKeywords: ['multi-detector', 'tilt-accel-crack'],
    productNameKeywords: ['多维检测仪', '一体机'],
    excludedKeywords: ['warning', '预警', '声光报警', '广播', '爆闪灯', '情报板']
  }
}

function normalize(value?: string | null) {
  return value?.trim().toLowerCase() ?? ''
}

export function resolveApplicableNormativePreset(
  product?: Pick<Product, 'productKey' | 'productName'> | null
) {
  const productKey = normalize(product?.productKey)
  const productName = normalize(product?.productName)
  return PRODUCT_MODEL_GOVERNANCE_NORMATIVE_PRESETS.find((preset) => {
    const excluded = preset.matcher.excludedKeywords?.some((keyword) =>
      productKey.includes(normalize(keyword)) || productName.includes(normalize(keyword))
    )
    if (excluded) {
      return false
    }
    const exactMatch = preset.matcher.productKeys?.map(normalize).includes(productKey)
    const keyKeywordMatch = preset.matcher.productKeyKeywords?.some((keyword) => productKey.includes(normalize(keyword)))
    const nameKeywordMatch = preset.matcher.productNameKeywords?.some((keyword) => productName.includes(normalize(keyword)))
    return Boolean(exactMatch || keyKeywordMatch || nameKeywordMatch)
  }) ?? null
}
```

- [ ] **Step 4: Render the empty state and auto-fallback compare behavior**

Update the drawer script and template:

```ts
import { resolveApplicableNormativePreset } from '@/components/product/productModelGovernanceNormativePresets'
```

```ts
const governanceMode = ref<GovernanceMode>('normative')
const normativePresetCode = ref('')
const selectedNormativeIdentifiers = ref<string[]>([])
```

```ts
const activeNormativePreset = computed(() => resolveApplicableNormativePreset(props.product))
const hasApplicableNormativePreset = computed(() => Boolean(activeNormativePreset.value))

watch(
  () => activeNormativePreset.value?.code,
  (presetCode) => {
    normativePresetCode.value = presetCode ?? ''
    selectedNormativeIdentifiers.value = activeNormativePreset.value
      ? [...activeNormativePreset.value.defaultIdentifiers]
      : []
  },
  { immediate: true }
)
```

```ts
async function handleCompare() {
  if (!props.product?.id) {
    return
  }
  compareLoading.value = true
  errorMessage.value = ''
  const trimmedSamplePayload = manualSamplePayload.value.trim()
  const shouldUseNormativePreset = governanceMode.value === 'normative' && hasApplicableNormativePreset.value
  const effectiveGovernanceMode: GovernanceMode = shouldUseNormativePreset ? 'normative' : 'generic'
  if (governanceMode.value === 'normative' && !hasApplicableNormativePreset.value) {
    ElMessage.warning('当前产品暂无适用规范预设，已改按通用双证据生成对比结果')
  }
  try {
    const response = await productApi.compareProductModelGovernance(props.product.id, {
      governanceMode: effectiveGovernanceMode,
      normativePresetCode: shouldUseNormativePreset ? normativePresetCode.value : undefined,
      selectedNormativeIdentifiers: shouldUseNormativePreset ? selectedNormativeIdentifiers.value : undefined,
      manualExtract: effectiveGovernanceMode === 'generic' && trimmedSamplePayload
        ? {
            sampleType: manualSampleType.value,
            samplePayload: trimmedSamplePayload
          }
        : undefined,
      manualDraftItems: normalizedManualDraftItems(),
      includeRuntimeCandidates: includeRuntimeCandidates.value
    })
    compareResult.value = response.data
    decisionState.value = Object.fromEntries(
      (response.data?.compareRows ?? []).map((row) => [rowKey(row), defaultDecisionForRow(row)])
    )
  } catch (error) {
    compareResult.value = null
    decisionState.value = {}
    errorMessage.value = error instanceof Error ? error.message : '生成对比结果失败'
  } finally {
    compareLoading.value = false
  }
}
```

```ts
function resetGovernanceSession() {
  governanceMode.value = 'normative'
  normativePresetCode.value = ''
  selectedNormativeIdentifiers.value = []
  manualSampleType.value = 'business'
  manualSamplePayload.value = ''
  includeRuntimeCandidates.value = true
  compareResult.value = null
  manualDrafts.value = []
  decisionState.value = {}
}
```

```vue
<article
  v-if="governanceMode === 'normative' && activeNormativePreset"
  class="product-model-designer-drawer__preset-card"
>
  <div class="product-model-designer-drawer__preset-copy">
    <strong>{{ activeNormativePreset.title }}</strong>
    <p>{{ activeNormativePreset.description }}</p>
    <span>{{ activeNormativePreset.helperText }}</span>
  </div>
  <div class="product-model-designer-drawer__preset-identifiers">
    <label
      v-for="item in activeNormativePreset.availableIdentifiers"
      :key="item.identifier"
      class="product-model-designer-drawer__preset-item"
    >
      <input
        type="checkbox"
        :checked="selectedNormativeIdentifiers.includes(item.identifier)"
        @change="toggleNormativeIdentifier(item.identifier, ($event.target as HTMLInputElement).checked)"
      />
      <div>
        <strong>{{ item.label }}</strong>
        <span>{{ item.identifier }}</span>
      </div>
    </label>
  </div>
</article>

<article
  v-else-if="governanceMode === 'normative'"
  class="product-model-designer-drawer__preset-empty"
>
  <strong>当前产品暂无适用规范预设</strong>
  <p>该产品未命中监测型规范字段模板，请改用通用双证据治理，或待专属规范预设补齐后再使用规范治理。</p>
  <button
    type="button"
    class="product-model-designer-drawer__preset-empty-action"
    data-testid="governance-switch-generic"
    @click="governanceMode = 'generic'"
  >
    切换到通用双证据
  </button>
</article>
```

Add the matching scoped styles:

```css
.product-model-designer-drawer__preset-empty {
  display: grid;
  gap: 0.72rem;
  padding: 1rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 26%, var(--panel-border));
  border-radius: 1rem;
  background: color-mix(in srgb, var(--panel-background) 92%, white);
}

.product-model-designer-drawer__preset-empty-action {
  width: fit-content;
  border: 1px solid var(--brand);
  border-radius: 999px;
  background: transparent;
  color: var(--brand);
  cursor: pointer;
  padding: 0.46rem 0.9rem;
}
```

- [ ] **Step 5: Run the drawer test to verify it passes**

Run:

```bash
CI=1 npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
```

Expected: PASS with the warning-product empty state and generic compare fallback covered.

- [ ] **Step 6: Run the full product governance component tests**

Run:

```bash
CI=1 npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
```

Expected: PASS so the existing normative-monitoring flow still works while warning products degrade cleanly.

- [ ] **Step 7: Commit**

```bash
git add \
  spring-boot-iot-ui/src/components/product/productModelGovernanceNormativePresets.ts \
  spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue \
  spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
git commit -m "fix(ui): gate normative governance preset by product"
```

### Task 3: Sync Docs and Run Final Verification

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the repo-level and project-level baseline docs**

Make these documentation changes:

```md
<!-- README.md / AGENTS.md -->
产品物模型设计器当前继续在 `/products` 内复用同一抽屉；规范预设不再对所有产品固定套用同一套字段，而是按当前产品适用性分流。若产品未命中任何规范预设，抽屉会在“规范证据优先”模式下展示“暂无适用规范预设”空态，并引导或自动退化到通用双证据治理；`zhd-warning-sound-light-alarm-v1` 一类预警型产品不再展示“倾角 / 加速度 / 裂缝一体机”字段。
```

```md
<!-- docs/02-业务功能与流程说明.md -->
产品物模型设计器当前继续挂在 `/products` 产品定义中心内；规范治理入口不再对所有产品固定展示同一套字段，而是按当前产品命中的规范预设决定展示内容。监测型一体机继续显示 `landslide-integrated-tilt-accel-crack-v1`；预警型声光报警器等未命中预设的产品会显示“暂无适用规范预设”空态，并可切换到通用双证据治理。
```

```md
<!-- docs/03-接口规范与接口清单.md -->
`POST /api/device/product/{productId}/model-governance/compare` 当前只有在产品命中适用规范预设时才允许按 `governanceMode=normative` 发送 `normativePresetCode` 与 `selectedNormativeIdentifiers[]`；若当前产品无适用预设，前端会退化为 `generic`，后端也会拒绝不适用产品误传的规范预设编码。
```

```md
<!-- docs/08-变更记录与技术债清单.md -->
- 2026-04-04：产品物模型治理继续补齐“规范预设适用性分流”。`/products` 双证据治理当前不再把 `landslide-integrated-tilt-accel-crack-v1` 固定展示给所有产品；监测型一体机继续使用该预设，`zhd-warning-sound-light-alarm-v1` 一类预警型产品会显示“暂无适用规范预设”空态，并在 compare 时自动退化为通用双证据。README.md、AGENTS.md、`docs/02`、`docs/03` 已同步更新。
```

- [ ] **Step 2: Run final project verification**

Run:

```bash
mvn -pl spring-boot-iot-device -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelNormativePresetRegistryTest,ProductModelServiceImplTest test
CI=1 npm --prefix spring-boot-iot-ui run test -- --run \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts
node scripts/run-quality-gates.mjs
```

Expected:

1. Maven targeted tests: `BUILD SUCCESS`
2. Vitest selected product governance suites: all requested files pass without snapshot or assertion regressions
3. Quality gates: command exits successfully and prints the local quality gate success summary

- [ ] **Step 3: Verify the real-environment governance route manually**

Use the shared dev baseline backed by `spring-boot-iot-admin/src/main/resources/application-dev.yml`:

```bash
mvn -pl spring-boot-iot-admin -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Then validate these UI paths in `/products`:

1. 打开一个监测型一体机产品，进入“物模型治理 -> 双证据治理”，确认仍展示 `倾角 / 加速度 / 裂缝一体机` 预设与字段勾选卡。
2. 打开 `zhd-warning-sound-light-alarm-v1`，确认规范模式显示“当前产品暂无适用规范预设”，且不出现倾角 / 加速度 / 裂缝字段。
3. 在 `zhd-warning-sound-light-alarm-v1` 上点击“切换到通用双证据”或直接点击“生成对比结果”，确认流程继续执行且 compare 请求不再按 `normative` 发送。

- [ ] **Step 4: Commit**

```bash
git add \
  README.md \
  AGENTS.md \
  docs/02-业务功能与流程说明.md \
  docs/03-接口规范与接口清单.md \
  docs/08-变更记录与技术债清单.md
git commit -m "docs: describe preset applicability governance flow"
```
