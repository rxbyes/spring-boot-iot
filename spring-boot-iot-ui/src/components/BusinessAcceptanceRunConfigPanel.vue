<template>
  <PanelCard title="执行前配置" description="业务人员只需要选择环境、账号模板和模块范围，其他执行细节继续复用现有自动化底座。">
    <div class="business-acceptance-run-config-panel__form">
      <label class="business-acceptance-run-config-panel__field">
        <span>环境</span>
        <el-select
          :model-value="selectedEnvironment"
          placeholder="选择环境"
          @update:model-value="$emit('update:environment', String($event || ''))"
        >
          <el-option
            v-for="environment in environmentOptions"
            :key="environment"
            :label="environment"
            :value="environment"
          />
        </el-select>
      </label>

      <label class="business-acceptance-run-config-panel__field">
        <span>账号模板</span>
        <el-select
          :model-value="selectedAccountTemplate"
          placeholder="选择账号模板"
          @update:model-value="$emit('update:account-template', String($event || ''))"
        >
          <el-option
            v-for="template in accountTemplates"
            :key="template.templateCode"
            :label="`${template.templateName} (${template.username})`"
            :value="template.templateCode"
          />
        </el-select>
      </label>
    </div>

    <label class="business-acceptance-run-config-panel__field">
      <span>模块范围</span>
      <el-checkbox-group
        :model-value="selectedModuleCodes"
        class="business-acceptance-run-config-panel__modules"
        @update:model-value="$emit('update:module-codes', Array.isArray($event) ? $event.map(String) : [])"
      >
        <el-checkbox
          v-for="module in moduleOptions"
          :key="module.moduleCode"
          :label="module.moduleCode"
        >
          {{ module.moduleName }}
        </el-checkbox>
      </el-checkbox-group>
    </label>

    <div class="business-acceptance-run-config-panel__actions">
      <StandardButton action="confirm" :loading="launching" v-permission="'system:business-acceptance:launch'" @click="$emit('launch')">
        一键执行验收
      </StandardButton>
      <span class="business-acceptance-run-config-panel__hint">
        执行完成后自动跳转到模块结论页。
      </span>
    </div>

    <StandardInlineState
      v-if="statusMessage"
      :tone="statusTone"
      :message="statusMessage"
    />

    <div class="business-acceptance-run-config-panel__latest">
      <MetricCard
        size="compact"
        label="最近一次通过模块"
        :value="String(latestResult?.passedModuleCount ?? 0)"
        :badge="{ label: 'Pass', tone: 'success' }"
      />
      <MetricCard
        size="compact"
        label="最近一次未过模块"
        :value="String(latestResult?.failedModuleCount ?? 0)"
        :badge="{
          label: latestResult?.failedModuleCount ? 'Fail' : 'Clean',
          tone: latestResult?.failedModuleCount ? 'danger' : 'brand'
        }"
      />
    </div>

    <p class="business-acceptance-run-config-panel__latest-summary">
      {{ latestSummary }}
    </p>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type {
  BusinessAcceptanceAccountTemplate,
  BusinessAcceptanceLatestResult,
  BusinessAcceptancePackageModule,
  BusinessAcceptanceRunStatus
} from '@/types/businessAcceptance';
import MetricCard from './MetricCard.vue';
import PanelCard from './PanelCard.vue';
import StandardButton from './StandardButton.vue';
import StandardInlineState from './StandardInlineState.vue';

const props = withDefaults(
  defineProps<{
    environmentOptions: string[];
    accountTemplates: BusinessAcceptanceAccountTemplate[];
    moduleOptions: BusinessAcceptancePackageModule[];
    selectedEnvironment: string;
    selectedAccountTemplate: string;
    selectedModuleCodes: string[];
    latestResult?: BusinessAcceptanceLatestResult | null;
    launching?: boolean;
    launchErrorMessage?: string;
    runStatus?: BusinessAcceptanceRunStatus | null;
  }>(),
  {
    latestResult: null,
    launching: false,
    launchErrorMessage: '',
    runStatus: null
  }
);

defineEmits<{
  (event: 'update:environment', value: string): void;
  (event: 'update:account-template', value: string): void;
  (event: 'update:module-codes', value: string[]): void;
  (event: 'launch'): void;
}>();

const statusTone = computed<'info' | 'error'>(() => {
  if (props.launchErrorMessage) {
    return 'error';
  }
  return props.runStatus?.status === 'failed' || props.runStatus?.status === 'blocked' ? 'error' : 'info';
});

const statusMessage = computed(() => {
  if (props.launchErrorMessage) {
    return props.launchErrorMessage;
  }
  if (!props.runStatus) {
    return '';
  }
  if (props.runStatus.status === 'running') {
    return '验收任务正在执行，完成后会自动跳转到业务结论页。';
  }
  if (props.runStatus.runId) {
    return `验收任务已完成，运行编号 ${props.runStatus.runId}。`;
  }
  if (props.runStatus.errorMessage) {
    return props.runStatus.errorMessage;
  }
  return '验收任务已结束。';
});

const latestSummary = computed(() => {
  if (!props.latestResult || props.latestResult.status === 'neverRun') {
    return '当前验收包还没有历史结果，可以直接从这里发起第一次业务验收。';
  }
  if ((props.latestResult.failedModuleCount || 0) > 0) {
    return `最近一次未通过模块：${props.latestResult.failedModuleNames.join('、')}。`;
  }
  return '最近一次验收全部通过，可以继续按需复验。';
});
</script>

<style scoped>
.business-acceptance-run-config-panel__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.business-acceptance-run-config-panel__field {
  display: grid;
  gap: 0.45rem;
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.business-acceptance-run-config-panel__field span {
  color: var(--text-tertiary);
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.business-acceptance-run-config-panel__modules {
  display: grid;
  gap: 0.55rem;
}

.business-acceptance-run-config-panel__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.8rem;
  margin-top: 1rem;
}

.business-acceptance-run-config-panel__hint {
  color: var(--text-tertiary);
  font-size: 0.86rem;
}

.business-acceptance-run-config-panel__latest {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 1rem;
}

.business-acceptance-run-config-panel__latest-summary {
  margin: 0.9rem 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 960px) {
  .business-acceptance-run-config-panel__form,
  .business-acceptance-run-config-panel__latest {
    grid-template-columns: 1fr;
  }
}
</style>
