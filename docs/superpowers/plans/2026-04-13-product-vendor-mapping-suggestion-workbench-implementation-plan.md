# Product Vendor Mapping Suggestion Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `映射规则建议` block to `/products -> 契约字段`, allow users to accept actionable suggestions into `PRODUCT + DRAFT` vendor mapping rules, and show a stale-compare reminder after acceptance.

**Architecture:** Keep `ProductModelDesignerWorkspace.vue` as the orchestration shell for section order and cross-section reminders, and move suggestion-specific loading, filters, and row actions into a new `ProductVendorMappingSuggestionPanel.vue` child component. Add a dedicated frontend API module and shared types for suggestion read/create calls, then wire the panel into the workspace through `productId + refreshToken` props and an `accepted` event instead of auto-rerunning compare.

**Tech Stack:** Vue 3 Composition API, TypeScript, Vite, Vitest, Element Plus, existing `request` and `confirmAction` utilities.

---

## File Structure

- Create: `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts`
  Responsibility: isolated frontend API wrapper for `vendor-mapping-rule-suggestions` read calls and `vendor-mapping-rules` create calls.
- Create: `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue`
  Responsibility: render suggestion list, toggles, local loading/empty/error states, row-level accept action, and emit `accepted` to the parent.
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts`
  Responsibility: lock the new panel’s default query, filter reloads, action availability, low-confidence confirmation, and handled-error behavior.
- Modify: `spring-boot-iot-ui/src/types/api.ts`
  Responsibility: add shared suggestion/rule DTOs used by the API layer and panel.
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
  Responsibility: keep declaration mirror aligned with `src/types/api.ts`.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: insert the new section between `识别结果` and `本次生效`, own `refreshToken`, and show the compare-stale hint after accept.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
  Responsibility: verify section order, `accepted` event wiring, hint rendering, and product-switch prop flow without re-testing the panel’s inner behavior.
- Modify: `docs/02-业务功能与流程说明.md`
  Responsibility: document the new same-page governance block and its user-visible behavior.
- Modify: `docs/03-接口规范与接口清单.md`
  Responsibility: document how `/products -> 契约字段` consumes the existing suggestion/read-write endpoints.
- Modify: `docs/08-变更记录与技术债清单.md`
  Responsibility: record this frontend governance closure and verification evidence.
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: record the page-structure rule that suggestion governance stays in the same workbench instead of adding a drawer/route.

### Task 1: Add the Failing Suggestion Panel Tests

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'

import ProductVendorMappingSuggestionPanel from '@/components/product/ProductVendorMappingSuggestionPanel.vue'
import { createRequestError } from '@/api/request'

const {
  mockListVendorMetricMappingRuleSuggestions,
  mockCreateVendorMetricMappingRule,
  mockConfirmAction
} = vi.hoisted(() => ({
  mockListVendorMetricMappingRuleSuggestions: vi.fn(),
  mockCreateVendorMetricMappingRule: vi.fn(),
  mockConfirmAction: vi.fn()
}))

vi.mock('@/api/vendorMetricMappingRule', () => ({
  listVendorMetricMappingRuleSuggestions: mockListVendorMetricMappingRuleSuggestions,
  createVendorMetricMappingRule: mockCreateVendorMetricMappingRule
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: mockConfirmAction,
  isConfirmCancelled: (error: unknown) =>
    error === 'cancel' ||
    error === 'close' ||
    (error instanceof Error && ['cancel', 'close'].includes(error.message))
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  }
})

const actionableSuggestion = {
  rawIdentifier: 'L1_LF_1.value',
  logicalChannelCode: 'L1_LF_1',
  targetNormativeIdentifier: 'value',
  recommendedScopeType: 'PRODUCT',
  status: 'READY_TO_CREATE',
  confidence: '0.98',
  evidenceCount: 8,
  sampleValue: '0.2136',
  valueType: 'double',
  evidenceOrigin: 'RUNTIME',
  reason: '运行态证据与规范字段稳定命中'
}

const lowConfidenceSuggestion = {
  rawIdentifier: 'L1_QJ_1.AZI',
  logicalChannelCode: 'L1_QJ_1',
  targetNormativeIdentifier: 'sensor_state',
  recommendedScopeType: 'PRODUCT',
  status: 'LOW_CONFIDENCE',
  confidence: '0.41',
  evidenceCount: 1,
  sampleValue: '8.2772',
  valueType: 'double',
  evidenceOrigin: 'RUNTIME',
  reason: '证据次数较少，需要人工确认'
}

const conflictingSuggestion = {
  rawIdentifier: 'L1_QJ_1.angle',
  logicalChannelCode: 'L1_QJ_1',
  targetNormativeIdentifier: 'value',
  recommendedScopeType: 'PRODUCT',
  status: 'CONFLICTS_WITH_EXISTING',
  confidence: '0.93',
  evidenceCount: 5,
  sampleValue: '82.2744',
  valueType: 'double',
  evidenceOrigin: 'RUNTIME',
  reason: '当前产品已有命中规则',
  existingRuleId: 91,
  existingTargetNormativeIdentifier: 'sensor_state'
}

const coveredSuggestion = {
  rawIdentifier: 'L1_JS_1.gX',
  logicalChannelCode: 'L1_JS_1',
  targetNormativeIdentifier: 'value',
  recommendedScopeType: 'PRODUCT',
  status: 'ALREADY_COVERED',
  confidence: '1.00',
  evidenceCount: 12,
  sampleValue: '0.1667',
  valueType: 'double',
  evidenceOrigin: 'RUNTIME',
  reason: '现有规则已覆盖'
}

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountPanel(refreshToken = 0) {
  return mount(ProductVendorMappingSuggestionPanel, {
    props: {
      productId: 1001,
      refreshToken
    }
  })
}

describe('ProductVendorMappingSuggestionPanel', () => {
  beforeEach(() => {
    mockListVendorMetricMappingRuleSuggestions.mockReset()
    mockCreateVendorMetricMappingRule.mockReset()
    mockConfirmAction.mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.warning).mockReset()

    mockListVendorMetricMappingRuleSuggestions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [actionableSuggestion, lowConfidenceSuggestion, conflictingSuggestion]
    })

    mockCreateVendorMetricMappingRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 501,
        productId: 1001,
        scopeType: 'PRODUCT',
        rawIdentifier: 'L1_LF_1.value',
        logicalChannelCode: 'L1_LF_1',
        targetNormativeIdentifier: 'value',
        status: 'DRAFT'
      }
    })
  })

  it('loads actionable suggestions by default and reloads when filters or refresh context change', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(mockListVendorMetricMappingRuleSuggestions).toHaveBeenCalledWith(
      1001,
      { includeCovered: false, includeIgnored: false, minEvidenceCount: 1 },
      { suppressErrorToast: true }
    )
    expect(wrapper.text()).toContain('L1_LF_1.value')
    expect(wrapper.text()).toContain('L1_QJ_1.AZI')
    expect(wrapper.text()).toContain('L1_QJ_1.angle')
    expect(wrapper.text()).not.toContain('L1_JS_1.gX')

    mockListVendorMetricMappingRuleSuggestions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [actionableSuggestion, coveredSuggestion]
    })

    await wrapper.get('[data-testid="vendor-suggestion-toggle-covered"]').setValue(true)
    await flushPromises()

    expect(mockListVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(
      1001,
      { includeCovered: true, includeIgnored: false, minEvidenceCount: 1 },
      { suppressErrorToast: true }
    )
    expect(wrapper.text()).toContain('L1_JS_1.gX')

    mockListVendorMetricMappingRuleSuggestions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [actionableSuggestion]
    })

    await wrapper.setProps({ refreshToken: 1 })
    await flushPromises()

    expect(mockListVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(
      1001,
      { includeCovered: true, includeIgnored: false, minEvidenceCount: 1 },
      { suppressErrorToast: true }
    )

    mockListVendorMetricMappingRuleSuggestions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [lowConfidenceSuggestion]
    })

    await wrapper.setProps({ productId: 2002 })
    await flushPromises()

    expect(mockListVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(
      2002,
      { includeCovered: true, includeIgnored: false, minEvidenceCount: 1 },
      { suppressErrorToast: true }
    )
  })

  it('creates a PRODUCT draft rule for ready suggestions and emits accepted', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="vendor-suggestion-accept-L1_LF_1.value"]').trigger('click')
    await flushPromises()

    expect(mockCreateVendorMetricMappingRule).toHaveBeenCalledWith(
      1001,
      {
        scopeType: 'PRODUCT',
        rawIdentifier: 'L1_LF_1.value',
        logicalChannelCode: 'L1_LF_1',
        targetNormativeIdentifier: 'value',
        status: 'DRAFT'
      },
      {}
    )
    expect(wrapper.emitted('accepted')).toHaveLength(1)
    expect(ElMessage.success).toHaveBeenCalledWith('映射规则草稿已创建')
  })

  it('requires confirmation for low-confidence suggestions', async () => {
    mockConfirmAction.mockResolvedValue(undefined)

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="vendor-suggestion-accept-L1_QJ_1.AZI"]').trigger('click')
    await flushPromises()

    expect(mockConfirmAction).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '采纳低置信度建议',
        confirmButtonText: '继续创建草稿'
      })
    )
    expect(mockCreateVendorMetricMappingRule).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        rawIdentifier: 'L1_QJ_1.AZI',
        targetNormativeIdentifier: 'sensor_state',
        status: 'DRAFT'
      }),
      {}
    )
  })

  it('does not render an accept action for conflicting suggestions', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('existingRuleId')
    expect(wrapper.find('[data-testid="vendor-suggestion-accept-L1_QJ_1.angle"]').exists()).toBe(false)
  })

  it('does not add a second toast when create errors are already handled', async () => {
    mockCreateVendorMetricMappingRule.mockRejectedValueOnce(
      createRequestError('系统繁忙，请稍后重试！', true, 500)
    )

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.get('[data-testid="vendor-suggestion-accept-L1_LF_1.value"]').trigger('click')
    await flushPromises()

    expect(ElMessage.error).not.toHaveBeenCalled()
  })
})
```

- [ ] **Step 2: Run the panel test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts`

Expected: FAIL with a module resolution error for `@/components/product/ProductVendorMappingSuggestionPanel.vue` and/or `@/api/vendorMetricMappingRule`.

- [ ] **Step 3: Commit the red test**

```bash
git add spring-boot-iot-ui/src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts
git commit -m "test: cover vendor mapping suggestion panel"
```

### Task 2: Implement Shared Types, API Wrapper, and Suggestion Panel

**Files:**
- Create: `spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts`
- Create: `spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue`
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`

- [ ] **Step 1: Add the shared suggestion/rule types**

```ts
export type VendorMetricMappingRuleSuggestionStatus =
  | 'READY_TO_CREATE'
  | 'ALREADY_COVERED'
  | 'CONFLICTS_WITH_EXISTING'
  | 'LOW_CONFIDENCE'
  | `IGNORED_${string}`

export interface VendorMetricMappingRule {
  id?: IdType | null
  productId?: IdType | null
  scopeType?: string | null
  protocolCode?: string | null
  scenarioCode?: string | null
  deviceFamily?: string | null
  rawIdentifier?: string | null
  logicalChannelCode?: string | null
  relationConditionJson?: string | null
  normalizationRuleJson?: string | null
  targetNormativeIdentifier?: string | null
  status?: string | null
  versionNo?: number | null
  approvalOrderId?: IdType | null
  createBy?: IdType | null
  createTime?: string | null
  updateBy?: IdType | null
  updateTime?: string | null
}

export interface VendorMetricMappingRuleSuggestion {
  rawIdentifier: string
  logicalChannelCode?: string | null
  targetNormativeIdentifier: string
  recommendedScopeType?: string | null
  status: VendorMetricMappingRuleSuggestionStatus | string
  confidence?: string | null
  evidenceCount?: number | null
  sampleValue?: string | null
  valueType?: string | null
  evidenceOrigin?: string | null
  lastSeenTime?: string | null
  reason?: string | null
  existingRuleId?: IdType | null
  existingTargetNormativeIdentifier?: string | null
}

export interface VendorMetricMappingRuleSuggestionQuery {
  includeCovered?: boolean
  includeIgnored?: boolean
  minEvidenceCount?: number
}

export interface VendorMetricMappingRuleCreatePayload {
  scopeType: 'PRODUCT'
  rawIdentifier: string
  logicalChannelCode?: string | null
  targetNormativeIdentifier: string
  status: 'DRAFT'
}
```

- [ ] **Step 2: Add the dedicated frontend API module**

```ts
import { request } from './request'
import type { RequestOptions } from './request'
import type {
  ApiEnvelope,
  IdType,
  VendorMetricMappingRule,
  VendorMetricMappingRuleCreatePayload,
  VendorMetricMappingRuleSuggestion,
  VendorMetricMappingRuleSuggestionQuery
} from '../types/api'

type VendorMetricMappingRuleRequestOptions = Pick<RequestOptions, 'signal' | 'suppressErrorToast'>

function buildQuery(params: VendorMetricMappingRuleSuggestionQuery = {}) {
  const query = new URLSearchParams()
  if (params.includeCovered !== undefined) {
    query.set('includeCovered', String(params.includeCovered))
  }
  if (params.includeIgnored !== undefined) {
    query.set('includeIgnored', String(params.includeIgnored))
  }
  if (params.minEvidenceCount !== undefined) {
    query.set('minEvidenceCount', String(params.minEvidenceCount))
  }
  return query.toString()
}

export function listVendorMetricMappingRuleSuggestions(
  productId: IdType,
  query: VendorMetricMappingRuleSuggestionQuery = {},
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRuleSuggestion[]>> {
  const queryString = buildQuery(query)
  return request<VendorMetricMappingRuleSuggestion[]>(
    `/api/device/product/${productId}/vendor-mapping-rule-suggestions${queryString ? `?${queryString}` : ''}`,
    {
      method: 'GET',
      ...options
    }
  )
}

export function createVendorMetricMappingRule(
  productId: IdType,
  payload: VendorMetricMappingRuleCreatePayload,
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRule>> {
  return request<VendorMetricMappingRule>(`/api/device/product/${productId}/vendor-mapping-rules`, {
    method: 'POST',
    body: payload,
    ...options
  })
}
```

- [ ] **Step 3: Create the new panel component with read-side state and action availability**

```vue
<template>
  <div class="product-vendor-mapping-suggestion" data-testid="vendor-suggestion-panel">
    <div class="product-vendor-mapping-suggestion__toolbar">
      <label class="product-vendor-mapping-suggestion__toggle">
        <input
          data-testid="vendor-suggestion-toggle-covered"
          :checked="includeCovered"
          type="checkbox"
          @change="handleToggleCovered"
        />
        <span>显示已覆盖</span>
      </label>
      <label class="product-vendor-mapping-suggestion__toggle">
        <input
          data-testid="vendor-suggestion-toggle-ignored"
          :checked="includeIgnored"
          type="checkbox"
          @change="handleToggleIgnored"
        />
        <span>显示已忽略</span>
      </label>
    </div>

    <p v-if="loading" class="product-vendor-mapping-suggestion__tip">正在加载映射规则建议...</p>

    <div v-else-if="errorMessage" class="product-model-designer__empty">
      <strong>映射规则建议加载失败</strong>
      <p>{{ errorMessage }}</p>
      <button type="button" data-testid="vendor-suggestion-retry" @click="loadSuggestions">重试</button>
    </div>

    <div v-else-if="!suggestions.length" class="product-model-designer__empty">
      <strong>当前没有待处理的映射规则建议</strong>
      <p>当运行态证据命中未覆盖 rawIdentifier 时，这里会给出可治理的建议。</p>
    </div>

    <div v-else class="product-vendor-mapping-suggestion__list">
      <article
        v-for="suggestion in suggestions"
        :key="suggestionKey(suggestion)"
        class="product-vendor-mapping-suggestion__item"
      >
        <div class="product-vendor-mapping-suggestion__head">
          <div>
            <strong>{{ suggestion.rawIdentifier }}</strong>
            <p>{{ suggestion.targetNormativeIdentifier }} · {{ suggestion.status }}</p>
          </div>
          <button
            v-if="canAcceptSuggestion(suggestion)"
            :data-testid="`vendor-suggestion-accept-${suggestion.rawIdentifier}`"
            :disabled="isAccepting(suggestion)"
            type="button"
            @click="handleAccept(suggestion)"
          >
            采纳为草稿规则
          </button>
        </div>
        <div class="product-vendor-mapping-suggestion__meta">
          <span>{{ suggestion.logicalChannelCode || '--' }}</span>
          <span>{{ suggestion.confidence || '--' }}</span>
          <span>{{ suggestion.evidenceCount ?? 0 }}</span>
          <span>{{ suggestion.sampleValue || '--' }}</span>
        </div>
        <p>{{ suggestion.reason || '当前没有补充原因。' }}</p>
        <p v-if="suggestion.existingRuleId || suggestion.existingTargetNormativeIdentifier">
          existingRuleId={{ suggestion.existingRuleId || '--' }} · 已有目标={{ suggestion.existingTargetNormativeIdentifier || '--' }}
        </p>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

import { createVendorMetricMappingRule, listVendorMetricMappingRuleSuggestions } from '@/api/vendorMetricMappingRule'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import type { IdType, VendorMetricMappingRuleSuggestion } from '@/types/api'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'

const props = defineProps<{
  productId?: IdType | null
  refreshToken?: number
}>()

const emit = defineEmits<{
  (event: 'accepted', payload: { suggestion: VendorMetricMappingRuleSuggestion }): void
}>()

const includeCovered = ref(false)
const includeIgnored = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const suggestions = ref<VendorMetricMappingRuleSuggestion[]>([])
const acceptingKeys = ref<Record<string, boolean>>({})

function suggestionKey(suggestion: VendorMetricMappingRuleSuggestion) {
  return `${suggestion.rawIdentifier}::${suggestion.logicalChannelCode || ''}::${suggestion.targetNormativeIdentifier}`
}

function canAcceptSuggestion(suggestion: VendorMetricMappingRuleSuggestion) {
  return suggestion.status === 'READY_TO_CREATE' || suggestion.status === 'LOW_CONFIDENCE'
}

function isAccepting(suggestion: VendorMetricMappingRuleSuggestion) {
  return Boolean(acceptingKeys.value[suggestionKey(suggestion)])
}

async function loadSuggestions() {
  if (!props.productId) {
    suggestions.value = []
    errorMessage.value = ''
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listVendorMetricMappingRuleSuggestions(
      props.productId,
      {
        includeCovered: includeCovered.value,
        includeIgnored: includeIgnored.value,
        minEvidenceCount: 1
      },
      { suppressErrorToast: true }
    )
    suggestions.value = response.data ?? []
  } catch (error) {
    suggestions.value = []
    errorMessage.value = resolveRequestErrorMessage(error, '加载映射规则建议失败')
  } finally {
    loading.value = false
  }
}

async function handleAccept(suggestion: VendorMetricMappingRuleSuggestion) {
  const key = suggestionKey(suggestion)
  acceptingKeys.value = { ...acceptingKeys.value, [key]: true }
  try {
    if (suggestion.status === 'LOW_CONFIDENCE') {
      await confirmAction({
        title: '采纳低置信度建议',
        message: '当前证据次数较低，本次只会创建 DRAFT 草稿规则，不会自动生效。',
        confirmButtonText: '继续创建草稿'
      })
    }

    await createVendorMetricMappingRule(props.productId as IdType, {
      scopeType: 'PRODUCT',
      rawIdentifier: suggestion.rawIdentifier,
      logicalChannelCode: suggestion.logicalChannelCode ?? undefined,
      targetNormativeIdentifier: suggestion.targetNormativeIdentifier,
      status: 'DRAFT'
    })
    ElMessage.success('映射规则草稿已创建')
    emit('accepted', { suggestion })
  } catch (error) {
    if (!isConfirmCancelled(error) && !isHandledRequestError(error)) {
      ElMessage.error(resolveRequestErrorMessage(error, '创建映射规则草稿失败'))
    }
  } finally {
    acceptingKeys.value = { ...acceptingKeys.value, [key]: false }
  }
}

function handleToggleCovered(event: Event) {
  includeCovered.value = (event.target as HTMLInputElement).checked
}

function handleToggleIgnored(event: Event) {
  includeIgnored.value = (event.target as HTMLInputElement).checked
}

watch(
  [() => props.productId, () => props.refreshToken, includeCovered, includeIgnored],
  () => {
    void loadSuggestions()
  },
  { immediate: true }
)
</script>
```

- [ ] **Step 4: Add the minimal panel styles so it matches the existing white-card governance language**

```css
.product-vendor-mapping-suggestion,
.product-vendor-mapping-suggestion__list {
  display: grid;
  gap: 0.72rem;
}

.product-vendor-mapping-suggestion__toolbar,
.product-vendor-mapping-suggestion__meta,
.product-vendor-mapping-suggestion__head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.56rem;
  align-items: center;
  justify-content: space-between;
}

.product-vendor-mapping-suggestion__item {
  display: grid;
  gap: 0.4rem;
  padding: 0.88rem 0.94rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: 0.76rem;
  background: white;
}

.product-vendor-mapping-suggestion__toggle {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  color: var(--text-secondary);
}

.product-vendor-mapping-suggestion__tip,
.product-vendor-mapping-suggestion__item p,
.product-vendor-mapping-suggestion__meta span {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.56;
}
```

- [ ] **Step 5: Run the panel test to verify it passes**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts`

Expected: PASS for all `ProductVendorMappingSuggestionPanel` cases.

- [ ] **Step 6: Commit the panel implementation**

```bash
git add spring-boot-iot-ui/src/types/api.ts spring-boot-iot-ui/src/types/api.d.ts spring-boot-iot-ui/src/api/vendorMetricMappingRule.ts spring-boot-iot-ui/src/components/product/ProductVendorMappingSuggestionPanel.vue
git commit -m "feat: add vendor mapping suggestion panel"
```

### Task 3: Add the Failing Workspace Integration Tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Stub the new child component and add failing integration assertions**

```ts
const SuggestionPanelStub = defineComponent({
  name: 'ProductVendorMappingSuggestionPanel',
  props: ['productId', 'refreshToken'],
  emits: ['accepted'],
  template: `
    <section class="vendor-suggestion-panel-stub" data-testid="vendor-suggestion-panel-stub">
      <span data-testid="vendor-suggestion-panel-props">{{ productId }}|{{ refreshToken }}</span>
      <button type="button" data-testid="vendor-suggestion-panel-accepted" @click="$emit('accepted', { suggestion: { rawIdentifier: 'L1_LF_1.value' } })">
        accept
      </button>
    </section>
  `
})

function mountWorkspace(productOverrides?: Partial<{ id: number; productKey: string; productName: string; protocolCode: string; nodeType: number; deviceCount: number; metadataJson: string | null }>) {
  return mount(ProductModelDesignerWorkspace, {
    props: {
      product: {
        id: 1001,
        productKey: 'south-crack-sensor-v1',
        productName: '南方裂缝传感器',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        deviceCount: 3,
        metadataJson: null,
        ...productOverrides
      }
    },
    attachTo: document.body,
    global: {
      stubs: {
        StandardButton: StandardButtonStub,
        ProductModelGovernanceCompareTable: CompareTableStub,
        ProductVendorMappingSuggestionPanel: SuggestionPanelStub,
        ElInput: ElInputStub,
        ElTag: true
      }
    }
  })
}

it('renders the mapping suggestion section between compare and apply and bumps refresh token after accept', async () => {
  const wrapper = mountWorkspace()
  await flushPromises()
  await nextTick()

  const pageText = wrapper.text()
  expect(pageText.indexOf('识别结果')).toBeLessThan(pageText.indexOf('映射规则建议'))
  expect(pageText.indexOf('映射规则建议')).toBeLessThan(pageText.indexOf('本次生效'))
  expect(wrapper.get('[data-testid="vendor-suggestion-panel-props"]').text()).toContain('1001|0')

  await wrapper.get('[data-testid="vendor-suggestion-panel-accepted"]').trigger('click')
  await nextTick()

  expect(wrapper.get('[data-testid="vendor-suggestion-panel-props"]').text()).toContain('1001|1')
  expect(wrapper.get('[data-testid="contract-field-suggestion-refresh-hint"]').text()).toContain('映射规则草稿已创建')
})

it('clears the refresh hint and passes the new product id when switching products', async () => {
  const wrapper = mountWorkspace()
  await flushPromises()
  await nextTick()

  await wrapper.get('[data-testid="vendor-suggestion-panel-accepted"]').trigger('click')
  await nextTick()

  await wrapper.setProps({
    product: {
      id: 2002,
      productKey: 'north-tilt-sensor-v1',
      productName: '北坡倾角传感器',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      deviceCount: 2,
      metadataJson: null
    }
  })
  await flushPromises()
  await nextTick()

  expect(wrapper.get('[data-testid="vendor-suggestion-panel-props"]').text()).toContain('2002|0')
  expect(wrapper.find('[data-testid="contract-field-suggestion-refresh-hint"]').exists()).toBe(false)
})
```

- [ ] **Step 2: Run the workspace test to verify it fails**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

Expected: FAIL because `映射规则建议` section, refresh token wiring, and stale-compare hint do not exist yet.

- [ ] **Step 3: Commit the red integration test**

```bash
git add spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "test: cover suggestion workbench integration"
```

### Task 4: Wire the Suggestion Panel into the Contract Workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: Import the new child component and add local refresh/hint state**

```ts
import ProductVendorMappingSuggestionPanel from '@/components/product/ProductVendorMappingSuggestionPanel.vue'

const vendorSuggestionRefreshToken = ref(0)
const showSuggestionRefreshHint = ref(false)

function handleVendorSuggestionAccepted() {
  vendorSuggestionRefreshToken.value += 1
  showSuggestionRefreshHint.value = true
}
```

- [ ] **Step 2: Insert the new section between `识别结果` and `本次生效`**

```vue
<section class="product-model-designer__stage" data-testid="contract-field-vendor-suggestions">
  <header class="product-model-designer__stage-head">
    <div>
      <h3>映射规则建议</h3>
      <p>基于运行态证据和现有规则，展示当前产品值得人工采纳的厂商字段映射建议。</p>
    </div>
  </header>

  <ProductVendorMappingSuggestionPanel
    :product-id="props.product?.id ?? null"
    :refresh-token="vendorSuggestionRefreshToken"
    @accepted="handleVendorSuggestionAccepted"
  />

  <div
    v-if="showSuggestionRefreshHint"
    class="product-model-designer__governance-note"
    data-testid="contract-field-suggestion-refresh-hint"
  >
    <strong>映射规则草稿已创建</strong>
    <p>若要让本次识别结果使用新规则，请重新执行识别。</p>
  </div>
</section>
```

- [ ] **Step 3: Clear the stale hint when compare reruns or the session resets**

```ts
async function handleCompare() {
  if (!props.product?.id || !validateBeforeCompare()) {
    return
  }
  compareLoading.value = true
  showSuggestionRefreshHint.value = false
  applyResult.value = null
  rollbackResult.value = null
  applyApprovalDetail.value = null
  rollbackApprovalDetail.value = null
  try {
    const response = await productApi.compareProductModelGovernance(props.product.id, {
      manualExtract: {
        sampleType: sampleType.value,
        deviceStructure: deviceStructure.value,
        samplePayload: samplePayload.value.trim(),
        parentDeviceCode: deviceStructure.value === 'composite' ? parentDeviceCode.value.trim() || undefined : undefined,
        relationMappings: deviceStructure.value === 'composite' ? normalizeRelationMappings() : undefined
      }
    })
    compareResult.value = response.data ?? null
    decisionState.value = Object.fromEntries(
      (response.data?.compareRows ?? []).map((row) => [rowKey(row), defaultDecisionForRow(row)])
    )
  } catch (error) {
    compareResult.value = null
    decisionState.value = {}
    ElMessage.error(error instanceof Error ? error.message : '提取契约字段失败')
  } finally {
    compareLoading.value = false
  }
}

function resetSession() {
  compareResult.value = null
  applyResult.value = null
  rollbackResult.value = null
  vendorSuggestionRefreshToken.value = 0
  showSuggestionRefreshHint.value = false
  resetRollbackPreview()
  releaseLedgerRows.value = []
  resetVersionLedger()
  applyApprovalDetail.value = null
  rollbackApprovalDetail.value = null
  applyApprovalLoading.value = false
  rollbackApprovalLoading.value = false
  applyResubmitLoading.value = false
  rollbackResubmitLoading.value = false
  decisionState.value = {}
  latestReleaseBatchId.value = null
  sampleType.value = 'business'
  deviceStructure.value = 'single'
  samplePayload.value = ''
  parentDeviceCode.value = ''
  relationMappings.value = [createRelationRow()]
  samplePayloadError.value = ''
  cancelRenameModel()
}
```

- [ ] **Step 4: Run both focused frontend test files**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

Expected: PASS for the new panel tests and the updated workspace integration tests.

- [ ] **Step 5: Commit the workspace integration**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
git commit -m "feat: integrate vendor mapping suggestions into products workbench"
```

### Task 5: Update Docs and Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update `docs/02` to describe the new same-page governance flow**

```md
- `2026-04-13` 起，`/products -> 契约字段` 在 `识别结果` 与 `本次生效` 之间新增 `映射规则建议` 区块，同页展示当前产品的厂商字段映射建议。首屏默认只展示 `READY_TO_CREATE / CONFLICTS_WITH_EXISTING / LOW_CONFIDENCE` 三类需人工处理项，并通过 `显示已覆盖 / 显示已忽略` 轻开关按需补看 `ALREADY_COVERED / IGNORED_*`。
- 该区块当前只支持把 `READY_TO_CREATE` 与 `LOW_CONFIDENCE` 建议采纳为 `PRODUCT + DRAFT` 草稿规则；`LOW_CONFIDENCE` 采纳前必须二次确认，`CONFLICTS_WITH_EXISTING` 只展示冲突信息、不允许强制覆盖。采纳成功后页面只刷新建议区块，并提示“若要让本次识别结果使用新规则，请重新执行识别”，不会自动重跑 compare。
```

- [ ] **Step 2: Update `docs/03` to record the page’s read/write contract**

```md
- `GET /api/device/product/{productId}/vendor-mapping-rule-suggestions` 除了提供建议预览外，`2026-04-13` 起还被 `/products -> 契约字段 -> 映射规则建议` 同页工作区直接消费；前端默认查询参数固定为 `includeCovered=false`、`includeIgnored=false`、`minEvidenceCount=1`，切换 `显示已覆盖 / 显示已忽略` 时会重新请求该接口，而不是本地伪过滤。
- `/products -> 契约字段 -> 映射规则建议` 的“采纳为草稿规则”当前不新增专用写接口，继续复用 `POST /api/device/product/{productId}/vendor-mapping-rules`。该入口固定提交最小 payload：`scopeType=PRODUCT`、`rawIdentifier`、`logicalChannelCode`、`targetNormativeIdentifier`、`status=DRAFT`；首版不允许在该入口编辑 `relationConditionJson / normalizationRuleJson`，也不支持 scope 提升或批量采纳。
```

- [ ] **Step 3: Update `docs/08` to log the feature and verification evidence**

```md
- 2026-04-13：`/products -> 契约字段` 已补齐“厂商字段映射规则建议 -> 采纳为 DRAFT 草稿”前端最小闭环。`spring-boot-iot-ui` 当前新增 `ProductVendorMappingSuggestionPanel.vue` 与独立 `vendorMetricMappingRule.ts` API 封装，在 `识别结果` 与 `本次生效` 之间接入同页 `映射规则建议` 区块；默认只展示 `READY_TO_CREATE / CONFLICTS_WITH_EXISTING / LOW_CONFIDENCE`，并支持把 `READY_TO_CREATE` 与 `LOW_CONFIDENCE` 建议写成 `PRODUCT + DRAFT` 规则。采纳成功后工作台会提示“若要让本次识别结果使用新规则，请重新执行识别”，但不会自动重跑 compare。定向验证命令：`npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`，以及 `npm --prefix spring-boot-iot-ui run component:guard`。本轮同步更新 `docs/02`、`docs/03`、`docs/08`、`docs/15`；README.md 与 AGENTS.md 检查后无需变更。
```

- [ ] **Step 4: Update `docs/15` so future front-end work keeps this governance block in the same workbench**

```md
- `/products -> 契约字段` 若继续扩展治理候选区，必须保持“样本输入 / 识别结果 / 映射规则建议 / 本次生效 / 当前已生效字段”的同页工作区语法；`映射规则建议` 只能作为现有工作台内的治理区块扩展，不得再回流第二层抽屉、轻路由或页头跨页功能菜单。
```

- [ ] **Step 5: Run the final focused verification**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductVendorMappingSuggestionPanel.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
Expected: PASS for both updated frontend suites.

Run: `npm --prefix spring-boot-iot-ui run component:guard`
Expected: PASS with no shared component contract regressions.

- [ ] **Step 6: Commit docs and verification-backed finish**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: record vendor mapping suggestion workbench flow"
```
