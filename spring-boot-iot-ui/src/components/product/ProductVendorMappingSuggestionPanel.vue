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
  } catch (error) {
    if (controller.signal.aborted || currentLoadToken !== latestLoadToken) {
      return
    }
    suggestions.value = []
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

    const payload: VendorMetricMappingRuleCreatePayload = {
      scopeType: 'PRODUCT',
      rawIdentifier: suggestion.rawIdentifier,
      logicalChannelCode: suggestion.logicalChannelCode ?? undefined,
      targetNormativeIdentifier: suggestion.targetNormativeIdentifier,
      status: 'DRAFT'
    }

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
