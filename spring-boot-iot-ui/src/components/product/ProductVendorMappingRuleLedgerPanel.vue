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

    <div v-else-if="rows.length" class="product-vendor-rule-ledger__list">
      <article
        v-for="row in rows"
        :key="String(row.ruleId ?? `${row.rawIdentifier || '--'}-${row.targetNormativeIdentifier || '--'}`)"
        class="product-vendor-rule-ledger__item"
      >
        <div class="product-vendor-rule-ledger__headline">
          <div class="product-vendor-rule-ledger__title">
            <strong>{{ `${row.rawIdentifier || '--'} -> ${row.targetNormativeIdentifier || '--'}` }}</strong>
            <span>{{ `${scopeTypeLabel(row.scopeType)} · ${versionLabel(row)}` }}</span>
            <span>{{ `${row.draftStatus || '--'} · ${row.publishedStatus || '未发布'}` }}</span>
          </div>
          <span v-if="row.latestApprovalOrderId != null" class="product-vendor-rule-ledger__approval">
            {{ `审批单 ${row.latestApprovalOrderId}` }}
          </span>
        </div>

        <div class="product-vendor-rule-ledger__meta">
          <span>{{ `命中来源 ${row.publishedSource || '--'}` }}</span>
          <span>{{ `逻辑通道 ${row.logicalChannelCode || '--'}` }}</span>
        </div>

        <div class="product-vendor-rule-ledger__actions">
          <StandardButton
            :data-testid="`rule-ledger-preview-hit-${row.ruleId}`"
            :disabled="!row.rawIdentifier || isSubmitting(`preview-${row.ruleId}`)"
            @click="handlePreview(row)"
          >
            {{ isSubmitting(`preview-${row.ruleId}`) ? '试命中中...' : '试命中' }}
          </StandardButton>
          <StandardButton
            :data-testid="`rule-ledger-submit-publish-${row.ruleId}`"
            :disabled="!row.ruleId || isSubmitting(`publish-${row.ruleId}`)"
            @click="handleSubmitPublish(row)"
          >
            {{ isSubmitting(`publish-${row.ruleId}`) ? '提交中...' : '提交发布审批' }}
          </StandardButton>
          <StandardButton
            :data-testid="`rule-ledger-submit-rollback-${row.ruleId}`"
            :disabled="!canRollback(row) || isSubmitting(`rollback-${row.ruleId}`)"
            @click="handleSubmitRollback(row)"
          >
            {{ isSubmitting(`rollback-${row.ruleId}`) ? '提交中...' : '提交回滚审批' }}
          </StandardButton>
        </div>

        <div
          v-if="previewStateByRuleId[String(row.ruleId ?? '')]"
          :data-testid="`rule-ledger-preview-result-${row.ruleId}`"
          class="product-vendor-rule-ledger__preview"
        >
          <strong>{{ previewMatchedLabel(previewStateByRuleId[String(row.ruleId ?? '')]) }}</strong>
          <span>{{ previewSourceLabel(previewStateByRuleId[String(row.ruleId ?? '')]) }}</span>
        </div>
      </article>
    </div>

    <div v-else class="product-vendor-rule-ledger__empty">
      <strong>当前还没有映射规则台账</strong>
      <p>先在映射规则建议中采纳草稿，或等待运行态证据继续补齐候选。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import {
  listVendorMetricMappingRuleLedger,
  previewVendorMetricMappingRuleHit,
  submitVendorMetricMappingRulePublish,
  submitVendorMetricMappingRuleRollback
} from '@/api/vendorMetricMappingRule'
import { resolveRequestErrorMessage } from '@/api/request'
import StandardButton from '@/components/StandardButton.vue'
import { ElMessage } from '@/utils/message'
import type { IdType, VendorMetricMappingRuleHitPreview, VendorMetricMappingRuleLedgerRow } from '@/types/api'

const props = defineProps<{
  productId?: IdType | null
}>()

const rows = ref<VendorMetricMappingRuleLedgerRow[]>([])
const loading = ref(false)
const errorMessage = ref('')
const submittingKey = ref('')
const previewStateByRuleId = ref<Record<string, VendorMetricMappingRuleHitPreview>>({})

const publishedCount = computed(() => rows.value.filter((row) => row.publishedStatus === 'PUBLISHED').length)
const draftCount = computed(() => rows.value.filter((row) => row.draftStatus === 'DRAFT').length)

watch(
  () => props.productId,
  () => {
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

function isSubmitting(key: string) {
  return submittingKey.value === key
}

function canRollback(row: VendorMetricMappingRuleLedgerRow) {
  return Boolean(row.ruleId) && row.publishedStatus === 'PUBLISHED'
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

async function loadRows() {
  if (!hasProductId(props.productId)) {
    rows.value = []
    previewStateByRuleId.value = {}
    return
  }
  loading.value = true
  errorMessage.value = ''
  previewStateByRuleId.value = {}
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
  const previewKey = `preview-${row.ruleId}`
  submittingKey.value = previewKey
  try {
    const response = await previewVendorMetricMappingRuleHit(props.productId, {
      rawIdentifier: row.rawIdentifier,
      logicalChannelCode: row.logicalChannelCode ?? undefined
    })
    const nextState = { ...previewStateByRuleId.value }
    nextState[String(row.ruleId ?? row.rawIdentifier)] = response.data ?? {}
    previewStateByRuleId.value = nextState
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '映射规则试命中失败'))
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
    ElMessage.error(resolveRequestErrorMessage(error, '映射规则发布审批提交失败'))
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
    ElMessage.error(resolveRequestErrorMessage(error, '映射规则回滚审批提交失败'))
  } finally {
    submittingKey.value = ''
  }
}
</script>

<style scoped>
.product-vendor-rule-ledger,
.product-vendor-rule-ledger__summary,
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
  justify-content: space-between;
  gap: 0.72rem;
  align-items: flex-start;
}

.product-vendor-rule-ledger__title {
  display: grid;
  gap: 0.18rem;
}

.product-vendor-rule-ledger__meta,
.product-vendor-rule-ledger__preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.product-vendor-rule-ledger__actions {
  flex-wrap: wrap;
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
