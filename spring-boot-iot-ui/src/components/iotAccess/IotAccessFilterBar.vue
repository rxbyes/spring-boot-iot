<template>
  <section class="iot-access-filter-bar">
    <div class="iot-access-filter-bar__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="iot-access-filter-bar__footer">
      <slot name="footer" />
    </div>
  </section>
</template>

<script setup lang="ts">
</script>

<style scoped>
.iot-access-filter-bar {
  display: grid;
  gap: 0.65rem;
}

.iot-access-filter-bar__body,
.iot-access-filter-bar__footer {
  min-width: 0;
}
</style>
