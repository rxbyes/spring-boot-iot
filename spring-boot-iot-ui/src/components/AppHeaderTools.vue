<template>
  <div class="header-status">
    <div class="header-tools" aria-label="系统工具">
      <button
        type="button"
        class="tool-text tool-text--notice"
        :class="{ 'tool-text--active': showNoticePanel }"
        aria-label="打开消息通知"
        title="消息通知"
        :aria-expanded="showNoticePanel"
        :aria-controls="noticePanelId"
        @click="$emit('toggle-notice')"
      >
        <span class="tool-text__label">通知</span>
        <span v-if="unreadNoticeCount > 0" class="tool-text__badge">{{ unreadNoticeCount }}</span>
      </button>
      <button
        type="button"
        class="tool-text tool-text--help"
        :class="{ 'tool-text--active': showHelpPanel }"
        aria-label="打开帮助中心"
        title="帮助中心"
        :aria-expanded="showHelpPanel"
        :aria-controls="helpPanelId"
        @click="$emit('toggle-help')"
      >
        <span class="tool-text__label">帮助</span>
      </button>
    </div>

    <div
      ref="accountEntryRef"
      class="account-entry"
      @mouseenter="openAccountMenu"
      @mouseleave="closeAccountMenu"
      @focusin="openAccountMenu"
    >
      <button
        type="button"
        class="account-chip"
        :title="headerIdentity"
        :aria-expanded="accountMenuOpen"
        aria-haspopup="menu"
        @click.stop="toggleAccountMenu"
      >
        <span class="account-chip__avatar">{{ accountInitial }}</span>
        <span class="account-chip__meta">
          <strong>{{ headerAccountName }}</strong>
        </span>
        <span class="account-chip__sr">{{ headerRoleName }}</span>
        <span class="account-chip__caret" aria-hidden="true"></span>
      </button>

      <transition name="account-pop">
        <div v-if="accountMenuOpen" class="account-panel" role="menu" aria-label="账户菜单">
          <div class="account-panel__summary">
            <div class="account-panel__identity">
              <span class="account-panel__avatar">{{ accountInitial }}</span>
              <div class="account-panel__meta">
                <strong>{{ headerAccountName }}</strong>
                <p>登录账号：{{ headerAccountCode }}</p>
              </div>
            </div>

            <div class="account-panel__tags">
              <span class="account-panel__tag">{{ headerAccountType }}</span>
              <span class="account-panel__tag account-panel__tag--accent">{{ headerAuthStatus }}</span>
            </div>

            <p class="account-panel__hint">{{ headerRoleName }}</p>
            <p class="account-panel__hint" v-if="headerPrimaryContact">{{ headerPrimaryContact }}</p>
            <p class="account-panel__hint">登录方式：{{ headerLoginMethods }}</p>
          </div>

          <div class="account-panel__group">
            <button type="button" class="account-panel__action" data-action="account-center" @click="emitAction('open-account-center')">
              <span>账号中心</span>
              <small>查看当前账号信息与角色摘要</small>
            </button>
            <button type="button" class="account-panel__action" data-action="real-name-auth" @click="emitAction('open-real-name-auth')">
              <span>实名认证</span>
              <small>查看认证状态与后续接入说明</small>
            </button>
            <button type="button" class="account-panel__action" data-action="login-methods" @click="emitAction('open-login-methods')">
              <span>登录方式</span>
              <small>查看账号登录与手机号登录说明</small>
            </button>
          </div>

          <div class="account-panel__group">
            <button type="button" class="account-panel__action" data-action="change-password" @click="emitAction('open-change-password')">
              <span>修改密码</span>
              <small>修改当前登录账号密码</small>
            </button>
            <button type="button" class="account-panel__action account-panel__action--danger" data-action="logout" @click="emitAction('logout')">
              <span>退出登录</span>
              <small>退出当前会话并返回登录页</small>
            </button>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';

defineProps<{
  showNoticePanel: boolean;
  showHelpPanel: boolean;
  noticePanelId: string;
  helpPanelId: string;
  headerIdentity: string;
  headerAccountName: string;
  headerRoleName: string;
  headerAccountCode: string;
  headerAccountType: string;
  headerAuthStatus: string;
  headerPrimaryContact: string;
  headerLoginMethods: string;
  accountInitial: string;
  unreadNoticeCount: number;
}>();

const emit = defineEmits<{
  (e: 'toggle-notice'): void;
  (e: 'toggle-help'): void;
  (e: 'open-account-menu'): void;
  (e: 'open-account-center'): void;
  (e: 'open-real-name-auth'): void;
  (e: 'open-login-methods'): void;
  (e: 'open-change-password'): void;
  (e: 'logout'): void;
}>();

const accountEntryRef = ref<HTMLElement | null>(null);
const accountMenuOpen = ref(false);

function openAccountMenu() {
  if (accountMenuOpen.value) {
    return;
  }
  accountMenuOpen.value = true;
  emit('open-account-menu');
}

function closeAccountMenu() {
  accountMenuOpen.value = false;
}

function toggleAccountMenu() {
  if (accountMenuOpen.value) {
    closeAccountMenu();
    return;
  }
  openAccountMenu();
}

function emitAction(action: 'open-account-center' | 'open-real-name-auth' | 'open-login-methods' | 'open-change-password' | 'logout') {
  closeAccountMenu();
  emit(action);
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!accountMenuOpen.value) {
    return;
  }
  const target = event.target as Node | null;
  if (!target || accountEntryRef.value?.contains(target)) {
    return;
  }
  closeAccountMenu();
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeAccountMenu();
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown);
  document.removeEventListener('keydown', handleDocumentKeydown);
});
</script>

<style scoped>
.header-status {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.6rem;
}

.header-tools {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
}

.tool-text {
  min-height: 2rem;
  padding: 0 0.72rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, #ffffff, #f7f9fc);
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 600;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  box-shadow: var(--shadow-xs);
}

.tool-text:hover {
  border-color: color-mix(in srgb, var(--brand) 22%, white);
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.tool-text--active {
  border-color: color-mix(in srgb, var(--brand) 24%, white);
  color: var(--brand);
  background: color-mix(in srgb, var(--brand) 10%, white);
}

.tool-text__label {
  position: relative;
  padding-left: 0.8rem;
}

.tool-text__label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 0.42rem;
  height: 0.42rem;
  border-radius: var(--radius-pill);
  transform: translateY(-50%);
  background: rgba(126, 142, 166, 0.55);
}

.tool-text--notice .tool-text__label::before {
  background: color-mix(in srgb, var(--brand) 55%, transparent);
}

.tool-text--help .tool-text__label::before {
  background: rgba(82, 196, 26, 0.55);
}

.tool-text__badge {
  min-width: 1.04rem;
  height: 1.04rem;
  border-radius: var(--radius-pill);
  padding: 0 0.24rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--danger);
  color: var(--bg-card);
  font-size: 0.64rem;
  font-weight: 700;
}

.account-entry {
  position: relative;
}

.account-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 2.1rem;
  padding: 0.24rem 0.58rem 0.24rem 0.32rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, #ffffff, #f7f9fc);
  box-shadow: var(--shadow-xs);
}

.account-chip__avatar,
.account-panel__avatar {
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--bg-card);
  background: linear-gradient(160deg, var(--accent), var(--accent-deep));
}

.account-chip__avatar {
  width: 1.4rem;
  height: 1.4rem;
  font-size: 0.72rem;
}

.account-chip__meta {
  display: grid;
  gap: 0;
  line-height: 1.2;
}

.account-chip__meta strong {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-heading);
}

.account-chip__sr {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.account-chip__caret {
  width: 0.48rem;
  height: 0.48rem;
  border-right: 1.5px solid #738299;
  border-bottom: 1.5px solid #738299;
  transform: rotate(45deg) translateY(-1px);
}

.account-pop-enter-active,
.account-pop-leave-active {
  transition: all 140ms ease;
}

.account-pop-enter-from,
.account-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.account-panel {
  position: absolute;
  top: calc(100% + 0.45rem);
  right: 0;
  width: 18.5rem;
  border-radius: 1rem;
  border: 1px solid #dbe4f2;
  background: var(--bg-card);
  box-shadow: var(--shadow-popover);
  overflow: hidden;
  z-index: 25;
}

.account-panel__summary {
  padding: 0.95rem 1rem 0.85rem;
  border-bottom: 1px solid var(--line-soft);
  background: linear-gradient(180deg, #f8fbff 0%, var(--bg-card) 100%);
}

.account-panel__identity {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.account-panel__avatar {
  width: 2.5rem;
  height: 2.5rem;
  font-size: 1rem;
}

.account-panel__meta {
  min-width: 0;
}

.account-panel__meta strong {
  display: block;
  font-size: 0.96rem;
  color: var(--text-heading);
}

.account-panel__meta p {
  margin: 0.18rem 0 0;
  color: var(--text-caption-2);
  font-size: 0.75rem;
}

.account-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.75rem;
}

.account-panel__tag {
  display: inline-flex;
  align-items: center;
  min-height: 1.4rem;
  padding: 0 0.5rem;
  border-radius: var(--radius-pill);
  background: #eef3fb;
  color: #3a557f;
  font-size: 0.7rem;
}

.account-panel__tag--accent {
  background: color-mix(in srgb, var(--brand) 10%, white);
  color: var(--brand);
}

.account-panel__hint {
  margin: 0.72rem 0 0;
  color: #5e769e;
  font-size: 0.74rem;
}

.account-panel__group + .account-panel__group {
  border-top: 1px solid var(--line-soft);
}

.account-panel__action {
  width: 100%;
  padding: 0.82rem 1rem;
  display: grid;
  gap: 0.22rem;
  text-align: left;
  border: none;
  background: var(--bg-card);
}

.account-panel__action span {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-heading);
}

.account-panel__action small {
  color: var(--text-caption-2);
  font-size: 0.71rem;
}

.account-panel__action:hover,
.account-panel__action:focus-visible {
  background: #f7faff;
}

.account-panel__action--danger span {
  color: #d93025;
}

@media (max-width: 900px) {
  .header-status {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .header-tools {
    margin-left: auto;
  }
}

@media (max-width: 640px) {
  .account-panel {
    right: -0.25rem;
    width: min(18.5rem, calc(100vw - 1rem));
  }

  .account-chip {
    min-height: 1.76rem;
    padding-right: 0.28rem;
  }

  .account-chip__meta {
    display: none;
  }

  .tool-text {
    padding: 0 0.5rem;
  }
}
</style>


