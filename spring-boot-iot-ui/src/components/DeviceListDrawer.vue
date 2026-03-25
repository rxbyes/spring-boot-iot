<template>
  <el-drawer
    :model-value="modelValue"
    :size="size"
    direction="rtl"
    :destroy-on-close="destroyOnClose"
    class="device-list-drawer"
    @close="emit('update:modelValue', false)"
  >
    <template #header>
      <div class="device-drawer__header">
        <div class="device-drawer__heading">
          <p v-if="eyebrow" class="device-drawer__eyebrow">{{ eyebrow }}</p>
          <h2>{{ title }}</h2>
          <p v-if="subtitle" class="device-drawer__subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="tags.length" class="device-drawer__tags">
          <el-tag
            v-for="tag in tags"
            :key="`${tag.label}-${tag.type || 'info'}-${tag.effect || 'light'}`"
            :type="tag.type || 'info'"
            :effect="tag.effect || 'light'"
            round
          >
            {{ tag.label }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="device-drawer__body">
      <div v-if="loading" class="device-state">{{ loadingText }}</div>
      <div v-else-if="errorMessage" class="device-state device-state--error">{{ errorMessage }}</div>
      <div v-else-if="empty" class="device-state">{{ emptyText }}</div>
      <slot v-else>
        <div class="device-drawer__content">
          <section class="device-drawer__summary">
            <div class="device-drawer__summary-copy">
              <p class="device-drawer__summary-title">设备运行概览</p>
              <p class="device-drawer__summary-description">统一查看当前产品关联设备的在线状态、激活状态和最近上报。</p>
            </div>

            <ul class="device-drawer__metrics">
              <li
                v-for="metric in summaryMetrics"
                :key="metric.key"
                class="device-drawer__metric"
                :data-tone="metric.tone"
              >
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
                <small>{{ metric.hint }}</small>
              </li>
            </ul>
          </section>

          <section class="device-drawer__section">
            <div class="device-drawer__section-head">
              <strong>关联设备台账</strong>
              <small>快速核对设备身份、在线状态、激活状态和最近上报。</small>
            </div>

            <div class="device-drawer__table-shell">
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
                <el-table-column label="操作" width="100" fixed="right" show-overflow-tooltip>
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
      </slot>
    </div>

    <template #footer>
      <div v-if="hasFooterSlot" class="device-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, toRefs, useSlots } from 'vue'

import type { Device } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    eyebrow?: string;
    subtitle?: string;
    size?: string;
    destroyOnClose?: boolean;
    loading?: boolean;
    loadingText?: string;
    errorMessage?: string;
    empty?: boolean;
    emptyText?: string;
    tags?: Array<{
      label: string;
      type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
      effect?: 'dark' | 'light' | 'plain';
    }>;
    devices: Device[];
    totalDevices: number;
    onlineDevices: number;
    offlineDevices: number;
    devicesLoading?: boolean;
  }>(),
  {
    eyebrow: '',
    subtitle: '',
    size: '48rem',
    destroyOnClose: true,
    loading: false,
    loadingText: '正在加载设备...',
    errorMessage: '',
    empty: false,
    emptyText: '暂无设备数据',
    tags: () => [],
    devices: () => [],
    totalDevices: 0,
    onlineDevices: 0,
    offlineDevices: 0,
    devicesLoading: false
  }
);

const {
  modelValue,
  title,
  eyebrow,
  subtitle,
  size,
  destroyOnClose,
  loading,
  loadingText,
  errorMessage,
  empty,
  emptyText,
  tags,
  devices,
  totalDevices,
  onlineDevices,
  offlineDevices,
  devicesLoading
} = toRefs(props)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'viewDevice', device: Device): void;
}>();

const slots = useSlots()
const hasFooterSlot = computed(() => Boolean(slots.footer))
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
  emit('viewDevice', device);
}
</script>

<style scoped>
.device-list-drawer :deep(.el-drawer) {
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent) 10%, transparent), transparent 30%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--brand) 8%, transparent), transparent 22%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.99), rgba(244, 248, 253, 0.99));
  box-shadow: var(--shadow-drawer);
}

.device-list-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 28%);
}

.device-list-drawer :deep(.el-drawer__body) {
  padding: 18px 24px 24px;
  background: transparent;
}

.device-list-drawer :deep(.el-drawer__footer) {
  padding: 0;
  background: transparent;
}

.device-drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.25rem;
}

.device-drawer__heading {
  min-width: 0;
}

.device-drawer__eyebrow {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 58%, var(--text-caption));
  font-size: 11.5px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.device-drawer__heading h2 {
  margin: 0.28rem 0 0;
  color: var(--text-heading);
  font-size: clamp(1.4rem, 2vw, 1.64rem);
  line-height: 1.24;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.device-drawer__subtitle {
  margin: 0.5rem 0 0;
  max-width: 40rem;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 1.58;
}

.device-drawer__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.device-drawer__tags :deep(.el-tag) {
  min-height: 1.68rem;
  border-radius: var(--radius-pill);
  padding-inline: 0.64rem;
  border-color: color-mix(in srgb, var(--accent) 10%, transparent);
  background: rgba(255, 255, 255, 0.84);
}

.device-drawer__body {
  display: grid;
  gap: 0.92rem;
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

.device-drawer__footer {
  padding: 0 24px 22px;
}

.device-drawer__content {
  display: grid;
  gap: 0.92rem;
}

.device-drawer__summary {
  display: grid;
  gap: 0.5rem;
  padding: 0.8rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--panel-border));
  border-radius: 0.98rem;
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand) 8%, white), var(--bg-card));
}

.device-drawer__summary-copy {
  display: grid;
  gap: 0.18rem;
}

.device-drawer__summary-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.84rem;
  font-weight: 600;
}

.device-drawer__summary-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.74rem;
  line-height: 1.56;
}

.device-drawer__metrics {
  list-style: none;
  margin: 0.04rem 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.46rem;
}

.device-drawer__metric {
  display: grid;
  gap: 0.14rem;
  min-width: 0;
  padding: 0.56rem 0.62rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.82rem;
  background: rgba(255, 255, 255, 0.84);
}

.device-drawer__metric span {
  color: var(--text-tertiary);
  font-size: 0.68rem;
  line-height: 1.4;
}

.device-drawer__metric strong {
  color: var(--text-heading);
  font-size: 0.84rem;
  font-weight: 600;
  line-height: 1.42;
}

.device-drawer__metric small {
  color: var(--text-secondary);
  font-size: 0.7rem;
  line-height: 1.52;
}

.device-drawer__metric[data-tone='brand'] {
  border-color: color-mix(in srgb, var(--brand) 18%, transparent);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.device-drawer__metric[data-tone='accent'] {
  border-color: color-mix(in srgb, var(--accent) 18%, transparent);
  background: color-mix(in srgb, var(--accent) 8%, white);
}

.device-drawer__metric[data-tone='success'] {
  border-color: color-mix(in srgb, var(--success) 20%, transparent);
  background: color-mix(in srgb, var(--success) 10%, white);
}

.device-drawer__metric[data-tone='danger'] {
  border-color: color-mix(in srgb, var(--danger) 22%, transparent);
  background: color-mix(in srgb, var(--danger) 8%, white);
}

.device-drawer__section {
  display: grid;
  gap: 0.46rem;
}

.device-drawer__section-head {
  display: grid;
  gap: 0.12rem;
  padding: 0 0.12rem;
}

.device-drawer__section-head strong {
  color: var(--text-heading);
  font-size: 0.82rem;
  font-weight: 600;
}

.device-drawer__section-head small {
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.device-drawer__table-shell {
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 0.94rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 255, 0.94));
  box-shadow: var(--shadow-surface-soft-sm);
}

.device-drawer__table :deep(.el-table) {
  background: transparent;
}

.device-drawer__table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.device-drawer__table :deep(.el-table th.el-table__cell) {
  background: color-mix(in srgb, var(--brand) 6%, white);
  border-bottom-color: color-mix(in srgb, var(--brand) 10%, transparent);
}

.device-drawer__table :deep(.el-table td.el-table__cell) {
  border-bottom-color: color-mix(in srgb, var(--brand) 8%, transparent);
}

.device-drawer__table :deep(.el-table__row:hover) {
  background: color-mix(in srgb, var(--brand) 4%, transparent);
}

@media (max-width: 900px) {
  .device-drawer__header {
    flex-direction: column;
  }

  .device-drawer__tags {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .device-drawer__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
