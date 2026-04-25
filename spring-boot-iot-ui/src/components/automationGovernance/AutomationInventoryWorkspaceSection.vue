<template>
  <div class="automation-inventory-workspace-section">
    <section class="tri-grid">
      <PanelCard title="盘点概况" description="优先识别覆盖缺口，再批量生成候选页面脚手架。">
        <div class="quad-grid inventory-metrics">
          <MetricCard
            v-for="metric in inventoryMetrics"
            :key="metric.label"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
            size="compact"
          />
        </div>
      </PanelCard>

      <PanelCard title="盘点来源" description="来源统一基于授权菜单、自定义页面和静态种子。">
        <p class="inventory-summary">当前来源：{{ inventorySourceText }}</p>
        <ul class="inventory-list">
          <li>优先刷新页面盘点，确保当前菜单与补录页面都被纳入清单。</li>
          <li>针对未覆盖页面先做勾选，再批量生成页面脚手架。</li>
          <li>人工补录只留在这里维护，避免交叉写入计划编排。</li>
        </ul>
        <StandardActionGroup gap="sm">
          <StandardButton
            v-permission="'system:rd-automation-inventory:refresh'"
            action="refresh"
            @click="refreshPageInventory"
          >
            刷新盘点
          </StandardButton>
          <StandardButton
            v-permission="'system:rd-automation-inventory:select-uncovered'"
            action="batch"
            @click="selectUncoveredPages"
          >
            勾选未覆盖
          </StandardButton>
          <StandardButton
            v-permission="'system:rd-automation-inventory:generate-scaffold'"
            action="confirm"
            @click="generateUncoveredInventoryScenarios"
          >
            一键补齐脚手架
          </StandardButton>
        </StandardActionGroup>
      </PanelCard>

      <PanelCard title="下一步建议" description="页面盘点完成后，再转入模板或计划编排。">
        <ul class="inventory-list">
          <li>盘点清单准备好后，进入场景模板选择最贴近页面职责的模板。</li>
          <li>若本轮只需快速补齐缺口，可直接用这里生成页面冒烟脚手架。</li>
          <li>正式断言和步骤维护统一下沉到计划编排处理。</li>
        </ul>
      </PanelCard>
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
import AutomationManualPageDrawer from '@/components/AutomationManualPageDrawer.vue';
import AutomationPageDiscoveryPanel from '@/components/AutomationPageDiscoveryPanel.vue';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import StandardActionGroup from '@/components/StandardActionGroup.vue';
import StandardButton from '@/components/StandardButton.vue';
import { useAutomationInventoryWorkbench } from '@/composables/useAutomationInventoryWorkbench';

const {
  inventoryTableRef,
  inventoryMetrics,
  inventorySourceText,
  pageInventory,
  buildInventorySourceLabel,
  buildTemplateLabel,
  isRouteCovered,
  handleInventorySelectionChange,
  refreshPageInventory,
  selectUncoveredPages,
  generateSelectedInventoryScenarios,
  generateUncoveredInventoryScenarios,
  openManualPageDialog,
  removeManualPage,
  showManualPageDialog,
  scopeOptions,
  inventoryTemplateOptions,
  saveManualPage
} = useAutomationInventoryWorkbench();
</script>

<style scoped>
.automation-inventory-workspace-section {
  display: grid;
  gap: 1rem;
}

.inventory-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.inventory-summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.inventory-list {
  margin: 0.9rem 0;
  padding-left: 1.15rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .inventory-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
