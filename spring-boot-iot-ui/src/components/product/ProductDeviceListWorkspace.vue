<template>
  <div class="product-device-workspace">
    <div v-if="loading" class="device-state">{{ loadingText }}</div>
    <div v-else-if="errorMessage" class="device-state device-state--error">{{ errorMessage }}</div>
    <div v-else-if="empty" class="device-state">{{ emptyText }}</div>
    <div v-else class="device-workspace__content">
      <section class="device-workspace__summary-band">
        <div class="device-workspace__summary-copy">
          <p class="device-workspace__summary-title">设备运行概览</p>
          <p class="device-workspace__summary-description">先看设备规模和在线覆盖，再进入台账核对当前产品的运行情况。</p>
        </div>

        <ul class="device-workspace__metrics">
          <li
            v-for="metric in summaryMetrics"
            :key="metric.key"
            class="device-workspace__metric"
            :data-tone="metric.tone"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
            <small>{{ metric.hint }}</small>
          </li>
        </ul>
      </section>

      <section class="device-workspace__table-stage">
        <div class="device-workspace__section-copy">
          <strong>关联设备台账</strong>
          <small>快速核对设备身份、在线状态、激活状态和最近上报。</small>
        </div>

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
              width="100"
              fixed="right"
              show-overflow-tooltip
              class-name="standard-row-actions-column"
            >
              <template #default="{ row }">
                <StandardRowActions>
                  <StandardActionLink @click="handleViewDevice(row)">查看</StandardActionLink>
                </StandardRowActions>
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

import StandardActionLink from '@/components/StandardActionLink.vue'
import StandardRowActions from '@/components/StandardRowActions.vue'
import type { Device, Product } from '@/types/api'
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
    hint: totalDevices.value > 0 ? '当前产品下已建档的设备数量。' : '当前还没有关联设备。',
    tone: 'brand'
  },
  {
    key: 'online',
    label: '在线设备',
    value: String(onlineDevices.value),
    hint: onlineDevices.value > 0 ? '当前保持在线的设备数量。' : '当前没有设备在线。',
    tone: 'success'
  },
  {
    key: 'offline',
    label: '离线设备',
    value: String(offlineDevices.value),
    hint: offlineDevices.value > 0 ? '当前未在线的设备数量。' : '当前没有离线设备。',
    tone: 'danger'
  },
  {
    key: 'ratio',
    label: '在线比例',
    value: onlineRatioText.value,
    hint: totalDevices.value > 0 ? '在线设备在关联设备中的占比。' : '当前没有设备，暂不统计。',
    tone: 'accent'
  }
])

function handleViewDevice(device: Device) {
  emit('viewDevice', device)
}
</script>

<style scoped>
.product-device-workspace,
.device-workspace__content {
  display: grid;
  gap: 0.88rem;
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

.device-workspace__summary-band,
.device-workspace__table-stage {
  display: grid;
  gap: 0.48rem;
  padding: 0.88rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: var(--shadow-surface-soft-sm);
}

.device-workspace__summary-copy,
.device-workspace__section-copy {
  display: grid;
  gap: 0.18rem;
}

.device-workspace__summary-title,
.device-workspace__section-copy strong {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.82rem;
  font-weight: 600;
}

.device-workspace__summary-description,
.device-workspace__section-copy small {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.72rem;
  line-height: 1.56;
}

.device-workspace__metrics {
  list-style: none;
  margin: 0.04rem 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.46rem;
}

.device-workspace__metric {
  display: grid;
  gap: 0.14rem;
  min-width: 0;
  padding: 0.56rem 0.62rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.82rem;
  background: rgba(255, 255, 255, 0.84);
}

.device-workspace__metric span {
  color: var(--text-tertiary);
  font-size: 0.68rem;
  line-height: 1.4;
}

.device-workspace__metric strong {
  color: var(--text-heading);
  font-size: 0.84rem;
  font-weight: 600;
  line-height: 1.42;
}

.device-workspace__metric small {
  color: var(--text-secondary);
  font-size: 0.7rem;
  line-height: 1.52;
}

.device-workspace__metric[data-tone='brand'] {
  border-color: color-mix(in srgb, var(--brand) 18%, transparent);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.device-workspace__metric[data-tone='accent'] {
  border-color: color-mix(in srgb, var(--accent) 18%, transparent);
  background: color-mix(in srgb, var(--accent) 8%, white);
}

.device-workspace__metric[data-tone='success'] {
  border-color: color-mix(in srgb, var(--success) 20%, transparent);
  background: color-mix(in srgb, var(--success) 10%, white);
}

.device-workspace__metric[data-tone='danger'] {
  border-color: color-mix(in srgb, var(--danger) 22%, transparent);
  background: color-mix(in srgb, var(--danger) 8%, white);
}

.device-workspace__table-shell {
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 0.94rem;
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
  .device-workspace__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .device-workspace__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
