<template>
  <PanelCard :eyebrow="eyebrow" :title="title" :description="description">
    <template #actions>
      <el-button text type="primary" @click="copyToClipboard">
        复制 JSON
      </el-button>
    </template>
    <pre class="response-panel" aria-live="polite">{{ formatted }}</pre>
  </PanelCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import { prettyJson } from '../utils/format';
import PanelCard from './PanelCard.vue';

const props = defineProps<{
  title: string;
  eyebrow?: string;
  description?: string;
  body: unknown;
}>();

const formatted = computed(() => prettyJson(props.body));

async function copyToClipboard() {
  if (navigator.clipboard) {
    await navigator.clipboard.writeText(formatted.value);
  }
}
</script>

<style scoped>
.response-panel {
  margin: 0;
  padding: 1rem;
  overflow: auto;
  border-radius: var(--radius-md);
  border: 1px solid rgba(51, 72, 104, 0.2);
  background: linear-gradient(180deg, #f9fbff, #f3f7fe);
  color: #1f2a3d;
  font-size: 0.86rem;
  line-height: 1.65;
  font-family: var(--font-mono);
}
</style>
