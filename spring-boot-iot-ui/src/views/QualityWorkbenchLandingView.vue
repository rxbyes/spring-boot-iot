<template>
  <StandardPageShell class="quality-workbench-landing">
    <StandardWorkbenchPanel
      title="质量工场总览"
      description="只负责判断当前应进入研发工场、执行中心还是结果与基线中心，不再承载资产编辑正文。"
      show-notices
    >
      <template #notices>
        <div class="landing-chip-list">
          <span>研发优先</span>
          <span>执行共享</span>
          <span>结果共享</span>
          <span>兼容旧入口</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="结构概况" description="本轮把研发自动化资产编排抬升为独立主入口。">
          <div class="quad-grid landing-metrics">
            <MetricCard
              v-for="metric in landingMetrics"
              :key="metric.label"
              :label="metric.label"
              :value="metric.value"
              :badge="metric.badge"
              size="compact"
            />
          </div>
        </PanelCard>

        <PanelCard title="进入建议" description="先定职责，再进入对应专项页，避免重新回到同页堆功能。">
          <ul class="landing-list">
            <li v-for="step in entrySteps" :key="step">{{ step }}</li>
          </ul>
        </PanelCard>

        <PanelCard title="本轮默认入口" description="研发需要先整理资产时，直接进入研发工场总览。">
          <p class="landing-summary">
            质量工场继续保留为上层分组，研发工场成为研发自动化资产编排的首选入口。
          </p>
          <RouterLink to="/rd-workbench" class="landing-link landing-link--primary">
            进入研发工场
          </RouterLink>
        </PanelCard>
      </section>

      <section class="landing-grid">
        <RouterLink
          v-for="card in displayCards"
          :key="card.path"
          :to="card.path"
          class="landing-card"
        >
          <span class="landing-card__short">{{ card.short }}</span>
          <strong>{{ card.label }}</strong>
          <p>{{ card.description }}</p>
        </RouterLink>
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { usePermissionStore } from '../stores/permission';

const permissionStore = usePermissionStore();

const qualityCards = [
  {
    path: '/rd-workbench',
    label: '研发工场',
    description: '面向研发的自动化资产编排主入口。',
    short: '研'
  },
  {
    path: '/automation-execution',
    label: '执行中心',
    description: '统一维护目标环境、执行范围与验收注册表。',
    short: '执'
  },
  {
    path: '/automation-results',
    label: '结果与基线中心',
    description: '统一导入结果、查看失败并维护基线证据。',
    short: '果'
  }
] as const;

const entrySteps = [
  '研发先进入研发工场，拆开页面盘点、模板沉淀、计划编排与交付打包。',
  '准备正式回归时切到执行中心，统一校准范围、命令与验收注册表。',
  '回归完成后进入结果与基线中心，导入结果并沉淀质量证据。'
];

const landingMetrics = computed(() => [
  {
    label: '专项入口',
    value: String(qualityCards.length),
    badge: { label: 'Hub', tone: 'brand' as const }
  },
  {
    label: '研发主入口',
    value: '1',
    badge: { label: 'RD', tone: 'success' as const }
  },
  {
    label: '共享中心',
    value: '2',
    badge: { label: 'Share', tone: 'warning' as const }
  },
  {
    label: '兼容入口',
    value: '2',
    badge: { label: 'Legacy', tone: 'danger' as const }
  }
]);

const visibleCards = computed(() =>
  qualityCards.filter((card) => !permissionStore.isLoggedIn || permissionStore.hasRoutePermission(card.path))
);

const displayCards = computed(() => (visibleCards.value.length > 0 ? visibleCards.value : qualityCards));
</script>

<style scoped>
.quality-workbench-landing {
  min-width: 0;
}

.landing-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.landing-chip-list span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 5%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.landing-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.landing-list {
  margin: 0;
  padding-left: 1.15rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.landing-summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.landing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.95rem;
}

.landing-card {
  display: grid;
  gap: 0.45rem;
  padding: 1rem;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  box-shadow: var(--shadow-card-soft);
  transition:
    border-color var(--transition-base),
    transform var(--transition-base),
    box-shadow var(--transition-base);
}

.landing-card:hover,
.landing-link:hover {
  border-color: color-mix(in srgb, var(--brand) 26%, var(--panel-border));
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.landing-card strong {
  color: var(--text-heading);
}

.landing-card p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.7;
}

.landing-card__short {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--brand);
  font-weight: 700;
}

.landing-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.95rem;
  padding: 0.7rem 1rem;
  border-radius: var(--radius-pill);
  text-decoration: none;
  transition:
    border-color var(--transition-base),
    transform var(--transition-base),
    box-shadow var(--transition-base);
}

.landing-link--primary {
  background: var(--brand);
  border: 1px solid var(--brand);
  color: white;
  box-shadow: var(--shadow-brand);
}

@media (max-width: 1024px) {
  .landing-grid,
  .landing-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
