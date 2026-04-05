<template>
  <div class="product-detail-workbench">
    <section class="product-detail-workbench__lead-sheet" data-testid="product-detail-lead-sheet">
      <section class="product-detail-workbench__scale-ledger" data-testid="product-detail-scale-ledger">
        <article
          v-for="metric in scaleMetrics"
          :key="metric.key"
          class="product-detail-workbench__scale-note"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </section>
    </section>

    <section class="product-detail-workbench__archive-sheet" data-testid="product-detail-archive-sheet">
      <header class="product-detail-workbench__section-head">
        <strong class="product-detail-workbench__section-title">档案摘要</strong>
      </header>
      <div class="product-detail-workbench__archive-list">
        <article
          v-for="item in archiveSummaryItems"
          :key="item.key"
          class="product-detail-workbench__archive-note"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
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
.product-detail-workbench__scale-ledger,
.product-detail-workbench__archive-sheet,
.product-detail-workbench__archive-list {
  display: grid;
}

.product-detail-workbench {
  gap: 1.45rem;
}

.product-detail-workbench__lead-sheet {
  padding-top: 0.88rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-detail-workbench__scale-note span,
.product-detail-workbench__archive-note span {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.product-detail-workbench__scale-ledger {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
}

.product-detail-workbench__scale-note {
  display: grid;
  gap: 0.36rem;
  min-width: 0;
  padding: 0.38rem 1rem 0.72rem 0;
  border-right: 1px solid var(--panel-border);
}

.product-detail-workbench__scale-note strong,
.product-detail-workbench__section-title,
.product-detail-workbench__archive-note strong {
  color: var(--text-heading);
  font-size: 1.1rem;
  line-height: 1.34;
}

.product-detail-workbench__archive-sheet {
  gap: 0.72rem;
  padding-top: 0.86rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-detail-workbench__section-head {
  display: grid;
  justify-items: start;
}

.product-detail-workbench__section-title {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 1.08rem;
}

.product-detail-workbench__archive-note {
  display: grid;
  gap: 0.26rem;
  min-width: 0;
  padding: 0.82rem 1rem 0.82rem 0;
  border-right: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
}

.product-detail-workbench__archive-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
}

@media (max-width: 1024px) {
  .product-detail-workbench__scale-note {
    padding-right: 0.82rem;
  }

  .product-detail-workbench__archive-list {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__archive-note {
    padding-right: 0;
    border-right: 0;
    border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  }
}

@media (max-width: 720px) {
  .product-detail-workbench__scale-ledger {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__scale-note {
    padding-right: 0;
    border-right: 0;
    border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  }
}
</style>
