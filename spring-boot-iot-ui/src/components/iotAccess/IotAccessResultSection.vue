<template>
  <section class="iot-access-result-section">
    <header
      v-if="title || description || $slots.meta || $slots.toolbar"
      class="iot-access-result-section__header"
    >
      <div v-if="title || description || $slots.meta" class="iot-access-result-section__copy">
        <h2 v-if="title" class="iot-access-result-section__title">{{ title }}</h2>
        <p v-if="description" class="iot-access-result-section__description">{{ description }}</p>
        <div v-if="$slots.meta" class="iot-access-result-section__meta">
          <slot name="meta" />
        </div>
      </div>

      <div v-if="$slots.toolbar" class="iot-access-result-section__toolbar">
        <slot name="toolbar" />
      </div>
    </header>

    <div class="iot-access-result-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '',
    description: ''
  }
)
</script>

<style scoped>
.iot-access-result-section {
  display: grid;
  gap: 0.9rem;
}

.iot-access-result-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.iot-access-result-section__copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.iot-access-result-section__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
  font-weight: 700;
}

.iot-access-result-section__description,
.iot-access-result-section__meta {
  color: var(--text-caption);
  font-size: 0.88rem;
  line-height: 1.6;
}

.iot-access-result-section__toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.iot-access-result-section__body {
  min-width: 0;
}

@media (max-width: 900px) {
  .iot-access-result-section__header {
    flex-direction: column;
  }

  .iot-access-result-section__toolbar {
    justify-content: flex-start;
  }
}
</style>
