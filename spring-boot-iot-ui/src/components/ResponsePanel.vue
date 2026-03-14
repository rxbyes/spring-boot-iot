<template>
  <PanelCard :eyebrow="eyebrow" :title="title" :description="description">
    <template #actions>
      <button class="ghost-button" type="button" @click="copyToClipboard">
        复制 JSON
      </button>
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
.ghost-button {
  padding: 0.55rem 0.9rem;
  border: 1px solid var(--panel-border-strong);
  border-radius: 999px;
  background: rgba(8, 13, 26, 0.9);
  color: var(--text-secondary);
}

.response-panel {
  margin: 0;
  padding: 1rem;
  overflow: auto;
  border-radius: var(--radius-md);
  border: 1px solid rgba(67, 98, 148, 0.4);
  background: rgba(3, 8, 18, 0.92);
  color: var(--text-primary);
  font-size: 0.86rem;
  line-height: 1.65;
  font-family: var(--font-mono);
}
</style>
