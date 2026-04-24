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

import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import type { DeviceCapability, DeviceCapabilityParamSchemaField } from '@/types/api'

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
  const nextParams: Record<string, unknown> = {}
  for (const entry of schemaEntries.value) {
    nextParams[entry.key] = entry.type === 'integer' ? undefined : ''
  }
  formModel.params = nextParams
  formRef.value?.clearValidate()
}

function resetFormModel() {
  formModel.params = {}
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
  .device-capability-execute-drawer__grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
