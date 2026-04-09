<template>
  <section
    class="iot-access-signal-deck"
    :class="hasMetrics ? 'iot-access-signal-deck--with-metrics' : 'iot-access-signal-deck--lead-only'"
  >
    <PanelCard class="iot-access-signal-deck__lead">
      <h3 class="iot-access-signal-deck__lead-title">{{ lead.title }}</h3>
      <p v-if="lead.description" class="iot-access-signal-deck__lead-description">{{ lead.description }}</p>
      <RouterLink
        v-if="resolvedAction"
        :to="resolvedAction.to"
        class="iot-access-signal-deck__lead-action"
      >
        {{ resolvedAction.label }}
      </RouterLink>
    </PanelCard>

    <div v-if="metrics.length" class="iot-access-signal-deck__metrics">
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
import { computed } from 'vue';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';

interface SignalLead {
  title: string;
  description?: string;
  action?: {
    label: string;
    to: string;
  };
  actionLabel?: string;
  actionTo?: string;
}

interface SignalMetric {
  label: string;
  value: string;
}

const props = withDefaults(
  defineProps<{
    lead: SignalLead;
    metrics?: SignalMetric[];
  }>(),
  {
    metrics: () => []
  }
);

const resolvedAction = computed(() => {
  if (props.lead.action?.label && props.lead.action?.to) {
    return props.lead.action;
  }
  if (props.lead.actionLabel && props.lead.actionTo) {
    return {
      label: props.lead.actionLabel,
      to: props.lead.actionTo
    };
  }
  return null;
});

const hasMetrics = computed(() => props.metrics.length > 0);
</script>

<style scoped>
.iot-access-signal-deck {
  display: grid;
  gap: 0.95rem;
}

.iot-access-signal-deck__lead-title {
  margin: 0;
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
  .iot-access-signal-deck--with-metrics {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    align-items: start;
  }

  .iot-access-signal-deck--with-metrics .iot-access-signal-deck__metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
