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
          <div class="device-drawer__summary">
            <div class="device-summary__item">
              <span class="device-summary__label">设备总数</span>
              <strong class="device-summary__value">{{ totalDevices }}</strong>
            </div>
            <div class="device-summary__item">
              <span class="device-summary__label">在线设备</span>
              <strong class="device-summary__value">{{ onlineDevices }}</strong>
            </div>
            <div class="device-summary__item">
              <span class="device-summary__label">离线设备</span>
              <strong class="device-summary__value">{{ offlineDevices }}</strong>
            </div>
          </div>
          <el-table
            v-loading="devicesLoading"
            :data="devices"
            border
            stripe
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
                <el-button type="primary" link @click="handleViewDevice(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </slot>
    </div>

    <template v-if="$slots.footer" #footer>
      <div class="device-drawer__footer">
        <slot name="footer" />
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import type { Device } from '@/types/api'
import { formatDateTime } from '@/utils/format'

withDefaults(
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

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'viewDevice', device: Device): void;
}>();

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
  padding: 26px 28px 22px;
  border-bottom: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.92)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 28%);
}

.device-list-drawer :deep(.el-drawer__body) {
  padding: 24px 28px 28px;
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
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.device-drawer__heading h2 {
  margin: 0.45rem 0 0;
  color: var(--text-heading);
  font-size: clamp(1.65rem, 2.2vw, 2.05rem);
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.device-drawer__subtitle {
  margin: 0.7rem 0 0;
  max-width: 40rem;
  color: var(--text-caption);
  font-size: 14px;
  line-height: 1.6;
}

.device-drawer__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.device-drawer__tags :deep(.el-tag) {
  min-height: 1.8rem;
  border-radius: var(--radius-pill);
  padding-inline: 0.7rem;
  border-color: color-mix(in srgb, var(--accent) 10%, transparent);
}

.device-drawer__body {
  display: grid;
  gap: 1.1rem;
}

.device-state {
  padding: 1.1rem 1.25rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 250, 255, 0.92));
  color: var(--text-caption);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.device-state--error {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 22%, transparent);
  background: linear-gradient(180deg, rgba(255, 246, 246, 0.98), rgba(255, 241, 241, 0.96));
}

.device-drawer__footer {
  padding: 0 28px 24px;
}

.device-drawer__content {
  display: grid;
  gap: 1.2rem;
}

.device-drawer__summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.device-summary__item {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.92));
}

.device-summary__label {
  color: var(--text-caption-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.device-summary__value {
  color: var(--text-heading);
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
}

.device-summary__value--online {
  color: var(--success);
}

.device-summary__value--offline {
  color: var(--danger);
}

.device-drawer__table :deep(.el-table) {
  background: transparent;
}

.device-drawer__table :deep(.el-table th.el-table__cell) {
  background: color-mix(in srgb, var(--brand) 6%, white);
  border-bottom-color: color-mix(in srgb, var(--brand) 10%, transparent);
}

.device-drawer__table :deep(.el-table__row:hover) {
  background: color-mix(in srgb, var(--brand) 4%, transparent);
}

@media (max-width: 768px) {
  .device-drawer__summary {
    grid-template-columns: repeat(1, 1fr);
  }
}
</style>