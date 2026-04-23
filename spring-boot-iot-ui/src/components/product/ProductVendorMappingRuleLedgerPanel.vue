<template>
  <section class="product-vendor-rule-ledger" data-testid="product-vendor-rule-ledger">
    <div class="product-vendor-rule-ledger__summary">
      <article class="product-vendor-rule-ledger__summary-card">
        <span>规则条目</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article class="product-vendor-rule-ledger__summary-card">
        <span>已发布</span>
        <strong>{{ publishedCount }}</strong>
      </article>
      <article class="product-vendor-rule-ledger__summary-card">
        <span>草稿待发布</span>
        <strong>{{ draftCount }}</strong>
      </article>
    </div>

    <p v-if="loading" class="product-vendor-rule-ledger__hint">正在加载映射规则台账...</p>
    <p v-else-if="errorMessage" class="product-vendor-rule-ledger__hint">{{ errorMessage }}</p>

    <template v-else-if="rows.length">
      <div class="product-vendor-rule-ledger__batch">
        <label class="product-vendor-rule-ledger__batch-select-all">
          <input
            data-testid="rule-ledger-select-all"
            type="checkbox"
            :checked="allSelectableSelected"
            :disabled="!selectableRuleIds.length"
            @change="toggleSelectAll(($event.target as HTMLInputElement).checked)"
          />
          <span>{{ `已选 ${selectedRuleIds.length} 项` }}</span>
        </label>
        <div class="product-vendor-rule-ledger__batch-actions">
          <StandardButton
            data-testid="rule-ledger-batch-status-active"
            :disabled="!selectedRuleIds.length || isSubmitting('batch-status-active')"
            @click="handleBatchStatus('ACTIVE')"
          >
            {{ isSubmitting('batch-status-active') ? '提交中...' : '批量启用' }}
          </StandardButton>
          <StandardButton
            data-testid="rule-ledger-batch-status-disabled"
            :disabled="!selectedRuleIds.length || isSubmitting('batch-status-disabled')"
            @click="handleBatchStatus('DISABLED')"
          >
            {{ isSubmitting('batch-status-disabled') ? '提交中...' : '批量停用' }}
          </StandardButton>
        </div>
      </div>
      <p
        v-if="batchStatusSummary"
        data-testid="rule-ledger-batch-status-summary"
        class="product-vendor-rule-ledger__hint"
      >
        {{ batchStatusSummaryLabel(batchStatusSummary) }}
      </p>

      <div class="product-vendor-rule-ledger__list">
        <article
          v-for="row in rows"
          :key="rowIdentity(row)"
          class="product-vendor-rule-ledger__item"
        >
        <div class="product-vendor-rule-ledger__headline">
          <label class="product-vendor-rule-ledger__item-selector">
            <input
              :data-testid="`rule-ledger-select-${rowIdentity(row)}`"
              type="checkbox"
              :checked="isRowSelected(row)"
              :disabled="row.ruleId == null"
              @change="toggleRowSelection(row, ($event.target as HTMLInputElement).checked)"
            />
            <span>选择</span>
          </label>
          <div class="product-vendor-rule-ledger__title">
            <strong>{{ `${row.rawIdentifier || '--'} -> ${row.targetNormativeIdentifier || '--'}` }}</strong>
            <span>{{ `${scopeTypeLabel(row.scopeType)} · ${versionLabel(row)}` }}</span>
            <span>{{ `${row.draftStatus || '--'} · ${row.publishedStatus || '未发布'}` }}</span>
            <span
              v-if="row.coveredByFormalField"
              class="product-vendor-rule-ledger__coverage-tag"
            >
              已被正式字段覆盖
            </span>
          </div>
          <span v-if="row.latestApprovalOrderId != null" class="product-vendor-rule-ledger__approval">
            {{ `审批单 ${row.latestApprovalOrderId}` }}
          </span>
        </div>

        <div class="product-vendor-rule-ledger__meta">
          <span>{{ `命中来源 ${row.publishedSource || '--'}` }}</span>
          <span>{{ `逻辑通道 ${row.logicalChannelCode || '--'}` }}</span>
          <span v-if="scopeSignatureValue(row)">{{ `范围 ${scopeSignatureValue(row)}` }}</span>
        </div>

        <div class="product-vendor-rule-ledger__actions">
          <StandardButton
            v-if="row.coveredByFormalField && row.draftStatus === 'ACTIVE'"
            :data-testid="`rule-ledger-disable-covered-${rowIdentity(row)}`"
            :disabled="isSubmitting(`disable-${rowIdentity(row)}`)"
            @click="handleDisableCovered(row)"
          >
            {{ isSubmitting(`disable-${rowIdentity(row)}`) ? '停用中...' : '一键停用' }}
          </StandardButton>
          <StandardButton
            :data-testid="`rule-ledger-preview-hit-${rowIdentity(row)}`"
            :disabled="!row.rawIdentifier || isSubmitting(`preview-${rowIdentity(row)}`)"
            @click="handlePreview(row)"
          >
            {{ isSubmitting(`preview-${rowIdentity(row)}`) ? '试命中中...' : '试命中' }}
          </StandardButton>
          <StandardButton
            :data-testid="`rule-ledger-submit-publish-${rowIdentity(row)}`"
            :disabled="!row.ruleId || isSubmitting(`publish-${row.ruleId}`)"
            @click="handleSubmitPublish(row)"
          >
            {{ isSubmitting(`publish-${row.ruleId}`) ? '提交中...' : '提交发布审批' }}
          </StandardButton>
          <StandardButton
            :data-testid="`rule-ledger-submit-rollback-${rowIdentity(row)}`"
            :disabled="!canRollback(row) || isSubmitting(`rollback-${row.ruleId}`)"
            @click="handleSubmitRollback(row)"
          >
            {{ isSubmitting(`rollback-${row.ruleId}`) ? '提交中...' : '提交回滚审批' }}
          </StandardButton>
        </div>

        <div class="product-vendor-rule-ledger__replay">
          <input
            :data-testid="`rule-ledger-replay-sample-${rowIdentity(row)}`"
            class="product-vendor-rule-ledger__replay-input"
            type="text"
            placeholder="样例值（可选）"
            :value="replaySampleByRuleId[rowIdentity(row)] || ''"
            @input="handleReplaySampleInput(row, ($event.target as HTMLInputElement).value)"
          />
          <StandardButton
            :data-testid="`rule-ledger-replay-submit-${rowIdentity(row)}`"
            :disabled="!row.rawIdentifier || isSubmitting(`replay-${rowIdentity(row)}`)"
            @click="handleReplay(row)"
          >
            {{ isSubmitting(`replay-${rowIdentity(row)}`) ? '回放中...' : '回放校验' }}
          </StandardButton>
        </div>

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
        </article>
      </div>
    </template>

    <div v-else class="product-vendor-rule-ledger__empty">
      <strong>当前还没有映射规则台账</strong>
      <p>先在映射规则建议中采纳草稿，或等待运行态证据继续补齐候选。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import {
  batchUpdateVendorMetricMappingRuleStatus,
  listVendorMetricMappingRuleLedger,
  previewVendorMetricMappingRuleHit,
  replayVendorMetricMappingRule,
  submitVendorMetricMappingRulePublish,
  submitVendorMetricMappingRuleRollback
} from '@/api/vendorMetricMappingRule'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import StandardButton from '@/components/StandardButton.vue'
import { confirmAction } from '@/utils/confirm'
import { ElMessage } from '@/utils/message'
import type {
  IdType,
  VendorMetricMappingRuleBatchStatusResult,
  VendorMetricMappingRuleHitPreview,
  VendorMetricMappingRuleLedgerRow,
  VendorMetricMappingRuleReplay
} from '@/types/api'

const props = defineProps<{
  productId?: IdType | null
}>()

const rows = ref<VendorMetricMappingRuleLedgerRow[]>([])
const loading = ref(false)
const errorMessage = ref('')
const submittingKey = ref('')
const previewStateByRuleId = ref<Record<string, VendorMetricMappingRuleHitPreview>>({})
const replayStateByRuleId = ref<Record<string, VendorMetricMappingRuleReplay>>({})
const replaySampleByRuleId = ref<Record<string, string>>({})
const selectedRuleIds = ref<IdType[]>([])
const batchStatusSummary = ref<VendorMetricMappingRuleBatchStatusResult | null>(null)

const publishedCount = computed(() => rows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length)
const draftCount = computed(() => rows.value.filter((row) => row.draftStatus === 'DRAFT').length)
const selectableRuleIds = computed(() =>
  rows.value
    .map((row) => row.ruleId)
    .filter((ruleId): ruleId is IdType => ruleId !== null && ruleId !== undefined && ruleId !== '')
)
const allSelectableSelected = computed(() =>
  Boolean(selectableRuleIds.value.length) && selectedRuleIds.value.length === selectableRuleIds.value.length
)

watch(
  () => props.productId,
  () => {
    batchStatusSummary.value = null
    void loadRows()
  },
  { immediate: true }
)

function hasProductId(value: IdType | null | undefined): value is IdType {
  return value !== null && value !== undefined && value !== ''
}

function versionLabel(row: VendorMetricMappingRuleLedgerRow) {
  return `v${row.draftVersionNo ?? '--'} / v${row.publishedVersionNo ?? '--'}`
}

function rowIdentity(row: VendorMetricMappingRuleLedgerRow) {
  if (row.ruleId !== null && row.ruleId !== undefined && row.ruleId !== '') {
    return String(row.ruleId)
  }
  return `${row.rawIdentifier || '--'}-${row.targetNormativeIdentifier || '--'}`
}

function normalizeId(value: IdType) {
  return String(value)
}

function scopeTypeLabel(scopeType?: string | null) {
  switch (scopeType) {
    case 'PRODUCT':
      return '产品级'
    case 'DEVICE_FAMILY':
      return '设备族级'
    case 'SCENARIO':
      return '场景级'
    case 'PROTOCOL':
      return '协议级'
    case 'TENANT_DEFAULT':
      return '租户默认'
    default:
      return scopeType || '--'
  }
}

function scopeSignatureValue(row: VendorMetricMappingRuleLedgerRow) {
  switch (row.scopeType) {
    case 'DEVICE_FAMILY':
      return row.deviceFamily || '--'
    case 'SCENARIO':
      return row.scenarioCode || '--'
    case 'PROTOCOL':
      return row.protocolCode || '--'
    default:
      return ''
  }
}

function isSubmitting(key: string) {
  return submittingKey.value === key
}

function canRollback(row: VendorMetricMappingRuleLedgerRow) {
  return Boolean(row.ruleId) && row.publishedStatus === 'PUBLISHED'
}

function isRowSelected(row: VendorMetricMappingRuleLedgerRow) {
  if (row.ruleId === null || row.ruleId === undefined || row.ruleId === '') {
    return false
  }
  const ruleId = normalizeId(row.ruleId)
  return selectedRuleIds.value.some((selectedRuleId) => normalizeId(selectedRuleId) === ruleId)
}

function toggleRowSelection(row: VendorMetricMappingRuleLedgerRow, checked: boolean) {
  if (row.ruleId === null || row.ruleId === undefined || row.ruleId === '') {
    return
  }
  const ruleId = normalizeId(row.ruleId)
  const nextSelectedRuleIds = selectedRuleIds.value.filter(
    (selectedRuleId) => normalizeId(selectedRuleId) !== ruleId
  )
  if (checked) {
    nextSelectedRuleIds.push(row.ruleId)
  }
  selectedRuleIds.value = nextSelectedRuleIds
}

function toggleSelectAll(checked: boolean) {
  selectedRuleIds.value = checked ? [...selectableRuleIds.value] : []
}

function batchStatusSummaryLabel(summary: VendorMetricMappingRuleBatchStatusResult) {
  const requested = summary.requestedCount ?? 0
  const matched = summary.matchedCount ?? 0
  const changed = summary.changedCount ?? 0
  const targetStatus = summary.targetStatus || '--'
  return `请求 ${requested} · 命中 ${matched} · 变更 ${changed} · 目标 ${targetStatus}`
}

function previewMatchedLabel(preview?: VendorMetricMappingRuleHitPreview | null) {
  if (!preview) {
    return '--'
  }
  return preview.matched ? '已命中发布规则' : '未命中发布规则'
}

function previewSourceLabel(preview?: VendorMetricMappingRuleHitPreview | null) {
  if (!preview) {
    return '--'
  }
  const target = preview.targetNormativeIdentifier || '--'
  const source = preview.hitSource || '--'
  return `${source} · ${target}`
}

function replayMatchedLabel(replay?: VendorMetricMappingRuleReplay | null) {
  if (!replay) {
    return '--'
  }
  return replay.matched ? '回放命中规则' : '回放未命中规则'
}

function replaySourceAndScopeLabel(replay?: VendorMetricMappingRuleReplay | null) {
  if (!replay) {
    return '--'
  }
  return `${replay.hitSource || '--'} · ${replay.matchedScopeType || '--'}`
}

function replayCanonicalLabel(replay?: VendorMetricMappingRuleReplay | null) {
  if (!replay) {
    return 'canonical --'
  }
  const canonicalIdentifier = replay.canonicalIdentifier || replay.targetNormativeIdentifier || '--'
  return `canonical ${canonicalIdentifier}`
}

function replaySampleLabel(replay?: VendorMetricMappingRuleReplay | null) {
  if (!replay) {
    return '样例值 --'
  }
  return `样例值 ${replay.sampleValue || '--'}`
}

function showRequestErrorMessage(error: unknown, fallbackMessage: string) {
  if (isHandledRequestError(error)) {
    return
  }
  ElMessage.error(resolveRequestErrorMessage(error, fallbackMessage))
}

async function loadRows() {
  if (!hasProductId(props.productId)) {
    rows.value = []
    previewStateByRuleId.value = {}
    replayStateByRuleId.value = {}
    replaySampleByRuleId.value = {}
    selectedRuleIds.value = []
    batchStatusSummary.value = null
    return
  }
  loading.value = true
  errorMessage.value = ''
  previewStateByRuleId.value = {}
  replayStateByRuleId.value = {}
  replaySampleByRuleId.value = {}
  selectedRuleIds.value = []
  try {
    const response = await listVendorMetricMappingRuleLedger(props.productId)
    rows.value = response.data ?? []
  } catch (error) {
    rows.value = []
    errorMessage.value = resolveRequestErrorMessage(error, '映射规则台账加载失败')
  } finally {
    loading.value = false
  }
}

async function handlePreview(row: VendorMetricMappingRuleLedgerRow) {
  if (!hasProductId(props.productId) || !row.rawIdentifier) {
    return
  }
  const previewKey = `preview-${rowIdentity(row)}`
  submittingKey.value = previewKey
  try {
    const response = await previewVendorMetricMappingRuleHit(props.productId, {
      rawIdentifier: row.rawIdentifier,
      logicalChannelCode: row.logicalChannelCode ?? undefined
    })
    const nextState = { ...previewStateByRuleId.value }
    nextState[rowIdentity(row)] = response.data ?? {}
    previewStateByRuleId.value = nextState
  } catch (error) {
    showRequestErrorMessage(error, '映射规则试命中失败')
  } finally {
    submittingKey.value = ''
  }
}

function handleReplaySampleInput(row: VendorMetricMappingRuleLedgerRow, value: string) {
  const key = rowIdentity(row)
  replaySampleByRuleId.value = {
    ...replaySampleByRuleId.value,
    [key]: value
  }
}

async function handleReplay(row: VendorMetricMappingRuleLedgerRow) {
  if (!hasProductId(props.productId) || !row.rawIdentifier) {
    return
  }
  const key = rowIdentity(row)
  const replayKey = `replay-${key}`
  submittingKey.value = replayKey
  try {
    const sampleValue = replaySampleByRuleId.value[key]?.trim()
    const response = await replayVendorMetricMappingRule(props.productId, {
      rawIdentifier: row.rawIdentifier,
      logicalChannelCode: row.logicalChannelCode ?? undefined,
      sampleValue: sampleValue || undefined
    })
    replayStateByRuleId.value = {
      ...replayStateByRuleId.value,
      [key]: response.data ?? {}
    }
  } catch (error) {
    showRequestErrorMessage(error, '映射规则回放校验失败')
  } finally {
    submittingKey.value = ''
  }
}

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

async function handleBatchStatus(targetStatus: string) {
  if (!hasProductId(props.productId) || !selectedRuleIds.value.length) {
    return
  }
  const batchKey = `batch-status-${targetStatus.toLowerCase()}`
  submittingKey.value = batchKey
  try {
    const response = await batchUpdateVendorMetricMappingRuleStatus(props.productId, {
      ruleIds: selectedRuleIds.value,
      targetStatus
    })
    batchStatusSummary.value = response.data ?? null
    if (batchStatusSummary.value) {
      ElMessage.success(`映射规则批量状态切换完成：${batchStatusSummaryLabel(batchStatusSummary.value)}`)
    } else {
      ElMessage.success('映射规则批量状态切换完成')
    }
    await loadRows()
  } catch (error) {
    showRequestErrorMessage(error, '映射规则批量状态切换失败')
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitPublish(row: VendorMetricMappingRuleLedgerRow) {
  if (!hasProductId(props.productId) || row.ruleId == null) {
    return
  }
  const publishKey = `publish-${row.ruleId}`
  submittingKey.value = publishKey
  try {
    const response = await submitVendorMetricMappingRulePublish(
      props.productId,
      row.ruleId,
      `提交映射规则发布审批：${row.rawIdentifier || '--'} -> ${row.targetNormativeIdentifier || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `映射规则发布审批已提交，审批单 ${response.data.approvalOrderId}`
        : '映射规则发布审批已提交'
    )
    await loadRows()
  } catch (error) {
    showRequestErrorMessage(error, '映射规则发布审批提交失败')
  } finally {
    submittingKey.value = ''
  }
}

async function handleSubmitRollback(row: VendorMetricMappingRuleLedgerRow) {
  if (!hasProductId(props.productId) || row.ruleId == null) {
    return
  }
  const rollbackKey = `rollback-${row.ruleId}`
  submittingKey.value = rollbackKey
  try {
    const response = await submitVendorMetricMappingRuleRollback(
      props.productId,
      row.ruleId,
      `提交映射规则回滚审批：${row.rawIdentifier || '--'} -> ${row.targetNormativeIdentifier || '--'}`
    )
    ElMessage.success(
      response.data?.approvalOrderId != null
        ? `映射规则回滚审批已提交，审批单 ${response.data.approvalOrderId}`
        : '映射规则回滚审批已提交'
    )
    await loadRows()
  } catch (error) {
    showRequestErrorMessage(error, '映射规则回滚审批提交失败')
  } finally {
    submittingKey.value = ''
  }
}
</script>

<style scoped>
.product-vendor-rule-ledger,
.product-vendor-rule-ledger__summary,
.product-vendor-rule-ledger__batch,
.product-vendor-rule-ledger__list,
.product-vendor-rule-ledger__item {
  display: grid;
}

.product-vendor-rule-ledger {
  gap: 0.72rem;
}

.product-vendor-rule-ledger__summary {
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.72rem;
}

.product-vendor-rule-ledger__summary-card,
.product-vendor-rule-ledger__item,
.product-vendor-rule-ledger__batch,
.product-vendor-rule-ledger__empty {
  padding: 0.82rem 0.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: 0.72rem;
  background: white;
}

.product-vendor-rule-ledger__summary-card {
  display: grid;
  gap: 0.24rem;
}

.product-vendor-rule-ledger__batch {
  gap: 0.68rem;
}

.product-vendor-rule-ledger__batch-select-all,
.product-vendor-rule-ledger__item-selector {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.product-vendor-rule-ledger__batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.product-vendor-rule-ledger__summary-card span,
.product-vendor-rule-ledger__meta span,
.product-vendor-rule-ledger__title span,
.product-vendor-rule-ledger__approval,
.product-vendor-rule-ledger__preview span,
.product-vendor-rule-ledger__hint,
.product-vendor-rule-ledger__empty p {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.56;
}

.product-vendor-rule-ledger__summary-card strong,
.product-vendor-rule-ledger__title strong,
.product-vendor-rule-ledger__preview strong,
.product-vendor-rule-ledger__empty strong {
  color: var(--text-heading);
}

.product-vendor-rule-ledger__list {
  gap: 0.72rem;
}

.product-vendor-rule-ledger__item {
  gap: 0.68rem;
}

.product-vendor-rule-ledger__headline,
.product-vendor-rule-ledger__actions {
  display: flex;
  gap: 0.72rem;
  align-items: flex-start;
}

.product-vendor-rule-ledger__headline {
  justify-content: space-between;
}

.product-vendor-rule-ledger__title {
  display: grid;
  gap: 0.18rem;
  flex: 1;
}

.product-vendor-rule-ledger__meta,
.product-vendor-rule-ledger__preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.product-vendor-rule-ledger__actions {
  flex-wrap: wrap;
  justify-content: space-between;
}

.product-vendor-rule-ledger__replay {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.product-vendor-rule-ledger__replay-input {
  width: min(20rem, 100%);
  min-height: 2rem;
  border-radius: 0.5rem;
  border: 1px solid var(--panel-border);
  padding: 0.25rem 0.6rem;
  color: var(--text-heading);
}

.product-vendor-rule-ledger__preview {
  padding-top: 0.08rem;
  border-top: 1px dashed color-mix(in srgb, var(--brand) 14%, var(--panel-border));
}

.product-vendor-rule-ledger__empty {
  display: grid;
  gap: 0.24rem;
}

@media (max-width: 720px) {
  .product-vendor-rule-ledger__headline,
  .product-vendor-rule-ledger__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
