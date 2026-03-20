<template>
  <div class="flow-rail standard-flow-rail">
    <article
      v-for="item in items"
      :key="item.key ?? `${item.index}-${item.title}`"
      class="flow-rail__item standard-flow-rail__item"
    >
      <span class="flow-rail__index">{{ item.index }}</span>
      <div>
        <strong>{{ item.title }}</strong>
        <span v-if="showDescription && item.description">{{ item.description }}</span>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
export interface StandardFlowRailItem {
  key?: string | number
  index: string
  title: string
  description?: string
}

withDefaults(
  defineProps<{
    items: StandardFlowRailItem[]
    showDescription?: boolean
  }>(),
  {
    showDescription: true
  }
)
</script>
