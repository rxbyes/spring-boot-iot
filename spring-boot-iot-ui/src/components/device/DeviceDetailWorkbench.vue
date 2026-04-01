<template>
  <div class="device-detail-workbench">
    <template v-if="isRegistered">
      <section
        v-if="showArchiveSection"
        class="device-detail-workbench__section"
        data-testid="device-detail-section-archive"
      >
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>基础档案</h3>
            <p>统一查看设备身份、产品归属与接入基线，保持与编辑页相同的主档阅读顺序。</p>
          </div>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in archiveFields"
            :key="field.key"
            :class="[
              'device-detail-workbench__field',
              { 'device-detail-workbench__field--full': field.fullWidth }
            ]"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showTopologySection"
        class="device-detail-workbench__section"
        data-testid="device-detail-section-topology"
      >
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>父子拓扑</h3>
            <p>把父设备、网关关系与关键主键收在同一板块，避免在详情里来回跳段核对。</p>
          </div>
        </div>

        <div
          v-if="showRelationSummary"
          class="device-detail-workbench__relation-summary"
        >
          <article class="device-detail-workbench__relation-card">
            <span>当前父设备</span>
            <span class="device-detail-workbench__relation-value">{{ relationSummary.parentDevice }}</span>
          </article>
          <article class="device-detail-workbench__relation-card">
            <span>网关设备</span>
            <span class="device-detail-workbench__relation-value">{{ relationSummary.gatewayDevice }}</span>
          </article>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in topologyFields"
            :key="field.key"
            class="device-detail-workbench__field"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showStatusSection"
        class="device-detail-workbench__section"
        data-testid="device-detail-section-status"
      >
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>状态与维护属性</h3>
            <p>围绕维护现场最常核对的时间、固件、地址和状态信息展开，不再拆成并列台账。</p>
          </div>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in statusFields"
            :key="field.key"
            :class="[
              'device-detail-workbench__field',
              { 'device-detail-workbench__field--full': field.fullWidth }
            ]"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showAuthSection"
        class="device-detail-workbench__section"
        data-testid="device-detail-section-auth"
      >
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>认证字段</h3>
            <p>保留接入校验所需的核心认证字段，查看逻辑与编辑页保持同一板块顺序。</p>
          </div>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in authFields"
            :key="field.key"
            class="device-detail-workbench__field"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section
        v-if="showMetadataSection"
        class="device-detail-workbench__section"
        data-testid="device-detail-section-metadata"
      >
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>扩展信息</h3>
            <p>用于查看站点、责任人、批次等补充 metadata，不再与关系区并列压缩展示。</p>
          </div>
        </div>

        <pre class="device-detail-workbench__code-block">{{ metadataPreview }}</pre>
      </section>
    </template>

    <template v-else>
      <section class="device-detail-workbench__section" data-testid="device-detail-section-source">
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>来源档案</h3>
            <p>先确认未登记设备的来源、协议和主键归档信息，再决定是否进入补建设备主档。</p>
          </div>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in sourceFields"
            :key="field.key"
            class="device-detail-workbench__field"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section class="device-detail-workbench__section" data-testid="device-detail-section-failure">
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>失败摘要</h3>
            <p>集中查看失败阶段、Trace 和 Topic，便于快速判断当前是补档问题还是协议问题。</p>
          </div>
        </div>

        <div class="device-detail-workbench__grid">
          <article
            v-for="field in failureFields"
            :key="field.key"
            :class="[
              'device-detail-workbench__field',
              { 'device-detail-workbench__field--full': field.fullWidth }
            ]"
          >
            <span class="device-detail-workbench__field-label">{{ field.label }}</span>
            <span class="device-detail-workbench__field-value">{{ field.value }}</span>
          </article>
        </div>
      </section>

      <section class="device-detail-workbench__section" data-testid="device-detail-section-payload">
        <div class="device-detail-workbench__section-header">
          <div>
            <h3>最近载荷</h3>
            <p>保留最近一次未登记上报的原始载荷，方便补建主档或回查协议映射。</p>
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

type DetailField = {
  key: string
  label: string
  value: string
  fullWidth?: boolean
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

function hasRenderableText(value: string) {
  return value.trim() !== '' && value !== emptyDetailValue
}

function hasRenderableField(field: DetailField) {
  return hasRenderableText(field.value)
}

function hasRenderableJsonPreview(value: string) {
  const normalized = value.trim()
  return normalized !== '' && normalized !== '{}' && normalized !== '[]' && normalized !== 'null'
}

const archiveFields = computed<DetailField[]>(() => [
  { key: 'id', label: '设备 ID', value: toDisplayText(device.value.id) },
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode) },
  { key: 'deviceName', label: '设备名称', value: toDisplayText(device.value.deviceName) },
  { key: 'productName', label: '产品归属', value: toDisplayText(device.value.productName) },
  { key: 'productKey', label: '产品 Key', value: toDisplayText(device.value.productKey) },
  { key: 'nodeType', label: '节点类型', value: getNodeTypeText(device.value.nodeType) },
  { key: 'protocolCode', label: '接入协议', value: toDisplayText(device.value.protocolCode) }
])

const relationSummary = computed(() => ({
  parentDevice: formatDeviceRelationValue(device.value.parentDeviceName, device.value.parentDeviceCode),
  gatewayDevice: formatDeviceRelationValue(device.value.gatewayDeviceName, device.value.gatewayDeviceCode)
}))

const topologyFields = computed<DetailField[]>(() => [
  { key: 'parentDeviceId', label: '父设备主键', value: toDisplayText(device.value.parentDeviceId) },
  { key: 'gatewayId', label: '网关主键', value: toDisplayText(device.value.gatewayId) }
])

const statusFields = computed<DetailField[]>(() => [
  { key: 'firmwareVersion', label: '固件版本', value: toDisplayText(device.value.firmwareVersion) },
  { key: 'ipAddress', label: 'IP 地址', value: toDisplayText(device.value.ipAddress) },
  { key: 'lastOnlineTime', label: '最近在线', value: formatDateTime(device.value.lastOnlineTime) },
  { key: 'lastOfflineTime', label: '最近离线', value: formatDateTime(device.value.lastOfflineTime) },
  { key: 'lastReportTime', label: '最近上报', value: formatDateTime(device.value.lastReportTime) },
  { key: 'createTime', label: '创建时间', value: formatDateTime(device.value.createTime) },
  { key: 'updateTime', label: '更新时间', value: formatDateTime(device.value.updateTime) },
  { key: 'address', label: '部署位置', value: toDisplayText(device.value.address), fullWidth: true }
])

const authFields = computed<DetailField[]>(() => [
  { key: 'clientId', label: 'Client ID', value: toDisplayText(device.value.clientId) },
  { key: 'username', label: '用户名', value: toDisplayText(device.value.username) },
  { key: 'password', label: '密码', value: maskSecret(device.value.password) },
  { key: 'deviceSecret', label: '设备密钥', value: maskSecret(device.value.deviceSecret) }
])

const sourceFields = computed<DetailField[]>(() => [
  { key: 'registrationStatus', label: '登记状态', value: getRegistrationStatusText(device.value.registrationStatus) },
  { key: 'assetSourceType', label: '来源类型', value: getSourceTypeText(device.value.assetSourceType) },
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode) },
  { key: 'productKey', label: '产品标识', value: toDisplayText(device.value.productKey) },
  { key: 'protocolCode', label: '协议编码', value: toDisplayText(device.value.protocolCode) },
  { key: 'sourceRecordId', label: '来源记录', value: toDisplayText(device.value.sourceRecordId) }
])

const failureFields = computed<DetailField[]>(() => [
  { key: 'lastFailureStage', label: '失败阶段', value: toDisplayText(device.value.lastFailureStage) },
  { key: 'lastTraceId', label: 'Trace ID', value: toDisplayText(device.value.lastTraceId) },
  { key: 'lastReportTime', label: '最近上报', value: formatDateTime(device.value.lastReportTime) },
  { key: 'lastReportTopic', label: 'Topic', value: toDisplayText(device.value.lastReportTopic), fullWidth: true },
  { key: 'lastErrorMessage', label: '失败摘要', value: toDisplayText(device.value.lastErrorMessage), fullWidth: true }
])

const showArchiveSection = computed(() => archiveFields.value.some(hasRenderableField))
const showRelationSummary = computed(
  () => hasRenderableText(relationSummary.value.parentDevice) || hasRenderableText(relationSummary.value.gatewayDevice)
)
const showTopologySection = computed(
  () => showRelationSummary.value || topologyFields.value.some(hasRenderableField)
)
const showStatusSection = computed(() => statusFields.value.some(hasRenderableField))
const showAuthSection = computed(() => authFields.value.some(hasRenderableField))
const metadataPreview = computed(() => prettyJson(device.value.metadataJson ?? ''))
const showMetadataSection = computed(() => hasRenderableJsonPreview(metadataPreview.value))
const payloadPreview = computed(() => prettyJson(device.value.lastPayload || emptyDetailValue))
</script>

<style scoped>
.device-detail-workbench {
  display: grid;
  gap: 1rem;
}

.device-detail-workbench__section {
  display: grid;
  gap: 0.92rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-inset-highlight-78);
}

.device-detail-workbench__section-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
}

.device-detail-workbench__section-header p {
  margin: 0.38rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.65;
}

.device-detail-workbench__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.device-detail-workbench__field {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-detail-workbench__field--full {
  grid-column: 1 / -1;
}

.device-detail-workbench__field-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.4;
}

.device-detail-workbench__field-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.device-detail-workbench__relation-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
}

.device-detail-workbench__relation-card {
  display: grid;
  gap: 0.34rem;
  min-width: 0;
  padding: 0.92rem 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-detail-workbench__relation-card span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.4;
}

.device-detail-workbench__relation-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.5;
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

@media (max-width: 900px) {
  .device-detail-workbench__grid,
  .device-detail-workbench__relation-summary {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
