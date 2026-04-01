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
      <section class="product-model-designer__journal-head">
        <div class="product-model-designer__journal-copy">
          <span class="product-model-designer__journal-kicker">物模型治理</span>
          <h3 class="product-model-designer__journal-title">{{ designerStageTitle }}</h3>
          <p class="product-model-designer__journal-summary">{{ headerStatement }}</p>
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

      <section class="product-model-designer__journal-ruler">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="product-model-designer__journal-ruler-item"
        >
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
        </article>
      </section>

      <section v-if="designerMode === 'manual'" class="product-model-designer__governance-sheet">
        <div class="product-model-designer__sheet-head">
          <div class="product-model-designer__sheet-copy">
            <strong>手动提炼</strong>
            <p>围绕当前产品的单设备业务 / 状态 / 其他样本完成提炼、核对与写库确认。</p>
          </div>
          <StandardButton action="confirm" data-testid="confirm-model-candidates" @click="fullDesignerVisible = true">
            进入完整治理
          </StandardButton>
        </div>

        <ol class="product-model-designer__governance-steps">
          <li
            v-for="(item, index) in manualLedgerItems"
            :key="item.key"
            class="product-model-designer__governance-step"
          >
            <span class="product-model-designer__step-index">
              {{ String(index + 1).padStart(2, '0') }}
            </span>
            <div class="product-model-designer__step-copy">
              <strong>{{ item.title }}</strong>
              <span>{{ item.summary }}</span>
              <p>{{ item.description }}</p>
            </div>
          </li>
        </ol>
      </section>

      <section v-else class="product-model-designer__formal-sheet">
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
    return '正式模型维持当前契约总表，新增字段再回到手动提炼核对来源证据。'
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
.product-model-designer__journal-copy,
.product-model-designer__journal-ruler,
.product-model-designer__governance-sheet,
.product-model-designer__formal-sheet,
.product-model-designer__list,
.product-model-designer__card,
.product-model-designer__formal-stage-copy,
.product-model-designer__sheet-copy,
.product-model-designer__governance-steps,
.product-model-designer__step-copy {
  display: grid;
}

.product-model-designer-workspace {
  gap: 1rem;
}

.product-model-designer__journal-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem 1.4rem;
  align-items: end;
  padding-top: 0.86rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-model-designer__journal-copy {
  gap: 0.34rem;
}

.product-model-designer__journal-kicker {
  display: inline-flex;
  width: max-content;
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.product-model-designer__journal-title,
.product-model-designer__formal-title,
.product-model-designer__sheet-copy strong,
.product-model-designer__card-heading strong {
  margin: 0;
  color: var(--text-heading);
}

.product-model-designer__journal-title {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(1.62rem, 2.4vw, 2.2rem);
  line-height: 1.14;
}

.product-model-designer__journal-summary,
.product-model-designer__sheet-copy p,
.product-model-designer__formal-intro,
.product-model-designer__description,
.product-model-designer__empty p,
.product-model-designer__step-copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.product-model-designer__mode-switcher {
  display: inline-flex;
  gap: 0.4rem;
  padding-bottom: 0.12rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
}

.product-model-designer__mode-chip {
  padding: 0 0 0.46rem;
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
}

.product-model-designer__mode-chip--active {
  color: var(--brand);
}

.product-model-designer__journal-ruler {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid var(--panel-border);
  border-bottom: 1px solid var(--panel-border);
}

.product-model-designer__journal-ruler-item,
.product-model-designer__formal-overview-card {
  display: grid;
  gap: 0.3rem;
}

.product-model-designer__journal-ruler-item {
  min-width: 0;
  padding: 0.96rem 1rem 0.9rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-model-designer__journal-ruler-item:last-child {
  border-right: 0;
}

.product-model-designer__journal-ruler-item span,
.product-model-designer__step-copy span,
.product-model-designer__card-heading span,
.product-model-designer__card-summary,
.product-model-designer__empty strong,
.product-model-designer__formal-overview-card span,
.product-model-designer__formal-overview-card small {
  color: var(--text-secondary);
}

.product-model-designer__journal-ruler-item span,
.product-model-designer__step-copy span,
.product-model-designer__formal-overview-card span,
.product-model-designer__formal-overview-card small {
  font-size: 0.8rem;
  line-height: 1.6;
}

.product-model-designer__journal-ruler-item strong,
.product-model-designer__formal-overview-card strong,
.product-model-designer__step-copy strong {
  color: var(--text-heading);
  font-size: 1.08rem;
  line-height: 1.4;
}

.product-model-designer__governance-sheet,
.product-model-designer__formal-sheet {
  gap: 0.96rem;
  padding-top: 0.9rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-model-designer__sheet-head,
.product-model-designer__card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-model-designer__governance-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 0;
}

.product-model-designer__governance-step {
  display: grid;
  grid-template-columns: 2.4rem minmax(0, 1fr);
  gap: 0.94rem;
  padding: 0.96rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
}

.product-model-designer__governance-step:last-child {
  border-bottom: 0;
}

.product-model-designer__step-index {
  color: color-mix(in srgb, var(--brand) 72%, var(--text-caption));
  font-size: 0.82rem;
  font-weight: 700;
  line-height: 1.6;
  letter-spacing: 0.08em;
}

.product-model-designer__step-copy {
  gap: 0.22rem;
}

.product-model-designer__formal-title {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 1.1rem;
  line-height: 1.4;
}

.product-model-designer__formal-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.product-model-designer__formal-overview-card {
  width: 100%;
  padding: 0.94rem 1rem;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 22%, white), white);
  text-align: left;
}

.product-model-designer__formal-overview-card--active {
  border-color: color-mix(in srgb, var(--brand) 24%, var(--panel-border));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--brand) 10%, transparent);
}

.product-model-designer__list {
  gap: 0;
  border-top: 1px solid var(--panel-border);
}

.product-model-designer__card {
  gap: 0.44rem;
  padding: 1rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
}

.product-model-designer__card-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem 0.8rem;
  font-size: 0.84rem;
  line-height: 1.6;
}

.product-model-designer__empty {
  display: grid;
  gap: 0.4rem;
  padding: 1rem 0;
  border-top: 1px solid var(--panel-border);
}

@media (max-width: 900px) {
  .product-model-designer__journal-head {
    grid-template-columns: 1fr;
  }

  .product-model-designer__mode-switcher {
    width: max-content;
  }

  .product-model-designer__journal-ruler,
  .product-model-designer__formal-overview {
    grid-template-columns: 1fr;
  }

  .product-model-designer__journal-ruler-item {
    border-right: 0;
    border-top: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  }

  .product-model-designer__journal-ruler-item:first-child {
    border-top: 0;
  }
}

@media (max-width: 720px) {
  .product-model-designer__sheet-head,
  .product-model-designer__card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-model-designer__mode-switcher {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
