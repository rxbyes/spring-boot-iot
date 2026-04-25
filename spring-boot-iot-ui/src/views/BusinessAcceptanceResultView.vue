<template>
  <StandardPageShell class="business-acceptance-result-view">
    <StandardWorkbenchPanel
      :title="pageTitle"
      :description="pageDescription"
      show-notices
    >
      <template #notices>
        <div class="business-acceptance-result-view__chips">
          <span v-for="chip in noticeChips" :key="chip">{{ chip }}</span>
        </div>
      </template>

      <StandardInlineState
        v-if="resultErrorMessage"
        tone="error"
        :message="resultErrorMessage"
      />

      <div v-else-if="resultLoading" class="business-acceptance-result-view__loading">
        {{ loadingMessage }}
      </div>

      <template v-else-if="result">
        <section>
          <BusinessAcceptanceResultSummaryPanel
            :status="result.status"
            :passed-module-count="result.passedModuleCount"
            :failed-module-count="result.failedModuleCount"
            :failed-module-names="result.failedModuleNames"
            :duration-text="result.durationText"
            :show-evidence-action="canOpenAutomationEvidence"
            @open-automation-results="goToAutomationEvidence(result.runId)"
          />
        </section>

        <section class="business-acceptance-result-view__modules">
          <BusinessAcceptanceModuleResultPanel
            :modules="result.modules"
            :active-module-code="activeModuleCode"
            @select-module="activeModuleCode = $event"
          />
        </section>
      </template>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import BusinessAcceptanceModuleResultPanel from '../components/BusinessAcceptanceModuleResultPanel.vue';
import BusinessAcceptanceResultSummaryPanel from '../components/BusinessAcceptanceResultSummaryPanel.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardPageShell from '../components/StandardPageShell.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { useBusinessAcceptanceWorkbench } from '../composables/useBusinessAcceptanceWorkbench';
import { usePermissionStore } from '../stores/permission';

const pageTitle = '\u4e1a\u52a1\u9a8c\u6536\u7ed3\u679c';
const pageDescription =
  '\u7ed3\u679c\u9996\u5c4f\u76f4\u63a5\u56de\u7b54\u662f\u5426\u901a\u8fc7\u3001\u54ea\u4e9b\u6a21\u5757\u6ca1\u8fc7\uff0c\u5e76\u53ef\u4ee5\u4e00\u952e\u5c55\u5f00\u5177\u4f53\u5931\u8d25\u6b65\u9aa4\u3001\u63a5\u53e3\u548c\u9875\u9762\u52a8\u4f5c\u3002';
const noticeChips = [
  '\u662f\u5426\u901a\u8fc7',
  '\u54ea\u4e9b\u6a21\u5757\u6ca1\u8fc7',
  '\u5931\u8d25\u6b65\u9aa4 / \u63a5\u53e3 / \u9875\u9762\u52a8\u4f5c',
  '\u8df3\u8f6c\u5230\u81ea\u52a8\u5316\u6cbb\u7406\u53f0\u7ed3\u679c\u8bc1\u636e'
];
const loadingMessage = '\u6b63\u5728\u52a0\u8f7d\u4e1a\u52a1\u9a8c\u6536\u7ed3\u679c...';

const route = useRoute();
const permissionStore = usePermissionStore();
const {
  result,
  resultLoading,
  resultErrorMessage,
  activeModuleCode,
  loadResultFromRoute,
  goToAutomationEvidence
} = useBusinessAcceptanceWorkbench();

const canOpenAutomationEvidence = computed(() =>
  permissionStore.hasPermission('system:business-acceptance:open-result')
  && permissionStore.hasRoutePermission('/automation-governance')
);

onMounted(() => {
  void loadResultFromRoute();
});

watch(
  () => [route.params.runId, route.query.packageCode],
  () => {
    void loadResultFromRoute();
  }
);
</script>

<style scoped>
.business-acceptance-result-view {
  min-width: 0;
}

.business-acceptance-result-view__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.business-acceptance-result-view__chips span {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 6%, white);
  border: 1px solid color-mix(in srgb, var(--brand) 12%, white);
  color: var(--text-primary);
  font-size: 0.88rem;
}

.business-acceptance-result-view__loading {
  padding: 1rem;
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.business-acceptance-result-view__modules {
  margin-top: 0.95rem;
}
</style>
