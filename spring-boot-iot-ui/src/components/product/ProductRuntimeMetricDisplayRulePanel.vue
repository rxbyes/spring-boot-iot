<template>
  <section class="product-runtime-display-rule" data-testid="product-runtime-display-rule-panel">
    <div class="product-runtime-display-rule__summary">
      <article class="product-runtime-display-rule__summary-card">
        <span>治理规则</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article class="product-runtime-display-rule__summary-card">
        <span>启用中</span>
        <strong>{{ activeCount }}</strong>
      </article>
      <article class="product-runtime-display-rule__summary-card">
        <span>已补单位</span>
        <strong>{{ unitConfiguredCount }}</strong>
      </article>
    </div>

    <section
      v-if="routeCandidate.rawIdentifier"
      class="product-runtime-display-rule__candidate"
      data-testid="runtime-display-rule-candidate"
    >
      <div>
        <strong>待治理候选</strong>
        <p>
          {{ routeCandidate.deviceCode ? `来源设备 ${routeCandidate.deviceCode}` : '来自运行态治理入口' }}
        </p>
      </div>
      <div class="product-runtime-display-rule__candidate-meta">
        <span>{{ routeCandidate.rawIdentifier }}</span>
        <span v-if="routeCandidate.displayName">{{ routeCandidate.displayName }}</span>
        <span v-if="routeCandidate.unit">{{ `单位 ${routeCandidate.unit}` }}</span>
      </div>
      <StandardButton
        data-testid="runtime-display-rule-candidate-adopt"
        @click="adoptRouteCandidate"
      >
        带入表单
      </StandardButton>
    </section>

    <section class="product-runtime-display-rule__editor">
      <div class="product-runtime-display-rule__editor-head">
        <div>
          <strong>{{ isEditing ? '编辑运行态规则' : '新增运行态规则' }}</strong>
          <p>只治理非正式字段的中文名称和单位，正式字段仍以契约发布结果为准。</p>
        </div>
        <StandardButton
          data-testid="runtime-display-rule-reset"
          :disabled="!isEditing && !hasDirtyForm"
          @click="resetForm"
        >
          清空表单
        </StandardButton>
      </div>

      <p
        v-if="formMessage"
        data-testid="runtime-display-rule-form-message"
        class="product-runtime-display-rule__hint"
      >
        {{ formMessage }}
      </p>

      <div class="product-runtime-display-rule__form-grid">
        <label class="product-runtime-display-rule__field">
          <span>作用域</span>
          <select
            v-model="form.scopeType"
            data-testid="runtime-display-rule-scope-type"
            class="product-runtime-display-rule__select"
          >
            <option v-for="option in scopeTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="product-runtime-display-rule__field">
          <span>状态</span>
          <select
            v-model="form.status"
            data-testid="runtime-display-rule-status"
            class="product-runtime-display-rule__select"
          >
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </label>

        <label class="product-runtime-display-rule__field">
          <span>原始字段标识</span>
          <input
            v-model="form.rawIdentifier"
            data-testid="runtime-display-rule-raw-identifier"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 S1_ZT_1.humidity"
          />
        </label>

        <label class="product-runtime-display-rule__field">
          <span>显示名称</span>
          <input
            v-model="form.displayName"
            data-testid="runtime-display-rule-display-name"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 相对湿度"
          />
        </label>

        <label class="product-runtime-display-rule__field">
          <span>单位</span>
          <input
            v-model="form.unit"
            data-testid="runtime-display-rule-unit"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 %RH"
          />
        </label>

        <label v-if="showScenarioCodeField" class="product-runtime-display-rule__field">
          <span>场景编码</span>
          <input
            v-model="form.scenarioCode"
            data-testid="runtime-display-rule-scenario-code"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 phase4-rain-gauge"
          />
        </label>

        <label v-if="showDeviceFamilyField" class="product-runtime-display-rule__field">
          <span>设备族编码</span>
          <input
            v-model="form.deviceFamily"
            data-testid="runtime-display-rule-device-family"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 rain_gauge"
          />
        </label>

        <label v-if="showProtocolCodeField" class="product-runtime-display-rule__field">
          <span>协议编码</span>
          <input
            v-model="form.protocolCode"
            data-testid="runtime-display-rule-protocol-code"
            class="product-runtime-display-rule__input"
            type="text"
            placeholder="例如 mqtt-json"
          />
        </label>
      </div>

      <div class="product-runtime-display-rule__form-actions">
        <StandardButton
          v-permission="'iot:product-contract:govern'"
          data-testid="runtime-display-rule-submit"
          :disabled="!hasProductId(props.productId) || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : isEditing ? '更新规则' : '新增规则' }}
        </StandardButton>
      </div>
    </section>

    <div
      v-if="previewMessages.length"
      class="product-runtime-display-rule__preview"
      data-testid="runtime-display-rule-preview"
    >
      <span v-for="message in previewMessages" :key="message">{{ message }}</span>
    </div>

    <p v-if="loading" class="product-runtime-display-rule__hint">正在加载运行态名称/单位治理规则...</p>
    <p v-else-if="errorMessage" class="product-runtime-display-rule__hint">{{ errorMessage }}</p>

    <div v-else-if="rows.length" class="product-runtime-display-rule__list">
      <article
        v-for="row in rows"
        :key="String(row.id ?? `${row.scopeType || '--'}-${row.rawIdentifier || '--'}`)"
        class="product-runtime-display-rule__item"
        :data-testid="`runtime-display-rule-item-${String(row.id ?? row.rawIdentifier ?? 'unknown')}`"
      >
        <div class="product-runtime-display-rule__headline">
          <div class="product-runtime-display-rule__title">
            <strong>{{ row.displayName || '--' }}</strong>
            <span>{{ `${row.rawIdentifier || '--'} · ${scopeTypeLabel(row.scopeType)}` }}</span>
          </div>
          <span class="product-runtime-display-rule__status">{{ statusLabel(row.status) }}</span>
        </div>

        <div class="product-runtime-display-rule__meta">
          <span>{{ unitLabel(row.unit) }}</span>
          <span>{{ `版本 v${row.versionNo ?? '--'}` }}</span>
          <span v-if="scopeSignatureValue(row)">{{ `范围 ${scopeSignatureValue(row)}` }}</span>
          <span v-if="isFormalCovered(row)">已被正式字段覆盖</span>
        </div>

        <div class="product-runtime-display-rule__item-actions">
          <StandardButton
            v-if="isFormalCovered(row) && row.status !== 'DISABLED'"
            v-permission="'iot:product-contract:govern'"
            :data-testid="`runtime-display-rule-disable-${String(row.id ?? row.rawIdentifier ?? 'unknown')}`"
            :disabled="submitting"
            @click="quickDisableRow(row)"
          >
            停用
          </StandardButton>
          <StandardButton
            :data-testid="`runtime-display-rule-edit-${String(row.id ?? row.rawIdentifier ?? 'unknown')}`"
            @click="startEdit(row)"
          >
            编辑
          </StandardButton>
        </div>
      </article>
    </div>

    <div v-else class="product-runtime-display-rule__empty">
      <strong>当前还没有运行态名称/单位治理规则</strong>
      <p>当设备属性快照出现非正式字段时，可在这里补中文名称和单位，再由对象洞察读侧复用。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import {
  createRuntimeMetricDisplayRule,
  listRuntimeMetricDisplayRules,
  updateRuntimeMetricDisplayRule
} from '@/api/runtimeMetricDisplayRule'
import StandardButton from '@/components/StandardButton.vue'
import { ElMessage } from '@/utils/message'
import type {
  IdType,
  RuntimeMetricDisplayRule,
  RuntimeMetricDisplayRuleScopeType,
  RuntimeMetricDisplayRuleStatus,
  RuntimeMetricDisplayRuleUpsertPayload
} from '@/types/api'

const props = defineProps<{
  productId?: IdType | null
  formalPropertyIdentifiers?: string[]
  focusRawIdentifier?: string | null
  focusToken?: string | number | null
}>()

const route = useRoute()

type RuleFormState = {
  id: IdType | null
  scopeType: RuntimeMetricDisplayRuleScopeType
  status: RuntimeMetricDisplayRuleStatus
  rawIdentifier: string
  displayName: string
  unit: string
  scenarioCode: string
  deviceFamily: string
  protocolCode: string
}

type RuntimeDisplayRouteCandidate = {
  rawIdentifier: string
  displayName: string
  unit: string
  deviceCode: string
}

const scopeTypeOptions: Array<{ label: string; value: RuntimeMetricDisplayRuleScopeType }> = [
  { label: '产品级', value: 'PRODUCT' },
  { label: '设备族级', value: 'DEVICE_FAMILY' },
  { label: '场景级', value: 'SCENARIO' },
  { label: '协议级', value: 'PROTOCOL' },
  { label: '租户默认', value: 'TENANT_DEFAULT' }
]

const rows = ref<RuntimeMetricDisplayRule[]>([])
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const formMessage = ref('')
const form = reactive(createEmptyForm())

const isEditing = computed(() => form.id !== null && form.id !== undefined && form.id !== '')
const activeCount = computed(() => rows.value.filter((row) => row.status !== 'DISABLED').length)
const unitConfiguredCount = computed(() => rows.value.filter((row) => Boolean(row.unit?.trim())).length)
const showScenarioCodeField = computed(
  () => form.scopeType === 'SCENARIO' || form.scopeType === 'DEVICE_FAMILY'
)
const showDeviceFamilyField = computed(() => form.scopeType === 'DEVICE_FAMILY')
const showProtocolCodeField = computed(() => form.scopeType === 'PROTOCOL')
const formalIdentifierSet = computed(
  () => new Set((props.formalPropertyIdentifiers ?? []).map((identifier) => identifier.trim()).filter(Boolean))
)
const routeCandidate = computed<RuntimeDisplayRouteCandidate>(() => ({
  rawIdentifier: queryText('rawIdentifier'),
  displayName: queryText('displayName'),
  unit: queryText('unit'),
  deviceCode: queryText('deviceCode')
}))
const previewMessages = computed(() => {
  if (!routeCandidate.value.rawIdentifier && !form.rawIdentifier.trim()) {
    return []
  }
  const rawIdentifier = routeCandidate.value.rawIdentifier || form.rawIdentifier.trim()
  const messages = ['设备属性快照', '历史趋势', '对象洞察', scopeTypeLabel(form.scopeType)]
  if (hasSameScopeRule(rawIdentifier, form.scopeType)) {
    messages.push('已存在同范围治理规则')
  }
  if (formalIdentifierSet.value.has(rawIdentifier)) {
    messages.push('已被正式字段覆盖')
  }
  return messages
})
const hasDirtyForm = computed(
  () =>
    Boolean(form.rawIdentifier.trim()) ||
    Boolean(form.displayName.trim()) ||
    Boolean(form.unit.trim()) ||
    Boolean(form.scenarioCode.trim()) ||
    Boolean(form.deviceFamily.trim()) ||
    Boolean(form.protocolCode.trim())
)

watch(
  () => props.productId,
  () => {
    resetForm()
    void loadRows()
  },
  { immediate: true }
)

watch(
  () => [props.focusRawIdentifier, props.focusToken],
  () => {
    adoptFocusRawIdentifier(props.focusRawIdentifier)
  },
  { immediate: true }
)

function createEmptyForm(): RuleFormState {
  return {
    id: null,
    scopeType: 'PRODUCT',
    status: 'ACTIVE',
    rawIdentifier: '',
    displayName: '',
    unit: '',
    scenarioCode: '',
    deviceFamily: '',
    protocolCode: ''
  }
}

function queryText(key: string) {
  const value = route.query?.[key]
  if (Array.isArray(value)) {
    return String(value[0] ?? '').trim()
  }
  return typeof value === 'string' ? value.trim() : ''
}

function hasProductId(value: IdType | null | undefined): value is IdType {
  return value !== null && value !== undefined && value !== ''
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  formMessage.value = ''
}

function adoptFocusRawIdentifier(rawIdentifier?: string | null) {
  const normalized = rawIdentifier?.trim() ?? ''
  if (!normalized) {
    return
  }
  Object.assign(form, {
    ...createEmptyForm(),
    rawIdentifier: normalized
  })
  formMessage.value = '已带入原始字段，请补充显示名称和单位'
}

function adoptRouteCandidate() {
  if (!routeCandidate.value.rawIdentifier) {
    return
  }
  Object.assign(form, {
    ...createEmptyForm(),
    rawIdentifier: routeCandidate.value.rawIdentifier,
    displayName: routeCandidate.value.displayName,
    unit: routeCandidate.value.unit
  })
  formMessage.value = '已带入待治理候选，请确认后保存'
}

function startEdit(row: RuntimeMetricDisplayRule) {
  form.id = row.id ?? null
  form.scopeType = normalizeScopeType(row.scopeType)
  form.status = row.status || 'ACTIVE'
  form.rawIdentifier = row.rawIdentifier || ''
  form.displayName = row.displayName || ''
  form.unit = row.unit || ''
  form.scenarioCode = row.scenarioCode || ''
  form.deviceFamily = row.deviceFamily || ''
  form.protocolCode = row.protocolCode || ''
  formMessage.value = row.id != null ? `正在编辑规则 ${row.id}` : '正在编辑未命名规则'
}

function normalizeScopeType(value?: string | null): RuntimeMetricDisplayRuleScopeType {
  const matched = scopeTypeOptions.find((option) => option.value === value)
  return matched?.value ?? 'PRODUCT'
}

function scopeTypeLabel(scopeType?: string | null) {
  return scopeTypeOptions.find((option) => option.value === scopeType)?.label || scopeType || '--'
}

function statusLabel(status?: string | null) {
  return status === 'DISABLED' ? '已停用' : '启用中'
}

function unitLabel(unit?: string | null) {
  return unit?.trim() ? `单位 ${unit.trim()}` : '未设置单位'
}

function scopeSignatureValue(row: RuntimeMetricDisplayRule) {
  switch (row.scopeType) {
    case 'DEVICE_FAMILY':
      return [row.scenarioCode, row.deviceFamily].filter(Boolean).join(' / ')
    case 'SCENARIO':
      return row.scenarioCode || '--'
    case 'PROTOCOL':
      return row.protocolCode || '--'
    case 'TENANT_DEFAULT':
      return '租户默认'
    default:
      return ''
  }
}

function isFormalCovered(row: RuntimeMetricDisplayRule) {
  return Boolean(row.rawIdentifier?.trim() && formalIdentifierSet.value.has(row.rawIdentifier.trim()))
}

function hasSameScopeRule(rawIdentifier: string, scopeType: RuntimeMetricDisplayRuleScopeType) {
  const normalizedRawIdentifier = rawIdentifier.trim()
  if (!normalizedRawIdentifier) {
    return false
  }
  return rows.value.some(
    (row) =>
      row.rawIdentifier?.trim() === normalizedRawIdentifier
      && normalizeScopeType(row.scopeType) === scopeType
  )
}

function normalizeText(value: string) {
  const normalized = value.trim()
  return normalized ? normalized : null
}

function buildPayload(source: RuleFormState): RuntimeMetricDisplayRuleUpsertPayload {
  return {
    scopeType: source.scopeType,
    rawIdentifier: source.rawIdentifier.trim(),
    displayName: source.displayName.trim(),
    unit: normalizeText(source.unit),
    status: source.status,
    scenarioCode: showScenarioCode(source.scopeType) ? normalizeText(source.scenarioCode) : null,
    deviceFamily: source.scopeType === 'DEVICE_FAMILY' ? normalizeText(source.deviceFamily) : null,
    protocolCode: source.scopeType === 'PROTOCOL' ? normalizeText(source.protocolCode) : null
  }
}

function showScenarioCode(scopeType: RuntimeMetricDisplayRuleScopeType) {
  return scopeType === 'SCENARIO' || scopeType === 'DEVICE_FAMILY'
}

function validatePayload(payload: RuntimeMetricDisplayRuleUpsertPayload) {
  if (!payload.rawIdentifier) {
    return '请先填写原始字段标识'
  }
  if (!payload.displayName) {
    return '请先填写显示名称'
  }
  if (payload.scopeType === 'SCENARIO' && !payload.scenarioCode) {
    return '场景级规则必须填写场景编码'
  }
  if (payload.scopeType === 'DEVICE_FAMILY' && !payload.scenarioCode) {
    return '设备族级规则必须填写场景编码'
  }
  if (payload.scopeType === 'DEVICE_FAMILY' && !payload.deviceFamily) {
    return '设备族级规则必须填写设备族编码'
  }
  if (payload.scopeType === 'PROTOCOL' && !payload.protocolCode) {
    return '协议级规则必须填写协议编码'
  }
  return ''
}

function showRequestErrorMessage(error: unknown, fallbackMessage: string) {
  if (isHandledRequestError(error)) {
    return
  }
  ElMessage.error(resolveRequestErrorMessage(error, fallbackMessage))
}

function buildPayloadFromRow(
  row: RuntimeMetricDisplayRule,
  status: RuntimeMetricDisplayRuleStatus
): RuntimeMetricDisplayRuleUpsertPayload {
  const scopeType = normalizeScopeType(row.scopeType)
  return {
    scopeType,
    rawIdentifier: row.rawIdentifier || '',
    displayName: row.displayName || '',
    unit: normalizeText(row.unit || ''),
    status,
    scenarioCode: showScenarioCode(scopeType) ? normalizeText(row.scenarioCode || '') : null,
    deviceFamily: scopeType === 'DEVICE_FAMILY' ? normalizeText(row.deviceFamily || '') : null,
    protocolCode: scopeType === 'PROTOCOL' ? normalizeText(row.protocolCode || '') : null
  }
}

async function quickDisableRow(row: RuntimeMetricDisplayRule) {
  if (!hasProductId(props.productId) || row.id === null || row.id === undefined || row.id === '') {
    return
  }
  submitting.value = true
  try {
    await updateRuntimeMetricDisplayRule(props.productId, row.id, buildPayloadFromRow(row, 'DISABLED'))
    ElMessage.success('运行态名称/单位治理规则已停用')
    await loadRows()
  } catch (error) {
    showRequestErrorMessage(error, '运行态名称/单位治理规则停用失败')
  } finally {
    submitting.value = false
  }
}

async function loadRows() {
  if (!hasProductId(props.productId)) {
    rows.value = []
    errorMessage.value = ''
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await listRuntimeMetricDisplayRules(props.productId, {
      pageNum: 1,
      pageSize: 100
    })
    rows.value = response.data?.records ?? []
  } catch (error) {
    rows.value = []
    errorMessage.value = resolveRequestErrorMessage(error, '运行态名称/单位治理规则加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!hasProductId(props.productId)) {
    return
  }
  const payload = buildPayload(form)
  const validationMessage = validatePayload(payload)
  if (validationMessage) {
    formMessage.value = validationMessage
    return
  }
  submitting.value = true
  formMessage.value = ''
  try {
    if (isEditing.value && form.id != null) {
      await updateRuntimeMetricDisplayRule(props.productId, form.id, payload)
      ElMessage.success('运行态名称/单位治理规则已更新')
    } else {
      await createRuntimeMetricDisplayRule(props.productId, payload)
      ElMessage.success('运行态名称/单位治理规则已新增')
    }
    await loadRows()
    resetForm()
  } catch (error) {
    showRequestErrorMessage(error, isEditing.value ? '运行态名称/单位治理规则更新失败' : '运行态名称/单位治理规则新增失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.product-runtime-display-rule,
.product-runtime-display-rule__summary,
.product-runtime-display-rule__editor,
.product-runtime-display-rule__form-grid,
.product-runtime-display-rule__list,
.product-runtime-display-rule__item,
.product-runtime-display-rule__title {
  display: grid;
}

.product-runtime-display-rule {
  gap: 0.72rem;
}

.product-runtime-display-rule__summary {
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.72rem;
}

.product-runtime-display-rule__summary-card,
.product-runtime-display-rule__editor,
.product-runtime-display-rule__item,
.product-runtime-display-rule__empty {
  padding: 0.82rem 0.9rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: 0.72rem;
  background: white;
}

.product-runtime-display-rule__summary-card {
  gap: 0.24rem;
}

.product-runtime-display-rule__summary-card span,
.product-runtime-display-rule__meta span,
.product-runtime-display-rule__title span,
.product-runtime-display-rule__hint,
.product-runtime-display-rule__empty p,
.product-runtime-display-rule__field span,
.product-runtime-display-rule__status {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.56;
}

.product-runtime-display-rule__summary-card strong,
.product-runtime-display-rule__editor-head strong,
.product-runtime-display-rule__title strong,
.product-runtime-display-rule__empty strong {
  color: var(--text-heading);
}

.product-runtime-display-rule__editor {
  gap: 0.82rem;
}

.product-runtime-display-rule__editor-head,
.product-runtime-display-rule__headline,
.product-runtime-display-rule__item-actions {
  display: flex;
  gap: 0.72rem;
  justify-content: space-between;
  align-items: flex-start;
}

.product-runtime-display-rule__editor-head p {
  margin: 0.2rem 0 0;
  color: var(--text-secondary);
  line-height: 1.56;
}

.product-runtime-display-rule__form-grid {
  grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
  gap: 0.72rem;
}

.product-runtime-display-rule__field {
  display: grid;
  gap: 0.34rem;
}

.product-runtime-display-rule__input,
.product-runtime-display-rule__select {
  min-height: 2.2rem;
  border-radius: 0.58rem;
  border: 1px solid var(--panel-border);
  padding: 0.38rem 0.68rem;
  color: var(--text-heading);
  background: white;
}

.product-runtime-display-rule__form-actions {
  display: flex;
  justify-content: flex-end;
}

.product-runtime-display-rule__list {
  gap: 0.72rem;
}

.product-runtime-display-rule__item {
  gap: 0.62rem;
}

.product-runtime-display-rule__title {
  gap: 0.18rem;
}

.product-runtime-display-rule__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.product-runtime-display-rule__empty {
  display: grid;
  gap: 0.24rem;
}

@media (max-width: 720px) {
  .product-runtime-display-rule__editor-head,
  .product-runtime-display-rule__headline,
  .product-runtime-display-rule__item-actions,
  .product-runtime-display-rule__form-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-runtime-display-rule__form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
