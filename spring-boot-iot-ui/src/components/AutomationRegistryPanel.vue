<template>
  <PanelCard
    title="验收注册表"
    description="前端直接读取统一验收注册表，展示场景、执行器、阻断等级、依赖关系与文档映射。"
  >
    <div class="registry-metrics">
      <span>场景 {{ summary.total }}</span>
      <span>阻断 {{ summary.blockerCount }}</span>
      <span>执行器 {{ runnerBreakdown }}</span>
    </div>

    <div v-if="scenarios.length === 0" class="registry-empty">
      当前尚未加载到验收注册表场景。
    </div>

    <ul v-else class="registry-list">
      <li v-for="scenario in scenarios" :key="scenario.id" class="registry-list__item">
        <div class="registry-list__header">
          <strong>{{ scenario.title }}</strong>
          <code>{{ scenario.id }}</code>
        </div>
        <div class="registry-list__chips">
          <span>{{ scenario.runnerType }}</span>
          <span>{{ scenario.blocking }}</span>
          <span>{{ scenario.scope }}</span>
        </div>
        <p class="registry-list__doc">{{ scenario.docRef || '未映射文档章节' }}</p>
        <p class="registry-list__deps">
          依赖：{{ scenario.dependsOn.length > 0 ? scenario.dependsOn.join(' / ') : '无' }}
        </p>
      </li>
    </ul>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import PanelCard from './PanelCard.vue';
import type {
  AcceptanceRegistryScenario,
  AcceptanceRegistrySummary
} from '../types/automation';

const props = defineProps<{
  scenarios: AcceptanceRegistryScenario[];
  summary: AcceptanceRegistrySummary;
}>();

const runnerBreakdown = computed(() =>
  Object.entries(props.summary.byRunner)
    .map(([runnerType, count]) => `${runnerType} ${count}`)
    .join(' / ')
);
</script>

<style scoped>
.registry-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-bottom: 1rem;
  color: var(--text-secondary);
  font-size: 0.92rem;
}

.registry-list {
  display: grid;
  gap: 0.85rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.registry-list__item {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.registry-list__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: baseline;
}

.registry-list__header strong {
  color: var(--text-heading);
}

.registry-list__header code {
  font-family: var(--font-mono);
  color: var(--text-secondary);
  white-space: nowrap;
}

.registry-list__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  margin: 0.7rem 0;
}

.registry-list__chips span {
  padding: 0.24rem 0.65rem;
  border-radius: var(--radius-pill);
  border: 1px solid color-mix(in srgb, var(--brand) 12%, white);
  background: white;
  color: var(--text-primary);
  font-size: 0.84rem;
}

.registry-list__doc,
.registry-list__deps,
.registry-empty {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
