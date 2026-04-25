<template>
  <PanelCard title="业务验收结论" description="先回答是否通过、哪些模块没过，再决定是否继续下钻到具体失败步骤和底层证据。">
    <div class="business-acceptance-result-summary-panel__metrics">
      <MetricCard
        size="compact"
        label="是否通过"
        :value="statusLabel"
        :badge="{ label: statusBadge.label, tone: statusBadge.tone }"
      />
      <MetricCard
        size="compact"
        label="通过模块"
        :value="String(passedModuleCount)"
        :badge="{ label: 'Pass', tone: 'success' }"
      />
      <MetricCard
        size="compact"
        label="未过模块"
        :value="String(failedModuleCount)"
        :badge="{ label: failedModuleCount ? 'Fail' : 'Clean', tone: failedModuleCount ? 'danger' : 'brand' }"
      />
      <MetricCard
        size="compact"
        label="总耗时"
        :value="durationText || '--'"
        :badge="{ label: 'Time', tone: 'muted' }"
      />
    </div>

    <StandardInlineState :tone="failedModuleCount ? 'error' : 'info'" :message="summaryMessage" />

    <div v-if="failedModuleNames.length > 0" class="business-acceptance-result-summary-panel__failed">
      <strong>未通过模块</strong>
      <div class="business-acceptance-result-summary-panel__chips">
        <span v-for="name in failedModuleNames" :key="name">{{ name }}</span>
      </div>
    </div>

    <div v-if="showEvidenceAction" class="business-acceptance-result-summary-panel__actions">
      <StandardButton v-permission="'system:business-acceptance:open-result'" action="query" @click="$emit('open-automation-results')">
        进入结果证据
      </StandardButton>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { BusinessAcceptanceStatus } from '@/types/businessAcceptance';
import MetricCard from './MetricCard.vue';
import PanelCard from './PanelCard.vue';
import StandardButton from './StandardButton.vue';
import StandardInlineState from './StandardInlineState.vue';

const props = defineProps<{
  status: BusinessAcceptanceStatus;
  passedModuleCount: number;
  failedModuleCount: number;
  failedModuleNames: string[];
  durationText?: string;
  showEvidenceAction?: boolean;
}>();

defineEmits<{
  (event: 'open-automation-results'): void;
}>();

const statusLabel = computed(() => {
  if (props.status === 'passed') {
    return '通过';
  }
  if (props.status === 'blocked') {
    return '阻塞';
  }
  if (props.status === 'failed') {
    return '未通过';
  }
  return props.status || '--';
});

const statusBadge = computed(() => {
  if (props.status === 'passed') {
    return { label: 'Pass', tone: 'success' as const };
  }
  if (props.status === 'blocked') {
    return { label: 'Blocked', tone: 'warning' as const };
  }
  return { label: 'Fail', tone: 'danger' as const };
});

const summaryMessage = computed(() => {
  if (props.failedModuleCount > 0) {
    return `本次业务验收共有 ${props.failedModuleCount} 个模块未通过，请继续展开模块明细查看失败步骤、接口和页面动作。`;
  }
  return `本次业务验收共通过 ${props.passedModuleCount} 个模块，可按需进入自动化治理台查看结果证据。`;
});
</script>

<style scoped>
.business-acceptance-result-summary-panel__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.business-acceptance-result-summary-panel__failed {
  margin-top: 1rem;
  display: grid;
  gap: 0.55rem;
}

.business-acceptance-result-summary-panel__failed strong {
  color: var(--text-heading);
}

.business-acceptance-result-summary-panel__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.business-acceptance-result-summary-panel__chips span {
  padding: 0.3rem 0.7rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--danger, #d84f45) 8%, white);
  color: color-mix(in srgb, var(--danger, #d84f45) 80%, var(--text-heading));
  font-size: 0.8rem;
  font-weight: 600;
}

.business-acceptance-result-summary-panel__actions {
  margin-top: 1rem;
}

@media (max-width: 1100px) {
  .business-acceptance-result-summary-panel__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .business-acceptance-result-summary-panel__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
