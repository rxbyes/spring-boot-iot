<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-model-designer-drawer"
    size="68rem"
    title="物模型双证据治理"
    subtitle="围绕当前产品执行手动提炼、自动提炼对比与正式应用，不新增并行草稿入口。"
    :loading="loading && !hasLoadedContent"
    loading-text="正在加载产品物模型..."
    :error-message="errorMessage"
    :empty="!product"
    empty-text="请先选择产品，再进入完整治理。"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section v-if="product" class="detail-panel product-model-designer-drawer__header">
      <div class="product-model-designer-drawer__identity">
        <p class="product-model-designer-drawer__kicker">双证据治理</p>
        <h3 class="product-model-designer-drawer__headline">{{ product.productName || product.productKey || '--' }}</h3>
        <div class="product-model-designer-drawer__meta">
          <span>{{ product.productKey || '--' }}</span>
          <span>{{ product.protocolCode || '--' }}</span>
          <span>{{ product.nodeType === 2 ? '网关设备' : '直连设备' }}</span>
        </div>
      </div>

      <div class="product-model-designer-drawer__summary">
        <div class="product-model-designer-drawer__summary-item">
          <span>正式模型</span>
          <strong>{{ models.length }}</strong>
        </div>
        <div class="product-model-designer-drawer__summary-item">
          <span>对比结果</span>
          <strong>{{ compareRows.length }}</strong>
        </div>
        <div class="product-model-designer-drawer__summary-item">
          <span>待应用项</span>
          <strong>{{ selectedApplyItems.length }}</strong>
        </div>
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__evidence-stage">
      <div class="detail-section-header">
        <div>
          <h3>证据入口</h3>
          <p>手动样本与人工补录用于表达业务理解，运行期证据用于补齐真实上报，最终统一进入 compare。</p>
        </div>
      </div>

      <div class="product-model-designer-drawer__sample-type">
        <button
          v-for="option in sampleTypeOptions"
          :key="option.value"
          type="button"
          class="product-model-designer-drawer__sample-type-button"
          :class="{ 'product-model-designer-drawer__sample-type-button--active': manualSampleType === option.value }"
          @click="manualSampleType = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="product-model-designer-drawer__sample-input" data-testid="manual-sample-input">
        <ElInput
          v-model="manualSamplePayload"
          type="textarea"
          :rows="10"
          placeholder="请输入当前产品下单设备样本 JSON，作为手动证据来源。"
        />
      </div>

      <div class="product-model-designer-drawer__runtime-toggle">
        <button
          type="button"
          class="product-model-designer-drawer__runtime-button"
          :class="{ 'product-model-designer-drawer__runtime-button--active': includeRuntimeCandidates }"
          @click="includeRuntimeCandidates = !includeRuntimeCandidates"
        >
          自动提炼：{{ includeRuntimeCandidates ? '开启' : '关闭' }}
        </button>
        <span>开启后会并列拉取属性快照、消息日志与命令记录形成自动证据。</span>
      </div>

      <div class="product-model-designer-drawer__draft-stage">
        <div class="product-model-designer-drawer__draft-head">
          <strong>人工补录候选</strong>
          <button type="button" class="product-model-designer-drawer__draft-add" data-testid="add-manual-draft" @click="addManualDraft">
            添加补录
          </button>
        </div>

        <div v-if="manualDrafts.length" class="product-model-designer-drawer__draft-list">
          <article
            v-for="draft in manualDrafts"
            :key="draft.key"
            class="product-model-designer-drawer__draft-card"
          >
            <div class="product-model-designer-drawer__draft-type-switch">
              <button
                v-for="option in draftTypeOptions"
                :key="option.value"
                type="button"
                class="product-model-designer-drawer__draft-type-button"
                :class="{ 'product-model-designer-drawer__draft-type-button--active': draft.modelType === option.value }"
                @click="draft.modelType = option.value"
              >
                {{ option.label }}
              </button>
            </div>

            <div class="product-model-designer-drawer__draft-grid">
              <label>
                <span>标识</span>
                <input v-model.trim="draft.identifier" type="text" />
              </label>
              <label>
                <span>名称</span>
                <input v-model.trim="draft.modelName" type="text" />
              </label>
              <label v-if="draft.modelType === 'property'">
                <span>数据类型</span>
                <input v-model.trim="draft.dataType" type="text" />
              </label>
              <label v-if="draft.modelType === 'property'">
                <span>规格 JSON</span>
                <input v-model.trim="draft.specsJson" type="text" />
              </label>
              <label v-if="draft.modelType === 'event'">
                <span>事件类型</span>
                <input v-model.trim="draft.eventType" type="text" />
              </label>
              <label v-if="draft.modelType === 'service'">
                <span>输入 JSON</span>
                <input v-model.trim="draft.serviceInputJson" type="text" />
              </label>
              <label v-if="draft.modelType === 'service'">
                <span>输出 JSON</span>
                <input v-model.trim="draft.serviceOutputJson" type="text" />
              </label>
              <label class="product-model-designer-drawer__draft-description">
                <span>说明</span>
                <input v-model.trim="draft.description" type="text" />
              </label>
            </div>

            <button type="button" class="product-model-designer-drawer__draft-remove" @click="removeManualDraft(draft.key)">
              移除
            </button>
          </article>
        </div>

        <div v-else class="detail-empty">
          当前没有人工补录项；如需补充事件或服务定义，可在这里手工添加。
        </div>
      </div>

      <div class="product-model-designer-drawer__actions">
        <StandardButton
          action="confirm"
          :loading="compareLoading"
          data-testid="governance-compare-submit"
          @click="handleCompare"
        >
          生成对比结果
        </StandardButton>
      </div>
    </section>

    <section v-if="product && compareSummary" class="detail-panel product-model-designer-drawer__summary-stage">
      <div class="detail-section-header">
        <div>
          <h3>治理摘要</h3>
          <p>治理摘要按对比结果汇总风险与收获，不再只统计本次提炼条数。</p>
        </div>
      </div>

      <div class="product-model-designer-drawer__result-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">样本设备</span>
          <strong class="detail-summary-card__value">{{ compareResult?.manualSummary?.sampleDeviceCode || '--' }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-manual">
          <span class="detail-summary-card__label">手动命中</span>
          <strong class="detail-summary-card__value">{{ compareSummary.manualCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-runtime">
          <span class="detail-summary-card__label">自动命中</span>
          <strong class="detail-summary-card__value">{{ compareSummary.runtimeCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-double-aligned">
          <span class="detail-summary-card__label">双证据一致</span>
          <strong class="detail-summary-card__value">{{ compareSummary.doubleAlignedCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-formal-exists">
          <span class="detail-summary-card__label">正式已存在</span>
          <strong class="detail-summary-card__value">{{ compareSummary.formalExistsCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-conflict">
          <span class="detail-summary-card__label">冲突待裁决</span>
          <strong class="detail-summary-card__value">{{ compareSummary.suspectedConflictCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card" data-testid="governance-summary-evidence-insufficient">
          <span class="detail-summary-card__label">证据不足</span>
          <strong class="detail-summary-card__value">{{ compareSummary.evidenceInsufficientCount ?? 0 }}</strong>
        </article>
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__compare-stage">
      <div class="detail-section-header">
        <div>
          <h3>对比工作区</h3>
          <p>对比行统一收口手动证据、自动证据与正式模型基线，并支持显式决策。</p>
        </div>
      </div>

      <ProductModelGovernanceCompareTable
        v-if="compareRows.length"
        :rows="compareRows"
        :decision-state="decisionState"
        @change-decision="handleDecisionChange"
      />

      <div v-else class="detail-empty">
        当前还没有对比结果，请先生成一次双证据 compare。
      </div>
    </section>

    <section
      v-if="product"
      class="detail-panel product-model-designer-drawer__confirm-stage"
      data-testid="governance-apply-stage"
    >
      <div class="detail-section-header">
        <div>
          <h3>正式模型确认区</h3>
          <p>这里只承接已明确选择“纳入新增 / 纳入修订”的项；继续观察、人工裁决和忽略不会写入正式模型。</p>
        </div>
      </div>

      <div
        v-if="selectedApplyEntries.length"
        class="product-model-designer-drawer__apply-list"
      >
        <article
          v-for="entry in selectedApplyEntries"
          :key="entry.key"
          class="detail-card"
          :data-testid="`governance-apply-item-${entry.key}`"
        >
          <div class="detail-card__header">
            <strong>{{ entry.item.modelName }}</strong>
            <span class="product-model-designer-drawer__candidate-identifier">{{ applyDecisionLabel(entry.decision) }}</span>
          </div>
          <div class="detail-card__meta">
            <span>{{ entry.item.modelType }}</span>
            <span>{{ entry.item.identifier }}</span>
            <span>{{ compareStatusLabel(entry.row.compareStatus) }}</span>
          </div>
          <p class="product-model-designer-drawer__candidate-description">
            {{ applyEvidenceSummary(entry.row) }}
          </p>
        </article>
      </div>

      <div v-else class="detail-empty">
        当前还没有待正式应用项；继续观察、人工裁决和忽略会停留在 compare 阶段。
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__formal-stage">
      <div class="detail-section-header">
        <div>
          <h3>正式模型</h3>
          <p>治理应用成功后会回刷当前产品的正式模型列表。</p>
        </div>
      </div>

      <div v-if="models.length" class="product-model-designer-drawer__formal-list">
        <article v-for="model in models" :key="String(model.id)" class="detail-card">
          <div class="detail-card__header">
            <strong>{{ model.modelName }}</strong>
            <span class="product-model-designer-drawer__candidate-identifier">{{ model.identifier }}</span>
          </div>
          <div class="detail-card__meta">
            <span>{{ model.modelType }}</span>
            <span>{{ model.dataType || model.eventType || '--' }}</span>
            <span>排序 {{ model.sortNo ?? '--' }}</span>
          </div>
          <p class="product-model-designer-drawer__candidate-description">
            {{ model.description?.trim() || '当前没有补充说明。' }}
          </p>
        </article>
      </div>

      <div v-else class="detail-empty">
        当前产品还没有正式物模型。
      </div>
    </section>

    <template #footer>
      <StandardDrawerFooter
        cancel-text="关闭"
        confirm-text="确认应用"
        :confirm-loading="applyLoading"
        :confirm-disabled="!selectedApplyItems.length"
        @cancel="emit('update:modelValue', false)"
        @confirm="handleApply"
      />
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import ProductModelGovernanceCompareTable from '@/components/product/ProductModelGovernanceCompareTable.vue'
import { productApi } from '@/api/product'
import type {
  Product,
  ProductModel,
  ProductModelGovernanceApplyItem,
  ProductModelGovernanceCompareResult,
  ProductModelGovernanceCompareRow,
  ProductModelGovernanceDecision,
  ProductModelGovernanceManualDraftItem,
  ProductModelManualSampleType,
  ProductModelType
} from '@/types/api'
import { ElMessage } from '@/utils/message'

const props = withDefaults(defineProps<{
  modelValue: boolean
  product?: Product | null
}>(), {
  product: null
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

interface ManualDraftForm extends ProductModelGovernanceManualDraftItem {
  key: string
  dataType: string
  specsJson: string
  eventType: string
  serviceInputJson: string
  serviceOutputJson: string
  description: string
}

type GovernanceDecisionUi = ProductModelGovernanceDecision | 'observe' | 'review' | 'ignore'

const sampleTypeOptions: Array<{ label: string; value: ProductModelManualSampleType }> = [
  { label: '业务数据', value: 'business' },
  { label: '状态数据', value: 'status' },
  { label: '其他数据', value: 'other' }
]

const draftTypeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性', value: 'property' },
  { label: '事件', value: 'event' },
  { label: '服务', value: 'service' }
]

const loading = ref(false)
const compareLoading = ref(false)
const applyLoading = ref(false)
const errorMessage = ref('')
const manualSampleType = ref<ProductModelManualSampleType>('business')
const manualSamplePayload = ref('')
const includeRuntimeCandidates = ref(true)
const models = ref<ProductModel[]>([])
const compareResult = ref<ProductModelGovernanceCompareResult | null>(null)
const manualDrafts = ref<ManualDraftForm[]>([])
const decisionState = ref<Record<string, GovernanceDecisionUi>>({})
let manualDraftSeed = 0

const compareSummary = computed(() => compareResult.value?.summary ?? null)
const compareRows = computed<ProductModelGovernanceCompareRow[]>(() => compareResult.value?.compareRows ?? [])
const selectedApplyEntries = computed(() =>
  compareRows.value
    .map((row) => ({ row, decision: decisionState.value[rowKey(row)] }))
    .filter((item): item is { row: ProductModelGovernanceCompareRow; decision: ProductModelGovernanceDecision } =>
      item.decision === 'create' || item.decision === 'update'
    )
    .map(({ row, decision }) => ({
      key: rowKey(row),
      row,
      decision,
      item: buildApplyItem(row, decision)
    }))
)
const selectedApplyItems = computed<ProductModelGovernanceApplyItem[]>(() => selectedApplyEntries.value.map((entry) => entry.item))
const hasLoadedContent = computed(() => Boolean(models.value.length || compareResult.value))

watch(
  () => [props.modelValue, props.product?.id] as const,
  async ([visible, productId], previousValue) => {
    const previousProductId = previousValue?.[1]
    if (!visible || !productId) {
      if (!visible) {
        errorMessage.value = ''
      }
      return
    }
    if (productId !== previousProductId) {
      resetGovernanceSession()
    }
    await loadModels(productId)
  },
  { immediate: true }
)

function rowKey(row: ProductModelGovernanceCompareRow) {
  return `${row.modelType}:${row.identifier}`
}

function compareStatusLabel(status: ProductModelGovernanceCompareRow['compareStatus']) {
  return {
    double_aligned: '双证据一致',
    manual_only: '仅手动命中',
    runtime_only: '自动证据独有',
    formal_exists: '正式模型已存在',
    suspected_conflict: '疑似冲突',
    evidence_insufficient: '证据不足'
  }[status] ?? status
}

function applyDecisionLabel(decision: ProductModelGovernanceDecision) {
  return decision === 'update' ? '纳入修订' : '纳入新增'
}

function applyEvidenceSummary(row: ProductModelGovernanceCompareRow) {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  if (!source) {
    return '当前没有可用于正式应用的证据摘要。'
  }
  const dataHint = source.dataType || source.eventType || formatServiceHint(source.serviceInputJson, source.serviceOutputJson)
  const sourceTables = source.sourceTables?.length ? source.sourceTables.join(' / ') : '未标注来源'
  return [dataHint, sourceTables, source.description?.trim() || '当前没有补充说明。']
    .filter(Boolean)
    .join(' · ')
}

function formatServiceHint(inputJson?: string | null, outputJson?: string | null) {
  if (inputJson?.trim() && outputJson?.trim()) {
    return '输入/输出已定义'
  }
  if (inputJson?.trim()) {
    return '仅定义输入'
  }
  if (outputJson?.trim()) {
    return '仅定义输出'
  }
  return ''
}

async function loadModels(productId: string | number) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await productApi.listProductModels(productId)
    models.value = response.data ?? []
  } catch (error) {
    models.value = []
    errorMessage.value = error instanceof Error ? error.message : '加载产品物模型失败'
  } finally {
    loading.value = false
  }
}

function addManualDraft() {
  manualDraftSeed += 1
  manualDrafts.value = [
    ...manualDrafts.value,
    {
      key: `draft-${manualDraftSeed}`,
      modelType: 'property',
      identifier: '',
      modelName: '',
      dataType: '',
      specsJson: '',
      eventType: '',
      serviceInputJson: '',
      serviceOutputJson: '',
      description: ''
    }
  ]
}

function removeManualDraft(key: string) {
  manualDrafts.value = manualDrafts.value.filter((draft) => draft.key !== key)
}

async function handleCompare() {
  if (!props.product?.id) {
    return
  }
  compareLoading.value = true
  errorMessage.value = ''
  try {
    const response = await productApi.compareProductModelGovernance(props.product.id, {
      manualExtract: manualSamplePayload.value.trim()
        ? {
            sampleType: manualSampleType.value,
            samplePayload: manualSamplePayload.value.trim()
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

function handleDecisionChange(payload: { key: string; decision: GovernanceDecisionUi }) {
  decisionState.value = {
    ...decisionState.value,
    [payload.key]: payload.decision
  }
}

async function handleApply() {
  if (!props.product?.id || !selectedApplyItems.value.length) {
    return
  }
  applyLoading.value = true
  errorMessage.value = ''
  try {
    await productApi.applyProductModelGovernance(props.product.id, {
      items: selectedApplyItems.value
    })
    ElMessage.success('治理应用成功')
    await loadModels(props.product.id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '治理应用失败'
    ElMessage.error(errorMessage.value)
  } finally {
    applyLoading.value = false
  }
}

function defaultDecisionForRow(row: ProductModelGovernanceCompareRow): GovernanceDecisionUi {
  switch (row.compareStatus) {
    case 'double_aligned':
      return 'create'
    case 'formal_exists':
      return 'ignore'
    case 'suspected_conflict':
      return 'review'
    case 'manual_only':
    case 'runtime_only':
    case 'evidence_insufficient':
    default:
      return 'observe'
  }
}

function normalizedManualDraftItems(): ProductModelGovernanceManualDraftItem[] {
  return manualDrafts.value
    .filter((draft) => draft.identifier.trim() && draft.modelName.trim())
    .map((draft) => ({
      modelType: draft.modelType,
      identifier: draft.identifier.trim(),
      modelName: draft.modelName.trim(),
      dataType: draft.dataType.trim() || undefined,
      specsJson: draft.specsJson.trim() || undefined,
      eventType: draft.eventType.trim() || undefined,
      serviceInputJson: draft.serviceInputJson.trim() || undefined,
      serviceOutputJson: draft.serviceOutputJson.trim() || undefined,
      description: draft.description.trim() || undefined
    }))
}

function buildApplyItem(row: ProductModelGovernanceCompareRow, decision: ProductModelGovernanceDecision): ProductModelGovernanceApplyItem {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  return {
    decision,
    targetModelId: decision === 'update' ? row.formalModel?.modelId ?? undefined : undefined,
    modelType: row.modelType,
    identifier: row.identifier,
    modelName: source?.modelName || row.identifier,
    dataType: source?.dataType ?? undefined,
    specsJson: source?.specsJson ?? undefined,
    eventType: source?.eventType ?? undefined,
    serviceInputJson: source?.serviceInputJson ?? undefined,
    serviceOutputJson: source?.serviceOutputJson ?? undefined,
    sortNo: source?.sortNo ?? undefined,
    requiredFlag: source?.requiredFlag ?? undefined,
    description: source?.description ?? undefined,
    compareStatus: row.compareStatus
  }
}

function resetGovernanceSession() {
  manualSampleType.value = 'business'
  manualSamplePayload.value = ''
  includeRuntimeCandidates.value = true
  compareResult.value = null
  manualDrafts.value = []
  decisionState.value = {}
}
</script>

<style scoped>
.product-model-designer-drawer__header,
.product-model-designer-drawer__evidence-stage,
.product-model-designer-drawer__summary-stage,
.product-model-designer-drawer__compare-stage,
.product-model-designer-drawer__confirm-stage,
.product-model-designer-drawer__formal-stage,
.product-model-designer-drawer__draft-stage {
  display: grid;
  gap: 1rem;
}

.product-model-designer-drawer__header {
  grid-template-columns: minmax(0, 1fr) 18rem;
  align-items: start;
}

.product-model-designer-drawer__identity,
.product-model-designer-drawer__summary,
.product-model-designer-drawer__draft-list,
.product-model-designer-drawer__draft-grid {
  display: grid;
  gap: 0.72rem;
}

.product-model-designer-drawer__kicker,
.product-model-designer-drawer__summary-item span,
.product-model-designer-drawer__candidate-identifier,
.product-model-designer-drawer__runtime-toggle span,
.product-model-designer-drawer__draft-grid label span {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.82rem;
  line-height: 1.6;
}

.product-model-designer-drawer__kicker {
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.product-model-designer-drawer__headline {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.4rem, 2vw, 1.86rem);
  line-height: 1.18;
}

.product-model-designer-drawer__meta,
.product-model-designer-drawer__actions,
.product-model-designer-drawer__sample-type,
.product-model-designer-drawer__draft-type-switch,
.product-model-designer-drawer__runtime-toggle,
.product-model-designer-drawer__draft-head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.58rem;
  align-items: center;
}

.product-model-designer-drawer__meta span,
.product-model-designer-drawer__summary-item,
.product-model-designer-drawer__runtime-button,
.product-model-designer-drawer__sample-type-button,
.product-model-designer-drawer__draft-type-button,
.product-model-designer-drawer__draft-add,
.product-model-designer-drawer__draft-remove {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.94);
}

.product-model-designer-drawer__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.92rem;
  padding: 0.24rem 0.72rem;
  color: var(--text-secondary);
}

.product-model-designer-drawer__summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-model-designer-drawer__summary-item {
  display: grid;
  gap: 0.26rem;
  padding: 0.84rem 0.9rem;
}

.product-model-designer-drawer__summary-item strong,
.product-model-designer-drawer__draft-head strong {
  color: var(--text-heading);
}

.product-model-designer-drawer__runtime-button,
.product-model-designer-drawer__sample-type-button,
.product-model-designer-drawer__draft-type-button,
.product-model-designer-drawer__draft-add,
.product-model-designer-drawer__draft-remove {
  min-height: 2rem;
  padding: 0.24rem 0.78rem;
  color: var(--text-secondary);
  cursor: pointer;
}

.product-model-designer-drawer__runtime-button--active,
.product-model-designer-drawer__sample-type-button--active,
.product-model-designer-drawer__draft-type-button--active {
  border-color: color-mix(in srgb, var(--brand) 52%, #fff);
  color: var(--brand);
}

.product-model-designer-drawer__draft-list {
  gap: 0.86rem;
}

.product-model-designer-drawer__draft-card {
  display: grid;
  gap: 0.86rem;
  padding: 0.92rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 251, 253, 0.98));
}

.product-model-designer-drawer__draft-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.product-model-designer-drawer__draft-grid label {
  display: grid;
  gap: 0.32rem;
}

.product-model-designer-drawer__draft-grid input {
  min-height: 2.3rem;
  padding: 0.46rem 0.7rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.8rem;
  color: var(--text-primary);
}

.product-model-designer-drawer__draft-description {
  grid-column: 1 / -1;
}

.product-model-designer-drawer__result-grid,
.product-model-designer-drawer__apply-list,
.product-model-designer-drawer__formal-list {
  display: grid;
  gap: 0.86rem;
}

.product-model-designer-drawer__result-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 960px) {
  .product-model-designer-drawer__header,
  .product-model-designer-drawer__summary,
  .product-model-designer-drawer__draft-grid,
  .product-model-designer-drawer__result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
