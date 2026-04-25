<template>
  <StandardPageShell class="business-acceptance-workbench">
    <StandardWorkbenchPanel
      :title="pageTitle"
      :description="pageDescription"
      show-notices
    >
      <template #notices>
        <div class="business-acceptance-workbench__chips">
          <span v-for="chip in noticeChips" :key="chip">{{ chip }}</span>
        </div>
      </template>

      <section class="business-acceptance-workbench__grid">
        <BusinessAcceptancePackagePanel
          :packages="packages"
          :selected-package-code="selectedPackageCode"
          :error-message="initialErrorMessage"
          @select-package="selectedPackageCode = $event"
        />

        <BusinessAcceptanceRunConfigPanel
          :environment-options="environmentOptions"
          :account-templates="availableAccountTemplates"
          :module-options="selectedPackageModules"
          :selected-environment="selectedEnvironment"
          :selected-account-template="selectedAccountTemplate"
          :selected-module-codes="selectedModuleCodes"
          :latest-result="selectedLatestResult"
          :launching="launching"
          :launch-error-message="launchErrorMessage"
          :run-status="runStatus"
          @update:environment="selectedEnvironment = $event"
          @update:account-template="selectedAccountTemplate = $event"
          @update:module-codes="selectedModuleCodes = $event"
          @launch="launchSelectedPackage"
        />
      </section>

      <section class="business-acceptance-workbench__summary-block">
        <PanelCard :title="latestPanelTitle" :description="latestPanelDescription">
          <div class="business-acceptance-workbench__metrics">
            <MetricCard
              size="compact"
              :label="latestStatusLabel"
              :value="latestStatusText"
              :badge="{ label: latestStatusBadge.label, tone: latestStatusBadge.tone }"
            />
            <MetricCard
              size="compact"
              :label="passedModulesLabel"
              :value="String(selectedLatestResult?.passedModuleCount ?? 0)"
              :badge="{ label: 'Pass', tone: 'success' }"
            />
            <MetricCard
              size="compact"
              :label="failedModulesLabel"
              :value="String(selectedLatestResult?.failedModuleCount ?? 0)"
              :badge="{
                label: (selectedLatestResult?.failedModuleCount ?? 0) > 0 ? 'Fail' : 'Clean',
                tone: (selectedLatestResult?.failedModuleCount ?? 0) > 0 ? 'danger' : 'brand'
              }"
            />
          </div>

          <StandardInlineState :tone="latestSummaryTone" :message="latestSummaryMessage" />

          <div class="business-acceptance-workbench__summary-actions">
            <StandardButton
              v-permission="'system:business-acceptance:open-result'"
              action="query"
              :disabled="!selectedLatestResult?.runId"
              @click="openLatestRun"
            >
              {{ openResultsCenterLabel }}
            </StandardButton>
          </div>
        </PanelCard>
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import BusinessAcceptancePackagePanel from '../components/BusinessAcceptancePackagePanel.vue';
import BusinessAcceptanceRunConfigPanel from '../components/BusinessAcceptanceRunConfigPanel.vue';
import MetricCard from '../components/MetricCard.vue';
import PanelCard from '../components/PanelCard.vue';
import StandardButton from '../components/StandardButton.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useBusinessAcceptanceWorkbench } from '../composables/useBusinessAcceptanceWorkbench';

const pageTitle = '\u4e1a\u52a1\u9a8c\u6536\u53f0';
const pageDescription =
  '\u6309\u4ea4\u4ed8\u6e05\u5355\u9009\u62e9\u9884\u7f6e\u9a8c\u6536\u5305\uff0c\u53ea\u4fdd\u7559\u73af\u5883\u3001\u8d26\u53f7\u6a21\u677f\u548c\u6a21\u5757\u8303\u56f4\u4e09\u9879\u8f7b\u914d\u7f6e\uff0c\u8ba9\u4e1a\u52a1\u4eba\u5458\u4e5f\u80fd\u4e00\u952e\u53d1\u8d77\u9a8c\u6536\u3002';
const noticeChips = [
  '\u4ea4\u4ed8\u6e05\u5355',
  '\u73af\u5883 / \u8d26\u53f7\u6a21\u677f / \u6a21\u5757\u8303\u56f4',
  '\u662f\u5426\u901a\u8fc7',
  '\u54ea\u4e9b\u6a21\u5757\u6ca1\u8fc7'
];
const latestPanelTitle = '\u6700\u8fd1\u4e00\u6b21\u7ed3\u8bba';
const latestPanelDescription =
  '\u9996\u5c4f\u76f4\u63a5\u7ed9\u4e1a\u52a1\u4eba\u5458\u6700\u8fd1\u4e00\u6b21\u7ed3\u679c\u7ed3\u8bba\uff0c\u5e76\u4fdd\u7559\u8df3\u8f6c\u5230\u5e95\u5c42\u7ed3\u679c\u4e2d\u5fc3\u7684\u80fd\u529b\u3002';
const latestStatusLabel = '\u6700\u8fd1\u4e00\u6b21\u72b6\u6001';
const passedModulesLabel = '\u901a\u8fc7\u6a21\u5757';
const failedModulesLabel = '\u672a\u8fc7\u6a21\u5757';
const openResultsCenterLabel = '\u8fdb\u5165\u7ed3\u679c\u4e0e\u57fa\u7ebf\u4e2d\u5fc3';

const {
  packages,
  initialErrorMessage,
  selectedPackageCode,
  selectedEnvironment,
  selectedAccountTemplate,
  selectedModuleCodes,
  selectedPackage,
  selectedPackageModules,
  selectedLatestResult,
  environmentOptions,
  availableAccountTemplates,
  launching,
  launchErrorMessage,
  runStatus,
  loadInitialData,
  launchSelectedPackage,
  goToAutomationResults
} = useBusinessAcceptanceWorkbench();

const latestSummaryTone = computed<'info' | 'error'>(() => {
  return (selectedLatestResult.value?.failedModuleCount ?? 0) > 0 ? 'error' : 'info';
});

const latestStatusText = computed(() => {
  if (!selectedLatestResult.value || selectedLatestResult.value.status === 'neverRun') {
    return '\u672a\u8fd0\u884c';
  }
  if (selectedLatestResult.value.status === 'passed') {
    return '\u901a\u8fc7';
  }
  if (selectedLatestResult.value.status === 'blocked') {
    return '\u963b\u585e';
  }
  return '\u672a\u901a\u8fc7';
});

const latestStatusBadge = computed(() => {
  if (!selectedLatestResult.value || selectedLatestResult.value.status === 'neverRun') {
    return { label: 'New', tone: 'brand' as const };
  }
  if (selectedLatestResult.value.status === 'passed') {
    return { label: 'Pass', tone: 'success' as const };
  }
  if (selectedLatestResult.value.status === 'blocked') {
    return { label: 'Blocked', tone: 'warning' as const };
  }
  return { label: 'Fail', tone: 'danger' as const };
});

const latestSummaryMessage = computed(() => {
  if (initialErrorMessage.value) {
    return initialErrorMessage.value;
  }
  if (!selectedPackage.value) {
    return '\u8bf7\u5148\u9009\u62e9\u4e00\u4e2a\u4e1a\u52a1\u9a8c\u6536\u5305\u3002';
  }
  if (!selectedLatestResult.value || selectedLatestResult.value.status === 'neverRun') {
    return `\u5f53\u524d\u9a8c\u6536\u5305\u201c${selectedPackage.value.packageName}\u201d\u8fd8\u6ca1\u6709\u5386\u53f2\u7ed3\u679c\uff0c\u53ef\u4ee5\u76f4\u63a5\u53d1\u8d77\u7b2c\u4e00\u6b21\u4e1a\u52a1\u9a8c\u6536\u3002`;
  }
  if ((selectedLatestResult.value.failedModuleCount ?? 0) > 0) {
    return `\u6700\u8fd1\u4e00\u6b21\u672a\u901a\u8fc7\u6a21\u5757\uff1a${selectedLatestResult.value.failedModuleNames.join('\u3001')}\u3002`;
  }
  return `\u6700\u8fd1\u4e00\u6b21\u201c${selectedPackage.value.packageName}\u201d\u5df2\u5168\u90e8\u901a\u8fc7\u3002`;
});

async function openLatestRun() {
  const runId = selectedLatestResult.value?.runId;
  if (!runId) {
    return;
  }
  await goToAutomationResults(runId);
}

onMounted(() => {
  void loadInitialData();
});
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
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 0.95rem;
}

.business-acceptance-workbench__summary-block {
  margin-top: 0.95rem;
}

.business-acceptance-workbench__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.business-acceptance-workbench__summary-actions {
  margin-top: 1rem;
}

@media (max-width: 1100px) {
  .business-acceptance-workbench__grid,
  .business-acceptance-workbench__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
