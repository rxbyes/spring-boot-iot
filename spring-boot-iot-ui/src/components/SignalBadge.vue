<template>
  <span :class="['signal-badge', `signal-badge--${tone || 'muted'}`]">
    <span class="signal-badge__dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
defineProps<{
  label: string;
  tone?: 'success' | 'warning' | 'danger' | 'muted' | 'brand';
}>();
</script>

<style scoped>
.signal-badge {
  align-items: center;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-pill);
  display: inline-flex;
  gap: 0.45rem;
  letter-spacing: 0.04em;
  padding: 0.35rem 0.7rem;
  text-transform: uppercase;
  font-size: 0.72rem;
  color: var(--text-secondary);
  background: #ffffff;
}

.signal-badge__dot {
  border-radius: 50%;
  display: inline-flex;
  height: 0.5rem;
  width: 0.5rem;
  background: currentColor;
  box-shadow: 0 0 0 2px color-mix(in srgb, currentColor 20%, transparent);
}

.signal-badge--success {
  color: var(--success);
}

.signal-badge--warning {
  color: var(--warning);
}

.signal-badge--danger {
  color: var(--danger);
}

.signal-badge--brand {
  color: var(--brand-bright);
}

.signal-badge--muted {
  color: var(--text-secondary);
}
</style>
