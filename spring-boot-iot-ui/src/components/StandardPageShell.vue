<template>
  <section class="standard-page-shell">
    <nav
      v-if="showBreadcrumbs && breadcrumbs.length"
      class="standard-page-shell__breadcrumbs"
      aria-label="页面层级"
    >
      <template v-for="(item, index) in breadcrumbs" :key="`${item.label}-${index}`">
        <RouterLink
          v-if="item.to"
          :to="item.to"
          class="standard-page-shell__breadcrumb-item standard-page-shell__breadcrumb-item--link"
        >
          {{ item.label }}
        </RouterLink>
        <span
          v-else
          class="standard-page-shell__breadcrumb-item standard-page-shell__breadcrumb-item--current"
        >
          {{ item.label }}
        </span>
      </template>
    </nav>

    <div v-if="showHeadline" class="standard-page-shell__headline">
      <div class="standard-page-shell__copy">
        <p v-if="eyebrow" class="standard-page-shell__eyebrow">{{ eyebrow }}</p>
        <h1 v-if="showTitle && title" class="standard-page-shell__title">{{ title }}</h1>
        <p v-if="description" class="standard-page-shell__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="standard-page-shell__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.default" class="standard-page-shell__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

export interface StandardPageShellBreadcrumb {
  label: string
  to?: string
}

const props = withDefaults(defineProps<{
  eyebrow?: string
  title?: string
  description?: string
  showTitle?: boolean
  showBreadcrumbs?: boolean
  breadcrumbs?: StandardPageShellBreadcrumb[]
}>(), {
  eyebrow: '',
  title: '',
  description: '',
  showTitle: true,
  showBreadcrumbs: false,
  breadcrumbs: () => []
})

const slots = useSlots()
const showHeadline = computed(() => Boolean(props.eyebrow) || (props.showTitle && Boolean(props.title)) || Boolean(props.description) || Boolean(slots.actions))
</script>

<style scoped>
.standard-page-shell {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
}

.standard-page-shell__breadcrumbs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.34rem;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.55;
}

.standard-page-shell__breadcrumb-item {
  display: inline-flex;
  align-items: center;
  gap: 0.34rem;
}

.standard-page-shell__breadcrumb-item:not(:last-child)::after {
  content: "/";
  color: var(--text-disabled);
}

.standard-page-shell__breadcrumb-item--link {
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.standard-page-shell__breadcrumb-item--link:hover {
  color: var(--brand);
}

.standard-page-shell__breadcrumb-item--current {
  color: var(--text-secondary);
  font-weight: 600;
}

.standard-page-shell__headline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.88rem;
}

.standard-page-shell__copy {
  min-width: 0;
}

.standard-page-shell__eyebrow {
  margin: 0 0 0.3rem;
  color: var(--text-tertiary);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.standard-page-shell__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.18rem;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.015em;
}

.standard-page-shell__description {
  margin: 0.28rem 0 0;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.6;
}

.standard-page-shell__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.standard-page-shell__body {
  min-width: 0;
}

@media (max-width: 900px) {
  .standard-page-shell__headline {
    flex-direction: column;
  }

  .standard-page-shell__actions {
    justify-content: flex-start;
  }
}
</style>
