<template>
  <StandardPageShell class="business-acceptance-workbench">
    <StandardWorkbenchPanel
      title="业务验收台"
      description="按交付清单选择预置验收包，并以环境、账号模板和模块范围三类轻配置发起一键验收。"
      show-notices
    >
      <template #notices>
        <div class="business-acceptance-workbench__chips">
          <span>交付清单</span>
          <span>一键执行</span>
          <span>模块结论</span>
        </div>
      </template>

      <section class="business-acceptance-workbench__grid">
        <PanelCard title="预置验收包" description="Phase 1 先冻结四类业务验收包。">
          <!-- <BusinessAcceptancePackagePanel /> -->
          <ul class="business-acceptance-workbench__list">
            <li>产品与设备</li>
            <li>设备上报与链路</li>
            <li>风险运营闭环</li>
            <li>平台治理基础</li>
          </ul>
        </PanelCard>

        <PanelCard title="运行前配置" description="业务用户仅选择环境、账号模板和模块范围。">
          <!-- <BusinessAcceptanceRunConfigPanel /> -->
          <ul class="business-acceptance-workbench__list">
            <li>环境</li>
            <li>账号模板</li>
            <li>模块范围</li>
          </ul>
        </PanelCard>

        <PanelCard title="结果导向" description="结果页首屏将直接回答是否通过、哪些模块没过。">
          <p class="business-acceptance-workbench__summary">
            当前为 Task 1 占位页，Task 6 会补齐真实组件、执行编排与结果摘要。
          </p>
          <RouterLink to="/automation-results" class="business-acceptance-workbench__link">
            查看现有结果中心
          </RouterLink>
        </PanelCard>
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';

import PanelCard from '../components/PanelCard.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
</script>

<style scoped>
.business-acceptance-workbench {
  min-width: 0;
}

.business-acceptance-workbench__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.business-acceptance-workbench__chips span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 6%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 12%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.business-acceptance-workbench__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.95rem;
}

.business-acceptance-workbench__list {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.business-acceptance-workbench__summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.business-acceptance-workbench__link {
  display: inline-flex;
  margin-top: 0.95rem;
  color: var(--brand);
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .business-acceptance-workbench__grid {
    grid-template-columns: 1fr;
  }
}
</style>
