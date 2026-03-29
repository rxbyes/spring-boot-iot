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

    <div v-if="showHeadline" class="iot-access-page-shell__headline">
      <div class="iot-access-page-shell__copy">
        <h1 v-if="showTitle" class="iot-access-page-shell__title">{{ title }}</h1>
        <p v-if="description" class="iot-access-page-shell__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="iot-access-page-shell__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.default" class="iot-access-page-shell__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { RouterLink } from 'vue-router'

export interface IotAccessPageShellBreadcrumb {
  label: string
  to?: string
}

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  showTitle?: boolean
  breadcrumbs?: IotAccessPageShellBreadcrumb[]
}>(), {
  title: '',
  description: '',
  showTitle: true,
  breadcrumbs: () => []
})

const slots = useSlots()
const showHeadline = computed(() => props.showTitle || Boolean(props.description) || Boolean(slots.actions))
</script>

<style scoped>
.iot-access-page-shell {
  display: grid;
  gap: 0.72rem;
}

.iot-access-page-shell__breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
}

.iot-access-page-shell__breadcrumb-item {
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
  letter-spacing: 0.01em;
}

.iot-access-page-shell__breadcrumb-item--link {
  text-decoration: none;
  transition: color var(--transition-fast);
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.88rem;
}

.iot-access-page-shell__copy {
  min-width: 0;
}

.iot-access-page-shell__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.18rem;
  line-height: 1.25;
  font-weight: 700;
}

.iot-access-page-shell__description {
  margin: 0.28rem 0 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.6;
}

.iot-access-page-shell__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.iot-access-page-shell__body {
  min-width: 0;
}

@media (max-width: 900px) {
  .iot-access-page-shell__headline {
    flex-direction: column;
  }

  .iot-access-page-shell__actions {
    justify-content: flex-start;
  }
}
</style>
