<template>
  <section class="iot-access-signal-deck">
    <PanelCard class="iot-access-signal-deck__lead">
      <p v-if="lead.eyebrow" class="iot-access-signal-deck__lead-eyebrow">{{ lead.eyebrow }}</p>
      <h3 class="iot-access-signal-deck__lead-title">{{ lead.title }}</h3>
      <p v-if="lead.description" class="iot-access-signal-deck__lead-description">{{ lead.description }}</p>
      <RouterLink
        v-if="lead.actionLabel && lead.actionTo"
        :to="lead.actionTo"
        class="iot-access-signal-deck__lead-action"
      >
        {{ lead.actionLabel }}
      </RouterLink>
    </PanelCard>

    <div class="iot-access-signal-deck__metrics">
      <MetricCard
        v-for="metric in metrics"
        :key="`${metric.label}-${metric.value}`"
        :label="metric.label"
        :value="metric.value"
        size="compact"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';

interface SignalLead {
  eyebrow?: string;
  title: string;
  description?: string;
  actionLabel?: string;
  actionTo?: string;
}

interface SignalMetric {
  label: string;
  value: string;
}

defineProps<{
  lead: SignalLead;
  metrics: SignalMetric[];
}>();
</script>

<style scoped>
.iot-access-signal-deck {
  display: grid;
  gap: 0.95rem;
}

.iot-access-signal-deck__lead-eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.iot-access-signal-deck__lead-title {
  margin: 0.4rem 0 0;
  color: var(--text-heading);
  font-size: 1.04rem;
}

.iot-access-signal-deck__lead-description {
  margin: 0.6rem 0 0;
  color: var(--text-caption);
  line-height: 1.58;
}

.iot-access-signal-deck__lead-action {
  display: inline-flex;
  margin-top: 0.78rem;
  padding: 0.38rem 0.72rem;
  border-radius: var(--radius-pill);
  text-decoration: none;
  border: 1px solid var(--line-soft);
  color: var(--text-heading);
  background: var(--bg-surface-1);
}

.iot-access-signal-deck__metrics {
  display: grid;
  gap: 0.72rem;
}

@media (min-width: 960px) {
  .iot-access-signal-deck {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    align-items: start;
  }

  .iot-access-signal-deck__metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
