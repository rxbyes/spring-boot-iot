<template>
  <div v-if="tabs.length > 1" class="tabs-view" aria-label="最近访问标签">
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
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.8rem;
}

.tabs-view__item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.2rem;
  padding: 0.2rem 0.3rem 0.2rem 0.36rem;
  border-radius: 0.6rem;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 251, 255, 0.92));
  box-shadow: var(--shadow-sm);
}

.tabs-view__item--active {
  border-color: color-mix(in srgb, var(--brand) 28%, transparent);
  background: linear-gradient(
    120deg,
    color-mix(in srgb, var(--brand) 13%, transparent),
    color-mix(in srgb, var(--brand) 4%, transparent)
  );
}

.tabs-view__link {
  display: inline-flex;
  align-items: center;
  min-height: 1.8rem;
  padding: 0 0.5rem;
  color: var(--text-secondary);
  text-decoration: none;
}

.tabs-view__item--active .tabs-view__link {
  color: var(--brand);
}

.tabs-view__title {
  white-space: nowrap;
  font-size: 0.84rem;
}

.tabs-view__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-tertiary);
}

.tabs-view__close:hover,
.tabs-view__close:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 28%, transparent);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
  color: var(--brand);
}
</style>
