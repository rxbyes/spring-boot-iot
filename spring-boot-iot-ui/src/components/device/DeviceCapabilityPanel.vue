<template>
  <div class="device-capability-panel">
    <section class="device-capability-panel__stage" data-testid="device-capability-summary-stage">
      <div class="device-capability-panel__stage-header">
        <div>
          <h3>设备能力与命令</h3>
          <p>按产品元数据自动展示可执行能力，并同步最近下发记录。</p>
        </div>

        <div class="device-capability-panel__tags">
          <el-tag v-if="capabilityTypeLabel" round>{{ capabilityTypeLabel }}</el-tag>
          <el-tag v-if="subTypeLabel" round>{{ subTypeLabel }}</el-tag>
        </div>
      </div>

      <StandardInlineState
        v-if="overview?.disabledReason"
        :message="overview.disabledReason"
        tone="error"
      />

      <template v-if="groupEntries.length">
        <section
          v-for="group in groupEntries"
          :key="group.key"
          class="device-capability-panel__group"
        >
          <div class="device-capability-panel__group-header">
            <div>
              <h4>{{ group.label }}</h4>
              <p>{{ group.description }}</p>
            </div>
            <span class="device-capability-panel__group-count">{{ group.items.length }} 项</span>
          </div>

          <div class="device-capability-panel__capabilities">
            <StandardButton
              v-for="capability in group.items"
              :key="capability.code"
              :action="capability.enabled ? 'confirm' : 'default'"
              class="device-capability-panel__capability-button"
              :disabled="isCapabilityDisabled(capability)"
              :title="resolveCapabilityDisabledReason(capability) || undefined"
              @click="emit('execute', capability)"
            >
              {{ capability.name }}
            </StandardButton>
          </div>

          <p v-if="group.hint" class="device-capability-panel__group-hint">
            {{ group.hint }}
          </p>
        </section>
      </template>

      <el-empty v-else description="当前产品暂未开放可执行能力" />
    </section>

    <section class="device-capability-panel__stage" data-testid="device-capability-command-stage">
      <StandardTableToolbar
        compact
        :meta-items="commandMetaItems"
      >
        <template #right>
          <StandardButton action="refresh" link :loading="commandLoading" @click="emit('refreshCommands')">
            刷新命令
          </StandardButton>
        </template>
      </StandardTableToolbar>

      <div class="device-capability-panel__command-table" v-loading="commandLoading">
        <el-table
          v-if="commands.length"
          :data="commands"
          border
          stripe
          size="small"
        >
          <el-table-column prop="commandId" label="命令号" min-width="168" />
          <el-table-column prop="serviceIdentifier" label="能力" min-width="132" />
          <el-table-column prop="status" label="状态" width="108">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" round>{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sendTime" label="下发时间" min-width="178">
            <template #default="{ row }">
              {{ formatDateTime(row.sendTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="ackTime" label="反馈时间" min-width="178">
            <template #default="{ row }">
              {{ formatDateTime(row.ackTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="topic" label="Topic" min-width="220" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="摘要" min-width="180" show-overflow-tooltip />
        </el-table>

        <el-empty v-else description="最近暂无设备命令记录" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import type { CommandRecordPageItem, DeviceCapability, DeviceCapabilityOverview } from '@/types/api'
import { formatDateTime } from '@/utils/format'

type CapabilityGroupEntry = {
  key: string
  label: string
  description: string
  hint?: string
  items: DeviceCapability[]
}

const props = withDefaults(
  defineProps<{
    overview?: DeviceCapabilityOverview | null
    commands?: CommandRecordPageItem[]
    loading?: boolean
    commandLoading?: boolean
  }>(),
  {
    overview: null,
    commands: () => [],
    loading: false,
    commandLoading: false
  }
)

const emit = defineEmits<{
  (event: 'execute', capability: DeviceCapability): void
  (event: 'refreshCommands'): void
}>()

const capabilityTypeLabelMap: Record<string, string> = {
  COLLECTING: '采集型',
  MONITORING: '监测型',
  WARNING: '预警型',
  VIDEO: '视频型',
  UNKNOWN: '未知能力'
}

const subTypeLabelMap: Record<string, string> = {
  BROADCAST: '广播喇叭',
  LED: '情报板',
  FLASH: '爆闪灯',
  FIXED_CAMERA: '固定摄像头',
  PTZ_CAMERA: '视频云台'
}

const groupMetaMap: Record<string, { label: string; description: string; hint: string }> = {
  基础维护: {
    label: '基础维护',
    description: '面向在线设备的常规运维操作。',
    hint: '重启、开关和固件升级需要设备保持在线。'
  },
  广播预警: {
    label: '广播预警',
    description: '广播喇叭的内容播报与音量控制。',
    hint: '播报类能力会按下发内容拼装查询串。'
  },
  情报板: {
    label: '情报板',
    description: '情报板节目和停播控制。',
    hint: '节目控制会携带 type、brigh 和 freq 参数。'
  },
  爆闪灯: {
    label: '爆闪灯',
    description: '爆闪灯控制与停机操作。',
    hint: '爆闪控制会携带 type、brigh 和 freq 参数。'
  },
  视频控制: {
    label: '视频控制',
    description: '视频播放、停止与方位角转向。',
    hint: 'PTZ 设备会额外开放方位角转向能力。'
  },
  未分组: {
    label: '未分组',
    description: '未归类能力。',
    hint: ''
  }
}

const capabilityTypeLabel = computed(() => capabilityTypeLabelMap[props.overview?.productCapabilityType || 'UNKNOWN'] || '未知能力')
const subTypeLabel = computed(() => {
  const subType = props.overview?.subType
  if (!subType) {
    return ''
  }
  return subTypeLabelMap[subType] || subType
})

const groupEntries = computed<CapabilityGroupEntry[]>(() => {
  const groups = new Map<string, DeviceCapability[]>()
  for (const capability of props.overview?.capabilities || []) {
    const groupKey = capability.group?.trim() || '未分组'
    const items = groups.get(groupKey) || []
    items.push(capability)
    groups.set(groupKey, items)
  }

  return Array.from(groups.entries()).map(([key, items]) => {
    const meta = groupMetaMap[key] || groupMetaMap.未分组
    return {
      key,
      label: meta.label,
      description: meta.description,
      hint: meta.hint,
      items
    }
  })
})

const commandMetaItems = computed(() => [
  `最近命令 ${props.commands?.length || 0} 条`,
  `已反馈 ${props.commands?.filter((item) => Boolean(item.ackTime)).length || 0} 条`,
  `失败 ${props.commands?.filter((item) => item.status === 'FAILED').length || 0} 条`
])

function isCapabilityDisabled(capability: DeviceCapability) {
  return !capability.enabled || Boolean(resolveCapabilityDisabledReason(capability))
}

function resolveCapabilityDisabledReason(capability: DeviceCapability) {
  return capability.disabledReason || props.overview?.disabledReason || ''
}

function getStatusLabel(value?: string | null) {
  if (value === 'SENT') {
    return '已下发'
  }
  if (value === 'SUCCESS') {
    return '执行成功'
  }
  if (value === 'FAILED') {
    return '执行失败'
  }
  if (value === 'TIMEOUT') {
    return '反馈超时'
  }
  if (value === 'CREATED') {
    return '已创建'
  }
  return value || '--'
}

function getStatusTagType(value?: string | null) {
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'FAILED' || value === 'TIMEOUT') {
    return 'danger'
  }
  if (value === 'SENT') {
    return 'warning'
  }
  return 'info'
}
</script>

<style scoped>
.device-capability-panel {
  display: grid;
  gap: 1rem;
}

.device-capability-panel__stage {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.device-capability-panel__stage-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.device-capability-panel__stage-header h3,
.device-capability-panel__group-header h4 {
  margin: 0;
  color: var(--text-heading);
  line-height: 1.4;
}

.device-capability-panel__stage-header h3 {
  font-size: 16px;
}

.device-capability-panel__stage-header p,
.device-capability-panel__group-header p,
.device-capability-panel__group-hint {
  margin: 0.3rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.device-capability-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  justify-content: flex-end;
}

.device-capability-panel__group {
  display: grid;
  gap: 0.75rem;
  padding: 0.88rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.95);
}

.device-capability-panel__group-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.device-capability-panel__group-count {
  flex: none;
  color: color-mix(in srgb, var(--brand) 72%, var(--text-caption));
  font-size: 12px;
  font-weight: 700;
}

.device-capability-panel__capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.device-capability-panel__capability-button {
  min-width: 7.5rem;
}

.device-capability-panel__command-table {
  min-height: 8rem;
}

.device-capability-panel__command-table :deep(.el-table) {
  width: 100%;
}

@media (max-width: 900px) {
  .device-capability-panel__stage-header,
  .device-capability-panel__group-header {
    flex-direction: column;
  }

  .device-capability-panel__tags {
    justify-content: flex-start;
  }
}
</style>
