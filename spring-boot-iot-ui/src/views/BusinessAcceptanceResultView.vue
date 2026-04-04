<template>
  <StandardPageShell class="business-acceptance-result">
    <StandardWorkbenchPanel
      title="业务验收结果"
      description="业务结果页会优先显示是否通过、未通过模块和失败明细下钻入口。"
      show-notices
    >
      <template #notices>
        <div class="business-acceptance-result__chips">
          <span>是否通过</span>
          <span>失败模块</span>
          <span>失败明细</span>
        </div>
      </template>

      <section class="business-acceptance-result__grid">
        <PanelCard title="结果摘要" description="首屏将直接给出业务结论。">
          <p class="business-acceptance-result__summary">
            Task 1 先建立结果页路由，后续会补齐模块结论、失败步骤、关联接口和页面动作。
          </p>
        </PanelCard>

        <PanelCard title="下钻入口" description="需要时仍可跳转到底层结果中心查看证据。">
          <RouterLink to="/automation-results" class="business-acceptance-result__link">
            进入结果与基线中心
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
.business-acceptance-result {
  min-width: 0;
}

.business-acceptance-result__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.business-acceptance-result__chips span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 6%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 12%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.business-acceptance-result__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.95rem;
}

.business-acceptance-result__summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.business-acceptance-result__link {
  display: inline-flex;
  color: var(--brand);
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .business-acceptance-result__grid {
    grid-template-columns: 1fr;
  }
}
</style>
