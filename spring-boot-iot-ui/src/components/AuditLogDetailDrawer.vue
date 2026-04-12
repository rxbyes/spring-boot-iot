<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :tags="drawerTags"
    :loading="loading"
    loading-text="正在加载日志详情..."
    :error-message="errorMessage"
    :empty="!hasDetail"
    empty-text="暂无日志详情"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <div class="audit-log-detail-workbench">
      <section class="audit-log-detail-workbench__stage" data-testid="audit-log-detail-summary-stage">
        <div class="audit-log-detail-workbench__stage-header">
          <h3>异常态势与处理概况</h3>
        </div>

        <div class="audit-log-detail-workbench__summary-grid">
          <article
            v-for="card in summaryCards"
            :key="card.key"
            class="audit-log-detail-workbench__summary-card"
          >
            <span class="audit-log-detail-workbench__summary-label">{{ card.label }}</span>
            <span class="audit-log-detail-workbench__summary-value">{{ card.value }}</span>
            <span v-if="card.hint" class="audit-log-detail-workbench__summary-hint">{{ card.hint }}</span>
          </article>
        </div>
      </section>

      <section
        class="audit-log-detail-workbench__stage audit-log-detail-workbench__stage--subtle"
        data-testid="audit-log-detail-identity-stage"
      >
        <div class="audit-log-detail-workbench__stage-header">
          <h3>链路与主体台账</h3>
        </div>

        <div class="audit-log-detail-workbench__fact-table">
          <div
            v-for="item in identityItems"
            :key="item.key"
            :class="[
              'audit-log-detail-workbench__fact-row',
              { 'audit-log-detail-workbench__fact-row--stacked': item.wide }
            ]"
          >
            <span class="audit-log-detail-workbench__fact-label">{{ item.label }}</span>
            <span class="audit-log-detail-workbench__fact-value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section
        class="audit-log-detail-workbench__stage audit-log-detail-workbench__stage--subtle"
        data-testid="audit-log-detail-diagnosis-stage"
      >
        <div class="audit-log-detail-workbench__stage-header">
          <h3>异常诊断与回跳</h3>
        </div>

        <div class="audit-log-detail-workbench__fact-table">
          <div
            v-for="item in diagnosisItems"
            :key="item.key"
            :class="[
              'audit-log-detail-workbench__fact-row',
              { 'audit-log-detail-workbench__fact-row--stacked': item.wide }
            ]"
          >
            <span class="audit-log-detail-workbench__fact-label">{{ item.label }}</span>
            <span class="audit-log-detail-workbench__fact-value">{{ item.value }}</span>
          </div>
        </div>

        <div :class="['audit-log-detail-workbench__notice', { 'audit-log-detail-workbench__notice--danger': isFailure }]">
          <span class="audit-log-detail-workbench__notice-label">
            {{ isFailure ? '失败原因 / 结果说明' : '执行说明' }}
          </span>
          <strong class="audit-log-detail-workbench__notice-value">
            {{ formatValue(detailRecord.resultMessage) }}
          </strong>
          <div
            v-if="showTraceAction || showAccessErrorAction || showProductAction || showDeviceAction"
            class="audit-log-detail-workbench__notice-actions"
          >
            <StandardButton v-if="showTraceAction" action="refresh" link @click="emit('jump-message-trace')">
              返回链路追踪
            </StandardButton>
            <StandardButton v-if="showAccessErrorAction" action="reset" link @click="emit('jump-access-error')">
              回看失败归档
            </StandardButton>
            <StandardButton v-if="showProductAction" action="refresh" link @click="emit('jump-product-governance')">
              产品定义中心
            </StandardButton>
            <StandardButton v-if="showDeviceAction" action="refresh" link @click="emit('jump-device-governance')">
              设备资产中心
            </StandardButton>
          </div>
        </div>
      </section>

      <section
        class="audit-log-detail-workbench__stage audit-log-detail-workbench__stage--subtle"
        data-testid="audit-log-detail-payload-stage"
      >
        <div class="audit-log-detail-workbench__stage-header">
          <h3>请求与响应快照</h3>
        </div>

        <div class="audit-log-detail-workbench__payload-stack">
          <article class="audit-log-detail-workbench__payload-panel">
            <div class="audit-log-detail-workbench__payload-header">请求参数</div>
            <pre class="audit-log-detail-workbench__code-block">{{ formatPayload(detailRecord.requestParams) }}</pre>
          </article>
          <article class="audit-log-detail-workbench__payload-panel">
            <div class="audit-log-detail-workbench__payload-header">响应结果</div>
            <pre class="audit-log-detail-workbench__code-block">{{ formatPayload(detailRecord.responseResult) }}</pre>
          </article>
        </div>
      </section>
    </div>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { AuditLogRecord } from '@/api/auditLog'
import { formatDateTime, prettyJson } from '@/utils/format'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardButton from '@/components/StandardButton.vue'

type SummaryCard = {
  key: string
  label: string
  value: string
  hint?: string
}

type LedgerItem = {
  key: string
  label: string
  value: string
  wide?: boolean
}

const props = defineProps<{
  modelValue: boolean
  title: string
  detail: Partial<AuditLogRecord>
  loading?: boolean
  errorMessage?: string
  showTraceAction?: boolean
  showAccessErrorAction?: boolean
  showProductAction?: boolean
  showDeviceAction?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'jump-message-trace'): void
  (event: 'jump-access-error'): void
  (event: 'jump-product-governance'): void
  (event: 'jump-device-governance'): void
}>()

const detailRecord = computed(() => props.detail || {})
const hasDetail = computed(() => Object.keys(detailRecord.value).length > 0)
const drawerTitle = computed(() => detailRecord.value.operationModule || props.title)
const drawerSubtitle = computed(() => detailRecord.value.operationMethod || detailRecord.value.requestUrl || '查看审计记录详情')
const isFailure = computed(() => detailRecord.value.operationResult === 0 || detailRecord.value.operationType === 'system_error')
const drawerTags = computed(() => {
  if (!hasDetail.value) {
    return []
  }
  return [
    {
      label: getOperationTypeName(detailRecord.value.operationType),
      type: getOperationTypeTag(detailRecord.value.operationType)
    },
    {
      label: getOperationResultName(detailRecord.value.operationResult),
      type: getOperationResultTag(detailRecord.value.operationResult)
    }
  ]
})

const summaryCards = computed<SummaryCard[]>(() => [
  {
    key: 'requestMethod',
    label: '请求通道',
    value: formatValue(detailRecord.value.requestMethod)
  },
  {
    key: 'errorCode',
    label: '异常编码',
    value: formatValue(detailRecord.value.errorCode)
  },
  {
    key: 'time',
    label: '发生时间',
    value: formatDateTime(detailRecord.value.operationTime)
  },
  {
    key: 'deviceCode',
    label: '关联设备',
    value: formatValue(detailRecord.value.deviceCode)
  }
])

const identityItems = computed<LedgerItem[]>(() => [
  { key: 'id', label: '日志 ID', value: formatValue(detailRecord.value.id) },
  { key: 'tenantId', label: '租户 ID', value: formatValue(detailRecord.value.tenantId) },
  { key: 'productKey', label: '产品标识', value: formatValue(detailRecord.value.productKey) },
  { key: 'userName', label: '操作用户', value: formatValue(detailRecord.value.userName) },
  { key: 'ipAddress', label: '操作 IP', value: formatValue(detailRecord.value.ipAddress) },
  { key: 'traceId', label: 'TraceId', value: formatValue(detailRecord.value.traceId), wide: true },
  { key: 'requestUrl', label: '请求 URL / 目标', value: formatValue(detailRecord.value.requestUrl), wide: true }
])

const diagnosisItems = computed<LedgerItem[]>(() => [
  {
    key: 'operationType',
    label: '操作类型',
    value: getOperationTypeName(detailRecord.value.operationType)
  },
  {
    key: 'operationResult',
    label: '操作结果',
    value: getOperationResultName(detailRecord.value.operationResult)
  },
  {
    key: 'errorCode',
    label: '异常编码',
    value: formatValue(detailRecord.value.errorCode)
  },
  {
    key: 'exceptionClass',
    label: '异常类型',
    value: formatValue(detailRecord.value.exceptionClass),
    wide: true
  }
])

function getOperationTypeName(type?: string) {
  const map: Record<string, string> = {
    insert: '新增',
    update: '修改',
    delete: '删除',
    select: '查询',
    system_error: '系统异常'
  }
  return type ? map[type] || type : '--'
}

function getOperationTypeTag(type?: string): 'primary' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'warning' | 'danger' | 'info'> = {
    insert: 'primary',
    update: 'warning',
    delete: 'danger',
    select: 'info',
    system_error: 'danger'
  }
  return type ? map[type] || 'info' : 'info'
}

function getOperationResultName(result?: number | null) {
  if (result === 1) return '成功'
  if (result === 0) return '失败'
  return '--'
}

function getOperationResultTag(result?: number | null): 'success' | 'danger' | 'info' {
  if (result === 1) return 'success'
  if (result === 0) return 'danger'
  return 'info'
}

function formatValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

function formatPayload(payload?: string) {
  if (!payload) {
    return '--'
  }
  return prettyJson(payload)
}
</script>

<style scoped>
.audit-log-detail-workbench {
  display: grid;
  gap: 1rem;
}

.audit-log-detail-workbench__stage,
.audit-log-detail-workbench__payload-panel {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.audit-log-detail-workbench__stage--subtle,
.audit-log-detail-workbench__payload-panel {
  background: rgba(255, 255, 255, 0.9);
}

.audit-log-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.audit-log-detail-workbench__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.audit-log-detail-workbench__summary-card {
  display: grid;
  gap: 0.38rem;
  min-width: 0;
  padding: 1rem 1.05rem;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: calc(var(--radius-md) + 2px);
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(244, 248, 255, 0.94) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.audit-log-detail-workbench__summary-label,
.audit-log-detail-workbench__ledger-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.audit-log-detail-workbench__summary-value {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.audit-log-detail-workbench__summary-hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.audit-log-detail-workbench__payload-stack {
  display: grid;
  gap: 1rem;
}

.audit-log-detail-workbench__fact-table {
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.96);
}

.audit-log-detail-workbench__fact-row {
  display: grid;
  grid-template-columns: 8.5rem minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.audit-log-detail-workbench__fact-row:last-child {
  border-bottom: none;
}

.audit-log-detail-workbench__fact-row--stacked {
  grid-template-columns: minmax(0, 1fr);
}

.audit-log-detail-workbench__fact-label,
.audit-log-detail-workbench__fact-value {
  min-width: 0;
  padding: 0.88rem 1rem;
  line-height: 1.6;
}

.audit-log-detail-workbench__fact-label {
  display: flex;
  align-items: center;
  color: var(--text-caption);
  font-size: 12px;
  background: rgba(248, 250, 252, 0.96);
  border-right: 1px solid rgba(226, 232, 240, 0.92);
}

.audit-log-detail-workbench__fact-row--stacked .audit-log-detail-workbench__fact-label {
  border-right: none;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.audit-log-detail-workbench__fact-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  overflow-wrap: anywhere;
}

.audit-log-detail-workbench__ledger-value,
.audit-log-detail-workbench__notice-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.audit-log-detail-workbench__notice {
  display: grid;
  gap: 0.75rem;
  padding: 0.92rem 1rem;
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: calc(var(--radius-md) + 2px);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(244, 248, 255, 0.94) 100%);
}

.audit-log-detail-workbench__notice--danger {
  border-color: color-mix(in srgb, var(--danger, #d45d5d) 26%, var(--panel-border));
  background: color-mix(in srgb, #fff7f7 72%, rgba(255, 255, 255, 0.92));
}

.audit-log-detail-workbench__notice-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.audit-log-detail-workbench__notice-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.audit-log-detail-workbench__payload-header {
  margin: -1rem -1rem 0;
  padding: 0.78rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
  background: rgba(248, 250, 252, 0.96);
}

.audit-log-detail-workbench__code-block {
  margin: 0;
  min-height: 11rem;
  padding: 1rem 0;
  border: none;
  border-radius: calc(var(--radius-md) + 2px);
  background: transparent;
  color: var(--text-heading);
  font-size: 13px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .audit-log-detail-workbench__summary-grid,
  .audit-log-detail-workbench__payload-stack {
    grid-template-columns: minmax(0, 1fr);
  }

  .audit-log-detail-workbench__fact-row {
    grid-template-columns: 7.5rem minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .audit-log-detail-workbench__summary-grid,
  .audit-log-detail-workbench__fact-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .audit-log-detail-workbench__fact-label {
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  }
}

@media (max-width: 520px) {
  .audit-log-detail-workbench__summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
