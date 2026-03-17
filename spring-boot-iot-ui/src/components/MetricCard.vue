<template>
  <article class="metric-card">
    <p class="metric-card__label">{{ label }}</p>
    <div class="metric-card__value-row">
      <strong class="metric-card__value">{{ value }}</strong>
      <SignalBadge v-if="badge" :label="badge.label" :tone="badge.tone" />
    </div>
  </article>
</template>

<script setup lang="ts">
import SignalBadge from './SignalBadge.vue';

defineProps<{
  label: string;
  value: string;
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
  padding: 1.05rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 251, 255, 0.97)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.08), transparent 50%);
  box-shadow: 0 4px 12px rgba(31, 35, 41, 0.05);
  min-height: 10rem;
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: auto 1.2rem 1rem auto;
  width: 3.2rem;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--brand-bright));
  opacity: 0.7;
}

.metric-card__label {
  margin: 0;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.72rem;
}

.metric-card__value-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 0.95rem;
}

.metric-card__value {
  font-family: var(--font-display);
  font-size: clamp(2rem, 2.8vw, 3rem);
  line-height: 1;
}

</style>
