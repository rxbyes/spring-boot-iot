<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__hero-stage" data-testid="product-detail-hero-stage">
      <span class="product-detail-workbench__hero-label">核心规模</span>
      <strong class="product-detail-workbench__hero-value" data-testid="product-detail-primary-metric">
        {{ primaryMetricValue }}
      </strong>
      <p class="product-detail-workbench__hero-summary">{{ heroSummary }}</p>
    </section>

    <section class="product-detail-workbench__secondary-metrics" data-testid="product-detail-secondary-metrics">
      <article
        v-for="metric in secondaryMetrics"
        :key="metric.key"
        class="detail-panel product-detail-workbench__metric-card"
      >
        <strong>{{ metric.value }}</strong>
        <span>{{ metric.label }}</span>
      </article>
    </section>

    <section class="product-detail-workbench__brief-grid" data-testid="product-detail-brief-grid">
      <article
        v-for="brief in briefCards"
        :key="brief.key"
        class="detail-panel product-detail-workbench__brief-card"
      >
        <strong>{{ brief.label }}</strong>
        <p>{{ brief.value }}</p>
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
const onlineCoverageText = computed(() =>
  toDisplayPercent(product.value.onlineDeviceCount, product.value.deviceCount)
)
const heroSummary = computed(() => {
  const totalDevices = Number(product.value.deviceCount ?? 0)
  if (product.value.status === 0) {
    return '当前产品已停用，当前工作台以历史台账核对和治理留痕为主。'
  }
  if (totalDevices <= 0) {
    return '当前还没有关联设备，可先收口契约、物模型与首批接入验证。'
  }

  const onlineCoverage = Math.round((Number(product.value.onlineDeviceCount ?? 0) / totalDevices) * 100)
  if (onlineCoverage >= 60) {
    return '当前设备规模已形成稳定接入，优先继续核对活跃覆盖与契约基线。'
  }
  if (onlineCoverage > 0) {
    return '当前已形成首批接入规模，下一步优先提升在线覆盖并补齐治理细节。'
  }
  return '当前已登记关联设备，但在线覆盖仍未形成有效样本。'
})
const currentJudgement = computed(() => {
  if (product.value.status === 0) {
    return '产品已停用，当前以历史档案核对、变更留痕和影响范围复核为主。'
  }
  if ((product.value.deviceCount ?? 0) <= 0) {
    return '当前仍处于建档期，建议先完成首批样机接入和最近上报验证。'
  }
  if ((product.value.thirtyDaysActiveCount ?? 0) > 0 && onlineCoverageText.value !== '--') {
    return `近 30 日已有 ${toDisplayText(product.value.thirtyDaysActiveCount)} 台活跃设备，经营基线已进入持续优化阶段。`
  }
  return '设备已进入运行期，但活跃样本仍偏少，建议优先关注在线覆盖与首批稳定性。'
})
const contractBaselineText = computed(() => [
  toDisplayText(product.value.protocolCode),
  getNodeTypeText(product.value.nodeType),
  toDisplayText(product.value.dataFormat)
].join(' / '))
const archiveSummaryText = computed(() => {
  const archiveFragments = [
    toDisplayText(product.value.manufacturer),
    formatDateTime(product.value.updateTime),
    product.value.description?.trim() || '当前没有补充说明'
  ]

  return archiveFragments.join(' / ')
})
const secondaryMetrics = computed(() => [
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
const briefCards = computed(() => [
  {
    key: 'currentJudgement',
    label: '当前判断',
    value: currentJudgement.value
  },
  {
    key: 'contractBaseline',
    label: '契约基线',
    value: contractBaselineText.value
  },
  {
    key: 'archiveSummary',
    label: '档案摘要',
    value: archiveSummaryText.value
  }
])
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__hero-stage,
.product-detail-workbench__secondary-metrics,
.product-detail-workbench__brief-grid {
  display: grid;
  gap: 0.82rem;
}

.product-detail-workbench__hero-stage {
  gap: 0.62rem;
  padding: 1.18rem 1.22rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 14px 28px rgba(28, 53, 87, 0.05);
}

.product-detail-workbench__hero-label,
.product-detail-workbench__metric-card span {
  color: var(--text-secondary);
  font-size: 0.84rem;
  line-height: 1.6;
}

.product-detail-workbench__hero-value {
  color: var(--text-heading);
  font-size: clamp(2.18rem, 2.8vw, 2.72rem);
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.product-detail-workbench__hero-summary,
.product-detail-workbench__brief-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.72;
}

.product-detail-workbench__secondary-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-detail-workbench__metric-card {
  display: grid;
  gap: 0.3rem;
  min-height: 6rem;
  padding: 0.98rem 1rem;
}

.product-detail-workbench__metric-card strong,
.product-detail-workbench__brief-card strong {
  color: var(--text-heading);
  font-size: 1.24rem;
  line-height: 1.28;
}

.product-detail-workbench__brief-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-detail-workbench__brief-card {
  gap: 0.5rem;
  min-height: 8.8rem;
  padding: 1rem 1.04rem;
}

.product-detail-workbench__brief-card strong {
  font-size: 1.08rem;
}

@media (max-width: 960px) {
  .product-detail-workbench__secondary-metrics,
  .product-detail-workbench__brief-grid {
    grid-template-columns: 1fr;
  }
}
</style>
