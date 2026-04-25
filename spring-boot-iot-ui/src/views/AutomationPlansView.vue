<template>
  <StandardPageShell class="automation-plans-view">
    <StandardWorkbenchPanel
      title="计划编排台"
      description="只负责正式计划、步骤与断言维护，以及导入导出，不再承载页面盘点或结果导入。"
      show-notices
    >
      <template #notices>
        <div class="automation-chip-list">
          <span>场景顺序</span>
          <span>步骤断言</span>
          <span>计划导入导出</span>
          <span>JSON 快照</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="计划概况" description="正式计划统一在本页维护，不再和盘点、交付说明混写。">
          <div class="quad-grid plan-metrics">
            <MetricCard
              v-for="metric in composerMetrics"
              :key="metric.label"
              :label="metric.label"
              :value="metric.value"
              :badge="metric.badge"
              size="compact"
            />
          </div>
        </PanelCard>

        <PanelCard title="导入导出" description="计划编排台只负责计划 JSON 的导入、导出与重置。">
          <ul class="plan-note-list">
            <li>模板起步应优先在场景模板台完成，再回到本页细调步骤。</li>
            <li>如果需要恢复既有计划，可直接导入 JSON，而不是重新手工拼接。</li>
            <li>导出结果会继续交给既有浏览器执行器和执行中心消费。</li>
          </ul>
          <StandardActionGroup gap="sm">
            <StandardButton action="batch" v-permission="'system:rd-automation-plans:import'" @click="showImportDialog = true">导入计划</StandardButton>
            <StandardButton action="confirm" v-permission="'system:rd-automation-plans:export'" @click="downloadPlan">导出 JSON</StandardButton>
            <StandardButton action="reset" v-permission="'system:rd-automation-plans:reset'" @click="resetPlan">恢复默认计划</StandardButton>
          </StandardActionGroup>
        </PanelCard>

        <PanelCard title="当前建议" description="计划层面的提醒只在这里查看，不混入结果中心。">
          <div v-if="suggestions.length === 0" class="empty-block">
            当前没有计划建议，可以继续维护步骤与断言。
          </div>
          <ul v-else class="suggestion-list">
            <li v-for="item in suggestions" :key="`${item.level}-${item.title}`" class="suggestion-list__item">
              <strong>{{ item.title }}</strong>
              <p>{{ item.detail }}</p>
            </li>
          </ul>
        </PanelCard>
      </section>

      <section>
        <PanelCard
          title="场景编辑"
          description="模板起步和脚手架生成完成后，在这里维护正式步骤、接口检查和断言。"
        >
          <div v-if="plan.scenarios.length === 0" class="empty-block">
            当前暂无场景，请先前往场景模板台补充模板，或导入既有计划。
          </div>

          <AutomationScenarioEditor
            v-for="(scenario, scenarioIndex) in plan.scenarios"
            :key="scenario.key"
            :scenario="scenario"
            :scenario-index="scenarioIndex"
            :scenario-count="plan.scenarios.length"
            :scope-options="scopeOptions"
            :locator-type-options="locatorTypeOptions"
            :step-type-options="stepTypeOptions"
            @move-scenario="moveScenario(scenarioIndex, $event)"
            @copy-scenario="copyScenario(scenarioIndex)"
            @remove-scenario="removeScenario(scenarioIndex)"
            @add-initial-api="addInitialApi(scenario)"
            @add-step="addStep(scenario)"
            @move-step="moveStep(scenario, $event.stepIndex, $event.offset)"
            @remove-step="scenario.steps.splice($event.stepIndex, 1)"
            @change-step-type="handleStepTypeChange($event.step)"
            @change-screenshot-target="handleScreenshotTargetChange($event.step)"
            @add-capture="addCapture($event.step)"
          />
        </PanelCard>
      </section>

      <section class="two-column-grid">
        <PanelCard title="场景预览" description="快速确认正式计划的范围、步骤密度和断言覆盖。">
          <StandardTableToolbar
            compact
            :meta-items="[
              `当前场景 ${scenarioPreviews.length} 个`,
              `待补齐建议 ${warningSuggestions.length} 个`
            ]"
          />
          <el-table :data="scenarioPreviews" size="small" border>
            <StandardTableTextColumn prop="name" label="场景" :min-width="180" />
            <StandardTableTextColumn prop="scope" label="范围" :width="110" />
            <StandardTableTextColumn prop="stepCount" label="步骤" :width="90" />
            <StandardTableTextColumn prop="apiCount" label="接口" :width="90" />
            <el-table-column label="断言" width="90">
              <template #default="{ row }">
                <el-tag :type="row.hasAssertion ? 'success' : 'warning'">
                  {{ row.hasAssertion ? '已覆盖' : '待补齐' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </PanelCard>

        <ResponsePanel
          title="当前计划快照"
          description="这里保留正式计划 JSON 快照，便于交给执行中心与交付打包台复用。"
          :body="plan"
        />
      </section>
    </StandardWorkbenchPanel>

    <AutomationPlanImportDrawer
      v-model="showImportDialog"
      @confirm="applyImport"
    />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import AutomationPlanImportDrawer from '../components/AutomationPlanImportDrawer.vue';
import AutomationScenarioEditor from '../components/AutomationScenarioEditor.vue';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import StandardButton from '../components/StandardButton.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useAutomationPlanComposer } from '../composables/useAutomationPlanComposer';

const {
  plan,
  scopeOptions,
  locatorTypeOptions,
  stepTypeOptions,
  showImportDialog,
  scenarioPreviews,
  suggestions,
  warningSuggestions,
  planSummary,
  copyScenario,
  removeScenario,
  moveScenario,
  addInitialApi,
  addStep,
  addCapture,
  handleStepTypeChange,
  handleScreenshotTargetChange,
  moveStep,
  downloadPlan,
  resetPlan,
  applyImport
} = useAutomationPlanComposer();

const composerMetrics = computed(() => [
  {
    label: '正式场景',
    value: String(planSummary.value.scenarioCount),
    badge: { label: 'Plan', tone: 'brand' as const }
  },
  {
    label: '执行范围',
    value: String(planSummary.value.scenarioScopes.length),
    badge: { label: 'Scope', tone: 'success' as const }
  },
  {
    label: '阻断范围',
    value: String(planSummary.value.failScopes.length),
    badge: { label: 'Block', tone: 'warning' as const }
  },
  {
    label: '待补齐建议',
    value: String(warningSuggestions.value.length),
    badge: { label: 'Gap', tone: 'danger' as const }
  }
]);
</script>

<style scoped>
.automation-plans-view {
  min-width: 0;
}

.automation-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.automation-chip-list span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 5%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.plan-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.plan-note-list,
.suggestion-list {
  margin: 0;
  padding-left: 1.15rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.suggestion-list {
  list-style: none;
  padding-left: 0;
}

.suggestion-list__item {
  padding: 0.85rem 0;
  border-bottom: 1px solid var(--line-soft);
}

.suggestion-list__item:last-child {
  border-bottom: none;
}

.suggestion-list__item strong {
  color: var(--text-heading);
}

.suggestion-list__item p {
  margin: 0.35rem 0 0;
}

.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .plan-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
