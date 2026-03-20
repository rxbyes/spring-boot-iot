<template>
  <div class="info-grid standard-info-grid" :style="gridStyle">
    <article
      v-for="item in normalizedItems"
      :key="item.key"
      class="info-chip standard-info-grid__item"
    >
      <span class="standard-info-grid__label">{{ item.label }}</span>
      <strong
        class="standard-info-grid__value"
        :class="{ 'standard-info-grid__value--multiline': item.multiline }"
        :title="item.multiline ? undefined : item.title"
      >
        {{ item.value }}
      </strong>
    </article>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface StandardInfoGridItem {
  key?: string | number
  label: string
  value?: string | number | boolean | null
  fallback?: string
  multiline?: boolean
  title?: string
}

const props = withDefaults(
  defineProps<{
    items: StandardInfoGridItem[]
    columns?: number
  }>(),
  {
    columns: 2
  }
)

const normalizedItems = computed(() =>
  props.items.map((item, index) => {
    const resolvedValue =
      item.value === null || item.value === undefined || item.value === ''
        ? item.fallback ?? '--'
        : String(item.value)

    return {
      key: item.key ?? `${item.label}-${index}`,
      label: item.label,
      value: resolvedValue,
      multiline: item.multiline ?? false,
      title: item.title ?? resolvedValue
    }
  })
)

const gridStyle = computed(() => ({
  '--standard-info-grid-columns': String(Math.max(props.columns, 1))
}))
</script>

<style scoped>
.standard-info-grid {
  grid-template-columns: repeat(var(--standard-info-grid-columns, 2), minmax(0, 1fr));
}

.standard-info-grid__item {
  min-width: 0;
}

.standard-info-grid__label {
  display: block;
}

.standard-info-grid__value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.standard-info-grid__value--multiline {
  overflow: visible;
  white-space: normal;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .standard-info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
