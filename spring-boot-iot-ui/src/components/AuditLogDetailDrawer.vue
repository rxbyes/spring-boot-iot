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
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>运行概览</h3>
          <p>聚合当前操作的执行状态、处理主体与链路标识，帮助快速判断本次日志的处理结果。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">操作类型</span>
          <strong class="detail-summary-card__value">{{ getOperationTypeName(detail?.operationType) }}</strong>
          <p class="detail-summary-card__hint">模块：{{ formatValue(detail?.operationModule) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">执行结果</span>
          <strong class="detail-summary-card__value">{{ getOperationResultName(detail?.operationResult) }}</strong>
          <p class="detail-summary-card__hint">请求通道：{{ formatValue(detail?.requestMethod) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">操作时间</span>
          <strong class="detail-summary-card__value">{{ formatDateTime(detail?.operationTime) }}</strong>
          <p class="detail-summary-card__hint">TraceId：{{ formatValue(detail?.traceId) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">处理人</span>
          <strong class="detail-summary-card__value">{{ formatValue(detail?.userName) }}</strong>
          <p class="detail-summary-card__hint">IP：{{ formatValue(detail?.ipAddress) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">关联设备</span>
          <strong class="detail-summary-card__value">{{ formatValue(detail?.deviceCode) }}</strong>
          <p class="detail-summary-card__hint">产品标识：{{ formatValue(detail?.productKey) }}</p>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>主体信息</h3>
          <p>呈现日志归属、操作模块和目标资源，便于还原本次业务动作的上下文。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">日志 ID</span>
          <strong class="detail-field__value">{{ formatValue(detail?.id) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">租户 ID</span>
          <strong class="detail-field__value">{{ formatValue(detail?.tenantId) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作类型</span>
          <strong class="detail-field__value">{{ getOperationTypeName(detail?.operationType) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作结果</span>
          <strong class="detail-field__value">{{ getOperationResultName(detail?.operationResult) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作模块</span>
          <strong class="detail-field__value">{{ formatValue(detail?.operationModule) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作方法</span>
          <strong class="detail-field__value">{{ formatValue(detail?.operationMethod) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.operationTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作用户</span>
          <strong class="detail-field__value">{{ formatValue(detail?.userName) }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">请求 URL / 目标</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.requestUrl) }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>链路信息</h3>
          <p>统一展示请求入口、设备标识与 TraceId，方便与链路追踪台和异常观测台联动排查。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">请求通道</span>
          <strong class="detail-field__value">{{ formatValue(detail?.requestMethod) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">TraceId</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.traceId) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作时间</span>
          <strong class="detail-field__value">{{ formatDateTime(detail?.operationTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作用户</span>
          <strong class="detail-field__value">{{ formatValue(detail?.userName) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">操作 IP</span>
          <strong class="detail-field__value">{{ formatValue(detail?.ipAddress) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">设备编码</span>
          <strong class="detail-field__value">{{ formatValue(detail?.deviceCode) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">产品标识</span>
          <strong class="detail-field__value">{{ formatValue(detail?.productKey) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">异常编码</span>
          <strong class="detail-field__value">{{ formatValue(detail?.errorCode) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">异常类型</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.exceptionClass) }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>异常诊断</h3>
          <p>聚焦错误编码、异常类型和执行说明，用于快速判断失败原因或确认处理反馈。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">异常编码</span>
          <strong class="detail-field__value">{{ formatValue(detail?.errorCode) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">异常类型</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.exceptionClass) }}</strong>
        </div>
      </div>
      <div :class="['detail-notice', { 'detail-notice--danger': isFailure }]">
        <span class="detail-notice__label">{{ isFailure ? '失败原因 / 结果说明' : '执行说明' }}</span>
        <strong class="detail-notice__value">{{ formatValue(detail?.resultMessage) }}</strong>
        <div
          v-if="showTraceAction || showAccessErrorAction || showProductAction || showDeviceAction"
          class="detail-notice__actions"
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

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>请求与结果</h3>
          <p>使用深色报文块承载请求参数和响应结果，长文本与 JSON 内容在查看时更聚焦。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">请求参数</span>
          <div class="detail-field__value detail-field__value--pre">{{ formatPayload(detail?.requestParams) }}</div>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">响应结果</span>
          <div class="detail-field__value detail-field__value--pre">{{ formatPayload(detail?.responseResult) }}</div>
        </div>
      </div>
    </section>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { AuditLogRecord } from '@/api/auditLog'
import { formatDateTime, prettyJson } from '@/utils/format'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardButton from '@/components/StandardButton.vue'

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

const hasDetail = computed(() => Object.keys(props.detail || {}).length > 0)
const drawerTitle = computed(() => props.detail.operationModule || props.title)
const drawerSubtitle = computed(() => props.detail.operationMethod || props.detail.requestUrl || '查看审计记录详情')
const isFailure = computed(() => props.detail.operationResult === 0 || props.detail.operationType === 'system_error')
const drawerTags = computed(() => {
  if (!hasDetail.value) {
    return []
  }
  return [
    { label: getOperationTypeName(props.detail.operationType), type: getOperationTypeTag(props.detail.operationType) },
    { label: getOperationResultName(props.detail.operationResult), type: getOperationResultTag(props.detail.operationResult) },
    { label: formatValue(props.detail.requestMethod), type: 'info' as const },
    ...(props.detail.traceId ? [{ label: `Trace ${props.detail.traceId}`, type: 'info' as const }] : [])
  ]
})

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
.detail-notice__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}
</style>
