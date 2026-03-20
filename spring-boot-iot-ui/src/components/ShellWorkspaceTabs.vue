<template>
  <div class="shell-workspace-tabs">
    <div class="shell-workspace-tabs__label">
      <span>业务分区</span>
      <small>{{ activeDescription }}</small>
    </div>

    <nav class="shell-workspace-tabs__nav" aria-label="一级导航">
      <el-tooltip
        v-for="group in groups"
        :key="group.key"
        placement="bottom"
        effect="light"
        :content="group.description"
      >
        <button
          type="button"
          class="shell-workspace-tabs__item"
          :class="{ 'shell-workspace-tabs__item--active': activeGroupKey === group.key }"
          :aria-label="`${group.label}，${group.description}`"
          @click="$emit('switch-group', group.key)"
        >
          <span>{{ group.label }}</span>
        </button>
      </el-tooltip>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { ShellWorkspaceTabsProps } from '../types/shell';

const props = defineProps<ShellWorkspaceTabsProps>();

defineEmits<{
  (event: 'switch-group', groupKey: string): void;
}>();

const activeDescription = computed(() => {
  return props.groups.find((group) => group.key === props.activeGroupKey)?.description || '';
});
</script>

<style scoped>
.shell-workspace-tabs {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 1rem;
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  padding: 0 0 0.7rem;
}

.shell-workspace-tabs__label {
  display: grid;
  gap: 0.12rem;
  min-width: 7rem;
}

.shell-workspace-tabs__label span {
  color: var(--text-heading);
  font-size: 0.76rem;
  font-weight: 700;
}

.shell-workspace-tabs__label small {
  color: var(--text-caption-2);
  font-size: 0.72rem;
}

.shell-workspace-tabs__nav {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-start;
  gap: 0.28rem;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
  padding: 0;
}

.shell-workspace-tabs__nav::-webkit-scrollbar {
  display: none;
}

.shell-workspace-tabs__item {
  border: none;
  border-radius: calc(var(--radius-md) + 2px);
  background: transparent;
  color: #3f4653;
  min-height: 2.3rem;
  padding: 0.5rem 1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  position: relative;
  transition: all 160ms ease;
}

.shell-workspace-tabs__item span {
  font-size: 0.88rem;
  font-weight: 700;
}

.shell-workspace-tabs__item:hover {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.shell-workspace-tabs__item--active {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.shell-workspace-tabs__item--active::after {
  content: '';
  position: absolute;
  left: 0.75rem;
  right: 0.75rem;
  bottom: 0.18rem;
  height: 3px;
  border-radius: var(--radius-2xs);
  background: var(--brand);
}

@media (max-width: 1200px) {
  .shell-workspace-tabs {
    grid-template-columns: 1fr;
    gap: 0.6rem;
  }
}

@media (max-width: 900px) {
  .shell-workspace-tabs__label {
    display: none;
  }

  .shell-workspace-tabs__item {
    min-width: 7.4rem;
  }
}
</style>
