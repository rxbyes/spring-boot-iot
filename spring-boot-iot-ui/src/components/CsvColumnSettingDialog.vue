<template>
  <StandardFormDrawer
    :model-value="modelValue"
    :title="dialogTitle"
    subtitle="统一通过右侧抽屉维护导出列、模板分组与导入导出设置。"
    size="38rem"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <div class="csv-column-setting">
      <div class="csv-column-setting__toolbar">
        <el-input
          v-model="presetKeyword"
          clearable
          placeholder="搜索模板名称"
          class="csv-column-setting__search"
        />
        <StandardActionLink @click="clearRecentPresets">清空最近</StandardActionLink>
      </div>
      <div v-if="allPresets.length" class="csv-column-setting__presets">
        <span class="csv-column-setting__preset-label">导出模板：</span>
        <div v-for="preset in filteredPresets" :key="`${preset.label}-${preset.custom ? 'c' : 'b'}`" class="csv-column-setting__preset-item">
          <div class="csv-column-setting__preset-main">
            <StandardActionLink @click="applyPreset(preset)">{{ preset.label }}</StandardActionLink>
            <el-tag v-if="isPinnedPreset(preset.label)" size="small" type="success" effect="plain">置顶</el-tag>
            <el-tag v-if="isRecentPreset(preset.label)" size="small" type="warning" effect="plain">最近</el-tag>
            <el-tag v-if="preset.group" size="small" effect="plain">{{ preset.group }}</el-tag>
            <StandardActionLink @click="togglePinnedPreset(preset.label)">
              {{ isPinnedPreset(preset.label) ? '取消置顶' : '置顶' }}
            </StandardActionLink>
            <StandardActionLink
              v-if="preset.custom"
              @click="handleRenamePreset(preset)"
            >
              重命名
            </StandardActionLink>
            <StandardActionLink
              v-if="preset.custom"
              @click="handleDeletePreset(preset.label)"
            >
              删除
            </StandardActionLink>
          </div>
          <div class="csv-column-setting__preset-meta">
            <span>最近使用：{{ formatPresetTime(getRecentUsedAt(preset.label)) }}</span>
            <span>最后修改：{{ formatPresetTime(preset.lastModifiedAt) }}</span>
          </div>
        </div>
      </div>
      <div v-if="presetGroups.length" class="csv-column-setting__groups">
        <span class="csv-column-setting__preset-label">分组筛选：</span>
        <StandardButton
          v-for="group in presetGroups"
          :key="group"
          :type="activeGroup === group ? 'primary' : ''"
          text
          @click="activeGroup = group"
        >
          {{ group }}
        </StandardButton>
      </div>
      <div v-for="(item, index) in rows" :key="item.key" class="csv-column-setting__row">
        <el-checkbox v-model="item.checked">{{ item.label }}</el-checkbox>
        <div class="csv-column-setting__actions">
          <StandardActionLink :disabled="index === 0" @click="moveUp(index)">上移</StandardActionLink>
          <StandardActionLink :disabled="index === rows.length - 1" @click="moveDown(index)">下移</StandardActionLink>
        </div>
      </div>
    </div>
    <template #footer>
      <StandardButton action="reset" class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleSavePreset">保存为模板</StandardButton>
      <StandardButton action="reset" class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleExportPresets">导出模板JSON</StandardButton>
      <StandardButton action="reset" class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleImportPresets">导入模板JSON</StandardButton>
      <StandardButton action="reset" class="sys-dialog__btn sys-dialog__btn--ghost" @click="handleResetDefault">恢复默认</StandardButton>
      <StandardButton action="cancel" class="sys-dialog__btn sys-dialog__btn--ghost" @click="emit('update:modelValue', false)">取消</StandardButton>
      <StandardButton action="confirm" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleConfirm">确定</StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    v-model="importPreviewDetailVisible"
    title="导入预览明细"
    :subtitle="`冲突策略：${resolveStrategyLabel(importPreviewDetailStrategy)}`"
    size="44rem"
    @close="handleImportPreviewDetailClose"
  >
    <div class="csv-import-preview">
      <div class="csv-import-preview__summary">
        <span>冲突策略：{{ resolveStrategyLabel(importPreviewDetailStrategy) }}</span>
        <span>新增 {{ (importPreviewDetail?.added || []).length }} 个</span>
        <span>覆盖 {{ (importPreviewDetail?.overwritten || []).length }} 个</span>
        <span>重命名 {{ (importPreviewDetail?.renamed || []).length }} 个</span>
        <span>跳过 {{ (importPreviewDetail?.skipped || []).length }} 个</span>
      </div>
      <el-tabs v-model="importPreviewDetailTab" class="csv-import-preview__tabs">
        <el-tab-pane
          v-for="section in importPreviewSections"
          :key="section.key"
          :label="`${section.label} (${section.items.length})`"
          :name="section.key"
        >
          <div v-if="!section.items.length" class="csv-import-preview__empty">当前分类无数据</div>
          <el-scrollbar v-else max-height="300px">
            <ol class="csv-import-preview__list">
              <li v-for="item in section.items" :key="`${section.key}-${item}`">{{ item }}</li>
            </ol>
          </el-scrollbar>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer>
      <StandardButton action="cancel" class="sys-dialog__btn sys-dialog__btn--ghost" @click="closeImportPreviewDetail('back')">取消导入</StandardButton>
      <StandardButton action="confirm" class="sys-dialog__btn sys-dialog__btn--primary" @click="closeImportPreviewDetail('confirm')">
        确认导入
      </StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    v-model="presetEditorVisible"
    :title="presetEditorTitle"
    :subtitle="presetEditorSubtitle"
    size="34rem"
    @close="closePresetEditor"
  >
    <el-form label-width="90px" class="csv-column-setting__form">
      <el-form-item label="模板名称">
        <el-input
          v-model="presetEditorInput"
          :placeholder="presetEditorPlaceholder"
          @keyup.enter="confirmPresetEditor"
        />
        <p class="csv-column-setting__hint">支持「分组/模板名」格式，例如：`运维/夜班模板`。</p>
      </el-form-item>
    </el-form>
    <template #footer>
      <StandardButton action="cancel" class="sys-dialog__btn sys-dialog__btn--ghost" @click="closePresetEditor">取消</StandardButton>
      <StandardButton action="confirm" class="sys-dialog__btn sys-dialog__btn--primary" @click="confirmPresetEditor">
        {{ presetEditorConfirmText }}
      </StandardButton>
    </template>
  </StandardFormDrawer>

  <StandardFormDrawer
    v-model="importStrategyVisible"
    title="导入冲突处理"
    :subtitle="importStrategySubtitle"
    size="34rem"
    @close="closeImportStrategy"
  >
    <div class="csv-column-setting__strategy-grid">
      <button
        type="button"
        class="csv-column-setting__strategy-card"
        :class="{ 'is-active': importStrategyValue === 'overwrite' }"
        @click="importStrategyValue = 'overwrite'"
      >
        <strong>覆盖</strong>
        <p>直接使用导入模板替换本地同名模板。</p>
      </button>
      <button
        type="button"
        class="csv-column-setting__strategy-card"
        :class="{ 'is-active': importStrategyValue === 'skip' }"
        @click="importStrategyValue = 'skip'"
      >
        <strong>跳过</strong>
        <p>保留本地模板，忽略导入文件中的同名模板。</p>
      </button>
      <button
        type="button"
        class="csv-column-setting__strategy-card"
        :class="{ 'is-active': importStrategyValue === 'rename' }"
        @click="importStrategyValue = 'rename'"
      >
        <strong>重命名</strong>
        <p>为导入的同名模板自动追加后缀，保留两份模板。</p>
      </button>
    </div>
    <template #footer>
      <StandardButton action="cancel" class="sys-dialog__btn sys-dialog__btn--ghost" @click="closeImportStrategy">取消</StandardButton>
      <StandardButton action="confirm" class="sys-dialog__btn sys-dialog__btn--primary" @click="confirmImportStrategy">
        确定策略
      </StandardButton>
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from '@/utils/message'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import {
  loadCsvPinnedPresetLabels,
  loadCsvRecentPresetRecords,
  loadCsvPresetTemplates,
  saveCsvPinnedPresetLabels,
  saveCsvRecentPresetRecords,
  saveCsvPresetTemplates,
  type CsvColumnOption,
  type CsvColumnPreset,
  type CsvRecentPresetRecord
} from '@/utils/csvColumns'

interface RowItem extends CsvColumnOption {
  checked: boolean
}

const props = defineProps<{
  modelValue: boolean
  title?: string
  options: CsvColumnOption[]
  selectedKeys: string[]
  presets?: CsvColumnPreset[]
  presetStorageKey?: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'confirm', selectedKeys: string[]): void
}>()

const rows = ref<RowItem[]>([])
const customPresets = ref<CsvColumnPreset[]>([])
const activeGroup = ref('全部')
const presetKeyword = ref('')
const recentPresetRecords = ref<CsvRecentPresetRecord[]>([])
const pinnedPresetLabels = ref<string[]>([])
const builtInPresets = computed(() =>
  (props.presets || []).map((item) => ({
    ...item,
    group: item.group || inferGroupByLabel(item.label)
  }))
)
const allPresets = computed(() => [
  ...builtInPresets.value.map((item) => ({ ...item, custom: false })),
  ...customPresets.value.map((item) => ({ ...item, custom: true }))
])
const sortedPresets = computed(() => {
  const pinnedIndexMap = new Map(pinnedPresetLabels.value.map((label, index) => [label, index]))
  const indexMap = new Map(recentPresetRecords.value.map((item, index) => [item.label, index]))
  return [...allPresets.value].sort((a, b) => {
    const ap = pinnedIndexMap.has(a.label) ? (pinnedIndexMap.get(a.label) as number) : Number.MAX_SAFE_INTEGER
    const bp = pinnedIndexMap.has(b.label) ? (pinnedIndexMap.get(b.label) as number) : Number.MAX_SAFE_INTEGER
    if (ap !== bp) {
      return ap - bp
    }
    const ai = indexMap.has(a.label) ? (indexMap.get(a.label) as number) : Number.MAX_SAFE_INTEGER
    const bi = indexMap.has(b.label) ? (indexMap.get(b.label) as number) : Number.MAX_SAFE_INTEGER
    if (ai !== bi) {
      return ai - bi
    }
    return a.label.localeCompare(b.label, 'zh-CN')
  })
})
const presetGroups = computed(() => {
  const groups = sortedPresets.value
    .map((item) => item.group?.trim())
    .filter((item): item is string => Boolean(item))
  return ['全部', ...Array.from(new Set(groups))]
})
const filteredPresets = computed(() =>
  sortedPresets.value.filter((item) => {
    const groupMatched = activeGroup.value === '全部' || item.group === activeGroup.value
    const keyword = presetKeyword.value.trim().toLowerCase()
    const keywordMatched = !keyword || item.label.toLowerCase().includes(keyword)
    return groupMatched && keywordMatched
  })
)

type ImportPreviewStrategy = 'overwrite' | 'skip' | 'rename'

const dialogTitle = computed(() => props.title || '导出列设置')
type PresetEditorMode = 'save' | 'rename'
const presetEditorVisible = ref(false)
const presetEditorMode = ref<PresetEditorMode>('save')
const presetEditorInput = ref('')
let presetEditorResolver: ((value: string | null) => void) | null = null
const presetEditorTitle = computed(() => (presetEditorMode.value === 'save' ? '保存导出模板' : '重命名模板'))
const presetEditorSubtitle = computed(() =>
  presetEditorMode.value === 'save'
    ? '统一通过右侧抽屉保存自定义导出模板。'
    : '统一通过右侧抽屉修改模板名称和分组。'
)
const presetEditorPlaceholder = computed(() =>
  presetEditorMode.value === 'save' ? '请输入模板名' : '请输入新的模板名'
)
const presetEditorConfirmText = computed(() => (presetEditorMode.value === 'save' ? '保存模板' : '确认重命名'))
const importStrategyVisible = ref(false)
const importStrategyValue = ref<ImportPreviewStrategy>('overwrite')
const importStrategyConflictCount = ref(0)
let importStrategyResolver: ((value: ImportPreviewStrategy) => void) | null = null
const importStrategySubtitle = computed(
  () => `检测到 ${importStrategyConflictCount.value} 个同名模板，请选择导入冲突处理方式。`
)

const openPresetEditor = (mode: PresetEditorMode, initialValue = '') =>
  new Promise<string | null>((resolve) => {
    presetEditorMode.value = mode
    presetEditorInput.value = initialValue
    presetEditorVisible.value = true
    presetEditorResolver = resolve
  })

const confirmPresetEditor = () => {
  const value = presetEditorInput.value.trim()
  if (!value) {
    ElMessage.warning('模板名称不能为空')
    return
  }
  presetEditorVisible.value = false
  presetEditorInput.value = ''
  if (presetEditorResolver) {
    const resolve = presetEditorResolver
    presetEditorResolver = null
    resolve(value)
  }
}

const closePresetEditor = () => {
  presetEditorVisible.value = false
  presetEditorInput.value = ''
  if (presetEditorResolver) {
    const resolve = presetEditorResolver
    presetEditorResolver = null
    resolve(null)
  }
}

const openImportStrategy = (conflictCount: number) =>
  new Promise<ImportPreviewStrategy>((resolve) => {
    importStrategyConflictCount.value = conflictCount
    importStrategyValue.value = 'overwrite'
    importStrategyVisible.value = true
    importStrategyResolver = resolve
  })

const confirmImportStrategy = () => {
  importStrategyVisible.value = false
  if (importStrategyResolver) {
    const resolve = importStrategyResolver
    importStrategyResolver = null
    resolve(importStrategyValue.value)
  }
}

const closeImportStrategy = () => {
  importStrategyVisible.value = false
  if (importStrategyResolver) {
    const resolve = importStrategyResolver
    importStrategyResolver = null
    resolve('overwrite')
  }
}

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
      recentPresetRecords.value = props.presetStorageKey ? loadCsvRecentPresetRecords(props.presetStorageKey) : []
      pinnedPresetLabels.value = props.presetStorageKey ? loadCsvPinnedPresetLabels(props.presetStorageKey) : []
      activeGroup.value = '全部'
      presetKeyword.value = ''
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

const applyPreset = (preset: { label: string; keys: string[] }) => {
  const presetKeys = preset.keys
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
  touchRecentPreset(preset.label)
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
  const result = await openPresetEditor('save')
  if (!result) {
    return
  }
  const parsed = parsePresetText(result)
  const label = parsed.label
  const group = parsed.group
  const existed = customPresets.value.some((item) => item.label === label)
  if (!existed && customPresets.value.length >= 20) {
    ElMessage.warning('自定义模板最多 20 个，请先删除部分模板再新增')
    return
  }
  const next = customPresets.value.filter((item) => item.label !== label)
  next.unshift({ label, group, keys: selectedKeys, lastModifiedAt: new Date().toISOString() })
  customPresets.value = next.slice(0, 20)
  saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
  ElMessage.success('模板保存成功')
}

const handleRenamePreset = async (preset: CsvColumnPreset) => {
  if (!props.presetStorageKey) {
    return
  }
  const initValue = preset.group ? `${preset.group}/${preset.label}` : preset.label
  const result = await openPresetEditor('rename', initValue)
  if (!result) {
    return
  }
  const parsed = parsePresetText(result)
  const next = customPresets.value
    .filter((item) => item.label !== preset.label)
    .filter((item) => item.label !== parsed.label)
  next.unshift({ ...preset, label: parsed.label, group: parsed.group, lastModifiedAt: new Date().toISOString() })
  customPresets.value = next.slice(0, 20)
  saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
  recentPresetRecords.value = recentPresetRecords.value.map((item) =>
    item.label === preset.label ? { ...item, label: parsed.label } : item
  )
  pinnedPresetLabels.value = pinnedPresetLabels.value.map((item) =>
    item === preset.label ? parsed.label : item
  )
  saveCsvRecentPresetRecords(props.presetStorageKey, recentPresetRecords.value)
  saveCsvPinnedPresetLabels(props.presetStorageKey, pinnedPresetLabels.value)
  ElMessage.success('模板重命名成功')
}

const handleDeletePreset = (label: string) => {
  if (!props.presetStorageKey) {
    return
  }
  customPresets.value = customPresets.value.filter((item) => item.label !== label)
  recentPresetRecords.value = recentPresetRecords.value.filter((item) => item.label !== label)
  pinnedPresetLabels.value = pinnedPresetLabels.value.filter((item) => item !== label)
  saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
  saveCsvRecentPresetRecords(props.presetStorageKey, recentPresetRecords.value)
  saveCsvPinnedPresetLabels(props.presetStorageKey, pinnedPresetLabels.value)
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
      void (async () => {
        try {
        const parsed = JSON.parse(String(reader.result || '{}'))
        const importedPresets = Array.isArray(parsed?.presets) ? parsed.presets : []
        const normalized: CsvColumnPreset[] = importedPresets
          .filter((item) => item && typeof item.label === 'string' && Array.isArray(item.keys))
          .map((item) => ({
            label: item.label.trim(),
            keys: item.keys.filter((key: unknown) => typeof key === 'string'),
            group: typeof item.group === 'string' ? item.group.trim() : undefined,
            lastModifiedAt: typeof item.lastModifiedAt === 'string' ? item.lastModifiedAt : new Date().toISOString()
          }))
          .filter((item) => item.label && item.keys.length)
        if (!normalized.length) {
          ElMessage.warning('未识别到有效模板')
          return
        }
        const conflictLabels = normalized
          .map((item) => item.label)
          .filter((label) => customPresets.value.some((preset) => preset.label === label))
        const strategy = await resolveImportConflictStrategy(conflictLabels.length)
        const preview = buildImportPreview(customPresets.value, normalized, strategy)
        const confirmed = await confirmImportPreview(preview, strategy)
        if (!confirmed) {
          return
        }
        const merged = mergeImportedPresets(customPresets.value, normalized, strategy)
        if (merged.length > 20) {
          ElMessage.warning(`导入后模板超过 20 个，已仅保留前 20 个`)
        }
        customPresets.value = merged.slice(0, 20)
        saveCsvPresetTemplates(props.presetStorageKey, customPresets.value)
        ElMessage.success(
          `已导入 ${normalized.length} 个模板（导入不会修改当前导出列配置，需手动应用模板）`
        )
      } catch {
        ElMessage.error('导入失败，文件内容不是有效JSON')
      }
      })()
    }
    reader.readAsText(file)
  }
  input.click()
}

const isRecentPreset = (label: string) =>
  recentPresetRecords.value.some((item) => item.label === label)

const clearRecentPresets = () => {
  recentPresetRecords.value = []
  if (props.presetStorageKey) {
    saveCsvRecentPresetRecords(props.presetStorageKey, [])
  }
}

const isPinnedPreset = (label: string) => pinnedPresetLabels.value.includes(label)

const togglePinnedPreset = (label: string) => {
  if (!props.presetStorageKey) {
    return
  }
  if (isPinnedPreset(label)) {
    pinnedPresetLabels.value = pinnedPresetLabels.value.filter((item) => item !== label)
  } else {
    pinnedPresetLabels.value = [label, ...pinnedPresetLabels.value.filter((item) => item !== label)]
  }
  saveCsvPinnedPresetLabels(props.presetStorageKey, pinnedPresetLabels.value)
}

const parsePresetText = (raw: string): { label: string; group?: string } => {
  const text = raw.trim()
  const splitIndex = text.indexOf('/')
  if (splitIndex <= 0 || splitIndex >= text.length - 1) {
    return { label: text }
  }
  const group = text.slice(0, splitIndex).trim()
  const label = text.slice(splitIndex + 1).trim()
  if (!group || !label) {
    return { label: text }
  }
  return { label, group }
}

const inferGroupByLabel = (label: string): string | undefined => {
  if (label.includes('默认')) return '默认'
  if (label.includes('运维')) return '运维'
  if (label.includes('管理')) return '管理'
  return undefined
}

const resolveImportConflictStrategy = async (
  conflictCount: number
): Promise<'overwrite' | 'skip' | 'rename'> => {
  if (!conflictCount) {
    return 'overwrite'
  }
  return openImportStrategy(conflictCount)
}

interface ImportPreview {
  added: string[]
  overwritten: string[]
  renamed: string[]
  skipped: string[]
}

type ImportPreviewTab = keyof ImportPreview
type ImportPreviewDecision = 'back' | 'confirm'

const importPreviewDetailVisible = ref(false)
const importPreviewDetail = ref<ImportPreview | null>(null)
const importPreviewDetailStrategy = ref<ImportPreviewStrategy>('overwrite')
const importPreviewDetailTab = ref<ImportPreviewTab>('added')
let importPreviewDetailResolver: ((decision: ImportPreviewDecision) => void) | null = null

const importPreviewSections = computed(() => {
  const preview = importPreviewDetail.value
  return [
    { key: 'added' as const, label: '新增', items: preview?.added || [] },
    { key: 'overwritten' as const, label: '覆盖', items: preview?.overwritten || [] },
    { key: 'renamed' as const, label: '重命名', items: preview?.renamed || [] },
    { key: 'skipped' as const, label: '跳过', items: preview?.skipped || [] }
  ]
})

const buildImportPreview = (
  currentPresets: CsvColumnPreset[],
  importedPresets: CsvColumnPreset[],
  strategy: ImportPreviewStrategy
): ImportPreview => {
  const currentLabels = new Set(currentPresets.map((item) => item.label))
  const added: string[] = []
  const overwritten: string[] = []
  const renamed: string[] = []
  const skipped: string[] = []
  importedPresets.forEach((item) => {
    if (!currentLabels.has(item.label)) {
      added.push(item.label)
      currentLabels.add(item.label)
      return
    }
    if (strategy === 'overwrite') {
      overwritten.push(item.label)
      return
    }
    if (strategy === 'skip') {
      skipped.push(item.label)
      return
    }
    const renamedLabel = `${item.label}(导入)`
    renamed.push(renamedLabel)
    currentLabels.add(renamedLabel)
  })
  return { added, overwritten, renamed, skipped }
}

const confirmImportPreview = async (
  preview: ImportPreview,
  strategy: ImportPreviewStrategy
): Promise<boolean> => {
  const decision = await openImportPreviewDetail(preview, strategy)
  return decision === 'confirm'
}

const mergeImportedPresets = (
  currentPresets: CsvColumnPreset[],
  importedPresets: CsvColumnPreset[],
  strategy: ImportPreviewStrategy
): CsvColumnPreset[] => {
  const result = [...currentPresets]
  importedPresets.forEach((item) => {
    const existsIndex = result.findIndex((preset) => preset.label === item.label)
    if (existsIndex === -1) {
      result.push(item)
      return
    }
    if (strategy === 'skip') {
      return
    }
    if (strategy === 'overwrite') {
      result[existsIndex] = item
      return
    }
    const renamed = renameImportedPreset(item, result)
    result.push(renamed)
  })
  return result
}

const renameImportedPreset = (item: CsvColumnPreset, existing: CsvColumnPreset[]): CsvColumnPreset => {
  let suffix = 1
  let label = `${item.label}(导入${suffix})`
  while (existing.some((preset) => preset.label === label)) {
    suffix += 1
    label = `${item.label}(导入${suffix})`
  }
  return { ...item, label, lastModifiedAt: new Date().toISOString() }
}

const resolveStrategyLabel = (strategy: ImportPreviewStrategy) => {
  if (strategy === 'skip') return '跳过'
  if (strategy === 'rename') return '重命名'
  return '覆盖'
}

const openImportPreviewDetail = (preview: ImportPreview, strategy: ImportPreviewStrategy) => {
  importPreviewDetail.value = preview
  importPreviewDetailStrategy.value = strategy
  importPreviewDetailTab.value = resolveDefaultImportPreviewTab(preview)
  importPreviewDetailVisible.value = true
  return new Promise<ImportPreviewDecision>((resolve) => {
    importPreviewDetailResolver = resolve
  })
}

const resolveDefaultImportPreviewTab = (preview: ImportPreview): ImportPreviewTab => {
  if (preview.added.length) return 'added'
  if (preview.overwritten.length) return 'overwritten'
  if (preview.renamed.length) return 'renamed'
  return 'skipped'
}

const closeImportPreviewDetail = (decision: ImportPreviewDecision) => {
  importPreviewDetailVisible.value = false
  importPreviewDetail.value = null
  if (importPreviewDetailResolver) {
    importPreviewDetailResolver(decision)
    importPreviewDetailResolver = null
  }
}

const handleImportPreviewDetailClose = () => closeImportPreviewDetail('back')

const getRecentUsedAt = (label: string): string | undefined =>
  recentPresetRecords.value.find((item) => item.label === label)?.usedAt

const formatPresetTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN')
}

const touchRecentPreset = (label: string) => {
  if (!props.presetStorageKey) {
    return
  }
  const usedAt = new Date().toISOString()
  const next = [
    { label, usedAt },
    ...recentPresetRecords.value.filter((item) => item.label !== label)
  ]
  recentPresetRecords.value = next.slice(0, 10)
  saveCsvRecentPresetRecords(props.presetStorageKey, recentPresetRecords.value)
}
</script>

<style scoped>
.csv-column-setting {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 68vh;
  overflow: auto;
  padding: 4px 2px;
}

.csv-column-setting__form {
  padding: 0;
}

.csv-column-setting__hint {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.csv-column-setting__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.csv-column-setting__search {
  flex: 1;
}

.csv-column-setting__presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  padding: 6px 8px;
  background: var(--el-fill-color-light);
  border-radius: calc(var(--radius-md) + 2px);
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

.csv-column-setting__groups {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.csv-column-setting__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid var(--iot-border-color, #dcdfe6);
  border-radius: calc(var(--radius-md) + 2px);
  background: var(--el-bg-color-page);
}

.csv-column-setting__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.csv-import-preview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.csv-import-preview__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-radius: calc(var(--radius-md) + 2px);
}

.csv-import-preview__tabs {
  margin-top: 4px;
}

.csv-import-preview__empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 10px 0;
}

.csv-import-preview__list {
  margin: 0;
  padding: 0 0 0 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.csv-column-setting__strategy-grid {
  display: grid;
  gap: 12px;
}

.csv-column-setting__strategy-card {
  width: 100%;
  padding: 16px 18px;
  text-align: left;
  border: 1px solid rgba(42, 63, 95, 0.12);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.84);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.csv-column-setting__strategy-card:hover {
  border-color: rgba(64, 158, 255, 0.45);
  box-shadow: var(--shadow-surface-soft-md);
}

.csv-column-setting__strategy-card.is-active {
  border-color: #409eff;
  box-shadow: var(--shadow-surface-brand-md);
  transform: translateY(-1px);
}

.csv-column-setting__strategy-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text-primary, #243448);
  font-size: 15px;
}

.csv-column-setting__strategy-card p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}
</style>
