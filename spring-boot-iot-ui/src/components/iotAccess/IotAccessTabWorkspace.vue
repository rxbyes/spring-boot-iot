<template>
  <section class="iot-access-tab-workspace">
    <nav class="iot-access-tab-workspace__tabs" aria-label="工作区切换">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="iot-access-tab-workspace__tab"
        :class="{ 'iot-access-tab-workspace__tab--active': item.key === activeKey }"
        :disabled="Boolean(item.disabled)"
        :aria-current="item.key === activeKey ? 'page' : undefined"
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

const activeItem = computed(
  () => props.items.find((item) => item.key === activeKey.value) || null
)

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

  await router.replace({
    query: {
      ...(route.query || {}),
      [props.queryKey]: nextKey
    }
  })
}
</script>

<style scoped>
.iot-access-tab-workspace {
  display: grid;
  gap: 0.9rem;
}

.iot-access-tab-workspace__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.iot-access-tab-workspace__tab {
  border: 1px solid var(--shell-border);
  border-radius: var(--radius-md);
  background: white;
  color: var(--text-secondary);
  min-height: 2.4rem;
  padding: 0.55rem 1rem;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 160ms ease;
}

.iot-access-tab-workspace__tab:hover {
  border-color: color-mix(in srgb, var(--brand) 20%, white);
  color: var(--brand);
}

.iot-access-tab-workspace__tab--active {
  border-color: color-mix(in srgb, var(--brand) 30%, white);
  background: color-mix(in srgb, var(--brand) 6%, white);
  color: var(--brand);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--brand) 10%, white);
}

.iot-access-tab-workspace__panel {
  min-width: 0;
}
</style>
