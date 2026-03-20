<template>
  <div
    class="cloud-shell"
    :class="{ 'cloud-shell--collapsed': sidebarCollapsed, 'cloud-shell--mobile-open': mobileMenuOpen }"
    :style="shellViewportStyle"
  >
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

        <button
          type="button"
          class="command-trigger"
          aria-label="打开全局命令面板"
          @click="openCommandPalette"
        >
          <span class="command-trigger__icon" aria-hidden="true"></span>
          <span class="command-trigger__text">搜索工作台</span>
          <span class="command-trigger__kbd">Ctrl K</span>
        </button>

        <AppHeaderTools
          :show-notice-panel="showNoticePanel"
          :show-help-panel="showHelpPanel"
          :notice-panel-id="noticePanelId"
          :help-panel-id="helpPanelId"
          :header-identity="headerIdentity"
          :header-account-name="accountSummary.name"
          :header-role-name="accountSummary.roleName"
          :header-account-code="accountSummary.code"
          :header-account-type="accountSummary.type"
          :header-auth-status="accountSummary.authStatus"
          :header-primary-contact="accountSummary.primaryContact"
          :header-login-methods="accountSummary.loginMethods"
          :account-initial="accountSummary.initial"
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

      <ShellWorkspaceTabs
        :groups="navigationGroups"
        :active-group-key="activeGroup.key"
        @switch-group="switchGroup"
      />

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

    <ShellCommandPalette
      v-model="showCommandPalette"
      v-model:query="commandKeyword"
      :groups="commandGroups"
      :recent-items="recentCommandItems"
      @select="selectCommandPath"
    />

    <div class="cloud-layout">
      <ShellSidebarNav
        :group="activeGroup"
        :current-route-path="currentRoutePath"
        :sidebar-collapsed="sidebarCollapsed"
        :is-mobile="isMobile"
        :mobile-menu-open="mobileMenuOpen"
      />

      <section class="cloud-content">
        <ShellBreadcrumb
          v-if="!showSidebarContext"
          :group-label="activeGroup.label"
          :active-title="activeTitle"
        />

        <main id="main-content" class="content-frame">
          <RouterView />
        </main>

        <ShellAccountDrawers
          v-model:show-account-dialog="showAccountDialog"
          v-model:show-real-name-auth-dialog="showRealNameAuthDialog"
          v-model:show-login-methods-dialog="showLoginMethodsDialog"
          v-model:show-change-password-dialog="showChangePasswordDialog"
          :summary="accountSummary"
          :password-submitting="passwordSubmitting"
          @open-account-center="openAccountCenter"
          @open-real-name-auth="openRealNameAuth"
          @open-login-methods="openLoginMethods"
          @open-change-password-dialog="openChangePasswordDialog"
          @submit-change-password="submitChangePassword"
        />
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
import { RouterLink, RouterView } from 'vue-router';

import { useShellOrchestrator } from '../composables/useShellOrchestrator';
import AppHeaderTools from './AppHeaderTools.vue';
import HeaderPopoverPanel from './HeaderPopoverPanel.vue';
import ShellAccountDrawers from './ShellAccountDrawers.vue';
import ShellBreadcrumb from './ShellBreadcrumb.vue';
import ShellCommandPalette from './ShellCommandPalette.vue';
import ShellSidebarNav from './ShellSidebarNav.vue';
import ShellWorkspaceTabs from './ShellWorkspaceTabs.vue';

const {
  headerRef,
  shellViewportStyle,
  isMobile,
  mobileMenuOpen,
  sidebarCollapsed,
  toggleSidebar,
  showAccountDialog,
  showRealNameAuthDialog,
  showLoginMethodsDialog,
  showChangePasswordDialog,
  passwordSubmitting,
  headerIdentity,
  accountSummary,
  openAccountCenter,
  openRealNameAuth,
  openLoginMethods,
  openChangePasswordDialog,
  submitChangePassword,
  handleLogout,
  navigationGroups,
  currentRoutePath,
  activeGroup,
  showSidebarContext,
  activeTitle,
  switchGroup,
  showCommandPalette,
  commandKeyword,
  showNoticePanel,
  showHelpPanel,
  noticePanelId,
  helpPanelId,
  noticePopoverItems,
  unreadNoticeCount,
  helpPopoverItems,
  commandGroups,
  recentCommandItems,
  openCommandPalette,
  selectCommandPath,
  toggleNoticePanel,
  toggleHelpPanel,
  openNotice,
  openHelp,
  closeHeaderPanels
} = useShellOrchestrator();
</script>

<style scoped>
.cloud-shell {
  --shell-max-width: 1760px;
  --shell-gutter: clamp(16px, 2vw, 28px);
  --shell-header-height: 122px;
  min-height: 100vh;
  height: 100vh;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
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

.command-trigger {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  justify-self: stretch;
  gap: 0.5rem;
  width: min(100%, 28rem);
  min-height: 2.5rem;
  margin-left: auto;
  min-width: 0;
  padding: 0 0.22rem 0 0;
  border: 1px solid var(--line-panel-2);
  border-radius: var(--radius-pill);
  background: linear-gradient(180deg, rgba(248, 249, 251, 0.96), rgba(245, 247, 250, 0.98));
  overflow: hidden;
  transition: border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.command-trigger:hover {
  border-color: color-mix(in srgb, var(--brand) 20%, white);
  background: var(--bg-card);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 8%, transparent);
}

.command-trigger__icon {
  position: relative;
  width: 2.2rem;
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.command-trigger__icon::before {
  content: '';
  width: 0.56rem;
  height: 0.56rem;
  border: 1.6px solid #8ea0ba;
  border-radius: 50%;
}

.command-trigger__icon::after {
  content: '';
  position: absolute;
  width: 0.3rem;
  height: 1.6px;
  background: #8ea0ba;
  border-radius: var(--radius-pill);
  transform: translate(0.26rem, 0.28rem) rotate(45deg);
}

.command-trigger__text {
  min-width: 0;
  color: var(--text-caption);
  font-size: 0.84rem;
  font-weight: 600;
  text-align: left;
}

.command-trigger__kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 4.3rem;
  height: 2rem;
  padding: 0 0.7rem;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--brand);
  font-size: 0.76rem;
  font-weight: 700;
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

.cloud-layout {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  width: min(var(--shell-max-width), calc(100vw - var(--shell-gutter) * 2));
  margin: 0 auto;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.cloud-shell--collapsed .cloud-layout {
  grid-template-columns: 88px minmax(0, 1fr);
}

.cloud-content {
  padding: 1rem 1.15rem 1.2rem;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
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
    width: 100%;
  }

  .cloud-content {
    padding: 0.92rem;
  }
}

@media (max-width: 900px) {
  .cloud-header__main {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .command-trigger {
    grid-column: 1 / -1;
    order: 5;
    width: 100%;
  }
}
</style>
