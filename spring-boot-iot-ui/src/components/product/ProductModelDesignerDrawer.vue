<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-model-designer-drawer"
    size="60rem"
    title="物模型完整治理"
    subtitle="围绕当前产品执行手动提炼、候选确认和正式模型回刷，不新增并行草稿入口。"
    :loading="loading && !hasLoadedContent"
    loading-text="正在加载产品物模型..."
    :error-message="errorMessage"
    :empty="!product"
    empty-text="请先选择产品，再进入完整治理。"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section v-if="product" class="detail-panel product-model-designer-drawer__header">
      <div class="product-model-designer-drawer__identity">
        <p class="product-model-designer-drawer__kicker">手动提炼</p>
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
          <span>候选条数</span>
          <strong>{{ allCandidates.length }}</strong>
        </div>
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__manual-stage">
      <div class="detail-section-header">
        <div>
          <h3>手动样本提炼</h3>
          <p>单次只支持一个设备样本。先粘贴 JSON，再把候选确认写入 `iot_product_model`。</p>
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
          placeholder="请输入当前产品下单设备样本 JSON。"
        />
      </div>

      <div class="product-model-designer-drawer__actions">
        <StandardButton
          action="confirm"
          :loading="manualExtractLoading"
          :disabled="!manualSamplePayload.trim()"
          data-testid="manual-extract-submit"
          @click="handleManualExtract"
        >
          提炼候选
        </StandardButton>
      </div>
    </section>

    <section v-if="product && extractionSummary" class="detail-panel product-model-designer-drawer__summary-stage">
      <div class="detail-section-header">
        <div>
          <h3>提炼摘要</h3>
          <p>当前只保留与你本次样本直接相关的结果。</p>
        </div>
      </div>

      <div class="product-model-designer-drawer__result-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">样本设备</span>
          <strong class="detail-summary-card__value">{{ extractionSummary.sampleDeviceCode || '--' }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">提炼模式</span>
          <strong class="detail-summary-card__value">{{ extractionSummary.extractionMode || 'manual' }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">属性候选</span>
          <strong class="detail-summary-card__value">{{ extractionSummary.propertyCandidateCount ?? 0 }}</strong>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">待人工确认</span>
          <strong class="detail-summary-card__value">{{ extractionSummary.needsReviewCount ?? 0 }}</strong>
        </article>
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__candidate-stage">
      <div class="detail-section-header">
        <div>
          <h3>候选确认</h3>
          <p>默认勾选本次提炼的全部候选，确认后直接写入正式模型。</p>
        </div>
      </div>

      <div v-if="allCandidates.length" class="product-model-designer-drawer__candidate-list">
        <article
          v-for="candidate in allCandidates"
          :key="candidateKey(candidate)"
          class="detail-card product-model-designer-drawer__candidate-card"
        >
          <div class="product-model-designer-drawer__candidate-select">
            <ElCheckbox
              :model-value="selectedCandidateKeys.includes(candidateKey(candidate))"
              @update:modelValue="toggleCandidate(candidate, $event)"
            />
          </div>

          <div class="product-model-designer-drawer__candidate-body">
            <div class="detail-card__header">
              <strong>{{ candidate.modelName }}</strong>
              <span class="product-model-designer-drawer__candidate-identifier">{{ candidate.identifier }}</span>
            </div>

            <div class="detail-card__meta">
              <span>{{ candidate.modelType }}</span>
              <span>{{ candidate.dataType || '--' }}</span>
              <span>{{ candidate.groupKey || '--' }}</span>
            </div>

            <p class="product-model-designer-drawer__candidate-description">
              {{ candidate.description?.trim() || '当前没有补充说明。' }}
            </p>
          </div>
        </article>
      </div>

      <div v-else class="detail-empty">
        当前还没有候选项，请先完成一次手动提炼。
      </div>
    </section>

    <section v-if="product" class="detail-panel product-model-designer-drawer__formal-stage">
      <div class="detail-section-header">
        <div>
          <h3>正式模型</h3>
          <p>确认写库后会回刷当前产品的正式模型列表。</p>
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
        confirm-text="确认写库"
        :confirm-loading="confirmLoading"
        :confirm-disabled="!selectedCandidateItems.length"
        @cancel="emit('update:modelValue', false)"
        @confirm="handleConfirm"
      />
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import { productApi } from '@/api/product'
import type {
  Product,
  ProductModel,
  ProductModelCandidate,
  ProductModelCandidateConfirmItem,
  ProductModelCandidateResult,
  ProductModelCandidateSummary,
  ProductModelManualSampleType
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

const sampleTypeOptions: Array<{ label: string; value: ProductModelManualSampleType }> = [
  { label: '业务数据', value: 'business' },
  { label: '状态数据', value: 'status' },
  { label: '其他数据', value: 'other' }
]

const loading = ref(false)
const manualExtractLoading = ref(false)
const confirmLoading = ref(false)
const errorMessage = ref('')
const manualSampleType = ref<ProductModelManualSampleType>('business')
const manualSamplePayload = ref('')
const models = ref<ProductModel[]>([])
const candidateResult = ref<ProductModelCandidateResult | null>(null)
const selectedCandidateKeys = ref<string[]>([])

const extractionSummary = computed<ProductModelCandidateSummary | null>(() => candidateResult.value?.summary ?? null)
const allCandidates = computed<ProductModelCandidate[]>(() => {
  if (!candidateResult.value) {
    return []
  }
  return [
    ...(candidateResult.value.propertyCandidates ?? []),
    ...(candidateResult.value.eventCandidates ?? []),
    ...(candidateResult.value.serviceCandidates ?? [])
  ]
})
const selectedCandidateItems = computed<ProductModelCandidateConfirmItem[]>(() =>
  allCandidates.value
    .filter((candidate) => selectedCandidateKeys.value.includes(candidateKey(candidate)))
    .map((candidate) => ({
      modelType: candidate.modelType,
      identifier: candidate.identifier,
      modelName: candidate.modelName,
      dataType: candidate.dataType ?? undefined,
      specsJson: candidate.specsJson ?? undefined,
      eventType: candidate.eventType ?? undefined,
      serviceInputJson: candidate.serviceInputJson ?? undefined,
      serviceOutputJson: candidate.serviceOutputJson ?? undefined,
      sortNo: candidate.sortNo ?? undefined,
      requiredFlag: candidate.requiredFlag ?? undefined,
      description: candidate.description ?? undefined
    }))
)
const hasLoadedContent = computed(() => Boolean(models.value.length || candidateResult.value))

watch(
  () => [props.modelValue, props.product?.id] as const,
  async ([visible, productId]) => {
    if (!visible || !productId) {
      if (!visible) {
        errorMessage.value = ''
      }
      return
    }
    await loadModels(productId)
  },
  { immediate: true }
)

function candidateKey(candidate: ProductModelCandidate) {
  return `${candidate.modelType}:${candidate.identifier}`
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

function toggleCandidate(candidate: ProductModelCandidate, checked: unknown) {
  const key = candidateKey(candidate)
  if (checked) {
    if (!selectedCandidateKeys.value.includes(key)) {
      selectedCandidateKeys.value = [...selectedCandidateKeys.value, key]
    }
    return
  }
  selectedCandidateKeys.value = selectedCandidateKeys.value.filter((item) => item !== key)
}

async function handleManualExtract() {
  if (!props.product?.id || !manualSamplePayload.value.trim()) {
    return
  }
  manualExtractLoading.value = true
  errorMessage.value = ''
  try {
    const response = await productApi.manualExtractProductModelCandidates(props.product.id, {
      sampleType: manualSampleType.value,
      samplePayload: manualSamplePayload.value.trim()
    })
    candidateResult.value = response.data
    selectedCandidateKeys.value = allCandidates.value.map((candidate) => candidateKey(candidate))
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '手动提炼失败'
    candidateResult.value = null
    selectedCandidateKeys.value = []
  } finally {
    manualExtractLoading.value = false
  }
}

async function handleConfirm() {
  if (!props.product?.id || !selectedCandidateItems.value.length) {
    return
  }
  confirmLoading.value = true
  errorMessage.value = ''
  try {
    await productApi.confirmProductModelCandidates(props.product.id, {
      items: selectedCandidateItems.value
    })
    ElMessage.success('确认写库成功')
    candidateResult.value = null
    selectedCandidateKeys.value = []
    manualSamplePayload.value = ''
    await loadModels(props.product.id)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '确认写库失败'
    ElMessage.error(errorMessage.value)
  } finally {
    confirmLoading.value = false
  }
}
</script>

<style scoped>
.product-model-designer-drawer__header,
.product-model-designer-drawer__manual-stage,
.product-model-designer-drawer__summary-stage,
.product-model-designer-drawer__candidate-stage,
.product-model-designer-drawer__formal-stage {
  display: grid;
  gap: 1rem;
}

.product-model-designer-drawer__header {
  grid-template-columns: minmax(0, 1fr) 14rem;
  align-items: start;
}

.product-model-designer-drawer__identity,
.product-model-designer-drawer__summary {
  display: grid;
  gap: 0.72rem;
}

.product-model-designer-drawer__kicker,
.product-model-designer-drawer__summary-item span,
.product-model-designer-drawer__candidate-identifier {
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
.product-model-designer-drawer__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.58rem;
}

.product-model-designer-drawer__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 1.92rem;
  padding: 0.24rem 0.72rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.product-model-designer-drawer__summary-item {
  display: grid;
  gap: 0.24rem;
  padding: 0.88rem 0.94rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.95);
}

.product-model-designer-drawer__summary-item strong {
  color: var(--text-heading);
  font-size: 1.14rem;
  line-height: 1.3;
}

.product-model-designer-drawer__sample-type {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.58rem;
}

.product-model-designer-drawer__sample-type-button {
  min-height: 2.2rem;
  padding: 0.4rem 0.94rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-secondary);
  font-size: 0.88rem;
  cursor: pointer;
}

.product-model-designer-drawer__sample-type-button--active {
  border-color: color-mix(in srgb, var(--brand) 24%, white);
  background: linear-gradient(180deg, rgba(255, 249, 244, 0.98), rgba(255, 245, 236, 0.98));
  color: var(--brand);
}

.product-model-designer-drawer__sample-input {
  min-width: 0;
}

.product-model-designer-drawer__sample-input :deep(textarea) {
  min-height: 14rem;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  line-height: 1.65;
}

.product-model-designer-drawer__result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.product-model-designer-drawer__candidate-list,
.product-model-designer-drawer__formal-list {
  display: grid;
  gap: 0.84rem;
}

.product-model-designer-drawer__candidate-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.82rem;
  align-items: start;
}

.product-model-designer-drawer__candidate-select {
  padding-top: 0.12rem;
}

.product-model-designer-drawer__candidate-body {
  min-width: 0;
}

.product-model-designer-drawer__candidate-description {
  margin: 0.66rem 0 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.68;
}

@media (max-width: 900px) {
  .product-model-designer-drawer__header,
  .product-model-designer-drawer__result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
