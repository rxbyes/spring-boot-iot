<template>
  <div class="standard-workbench-panel ops-workbench standard-list-view">
    <PanelCard class="ops-hero-card ops-table-card standard-workbench-panel__card">
      <template #header>
        <div class="standard-workbench-panel__header">
          <div class="standard-workbench-panel__heading">
            <h2 class="standard-workbench-panel__title">{{ title }}</h2>
            <p v-if="description" class="standard-workbench-panel__caption">{{ description }}</p>
          </div>
          <div v-if="showHeaderActions" class="standard-workbench-panel__header-actions">
            <slot name="header-actions" />
          </div>
        </div>
      </template>

      <section v-if="showFilters || showFiltersExtra" class="standard-workbench-panel__filters">
        <slot name="filters" />
        <div v-if="showFiltersExtra" class="standard-workbench-panel__filters-extra">
          <slot name="filters-extra" />
        </div>
      </section>

      <section v-if="showAppliedFilters" class="standard-workbench-panel__applied-filters">
        <slot name="applied-filters" />
      </section>

      <section v-if="showNotices" class="standard-workbench-panel__notices">
        <slot name="notices" />
      </section>

      <section v-if="showToolbar" class="standard-workbench-panel__toolbar">
        <slot name="toolbar" />
      </section>

      <section v-if="showInlineState" class="standard-workbench-panel__inline-state">
        <slot name="inline-state" />
      </section>

      <div class="standard-workbench-panel__body">
        <slot />
      </div>

      <div v-if="showPagination" class="standard-workbench-panel__pagination">
        <slot name="pagination" />
      </div>
    </PanelCard>
  </div>
</template>

<script setup lang="ts">
import PanelCard from '@/components/PanelCard.vue'

withDefaults(defineProps<{
  title: string
  description?: string
  showHeaderActions?: boolean
  showFilters?: boolean
  showFiltersExtra?: boolean
  showAppliedFilters?: boolean
  showNotices?: boolean
  showToolbar?: boolean
  showInlineState?: boolean
  showPagination?: boolean
}>(), {
  showHeaderActions: false,
  showFilters: false,
  showFiltersExtra: false,
  showAppliedFilters: false,
  showNotices: false,
  showToolbar: false,
  showInlineState: false,
  showPagination: false
})
</script>

<style scoped>
.standard-workbench-panel {
  min-width: 0;
}

.standard-workbench-panel__card {
  min-width: 0;
}

.standard-workbench-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
}

.standard-workbench-panel__heading {
  min-width: 0;
}

.standard-workbench-panel__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.08rem;
}

.standard-workbench-panel__caption {
  margin: 0.35rem 0 0;
  color: var(--text-caption);
  font-size: 13px;
  line-height: 1.6;
}

.standard-workbench-panel__header-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.standard-workbench-panel__filters,
.standard-workbench-panel__applied-filters,
.standard-workbench-panel__notices,
.standard-workbench-panel__toolbar,
.standard-workbench-panel__inline-state,
.standard-workbench-panel__pagination {
  min-width: 0;
}

.standard-workbench-panel__filters,
.standard-workbench-panel__applied-filters,
.standard-workbench-panel__notices,
.standard-workbench-panel__toolbar,
.standard-workbench-panel__inline-state {
  margin-bottom: 0.72rem;
}

.standard-workbench-panel__filters-extra {
  margin-top: 0.5rem;
}

.standard-workbench-panel__body {
  min-width: 0;
}

.standard-workbench-panel__pagination {
  margin-top: 0.72rem;
}

@media (max-width: 720px) {
  .standard-workbench-panel__header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
