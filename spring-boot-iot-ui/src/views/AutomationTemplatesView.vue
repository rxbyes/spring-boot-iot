<template>
  <StandardPageShell class="automation-templates-view">
    <StandardWorkbenchPanel
      title="场景模板台"
      description="只负责模板起步与复用建议，不直接承载正式计划编辑。"
      show-notices
    >
      <template #notices>
        <div class="automation-chip-list">
          <span>页面冒烟</span>
          <span>表单提交</span>
          <span>列表详情</span>
          <span>模板复用</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="模板概况" description="先从稳定模板起步，再转入计划编排台做精细调整。">
          <div class="quad-grid template-metrics">
            <MetricCard
              v-for="metric in templateMetrics"
              :key="metric.label"
              :label="metric.label"
              :value="metric.value"
              :badge="metric.badge"
              size="compact"
            />
          </div>
        </PanelCard>

        <PanelCard title="当前使用" description="快速查看各类模板在当前计划中的使用情况。">
          <ul class="template-usage-list">
            <li v-for="item in templateUsage" :key="item.type" class="template-usage-list__item">
              <strong>{{ item.title }}</strong>
              <span>{{ item.count }} 个场景</span>
            </li>
          </ul>
        </PanelCard>

        <PanelCard title="下一步建议" description="模板起步后应尽快转到计划编排台补齐步骤与断言。">
          <ul class="template-note-list">
            <li>模板页优先解决“从哪里开始写”的问题，而不是完成正式计划。</li>
            <li>新增模板后可直接进入计划编排台调整选择器、接口匹配和截图断言。</li>
            <li>交付说明、执行命令与基线备注统一下沉到交付打包台。</li>
          </ul>
        </PanelCard>
      </section>

      <section class="template-grid">
        <PanelCard title="页面冒烟模板" description="适合为新页面快速建立最小可执行脚手架。">
          <ul class="template-focus-list">
            <li>页面进入</li>
            <li>就绪断言</li>
            <li>截图基线</li>
          </ul>
          <StandardButton action="add" v-permission="'system:rd-automation-templates:add-page-smoke'" @click="addScenario('pageSmoke')">新增页面冒烟模板</StandardButton>
        </PanelCard>

        <PanelCard title="表单提交模板" description="适合新增或改造表单页的研发自测起步。">
          <ul class="template-focus-list">
            <li>表单填写</li>
            <li>提交动作</li>
            <li>接口回执</li>
          </ul>
          <StandardButton action="add" v-permission="'system:rd-automation-templates:add-form-submit'" @click="addScenario('formSubmit')">新增表单提交模板</StandardButton>
        </PanelCard>

        <PanelCard title="列表详情模板" description="适合列表查询、详情抽屉与行级动作页面。">
          <ul class="template-focus-list">
            <li>列表筛选</li>
            <li>行级动作</li>
            <li>详情抽屉</li>
          </ul>
          <StandardButton action="add" v-permission="'system:rd-automation-templates:add-list-detail'" @click="addScenario('listDetail')">新增列表详情模板</StandardButton>
        </PanelCard>
      </section>

      <section>
        <PanelCard title="最近计划场景" description="这里仅用于确认模板投放后的当前计划状态。">
          <StandardTableToolbar
            compact
            :meta-items="[
              `当前场景 ${latestScenarioPreviews.length} 个`,
              `模板入口 ${templateUsage.length} 个`
            ]"
          />
          <el-table :data="latestScenarioPreviews" size="small" border>
            <StandardTableTextColumn prop="name" label="场景" :min-width="180" />
            <StandardTableTextColumn prop="route" label="页面路由" :min-width="180" />
            <StandardTableTextColumn prop="scope" label="范围" :width="110" />
            <StandardTableTextColumn prop="stepCount" label="步骤" :width="90" />
          </el-table>
        </PanelCard>
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import StandardButton from '../components/StandardButton.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardTableTextColumn from '../components/StandardTableTextColumn.vue';
import StandardTableToolbar from '../components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useAutomationTemplateWorkbench } from '../composables/useAutomationTemplateWorkbench';

const {
  templateMetrics,
  templateUsage,
  latestScenarioPreviews,
  addScenario
} = useAutomationTemplateWorkbench();
</script>

<style scoped>
.automation-templates-view {
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

.template-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.95rem;
}

.template-usage-list,
.template-note-list,
.template-focus-list {
  margin: 0;
  padding-left: 1.15rem;
  line-height: 1.8;
  color: var(--text-secondary);
}

.template-usage-list {
  list-style: none;
  padding-left: 0;
}

.template-usage-list__item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 0;
  border-bottom: 1px solid var(--line-soft);
}

.template-usage-list__item:last-child {
  border-bottom: none;
}

@media (max-width: 1024px) {
  .template-grid,
  .template-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
