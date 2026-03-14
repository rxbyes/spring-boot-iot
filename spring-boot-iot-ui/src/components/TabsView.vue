<template>
  <div class="tabs-view" aria-label="最近访问标签">
    <div
      v-for="tab in tabs"
      :key="tab.path"
      class="tabs-view__item"
      :class="{ 'tabs-view__item--active': route.path === tab.path }"
    >
      <RouterLink :to="tab.path" class="tabs-view__link">
        <span class="tabs-view__title">{{ tab.title }}</span>
      </RouterLink>
      <button
        v-if="tabs.length > 1"
        class="tabs-view__close"
        type="button"
        :aria-label="`关闭 ${tab.title}`"
        @click="handleClose(tab.path)"
      >
        ×
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, useRoute, useRouter } from 'vue-router';

import { closeVisitedTab, visitedTabs } from '../stores/tabs';

const route = useRoute();
const router = useRouter();
const tabs = visitedTabs;

function handleClose(path: string) {
  const isActive = route.path === path;
  closeVisitedTab(path);

  if (isActive && tabs.value.length) {
    router.push(tabs.value[tabs.value.length - 1].path);
  }
}
</script>

<style scoped>
.tabs-view {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}

.tabs-view__item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.6rem;
  padding: 0.25rem 0.35rem 0.25rem 0.4rem;
  border-radius: 999px;
  border: 1px solid var(--panel-border);
  background: rgba(8, 13, 26, 0.82);
}

.tabs-view__item--active {
  border-color: var(--panel-border-strong);
  background: rgba(12, 22, 42, 0.95);
}

.tabs-view__link {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.55rem;
  color: inherit;
  text-decoration: none;
}

.tabs-view__item--active .tabs-view__link {
  color: var(--text-primary);
}

.tabs-view__title {
  white-space: nowrap;
}

.tabs-view__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 50%;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
}

.tabs-view__close:hover,
.tabs-view__close:focus-visible {
  border-color: var(--panel-border-strong);
  background: rgba(57, 241, 255, 0.1);
  color: var(--text-primary);
}
</style>
