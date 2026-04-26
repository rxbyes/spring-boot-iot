<template>
  <div class="automation-execution-workspace-section">
    <section class="tri-grid">
      <PanelCard title="执行概况" description="先统一计划范围和阻断策略，再组织正式回归。">
        <div class="quad-grid execution-metrics">
          <MetricCard
            v-for="metric in executionMetrics"
            :key="metric.label"
            class="execution-metrics__card"
            :label="metric.label"
            :value="metric.value"
            :badge="metric.badge"
          />
        </div>
      </PanelCard>

      <PanelCard title="执行命令" description="执行配置只负责命令预览与口径校准，不再承载资产编辑。">
        <div class="command-box">
          <code>{{ commandPreview }}</code>
        </div>
        <ul class="phase-ideas">
          <li>场景计划继续在资产编排里维护，这里只负责组织执行。</li>
          <li>统一注册表决定阻断等级、依赖关系和执行器类型。</li>
          <li>建议先校准范围与账号，再交给既有 `scripts/auto` 执行。</li>
        </ul>
        <StandardActionGroup gap="sm">
          <StandardButton
            v-permission="'system:automation-governance:execution:copy-command'"
            action="confirm"
            @click="copyCommand"
          >
            复制命令
          </StandardButton>
        </StandardActionGroup>
      </PanelCard>

      <PanelCard title="阻断关注" description="优先关注发布阻断项，避免回归结果与注册表口径漂移。">
        <div v-if="registryBlockers.length === 0" class="empty-block">
          当前注册表未标记 blocker 场景。
        </div>
        <ul v-else class="highlight-list">
          <li v-for="scenario in registryBlockers" :key="scenario.id" class="highlight-list__item">
            <div class="highlight-list__header">
              <strong>{{ scenario.title }}</strong>
              <code>{{ scenario.id }}</code>
            </div>
            <p>{{ scenario.scope }} / {{ scenario.runnerType }}</p>
            <p>{{ scenario.docRef }}</p>
          </li>
        </ul>
      </PanelCard>
    </section>

    <section class="two-column-grid">
      <AutomationExecutionConfigPanel :target="plan.target" :scope-options="scopeOptions" />
      <AutomationRegistryPanel
        :scenarios="registryScenarios"
        :summary="registrySummary"
      />
    </section>

    <section class="two-column-grid">
      <PanelCard title="范围校准" description="并列查看执行范围与失败阻断范围，避免执行口径失焦。">
        <ul class="scope-list">
          <li v-for="item in scopeBreakdown" :key="item.scope" class="scope-list__item">
            <div class="scope-list__header">
              <strong>{{ item.scope }}</strong>
              <span>{{ item.scenarioCount }} 个计划场景</span>
            </div>
            <div class="scope-list__chips">
              <span :class="['scope-chip', item.enabled ? 'scope-chip--enabled' : 'scope-chip--muted']">
                {{ item.enabled ? '纳入执行' : '未纳入执行' }}
              </span>
              <span :class="['scope-chip', item.blocking ? 'scope-chip--blocking' : 'scope-chip--muted']">
                {{ item.blocking ? '失败阻断' : '仅提醒' }}
              </span>
            </div>
          </li>
        </ul>
      </PanelCard>

      <PanelCard title="执行清单" description="快速查看当前计划中每个场景的执行密度。">
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前场景 ${scenarioPreviews.length} 个`,
            `含接口检查 ${scenarioPreviews.filter((item) => item.apiCount > 0).length} 个`
          ]"
        />
        <el-table :data="scenarioPreviews" size="small" border>
          <StandardTableTextColumn prop="name" label="场景" :min-width="160" />
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
    </section>
  </div>
</template>

<script setup lang="ts">
import AutomationExecutionConfigPanel from '@/components/AutomationExecutionConfigPanel.vue';
import AutomationRegistryPanel from '@/components/AutomationRegistryPanel.vue';
import MetricCard from '@/components/MetricCard.vue';
import PanelCard from '@/components/PanelCard.vue';
import StandardActionGroup from '@/components/StandardActionGroup.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import { useAutomationExecutionWorkbench } from '@/composables/useAutomationExecutionWorkbench';

const {
  plan,
  scopeOptions,
  scenarioPreviews,
  registryScenarios,
  registrySummary,
  executionMetrics,
  commandPreview,
  registryBlockers,
  scopeBreakdown,
  copyCommand
} = useAutomationExecutionWorkbench();
</script>

<style scoped>
.automation-execution-workspace-section {
  display: grid;
  gap: 1rem;
}

.execution-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.execution-metrics__card {
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

.phase-ideas {
  margin: 0.9rem 0 0;
  padding-left: 1.1rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.highlight-list,
.scope-list {
  display: grid;
  gap: 0.85rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.highlight-list__item,
.scope-list__item {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.highlight-list__header,
.scope-list__header {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  align-items: center;
  justify-content: space-between;
}

.highlight-list__header strong,
.scope-list__header strong {
  color: var(--text-heading);
}

.highlight-list__item p {
  margin: 0.45rem 0 0;
  color: var(--text-secondary);
}

.scope-list__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.7rem;
}

.scope-chip {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  font-size: 0.88rem;
}

.scope-chip--enabled {
  background: color-mix(in srgb, var(--success, #2f9e44) 10%, white);
  color: color-mix(in srgb, var(--success, #2f9e44) 78%, var(--text-heading));
}

.scope-chip--blocking {
  background: color-mix(in srgb, var(--danger, #d84f45) 10%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 78%, var(--text-heading));
}

.scope-chip--muted {
  background: color-mix(in srgb, var(--text-secondary) 8%, white);
  color: var(--text-secondary);
}

@media (max-width: 1024px) {
  .execution-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
