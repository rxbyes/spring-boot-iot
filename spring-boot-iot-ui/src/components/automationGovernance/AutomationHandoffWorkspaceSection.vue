<template>
  <div class="automation-handoff-workspace-section">
    <section class="tri-grid">
      <PanelCard title="交付概况" description="交付打包只消费当前计划，不再回写新的计划结构。">
        <div class="quad-grid handoff-metrics">
          <MetricCard
            v-for="metric in planMetrics"
            :key="metric.label"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
            size="compact"
          />
        </div>
      </PanelCard>

      <PanelCard title="执行建议" description="交付时统一提供命令、范围和阻断口径。">
        <div class="command-box">
          <code>{{ commandPreview }}</code>
        </div>
        <ul class="handoff-list">
          <li v-for="item in executionAdvice" :key="item">{{ item }}</li>
        </ul>
        <StandardActionGroup gap="sm">
          <StandardButton
            v-permission="'system:rd-automation-handoff:copy-command'"
            action="confirm"
            @click="copyCommand"
          >
            复制命令
          </StandardButton>
          <StandardButton
            v-permission="'system:rd-automation-handoff:export-plan'"
            action="batch"
            @click="downloadPlan"
          >
            导出计划
          </StandardButton>
        </StandardActionGroup>
      </PanelCard>

      <PanelCard title="交付备注" description="把计划名、输出路径和问题文档一起整理给下游执行方。">
        <ul class="handoff-list">
          <li v-for="item in deliveryNotes" :key="item">{{ item }}</li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <PanelCard title="交付清单" description="优先展示当前计划里最需要被确认的核心场景。">
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前场景 ${scenarioPreviews.length} 个`,
            `重点展示 ${focusScenarios.length} 个`
          ]"
        />
        <el-table :data="focusScenarios" size="small" border>
          <StandardTableTextColumn prop="name" label="场景" :min-width="180" />
          <StandardTableTextColumn prop="scope" label="范围" :width="110" />
          <StandardTableTextColumn prop="stepCount" label="步骤" :width="90" />
          <StandardTableTextColumn prop="apiCount" label="接口" :width="90" />
        </el-table>
      </PanelCard>

      <ResponsePanel
        title="交付快照"
        description="交付打包保留计划摘要，便于转给执行配置和验收负责人。"
        :body="handoffSummary"
      />
    </section>

    <section>
      <PanelCard title="当前风险与建议" description="交付时仍需把计划层面的提醒同步给执行方。">
        <div v-if="suggestions.length === 0" class="empty-block">
          当前没有新增计划建议，可直接进入执行配置组织回归。
        </div>
        <ul v-else class="suggestion-list">
          <li v-for="item in suggestions" :key="`${item.level}-${item.title}`" class="suggestion-list__item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.detail }}</p>
          </li>
        </ul>
      </PanelCard>
    </section>
  </div>
</template>

<script setup lang="ts">
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import ResponsePanel from '@/components/ResponsePanel.vue';
import StandardActionGroup from '@/components/StandardActionGroup.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import { useAutomationHandoffWorkbench } from '@/composables/useAutomationHandoffWorkbench';

const {
  planMetrics,
  scenarioPreviews,
  suggestions,
  commandPreview,
  executionAdvice,
  deliveryNotes,
  handoffSummary,
  focusScenarios,
  copyCommand,
  downloadPlan
} = useAutomationHandoffWorkbench();
</script>

<style scoped>
.automation-handoff-workspace-section {
  display: grid;
  gap: 1rem;
}

.handoff-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.handoff-list,
.suggestion-list {
  margin: 0.9rem 0 0;
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
  .handoff-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
