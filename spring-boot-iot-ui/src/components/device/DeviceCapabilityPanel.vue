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

      <section v-if="summaryCards.length" class="device-capability-panel__summary-strip">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          :class="[
            'device-capability-panel__summary-card',
            { 'device-capability-panel__summary-card--primary': card.primary }
          ]"
        >
          <span class="device-capability-panel__summary-label">{{ card.label }}</span>
          <strong class="device-capability-panel__summary-value">{{ card.value }}</strong>
          <div v-if="card.meta?.length" class="device-capability-panel__summary-meta">
            <small v-for="item in card.meta" :key="item">{{ item }}</small>
          </div>
        </article>
      </section>

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
            <span class="device-capability-panel__group-count-pill">{{ group.items.length }} 项</span>
          </div>

          <div class="device-capability-panel__capabilities">
            <article
              v-for="capability in group.items"
              :key="capability.code"
              :class="[
                'device-capability-panel__capability-card',
                { 'device-capability-panel__capability-card--disabled': isCapabilityDisabled(capability) }
              ]"
            >
              <StandardButton
                v-permission="'iot:device-capability:execute'"
                :action="capability.enabled ? 'confirm' : 'default'"
                class="device-capability-panel__capability-button"
                :disabled="isCapabilityDisabled(capability)"
                :title="resolveCapabilityDisabledReason(capability) || undefined"
                @click="emit('execute', capability)"
              >
                {{ capability.name }}
              </StandardButton>

              <div class="device-capability-panel__capability-state">
                <el-tag :type="getCapabilityStateTagType(capability)" round size="small">
                  {{ getCapabilityStateLabel(capability) }}
                </el-tag>
                <span>{{ getCapabilityStateDescription(capability) }}</span>
              </div>
            </article>
          </div>

          <p v-if="group.hint" class="device-capability-panel__group-hint">
            {{ group.hint }}
          </p>
        </section>
      </template>

      <el-empty v-else description="当前产品暂未开放可执行能力" />
    </section>

    <section class="device-capability-panel__stage" data-testid="device-capability-command-stage">
      <div class="device-capability-panel__command-stage-header">
        <div>
          <h3>命令记录</h3>
          <p>保留最近下发与反馈结果，便于回看执行链路。</p>
        </div>
      </div>

      <StandardTableToolbar compact :meta-items="commandMetaItems">
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
              <span
                :class="[
                  'device-capability-panel__command-status-pill',
                  `device-capability-panel__command-status-pill--${getCommandStatusTone(row.status)}`
                ]"
              >
                {{ getStatusLabel(row.status) }}
              </span>
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

type SummaryCard = {
  key: string
  label: string
  value: string
  meta?: string[]
  primary?: boolean
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

const defaultGroupMeta = {
  label: '未分组',
  description: '未归类能力。',
  hint: ''
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
  未分组: defaultGroupMeta
}

const capabilityTypeLabel = computed(
  () => capabilityTypeLabelMap[props.overview?.productCapabilityType || 'UNKNOWN'] || '未知能力'
)
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
    const meta = groupMetaMap[key] || defaultGroupMeta
    return {
      key,
      label: meta.label,
      description: meta.description,
      hint: meta.hint,
      items
    }
  })
})

const summaryCards = computed<SummaryCard[]>(() => {
  const capabilities = props.overview?.capabilities || []
  const executableCount = capabilities.filter((capability) => !isCapabilityDisabled(capability)).length
  const blockedCount = capabilities.length - executableCount
  const groupCount = groupEntries.value.length

  return [
    {
      key: 'total',
      label: '总能力',
      value: `${capabilities.length} 项`,
      meta: [`可执行 ${executableCount} 项`, `受限 ${blockedCount} 项`],
      primary: true
    },
    {
      key: 'groups',
      label: '能力分组',
      value: `${groupCount} 组`,
      meta: [capabilityTypeLabel.value, subTypeLabel.value || '未细分']
    },
    {
      key: 'commands',
      label: '最近命令',
      value: `${props.commands?.length || 0} 条`,
      meta: [
        `已反馈 ${props.commands?.filter((item) => Boolean(item.ackTime)).length || 0} 条`,
        `失败 ${props.commands?.filter((item) => item.status === 'FAILED').length || 0} 条`
      ]
    }
  ]
})

const commandMetaItems = computed(() => [
  `最近命令 ${props.commands?.length || 0} 条`,
  `已反馈 ${props.commands?.filter((item) => Boolean(item.ackTime)).length || 0} 条`,
  `失败 ${props.commands?.filter((item) => item.status === 'FAILED').length || 0} 条`
])

function isCapabilityDisabled(capability: DeviceCapability) {
  return (
    !capability.enabled ||
    Boolean(resolveCapabilityDisabledReason(capability)) ||
    Boolean(capability.requiresOnline && props.overview?.onlineExecutable === false)
  )
}

function resolveCapabilityDisabledReason(capability: DeviceCapability) {
  if (capability.disabledReason) {
    return capability.disabledReason
  }
  if (props.overview?.disabledReason) {
    return props.overview.disabledReason
  }
  if (capability.requiresOnline && props.overview?.onlineExecutable === false) {
    return '当前设备离线，暂不可执行'
  }
  return ''
}

function getCapabilityStateLabel(capability: DeviceCapability) {
  return isCapabilityDisabled(capability) ? '受限' : '可执行'
}

function getCapabilityStateDescription(capability: DeviceCapability) {
  const reason = resolveCapabilityDisabledReason(capability)
  if (reason) {
    return reason
  }
  if (!capability.enabled) {
    return '当前产品未开放该能力'
  }
  if (capability.requiresOnline && props.overview?.onlineExecutable === false) {
    return '当前设备离线，暂不可执行'
  }
  return '点击可直接下发'
}

function getCapabilityStateTagType(capability: DeviceCapability) {
  return isCapabilityDisabled(capability) ? 'warning' : 'success'
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

function getCommandStatusTone(value?: string | null) {
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

.device-capability-panel__command-stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.4;
}

.device-capability-panel__command-stage-header p {
  margin: 0.28rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
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

.device-capability-panel__summary-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}

.device-capability-panel__summary-card {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
  padding: 0.78rem 0.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-capability-panel__summary-card--primary {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.95)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 9%, transparent), transparent 40%);
}

.device-capability-panel__summary-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.device-capability-panel__summary-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.device-capability-panel__summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.7rem;
}

.device-capability-panel__summary-meta small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
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

.device-capability-panel__group-count-pill {
  flex: none;
  align-self: center;
  padding: 0.18rem 0.55rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 9%, rgba(255, 255, 255, 0.96));
  color: color-mix(in srgb, var(--brand) 74%, var(--text-caption));
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
}

.device-capability-panel__capabilities {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 0.65rem;
}

.device-capability-panel__capability-card {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.78rem 0.82rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.92);
}

.device-capability-panel__capability-card--disabled {
  border-color: color-mix(in srgb, var(--warning) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--warning) 5%, rgba(255, 255, 255, 0.92));
}

.device-capability-panel__capability-button {
  width: 100%;
}

.device-capability-panel__capability-state {
  display: flex;
  gap: 0.45rem;
  align-items: flex-start;
  min-width: 0;
}

.device-capability-panel__capability-state span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.55;
}

.device-capability-panel__capability-state :deep(.el-tag) {
  flex: none;
}

.device-capability-panel__command-table {
  min-height: 8rem;
}

.device-capability-panel__command-table :deep(.el-table) {
  width: 100%;
}

.device-capability-panel__command-status-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.58rem;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.device-capability-panel__command-status-pill--success {
  background: color-mix(in srgb, var(--success) 10%, rgba(255, 255, 255, 0.96));
  color: color-mix(in srgb, var(--success) 72%, var(--text-secondary));
}

.device-capability-panel__command-status-pill--warning {
  background: color-mix(in srgb, var(--warning) 12%, rgba(255, 255, 255, 0.96));
  color: color-mix(in srgb, var(--warning) 72%, var(--text-secondary));
}

.device-capability-panel__command-status-pill--danger {
  background: color-mix(in srgb, var(--danger) 10%, rgba(255, 255, 255, 0.96));
  color: color-mix(in srgb, var(--danger) 72%, var(--text-secondary));
}

.device-capability-panel__command-status-pill--info {
  background: color-mix(in srgb, var(--brand) 8%, rgba(255, 255, 255, 0.96));
  color: var(--text-secondary);
}

@media (max-width: 900px) {
  .device-capability-panel__stage-header,
  .device-capability-panel__group-header {
    flex-direction: column;
  }

  .device-capability-panel__tags {
    justify-content: flex-start;
  }

  .device-capability-panel__summary-strip {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
