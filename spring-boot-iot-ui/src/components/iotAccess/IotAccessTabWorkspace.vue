<template>
  <section class="iot-access-tab-workspace">
    <nav class="iot-access-tab-workspace__tabs" aria-label="业务视图切换">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="iot-access-tab-workspace__tab"
        :class="{ 'iot-access-tab-workspace__tab--active': item.key === activeKey }"
        :disabled="Boolean(item.disabled)"
        :aria-current="item.key === activeKey ? 'page' : undefined"
        v-bind="resolveButtonAttrs(item)"
        @click="handleTabChange(item.key)"
      >
        {{ item.label }}
      </button>
    </nav>

    <div class="iot-access-tab-workspace__panel">
      <slot :active-key="activeKey" :active-item="activeItem" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

export interface IotAccessTabWorkspaceItem {
  key: string
  label: string
  disabled?: boolean
  buttonAttrs?: Record<string, string | number | boolean | undefined>
  activeButtonAttrs?: Record<string, string | number | boolean | undefined>
}

const props = withDefaults(
  defineProps<{
    items: IotAccessTabWorkspaceItem[]
    modelValue?: string
    defaultKey?: string
    queryKey?: string
    syncQuery?: boolean
  }>(),
  {
    modelValue: '',
    defaultKey: '',
    queryKey: 'tab',
    syncQuery: true
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'change', value: string): void
}>()

const route = useRoute()
const router = useRouter()

function isValidKey(value: unknown) {
  return typeof value === 'string' && props.items.some((item) => item.key === value)
}

function resolveActiveKey(...candidates: unknown[]) {
  const matched = candidates.find(isValidKey)
  if (typeof matched === 'string') {
    return matched
  }
  return props.items[0]?.key || ''
}

const activeKey = ref(
  resolveActiveKey(route.query?.[props.queryKey], props.modelValue, props.defaultKey)
)

watch(
  () => [route.query?.[props.queryKey], props.modelValue, props.defaultKey, props.items] as const,
  ([routeValue, modelValue, defaultKey]) => {
    activeKey.value = resolveActiveKey(routeValue, modelValue, defaultKey)
  },
  { deep: true }
)

const activeItem = computed(() => props.items.find((item) => item.key === activeKey.value) || null)

function resolveButtonAttrs(item: IotAccessTabWorkspaceItem) {
  return item.key === activeKey.value
    ? { ...(item.buttonAttrs || {}), ...(item.activeButtonAttrs || {}) }
    : { ...(item.buttonAttrs || {}) }
}

async function handleTabChange(nextKey: string) {
  if (!isValidKey(nextKey) || nextKey === activeKey.value) {
    return
  }

  activeKey.value = nextKey
  emit('update:modelValue', nextKey)
  emit('change', nextKey)

  if (!props.syncQuery) {
    return
  }

  const nextQuery = { ...(route.query || {}) } as Record<string, unknown>
  if (props.queryKey === 'mode' && nextKey === props.defaultKey) {
    delete nextQuery[props.queryKey]
  } else {
    nextQuery[props.queryKey] = nextKey
  }

  await router.replace({
    query: nextQuery
  })
}
</script>

<style scoped>
.iot-access-tab-workspace {
  display: grid;
  gap: 0.82rem;
}

.iot-access-tab-workspace__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  border-bottom: 1px solid var(--line-panel);
}

.iot-access-tab-workspace__tab {
  position: relative;
  min-height: 2.5rem;
  padding: 0.68rem 0.95rem;
  border: none;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  transition:
    color var(--transition-fast),
    border-color var(--transition-fast);
}

.iot-access-tab-workspace__tab:hover {
  color: var(--brand);
}

.iot-access-tab-workspace__tab--active {
  color: var(--brand);
  border-bottom-color: var(--brand);
}

.iot-access-tab-workspace__panel {
  min-width: 0;
}
</style>
