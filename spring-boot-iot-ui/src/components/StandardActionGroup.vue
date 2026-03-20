<template>
  <div :class="classes">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    fullWidth?: boolean
    marginTop?: 'none' | 'sm' | 'md'
    marginBottom?: 'none' | 'sm' | 'md'
    gap?: 'sm' | 'md'
  }>(),
  {
    fullWidth: false,
    marginTop: 'none',
    marginBottom: 'none',
    gap: 'md'
  }
);

const classes = computed(() => [
  'standard-action-group',
  `standard-action-group--gap-${props.gap}`,
  `standard-action-group--mt-${props.marginTop}`,
  `standard-action-group--mb-${props.marginBottom}`,
  {
    'standard-action-group--full-width': props.fullWidth
  }
]);
</script>

<style scoped>
.standard-action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.standard-action-group--gap-sm {
  gap: 0.6rem;
}

.standard-action-group--full-width {
  grid-column: 1 / -1;
}

.standard-action-group--mt-sm {
  margin-top: 1rem;
}

.standard-action-group--mt-md {
  margin-top: 1.25rem;
}

.standard-action-group--mb-sm {
  margin-bottom: 1rem;
}

.standard-action-group--mb-md {
  margin-bottom: 1.25rem;
}
</style>
