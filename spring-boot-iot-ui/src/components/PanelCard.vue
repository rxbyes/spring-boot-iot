<template>
  <el-card shadow="never" class="panel-card">
    <header class="panel-card__header">
      <div>
        <p v-if="eyebrow" class="panel-card__eyebrow">{{ eyebrow }}</p>
        <h2 class="panel-card__title">{{ title }}</h2>
      </div>
      <slot name="actions" />
    </header>
    <p v-if="description" class="panel-card__description">{{ description }}</p>
    <div class="panel-card__content">
      <slot />
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string;
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.panel-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.95)),
    radial-gradient(circle at top left, rgba(255, 106, 0, 0.1), transparent 42%);
  box-shadow: var(--shadow-sm);
}

.panel-card :deep(.el-card__body) {
  padding: 1.2rem;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.panel-card__eyebrow {
  margin: 0 0 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.panel-card__title {
  margin: 0;
  font-size: 1.15rem;
}

.panel-card__description {
  margin: 0.8rem 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.panel-card__content {
  margin-top: 1.1rem;
}
</style>
