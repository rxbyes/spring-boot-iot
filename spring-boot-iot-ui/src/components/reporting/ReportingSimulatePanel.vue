<template>
  <PanelCard class="reporting-workspace-card reporting-workspace-card--simulate">
    <template #header>
      <div class="reporting-workspace-card__header">
        <div class="reporting-workspace-card__heading">
          <p class="reporting-workspace-card__eyebrow">SIMULATION LAB</p>
          <h2 class="reporting-workspace-card__title">模拟上报</h2>
          <p class="reporting-workspace-card__description">
            按设备编码加载接入契约后，完成 HTTP / MQTT 双通道模拟上报。
          </p>
        </div>
        <span class="reporting-workspace-card__badge">设备联调</span>
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

.reporting-workspace-card__eyebrow {
  margin: 0 0 0.28rem;
  color: var(--brand);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
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

.reporting-workspace-card__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.34rem 0.68rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
}
</style>
