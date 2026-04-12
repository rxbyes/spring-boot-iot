<template>
  <div class="message-trace-payload-comparison">
    <section
      v-for="panel in panels"
      :key="panel.key"
      class="message-trace-payload-comparison__panel detail-card"
      :class="{ 'message-trace-payload-comparison__panel--expanded': isExpanded(panel.key) }"
    >
      <header class="message-trace-payload-comparison__panel-header detail-card__header">
        <div class="message-trace-payload-comparison__panel-heading">
          <strong class="message-trace-payload-comparison__panel-title">{{ panel.title }}</strong>
          <div class="message-trace-payload-comparison__panel-meta detail-card__meta">
            <span>当前状态：{{ panel.available ? '已恢复' : '暂缺' }}</span>
            <span>{{ panel.description }}</span>
          </div>
        </div>
        <div class="message-trace-payload-comparison__panel-actions">
          <button
            type="button"
            class="message-trace-payload-comparison__action"
            :data-testid="`message-trace-payload-copy-${panel.key}`"
            :disabled="!panel.available"
            @click="handleCopy(panel)"
          >
            复制
          </button>
          <button
            type="button"
            class="message-trace-payload-comparison__action message-trace-payload-comparison__action--primary"
            :data-testid="`message-trace-payload-toggle-${panel.key}`"
            @click="togglePanel(panel.key)"
          >
            {{ isExpanded(panel.key) ? '收起' : '展开' }}
          </button>
        </div>
      </header>

      <div v-if="isExpanded(panel.key)" class="message-trace-payload-comparison__panel-body">
        <pre v-if="panel.available" class="message-trace-payload-comparison__code">{{ panel.content }}</pre>
        <div v-else class="message-trace-payload-comparison__empty">
          <strong class="message-trace-payload-comparison__empty-text">{{ panel.emptyText }}</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import type { MessageTracePayloadComparisonPanel } from '@/utils/messageTracePayloadComparison';

const props = defineProps<{
  panels: MessageTracePayloadComparisonPanel[];
}>();

const expandedState = ref<Record<MessageTracePayloadComparisonPanel['key'], boolean>>({
  raw: false,
  decrypted: false,
  decoded: false
});

watch(
  () => props.panels,
  () => {
    expandedState.value = {
      raw: false,
      decrypted: false,
      decoded: false
    };
  },
  { immediate: true, deep: true }
);

function isExpanded(key: MessageTracePayloadComparisonPanel['key']) {
  return Boolean(expandedState.value[key]);
}

function togglePanel(key: MessageTracePayloadComparisonPanel['key']) {
  expandedState.value[key] = !expandedState.value[key];
}

async function handleCopy(panel: MessageTracePayloadComparisonPanel) {
  if (!panel.available || !panel.content) {
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(panel.content);
    } else if (!copyWithExecCommand(panel.content)) {
      ElMessage.warning('当前环境暂不支持复制');
      return;
    }
    ElMessage.success(`${panel.title}已复制`);
  } catch {
    if (copyWithExecCommand(panel.content)) {
      ElMessage.success(`${panel.title}已复制`);
      return;
    }
    ElMessage.warning('复制失败，请稍后重试');
  }
}

function copyWithExecCommand(content: string) {
  if (typeof document === 'undefined' || typeof document.execCommand !== 'function' || !document.body) {
    return false;
  }

  const textArea = document.createElement('textarea');
  textArea.value = content;
  textArea.setAttribute('readonly', 'true');
  textArea.style.position = 'fixed';
  textArea.style.top = '0';
  textArea.style.left = '0';
  textArea.style.opacity = '0';
  textArea.style.pointerEvents = 'none';
  document.body.appendChild(textArea);

  try {
    textArea.focus();
    textArea.select();
    textArea.setSelectionRange(0, textArea.value.length);
    return document.execCommand('copy');
  } catch {
    return false;
  } finally {
    textArea.remove();
  }
}
</script>

<style scoped>
.message-trace-payload-comparison {
  display: grid;
  gap: 14px;
}

.message-trace-payload-comparison__panel {
  overflow: hidden;
}

.message-trace-payload-comparison__panel--expanded {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
}

.message-trace-payload-comparison__panel-header,
.message-trace-payload-comparison__panel-actions {
  display: flex;
  align-items: center;
}

.message-trace-payload-comparison__panel-header {
  justify-content: space-between;
  gap: 16px;
}

.message-trace-payload-comparison__panel-heading {
  display: grid;
  gap: 0.65rem;
  min-width: 0;
}

.message-trace-payload-comparison__panel-title {
  color: var(--text-heading);
  font-size: 15px;
  line-height: 1.5;
  font-weight: 700;
  min-width: 0;
}

.message-trace-payload-comparison__panel-meta {
  margin-top: 0;
}

.message-trace-payload-comparison__panel-meta span:last-child {
  white-space: normal;
}

.message-trace-payload-comparison__panel-actions {
  gap: 10px;
  flex-shrink: 0;
}

.message-trace-payload-comparison__action {
  min-width: 72px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--panel-border);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    color 0.18s ease,
    background 0.18s ease;
}

.message-trace-payload-comparison__action:hover:not(:disabled) {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.message-trace-payload-comparison__action:disabled {
  cursor: not-allowed;
  opacity: 0.52;
}

.message-trace-payload-comparison__action--primary {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 22%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.message-trace-payload-comparison__panel-body {
  margin-top: 0.85rem;
  padding-top: 1rem;
  border-top: 1px solid var(--line-soft);
}

.message-trace-payload-comparison__code,
.message-trace-payload-comparison__empty {
  border-radius: 16px;
}

.message-trace-payload-comparison__code {
  margin-bottom: 0;
  overflow: auto;
  padding: 18px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.7;
  font-family: var(--font-mono);
  color: var(--text-ink);
  background: var(--surface-muted);
  border: 1px solid var(--panel-border);
  box-shadow: var(--shadow-inset-highlight-76);
}

.message-trace-payload-comparison__empty {
  display: flex;
  align-items: center;
  min-height: 88px;
  padding: 16px 18px;
  background: var(--surface-muted);
  border: 1px dashed color-mix(in srgb, var(--text-tertiary) 36%, var(--panel-border));
}

.message-trace-payload-comparison__empty-text {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 720px) {
  .message-trace-payload-comparison__panel-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .message-trace-payload-comparison__panel-heading,
  .message-trace-payload-comparison__panel-actions {
    width: 100%;
  }

  .message-trace-payload-comparison__panel-actions {
    justify-content: flex-end;
  }
}
</style>
