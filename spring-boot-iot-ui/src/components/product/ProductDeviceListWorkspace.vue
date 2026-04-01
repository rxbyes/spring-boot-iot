<template>
  <div class="product-device-workspace">
    <div v-if="loading" class="device-state">{{ loadingText }}</div>
    <div v-else-if="errorMessage" class="device-state device-state--error">{{ errorMessage }}</div>
    <div v-else-if="empty" class="device-state">{{ emptyText }}</div>
    <div v-else class="device-workspace__content">
      <section class="detail-panel device-workspace__ledger-marquee">
        <ul class="device-workspace__marquee-metrics">
          <li
            v-for="metric in summaryMetrics"
            :key="metric.key"
            class="device-workspace__marquee-item"
            :data-tone="metric.tone"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </li>
        </ul>
      </section>

      <section class="detail-panel device-workspace__table-stage device-workspace__ledger-stage">
        <div class="device-workspace__section-copy device-workspace__ledger-heading">
          <strong>设备清册 · 关联设备台账</strong>
        </div>

        <div class="device-workspace__table-hall">
          <div class="device-workspace__table-shell">
            <el-table
              v-loading="devicesLoading"
              :data="devices"
              border
              stripe
              empty-text="当前产品还没有关联设备"
              class="device-drawer__table"
            >
              <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="deviceCode" label="设备编码" min-width="160" show-overflow-tooltip />
              <el-table-column prop="onlineStatus" label="在线状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.onlineStatus === 1 ? 'success' : 'danger'" round>
                    {{ row.onlineStatus === 1 ? '在线' : '离线' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="activateStatus" label="激活状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.activateStatus === 1 ? 'success' : 'info'" round>
                    {{ row.activateStatus === 1 ? '已激活' : '未激活' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="firmwareVersion" label="固件版本" width="120" show-overflow-tooltip />
              <el-table-column prop="lastReportTime" label="最近上报" width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row.lastReportTime) }}</span>
                </template>
              </el-table-column>
              <el-table-column
                label="操作"
                :width="deviceLedgerActionColumnWidth"
                fixed="right"
                show-overflow-tooltip
                class-name="standard-row-actions-column"
              >
                <template #default="{ row }">
                  <StandardWorkbenchRowActions
                    variant="table"
                    :direct-items="deviceLedgerRowActions"
                    @command="() => handleViewDevice(row)"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, toRefs } from 'vue'

import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import type { Device, Product } from '@/types/api'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    product?: Product | null
    devices: Device[]
    totalDevices: number
    onlineDevices: number
    offlineDevices: number
    loading?: boolean
    loadingText?: string
    errorMessage?: string
    empty?: boolean
    emptyText?: string
    devicesLoading?: boolean
  }>(),
  {
    product: null,
    loading: false,
    loadingText: '正在加载设备...',
    errorMessage: '',
    empty: false,
    emptyText: '暂无设备数据',
    devicesLoading: false
  }
)

const {
  devices,
  totalDevices,
  onlineDevices,
  offlineDevices,
  loading,
  loadingText,
  errorMessage,
  empty,
  emptyText,
  devicesLoading
} = toRefs(props)

const emit = defineEmits<{
  (event: 'viewDevice', device: Device): void
}>()
const deviceLedgerRowActions = [{ command: 'view' as const, label: '查看' }]

const onlineRatioText = computed(() => {
  if (totalDevices.value <= 0) {
    return '--'
  }
  return `${Math.round((onlineDevices.value / totalDevices.value) * 100)}%`
})

const summaryMetrics = computed(() => [
  {
    key: 'total',
    label: '设备总数',
    value: String(totalDevices.value),
    tone: 'brand'
  },
  {
    key: 'online',
    label: '在线设备',
    value: String(onlineDevices.value),
    tone: 'brand'
  },
  {
    key: 'offline',
    label: '离线设备',
    value: String(offlineDevices.value),
    tone: 'brand'
  },
  {
    key: 'ratio',
    label: '在线比例',
    value: onlineRatioText.value,
    tone: 'brand'
  }
])

const deviceLedgerActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: deviceLedgerRowActions
})

function handleViewDevice(device: Device) {
  emit('viewDevice', device)
}
</script>

<style scoped>
.product-device-workspace,
.device-workspace__content,
.device-workspace__ledger-marquee,
.device-workspace__marquee-metrics,
.device-workspace__table-stage,
.device-workspace__table-hall {
  display: grid;
}

.product-device-workspace,
.device-workspace__content {
  gap: 1.08rem;
}

.device-state {
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--panel-border));
  border-radius: 0.92rem;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 6%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(245, 249, 255, 0.94));
  color: var(--text-secondary);
  font-size: 12.5px;
  line-height: 1.6;
  box-shadow: var(--shadow-inset-highlight-78);
}

.device-state--error {
  color: color-mix(in srgb, var(--danger) 76%, var(--text-secondary));
  border-color: color-mix(in srgb, var(--danger) 22%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--danger) 8%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 241, 241, 0.96));
}

.device-workspace__ledger-marquee,
.device-workspace__table-stage {
  gap: 0.88rem;
  padding: 1.12rem 1.18rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background:
    linear-gradient(180deg, rgba(250, 248, 244, 0.62), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: var(--shadow-surface-soft-sm);
}

.device-workspace__section-copy {
  display: grid;
  gap: 0.26rem;
}

.device-workspace__section-copy strong {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.04rem;
  font-weight: 700;
  line-height: 1.42;
}

.device-workspace__ledger-marquee {
  gap: 0;
}

.device-workspace__marquee-metrics {
  list-style: none;
  margin: 0;
  padding: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
}

.device-workspace__marquee-item {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.9rem 0.96rem 0.84rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  align-content: start;
}

.device-workspace__marquee-item:last-child {
  border-right: none;
}

.device-workspace__marquee-item span {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  letter-spacing: 0.03em;
  line-height: 1.4;
}

.device-workspace__marquee-item strong {
  color: var(--text-heading);
  font-size: 1.36rem;
  font-weight: 700;
  line-height: 1.14;
}

.device-workspace__marquee-item[data-tone='brand'] {
  background: transparent;
}

.device-workspace__ledger-heading {
  justify-items: center;
  text-align: center;
  padding-bottom: 0.16rem;
}

.device-workspace__ledger-heading strong {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  padding-bottom: 0.42rem;
  border-bottom: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  font-size: 1rem;
  letter-spacing: 0.08em;
}

.device-workspace__table-hall {
  gap: 0;
}

.device-workspace__table-shell {
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 5px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 255, 0.94));
  box-shadow: var(--shadow-surface-soft-sm);
}

.device-workspace__table-shell :deep(.el-table) {
  background: transparent;
}

.device-workspace__table-shell :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.device-workspace__table-shell :deep(.el-table th.el-table__cell) {
  background: color-mix(in srgb, var(--brand) 6%, white);
  border-bottom-color: color-mix(in srgb, var(--brand) 10%, transparent);
}

.device-workspace__table-shell :deep(.el-table td.el-table__cell) {
  border-bottom-color: color-mix(in srgb, var(--brand) 8%, transparent);
}

.device-workspace__table-shell :deep(.el-table__row:hover) {
  background: color-mix(in srgb, var(--brand) 4%, transparent);
}

@media (max-width: 960px) {
  .device-workspace__marquee-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .device-workspace__marquee-item:nth-child(2n) {
    border-right: none;
  }

  .device-workspace__marquee-item:nth-child(n + 3) {
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }
}

@media (max-width: 720px) {
  .device-workspace__marquee-metrics {
    grid-template-columns: 1fr;
  }

  .device-workspace__marquee-item {
    border-right: none;
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }

  .device-workspace__marquee-item:first-child {
    border-top: none;
  }
}
</style>
