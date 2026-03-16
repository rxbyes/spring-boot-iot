<template>
  <div class="login-page">
    <div class="login-page__halo login-page__halo--brand" />
    <div class="login-page__halo login-page__halo--accent" />
    <div class="login-page__grid" />

    <header class="login-page__header">
      <RouterLink class="login-brand" to="/login">
        <span class="login-brand__logo" aria-hidden="true" />
        <span class="login-brand__name">监测预警平台</span>
      </RouterLink>
    </header>

    <main class="login-stage">
      <section class="login-hero">
        <p class="login-hero__eyebrow">Unified Access</p>
        <h1>风险监测预警处置平台</h1>
        <p class="login-hero__lead">
          面向园区与工业场景的风险监测、预警协同与事件处置统一入口。
        </p>
      </section>

      <section class="auth-panel">
        <div class="auth-panel__scan">
          <div class="auth-panel__scan-head">
            <p>微信扫码登录</p>
            <span>企业微信 / 微信</span>
          </div>

          <div class="qr-stage" aria-hidden="true">
            <div class="qr-stage__scanner" />
            <div class="qr-grid">
              <span
                v-for="cell in qrCells"
                :key="cell.id"
                class="qr-grid__cell"
                :class="{
                  'qr-grid__cell--filled': cell.filled,
                  'qr-grid__cell--finder': cell.finder
                }"
              />
            </div>
          </div>

          <p class="auth-panel__scan-copy">支持企业微信 / 微信扫码登录，能力正在持续开通中。</p>

          <div class="auth-panel__scan-actions">
            <button class="secondary-button" type="button" @click="showWechatHint">查看接入状态</button>
            <button class="ghost-button" type="button" @click="activeTab = 'account'">切换账号登录</button>
          </div>
        </div>

        <div class="auth-panel__divider" />

        <div class="auth-panel__form">
          <div class="auth-tabs" role="tablist" aria-label="登录方式">
            <button
              type="button"
              class="auth-tabs__item"
              :class="{ 'auth-tabs__item--active': activeTab === 'account' }"
              :aria-selected="activeTab === 'account'"
              @click="switchTab('account')"
            >
              账号密码登录
            </button>
            <button
              type="button"
              class="auth-tabs__item"
              :class="{ 'auth-tabs__item--active': activeTab === 'phone' }"
              :aria-selected="activeTab === 'phone'"
              @click="switchTab('phone')"
            >
              手机号登录
            </button>
          </div>

          <form class="auth-form" @submit.prevent="handleSubmit">
            <template v-if="activeTab === 'account'">
              <label class="auth-form__field">
                <span>账号</span>
                <input
                  id="login-username"
                  v-model.trim="accountForm.username"
                  autocomplete="username"
                  name="login_username"
                  placeholder="请输入用户名"
                  spellcheck="false"
                />
              </label>
            </template>
            <template v-else>
              <label class="auth-form__field">
                <span>手机</span>
                <input
                  id="login-phone"
                  v-model.trim="phoneForm.phone"
                  autocomplete="tel"
                  inputmode="numeric"
                  maxlength="11"
                  name="login_phone"
                  placeholder="请输入手机号"
                />
              </label>
            </template>

            <label class="auth-form__field">
              <span>密码</span>
              <div class="password-field">
                <input
                  id="login-password"
                  v-model="passwordModel"
                  :type="passwordVisible ? 'text' : 'password'"
                  autocomplete="current-password"
                  name="login_password"
                  placeholder="请输入密码"
                />
                <button type="button" class="password-field__toggle" @click="passwordVisible = !passwordVisible">
                  {{ passwordVisible ? '隐藏' : '显示' }}
                </button>
              </div>
            </label>

            <label class="auth-form__field auth-form__field--compact">
              <span>接入地址</span>
              <div class="runtime-field">
                <input
                  id="login-base-url"
                  v-model.trim="baseUrlDraft"
                  autocomplete="url"
                  name="login_base_url"
                  placeholder="可选填写外部网关地址"
                  spellcheck="false"
                  @keydown.enter.prevent="persistApiBaseUrl()"
                />
                <button type="button" class="ghost-button" @click="persistApiBaseUrl()">保存</button>
              </div>
            </label>

            <p class="auth-form__tip">{{ formTip }}</p>

            <div class="auth-form__actions">
              <button id="login-submit" class="primary-button auth-form__submit" type="submit" :disabled="submitting">
                {{ submitting ? '登录中...' : submitLabel }}
              </button>
              <button id="login-fill-demo" class="secondary-button" type="button" @click="fillDemoAccount">填入演示账号</button>
            </div>
          </form>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from '@/utils/message';
import { RouterLink, useRoute, useRouter } from 'vue-router';

import { login as loginApi } from '../api/auth';
import { usePermissionStore } from '../stores/permission';
import { runtimeState, setApiBaseUrl } from '../stores/runtime';

type LoginTab = 'account' | 'phone';

interface QrCell {
  id: string;
  filled: boolean;
  finder: boolean;
}

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();

const activeTab = ref<LoginTab>('account');
const passwordVisible = ref(false);
const submitting = ref(false);
const baseUrlDraft = ref(runtimeState.apiBaseUrl);

const accountForm = reactive({
  username: 'admin',
  password: '123456'
});

const phoneForm = reactive({
  phone: '',
  password: ''
});

const submitLabel = computed(() => (activeTab.value === 'account' ? '立即登录' : '手机号登录'));
const formTip = computed(() => (
  activeTab.value === 'account'
    ? '请输入账号和密码完成登录'
    : '请输入手机号和密码完成登录'
));

const passwordModel = computed({
  get() {
    return activeTab.value === 'account' ? accountForm.password : phoneForm.password;
  },
  set(value: string) {
    if (activeTab.value === 'account') {
      accountForm.password = value;
      return;
    }
    phoneForm.password = value;
  }
});

const qrCells = computed(() => createQrCells('spring-boot-iot-wechat-login'));

function switchTab(tab: LoginTab) {
  activeTab.value = tab;
  passwordVisible.value = false;
}

function fillDemoAccount() {
  activeTab.value = 'account';
  accountForm.username = 'admin';
  accountForm.password = '123456';
  passwordVisible.value = false;
  ElMessage.success('已填入默认演示账号');
}

function persistApiBaseUrl() {
  setApiBaseUrl(baseUrlDraft.value);
  ElMessage.success('接入地址已保存');
}

function showWechatHint() {
  ElMessage.info('微信扫码登录正在逐步开放中');
}

function resolveRedirectPath() {
  const redirect = route.query.redirect;
  if (
    typeof redirect === 'string' &&
    redirect.startsWith('/') &&
    redirect !== '/login' &&
    permissionStore.hasRoutePermission(redirect)
  ) {
    return redirect;
  }
  return permissionStore.homePath || '/';
}

async function handleSubmit() {
  if (activeTab.value === 'account') {
    if (!accountForm.username || !accountForm.password) {
      ElMessage.warning('请输入账号和密码');
      return;
    }
  } else {
    if (!/^1\d{10}$/.test(phoneForm.phone)) {
      ElMessage.warning('请输入合法的 11 位手机号');
      return;
    }
    if (!phoneForm.password) {
      ElMessage.warning('请输入登录密码');
      return;
    }
  }

  setApiBaseUrl(baseUrlDraft.value);
  submitting.value = true;
  try {
    const response = await loginApi(
      activeTab.value === 'account'
        ? {
            loginType: 'account',
            username: accountForm.username,
            password: accountForm.password
          }
        : {
            loginType: 'phone',
            phone: phoneForm.phone,
            password: phoneForm.password
          }
    );

    const data = response.data;
    if (!data?.token || !data?.authContext) {
      ElMessage.error('登录失败，未获取到完整登录上下文');
      return;
    }

    permissionStore.login(data);

    await router.replace(resolveRedirectPath());
    ElMessage.success('登录成功');
  } finally {
    submitting.value = false;
  }
}

function createQrCells(seedText: string) {
  const size = 25;
  let seed = hashSeed(seedText);
  const cells: QrCell[] = [];

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
      const finder = isFinderCell(row, col, size);
      let filled = finder ? isFinderFilled(row, col, size) : nextRandom();

      if (!finder && row > 8 && row < 16 && col > 8 && col < 16) {
        filled = nextRandom() && (row + col) % 3 !== 0;
      }

      cells.push({
        id: `${row}-${col}`,
        filled,
        finder
      });
    }
  }

  return cells;

  function nextRandom() {
    seed = (seed * 1664525 + 1013904223) >>> 0;
    return (seed & 1) === 1;
  }
}

function hashSeed(input: string) {
  let hash = 2166136261;
  for (const char of input) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function isFinderCell(row: number, col: number, size: number) {
  return finderOrigins(size).some(([top, left]) => row >= top && row < top + 7 && col >= left && col < left + 7);
}

function isFinderFilled(row: number, col: number, size: number) {
  return finderOrigins(size).some(([top, left]) => {
    if (row < top || row >= top + 7 || col < left || col >= left + 7) {
      return false;
    }

    const innerRow = row - top;
    const innerCol = col - left;
    const border = innerRow === 0 || innerRow === 6 || innerCol === 0 || innerCol === 6;
    const core = innerRow >= 2 && innerRow <= 4 && innerCol >= 2 && innerCol <= 4;
    return border || core;
  });
}

function finderOrigins(size: number) {
  return [
    [0, 0],
    [0, size - 7],
    [size - 7, 0]
  ];
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #152033;
  background:
    radial-gradient(circle at 12% 8%, rgba(255, 106, 0, 0.22), transparent 28%),
    radial-gradient(circle at 82% 0, rgba(30, 128, 255, 0.24), transparent 26%),
    linear-gradient(135deg, #fef6ee 0%, #edf6ff 38%, #dceeff 100%);
}

.login-page__halo {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.login-page__halo--brand {
  width: 22rem;
  height: 22rem;
  top: -7rem;
  left: -6rem;
  background: rgba(255, 106, 0, 0.14);
  animation: float-halo 12s ease-in-out infinite;
}

.login-page__halo--accent {
  width: 28rem;
  height: 28rem;
  top: -12rem;
  right: -4rem;
  background: rgba(30, 128, 255, 0.18);
  animation: float-halo 16s ease-in-out infinite reverse;
}

.login-page__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(44, 73, 121, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(44, 73, 121, 0.05) 1px, transparent 1px);
  background-size: 58px 58px;
  mask-image: radial-gradient(circle at center, rgba(0, 0, 0, 0.82), transparent 88%);
}

.login-page__header,
.login-stage {
  position: relative;
  z-index: 1;
}

.login-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.35rem 2rem 0;
}

.login-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  color: #152033;
  text-decoration: none;
}

.login-brand__logo {
  width: 2rem;
  height: 2rem;
  border-radius: 0.7rem;
  background: linear-gradient(145deg, #ff6a00, #ff9f4a);
  box-shadow: 0 10px 26px rgba(255, 106, 0, 0.26);
  position: relative;
}

.login-brand__logo::before {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 0.45rem;
  border: 2px solid rgba(255, 255, 255, 0.75);
}

.login-brand__name {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
}

.login-stage {
  width: min(1180px, calc(100% - 2.4rem));
  margin: 2rem auto;
  display: grid;
  gap: 1.8rem;
}

.login-hero {
  display: grid;
  gap: 0.9rem;
  justify-items: center;
  text-align: center;
  animation: rise-in 560ms ease;
}

.login-hero__eyebrow {
  margin: 0;
  color: #7f8eb0;
  font-size: 0.78rem;
  letter-spacing: 0.28em;
  text-transform: uppercase;
}

.login-hero h1 {
  margin: 0;
  max-width: 14ch;
  font-size: clamp(2.4rem, 5vw, 4.4rem);
  line-height: 1.08;
}

.login-hero__lead {
  margin: 0;
  max-width: 44rem;
  color: #4d607f;
  font-size: 1rem;
  line-height: 1.8;
}

.auth-panel {
  display: grid;
  grid-template-columns: 0.9fr auto 1.1fr;
  align-items: stretch;
  min-height: 34rem;
  border-radius: 1.5rem;
  border: 1px solid rgba(55, 83, 129, 0.12);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 28px 72px rgba(22, 44, 90, 0.16);
  overflow: hidden;
  animation: rise-in 700ms ease;
}

.auth-panel__scan,
.auth-panel__form {
  padding: 2rem 2.1rem;
  display: grid;
  align-content: start;
}

.auth-panel__scan {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(244, 248, 255, 0.96)),
    radial-gradient(circle at 50% 20%, rgba(255, 106, 0, 0.08), transparent 28%);
}

.auth-panel__scan-head p,
.auth-panel__scan-head span {
  margin: 0;
}

.auth-panel__scan-head p {
  font-size: 2rem;
  font-family: var(--font-display);
  font-weight: 700;
}

.auth-panel__scan-head span {
  margin-top: 0.35rem;
  display: inline-block;
  color: #1e80ff;
  font-size: 0.92rem;
}

.qr-stage {
  position: relative;
  width: min(18rem, 100%);
  aspect-ratio: 1;
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(32, 53, 94, 0.12);
  background: #ffffff;
  box-shadow:
    inset 0 0 0 1px rgba(32, 53, 94, 0.04),
    0 14px 32px rgba(30, 53, 103, 0.12);
}

.qr-stage__scanner {
  position: absolute;
  left: 1rem;
  right: 1rem;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(30, 128, 255, 0.75), transparent);
  box-shadow: 0 0 12px rgba(30, 128, 255, 0.45);
  animation: scan-line 2.6s linear infinite;
}

.qr-grid {
  display: grid;
  grid-template-columns: repeat(25, minmax(0, 1fr));
  gap: 2px;
  width: 100%;
  height: 100%;
}

.qr-grid__cell {
  border-radius: 1px;
  background: transparent;
}

.qr-grid__cell--filled {
  background: #111827;
}

.qr-grid__cell--finder {
  background: #111827;
}

.auth-panel__scan-copy {
  margin: 1.2rem 0 0;
  max-width: 24rem;
  color: #546784;
  line-height: 1.8;
}

.auth-panel__scan-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1.25rem;
}

.auth-panel__divider {
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(108, 128, 167, 0.32), transparent);
}

.auth-tabs {
  display: flex;
  gap: 0.4rem;
  padding: 0.3rem;
  border-radius: 999px;
  background: rgba(241, 245, 252, 0.95);
  width: fit-content;
}

.auth-tabs__item {
  min-height: 2.6rem;
  padding: 0 1rem;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #53657f;
  font-weight: 600;
}

.auth-tabs__item--active {
  background: linear-gradient(135deg, rgba(30, 128, 255, 0.14), rgba(30, 128, 255, 0.06));
  color: #1e80ff;
  box-shadow: inset 0 0 0 1px rgba(30, 128, 255, 0.12);
}

.auth-form {
  margin-top: 1.4rem;
  display: grid;
  gap: 1rem;
}

.auth-form__field {
  display: grid;
  gap: 0.45rem;
}

.auth-form__field span {
  color: #7e8da6;
  font-size: 0.75rem;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.auth-form__field input {
  height: 3.3rem;
  border: 1px solid rgba(83, 104, 142, 0.16);
  border-radius: 0.9rem;
  background: rgba(248, 250, 254, 0.95);
  box-shadow: none;
}

.auth-form__field input:focus-visible {
  outline: none;
  border-color: rgba(30, 128, 255, 0.38);
  box-shadow:
    0 0 0 4px rgba(30, 128, 255, 0.1),
    inset 0 0 0 1px rgba(30, 128, 255, 0.15);
}

.password-field,
.runtime-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.password-field__toggle {
  min-width: 4.6rem;
  height: 3.3rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(83, 104, 142, 0.12);
  background: #ffffff;
  color: #53657f;
}

.runtime-field .ghost-button {
  min-height: 3.3rem;
  padding: 0 1rem;
  border-radius: 0.9rem;
  background: rgba(255, 255, 255, 0.92);
}

.auth-form__field--compact input {
  min-width: 0;
}

.auth-form__tip {
  margin: 0;
  color: #5e7290;
  line-height: 1.75;
}

.auth-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 0.3rem;
}

.auth-form__submit {
  min-width: min(18rem, 100%);
  min-height: 3.35rem;
  justify-content: center;
  background: linear-gradient(135deg, #1e80ff, #4f8fff);
  box-shadow: 0 18px 32px rgba(30, 128, 255, 0.22);
}

.auth-form__submit:hover {
  box-shadow: 0 22px 38px rgba(30, 128, 255, 0.3);
}

.auth-form__submit:disabled {
  cursor: progress;
  opacity: 0.8;
  transform: none;
}

@keyframes scan-line {
  0% {
    top: 1rem;
    opacity: 0;
  }

  10%,
  90% {
    opacity: 1;
  }

  100% {
    top: calc(100% - 1.2rem);
    opacity: 0;
  }
}

@keyframes float-halo {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    transform: translate3d(1.4rem, 1rem, 0) scale(1.06);
  }
}

@keyframes rise-in {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }

  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1080px) {
  .auth-panel {
    grid-template-columns: 1fr;
  }

  .auth-panel__divider {
    width: auto;
    height: 1px;
  }

  .auth-panel__scan,
  .auth-panel__form {
    padding: 1.4rem;
  }

  .qr-stage {
    width: min(15rem, 100%);
  }
}

@media (max-width: 720px) {
  .login-page__header {
    padding: 1rem 1rem 0;
    flex-direction: column;
    align-items: flex-start;
  }

  .login-stage {
    width: calc(100% - 1.2rem);
    margin-top: 1.4rem;
  }

  .login-hero {
    justify-items: start;
    text-align: left;
  }

  .login-page__status,
  .auth-form__actions,
  .runtime-field,
  .password-field {
    width: 100%;
  }

  .auth-form__actions > *,
  .runtime-field > *,
  .password-field > * {
    width: 100%;
  }
}
</style>
