<template>
  <PanelCard class="reporting-workspace-card reporting-workspace-card--replay">
    <template #header>
      <div class="reporting-workspace-card__header">
        <div class="reporting-workspace-card__heading">
          <h2 class="reporting-workspace-card__title">结果复盘</h2>
          <p class="reporting-workspace-card__description">
            聚合时间线、实际发送内容和最近一次页面响应，优先表达本轮验证结论。
          </p>
        </div>
      </div>
    </template>

    <slot />
  </PanelCard>
</template>

<script setup lang="ts">
import PanelCard from '@/components/PanelCard.vue';
</script>

<style scoped>
.reporting-workspace-card {
  min-width: 0;
}

.reporting-workspace-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
}

.reporting-workspace-card__heading {
  min-width: 0;
}

.reporting-workspace-card__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.08rem;
}

.reporting-workspace-card__description {
  margin: 0.42rem 0 0;
  color: var(--text-secondary);
  line-height: 1.68;
}
</style>
