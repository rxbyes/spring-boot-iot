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
      <section class="detail-panel product-model-designer__exhibit-head">
        <div class="product-model-designer__exhibit-copy">
          <span class="product-model-designer__eyebrow">契约策展</span>
          <h3 class="product-model-designer__headline">{{ designerStageTitle }}</h3>
          <p class="product-model-designer__brief-statement">{{ headerStatement }}</p>
        </div>

        <div class="product-model-designer__mode-switcher" role="tablist" aria-label="设计器模式">
          <button
            type="button"
            class="product-model-designer__mode-chip"
            :class="{ 'product-model-designer__mode-chip--active': designerMode === 'manual' }"
            @click="designerMode = 'manual'"
          >
            手动提炼
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
      </section>

      <section class="product-model-designer__curation-strip">
        <div class="product-model-designer__curation-strip-shell">
          <article
            v-for="card in summaryCards"
            :key="card.key"
            class="product-model-designer__curation-strip-item"
          >
            <span class="product-model-designer__summary-label">{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </div>
      </section>

      <section v-if="designerMode === 'manual'" class="detail-panel product-model-designer__workspace-shell">
        <div class="product-model-designer__candidate-stage">
          <div class="product-model-designer__candidate-body">
            <div class="product-model-designer__candidate-body-header">
              <div class="product-model-designer__stage-copy">
                <h3>手动提炼工作区</h3>
                <p class="product-model-designer__candidate-body-intro">
                  围绕当前产品的单设备业务 / 状态 / 其他样本完成提炼、核对与写库确认。
                </p>
              </div>
              <StandardButton action="confirm" data-testid="confirm-model-candidates" @click="fullDesignerVisible = true">
                进入完整治理
              </StandardButton>
            </div>

            <section class="product-model-designer__curation-board">
              <ol class="product-model-designer__curation-flow">
                <li
                  v-for="(item, index) in manualLedgerItems"
                  :key="item.key"
                  class="product-model-designer__curation-item product-model-designer__flow-item"
                >
                  <span class="product-model-designer__curation-index">
                    {{ String(index + 1).padStart(2, '0') }}
                  </span>
                  <div class="product-model-designer__curation-copy">
                    <strong>{{ item.title }}</strong>
                    <span>{{ item.summary }}</span>
                    <p class="product-model-designer__description">{{ item.description }}</p>
                  </div>
                </li>
              </ol>
            </section>
          </div>
        </div>
      </section>

      <section v-else class="detail-panel product-model-designer__workspace-shell product-model-designer__workspace-shell--formal">
        <div class="product-model-designer__formal-stage">
          <div class="product-model-designer__formal-stage-copy">
            <h3 class="product-model-designer__formal-title">统一维护产品正式物模型</h3>
            <p class="product-model-designer__formal-intro">
              正式模型只保留已经确认的契约，便于按类型集中核对字段、排序和说明。
            </p>
          </div>
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
import type { Product, ProductModel, ProductModelType } from '@/types/api'

const props = defineProps<{ product: Product | null }>()

type DesignerMode = 'manual' | 'formal'

const typeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性模型', value: 'property' },
  { label: '事件模型', value: 'event' },
  { label: '服务模型', value: 'service' }
]
const manualLedgerItems = [
  {
    key: 'sample',
    title: '粘贴样本 JSON',
    summary: '单次只解析一个设备样本',
    description: '支持业务数据、状态数据和其他数据三类样本。时间戳层只作为证据层，不会进入正式 identifier。'
  },
  {
    key: 'review',
    title: '核对正式候选',
    summary: '其他数据默认 needsReview',
    description: '数组不会自动进入正式物模型；对象会继续下钻到标量叶子后再生成候选，避免把结构噪音直接写成属性。'
  },
  {
    key: 'confirm',
    title: '确认写库',
    summary: '继续落在 iot_product_model',
    description: '提炼后的候选会先进入正式模型确认，再通过“确认写库”提交到数据库，不新增草稿表和并行路由。'
  }
] as const

const emptyDescriptionMap: Record<ProductModelType, string> = {
  property: '当前还没有属性模型，可以先定义遥测属性、规格 JSON 和风险监测字段。',
  event: '当前还没有事件模型，可以补齐告警、状态变化或故障上报事件。',
  service: '当前还没有服务模型，可以定义远程命令、输入输出参数和调用约束。'
}

const loading = ref(false)
const errorMessage = ref('')
const models = ref<ProductModel[]>([])
const designerMode = ref<DesignerMode>('manual')
const activeType = ref<ProductModelType>('property')
const fullDesignerVisible = ref(false)

const designerStageTitle = computed(() =>
  designerMode.value === 'manual' ? '基于手动样本提炼产品契约' : '统一维护产品正式物模型'
)
const activeModels = computed(() => models.value.filter((model) => model.modelType === activeType.value))
const headerStatement = computed(() => {
  if (designerMode.value === 'formal') {
    return '先维护正式契约，再回到手动提炼流程补充新样本候选。'
  }
  return '先围绕单设备样本 JSON 手动提炼，再进入正式模型确认并写库。'
})
const summaryCards = computed(() => [
  {
    key: 'currentMode',
    label: '当前入口',
    value: designerMode.value === 'manual' ? '手动提炼' : '正式模型'
  },
  {
    key: 'evidenceBoundary',
    label: '证据边界',
    value: '单设备 JSON'
  },
  {
    key: 'formalModels',
    label: '正式模型',
    value: `${models.value.length} 条`
  }
])

watch(
  () => props.product?.id,
  async (productId) => {
    if (!productId) {
      models.value = []
      errorMessage.value = ''
      return
    }
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
  },
  { immediate: true }
)

function countByType(type: ProductModelType) {
  return models.value.filter((model) => model.modelType === type).length
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
.product-model-designer__exhibit-head,
.product-model-designer__exhibit-copy,
.product-model-designer__curation-strip-shell,
.product-model-designer__candidate-body,
.product-model-designer__formal-stage,
.product-model-designer__card,
.product-model-designer__curation-flow {
  display: grid;
  gap: 0.85rem;
}

.product-model-designer-workspace {
  gap: 0.95rem;
}

.product-model-designer__exhibit-head {
  padding: 1.4rem 1.34rem 1.22rem;
  gap: 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background:
    radial-gradient(circle at top left, rgba(237, 230, 221, 0.58), transparent 30%),
    linear-gradient(180deg, rgba(250, 248, 244, 0.7), rgba(255, 255, 255, 0) 42%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-form-surface);
}

.product-model-designer__eyebrow {
  display: inline-flex;
  width: max-content;
  padding-bottom: 0.38rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 20%, var(--panel-border));
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.product-model-designer__summary-label {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 60%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.product-model-designer__headline,
.product-model-designer__curation-strip-item strong,
.product-model-designer__card-heading strong,
.product-model-designer__curation-copy strong {
  margin: 0;
  color: var(--text-heading);
}

.product-model-designer__headline {
  font-size: clamp(1.7rem, 2.4vw, 2.34rem);
  line-height: 1.06;
  letter-spacing: -0.04em;
}

.product-model-designer__description,
.product-model-designer__formal-intro {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.product-model-designer__brief-statement {
  margin: 0;
  max-width: 38rem;
  color: var(--text-heading);
  font-size: 0.9rem;
  font-weight: 500;
  line-height: 1.7;
}

.product-model-designer__mode-switcher {
  display: inline-flex;
  width: max-content;
  padding: 0.24rem;
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
.product-model-designer__formal-overview-card--active {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background: linear-gradient(180deg, rgba(255, 249, 244, 0.98), rgba(255, 252, 248, 0.98));
}

.product-model-designer__curation-strip,
.product-model-designer__formal-overview {
  display: grid;
  gap: 0.85rem;
}

.product-model-designer__curation-strip-shell {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background:
    linear-gradient(180deg, rgba(248, 246, 242, 0.62), rgba(255, 255, 255, 0) 30%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__curation-strip-item {
  display: grid;
  gap: 0.4rem;
  align-content: start;
  padding: 1rem 1.08rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-model-designer__curation-strip-item:last-child {
  border-right: none;
}

.product-model-designer__curation-strip-item strong {
  font-size: 1rem;
}

.product-model-designer__workspace-shell {
  padding: 1rem 1.08rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__candidate-stage {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  align-items: start;
}

.product-model-designer__candidate-body,
.product-model-designer__formal-stage {
  padding: 1.04rem 1.08rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.98);
}

.product-model-designer__curation-board {
  display: grid;
  gap: 0.88rem;
  padding-top: 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(247, 244, 239, 0.48), rgba(255, 255, 255, 0) 22%),
    linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(255, 255, 255, 0.98));
}

.product-model-designer__curation-flow {
  list-style: none;
  margin: 0;
  padding: 0 1rem 0.1rem;
  gap: 0;
}

.product-model-designer__flow-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.96rem;
  align-items: start;
  padding: 0.96rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-model-designer__flow-item:last-child {
  border-bottom-color: transparent;
}

.product-model-designer__curation-index {
  color: color-mix(in srgb, var(--brand) 72%, var(--text-caption));
  font-size: 0.82rem;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: 0.06em;
}

.product-model-designer__curation-copy {
  display: grid;
  gap: 0.24rem;
}

.product-model-designer__curation-copy span {
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
}

.product-model-designer__formal-overview {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-model-designer__formal-overview-card {
  width: 100%;
  padding: 0.92rem 0.98rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.96);
  text-align: left;
}

.product-model-designer__card-header,
.product-model-designer__candidate-body-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.9rem;
}

.product-model-designer__candidate-body-intro {
  margin: 0.24rem 0 0;
  color: var(--text-secondary);
  line-height: 1.72;
}

.product-model-designer__formal-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
}

.product-model-designer__formal-stage-copy,
.product-model-designer__card-summary {
  display: grid;
  gap: 0.44rem;
}

.product-model-designer__card-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
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
  .product-model-designer__curation-strip-shell,
  .product-model-designer__formal-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .product-model-designer__candidate-body-header,
  .product-model-designer__card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-model-designer__curation-strip-shell,
  .product-model-designer__formal-overview {
    grid-template-columns: 1fr;
  }

  .product-model-designer__curation-strip-item {
    border-right: none;
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }

  .product-model-designer__curation-strip-item:first-child {
    border-top: none;
  }

  .product-model-designer__mode-switcher {
    display: grid;
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
