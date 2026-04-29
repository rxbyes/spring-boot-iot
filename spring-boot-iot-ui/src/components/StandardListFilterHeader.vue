<template>
  <div
    class="standard-list-filter-header standard-list-filter-header--minimal standard-list-filter-header--workbench-foundation"
  >
    <el-form :model="model" class="standard-list-filter-header__form" @submit.prevent>
      <div ref="primaryRowRef" class="standard-list-filter-header__row" :style="layoutVars">
        <slot name="primary" />
      </div>

      <el-collapse-transition>
        <div v-if="hasAdvancedSlot" v-show="showAdvanced" class="standard-list-filter-header__advanced">
          <div class="standard-list-filter-header__advanced-grid" :style="layoutVars">
            <slot name="advanced" />
          </div>
        </div>
      </el-collapse-transition>

      <div
        class="standard-list-filter-header__actions-row standard-list-filter-header__actions-row--minimal standard-list-filter-header__actions-row--workbench"
      >
        <StandardActionGroup gap="sm" class="standard-list-filter-header__actions">
          <slot name="actions" />
        </StandardActionGroup>
        <StandardActionLink
          v-if="showFilterToggle"
          class="standard-list-filter-header__toggle"
          @click="handleToggleFilters"
        >
          {{ filtersExpanded ? collapseText : expandText }}
        </StandardActionLink>
        <span v-if="advancedHint" class="standard-list-filter-header__hint">{{ advancedHint }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUpdated, ref, useSlots, watch } from 'vue'
import StandardActionGroup from '@/components/StandardActionGroup.vue'

interface Props {
  model: Record<string, unknown>
  showAdvanced?: boolean
  showAdvancedToggle?: boolean
  advancedHint?: string
  expandText?: string
  collapseText?: string
  primaryColumns?: string
  advancedColumns?: string
  primaryVisibleCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  showAdvanced: false,
  showAdvancedToggle: false,
  advancedHint: '',
  expandText: '展开全部筛选项',
  collapseText: '收起筛选项',
  primaryColumns: 'repeat(4, minmax(220px, 1fr))',
  advancedColumns: 'repeat(4, minmax(160px, 1fr))',
  primaryVisibleCount: 4
})

const emit = defineEmits<{
  (e: 'toggle-advanced'): void
}>()

const slots = useSlots()
const primaryRowRef = ref<HTMLElement | null>(null)
const primaryFieldCount = ref(0)
const primaryExpanded = ref(false)

const hasAdvancedSlot = computed(() => Boolean(slots.advanced))
const hasAdvancedToggle = computed(() => props.showAdvancedToggle && hasAdvancedSlot.value)
const hasPrimaryOverflow = computed(() => primaryFieldCount.value > props.primaryVisibleCount)
const filtersExpanded = computed(() => primaryExpanded.value || props.showAdvanced)
const showFilterToggle = computed(() => hasPrimaryOverflow.value || hasAdvancedToggle.value)

const layoutVars = computed(() => ({
  '--slfh-primary-columns': props.primaryColumns,
  '--slfh-advanced-columns': props.advancedColumns
}))

function resolvePrimaryFields() {
  const container = primaryRowRef.value
  if (!container) {
    primaryFieldCount.value = 0
    return []
  }

  return Array.from(container.querySelectorAll<HTMLElement>('.el-form-item')).filter(
    (field) => field.parentElement === container
  )
}

function syncPrimaryFieldVisibility() {
  const fields = resolvePrimaryFields()
  primaryFieldCount.value = fields.length

  fields.forEach((field, index) => {
    const shouldHide = hasPrimaryOverflow.value && !filtersExpanded.value && index >= props.primaryVisibleCount
    field.classList.toggle('standard-list-filter-header__primary-field--hidden', shouldHide)
  })
}

function handleToggleFilters() {
  const nextExpanded = !filtersExpanded.value
  if (hasPrimaryOverflow.value) {
    primaryExpanded.value = nextExpanded
  }
  if (hasAdvancedToggle.value) {
    emit('toggle-advanced')
  }
}

watch(
  () => [props.showAdvanced, props.primaryVisibleCount, primaryExpanded.value],
  async () => {
    await nextTick()
    syncPrimaryFieldVisibility()
  }
)

onMounted(async () => {
  await nextTick()
  syncPrimaryFieldVisibility()
})

onUpdated(() => {
  syncPrimaryFieldVisibility()
})
</script>

<style scoped>
.standard-list-filter-header {
  display: grid;
}

.standard-list-filter-header--minimal {
  gap: 0.1rem;
}

.standard-list-filter-header--workbench-foundation {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.standard-list-filter-header__form {
  display: grid;
}

.standard-list-filter-header__row {
  display: grid;
  grid-template-columns: var(--slfh-primary-columns);
  gap: var(--ops-filter-grid-gap, 8px);
  align-items: end;
}

.standard-list-filter-header__row :deep(.el-form-item) {
  margin-bottom: 0;
  min-width: 0;
}

.standard-list-filter-header__row :deep(.standard-list-filter-header__primary-field--hidden) {
  display: none;
}

.standard-list-filter-header__advanced {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft);
}

.standard-list-filter-header__advanced-grid {
  display: grid;
  grid-template-columns: var(--slfh-advanced-columns);
  gap: var(--ops-filter-grid-gap, 8px);
}

.standard-list-filter-header__advanced-grid :deep(.el-form-item) {
  margin-bottom: 0;
  min-width: 0;
}

.standard-list-filter-header__actions-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-top: 8px;
}

.standard-list-filter-header__actions-row--minimal {
  margin-top: 12px;
  gap: 8px var(--ops-filter-actions-gap, 14px);
}

.standard-list-filter-header__actions-row--workbench {
  margin-top: 10px;
}

.standard-list-filter-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.standard-list-filter-header__toggle {
  padding-inline: 0.08rem;
  font-weight: 600;
  font-size: var(--type-label-size);
  letter-spacing: calc(var(--font-letter-spacing-wide) * 0.55);
}

.standard-list-filter-header__hint {
  color: var(--text-caption);
  font-size: var(--type-caption-size);
  line-height: var(--type-caption-line-height);
}

@media (max-width: 1240px) {
  .standard-list-filter-header__row {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
    gap: 12px;
  }

  .standard-list-filter-header__advanced-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 900px) {
  .standard-list-filter-header__row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .standard-list-filter-header__advanced-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .standard-list-filter-header__actions-row {
    align-items: flex-start;
  }

  .standard-list-filter-header__hint {
    width: 100%;
  }
}
</style>
