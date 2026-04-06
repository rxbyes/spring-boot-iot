<template>
  <div class="device-detail-workbench">
    <section class="device-detail-workbench__stage" data-testid="device-detail-summary-stage">
      <div class="device-detail-workbench__stage-header">
        <h3>资产状态与接入概况</h3>
      </div>

      <div class="device-detail-workbench__summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.key"
          class="device-detail-workbench__summary-card"
        >
          <span class="device-detail-workbench__summary-label">{{ card.label }}</span>
          <span class="device-detail-workbench__summary-value">{{ card.value }}</span>
          <span class="device-detail-workbench__summary-hint">{{ card.hint }}</span>
        </article>
      </div>
    </section>

    <template v-if="isRegistered">
      <section
        v-if="showOverviewStage"
        class="device-detail-workbench__stage device-detail-workbench__stage--subtle"
        data-testid="device-detail-overview-stage"
      >
        <div class="device-detail-workbench__overview-pair">
          <article class="device-detail-workbench__overview-panel">
            <div class="device-detail-workbench__stage-header">
              <h3>资产概览</h3>
            </div>

            <div class="device-detail-workbench__ledger-grid">
              <article
                v-for="item in assetOverviewItems"
                :key="item.key"
                :class="[
                  'device-detail-workbench__ledger-item',
                  { 'device-detail-workbench__ledger-item--wide': item.wide }
                ]"
              >
                <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
                <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
              </article>
            </div>
          </article>

          <article class="device-detail-workbench__overview-panel">
            <div class="device-detail-workbench__stage-header">
              <h3>运行概览</h3>
            </div>

            <div class="device-detail-workbench__ledger-grid">
              <article
                v-for="item in runtimeOverviewItems"
                :key="item.key"
                :class="[
                  'device-detail-workbench__ledger-item',
                  { 'device-detail-workbench__ledger-item--wide': item.wide }
                ]"
              >
                <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
                <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
              </article>
            </div>
          </article>
        </div>
      </section>

      <section
        v-if="showIdentityStage"
        class="device-detail-workbench__stage"
        data-testid="device-detail-identity-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>身份与部署台账</h3>
        </div>

        <div class="device-detail-workbench__ledger-grid">
          <article
            v-for="item in identityItems"
            :key="item.key"
            :class="[
              'device-detail-workbench__ledger-item',
              { 'device-detail-workbench__ledger-item--wide': item.wide }
            ]"
          >
            <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
            <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showRuntimeStage"
        class="device-detail-workbench__stage"
        data-testid="device-detail-runtime-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>运行与认证台账</h3>
        </div>

        <div class="device-detail-workbench__ledger-grid">
          <article
            v-for="item in runtimeItems"
            :key="item.key"
            :class="[
              'device-detail-workbench__ledger-item',
              { 'device-detail-workbench__ledger-item--wide': item.wide }
            ]"
          >
            <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
            <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showSupportStage"
        class="device-detail-workbench__stage device-detail-workbench__stage--subtle"
        data-testid="device-detail-support-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>关系与建档补充</h3>
        </div>

        <div class="device-detail-workbench__ledger-grid">
          <article
            v-for="item in supportItems"
            :key="item.key"
            :class="[
              'device-detail-workbench__ledger-item',
              { 'device-detail-workbench__ledger-item--wide': item.wide }
            ]"
          >
            <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
            <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showMetadataStage"
        class="device-detail-workbench__stage device-detail-workbench__stage--subtle"
        data-testid="device-detail-metadata-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>扩展元数据快照</h3>
        </div>

        <pre class="device-detail-workbench__code-block">{{ metadataPreview }}</pre>
      </section>
    </template>

    <template v-else>
      <section
        v-if="showSourceStage"
        class="device-detail-workbench__stage"
        data-testid="device-detail-source-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>来源档案与失败摘要</h3>
        </div>

        <div class="device-detail-workbench__ledger-grid">
          <article
            v-for="item in sourceItems"
            :key="item.key"
            :class="[
              'device-detail-workbench__ledger-item',
              { 'device-detail-workbench__ledger-item--wide': item.wide }
            ]"
          >
            <span class="device-detail-workbench__ledger-label">{{ item.label }}</span>
            <span class="device-detail-workbench__ledger-value">{{ item.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showPayloadStage"
        class="device-detail-workbench__stage device-detail-workbench__stage--subtle"
        data-testid="device-detail-payload-stage"
      >
        <div class="device-detail-workbench__stage-header">
          <h3>最近载荷</h3>
        </div>

        <pre class="device-detail-workbench__code-block">{{ payloadPreview }}</pre>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Device } from '@/types/api'
import { formatDateTime, formatDeviceReportTime, prettyJson } from '@/utils/format'

type SummaryCard = {
  key: string
  label: string
  value: string
  hint: string
}

type LedgerItem = {
  key: string
  label: string
  value: string
  wide?: boolean
}

const emptyDetailValue = '--'

const props = defineProps<{
  device: Device
}>()

const device = computed(() => props.device)
const isRegistered = computed(() => device.value.registrationStatus !== 0)

function toDisplayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return emptyDetailValue
  }
  return String(value)
}

function hasRenderableText(value: string) {
  return value.trim() !== '' && value !== emptyDetailValue
}

function hasRenderableField(field: LedgerItem) {
  return hasRenderableText(field.value)
}

function hasRenderableJsonPreview(value: string) {
  const normalized = value.trim()
  return normalized !== '' && normalized !== '{}' && normalized !== '[]' && normalized !== 'null'
}

function getRegistrationStatusText(value?: number | null) {
  return value === 0 ? '未登记' : '已登记'
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
  return emptyDetailValue
}

function formatDeviceRelationValue(name?: string | null, code?: string | null) {
  if (name && code) {
    return `${name} (${code})`
  }
  return name || code || emptyDetailValue
}

function maskSecret(value?: string | null) {
  if (!value) {
    return emptyDetailValue
  }
  if (value.length <= 4) {
    return '****'
  }
  return `${value.slice(0, 2)}****${value.slice(-2)}`
}

const summaryCards = computed<SummaryCard[]>(() =>
  isRegistered.value
    ? [
        {
          key: 'productName',
          label: '产品归属',
          value: toDisplayText(device.value.productName),
          hint: '确认产品归属'
        },
        {
          key: 'onlineStatus',
          label: '在线状态',
          value: getOnlineStatusText(device.value.onlineStatus),
          hint: '查看当前连接判断'
        },
        {
          key: 'activateStatus',
          label: '激活状态',
          value: getActivateStatusText(device.value.activateStatus),
          hint: '确认是否已激活'
        },
        {
          key: 'deviceStatus',
          label: '设备状态',
          value: getDeviceStatusText(device.value.deviceStatus),
          hint: '确认是否允许接入'
        }
      ]
    : [
        {
          key: 'registrationStatus',
          label: '登记状态',
          value: getRegistrationStatusText(device.value.registrationStatus),
          hint: '先判断是否已建档'
        },
        {
          key: 'lastReportTime',
          label: '最近上报',
          value: formatDeviceReportTime(device.value.lastReportTime, device.value.updateTime, device.value.createTime),
          hint: '定位最近一次上报'
        },
        {
          key: 'lastFailureStage',
          label: '失败阶段',
          value: toDisplayText(device.value.lastFailureStage),
          hint: '确认当前失败节点'
        },
        {
          key: 'lastErrorMessage',
          label: '失败摘要',
          value: toDisplayText(device.value.lastErrorMessage),
          hint: '提炼当前失败判断'
        }
      ]
)

const assetOverviewItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode), wide: true },
  { key: 'deviceName', label: '设备名称', value: toDisplayText(device.value.deviceName), wide: true },
  { key: 'orgName', label: '所属机构', value: toDisplayText(device.value.orgName), wide: true },
  { key: 'productName', label: '产品归属', value: toDisplayText(device.value.productName), wide: true },
  { key: 'address', label: '部署位置', value: toDisplayText(device.value.address), wide: true }
])

const runtimeOverviewItems = computed<LedgerItem[]>(() => [
  { key: 'lastOnlineTime', label: '最近在线', value: formatDateTime(device.value.lastOnlineTime), wide: true },
  { key: 'lastOfflineTime', label: '最近离线', value: formatDateTime(device.value.lastOfflineTime), wide: true },
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDeviceReportTime(device.value.lastReportTime, device.value.updateTime, device.value.createTime),
    wide: true
  },
  { key: 'updateTime', label: '更新时间', value: formatDateTime(device.value.updateTime), wide: true }
])

const identityItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode), wide: true },
  { key: 'deviceName', label: '设备名称', value: toDisplayText(device.value.deviceName), wide: true },
  { key: 'orgName', label: '所属机构', value: toDisplayText(device.value.orgName), wide: true },
  { key: 'productName', label: '产品归属', value: toDisplayText(device.value.productName), wide: true },
  { key: 'address', label: '部署位置', value: toDisplayText(device.value.address), wide: true },
  { key: 'id', label: '设备 ID', value: toDisplayText(device.value.id) },
  { key: 'nodeType', label: '节点类型', value: getNodeTypeText(device.value.nodeType) },
  { key: 'protocolCode', label: '接入协议', value: toDisplayText(device.value.protocolCode) },
  { key: 'firmwareVersion', label: '固件版本', value: toDisplayText(device.value.firmwareVersion) },
  { key: 'ipAddress', label: 'IP 地址', value: toDisplayText(device.value.ipAddress) }
])

const runtimeItems = computed<LedgerItem[]>(() => [
  { key: 'lastOnlineTime', label: '最近在线', value: formatDateTime(device.value.lastOnlineTime), wide: true },
  { key: 'lastOfflineTime', label: '最近离线', value: formatDateTime(device.value.lastOfflineTime), wide: true },
  {
    key: 'lastReportTime',
    label: '最近上报',
    value: formatDeviceReportTime(device.value.lastReportTime, device.value.updateTime, device.value.createTime),
    wide: true
  },
  { key: 'createTime', label: '创建时间', value: formatDateTime(device.value.createTime) },
  { key: 'updateTime', label: '更新时间', value: formatDateTime(device.value.updateTime) },
  { key: 'clientId', label: 'Client ID', value: toDisplayText(device.value.clientId) },
  { key: 'username', label: '用户名', value: toDisplayText(device.value.username) },
  { key: 'password', label: '密码', value: maskSecret(device.value.password) },
  { key: 'deviceSecret', label: '设备密钥', value: maskSecret(device.value.deviceSecret) }
])

const supportItems = computed<LedgerItem[]>(() => [
  {
    key: 'parentDevice',
    label: '父设备',
    value: formatDeviceRelationValue(device.value.parentDeviceName, device.value.parentDeviceCode),
    wide: true
  },
  {
    key: 'gatewayDevice',
    label: '网关设备',
    value: formatDeviceRelationValue(device.value.gatewayDeviceName, device.value.gatewayDeviceCode),
    wide: true
  },
  { key: 'parentDeviceId', label: '父设备主键', value: toDisplayText(device.value.parentDeviceId) },
  { key: 'gatewayId', label: '网关主键', value: toDisplayText(device.value.gatewayId) }
])

const sourceItems = computed<LedgerItem[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode) },
  { key: 'productKey', label: '产品标识', value: toDisplayText(device.value.productKey) },
  { key: 'protocolCode', label: '协议编码', value: toDisplayText(device.value.protocolCode) },
  { key: 'sourceRecordId', label: '来源记录', value: toDisplayText(device.value.sourceRecordId) },
  { key: 'lastReportTopic', label: 'Topic', value: toDisplayText(device.value.lastReportTopic), wide: true },
  { key: 'lastErrorMessage', label: '失败摘要', value: toDisplayText(device.value.lastErrorMessage), wide: true }
])

const showOverviewStage = computed(() =>
  assetOverviewItems.value.some(hasRenderableField) || runtimeOverviewItems.value.some(hasRenderableField)
)
const showIdentityStage = computed(() => identityItems.value.some(hasRenderableField))
const showRuntimeStage = computed(() => runtimeItems.value.some(hasRenderableField))
const showSupportStage = computed(() => supportItems.value.some(hasRenderableField))

const metadataPreview = computed(() => prettyJson(device.value.metadataJson ?? ''))
const showMetadataStage = computed(() => hasRenderableJsonPreview(metadataPreview.value))

const showSourceStage = computed(() => sourceItems.value.some(hasRenderableField))
const payloadPreview = computed(() => prettyJson(device.value.lastPayload ?? ''))
const showPayloadStage = computed(() => hasRenderableJsonPreview(payloadPreview.value))
</script>

<style scoped>
.device-detail-workbench {
  display: grid;
  gap: 1rem;
}

.device-detail-workbench__stage,
.device-detail-workbench__overview-panel {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.device-detail-workbench__stage--subtle,
.device-detail-workbench__overview-panel {
  background: rgba(255, 255, 255, 0.9);
}

.device-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.device-detail-workbench__summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.9rem;
}

.device-detail-workbench__summary-card {
  display: grid;
  gap: 0.3rem;
  min-width: 0;
  padding: 0.95rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.96);
}

.device-detail-workbench__summary-label,
.device-detail-workbench__ledger-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.device-detail-workbench__summary-value {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.device-detail-workbench__summary-hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.device-detail-workbench__overview-pair {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.device-detail-workbench__ledger-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.device-detail-workbench__ledger-item {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-detail-workbench__ledger-item--wide {
  grid-column: 1 / -1;
}

.device-detail-workbench__ledger-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.device-detail-workbench__code-block {
  margin: 0;
  min-height: 11rem;
  padding: 1rem 1.05rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
  color: var(--text-heading);
  font-size: 13px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .device-detail-workbench__summary-grid,
  .device-detail-workbench__overview-pair,
  .device-detail-workbench__ledger-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-detail-workbench__ledger-item--wide {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .device-detail-workbench__summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .device-detail-workbench__summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
