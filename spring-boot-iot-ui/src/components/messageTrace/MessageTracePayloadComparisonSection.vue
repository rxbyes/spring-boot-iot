<template>
  <div class="message-trace-payload-comparison">
    <div class="message-trace-payload-comparison__grid">
      <PanelCard
        v-for="panel in panels"
        :key="panel.key"
        class="message-trace-payload-comparison__card"
        :title="panel.title"
        :description="panel.description"
      >
        <pre v-if="panel.available" class="message-trace-payload-comparison__code">{{ panel.content }}</pre>
        <div v-else class="message-trace-payload-comparison__empty">
          <span class="message-trace-payload-comparison__empty-label">当前状态</span>
          <strong class="message-trace-payload-comparison__empty-text">{{ panel.emptyText }}</strong>
        </div>
      </PanelCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import PanelCard from '@/components/PanelCard.vue';
import type { MessageTracePayloadComparisonPanel } from '@/utils/messageTracePayloadComparison';

defineProps<{
  panels: MessageTracePayloadComparisonPanel[];
}>();
</script>

<style scoped>
.message-trace-payload-comparison__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.message-trace-payload-comparison__card {
  min-height: 100%;
}

.message-trace-payload-comparison__code,
.message-trace-payload-comparison__empty {
  min-height: 240px;
  border-radius: 16px;
}

.message-trace-payload-comparison__code {
  margin: 0;
  overflow: auto;
  padding: 18px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.65;
  color: #e2e8f0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.98) 0%, rgba(30, 41, 59, 0.95) 100%);
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.message-trace-payload-comparison__empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.98) 0%, rgba(241, 245, 249, 0.96) 100%);
  border: 1px dashed rgba(148, 163, 184, 0.5);
}

.message-trace-payload-comparison__empty-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #64748b;
}

.message-trace-payload-comparison__empty-text {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
}

@media (max-width: 1200px) {
  .message-trace-payload-comparison__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .message-trace-payload-comparison__grid {
    grid-template-columns: 1fr;
  }

  .message-trace-payload-comparison__code,
  .message-trace-payload-comparison__empty {
    min-height: 200px;
  }
}
</style>
