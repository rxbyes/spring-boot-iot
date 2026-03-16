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
          <span class="status-chip status-chip--brand">商业化首页</span>
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
              <el-button class="toolbar-button toolbar-button--ghost" @click="showAccessPanel = !showAccessPanel">
                {{ showAccessPanel ? '收起接入设置' : '接入设置' }}
              </el-button>
              <el-button
                v-if="permissionStore.isLoggedIn"
                class="toolbar-button"
                @click="handleLogout"
              >
                退出登录
              </el-button>
            </div>
          </div>
        </section>

        <transition name="console-settings">
          <section v-if="showAccessPanel" class="console-settings">
            <div class="console-settings__intro">
              <p>接入配置</p>
              <h2>开发联调能力已收纳到次级面板，不再占据首页主视图。</h2>
              <span>{{ environmentValue }}</span>
            </div>

            <div class="console-settings__content">
              <label class="toolbar-field toolbar-field--wide">
                <span>接入地址</span>
                <el-input
                  v-model="baseUrlDraft"
                  name="api_base_url"
                  autocomplete="url"
                  spellcheck="false"
                  placeholder="留空使用当前站点同源地址，或填写外部网关地址"
                  @keyup.enter="saveApiBaseUrl"
                />
              </label>

              <el-button class="toolbar-button" type="primary" @click="saveApiBaseUrl">应用地址</el-button>

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

              <div v-else class="logged-user-card">
                <strong>{{ permissionStore.userInfo?.nickname || permissionStore.userInfo?.username }}</strong>
                <span>已完成账号接入，可进入受保护工作台查看业务详情。</span>
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
import { ElMessage } from 'element-plus';

import { login as loginApi } from '../api/auth';
import { usePermissionStore } from '../stores/permission';
import { runtimeState, setApiBaseUrl } from '../stores/runtime';
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
    label: '平台首页',
    description: '态势总览',
    menuTitle: '监测预警平台首页',
    menuHint: '围绕业务态势、处置闭环和核心模块入口组织产品首页。',
    items: [{ to: '/', label: '首页总览', caption: '平台定位、闭环路径与核心能力映射', short: '首' }]
  },
  {
    key: 'iot-base',
    label: '设备接入',
    description: '接入与运维',
    menuTitle: '设备接入与运维',
    menuHint: '管理产品模板、设备台账、上报回放与设备侧联调能力。',
    items: [
      { to: '/products', label: '产品模板中心', caption: '产品模板建模、协议绑定与设备归属', short: '产' },
      { to: '/devices', label: '设备运维中心', caption: '设备建档、在线状态核查与基础运维', short: '设' },
      { to: '/reporting', label: '接入回放台', caption: 'HTTP 上报模拟、payload 回放与联调', short: '报' },
      { to: '/insight', label: '风险点工作台', caption: '设备属性、消息日志与风险研判线索', short: '洞' },
      { to: '/file-debug', label: '文件与固件校验', caption: '文件快照与固件聚合结果核验', short: '校' }
    ]
  },
  {
    key: 'risk-ops',
    label: '预警处置',
    description: '闭环与复盘',
    menuTitle: '风险处置闭环',
    menuHint: '覆盖告警、事件、风险点、规则、预案与分析报表。',
    items: [
      { to: '/alarm-center', label: '告警中心', caption: '告警列表、确认、抑制与关闭', short: '警' },
      { to: '/event-disposal', label: '事件处置', caption: '工单派发、处置反馈与事件闭环', short: '事' },
      { to: '/risk-point', label: '风险点管理', caption: '风险点建档、设备绑定与等级治理', short: '点' },
      { to: '/rule-definition', label: '阈值规则配置', caption: '阈值规则维护与触发条件治理', short: '阈' },
      { to: '/linkage-rule', label: '联动规则', caption: '触发条件与联动动作编排', short: '联' },
      { to: '/emergency-plan', label: '应急预案', caption: '预案维护、步骤编排与响应协同', short: '预' },
      { to: '/report-analysis', label: '分析报表', caption: '风险趋势、告警统计与设备健康复盘', short: '报' }
    ]
  },
  {
    key: 'system',
    label: '系统治理',
    description: '组织与审计',
    menuTitle: '组织治理与审计',
    menuHint: '维护组织、用户、权限、区域、字典、通知和审计日志。',
    items: [
      { to: '/organization', label: '组织机构', caption: '组织树维护与责任主体管理', short: '组' },
      { to: '/user', label: '用户管理', caption: '用户维护、状态管理与密码重置', short: '用' },
      { to: '/role', label: '角色管理', caption: '角色维护与用户角色映射', short: '角' },
      { to: '/region', label: '区域管理', caption: '区域树与业务区域归属维护', short: '区' },
      { to: '/dict', label: '字典配置', caption: '字典类型与字典项配置', short: '字' },
      { to: '/channel', label: '通知渠道', caption: '通知渠道配置、启停与测试', short: '通' },
      { to: '/audit-log', label: '审计日志', caption: '关键操作记录审计与追踪', short: '审' }
    ]
  }
];

const flattenedItems = navigationGroups.flatMap((group) => group.items);

const activeGroup = computed(() => {
  const group = navigationGroups.find((item) => item.items.some((navItem) => navItem.to === route.path));
  return group || navigationGroups[0];
});

const searchKeyword = ref('');
const baseUrlDraft = ref(runtimeState.apiBaseUrl);
const loginForm = ref({
  username: 'admin',
  password: '123456'
});
const loginLoading = ref(false);
const showAccessPanel = ref(false);

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
    showAccessPanel.value = false;
  }
);

const activeTitle = computed(() => String(route.meta.title || '平台首页'));
const activeDescription = computed(() => String(route.meta.description || '围绕告警、事件、风险点和设备健康组织平台能力。'));
const headerIdentity = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '访客模式';
  }
  return permissionStore.userInfo?.nickname || permissionStore.userInfo?.username || '已登录';
});
const environmentLabel = computed(() => (runtimeState.apiBaseUrl ? '外部网关接入' : '当前站点同源接入'));
const environmentValue = computed(() => runtimeState.apiBaseUrl || '当前站点同源访问 /api');
const authStatusLabel = computed(() => (permissionStore.isLoggedIn ? '已完成账号接入' : '未登录，仅开放首页预览'));

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
    ElMessage.info('请输入要跳转的业务模块关键词');
    return;
  }

  const lowerKeyword = keyword.toLowerCase();
  const target = flattenedItems.find((item) => {
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
    showAccessPanel.value = false;
    ElMessage.success('登录成功');
  } finally {
    loginLoading.value = false;
  }
}

function handleLogout() {
  permissionStore.logout();
  if (route.meta.requiresAuth !== false) {
    router.push('/');
  }
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

.console-toolbar :deep(.el-input__wrapper),
.console-settings :deep(.el-input__wrapper) {
  background: #fff;
  box-shadow: 0 0 0 1px #d5deeb inset;
}

.console-toolbar :deep(.el-input__wrapper.is-focus),
.console-settings :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(255, 106, 0, 0.48) inset,
    0 0 0 3px rgba(255, 106, 0, 0.12);
}

.console-toolbar :deep(.el-button),
.console-settings :deep(.el-button) {
  border-radius: 0.55rem;
  border-color: #d8e0ec;
  color: #43556d;
  background: #fff;
}

.console-toolbar :deep(.el-button--primary),
.console-settings :deep(.el-button--primary) {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #ff7b1a, #ff9b42);
}

.console-settings :deep(.el-button--success) {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #1fa46d, #34c88d);
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
