<template>
  <div class="product-detail-workbench">
    <section class="product-detail-workbench__lead-sheet" data-testid="product-detail-lead-sheet">
      <div class="product-detail-workbench__lead-main">
        <span class="product-detail-workbench__section-kicker">核心规模</span>
        <strong class="product-detail-workbench__lead-value" data-testid="product-detail-primary-metric">
          {{ primaryMetricValue }}
        </strong>
        <span class="product-detail-workbench__lead-caption">{{ primaryMetricCaption }}</span>
      </div>

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

    <section class="product-detail-workbench__journal-grid">
      <section class="product-detail-workbench__contract-sheet" data-testid="product-detail-contract-sheet">
        <header class="product-detail-workbench__section-head">
          <strong class="product-detail-workbench__section-title">契约基线</strong>
        </header>
        <div class="product-detail-workbench__contract-table">
          <article
            v-for="item in contractBaselineRows"
            :key="item.key"
            class="product-detail-workbench__contract-line"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
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

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  return '--'
}

const product = computed(() => props.product)
const primaryMetricValue = computed(() => toDisplayText(product.value.deviceCount))
const primaryMetricCaption = computed(() => (product.value.status === 0 ? '历史关联设备总量' : '关联设备总量'))
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
const contractBaselineRows = computed(() => [
  {
    key: 'protocolCode',
    label: '接入协议',
    value: toDisplayText(product.value.protocolCode)
  },
  {
    key: 'nodeType',
    label: '节点类型',
    value: getNodeTypeText(product.value.nodeType)
  },
  {
    key: 'dataFormat',
    label: '数据格式',
    value: toDisplayText(product.value.dataFormat)
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
.product-detail-workbench__lead-main,
.product-detail-workbench__scale-ledger,
.product-detail-workbench__contract-sheet,
.product-detail-workbench__archive-sheet,
.product-detail-workbench__archive-list {
  display: grid;
}

.product-detail-workbench {
  gap: 1.45rem;
}

.product-detail-workbench__lead-sheet {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(18rem, 0.9fr);
  gap: 1.8rem;
  align-items: start;
  padding-top: 0.88rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-detail-workbench__lead-main {
  gap: 0.36rem;
  padding-right: 1.6rem;
  border-right: 1px solid var(--panel-border);
}

.product-detail-workbench__section-kicker,
.product-detail-workbench__scale-note span,
.product-detail-workbench__contract-line span,
.product-detail-workbench__archive-note span {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.product-detail-workbench__section-kicker {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-detail-workbench__lead-value {
  color: var(--text-heading);
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(3rem, 4.1vw, 4.2rem);
  font-weight: 700;
  line-height: 1;
}

.product-detail-workbench__lead-caption {
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.56;
}

.product-detail-workbench__scale-ledger {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  align-content: start;
}

.product-detail-workbench__scale-note {
  display: grid;
  gap: 0.36rem;
  min-height: 5.4rem;
  padding: 0.38rem 0 0.72rem 1rem;
  border-left: 1px solid var(--panel-border);
}

.product-detail-workbench__scale-note strong,
.product-detail-workbench__section-title,
.product-detail-workbench__contract-line strong,
.product-detail-workbench__archive-note strong {
  color: var(--text-heading);
  font-size: 1.1rem;
  line-height: 1.34;
}

.product-detail-workbench__journal-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(0, 1.04fr);
  gap: 2rem;
}

.product-detail-workbench__contract-sheet,
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

.product-detail-workbench__contract-table {
  display: grid;
}

.product-detail-workbench__contract-line,
.product-detail-workbench__archive-note {
  display: grid;
  gap: 0.26rem;
  padding: 0.82rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
}

.product-detail-workbench__contract-line {
  grid-template-columns: 5.4rem minmax(0, 1fr);
  align-items: center;
  gap: 1rem;
}

.product-detail-workbench__contract-line strong {
  text-align: right;
}

.product-detail-workbench__archive-list {
  gap: 0;
}

@media (max-width: 1024px) {
  .product-detail-workbench__lead-sheet,
  .product-detail-workbench__journal-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__lead-main {
    padding-right: 0;
    padding-bottom: 0.9rem;
    border-right: 0;
    border-bottom: 1px solid var(--panel-border);
  }

  .product-detail-workbench__scale-note {
    min-height: auto;
    padding-left: 0.82rem;
  }
}

@media (max-width: 720px) {
  .product-detail-workbench__scale-ledger {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__scale-note {
    padding-left: 0;
    border-left: 0;
    border-bottom: 1px solid color-mix(in srgb, var(--brand) 7%, var(--panel-border));
  }
}
</style>
