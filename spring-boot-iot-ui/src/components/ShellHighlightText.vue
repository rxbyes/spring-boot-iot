<template>
  <component :is="tag" class="shell-highlight-text">
    <template v-for="(segment, index) in segments" :key="`${index}-${segment.text}`">
      <mark v-if="segment.matched" class="shell-highlight-text__mark">{{ segment.text }}</mark>
      <template v-else>{{ segment.text }}</template>
    </template>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import { splitHighlightedText } from '@/utils/textHighlight'

const props = withDefaults(defineProps<{
  text?: string | null
  keyword?: string | null
  tag?: string
}>(), {
  text: '',
  keyword: '',
  tag: 'span'
})

const segments = computed(() => splitHighlightedText(props.text, props.keyword))
</script>

<style scoped>
.shell-highlight-text {
  min-width: 0;
}

.shell-highlight-text__mark {
  padding: 0 0.14em;
  border-radius: 0.24rem;
  background: color-mix(in srgb, var(--warning) 18%, white);
  color: inherit;
}
</style>
