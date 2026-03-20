<template>
  <section class="shell-breadcrumb">
    <nav class="shell-breadcrumb__nav" aria-label="当前位置">
      <span class="shell-breadcrumb__item">{{ groupLabel }}</span>
      <span class="shell-breadcrumb__separator">/</span>
      <span class="shell-breadcrumb__item shell-breadcrumb__item--current">{{ activeTitle }}</span>
    </nav>
  </section>
</template>

<script setup lang="ts">
import type { ShellBreadcrumbProps } from '../types/shell';

defineProps<ShellBreadcrumbProps>();
</script>

<style scoped>
.shell-breadcrumb {
  padding: 0.05rem 0.1rem 0.55rem;
  border: none;
  background: transparent;
  box-shadow: none;
}

.shell-breadcrumb__nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.42rem;
  margin: 0;
  color: var(--text-muted-3);
  font-size: 0.76rem;
  font-weight: 600;
}

.shell-breadcrumb__item--current {
  color: var(--text-heading);
}

.shell-breadcrumb__separator {
  color: #b0bac8;
}

@media (max-width: 1200px) {
  .shell-breadcrumb {
    padding-bottom: 0.45rem;
  }
}
</style>
