<template>
  <section class="product-model-governance-compare-table">
    <div class="product-model-governance-compare-table__tabs" role="tablist" aria-label="物模型对比类型">
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
            <strong>{{ row.identifier }}</strong>
            <span>{{ statusLabel(row.compareStatus) }}</span>
          </div>
          <div class="product-model-governance-compare-table__row-side">
            <div class="product-model-governance-compare-table__action-copy">
              <span>建议动作</span>
              <strong>{{ row.suggestedAction || '继续观察' }}</strong>
            </div>
            <div class="product-model-governance-compare-table__action-copy">
              <span>当前决策</span>
              <strong>{{ currentDecisionLabel(row) }}</strong>
            </div>
          </div>
        </header>

        <div class="product-model-governance-compare-table__evidence-grid">
          <section class="product-model-governance-compare-table__evidence-card">
            <span>规范证据</span>
            <strong>{{ evidenceTitle(row.manualCandidate) }}</strong>
            <p>{{ evidenceSummary(row.manualCandidate) }}</p>
            <div v-if="evidenceNotes(row.manualCandidate).length" class="product-model-governance-compare-table__evidence-notes">
              <small
                v-for="note in evidenceNotes(row.manualCandidate)"
                :key="note"
              >
                {{ note }}
              </small>
            </div>
          </section>
          <section class="product-model-governance-compare-table__evidence-card">
            <span>报文证据</span>
            <strong>{{ evidenceTitle(row.runtimeCandidate) }}</strong>
            <p>{{ evidenceSummary(row.runtimeCandidate) }}</p>
            <div v-if="evidenceNotes(row.runtimeCandidate).length" class="product-model-governance-compare-table__evidence-notes">
              <small
                v-for="note in evidenceNotes(row.runtimeCandidate)"
                :key="note"
              >
                {{ note }}
              </small>
            </div>
          </section>
          <section class="product-model-governance-compare-table__evidence-card">
            <span>正式模型</span>
            <strong>{{ evidenceTitle(row.formalModel) }}</strong>
            <p>{{ evidenceSummary(row.formalModel) }}</p>
            <div v-if="evidenceNotes(row.formalModel).length" class="product-model-governance-compare-table__evidence-notes">
              <small
                v-for="note in evidenceNotes(row.formalModel)"
                :key="note"
              >
                {{ note }}
              </small>
            </div>
          </section>
        </div>

        <div v-if="row.riskFlags?.length" class="product-model-governance-compare-table__risk-flags">
          <span
            v-for="flag in row.riskFlags"
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
import { computed, ref } from 'vue'

import type {
  ProductModelGovernanceCompareRow,
  ProductModelGovernanceDecision,
  ProductModelGovernanceEvidence,
  ProductModelGovernanceCompareStatus,
  ProductModelType
} from '@/types/api'

type GovernanceDecisionUi = ProductModelGovernanceDecision | 'observe' | 'review' | 'ignore'

const props = withDefaults(defineProps<{
  rows: ProductModelGovernanceCompareRow[]
  decisionState?: Record<string, GovernanceDecisionUi>
}>(), {
  decisionState: () => ({})
})

const emit = defineEmits<{
  (event: 'change-decision', payload: { key: string; decision: GovernanceDecisionUi }): void
}>()

const typeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性', value: 'property' },
  { label: '事件', value: 'event' },
  { label: '服务', value: 'service' }
]

const activeType = ref<ProductModelType>('property')

const activeRows = computed(() => rowsByType(activeType.value))

function rowsByType(type: ProductModelType) {
  return props.rows.filter((row) => row.modelType === type)
}

function rowKey(row: ProductModelGovernanceCompareRow) {
  return `${row.modelType}:${row.identifier}`
}

function statusLabel(status: ProductModelGovernanceCompareStatus) {
  return {
    double_aligned: '双证据一致',
    manual_only: '仅手动命中',
    runtime_only: '自动证据独有',
    formal_exists: '正式模型已存在',
    suspected_conflict: '疑似冲突',
    evidence_insufficient: '证据不足'
  }[status] ?? status
}

function riskLabel(flag: string) {
  return {
    definition_mismatch: '定义不一致',
    needs_review: '需人工核对',
    formal_baseline: '正式基线已存在',
    manual_missing: '缺少手动证据',
    runtime_missing: '缺少自动证据',
    suspected_match: '存在疑似同义项'
  }[flag] ?? flag
}

function evidenceTitle(evidence?: ProductModelGovernanceEvidence | null) {
  return evidence?.modelName || '暂无'
}

function evidenceSummary(evidence?: ProductModelGovernanceEvidence | null) {
  if (!evidence) {
    return '当前侧暂无可用证据。'
  }
  const typePart = evidence.dataType || evidence.eventType || formatServiceSummary(evidence)
  const meta = [
    evidence.unit,
    evidence.monitorTypeCode,
    evidence.sourceTables?.length ? evidence.sourceTables.join(' / ') : null
  ].filter(Boolean)
  return [typePart, ...meta].filter(Boolean).join(' · ') || '当前侧已识别到证据。'
}

function evidenceNotes(evidence?: ProductModelGovernanceEvidence | null) {
  if (!evidence) {
    return []
  }
  return [
    evidence.normativeSource,
    evidence.rawIdentifiers?.length ? evidence.rawIdentifiers.join(' / ') : null
  ].filter((item): item is string => Boolean(item))
}

function formatServiceSummary(evidence: ProductModelGovernanceEvidence) {
  const hasInput = Boolean(evidence.serviceInputJson?.trim())
  const hasOutput = Boolean(evidence.serviceOutputJson?.trim())
  if (hasInput && hasOutput) return '输入/输出已定义'
  if (hasInput) return '仅定义输入'
  if (hasOutput) return '仅定义输出'
  return ''
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
        { label: '人工裁决', value: 'review' },
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

function currentDecisionLabel(row: ProductModelGovernanceCompareRow) {
  const decision = currentDecision(row)
  if (!decision) {
    return '未选择'
  }
  return {
    create: '纳入新增',
    update: '纳入修订',
    observe: '继续观察',
    review: '人工裁决',
    ignore: '忽略'
  }[decision] ?? decision
}

function emitDecision(row: ProductModelGovernanceCompareRow, decision: GovernanceDecisionUi) {
  emit('change-decision', { key: rowKey(row), decision })
}
</script>

<style scoped>
.product-model-governance-compare-table,
.product-model-governance-compare-table__tabs,
.product-model-governance-compare-table__list,
.product-model-governance-compare-table__row,
.product-model-governance-compare-table__evidence-grid {
  display: grid;
}

.product-model-governance-compare-table {
  gap: 1rem;
}

.product-model-governance-compare-table__tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.product-model-governance-compare-table__tab,
.product-model-governance-compare-table__decision-button {
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: #fff;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.product-model-governance-compare-table__tab {
  display: grid;
  gap: 0.22rem;
  justify-items: start;
  padding: 0.9rem 1rem;
  text-align: left;
}

.product-model-governance-compare-table__tab strong,
.product-model-governance-compare-table__row-heading strong,
.product-model-governance-compare-table__action-copy strong,
.product-model-governance-compare-table__evidence-card strong {
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
  gap: 0.88rem;
}

.product-model-governance-compare-table__row {
  gap: 0.88rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 1.1rem;
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

.product-model-governance-compare-table__row-side,
.product-model-governance-compare-table__row-heading,
.product-model-governance-compare-table__action-copy,
.product-model-governance-compare-table__evidence-card {
  display: grid;
  gap: 0.22rem;
}

.product-model-governance-compare-table__row-side {
  justify-items: end;
}

.product-model-governance-compare-table__row-heading span,
.product-model-governance-compare-table__action-copy span,
.product-model-governance-compare-table__evidence-card span,
.product-model-governance-compare-table__risk-flag {
  color: var(--text-caption);
  font-size: 0.82rem;
}

.product-model-governance-compare-table__evidence-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.product-model-governance-compare-table__evidence-card {
  min-height: 7rem;
  padding: 0.9rem 1rem;
  border-radius: 0.96rem;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid color-mix(in srgb, var(--panel-border) 78%, #fff);
}

.product-model-governance-compare-table__evidence-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.product-model-governance-compare-table__evidence-notes {
  display: grid;
  gap: 0.18rem;
}

.product-model-governance-compare-table__evidence-notes small {
  color: var(--text-caption);
  line-height: 1.5;
}

.product-model-governance-compare-table__risk-flag {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.1);
  color: #c2410c;
}

.product-model-governance-compare-table__decisions {
  justify-content: flex-start;
}

.product-model-governance-compare-table__decision-button {
  padding: 0.5rem 0.9rem;
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
  .product-model-governance-compare-table__evidence-grid {
    grid-template-columns: 1fr;
  }

  .product-model-governance-compare-table__row-side {
    justify-items: start;
  }
}
</style>
