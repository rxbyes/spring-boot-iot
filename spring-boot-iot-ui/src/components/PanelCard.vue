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
  border-radius: calc(var(--radius-lg) + 2px);
  border: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 251, 255, 0.97)),
    radial-gradient(circle at top left, rgba(255, 106, 0, 0.06), transparent 38%);
  box-shadow: 0 4px 12px rgba(31, 35, 41, 0.05);
}

.panel-card :deep(.el-card__body) {
  padding: 1.1rem 1.15rem;
}

.panel-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
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
  font-size: 1.08rem;
}

.panel-card__description {
  margin: 0.65rem 0 0;
  color: var(--text-secondary);
  line-height: 1.65;
}

.panel-card__content {
  margin-top: 0.95rem;
}
</style>
