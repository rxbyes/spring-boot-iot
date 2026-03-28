<template>
  <div
    class="standard-table-toolbar standard-table-toolbar--minimal table-action-bar"
    :class="{
      'standard-table-toolbar--compact': compact
    }"
  >
    <div v-if="hasLeft" class="table-action-bar__left standard-table-toolbar__meta-rail">
      <slot name="left">
        <span
          v-for="item in normalizedMetaItems"
          :key="item.key"
          class="table-action-bar__meta"
        >
          {{ item.label }}
        </span>
      </slot>
    </div>

    <div v-if="hasRight" class="table-action-bar__right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

type MetaItem = string | number | { key?: string | number; label: string | number }

const props = withDefaults(
  defineProps<{
    metaItems?: MetaItem[]
    compact?: boolean
  }>(),
  {
    metaItems: () => [],
    compact: false
  }
)

const slots = useSlots()

const normalizedMetaItems = computed(() =>
  props.metaItems
    .filter(item => item !== null && item !== undefined && String(typeof item === 'object' ? item.label : item).trim())
    .map((item, index) => {
      if (typeof item === 'object') {
        return {
          key: item.key ?? `${item.label}-${index}`,
          label: item.label
        }
      }

      return {
        key: `${item}-${index}`,
        label: item
      }
    })
)

const hasLeft = computed(() => Boolean(slots.left) || normalizedMetaItems.value.length > 0)
const hasRight = computed(() => Boolean(slots.right))
</script>

<style scoped>
.standard-table-toolbar--minimal {
  min-height: 3.2rem;
}

.standard-table-toolbar__meta-rail {
  align-items: center;
}
</style>
