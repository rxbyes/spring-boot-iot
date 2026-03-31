<template>
  <div class="product-detail-workbench">
    <section class="detail-panel product-detail-workbench__ledger-stage" data-testid="product-detail-ledger-stage">
      <div class="detail-section-header">
        <div>
          <h3 data-testid="product-detail-stage-title">趋势摘要与契约档案</h3>
        </div>
      </div>

      <div class="product-detail-workbench__ledger-board" data-testid="product-detail-ledger-table">
        <article class="product-detail-workbench__ledger-module">
          <div class="product-detail-workbench__ledger-module-header">
            <span>趋势摘要</span>
          </div>

          <div
            v-if="hasTrendMetrics"
            class="product-detail-workbench__trend-grid"
            data-testid="product-detail-ledger-trend-rows"
          >
            <div
              v-for="cell in trendCells"
              :key="cell.key"
              class="product-detail-workbench__trend-cell"
            >
              <span class="product-detail-workbench__cell-label">{{ cell.label }}</span>
              <span class="product-detail-workbench__cell-value">{{ cell.value }}</span>
            </div>
          </div>

          <div v-else class="detail-notice product-detail-workbench__ledger-notice">
            <span class="detail-notice__label">趋势摘要</span>
            <span class="detail-notice__value">{{ trendEmptyText }}</span>
          </div>
        </article>

        <article class="product-detail-workbench__ledger-module">
          <div class="product-detail-workbench__ledger-module-header">
            <span>接入契约与产品档案</span>
          </div>

          <div
            class="product-detail-workbench__archive-grid"
            data-testid="product-detail-ledger-archive-rows"
          >
            <div
              v-for="cell in archiveCells"
              :key="cell.key"
              class="product-detail-workbench__archive-cell"
              :class="{ 'product-detail-workbench__archive-cell--wide': cell.wide }"
            >
              <span class="product-detail-workbench__cell-label">{{ cell.label }}</span>
              <span class="product-detail-workbench__cell-value">{{ cell.value }}</span>
            </div>
          </div>
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

const hasTrendMetrics = computed(() => {
  return product.value.todayActiveCount != null
    || product.value.sevenDaysActiveCount != null
    || product.value.thirtyDaysActiveCount != null
    || product.value.avgOnlineDuration != null
    || product.value.maxOnlineDuration != null
})
const trendHeadline = computed(() => {
  if (product.value.status === 0) {
    return '停用期，仅供历史回看。'
  }
  if ((product.value.todayActiveCount ?? 0) > 0 && (product.value.sevenDaysActiveCount ?? 0) > 0) {
    return '近期活跃保持平稳。'
  }
  return '活跃表现仍在爬坡。'
})
const trendCells = computed(() => [
  {
    key: 'trendHeadline',
    label: '趋势判断',
    value: trendHeadline.value
  },
  {
    key: 'todayActiveCount',
    label: '今日活跃',
    value: toDisplayText(product.value.todayActiveCount)
  },
  {
    key: 'sevenDaysActiveCount',
    label: '7 日活跃',
    value: toDisplayText(product.value.sevenDaysActiveCount)
  },
  {
    key: 'thirtyDaysActiveCount',
    label: '30 日活跃',
    value: toDisplayText(product.value.thirtyDaysActiveCount)
  },
  {
    key: 'avgOnlineDuration',
    label: '平均在线时长',
    value: toDisplayDuration(product.value.avgOnlineDuration)
  },
  {
    key: 'maxOnlineDuration',
    label: '最长在线时长',
    value: toDisplayDuration(product.value.maxOnlineDuration)
  }
])
const trendEmptyText = '当前还没有足够的活跃度样本，请先结合最近上报继续观察。'
const archiveCells = computed(() => [
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
  },
  {
    key: 'manufacturer',
    label: '厂商',
    value: toDisplayText(product.value.manufacturer)
  },
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDateTime(product.value.lastReportTime)
  },
  {
    key: 'updateTime',
    label: '更新时间',
    value: formatDateTime(product.value.updateTime)
  },
  {
    key: 'status',
    label: '产品状态',
    value: getStatusText(product.value.status)
  },
  {
    key: 'description',
    label: '产品说明',
    value: product.value.description?.trim() || '当前没有补充说明，可继续结合现场设备和接入边界补齐档案。',
    wide: true
  }
])
</script>

<style scoped>
.product-detail-workbench {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__ledger-stage {
  display: grid;
  gap: 1rem;
}

.product-detail-workbench__ledger-board {
  display: grid;
  gap: 1rem;
  padding: 1.08rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 6px);
  background: linear-gradient(180deg, rgba(252, 249, 246, 0.98), rgba(255, 255, 255, 0.99));
  box-shadow: 0 14px 30px rgba(28, 53, 87, 0.05);
}

.product-detail-workbench__ledger-module {
  display: grid;
  gap: 0.86rem;
  padding: 1rem 1.04rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.98);
}

.product-detail-workbench__ledger-module-header {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.96rem;
  font-weight: 500;
  line-height: 1.5;
}

.product-detail-workbench__trend-grid,
.product-detail-workbench__archive-grid {
  display: grid;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--brand) 9%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 1px);
  background: rgba(252, 252, 252, 0.98);
}

.product-detail-workbench__trend-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-detail-workbench__archive-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.product-detail-workbench__trend-cell,
.product-detail-workbench__archive-cell {
  display: grid;
  gap: 0.34rem;
  min-height: 5.4rem;
  padding: 0.9rem 1rem;
  background: rgba(255, 255, 255, 0.94);
}

.product-detail-workbench__trend-cell {
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__trend-cell:nth-child(3n) {
  border-right: none;
}

.product-detail-workbench__trend-cell:nth-last-child(-n + 3) {
  border-bottom: none;
}

.product-detail-workbench__archive-cell {
  min-height: 5.2rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.product-detail-workbench__archive-cell:nth-child(3n) {
  border-right: none;
}

.product-detail-workbench__archive-cell:nth-last-child(-n + 2) {
  border-bottom: none;
}

.product-detail-workbench__archive-cell--wide {
  grid-column: span 2;
  border-right: none;
}

.product-detail-workbench__cell-label {
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 400;
  line-height: 1.65;
}

.product-detail-workbench__cell-value {
  color: var(--text-heading);
  font-size: 0.95rem;
  font-weight: 400;
  line-height: 1.68;
}

.product-detail-workbench__ledger-notice {
  margin: 0;
}

@media (max-width: 960px) {
  .product-detail-workbench__trend-grid,
  .product-detail-workbench__archive-grid {
    grid-template-columns: 1fr;
  }

  .product-detail-workbench__trend-cell,
  .product-detail-workbench__archive-cell,
  .product-detail-workbench__archive-cell--wide {
    min-height: auto;
    border-right: none;
    border-bottom: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
    grid-column: auto;
  }

  .product-detail-workbench__trend-cell:last-child,
  .product-detail-workbench__archive-cell:last-child,
  .product-detail-workbench__archive-cell--wide:last-child {
    border-bottom: none;
  }
}
</style>
