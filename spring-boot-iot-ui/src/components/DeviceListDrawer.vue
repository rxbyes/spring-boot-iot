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
      <slot>
        <ProductDeviceListWorkspace
          :devices="devices"
          :total-devices="totalDevices"
          :online-devices="onlineDevices"
          :offline-devices="offlineDevices"
          :loading="loading"
          :loading-text="loadingText"
          :error-message="errorMessage"
          :empty="empty"
          :empty-text="emptyText"
          :devices-loading="devicesLoading"
          @view-device="handleViewDevice"
        />
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

import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'
import type { Device } from '@/types/api'

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
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

.device-drawer__heading h2 {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.4rem, 2vw, 1.64rem);
  line-height: 1.24;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.device-drawer__subtitle {
  margin: 0.42rem 0 0;
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

@media (max-width: 900px) {
  .device-drawer__header {
    flex-direction: column;
  }

  .device-drawer__tags {
    justify-content: flex-start;
  }
}

</style>
