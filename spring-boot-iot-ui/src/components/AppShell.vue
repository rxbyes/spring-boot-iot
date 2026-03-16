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
          <nav class="header-links" aria-label="快捷功能">
            <a href="#" @click.prevent>费用</a>
            <a href="#" @click.prevent>备案</a>
            <a href="#" @click.prevent>企业</a>
            <a href="#" @click.prevent>支持</a>
          </nav>
          <div class="header-tools" aria-label="系统工具">
            <button type="button" class="tool-icon" aria-label="工作台" @click="goWorkbench">⌂</button>
            <button
              type="button"
              class="tool-icon"
              :class="{ 'tool-icon--active': showNoticePanel }"
              aria-label="消息通知"
              @click="toggleNoticePanel"
            >
              🔔
            </button>
            <button
              type="button"
              class="tool-icon"
              :class="{ 'tool-icon--active': showHelpPanel }"
              aria-label="帮助中心"
              @click="toggleHelpPanel"
            >
              ?
            </button>
          </div>
          <div class="account-chip" :title="headerIdentity">
            <span class="account-chip__avatar">{{ accountInitial }}</span>
            <span class="account-chip__meta">
              <strong>{{ headerAccountName }}</strong>
              <small>{{ headerRoleName }}</small>
            </span>
          </div>
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

      <transition name="header-pop">
        <section v-if="showNoticePanel" class="header-popover header-popover--notice" aria-label="消息通知面板">
          <div class="header-popover__title">
            <strong>消息通知</strong>
            <small>最近操作与系统提醒</small>
          </div>
          <ul class="header-popover__list">
            <li v-for="item in noticeItems" :key="item.id">
              <button type="button" @click="openNotice(item.path)">
                <strong>{{ item.title }}</strong>
                <span>{{ item.time }}</span>
              </button>
            </li>
          </ul>
        </section>
      </transition>

      <transition name="header-pop">
        <section v-if="showHelpPanel" class="header-popover header-popover--help" aria-label="帮助中心面板">
          <div class="header-popover__title">
            <strong>帮助中心</strong>
            <small>常用入口与使用说明</small>
          </div>
          <ul class="header-popover__list">
            <li v-for="item in helpItems" :key="item.label">
              <button type="button" @click="openHelp(item.path)">
                <strong>{{ item.label }}</strong>
                <span>{{ item.caption }}</span>
              </button>
            </li>
          </ul>
        </section>
      </transition>
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
              <span v-if="showGroupDescriptionTag" class="toolbar-tag">{{ activeGroup.description }}</span>
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
              <h2>导航采用统一控制台模板，按钮权限仍按数据库授权控制。</h2>
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

import { activityEntries } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import { runtimeState, setApiBaseUrl } from '../stores/runtime';
import { formatDateTime } from '../utils/format';
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

const docFallbackGroups: NavGroup[] = [
  {
    key: 'iot-core',
    label: '设备接入',
    description: '接入与数据校验',
    menuTitle: '设备接入与运维',
    menuHint: '覆盖产品建模、设备建档、接入验证与数据洞察。',
    items: [
      { to: '/products', label: '产品模板中心', caption: '产品模板建模与协议绑定', short: '产' },
      { to: '/devices', label: '设备运维中心', caption: '设备建档、在线状态与运维管理', short: '设' },
      { to: '/reporting', label: '接入验证中心', caption: 'HTTP 上报与主链路验证', short: '验' },
      { to: '/insight', label: '监测对象工作台', caption: '设备属性、日志与研判线索', short: '洞' },
      { to: '/file-debug', label: '数据完整性校验', caption: '文件快照与固件聚合调试', short: '校' }
    ]
  },
  {
    key: 'risk-core',
    label: '风险处置',
    description: '告警、事件、规则与报表',
    menuTitle: '风险预警与处置',
    menuHint: '覆盖告警中心、事件闭环、风险配置与分析报表。',
    items: [
      { to: '/alarm-center', label: '告警中心', caption: '告警列表、详情、确认、抑制、关闭', short: '告' },
      { to: '/event-disposal', label: '事件处置', caption: '事件派发、工单流转与闭环', short: '事' },
      { to: '/risk-point', label: '风险点管理', caption: '风险点 CRUD 与设备绑定', short: '险' },
      { to: '/rule-definition', label: '阈值规则', caption: '阈值规则定义与启停管理', short: '阈' },
      { to: '/linkage-rule', label: '联动规则', caption: '联动触发条件与动作配置', short: '联' },
      { to: '/emergency-plan', label: '应急预案', caption: '应急预案维护与执行准备', short: '预' },
      { to: '/report-analysis', label: '分析报表', caption: '风险趋势、告警统计与事件闭环', short: '报' }
    ]
  },
  {
    key: 'system-core',
    label: '系统管理',
    description: '组织与权限治理',
    menuTitle: '系统治理与权限',
    menuHint: '覆盖组织、用户、角色、菜单、区域、字典、通知与审计。',
    items: [
      { to: '/organization', label: '组织管理', caption: '组织树维护与层级治理', short: '组' },
      { to: '/user', label: '用户管理', caption: '用户档案、状态与角色分配', short: '用' },
      { to: '/role', label: '角色管理', caption: '角色与菜单权限授权', short: '角' },
      { to: '/menu', label: '菜单管理', caption: '菜单树结构与页面权限项维护', short: '菜' },
      { to: '/region', label: '区域管理', caption: '区域树维护与引用配置', short: '区' },
      { to: '/dict', label: '字典配置', caption: '字典分类维护与编码查询', short: '字' },
      { to: '/channel', label: '通知渠道', caption: '通知渠道增删改查', short: '通' },
      { to: '/audit-log', label: '审计日志', caption: '关键操作审计查询', short: '审' }
    ]
  },
  {
    key: 'risk-enhance',
    label: '风险增强',
    description: '实时监测与 GIS',
    menuTitle: '风险监测增强能力',
    menuHint: '实时监测与 GIS 态势页面，按阶段逐步纳入验收。',
    items: [
      { to: '/risk-monitoring', label: '实时监测', caption: '监测列表与统一详情抽屉', short: '实' },
      { to: '/risk-monitoring-gis', label: 'GIS 风险态势', caption: '风险点位态势与详情联动', short: '图' }
    ]
  }
];

function cloneGroups(groups: NavGroup[]): NavGroup[] {
  return groups.map((group) => ({
    ...group,
    items: group.items.map((item) => ({ ...item }))
  }));
}

const searchKeyword = ref('');
const baseUrlDraft = ref(runtimeState.apiBaseUrl);
const showAccessPanel = ref(false);

const isMobile = ref(false);
const mobileMenuOpen = ref(false);
const sidebarCollapsed = ref(false);
const showNoticePanel = ref(false);
const showHelpPanel = ref(false);
const staticNavigationGroups = cloneGroups(docFallbackGroups);

const navigationGroups = computed<NavGroup[]>(() => {
  if (!permissionStore.isLoggedIn) {
    return [guestGroup];
  }
  return staticNavigationGroups;
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
    showNoticePanel.value = false;
    showHelpPanel.value = false;
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
  return '系统管理员 · 超级管理员';
});
const headerAccountName = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '访客账号';
  }
  return permissionStore.displayName || permissionStore.userInfo?.username || '系统管理员';
});
const headerRoleName = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '未登录';
  }
  return permissionStore.primaryRoleName || '超级管理员';
});
const accountInitial = computed(() => {
  const source = headerAccountName.value.trim();
  return source ? source.slice(0, 1).toUpperCase() : '管';
});
const noticeItems = computed(() => {
  const fromActivity = activityEntries.value.slice(0, 4).map((item) => ({
    id: item.id,
    title: `${item.module} · ${item.action}`,
    time: formatDateTime(item.createdAt),
    path: route.path
  }));

  if (fromActivity.length > 0) {
    return fromActivity;
  }

  return [
    { id: 'notice-1', title: '系统导航已升级为统一控制台样式', time: '刚刚', path: '/' },
    { id: 'notice-2', title: '按钮权限仍按数据库角色授权控制', time: '刚刚', path: '/role' },
    { id: 'notice-3', title: '可在接入设置中切换 API 网关地址', time: '刚刚', path: route.path }
  ];
});
const helpItems = [
  { label: '平台首页', caption: '查看系统总览和业务入口', path: '/' },
  { label: '接入验证中心', caption: '验证 HTTP 上报与链路解析', path: '/reporting' },
  { label: '演进蓝图', caption: '查看规划能力与后续路线', path: '/future-lab' },
  { label: '系统角色管理', caption: '维护角色与权限关系', path: '/role' }
];
const environmentLabel = computed(() => (runtimeState.apiBaseUrl ? '外部网关接入' : '当前站点同源接入'));
const environmentValue = computed(() => runtimeState.apiBaseUrl || '当前站点同源访问 /api');
const showGroupDescriptionTag = computed(() => route.path !== '/');
const authStatusLabel = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '未登录，仅开放首页预览';
  }
  return '已登录 · 超级管理员';
});
const loggedUserHint = computed(() => {
  const roleText = permissionStore.roleNames.join(' / ') || '未分配角色';
  return `当前角色：${roleText}，一级导航使用统一模板，按钮权限按数据库授权控制。`;
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

function goWorkbench() {
  router.push('/');
}

function toggleNoticePanel() {
  showNoticePanel.value = !showNoticePanel.value;
  if (showNoticePanel.value) {
    showHelpPanel.value = false;
  }
}

function toggleHelpPanel() {
  showHelpPanel.value = !showHelpPanel.value;
  if (showHelpPanel.value) {
    showNoticePanel.value = false;
  }
}

function openNotice(path: string) {
  showNoticePanel.value = false;
  router.push(path);
}

function openHelp(path: string) {
  showHelpPanel.value = false;
  router.push(path);
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
  --shell-max-width: 1760px;
  --shell-gutter: clamp(12px, 2vw, 32px);
  min-height: 100vh;
  color: #1f2329;
  background: #f5f7fa;
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
  border-bottom: 1px solid #e6eaf0;
  background: #fff;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.08);
}

.cloud-header__main {
  display: grid;
  grid-template-columns: auto auto minmax(320px, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  padding: 0.42rem 0;
}

.menu-trigger {
  width: 2rem;
  height: 2rem;
  padding: 0;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  border: 1px solid #d8dfeb;
  border-radius: 4px;
  background: #fafbfc;
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
  gap: 0.5rem;
  color: #1f2329;
  text-decoration: none;
  min-width: 10.5rem;
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
  font-size: 0.9rem;
  font-weight: 600;
  letter-spacing: 0;
}

.header-search {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  justify-self: center;
  width: min(100%, 760px);
  height: 2rem;
  min-width: 0;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
  background: #fff;
  overflow: hidden;
}

.header-search input {
  border: none;
  background: transparent;
  padding: 0.42rem 0.72rem;
  min-width: 0;
  font-size: 0.82rem;
}

.header-search input:focus {
  outline: none;
}

.header-search button {
  border: none;
  border-left: 1px solid #d8dfeb;
  border-radius: 0;
  height: 100%;
  padding: 0 0.9rem;
  background: #f7f8fa;
  color: #4b5565;
  font-size: 0.8rem;
}

.header-status {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.55rem;
}

.header-links {
  display: inline-flex;
  align-items: center;
  gap: 0.62rem;
}

.header-links a {
  color: #4f5969;
  text-decoration: none;
  font-size: 0.76rem;
  line-height: 1;
}

.header-links a:hover {
  color: #1677ff;
}

.header-tools {
  display: inline-flex;
  align-items: center;
  gap: 0.34rem;
}

.tool-icon {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 999px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #3e4e66;
  font-size: 0.74rem;
  line-height: 1;
  padding: 0;
}

.tool-icon:hover {
  border-color: #bcd1ef;
  color: #1677ff;
  background: #f4f8ff;
}

.tool-icon--active {
  border-color: #a8c4ef;
  color: #1677ff;
  background: #eef5ff;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.82rem;
  border-radius: 999px;
  border: 1px solid #dbe4f1;
  background: rgba(255, 255, 255, 0.88);
  color: #53647d;
  font-size: 0.78rem;
}

.status-chip--identity {
  border-color: #d6e5ff;
  background: #f4f8ff;
  color: #1d4fba;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2rem;
  padding: 0.22rem 0.5rem 0.22rem 0.3rem;
  border-radius: 2px;
  border: 1px solid #e6eaf0;
  background: #fff;
}

.account-chip__avatar {
  width: 1.4rem;
  height: 1.4rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(160deg, #4d8bff, #2f66da);
}

.account-chip__meta {
  display: grid;
  gap: 0.05rem;
  line-height: 1.2;
}

.account-chip__meta strong {
  font-size: 0.76rem;
  font-weight: 600;
  color: #1f3558;
}

.account-chip__meta small {
  font-size: 0.68rem;
  color: #5e769e;
}

.cloud-header__sections {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  gap: 0;
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  padding: 0;
}

.header-popover {
  position: absolute;
  top: calc(100% - 0.2rem);
  right: max(calc((100vw - var(--shell-max-width)) / 2), var(--shell-gutter));
  width: min(25rem, calc(100vw - var(--shell-gutter) * 2));
  border: 1px solid #dbe6f5;
  border-radius: 0.85rem;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 28px rgba(22, 43, 77, 0.14);
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
  color: #203557;
  font-size: 0.9rem;
}

.header-popover__title small {
  color: #6780a4;
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
  border: 1px solid #e5edf8;
  border-radius: 0.68rem;
  background: #f8fbff;
  padding: 0.56rem 0.62rem;
  text-align: left;
  display: grid;
  gap: 0.18rem;
  color: #304766;
}

.header-popover__list li button strong {
  font-size: 0.82rem;
  font-weight: 600;
}

.header-popover__list li button span {
  font-size: 0.72rem;
  color: #6681a7;
}

.header-popover__list li button:hover {
  border-color: #bfd3f0;
  background: #f1f7ff;
}

.header-pop-enter-active,
.header-pop-leave-active {
  transition: all 140ms ease;
}

.header-pop-enter-from,
.header-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.section-tab {
  border: none;
  border-radius: 0;
  background: transparent;
  color: #3f4653;
  padding: 0.55rem 1.2rem;
  display: grid;
  gap: 0;
  text-align: center;
  position: relative;
}

.section-tab span {
  font-size: 0.84rem;
  font-weight: 500;
}

.section-tab small {
  display: none;
}

.section-tab:hover {
  color: #1677ff;
}

.section-tab--active {
  color: #1677ff;
}

.section-tab--active::after {
  content: '';
  position: absolute;
  left: 20%;
  right: 20%;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: #1677ff;
}

.cloud-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  min-height: calc(100vh - 104px);
}

.cloud-shell--collapsed .cloud-layout {
  grid-template-columns: 88px minmax(0, 1fr);
}

.cloud-sidebar {
  border-right: 1px solid #e6eaf0;
  padding: 0.7rem 0.5rem;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-context {
  padding: 0.7rem 0.65rem;
  border-radius: 4px;
  border: 1px solid #e9edf3;
  background: #fafbfd;
}

.sidebar-context__eyebrow {
  margin: 0;
  font-size: 0.66rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #8490a5;
}

.sidebar-context h2 {
  margin: 0.35rem 0;
  font-size: 0.92rem;
}

.sidebar-context p {
  margin: 0;
  color: #637084;
  line-height: 1.6;
  font-size: 0.78rem;
}

.side-menu {
  display: grid;
  gap: 0.25rem;
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
  border-radius: 4px;
  padding: 0.5rem 0.52rem;
  transition: all 160ms ease;
}

.side-menu__marker {
  width: 1.45rem;
  height: 1.45rem;
  border-radius: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
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
  font-size: 0.82rem;
}

.side-menu__content small {
  margin-top: 0.22rem;
  color: #728197;
  font-size: 0.72rem;
  line-height: 1.45;
}

.side-menu__item:hover {
  border-color: #d6deeb;
  background: rgba(255, 255, 255, 0.7);
}

.side-menu__item--active {
  border-color: #cfe1ff;
  background: #edf4ff;
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
  padding: 0.75rem 0.85rem 1rem;
  min-width: 0;
}

.console-toolbar {
  padding: 0.75rem 0.85rem;
  border-radius: 6px;
  border: 1px solid #e6eaf0;
  background: #fff;
  box-shadow: none;
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
  font-size: 0.66rem;
}

.console-toolbar__heading h1 {
  margin: 0;
  color: #1f2a3d;
  font-size: clamp(1.05rem, 1.6vw, 1.35rem);
}

.console-toolbar__heading p {
  margin: 0;
  color: #69778c;
  line-height: 1.6;
  font-size: 0.84rem;
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
  border-radius: 2px;
  border: 1px solid #e3e7ee;
  background: #fafbfd;
  color: #5f6c80;
  font-size: 0.74rem;
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
  padding: 0.85rem;
  border-radius: 6px;
  border: 1px solid #e6eaf0;
  background: #fff;
  box-shadow: none;
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
  min-height: 2.2rem;
  padding: 0.52rem 0.68rem;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
  background: #fff;
  color: #1f2a3d;
  font-size: 0.84rem;
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
  min-height: 2.2rem;
  padding: 0.5rem 0.82rem;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
  background: #fff;
  color: #43556d;
  font-weight: 600;
  font-size: 0.82rem;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.toolbar-button:hover {
  border-color: #c9d2e3;
  background: #f5f9ff;
  box-shadow: none;
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
  background: linear-gradient(135deg, #ff6a00, #ff9030);
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
  border-radius: 4px;
  border: 1px solid #e3e7ee;
  background: #fafbfd;
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
  margin-top: 0.7rem;
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
    grid-template-columns: auto auto minmax(240px, 1fr) auto;
  }

  .header-links {
    display: none;
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

  .header-popover {
    right: var(--shell-gutter);
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
    grid-template-columns: auto auto minmax(0, 1fr);
  }

  .header-search {
    grid-column: 1 / -1;
    order: 5;
  }

  .header-status {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .header-tools {
    margin-left: auto;
  }

  .cloud-header__sections {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .header-popover {
    top: calc(100% + 2.2rem);
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
