<template>
  <div class="product-detail-workbench">
    <section class="product-detail-stage product-detail-stage--hero">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Hero Stage</span>
        <h3 data-testid="product-detail-stage-title">设备规模与经营判断</h3>
      </header>

      <div class="product-detail-hero">
        <article class="product-detail-hero__main">
          <span class="product-detail-hero__label">关联设备总量</span>
          <strong data-testid="product-detail-hero-total">{{ heroTotal }}</strong>
          <p class="product-detail-hero__headline">{{ heroHeadline }}</p>
          <p class="product-detail-hero__summary">{{ heroSummary }}</p>
        </article>

        <div class="product-detail-hero__secondary">
          <article
            v-for="metric in heroSecondaryMetrics"
            :key="metric.key"
            class="product-detail-hero-metric"
            :data-testid="`product-detail-hero-secondary-${metric.key}`"
          >
            <span>{{ metric.label }}</span>
            <strong :data-testid="`product-detail-hero-secondary-${metric.key}-value`">{{ metric.value }}</strong>
            <p>{{ metric.hint }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="product-detail-stage" data-testid="product-detail-stage-trend">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Trend Stage</span>
        <h3 data-testid="product-detail-stage-title">活跃趋势与状态判断</h3>
      </header>

      <div v-if="hasTrendMetrics" class="product-detail-trend" data-testid="product-detail-trend-metrics">
        <article class="product-detail-trend__main">
          <strong>{{ trendHeadline }}</strong>
          <p>{{ trendSummary }}</p>
        </article>

        <div class="product-detail-trend__rail">
          <article v-for="metric in trendMetrics" :key="metric.key" class="product-detail-trend-metric">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <p>{{ metric.hint }}</p>
          </article>
        </div>
      </div>

      <div v-else class="product-detail-trend__empty">
        当前还没有足够的活跃度样本，请先结合最近上报和在线覆盖继续观察。
      </div>
    </section>

    <section class="product-detail-stage">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Contract &amp; Archive Stage</span>
        <h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3>
      </header>
    </section>

    <section class="product-detail-stage">
      <header class="product-detail-stage__header">
        <span class="product-detail-stage__eyebrow">Governance Stage</span>
        <h3 data-testid="product-detail-stage-title">维护与治理</h3>
      </header>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Product } from '@/types/api'

const props = defineProps<{
  product: Product
}>()

function toDisplayCount(value?: number | null) {
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

const heroTotal = computed(() => toDisplayCount(props.product.deviceCount))

const heroHeadline = computed(() => {
  if ((props.product.status ?? 1) === 0) {
    return '当前产品已停用，建议先确认是否仍需保留现场库存设备。'
  }
  if ((props.product.deviceCount ?? 0) === 0) {
    return '当前产品还没有形成设备规模，可继续完成首批建档。'
  }
  return '当前产品已形成稳定设备规模，可继续观察活跃表现与治理动作。'
})

const heroSummary = computed(() => `最近设备上报：${props.product.lastReportTime || '--'}`)

const heroSecondaryMetrics = computed(() => [
  {
    key: 'onlineDeviceCount',
    label: '在线设备',
    value: toDisplayCount(props.product.onlineDeviceCount),
    hint: '当前在线覆盖'
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: toDisplayCount(props.product.thirtyDaysActiveCount),
    hint: '近 30 天有上报的设备'
  },
  {
    key: 'avgOnlineDuration',
    label: '平均在线时长',
    value: props.product.avgOnlineDuration != null ? `${props.product.avgOnlineDuration} 分钟` : '--',
    hint: '近 30 天会话平均时长'
  }
])

const hasTrendMetrics = computed(() => {
  return props.product.todayActiveCount != null
    || props.product.sevenDaysActiveCount != null
    || props.product.thirtyDaysActiveCount != null
    || props.product.avgOnlineDuration != null
    || props.product.maxOnlineDuration != null
})

const trendHeadline = computed(() => {
  if ((props.product.todayActiveCount ?? 0) > 0 && (props.product.onlineDeviceCount ?? 0) > 0) {
    return '近期活跃表现稳定，可继续结合 7 日活跃和最长在线时长观察现场波动。'
  }
  return '活跃表现仍在爬坡，建议优先确认在线覆盖和最近上报节奏。'
})

const trendSummary = computed(() => `最近上报时间 ${props.product.lastReportTime || '--'}，用于辅助判断接入稳定性。`)

const trendMetrics = computed(() => {
  const metrics: Array<{ key: string; label: string; value: string; hint: string }> = []

  if (props.product.todayActiveCount != null) {
    metrics.push({
      key: 'today',
      label: '今日活跃',
      value: String(props.product.todayActiveCount),
      hint: '今天有上报的设备'
    })
  }

  if (props.product.sevenDaysActiveCount != null) {
    metrics.push({
      key: 'seven',
      label: '7 日活跃',
      value: String(props.product.sevenDaysActiveCount),
      hint: '最近 7 天有上报的设备'
    })
  }

  if (props.product.maxOnlineDuration != null) {
    metrics.push({
      key: 'maxOnlineDuration',
      label: '最长在线时长',
      value: `${props.product.maxOnlineDuration} 分钟`,
      hint: '近 30 天单次最长在线'
    })
  }

  return metrics
})
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 18px;
}

.product-detail-stage {
  padding: 16px;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: #fff;
}

.product-detail-stage__header {
  display: grid;
  gap: 4px;
}

.product-detail-stage__eyebrow {
  color: var(--text-caption-2);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-detail-stage__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 18px;
  line-height: 1.4;
}

.product-detail-stage--hero {
  background: linear-gradient(135deg, rgba(232, 128, 56, 0.08), rgba(255, 255, 255, 0.98));
}

.product-detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 16px;
  margin-top: 16px;
}

.product-detail-trend {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 16px;
  margin-top: 16px;
}

.product-detail-hero__main,
.product-detail-hero-metric {
  padding: 16px;
  border: 1px solid rgba(107, 142, 199, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
}

.product-detail-hero__main {
  display: grid;
  gap: 8px;
}

.product-detail-hero__label,
.product-detail-hero-metric span {
  color: var(--text-caption-2);
  font-size: 12px;
  line-height: 1.5;
}

.product-detail-hero__main strong {
  color: var(--text-heading);
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1;
}

.product-detail-hero__headline,
.product-detail-hero__summary,
.product-detail-hero-metric p,
.product-detail-trend__main p,
.product-detail-trend-metric p,
.product-detail-trend__empty {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
}

.product-detail-hero__secondary {
  display: grid;
  gap: 12px;
}

.product-detail-hero-metric,
.product-detail-trend__main,
.product-detail-trend-metric {
  display: grid;
  gap: 6px;
}

.product-detail-hero-metric strong,
.product-detail-trend__main strong,
.product-detail-trend-metric strong {
  color: var(--text-heading);
  font-size: 18px;
  line-height: 1.3;
}

.product-detail-trend__rail {
  display: grid;
  gap: 12px;
}

.product-detail-trend__main,
.product-detail-trend-metric {
  padding: 16px;
  border: 1px solid rgba(107, 142, 199, 0.14);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
}

.product-detail-trend__empty {
  margin-top: 16px;
}

@media (max-width: 1024px) {
  .product-detail-hero,
  .product-detail-trend {
    grid-template-columns: 1fr;
  }
}
</style>
