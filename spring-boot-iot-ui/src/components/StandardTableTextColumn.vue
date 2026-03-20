<template>
  <el-table-column
    v-bind="attrs"
    :prop="prop"
    :label="label"
    :min-width="minWidth"
    :width="width"
    :fixed="fixed"
    :align="align"
    :header-align="headerAlign"
    :show-overflow-tooltip="resolvedShowOverflowTooltip"
  >
    <template v-if="$slots.default" #default="scope">
      <slot v-bind="scope" />
    </template>

    <template v-if="$slots.header" #header="scope">
      <slot name="header" v-bind="scope" />
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'

defineOptions({
  inheritAttrs: false
})

const props = withDefaults(
  defineProps<{
    prop?: string
    label?: string
    minWidth?: string | number
    width?: string | number
    fixed?: boolean | 'left' | 'right'
    align?: 'left' | 'center' | 'right'
    headerAlign?: 'left' | 'center' | 'right'
    showOverflowTooltip?: boolean | Record<string, unknown>
  }>(),
  {
    prop: undefined,
    label: undefined,
    minWidth: undefined,
    width: undefined,
    fixed: undefined,
    align: 'left',
    headerAlign: undefined,
    showOverflowTooltip: true
  }
)

const attrs = useAttrs()

const resolvedShowOverflowTooltip = computed(() => {
  if (props.showOverflowTooltip === false) {
    return false
  }

  if (props.showOverflowTooltip === true) {
    return {
      effect: 'light',
      placement: 'top-start'
    }
  }

  return {
    effect: 'light',
    placement: 'top-start',
    ...props.showOverflowTooltip
  }
})
</script>
