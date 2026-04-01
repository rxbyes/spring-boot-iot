<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__hero-plinth" data-testid="product-detail-hero-plinth">
      <div class="product-detail-workbench__hero-main">
        <span class="product-detail-workbench__hero-label">核心规模</span>
        <div class="product-detail-workbench__hero-value-block">
          <strong class="product-detail-workbench__hero-value" data-testid="product-detail-primary-metric">
            {{ primaryMetricValue }}
          </strong>
          <span class="product-detail-workbench__hero-caption">{{ primaryMetricCaption }}</span>
        </div>
      </div>

      <section class="product-detail-workbench__metric-ribbon" data-testid="product-detail-metric-ribbon">
        <article
          v-for="metric in scaleMetrics"
          :key="metric.key"
          class="product-detail-workbench__scale-metric"
        >
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </section>
    </section>

    <section class="product-detail-workbench__brief-stage" data-testid="product-detail-brief-stage">
      <div class="detail-panel product-detail-workbench__exhibit-sheet">
        <section class="product-detail-workbench__contract-ledger" data-testid="product-detail-contract-ledger">
          <header class="product-detail-workbench__section-head">
            <strong class="product-detail-workbench__section-title">契约基线</strong>
          </header>
          <div class="product-detail-workbench__contract-table">
            <article
              v-for="item in contractBaselineRows"
              :key="item.key"
              class="product-detail-workbench__contract-row"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </section>

        <div class="product-detail-workbench__brief-divider" aria-hidden="true" />

        <section class="product-detail-workbench__archive-notes" data-testid="product-detail-archive-notes">
          <header class="product-detail-workbench__section-head">
            <strong class="product-detail-workbench__section-title">档案摘要</strong>
          </header>
          <div class="product-detail-workbench__archive-grid">
            <article
              v-for="item in archiveSummaryItems"
              :key="item.key"
              class="product-detail-workbench__archive-note-card"
              :class="{ 'product-detail-workbench__archive-note-card--wide': item.wide }"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </section>
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
    key: 'contractBaseline',
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
    value: toDisplayText(product.value.manufacturer),
    wide: false
  },
  {
    key: 'updateTime',
    label: '最近更新',
    value: formatDateTime(product.value.updateTime),
    wide: false
  },
  {
    key: 'description',
    label: '补充说明',
    value: product.value.description?.trim() || '当前没有补充说明',
    wide: true
  }
])
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1.32rem;
}

.product-detail-workbench__hero-plinth,
.product-detail-workbench__brief-stage,
.product-detail-workbench__exhibit-sheet,
.product-detail-workbench__contract-ledger,
.product-detail-workbench__archive-notes {
  display: grid;
  gap: 0.82rem;
}

.product-detail-workbench__hero-plinth {
  gap: 1.18rem;
  padding: 1.9rem 1.72rem 1.46rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background:
    radial-gradient(circle at top left, rgba(238, 230, 221, 0.6), transparent 28%),
    linear-gradient(180deg, rgba(250, 248, 244, 0.72), rgba(255, 255, 255, 0) 42%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-form-surface);
}

.product-detail-workbench__hero-main {
  display: grid;
  gap: 0.5rem;
}

.product-detail-workbench__hero-value-block {
  display: grid;
  gap: 0.42rem;
}

.product-detail-workbench__hero-label,
.product-detail-workbench__scale-metric span,
.product-detail-workbench__contract-row span,
.product-detail-workbench__archive-note-card span {
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
}

.product-detail-workbench__hero-value {
  color: var(--text-heading);
  font-size: clamp(3rem, 4vw, 4rem);
  line-height: 1;
  letter-spacing: -0.06em;
}

.product-detail-workbench__hero-caption {
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.48;
}

.product-detail-workbench__metric-ribbon {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  padding-top: 1.14rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__scale-metric {
  display: grid;
  gap: 0.28rem;
  padding: 0.08rem 1rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__scale-metric:last-child {
  border-right: none;
}

.product-detail-workbench__scale-metric strong,
.product-detail-workbench__section-title,
.product-detail-workbench__contract-row strong,
.product-detail-workbench__archive-note-card strong {
  color: var(--text-heading);
  font-size: 1.04rem;
  line-height: 1.28;
}

.product-detail-workbench__brief-stage {
  gap: 0;
}

.product-detail-workbench__exhibit-sheet {
  gap: 1.12rem;
  padding: 1.18rem 1.26rem 1.3rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background:
    linear-gradient(180deg, rgba(248, 246, 242, 0.64), rgba(255, 255, 255, 0) 30%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-detail-workbench__section-head {
  display: grid;
  justify-items: start;
  gap: 0.24rem;
}

.product-detail-workbench__section-title {
  font-size: 1rem;
}

.product-detail-workbench__brief-divider {
  height: 1px;
  background: color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__contract-table {
  display: grid;
  border-top: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
}

.product-detail-workbench__contract-row {
  display: grid;
  grid-template-columns: 5.8rem minmax(0, 1fr);
  gap: 1rem;
  align-items: center;
  padding: 0.88rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 6%, var(--panel-border));
}

.product-detail-workbench__contract-row strong {
  text-align: right;
}

.product-detail-workbench__archive-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.82rem;
}

.product-detail-workbench__archive-note-card {
  display: grid;
  gap: 0.32rem;
  align-items: start;
  min-height: 6rem;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: var(--shadow-xs);
}

.product-detail-workbench__archive-note-card--wide {
  grid-column: 1 / -1;
}

.product-detail-workbench__archive-note-card strong {
  font-size: 0.96rem;
  line-height: 1.6;
  text-align: left;
}

.product-detail-workbench__archive-note-card--wide strong {
  line-height: 1.72;
}

@media (max-width: 960px) {
  .product-detail-workbench__hero-plinth {
    padding-inline: 1.16rem;
  }
}

@media (max-width: 720px) {
  .product-detail-workbench__metric-ribbon,
  .product-detail-workbench__contract-row,
  .product-detail-workbench__archive-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__scale-metric {
    border-right: none;
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
    padding-block: 0.74rem 0;
  }

  .product-detail-workbench__scale-metric:first-child {
    border-top: none;
    padding-top: 0.08rem;
  }

  .product-detail-workbench__contract-row strong,
  .product-detail-workbench__archive-note-card strong {
    text-align: left;
  }

  .product-detail-workbench__archive-note-card--wide {
    grid-column: auto;
  }
}
</style>
