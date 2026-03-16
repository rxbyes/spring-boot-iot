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
          <span class="cloud-brand__name">Spring IoT 控制台</span>
        </RouterLink>

        <label class="header-search" for="global-search">
          <input
            id="global-search"
            v-model="searchKeyword"
            type="search"
            placeholder="搜索页面，例如：告警、设备、规则"
            @keydown.enter="handleSearch"
          />
          <button type="button" @click="handleSearch">搜索</button>
        </label>

        <nav class="header-links" aria-label="快捷入口">
          <RouterLink v-for="link in quickLinks" :key="link.to" :to="link.to">{{ link.label }}</RouterLink>
        </nav>

        <SignalBadge :label="connectionLabel" :tone="connectionTone" />
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

        <div class="sidebar-foot">
          <p>常用入口</p>
          <div class="sidebar-foot__links">
            <RouterLink v-for="link in sidebarQuickLinks" :key="link.to" :to="link.to">{{ link.label }}</RouterLink>
          </div>
        </div>
      </aside>

      <section class="cloud-content">
        <section class="console-toolbar">
          <div class="console-toolbar__heading">
            <p class="toolbar-eyebrow">当前工作区</p>
            <h1>{{ activeTitle }}</h1>
            <p>{{ activeDescription }}</p>
          </div>

          <div class="console-toolbar__actions">
            <label class="toolbar-field toolbar-field--wide">
              <span>API Base URL</span>
              <el-input
                v-model="baseUrlDraft"
                name="api_base_url"
                autocomplete="url"
                spellcheck="false"
                placeholder="留空走本地代理，或填写例如 http://localhost:9999"
                @keyup.enter="saveApiBaseUrl"
              />
            </label>

            <el-button class="toolbar-button" type="primary" @click="saveApiBaseUrl">保存地址</el-button>

            <template v-if="!permissionStore.isLoggedIn">
              <label class="toolbar-field">
                <span>账号</span>
                <el-input v-model="loginForm.username" placeholder="admin" />
              </label>
              <label class="toolbar-field">
                <span>密码</span>
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="123456"
                  @keyup.enter="handleLogin"
                />
              </label>
              <el-button class="toolbar-button toolbar-button--success" type="success" :loading="loginLoading" @click="handleLogin">
                登录
              </el-button>
            </template>
            <template v-else>
              <span class="logged-user">{{ permissionStore.userInfo?.nickname || permissionStore.userInfo?.username }}</span>
              <el-button class="toolbar-button" @click="handleLogout">退出登录</el-button>
            </template>
          </div>
        </section>

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
import { ElMessage } from 'element-plus';

import { login as loginApi } from '../api/auth';
import { usePermissionStore } from '../stores/permission';
import { runtimeState, setApiBaseUrl } from '../stores/runtime';
import SignalBadge from './SignalBadge.vue';
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

const navigationGroups: NavGroup[] = [
  {
    key: 'overview',
    label: '概览',
    description: '工作台总览',
    menuTitle: '风险监测驾驶舱',
    menuHint: '统一查看平台态势、角色入口和处置闭环。',
    items: [{ to: '/', label: '驾驶舱首页', caption: '风险态势、待办提醒与角色入口', short: '驾' }]
  },
  {
    key: 'iot-base',
    label: 'IoT 基础',
    description: '设备与接入',
    menuTitle: '设备接入与联调',
    menuHint: '管理产品模板、设备台账和上报链路联调能力。',
    items: [
      { to: '/products', label: '产品模板中心', caption: '产品建模、协议绑定与设备归属', short: '产' },
      { to: '/devices', label: '设备运维中心', caption: '设备建档、状态检索与在线核查', short: '设' },
      { to: '/reporting', label: '接入回放台', caption: 'HTTP 上报模拟与 payload 回放', short: '报' },
      { to: '/insight', label: '风险点工作台', caption: '设备详情、属性趋势、日志追踪', short: '洞' },
      { to: '/file-debug', label: '文件与固件调试', caption: '文件快照与固件聚合校验', short: '文' },
      { to: '/future-lab', label: '未来演进蓝图', caption: '规则、孪生、拓扑与 OTA 预留', short: '蓝' }
    ]
  },
  {
    key: 'risk-ops',
    label: '风险处置',
    description: '告警闭环',
    menuTitle: '风险监测与处置',
    menuHint: '覆盖告警中心、事件工单、规则配置与分析报表。',
    items: [
      { to: '/alarm-center', label: '告警中心', caption: '告警列表、确认、抑制与关闭', short: '警' },
      { to: '/event-disposal', label: '事件处置', caption: '工单派发、流转反馈、事件闭环', short: '事' },
      { to: '/risk-point', label: '风险点管理', caption: '风险点维护、设备绑定、等级管理', short: '点' },
      { to: '/rule-definition', label: '阈值规则配置', caption: '规则 CRUD、阈值模型、触发条件', short: '阈' },
      { to: '/linkage-rule', label: '联动规则', caption: '触发条件与联动动作编排', short: '联' },
      { to: '/emergency-plan', label: '应急预案', caption: '响应步骤、联系人与执行预案', short: '预' },
      { to: '/report-analysis', label: '分析报表', caption: '风险趋势、告警统计、设备健康', short: '报' }
    ]
  },
  {
    key: 'system',
    label: '系统管理',
    description: '组织与权限',
    menuTitle: '平台系统治理',
    menuHint: '维护组织、用户、权限、字典、通知与审计日志。',
    items: [
      { to: '/organization', label: '组织机构', caption: '组织树维护与负责人管理', short: '组' },
      { to: '/user', label: '用户管理', caption: '用户 CRUD 与密码重置', short: '用' },
      { to: '/role', label: '角色管理', caption: '角色维护与用户角色查询', short: '角' },
      { to: '/region', label: '区域管理', caption: '区域树维护与区域 CRUD', short: '区' },
      { to: '/dict', label: '字典配置', caption: '主字典与字典项配置', short: '字' },
      { to: '/channel', label: '通知渠道', caption: '通知渠道配置与状态管理', short: '通' },
      { to: '/audit-log', label: '审计日志', caption: '关键操作日志审计与追踪', short: '审' }
    ]
  }
];

const quickLinks = [
  { label: '费用与成本', to: '/report-analysis' },
  { label: '工单处置', to: '/event-disposal' },
  { label: '审计日志', to: '/audit-log' },
  { label: '支持文档', to: '/future-lab' }
];

const flattenedItems = navigationGroups.flatMap((group) => group.items);

const activeGroup = computed(() => {
  const group = navigationGroups.find((item) => item.items.some((navItem) => navItem.to === route.path));
  return group || navigationGroups[0];
});

const sidebarQuickLinks = computed(() => activeGroup.value.items.slice(0, 4));

const searchKeyword = ref('');
const baseUrlDraft = ref(runtimeState.apiBaseUrl);
const loginForm = ref({
  username: 'admin',
  password: '123456'
});
const loginLoading = ref(false);

const isMobile = ref(false);
const mobileMenuOpen = ref(false);
const sidebarCollapsed = ref(false);

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
  }
);

const activeTitle = computed(() => String(route.meta.title || '风险监测平台'));
const activeDescription = computed(() => String(route.meta.description || '按业务域分区管理，保证高频功能就近访问。'));
const connectionLabel = computed(() => (runtimeState.isProxyMode ? '代理模式' : '直连模式'));
const connectionTone = computed<'brand' | 'success'>(() => (runtimeState.isProxyMode ? 'brand' : 'success'));

function switchGroup(groupKey: string) {
  const group = navigationGroups.find((item) => item.key === groupKey);
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
    ElMessage.info('请输入要跳转的页面关键字');
    return;
  }

  // 这里用前端静态导航做模糊匹配，避免引入额外请求。
  const lowerKeyword = keyword.toLowerCase();
  const target = flattenedItems.find((item) => {
    return item.label.toLowerCase().includes(lowerKeyword) || item.caption.toLowerCase().includes(lowerKeyword);
  });

  if (!target) {
    ElMessage.warning('未找到匹配页面，请尝试更换关键字');
    return;
  }

  router.push(target.to);
}

function saveApiBaseUrl() {
  setApiBaseUrl(baseUrlDraft.value);
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

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }

  loginLoading.value = true;
  try {
    const response = await loginApi({
      username: loginForm.value.username,
      password: loginForm.value.password
    });
    const data = response.data;
    if (!data?.token) {
      ElMessage.error('登录失败，未获取到 token');
      return;
    }

    permissionStore.login(
      {
        id: Number(data.userId || 0),
        username: data.username || loginForm.value.username,
        nickname: data.realName || data.username || loginForm.value.username,
        role: 'manager'
      },
      data.token
    );
    ElMessage.success('登录成功');
  } finally {
    loginLoading.value = false;
  }
}

function handleLogout() {
  permissionStore.logout();
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
  grid-template-columns: auto auto minmax(220px, 1fr) auto auto;
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

.header-links {
  display: inline-flex;
  align-items: center;
  gap: 0.85rem;
}

.header-links a {
  color: #4a5a74;
  text-decoration: none;
  font-size: 0.88rem;
}

.header-links a:hover {
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

.sidebar-foot {
  margin-top: auto;
  padding: 0.8rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid #dce4f0;
  background: rgba(255, 255, 255, 0.88);
}

.sidebar-foot p {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 600;
  color: #617187;
}

.sidebar-foot__links {
  margin-top: 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.sidebar-foot__links a {
  padding: 0.3rem 0.58rem;
  border-radius: 999px;
  border: 1px solid #dbe3f0;
  background: #fff;
  color: #54647d;
  text-decoration: none;
  font-size: 0.75rem;
}

.sidebar-foot__links a:hover {
  border-color: rgba(255, 106, 0, 0.35);
  color: #ff6a00;
}

.cloud-shell--collapsed .sidebar-context,
.cloud-shell--collapsed .sidebar-foot {
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
}

.console-toolbar__heading {
  display: grid;
  gap: 0.35rem;
  max-width: 36rem;
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

.console-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
  align-items: flex-end;
}

.toolbar-field {
  display: grid;
  gap: 0.32rem;
  min-width: 9.6rem;
}

.toolbar-field--wide {
  min-width: min(22rem, 100%);
}

.toolbar-field span {
  font-size: 0.68rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8490a5;
}

.logged-user {
  display: inline-flex;
  align-items: center;
  min-height: 2rem;
  padding: 0 0.72rem;
  border-radius: 999px;
  border: 1px solid #d9e0ec;
  background: #fff;
  color: #41526a;
  font-size: 0.84rem;
}

.cloud-header :deep(.signal-badge) {
  border-color: #d7e0ee;
  background: #f6f8fc;
  color: #5f6f86;
  letter-spacing: 0.03em;
}

.cloud-header :deep(.signal-badge__dot) {
  box-shadow: none;
}

.cloud-header :deep(.signal-badge--brand) {
  color: #ff6a00;
}

.cloud-header :deep(.signal-badge--success) {
  color: #16a34a;
}

.console-toolbar :deep(.el-input__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px #d5deeb inset;
}

.console-toolbar :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(255, 106, 0, 0.48) inset,
    0 0 0 3px rgba(255, 106, 0, 0.12);
}

.console-toolbar :deep(.el-button) {
  border-radius: 0.55rem;
  border-color: #d8e0ec;
  color: #43556d;
  background: #fff;
}

.console-toolbar :deep(.el-button--primary) {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #ff7b1a, #ff9b42);
}

.console-toolbar :deep(.el-button--success) {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #1fa46d, #34c88d);
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
    grid-template-columns: auto auto minmax(180px, 1fr) auto;
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

  .cloud-content {
    padding: 0.92rem;
  }

  .console-toolbar {
    flex-direction: column;
  }

  .console-toolbar__actions {
    width: 100%;
    justify-content: flex-start;
  }

  .toolbar-field,
  .toolbar-field--wide {
    min-width: 100%;
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

  .cloud-header__sections {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .section-tab {
    min-width: 8.2rem;
  }
}
</style>
