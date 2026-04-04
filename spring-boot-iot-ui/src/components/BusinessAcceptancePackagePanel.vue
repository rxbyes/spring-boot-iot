<template>
  <PanelCard title="预置验收包" description="按交付清单选择业务验收包，聚焦验收人员、产品和项目经理可直接理解的模块范围。">
    <StandardInlineState v-if="errorMessage" tone="error" :message="errorMessage" />
    <div v-else-if="packages.length === 0" class="business-acceptance-package-panel__empty">
      暂无可用业务验收包。
    </div>
    <div v-else class="business-acceptance-package-panel__grid">
      <button
        v-for="item in packages"
        :key="item.packageCode"
        type="button"
        class="business-acceptance-package-panel__card"
        :class="{
          'business-acceptance-package-panel__card--active': item.packageCode === selectedPackageCode
        }"
        @click="$emit('select-package', item.packageCode)"
      >
        <div class="business-acceptance-package-panel__header">
          <strong>{{ item.packageName }}</strong>
          <span>{{ item.modules.length }} 个模块</span>
        </div>
        <p class="business-acceptance-package-panel__description">
          {{ item.description || '围绕业务交付主链路给出通过或未通过结论。' }}
        </p>
        <div class="business-acceptance-package-panel__meta">
          <span v-for="role in item.targetRoles" :key="role">{{ role }}</span>
        </div>
      </button>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import type { BusinessAcceptancePackage } from '@/types/businessAcceptance';
import PanelCard from './PanelCard.vue';
import StandardInlineState from './StandardInlineState.vue';

defineProps<{
  packages: BusinessAcceptancePackage[];
  selectedPackageCode: string;
  errorMessage?: string;
}>();

defineEmits<{
  (event: 'select-package', packageCode: string): void;
}>();
</script>

<style scoped>
.business-acceptance-package-panel__grid {
  display: grid;
  gap: 0.8rem;
}

.business-acceptance-package-panel__card {
  display: grid;
  gap: 0.7rem;
  width: 100%;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-xl);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.96)),
    radial-gradient(circle at top right, rgba(30, 128, 255, 0.08), transparent 52%);
  text-align: left;
  transition:
    border-color var(--transition-base),
    transform var(--transition-base),
    box-shadow var(--transition-base);
}

.business-acceptance-package-panel__card:hover {
  border-color: color-mix(in srgb, var(--brand) 28%, var(--panel-border));
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.business-acceptance-package-panel__card--active {
  border-color: color-mix(in srgb, var(--brand) 38%, var(--panel-border));
  box-shadow: var(--shadow-brand);
}

.business-acceptance-package-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.business-acceptance-package-panel__header strong {
  color: var(--text-heading);
  font-size: 1rem;
}

.business-acceptance-package-panel__header span {
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.business-acceptance-package-panel__description {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.business-acceptance-package-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.business-acceptance-package-panel__meta span {
  padding: 0.28rem 0.65rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 7%, white);
  color: var(--brand-deep);
  font-size: 0.78rem;
  font-weight: 600;
}

.business-acceptance-package-panel__empty {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}
</style>
