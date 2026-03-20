<template>
  <aside
    class="shell-sidebar-nav"
    :class="{
      'shell-sidebar-nav--collapsed': sidebarCollapsed,
      'shell-sidebar-nav--mobile': isMobile,
      'shell-sidebar-nav--mobile-open': isMobile && mobileMenuOpen
    }"
    :aria-hidden="isMobile && !mobileMenuOpen"
  >
    <div class="shell-sidebar-nav__context">
      <p class="shell-sidebar-nav__eyebrow">当前工作台</p>
      <h2>{{ group.label }}</h2>
    </div>

    <nav class="shell-sidebar-nav__menu" aria-label="二级导航">
      <el-tooltip
        v-for="item in group.items"
        :key="item.to"
        placement="right"
        effect="light"
        :content="item.caption || item.label"
        :disabled="!sidebarCollapsed"
      >
        <RouterLink
          :to="item.to"
          class="shell-sidebar-nav__item"
          :class="{ 'shell-sidebar-nav__item--active': currentRoutePath === item.to }"
          :title="item.caption ? `${item.label}：${item.caption}` : item.label"
          :aria-label="item.caption ? `${item.label}，${item.caption}` : item.label"
        >
          <span class="shell-sidebar-nav__marker">{{ sidebarCollapsed ? item.short : '' }}</span>
          <span class="shell-sidebar-nav__content">
            <strong>{{ item.label }}</strong>
          </span>
        </RouterLink>
      </el-tooltip>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';

import type { ShellSidebarNavProps } from '../types/shell';

defineProps<ShellSidebarNavProps>();
</script>

<style scoped>
.shell-sidebar-nav {
  border-right: 1px solid var(--panel-border);
  padding: 1rem 0.7rem 1.15rem;
  background: linear-gradient(180deg, rgba(250, 251, 253, 0.98), rgba(246, 248, 251, 0.98));
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.shell-sidebar-nav__context {
  padding: 0.05rem 0.1rem 0.72rem;
  border-bottom: 1px solid var(--line-soft);
}

.shell-sidebar-nav__eyebrow {
  margin: 0;
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-caption-2);
}

.shell-sidebar-nav__context h2 {
  margin: 0.34rem 0 0;
  font-size: 0.94rem;
  color: var(--text-heading);
  font-weight: 700;
}

.shell-sidebar-nav__menu {
  flex: 1 1 auto;
  display: grid;
  align-content: start;
  gap: 0.12rem;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.15rem;
  overscroll-behavior: contain;
}

.shell-sidebar-nav__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.62rem;
  align-items: center;
  text-decoration: none;
  color: #334155;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  padding: 0.58rem 0.72rem 0.58rem 0.64rem;
  transition: all 160ms ease;
  position: relative;
  min-height: 2.45rem;
}

.shell-sidebar-nav__item::before {
  content: '';
  position: absolute;
  left: -0.7rem;
  top: 0.55rem;
  bottom: 0.55rem;
  width: 3px;
  border-radius: 0 var(--radius-pill) var(--radius-pill) 0;
  background: var(--brand);
  opacity: 0;
  transition: opacity 160ms ease;
}

.shell-sidebar-nav__marker {
  width: 0.42rem;
  height: 0.42rem;
  border-radius: var(--radius-pill);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0;
  font-weight: 600;
  color: transparent;
  background: rgba(148, 163, 184, 0.55);
  flex-shrink: 0;
}

.shell-sidebar-nav__content {
  min-width: 0;
}

.shell-sidebar-nav__content strong {
  display: block;
  font-size: 0.82rem;
  line-height: 1.45;
  font-weight: 600;
}

.shell-sidebar-nav__item:hover {
  border-color: #dfe5ee;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--shadow-card-soft);
}

.shell-sidebar-nav__item:hover .shell-sidebar-nav__marker {
  background: color-mix(in srgb, var(--brand) 42%, transparent);
}

.shell-sidebar-nav__item--active {
  border-color: rgba(255, 106, 0, 0.16);
  background: linear-gradient(90deg, rgba(255, 106, 0, 0.1), rgba(255, 255, 255, 0.98));
  box-shadow: inset 0 0 0 1px rgba(255, 106, 0, 0.04);
}

.shell-sidebar-nav__item--active::before {
  opacity: 1;
}

.shell-sidebar-nav__item--active .shell-sidebar-nav__marker {
  background: var(--brand);
}

.shell-sidebar-nav__item--active .shell-sidebar-nav__content strong {
  color: var(--brand);
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__context {
  display: none;
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__item {
  grid-template-columns: 1fr;
  justify-items: center;
  padding: 0.62rem 0.45rem;
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__marker {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: var(--radius-md);
  font-size: 0.72rem;
  color: var(--brand-deep);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__item:hover .shell-sidebar-nav__marker {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__item--active .shell-sidebar-nav__marker {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__content {
  display: none;
}

.shell-sidebar-nav--collapsed .shell-sidebar-nav__item::before {
  display: none;
}

.shell-sidebar-nav--mobile {
  position: fixed;
  top: var(--shell-header-height);
  left: 0;
  width: min(78vw, 320px);
  height: calc(100vh - var(--shell-header-height));
  z-index: 80;
  transform: translateX(-102%);
  transition: transform 220ms ease;
  box-shadow: var(--shadow-side-panel);
}

.shell-sidebar-nav--mobile.shell-sidebar-nav--mobile-open {
  transform: translateX(0);
}
</style>
