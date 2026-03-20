<template>
  <transition name="command-palette-fade">
    <div v-if="modelValue" class="command-palette">
      <button type="button" class="command-palette__mask" aria-label="关闭全局命令面板" @click="closePalette" />
      <div class="command-palette__panel" role="dialog" aria-modal="true" aria-label="全局命令面板">
        <div class="command-palette__header">
          <label class="command-palette__search" for="command-palette-input">
            <span class="command-palette__icon" aria-hidden="true"></span>
            <input id="command-palette-input" ref="inputRef" :value="query" type="search" placeholder="搜索工作台、功能页或输入路由" @input="updateQuery" @keydown.esc.prevent="closePalette" />
          </label>
          <button type="button" class="command-palette__close" @click="closePalette">Esc</button>
        </div>

        <section v-if="showRecent" class="command-palette__section">
          <header class="command-palette__section-head"><span>最近入口</span><small>最近处理过的页面</small></header>
          <div class="command-palette__list">
            <button v-for="item in recentItems" :key="`recent-${item.path}`" type="button" class="command-palette__item" @click="selectPath(item.path)">
              <span class="command-palette__item-short">{{ item.short || '近' }}</span>
              <span class="command-palette__item-main"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
              <span class="command-palette__item-badge">{{ item.workspaceLabel || '最近使用' }}</span>
            </button>
          </div>
        </section>

        <section v-for="group in groups" :key="group.key" class="command-palette__section">
          <header class="command-palette__section-head"><span>{{ group.label }}</span><small>{{ group.items.length }} 项</small></header>
          <div class="command-palette__list">
            <button v-for="item in group.items" :key="item.path" type="button" class="command-palette__item" @click="selectPath(item.path)">
              <span class="command-palette__item-short">{{ item.short || '页' }}</span>
              <span class="command-palette__item-main"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
              <span class="command-palette__item-badge">{{ item.workspaceLabel }}</span>
            </button>
          </div>
        </section>

        <section v-if="showEmpty" class="command-palette__empty"><strong>未找到匹配入口</strong><p>可尝试输入工作台名称、功能页名称或路由路径。</p></section>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';

import type { ShellCommandPaletteProps } from '../types/shell';

const props = defineProps<ShellCommandPaletteProps>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'update:query', value: string): void;
  (event: 'select', path: string): void;
}>();
const inputRef = ref<HTMLInputElement | null>(null);
const showRecent = computed(() => props.modelValue && !props.query.trim() && props.recentItems.length > 0);
const showEmpty = computed(() => props.modelValue && !showRecent.value && props.groups.length === 0);

watch(() => props.modelValue, async (open) => {
  if (!open) { emit('update:query', ''); return; }
  await nextTick();
  inputRef.value?.focus();
  inputRef.value?.select();
});

function updateQuery(event: Event) { emit('update:query', (event.target as HTMLInputElement).value || ''); }
function closePalette() { emit('update:modelValue', false); }
function selectPath(path: string) { emit('select', path); emit('update:modelValue', false); }
</script>

<style scoped>
.command-palette { position: fixed; inset: 0; z-index: 120; display: flex; align-items: flex-start; justify-content: center; padding: 7vh 1.2rem 1.2rem; }
.command-palette__mask { position: absolute; inset: 0; border: none; background: rgba(15, 23, 42, 0.36); backdrop-filter: blur(10px); }
.command-palette__panel { position: relative; z-index: 1; width: min(48rem, calc(100vw - 2rem)); max-height: 80vh; overflow-y: auto; padding: 1rem; border-radius: 1.2rem; border: 1px solid rgba(226, 232, 240, 0.88); background: rgba(255, 255, 255, 0.96); box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2); }
.command-palette__header { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 0.8rem; align-items: center; margin-bottom: 0.9rem; }
.command-palette__search { display: grid; grid-template-columns: auto 1fr; align-items: center; min-height: 3rem; border: 1px solid rgba(201, 211, 225, 0.92); border-radius: 999px; background: linear-gradient(180deg, rgba(251, 252, 254, 0.98), rgba(246, 248, 252, 0.98)); transition: border-color 160ms ease, box-shadow 160ms ease; }
.command-palette__search:focus-within { border-color: color-mix(in srgb, var(--brand) 26%, white); box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 12%, transparent); }
.command-palette__icon { position: relative; width: 2.8rem; height: 100%; display: inline-flex; align-items: center; justify-content: center; }
.command-palette__icon::before { content: ''; width: 0.72rem; height: 0.72rem; border: 1.8px solid #8ea0ba; border-radius: 50%; }
.command-palette__icon::after { content: ''; position: absolute; width: 0.36rem; height: 1.8px; background: #8ea0ba; border-radius: 999px; transform: translate(0.34rem, 0.34rem) rotate(45deg); }
.command-palette__search input { min-width: 0; border: none; outline: none; background: transparent; font-size: 0.94rem; color: var(--text-heading); padding-right: 0.9rem; }
.command-palette__close { min-height: 2.75rem; padding: 0 0.9rem; border: 1px solid rgba(208, 216, 227, 0.96); border-radius: 999px; background: linear-gradient(180deg, #ffffff, #f7f9fc); color: var(--text-secondary); font-weight: 700; }
.command-palette__section { display: grid; gap: 0.5rem; }
.command-palette__section + .command-palette__section { margin-top: 0.85rem; }
.command-palette__section-head { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; }
.command-palette__section-head span { color: var(--text-heading); font-size: 0.86rem; font-weight: 700; }
.command-palette__section-head small { color: var(--text-caption); font-size: 0.76rem; }
.command-palette__list { display: grid; gap: 0.45rem; }
.command-palette__item { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 0.75rem; width: 100%; padding: 0.78rem 0.9rem; border: 1px solid rgba(229, 234, 241, 0.98); border-radius: 1rem; background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 253, 0.98)); text-align: left; transition: border-color 160ms ease, transform 160ms ease, box-shadow 160ms ease; }
.command-palette__item:hover { border-color: color-mix(in srgb, var(--brand) 18%, white); box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08); transform: translateY(-1px); }
.command-palette__item-short { width: 1.9rem; height: 1.9rem; display: inline-flex; align-items: center; justify-content: center; border-radius: 0.72rem; background: color-mix(in srgb, var(--brand) 10%, white); color: var(--brand); font-size: 0.82rem; font-weight: 700; }
.command-palette__item-main { display: grid; gap: 0.14rem; min-width: 0; }
.command-palette__item-main strong { color: var(--text-heading); font-size: 0.92rem; }
.command-palette__item-main small { color: var(--text-caption); font-size: 0.78rem; line-height: 1.5; }
.command-palette__item-badge { color: var(--text-caption-2); font-size: 0.74rem; font-weight: 700; }
.command-palette__empty { padding: 1.4rem 1rem 0.6rem; text-align: center; }
.command-palette__empty strong { color: var(--text-heading); font-size: 0.95rem; }
.command-palette__empty p { margin: 0.4rem 0 0; color: var(--text-caption); font-size: 0.8rem; }
.command-palette-fade-enter-active, .command-palette-fade-leave-active { transition: opacity 160ms ease; }
.command-palette-fade-enter-from, .command-palette-fade-leave-to { opacity: 0; }
@media (max-width: 720px) { .command-palette { padding-top: 5vh; } .command-palette__panel { padding: 0.8rem; } .command-palette__header { grid-template-columns: 1fr; } .command-palette__item { grid-template-columns: auto minmax(0, 1fr); } .command-palette__item-badge { display: none; } }
</style>
