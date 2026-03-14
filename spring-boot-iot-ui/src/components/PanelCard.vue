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
  background:
    linear-gradient(180deg, rgba(10, 16, 34, 0.92), rgba(6, 10, 22, 0.88)),
    radial-gradient(circle at top left, rgba(42, 185, 255, 0.12), transparent 40%);
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
