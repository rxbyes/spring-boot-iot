<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="520px"
    class="sys-dialog"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <div class="csv-column-setting">
      <div v-for="(item, index) in rows" :key="item.key" class="csv-column-setting__row">
        <el-checkbox v-model="item.checked">{{ item.label }}</el-checkbox>
        <div class="csv-column-setting__actions">
          <el-button link :disabled="index === 0" @click="moveUp(index)">上移</el-button>
          <el-button link :disabled="index === rows.length - 1" @click="moveDown(index)">下移</el-button>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button class="sys-dialog__btn sys-dialog__btn--ghost" @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" class="sys-dialog__btn sys-dialog__btn--primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from '@/utils/message'
import type { CsvColumnOption } from '@/utils/csvColumns'

interface RowItem extends CsvColumnOption {
  checked: boolean
}

const props = defineProps<{
  modelValue: boolean
  title?: string
  options: CsvColumnOption[]
  selectedKeys: string[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'confirm', selectedKeys: string[]): void
}>()

const rows = ref<RowItem[]>([])

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
    }
  },
  { immediate: true, deep: true }
)

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
  const selected = rows.value.filter((item) => item.checked).map((item) => item.key)
  if (!selected.length) {
    ElMessage.warning('至少保留一个导出列')
    return
  }
  emit('confirm', selected)
  emit('update:modelValue', false)
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
