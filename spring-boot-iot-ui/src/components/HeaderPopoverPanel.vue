<template>
  <section :id="panelId" ref="panelRef" class="header-popover" :class="panelClass" :aria-label="ariaLabel" role="dialog" aria-modal="false" tabindex="-1">
    <div class="header-popover__title">
      <strong>{{ content.title }}</strong>
      <small>{{ content.subtitle }}</small>
    </div>

    <div class="header-popover__summary">
      <p class="header-popover__summary-title">{{ content.summaryTitle }}</p>
      <p class="header-popover__summary-description">{{ content.summaryDescription }}</p>

      <ul v-if="content.metrics.length > 0" class="header-popover__metrics">
        <li v-for="metric in content.metrics" :key="metric.id" class="header-popover__metric" :data-tone="metric.tone || 'neutral'">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </li>
      </ul>
    </div>

    <div class="header-popover__sections">
      <section v-for="section in content.sections" :key="section.id" class="header-popover__section">
        <div class="header-popover__section-head">
          <strong>{{ section.title }}</strong>
          <small>{{ section.description }}</small>
        </div>

        <ul class="header-popover__list">
          <li v-for="item in section.items" :key="item.id">
            <button
              v-if="item.path"
              type="button"
              class="header-popover__item"
              :data-tone="item.tone || 'neutral'"
              @click="$emit('select', item.path)"
            >
              <div class="header-popover__item-top">
                <strong>{{ item.title }}</strong>
                <span v-if="item.badge" class="header-popover__item-badge">{{ item.badge }}</span>
              </div>
              <p>{{ item.description }}</p>
              <div class="header-popover__item-meta">
                <span>{{ item.meta || '站内入口' }}</span>
                <span class="header-popover__item-action">进入</span>
              </div>
            </button>

            <article v-else class="header-popover__item header-popover__item--static" :data-tone="item.tone || 'neutral'">
              <div class="header-popover__item-top">
                <strong>{{ item.title }}</strong>
                <span v-if="item.badge" class="header-popover__item-badge">{{ item.badge }}</span>
              </div>
              <p>{{ item.description }}</p>
              <div class="header-popover__item-meta">
                <span>{{ item.meta || '站内说明' }}</span>
              </div>
            </article>
          </li>
        </ul>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue';

import type { HeaderPopoverPanelProps } from '../types/shell';

const props = defineProps<HeaderPopoverPanelProps>();
defineEmits<{ (e: 'select', path: string): void; }>();
const panelRef = ref<HTMLElement | null>(null);

onMounted(() => {
  void props;
  nextTick(() => {
    const firstAction = panelRef.value?.querySelector<HTMLButtonElement>('button.header-popover__item');
    firstAction?.focus();
    if (document.activeElement !== firstAction) {
      panelRef.value?.focus();
    }
  });
});
</script>

<style scoped>
.header-popover {
  position: absolute;
  top: calc(100% - 0.2rem);
  right: max(calc((100vw - var(--shell-max-width)) / 2), var(--shell-gutter));
  width: min(30rem, calc(100vw - var(--shell-gutter) * 2));
  max-height: min(72vh, 46rem);
  overflow: auto;
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 36%),
    var(--bg-panel);
  box-shadow: var(--shadow-lg);
  padding: 0.85rem;
  z-index: 110;
  scrollbar-gutter: stable;
}

.header-popover--notice { width: min(30rem, calc(100vw - var(--shell-gutter) * 2)); }
.header-popover--help { width: min(31rem, calc(100vw - var(--shell-gutter) * 2)); }

.header-popover__title {
  display: grid;
  gap: 0.16rem;
  padding: 0.1rem 0.14rem 0.52rem;
}

.header-popover__title strong {
  color: var(--text-primary);
  font-size: 0.94rem;
}

.header-popover__title small {
  color: var(--text-tertiary);
  font-size: 0.74rem;
}

.header-popover__summary {
  border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--panel-border));
  border-radius: 0.9rem;
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand) 8%, white), var(--bg-card));
  padding: 0.76rem 0.8rem;
  display: grid;
  gap: 0.38rem;
}

.header-popover__summary-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.84rem;
  font-weight: 600;
}

.header-popover__summary-description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.74rem;
  line-height: 1.55;
}

.header-popover__metrics {
  list-style: none;
  margin: 0.08rem 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.46rem;
}

.header-popover__metric {
  min-width: 0;
  padding: 0.52rem 0.58rem;
  border-radius: 0.8rem;
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.82);
  display: grid;
  gap: 0.12rem;
}

.header-popover__metric span {
  font-size: 0.68rem;
  color: var(--text-tertiary);
}

.header-popover__metric strong {
  font-size: 0.8rem;
  color: var(--text-heading);
  font-weight: 600;
}

.header-popover__metric[data-tone='brand'] {
  border-color: color-mix(in srgb, var(--brand) 18%, transparent);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.header-popover__metric[data-tone='accent'] {
  border-color: color-mix(in srgb, var(--accent) 18%, transparent);
  background: color-mix(in srgb, var(--accent) 8%, white);
}

.header-popover__metric[data-tone='success'] {
  border-color: color-mix(in srgb, var(--success) 20%, transparent);
  background: color-mix(in srgb, var(--success) 10%, white);
}

.header-popover__metric[data-tone='warning'] {
  border-color: color-mix(in srgb, var(--warning) 22%, transparent);
  background: color-mix(in srgb, var(--warning) 10%, white);
}

.header-popover__metric[data-tone='danger'] {
  border-color: color-mix(in srgb, var(--danger) 22%, transparent);
  background: color-mix(in srgb, var(--danger) 8%, white);
}

.header-popover__sections {
  display: grid;
  gap: 0.72rem;
  margin-top: 0.72rem;
}

.header-popover__section {
  display: grid;
  gap: 0.42rem;
}

.header-popover__section-head {
  display: grid;
  gap: 0.12rem;
  padding: 0 0.14rem;
}

.header-popover__section-head strong {
  color: var(--text-heading);
  font-size: 0.82rem;
  font-weight: 600;
}

.header-popover__section-head small {
  color: var(--text-tertiary);
  font-size: 0.72rem;
}

.header-popover__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.42rem;
}

.header-popover__item {
  width: 100%;
  border: 1px solid var(--panel-border);
  border-radius: 0.86rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), var(--bg-hover));
  padding: 0.7rem 0.74rem;
  text-align: left;
  display: grid;
  gap: 0.34rem;
  color: var(--text-secondary);
}

.header-popover__item:hover,
.header-popover__item:focus-visible {
  border-color: color-mix(in srgb, var(--brand) 26%, transparent);
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.header-popover__item--static:hover,
.header-popover__item--static:focus-visible {
  border-color: var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), var(--bg-hover));
}

.header-popover__item[data-tone='brand'] { border-left: 3px solid color-mix(in srgb, var(--brand) 72%, white); }
.header-popover__item[data-tone='accent'] { border-left: 3px solid color-mix(in srgb, var(--accent) 72%, white); }
.header-popover__item[data-tone='success'] { border-left: 3px solid color-mix(in srgb, var(--success) 72%, white); }
.header-popover__item[data-tone='warning'] { border-left: 3px solid color-mix(in srgb, var(--warning) 72%, white); }
.header-popover__item[data-tone='danger'] { border-left: 3px solid color-mix(in srgb, var(--danger) 72%, white); }

.header-popover__item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.6rem;
}

.header-popover__item-top strong {
  min-width: 0;
  color: var(--text-heading);
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.45;
}

.header-popover__item-badge {
  flex: none;
  display: inline-flex;
  align-items: center;
  min-height: 1.3rem;
  padding: 0 0.46rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--brand);
  font-size: 0.66rem;
  font-weight: 600;
}

.header-popover__item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.73rem;
  line-height: 1.58;
}

.header-popover__item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  color: var(--text-tertiary);
  font-size: 0.69rem;
}

.header-popover__item-meta span:first-child {
  min-width: 0;
}

.header-popover__item-action {
  flex: none;
  color: var(--brand);
  font-weight: 600;
}

@media (max-width: 1200px) { .header-popover { right: var(--shell-gutter); } }
@media (max-width: 900px) { .header-popover { top: calc(100% + 2.2rem); } }
@media (max-width: 640px) {
  .header-popover,
  .header-popover--notice,
  .header-popover--help {
    width: calc(100vw - var(--shell-gutter) * 2);
    max-height: min(70vh, 40rem);
    padding: 0.76rem;
  }

  .header-popover__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
