<template>
  <div :class="classes">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    variant?: 'table' | 'card' | 'editor'
    gap?: 'compact' | 'comfortable' | 'wide'
    wrap?: boolean
  }>(),
  {
    variant: 'table',
    gap: 'compact',
    wrap: false
  }
)

const shouldWrap = computed(() => props.wrap || props.variant === 'editor')

const classes = computed(() => [
  'standard-row-actions',
  `standard-row-actions--variant-${props.variant}`,
  `standard-row-actions--${props.gap}`,
  {
    'standard-row-actions--wrap': shouldWrap.value
  }
])
</script>
