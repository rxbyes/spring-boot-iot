<template>
  <div class="standard-inline-section-header">
    <div class="standard-inline-section-header__main">
      <strong>{{ title }}</strong>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="standard-inline-section-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.standard-inline-section-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.standard-inline-section-header__main {
  min-width: 0;
}

.standard-inline-section-header__main strong {
  display: block;
}

.standard-inline-section-header__main p {
  margin: 0.3rem 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 0.88rem;
}

.standard-inline-section-header__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .standard-inline-section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .standard-inline-section-header__actions {
    justify-content: flex-start;
  }
}
</style>
