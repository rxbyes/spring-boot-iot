<template>
  <section class="iot-access-page-shell">
    <nav v-if="breadcrumbs.length" class="iot-access-page-shell__breadcrumbs" aria-label="页面层级">
      <template v-for="(item, index) in breadcrumbs" :key="`${item.label}-${index}`">
        <RouterLink
          v-if="item.to"
          :to="item.to"
          class="iot-access-page-shell__breadcrumb-item iot-access-page-shell__breadcrumb-item--link"
        >
          {{ item.label }}
        </RouterLink>
        <span
          v-else
          class="iot-access-page-shell__breadcrumb-item iot-access-page-shell__breadcrumb-item--current"
        >
          {{ item.label }}
        </span>
      </template>
    </nav>

    <div class="iot-access-page-shell__headline">
      <div class="iot-access-page-shell__copy">
        <h1 class="iot-access-page-shell__title">{{ title }}</h1>
      </div>
      <div v-if="$slots.actions" class="iot-access-page-shell__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="status || $slots.status" class="iot-access-page-shell__status">
      <slot name="status">{{ status }}</slot>
    </div>

    <div v-if="$slots.default" class="iot-access-page-shell__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

export interface IotAccessPageShellBreadcrumb {
  label: string
  to?: string
}

withDefaults(
  defineProps<{
    title: string
    breadcrumbs?: IotAccessPageShellBreadcrumb[]
    status?: string
  }>(),
  {
    breadcrumbs: () => [],
    status: ''
  }
)
</script>

<style scoped>
.iot-access-page-shell {
  display: grid;
  gap: 0.75rem;
}

.iot-access-page-shell__breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
}

.iot-access-page-shell__breadcrumb-item {
  color: var(--text-caption);
  font-size: 0.78rem;
  line-height: 1.4;
}

.iot-access-page-shell__breadcrumb-item--link {
  text-decoration: none;
  transition: color 160ms ease;
}

.iot-access-page-shell__breadcrumb-item--link:hover {
  color: var(--brand);
}

.iot-access-page-shell__breadcrumb-item--current {
  color: var(--text-secondary);
  font-weight: 600;
}

.iot-access-page-shell__headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.iot-access-page-shell__copy {
  min-width: 0;
}

.iot-access-page-shell__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.3rem;
  font-weight: 700;
  line-height: 1.3;
}

.iot-access-page-shell__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.iot-access-page-shell__status {
  border: 1px solid color-mix(in srgb, var(--brand) 16%, white);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
  color: var(--text-secondary);
  padding: 0.7rem 0.9rem;
  font-size: 0.88rem;
  line-height: 1.6;
}

.iot-access-page-shell__body {
  min-width: 0;
}

@media (max-width: 900px) {
  .iot-access-page-shell__headline {
    align-items: flex-start;
    flex-direction: column;
  }

  .iot-access-page-shell__actions {
    justify-content: flex-start;
  }
}
</style>
