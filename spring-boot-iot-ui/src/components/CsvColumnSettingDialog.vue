<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="520px"
    class="sys-dialog"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <div class="csv-column-setting">
      <div v-if="allPresets.length" class="csv-column-setting__presets">
        <span class="csv-column-setting__preset-label">导出模板：</span>
        <div v-for="preset in allPresets" :key="`${preset.label}-${preset.custom ? 'c' : 'b'}`" class="csv-column-setting__preset-item">
          <el-button link @click="applyPreset(preset.keys)">
            {{ preset.label }}
          </el-button>
          <el-button
            v-if="preset.custom"
            link
            type="danger"
            @click="handleDeletePreset(preset.label)"
          >
            删除
          </el-button>
        </div>
      </div>
      <div v-for="(item, index) in rows" :key="item.key" class="csv-column-setting__row">
        <el-checkbox v-model="item.checked">{{ item.label }}</el-checkbox>
        <div class="csv-column-setting__actions">
          <el-button link :disabled="index === 0" @click="moveUp(index)">上移</el-button>
          <el-button link :disabled="index === rows.length - 1" @click="moveDown(index)">下移</el-button>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleSavePreset">保存为模板</el-button>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleExportPresets">导出模板JSON</el-button>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleImportPresets">导入模板JSON</el-button>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleResetDefault">恢复默认</el-button>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from '@/utils/message'
import { ElMessageBox } from '@/utils/messageBox'
import {
  loadCsvPresetTemplates,
  saveCsvPresetTemplates,
  type CsvColumnOption,
  type CsvColumnPreset
} from '@/utils/csvColumns'

interface RowItem extends CsvColumnOption {
  checked: boolean
}

const props = defineProps<{
  modelValue: boolean
  title?: string
  options: CsvColumnOption[]
  selectedKeys: string[]
  presets?: Array<{
    label: string
    keys: string[]
  }>
  presetStorageKey?: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'confirm', selectedKeys: string[]): void
}>()

const rows = ref<RowItem[]>([])
const customPresets = ref<CsvColumnPreset[]>([])
const builtInPresets = computed(() => props.presets || [])
const allPresets = computed(() => [
  ...builtInPresets.value.map((item) => ({ ...item, custom: false })),
  ...customPresets.value.map((item) => ({ ...item, custom: true }))
])

const dialogTitle = computed(() => props.title || '导出列设置')

const syncRows = () => {
  const selected = new Set(props.selectedKeys)
  rows.value = props.options.map((item) => ({
    ...item,
    checked: selected.has(item.key)
  }))
}

watch(
  () => [props.modelValue, props.options, props.selectedKeys] as const,
  () => {
    if (props.modelValue) {
      syncRows()
      customPresets.value = props.presetStorageKey ? loadCsvPresetTemplates(props.presetStorageKey) : []
    }
  },
  { immediate: true, deep: true }
)

const getCurrentSelectedKeys = () => rows.value.filter((item) => item.checked).map((item) => item.key)

const moveUp = (index: number) => {
  if (index <= 0) return
  const copy = [...rows.value]
  ;[copy[index - 1], copy[index]] = [copy[index], copy[index - 1]]
  rows.value = copy
}

const moveDown = (index: number) => {
  if (index >= rows.value.length - 1) return
  const copy = [...rows.value]
  ;[copy[index], copy[index + 1]] = [copy[index + 1], copy[index]]
  rows.value = copy
}

const handleConfirm = () => {
  const selected = getCurrentSelectedKeys()
  if (!selected.length) {
    ElMessage.warning('至少保留一个导出列')
    return
  }
  emit('confirm', selected)
  emit('update:modelValue', false)
}

const handleResetDefault = () => {
  rows.value = props.options.map((item) => ({
    ...item,
    checked: true
  }))
}

const applyPreset = (presetKeys: string[]) => {
  const keySet = new Set(presetKeys)
  const map = new Map(props.options.map((item) => [item.key, item]))
  const ordered = [
    ...presetKeys.map((key) => map.get(key)).filter((item): item is CsvColumnOption => Boolean(item)),
    ...props.options.filter((item) => !keySet.has(item.key))
  ]
  rows.value = ordered.map((item) => ({
    ...item,
    checked: keySet.has(item.key)
  }))
}

const handleSavePreset = async () => {
  if (!props.presetStorageKey) {
    ElMessage.warning('当前页面未启用模板保存')
    return
  }
  const selectedKeys = getCurrentSelectedKeys()
  if (!selectedKeys.length) {
    ElMessage.warning('至少保留一个导出列后再保存模板')
    return
  }
  try {
    const result = await ElMessageBox.prompt('请输入模板名称', '保存导出模板', {
      inputValue: '',
      inputPattern: /\S+/,
      inputErrorMessage: '模板名称不能为空'
    })
    const label = result.value.trim()
    const next = customPresets.value.filter((item) => item.label !== label)
    next.unshift({ label, keys: selectedKeys })
    customPresets.value = next.slice(0, 20)
    saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
    ElMessage.success('模板保存成功')
  } catch {
    // 用户取消输入
  }
}

const handleDeletePreset = (label: string) => {
  if (!props.presetStorageKey) {
    return
  }
  customPresets.value = customPresets.value.filter((item) => item.label !== label)
  saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
}

const handleExportPresets = () => {
  if (!props.presetStorageKey) {
    ElMessage.warning('当前页面未启用模板导出')
    return
  }
  if (!customPresets.value.length) {
    ElMessage.warning('暂无自定义模板可导出')
    return
  }
  const payload = {
    version: 1,
    source: props.presetStorageKey,
    exportedAt: new Date().toISOString(),
    presets: customPresets.value
  }
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `导出模板-${props.presetStorageKey}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

const handleImportPresets = () => {
  if (!props.presetStorageKey) {
    ElMessage.warning('当前页面未启用模板导入')
    return
  }
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'application/json,.json'
  input.onchange = () => {
    const file = input.files?.[0]
    if (!file) {
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      try {
        const parsed = JSON.parse(String(reader.result || '{}'))
        const importedPresets = Array.isArray(parsed?.presets) ? parsed.presets : []
        const normalized = importedPresets
          .filter((item) => item && typeof item.label === 'string' && Array.isArray(item.keys))
          .map((item) => ({
            label: item.label.trim(),
            keys: item.keys.filter((key: unknown) => typeof key === 'string')
          }))
          .filter((item) => item.label && item.keys.length)
        if (!normalized.length) {
          ElMessage.warning('未识别到有效模板')
          return
        }
        const merged = [...customPresets.value, ...normalized]
          .reduce((map, item) => map.set(item.label, item), new Map<string, CsvColumnPreset>())
        customPresets.value = Array.from(merged.values()).slice(0, 20)
        saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
        ElMessage.success(`已导入 ${normalized.length} 个模板`)
      } catch {
        ElMessage.error('导入失败，文件内容不是有效JSON')
      }
    }
    reader.readAsText(file)
  }
  input.click()
}
</script>

<style scoped>
.csv-column-setting {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow: auto;
  padding: 4px 2px;
}

.csv-column-setting__presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  padding: 6px 8px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.csv-column-setting__preset-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.csv-column-setting__preset-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.csv-column-setting__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid var(--iot-border-color, #dcdfe6);
  border-radius: 8px;
  background: var(--el-bg-color-page);
}

.csv-column-setting__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
