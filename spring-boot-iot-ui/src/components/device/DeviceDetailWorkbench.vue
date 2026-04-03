<template>
  <div class="device-detail-workbench">
    <section class="device-detail-workbench__hero" data-testid="device-detail-hero">
      <div class="device-detail-workbench__hero-body">
        <p class="device-detail-workbench__hero-eyebrow">{{ heroEyebrow }}</p>
        <h3 class="device-detail-workbench__hero-title">{{ heroTitle }}</h3>
        <p class="device-detail-workbench__hero-message">{{ heroMessage }}</p>
      </div>

      <div class="device-detail-workbench__hero-tags">
        <span
          v-for="tag in heroTags"
          :key="tag.key"
          :class="[
            'device-detail-workbench__hero-tag',
            `device-detail-workbench__hero-tag--${tag.tone}`
          ]"
        >
          {{ tag.label }}
        </span>
      </div>
    </section>

    <section class="device-detail-workbench__stage" data-testid="device-detail-summary-stage">
      <div class="device-detail-workbench__stage-header">
        <div>
          <h3>资产状态与接入概况</h3>
          <p>{{ summaryMessage }}</p>
        </div>
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
              <div>
                <h3>资产概览</h3>
                <p>先确认资产身份、产品归属与部署位置。</p>
              </div>
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
              <div>
                <h3>运行概览</h3>
                <p>先确认最近在线、离线、上报和更新时间。</p>
              </div>
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
          <div>
            <h3>身份与部署台账</h3>
            <p>长字段跨列展示，避免设备主档继续挤成等宽小卡墙。</p>
          </div>
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
          <div>
            <h3>运行与认证台账</h3>
            <p>先看时间节奏，再核对接入凭据，保持同一层台账阅读顺序。</p>
          </div>
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
          <div>
            <h3>关系与建档补充</h3>
            <p>后置关系字段和补建主档线索，但继续复用同一套台账语法。</p>
          </div>
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
          <div>
            <h3>扩展元数据快照</h3>
            <p>保留结构化补充信息，作为安静的后置快照区。</p>
          </div>
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
          <div>
            <h3>来源档案与失败摘要</h3>
            <p>把来源主键、协议上下文和失败摘要放在同一层，方便补档与排障。</p>
          </div>
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
          <div>
            <h3>最近载荷</h3>
            <p>保留最近一次原始报文，便于回查协议映射和补建设备主档。</p>
          </div>
        </div>

        <pre class="device-detail-workbench__code-block">{{ payloadPreview }}</pre>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { Device } from '@/types/api'
import { formatDateTime, prettyJson } from '@/utils/format'

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

type HeroTag = {
  key: string
  label: string
  tone: 'success' | 'warning' | 'muted'
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

const heroEyebrow = computed(() => (isRegistered.value ? '设备资产详情' : '未登记设备详情'))
const heroTitle = computed(() => device.value.deviceName || device.value.deviceCode || '设备详情')
const heroMessage = computed(() =>
  isRegistered.value
    ? '资产信息完整，可继续核对部署、运行与认证台账。'
    : '当前仍是未登记上报，优先确认失败阶段与来源记录。'
)

const heroTags = computed<HeroTag[]>(() =>
  isRegistered.value
    ? [
        {
          key: 'registration',
          label: getRegistrationStatusText(device.value.registrationStatus),
          tone: 'success'
        },
        {
          key: 'online',
          label: getOnlineStatusText(device.value.onlineStatus),
          tone: device.value.onlineStatus === 1 ? 'success' : 'muted'
        },
        {
          key: 'activate',
          label: getActivateStatusText(device.value.activateStatus),
          tone: device.value.activateStatus === 1 ? 'success' : 'warning'
        },
        {
          key: 'deviceStatus',
          label: getDeviceStatusText(device.value.deviceStatus),
          tone: device.value.deviceStatus === 1 ? 'success' : 'warning'
        }
      ]
    : [
        {
          key: 'registration',
          label: getRegistrationStatusText(device.value.registrationStatus),
          tone: 'warning'
        },
        {
          key: 'source',
          label: getSourceTypeText(device.value.assetSourceType),
          tone: 'muted'
        }
      ]
)

const summaryMessage = computed(() =>
  isRegistered.value
    ? '先完成当前状态判断，再进入完整台账核对。'
    : '先确认失败判断和最近上报，再决定是否补建设备主档。'
)

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
          value: formatDateTime(device.value.lastReportTime),
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
  { key: 'lastReportTime', label: '最近上报', value: formatDateTime(device.value.lastReportTime), wide: true },
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
  { key: 'lastReportTime', label: '最近上报', value: formatDateTime(device.value.lastReportTime), wide: true },
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

.device-detail-workbench__hero,
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

.device-detail-workbench__hero {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  background:
    linear-gradient(135deg, rgba(247, 250, 255, 0.98), rgba(255, 255, 255, 0.94)),
    rgba(255, 255, 255, 0.94);
}

.device-detail-workbench__stage--subtle,
.device-detail-workbench__overview-panel {
  background: rgba(255, 255, 255, 0.9);
}

.device-detail-workbench__hero-body {
  display: grid;
  gap: 0.32rem;
}

.device-detail-workbench__hero-eyebrow {
  margin: 0;
  color: var(--text-caption);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.device-detail-workbench__hero-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 20px;
  line-height: 1.35;
}

.device-detail-workbench__hero-message {
  margin: 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.device-detail-workbench__hero-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.5rem;
}

.device-detail-workbench__hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0.4rem 0.75rem;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.device-detail-workbench__hero-tag--success {
  border-color: rgba(35, 140, 81, 0.16);
  background: rgba(35, 140, 81, 0.1);
  color: #2f6e46;
}

.device-detail-workbench__hero-tag--warning {
  border-color: rgba(177, 92, 29, 0.16);
  background: rgba(177, 92, 29, 0.1);
  color: #8b4c1c;
}

.device-detail-workbench__hero-tag--muted {
  border-color: rgba(78, 94, 117, 0.14);
  background: rgba(78, 94, 117, 0.08);
  color: #536074;
}

.device-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.device-detail-workbench__stage-header p {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
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
  .device-detail-workbench__hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-detail-workbench__hero-tags {
    justify-content: flex-start;
  }

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
