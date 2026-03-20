<template>
  <section
    :id="panelId"
    ref="panelRef"
    class="header-popover"
    :class="panelClass"
    :aria-label="ariaLabel"
    role="dialog"
    aria-modal="false"
    tabindex="-1"
  >
    <div class="header-popover__title">
      <strong>{{ title }}</strong>
      <small>{{ subtitle }}</small>
    </div>
    <ul class="header-popover__list">
      <li v-for="item in items" :key="item.id">
        <button type="button" @click="$emit('select', item.path)">
          <strong>{{ item.title }}</strong>
          <span>{{ item.description }}</span>
        </button>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';

interface HeaderPopoverItem {
  id: string;
  title: string;
  description: string;
  path: string;
}

const props = defineProps<{
  panelId: string;
  panelClass?: string;
  ariaLabel: string;
  title: string;
  subtitle: string;
  items: HeaderPopoverItem[];
}>();

defineEmits<{
  (e: 'select', path: string): void;
}>();

const panelRef = ref<HTMLElement | null>(null);

onMounted(() => {
  void props;
  nextTick(() => {
    const firstAction = panelRef.value?.querySelector<HTMLButtonElement>('button');
    firstAction?.focus();
  });
});
</script>

<style scoped>
.header-popover {
  position: absolute;
  top: calc(100% - 0.2rem);
  right: max(calc((100vw - var(--shell-max-width)) / 2), var(--shell-gutter));
  width: min(25rem, calc(100vw - var(--shell-gutter) * 2));
  border: 1px solid var(--panel-border);
  border-radius: 0.85rem;
  background: var(--bg-panel);
  box-shadow: var(--shadow-lg);
  padding: 0.72rem;
  z-index: 110;
}

.header-popover--help {
  width: min(22rem, calc(100vw - var(--shell-gutter) * 2));
}

.header-popover__title {
  display: grid;
  gap: 0.14rem;
  padding: 0.1rem 0.12rem 0.46rem;
}

.header-popover__title strong {
  color: var(--text-primary);
  font-size: 0.9rem;
}

.header-popover__title small {
  color: var(--text-tertiary);
  font-size: 0.76rem;
}

.header-popover__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.35rem;
}

.header-popover__list li button {
  width: 100%;
  border: 1px solid var(--panel-border);
  border-radius: 0.68rem;
  background: var(--bg-hover);
  padding: 0.56rem 0.62rem;
  text-align: left;
  display: grid;
  gap: 0.18rem;
  color: var(--text-secondary);
}

.header-popover__list li button strong {
  font-size: 0.82rem;
  font-weight: 600;
}

.header-popover__list li button span {
  font-size: 0.72rem;
  color: var(--text-tertiary);
}

.header-popover__list li button:hover {
  border-color: #bfd3f0;
  background: var(--bg-active);
}

@media (max-width: 1200px) {
  .header-popover {
    right: var(--shell-gutter);
  }
}

@media (max-width: 900px) {
  .header-popover {
    top: calc(100% + 2.2rem);
  }
}
</style>
