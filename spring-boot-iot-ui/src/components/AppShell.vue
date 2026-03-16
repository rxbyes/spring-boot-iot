<template>
  <div class="cloud-shell" :class="{ 'cloud-shell--collapsed': sidebarCollapsed, 'cloud-shell--mobile-open': mobileMenuOpen }">
    <a class="skip-link" href="#main-content">跳到主内容</a>

    <header class="cloud-header">
      <div class="cloud-header__main">
        <button
          class="menu-trigger"
          type="button"
          :aria-label="sidebarCollapsed ? '展开侧边菜单' : '收起侧边菜单'"
          @click="toggleSidebar"
        >
          <span />
          <span />
          <span />
        </button>

        <RouterLink class="cloud-brand" to="/">
          <span class="cloud-brand__logo" aria-hidden="true"></span>
          <span class="cloud-brand__name">监测预警平台</span>
        </RouterLink>

        <label class="header-search" for="global-search">
          <input
            id="global-search"
            v-model="searchKeyword"
            type="search"
            placeholder="搜索业务模块，如：告警中心、事件处置、风险点"
            @keydown.enter="handleSearch"
          />
          <button type="button" @click="handleSearch">搜索</button>
        </label>

        <div class="header-status">
          <span class="status-chip status-chip--brand">动态权限菜单</span>
          <span class="status-chip">{{ headerIdentity }}</span>
        </div>
      </div>

      <nav class="cloud-header__sections" aria-label="一级导航">
        <button
          v-for="group in navigationGroups"
          :key="group.key"
          type="button"
          class="section-tab"
          :class="{ 'section-tab--active': activeGroup.key === group.key }"
          @click="switchGroup(group.key)"
        >
          <span>{{ group.label }}</span>
          <small>{{ group.description }}</small>
        </button>
      </nav>
    </header>

    <div class="cloud-layout">
      <aside class="cloud-sidebar" :aria-hidden="isMobile && !mobileMenuOpen">
        <div class="sidebar-context">
          <p class="sidebar-context__eyebrow">{{ activeGroup.label }}</p>
          <h2>{{ activeGroup.menuTitle }}</h2>
          <p>{{ activeGroup.menuHint }}</p>
        </div>

        <nav class="side-menu" aria-label="二级导航">
          <RouterLink
            v-for="item in activeGroup.items"
            :key="item.to"
            :to="item.to"
            class="side-menu__item"
            :class="{ 'side-menu__item--active': route.path === item.to }"
            :title="item.label"
          >
            <span class="side-menu__marker">{{ item.short }}</span>
            <span class="side-menu__content">
              <strong>{{ item.label }}</strong>
              <small>{{ item.caption }}</small>
            </span>
          </RouterLink>
        </nav>
      </aside>

      <section class="cloud-content">
        <section class="console-toolbar">
          <div class="console-toolbar__heading">
            <p class="toolbar-eyebrow">{{ activeGroup.label }}</p>
            <h1>{{ activeTitle }}</h1>
            <p>{{ activeDescription }}</p>
          </div>

          <div class="console-toolbar__summary">
            <div class="toolbar-tags">
              <span class="toolbar-tag">{{ activeGroup.description }}</span>
              <span class="toolbar-tag">{{ environmentLabel }}</span>
              <span class="toolbar-tag">{{ authStatusLabel }}</span>
            </div>

            <div class="console-toolbar__actions">
              <button
                type="button"
                class="toolbar-button toolbar-button--ghost"
                @click="showAccessPanel = !showAccessPanel"
              >
                {{ showAccessPanel ? '收起接入设置' : '接入设置' }}
              </button>
              <button
                v-if="permissionStore.isLoggedIn"
                type="button"
                class="toolbar-button"
                @click="handleLogout"
              >
                退出登录
              </button>
            </div>
          </div>
        </section>

        <transition name="console-settings">
          <section v-if="showAccessPanel" class="console-settings">
            <div class="console-settings__intro">
              <p>接入配置</p>
              <h2>权限与菜单来自真实数据库，前端不再内置角色菜单硬编码。</h2>
              <span>{{ environmentValue }}</span>
            </div>

            <div class="console-settings__content">
              <label class="toolbar-field toolbar-field--wide">
                <span>接入地址</span>
                <input
                  v-model="baseUrlDraft"
                  class="toolbar-input"
                  name="api_base_url"
                  type="url"
                  autocomplete="url"
                  spellcheck="false"
                  placeholder="留空使用当前站点同源地址，或填写外部网关地址"
                  @keydown.enter="saveApiBaseUrl"
                />
              </label>

              <button type="button" class="toolbar-button toolbar-button--primary" @click="saveApiBaseUrl">应用地址</button>

              <template v-if="!permissionStore.isLoggedIn">
                <button type="button" class="toolbar-button toolbar-button--success" @click="goToLogin">
                  前往登录
                </button>
              </template>

              <div v-else class="logged-user-card">
                <strong>{{ permissionStore.displayName || permissionStore.userInfo?.username }}</strong>
                <span>{{ loggedUserHint }}</span>
              </div>
            </div>
          </section>
        </transition>

        <TabsView />

        <main id="main-content" class="content-frame">
          <RouterView />
        </main>
      </section>
    </div>

    <button
      v-if="isMobile && mobileMenuOpen"
      class="sidebar-mask"
      type="button"
      aria-label="关闭菜单"
      @click="mobileMenuOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';

import { usePermissionStore } from '../stores/permission';
import { runtimeState, setApiBaseUrl } from '../stores/runtime';
import type { MenuTreeNode } from '../types/auth';
import TabsView from './TabsView.vue';

interface NavItem {
  to: string;
  label: string;
  caption: string;
  short: string;
}

interface NavGroup {
  key: string;
  label: string;
  description: string;
  menuTitle: string;
  menuHint: string;
  items: NavItem[];
}

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();

const guestGroup: NavGroup = {
  key: 'guest-overview',
  label: '平台首页',
  description: '游客预览',
  menuTitle: '监测预警平台首页',
  menuHint: '未登录时仅开放首页预览与登录入口。',
  items: [{ to: '/', label: '首页总览', caption: '平台定位、能力边界与登录入口说明', short: '首' }]
};

function buildNavItem(node: MenuTreeNode): NavItem {
  return {
    to: node.path || '/',
    label: node.menuName,
    caption: node.meta?.caption || node.meta?.description || '基于角色动态装载的业务入口',
    short: node.meta?.shortLabel || node.menuName.trim().slice(0, 1) || '导'
  };
}

function collectGroupItems(node: MenuTreeNode): NavItem[] {
  const items: NavItem[] = [];

  const visit = (current: MenuTreeNode) => {
    if (current.type !== 2 && current.path) {
      items.push(buildNavItem(current));
    }
    current.children?.forEach((child) => {
      if (child.type !== 2) {
        visit(child);
      }
    });
  };

  visit(node);
  return Array.from(new Map(items.map((item) => [item.to, item])).values());
}

function buildNavigationGroup(node: MenuTreeNode): NavGroup {
  return {
    key: node.menuCode || `menu-${node.id}`,
    label: node.menuName,
    description: node.meta?.description || '角色授权的一级导航分组',
    menuTitle: node.meta?.menuTitle || node.menuName,
    menuHint: node.meta?.menuHint || node.meta?.description || '根据当前账号角色动态加载可访问页面。',
    items: collectGroupItems(node)
  };
}

const searchKeyword = ref('');
const baseUrlDraft = ref(runtimeState.apiBaseUrl);
const showAccessPanel = ref(false);

const isMobile = ref(false);
const mobileMenuOpen = ref(false);
const sidebarCollapsed = ref(false);

const navigationGroups = computed<NavGroup[]>(() => {
  if (!permissionStore.isLoggedIn) {
    return [guestGroup];
  }

  const groups = permissionStore.menus
    .map(buildNavigationGroup)
    .filter((group) => group.items.length > 0);

  if (groups.length > 0) {
    return groups;
  }

  return [
    {
      key: 'empty-auth',
      label: '未分配菜单',
      description: '权限待配置',
      menuTitle: '当前账号暂无业务菜单',
      menuHint: '请联系超级管理员分配角色和菜单权限。',
      items: [{ to: '/', label: '平台首页', caption: '可先查看首页与当前登录信息', short: '首' }]
    }
  ];
});

const flattenedItems = computed(() => navigationGroups.value.flatMap((group) => group.items));

const activeGroup = computed(() => {
  const matchedGroup = navigationGroups.value.find((group) => group.items.some((item) => item.to === route.path));
  return matchedGroup || navigationGroups.value[0] || guestGroup;
});

const activeMenuItem = computed(() => flattenedItems.value.find((item) => item.to === route.path) || null);

watch(
  () => runtimeState.apiBaseUrl,
  (value) => {
    baseUrlDraft.value = value;
  }
);

watch(
  () => route.path,
  () => {
    if (isMobile.value) {
      mobileMenuOpen.value = false;
    }
    showAccessPanel.value = false;
  }
);

const activeTitle = computed(() => activeMenuItem.value?.label || String(route.meta.title || '平台首页'));
const activeDescription = computed(() => {
  return activeMenuItem.value?.caption || String(route.meta.description || '围绕告警、事件、风险点和设备健康组织平台能力。');
});
const headerIdentity = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '访客模式';
  }
  if (permissionStore.primaryRoleName) {
    return `${permissionStore.displayName} · ${permissionStore.primaryRoleName}`;
  }
  return permissionStore.displayName || permissionStore.userInfo?.username || '已登录';
});
const environmentLabel = computed(() => (runtimeState.apiBaseUrl ? '外部网关接入' : '当前站点同源接入'));
const environmentValue = computed(() => runtimeState.apiBaseUrl || '当前站点同源访问 /api');
const authStatusLabel = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '未登录，仅开放首页预览';
  }
  return `已登录 · ${permissionStore.roleNames.join(' / ') || '未分配角色'}`;
});
const loggedUserHint = computed(() => {
  const roleText = permissionStore.roleNames.join(' / ') || '未分配角色';
  return `当前角色：${roleText}，菜单与按钮权限均已按数据库授权动态生效。`;
});

function switchGroup(groupKey: string) {
  const group = navigationGroups.value.find((item) => item.key === groupKey);
  if (!group || group.items.length === 0) {
    return;
  }

  if (!group.items.some((item) => item.to === route.path)) {
    router.push(group.items[0].to);
  }
}

function handleSearch() {
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    ElMessage.info('请输入要跳转的业务模块关键词');
    return;
  }

  const lowerKeyword = keyword.toLowerCase();
  const target = flattenedItems.value.find((item) => {
    return item.label.toLowerCase().includes(lowerKeyword) || item.caption.toLowerCase().includes(lowerKeyword);
  });

  if (!target) {
    ElMessage.warning('未找到匹配模块，请尝试更换关键词');
    return;
  }

  router.push(target.to);
}

function saveApiBaseUrl() {
  setApiBaseUrl(baseUrlDraft.value);
  ElMessage.success('接入地址已更新');
}

function goToLogin() {
  router.push('/login');
}

function updateViewportState() {
  isMobile.value = window.matchMedia('(max-width: 1200px)').matches;
  if (isMobile.value) {
    sidebarCollapsed.value = false;
    return;
  }

  mobileMenuOpen.value = false;
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileMenuOpen.value = !mobileMenuOpen.value;
    return;
  }

  sidebarCollapsed.value = !sidebarCollapsed.value;
}

function handleLogout() {
  permissionStore.logout();
  router.push('/login');
  ElMessage.success('已退出登录');
}

onMounted(() => {
  updateViewportState();
  window.addEventListener('resize', updateViewportState);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportState);
});
</script>

<style scoped>
.cloud-shell {
  min-height: 100vh;
  color: #1f2329;
  background:
    radial-gradient(circle at 10% -10%, rgba(255, 106, 0, 0.08), transparent 24%),
    radial-gradient(circle at 92% 0, rgba(76, 143, 255, 0.1), transparent 18%),
    linear-gradient(180deg, #f7f9fc 0%, #eff3fa 100%);
}

.skip-link {
  position: absolute;
  top: -40px;
  left: 1rem;
  z-index: 120;
  padding: 0.7rem 1rem;
  border-radius: 0.5rem;
  background: #ff6a00;
  color: #fff;
}

.skip-link:focus {
  top: 1rem;
}

.cloud-header {
  position: sticky;
  top: 0;
  z-index: 90;
  border-bottom: 1px solid #e6ebf4;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 26px rgba(20, 36, 77, 0.06);
}

.cloud-header__main {
  display: grid;
  grid-template-columns: auto auto minmax(240px, 1fr) auto;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.2rem;
}

.menu-trigger {
  width: 2.25rem;
  height: 2.25rem;
  padding: 0;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  border: 1px solid #d8dfeb;
  border-radius: 0.5rem;
  background: #fff;
}

.menu-trigger span {
  width: 0.95rem;
  height: 2px;
  margin: 0 auto;
  border-radius: 999px;
  background: #45556f;
}

.cloud-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  color: #1f2329;
  text-decoration: none;
  min-width: 11.5rem;
}

.cloud-brand__logo {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 0.45rem;
  background: linear-gradient(145deg, #ff6a00, #ff9b42);
  position: relative;
  box-shadow: 0 6px 14px rgba(255, 106, 0, 0.26);
}

.cloud-brand__logo::before {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 0.3rem;
  border: 2px solid rgba(255, 255, 255, 0.72);
}

.cloud-brand__name {
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.header-search {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  min-width: 0;
  border: 1px solid #d8dfeb;
  border-radius: 999px;
  background: #f8fafd;
  overflow: hidden;
}

.header-search input {
  border: none;
  background: transparent;
  padding: 0.6rem 0.9rem;
  min-width: 0;
}

.header-search input:focus {
  outline: none;
}

.header-search button {
  border: none;
  border-left: 1px solid #d8dfeb;
  border-radius: 0;
  height: 100%;
  padding: 0 1rem;
  background: #fff;
  color: #334155;
}

.header-status {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.42rem 0.8rem;
  border-radius: 999px;
  border: 1px solid #dbe4f1;
  background: rgba(255, 255, 255, 0.88);
  color: #53647d;
  font-size: 0.8rem;
}

.status-chip--brand {
  border-color: rgba(255, 106, 0, 0.18);
  background: rgba(255, 106, 0, 0.1);
  color: #ff6a00;
}

.cloud-header__sections {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  padding: 0 1.2rem 0.72rem;
}

.section-tab {
  border: 1px solid transparent;
  border-radius: 0.6rem;
  background: transparent;
  color: #4c5c75;
  padding: 0.55rem 0.85rem;
  display: grid;
  gap: 0.12rem;
  text-align: left;
}

.section-tab span {
  font-size: 0.92rem;
  font-weight: 600;
}

.section-tab small {
  font-size: 0.72rem;
  color: #7f8ca1;
}

.section-tab:hover {
  background: #f2f6fd;
}

.section-tab--active {
  border-color: rgba(255, 106, 0, 0.24);
  background: linear-gradient(180deg, rgba(255, 106, 0, 0.12), rgba(255, 106, 0, 0.06));
  color: #ff6a00;
}

.section-tab--active small {
  color: #cc6f2f;
}

.cloud-layout {
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  min-height: calc(100vh - 132px);
}

.cloud-shell--collapsed .cloud-layout {
  grid-template-columns: 88px minmax(0, 1fr);
}

.cloud-sidebar {
  border-right: 1px solid #e2e9f3;
  padding: 1rem 0.75rem;
  background: linear-gradient(180deg, #f6f8fc, #edf2fa);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-context {
  padding: 0.8rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid #dee5f1;
  background: rgba(255, 255, 255, 0.92);
}

.sidebar-context__eyebrow {
  margin: 0;
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #8490a5;
}

.sidebar-context h2 {
  margin: 0.35rem 0;
  font-size: 1rem;
}

.sidebar-context p {
  margin: 0;
  color: #637084;
  line-height: 1.6;
  font-size: 0.83rem;
}

.side-menu {
  display: grid;
  gap: 0.5rem;
  overflow-y: auto;
  padding-right: 0.15rem;
}

.side-menu__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.65rem;
  align-items: start;
  text-decoration: none;
  color: #334155;
  border: 1px solid transparent;
  border-radius: 0.65rem;
  padding: 0.62rem 0.65rem;
  transition: all 160ms ease;
}

.side-menu__marker {
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 0.45rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.76rem;
  font-weight: 600;
  color: #ff6a00;
  background: rgba(255, 106, 0, 0.12);
}

.side-menu__content {
  min-width: 0;
}

.side-menu__content strong,
.side-menu__content small {
  display: block;
}

.side-menu__content strong {
  font-size: 0.88rem;
}

.side-menu__content small {
  margin-top: 0.22rem;
  color: #728197;
  font-size: 0.76rem;
  line-height: 1.45;
}

.side-menu__item:hover {
  border-color: #d6deeb;
  background: rgba(255, 255, 255, 0.7);
}

.side-menu__item--active {
  border-color: rgba(255, 106, 0, 0.24);
  background: linear-gradient(120deg, rgba(255, 106, 0, 0.14), rgba(255, 106, 0, 0.04));
}

.cloud-shell--collapsed .sidebar-context {
  display: none;
}

.cloud-shell--collapsed .side-menu__item {
  grid-template-columns: 1fr;
  justify-items: center;
  padding: 0.55rem;
}

.cloud-shell--collapsed .side-menu__content {
  display: none;
}

.cloud-content {
  padding: 1rem 1.2rem 1.3rem;
  min-width: 0;
}

.console-toolbar {
  padding: 0.95rem 1rem;
  border-radius: 0.9rem;
  border: 1px solid #dde5f1;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 24px rgba(31, 49, 90, 0.07);
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.console-toolbar__heading {
  display: grid;
  gap: 0.35rem;
  max-width: 40rem;
}

.toolbar-eyebrow {
  margin: 0;
  color: #8290a6;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.7rem;
}

.console-toolbar__heading h1 {
  margin: 0;
  color: #1f2a3d;
  font-size: clamp(1.28rem, 1.8vw, 1.66rem);
}

.console-toolbar__heading p {
  margin: 0;
  color: #5c6d84;
  line-height: 1.6;
}

.console-toolbar__summary {
  display: grid;
  gap: 0.7rem;
  justify-items: end;
}

.toolbar-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.toolbar-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.38rem 0.75rem;
  border-radius: 999px;
  border: 1px solid #d9e1ed;
  background: #fff;
  color: #52637c;
  font-size: 0.78rem;
}

.console-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
  align-items: center;
}

.console-settings {
  margin-top: 0.9rem;
  padding: 1rem;
  border-radius: 0.9rem;
  border: 1px solid #dde5f1;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.95)),
    radial-gradient(circle at top right, rgba(255, 106, 0, 0.12), transparent 38%);
  box-shadow: 0 10px 24px rgba(31, 49, 90, 0.06);
}

.console-settings__intro {
  display: grid;
  gap: 0.3rem;
}

.console-settings__intro p {
  margin: 0;
  color: #8290a6;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.7rem;
}

.console-settings__intro h2 {
  margin: 0;
  font-size: 1.05rem;
  color: #1f2a3d;
}

.console-settings__intro span {
  color: #5f7088;
  font-size: 0.85rem;
}

.console-settings__content {
  margin-top: 1rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: flex-end;
}

.toolbar-field {
  display: grid;
  gap: 0.32rem;
  min-width: 10rem;
}

.toolbar-field--wide {
  min-width: min(24rem, 100%);
}

.toolbar-field span {
  font-size: 0.68rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8490a5;
}

.toolbar-input {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.68rem 0.85rem;
  border: 1px solid #d5deeb;
  border-radius: 0.62rem;
  background: #fff;
  color: #1f2a3d;
  font-size: 0.92rem;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.toolbar-input::placeholder {
  color: #8b97ab;
}

.toolbar-input:focus {
  outline: none;
  border-color: rgba(255, 106, 0, 0.48);
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.12);
}

.toolbar-button {
  min-height: 2.5rem;
  padding: 0.62rem 1rem;
  border: 1px solid #d8e0ec;
  border-radius: 0.55rem;
  background: #fff;
  color: #43556d;
  font-weight: 600;
  font-size: 0.9rem;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.toolbar-button:hover {
  border-color: #c7d3e3;
  background: #f8fbff;
  box-shadow: 0 10px 20px rgba(31, 49, 90, 0.08);
}

.toolbar-button:focus-visible {
  outline: none;
  border-color: rgba(255, 106, 0, 0.48);
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.14);
}

.toolbar-button:active {
  transform: translateY(1px);
}

.toolbar-button--primary {
  border-color: transparent;
  background: linear-gradient(135deg, #ff7b1a, #ff9b42);
  color: #fff;
}

.toolbar-button--primary:hover {
  border-color: transparent;
  background: linear-gradient(135deg, #f47216, #ff9638);
}

.toolbar-button--success {
  border-color: transparent;
  background: linear-gradient(135deg, #1fa46d, #34c88d);
  color: #fff;
}

.toolbar-button--success:hover {
  border-color: transparent;
  background: linear-gradient(135deg, #1c9a66, #2fbc84);
}

.logged-user-card {
  display: grid;
  gap: 0.18rem;
  min-height: 2.5rem;
  padding: 0.8rem 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid #d9e1ed;
  background: rgba(255, 255, 255, 0.9);
  color: #445671;
}

.logged-user-card strong {
  font-size: 0.92rem;
  color: #1f2a3d;
}

.logged-user-card span {
  font-size: 0.8rem;
  line-height: 1.5;
}

.console-settings-enter-active,
.console-settings-leave-active {
  transition: all 180ms ease;
}

.console-settings-enter-from,
.console-settings-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.toolbar-button--ghost {
  border-style: dashed;
}

.content-frame {
  margin-top: 0.95rem;
}

.sidebar-mask {
  position: fixed;
  inset: 0;
  border: none;
  background: rgba(16, 23, 38, 0.45);
  z-index: 70;
}

@media (max-width: 1400px) {
  .cloud-header__main {
    grid-template-columns: auto auto minmax(200px, 1fr) auto;
  }
}

@media (max-width: 1200px) {
  .cloud-layout {
    grid-template-columns: 1fr;
  }

  .cloud-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    width: min(78vw, 320px);
    height: 100vh;
    z-index: 80;
    transform: translateX(-102%);
    transition: transform 220ms ease;
    box-shadow: 12px 0 24px rgba(16, 29, 57, 0.18);
  }

  .cloud-shell--mobile-open .cloud-sidebar {
    transform: translateX(0);
  }

  .cloud-content {
    padding: 0.92rem;
  }

  .console-toolbar {
    flex-direction: column;
  }

  .console-toolbar__summary {
    justify-items: start;
  }

  .toolbar-tags,
  .console-toolbar__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .cloud-header__main {
    grid-template-columns: auto auto 1fr;
  }

  .header-search {
    grid-column: 1 / -1;
    order: 5;
  }

  .header-status {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }

  .cloud-header__sections {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .section-tab {
    min-width: 8.2rem;
  }

  .console-settings__content {
    align-items: stretch;
  }

  .toolbar-field,
  .toolbar-field--wide,
  .logged-user-card {
    min-width: 100%;
  }
}
</style>
