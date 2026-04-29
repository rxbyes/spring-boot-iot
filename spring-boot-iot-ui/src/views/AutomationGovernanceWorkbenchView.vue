<template>
  <StandardPageShell class="automation-governance-workbench">
    <StandardWorkbenchPanel
      title="自动化治理台"
      description="把资产编排、执行配置和结果证据收进同一治理工作区，减少入口层级和跨页切换。"
      show-notices
    >
      <template #notices>
        <div class="automation-governance-workbench__chips">
          <span>资产编排</span>
          <span>执行配置</span>
          <span>结果证据</span>
        </div>
      </template>

      <section class="automation-governance-workbench__hero">
        <div class="automation-governance-workbench__hero-copy">
          <h2>自动化治理台</h2>
          <p>
            研发、测试和管理员统一在这里维护自动化资产、执行配置和结果证据。入口收口后，质量工场只保留业务验收台和治理台两条主路径。
          </p>
          <div class="automation-governance-workbench__hero-context">
            <span>{{ currentTab.label }}</span>
            <span>{{ tabSummary }}</span>
          </div>
        </div>

        <div class="automation-governance-workbench__hero-metrics">
          <MetricCard
            v-for="metric in governanceMetrics"
            :key="metric.label"
            size="compact"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
          />
        </div>
      </section>

      <section class="automation-governance-workbench__balanced-grid">
        <PanelCard title="合并原则" description="收入口径，不收敛能力边界。">
          <ol class="automation-governance-workbench__list">
            <li>质量工场一级入口只保留业务验收台和自动化治理台。</li>
            <li>治理台内部按资产编排、执行配置、结果证据三段组织工作流。</li>
            <li>runId 深链统一落到结果证据视图，不再绕回旧入口。</li>
          </ol>
        </PanelCard>

        <PanelCard title="当前聚焦" description="按当前 query 自动切换治理上下文。">
          <div class="automation-governance-workbench__focus-grid">
            <article>
              <span>主工作区</span>
              <strong>{{ currentTab.label }}</strong>
            </article>
            <article>
              <span>资产子区</span>
              <strong>{{ currentAssetTab.label }}</strong>
            </article>
            <article>
              <span>runId</span>
              <strong>{{ activeRunId || '未预选' }}</strong>
            </article>
          </div>
        </PanelCard>
      </section>

      <nav class="automation-governance-workbench__tabs" aria-label="治理工作区切换">
        <button
          v-for="item in governanceTabs"
          :key="item.key"
          type="button"
          class="automation-governance-workbench__tab"
          :class="{ 'automation-governance-workbench__tab--active': item.key === activeTab }"
          @click="selectTab(item.key)"
        >
          <strong>{{ item.label }}</strong>
          <span>{{ item.description }}</span>
        </button>
      </nav>

      <section
        v-if="activeTab === 'assets'"
        class="automation-governance-workbench__panel automation-governance-workbench__panel--assets"
      >
        <AutomationAssetsWorkspaceSection
          :active-tab="activeAssetTab"
          :tabs="assetTabs"
          @select-tab="selectAssetTab"
        />
      </section>

      <section
        v-else-if="activeTab === 'execution'"
        class="automation-governance-workbench__panel"
      >
        <AutomationExecutionWorkspaceSection />
      </section>

      <section v-else class="automation-governance-workbench__panel">
        <AutomationEvidenceWorkspaceSection />
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import AutomationAssetsWorkspaceSection from '../components/automationGovernance/AutomationAssetsWorkspaceSection.vue';
import AutomationEvidenceWorkspaceSection from '../components/automationGovernance/AutomationEvidenceWorkspaceSection.vue';
import AutomationExecutionWorkspaceSection from '../components/automationGovernance/AutomationExecutionWorkspaceSection.vue';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useAutomationGovernanceWorkbench } from '../composables/useAutomationGovernanceWorkbench';

const {
  governanceTabs,
  assetTabs,
  activeTab,
  activeAssetTab,
  activeRunId,
  currentTab,
  currentAssetTab,
  governanceMetrics,
  tabSummary,
  selectTab,
  selectAssetTab
} = useAutomationGovernanceWorkbench();
</script>

<style scoped>
.automation-governance-workbench {
  min-width: 0;
}

.automation-governance-workbench__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.automation-governance-workbench__chips span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 5%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.automation-governance-workbench__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  gap: 1rem;
}

.automation-governance-workbench__hero-copy,
.automation-governance-workbench__hero-metrics {
  padding: 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: var(--bg-card);
  box-shadow: var(--shadow-card-soft);
}

.automation-governance-workbench__hero-copy {
  display: grid;
  gap: 0.8rem;
  align-content: start;
}

.automation-governance-workbench__hero-copy h2 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.75rem;
  line-height: 1.25;
}

.automation-governance-workbench__hero-copy p,
.automation-governance-workbench__paragraph {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.automation-governance-workbench__hero-context {
  display: grid;
  gap: 0.45rem;
}

.automation-governance-workbench__hero-context span:first-child {
  color: var(--text-heading);
  font-weight: 700;
}

.automation-governance-workbench__hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.automation-governance-workbench__balanced-grid,
.automation-governance-workbench__workspace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.automation-governance-workbench__balanced-grid {
  margin-top: 1rem;
}

.automation-governance-workbench__list,
.automation-governance-workbench__note-list {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.automation-governance-workbench__focus-grid {
  display: grid;
  gap: 0.8rem;
}

.automation-governance-workbench__focus-grid article {
  display: grid;
  gap: 0.3rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.automation-governance-workbench__focus-grid span {
  color: var(--text-caption);
  font-size: 0.88rem;
}

.automation-governance-workbench__focus-grid strong {
  color: var(--text-heading);
}

.automation-governance-workbench__tabs {
  display: grid;
  gap: 0.85rem;
  margin-top: 1rem;
}

.automation-governance-workbench__tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.automation-governance-workbench__tab {
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
  color: inherit;
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    transform var(--transition-base);
}

.automation-governance-workbench__tab {
  display: grid;
  gap: 0.4rem;
  align-content: start;
  min-height: 100%;
  padding: 1rem;
  border-radius: var(--radius-xl);
  text-align: left;
}

.automation-governance-workbench__tab strong {
  color: var(--text-heading);
}

.automation-governance-workbench__tab span {
  color: var(--text-caption);
  line-height: 1.6;
}

.automation-governance-workbench__tab--active,
.automation-governance-workbench__tab:hover {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  box-shadow: var(--shadow-card-soft);
  transform: translateY(-1px);
}

.automation-governance-workbench__tab--active {
  background: color-mix(in srgb, var(--brand) 5%, white);
}

.automation-governance-workbench__panel {
  margin-top: 1rem;
}

.automation-governance-workbench__panel--assets {
  display: grid;
  gap: 1rem;
}

@media (max-width: 1024px) {
  .automation-governance-workbench__hero,
  .automation-governance-workbench__balanced-grid,
  .automation-governance-workbench__workspace-grid,
  .automation-governance-workbench__hero-metrics,
  .automation-governance-workbench__tabs {
    grid-template-columns: 1fr;
  }
}
</style>
