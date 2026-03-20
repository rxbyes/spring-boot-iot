<template>
  <PanelCard eyebrow="Suggestion Engine" title="测试建议" description="按计划结构自动提示薄弱点，帮助持续完善测试质量。">
    <ul class="automation-suggestion-panel__list">
      <li
        v-for="item in suggestions"
        :key="`${item.level}-${item.title}`"
        class="automation-suggestion-panel__item"
        :class="`automation-suggestion-panel__item--${item.level}`"
      >
        <strong>{{ item.title }}</strong>
        <p>{{ item.detail }}</p>
      </li>
    </ul>
  </PanelCard>
</template>

<script setup lang="ts">
import PanelCard from './PanelCard.vue';
import type { AutomationPlanSuggestion } from '../types/automation';

defineProps<{
  suggestions: AutomationPlanSuggestion[];
}>();
</script>

<style scoped>
.automation-suggestion-panel__list {
  display: grid;
  gap: 0.85rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.automation-suggestion-panel__item {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
}

.automation-suggestion-panel__item strong {
  display: block;
  margin-bottom: 0.35rem;
}

.automation-suggestion-panel__item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.automation-suggestion-panel__item--warning {
  background: color-mix(in srgb, var(--warning) 10%, white);
  border-color: color-mix(in srgb, var(--warning) 20%, transparent);
}

.automation-suggestion-panel__item--info {
  background: color-mix(in srgb, var(--accent) 8%, white);
  border-color: color-mix(in srgb, var(--accent) 20%, transparent);
}

.automation-suggestion-panel__item--success {
  background: color-mix(in srgb, var(--success) 9%, white);
  border-color: color-mix(in srgb, var(--success) 22%, transparent);
}
</style>
