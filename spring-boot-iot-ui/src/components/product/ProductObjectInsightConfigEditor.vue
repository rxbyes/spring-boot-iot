<template>
  <section class="ops-drawer-section product-object-insight-config-editor">
    <div class="ops-drawer-section__header product-object-insight-config-editor__header">
      <div class="product-object-insight-config-editor__summary">
        <div class="product-object-insight-config-editor__title-row">
          <h3>对象洞察配置</h3>
          <el-tag type="info" effect="plain">{{ countLabel }}</el-tag>
        </div>
        <p>
          产品级正式配置会在设备没有独立对象洞察配置时生效，用于维护单设备分析最关心的监测值、状态值和扩展分析文案。
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
      仅面向单设备对象洞察配置使用；趋势图会按“监测数据 / 状态数据”分组展示，后续可继续扩展湿度、4G 信号、电量等状态参数。
    </el-alert>

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
              @update:model-value="(value) => updateMetric(index, { group: value === 'measure' ? 'measure' : 'status' })"
            >
              <el-option label="监测数据" value="measure" />
              <el-option label="状态数据" value="status" />
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
import type { ProductObjectInsightCustomMetricConfig } from '@/types/api'
import {
  MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS,
  createEmptyProductObjectInsightMetric
} from '@/utils/productObjectInsightConfig'

const props = defineProps<{
  modelValue: ProductObjectInsightCustomMetricConfig[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: ProductObjectInsightCustomMetricConfig[]): void
}>()

const normalizedMetrics = computed(() =>
  props.modelValue.map((metric) => ({
    ...createEmptyProductObjectInsightMetric(),
    ...metric
  }))
)

const countLabel = computed(
  () => `已配置 ${props.modelValue.length}/${MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS} 项`
)

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
</script>

<style scoped>
.product-object-insight-config-editor {
  display: grid;
  gap: 1rem;
}

.product-object-insight-config-editor__header,
.product-object-insight-config-editor__title-row,
.product-object-insight-config-editor__metric-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-object-insight-config-editor__summary,
.product-object-insight-config-editor__list,
.product-object-insight-config-editor__metric-copy {
  display: grid;
  gap: 0.4rem;
}

.product-object-insight-config-editor__summary p,
.product-object-insight-config-editor__metric-copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.product-object-insight-config-editor__notice {
  margin: 0;
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
  .product-object-insight-config-editor__title-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
