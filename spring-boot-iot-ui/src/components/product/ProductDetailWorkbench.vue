<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__hero-stage" data-testid="product-detail-hero-stage">
      <div class="detail-section-header">
        <div>
          <p class="product-detail-workbench__section-kicker" data-testid="product-detail-hero-stage-kicker">经营主判断</p>
          <h3 data-testid="product-detail-stage-title">设备规模与经营判断</h3>
        </div>
        <span
          class="product-detail-workbench__status-chip"
          :class="`product-detail-workbench__status-chip--${statusTone}`"
        >
          {{ lifecycleLabel }}
        </span>
      </div>

      <div class="product-detail-workbench__hero-grid">
        <article class="detail-summary-card product-detail-workbench__primary-card" data-testid="product-detail-primary-metric">
          <span class="detail-summary-card__label">关联设备总量</span>
          <strong class="detail-summary-card__value" data-testid="product-detail-hero-total">{{ heroTotal }}</strong>
          <p class="detail-summary-card__hint">{{ deviceScaleHint }}</p>
          <p class="product-detail-workbench__statement" data-testid="product-detail-primary-statement">{{ heroHeadline }}</p>
        </article>

        <div class="product-detail-workbench__secondary-metrics" data-testid="product-detail-secondary-metrics">
          <article
            v-for="metric in heroSecondaryMetrics"
            :key="metric.key"
            class="detail-summary-card"
          >
            <span class="detail-summary-card__label">{{ metric.label }}</span>
            <strong
              class="detail-summary-card__value"
              :data-testid="`product-detail-hero-secondary-${metric.key}-value`"
            >
              {{ metric.value }}
            </strong>
            <p class="detail-summary-card__hint">{{ metric.hint }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="detail-panel product-detail-workbench__judgement-stage" data-testid="product-detail-judgement-stage">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">活跃趋势与状态判断</h3>
        </div>
      </div>

      <div class="product-detail-workbench__judgement-grid" data-testid="product-detail-judgement-summary">
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>趋势摘要</strong>
          </div>
          <p class="product-detail-workbench__copy">{{ trendHeadline }}</p>
        </article>
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>接入提示</strong>
          </div>
          <p class="product-detail-workbench__copy">{{ contractReminder }}</p>
        </article>
      </div>

      <div v-if="hasTrendMetrics" data-testid="product-detail-trend-metrics" class="product-detail-workbench__trend-strip">
        <article v-for="metric in trendMetrics" :key="metric.key" class="detail-summary-card">
          <span class="detail-summary-card__label">{{ metric.label }}</span>
          <strong class="detail-summary-card__value">{{ metric.value }}</strong>
          <p class="detail-summary-card__hint">{{ metric.hint }}</p>
        </article>
      </div>

      <div v-else class="detail-notice">
        <span class="detail-notice__label">趋势提示</span>
        <strong class="detail-notice__value">当前还没有足够的活跃度样本，请先结合最近上报和在线覆盖继续观察。</strong>
      </div>
    </section>

    <section class="detail-panel product-detail-workbench__contract-archive-stage" data-testid="product-detail-contract-archive-stage">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3>
        </div>
      </div>

      <div class="product-detail-workbench__contract-archive-grid" data-testid="product-detail-contract-summary">
        <div class="product-detail-workbench__contract-grid" data-testid="product-detail-contract">
          <article v-for="card in contractCards" :key="card.key" class="detail-summary-card">
            <span class="detail-summary-card__label">{{ card.label }}</span>
            <strong class="detail-summary-card__value">{{ card.value }}</strong>
          </article>
        </div>

        <div class="detail-grid product-detail-workbench__archive-grid" data-testid="product-detail-archive">
          <div class="detail-field">
            <span class="detail-field__label">产品 ID</span>
            <strong class="detail-field__value">{{ archiveFields.id }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">产品 Key</span>
            <strong class="detail-field__value">{{ archiveFields.productKey }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">产品名称</span>
            <strong class="detail-field__value">{{ archiveFields.productName }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">厂商</span>
            <strong class="detail-field__value">{{ archiveFields.manufacturer }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">创建时间</span>
            <strong class="detail-field__value">{{ archiveFields.createTime }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">更新时间</span>
            <strong class="detail-field__value">{{ archiveFields.updateTime }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">最近上报</span>
            <strong class="detail-field__value">{{ archiveFields.lastReportTime }}</strong>
          </div>
          <div class="detail-field">
            <span class="detail-field__label">产品状态</span>
            <strong class="detail-field__value">{{ archiveFields.status }}</strong>
          </div>
          <div class="detail-field detail-field--full">
            <span class="detail-field__label">产品说明</span>
            <strong class="detail-field__value detail-field__value--plain">{{ archiveFields.description }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="detail-panel product-detail-workbench__governance-stage" data-testid="product-detail-governance">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">维护与治理</h3>
        </div>
      </div>

      <div :class="['detail-notice', { 'detail-notice--danger': product.status === 0 }]">
        <span class="detail-notice__label">当前建议</span>
        <strong class="detail-notice__value">{{ governanceHeadline }}</strong>
      </div>

      <div class="product-detail-workbench__governance-grid">
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>维护规则</strong>
          </div>
          <ul class="product-detail-workbench__rule-list">
            <li v-for="rule in maintenanceRules" :key="rule">{{ rule }}</li>
          </ul>
        </article>
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>变更前确认</strong>
          </div>
          <ul class="product-detail-workbench__rule-list">
            <li v-for="item in changeChecklist" :key="item">{{ item }}</li>
          </ul>
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

function toDisplayCount(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  const count = Number(value)
  return Number.isFinite(count) ? String(count) : '--'
}

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function toDisplayDuration(value?: number | null) {
  return value != null ? `${value} 分钟` : '--'
}

function toCountValue(value?: number | null) {
  if (value === undefined || value === null) {
    return null
  }
  const count = Number(value)
  return Number.isFinite(count) ? count : null
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

function getStatusText(value?: number | null) {
  return value === 0 ? '停用' : '启用'
}

const product = computed(() => props.product)
const heroTotal = computed(() => toDisplayCount(product.value.deviceCount))
const onlineRatioText = computed(() => {
  const deviceCount = toCountValue(product.value.deviceCount)
  const onlineCount = toCountValue(product.value.onlineDeviceCount)
  if (!deviceCount || onlineCount == null) {
    return '--'
  }
  return `${Math.round((onlineCount / deviceCount) * 100)}%`
})
const lifecycleLabel = computed(() => {
  if (product.value.status === 0) {
    return '已停用'
  }
  if ((product.value.deviceCount ?? 0) > 0) {
    return '稳定使用中'
  }
  return '接入调试中'
})
const statusTone = computed(() => (product.value.status === 0 ? 'inactive' : 'active'))
const deviceScaleHint = computed(() => {
  const deviceCount = toCountValue(product.value.deviceCount)
  const onlineCount = toCountValue(product.value.onlineDeviceCount)
  if (!deviceCount) {
    return '当前还没有形成设备规模。'
  }
  if (onlineCount == null) {
    return `当前共有关联设备 ${deviceCount} 台。`
  }
  return `当前共有关联设备 ${deviceCount} 台，在线 ${onlineCount} 台。`
})
const heroHeadline = computed(() => {
  if (product.value.status === 0) {
    return '当前产品已停用，建议优先核查现场是否仍有存量设备依赖这条定义。'
  }
  if ((product.value.deviceCount ?? 0) === 0) {
    return '当前产品还没有形成设备规模，适合继续完成首批接入、建档和契约补齐。'
  }
  if ((product.value.onlineDeviceCount ?? 0) > 0) {
    return '当前产品已经形成稳定使用规模，可以继续结合活跃趋势和治理动作做日常运营判断。'
  }
  return '当前产品下已有设备，但在线覆盖偏弱，建议结合最近上报和链路验证继续排查。'
})
const contractReminder = computed(
  () => `接入基线：${toDisplayText(product.value.protocolCode)} / ${getNodeTypeText(product.value.nodeType)} / ${toDisplayText(product.value.dataFormat)}`
)
const heroSecondaryMetrics = computed(() => [
  {
    key: 'onlineDeviceCount',
    label: '在线设备',
    value: toDisplayCount(product.value.onlineDeviceCount),
    hint: onlineRatioText.value === '--' ? '当前还没有可统计的在线覆盖比例。' : `在线覆盖 ${onlineRatioText.value}`
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: toDisplayCount(product.value.thirtyDaysActiveCount),
    hint: '近 30 天有上报的设备数量。'
  },
  {
    key: 'avgOnlineDuration',
    label: '平均在线时长',
    value: toDisplayDuration(product.value.avgOnlineDuration),
    hint: '近 30 天会话平均在线时长。'
  }
])

const hasTrendMetrics = computed(() => {
  return product.value.todayActiveCount != null
    || product.value.sevenDaysActiveCount != null
    || product.value.thirtyDaysActiveCount != null
    || product.value.avgOnlineDuration != null
    || product.value.maxOnlineDuration != null
})
const trendHeadline = computed(() => {
  if (product.value.status === 0) {
    return '当前产品已停用，活跃趋势仅供历史回看。'
  }
  if ((product.value.todayActiveCount ?? 0) > 0 && (product.value.sevenDaysActiveCount ?? 0) > 0) {
    return '近期活跃表现稳定。'
  }
  return '活跃表现仍在爬坡。'
})
const trendMetrics = computed(() => {
  const metrics: Array<{ key: string; label: string; value: string; hint: string }> = []

  if (product.value.todayActiveCount != null) {
    metrics.push({
      key: 'todayActiveCount',
      label: '今日活跃',
      value: String(product.value.todayActiveCount),
      hint: '今天有上报的设备数量。'
    })
  }

  if (product.value.sevenDaysActiveCount != null) {
    metrics.push({
      key: 'sevenDaysActiveCount',
      label: '7 日活跃',
      value: String(product.value.sevenDaysActiveCount),
      hint: '最近 7 天有上报的设备数量。'
    })
  }

  if (product.value.thirtyDaysActiveCount != null) {
    metrics.push({
      key: 'thirtyDaysActiveCount',
      label: '30 日活跃',
      value: String(product.value.thirtyDaysActiveCount),
      hint: '最近 30 天有上报的设备数量。'
    })
  }

  if (product.value.avgOnlineDuration != null) {
    metrics.push({
      key: 'avgOnlineDuration',
      label: '平均在线时长',
      value: toDisplayDuration(product.value.avgOnlineDuration),
      hint: '近 30 天会话平均时长。'
    })
  }

  if (product.value.maxOnlineDuration != null) {
    metrics.push({
      key: 'maxOnlineDuration',
      label: '最长在线时长',
      value: toDisplayDuration(product.value.maxOnlineDuration),
      hint: '近 30 天单次最长在线时长。'
    })
  }

  return metrics
})

const contractCards = computed(() => [
  {
    key: 'protocolCode',
    label: '协议编码',
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
const archiveFields = computed(() => ({
  id: toDisplayText(product.value.id),
  productKey: toDisplayText(product.value.productKey),
  productName: toDisplayText(product.value.productName),
  manufacturer: toDisplayText(product.value.manufacturer),
  createTime: formatDateTime(product.value.createTime),
  updateTime: formatDateTime(product.value.updateTime),
  lastReportTime: formatDateTime(product.value.lastReportTime),
  status: getStatusText(product.value.status),
  description: product.value.description?.trim() || '当前没有补充说明，可继续结合接入方式和现场设备规模补充产品档案。'
}))

const governanceHeadline = computed(() => {
  if (product.value.status === 0) {
    return '先核查停用是否影响现有设备。'
  }
  if ((product.value.deviceCount ?? 0) === 0) {
    return '当前还没有现网设备压力。'
  }
  return '当前已有设备在用，任何核心契约变更都应先做影响评估和灰度验证。'
})
const maintenanceRules = [
  '产品 Key 尽量保持稳定。',
  '协议、节点、格式不要频繁调整。',
  '有存量设备时优先走灰度或新版本。'
]
const changeChecklist = [
  '先确认是否已有设备在用。',
  '再确认是否应由新版本承接。',
  '最后确认不影响建档和上报。'
]
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1.15rem;
}

.product-detail-workbench__hero-stage,
.product-detail-workbench__judgement-stage,
.product-detail-workbench__contract-archive-stage,
.product-detail-workbench__governance-stage {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__section-kicker,
.product-detail-workbench__section-brief span {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.product-detail-workbench__status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.05rem;
  padding: 0.3rem 0.84rem;
  border: 1px solid color-mix(in srgb, var(--brand) 18%, transparent);
  border-radius: var(--radius-pill);
  background: linear-gradient(180deg, rgba(255, 249, 245, 0.98), rgba(255, 244, 237, 0.98));
  color: color-mix(in srgb, var(--brand) 78%, var(--text-heading));
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  white-space: nowrap;
  box-shadow: 0 10px 18px rgba(217, 120, 47, 0.1);
}

.product-detail-workbench__status-chip--inactive {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 18%, transparent);
  background: color-mix(in srgb, var(--danger, #d84f45) 8%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 82%, var(--text-heading));
}

.product-detail-workbench__hero-grid,
.product-detail-workbench__judgement-grid,
.product-detail-workbench__contract-archive-grid,
.product-detail-workbench__governance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.product-detail-workbench__primary-card,
.product-detail-workbench__secondary-metrics,
.product-detail-workbench__contract-grid,
.product-detail-workbench__archive-grid {
  display: grid;
  gap: 0.82rem;
}

.product-detail-workbench__primary-card {
  align-content: start;
  padding: 1.12rem 1.16rem;
  border-radius: calc(var(--radius-lg) + 4px);
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 36%),
    linear-gradient(180deg, rgba(255, 251, 248, 0.98), rgba(255, 255, 255, 0.98));
}

.product-detail-workbench__secondary-metrics,
.product-detail-workbench__trend-strip {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-detail-workbench__secondary-metrics .detail-summary-card,
.product-detail-workbench__trend-strip .detail-summary-card,
.product-detail-workbench__contract-grid .detail-summary-card {
  padding: 0.92rem 0.96rem;
  border-radius: calc(var(--radius-lg) + 2px);
  background: rgba(255, 255, 255, 0.96);
}

.product-detail-workbench__section-brief {
  display: grid;
  gap: 0.26rem;
  padding: 0.92rem 0.96rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(255, 255, 255, 0.98));
}

.product-detail-workbench__section-brief strong {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
  line-height: 1.45;
}

.product-detail-workbench__statement,
.product-detail-workbench__copy,
.product-detail-workbench__notice-copy {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.72;
}

.product-detail-workbench__statement {
  color: var(--text-heading);
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.6;
}

.product-detail-workbench__archive-grid {
  padding: 0.2rem;
}

.product-detail-workbench__rule-list {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-body);
  font-size: 13px;
  line-height: 1.7;
}

.product-detail-workbench__rule-list li + li {
  margin-top: 0.48rem;
}

@media (max-width: 960px) {
  .product-detail-workbench__hero-grid,
  .product-detail-workbench__judgement-grid,
  .product-detail-workbench__contract-archive-grid,
  .product-detail-workbench__governance-grid,
  .product-detail-workbench__secondary-metrics,
  .product-detail-workbench__trend-strip {
    grid-template-columns: 1fr;
  }
}
</style>
