<template>
  <div class="product-detail-workbench">
    <section class="product-detail-workbench__metrics-row" data-testid="product-detail-metrics-row">
      <article
        v-for="metric in scaleMetrics"
        :key="metric.key"
        class="product-detail-workbench__metric-card"
      >
        <span class="product-detail-workbench__copy-label product-detail-workbench__copy-label--metric">{{ metric.label }}</span>
        <strong class="product-detail-workbench__copy-value product-detail-workbench__copy-value--metric">{{ metric.value }}</strong>
      </article>
    </section>

    <section class="product-detail-workbench__archive-card" data-testid="product-detail-archive-card">
      <header class="product-detail-workbench__archive-header">
        <strong class="product-detail-workbench__archive-title product-detail-workbench__copy-label">档案摘要</strong>
      </header>
      <div class="product-detail-workbench__archive-grid">
        <article
          v-for="item in archiveSummaryItems"
          :key="item.key"
          class="product-detail-workbench__archive-item"
          :class="{
            'product-detail-workbench__archive-item--description': item.key === 'description'
          }"
        >
          <span class="product-detail-workbench__copy-label">{{ item.label }}</span>
          <strong
            class="product-detail-workbench__copy-value product-detail-workbench__copy-value--body"
            :data-testid="item.key === 'description' ? 'product-detail-archive-description' : undefined"
          >
            {{ item.value }}
          </strong>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Product } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const props = defineProps<{
  product: Product
}>()

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function toDisplayDuration(value?: number | null) {
  return value != null ? `${value} 分钟` : '--'
}

function toDisplayPercent(numerator?: number | null, denominator?: number | null) {
  if (numerator == null || denominator == null || denominator <= 0) {
    return '--'
  }
  return `${Math.round((numerator / denominator) * 100)}%`
}

const product = computed(() => props.product)
const onlineCoverageText = computed(() =>
  toDisplayPercent(product.value.onlineDeviceCount, product.value.deviceCount)
)
const scaleMetrics = computed(() => [
  {
    key: 'onlineCoverage',
    label: '在线覆盖',
    value: onlineCoverageText.value
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: toDisplayText(product.value.thirtyDaysActiveCount)
  },
  {
    key: 'avgOnlineDuration',
    label: '平均在线',
    value: toDisplayDuration(product.value.avgOnlineDuration)
  }
])
const archiveSummaryItems = computed(() => [
  {
    key: 'manufacturer',
    label: '厂商',
    value: toDisplayText(product.value.manufacturer)
  },
  {
    key: 'updateTime',
    label: '最近更新',
    value: formatDateTime(product.value.updateTime)
  },
  {
    key: 'description',
    label: '补充说明',
    value: product.value.description?.trim() || '当前没有补充说明'
  }
])
</script>

<style scoped>
.product-detail-workbench,
.product-detail-workbench__metrics-row,
.product-detail-workbench__archive-card,
.product-detail-workbench__archive-grid,
.product-detail-workbench__metric-card,
.product-detail-workbench__archive-item {
  display: grid;
}

.product-detail-workbench {
  gap: 0.96rem;
}

.product-detail-workbench__metrics-row {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.product-detail-workbench__metric-card,
.product-detail-workbench__archive-card {
  border: 1px solid color-mix(in srgb, var(--brand) 9%, var(--panel-border));
  background: linear-gradient(
    160deg,
    color-mix(in srgb, var(--brand-light) 16%, white) 0%,
    rgba(255, 255, 255, 0.98) 100%
  );
  box-shadow: 0 8px 20px -20px color-mix(in srgb, var(--brand) 42%, transparent);
}

.product-detail-workbench__metric-card {
  gap: 0.3rem;
  min-width: 0;
  padding: 0.72rem 0.84rem;
}

.product-detail-workbench__copy-label {
  color: var(--text-secondary);
  font-size: 0.76rem;
  line-height: 1.5;
}

.product-detail-workbench__copy-label--metric {
  color: var(--text-caption);
  letter-spacing: 0.01em;
}

.product-detail-workbench__copy-value {
  color: var(--text-heading);
  font-size: 0.96rem;
  line-height: 1.38;
}

.product-detail-workbench__copy-value--metric {
  font-size: 1rem;
  line-height: 1.28;
}

.product-detail-workbench__copy-value--body {
  font-size: 0.94rem;
}

.product-detail-workbench__archive-card {
  gap: 0.72rem;
  padding: 0.82rem 0.88rem;
}

.product-detail-workbench__archive-header {
  display: grid;
  justify-items: start;
}

.product-detail-workbench__archive-title {
  font-weight: 600;
}

.product-detail-workbench__archive-grid {
  grid-template-columns: repeat(2, minmax(180px, max-content)) minmax(220px, 1fr);
  gap: 0.72rem;
  align-items: center;
}

.product-detail-workbench__archive-item {
  gap: 0.2rem;
  min-width: 0;
}

.product-detail-workbench__archive-item--description {
  justify-self: stretch;
}

.product-detail-workbench__archive-item--description strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .product-detail-workbench__archive-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-detail-workbench__archive-item--description {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .product-detail-workbench__metrics-row,
  .product-detail-workbench__archive-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__archive-item--description {
    grid-column: auto;
  }
}
</style>
