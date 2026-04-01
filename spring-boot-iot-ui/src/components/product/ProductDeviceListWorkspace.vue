<template>
  <div class="product-device-workspace">
    <div v-if="loading" class="device-state">{{ loadingText }}</div>
    <div v-else-if="errorMessage" class="device-state device-state--error">{{ errorMessage }}</div>
    <div v-else-if="empty" class="device-state">{{ emptyText }}</div>
    <div v-else class="device-workspace__content">
      <section class="device-workspace__ledger-ruler">
        <ul class="device-workspace__ruler-metrics">
          <li
            v-for="metric in summaryMetrics"
            :key="metric.key"
            class="device-workspace__ruler-item"
            :data-tone="metric.tone"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </li>
        </ul>
      </section>

      <section class="device-workspace__registry-sheet">
        <header class="device-workspace__registry-heading">
          <strong>设备清册</strong>
        </header>

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
.device-workspace__ledger-ruler,
.device-workspace__ruler-metrics,
.device-workspace__registry-sheet {
  display: grid;
}

.product-device-workspace,
.device-workspace__content {
  gap: 1rem;
}

.device-state {
  padding: 0.96rem 1rem;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 16%, white), white);
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.7;
}

.device-state--error {
  color: color-mix(in srgb, var(--danger) 78%, var(--text-secondary));
  border-color: color-mix(in srgb, var(--danger) 20%, var(--panel-border));
}

.device-workspace__ledger-ruler {
  border-top: 1px solid var(--panel-border-strong);
  border-bottom: 1px solid var(--panel-border);
}

.device-workspace__ruler-metrics {
  list-style: none;
  margin: 0;
  padding: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
}

.device-workspace__ruler-item {
  display: grid;
  gap: 0.32rem;
  min-width: 0;
  padding: 0.96rem 1rem 0.9rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.device-workspace__ruler-item:last-child {
  border-right: 0;
}

.device-workspace__ruler-item span {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.device-workspace__ruler-item strong,
.device-workspace__registry-heading strong {
  color: var(--text-heading);
}

.device-workspace__ruler-item strong {
  font-size: 1.18rem;
  line-height: 1.36;
}

.device-workspace__registry-sheet {
  gap: 0.88rem;
  padding-top: 0.9rem;
  border-top: 1px solid var(--panel-border-strong);
}

.device-workspace__registry-heading {
  display: grid;
  justify-items: start;
}

.device-workspace__registry-heading strong {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 1.08rem;
  line-height: 1.4;
}

.device-workspace__table-shell {
  overflow: hidden;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--brand-light) 10%, white));
}

.device-workspace__table-shell :deep(.el-table) {
  background: transparent;
}

.device-workspace__table-shell :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.device-workspace__table-shell :deep(.el-table th.el-table__cell) {
  background: color-mix(in srgb, var(--brand-light) 12%, white);
  border-bottom-color: color-mix(in srgb, var(--brand) 8%, var(--panel-border));
}

.device-workspace__table-shell :deep(.el-table td.el-table__cell) {
  border-bottom-color: color-mix(in srgb, var(--brand) 6%, var(--panel-border));
}

.device-workspace__table-shell :deep(.el-table__row:hover) {
  background: color-mix(in srgb, var(--brand) 4%, transparent);
}

@media (max-width: 960px) {
  .device-workspace__ruler-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .device-workspace__ruler-item:nth-child(2n) {
    border-right: 0;
  }

  .device-workspace__ruler-item:nth-child(n + 3) {
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }
}

@media (max-width: 720px) {
  .device-workspace__ruler-metrics {
    grid-template-columns: 1fr;
  }

  .device-workspace__ruler-item {
    border-right: 0;
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }

  .device-workspace__ruler-item:first-child {
    border-top: 0;
  }
}
</style>
