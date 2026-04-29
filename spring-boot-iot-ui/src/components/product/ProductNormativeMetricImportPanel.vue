<template>
  <section class="product-normative-import" data-testid="product-normative-import-panel">
    <div class="product-normative-import__summary">
      <article class="product-normative-import__summary-card">
        <span>预检条目</span>
        <strong>{{ previewResult?.totalCount ?? 0 }}</strong>
      </article>
      <article class="product-normative-import__summary-card">
        <span>可导入</span>
        <strong>{{ previewResult?.readyCount ?? 0 }}</strong>
      </article>
      <article class="product-normative-import__summary-card">
        <span>冲突</span>
        <strong>{{ previewResult?.conflictCount ?? 0 }}</strong>
      </article>
      <article class="product-normative-import__summary-card">
        <span>已落库</span>
        <strong>{{ previewResult?.appliedCount ?? 0 }}</strong>
      </article>
    </div>

    <section class="product-normative-import__editor">
      <div class="product-normative-import__editor-head">
        <div>
          <strong>JSON 导入</strong>
          <p>支持粘贴数组或 `{ "items": [...] }`，预检通过后再落库到规范字段库。</p>
        </div>
        <StandardButton data-testid="normative-import-example" @click="fillExample">
          填入示例
        </StandardButton>
      </div>

      <textarea
        v-model="jsonText"
        data-testid="normative-import-json"
        class="product-normative-import__textarea"
        spellcheck="false"
        :placeholder="exampleJson"
      />

      <p
        v-if="formMessage"
        data-testid="normative-import-message"
        class="product-normative-import__hint"
      >
        {{ formMessage }}
      </p>

      <div class="product-normative-import__actions">
        <StandardButton
          v-permission="'iot:product-contract:govern'"
          data-testid="normative-import-preview"
          :disabled="previewing || applying"
          @click="handlePreview"
        >
          {{ previewing ? '预检中...' : '导入预检' }}
        </StandardButton>
        <StandardButton
          v-permission="'iot:product-contract:govern'"
          data-testid="normative-import-apply"
          :disabled="!canApply || applying || previewing"
          @click="handleApply"
        >
          {{ applying ? '导入中...' : '确认导入' }}
        </StandardButton>
      </div>
    </section>

    <div v-if="rows.length" class="product-normative-import__list">
      <article
        v-for="row in rows"
        :key="`${row.rowIndex ?? 'row'}-${row.fallbackKey ?? row.identifier ?? 'unknown'}`"
        class="product-normative-import__item"
        :class="{ 'product-normative-import__item--conflict': isConflict(row.status) }"
      >
        <div class="product-normative-import__item-head">
          <div>
            <strong>{{ row.displayName || row.identifier || '--' }}</strong>
            <span>{{ `${row.scenarioCode || '--'} · ${row.deviceFamily || '--'}` }}</span>
          </div>
          <span class="product-normative-import__status">{{ statusLabel(row.status) }}</span>
        </div>
        <div class="product-normative-import__meta">
          <span>{{ row.fallbackKey || `${row.monitorContentCode || '--'}/${row.monitorTypeCode || '--'}/${row.identifier || '--'}` }}</span>
          <span>{{ actionLabel(row.action) }}</span>
          <span>{{ row.message || '--' }}</span>
        </div>
      </article>
    </div>

    <div v-else class="product-normative-import__empty">
      <strong>尚未执行导入预检</strong>
      <p>建议先复制 1 到 5 条附件规范字段试跑，确认无冲突后再批量扩展。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

import {
  applyNormativeMetricImport,
  previewNormativeMetricImport
} from '@/api/normativeMetricDefinition'
import StandardButton from '@/components/StandardButton.vue'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import { ElMessage } from '@/utils/message'
import type {
  NormativeMetricDefinitionImportItem,
  NormativeMetricDefinitionImportPayload,
  NormativeMetricDefinitionImportResult
} from '@/types/api'

const exampleItems: NormativeMetricDefinitionImportItem[] = [
  {
    scenarioCode: 'phase4-surface-flow-speed',
    deviceFamily: 'SURFACE_FLOW_SPEED',
    identifier: 'value',
    displayName: '表面流速',
    unit: 'm/s',
    precisionDigits: 3,
    monitorContentCode: 'L4',
    monitorTypeCode: 'BMLS',
    riskEnabled: 0,
    trendEnabled: 1,
    metricDimension: 'flow_speed',
    thresholdType: 'absolute',
    semanticDirection: 'HIGHER_IS_RISKIER',
    gisEnabled: 0,
    insightEnabled: 1,
    analyticsEnabled: 1,
    status: 'ACTIVE',
    versionNo: 1,
    metadataJson: {
      thresholdKind: 'absolute',
      riskCategory: 'SURFACE_FLOW_SPEED',
      metricRole: 'PRIMARY'
    }
  }
]

const exampleJson = JSON.stringify(exampleItems, null, 2)
const jsonText = ref('')
const previewResult = ref<NormativeMetricDefinitionImportResult | null>(null)
const lastPayload = ref<NormativeMetricDefinitionImportPayload | null>(null)
const formMessage = ref('')
const previewing = ref(false)
const applying = ref(false)

const rows = computed(() => previewResult.value?.rows ?? [])
const canApply = computed(() =>
  Boolean(lastPayload.value)
  && Number(previewResult.value?.readyCount ?? 0) > 0
  && Number(previewResult.value?.conflictCount ?? 0) === 0
)

function fillExample() {
  jsonText.value = exampleJson
  formMessage.value = ''
}

async function handlePreview() {
  const payload = parsePayload()
  if (!payload) {
    return
  }
  previewing.value = true
  formMessage.value = ''
  try {
    const response = await previewNormativeMetricImport(payload)
    previewResult.value = response.data ?? null
    lastPayload.value = payload
    const conflictCount = Number(response.data?.conflictCount ?? 0)
    formMessage.value = conflictCount > 0
      ? `预检发现 ${conflictCount} 条冲突，请修正后再导入。`
      : '预检通过，可以确认导入。'
  } catch (error) {
    handleRequestError(error, '规范字段导入预检失败')
  } finally {
    previewing.value = false
  }
}

async function handleApply() {
  if (!lastPayload.value || !canApply.value) {
    formMessage.value = '请先完成无冲突预检。'
    return
  }
  applying.value = true
  try {
    const response = await applyNormativeMetricImport(lastPayload.value)
    previewResult.value = response.data ?? null
    const appliedCount = Number(response.data?.appliedCount ?? 0)
    ElMessage.success(`规范字段已导入 ${appliedCount} 条`)
    formMessage.value = `已导入 ${appliedCount} 条规范字段。`
  } catch (error) {
    handleRequestError(error, '规范字段导入失败')
  } finally {
    applying.value = false
  }
}

function parsePayload(): NormativeMetricDefinitionImportPayload | null {
  try {
    const parsed = JSON.parse(jsonText.value || '[]')
    const wrapped = parsed as { items?: unknown }
    const items = Array.isArray(parsed)
      ? parsed
      : Array.isArray(wrapped.items)
        ? wrapped.items
        : null
    if (!items) {
      formMessage.value = 'JSON 必须是数组，或包含 items 数组。'
      return null
    }
    if (!items.length) {
      formMessage.value = '至少需要 1 条规范字段。'
      return null
    }
    return { items }
  } catch {
    formMessage.value = 'JSON 格式不正确，请检查逗号、引号和括号。'
    return null
  }
}

function handleRequestError(error: unknown, fallbackMessage: string) {
  const message = resolveRequestErrorMessage(error, fallbackMessage)
  formMessage.value = message
  if (!isHandledRequestError(error)) {
    ElMessage.error(message)
  }
}

function statusLabel(status?: string | null) {
  const normalized = (status || '').toUpperCase()
  if (normalized === 'READY') {
    return '可导入'
  }
  if (normalized === 'APPLIED_CREATE') {
    return '已创建'
  }
  if (normalized === 'APPLIED_UPDATE') {
    return '已更新'
  }
  if (normalized.includes('CONFLICT')) {
    return '冲突'
  }
  if (normalized.includes('INVALID')) {
    return '需修正'
  }
  return status || '--'
}

function actionLabel(action?: string | null) {
  const normalized = (action || '').toUpperCase()
  if (normalized === 'CREATE') {
    return '新增'
  }
  if (normalized === 'UPDATE') {
    return '更新'
  }
  return action || '--'
}

function isConflict(status?: string | null) {
  const normalized = (status || '').toUpperCase()
  return normalized.includes('CONFLICT') || normalized.includes('INVALID')
}
</script>

<style scoped>
.product-normative-import {
  display: grid;
  gap: 16px;
}

.product-normative-import__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.product-normative-import__summary-card,
.product-normative-import__editor,
.product-normative-import__item,
.product-normative-import__empty {
  border: 1px solid rgba(100, 116, 139, 0.18);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(241, 245, 249, 0.68));
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.product-normative-import__summary-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.product-normative-import__summary-card span,
.product-normative-import__editor p,
.product-normative-import__meta,
.product-normative-import__empty p {
  color: #64748b;
  font-size: 13px;
}

.product-normative-import__summary-card strong {
  color: #0f172a;
  font-size: 24px;
}

.product-normative-import__editor {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.product-normative-import__editor-head,
.product-normative-import__item-head,
.product-normative-import__actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.product-normative-import__textarea {
  min-height: 220px;
  width: 100%;
  resize: vertical;
  border: 1px solid rgba(100, 116, 139, 0.22);
  border-radius: 14px;
  padding: 14px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.88);
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
}

.product-normative-import__hint {
  margin: 0;
  color: #475569;
  font-size: 13px;
}

.product-normative-import__actions {
  justify-content: flex-end;
}

.product-normative-import__list {
  display: grid;
  gap: 10px;
}

.product-normative-import__item {
  padding: 14px 16px;
}

.product-normative-import__item--conflict {
  border-color: rgba(220, 38, 38, 0.28);
  background: linear-gradient(135deg, rgba(254, 242, 242, 0.92), rgba(255, 247, 237, 0.72));
}

.product-normative-import__item-head strong {
  display: block;
  color: #0f172a;
}

.product-normative-import__item-head span,
.product-normative-import__status {
  color: #64748b;
  font-size: 13px;
}

.product-normative-import__status {
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  padding: 4px 10px;
  white-space: nowrap;
}

.product-normative-import__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.product-normative-import__meta span {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  padding: 4px 9px;
}

.product-normative-import__empty {
  padding: 18px;
  text-align: center;
}

.product-normative-import__empty strong {
  color: #0f172a;
}

@media (max-width: 768px) {
  .product-normative-import__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-normative-import__editor-head,
  .product-normative-import__item-head,
  .product-normative-import__actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
