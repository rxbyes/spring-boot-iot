<template>
  <section class="reporting-status-header">
    <div class="reporting-status-header__grid">
      <article class="reporting-status-header__card reporting-status-header__card--status">
        <span class="reporting-status-header__eyebrow">当前状态</span>
        <strong class="reporting-status-header__title">当前{{ title }}</strong>
        <p class="reporting-status-header__summary">{{ summary }}</p>
        <p v-if="headline" class="reporting-status-header__finding">
          {{ headline }}
          <span v-if="headlineNote"> · {{ headlineNote }}</span>
        </p>
      </article>

      <article class="reporting-status-header__card">
        <span class="reporting-status-header__eyebrow">{{ blockerLabel }}</span>
        <p class="reporting-status-header__blocker">{{ blockerValue }}</p>
      </article>

      <article class="reporting-status-header__card reporting-status-header__card--actions">
        <span class="reporting-status-header__eyebrow">下一步动作</span>
        <StandardActionGroup class="reporting-status-header__actions" gap="sm">
          <StandardButton
            v-for="action in actions"
            :key="action.target"
            action="refresh"
            plain
            @click="$emit('action', action.target)"
          >
            {{ action.label }}
          </StandardButton>
        </StandardActionGroup>
      </article>
    </div>

    <div class="reporting-status-header__context">
      <span
        v-for="item in contextItems"
        :key="item.key"
        class="reporting-status-header__context-chip"
      >
        <em>{{ item.label }}</em>
        <strong>{{ item.value || '--' }}</strong>
      </span>
    </div>
  </section>
</template>

<script setup lang="ts">
import StandardActionGroup from '@/components/StandardActionGroup.vue';
import StandardButton from '@/components/StandardButton.vue';
import type { ReportingActionTarget } from '@/views/reportingDiagnosis';

export interface ReportingStatusHeaderAction {
  label: string;
  target: ReportingActionTarget;
}

export interface ReportingStatusHeaderContextItem {
  key: string;
  label: string;
  value: string;
}

defineProps<{
  title: string;
  summary: string;
  headline?: string;
  headlineNote?: string;
  blockerLabel: string;
  blockerValue: string;
  actions: ReportingStatusHeaderAction[];
  contextItems: ReportingStatusHeaderContextItem[];
}>();

defineEmits<{
  (event: 'action', target: ReportingActionTarget): void;
}>();
</script>

<style scoped>
.reporting-status-header {
  display: grid;
  gap: 0.82rem;
  padding: 0.98rem 1rem 1rem;
  border: 1px solid var(--line-panel);
  border-radius: var(--radius-card);
  background:
    linear-gradient(180deg, rgba(14, 116, 144, 0.04), rgba(14, 116, 144, 0)),
    var(--panel);
  box-shadow: var(--shadow-card);
}

.reporting-status-header__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.24fr) minmax(0, 0.88fr) minmax(0, 0.9fr);
  gap: 0.82rem;
}

.reporting-status-header__card {
  display: grid;
  gap: 0.42rem;
  padding: 0.92rem 0.96rem;
  border-radius: calc(var(--radius-card) - 4px);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.reporting-status-header__card--status {
  background:
    linear-gradient(135deg, rgba(14, 116, 144, 0.08), rgba(14, 116, 144, 0.02)),
    rgba(255, 255, 255, 0.92);
}

.reporting-status-header__card--actions {
  align-content: start;
}

.reporting-status-header__eyebrow {
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.reporting-status-header__title {
  color: var(--text-heading);
  font-size: 1.18rem;
  line-height: 1.25;
}

.reporting-status-header__summary,
.reporting-status-header__blocker,
.reporting-status-header__finding {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.reporting-status-header__finding {
  color: var(--text-primary);
  font-weight: 600;
}

.reporting-status-header__actions {
  flex-wrap: wrap;
}

.reporting-status-header__context {
  display: flex;
  flex-wrap: wrap;
  gap: 0.62rem;
}

.reporting-status-header__context-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  min-height: 2rem;
  padding: 0.32rem 0.72rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.04);
  color: var(--text-secondary);
}

.reporting-status-header__context-chip em {
  font-style: normal;
  color: var(--text-tertiary);
}

.reporting-status-header__context-chip strong {
  color: var(--text-primary);
  font-size: 0.88rem;
}

@media (max-width: 1080px) {
  .reporting-status-header__grid {
    grid-template-columns: 1fr;
  }
}
</style>
