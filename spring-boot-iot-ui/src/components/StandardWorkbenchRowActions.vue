<template>
  <StandardRowActions
    :variant="variant"
    :gap="resolvedGap"
    :distribution="resolvedDistribution"
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
import { getActivePinia } from 'pinia'
import StandardActionLink from '@/components/StandardActionLink.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardRowActions from '@/components/StandardRowActions.vue'
import { usePermissionStore } from '@/stores/permission'
import { splitWorkbenchRowActions, WORKBENCH_TABLE_ACTION_GAP } from '@/utils/adaptiveActionColumn'

defineOptions({
  name: 'StandardWorkbenchRowActions'
})

type WorkbenchRowCommand = string | number | object
type WorkbenchActionPermission = string | string[]

interface WorkbenchDirectActionItem {
  key?: string
  command: WorkbenchRowCommand
  label: string
  disabled?: boolean
  title?: string
  dataTestid?: string
  permission?: WorkbenchActionPermission
}

interface WorkbenchMenuActionItem {
  key?: string
  command: WorkbenchRowCommand
  label: string
  disabled?: boolean
  divided?: boolean
  permission?: WorkbenchActionPermission
}

const props = withDefaults(
  defineProps<{
    variant?: 'table' | 'card' | 'editor'
    gap?: 'compact' | 'comfortable' | 'wide'
    distribution?: 'start' | 'between' | 'center'
    directItems?: WorkbenchDirectActionItem[]
    menuItems?: WorkbenchMenuActionItem[]
    menuLabel?: string
    maxDirectItems?: number
  }>(),
  {
    variant: 'table',
    gap: undefined,
    distribution: undefined,
    directItems: () => [],
    menuItems: () => [],
    menuLabel: '更多',
    maxDirectItems: 3
  }
)

const emit = defineEmits<{
  (event: 'command', command: WorkbenchRowCommand): void
}>()

let permissionStore: ReturnType<typeof usePermissionStore> | null = null

function getPermissionStore() {
  if (!getActivePinia()) {
    return null
  }
  permissionStore = permissionStore ?? usePermissionStore()
  return permissionStore
}

function hasActionPermission(permission?: WorkbenchActionPermission) {
  if (!permission) {
    return true
  }
  const store = getPermissionStore()
  if (!store) {
    return true
  }
  if (Array.isArray(permission)) {
    return permission.some((code) => store.hasPermission(code))
  }
  return store.hasPermission(permission)
}

const resolvedGap = computed(() => {
  if (props.variant === 'table') {
    return WORKBENCH_TABLE_ACTION_GAP
  }
  return props.gap ?? 'comfortable'
})
const resolvedActions = computed(() =>
  splitWorkbenchRowActions({
    directItems: props.directItems.filter((item) => hasActionPermission(item.permission)),
    menuItems: props.menuItems.filter((item) => hasActionPermission(item.permission)),
    maxDirectItems: props.maxDirectItems
  })
)
const resolvedDirectItems = computed(() => resolvedActions.value.directItems)
const resolvedMenuItems = computed(() => resolvedActions.value.menuItems)
const hasMenuItems = computed(() => resolvedMenuItems.value.length > 0)
const resolvedDistribution = computed<'start' | 'between' | 'center'>(() => {
  if (props.distribution) {
    return props.distribution
  }
  return 'start'
})
</script>
