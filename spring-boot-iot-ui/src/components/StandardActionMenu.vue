<template>
  <el-dropdown
    class="standard-action-menu"
    trigger="click"
    :disabled="resolvedDisabled"
    @command="emit('command', $event)"
  >
    <StandardActionLink class="standard-action-menu__trigger" :disabled="resolvedDisabled">
      {{ label }}
    </StandardActionLink>

    <template #dropdown>
      <el-dropdown-menu class="standard-action-menu__dropdown">
        <el-dropdown-item
          v-for="(item, index) in resolvedItems"
          :key="item.key ?? `${item.label}-${index}`"
          :command="item.command"
          :disabled="item.disabled"
          :divided="item.divided"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ActionMenuItem = {
  key?: string
  command: string | number | object
  label: string
  disabled?: boolean
  divided?: boolean
}

const props = withDefaults(
  defineProps<{
    items?: ActionMenuItem[]
    label?: string
    disabled?: boolean
  }>(),
  {
    items: () => [],
    label: '更多',
    disabled: false
  }
)

const emit = defineEmits<{
  (event: 'command', command: string | number | object): void
}>()

const resolvedItems = computed(() => props.items ?? [])
const resolvedDisabled = computed(() => props.disabled || resolvedItems.value.length === 0)
</script>
