<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__panel">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">设备规模与经营判断</h3>
          <p>先看关联设备、在线覆盖与最近上报，判断当前产品是否已经进入稳定使用阶段。</p>
        </div>
        <span
          class="product-detail-workbench__status-chip"
          :class="`product-detail-workbench__status-chip--${statusTone}`"
        >
          {{ lifecycleLabel }}
        </span>
      </div>

      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">关联设备总量</span>
          <strong class="detail-summary-card__value" data-testid="product-detail-hero-total">{{ heroTotal }}</strong>
          <p class="detail-summary-card__hint">{{ deviceScaleHint }}</p>
        </article>
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

      <div class="product-detail-workbench__brief-grid">
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>当前判断</strong>
          </div>
          <p class="product-detail-workbench__copy">{{ heroHeadline }}</p>
          <p class="product-detail-workbench__subcopy">{{ heroSummary }}</p>
        </article>
        <article class="detail-card">
          <div class="detail-card__header">
            <strong>接入提示</strong>
          </div>
          <p class="product-detail-workbench__copy">{{ contractReminder }}</p>
          <p class="product-detail-workbench__subcopy">{{ operationReminder }}</p>
        </article>
      </div>
    </section>

    <section class="detail-panel" data-testid="product-detail-stage-trend">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">活跃趋势与状态判断</h3>
          <p>把活跃设备数和在线时长合在一起看，快速判断产品最近的使用热度与接入稳定性。</p>
        </div>
      </div>

      <div v-if="hasTrendMetrics" data-testid="product-detail-trend-metrics" class="product-detail-workbench__trend-stack">
        <article class="detail-card product-detail-workbench__trend-lead">
          <div class="detail-card__header">
            <strong>{{ trendHeadline }}</strong>
          </div>
          <p class="product-detail-workbench__copy">{{ trendSummary }}</p>
        </article>
        <div class="detail-summary-grid">
          <article v-for="metric in trendMetrics" :key="metric.key" class="detail-summary-card">
            <span class="detail-summary-card__label">{{ metric.label }}</span>
            <strong class="detail-summary-card__value">{{ metric.value }}</strong>
            <p class="detail-summary-card__hint">{{ metric.hint }}</p>
          </article>
        </div>
      </div>

      <div v-else class="detail-notice">
        <span class="detail-notice__label">趋势提示</span>
        <strong class="detail-notice__value">当前还没有足够的活跃度样本，请先结合最近上报和在线覆盖继续观察。</strong>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">接入契约与产品档案</h3>
          <p>统一归拢产品标识、协议基线和建档说明，减少排障时在多处切换信息的成本。</p>
        </div>
      </div>

      <div class="detail-summary-grid" data-testid="product-detail-contract">
        <article v-for="card in contractCards" :key="card.key" class="detail-summary-card">
          <span class="detail-summary-card__label">{{ card.label }}</span>
          <strong class="detail-summary-card__value">{{ card.value }}</strong>
          <p class="detail-summary-card__hint">{{ card.hint }}</p>
        </article>
      </div>

      <div class="detail-grid" data-testid="product-detail-archive">
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
    </section>

    <section class="detail-panel" data-testid="product-detail-governance">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">维护与治理</h3>
          <p>把稳定性判断落成可执行约束，确保产品变更前先评估对现网设备和接入链路的影响。</p>
        </div>
      </div>

      <div :class="['detail-notice', { 'detail-notice--danger': product.status === 0 }]">
        <span class="detail-notice__label">当前建议</span>
        <strong class="detail-notice__value">{{ governanceHeadline }}</strong>
        <p class="product-detail-workbench__notice-copy">{{ governanceNotice }}</p>
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
const heroSummary = computed(() => `最近设备上报：${formatDateTime(product.value.lastReportTime)}`)
const contractReminder = computed(
  () => `接入基线：${toDisplayText(product.value.protocolCode)} / ${getNodeTypeText(product.value.nodeType)} / ${toDisplayText(product.value.dataFormat)}`
)
const operationReminder = computed(() => {
  if (product.value.status === 0) {
    return '停用状态下，新增设备、设备替换与链路验证都应先确认是否需要重启用或新建版本。'
  }
  if ((product.value.deviceCount ?? 0) > 0) {
    return '已有设备在用时，协议编码、节点类型和数据格式调整前要先做兼容性评估。'
  }
  return '当前还处于接入准备阶段，可以继续整理产品说明、命名边界和首批接入规则。'
})
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
    return '当前产品已停用，活跃趋势仅作为历史回看依据。'
  }
  if ((product.value.todayActiveCount ?? 0) > 0 && (product.value.sevenDaysActiveCount ?? 0) > 0) {
    return '近期活跃表现稳定，可继续结合最长在线时长和最近上报观察现场波动。'
  }
  return '活跃表现仍在爬坡，建议继续核对在线覆盖、首批设备使用情况和最近上报节奏。'
})
const trendSummary = computed(() => {
  return `最近上报 ${formatDateTime(product.value.lastReportTime)}，用于辅助判断当前产品链路是否稳定。`
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
    value: toDisplayText(product.value.protocolCode),
    hint: '接入链路识别和报文解析的核心基线。'
  },
  {
    key: 'nodeType',
    label: '节点类型',
    value: getNodeTypeText(product.value.nodeType),
    hint: '用于区分直连设备与网关设备的接入方式。'
  },
  {
    key: 'dataFormat',
    label: '数据格式',
    value: toDisplayText(product.value.dataFormat),
    hint: '统一定义产品消息结构和载荷组织方式。'
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
    return '先核查停用是否会影响现有设备，再决定是否保留或重建产品定义。'
  }
  if ((product.value.deviceCount ?? 0) === 0) {
    return '当前还没有现网设备压力，可以继续作为新设备接入模板完善。'
  }
  return '当前已有设备在用，任何核心契约变更都应先做影响评估和灰度验证。'
})
const governanceNotice = computed(() => {
  if (product.value.status === 0) {
    return '停用状态下建议同步核查设备替换计划、历史日志查询和现场库存处理，避免业务仍依赖旧产品定义。'
  }
  if ((product.value.deviceCount ?? 0) > 0) {
    return '协议编码、节点类型、数据格式和物模型边界都属于高影响项，变更前要先确认不会影响设备建档、上报和链路追踪。'
  }
  return '当前适合继续补齐说明文档、命名边界和首批接入校验规则，为后续规模化接入做准备。'
})
const maintenanceRules = [
  '产品 Key 建立后尽量保持稳定，不建议直接改名。',
  '协议编码、节点类型和数据格式属于接入核心规则，应避免频繁调整。',
  '已有设备在用时，优先通过新建版本或灰度方式承接兼容性变化。'
]
const changeChecklist = [
  '先确认现场是否已经有设备在使用这条产品定义。',
  '再确认协议或物模型变化是否更适合通过新版本承接。',
  '最后确认调整后不会影响设备建档、上报链路和历史检索。'
]
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__panel {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.9rem;
  padding: 0.28rem 0.78rem;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, transparent);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: color-mix(in srgb, var(--brand) 78%, var(--text-heading));
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  white-space: nowrap;
}

.product-detail-workbench__status-chip--inactive {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 18%, transparent);
  background: color-mix(in srgb, var(--danger, #d84f45) 8%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 82%, var(--text-heading));
}

.product-detail-workbench__brief-grid,
.product-detail-workbench__governance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.product-detail-workbench__trend-stack {
  display: grid;
  gap: 0.9rem;
}

.product-detail-workbench__trend-lead {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.9));
}

.product-detail-workbench__copy,
.product-detail-workbench__subcopy,
.product-detail-workbench__notice-copy {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.7;
}

.product-detail-workbench__subcopy,
.product-detail-workbench__notice-copy {
  margin-top: 0.45rem;
  color: var(--text-caption-2);
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

@media (max-width: 900px) {
  .product-detail-workbench__brief-grid,
  .product-detail-workbench__governance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
