<template>
  <section class="ops-drawer-section product-object-insight-config-editor">
    <div class="ops-drawer-section__header product-object-insight-config-editor__header">
      <div class="product-object-insight-config-editor__summary">
        <div class="product-object-insight-config-editor__title-row">
          <h3>对象洞察配置</h3>
          <el-tag type="info" effect="plain">{{ countLabel }}</el-tag>
        </div>
        <p>
          产品级正式配置会在设备没有独立对象洞察配置时生效，用于维护单设备分析最关心的监测数据、状态事件、运行参数和扩展分析文案。
        </p>
      </div>
      <StandardButton
        data-testid="product-object-insight-add"
        action="add"
        :disabled="modelValue.length >= MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS"
        @click="handleAdd"
      >
        新增指标
      </StandardButton>
    </div>

    <el-alert type="info" :closable="false" show-icon class="product-object-insight-config-editor__notice">
      仅面向单设备对象洞察配置使用；趋势图会按“监测数据 / 状态事件 / 运行参数”分组展示，旧版双分组配置会自动归并到对应分组。
    </el-alert>

    <section v-if="availablePropertyModels.length" class="product-object-insight-config-editor__candidate-section">
      <header class="product-object-insight-config-editor__candidate-head">
        <div>
          <h4>从正式字段快速加入</h4>
          <p>只展示当前已生效的属性字段，可直接设为对象洞察趋势重点指标。</p>
        </div>
      </header>

      <div class="product-object-insight-config-editor__candidate-list">
        <article
          v-for="model in availablePropertyModels"
          :key="model.identifier"
          class="product-object-insight-config-editor__candidate-card"
        >
          <div class="product-object-insight-config-editor__candidate-copy">
            <strong>{{ model.modelName }}</strong>
            <p>{{ model.identifier }}</p>
          </div>
          <div class="product-object-insight-config-editor__candidate-actions">
            <span class="product-object-insight-config-editor__candidate-state">
              {{ resolveMetricStateLabel(model.identifier) }}
            </span>
            <StandardButton
              :data-testid="`product-object-insight-add-measure-${model.identifier}`"
              action="query"
              link
              :disabled="isQuickAddDisabled(model.identifier)"
              @click="handleQuickAdd(model, 'measure')"
            >
              设为监测数据
            </StandardButton>
            <StandardButton
              :data-testid="`product-object-insight-add-status-event-${model.identifier}`"
              action="query"
              link
              :disabled="isQuickAddDisabled(model.identifier)"
              @click="handleQuickAdd(model, 'statusEvent')"
            >
              设为状态事件
            </StandardButton>
            <StandardButton
              :data-testid="`product-object-insight-add-runtime-${model.identifier}`"
              action="query"
              link
              :disabled="isQuickAddDisabled(model.identifier)"
              @click="handleQuickAdd(model, 'runtime')"
            >
              设为运行参数
            </StandardButton>
            <StandardButton
              v-if="hasMetric(model.identifier)"
              :data-testid="`product-object-insight-remove-identifier-${model.identifier}`"
              action="delete"
              link
              @click="handleQuickRemove(model.identifier)"
            >
              取消趋势
            </StandardButton>
          </div>
        </article>
      </div>
    </section>

    <div v-if="normalizedMetrics.length" class="product-object-insight-config-editor__list">
      <article
        v-for="(metric, index) in normalizedMetrics"
        :key="`${metric.identifier || 'metric'}-${index}`"
        class="product-object-insight-config-editor__metric-card"
      >
        <header class="product-object-insight-config-editor__metric-head">
          <div class="product-object-insight-config-editor__metric-copy">
            <strong>{{ metric.displayName || `自定义指标 ${index + 1}` }}</strong>
            <p>{{ metric.identifier || '请填写指标标识，例如 S1_ZT_1.humidity' }}</p>
          </div>
          <StandardButton
            :data-testid="`product-object-insight-remove-${index}`"
            action="delete"
            link
            @click="handleRemove(index)"
          >
            移除
          </StandardButton>
        </header>

        <div class="ops-drawer-grid">
          <el-form-item label="指标标识">
            <el-input
              :model-value="metric.identifier"
              placeholder="例如 S1_ZT_1.humidity"
              @update:model-value="(value) => updateMetric(index, { identifier: normalizeText(value) })"
            />
          </el-form-item>

          <el-form-item label="中文名称">
            <el-input
              :model-value="metric.displayName"
              placeholder="例如 相对湿度"
              @update:model-value="(value) => updateMetric(index, { displayName: normalizeText(value) })"
            />
          </el-form-item>

          <el-form-item label="指标分组">
            <el-select
              :model-value="metric.group"
              placeholder="请选择指标分组"
              @update:model-value="(value) => updateMetric(index, { group: normalizeMetricGroup(value) })"
            >
              <el-option
                v-for="option in metricGroupOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="排序">
            <el-input-number
              :model-value="metric.sortNo ?? 10"
              :min="0"
              :max="999"
              @update:model-value="(value) => updateMetric(index, { sortNo: normalizeSortNo(value) })"
            />
          </el-form-item>

          <el-form-item label="计入趋势">
            <el-switch
              :model-value="metric.includeInTrend !== false"
              @update:model-value="(value) => updateMetric(index, { includeInTrend: Boolean(value) })"
            />
          </el-form-item>

          <el-form-item label="计入扩展参数">
            <el-switch
              :model-value="metric.includeInExtension !== false"
              @update:model-value="(value) => updateMetric(index, { includeInExtension: Boolean(value) })"
            />
          </el-form-item>

          <el-form-item label="启用">
            <el-switch
              :model-value="metric.enabled !== false"
              @update:model-value="(value) => updateMetric(index, { enabled: Boolean(value) })"
            />
          </el-form-item>

          <el-form-item label="分析标题">
            <el-input
              :model-value="metric.analysisTitle"
              placeholder="例如 现场环境补充"
              @update:model-value="(value) => updateMetric(index, { analysisTitle: normalizeText(value) })"
            />
          </el-form-item>

          <el-form-item label="分析标签">
            <el-input
              :model-value="metric.analysisTag"
              placeholder="例如 系统自定义参数"
              @update:model-value="(value) => updateMetric(index, { analysisTag: normalizeText(value) })"
            />
          </el-form-item>

          <el-form-item label="分析描述模板" class="ops-drawer-grid__full">
            <el-input
              :model-value="metric.analysisTemplate"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="支持 {{label}}、{{value}} 等占位符，例如 {{label}}当前为{{value}}。"
              @update:model-value="(value) => updateMetric(index, { analysisTemplate: normalizeText(value) })"
            />
          </el-form-item>
        </div>
      </article>
    </div>

    <div v-else class="product-object-insight-config-editor__empty">
      暂未配置产品级对象洞察指标，可按湿度、4G 信号、电量等扩展状态参数补充分析内容。
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import type {
  ProductModel,
  ProductObjectInsightCustomMetricConfig,
  ProductObjectInsightMetricGroup
} from '@/types/api'
import {
  OBJECT_INSIGHT_METRIC_GROUP_OPTIONS,
  getObjectInsightMetricGroupLabel,
  normalizeObjectInsightMetricGroup
} from '@/utils/objectInsightMetricGroup'
import {
  MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS,
  createEmptyProductObjectInsightMetric,
  createProductObjectInsightMetricFromModel,
  findProductObjectInsightMetric,
  removeProductObjectInsightMetric,
  upsertProductObjectInsightMetric
} from '@/utils/productObjectInsightConfig'

const props = withDefaults(
  defineProps<{
    modelValue: ProductObjectInsightCustomMetricConfig[]
    availableModels?: ProductModel[]
  }>(),
  {
    availableModels: () => []
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: ProductObjectInsightCustomMetricConfig[]): void
}>()

const normalizedMetrics = computed(() =>
  props.modelValue.map((metric) => ({
    ...createEmptyProductObjectInsightMetric(),
    ...metric
  }))
)
const availablePropertyModels = computed(() =>
  (props.availableModels ?? []).filter(
    (item) => item.modelType === 'property' && normalizeText(item.identifier) && normalizeText(item.modelName)
  )
)

const countLabel = computed(
  () => `已配置 ${props.modelValue.length}/${MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS} 项`
)
const metricGroupOptions = OBJECT_INSIGHT_METRIC_GROUP_OPTIONS

function handleAdd() {
  if (props.modelValue.length >= MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS) {
    return
  }
  emit('update:modelValue', [...normalizedMetrics.value, createEmptyProductObjectInsightMetric()])
}

function handleRemove(index: number) {
  emit(
    'update:modelValue',
    normalizedMetrics.value.filter((_, rowIndex) => rowIndex !== index)
  )
}

function handleQuickAdd(model: ProductModel, group: ProductObjectInsightMetricGroup) {
  const existingCount = normalizedMetrics.value.length
  const identifierExists = hasMetric(model.identifier)
  if (!identifierExists && existingCount >= MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS) {
    return
  }
  emit(
    'update:modelValue',
    upsertProductObjectInsightMetric(
      normalizedMetrics.value,
      createProductObjectInsightMetricFromModel(model, group)
    )
  )
}

function handleQuickRemove(identifier: string) {
  emit('update:modelValue', removeProductObjectInsightMetric(normalizedMetrics.value, identifier))
}

function updateMetric(index: number, patch: Partial<ProductObjectInsightCustomMetricConfig>) {
  const nextMetrics = normalizedMetrics.value.map((metric, rowIndex) =>
    rowIndex === index
      ? {
          ...metric,
          ...patch
        }
      : metric
  )
  emit('update:modelValue', nextMetrics)
}

function normalizeText(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeSortNo(value: unknown) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 10
}

function normalizeMetricGroup(value: unknown) {
  return normalizeObjectInsightMetricGroup(value)
}

function hasMetric(identifier: string) {
  return Boolean(findProductObjectInsightMetric(normalizedMetrics.value, identifier))
}

function isQuickAddDisabled(identifier: string) {
  return !hasMetric(identifier) && normalizedMetrics.value.length >= MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS
}

function resolveMetricStateLabel(identifier: string) {
  const metric = findProductObjectInsightMetric(normalizedMetrics.value, identifier)
  if (!metric) {
    return '当前未加入趋势'
  }
  return `当前为${getObjectInsightMetricGroupLabel(metric.group)}`
}
</script>

<style scoped>
.product-object-insight-config-editor {
  display: grid;
  gap: 1rem;
}

.product-object-insight-config-editor__header,
.product-object-insight-config-editor__title-row,
.product-object-insight-config-editor__metric-head,
.product-object-insight-config-editor__candidate-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-object-insight-config-editor__summary,
.product-object-insight-config-editor__list,
.product-object-insight-config-editor__metric-copy,
.product-object-insight-config-editor__candidate-section,
.product-object-insight-config-editor__candidate-copy,
.product-object-insight-config-editor__candidate-list {
  display: grid;
  gap: 0.4rem;
}

.product-object-insight-config-editor__summary p,
.product-object-insight-config-editor__metric-copy p,
.product-object-insight-config-editor__candidate-copy p,
.product-object-insight-config-editor__candidate-head p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.product-object-insight-config-editor__candidate-head h4 {
  margin: 0;
  color: var(--text-primary);
}

.product-object-insight-config-editor__notice {
  margin: 0;
}

.product-object-insight-config-editor__candidate-section {
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 252, 244, 0.92), rgba(255, 255, 255, 0.98));
}

.product-object-insight-config-editor__candidate-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.88rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.94);
}

.product-object-insight-config-editor__candidate-copy strong {
  color: var(--text-primary);
}

.product-object-insight-config-editor__candidate-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem 0.75rem;
  align-items: center;
}

.product-object-insight-config-editor__candidate-state {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.5;
}

.product-object-insight-config-editor__list {
  gap: 1rem;
}

.product-object-insight-config-editor__metric-card,
.product-object-insight-config-editor__empty {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95));
}

.product-object-insight-config-editor__metric-card {
  padding: 1rem;
}

.product-object-insight-config-editor__metric-copy strong {
  color: var(--text-primary);
}

.product-object-insight-config-editor__empty {
  padding: 1rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 768px) {
  .product-object-insight-config-editor__header,
  .product-object-insight-config-editor__metric-head,
  .product-object-insight-config-editor__title-row,
  .product-object-insight-config-editor__candidate-head,
  .product-object-insight-config-editor__candidate-card {
    flex-direction: column;
    align-items: stretch;
  }

  .product-object-insight-config-editor__candidate-actions {
    justify-content: flex-start;
  }
}
</style>
