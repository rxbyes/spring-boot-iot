<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__hero-stage" data-testid="product-detail-hero-stage">
      <div class="product-detail-workbench__hero-main">
        <span class="product-detail-workbench__hero-label">核心规模</span>
        <div class="product-detail-workbench__hero-value-block">
          <strong class="product-detail-workbench__hero-value" data-testid="product-detail-primary-metric">
            {{ primaryMetricValue }}
          </strong>
          <span class="product-detail-workbench__hero-caption">{{ primaryMetricCaption }}</span>
        </div>
      </div>

      <section class="product-detail-workbench__scale-metrics" data-testid="product-detail-scale-metrics">
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
      <section class="detail-panel product-detail-workbench__contract-stage" data-testid="product-detail-contract-stage">
        <strong class="product-detail-workbench__section-title">契约基线</strong>
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

      <article class="detail-panel product-detail-workbench__archive-stage" data-testid="product-detail-archive-stage">
        <strong class="product-detail-workbench__section-title">档案摘要</strong>
        <div class="product-detail-workbench__archive-list">
          <article
            v-for="item in archiveSummaryItems"
            :key="item.key"
            class="product-detail-workbench__archive-item"
            :class="{ 'product-detail-workbench__archive-item--multiline': item.multiline }"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </article>
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
    multiline: false
  },
  {
    key: 'updateTime',
    label: '最近更新',
    value: formatDateTime(product.value.updateTime),
    multiline: false
  },
  {
    key: 'description',
    label: '补充说明',
    value: product.value.description?.trim() || '当前没有补充说明',
    multiline: true
  }
])
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1.08rem;
}

.product-detail-workbench__hero-stage,
.product-detail-workbench__brief-stage,
.product-detail-workbench__contract-stage,
.product-detail-workbench__archive-stage {
  display: grid;
  gap: 0.82rem;
}

.product-detail-workbench__hero-stage {
  gap: 0.8rem;
  padding: 1.42rem 1.38rem 1.22rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 14px 28px rgba(28, 53, 87, 0.05);
}

.product-detail-workbench__hero-main {
  display: grid;
  justify-items: center;
  text-align: center;
  gap: 0.3rem;
}

.product-detail-workbench__hero-value-block {
  display: grid;
  gap: 0.3rem;
  justify-items: center;
}

.product-detail-workbench__hero-label,
.product-detail-workbench__scale-metric span,
.product-detail-workbench__contract-row span,
.product-detail-workbench__archive-item span {
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
}

.product-detail-workbench__hero-value {
  color: var(--text-heading);
  font-size: clamp(2.48rem, 3vw, 3.18rem);
  line-height: 1;
  letter-spacing: -0.04em;
}

.product-detail-workbench__hero-caption {
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.4;
}

.product-detail-workbench__scale-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  padding-top: 1.02rem;
  border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__scale-metric {
  display: grid;
  gap: 0.28rem;
  justify-items: center;
  text-align: center;
}

.product-detail-workbench__scale-metric strong,
.product-detail-workbench__section-title,
.product-detail-workbench__contract-row strong,
.product-detail-workbench__archive-item strong {
  color: var(--text-heading);
  font-size: 1.04rem;
  line-height: 1.28;
}

.product-detail-workbench__brief-stage {
  gap: 0.92rem;
}

.product-detail-workbench__contract-stage,
.product-detail-workbench__archive-stage {
  padding: 1.04rem 1.08rem;
}

.product-detail-workbench__section-title {
  font-size: 1rem;
  justify-self: center;
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

.product-detail-workbench__archive-list {
  display: grid;
  gap: 0;
  border-top: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
}

.product-detail-workbench__archive-item {
  display: grid;
  grid-template-columns: 5.8rem minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
  padding: 0.88rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 6%, var(--panel-border));
  background: transparent;
}

.product-detail-workbench__archive-item strong {
  font-size: 0.96rem;
  line-height: 1.6;
  text-align: right;
}

.product-detail-workbench__archive-item--multiline strong {
  line-height: 1.72;
}

@media (max-width: 960px) {
  .product-detail-workbench__hero-stage {
    padding-inline: 1.16rem;
  }
}

@media (max-width: 720px) {
  .product-detail-workbench__scale-metrics,
  .product-detail-workbench__contract-row,
  .product-detail-workbench__archive-item {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__contract-row strong,
  .product-detail-workbench__archive-item strong {
    text-align: left;
  }
}
</style>
