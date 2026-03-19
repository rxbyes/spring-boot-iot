<template>
  <div class="automation-capture-editor">
    <StandardInlineSectionHeader :title="title">
      <template #actions>
        <el-button text @click="$emit('add')">{{ addText }}</el-button>
      </template>
    </StandardInlineSectionHeader>

    <div v-if="!captures?.length" class="automation-capture-editor__empty">
      {{ emptyText }}
    </div>

    <div
      v-for="(capture, captureIndex) in captures"
      :key="`${itemKeyPrefix}-${captureIndex}`"
      class="automation-capture-editor__row"
    >
      <el-input v-model="capture.variable" :placeholder="variablePlaceholder" />
      <el-input v-model="capture.path" :placeholder="pathPlaceholder" />
      <el-button text type="danger" @click="$emit('remove', captureIndex)">移除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import StandardInlineSectionHeader from './StandardInlineSectionHeader.vue';
import type { AutomationApiCapture } from '../types/automation';

withDefaults(
  defineProps<{
    captures?: AutomationApiCapture[];
    title?: string;
    addText?: string;
    emptyText: string;
    variablePlaceholder?: string;
    pathPlaceholder?: string;
    itemKeyPrefix?: string;
  }>(),
  {
    title: '变量捕获',
    addText: '新增捕获',
    variablePlaceholder: '变量名，如 productId',
    pathPlaceholder: '响应路径，如 payload.data.id',
    itemKeyPrefix: 'capture'
  }
);

defineEmits<{
  add: [];
  remove: [index: number];
}>();
</script>

<style scoped>
.automation-capture-editor__empty {
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
}

.automation-capture-editor__row {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: 1fr 1fr auto;
  align-items: center;
}

.automation-capture-editor__row + .automation-capture-editor__row {
  margin-top: 0.65rem;
}

@media (max-width: 1024px) {
  .automation-capture-editor__row {
    grid-template-columns: 1fr;
  }
}
</style>
