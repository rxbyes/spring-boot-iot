<template>
  <div class="standard-list-filter-header">
    <el-form :model="model" class="standard-list-filter-header__form" @submit.prevent>
      <div class="standard-list-filter-header__row" :style="layoutVars">
        <slot name="primary" />
      </div>

      <el-collapse-transition>
        <div v-if="hasAdvancedSlot" v-show="showAdvanced" class="standard-list-filter-header__advanced">
          <div class="standard-list-filter-header__advanced-grid" :style="layoutVars">
            <slot name="advanced" />
          </div>
        </div>
      </el-collapse-transition>

      <div class="standard-list-filter-header__actions-row">
        <StandardActionGroup gap="sm" class="standard-list-filter-header__actions">
          <slot name="actions" />
        </StandardActionGroup>
        <el-button
          v-if="showAdvancedToggle && hasAdvancedSlot"
          link
          class="standard-list-filter-header__toggle"
          @click="emit('toggle-advanced')"
        >
          {{ showAdvanced ? collapseText : expandText }}
        </el-button>
        <span v-if="advancedHint" class="standard-list-filter-header__hint">{{ advancedHint }}</span>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
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
}

const props = withDefaults(defineProps<Props>(), {
  showAdvanced: false,
  showAdvancedToggle: false,
  advancedHint: '',
  expandText: '展开更多条件',
  collapseText: '收起更多条件',
  primaryColumns: 'repeat(3, minmax(220px, 1fr))',
  advancedColumns: 'repeat(4, minmax(160px, 1fr))'
})

const emit = defineEmits<{
  (e: 'toggle-advanced'): void
}>()

const slots = useSlots()

const hasAdvancedSlot = computed(() => Boolean(slots.advanced))

const layoutVars = computed(() => ({
  '--slfh-primary-columns': props.primaryColumns,
  '--slfh-advanced-columns': props.advancedColumns
}))
</script>

<style scoped>
.standard-list-filter-header {
  display: grid;
}

.standard-list-filter-header__form {
  display: grid;
}

.standard-list-filter-header__row {
  display: grid;
  grid-template-columns: var(--slfh-primary-columns);
  gap: 12px 14px;
  align-items: end;
}

.standard-list-filter-header__row :deep(.el-form-item) {
  margin-bottom: 0;
  min-width: 0;
}

.standard-list-filter-header__advanced {
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px dashed color-mix(in srgb, var(--brand) 18%, transparent);
}

.standard-list-filter-header__advanced-grid {
  display: grid;
  grid-template-columns: var(--slfh-advanced-columns);
  gap: 12px 14px;
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
}

.standard-list-filter-header__hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
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
