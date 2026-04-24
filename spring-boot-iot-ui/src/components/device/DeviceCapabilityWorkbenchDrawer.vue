<template>
  <StandardDetailDrawer
    v-model="visible"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :empty="!device"
    size="52rem"
    destroy-on-close
  >
    <div v-if="device" class="device-capability-workbench-drawer">
      <section class="device-capability-workbench-drawer__summary">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="device-capability-workbench-drawer__summary-card"
        >
          <span class="device-capability-workbench-drawer__summary-label">{{ card.label }}</span>
          <strong class="device-capability-workbench-drawer__summary-value">{{ card.value }}</strong>
        </article>
      </section>

      <StandardInlineState
        v-if="showLoadingState"
        message="正在加载设备能力..."
        tone="info"
      />

      <DeviceCapabilityPanel
        v-else
        :overview="overview"
        :commands="commands"
        :loading="capabilityLoading"
        :command-loading="commandLoading"
        @execute="(capability) => emit('executeCapability', capability)"
        @refreshCommands="emit('refreshCommands')"
      />
    </div>

    <template #footer>
      <div class="device-capability-workbench-drawer__footer">
        <StandardButton
          action="cancel"
          class="device-capability-workbench-drawer__close"
          @click="visible = false"
        >
          关闭
        </StandardButton>
      </div>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import DeviceCapabilityPanel from '@/components/device/DeviceCapabilityPanel.vue'
import type { CommandRecordPageItem, Device, DeviceCapability, DeviceCapabilityOverview } from '@/types/api'

type SummaryCard = {
  key: string
  label: string
  value: string
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    device?: Device | null
    overview?: DeviceCapabilityOverview | null
    commands?: CommandRecordPageItem[]
    capabilityLoading?: boolean
    commandLoading?: boolean
  }>(),
  {
    device: null,
    overview: null,
    commands: () => [],
    capabilityLoading: false,
    commandLoading: false
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'executeCapability', capability: DeviceCapability): void
  (event: 'refreshCommands'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const drawerTitle = computed(() => '设备操作')
const drawerSubtitle = computed(() => {
  if (!props.device) {
    return ''
  }
  const name = props.device.deviceName || props.device.deviceCode || '当前设备'
  const product = props.device.productName || props.device.productKey || '--'
  return `${name} · ${product}`
})

const summaryCards = computed<SummaryCard[]>(() => {
  const device = props.device
  if (!device) {
    return []
  }
  return [
    {
      key: 'deviceCode',
      label: '设备编码',
      value: toDisplayText(device.deviceCode)
    },
    {
      key: 'deviceName',
      label: '设备名称',
      value: toDisplayText(device.deviceName)
    },
    {
      key: 'productName',
      label: '产品归属',
      value: toDisplayText(device.productName || device.productKey)
    },
    {
      key: 'status',
      label: '状态概览',
      value: `${getOnlineStatusText(device.onlineStatus)} / ${getActivateStatusText(device.activateStatus)} / ${getDeviceStatusText(device.deviceStatus)}`
    }
  ]
})

const showLoadingState = computed(() => props.capabilityLoading === true && !props.overview)

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function getOnlineStatusText(value?: number | null) {
  return value === 1 ? '在线' : '离线'
}

function getActivateStatusText(value?: number | null) {
  return value === 1 ? '已激活' : '未激活'
}

function getDeviceStatusText(value?: number | null) {
  return value === 1 ? '启用' : '禁用'
}
</script>

<style scoped>
.device-capability-workbench-drawer {
  display: grid;
  gap: 1rem;
}

.device-capability-workbench-drawer__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.device-capability-workbench-drawer__summary-card {
  display: grid;
  gap: 0.3rem;
  min-width: 0;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.94);
}

.device-capability-workbench-drawer__summary-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.device-capability-workbench-drawer__summary-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.device-capability-workbench-drawer__footer {
  display: flex;
  justify-content: flex-end;
  padding: 0 28px 28px;
}

@media (max-width: 960px) {
  .device-capability-workbench-drawer__summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-capability-workbench-drawer__footer {
    padding-inline: 20px;
  }
}
</style>
