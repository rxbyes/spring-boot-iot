<template>
  <div class="cloud-shell" :class="{ 'cloud-shell--collapsed': sidebarCollapsed, 'cloud-shell--mobile-open': mobileMenuOpen }">
    <a class="skip-link" href="#main-content">跳到主内容</a>

    <header ref="headerRef" class="cloud-header">
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
          <span class="cloud-brand__text">
            <span class="cloud-brand__name">监测预警平台</span>
            <span class="cloud-brand__caption">IoT Risk Console</span>
          </span>
        </RouterLink>

        <label class="header-search" for="global-search">
          <span class="header-search__icon" aria-hidden="true"></span>
          <input
            id="global-search"
            v-model="searchKeyword"
            type="search"
            placeholder="搜索菜单、设备或规则"
            @keydown.enter="handleSearch"
          />
          <button type="button" @click="handleSearch">搜索</button>
        </label>

        <AppHeaderTools
          :show-notice-panel="showNoticePanel"
          :show-help-panel="showHelpPanel"
          :notice-panel-id="noticePanelId"
          :help-panel-id="helpPanelId"
          :header-identity="headerIdentity"
          :header-account-name="headerAccountName"
          :header-role-name="headerRoleName"
          :header-account-code="headerAccountCode"
          :header-account-type="headerAccountType"
          :header-auth-status="headerAuthStatus"
          :header-primary-contact="headerPrimaryContact"
          :header-login-methods="headerLoginMethods"
          :account-initial="accountInitial"
          :unread-notice-count="unreadNoticeCount"
          @toggle-notice="toggleNoticePanel"
          @toggle-help="toggleHelpPanel"
          @open-account-menu="closeHeaderPanels"
          @open-account-center="openAccountCenter"
          @open-real-name-auth="openRealNameAuth"
          @open-login-methods="openLoginMethods"
          @open-change-password="openChangePasswordDialog"
          @logout="handleLogout"
        />
      </div>

      <div class="cloud-header__sections-wrap">
        <div class="cloud-header__sections-label">
          <span>业务分区</span>
          <small>{{ activeGroup.description }}</small>
        </div>

        <nav class="cloud-header__sections" aria-label="一级导航">
          <el-tooltip
            v-for="group in navigationGroups"
            :key="group.key"
            placement="bottom"
            effect="light"
            :content="group.description"
          >
            <button
              type="button"
              class="section-tab"
              :class="{ 'section-tab--active': activeGroup.key === group.key }"
              :aria-label="`${group.label}，${group.description}`"
              @click="switchGroup(group.key)"
            >
              <span>{{ group.label }}</span>
            </button>
          </el-tooltip>
        </nav>
      </div>

      <transition name="header-pop">
        <HeaderPopoverPanel
          v-if="showNoticePanel"
          :panel-id="noticePanelId"
          aria-label="消息通知面板"
          title="消息通知"
          subtitle="最近操作与系统提醒"
          :items="noticePopoverItems"
          @select="openNotice"
        />
      </transition>

      <transition name="header-pop">
        <HeaderPopoverPanel
          v-if="showHelpPanel"
          :panel-id="helpPanelId"
          panel-class="header-popover--help"
          aria-label="帮助中心面板"
          title="帮助中心"
          subtitle="常用入口与使用说明"
          :items="helpPopoverItems"
          @select="openHelp"
        />
      </transition>
    </header>

    <div class="cloud-layout">
      <aside class="cloud-sidebar" :aria-hidden="isMobile && !mobileMenuOpen">
        <div class="sidebar-context">
          <p class="sidebar-context__eyebrow">当前工作台</p>
          <h2>{{ activeGroup.label }}</h2>
        </div>

        <nav class="side-menu" aria-label="二级导航">
          <el-tooltip
            v-for="item in activeGroup.items"
            :key="item.to"
            placement="right"
            effect="light"
            :content="item.caption || item.label"
          >
            <RouterLink
              :to="item.to"
              class="side-menu__item"
              :class="{ 'side-menu__item--active': currentRoutePath === item.to }"
              :title="item.caption ? `${item.label}：${item.caption}` : item.label"
              :aria-label="item.caption ? `${item.label}，${item.caption}` : item.label"
            >
              <span class="side-menu__marker">{{ sidebarCollapsed ? item.short : '' }}</span>
              <span class="side-menu__content">
                <strong>{{ item.label }}</strong>
              </span>
            </RouterLink>
          </el-tooltip>
        </nav>
      </aside>

      <section class="cloud-content">
        <section v-if="!showSidebarContext" class="console-toolbar">
          <nav class="toolbar-breadcrumb" aria-label="当前位置">
            <span class="toolbar-breadcrumb__item">{{ activeGroup.label }}</span>
            <span class="toolbar-breadcrumb__separator">/</span>
            <span class="toolbar-breadcrumb__item toolbar-breadcrumb__item--current">{{ activeTitle }}</span>
          </nav>
        </section>

        <main id="main-content" class="content-frame">
          <RouterView />
        </main>

        <StandardFormDrawer
          v-model="showAccountDialog"
          eyebrow="Account Center"
          title="账号中心"
          subtitle="统一通过右侧抽屉查看当前账号、角色、联系方式与认证状态。"
          size="32rem"
          @close="closeAccountDialog"
        >
          <div class="account-dialog">
            <div class="account-dialog__hero">
              <span class="account-dialog__avatar">{{ accountInitial }}</span>
              <div class="account-dialog__hero-content">
                <strong>{{ headerAccountName }}</strong>
                <p>登录账号：{{ headerAccountCode }}</p>
              </div>
            </div>

            <dl class="account-dialog__list">
              <div>
                <dt>账号类型</dt>
                <dd>{{ headerAccountType }}</dd>
              </div>
              <div>
                <dt>当前角色</dt>
                <dd>{{ headerRoleName }}</dd>
              </div>
              <div>
                <dt>真实姓名</dt>
                <dd>{{ permissionStore.authContext?.realName || '未填写' }}</dd>
              </div>
              <div>
                <dt>显示名称</dt>
                <dd>{{ permissionStore.displayName || headerAccountCode }}</dd>
              </div>
              <div>
                <dt>手机号</dt>
                <dd>{{ maskedPhone }}</dd>
              </div>
              <div>
                <dt>邮箱</dt>
                <dd>{{ maskedEmail }}</dd>
              </div>
              <div>
                <dt>认证状态</dt>
                <dd>{{ headerAuthStatus }}</dd>
              </div>
              <div>
                <dt>登录方式</dt>
                <dd>{{ headerLoginMethods }}</dd>
              </div>
            </dl>
          </div>

          <template #footer>
            <el-button @click="openRealNameAuth">实名认证</el-button>
            <el-button @click="openLoginMethods">登录方式</el-button>
            <el-button @click="openChangePasswordDialog">修改密码</el-button>
            <el-button type="primary" @click="closeAccountDialog">关闭</el-button>
          </template>
        </StandardFormDrawer>

        <StandardFormDrawer
          v-model="showRealNameAuthDialog"
          eyebrow="Account Verification"
          title="实名认证"
          subtitle="当前版本先展示账号实名信息与接入状态，后续再补独立认证流程。"
          size="30rem"
          @close="closeRealNameAuthDialog"
        >
          <div class="account-dialog">
            <dl class="account-dialog__list">
              <div>
                <dt>实名状态</dt>
                <dd>{{ headerAuthStatus }}</dd>
              </div>
              <div>
                <dt>实名姓名</dt>
                <dd>{{ permissionStore.authContext?.realName || '未填写' }}</dd>
              </div>
              <div>
                <dt>账号类型</dt>
                <dd>{{ headerAccountType }}</dd>
              </div>
              <div>
                <dt>当前说明</dt>
                <dd>当前共享环境仅展示实名信息状态，不提供外部实名认证提交流程。</dd>
              </div>
            </dl>
          </div>

          <template #footer>
            <el-button @click="openAccountCenter">返回账号中心</el-button>
            <el-button type="primary" @click="closeRealNameAuthDialog">我知道了</el-button>
          </template>
        </StandardFormDrawer>

        <StandardFormDrawer
          v-model="showLoginMethodsDialog"
          eyebrow="Login Methods"
          title="登录方式"
          subtitle="展示当前账号可用的登录方式与联系信息，后续再补独立绑定流程。"
          size="30rem"
          @close="closeLoginMethodsDialog"
        >
          <div class="account-dialog">
            <dl class="account-dialog__list">
              <div>
                <dt>可用登录方式</dt>
                <dd>{{ headerLoginMethods }}</dd>
              </div>
              <div>
                <dt>账号登录</dt>
                <dd>{{ headerAccountCode }}</dd>
              </div>
              <div>
                <dt>手机号登录</dt>
                <dd>{{ maskedPhone }}</dd>
              </div>
              <div>
                <dt>邮箱通知</dt>
                <dd>{{ maskedEmail }}</dd>
              </div>
            </dl>
          </div>

          <template #footer>
            <el-button @click="openAccountCenter">返回账号中心</el-button>
            <el-button type="primary" @click="closeLoginMethodsDialog">关闭</el-button>
          </template>
        </StandardFormDrawer>

        <StandardFormDrawer
          v-model="showChangePasswordDialog"
          eyebrow="Account Security"
          title="修改密码"
          subtitle="统一通过右侧抽屉完成密码修改，提交成功后需要重新登录。"
          size="30rem"
          @close="closeChangePasswordDialog"
        >
          <el-form label-position="top" class="account-password-form">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </el-form>

          <template #footer>
            <el-button @click="closeChangePasswordDialog">取消</el-button>
            <el-button type="primary" :loading="passwordSubmitting" @click="submitChangePassword">确认修改</el-button>
          </template>
        </StandardFormDrawer>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';
import { ElMessage } from '@/utils/message';

import { changePassword } from '../api/user';
import { createSectionHomeNavItem } from '../utils/sectionWorkspaces';
import { activityEntries } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import type { MenuTreeNode } from '../types/auth';
import { formatDateTime } from '../utils/format';
import { normalizeOptionalRoutePath, normalizeRoutePath } from '../utils/routePath';
import AppHeaderTools from './AppHeaderTools.vue';
import HeaderPopoverPanel from './HeaderPopoverPanel.vue';
import StandardFormDrawer from './StandardFormDrawer.vue';

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
const headerRef = ref<HTMLElement | null>(null);

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
    key: 'iot-access',
    label: '接入智维',
    description: '资产、链路与异常观测',
    menuTitle: '接入智维',
    menuHint: '覆盖产品定义、设备资产、链路验证、异常观测与数据校验。',
    items: [
      { to: '/products', label: '产品定义中心', caption: '产品模型、协议绑定与设备归属基线', short: '产' },
      { to: '/devices', label: '设备资产中心', caption: '设备建档、在线状态与资产运维', short: '设' },
      { to: '/reporting', label: '链路验证中心', caption: 'HTTP 上报与主链路验证', short: '验' },
      { to: '/system-log', label: '异常观测台', caption: '研发测试定位系统异常与接入问题', short: '观' },
      { to: '/message-trace', label: '链路追踪台', caption: '按 TraceId、设备编码与 Topic 串联接入链路', short: '追' },
      { to: '/file-debug', label: '数据校验台', caption: '文件快照与固件聚合调试', short: '校' }
    ]
  },
  {
    key: 'risk-ops',
    label: '风险运营',
    description: '态势、告警与协同闭环',
    menuTitle: '风险运营',
    menuHint: '覆盖实时监测、告警运营、事件协同、对象洞察与运营复盘。',
    items: [
      { to: '/risk-monitoring', label: '实时监测台', caption: '监测列表、统一详情与风险筛选', short: '监' },
      { to: '/risk-monitoring-gis', label: 'GIS态势图', caption: '风险点位态势与详情联动', short: '图' },
      { to: '/alarm-center', label: '告警运营台', caption: '告警列表、详情、确认、抑制、关闭', short: '告' },
      { to: '/event-disposal', label: '事件协同台', caption: '事件派发、工单流转与闭环', short: '事' },
      { to: '/insight', label: '对象洞察台', caption: '设备属性、日志与研判线索', short: '洞' },
      { to: '/report-analysis', label: '运营分析中心', caption: '风险趋势、告警统计与事件闭环', short: '析' }
    ]
  },
  {
    key: 'risk-config',
    label: '风险策略',
    description: '对象、阈值与联动配置',
    menuTitle: '风险策略',
    menuHint: '覆盖风险对象、阈值策略、联动编排与应急预案库。',
    items: [
      { to: '/risk-point', label: '风险对象中心', caption: '风险对象台账、设备绑定与监测范围', short: '险' },
      { to: '/rule-definition', label: '阈值策略', caption: '阈值策略设计、测试与启停管理', short: '阈' },
      { to: '/linkage-rule', label: '联动编排', caption: '联动触发条件与动作配置', short: '联' },
      { to: '/emergency-plan', label: '应急预案库', caption: '应急预案维护与执行准备', short: '预' }
    ]
  },
  {
    key: 'system-governance',
    label: '平台治理',
    description: '组织、权限与审计治理',
    menuTitle: '平台治理',
    menuHint: '覆盖组织、账号、角色、导航、区域、字典、通知与审计中心。',
    items: [
      { to: '/organization', label: '组织架构', caption: '组织树维护与层级治理', short: '组' },
      { to: '/user', label: '账号中心', caption: '账号档案、状态与角色分配', short: '账' },
      { to: '/role', label: '角色权限', caption: '角色与菜单权限授权', short: '角' },
      { to: '/menu', label: '导航编排', caption: '菜单树结构与页面权限项维护', short: '导' },
      { to: '/region', label: '区域版图', caption: '区域树维护与引用配置', short: '区' },
      { to: '/dict', label: '数据字典', caption: '字典分类维护与编码查询', short: '字' },
      { to: '/channel', label: '通知编排', caption: '通知配置、启停与测试', short: '通' },
      { to: '/audit-log', label: '审计中心', caption: '客户与治理侧业务操作审计', short: '审' }
    ]
  },
  {
    key: 'quality-workbench',
    label: '质量工场',
    description: '自动化与质量基线',
    menuTitle: '质量工场',
    menuHint: '覆盖自动化编排、回归计划与质量巡检资产。',
    items: [
      { to: '/automation-test', label: '自动化工场', caption: '配置驱动场景编排、执行计划与报告导出', short: '测' }
    ]
  }
];

function cloneGroups(groups: NavGroup[]): NavGroup[] {
  return groups.map((group) => ({
    ...group,
    items: group.items.map((item) => ({ ...item }))
  }));
}

function prependSectionHomeItem(groupKey: string, groupLabel: string, items: NavItem[]): NavItem[] {
  const overviewItem = createSectionHomeNavItem(groupKey, groupLabel);
  if (!overviewItem) {
    return items;
  }
  if (items.some((item) => item.to === overviewItem.to)) {
    return items;
  }
  return [overviewItem, ...items];
}

function buildShortLabel(label: string, fallback?: string): string {
  const short = (fallback || '').trim();
  if (short) {
    return short;
  }
  const text = (label || '').trim();
  return text ? text.slice(0, 1) : '-';
}

function appendNavItem(items: NavItem[], node: MenuTreeNode, pathSet: Set<string>): void {
  if (node.type === 2) {
    return;
  }
  const path = normalizeOptionalRoutePath(node.path);
  if (path && !pathSet.has(path)) {
    pathSet.add(path);
    items.push({
      to: path,
      label: node.menuName || path,
      caption: node.meta?.caption || node.meta?.description || `${node.menuName || path}功能`,
      short: buildShortLabel(node.menuName || path, node.meta?.shortLabel)
    });
  }

  (node.children || []).forEach((child) => appendNavItem(items, child, pathSet));
}

function buildDynamicGroups(menus: MenuTreeNode[]): NavGroup[] {
  return menus
    .filter((root) => root.type !== 2)
    .map((root) => {
      const items: NavItem[] = [];
      const pathSet = new Set<string>();
      appendNavItem(items, root, pathSet);
      return {
        key: root.menuCode || `menu-${root.id}`,
        label: root.menuName || '未命名分组',
        description: root.meta?.description || '权限分组',
        menuTitle: root.meta?.menuTitle || root.menuName || '菜单分组',
        menuHint: root.meta?.menuHint || root.meta?.description || '由后端菜单权限动态驱动。',
        items: prependSectionHomeItem(root.menuCode || `menu-${root.id}`, root.menuName || '', items)
      } as NavGroup;
    })
    .filter((group) => group.items.length > 0);
}

function resolveGroupLandingPath(group: NavGroup): string {
  const sectionHomeItem = createSectionHomeNavItem(group.key, group.label);
  if (sectionHomeItem && permissionStore.hasRoutePermission(sectionHomeItem.to)) {
    return normalizeRoutePath(sectionHomeItem.to);
  }
  return normalizeRoutePath(group.items[0]?.to || '/');
}

const searchKeyword = ref('');
const isMobile = ref(false);
const mobileMenuOpen = ref(false);
const sidebarCollapsed = ref(false);
const showNoticePanel = ref(false);
const showHelpPanel = ref(false);
const showAccountDialog = ref(false);
const showRealNameAuthDialog = ref(false);
const showLoginMethodsDialog = ref(false);
const showChangePasswordDialog = ref(false);
const passwordSubmitting = ref(false);
const readNoticeIds = ref<string[]>([]);
const noticePanelId = 'header-notice-panel';
const helpPanelId = 'header-help-panel';
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});
const staticNavigationGroups = cloneGroups(docFallbackGroups);

const navigationGroups = computed<NavGroup[]>(() => {
  if (!permissionStore.isLoggedIn) {
    return [guestGroup];
  }
  const dynamicGroups = buildDynamicGroups(permissionStore.menus || []);
  if (dynamicGroups.length > 0) {
    return dynamicGroups;
  }
  // 动态菜单缺失时，兜底菜单也必须按当前路由权限过滤，避免“可见但不可点”。
  const filteredFallbackGroups = staticNavigationGroups
    .map((group) => {
      const mergedItems = prependSectionHomeItem(group.key, group.label, group.items);
      const allowedItems = mergedItems.filter((item) => permissionStore.hasRoutePermission(item.to));
      return {
        ...group,
        items: allowedItems
      };
    })
    .filter((group) => group.items.length > 0);

  if (filteredFallbackGroups.length > 0) {
    return filteredFallbackGroups;
  }
  return [guestGroup];
});

const flattenedItems = computed(() => navigationGroups.value.flatMap((group) => group.items));
const currentRoutePath = computed(() => normalizeRoutePath(route.path));

const activeGroup = computed(() => {
  const matchedGroup = navigationGroups.value.find((group) => group.items.some((item) => item.to === currentRoutePath.value));
  return matchedGroup || navigationGroups.value[0] || guestGroup;
});

const activeMenuItem = computed(() => flattenedItems.value.find((item) => item.to === currentRoutePath.value) || null);

watch(
  () => route.path,
  () => {
    if (isMobile.value) {
      mobileMenuOpen.value = false;
    }
    showNoticePanel.value = false;
    showHelpPanel.value = false;
    showAccountDialog.value = false;
    showRealNameAuthDialog.value = false;
    showLoginMethodsDialog.value = false;
    showChangePasswordDialog.value = false;
  }
);

const activeGroupHomePath = computed(() => resolveGroupLandingPath(activeGroup.value));
const showSidebarContext = computed(() => currentRoutePath.value === activeGroupHomePath.value);
const activeTitle = computed(() => {
  if (showSidebarContext.value) {
    return String(route.meta.title || activeGroup.value.label || '平台首页');
  }
  return activeMenuItem.value?.label || String(route.meta.title || '平台首页');
});
const headerIdentity = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '访客模式';
  }
  const roleText = permissionStore.roleNames.join(' / ') || '未分配角色';
  return `当前角色：${roleText}`;
});
const headerAccountName = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '访客账号';
  }
  return permissionStore.displayName || permissionStore.userInfo?.username || '系统管理员';
});
const headerAccountCode = computed(() => permissionStore.userInfo?.username || 'guest');
const headerAccountType = computed(() => permissionStore.userInfo?.accountType || '子账号');
const headerRoleName = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '未登录';
  }
  return permissionStore.roleNames.join(' / ') || permissionStore.primaryRoleName || '未分配角色';
});
const headerAuthStatus = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '未登录';
  }
  return permissionStore.userInfo?.authStatus || '未填写实名信息';
});
const headerLoginMethods = computed(() => {
  if (!permissionStore.isLoggedIn) {
    return '账号登录';
  }
  const methods = permissionStore.userInfo?.loginMethods || [];
  return methods.length > 0 ? methods.join(' / ') : '账号登录';
});
const maskedPhone = computed(() => maskPhone(permissionStore.userInfo?.phone));
const maskedEmail = computed(() => maskEmail(permissionStore.userInfo?.email));
const headerPrimaryContact = computed(() => {
  if (maskedPhone.value !== '未绑定手机号') {
    return `手机号：${maskedPhone.value}`;
  }
  if (maskedEmail.value !== '未绑定邮箱') {
    return `邮箱：${maskedEmail.value}`;
  }
  return '';
});
const accountInitial = computed(() => {
  const source = headerAccountName.value.trim();
  return source ? source.slice(0, 1).toUpperCase() : '管';
});
  const noticeItems = computed(() => {
  const fromActivity = activityEntries.value.slice(0, 4).map((item) => ({
      id: item.id,
      title: item.title || [item.module, item.action].filter(Boolean).join(' · ') || '最近操作',
      time: formatDateTime(item.createdAt),
      path: item.path || route.path
    }));

  if (fromActivity.length > 0) {
    return fromActivity;
  }

  return [
    { id: 'notice-1', title: '系统导航已升级为统一控制台样式', time: '刚刚', path: '/' },
    { id: 'notice-2', title: '按钮权限仍按数据库角色授权控制', time: '刚刚', path: '/role' },
    { id: 'notice-3', title: '右上角头像已收口账号信息与安全操作', time: '刚刚', path: route.path }
  ];
});
const noticePopoverItems = computed(() =>
  noticeItems.value.map((item) => ({
    id: item.id,
    title: item.title,
    description: item.time,
    path: item.path
  }))
);
const unreadNoticeCount = computed(() => {
  return noticeItems.value.filter((item) => !readNoticeIds.value.includes(item.id)).length;
});
const helpItems = [
  { label: '平台首页', caption: '查看系统总览和业务入口', path: '/' },
  { label: '链路验证中心', caption: '验证 HTTP 上报与链路解析', path: '/reporting' },
  { label: '风险策略', caption: '进入对象、阈值与联动配置总览', path: '/risk-config' },
  { label: '角色权限', caption: '维护角色与权限关系', path: '/role' }
];
const helpPopoverItems = computed(() =>
  helpItems.map((item, index) => ({
    id: `help-${index}`,
    title: item.label,
    description: item.caption,
    path: item.path
  }))
);

watch(
  noticeItems,
  (items) => {
    const validIds = new Set(items.map((item) => item.id));
    readNoticeIds.value = readNoticeIds.value.filter((id) => validIds.has(id));
  },
  { immediate: true }
);

function switchGroup(groupKey: string) {
  const group = navigationGroups.value.find((item) => item.key === groupKey);
  if (!group || group.items.length === 0) {
    return;
  }

  const targetPath = resolveGroupLandingPath(group);
  if (targetPath !== currentRoutePath.value) {
    router.push(targetPath);
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

function toggleNoticePanel() {
  const willOpen = !showNoticePanel.value;
  showNoticePanel.value = willOpen;
  if (showNoticePanel.value) {
    readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...noticeItems.value.map((item) => item.id)]));
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
  readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...noticeItems.value.map((item) => item.id)]));
  showNoticePanel.value = false;
  router.push(path);
}

function openHelp(path: string) {
  showHelpPanel.value = false;
  router.push(path);
}

function closeHeaderPanels() {
  showNoticePanel.value = false;
  showHelpPanel.value = false;
}

function maskPhone(phone?: string | null): string {
  const value = (phone || '').trim();
  if (!value) {
    return '未绑定手机号';
  }
  if (value.length < 7) {
    return value;
  }
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}

function maskEmail(email?: string | null): string {
  const value = (email || '').trim();
  if (!value) {
    return '未绑定邮箱';
  }
  const [name, domain] = value.split('@');
  if (!name || !domain) {
    return value;
  }
  if (name.length <= 2) {
    return `${name.slice(0, 1)}***@${domain}`;
  }
  return `${name.slice(0, 2)}***@${domain}`;
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!showNoticePanel.value && !showHelpPanel.value) {
    return;
  }
  const target = event.target as Node | null;
  if (!target) {
    return;
  }
  if (headerRef.value?.contains(target)) {
    return;
  }
  closeHeaderPanels();
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') {
    return;
  }
  if (!showNoticePanel.value && !showHelpPanel.value) {
    return;
  }
  closeHeaderPanels();
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

function openAccountCenter() {
  closeRealNameAuthDialog();
  closeLoginMethodsDialog();
  showAccountDialog.value = true;
}

function closeAccountDialog() {
  showAccountDialog.value = false;
}

function openRealNameAuth() {
  closeAccountDialog();
  closeLoginMethodsDialog();
  showRealNameAuthDialog.value = true;
}

function closeRealNameAuthDialog() {
  showRealNameAuthDialog.value = false;
}

function openLoginMethods() {
  closeAccountDialog();
  closeRealNameAuthDialog();
  showLoginMethodsDialog.value = true;
}

function closeLoginMethodsDialog() {
  showLoginMethodsDialog.value = false;
}

function openChangePasswordDialog() {
  if (!permissionStore.isLoggedIn) {
    router.push('/login');
    return;
  }
  closeAccountDialog();
  closeRealNameAuthDialog();
  closeLoginMethodsDialog();
  resetChangePasswordForm();
  showChangePasswordDialog.value = true;
}

function resetChangePasswordForm() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordSubmitting.value = false;
}

function closeChangePasswordDialog() {
  showChangePasswordDialog.value = false;
  resetChangePasswordForm();
}

async function submitChangePassword() {
  const currentUserId = permissionStore.userInfo?.id;
  if (!currentUserId) {
    ElMessage.error('当前登录信息缺失，请重新登录后再试');
    return;
  }
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写密码信息');
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  passwordSubmitting.value = true;
  try {
    await changePassword({
      id: currentUserId,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    closeChangePasswordDialog();
    permissionStore.logout();
    await router.push('/login');
    ElMessage.success('密码已修改，请重新登录');
  } catch {
    passwordSubmitting.value = false;
  }
}

function handleLogout() {
  closeAccountDialog();
  closeRealNameAuthDialog();
  closeLoginMethodsDialog();
  closeChangePasswordDialog();
  permissionStore.logout();
  router.push('/login');
  ElMessage.success('已退出登录');
}

onMounted(() => {
  updateViewportState();
  window.addEventListener('resize', updateViewportState);
  document.addEventListener('pointerdown', handleDocumentPointerDown);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportState);
  document.removeEventListener('pointerdown', handleDocumentPointerDown);
  document.removeEventListener('keydown', handleDocumentKeydown);
});
</script>

<style scoped>
.cloud-shell {
  --shell-max-width: 1760px;
  --shell-gutter: clamp(16px, 2vw, 28px);
  min-height: 100vh;
  color: var(--text-primary);
  background:
    radial-gradient(circle at top right, rgba(255, 106, 0, 0.08), transparent 20rem),
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent 12rem);
}

.skip-link {
  position: absolute;
  top: -40px;
  left: 1rem;
  z-index: 120;
  padding: 0.7rem 1rem;
  border-radius: 0.5rem;
  background: var(--brand);
  color: var(--bg-card);
}

.skip-link:focus {
  top: 1rem;
}

.cloud-header {
  position: sticky;
  top: 0;
  z-index: 90;
  border-bottom: 1px solid rgba(31, 41, 55, 0.08);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.cloud-header__main {
  display: grid;
  grid-template-columns: auto minmax(180px, 240px) minmax(320px, 1fr) auto;
  align-items: center;
  gap: 1rem;
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  padding: 0.8rem 0 0.55rem;
}

.menu-trigger {
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  border: 1px solid var(--line-panel);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, #ffffff, #f7f9fc);
  box-shadow: var(--shadow-xs);
}

.menu-trigger span {
  width: 1rem;
  height: 2px;
  margin: 0 auto;
  border-radius: var(--radius-pill);
  background: #526378;
}

.cloud-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-primary);
  text-decoration: none;
  min-width: 13rem;
}

.cloud-brand__logo {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.75rem;
  background: linear-gradient(145deg, var(--brand), var(--brand-bright));
  position: relative;
  box-shadow: 0 10px 24px color-mix(in srgb, var(--brand) 24%, transparent);
}

.cloud-brand__logo::before {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 0.42rem;
  border: 2px solid rgba(255, 255, 255, 0.72);
}

.cloud-brand__text {
  display: grid;
  gap: 0.08rem;
}

.cloud-brand__name {
  font-size: 1.08rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--text-heading);
}

.cloud-brand__caption {
  font-size: 0.72rem;
  color: var(--text-caption-2);
}

.header-search {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  justify-self: stretch;
  width: min(100%, 28rem);
  height: 2.5rem;
  margin-left: auto;
  min-width: 0;
  border: 1px solid var(--line-panel-2);
  border-radius: var(--radius-pill);
  background: linear-gradient(180deg, rgba(248, 249, 251, 0.96), rgba(245, 247, 250, 0.98));
  overflow: hidden;
  transition: border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.header-search:focus-within {
  border-color: color-mix(in srgb, var(--brand) 26%, white);
  background: var(--bg-card);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 10%, transparent);
}

.header-search__icon {
  position: relative;
  width: 2.1rem;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.header-search__icon::before {
  content: '';
  width: 0.56rem;
  height: 0.56rem;
  border: 1.6px solid #8ea0ba;
  border-radius: 50%;
}

.header-search__icon::after {
  content: '';
  position: absolute;
  width: 0.3rem;
  height: 1.6px;
  background: #8ea0ba;
  border-radius: var(--radius-pill);
  transform: translate(0.26rem, 0.28rem) rotate(45deg);
}

.header-search input {
  border: none;
  background: transparent;
  padding: 0.35rem 0.2rem 0.35rem 0;
  min-width: 0;
  font-size: 0.84rem;
}

.header-search input:focus {
  outline: none;
}

.header-search button {
  border: none;
  border-radius: var(--radius-pill);
  height: 100%;
  margin: 0.18rem;
  padding: 0 1rem;
  background: color-mix(in srgb, var(--brand) 12%, white);
  color: var(--brand);
  font-size: 0.8rem;
  font-weight: 700;
}

.cloud-header__sections-wrap {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 1rem;
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  padding: 0 0 0.7rem;
}

.cloud-header__sections-label {
  display: grid;
  gap: 0.12rem;
  min-width: 7rem;
}

.cloud-header__sections-label span {
  color: var(--text-heading);
  font-size: 0.76rem;
  font-weight: 700;
}

.cloud-header__sections-label small {
  color: var(--text-caption-2);
  font-size: 0.72rem;
}

.cloud-header__sections {
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

.cloud-header__sections::-webkit-scrollbar {
  display: none;
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

.section-tab span {
  font-size: 0.88rem;
  font-weight: 700;
}

.section-tab:hover {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.section-tab--active {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.section-tab--active::after {
  content: '';
  position: absolute;
  left: 0.75rem;
  right: 0.75rem;
  bottom: 0.18rem;
  height: 3px;
  border-radius: var(--radius-2xs);
  background: var(--brand);
}

.cloud-layout {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  min-height: calc(100vh - 122px);
}

.cloud-shell--collapsed .cloud-layout {
  grid-template-columns: 88px minmax(0, 1fr);
}

.cloud-sidebar {
  border-right: 1px solid var(--panel-border);
  padding: 1rem 0.7rem 1.15rem;
  background: linear-gradient(180deg, rgba(250, 251, 253, 0.98), rgba(246, 248, 251, 0.98));
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.sidebar-context {
  padding: 0.05rem 0.1rem 0.72rem;
  border-bottom: 1px solid var(--line-soft);
}

.sidebar-context__eyebrow {
  margin: 0;
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-caption-2);
}

.sidebar-context h2 {
  margin: 0.34rem 0 0;
  font-size: 0.94rem;
  color: var(--text-heading);
  font-weight: 700;
}

.side-menu {
  display: grid;
  gap: 0.12rem;
  overflow-y: auto;
  padding-right: 0.15rem;
}

.side-menu__item {
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

.side-menu__item::before {
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

.side-menu__marker {
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

.side-menu__content {
  min-width: 0;
}

.side-menu__content strong {
  display: block;
  font-size: 0.82rem;
  line-height: 1.45;
  font-weight: 600;
}

.side-menu__item:hover {
  border-color: #dfe5ee;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--shadow-card-soft);
}

.side-menu__item:hover .side-menu__marker {
  background: color-mix(in srgb, var(--brand) 42%, transparent);
}

.side-menu__item--active {
  border-color: rgba(255, 106, 0, 0.16);
  background: linear-gradient(90deg, rgba(255, 106, 0, 0.1), rgba(255, 255, 255, 0.98));
  box-shadow: inset 0 0 0 1px rgba(255, 106, 0, 0.04);
}

.side-menu__item--active::before {
  opacity: 1;
}

.side-menu__item--active .side-menu__marker {
  background: var(--brand);
}

.side-menu__item--active .side-menu__content strong {
  color: var(--brand);
}

.cloud-shell--collapsed .sidebar-context {
  display: none;
}

.cloud-shell--collapsed .side-menu__item {
  grid-template-columns: 1fr;
  justify-items: center;
  padding: 0.62rem 0.45rem;
}

.cloud-shell--collapsed .side-menu__marker {
  width: 1.6rem;
  height: 1.6rem;
  border-radius: var(--radius-md);
  font-size: 0.72rem;
  color: var(--brand-deep);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.cloud-shell--collapsed .side-menu__item:hover .side-menu__marker {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.cloud-shell--collapsed .side-menu__item--active .side-menu__marker {
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 12%, transparent);
}

.cloud-shell--collapsed .side-menu__content {
  display: none;
}

.cloud-shell--collapsed .side-menu__item::before {
  display: none;
}

.cloud-content {
  padding: 1rem 1.15rem 1.2rem;
  min-width: 0;
}

.console-toolbar {
  padding: 0.05rem 0.1rem 0.55rem;
  border: none;
  background: transparent;
  box-shadow: none;
}

.toolbar-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.42rem;
  margin: 0;
  color: var(--text-muted-3);
  font-size: 0.76rem;
  font-weight: 600;
}

.toolbar-breadcrumb__item--current {
  color: var(--text-heading);
}

.toolbar-breadcrumb__separator {
  color: #b0bac8;
}

.console-settings {
  margin-top: 0.9rem;
  padding: 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--panel-border);
  background: var(--bg-card);
  box-shadow: none;
}

.console-settings__intro {
  display: grid;
  gap: 0.3rem;
}

.console-settings__intro p {
  margin: 0;
  color: var(--text-muted-1);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.7rem;
}

.console-settings__intro h2 {
  margin: 0;
  font-size: 1.05rem;
  color: var(--text-ink);
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
  color: var(--text-muted-2);
}

.toolbar-input {
  width: 100%;
  min-height: 2.2rem;
  padding: 0.52rem 0.68rem;
  border: 1px solid var(--line-muted);
  border-radius: var(--radius-2xs);
  background: var(--bg-card);
  color: var(--text-ink);
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
  border-color: color-mix(in srgb, var(--brand) 48%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 12%, transparent);
}

.toolbar-button {
  min-height: 2.2rem;
  padding: 0.5rem 0.82rem;
  border: 1px solid var(--line-muted);
  border-radius: var(--radius-2xs);
  background: var(--bg-card);
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
  background: var(--bg-hover);
  box-shadow: none;
}

.toolbar-button:focus-visible {
  outline: none;
  border-color: color-mix(in srgb, var(--brand) 48%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 14%, transparent);
}

.toolbar-button:active {
  transform: translateY(1px);
}

.toolbar-button--primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--brand), var(--brand-bright));
  color: var(--bg-card);
}

.toolbar-button--primary:hover {
  border-color: transparent;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand-bright));
}

.toolbar-button--success {
  border-color: transparent;
  background: linear-gradient(135deg, #1fa46d, #34c88d);
  color: var(--bg-card);
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
  border-radius: var(--radius-xs);
  border: 1px solid #e3e7ee;
  background: var(--surface-soft);
  color: #445671;
}

.logged-user-card strong {
  font-size: 0.92rem;
  color: var(--text-ink);
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

.account-dialog {
  display: grid;
  gap: 1rem;
}

.account-dialog__hero {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem;
  border-radius: 0.85rem;
  background: linear-gradient(180deg, #f8fbff 0%, var(--bg-card) 100%);
  border: 1px solid #e2eaf5;
}

.account-dialog__avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 700;
  color: var(--bg-card);
  background: linear-gradient(160deg, var(--accent), var(--accent-deep));
}

.account-dialog__hero-content strong {
  display: block;
  color: var(--text-heading);
  font-size: 1rem;
}

.account-dialog__hero-content p {
  margin: 0.22rem 0 0;
  color: #5e769e;
  font-size: 0.8rem;
}

.account-dialog__list {
  margin: 0;
  display: grid;
  gap: 0.7rem;
}

.account-dialog__list div {
  display: grid;
  gap: 0.22rem;
  padding: 0.9rem 1rem;
  border-radius: 0.8rem;
  background: var(--surface-muted);
  border: 1px solid #e8edf5;
}

.account-dialog__list dt {
  color: #7b8ca6;
  font-size: 0.75rem;
}

.account-dialog__list dd {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.86rem;
  font-weight: 500;
}

.account-password-form {
  padding-top: 0.25rem;
}

.content-frame {
  margin-top: 0.95rem;
}

.cloud-content > .content-frame:first-child {
  margin-top: 0.2rem;
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
    grid-template-columns: auto minmax(150px, 220px) minmax(260px, 1fr) auto;
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
    box-shadow: var(--shadow-side-panel);
  }

  .cloud-shell--mobile-open .cloud-sidebar {
    transform: translateX(0);
  }

  .cloud-content {
    padding: 0.92rem;
  }

  .cloud-header__sections-wrap {
    grid-template-columns: 1fr;
    gap: 0.6rem;
  }

  .console-toolbar {
    padding-bottom: 0.45rem;
  }
}

@media (max-width: 900px) {
  .cloud-header__main {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .header-search {
    grid-column: 1 / -1;
    order: 5;
    width: 100%;
  }

  .cloud-header__sections-label {
    display: none;
  }

  .section-tab {
    min-width: 7.4rem;
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
