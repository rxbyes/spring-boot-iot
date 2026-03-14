<template>
  <article class="metric-card">
    <p class="metric-card__label">{{ label }}</p>
    <div class="metric-card__value-row">
      <strong class="metric-card__value">{{ value }}</strong>
      <SignalBadge v-if="badge" :label="badge.label" :tone="badge.tone" />
    </div>
    <p class="metric-card__hint">{{ hint }}</p>
  </article>
</template>

<script setup lang="ts">
import SignalBadge from './SignalBadge.vue';

defineProps<{
  label: string;
  value: string;
  hint: string;
  badge?: {
    label: string;
    tone: 'success' | 'warning' | 'danger' | 'muted' | 'brand';
  };
}>();
</script>

<style scoped>
.metric-card {
  position: relative;
  overflow: hidden;
  padding: 1.2rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(160deg, rgba(10, 18, 38, 0.94), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(44, 227, 255, 0.12), transparent 50%);
  min-height: 11rem;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: auto 1.2rem 1rem auto;
  width: 4rem;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--brand-bright));
  opacity: 0.9;
}

.metric-card__label {
  margin: 0;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 0.72rem;
}

.metric-card__value-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1.1rem;
}

.metric-card__value {
  font-family: var(--font-display);
  font-size: clamp(2rem, 2.8vw, 3rem);
  line-height: 1;
}

.metric-card__hint {
  margin: 1.2rem 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
