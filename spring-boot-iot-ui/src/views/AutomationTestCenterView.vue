<template>
  <div class="page-stack automation-test-view">
    <section class="hero-panel">
      <p class="eyebrow">Automation Studio</p>
      <h1 class="headline">自动化工场</h1>
      <p class="hero-description">
        以现有 Playwright 骨架为执行底座，通过前端可视化配置生成声明式测试计划，覆盖登录、页面交互、接口回执、断言、报告与测试建议。
      </p>
      <div class="hero-chip-list">
        <span>配置驱动</span>
        <span>可插拔步骤</span>
        <span>任意 Web 接入</span>
        <span>测试报告输出</span>
        <span>改进建议生成</span>
      </div>
    </section>

    <section class="tri-grid">
      <PanelCard eyebrow="Plan Metrics" title="计划概况" description="先用场景粒度组织业务，再按步骤粒度沉淀可复用自动化模板。">
        <div class="quad-grid automation-plan-metrics">
          <MetricCard
            v-for="metric in planMetrics"
            :key="metric.label"
            class="automation-plan-metrics__card"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
          />
        </div>
      </PanelCard>

      <PanelCard eyebrow="Runtime" title="执行方式" description="导出的 JSON 计划可直接交给 `scripts/auto` 执行器运行。">
        <div class="command-box">
          <code>{{ commandPreview }}</code>
        </div>
        <ul class="phase-ideas">
          <li>支持 `--plan` 按任意 JSON 计划执行，不再局限于仓库内置页面。</li>
          <li>支持 `--dry-run` 预览执行计划，适合测试负责人先做编排审查。</li>
          <li>失败结果继续落盘到 `logs/acceptance`，可复用现有报告归档链路。</li>
        </ul>
        <StandardActionGroup gap="sm">
          <el-button type="primary" @click="copyCommand">复制命令</el-button>
          <el-button @click="downloadPlan">导出 JSON</el-button>
        </StandardActionGroup>
      </PanelCard>

      <PanelCard eyebrow="Roadmap" title="能力边界" description="本轮先建设平台骨架，后续可以逐步叠加更多模板、插件与 AI 辅助能力。">
        <ul class="phase-ideas">
          <li>当前已支持页面可达、交互动作、接口断言、变量捕获、报告建议。</li>
          <li>后续可继续扩展截图对比、表格比对、爬取式页面盘点、AI 用例补全。</li>
          <li>通过菜单与 JSON 计划解耦，可复用到任意带浏览器界面的业务系统。</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <AutomationExecutionConfigPanel :target="plan.target" :scope-options="scopeOptions" />
      <AutomationSuggestionPanel :suggestions="suggestions" />
    </section>

    <section>
      <AutomationPageDiscoveryPanel
        ref="inventoryTableRef"
        :metrics="inventoryMetrics"
        :inventory-source-text="inventorySourceText"
        :page-inventory="pageInventory"
        :build-inventory-source-label="buildInventorySourceLabel"
        :build-template-label="buildTemplateLabel"
        :is-route-covered="isRouteCovered"
        @refresh="refreshPageInventory"
        @select-uncovered="selectUncoveredPages"
        @generate-selected="generateSelectedInventoryScenarios"
        @generate-uncovered="generateUncoveredInventoryScenarios"
        @open-manual-page="openManualPageDialog"
        @selection-change="handleInventorySelectionChange"
        @remove-manual-page="removeManualPage"
      />
    </section>

    <section>
      <PanelCard
        eyebrow="Scenario Builder"
        title="场景编排"
        description="先通过模板快速起步，再替换页面路由、选择器、接口匹配与断言规则。"
      >
        <template #actions>
          <StandardActionGroup gap="sm">
            <el-button type="primary" @click="addScenario('pageSmoke')">新增页面冒烟模板</el-button>
            <el-button @click="addScenario('formSubmit')">新增表单提交模板</el-button>
            <el-button @click="addScenario('listDetail')">新增列表详情模板</el-button>
            <el-button @click="showImportDialog = true">导入计划</el-button>
            <el-button @click="resetPlan">恢复默认计划</el-button>
          </StandardActionGroup>
        </template>

        <div v-if="plan.scenarios.length === 0" class="empty-block">
          当前暂无场景，请先选择一个模板开始编排。
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
      <PanelCard eyebrow="Preview" title="场景预览" description="这里用于快速查看每个场景的覆盖粒度。">
        <el-table :data="scenarioPreviews" size="small" border>
          <el-table-column prop="key" label="编码" min-width="160" />
          <el-table-column prop="scope" label="范围" width="110" />
          <el-table-column prop="stepCount" label="步骤" width="90" />
          <el-table-column prop="apiCount" label="接口" width="90" />
          <el-table-column prop="featureCount" label="业务点" width="100" />
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
        eyebrow="Plan Export"
        title="导出计划 JSON"
        description="可直接交给 `node scripts/auto/run-browser-acceptance.mjs --plan=...` 执行。"
        :body="plan"
      />
    </section>

    <AutomationPlanImportDrawer
      v-model="showImportDialog"
      @confirm="applyImport"
    />

    <AutomationManualPageDrawer
      v-model="showManualPageDialog"
      :scope-options="scopeOptions"
      :template-options="inventoryTemplateOptions"
      :build-template-label="buildTemplateLabel"
      @save="saveManualPage"
    />
  </div>
</template>

<script setup lang="ts">
import AutomationExecutionConfigPanel from '../components/AutomationExecutionConfigPanel.vue';
import AutomationManualPageDrawer from '../components/AutomationManualPageDrawer.vue';
import AutomationPlanImportDrawer from '../components/AutomationPlanImportDrawer.vue';
import AutomationPageDiscoveryPanel from '../components/AutomationPageDiscoveryPanel.vue';
import AutomationScenarioEditor from '../components/AutomationScenarioEditor.vue';
import AutomationSuggestionPanel from '../components/AutomationSuggestionPanel.vue';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardActionGroup from '../components/StandardActionGroup.vue';
import { useAutomationPlanBuilder } from '../composables/useAutomationPlanBuilder';

const {
  scopeOptions,
  locatorTypeOptions,
  stepTypeOptions,
  inventoryTemplateOptions,
  plan,
  inventoryTableRef,
  showImportDialog,
  showManualPageDialog,
  scenarioPreviews,
  suggestions,
  pageInventory,
  inventorySourceText,
  planMetrics,
  inventoryMetrics,
  commandPreview,
  buildTemplateLabel,
  buildInventorySourceLabel,
  isRouteCovered,
  handleInventorySelectionChange,
  refreshPageInventory,
  selectUncoveredPages,
  generateSelectedInventoryScenarios,
  generateUncoveredInventoryScenarios,
  openManualPageDialog,
  saveManualPage,
  removeManualPage,
  addScenario,
  copyScenario,
  removeScenario,
  moveScenario,
  addInitialApi,
  addStep,
  addCapture,
  handleStepTypeChange,
  handleScreenshotTargetChange,
  moveStep,
  copyCommand,
  downloadPlan,
  resetPlan,
  applyImport
} = useAutomationPlanBuilder();
</script>

<style scoped>
.automation-test-view {
  padding-bottom: 1rem;
}

.hero-description {
  margin: 0.85rem 0 0;
  max-width: 72rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.hero-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1.2rem;
}

.hero-chip-list span {
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand) 5%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.phase-ideas {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.automation-plan-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.automation-plan-metrics__card {
  min-height: 7.75rem;
}

.command-box {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: color-mix(in srgb, var(--brand) 4%, white);
  overflow: auto;
}

.command-box code {
  font-family: var(--font-mono);
  color: var(--text-heading);
  white-space: nowrap;
}

.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .automation-plan-metrics {
    grid-template-columns: 1fr;
  }

}
</style>

