<template>
  <StandardPageShell class="quality-workbench-landing">
    <StandardWorkbenchPanel
      title="质量工场总览"
      description="把质量工场收成业务验收台和自动化治理台两条主路径，让入口更简单、结构更对称。"
      show-notices
    >
      <template #notices>
        <div class="landing-chip-list">
          <span>业务优先</span>
          <span>治理收口</span>
          <span>双入口</span>
        </div>
      </template>

      <section class="quality-workbench-landing__hero">
        <div class="quality-workbench-landing__hero-copy">
          <span class="quality-workbench-landing__eyebrow">Quality Factory</span>
          <h2>先做业务验收，再进入自动化治理。</h2>
          <p class="landing-summary">
            业务、产品和项目经理只从业务验收台发起交付验收；研发、测试和管理员在自动化治理台统一维护资产编排、执行配置和结果证据。
          </p>
          <RouterLink to="/business-acceptance" class="landing-link landing-link--primary">
            进入业务验收台
          </RouterLink>
        </div>

        <div class="quality-workbench-landing__summary-grid">
          <MetricCard
            v-for="metric in landingMetrics"
            :key="metric.label"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
            size="compact"
          />
        </div>
      </section>

      <section class="quality-workbench-landing__balanced-grid">
        <PanelCard title="进入顺序" description="按角色任务进入对应工作台，不在总览页堆叠细节。">
          <ol class="landing-list">
            <li v-for="step in entrySteps" :key="step">{{ step }}</li>
          </ol>
        </PanelCard>

        <PanelCard title="默认分工" description="总览只做分流，治理细节全部下沉到治理台内部。">
          <div class="landing-role-grid">
            <article
              v-for="item in roleHighlights"
              :key="item.title"
              class="landing-role-grid__item"
            >
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </PanelCard>
      </section>

      <section class="quality-workbench-landing__entry-grid">
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
    path: '/business-acceptance',
    label: '业务验收台',
    description: '按交付清单选择预置验收包并一键运行业务验收。',
    short: '验'
  },
  {
    path: '/automation-governance',
    label: '自动化治理台',
    description: '把资产编排、执行配置和结果证据收进同一治理工作区。',
    short: '治'
  }
] as const;

const entrySteps = [
  '验收人员、产品和项目经理先选择预置验收包，只配置环境、账号模板和模块范围。',
  '研发、测试和管理员进入自动化治理台，在资产编排里维护页面盘点、模板、计划和交付材料。',
  '执行配置与结果证据继续保留原有能力，但统一收口在治理台内部切换。'
];

const roleHighlights = [
  {
    title: '业务角色',
    description: '关注是否通过、哪些模块没过，以及对应运行编号。'
  },
  {
    title: '治理角色',
    description: '关注自动化资产编排、执行口径和失败证据沉淀。'
  }
];

const landingMetrics = computed(() => [
  {
    label: '专项入口',
    value: String(qualityCards.length),
    badge: { label: 'Hub', tone: 'brand' as const }
  },
  {
    label: '业务入口',
    value: '1',
    badge: { label: 'Biz', tone: 'success' as const }
  },
  {
    label: '治理入口',
    value: '1',
    badge: { label: 'RD', tone: 'warning' as const }
  },
  {
    label: '治理工作区',
    value: '3',
    badge: { label: 'Flow', tone: 'danger' as const }
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

.quality-workbench-landing__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 1rem;
  align-items: stretch;
}

.quality-workbench-landing__hero-copy,
.quality-workbench-landing__summary-grid {
  padding: 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-card-soft);
}

.quality-workbench-landing__hero-copy {
  display: grid;
  gap: 0.9rem;
  align-content: start;
}

.quality-workbench-landing__eyebrow {
  color: var(--text-tertiary);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quality-workbench-landing__hero-copy h2 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.85rem;
  line-height: 1.25;
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

.quality-workbench-landing__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.quality-workbench-landing__balanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.landing-list {
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.landing-role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.landing-role-grid__item {
  display: grid;
  gap: 0.45rem;
  min-height: 100%;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.landing-role-grid__item strong {
  color: var(--text-heading);
}

.landing-role-grid__item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.landing-summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.quality-workbench-landing__entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.landing-card {
  display: grid;
  gap: 0.55rem;
  min-height: 100%;
  padding: 1.1rem;
  border-radius: var(--radius-xl);
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
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
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
  width: 2.4rem;
  height: 2.4rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 8%, white);
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
  .quality-workbench-landing__hero,
  .quality-workbench-landing__balanced-grid,
  .quality-workbench-landing__entry-grid,
  .quality-workbench-landing__summary-grid,
  .landing-role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
