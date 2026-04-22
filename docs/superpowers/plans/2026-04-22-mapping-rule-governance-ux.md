# Mapping Rule Governance UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance mapping rule governance UX with one-click promote, save-before preview, governance candidate panel, and formal-field coverage tagging with one-click disable.

**Architecture:** Frontend-first incremental changes. 3 of 4 features are pure frontend (reusing existing APIs). 1 feature requires a small backend addition (`coveredByFormalField` on `VendorMetricMappingRuleVO` + batch coverage check in `pageRules`).

**Tech Stack:** Vue 3 + TypeScript + Element Plus (frontend), Spring Boot + MyBatis-Plus (backend)

---

## File Structure

| File | Responsibility | Action |
|------|---------------|--------|
| `spring-boot-iot-ui/src/views/DeviceInsightView.vue` | Insight page — deep link "补名称/单位" button for non-formal fields | Modify |
| `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue` | Suggestion panel — route query prefill + preview button | Modify |
| `spring-boot-iot-ui/src/components/product/ProductVendorMappingGovernanceCandidatePanel.vue` | Governance candidate panel — top N high-frequency unmapped fields | **Create** |
| `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue` | Workspace host — insert candidate panel | Modify |
| `spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue` | Ledger panel — coverage tag + one-click disable + unified preview format | Modify |
| `spring-boot-iot-ui/src/types/api.ts` | TypeScript types — add `coveredByFormalField` | Modify |
| `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts` | API layer — pass through `coveredByFormalField` | Modify |
| `spring-boot-iot-device/.../vo/VendorMetricMappingRuleVO.java` | Backend VO — add `coveredByFormalField` field | Modify |
| `spring-boot-iot-device/.../service/impl/VendorMetricMappingRuleServiceImpl.java` | Backend service — coverage check in `toVO` + `pageRules` | Modify |

---

### Task 1: One-click promote — DeviceInsightView deep link

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue:154-166` (template), `:530-544` (script)

- [ ] **Step 1: Add `handlePromoteToMappingRule` function after `handleEditFormalField`**

In `DeviceInsightView.vue`, after line 544 (end of `handleEditFormalField`), add:

```typescript
function handlePromoteToMappingRule(row: PropertySnapshotRow) {
  const productId = device.value?.productId;
  if (productId === undefined || productId === null) {
    return;
  }
  void router.push({
    path: buildProductWorkbenchSectionPath(productId, 'mapping-rules'),
    query: {
      rawIdentifier: row.identifier,
      scope: 'PRODUCT',
      source: 'insight'
    }
  });
}
```

- [ ] **Step 2: Replace "未形成正式字段" with deep link button in template**

Replace line 165 in `DeviceInsightView.vue`:

```html
<!-- Before -->
<span v-else class="monitoring-snapshot-table__action-hint">未形成正式字段</span>

<!-- After -->
<StandardButton
  v-else
  action="query"
  link
  :data-testid="`promote-mapping-rule-${row.identifier}`"
  @click="handlePromoteToMappingRule(row)"
>
  补名称/单位
</StandardButton>
```

- [ ] **Step 3: Verify in browser**

Navigate to `/insight?deviceCode=<code>`, find a property row where `canEditFormalField = false`, confirm the "补名称/单位" link button appears. Click it and verify it navigates to `/products/<productId>/mapping-rules?rawIdentifier=...&scope=PRODUCT&source=insight`.

- [ ] **Step 4: Commit**

```bash
git add spring-boot-iot-ui/src/views/DeviceInsightView.vue
git commit -m "feat: add deep-link '补名称/单位' button for non-formal fields in insight snapshot"
```

---

### Task 2: Suggestion panel — route query prefill + highlight

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue`

- [ ] **Step 1: Import useRoute and add route query reading logic**

In `ProductVendorMappingSuggestionPanel.vue`, after the existing imports (line 146), add `useRoute` to the vue-router import if not present. Then in the script setup, after `const acceptingRowKey = ref<string | null>(null)` (line 185), add:

```typescript
const route = useRoute();

const highlightRawIdentifier = computed(() => {
  const value = route.query.rawIdentifier;
  return typeof value === 'string' ? value.trim() : '';
});

const highlightScope = computed(() => {
  const value = route.query.scope;
  return typeof value === 'string' ? value.trim() : '';
});
```

Add `useRoute` to the vue import or import it separately:

```typescript
import { useRoute } from 'vue-router';
```

- [ ] **Step 2: Auto-scroll and highlight matching suggestion on mount**

After the `loadSuggestions` watch (line 198-204), add a watch + onMounted for highlight:

```typescript
import { computed, ref, watch, onMounted, nextTick } from 'vue'

let highlightTimer: ReturnType<typeof setTimeout> | null = null;

function applyHighlight() {
  if (!highlightRawIdentifier.value) return;
  nextTick(() => {
    const el = document.querySelector(
      `.product-vendor-mapping-suggestion-panel__item[data-raw-identifier="${CSS.escape(highlightRawIdentifier.value)}"]`
    );
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('product-vendor-mapping-suggestion-panel__item--highlight');
      if (highlightTimer) clearTimeout(highlightTimer);
      highlightTimer = setTimeout(() => {
        el.classList.remove('product-vendor-mapping-suggestion-panel__item--highlight');
      }, 3000);
    }
  });
}

watch(
  () => [suggestions.value, highlightRawIdentifier.value],
  () => { applyHighlight(); },
  { immediate: false }
);

onMounted(() => { applyHighlight(); });
```

- [ ] **Step 3: Pre-fill scope from route query when matching suggestion found**

In the `defaultScopeDraft` function (line 281), modify to use `highlightScope`:

```typescript
function defaultScopeDraft(suggestion: SuggestionRow): ScopeDraft {
  const isHighlightMatch = highlightRawIdentifier.value
    && suggestion.rawIdentifier === highlightRawIdentifier.value;
  return {
    scopeType: normalizeScopeType(isHighlightMatch && highlightScope.value ? highlightScope.value : suggestion.recommendedScopeType),
    protocolCode: '',
    scenarioCode: '',
    deviceFamily: ''
  };
}
```

- [ ] **Step 4: Add `data-raw-identifier` attribute to suggestion items in template**

In the template, on the `<article>` element inside the `v-for` (line 48), add the attribute:

```html
<article
  v-for="suggestion in suggestions"
  :key="suggestionKey(suggestion)"
  :data-raw-identifier="suggestion.rawIdentifier"
  class="product-vendor-mapping-suggestion-panel__item"
>
```

- [ ] **Step 5: Add highlight CSS class**

In the `<style scoped>` section, add:

```css
.product-vendor-mapping-suggestion-panel__item--highlight {
  border-color: var(--brand, #409eff);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--brand, #409eff) 20%, transparent);
  transition: border-color 0.3s, box-shadow 0.3s;
}
```

- [ ] **Step 6: Verify in browser**

From insight page, click "补名称/单位" on a non-formal field. On the mapping-rules page, verify the suggestion panel scrolls to and highlights the matching `rawIdentifier`, and the scope dropdown is pre-filled with `PRODUCT`.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue
git commit -m "feat: suggestion panel reads route query to prefill and highlight matching suggestion"
```

---

### Task 3: Save-before preview — suggestion panel preview button

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue`

- [ ] **Step 1: Add preview API imports**

In `ProductVendorMappingSuggestionPanel.vue`, add to the import from `@/api/vendorMetricMappingRule`:

```typescript
import {
  createVendorMetricMappingRule,
  listVendorMetricMappingRuleSuggestions,
  previewVendorMetricMappingRuleHit,
  replayVendorMetricMappingRule
} from '@/api/vendorMetricMappingRule'
```

Add type imports:

```typescript
import type {
  VendorMetricMappingRuleHitPreview,
  VendorMetricMappingRuleReplay
} from '@/types/api'
```

- [ ] **Step 2: Add preview state refs**

After `const acceptingRowKey = ref<string | null>(null)` (line 185), add:

```typescript
const previewKey = ref<string | null>(null)
const previewHitResult = ref<VendorMetricMappingRuleHitPreview | null>(null)
const replayResult = ref<VendorMetricMappingRuleReplay | null>(null)
```

- [ ] **Step 3: Add scope description helper and preview display logic**

Add these helper functions before `handleAccept`:

```typescript
function scopeDescription(scopeType?: string | null): string {
  switch (scopeType) {
    case 'PRODUCT': return '该产品下所有设备';
    case 'DEVICE_FAMILY': return '该产品该设备族设备';
    case 'SCENARIO': return '该产品该场景设备';
    case 'PROTOCOL': return '该产品该协议设备';
    case 'TENANT_DEFAULT': return '租户下所有产品';
    default: return '--';
  }
}

function previewConflictLabel(suggestion: SuggestionRow, replay: VendorMetricMappingRuleReplay | null): string {
  if (!replay || !replay.matched) return '';
  const formTarget = (suggestion.targetNormativeIdentifier || '').toLowerCase();
  const replayTarget = (replay.targetNormativeIdentifier || '').toLowerCase();
  if (formTarget && replayTarget && formTarget !== replayTarget) {
    return `与现有规则目标不一致（现有: ${replay.targetNormativeIdentifier}）`;
  }
  return '';
}

function previewCoveredLabel(hit: VendorMetricMappingRuleHitPreview | null): string {
  if (hit && hit.matched && hit.hitSource === 'SNAPSHOT') {
    return '已被发布快照覆盖';
  }
  return '';
}

async function handlePreview(suggestion: SuggestionRow) {
  if (!props.productId) return;
  const key = suggestionKey(suggestion);
  previewKey.value = key;
  previewHitResult.value = null;
  replayResult.value = null;
  const scopeDraft = resolveScopeDraft(suggestion);
  try {
    const [hitResp, replayResp] = await Promise.all([
      previewVendorMetricMappingRuleHit(props.productId as IdType, {
        rawIdentifier: suggestion.rawIdentifier,
        logicalChannelCode: suggestion.logicalChannelCode ?? undefined
      }),
      replayVendorMetricMappingRule(props.productId as IdType, {
        rawIdentifier: suggestion.rawIdentifier,
        logicalChannelCode: suggestion.logicalChannelCode ?? undefined
      })
    ]);
    previewHitResult.value = hitResp.data ?? null;
    replayResult.value = replayResp.data ?? null;
  } catch (error) {
    if (!isHandledRequestError(error)) {
      const message = await getMessageApi();
      message.error(error instanceof Error ? error.message : '预览失败');
    }
  } finally {
    previewKey.value = null;
  }
}
```

- [ ] **Step 4: Add "预览生效" button and preview result display in template**

After the "采纳建议" button (around line 139), add:

```html
<button
  v-if="canAccept(suggestion)"
  :data-testid="`vendor-mapping-suggestion-preview-${suggestionKey(suggestion)}`"
  class="product-vendor-mapping-suggestion-panel__action"
  type="button"
  :disabled="previewKey === suggestionKey(suggestion)"
  @click="handlePreview(suggestion)"
>
  {{ previewKey === suggestionKey(suggestion) ? '预览中...' : '预览生效' }}
</button>
```

After `</article>` closing the `product-vendor-mapping-suggestion-panel__item-main` div but still inside the `<article>`, add the preview result panel:

```html
<div
  v-if="previewHitResult || replayResult"
  class="product-vendor-mapping-suggestion-panel__preview"
>
  <div v-if="replayResult" class="product-vendor-mapping-suggestion-panel__preview-item">
    <strong>{{ replayResult.matched ? '会命中规则' : '未命中规则' }}</strong>
    <span v-if="replayResult.matched">{{ `${replayResult.matchedScopeType || '--'} · ${replayResult.canonicalIdentifier || replayResult.targetNormativeIdentifier || '--'}` }}</span>
  </div>
  <div v-if="previewConflictLabel(suggestion, replayResult)" class="product-vendor-mapping-suggestion-panel__preview-item product-vendor-mapping-suggestion-panel__preview-item--danger">
    {{ previewConflictLabel(suggestion, replayResult) }}
  </div>
  <div v-if="previewCoveredLabel(previewHitResult)" class="product-vendor-mapping-suggestion-panel__preview-item product-vendor-mapping-suggestion-panel__preview-item--warn">
    {{ previewCoveredLabel(previewHitResult) }}
  </div>
  <div class="product-vendor-mapping-suggestion-panel__preview-item">
    <span>影响范围：{{ scopeDescription(resolveScopeDraft(suggestion).scopeType) }}</span>
  </div>
</div>
```

- [ ] **Step 5: Add preview CSS styles**

```css
.product-vendor-mapping-suggestion-panel__preview {
  display: grid;
  gap: 0.3rem;
  padding: 0.6rem 0.9rem;
  margin-top: 0.3rem;
  border-top: 1px dashed var(--panel-border);
}

.product-vendor-mapping-suggestion-panel__preview-item {
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.product-vendor-mapping-suggestion-panel__preview-item--danger {
  color: var(--el-color-danger, #f56c6c);
}

.product-vendor-mapping-suggestion-panel__preview-item--warn {
  color: var(--el-color-warning, #e6a23c);
}
```

- [ ] **Step 6: Verify in browser**

On the mapping-rules suggestion panel, click "预览生效" on a suggestion. Confirm the 4-item preview display appears below the suggestion. Verify conflict warning shows when targets differ, coverage warning shows when snapshot covers it.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue
git commit -m "feat: add save-before preview button to suggestion panel"
```

---

### Task 4: Governance candidate panel

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductVendorMappingGovernanceCandidatePanel.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue:646-672`

- [ ] **Step 1: Create the candidate panel component**

Create `ProductVendorMappingGovernanceCandidatePanel.vue`:

```vue
<template>
  <PanelCard class="governance-candidate-panel">
    <div v-if="loading" class="governance-candidate-panel__state">
      正在加载待治理候选...
    </div>
    <div v-else-if="errorMessage" class="governance-candidate-panel__state">
      <strong>待治理候选加载失败</strong>
      <p>{{ errorMessage }}</p>
    </div>
    <div v-else-if="!candidates.length" class="governance-candidate-panel__state">
      <strong>暂无待治理候选</strong>
      <p>所有高频字段已正式化或已纳入建议。</p>
    </div>

    <template v-else>
      <div class="governance-candidate-panel__list">
        <article
          v-for="item in candidates"
          :key="item.rawIdentifier"
          class="governance-candidate-panel__item"
        >
          <div class="governance-candidate-panel__item-main">
            <strong>{{ item.rawIdentifier }}</strong>
            <span>{{ item.targetNormativeIdentifier || '--' }}</span>
            <small>
              证据 {{ item.evidenceCount ?? 0 }} · 置信度 {{ formatConfidence(item.confidence) }} · 最近出现 {{ item.lastSeenTime || '--' }}
            </small>
          </div>
          <StandardButton
            action="query"
            link
            @click="emit('goAccept', item)"
          >
            去采纳
          </StandardButton>
        </article>
      </div>
      <p
        v-if="totalSuggestionCount > candidates.length"
        class="governance-candidate-panel__more"
      >
        <StandardButton action="query" link @click="emit('viewAllSuggestions')">
          查看全部 {{ totalSuggestionCount }} 条建议
        </StandardButton>
      </p>
    </template>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  listVendorMetricMappingRuleSuggestions
} from '@/api/vendorMetricMappingRule'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import { ElMessage } from '@/utils/message'
import type { IdType, VendorMetricMappingRuleSuggestion } from '@/types/api'

const MAX_VISIBLE_CANDIDATES = 10

const props = defineProps<{
  productId?: IdType | null
}>()

const emit = defineEmits<{
  (event: 'goAccept', item: VendorMetricMappingRuleSuggestion): void
  (event: 'viewAllSuggestions'): void
}>()

const loading = ref(false)
const errorMessage = ref('')
const allSuggestions = ref<VendorMetricMappingRuleSuggestion[]>([])

const candidates = computed(() => {
  return allSuggestions.value
    .filter((s) => s.status === 'READY_TO_CREATE')
    .sort((a, b) => (b.evidenceCount ?? 0) - (a.evidenceCount ?? 0))
    .slice(0, MAX_VISIBLE_CANDIDATES)
})

const totalSuggestionCount = computed(() => allSuggestions.value.length)

watch(
  () => props.productId,
  () => { void loadCandidates() },
  { immediate: true }
)

function formatConfidence(confidence?: number | string | null) {
  const numeric = typeof confidence === 'string' && confidence.trim() !== ''
    ? Number(confidence)
    : confidence
  if (typeof numeric !== 'number' || Number.isNaN(numeric)) return '--'
  return numeric.toFixed(2)
}

async function loadCandidates() {
  if (props.productId === null || props.productId === undefined || props.productId === '') {
    allSuggestions.value = []
    errorMessage.value = ''
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listVendorMetricMappingRuleSuggestions(props.productId, {
      includeCovered: false,
      includeIgnored: false,
      minEvidenceCount: 2
    })
    allSuggestions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (!isHandledRequestError(error)) {
      errorMessage.value = resolveRequestErrorMessage(error, '待治理候选加载失败')
    }
    allSuggestions.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.governance-candidate-panel__state {
  padding: 1rem;
  color: var(--text-secondary);
}

.governance-candidate-panel__state p {
  margin: 0.45rem 0 0;
}

.governance-candidate-panel__list {
  display: grid;
  gap: 0.6rem;
}

.governance-candidate-panel__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.6rem;
  align-items: center;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.7rem;
}

.governance-candidate-panel__item-main {
  display: grid;
  gap: 0.15rem;
}

.governance-candidate-panel__item-main span,
.governance-candidate-panel__item-main small {
  color: var(--text-secondary);
  margin: 0;
}

.governance-candidate-panel__more {
  margin-top: 0.6rem;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
</style>
```

- [ ] **Step 2: Add `isHandledRequestError` import check**

Verify that `isHandledRequestError` is importable from `@/api/request`. If not, use a local try/catch pattern instead. Check the existing import pattern in `ProductVendorMappingRuleLedgerPanel.vue` — it imports from `@/api/request`.

- [ ] **Step 3: Insert candidate panel into ProductModelDesignerWorkspace**

In `ProductModelDesignerWorkspace.vue`, before the "映射规则建议" section (around line 646), add:

```html
<section
  v-if="showMappingRuleSections"
  class="product-model-designer__stage"
  data-testid="governance-candidate-panel"
>
  <header class="product-model-designer__stage-head">
    <div>
      <h3>待治理候选</h3>
      <p>基于运行态证据汇总的高频未正式化字段，建议优先补录名称和单位。</p>
    </div>
  </header>

  <ProductVendorMappingGovernanceCandidatePanel
    :product-id="props.product?.id ?? null"
    @go-accept="handleCandidateGoAccept"
    @view-all-suggestions="handleCandidateViewAll"
  />
</section>
```

- [ ] **Step 4: Add imports and event handlers in ProductModelDesignerWorkspace script**

Add import at the top of the script:

```typescript
import ProductVendorMappingGovernanceCandidatePanel from '@/components/product/ProductVendorMappingGovernanceCandidatePanel.vue'
```

Add event handlers (near other vendor mapping handlers):

```typescript
function handleCandidateGoAccept(item: VendorMetricMappingRuleSuggestion) {
  const suggestionSection = document.querySelector('[data-testid="contract-field-vendor-suggestions"]');
  if (suggestionSection) {
    suggestionSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function handleCandidateViewAll() {
  const suggestionSection = document.querySelector('[data-testid="contract-field-vendor-suggestions"]');
  if (suggestionSection) {
    suggestionSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
```

Add `VendorMetricMappingRuleSuggestion` type import if not already present.

- [ ] **Step 5: Verify in browser**

Navigate to a product's mapping-rules tab. Confirm the "待治理候选" section appears above "映射规则建议". Verify candidates are sorted by evidenceCount descending, limited to 10. Click "去采纳" and "查看全部 N 条建议" to confirm scroll behavior.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductVendorMappingGovernanceCandidatePanel.vue spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue
git commit -m "feat: add governance candidate panel for high-frequency unmapped fields"
```

---

### Task 5: Backend — Add `coveredByFormalField` to VO and service

**Files:**
- Modify: `spring-boot-iot-device/.../vo/VendorMetricMappingRuleVO.java`
- Modify: `spring-boot-iot-device/.../service/impl/VendorMetricMappingRuleServiceImpl.java`

- [ ] **Step 1: Add `coveredByFormalField` field to `VendorMetricMappingRuleVO`**

In `VendorMetricMappingRuleVO.java`, after `private LocalDateTime updateTime;` (line 50), add:

```java
private Boolean coveredByFormalField;
```

- [ ] **Step 2: Add dependency injections to `VendorMetricMappingRuleServiceImpl`**

In `VendorMetricMappingRuleServiceImpl.java`, add these fields after `private final VendorMetricMappingRuntimeServiceImpl runtimeService;` (line 59):

```java
private final NormativeMetricDefinitionMapper normativeMapper;
private final PublishedProductContractSnapshotService snapshotService;
```

Add the necessary imports:

```java
import com.ghlzm.iot.device.mapper.NormativeMetricDefinitionMapper;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
```

Update all constructors to accept the new dependencies. The `@Autowired` constructor (line 82) becomes:

```java
@Autowired
public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                          VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                          ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider,
                                          ProductMapper productMapper,
                                          VendorMetricMappingRuntimeServiceImpl runtimeService,
                                          NormativeMetricDefinitionMapper normativeMapper,
                                          PublishedProductContractSnapshotService snapshotService) {
    this.mapper = mapper;
    this.snapshotMapper = snapshotMapper;
    this.protocolSecurityDefinitionProvider = protocolSecurityDefinitionProvider;
    this.productMapper = productMapper;
    this.runtimeService = runtimeService;
    this.normativeMapper = normativeMapper;
    this.snapshotService = snapshotService;
}
```

Update the other constructors to pass `null` for the new params (maintaining backward compatibility for tests):

```java
public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper) {
    this(mapper, null, (ProtocolSecurityDefinitionProvider) null, null, null, null, null);
}

public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                          VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                          IotProperties iotProperties) {
    this(mapper, snapshotMapper,
            iotProperties == null ? null : new YamlProtocolSecurityDefinitionProvider(iotProperties),
            null, null, null, null);
}

public VendorMetricMappingRuleServiceImpl(VendorMetricMappingRuleMapper mapper,
                                          VendorMetricMappingRuleSnapshotMapper snapshotMapper,
                                          ProtocolSecurityDefinitionProvider protocolSecurityDefinitionProvider) {
    this(mapper, snapshotMapper, protocolSecurityDefinitionProvider, null, null, null, null);
}
```

- [ ] **Step 3: Add batch coverage check method**

Add this private method after the `loadLatestPublishedSnapshots` method (around line 341):

```java
private Map<String, Boolean> batchCheckCoverage(Long productId, List<VendorMetricMappingRule> rules) {
    if (normativeMapper == null || snapshotService == null || rules == null || rules.isEmpty()) {
        return Map.of();
    }
    Set<String> targetIdentifiers = rules.stream()
            .map(VendorMetricMappingRule::getTargetNormativeIdentifier)
            .filter(StringUtils::hasText)
            .collect(java.util.stream.Collectors.toSet());
    if (targetIdentifiers.isEmpty()) {
        return Map.of();
    }

    // Check NormativeMetricDefinition
    Set<String> normativeIdentifiers = new java.util.HashSet<>();
    try {
        List<NormativeMetricDefinition> normativeDefs = normativeMapper.selectList(
                new LambdaQueryWrapper<NormativeMetricDefinition>()
                        .in(NormativeMetricDefinition::getIdentifier, targetIdentifiers)
                        .eq(NormativeMetricDefinition::getDeleted, 0)
        );
        if (normativeDefs != null) {
            normativeDefs.stream()
                    .map(NormativeMetricDefinition::getIdentifier)
                    .filter(StringUtils::hasText)
                    .forEach(id -> normativeIdentifiers.add(id.toLowerCase()));
        }
    } catch (Exception ignored) {
        // Normative check is best-effort
    }

    // Check PublishedProductContractSnapshot
    Set<String> publishedIdentifiers = new java.util.HashSet<>();
    try {
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(productId);
        if (snapshot != null) {
            for (String identifier : targetIdentifiers) {
                if (snapshot.containsPublishedIdentifier(identifier)) {
                    publishedIdentifiers.add(identifier.toLowerCase());
                }
            }
        }
    } catch (Exception ignored) {
        // Snapshot check is best-effort
    }

    Map<String, Boolean> result = new HashMap<>();
    for (String identifier : targetIdentifiers) {
        String lower = identifier.toLowerCase();
        result.put(identifier, normativeIdentifiers.contains(lower) || publishedIdentifiers.contains(lower));
    }
    return result;
}
```

Add the missing imports:

```java
import java.util.stream.Collectors;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
```

- [ ] **Step 4: Update `pageRules` to pass coverage info to `toVO`**

Modify the `pageRules` method (line 95-108) to call `batchCheckCoverage` and pass the result:

```java
@Override
public PageResult<VendorMetricMappingRuleVO> pageRules(Long productId, String status, Long pageNum, Long pageSize) {
    Page<VendorMetricMappingRule> page = PageQueryUtils.buildPage(pageNum, pageSize);
    Page<VendorMetricMappingRule> result = mapper.selectPage(page, new LambdaQueryWrapper<VendorMetricMappingRule>()
            .eq(VendorMetricMappingRule::getDeleted, 0)
            .eq(productId != null, VendorMetricMappingRule::getProductId, productId)
            .eq(StringUtils.hasText(status), VendorMetricMappingRule::getStatus, normalizeUpper(status))
            .orderByDesc(VendorMetricMappingRule::getUpdateTime)
            .orderByDesc(VendorMetricMappingRule::getId));
    Map<Long, VendorMetricMappingRuleSnapshot> latestPublishedSnapshots = loadLatestPublishedSnapshots(result.getRecords());
    Map<String, Boolean> coverageMap = batchCheckCoverage(productId, result.getRecords());
    List<VendorMetricMappingRuleVO> records = result.getRecords().stream()
            .map(rule -> toVO(rule, latestPublishedSnapshots.get(rule.getId()), coverageMap))
            .toList();
    return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
}
```

- [ ] **Step 5: Update `toVO` signature and set `coveredByFormalField`**

Change `toVO` from:

```java
private VendorMetricMappingRuleVO toVO(VendorMetricMappingRule rule,
                                       VendorMetricMappingRuleSnapshot snapshot) {
```

To:

```java
private VendorMetricMappingRuleVO toVO(VendorMetricMappingRule rule,
                                       VendorMetricMappingRuleSnapshot snapshot,
                                       Map<String, Boolean> coverageMap) {
```

At the end of `toVO`, before `return vo;`, add:

```java
String targetId = rule.getTargetNormativeIdentifier();
vo.setCoveredByFormalField(Boolean.valueOf(
    coverageMap != null && targetId != null && Boolean.TRUE.equals(coverageMap.get(targetId))
));
```

- [ ] **Step 6: Update all other `toVO` call sites to pass `Map.of()`**

Update `createAndGet` (line 129):

```java
return toVO(getRequiredRule(productId, ruleId), null, Map.of());
```

Update `updateAndGet` (line 147):

```java
return toVO(rule, null, Map.of());
```

- [ ] **Step 7: Build and verify**

Run:

```bash
cd spring-boot-iot && mvn compile -pl spring-boot-iot-device -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java
git commit -m "feat: add coveredByFormalField to VendorMetricMappingRuleVO with batch coverage check"
```

---

### Task 6: Frontend — Add `coveredByFormalField` to types and API mapping

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts:751-801`
- Modify: `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts:62-97`

- [ ] **Step 1: Add `coveredByFormalField` to `VendorMetricMappingRule` interface**

In `api.ts`, after `updateTime?: string | null;` (line 769), add:

```typescript
coveredByFormalField?: boolean | null;
```

- [ ] **Step 2: Add `coveredByFormalField` to `VendorMetricMappingRuleLedgerRow` interface**

In `api.ts`, after `logicalChannelCode?: string | null;` (line 800), add:

```typescript
coveredByFormalField?: boolean | null;
```

- [ ] **Step 3: Pass through `coveredByFormalField` in ledger API mapping**

In `vendorMetricMappingRule.ts`, in the `listVendorMetricMappingRuleLedger` mapping (around line 94), add after `publishedSource`:

```typescript
coveredByFormalField: row.coveredByFormalField,
```

- [ ] **Step 4: Commit**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts
git commit -m "feat: add coveredByFormalField to frontend types and ledger API mapping"
```

---

### Task 7: Ledger panel — coverage tag + one-click disable

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue`

- [ ] **Step 1: Add `coveredByFormalField` tag in rule card title**

In the template, inside `product-vendor-rule-ledger__title` (around line 78), after the status `<span>`, add:

```html
<span
  v-if="row.coveredByFormalField"
  class="product-vendor-rule-ledger__coverage-tag"
>
  已被正式字段覆盖
</span>
```

- [ ] **Step 2: Add one-click disable button in actions area**

In the `product-vendor-rule-ledger__actions` div (around line 91), before the "试命中" button, add:

```html
<StandardButton
  v-if="row.coveredByFormalField && row.draftStatus === 'ACTIVE'"
  :data-testid="`rule-ledger-disable-covered-${rowIdentity(row)}`"
  :disabled="isSubmitting(`disable-${rowIdentity(row)}`)"
  @click="handleDisableCovered(row)"
>
  {{ isSubmitting(`disable-${rowIdentity(row)}`) ? '停用中...' : '一键停用' }}
</StandardButton>
```

- [ ] **Step 3: Add `handleDisableCovered` function in script**

After `handleBatchStatus` function (around line 466), add:

```typescript
async function handleDisableCovered(row: VendorMetricMappingRuleLedgerRow) {
  if (!hasProductId(props.productId) || row.ruleId == null) {
    return;
  }
  try {
    await confirmAction({
      title: '停用已被覆盖的规则',
      message: '此规则已被正式字段覆盖，停用后不再参与运行时解析。确认停用？',
      type: 'warning',
      confirmButtonText: '确认停用'
    });
  } catch {
    return;
  }
  const disableKey = `disable-${rowIdentity(row)}`;
  submittingKey.value = disableKey;
  try {
    await batchUpdateVendorMetricMappingRuleStatus(props.productId as IdType, {
      ruleIds: [row.ruleId as IdType],
      targetStatus: 'DISABLED'
    });
    ElMessage.success('已停用被覆盖的规则');
    await loadRows();
  } catch (error) {
    showRequestErrorMessage(error, '停用规则失败');
  } finally {
    submittingKey.value = '';
  }
}
```

Add the `confirmAction` import at the top of the script:

```typescript
import { confirmAction } from '@/utils/confirm'
```

- [ ] **Step 4: Add coverage tag CSS**

```css
.product-vendor-rule-ledger__coverage-tag {
  display: inline-block;
  padding: 0.1rem 0.5rem;
  border-radius: 0.4rem;
  font-size: 0.75rem;
  font-weight: 500;
  background: color-mix(in srgb, var(--el-color-warning, #e6a23c) 12%, transparent);
  color: var(--el-color-warning, #e6a23c);
}
```

- [ ] **Step 5: Verify in browser**

Navigate to a product's mapping-rules ledger. For rules where `coveredByFormalField = true`, confirm the orange "已被正式字段覆盖" tag appears. Click "一键停用" → confirm dialog → rule status changes to DISABLED → list refreshes.

- [ ] **Step 6: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue
git commit -m "feat: add coverage tag and one-click disable for rules covered by formal fields"
```

---

### Task 8: Unified preview format in ledger panel

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue`

- [ ] **Step 1: Replace existing preview/replay display with unified structured format**

Replace the existing preview and replay result divs (lines 133-151) with a unified display:

```html
<div
  v-if="previewStateByRuleId[rowIdentity(row)] || replayStateByRuleId[rowIdentity(row)]"
  :data-testid="`rule-ledger-preview-result-${rowIdentity(row)}`"
  class="product-vendor-rule-ledger__preview"
>
  <div v-if="replayStateByRuleId[rowIdentity(row)]" class="product-vendor-rule-ledger__preview-item">
    <strong>{{ replayStateByRuleId[rowIdentity(row)]?.matched ? '回放命中规则' : '回放未命中规则' }}</strong>
    <span v-if="replayStateByRuleId[rowIdentity(row)]?.matched">
      {{ `${scopeTypeLabel(replayStateByRuleId[rowIdentity(row)]?.matchedScopeType) || '--'} · ${replayStateByRuleId[rowIdentity(row)]?.canonicalIdentifier || replayStateByRuleId[rowIdentity(row)]?.targetNormativeIdentifier || '--'}` }}
    </span>
  </div>
  <div v-if="previewStateByRuleId[rowIdentity(row)]" class="product-vendor-rule-ledger__preview-item">
    <span>{{ previewMatchedLabel(previewStateByRuleId[rowIdentity(row)]) }}</span>
    <span>{{ previewSourceLabel(previewStateByRuleId[rowIdentity(row)]) }}</span>
  </div>
  <div v-if="isPreviewCovered(previewStateByRuleId[rowIdentity(row)])" class="product-vendor-rule-ledger__preview-item product-vendor-rule-ledger__preview-item--warn">
    已被发布快照覆盖
  </div>
  <div class="product-vendor-rule-ledger__preview-item">
    <span>影响范围：{{ scopeDescription(row.scopeType) }}</span>
  </div>
</div>
```

- [ ] **Step 2: Add `isPreviewCovered` and `scopeDescription` helpers in script**

After the existing helper functions, add:

```typescript
function isPreviewCovered(preview?: VendorMetricMappingRuleHitPreview | null): boolean {
  return Boolean(preview?.matched && preview?.hitSource === 'SNAPSHOT');
}

function scopeDescription(scopeType?: string | null): string {
  switch (scopeType) {
    case 'PRODUCT': return '该产品下所有设备';
    case 'DEVICE_FAMILY': return '该产品该设备族设备';
    case 'SCENARIO': return '该产品该场景设备';
    case 'PROTOCOL': return '该产品该协议设备';
    case 'TENANT_DEFAULT': return '租户下所有产品';
    default: return '--';
  }
}
```

- [ ] **Step 3: Add preview-item CSS**

```css
.product-vendor-rule-ledger__preview-item {
  font-size: 0.82rem;
}

.product-vendor-rule-ledger__preview-item--warn {
  color: var(--el-color-warning, #e6a23c);
}
```

- [ ] **Step 4: Verify in browser**

On the ledger panel, click "试命中" and "回放校验" on a rule. Confirm the unified preview shows: matched status, scope + identifier, coverage warning (if applicable), and impact scope description.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductVendorMappingRuleLedgerPanel.vue
git commit -m "feat: unify preview display format in ledger panel with structured info"
```

---

## Plan Self-Review

**1. Spec coverage:**
- Feature 1 (一键补录): Task 1 + Task 2 ✓
- Feature 2 (保存前预览): Task 3 + Task 8 ✓
- Feature 3 (待治理候选): Task 4 ✓
- Feature 4 (覆盖打标+一键停用): Task 5 + Task 6 + Task 7 ✓

**2. Placeholder scan:** No TBD/TODO/fill-in-later found. All code steps contain complete implementations.

**3. Type consistency:**
- `coveredByFormalField: Boolean` (Java VO) ↔ `coveredByFormalField?: boolean | null` (TS) — consistent
- `VendorMetricMappingRuleLedgerRow` updated in both `api.ts` and `vendorMetricMappingRule.ts` mapping — consistent
- `toVO` signature updated in all call sites — consistent
- `handlePromoteToMappingRule` uses `buildProductWorkbenchSectionPath` (existing) — consistent
- `handleDisableCovered` uses `batchUpdateVendorMetricMappingRuleStatus` (existing) — consistent
