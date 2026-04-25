<template>
  <div class="automation-assets-workspace-section">
    <nav class="automation-assets-workspace-section__tabs" aria-label="资产工作区切换">
      <button
        v-for="item in tabs"
        :key="item.key"
        type="button"
        class="automation-assets-workspace-section__tab"
        :class="{ 'automation-assets-workspace-section__tab--active': item.key === activeTab }"
        @click="$emit('select-tab', item.key)"
      >
        {{ item.label }}
      </button>
    </nav>

    <AutomationInventoryWorkspaceSection v-if="activeTab === 'inventory'" />
    <AutomationTemplatesWorkspaceSection v-else-if="activeTab === 'templates'" />
    <AutomationPlansWorkspaceSection v-else-if="activeTab === 'plans'" />
    <AutomationHandoffWorkspaceSection v-else />
  </div>
</template>

<script setup lang="ts">
import type { AutomationGovernanceAssetTab } from '@/utils/automationGovernance';
import AutomationHandoffWorkspaceSection from './AutomationHandoffWorkspaceSection.vue';
import AutomationInventoryWorkspaceSection from './AutomationInventoryWorkspaceSection.vue';
import AutomationPlansWorkspaceSection from './AutomationPlansWorkspaceSection.vue';
import AutomationTemplatesWorkspaceSection from './AutomationTemplatesWorkspaceSection.vue';

defineProps<{
  activeTab: AutomationGovernanceAssetTab;
  tabs: Array<{
    key: AutomationGovernanceAssetTab;
    label: string;
  }>;
}>();

defineEmits<{
  (event: 'select-tab', value: AutomationGovernanceAssetTab): void;
}>();
</script>

<style scoped>
.automation-assets-workspace-section {
  display: grid;
  gap: 1rem;
}

.automation-assets-workspace-section__tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.85rem;
}

.automation-assets-workspace-section__tab {
  display: grid;
  gap: 0.4rem;
  align-content: start;
  min-height: 100%;
  padding: 1rem;
  border-radius: var(--radius-xl);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
  color: inherit;
  text-align: left;
  transition:
    border-color var(--transition-base),
    box-shadow var(--transition-base),
    transform var(--transition-base);
}

.automation-assets-workspace-section__tab--active,
.automation-assets-workspace-section__tab:hover {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  box-shadow: var(--shadow-card-soft);
  transform: translateY(-1px);
}

.automation-assets-workspace-section__tab--active {
  background: color-mix(in srgb, var(--brand) 5%, white);
}

@media (max-width: 1024px) {
  .automation-assets-workspace-section__tabs {
    grid-template-columns: 1fr;
  }
}
</style>
