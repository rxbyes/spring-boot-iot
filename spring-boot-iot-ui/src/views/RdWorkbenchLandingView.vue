<template>
  <StandardPageShell class="rd-workbench-landing">
    <StandardWorkbenchPanel
      title="研发工场总览"
      description="研发工场只负责组织研发自动化资产编排主链路，不再和执行中心、结果中心同页堆叠。"
      show-notices
    >
      <template #notices>
        <div class="landing-chip-list">
          <span>页面盘点</span>
          <span>模板沉淀</span>
          <span>计划编排</span>
          <span>交付打包</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="研发链路" description="用四个专项页替代旧的单页自动化资产中心。">
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

        <PanelCard title="推荐顺序" description="按资产编排链路推进，避免在计划页里混做盘点与交付。">
          <ul class="landing-list">
            <li v-for="step in entrySteps" :key="step">{{ step }}</li>
          </ul>
        </PanelCard>

        <PanelCard title="下一跳" description="研发工场完成资产整理后，再切换到执行中心组织正式回归。">
          <p class="landing-summary">
            页面盘点解决覆盖缺口，模板台解决复用起步，计划编排台维护正式计划，交付打包台整理执行说明。
          </p>
          <RouterLink to="/rd-automation-plans" class="landing-link landing-link--primary">
            进入计划编排台
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

const rdCards = [
  {
    path: '/rd-automation-inventory',
    label: '页面盘点台',
    description: '维护页面清单、覆盖缺口与人工补录页面。',
    short: '盘'
  },
  {
    path: '/rd-automation-templates',
    label: '场景模板台',
    description: '沉淀页面冒烟、表单提交与列表详情模板。',
    short: '模'
  },
  {
    path: '/rd-automation-plans',
    label: '计划编排台',
    description: '维护场景顺序、步骤、断言、导入与导出。',
    short: '编'
  },
  {
    path: '/rd-automation-handoff',
    label: '交付打包台',
    description: '整理计划摘要、执行建议、基线说明与验收备注。',
    short: '交'
  }
] as const;

const entrySteps = [
  '先到页面盘点台识别页面清单、覆盖缺口和人工补录项。',
  '再到场景模板台挑选页面冒烟、表单提交或列表详情模板。',
  '随后在计划编排台维护顺序、步骤、断言与导入导出。',
  '最后到交付打包台整理执行建议、基线说明和交付备注。'
];

const landingMetrics = computed(() => [
  {
    label: '研发模块',
    value: String(rdCards.length),
    badge: { label: 'RD', tone: 'brand' as const }
  },
  {
    label: '模板入口',
    value: '3',
    badge: { label: 'Tpl', tone: 'success' as const }
  },
  {
    label: '正式计划',
    value: '1',
    badge: { label: 'Plan', tone: 'warning' as const }
  },
  {
    label: '交付出口',
    value: '1',
    badge: { label: 'Pack', tone: 'danger' as const }
  }
]);

const visibleCards = computed(() =>
  rdCards.filter((card) => !permissionStore.isLoggedIn || permissionStore.hasRoutePermission(card.path))
);

const displayCards = computed(() => (visibleCards.value.length > 0 ? visibleCards.value : rdCards));
</script>

<style scoped>
.rd-workbench-landing {
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
