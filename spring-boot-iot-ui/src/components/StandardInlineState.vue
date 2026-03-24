<template>
  <div :class="['standard-inline-state', `standard-inline-state--${tone}`]" role="status">
    {{ message }}
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    message: string
    tone?: 'info' | 'error'
  }>(),
  {
    tone: 'info'
  }
)
</script>

<style scoped>
.standard-inline-state {
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--brand) 18%, transparent);
  border-radius: calc(var(--radius-md) + 2px);
  background: color-mix(in srgb, var(--brand) 8%, white);
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 13px;
  line-height: 1.6;
}

.standard-inline-state--error {
  border-color: color-mix(in srgb, var(--danger, #d84f45) 22%, transparent);
  background: color-mix(in srgb, var(--danger, #d84f45) 7%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 72%, var(--text-body));
}
</style>
