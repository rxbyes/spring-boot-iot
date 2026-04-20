<template>
  <PanelCard class="product-vendor-mapping-suggestion-panel">
    <section class="product-vendor-mapping-suggestion-panel__filters">
      <label class="product-vendor-mapping-suggestion-panel__checkbox">
        <input
          :checked="includeCovered"
          data-testid="vendor-mapping-suggestion-filter-covered"
          type="checkbox"
          @click="includeCovered = !includeCovered"
        />
        <span>包含已覆盖建议</span>
      </label>
      <label class="product-vendor-mapping-suggestion-panel__checkbox">
        <input
          :checked="includeIgnored"
          data-testid="vendor-mapping-suggestion-filter-ignored"
          type="checkbox"
          @click="includeIgnored = !includeIgnored"
        />
        <span>包含已忽略建议</span>
      </label>
    </section>

    <div v-if="loading" class="product-vendor-mapping-suggestion-panel__state">
      正在加载映射规则建议...
    </div>
    <div v-else-if="errorMessage" class="product-vendor-mapping-suggestion-panel__state">
      <strong>映射规则建议加载失败</strong>
      <p>{{ errorMessage }}</p>
      <button
        class="product-vendor-mapping-suggestion-panel__retry"
        data-testid="vendor-mapping-suggestion-retry"
        type="button"
        @click="loadSuggestions"
      >
        重试
      </button>
    </div>
    <div v-else-if="!suggestions.length" class="product-vendor-mapping-suggestion-panel__state">
      <strong>当前没有待处理的映射规则建议</strong>
      <p>当运行态证据命中未覆盖 rawIdentifier 时，这里会给出可治理的建议。</p>
    </div>

    <div v-else class="product-vendor-mapping-suggestion-panel__list">
      <article
        v-for="suggestion in suggestions"
        :key="suggestionKey(suggestion)"
        class="product-vendor-mapping-suggestion-panel__item"
      >
        <div class="product-vendor-mapping-suggestion-panel__item-main">
          <strong>{{ suggestion.rawIdentifier }}</strong>
          <span>{{ resolveTargetName(suggestion) }}</span>
          <small>
            {{ resolveSuggestionStatus(suggestion) }} · 证据 {{ suggestion.evidenceCount ?? 0 }} · 置信度
            {{ formatConfidence(suggestion.confidence) }}
          </small>
          <small>
            逻辑通道 {{ suggestion.logicalChannelCode || '--' }} · 样例值 {{ suggestion.sampleValue || '--' }}
          </small>
          <p>{{ suggestion.reason || '当前没有补充原因。' }}</p>
          <p v-if="suggestion.existingRuleId || suggestion.existingTargetNormativeIdentifier">
            existingRuleId={{ suggestion.existingRuleId || '--' }} · 已有目标={{
              suggestion.existingTargetNormativeIdentifier || '--'
            }}
          </p>

          <div
            v-if="canAccept(suggestion)"
            class="product-vendor-mapping-suggestion-panel__scope-grid"
          >
            <label class="product-vendor-mapping-suggestion-panel__scope-field">
              <span>治理范围</span>
              <select
                :data-testid="`vendor-mapping-suggestion-scope-${suggestionKey(suggestion)}`"
                :value="resolveScopeDraft(suggestion).scopeType"
                @change="handleScopeTypeChange(suggestion, ($event.target as HTMLSelectElement).value)"
              >
                <option value="PRODUCT">产品级</option>
                <option value="DEVICE_FAMILY">设备族级</option>
                <option value="SCENARIO">场景级</option>
                <option value="PROTOCOL">协议级</option>
                <option value="TENANT_DEFAULT">租户默认</option>
              </select>
            </label>

            <label
              v-if="resolveScopeDraft(suggestion).scopeType === 'DEVICE_FAMILY'"
              class="product-vendor-mapping-suggestion-panel__scope-field"
            >
              <span>deviceFamily</span>
              <input
                :data-testid="`vendor-mapping-suggestion-device-family-${suggestionKey(suggestion)}`"
                type="text"
                placeholder="请输入设备族编码"
                :value="resolveScopeDraft(suggestion).deviceFamily || ''"
                @input="handleScopeFieldInput(suggestion, 'deviceFamily', ($event.target as HTMLInputElement).value)"
              />
            </label>

            <label
              v-if="resolveScopeDraft(suggestion).scopeType === 'SCENARIO'"
              class="product-vendor-mapping-suggestion-panel__scope-field"
            >
              <span>scenarioCode</span>
              <input
                :data-testid="`vendor-mapping-suggestion-scenario-code-${suggestionKey(suggestion)}`"
                type="text"
                placeholder="请输入场景编码"
                :value="resolveScopeDraft(suggestion).scenarioCode || ''"
                @input="handleScopeFieldInput(suggestion, 'scenarioCode', ($event.target as HTMLInputElement).value)"
              />
            </label>

            <label
              v-if="resolveScopeDraft(suggestion).scopeType === 'PROTOCOL'"
              class="product-vendor-mapping-suggestion-panel__scope-field"
            >
              <span>protocolCode</span>
              <input
                :data-testid="`vendor-mapping-suggestion-protocol-code-${suggestionKey(suggestion)}`"
                type="text"
                placeholder="请输入协议编码或 family:*"
                :value="resolveScopeDraft(suggestion).protocolCode || ''"
                @input="handleScopeFieldInput(suggestion, 'protocolCode', ($event.target as HTMLInputElement).value)"
              />
            </label>
          </div>
        </div>

        <button
          v-if="canAccept(suggestion)"
          :data-testid="`vendor-mapping-suggestion-accept-${suggestionKey(suggestion)}`"
          class="product-vendor-mapping-suggestion-panel__action"
          type="button"
          :disabled="acceptingRowKey === suggestionKey(suggestion)"
          @click="handleAccept(suggestion)"
        >
          {{ acceptingRowKey === suggestionKey(suggestion) ? '创建中...' : '采纳建议' }}
        </button>
      </article>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

import {
  createVendorMetricMappingRule,
  listVendorMetricMappingRuleSuggestions
} from '@/api/vendorMetricMappingRule'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import type {
  IdType,
  VendorMetricMappingRuleCreatePayload,
  VendorMetricMappingRuleSuggestion,
  VendorMetricMappingRuleSuggestionQuery
} from '@/types/api'

type SuggestionRow = VendorMetricMappingRuleSuggestion & {
  suggestionStatus?: string | null
  targetNormativeName?: string | null
}

type ScopeDraft = Pick<
  VendorMetricMappingRuleCreatePayload,
  'scopeType' | 'protocolCode' | 'scenarioCode' | 'deviceFamily'
>

const props = defineProps<{
  productId?: IdType | null
  refreshToken?: string | number
}>()

const emit = defineEmits<{
  (event: 'accepted', payload: VendorMetricMappingRuleSuggestion): void
}>()

const includeCovered = ref(false)
const includeIgnored = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const suggestions = ref<SuggestionRow[]>([])
const scopeDraftsBySuggestionKey = ref<Record<string, ScopeDraft>>({})
const acceptingRowKey = ref<string | null>(null)
let messageModulePromise: Promise<typeof import('element-plus').ElMessage> | null = null
let loadController: AbortController | null = null
let latestLoadToken = 0

function currentQuery(): VendorMetricMappingRuleSuggestionQuery {
  return {
    includeCovered: includeCovered.value,
    includeIgnored: includeIgnored.value,
    minEvidenceCount: 1
  }
}

watch(
  [() => props.productId, () => props.refreshToken, includeCovered, includeIgnored],
  () => {
    void loadSuggestions()
  },
  { immediate: true }
)

async function loadSuggestions() {
  loadController?.abort()

  if (props.productId === null || props.productId === undefined || props.productId === '') {
    loadController = null
    latestLoadToken += 1
    suggestions.value = []
    scopeDraftsBySuggestionKey.value = {}
    errorMessage.value = ''
    loading.value = false
    return
  }

  const controller = new AbortController()
  const currentLoadToken = latestLoadToken + 1
  loadController = controller
  latestLoadToken = currentLoadToken
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await listVendorMetricMappingRuleSuggestions(props.productId, currentQuery(), {
      signal: controller.signal,
      suppressErrorToast: true
    })
    if (currentLoadToken !== latestLoadToken) {
      return
    }
    suggestions.value = normalizeSuggestions(response.data)
    scopeDraftsBySuggestionKey.value = {}
  } catch (error) {
    if (controller.signal.aborted || currentLoadToken !== latestLoadToken) {
      return
    }
    suggestions.value = []
    scopeDraftsBySuggestionKey.value = {}
    errorMessage.value = error instanceof Error ? error.message : '映射建议加载失败'
  } finally {
    if (loadController === controller) {
      loadController = null
    }
    if (currentLoadToken === latestLoadToken) {
      loading.value = false
    }
  }
}

function suggestionKey(suggestion: SuggestionRow) {
  if (suggestion.id !== undefined && suggestion.id !== null && suggestion.id !== '') {
    return String(suggestion.id)
  }
  return [suggestion.rawIdentifier, suggestion.logicalChannelCode || '', suggestion.targetNormativeIdentifier].join('::')
}

function canAccept(suggestion: SuggestionRow) {
  const status = resolveSuggestionStatus(suggestion)
  return status === 'READY_TO_CREATE' || status === 'LOW_CONFIDENCE'
}

function normalizeScopeType(value?: string | null): VendorMetricMappingRuleCreatePayload['scopeType'] {
  switch ((value || '').trim().toUpperCase()) {
    case 'DEVICE_FAMILY':
      return 'DEVICE_FAMILY'
    case 'SCENARIO':
      return 'SCENARIO'
    case 'PROTOCOL':
      return 'PROTOCOL'
    case 'TENANT_DEFAULT':
      return 'TENANT_DEFAULT'
    case 'PRODUCT':
    default:
      return 'PRODUCT'
  }
}

function defaultScopeDraft(suggestion: SuggestionRow): ScopeDraft {
  return {
    scopeType: normalizeScopeType(suggestion.recommendedScopeType),
    protocolCode: '',
    scenarioCode: '',
    deviceFamily: ''
  }
}

function resolveScopeDraft(suggestion: SuggestionRow): ScopeDraft {
  return scopeDraftsBySuggestionKey.value[suggestionKey(suggestion)] || defaultScopeDraft(suggestion)
}

function updateScopeDraft(suggestion: SuggestionRow, partial: Partial<ScopeDraft>) {
  const key = suggestionKey(suggestion)
  scopeDraftsBySuggestionKey.value = {
    ...scopeDraftsBySuggestionKey.value,
    [key]: {
      ...resolveScopeDraft(suggestion),
      ...partial
    }
  }
}

function handleScopeTypeChange(suggestion: SuggestionRow, value: string) {
  const scopeType = normalizeScopeType(value)
  updateScopeDraft(suggestion, {
    scopeType,
    protocolCode: scopeType === 'PROTOCOL' ? resolveScopeDraft(suggestion).protocolCode : '',
    scenarioCode: scopeType === 'SCENARIO' ? resolveScopeDraft(suggestion).scenarioCode : '',
    deviceFamily: scopeType === 'DEVICE_FAMILY' ? resolveScopeDraft(suggestion).deviceFamily : ''
  })
}

function handleScopeFieldInput(
  suggestion: SuggestionRow,
  field: 'protocolCode' | 'scenarioCode' | 'deviceFamily',
  value: string
) {
  updateScopeDraft(suggestion, {
    [field]: value
  })
}

function formatConfidence(confidence?: number | string | null) {
  const numericConfidence =
    typeof confidence === 'string' && confidence.trim() !== ''
      ? Number(confidence)
      : confidence

  if (typeof numericConfidence !== 'number' || Number.isNaN(numericConfidence)) {
    return '--'
  }
  return numericConfidence.toFixed(2)
}

async function handleAccept(suggestion: SuggestionRow) {
  const rowKey = suggestionKey(suggestion)
  acceptingRowKey.value = rowKey

  try {
    if (resolveSuggestionStatus(suggestion) === 'LOW_CONFIDENCE') {
      await confirmAction({
        title: '采纳低置信度建议',
        message: '当前证据次数较低，本次只会创建 DRAFT 草稿规则，不会自动生效。',
        type: 'warning',
        confirmButtonText: '继续创建草稿'
      })
    }

    const scopeDraft = resolveScopeDraft(suggestion)
    const payload: VendorMetricMappingRuleCreatePayload = {
      scopeType: scopeDraft.scopeType,
      protocolCode: normalizeOptionalText(scopeDraft.protocolCode),
      scenarioCode: normalizeOptionalText(scopeDraft.scenarioCode),
      deviceFamily: normalizeOptionalText(scopeDraft.deviceFamily),
      rawIdentifier: suggestion.rawIdentifier,
      logicalChannelCode: suggestion.logicalChannelCode ?? undefined,
      targetNormativeIdentifier: suggestion.targetNormativeIdentifier,
      status: 'DRAFT'
    }
    validateCreatePayload(payload)

    await createVendorMetricMappingRule(props.productId as IdType, payload)
    const message = await getMessageApi()
    message.success('映射规则草稿已创建')
    emit('accepted', suggestion)
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    if (!isHandledRequestError(error)) {
      const message = await getMessageApi()
      message.error(error instanceof Error ? error.message : '映射规则创建失败')
    }
  } finally {
    acceptingRowKey.value = null
  }
}

function normalizeSuggestions(
  data:
    | SuggestionRow[]
    | {
        list?: SuggestionRow[] | null
        records?: SuggestionRow[] | null
      }
    | null
    | undefined
) {
  if (Array.isArray(data)) {
    return data
  }
  if (Array.isArray(data?.list)) {
    return data.list
  }
  if (Array.isArray(data?.records)) {
    return data.records
  }
  return []
}

function resolveSuggestionStatus(suggestion: SuggestionRow) {
  return suggestion.status ?? suggestion.suggestionStatus ?? ''
}

function resolveTargetName(suggestion: SuggestionRow) {
  return suggestion.targetNormativeName || suggestion.targetNormativeIdentifier || '--'
}

function normalizeOptionalText(value?: string | null) {
  const trimmed = (value || '').trim()
  return trimmed ? trimmed : undefined
}

function validateCreatePayload(payload: VendorMetricMappingRuleCreatePayload) {
  if (payload.scopeType === 'DEVICE_FAMILY' && !payload.deviceFamily) {
    throw new Error('deviceFamily 不能为空')
  }
  if (payload.scopeType === 'SCENARIO' && !payload.scenarioCode) {
    throw new Error('scenarioCode 不能为空')
  }
  if (payload.scopeType === 'PROTOCOL' && !payload.protocolCode) {
    throw new Error('protocolCode 不能为空')
  }
}

function isHandledRequestError(error: unknown): error is { handled?: boolean } {
  return Boolean(error && typeof error === 'object' && 'handled' in error && (error as { handled?: boolean }).handled)
}

async function getMessageApi() {
  if (!messageModulePromise) {
    messageModulePromise = import('element-plus').then(({ ElMessage }) => ElMessage)
  }
  return messageModulePromise
}
</script>

<style scoped>
.product-vendor-mapping-suggestion-panel,
.product-vendor-mapping-suggestion-panel__filters,
.product-vendor-mapping-suggestion-panel__list,
.product-vendor-mapping-suggestion-panel__item,
.product-vendor-mapping-suggestion-panel__item-main {
  display: grid;
}

.product-vendor-mapping-suggestion-panel {
  gap: 0.75rem;
}

.product-vendor-mapping-suggestion-panel__filters {
  grid-template-columns: repeat(auto-fit, minmax(12rem, max-content));
  gap: 0.75rem;
}

.product-vendor-mapping-suggestion-panel__checkbox {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.product-vendor-mapping-suggestion-panel__state {
  padding: 1rem;
  color: var(--text-secondary);
}

.product-vendor-mapping-suggestion-panel__state p {
  margin: 0.45rem 0 0;
}

.product-vendor-mapping-suggestion-panel__list {
  gap: 0.75rem;
}

.product-vendor-mapping-suggestion-panel__item {
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
  padding: 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.9rem;
}

.product-vendor-mapping-suggestion-panel__item-main {
  gap: 0.2rem;
}

.product-vendor-mapping-suggestion-panel__scope-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
  gap: 0.55rem;
  margin-top: 0.45rem;
}

.product-vendor-mapping-suggestion-panel__scope-field {
  display: grid;
  gap: 0.25rem;
}

.product-vendor-mapping-suggestion-panel__scope-field span {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.product-vendor-mapping-suggestion-panel__scope-field select,
.product-vendor-mapping-suggestion-panel__scope-field input {
  width: 100%;
  padding: 0.45rem 0.55rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.7rem;
  background: #fff;
  color: var(--text-primary);
}

.product-vendor-mapping-suggestion-panel__item-main span,
.product-vendor-mapping-suggestion-panel__item-main small,
.product-vendor-mapping-suggestion-panel__item-main p {
  color: var(--text-secondary);
  margin: 0;
}

.product-vendor-mapping-suggestion-panel__action {
  border: 1px solid color-mix(in srgb, var(--brand) 42%, #fff);
  border-radius: 999px;
  background: #fff;
  color: var(--brand);
  cursor: pointer;
  padding: 0.45rem 0.8rem;
}

.product-vendor-mapping-suggestion-panel__action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.product-vendor-mapping-suggestion-panel__retry {
  width: fit-content;
  margin-top: 0.75rem;
  border: 1px solid color-mix(in srgb, var(--brand) 42%, #fff);
  border-radius: 999px;
  background: #fff;
  color: var(--brand);
  cursor: pointer;
  padding: 0.45rem 0.8rem;
}
</style>
