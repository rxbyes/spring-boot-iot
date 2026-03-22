<template>
  <div :class="groupClasses" role="group">
    <button
      v-for="(option, index) in options"
      :key="option.key ?? `${String(option.value)}-${index}`"
      type="button"
      class="standard-choice-group__item"
      :class="{
        'standard-choice-group__item--active': option.value === modelValue
      }"
      :disabled="option.disabled"
      @click="handleSelect(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type ChoiceValue = string | number | boolean

type ChoiceOption = {
  key?: string
  label: string
  value: ChoiceValue
  disabled?: boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: ChoiceValue
    options: ChoiceOption[]
    responsive?: boolean
  }>(),
  {
    responsive: false
  }
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: ChoiceValue): void
  (event: 'change', value: ChoiceValue): void
}>()

const groupClasses = computed(() => [
  'standard-choice-group',
  {
    'standard-choice-group--responsive': props.responsive
  }
])

function handleSelect(value: ChoiceValue) {
  if (value === props.modelValue) {
    return
  }
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
.standard-choice-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.standard-choice-group__item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 5rem;
  min-height: 2.25rem;
  padding: 0 0.95rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 0.85rem;
  font-weight: 600;
  transition:
    border-color var(--transition-base),
    background var(--transition-base),
    color var(--transition-base),
    transform var(--transition-base);
}

.standard-choice-group__item:hover {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 6%, white);
  color: var(--brand-deep);
  transform: translateY(-1px);
}

.standard-choice-group__item:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--brand) 28%, white);
  outline-offset: 2px;
}

.standard-choice-group__item:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  transform: none;
}

.standard-choice-group__item--active {
  border-color: color-mix(in srgb, var(--brand) 32%, var(--panel-border));
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--brand) 13%, white),
    color-mix(in srgb, var(--brand) 5%, white)
  );
  color: var(--brand-deep);
}

@media (max-width: 720px) {
  .standard-choice-group--responsive .standard-choice-group__item {
    width: 100%;
  }
}
</style>
