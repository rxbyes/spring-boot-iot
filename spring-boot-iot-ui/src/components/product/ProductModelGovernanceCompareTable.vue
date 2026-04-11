<template>
  <section class="product-model-governance-compare-table">
    <div class="product-model-governance-compare-table__tabs" role="tablist" aria-label="待处理字段分组">
      <button
        v-for="group in groupOptions"
        :key="group.key"
        :data-testid="`governance-group-${group.key}`"
        type="button"
        class="product-model-governance-compare-table__tab"
        :class="{ 'product-model-governance-compare-table__tab--active': activeGroup === group.key }"
        @click="activeGroup = group.key"
      >
        <span>{{ group.label }}</span>
        <strong>{{ rowsByGroup(group.key).length }}</strong>
      </button>
    </div>

    <div class="product-model-governance-compare-table__type-tabs" role="tablist" aria-label="字段类型筛选">
      <button
        v-for="option in typeOptions"
        :key="option.value"
        :data-testid="`governance-type-${option.value}`"
        type="button"
        class="product-model-governance-compare-table__tab"
        :class="{ 'product-model-governance-compare-table__tab--active': activeType === option.value }"
        @click="activeType = option.value"
      >
        <span>{{ option.label }}</span>
        <strong>{{ rowsByType(option.value).length }}</strong>
      </button>
    </div>

    <div v-if="activeRows.length" class="product-model-governance-compare-table__list">
      <article
        v-for="row in activeRows"
        :key="rowKey(row)"
        class="product-model-governance-compare-table__row"
        :data-testid="`governance-row-${rowKey(row)}`"
      >
        <header class="product-model-governance-compare-table__row-header">
          <div class="product-model-governance-compare-table__row-heading">
            <span class="product-model-governance-compare-table__row-kicker">治理候选快照</span>
            <strong>{{ rowDisplayName(row) }}</strong>
            <div class="product-model-governance-compare-table__row-meta">
              <span>identifier: {{ row.identifier }}</span>
              <span>类型: {{ rowTypeLabel(row) }}</span>
              <span>{{ rowDataHint(row) }}</span>
              <span v-if="rowNormativeLabel(row)">规范字段：{{ rowNormativeLabel(row) }}</span>
              <span v-if="row.rawIdentifiers?.length">原始字段：{{ row.rawIdentifiers.join(' / ') }}</span>
            </div>
          </div>
        </header>

        <div class="product-model-governance-compare-table__evidence-summary">
          <div class="product-model-governance-compare-table__sample-card">
            <span>样例值</span>
            <strong>{{ rowSampleValue(row) }}</strong>
          </div>
          <div class="product-model-governance-compare-table__baseline-card">
            <span>正式字段：</span>
            <strong>{{ formalBaselineLabel(row) }}</strong>
          </div>
          <div class="product-model-governance-compare-table__source-chips">
            <span
              v-for="chip in sourceChips(row)"
              :key="chip"
              class="product-model-governance-compare-table__source-chip"
            >
              {{ chip }}
            </span>
          </div>
        </div>

        <p v-if="rowStatusSummary(row)" class="product-model-governance-compare-table__row-reason">
          {{ `当前建议：${rowStatusSummary(row)}` }}
        </p>

        <div v-if="templateSummaryParts(row).length" class="product-model-governance-compare-table__template-summary">
          <small
            v-for="part in templateSummaryParts(row)"
            :key="part"
          >
            {{ part }}
          </small>
        </div>

        <div v-if="row.riskReady || visibleRiskFlags(row).length" class="product-model-governance-compare-table__risk-flags">
          <span
            v-if="row.riskReady"
            class="product-model-governance-compare-table__risk-flag"
          >
            可进入风险闭环
          </span>
          <span
            v-for="flag in visibleRiskFlags(row)"
            :key="flag"
            class="product-model-governance-compare-table__risk-flag"
          >
            {{ riskLabel(flag) }}
          </span>
        </div>

        <div class="product-model-governance-compare-table__decisions">
          <button
            v-for="decision in availableDecisions(row)"
            :key="decision.value"
            :data-testid="`governance-decision-${rowKey(row)}-${decision.value}`"
            type="button"
            class="product-model-governance-compare-table__decision-button"
            :class="{ 'product-model-governance-compare-table__decision-button--active': currentDecision(row) === decision.value }"
            @click="emitDecision(row, decision.value)"
          >
            {{ decision.label }}
          </button>
        </div>
      </article>
    </div>

    <div v-else class="product-model-governance-compare-table__empty">
      当前类型下暂无对比结果。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type {
  ProductModelGovernanceCompareRow,
  ProductModelGovernanceDecision,
  ProductModelGovernanceEvidence,
  ProductModelType
} from '@/types/api'

type GovernanceDecisionUi = ProductModelGovernanceDecision | 'observe' | 'review' | 'ignore'
type GovernanceGroupKey = 'direct' | 'review' | 'observe' | 'conflict'
type GovernanceTypeFilter = 'all' | ProductModelType

const props = withDefaults(defineProps<{
  rows: ProductModelGovernanceCompareRow[]
  decisionState?: Record<string, GovernanceDecisionUi>
}>(), {
  decisionState: () => ({})
})

const emit = defineEmits<{
  (event: 'change-decision', payload: { key: string; decision: GovernanceDecisionUi }): void
}>()

const groupOptions: Array<{ label: string; key: GovernanceGroupKey }> = [
  { label: '可直接生效', key: 'direct' },
  { label: '待确认', key: 'review' },
  { label: '继续观察', key: 'observe' },
  { label: '存在差异', key: 'conflict' }
]

const typeOptions: Array<{ label: string; value: GovernanceTypeFilter }> = [
  { label: '全部', value: 'all' },
  { label: '属性', value: 'property' },
  { label: '事件', value: 'event' },
  { label: '服务', value: 'service' }
]

const activeGroup = ref<GovernanceGroupKey>('direct')
const activeType = ref<GovernanceTypeFilter>('all')

const activeRows = computed(() =>
  rowsByGroup(activeGroup.value).filter((row) => activeType.value === 'all' || row.modelType === activeType.value)
)

watch(
  () => props.rows,
  () => {
    if (!rowsByGroup(activeGroup.value).length) {
      activeGroup.value = firstAvailableGroup()
    }
  },
  { immediate: true, deep: true }
)

function rowsByType(type: GovernanceTypeFilter) {
  if (type === 'all') {
    return props.rows
  }
  return props.rows.filter((row) => row.modelType === type)
}

function rowKey(row: ProductModelGovernanceCompareRow) {
  return `${row.modelType}:${row.identifier}`
}

function rowsByGroup(group: GovernanceGroupKey) {
  return props.rows.filter((row) => resolveGroup(row) === group)
}

function riskLabel(flag: string) {
  return {
    definition_mismatch: '与现有字段存在差异',
    needs_review: '需要确认',
    formal_baseline: '正式契约已存在',
    manual_missing: '等待更多上报样本',
    runtime_missing: '等待运行数据补充',
    suspected_match: '存在疑似同义项'
  }[flag] ?? flag
}

function rowDisplayName(row: ProductModelGovernanceCompareRow) {
  return row.manualCandidate?.modelName
    || row.runtimeCandidate?.modelName
    || row.formalModel?.modelName
    || row.identifier
}

function rowTypeLabel(row: ProductModelGovernanceCompareRow) {
  return {
    property: '属性',
    event: '事件',
    service: '服务'
  }[row.modelType] ?? row.modelType
}

function rowNormativeLabel(row: ProductModelGovernanceCompareRow) {
  if (row.normativeName && row.normativeIdentifier && row.normativeName !== row.normativeIdentifier) {
    return `${row.normativeName} (${row.normativeIdentifier})`
  }
  return row.normativeName || row.normativeIdentifier || ''
}

function rowDataHint(row: ProductModelGovernanceCompareRow) {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  const dataHint = source?.dataType || source?.eventType || (source ? formatServiceSummary(source) : '')
  return dataHint ? `数据类型: ${dataHint}` : '数据类型: 待确认'
}

function rowSampleValue(row: ProductModelGovernanceCompareRow) {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  if (!source) {
    return '待上报后显示'
  }
  const protocolSummary = source.protocolTemplateEvidence?.logicalChannelCodes?.[0]
  if (protocolSummary) {
    return protocolSummary
  }
  return source.unit || source.monitorTypeCode || '待上报后显示'
}

function sourceChips(row: ProductModelGovernanceCompareRow) {
  return Array.from(new Set([
    sourceChipLabel(row.manualCandidate),
    sourceChipLabel(row.runtimeCandidate),
    row.formalModel ? '正式模型已存在' : null
  ].filter((item): item is string => Boolean(item))))
}

function sourceChipLabel(evidence?: ProductModelGovernanceEvidence | null) {
  if (!evidence) {
    return null
  }
  if (evidence.protocolTemplateEvidence) {
    return '来自父设备归一'
  }
  if (evidence.evidenceOrigin === 'sample_json' || evidence.sourceTables?.includes('manual_sample')) {
    return '来自上报样本'
  }
  if (evidence.evidenceOrigin === 'manual_draft' || evidence.sourceTables?.includes('manual_draft')) {
    return '来自手工补充'
  }
  if (evidence.evidenceOrigin === 'formal' || evidence.sourceTables?.includes('iot_product_model')) {
    return '正式模型已存在'
  }
  if (evidence.evidenceOrigin === 'normative') {
    return '来自规范预设'
  }
  if (evidence.evidenceOrigin === 'runtime' || evidence.sourceTables?.some((table) => table.startsWith('iot_'))) {
    return '来自运行数据'
  }
  return '来自识别结果'
}

function formatServiceSummary(evidence: ProductModelGovernanceEvidence) {
  const hasInput = Boolean(evidence.serviceInputJson?.trim())
  const hasOutput = Boolean(evidence.serviceOutputJson?.trim())
  if (hasInput && hasOutput) return '输入/输出已定义'
  if (hasInput) return '仅定义输入'
  if (hasOutput) return '仅定义输出'
  return ''
}

function resolveGroup(row: ProductModelGovernanceCompareRow): GovernanceGroupKey {
  if (row.compareStatus === 'suspected_conflict') {
    return 'conflict'
  }
  if (row.compareStatus === 'formal_exists') {
    return 'review'
  }
  if (
    row.compareStatus === 'manual_only'
    || row.compareStatus === 'runtime_only'
    || row.compareStatus === 'evidence_insufficient'
    || row.riskFlags?.includes('manual_missing')
    || row.riskFlags?.includes('runtime_missing')
    || row.riskFlags?.includes('runtime_low_evidence')
  ) {
    return 'observe'
  }
  return 'direct'
}

function firstAvailableGroup() {
  return groupOptions.find((group) => rowsByGroup(group.key).length > 0)?.key ?? 'direct'
}

function rowStatusSummary(row: ProductModelGovernanceCompareRow) {
  if (row.compareStatus === 'suspected_conflict' || row.riskFlags?.includes('definition_mismatch')) {
    return '与现有字段有差异，请确认后再生效'
  }
  if (row.compareStatus === 'formal_exists' || row.riskFlags?.includes('formal_baseline')) {
    return '正式模型已存在，可按需纳入修订'
  }
  if (
    row.compareStatus === 'manual_only'
    || row.compareStatus === 'runtime_only'
    || row.compareStatus === 'evidence_insufficient'
    || row.riskFlags?.includes('manual_missing')
    || row.riskFlags?.includes('runtime_missing')
    || row.riskFlags?.includes('runtime_low_evidence')
  ) {
    return '证据还不够，先继续观察'
  }
  return '当前字段可直接确认生效'
}

function formalBaselineLabel(row: ProductModelGovernanceCompareRow) {
  return row.formalModel?.modelId ? '已存在' : '暂无'
}

function templateSummaryParts(row: ProductModelGovernanceCompareRow) {
  const protocolTemplateEvidence = row.runtimeCandidate?.protocolTemplateEvidence
  if (!protocolTemplateEvidence) {
    return []
  }
  return [
    protocolTemplateEvidence.templateCodes?.length
      ? friendlyTemplateName(protocolTemplateEvidence.templateCodes[0])
      : null,
    protocolTemplateEvidence.childDeviceCodes?.length
      ? protocolTemplateEvidence.childDeviceCodes.join(' / ')
      : null,
    protocolTemplateEvidence.canonicalizationStrategies?.length
      ? protocolTemplateEvidence.canonicalizationStrategies.join(' / ')
      : null
  ].filter((item): item is string => Boolean(item))
}

function visibleRiskFlags(row: ProductModelGovernanceCompareRow) {
  return (row.riskFlags ?? []).filter((flag) => flag !== 'formal_baseline')
}

function availableDecisions(row: ProductModelGovernanceCompareRow) {
  switch (row.compareStatus) {
    case 'double_aligned':
      return [
        { label: '纳入新增', value: 'create' },
        { label: '继续观察', value: 'observe' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
    case 'manual_only':
    case 'runtime_only':
      return [
        { label: '纳入新增', value: 'create' },
        { label: '继续观察', value: 'observe' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
    case 'formal_exists':
      return [
        { label: '纳入修订', value: 'update' },
        { label: '继续观察', value: 'observe' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
    case 'suspected_conflict':
      return [
        { label: '待确认', value: 'review' },
        { label: row.formalModel?.modelId ? '纳入修订' : '纳入新增', value: row.formalModel?.modelId ? 'update' : 'create' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
    case 'evidence_insufficient':
      return [
        { label: '继续观察', value: 'observe' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
    default:
      return [
        { label: row.formalModel?.modelId ? '纳入修订' : '纳入新增', value: row.formalModel?.modelId ? 'update' : 'create' },
        { label: '继续观察', value: 'observe' },
        { label: '忽略', value: 'ignore' }
      ] satisfies Array<{ label: string; value: GovernanceDecisionUi }>
  }
}

function currentDecision(row: ProductModelGovernanceCompareRow) {
  return props.decisionState[rowKey(row)]
}

function emitDecision(row: ProductModelGovernanceCompareRow, decision: GovernanceDecisionUi) {
  emit('change-decision', { key: rowKey(row), decision })
}

function friendlyTemplateName(templateCode?: string | null) {
  switch (templateCode) {
    case 'crack_child_template':
      return '裂缝模板'
    case 'deep_displacement_child_template':
      return '深部位移模板'
    default:
      return templateCode || '模板'
  }
}
</script>

<style scoped>
.product-model-governance-compare-table,
.product-model-governance-compare-table__tabs,
.product-model-governance-compare-table__type-tabs,
.product-model-governance-compare-table__list,
.product-model-governance-compare-table__row,
.product-model-governance-compare-table__row-heading,
.product-model-governance-compare-table__row-meta,
.product-model-governance-compare-table__evidence-summary,
.product-model-governance-compare-table__sample-card,
.product-model-governance-compare-table__baseline-card,
.product-model-governance-compare-table__source-chips,
.product-model-governance-compare-table__template-summary {
  display: grid;
}

.product-model-governance-compare-table {
  gap: 0.88rem;
}

.product-model-governance-compare-table__tabs {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.56rem;
}

.product-model-governance-compare-table__type-tabs {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.56rem;
}

.product-model-governance-compare-table__tab,
.product-model-governance-compare-table__decision-button {
  border: 1px solid var(--panel-border);
  border-radius: 0.92rem;
  background: #fff;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.product-model-governance-compare-table__tab {
  display: grid;
  gap: 0.22rem;
  justify-items: start;
  padding: 0.72rem 0.84rem;
  text-align: left;
}

.product-model-governance-compare-table__tab strong,
.product-model-governance-compare-table__row-heading strong,
.product-model-governance-compare-table__sample-card strong,
.product-model-governance-compare-table__baseline-card strong {
  color: var(--text-heading);
}

.product-model-governance-compare-table__tab--active,
.product-model-governance-compare-table__decision-button--active {
  border-color: color-mix(in srgb, var(--brand) 52%, #fff);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.12);
  color: var(--brand);
  transform: translateY(-1px);
}

.product-model-governance-compare-table__list {
  gap: 0.72rem;
}

.product-model-governance-compare-table__row {
  gap: 0.72rem;
  padding: 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 253, 0.98));
}

.product-model-governance-compare-table__row-header,
.product-model-governance-compare-table__risk-flags,
.product-model-governance-compare-table__decisions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.72rem;
  align-items: center;
  justify-content: space-between;
}

.product-model-governance-compare-table__row-heading,
.product-model-governance-compare-table__row-meta,
.product-model-governance-compare-table__sample-card,
.product-model-governance-compare-table__baseline-card,
.product-model-governance-compare-table__source-chips,
.product-model-governance-compare-table__template-summary {
  gap: 0.22rem;
}

.product-model-governance-compare-table__row-reason {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.product-model-governance-compare-table__row-heading span,
.product-model-governance-compare-table__risk-flag,
.product-model-governance-compare-table__source-chip,
.product-model-governance-compare-table__sample-card span,
.product-model-governance-compare-table__baseline-card span,
.product-model-governance-compare-table__template-summary small {
  color: var(--text-caption);
  font-size: 0.82rem;
}

.product-model-governance-compare-table__row-kicker {
  color: var(--brand);
  font-weight: 600;
}

.product-model-governance-compare-table__row-meta {
  grid-template-columns: repeat(auto-fit, minmax(12rem, max-content));
  gap: 0.36rem 0.72rem;
}

.product-model-governance-compare-table__evidence-summary {
  grid-template-columns: minmax(0, 11rem) minmax(0, 11rem) minmax(0, 1fr);
  gap: 0.64rem;
  align-items: start;
}

.product-model-governance-compare-table__sample-card,
.product-model-governance-compare-table__baseline-card,
.product-model-governance-compare-table__source-chips {
  padding: 0.74rem 0.84rem;
  border-radius: 0.88rem;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid color-mix(in srgb, var(--panel-border) 78%, #fff);
}

.product-model-governance-compare-table__baseline-card {
  align-content: center;
}

.product-model-governance-compare-table__source-chips {
  grid-template-columns: repeat(auto-fit, minmax(8rem, max-content));
  gap: 0.42rem;
}

.product-model-governance-compare-table__source-chip {
  display: inline-flex;
  align-items: center;
  min-height: 1.78rem;
  padding: 0.2rem 0.64rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand-light) 38%, #fff);
  color: var(--brand);
  font-weight: 600;
}

.product-model-governance-compare-table__template-summary {
  grid-template-columns: repeat(auto-fit, minmax(6rem, max-content));
  gap: 0.42rem;
}

.product-model-governance-compare-table__template-summary small {
  display: inline-flex;
  align-items: center;
  min-height: 1.72rem;
  padding: 0.18rem 0.62rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
  color: var(--text-secondary);
}

.product-model-governance-compare-table__risk-flag {
  display: inline-flex;
  align-items: center;
  min-height: 1.76rem;
  padding: 0.18rem 0.62rem;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.1);
  color: #c2410c;
}

.product-model-governance-compare-table__decisions {
  justify-content: flex-start;
}

.product-model-governance-compare-table__decision-button {
  padding: 0.42rem 0.82rem;
}

.product-model-governance-compare-table__empty {
  padding: 1.4rem 1rem;
  border-radius: 1rem;
  border: 1px dashed var(--panel-border);
  color: var(--text-secondary);
  text-align: center;
}

@media (max-width: 960px) {
  .product-model-governance-compare-table__tabs,
  .product-model-governance-compare-table__type-tabs,
  .product-model-governance-compare-table__evidence-summary {
    grid-template-columns: 1fr;
  }
}
</style>
