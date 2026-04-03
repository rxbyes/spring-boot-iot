<template>
  <PanelCard
    title="运行结果导入"
    description="兼容导入外部 registry-run JSON，临时覆盖当前详情视图，但不会写入历史台账。"
  >
    <template #actions>
      <StandardActionGroup gap="sm">
        <StandardButton action="confirm" @click="handleImport">导入 JSON</StandardButton>
        <StandardButton action="reset" :disabled="!importedRun" @click="$emit('clear')">
          清空结果
        </StandardButton>
      </StandardActionGroup>
    </template>

    <el-input
      v-model="draftText"
      type="textarea"
      :rows="7"
      resize="vertical"
      placeholder="粘贴 registry-run-*.json 的完整内容"
    />

    <div v-if="importedRun" class="result-summary">
      <div class="result-summary__metrics">
        <span>总数 {{ importedRun.summary.total }}</span>
        <span>通过 {{ importedRun.summary.passed }}</span>
        <span>失败 {{ importedRun.summary.failed }}</span>
      </div>
      <div v-if="importedRun.failedScenarioIds.length > 0" class="result-summary__failed">
        <strong>失败场景</strong>
        <div class="result-summary__chips">
          <span v-for="scenarioId in importedRun.failedScenarioIds" :key="scenarioId">
            {{ scenarioId }}
          </span>
        </div>
      </div>
      <p v-else class="result-summary__empty">
        当前导入结果全部通过。
      </p>
    </div>

    <p v-else class="result-summary__empty">
      尚未导入统一运行汇总。
    </p>
  </PanelCard>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import PanelCard from './PanelCard.vue';
import StandardActionGroup from './StandardActionGroup.vue';
import StandardButton from './StandardButton.vue';
import type { ParsedAcceptanceRegistryRunSummary } from '../types/automation';

const props = defineProps<{
  importedRun: ParsedAcceptanceRegistryRunSummary | null;
}>();

const emit = defineEmits<{
  (event: 'import-json', rawText: string): void;
  (event: 'clear'): void;
}>();

const draftText = ref('');

function handleImport() {
  emit('import-json', draftText.value);
}
</script>

<style scoped>
.result-summary {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--panel-border);
}

.result-summary__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  color: var(--text-secondary);
  font-size: 0.92rem;
}

.result-summary__failed {
  margin-top: 0.9rem;
}

.result-summary__failed strong {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--text-heading);
}

.result-summary__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.result-summary__chips span {
  padding: 0.24rem 0.65rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--danger) 8%, white);
  border: 1px solid color-mix(in srgb, var(--danger) 18%, white);
  color: var(--danger);
  font-size: 0.84rem;
}

.result-summary__empty {
  margin: 0.9rem 0 0;
  color: var(--text-secondary);
}
</style>
