<template>
  <div class="product-model-designer-workspace">
    <div v-if="!product" class="product-model-designer__empty">
      <strong>请先选择产品</strong>
      <p>需要先选中产品，才能继续进入物模型治理。</p>
    </div>

    <div v-else-if="loading" class="detail-notice">
      <span class="detail-notice__label">加载中</span>
      <strong class="detail-notice__value">正在加载产品物模型...</strong>
    </div>

    <div v-else-if="errorMessage" class="detail-notice detail-notice--danger">
      <span class="detail-notice__label">加载失败</span>
      <strong class="detail-notice__value">{{ errorMessage }}</strong>
    </div>

    <template v-else>
      <section class="detail-panel product-model-designer__header">
        <div class="product-model-designer__header-heading">
          <div class="product-model-designer__header-copy">
            <p class="product-model-designer__kicker product-model-designer__header-kicker">物模型治理</p>
            <h3 class="product-model-designer__headline">{{ designerStageTitle }}</h3>
            <p class="product-model-designer__header-description">
              先看候选目录和正式契约，再进入完整治理抽屉处理深度变更。
            </p>
          </div>

          <div class="product-model-designer__mode-switcher" role="tablist" aria-label="设计器模式">
            <button
              type="button"
              class="product-model-designer__mode-chip"
              :class="{ 'product-model-designer__mode-chip--active': designerMode === 'candidates' }"
              @click="designerMode = 'candidates'"
            >
              候选提炼
            </button>
            <button
              type="button"
              class="product-model-designer__mode-chip"
              :class="{ 'product-model-designer__mode-chip--active': designerMode === 'formal' }"
              @click="designerMode = 'formal'"
            >
              正式模型
            </button>
          </div>
        </div>

        <div class="product-model-designer__header-meta">
          <span>{{ product.productName || '--' }}</span>
          <span>{{ product.productKey || '--' }}</span>
          <span>{{ product.protocolCode || '--' }}</span>
          <span>{{ productNodeTypeLabel }}</span>
        </div>
      </section>

      <section class="product-model-designer__summary-strip">
        <article class="product-model-designer__summary-card product-model-designer__summary-card--lead product-model-designer__summary-lead">
          <span class="product-model-designer__summary-label">真实证据概览</span>
          <strong>先提炼，再确认，再沉淀为正式物模型</strong>
          <p>属性优先来源于真实属性与消息快照，事件和服务在缺证据时保留诚实空态。</p>
        </article>
        <article class="product-model-designer__summary-card">
          <span class="product-model-designer__summary-label">属性候选</span>
          <strong>{{ candidateSummary.propertyCandidateCount ?? 0 }}</strong>
          <p>原始证据 {{ candidateSummary.propertyEvidenceCount ?? 0 }} 条</p>
        </article>
        <article class="product-model-designer__summary-card">
          <span class="product-model-designer__summary-label">待人工确认</span>
          <strong>{{ candidateSummary.needsReviewCount ?? 0 }}</strong>
          <p>{{ candidateSummary.eventHint || '当前无事件候选冲突' }}</p>
        </article>
        <article class="product-model-designer__summary-card">
          <span class="product-model-designer__summary-label">正式模型</span>
          <strong>{{ models.length }}</strong>
          <p>最近提炼：{{ formatDateTime(candidateSummary.lastExtractedAt) }}</p>
        </article>
      </section>

      <section v-if="designerMode === 'candidates'" class="detail-panel product-model-designer__workspace-shell">
        <div class="product-model-designer__candidate-workspace">
          <aside class="product-model-designer__candidate-nav">
            <button
              v-for="item in candidateNavItems"
              :key="item.key"
              type="button"
              class="product-model-designer__candidate-nav-item"
              :class="{ 'product-model-designer__candidate-nav-item--active': activeCandidateView === item.key }"
              @click="activeCandidateView = item.key"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </button>
          </aside>

          <div class="product-model-designer__candidate-body product-model-designer__workspace-main">
            <div class="product-model-designer__candidate-body-header">
              <div>
                <h3>{{ activeCandidateViewTitle }}</h3>
                <p>{{ activeCandidateViewDescription }}</p>
              </div>
              <span class="product-model-designer__candidate-body-meta">
                最近提炼：{{ formatDateTime(candidateSummary.lastExtractedAt) }}
              </span>
            </div>

            <div v-if="visibleCandidates.length" class="product-model-designer__candidate-list">
              <article
                v-for="candidate in visibleCandidates"
                :key="candidateKey(candidate)"
                class="product-model-designer__candidate-card"
              >
                <header class="product-model-designer__candidate-card-header">
                  <div class="product-model-designer__candidate-card-title">
                    <strong>{{ candidate.modelName }}</strong>
                    <span>{{ candidate.identifier }}</span>
                  </div>
                  <el-tag round>{{ candidate.needsReview ? '待人工确认' : '可直接采纳' }}</el-tag>
                </header>
                <div class="product-model-designer__candidate-card-tags">
                  <el-tag round>{{ candidateTypeLabel(candidate.modelType) }}</el-tag>
                  <el-tag round>{{ candidateGroupLabel(candidate.groupKey) }}</el-tag>
                </div>
                <p class="product-model-designer__description">{{ candidate.description || '暂无补充说明' }}</p>
              </article>
            </div>

            <div v-else class="product-model-designer__empty">
              <strong>当前目录暂无候选</strong>
              <p>{{ activeCandidateViewDescription }}</p>
            </div>
          </div>

          <aside class="product-model-designer__candidate-rail product-model-designer__workspace-rail">
            <div class="product-model-designer__confirm-metrics">
              <div>
                <span>属性候选</span>
                <strong>{{ candidateSummary.propertyCandidateCount ?? 0 }}</strong>
              </div>
              <div>
                <span>待人工确认</span>
                <strong>{{ candidateSummary.needsReviewCount ?? 0 }}</strong>
              </div>
              <div>
                <span>正式模型</span>
                <strong>{{ models.length }}</strong>
              </div>
            </div>
            <StandardButton action="confirm" data-testid="confirm-model-candidates" @click="fullDesignerVisible = true">
              进入完整治理
            </StandardButton>
          </aside>
        </div>
      </section>

      <section v-else class="detail-panel product-model-designer__workspace-shell product-model-designer__workspace-shell--formal">
        <div class="product-model-designer__formal-overview" role="tablist" aria-label="产品物模型类型">
          <button
            v-for="item in typeOptions"
            :key="item.value"
            type="button"
            class="product-model-designer__formal-overview-card"
            :class="{ 'product-model-designer__formal-overview-card--active': activeType === item.value }"
            @click="activeType = item.value"
          >
            <span>{{ item.label }}</span>
            <strong>{{ countByType(item.value) }}</strong>
            <small>{{ emptyDescriptionMap[item.value] }}</small>
          </button>
        </div>

        <div class="product-model-designer__formal-stage">
          <h3 class="product-model-designer__formal-title">统一维护产品正式物模型</h3>
          <div v-if="activeModels.length" class="product-model-designer__list">
            <article v-for="model in activeModels" :key="String(model.id)" class="product-model-designer__card">
              <header class="product-model-designer__card-header">
                <div class="product-model-designer__card-heading">
                  <strong>{{ model.modelName }}</strong>
                  <span>{{ model.identifier }}</span>
                </div>
                <el-tag :type="model.requiredFlag === 1 ? 'warning' : 'info'" round>
                  {{ model.requiredFlag === 1 ? '必填' : '选填' }}
                </el-tag>
              </header>
              <div class="product-model-designer__card-summary">
                <span>排序：{{ model.sortNo ?? '--' }}</span>
                <span v-if="model.modelType === 'property'">数据类型：{{ model.dataType || '--' }}</span>
                <span v-else-if="model.modelType === 'event'">事件类型：{{ model.eventType || '--' }}</span>
                <span v-else>服务输入/输出：{{ formatServiceSummary(model) }}</span>
              </div>
              <p class="product-model-designer__description">
                {{ model.description?.trim() || emptyDescriptionMap[model.modelType] }}
              </p>
            </article>
          </div>

          <div v-else class="product-model-designer__empty">
            <strong>暂无物模型</strong>
            <p>{{ emptyDescriptionMap[activeType] }}</p>
          </div>
        </div>
      </section>
    </template>

    <ProductModelDesignerDrawer v-model="fullDesignerVisible" :product="product" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import ProductModelDesignerDrawer from '@/components/product/ProductModelDesignerDrawer.vue'
import { productApi } from '@/api/product'
import type {
  Product,
  ProductModel,
  ProductModelCandidate,
  ProductModelCandidateResult,
  ProductModelCandidateSummary,
  ProductModelType
} from '@/types/api'

const props = defineProps<{ product: Product | null }>()

type DesignerMode = 'candidates' | 'formal'
type CandidateViewKey = ProductModelType | 'review'

const typeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性模型', value: 'property' },
  { label: '事件模型', value: 'event' },
  { label: '服务模型', value: 'service' }
]

const emptyDescriptionMap: Record<ProductModelType, string> = {
  property: '当前还没有属性模型，可以先定义遥测属性、规格 JSON 和风险监测字段。',
  event: '当前还没有事件模型，可以补齐告警、状态变化或故障上报事件。',
  service: '当前还没有服务模型，可以定义远程命令、输入输出参数和调用约束。'
}

const loading = ref(false)
const errorMessage = ref('')
const models = ref<ProductModel[]>([])
const candidateResult = ref<ProductModelCandidateResult | null>(null)
const designerMode = ref<DesignerMode>('candidates')
const activeType = ref<ProductModelType>('property')
const activeCandidateView = ref<CandidateViewKey>('property')
const fullDesignerVisible = ref(false)

const designerStageTitle = computed(() =>
  designerMode.value === 'candidates' ? '基于真实上报提炼产品契约' : '统一维护产品正式物模型'
)
const productNodeTypeLabel = computed(() => (props.product?.nodeType === 2 ? '网关设备' : '直连设备'))
const candidateSummary = computed<ProductModelCandidateSummary>(() => candidateResult.value?.summary ?? createEmptySummary())
const allCandidates = computed<ProductModelCandidate[]>(() => [
  ...(candidateResult.value?.propertyCandidates ?? []),
  ...(candidateResult.value?.eventCandidates ?? []),
  ...(candidateResult.value?.serviceCandidates ?? [])
])
const reviewCandidates = computed(() => allCandidates.value.filter((candidate) => Boolean(candidate.needsReview)))
const candidateNavItems = computed(() => [
  { key: 'property' as const, label: '属性候选', count: candidateResult.value?.propertyCandidates?.length ?? 0 },
  { key: 'event' as const, label: '事件候选', count: candidateResult.value?.eventCandidates?.length ?? 0 },
  { key: 'service' as const, label: '服务候选', count: candidateResult.value?.serviceCandidates?.length ?? 0 },
  { key: 'review' as const, label: '待人工确认', count: reviewCandidates.value.length }
])
const visibleCandidates = computed<ProductModelCandidate[]>(() => {
  if (activeCandidateView.value === 'review') return reviewCandidates.value
  if (activeCandidateView.value === 'property') return candidateResult.value?.propertyCandidates ?? []
  if (activeCandidateView.value === 'event') return candidateResult.value?.eventCandidates ?? []
  return candidateResult.value?.serviceCandidates ?? []
})
const activeCandidateViewTitle = computed(() => {
  if (activeCandidateView.value === 'review') return '待人工确认'
  return candidateTypeLabel(activeCandidateView.value)
})
const activeCandidateViewDescription = computed(() => {
  if (activeCandidateView.value === 'review') return '集中处理临时验证字段、命名漂移和边界尚不稳定的候选。'
  if (activeCandidateView.value === 'event') return candidateSummary.value.eventHint || '仅在存在真实事件证据时生成事件候选。'
  if (activeCandidateView.value === 'service') return candidateSummary.value.serviceHint || '仅在存在稳定命令证据时生成服务候选。'
  return '从真实属性快照和消息证据中提炼出的属性候选。'
})
const activeModels = computed(() => models.value.filter((model) => model.modelType === activeType.value))

watch(
  () => props.product?.id,
  async (productId) => {
    if (!productId) {
      models.value = []
      candidateResult.value = null
      errorMessage.value = ''
      return
    }
    loading.value = true
    errorMessage.value = ''
    try {
      const [modelResponse, candidateResponse] = await Promise.allSettled([
        productApi.listProductModels(productId),
        productApi.listProductModelCandidates(productId)
      ])
      if (modelResponse.status === 'rejected') throw modelResponse.reason
      models.value = modelResponse.value.data ?? []
      if (candidateResponse.status === 'fulfilled') {
        candidateResult.value = candidateResponse.value.data ?? createEmptyResult(productId)
      } else {
        candidateResult.value = createEmptyResult(productId)
        designerMode.value = 'formal'
      }
      activeCandidateView.value = candidateNavItems.value.find((item) => item.count > 0)?.key ?? 'property'
    } catch (error) {
      models.value = []
      candidateResult.value = createEmptyResult(productId)
      errorMessage.value = error instanceof Error ? error.message : '加载产品物模型失败'
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

function createEmptySummary(): ProductModelCandidateSummary {
  return {
    propertyEvidenceCount: 0,
    propertyCandidateCount: 0,
    eventCandidateCount: 0,
    serviceCandidateCount: 0,
    needsReviewCount: 0,
    eventHint: '暂无真实事件证据。',
    serviceHint: '暂无真实服务证据。',
    lastExtractedAt: null
  }
}

function createEmptyResult(productId?: string | number): ProductModelCandidateResult {
  return {
    productId: productId ?? '',
    summary: createEmptySummary(),
    propertyCandidates: [],
    eventCandidates: [],
    serviceCandidates: []
  }
}

function countByType(type: ProductModelType) {
  return models.value.filter((model) => model.modelType === type).length
}

function candidateTypeLabel(type: ProductModelType) {
  return typeOptions.find((item) => item.value === type)?.label ?? '物模型'
}

function candidateKey(candidate: ProductModelCandidate) {
  return `${candidate.modelType}:${candidate.identifier}`
}

function candidateGroupLabel(groupKey?: string | null) {
  if (groupKey === 'telemetry') return '业务测点'
  if (groupKey === 'device_status') return '设备状态'
  if (groupKey === 'location') return '定位信息'
  if (groupKey === 'service') return '服务证据'
  if (groupKey === 'event') return '事件证据'
  return '待归类'
}

function formatDateTime(value?: string | null) {
  return value?.trim() || '--'
}

function formatServiceSummary(model: ProductModel) {
  const hasInput = Boolean(model.serviceInputJson?.trim())
  const hasOutput = Boolean(model.serviceOutputJson?.trim())
  if (hasInput && hasOutput) return '已配置'
  if (hasInput) return '仅输入'
  if (hasOutput) return '仅输出'
  return '未配置'
}
</script>

<style scoped>
.product-model-designer-workspace,
.product-model-designer__header,
.product-model-designer__header-copy,
.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-stage,
.product-model-designer__candidate-card,
.product-model-designer__card {
  display: grid;
  gap: 0.85rem;
}

.product-model-designer-workspace {
  gap: 0.95rem;
}

.product-model-designer__header,
.product-model-designer__summary-card,
.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-overview-card,
.product-model-designer__formal-stage,
.product-model-designer__candidate-card,
.product-model-designer__card {
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__header {
  padding: 1.18rem 1.2rem 1.08rem;
  gap: 1rem;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 34%),
    linear-gradient(180deg, rgba(249, 251, 254, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 18px 42px rgba(28, 53, 87, 0.08);
}

.product-model-designer__header-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-model-designer__kicker,
.product-model-designer__summary-label {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 60%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.product-model-designer__headline,
.product-model-designer__summary-card strong,
.product-model-designer__card-heading strong,
.product-model-designer__candidate-card-title strong {
  margin: 0;
  color: var(--text-heading);
}

.product-model-designer__headline {
  font-size: clamp(1.4rem, 2vw, 1.9rem);
  line-height: 1.14;
  letter-spacing: -0.02em;
}

.product-model-designer__header-description,
.product-model-designer__description,
.product-model-designer__candidate-body-meta,
.product-model-designer__summary-card p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.product-model-designer__mode-switcher {
  display: inline-flex;
  padding: 0.28rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.92);
}

.product-model-designer__mode-chip {
  min-width: 6.8rem;
  padding: 0.56rem 0.94rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.product-model-designer__mode-chip--active,
.product-model-designer__candidate-nav-item--active,
.product-model-designer__formal-overview-card--active {
  color: var(--brand);
  background: linear-gradient(180deg, rgba(255, 249, 244, 0.98), rgba(255, 245, 236, 0.98));
  box-shadow: 0 12px 20px rgba(217, 120, 47, 0.1);
}

.product-model-designer__header-meta,
.product-model-designer__candidate-card-tags,
.product-model-designer__card-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.product-model-designer__header-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.85rem;
  padding: 0.26rem 0.7rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.product-model-designer__summary-strip,
.product-model-designer__confirm-metrics,
.product-model-designer__formal-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.product-model-designer__summary-card {
  display: grid;
  gap: 0.38rem;
  padding: 1rem 1.02rem;
}

.product-model-designer__summary-card--lead {
  grid-column: span 1;
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  background:
    linear-gradient(180deg, rgba(255, 251, 248, 0.98), rgba(255, 255, 255, 0.98));
}

.product-model-designer__workspace-shell {
  padding: 1rem 1.05rem;
}

.product-model-designer__candidate-workspace {
  display: grid;
  grid-template-columns: minmax(12rem, 13rem) minmax(0, 1fr) minmax(15rem, 17rem);
  gap: 1rem;
  align-items: start;
}

.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-stage {
  padding: 1rem 1.04rem;
}

.product-model-designer__candidate-nav-item,
.product-model-designer__formal-overview-card {
  width: 100%;
  padding: 0.9rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
  text-align: left;
  cursor: pointer;
}

.product-model-designer__candidate-card-header,
.product-model-designer__card-header,
.product-model-designer__candidate-body-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.9rem;
}

.product-model-designer__confirm-metrics div {
  display: grid;
  gap: 0.2rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(252, 253, 255, 0.96);
}

.product-model-designer__formal-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
}

.product-model-designer__empty {
  display: grid;
  gap: 0.45rem;
  padding: 1.1rem 1rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 24%, white);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.92), rgba(255, 255, 255, 0.96));
}

@media (max-width: 1200px) {
  .product-model-designer__summary-strip,
  .product-model-designer__formal-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-model-designer__candidate-workspace {
    grid-template-columns: minmax(11rem, 12rem) minmax(0, 1fr);
  }

  .product-model-designer__candidate-rail {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .product-model-designer__header-heading,
  .product-model-designer__candidate-body-header,
  .product-model-designer__candidate-card-header,
  .product-model-designer__card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-model-designer__summary-strip,
  .product-model-designer__candidate-workspace,
  .product-model-designer__formal-overview,
  .product-model-designer__confirm-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
