<template>
  <StandardFormDrawer
    :model-value="modelValue"
    title="维护绑定"
    subtitle="查看正式绑定摘要，并继续维护设备与测点关系。"
    size="42rem"
    @update:model-value="handleModelValueChange"
    @close="handleClose"
  >
    <div class="risk-point-binding-maintenance-drawer">
      <div class="ops-drawer-note">
        <strong>维护说明</strong>
        <span>这里维护的是风险点与设备、测点之间的正式绑定关系，不会修改设备主档、产品定义或厂家侧原始资料。</span>
      </div>

      <div v-if="!riskPoint" class="standard-list-empty-state">
        <EmptyState title="请选择风险点" description="从列表行内进入后，可在此查看正式绑定摘要，并继续维护设备与测点关系。" />
      </div>

      <section v-else class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <div>
            <h3>{{ riskPoint.riskPointName || '未命名风险点' }}</h3>
            <p>{{ riskPoint.riskPointCode || '尚未生成风险点编号' }}</p>
          </div>
        </div>
        <div class="risk-point-binding-maintenance-drawer__summary">
          <span>{{ summary?.boundDeviceCount ?? 0 }} 台设备</span>
          <span>{{ summary?.boundMetricCount ?? 0 }} 个测点</span>
          <span>待治理 {{ summary?.pendingBindingCount ?? 0 }} 条</span>
        </div>
      </section>
    </div>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue';
import StandardFormDrawer from '@/components/StandardFormDrawer.vue';
import type { RiskPoint, RiskPointBindingSummary } from '@/api/riskPoint';

defineProps<{
  modelValue: boolean;
  riskPoint?: RiskPoint | null;
  summary?: RiskPointBindingSummary | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  close: [];
}>();

const handleModelValueChange = (value: boolean) => {
  emit('update:modelValue', value);
};

const handleClose = () => {
  emit('close');
};
</script>

<style scoped>
.risk-point-binding-maintenance-drawer {
  display: grid;
  gap: 1rem;
}

.risk-point-binding-maintenance-drawer__summary {
  display: grid;
  gap: 0.5rem;
  color: var(--text-secondary);
}

.risk-point-binding-maintenance-drawer__summary span {
  color: var(--text-primary);
}
</style>
