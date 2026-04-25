<template>
  <StandardFormDrawer
    v-model="visible"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    size="46rem"
    destroy-on-close
    @close="handleClose"
  >
    <div class="device-capability-execute-drawer">
      <section class="device-capability-execute-drawer__summary">
        <article class="device-capability-execute-drawer__summary-card">
          <span>设备编码</span>
          <strong>{{ deviceCode || '--' }}</strong>
        </article>
        <article class="device-capability-execute-drawer__summary-card">
          <span>能力分组</span>
          <strong>{{ capability?.group || '--' }}</strong>
        </article>
        <article class="device-capability-execute-drawer__summary-card">
          <span>能力编码</span>
          <strong>{{ capability?.code || '--' }}</strong>
        </article>
      </section>

      <section v-if="templateOptions.length" class="device-capability-execute-drawer__templates">
        <div class="device-capability-execute-drawer__templates-header">
          <strong>参数模板</strong>
          <span>先选模板，再微调参数。</span>
        </div>
        <div class="device-capability-execute-drawer__template-actions">
          <StandardButton
            v-for="template in templateOptions"
            :key="template.key"
            :action="template.kind === 'primary' ? 'confirm' : 'default'"
            class="device-capability-execute-drawer__template-button"
            @click="applyTemplate(template.key)"
          >
            {{ template.label }}
          </StandardButton>
        </div>
      </section>

      <section v-if="schemaEntries.length" class="device-capability-execute-drawer__favorites">
        <div class="device-capability-execute-drawer__favorites-header">
          <strong>常用命令</strong>
          <span>收藏当前参数，点击即可回填。</span>
        </div>
        <div class="device-capability-execute-drawer__favorites-actions">
          <StandardButton action="default" class="device-capability-execute-drawer__favorite-save" @click="saveCurrentAsFavorite">
            收藏当前参数
          </StandardButton>
          <StandardButton
            v-if="favoriteCommands.length"
            action="reset"
            class="device-capability-execute-drawer__favorite-clear"
            @click="clearFavoriteCommands"
          >
            清空收藏
          </StandardButton>
        </div>
        <div v-if="sortedFavoriteCommands.length" class="device-capability-execute-drawer__favorite-list">
          <article
            v-for="favorite in sortedFavoriteCommands"
            :key="favorite.signature"
            class="device-capability-execute-drawer__favorite-card"
          >
            <template v-if="isEditingFavorite(favorite)">
              <el-input
                v-model="favoriteRenameDraft"
                class="device-capability-execute-drawer__favorite-rename-input"
                placeholder="请输入常用命令名称"
              />
              <div class="device-capability-execute-drawer__favorite-edit-actions">
                <StandardButton action="confirm" @click="confirmFavoriteRename(favorite)">
                  保存名称
                </StandardButton>
                <StandardButton action="cancel" @click="cancelFavoriteRename">
                  取消
                </StandardButton>
              </div>
            </template>
            <template v-else>
              <div class="device-capability-execute-drawer__favorite-meta">
                <strong>{{ getFavoriteDisplayLabel(favorite) }}</strong>
                <span v-if="favorite.pinned">已置顶</span>
              </div>
              <div class="device-capability-execute-drawer__favorite-action-row">
                <StandardButton action="confirm" @click="applyFavorite(favorite)">
                  回填
                </StandardButton>
                <StandardButton action="default" @click="toggleFavoritePinned(favorite)">
                  {{ favorite.pinned ? '取消置顶' : '置顶' }}
                </StandardButton>
                <StandardButton action="default" @click="startFavoriteRename(favorite)">
                  重命名
                </StandardButton>
                <StandardButton action="delete" @click="deleteFavorite(favorite)">
                  删除
                </StandardButton>
              </div>
            </template>
          </article>
        </div>
        <StandardInlineState
          v-else
          message="收藏一次常用参数后，可在这里直接回填。"
        />
      </section>

      <section v-if="capabilityPresetOptions.length" class="device-capability-execute-drawer__capability-presets">
        <div class="device-capability-execute-drawer__capability-presets-header">
          <strong>能力专用预设</strong>
          <span>按当前能力特征自动生成。</span>
        </div>
        <div class="device-capability-execute-drawer__capability-presets-actions">
          <StandardButton
            v-for="preset in capabilityPresetOptions"
            :key="preset.key"
            :action="preset.kind === 'primary' ? 'confirm' : 'default'"
            class="device-capability-execute-drawer__capability-preset-button"
            @click="applyCapabilityPreset(preset)"
          >
            {{ preset.label }}
          </StandardButton>
        </div>
      </section>

      <StandardInlineState
        v-if="hasRecentDraft"
        message="已自动回填最近一次参数草稿，可继续微调或切换模板重置。"
      />

      <StandardInlineState
        v-if="capability?.disabledReason"
        :message="capability.disabledReason"
        tone="error"
      />

      <StandardInlineState
        v-else-if="!schemaEntries.length"
        message="当前能力无需额外参数，直接下发即可。"
      />

      <el-form
        ref="formRef"
        :model="formModel"
        :rules="validationRules"
        class="device-capability-execute-drawer__form"
        label-position="top"
      >
        <div v-if="schemaEntries.length" class="device-capability-execute-drawer__grid">
          <el-form-item
            v-for="entry in schemaEntries"
            :key="entry.key"
            :label="entry.label"
            :prop="`params.${entry.key}`"
            class="device-capability-execute-drawer__field"
          >
            <el-input
              v-if="entry.type === 'string'"
              v-model="formModel.params[entry.key]"
              clearable
              :placeholder="entry.placeholder"
            />
            <el-input-number
              v-else-if="entry.type === 'integer'"
              v-model="formModel.params[entry.key]"
              :min="entry.min"
              :max="entry.max"
              controls-position="right"
              class="device-capability-execute-drawer__number"
            />
            <el-input
              v-else
              v-model="formModel.params[entry.key]"
              clearable
              :placeholder="entry.placeholder"
            />

            <p v-if="entry.hint" class="device-capability-execute-drawer__hint">
              {{ entry.hint }}
            </p>
          </el-form-item>
        </div>
      </el-form>
    </div>

    <template #footer>
      <StandardDrawerFooter
        :confirm-loading="submitting"
        confirm-permission="iot:device-capability:execute"
        confirm-text="确认下发"
        @cancel="visible = false"
        @confirm="handleSubmit"
      />
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import StandardButton from '@/components/StandardButton.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import type { DeviceCapability, DeviceCapabilityParamSchemaField } from '@/types/api'

const RECENT_DRAFT_STORAGE_PREFIX = 'iot:device-capability-execute:last-params'
const FAVORITE_STORAGE_PREFIX = 'iot:device-capability-execute:favorites'

type CapabilitySchemaEntry = {
  key: string
  label: string
  type: 'string' | 'integer' | string
  required: boolean
  min?: number
  max?: number
  placeholder: string
  hint: string
}

type TemplateOption = {
  key: 'recent' | 'blank' | 'conservative' | 'demo'
  label: string
  kind: 'primary' | 'default'
}

type FavoriteCommand = {
  signature: string
  label: string
  alias?: string | null
  params: Record<string, unknown>
  pinned?: boolean | null
  updatedAt: string
}

type CapabilityPresetOption = {
  key: string
  label: string
  kind: 'primary' | 'default'
  values: Record<string, unknown>
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    deviceCode?: string
    capability?: DeviceCapability | null
    submitting?: boolean
  }>(),
  {
    deviceCode: '',
    capability: null,
    submitting: false
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'submit', payload: { params: Record<string, unknown> }): void
}>()

const formRef = ref<FormInstance>()
const formModel = reactive<{ params: Record<string, unknown> }>({
  params: {}
})
const hasRecentDraft = ref(false)
const favoriteCommands = ref<FavoriteCommand[]>([])
const favoriteEditingSignature = ref('')
const favoriteRenameDraft = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const drawerTitle = computed(() => `执行能力：${props.capability?.name || '--'}`)
const drawerSubtitle = computed(() => {
  const deviceText = props.deviceCode ? `设备 ${props.deviceCode}` : '当前设备'
  const groupText = props.capability?.group ? ` / ${props.capability.group}` : ''
  return `${deviceText}${groupText}`
})

const schemaEntries = computed<CapabilitySchemaEntry[]>(() => {
  const schema = props.capability?.paramsSchema || {}
  return Object.entries(schema).map(([key, rawField]) => normalizeEntry(key, rawField))
})

const templateOptions = computed<TemplateOption[]>(() => {
  if (!schemaEntries.value.length) {
    return []
  }
  const templates: TemplateOption[] = []
  if (hasRecentDraft.value) {
    templates.push({ key: 'recent', label: '最近一次参数', kind: 'default' })
  }
  templates.push(
    { key: 'blank', label: '空白模板', kind: 'default' },
    { key: 'conservative', label: '保守模板', kind: 'default' },
    { key: 'demo', label: '演示模板', kind: 'primary' }
  )
  return templates
})

const capabilityPresetOptions = computed<CapabilityPresetOption[]>(() => buildCapabilityPresetOptions(schemaEntries.value))
const sortedFavoriteCommands = computed(() => [...favoriteCommands.value].sort(compareFavoriteCommands))

const validationRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  for (const entry of schemaEntries.value) {
    rules[`params.${entry.key}`] = [
      {
        validator: (_rule, value, callback) => {
          const error = validateEntry(entry, value)
          if (error) {
            callback(new Error(error))
            return
          }
          callback()
        },
        trigger: ['blur', 'change']
      }
    ]
  }
  return rules
})

watch(
  () => [props.modelValue, props.capability?.code, props.deviceCode] as const,
  ([visibleNow]) => {
    if (visibleNow) {
      syncFormModel()
      syncFavoriteCommands()
      return
    }
    resetFormModel()
  },
  { immediate: true }
)

function normalizeEntry(key: string, rawField: DeviceCapabilityParamSchemaField | Record<string, unknown> | null | undefined): CapabilitySchemaEntry {
  const field = rawField || {}
  const type = String(field.type || 'string')
  const required = Boolean(field.required)
  const min = toNumber(field.min)
  const max = toNumber(field.max)
  const label = String(field.label || key)
  const placeholder = type === 'integer' ? `请输入${label}` : `请输入${label}`
  const rangeHintParts: string[] = []
  if (required) {
    rangeHintParts.push('必填')
  }
  if (min !== undefined || max !== undefined) {
    rangeHintParts.push(`范围 ${min ?? '-∞'} ~ ${max ?? '+∞'}`)
  }
  return {
    key,
    label,
    type,
    required,
    min,
    max,
    placeholder,
    hint: rangeHintParts.join('，')
  }
}

function validateEntry(entry: CapabilitySchemaEntry, value: unknown) {
  if (entry.type === 'integer') {
    if (value === undefined || value === null || value === '') {
      return entry.required ? `${entry.label}不能为空` : ''
    }
    const numeric = Number(value)
    if (!Number.isFinite(numeric) || Number.isNaN(numeric)) {
      return `${entry.label}必须是整数`
    }
    if (!Number.isInteger(numeric)) {
      return `${entry.label}必须是整数`
    }
    if (entry.min !== undefined && numeric < entry.min) {
      return `${entry.label}不能小于 ${entry.min}`
    }
    if (entry.max !== undefined && numeric > entry.max) {
      return `${entry.label}不能大于 ${entry.max}`
    }
    return ''
  }

  const text = value === undefined || value === null ? '' : String(value).trim()
  if (entry.required && !text) {
    return `${entry.label}不能为空`
  }
  return ''
}

function syncFormModel() {
  const baseParams = createEmptyParams()
  const recentParams = readRecentDraftParams()
  formModel.params = recentParams ? { ...baseParams, ...recentParams } : baseParams
  hasRecentDraft.value = Boolean(recentParams)
  formRef.value?.clearValidate()
}

function resetFormModel() {
  formModel.params = {}
  hasRecentDraft.value = false
  favoriteCommands.value = []
  cancelFavoriteRename()
  formRef.value?.clearValidate()
}

function applyTemplate(templateKey: TemplateOption['key']) {
  if (templateKey === 'recent') {
    const recentParams = readRecentDraftParams()
    if (recentParams) {
      formModel.params = {
        ...createEmptyParams(),
        ...recentParams
      }
      hasRecentDraft.value = true
      formRef.value?.clearValidate()
    }
    return
  }

  const nextParams = createEmptyParams()
  const capabilityName = props.capability?.name || '设备能力'
  for (const entry of schemaEntries.value) {
    if (templateKey === 'blank') {
      continue
    }

    if (templateKey === 'conservative') {
      if (entry.type === 'integer') {
        nextParams[entry.key] = entry.min ?? 0
      } else {
        nextParams[entry.key] = entry.required ? `${entry.label}` : ''
      }
      continue
    }

    if (entry.type === 'integer') {
      nextParams[entry.key] = buildDemoIntegerValue(entry)
    } else {
      nextParams[entry.key] = `${capabilityName}-${entry.label}-示例`
    }
  }

  formModel.params = nextParams
  formRef.value?.clearValidate()
}

function applyCapabilityPreset(preset: CapabilityPresetOption) {
  formModel.params = {
    ...createEmptyParams(),
    ...preset.values
  }
  formRef.value?.clearValidate()
}

function handleClose() {
  resetFormModel()
  emit('update:modelValue', false)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  persistRecentDraft(formModel.params)
  emit('submit', {
    params: {
      ...formModel.params
    }
  })
}

function toNumber(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : undefined
}

function buildDemoIntegerValue(entry: CapabilitySchemaEntry) {
  if (entry.min !== undefined && entry.max !== undefined) {
    return Math.round((entry.min + entry.max) / 2)
  }
  if (entry.min !== undefined) {
    return entry.min
  }
  if (entry.max !== undefined) {
    return entry.max
  }
  return 1
}

function createEmptyParams() {
  const nextParams: Record<string, unknown> = {}
  for (const entry of schemaEntries.value) {
    nextParams[entry.key] = entry.type === 'integer' ? undefined : ''
  }
  return nextParams
}

function normalizeCurrentParams() {
  const nextParams = createEmptyParams()
  for (const entry of schemaEntries.value) {
    nextParams[entry.key] = normalizeDraftValue(entry, formModel.params[entry.key])
  }
  return nextParams
}

function getRecentDraftStorageKey() {
  if (!props.deviceCode || !props.capability?.code) {
    return ''
  }
  return `${RECENT_DRAFT_STORAGE_PREFIX}:${props.deviceCode}:${props.capability.code}`
}

function getFavoriteStorageKey() {
  if (!props.deviceCode || !props.capability?.code) {
    return ''
  }
  return `${FAVORITE_STORAGE_PREFIX}:${props.deviceCode}:${props.capability.code}`
}

function readRecentDraftParams() {
  const storageKey = getRecentDraftStorageKey()
  if (!storageKey || typeof window === 'undefined' || !window.localStorage) {
    return null
  }
  try {
    const raw = window.localStorage.getItem(storageKey)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Record<string, unknown>
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return normalizeDraftParams(parsed)
  } catch {
    return null
  }
}

function persistRecentDraft(params: Record<string, unknown>) {
  const storageKey = getRecentDraftStorageKey()
  if (!storageKey || typeof window === 'undefined' || !window.localStorage) {
    return
  }
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(params))
    hasRecentDraft.value = true
  } catch {
    // 本地草稿失败不影响下发主流程。
  }
}

function syncFavoriteCommands() {
  favoriteCommands.value = readFavoriteCommands()
}

function readFavoriteCommands() {
  const storageKey = getFavoriteStorageKey()
  if (!storageKey || typeof window === 'undefined' || !window.localStorage) {
    return []
  }
  try {
    const raw = window.localStorage.getItem(storageKey)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw) as FavoriteCommand[]
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed
      .map((item) => normalizeFavoriteCommand(item))
      .filter((item): item is FavoriteCommand => Boolean(item))
      .sort(compareFavoriteCommands)
  } catch {
    return []
  }
}

function persistFavoriteCommands(commands: FavoriteCommand[]) {
  const normalizedCommands = normalizeFavoriteCommands(commands)
  const storageKey = getFavoriteStorageKey()
  if (!storageKey || typeof window === 'undefined' || !window.localStorage) {
    favoriteCommands.value = normalizedCommands
    return
  }
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(normalizedCommands))
  } catch {
    // 收藏失败不影响下发主流程。
  }
  favoriteCommands.value = normalizedCommands
}

function saveCurrentAsFavorite() {
  const params = normalizeCurrentParams()
  const existing = favoriteCommands.value.find((item) => item.signature === buildFavoriteSignature(params))
  const favorite: FavoriteCommand = {
    signature: buildFavoriteSignature(params),
    label: buildFavoriteLabel(params),
    alias: existing?.alias || null,
    params,
    pinned: existing?.pinned ?? false,
    updatedAt: new Date().toISOString()
  }

  const nextCommands = [
    favorite,
    ...favoriteCommands.value.filter((item) => item.signature !== favorite.signature)
  ].slice(0, 6)

  persistFavoriteCommands(nextCommands)
}

function clearFavoriteCommands() {
  cancelFavoriteRename()
  persistFavoriteCommands([])
}

function applyFavorite(favorite: FavoriteCommand) {
  formModel.params = {
    ...createEmptyParams(),
    ...favorite.params
  }
  formRef.value?.clearValidate()
}

function normalizeFavoriteCommand(command: FavoriteCommand | Record<string, unknown>) {
  if (!command || typeof command !== 'object') {
    return null
  }
  const signature = typeof command.signature === 'string' ? command.signature : ''
  const label = typeof command.label === 'string' ? command.label : ''
  const alias = typeof command.alias === 'string' && command.alias.trim() ? command.alias.trim() : null
  const pinned = typeof command.pinned === 'boolean' ? command.pinned : false
  const params = command.params && typeof command.params === 'object' ? normalizeDraftParams(command.params as Record<string, unknown>) : null
  const updatedAt = typeof command.updatedAt === 'string' ? command.updatedAt : new Date().toISOString()
  if (!signature || !label || !params) {
    return null
  }
  return {
    signature,
    label,
    alias,
    params,
    pinned,
    updatedAt
  }
}

function normalizeFavoriteCommands(commands: FavoriteCommand[]) {
  return commands
    .map((item) => normalizeFavoriteCommand(item))
    .filter((item): item is FavoriteCommand => Boolean(item))
    .sort(compareFavoriteCommands)
    .slice(0, 6)
}

function compareFavoriteCommands(left: FavoriteCommand, right: FavoriteCommand) {
  if (Boolean(left.pinned) !== Boolean(right.pinned)) {
    return left.pinned ? -1 : 1
  }
  return Date.parse(right.updatedAt || '') - Date.parse(left.updatedAt || '')
}

function buildFavoriteSignature(params: Record<string, unknown>) {
  return JSON.stringify(params)
}

function buildFavoriteLabel(params: Record<string, unknown>) {
  const parts: string[] = []
  for (const entry of schemaEntries.value) {
    const value = params[entry.key]
    if (value === undefined || value === null || value === '') {
      continue
    }
    parts.push(`${entry.label} ${String(value)}`)
    if (parts.length >= 2) {
      break
    }
  }
  if (!parts.length) {
    return '空参数命令'
  }
  return parts.join(' · ')
}

function getFavoriteDisplayLabel(favorite: FavoriteCommand) {
  return favorite.alias?.trim() || favorite.label
}

function isEditingFavorite(favorite: FavoriteCommand) {
  return favoriteEditingSignature.value === favorite.signature
}

function startFavoriteRename(favorite: FavoriteCommand) {
  favoriteEditingSignature.value = favorite.signature
  favoriteRenameDraft.value = favorite.alias?.trim() || favorite.label
}

function confirmFavoriteRename(favorite: FavoriteCommand) {
  const nextAlias = favoriteRenameDraft.value.trim()
  if (!nextAlias) {
    return
  }
  const nextCommands = favoriteCommands.value.map((item) =>
    item.signature === favorite.signature
      ? {
          ...item,
          alias: nextAlias,
          updatedAt: new Date().toISOString()
        }
      : item
  )
  persistFavoriteCommands(nextCommands)
  cancelFavoriteRename()
}

function cancelFavoriteRename() {
  favoriteEditingSignature.value = ''
  favoriteRenameDraft.value = ''
}

function toggleFavoritePinned(favorite: FavoriteCommand) {
  const nextCommands = favoriteCommands.value.map((item) =>
    item.signature === favorite.signature
      ? {
          ...item,
          pinned: !item.pinned,
          updatedAt: new Date().toISOString()
        }
      : item
  )
  persistFavoriteCommands(nextCommands)
}

function deleteFavorite(favorite: FavoriteCommand) {
  const nextCommands = favoriteCommands.value.filter((item) => item.signature !== favorite.signature)
  persistFavoriteCommands(nextCommands)
  if (isEditingFavorite(favorite)) {
    cancelFavoriteRename()
  }
}

function buildCapabilityPresetOptions(entries: CapabilitySchemaEntry[]) {
  if (!entries.length) {
    return []
  }

  if (isBroadcastCapability()) {
    return [
      {
        key: 'broadcast-emergency',
        label: '应急播报',
        kind: 'primary' as const,
        values: buildBroadcastPresetValues(entries, 'emergency')
      },
      {
        key: 'broadcast-routine',
        label: '巡检播报',
        kind: 'default' as const,
        values: buildBroadcastPresetValues(entries, 'routine')
      }
    ]
  }

  if (isPtzCapability()) {
    return [
      {
        key: 'ptz-left',
        label: '左转预设',
        kind: 'primary' as const,
        values: buildPtzPresetValues(entries, 'left')
      },
      {
        key: 'ptz-right',
        label: '右转预设',
        kind: 'default' as const,
        values: buildPtzPresetValues(entries, 'right')
      },
      {
        key: 'ptz-stop',
        label: '停止预设',
        kind: 'default' as const,
        values: buildPtzPresetValues(entries, 'stop')
      }
    ]
  }

  return []
}

function isBroadcastCapability() {
  const text = buildCapabilityFingerprint()
  return /广播|播报|喇叭|sound|voice|alarm|speaker/i.test(text)
}

function isPtzCapability() {
  const text = buildCapabilityFingerprint()
  return /云台|转向|方位|ptz|pan|tilt|zoom/i.test(text)
}

function buildCapabilityFingerprint() {
  return [
    props.capability?.code,
    props.capability?.name,
    props.capability?.group,
    ...schemaEntries.value.flatMap((entry) => [entry.key, entry.label])
  ]
    .filter(Boolean)
    .join(' ')
}

function buildBroadcastPresetValues(entries: CapabilitySchemaEntry[], variant: 'emergency' | 'routine') {
  const values = createEmptyParams()
  for (const entry of entries) {
    const fieldFingerprint = `${entry.key} ${entry.label}`.toLowerCase()
    if (entry.type === 'string') {
      if (/(content|message|text|播报|内容|提示)/i.test(fieldFingerprint)) {
        values[entry.key] =
          variant === 'emergency'
            ? '设备异常，请注意现场安全并立即确认状态。'
            : '设备巡检播报，请保持通行畅通。'
        continue
      }
      values[entry.key] = entry.required ? entry.label : ''
      continue
    }

    if (/(volume|音量|loud|speaker)/i.test(fieldFingerprint)) {
      values[entry.key] = variant === 'emergency' ? pickNumericValue(entry, 85) : pickNumericValue(entry, 55)
      continue
    }
    if (/(duration|时长|time|秒|min)/i.test(fieldFingerprint)) {
      values[entry.key] = variant === 'emergency' ? pickNumericValue(entry, 30) : pickNumericValue(entry, 10)
      continue
    }
    values[entry.key] = pickNumericValue(entry, variant === 'emergency' ? 2 : 1)
  }
  return values
}

function buildPtzPresetValues(entries: CapabilitySchemaEntry[], direction: 'left' | 'right' | 'stop') {
  const values = createEmptyParams()
  for (const entry of entries) {
    const fieldFingerprint = `${entry.key} ${entry.label}`.toLowerCase()
    if (entry.type === 'string') {
      if (/(direction|动作|action|mode|command|指令|方向)/i.test(fieldFingerprint)) {
        values[entry.key] =
          direction === 'left' ? 'left' : direction === 'right' ? 'right' : 'stop'
        continue
      }
      if (/(content|message|text|播报|内容|提示)/i.test(fieldFingerprint)) {
        values[entry.key] = direction === 'stop' ? '停止预设' : `${direction === 'left' ? '左转' : '右转'}预设执行`
        continue
      }
      values[entry.key] = entry.required ? entry.label : ''
      continue
    }

    if (/(speed|速度|rate|rateNo|level)/i.test(fieldFingerprint)) {
      values[entry.key] =
        direction === 'stop' ? pickNumericValue(entry, 0) : pickNumericValue(entry, 3)
      continue
    }
    if (/(angle|角度|position|pos|step)/i.test(fieldFingerprint)) {
      values[entry.key] =
        direction === 'stop' ? pickNumericValue(entry, 0) : pickNumericValue(entry, 15)
      continue
    }
    values[entry.key] = pickNumericValue(entry, direction === 'stop' ? 0 : 1)
  }
  return values
}

function pickNumericValue(entry: CapabilitySchemaEntry, preferred: number) {
  if (entry.min !== undefined && entry.max !== undefined) {
    const clampedPreferred = Math.min(entry.max, Math.max(entry.min, preferred))
    return Math.trunc(clampedPreferred)
  }
  if (entry.min !== undefined) {
    return Math.trunc(Math.max(entry.min, preferred))
  }
  if (entry.max !== undefined) {
    return Math.trunc(Math.min(entry.max, preferred))
  }
  return Math.trunc(preferred)
}

function normalizeDraftParams(params: Record<string, unknown>) {
  const nextParams = createEmptyParams()
  for (const entry of schemaEntries.value) {
    if (!Object.prototype.hasOwnProperty.call(params, entry.key)) {
      continue
    }
    nextParams[entry.key] = normalizeDraftValue(entry, params[entry.key])
  }
  return nextParams
}

function normalizeDraftValue(entry: CapabilitySchemaEntry, value: unknown) {
  if (entry.type === 'integer') {
    const numeric = toNumber(value)
    return numeric === undefined ? undefined : Math.trunc(numeric)
  }
  return value === undefined || value === null ? '' : String(value)
}
</script>

<style scoped>
.device-capability-execute-drawer {
  display: grid;
  gap: 0.95rem;
}

.device-capability-execute-drawer__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.device-capability-execute-drawer__summary-card {
  display: grid;
  gap: 0.32rem;
  min-width: 0;
  padding: 0.88rem 0.96rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.94);
}

.device-capability-execute-drawer__summary-card span,
.device-capability-execute-drawer__hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.55;
}

.device-capability-execute-drawer__summary-card strong {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.device-capability-execute-drawer__templates {
  display: grid;
  gap: 0.65rem;
  padding: 0.86rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-capability-execute-drawer__templates-header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: baseline;
}

.device-capability-execute-drawer__templates-header strong {
  color: var(--text-heading);
  font-size: 14px;
}

.device-capability-execute-drawer__templates-header span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.device-capability-execute-drawer__template-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.device-capability-execute-drawer__template-button {
  min-width: 7.5rem;
}

.device-capability-execute-drawer__favorites,
.device-capability-execute-drawer__capability-presets {
  display: grid;
  gap: 0.65rem;
  padding: 0.86rem 0.92rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(248, 251, 255, 0.92);
}

.device-capability-execute-drawer__favorites-header,
.device-capability-execute-drawer__capability-presets-header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: baseline;
}

.device-capability-execute-drawer__favorites-header strong,
.device-capability-execute-drawer__capability-presets-header strong {
  color: var(--text-heading);
  font-size: 14px;
}

.device-capability-execute-drawer__favorites-header span,
.device-capability-execute-drawer__capability-presets-header span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.device-capability-execute-drawer__favorites-actions,
.device-capability-execute-drawer__capability-presets-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.device-capability-execute-drawer__favorite-list {
  display: grid;
  gap: 0.55rem;
}

.device-capability-execute-drawer__favorite-card {
  display: grid;
  gap: 0.55rem;
  padding: 0.82rem 0.86rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.92);
}

.device-capability-execute-drawer__favorite-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem 0.65rem;
  align-items: baseline;
}

.device-capability-execute-drawer__favorite-meta strong {
  color: var(--text-heading);
  font-size: 14px;
  line-height: 1.5;
}

.device-capability-execute-drawer__favorite-meta span {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.device-capability-execute-drawer__favorite-action-row,
.device-capability-execute-drawer__favorite-edit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.device-capability-execute-drawer__favorite-rename-input {
  width: 100%;
}

.device-capability-execute-drawer__form {
  display: grid;
  gap: 0.85rem;
}

.device-capability-execute-drawer__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.2rem 0.9rem;
}

.device-capability-execute-drawer__field {
  min-width: 0;
}

.device-capability-execute-drawer__field :deep(.el-form-item__label) {
  padding-bottom: 0.3rem;
  color: var(--text-secondary);
  font-weight: 600;
}

.device-capability-execute-drawer__number {
  width: 100%;
}

.device-capability-execute-drawer__hint {
  margin: 0.34rem 0 0;
}

@media (max-width: 900px) {
  .device-capability-execute-drawer__summary,
  .device-capability-execute-drawer__templates-header,
  .device-capability-execute-drawer__favorites-header,
  .device-capability-execute-drawer__capability-presets-header,
  .device-capability-execute-drawer__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-capability-execute-drawer__templates-header {
    display: grid;
  }

  .device-capability-execute-drawer__favorites-header,
  .device-capability-execute-drawer__capability-presets-header {
    display: grid;
  }
}
</style>
