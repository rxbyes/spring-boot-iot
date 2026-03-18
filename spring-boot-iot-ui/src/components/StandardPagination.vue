<template>
  <el-pagination
    class="standard-pagination"
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    :page-sizes="pageSizes"
    :layout="layout"
    :disabled="disabled"
    background
    @update:current-page="handleUpdateCurrentPage"
    @update:page-size="handleUpdatePageSize"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
  />
</template>

<script setup lang="ts">
interface StandardPaginationProps {
  currentPage: number
  pageSize: number
  total: number
  pageSizes?: number[]
  layout?: string
  disabled?: boolean
}

withDefaults(defineProps<StandardPaginationProps>(), {
  pageSizes: () => [10, 20, 50, 100],
  layout: 'total, sizes, prev, pager, next, jumper',
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:current-page', value: number): void
  (e: 'update:page-size', value: number): void
  (e: 'current-change', value: number): void
  (e: 'size-change', value: number): void
}>()

function handleUpdateCurrentPage(value: number) {
  emit('update:current-page', value)
}

function handleUpdatePageSize(value: number) {
  emit('update:page-size', value)
}

function handleCurrentChange(value: number) {
  emit('current-change', value)
}

function handleSizeChange(value: number) {
  emit('size-change', value)
}
</script>
