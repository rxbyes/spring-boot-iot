<template>
  <el-card shadow="never" class="panel-card" :class="{ 'panel-card--slot-header': hasHeaderSlot }">
    <header class="panel-card__header">
      <slot name="header">
        <div>
          <p v-if="eyebrow" class="panel-card__eyebrow">{{ eyebrow }}</p>
          <h2 v-if="title" class="panel-card__title">{{ title }}</h2>
        </div>
        <slot name="actions" />
      </slot>
    </header>
    <p v-if="description && !hasHeaderSlot" class="panel-card__description">{{ description }}</p>
    <div class="panel-card__content">
      <slot />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue';

defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
}>();

const slots = useSlots();
const hasHeaderSlot = computed(() => Boolean(slots.header));
</script>

<style scoped>
.panel-card {
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(249, 250, 252, 0.98));
  box-shadow: var(--shadow-card-soft);
}

.panel-card :deep(.el-card__body) {
  padding: 1rem 1.05rem;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding-bottom: 0.82rem;
  border-bottom: 1px solid var(--line-soft);
}

.panel-card__eyebrow {
  margin: 0 0 0.3rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.panel-card__title {
  margin: 0;
  font-size: 1.04rem;
  color: var(--text-heading);
}

.panel-card__description {
  margin: 0.72rem 0 0;
  color: var(--text-caption);
  line-height: 1.65;
}

.panel-card__content {
  margin-top: 0.8rem;
}

.panel-card--slot-header .panel-card__content {
  margin-top: 0.72rem;
}
</style>
