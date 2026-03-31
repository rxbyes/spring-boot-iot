<template>
  <StandardRowActions
    :variant="variant"
    :gap="resolvedGap"
    :distribution="distribution"
    class="standard-workbench-row-actions"
  >
    <StandardActionLink
      v-for="(item, index) in resolvedDirectItems"
      :key="item.key ?? `${String(item.command)}-${index}`"
      :disabled="item.disabled"
      :title="item.title"
      :data-testid="item.dataTestid"
      @click="emit('command', item.command)"
    >
      {{ item.label }}
    </StandardActionLink>

    <StandardActionMenu
      v-if="hasMenuItems"
      :label="menuLabel"
      :items="resolvedMenuItems"
      @command="emit('command', $event)"
    />
  </StandardRowActions>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StandardActionLink from '@/components/StandardActionLink.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardRowActions from '@/components/StandardRowActions.vue'
import { splitWorkbenchRowActions } from '@/utils/adaptiveActionColumn'

defineOptions({
  name: 'StandardWorkbenchRowActions'
})

type WorkbenchRowCommand = string | number | object

interface WorkbenchDirectActionItem {
  key?: string
  command: WorkbenchRowCommand
  label: string
  disabled?: boolean
  title?: string
  dataTestid?: string
}

interface WorkbenchMenuActionItem {
  key?: string
  command: WorkbenchRowCommand
  label: string
  disabled?: boolean
  divided?: boolean
}

const props = withDefaults(
  defineProps<{
    variant?: 'table' | 'card' | 'editor'
    gap?: 'compact' | 'comfortable' | 'wide'
    distribution?: 'start' | 'between'
    directItems?: WorkbenchDirectActionItem[]
    menuItems?: WorkbenchMenuActionItem[]
    menuLabel?: string
    maxDirectItems?: number
  }>(),
  {
    variant: 'table',
    gap: undefined,
    distribution: 'start',
    directItems: () => [],
    menuItems: () => [],
    menuLabel: '更多',
    maxDirectItems: 2
  }
)

const emit = defineEmits<{
  (event: 'command', command: WorkbenchRowCommand): void
}>()

const resolvedGap = computed(() => props.gap ?? (props.variant === 'table' ? 'compact' : 'comfortable'))
const resolvedActions = computed(() =>
  splitWorkbenchRowActions({
    directItems: props.directItems,
    menuItems: props.menuItems,
    maxDirectItems: props.maxDirectItems
  })
)
const resolvedDirectItems = computed(() => resolvedActions.value.directItems)
const resolvedMenuItems = computed(() => resolvedActions.value.menuItems)
const hasMenuItems = computed(() => resolvedMenuItems.value.length > 0)
</script>
