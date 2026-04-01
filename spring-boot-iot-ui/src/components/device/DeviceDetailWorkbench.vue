<template>
  <div class="device-detail-workbench">
    <section class="device-detail-workbench__stage device-detail-workbench__stage--summary" data-testid="device-detail-summary-stage">
      <div class="device-detail-workbench__stage-header">
        <div>
          <h3>资产状态与接入概况</h3>
          <p>{{ summaryDescription }}</p>
        </div>
      </div>

      <div class="device-detail-workbench__summary-grid">
        <article
          v-for="cell in summaryCells"
          :key="cell.key"
          class="device-detail-workbench__summary-cell"
        >
          <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
          <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
          <p class="device-detail-workbench__cell-hint">{{ cell.hint }}</p>
        </article>
      </div>
    </section>

    <template v-if="isRegistered">
      <div class="device-detail-workbench__ledger-pair">
        <section class="device-detail-workbench__stage" data-testid="device-detail-identity-stage">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>身份与部署台账</h3>
              <p>把设备身份、产品归属和部署上下文压回同一层台账，减少来回切段。</p>
            </div>
          </div>

          <div class="device-detail-workbench__ledger-grid device-detail-workbench__identity-grid">
            <article
              v-for="cell in identityCells"
              :key="cell.key"
              class="device-detail-workbench__ledger-cell"
            >
              <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
              <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
            </article>
          </div>
        </section>

        <section class="device-detail-workbench__stage" data-testid="device-detail-runtime-stage">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>运行与认证台账</h3>
              <p>把运行节奏和接入凭据放在同一侧，便于现场和接入侧按一条阅读路径核对。</p>
            </div>
          </div>

          <div class="device-detail-workbench__ledger-grid device-detail-workbench__runtime-grid">
            <article
              v-for="cell in runtimeCells"
              :key="cell.key"
              class="device-detail-workbench__ledger-cell"
            >
              <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
              <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
            </article>
          </div>
        </section>
      </div>

      <div class="device-detail-workbench__support-pair" data-testid="device-detail-support-pair">
        <section class="device-detail-workbench__stage device-detail-workbench__stage--support">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>关系与建档补充</h3>
              <p>父设备、网关关系和关键主键下沉为补充层，不再抢占主账本节奏。</p>
            </div>
          </div>

          <div class="device-detail-workbench__ledger-grid device-detail-workbench__ledger-grid--compact">
            <article
              v-for="cell in relationCells"
              :key="cell.key"
              class="device-detail-workbench__ledger-cell"
            >
              <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
              <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
            </article>
          </div>
        </section>

        <section class="device-detail-workbench__stage device-detail-workbench__stage--support">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>扩展元数据快照</h3>
              <p>补充库存、站点、责任人与批次信息，保持在详情尾部安静展示。</p>
            </div>
          </div>

          <pre class="device-detail-workbench__code-block">{{ metadataPreview }}</pre>
        </section>
      </div>
    </template>

    <template v-else>
      <div class="device-detail-workbench__ledger-pair device-detail-workbench__ledger-pair--unregistered">
        <section class="device-detail-workbench__stage" data-testid="device-detail-source-stage">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>来源档案与失败摘要</h3>
              <p>统一把未登记来源、协议上下文和失败摘要收在一层，便于快速判断是否需要补建设备主档。</p>
            </div>
          </div>

          <div class="device-detail-workbench__ledger-grid device-detail-workbench__ledger-grid--source">
            <article
              v-for="cell in sourceCells"
              :key="cell.key"
              class="device-detail-workbench__ledger-cell"
              :class="{ 'device-detail-workbench__ledger-cell--wide': cell.wide }"
            >
              <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
              <strong class="device-detail-workbench__cell-value device-detail-workbench__cell-value--multiline">{{ cell.value }}</strong>
            </article>
          </div>
        </section>

        <section class="device-detail-workbench__stage" data-testid="device-detail-payload-stage">
          <div class="device-detail-workbench__stage-header">
            <div>
              <h3>最近载荷</h3>
              <p>保留最近一次未登记上报的原始载荷，方便补建主档或回查协议映射。</p>
            </div>
          </div>

          <pre class="device-detail-workbench__code-block">{{ payloadPreview }}</pre>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Device } from '@/types/api'
import { formatDateTime, prettyJson } from '@/utils/format'

type DetailCell = {
  key: string
  label: string
  value: string
  hint?: string
  wide?: boolean
}

const props = defineProps<{
  device: Device
}>()

const device = computed(() => props.device)
const isRegistered = computed(() => device.value.registrationStatus !== 0)

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function getOnlineStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '在线' : '离线'
}

function getActivateStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '已激活' : '未激活'
}

function getDeviceStatusText(value?: number | null) {
  if (value === undefined || value === null) {
    return '--'
  }
  return value === 1 ? '启用' : '禁用'
}

function getRegistrationStatusText(value?: number | null) {
  return value === 0 ? '未登记' : '已登记'
}

function getSourceTypeText(value?: string | null) {
  if (value === 'access_error') {
    return '失败归档'
  }
  if (value === 'dispatch_failed') {
    return '失败轨迹'
  }
  if (value === 'invalid_report') {
    return '未登记上报'
  }
  return '设备主档'
}

function getNodeTypeText(value?: number | null) {
  if (value === 1) {
    return '直连设备'
  }
  if (value === 2) {
    return '网关设备'
  }
  if (value === 3) {
    return '网关子设备'
  }
  return '--'
}

function formatDeviceRelationValue(name?: string | null, code?: string | null) {
  if (name && code) {
    return `${name} (${code})`
  }
  return name || code || '--'
}

function maskSecret(value?: string | null) {
  if (!value) {
    return '--'
  }
  if (value.length <= 4) {
    return '****'
  }
  return `${value.slice(0, 2)}****${value.slice(-2)}`
}

const summaryDescription = computed(() => (
  isRegistered.value
    ? '顶部只保留资产当前判断，完整档案下沉到左右双轴台账，阅读路径更平直。'
    : '未登记设备沿用同一扁平语法，先判断失败阶段，再确认来源与原始载荷。'
))

const summaryCells = computed<DetailCell[]>(() => {
  if (!isRegistered.value) {
    return [
      {
        key: 'registrationStatus',
        label: '登记状态',
        value: getRegistrationStatusText(device.value.registrationStatus),
        hint: getSourceTypeText(device.value.assetSourceType)
      },
      {
        key: 'lastReportTime',
        label: '最近上报',
        value: formatDateTime(device.value.lastReportTime),
        hint: toDisplayText(device.value.lastReportTopic)
      },
      {
        key: 'lastFailureStage',
        label: '失败阶段',
        value: toDisplayText(device.value.lastFailureStage),
        hint: toDisplayText(device.value.lastTraceId)
      },
      {
        key: 'lastErrorMessage',
        label: '失败摘要',
        value: toDisplayText(device.value.lastErrorMessage),
        hint: '优先确认是否需要补建设备主档'
      }
    ]
  }

  return [
    {
      key: 'productName',
      label: '产品归属',
      value: toDisplayText(device.value.productName),
      hint: toDisplayText(device.value.productKey)
    },
    {
      key: 'onlineStatus',
      label: '在线状态',
      value: getOnlineStatusText(device.value.onlineStatus),
      hint: `最近上报 ${formatDateTime(device.value.lastReportTime)}`
    },
    {
      key: 'activateStatus',
      label: '激活状态',
      value: getActivateStatusText(device.value.activateStatus),
      hint: `最近在线 ${formatDateTime(device.value.lastOnlineTime)}`
    },
    {
      key: 'deviceStatus',
      label: '设备状态',
      value: getDeviceStatusText(device.value.deviceStatus),
      hint: `更新时间 ${formatDateTime(device.value.updateTime)}`
    }
  ]
})

const identityCells = computed<DetailCell[]>(() => [
  {
    key: 'id',
    label: '设备 ID',
    value: toDisplayText(device.value.id)
  },
  {
    key: 'deviceCode',
    label: '设备编码',
    value: toDisplayText(device.value.deviceCode)
  },
  {
    key: 'deviceName',
    label: '设备名称',
    value: toDisplayText(device.value.deviceName)
  },
  {
    key: 'productName',
    label: '产品归属',
    value: toDisplayText(device.value.productName)
  },
  {
    key: 'nodeType',
    label: '节点类型',
    value: getNodeTypeText(device.value.nodeType)
  },
  {
    key: 'protocolCode',
    label: '接入协议',
    value: toDisplayText(device.value.protocolCode)
  },
  {
    key: 'firmwareVersion',
    label: '固件版本',
    value: toDisplayText(device.value.firmwareVersion)
  },
  {
    key: 'ipAddress',
    label: 'IP 地址',
    value: toDisplayText(device.value.ipAddress)
  },
  {
    key: 'address',
    label: '部署位置',
    value: toDisplayText(device.value.address)
  }
])

const runtimeCells = computed<DetailCell[]>(() => [
  {
    key: 'lastOnlineTime',
    label: '最近在线',
    value: formatDateTime(device.value.lastOnlineTime)
  },
  {
    key: 'lastOfflineTime',
    label: '最近离线',
    value: formatDateTime(device.value.lastOfflineTime)
  },
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDateTime(device.value.lastReportTime)
  },
  {
    key: 'createTime',
    label: '创建时间',
    value: formatDateTime(device.value.createTime)
  },
  {
    key: 'updateTime',
    label: '更新时间',
    value: formatDateTime(device.value.updateTime)
  },
  {
    key: 'clientId',
    label: 'Client ID',
    value: toDisplayText(device.value.clientId)
  },
  {
    key: 'username',
    label: '用户名',
    value: toDisplayText(device.value.username)
  },
  {
    key: 'password',
    label: '密码',
    value: maskSecret(device.value.password)
  },
  {
    key: 'deviceSecret',
    label: '设备密钥',
    value: maskSecret(device.value.deviceSecret)
  }
])

const relationCells = computed<DetailCell[]>(() => [
  {
    key: 'parentDevice',
    label: '父设备',
    value: formatDeviceRelationValue(device.value.parentDeviceName, device.value.parentDeviceCode)
  },
  {
    key: 'gatewayDevice',
    label: '网关设备',
    value: formatDeviceRelationValue(device.value.gatewayDeviceName, device.value.gatewayDeviceCode)
  },
  {
    key: 'parentDeviceId',
    label: '父设备主键',
    value: toDisplayText(device.value.parentDeviceId)
  },
  {
    key: 'gatewayId',
    label: '网关主键',
    value: toDisplayText(device.value.gatewayId)
  }
])

const sourceCells = computed<DetailCell[]>(() => [
  {
    key: 'deviceCode',
    label: '设备编码',
    value: toDisplayText(device.value.deviceCode)
  },
  {
    key: 'productKey',
    label: '产品标识',
    value: toDisplayText(device.value.productKey)
  },
  {
    key: 'protocolCode',
    label: '协议编码',
    value: toDisplayText(device.value.protocolCode)
  },
  {
    key: 'sourceRecordId',
    label: '来源记录',
    value: toDisplayText(device.value.sourceRecordId)
  },
  {
    key: 'lastReportTopic',
    label: 'Topic',
    value: toDisplayText(device.value.lastReportTopic),
    wide: true
  },
  {
    key: 'lastErrorMessage',
    label: '失败摘要',
    value: toDisplayText(device.value.lastErrorMessage),
    wide: true
  }
])

const metadataPreview = computed(() => prettyJson(device.value.metadataJson || '{}'))
const payloadPreview = computed(() => prettyJson(device.value.lastPayload || '--'))
</script>

<style scoped>
.device-detail-workbench {
  display: grid;
  gap: 1rem;
}

.device-detail-workbench__ledger-pair,
.device-detail-workbench__support-pair {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
}

.device-detail-workbench__stage {
  display: grid;
  gap: 0.92rem;
  padding: 1rem 1.04rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 28px rgba(28, 53, 87, 0.04);
}

.device-detail-workbench__stage--summary {
  background:
    linear-gradient(180deg, rgba(251, 247, 242, 0.98), rgba(255, 255, 255, 0.99)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 40%);
}

.device-detail-workbench__stage--support {
  background: linear-gradient(180deg, rgba(252, 252, 252, 0.98), rgba(255, 255, 255, 0.98));
}

.device-detail-workbench__stage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.8rem;
}

.device-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
  line-height: 1.5;
}

.device-detail-workbench__stage-header p {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 0.81rem;
  line-height: 1.65;
}

.device-detail-workbench__summary-grid {
  display: grid;
  gap: 0.82rem;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.device-detail-workbench__summary-cell,
.device-detail-workbench__ledger-cell {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.88rem 0.94rem;
  border-radius: calc(var(--radius-md) + 2px);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  background: rgba(255, 255, 255, 0.94);
}

.device-detail-workbench__summary-cell {
  min-height: 6.2rem;
}

.device-detail-workbench__ledger-grid {
  display: grid;
  gap: 0.78rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.device-detail-workbench__ledger-grid--compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.device-detail-workbench__ledger-grid--source {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.device-detail-workbench__ledger-cell {
  min-height: 5.3rem;
}

.device-detail-workbench__ledger-cell--wide {
  grid-column: 1 / -1;
}

.device-detail-workbench__cell-label {
  color: var(--text-caption);
  font-size: 0.72rem;
  letter-spacing: 0.04em;
}

.device-detail-workbench__cell-value {
  color: var(--text-heading);
  font-size: 0.92rem;
  font-weight: 600;
  line-height: 1.58;
  word-break: break-word;
}

.device-detail-workbench__cell-value--multiline {
  font-weight: 500;
}

.device-detail-workbench__cell-hint {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.76rem;
  line-height: 1.58;
}

.device-detail-workbench__code-block {
  margin: 0;
  min-height: 12rem;
  padding: 0.96rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: color-mix(in srgb, var(--surface-page) 82%, white);
  color: var(--text-heading);
  font-size: 0.78rem;
  line-height: 1.68;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1080px) {
  .device-detail-workbench__summary-grid,
  .device-detail-workbench__ledger-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .device-detail-workbench__ledger-pair,
  .device-detail-workbench__support-pair,
  .device-detail-workbench__summary-grid,
  .device-detail-workbench__ledger-grid,
  .device-detail-workbench__ledger-grid--compact,
  .device-detail-workbench__ledger-grid--source {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-detail-workbench__ledger-cell--wide {
    grid-column: auto;
  }
}
</style>
