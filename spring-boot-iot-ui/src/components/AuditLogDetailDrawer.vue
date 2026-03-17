<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    eyebrow="Audit Log Detail"
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
    <section class="detail-panel">
      <h3>基础信息</h3>
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
          <span class="detail-field__label">请求通道</span>
          <strong class="detail-field__value">{{ formatValue(detail?.requestMethod) }}</strong>
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
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">请求 URL / 目标</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.requestUrl) }}</strong>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <h3>请求与结果</h3>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">请求参数</span>
          <div class="detail-field__value detail-field__value--pre">{{ formatPayload(detail?.requestParams) }}</div>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">响应结果</span>
          <div class="detail-field__value detail-field__value--pre">{{ formatPayload(detail?.responseResult) }}</div>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">结果消息</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detail?.resultMessage) }}</strong>
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

const props = defineProps<{
  modelValue: boolean
  title: string
  detail: Partial<AuditLogRecord>
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

const hasDetail = computed(() => Object.keys(props.detail || {}).length > 0)
const drawerTitle = computed(() => props.detail.operationModule || props.title)
const drawerSubtitle = computed(() => props.detail.operationMethod || props.detail.requestUrl || '查看审计记录详情')
const drawerTags = computed(() => {
  if (!hasDetail.value) {
    return []
  }
  return [
    { label: getOperationTypeName(props.detail.operationType), type: getOperationTypeTag(props.detail.operationType) },
    { label: getOperationResultName(props.detail.operationResult), type: getOperationResultTag(props.detail.operationResult) },
    { label: formatValue(props.detail.requestMethod), type: 'info' as const }
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
