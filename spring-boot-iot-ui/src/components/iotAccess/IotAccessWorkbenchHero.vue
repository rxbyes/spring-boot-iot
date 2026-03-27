<template>
  <PanelCard class="iot-access-workbench-hero">
    <div class="iot-access-workbench-hero__layout">
      <div class="iot-access-workbench-hero__main">
        <p v-if="eyebrow" class="iot-access-workbench-hero__eyebrow">{{ eyebrow }}</p>
        <h2 v-if="title" class="iot-access-workbench-hero__title">{{ title }}</h2>
        <p class="iot-access-workbench-hero__judgement">{{ judgement }}</p>
        <p v-if="description" class="iot-access-workbench-hero__description">{{ description }}</p>

        <div v-if="tags.length" class="iot-access-workbench-hero__tags">
          <span v-for="tag in tags" :key="`${tag.label}-${tag.value}`" class="iot-access-workbench-hero__tag">
            <small>{{ tag.label }}</small>
            <strong>{{ tag.value }}</strong>
          </span>
        </div>

        <div v-if="actions.length" class="iot-access-workbench-hero__actions">
          <RouterLink
            v-for="action in actions"
            :key="`${action.label}-${action.to}`"
            :to="action.to"
            class="iot-access-workbench-hero__action"
            :class="`iot-access-workbench-hero__action--${action.variant || 'secondary'}`"
          >
            {{ action.label }}
          </RouterLink>
        </div>
      </div>

      <aside v-if="summaryItems.length" class="iot-access-workbench-hero__summary">
        <article
          v-for="item in summaryItems"
          :key="`${item.label}-${item.value}`"
          class="iot-access-workbench-hero__summary-item"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </aside>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import PanelCard from '@/components/PanelCard.vue';

interface HeroTag {
  label: string;
  value: string;
}

interface HeroAction {
  label: string;
  to: string;
  variant?: 'primary' | 'secondary';
}

interface HeroSummaryItem {
  label: string;
  value: string;
}

withDefaults(
  defineProps<{
    eyebrow?: string;
    title?: string;
    judgement: string;
    description?: string;
    tags?: HeroTag[];
    actions?: HeroAction[];
    summaryItems?: HeroSummaryItem[];
  }>(),
  {
    eyebrow: '',
    title: '',
    description: '',
    tags: () => [],
    actions: () => [],
    summaryItems: () => []
  }
);
</script>

<style scoped>
.iot-access-workbench-hero {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 252, 255, 0.98));
}

.iot-access-workbench-hero__layout {
  display: grid;
  gap: 1rem;
}

.iot-access-workbench-hero__eyebrow {
  margin: 0;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 0.72rem;
}

.iot-access-workbench-hero__title {
  margin: 0.35rem 0 0;
  font-size: 1.12rem;
  color: var(--text-heading);
}

.iot-access-workbench-hero__judgement {
  margin: 0.7rem 0 0;
  color: var(--text-heading);
  font-weight: 600;
}

.iot-access-workbench-hero__description {
  margin: 0.55rem 0 0;
  color: var(--text-caption);
  line-height: 1.62;
}

.iot-access-workbench-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.iot-access-workbench-hero__tag {
  display: inline-flex;
  align-items: baseline;
  gap: 0.32rem;
  padding: 0.26rem 0.52rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--line-soft);
  background: var(--bg-surface-2);
}

.iot-access-workbench-hero__tag small {
  color: var(--text-tertiary);
}

.iot-access-workbench-hero__tag strong {
  color: var(--text-heading);
}

.iot-access-workbench-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin-top: 0.82rem;
}

.iot-access-workbench-hero__action {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-pill);
  font-weight: 600;
  text-decoration: none;
}

.iot-access-workbench-hero__action--primary {
  color: #fff;
  background: var(--brand-primary);
}

.iot-access-workbench-hero__action--secondary {
  color: var(--text-heading);
  border: 1px solid var(--line-soft);
  background: var(--bg-surface-1);
}

.iot-access-workbench-hero__summary {
  display: grid;
  gap: 0.5rem;
}

.iot-access-workbench-hero__summary-item {
  display: grid;
  gap: 0.24rem;
  padding: 0.62rem 0.75rem;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--bg-surface-1);
}

.iot-access-workbench-hero__summary-item span {
  color: var(--text-tertiary);
  font-size: 0.78rem;
}

.iot-access-workbench-hero__summary-item strong {
  color: var(--text-heading);
}

@media (min-width: 960px) {
  .iot-access-workbench-hero__layout {
    grid-template-columns: minmax(0, 1fr) 16rem;
    align-items: start;
  }
}
</style>
