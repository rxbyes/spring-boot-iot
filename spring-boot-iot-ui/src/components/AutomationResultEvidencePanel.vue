<template>
  <PanelCard title="证据清单与原文预览" description="历史台账中的已选运行可直接查看关联证据；兼容导入时保留只读提示，不强行构造后台证据。">
    <StandardInlineState
      v-if="!runId"
      tone="info"
      message="当前未绑定后台运行，兼容导入结果或空选择状态下暂无可读取的后台证据；如需原文预览，请先从历史台账载入某次运行。"
    />
    <StandardInlineState v-else-if="errorMessage" tone="error" :message="errorMessage" />
    <StandardInlineState v-else-if="loading && evidenceItems.length === 0" message="正在读取当前运行的证据清单..." />
    <div v-else-if="evidenceItems.length === 0" class="empty-block">
      当前运行未返回可预览的文本证据。
    </div>
    <div v-else class="evidence-grid">
      <section class="evidence-list">
        <StandardTableToolbar
          compact
          :meta-items="[
            `证据 ${evidenceItems.length} 份`,
            selectedPath ? `当前 ${selectedPath}` : '当前未选择证据'
          ]"
        />
        <el-table :data="evidenceRows" size="small" border>
          <StandardTableTextColumn prop="fileName" label="文件" :min-width="220" />
          <StandardTableTextColumn prop="categoryLabel" label="类型" :width="110" />
          <StandardTableTextColumn prop="sourceLabel" label="来源" :width="110" />
          <el-table-column label="操作" :width="110">
            <template #default="{ row }">
              <div class="action-cell">
                <span v-if="row.path === selectedPath" class="selected-label">当前预览</span>
                <StandardButton
                  v-else
                  v-permission="'system:automation-governance:evidence:evidence-preview'"
                  action="confirm"
                  :link="true"
                  @click="$emit('select-evidence', row.path)"
                >
                  查看原文
                </StandardButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="preview-panel">
        <header class="preview-header">
          <div>
            <h4>原文预览</h4>
            <p>{{ previewTitle }}</p>
          </div>
          <span v-if="preview?.truncated" class="preview-tag">已截断</span>
        </header>
        <StandardInlineState
          v-if="previewErrorMessage"
          tone="error"
          :message="previewErrorMessage"
        />
        <StandardInlineState
          v-else-if="previewLoading"
          message="正在读取证据原文..."
        />
        <div v-else-if="!preview" class="empty-block">
          请选择左侧证据查看原文。
        </div>
        <div v-else-if="preview?.category === 'image' && imagePreviewSrc" class="preview-image-frame">
          <img class="preview-image" :src="imagePreviewSrc" :alt="preview.fileName" />
        </div>
        <pre v-else class="preview-content">{{ preview.content }}</pre>
      </section>
    </div>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AutomationResultEvidenceContent, AutomationResultEvidenceItem } from '../types/automation';
import PanelCard from './PanelCard.vue';
import StandardButton from './StandardButton.vue';
import StandardInlineState from './StandardInlineState.vue';
import StandardTableTextColumn from './StandardTableTextColumn.vue';
import StandardTableToolbar from './StandardTableToolbar.vue';

const props = withDefaults(
  defineProps<{
    runId?: string;
    evidenceItems: AutomationResultEvidenceItem[];
    loading?: boolean;
    errorMessage?: string;
    selectedPath?: string;
    preview?: AutomationResultEvidenceContent | null;
    previewLoading?: boolean;
    previewErrorMessage?: string;
  }>(),
  {
    runId: '',
    loading: false,
    errorMessage: '',
    selectedPath: '',
    preview: null,
    previewLoading: false,
    previewErrorMessage: ''
  }
);

defineEmits<{
  'select-evidence': [path: string];
}>();

const categoryLabelMap: Record<string, string> = {
  'run-summary': '运行汇总',
  json: 'JSON',
  markdown: 'Markdown',
  text: '文本',
  image: 'PNG/JPG',
  unknown: '其他'
};

const sourceLabelMap: Record<string, string> = {
  report: '汇总主档',
  related: '关联证据',
  scenario: '场景产物'
};

const evidenceRows = computed(() =>
  props.evidenceItems.map((item) => ({
    ...item,
    categoryLabel: categoryLabelMap[item.category] || item.category,
    sourceLabel: sourceLabelMap[item.source] || item.source
  }))
);

const previewTitle = computed(() => {
  if (!props.runId) {
    return '当前未绑定后台运行，无法预览后台证据';
  }
  if (!props.preview) {
    return '选择证据后可查看原文片段';
  }
  return `${props.preview.fileName} · ${categoryLabelMap[props.preview.category] || props.preview.category}`;
});

const imagePreviewSrc = computed(() => {
  if (props.preview?.category !== 'image') {
    return '';
  }
  return props.preview.content.startsWith('data:image/') ? props.preview.content : '';
});
</script>

<style scoped>
.evidence-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.35fr);
  gap: 1rem;
}

.evidence-list,
.preview-panel {
  min-width: 0;
}

.preview-panel {
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--brand) 10%, white);
  background: linear-gradient(180deg, #fbfcff 0%, #f4f7fc 100%);
}

.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}

.preview-header h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--text-primary);
}

.preview-header p {
  margin: 0.35rem 0 0;
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.preview-tag {
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--warning) 12%, white);
  color: var(--warning);
  font-size: 0.78rem;
  font-weight: 600;
}

.preview-content {
  margin: 0;
  padding: 1rem;
  min-height: 19rem;
  overflow: auto;
  border-radius: var(--radius-md);
  border: 1px solid rgba(51, 72, 104, 0.12);
  background: rgba(255, 255, 255, 0.85);
  color: #1f2a3d;
  font-size: 0.85rem;
  line-height: 1.65;
  font-family: var(--font-mono);
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-image-frame {
  min-height: 19rem;
  padding: 0.75rem;
  overflow: auto;
  border-radius: var(--radius-md);
  border: 1px solid rgba(51, 72, 104, 0.12);
  background: rgba(255, 255, 255, 0.9);
}

.preview-image {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  border-radius: var(--radius-sm);
  box-shadow: 0 12px 28px rgba(31, 42, 61, 0.12);
}

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

@media (max-width: 1200px) {
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .preview-content {
    min-height: 15rem;
  }

  .preview-image-frame {
    min-height: 15rem;
  }
}
</style>
