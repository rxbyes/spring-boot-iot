<template>
  <section
    class="iot-access-tab-workspace"
    :class="{ 'iot-access-tab-workspace--workbench': variant === 'workbench' }"
  >
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
        <span class="iot-access-tab-workspace__tab-copy">
          <span class="iot-access-tab-workspace__tab-label">{{ item.label }}</span>
          <span v-if="item.meta" class="iot-access-tab-workspace__tab-meta">{{ item.meta }}</span>
        </span>
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
  meta?: string
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
    variant?: 'default' | 'workbench'
  }>(),
  {
    modelValue: '',
    defaultKey: '',
    queryKey: 'tab',
    syncQuery: true,
    variant: 'default'
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

.iot-access-tab-workspace__tabs,
.iot-access-tab-workspace__tab,
.iot-access-tab-workspace__tab-copy {
  min-width: 0;
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
    background-color var(--transition-fast),
    color var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.iot-access-tab-workspace__tab-copy {
  display: grid;
  gap: 0.14rem;
  justify-items: start;
}

.iot-access-tab-workspace__tab-label {
  display: block;
}

.iot-access-tab-workspace__tab-meta {
  display: block;
  color: var(--text-caption);
  font-size: 11px;
  font-weight: 500;
  line-height: 1.25;
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

.iot-access-tab-workspace--workbench {
  gap: 0.96rem;
}

.iot-access-tab-workspace--workbench .iot-access-tab-workspace__tabs {
  gap: 0.62rem;
  padding: 0.34rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 80%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, white 6%);
}

.iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab {
  flex: 1 1 12rem;
  min-height: 3.35rem;
  padding: 0.76rem 0.92rem;
  border: 1px solid transparent;
  border-radius: 8px;
  text-align: left;
}

.iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab:hover {
  background: color-mix(in srgb, var(--panel-bg) 76%, white 24%);
}

.iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab--active {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border) 78%);
  border-bottom-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border) 78%);
  background: color-mix(in srgb, white 92%, var(--brand) 8%);
  box-shadow: 0 14px 28px -24px color-mix(in srgb, var(--brand) 55%, transparent);
  color: var(--text-heading);
}

.iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab--active .iot-access-tab-workspace__tab-meta {
  color: color-mix(in srgb, var(--brand) 62%, var(--text-secondary) 38%);
}

@media (max-width: 960px) {
  .iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab {
    flex-basis: calc(50% - 0.31rem);
  }
}

@media (max-width: 720px) {
  .iot-access-tab-workspace--workbench .iot-access-tab-workspace__tab {
    flex-basis: 100%;
  }
}
</style>
