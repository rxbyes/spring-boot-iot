<template>
  <PanelCard title="最近运行结果" description="优先从后台读取最近的 registry-run 汇总，手工导入作为兼容路径保留。">
    <template #actions>
      <StandardActionGroup gap="sm">
        <StandardButton action="refresh" :loading="loading" @click="$emit('refresh')">
          刷新最近运行
        </StandardButton>
      </StandardActionGroup>
    </template>

    <StandardInlineState
      v-if="errorMessage"
      tone="error"
      :message="errorMessage"
    />
    <StandardInlineState
      v-else-if="loading && recentRuns.length === 0"
      :message="'正在读取最近运行结果...'"
    />
    <div v-else-if="recentRuns.length === 0" class="empty-block">
      当前还没有可读取的最近运行结果；可先执行统一验收脚本，或继续粘贴 JSON 汇总。
    </div>
    <template v-else>
      <StandardTableToolbar
        compact
        :meta-items="[
          `最近结果 ${recentRuns.length} 条`,
          selectedRunId ? `当前载入 ${selectedRunId}` : '当前未载入后台结果'
        ]"
      />
      <el-table :data="displayRows" size="small" border>
        <StandardTableTextColumn prop="runId" label="运行编号" :min-width="150" />
        <StandardTableTextColumn prop="updatedAt" label="更新时间" :min-width="170" />
        <StandardTableTextColumn prop="summaryText" label="汇总" :min-width="180" />
        <StandardTableTextColumn prop="evidenceText" label="证据" :width="100" />
        <el-table-column label="操作" :width="160">
          <template #default="{ row }">
            <div class="action-cell">
              <span v-if="row.runId === selectedRunId" class="selected-label">当前已载入</span>
              <StandardButton
                v-else
                action="confirm"
                :link="true"
                @click="$emit('import-run', row.runId)"
              >
                载入结果
              </StandardButton>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AutomationResultRecentRun } from '../types/automation';
import PanelCard from './PanelCard.vue';
import StandardActionGroup from './StandardActionGroup.vue';
import StandardButton from './StandardButton.vue';
import StandardInlineState from './StandardInlineState.vue';
import StandardTableTextColumn from './StandardTableTextColumn.vue';
import StandardTableToolbar from './StandardTableToolbar.vue';

const props = withDefaults(
  defineProps<{
    recentRuns: AutomationResultRecentRun[];
    loading?: boolean;
    errorMessage?: string;
    selectedRunId?: string;
  }>(),
  {
    loading: false,
    errorMessage: '',
    selectedRunId: ''
  }
);

defineEmits<{
  refresh: [];
  'import-run': [runId: string];
}>();

const displayRows = computed(() =>
  props.recentRuns.map((item) => ({
    ...item,
    summaryText: `总 ${item.summary.total} / 通过 ${item.summary.passed} / 失败 ${item.summary.failed}`,
    evidenceText: `${item.relatedEvidenceFiles.length} 份`
  }))
);
</script>

<style scoped>
.empty-block {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.action-cell {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.selected-label {
  color: var(--success);
  font-size: 0.88rem;
  font-weight: 600;
}
</style>
