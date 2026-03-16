<template>
  <div class="login-page">
    <div class="login-page__glow login-page__glow--warm" />
    <div class="login-page__glow login-page__glow--cool" />

    <header class="login-page__header">
      <RouterLink class="login-brand" to="/login">
        <span class="login-brand__logo" aria-hidden="true" />
        <span class="login-brand__name">监测预警平台</span>
      </RouterLink>
    </header>

    <main class="login-stage">
      <section class="login-hero">
        <h1>风险监测预警处置平台</h1>
        <p>预判风险、协同处置、提升服务品质</p>
      </section>

      <section class="auth-panel">
        <aside class="auth-panel__left">
          <h2>登录</h2>
          <p class="auth-panel__left-tip">微信扫码快速登录</p>

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

          <p class="auth-panel__left-copy">支持微信扫码登录，能力持续优化中。</p>
        </aside>

        <div class="auth-panel__divider" />

        <section class="auth-panel__right">
          <div class="auth-tabs" role="tablist" aria-label="登录方式">
            <button
              type="button"
              class="auth-tabs__item"
              :class="{ 'auth-tabs__item--active': activeTab === 'account' }"
              :aria-selected="activeTab === 'account'"
              @click="switchTab('account')"
            >
              账号登录
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
                <span>账号名</span>
                <input
                  id="login-username"
                  v-model.trim="accountForm.username"
                  autocomplete="username"
                  name="login_username"
                  placeholder="请输入账号名"
                  spellcheck="false"
                />
              </label>
            </template>
            <template v-else>
              <label class="auth-form__field">
                <span>手机号</span>
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

            <p class="auth-form__tip">{{ formTip }}</p>

            <div class="auth-form__actions">
              <button id="login-submit" class="primary-button auth-form__submit" type="submit" :disabled="submitting">
                {{ submitting ? '登录中...' : submitLabel }}
              </button>
            </div>

            <div class="auth-form__links">
              <a href="javascript:void(0)" @click.prevent="showForgotHint('忘记登录名')">忘记登录名</a>
              <a href="javascript:void(0)" @click.prevent="showForgotHint('忘记密码')">忘记密码</a>
            </div>
          </form>
        </section>
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

function showForgotHint(type: string) {
  ElMessage.info(`${type}功能正在建设中`);
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
  --panel-bg: rgba(255, 255, 255, 0.82);
  --line-color: rgba(95, 118, 158, 0.2);
  --text-main: #1f2d46;
  --text-subtle: #607594;
  --brand-blue: #1f6df0;

  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: var(--text-main);
  background:
    radial-gradient(circle at 10% 8%, rgba(255, 186, 126, 0.44), transparent 30%),
    radial-gradient(circle at 100% 0, rgba(156, 195, 255, 0.42), transparent 36%),
    linear-gradient(146deg, #f4e8db 0%, #eaf2fc 42%, #c4def7 100%);
}

.login-page__glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(24px);
}

.login-page__glow--warm {
  width: 28rem;
  height: 28rem;
  left: -9rem;
  top: 14rem;
  background: rgba(255, 157, 84, 0.18);
}

.login-page__glow--cool {
  width: 34rem;
  height: 34rem;
  right: -10rem;
  bottom: -8rem;
  background: rgba(72, 145, 255, 0.18);
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
  padding: 1.5rem 2rem 0;
}

.login-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  color: var(--text-main);
  text-decoration: none;
}

.login-brand__logo {
  width: 2.15rem;
  height: 2.15rem;
  border-radius: 0.72rem;
  background: linear-gradient(150deg, #ff7800, #ff9d51);
  box-shadow: 0 10px 24px rgba(255, 126, 20, 0.24);
  position: relative;
}

.login-brand__logo::after {
  content: '';
  position: absolute;
  inset: 5px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 0.5rem;
}

.login-brand__name {
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.login-page__region {
  color: #42597f;
  font-size: 0.95rem;
}

.login-stage {
  width: min(1050px, calc(100% - 3.2rem));
  margin: 3.8rem auto 2rem;
}

.login-hero {
  text-align: center;
  margin-bottom: 2.2rem;
}

.login-hero h1 {
  margin: 0;
  font-size: clamp(2.9rem, 6vw, 4.1rem);
  line-height: 1.15;
  letter-spacing: 0.03em;
  font-weight: 700;
  color: #2a354a;
}

.login-hero p {
  margin: 1.05rem auto 0;
  max-width: 48rem;
  color: #5f7392;
  font-size: 1.36rem;
  line-height: 1.75;
}

.auth-panel {
  display: grid;
  grid-template-columns: 38% 1px 1fr;
  border-radius: 0;
  background: var(--panel-bg);
  border: 1px solid rgba(131, 156, 198, 0.18);
  box-shadow: 0 24px 64px rgba(22, 48, 88, 0.13);
  backdrop-filter: blur(4px);
}

.auth-panel__left,
.auth-panel__right {
  padding: 2.55rem 2.7rem;
}

.auth-panel__left h2 {
  margin: 0;
  font-size: 2.8rem;
  font-weight: 700;
  line-height: 1.2;
}

.auth-panel__left-tip {
  margin: 0.75rem 0 0;
  color: #2876ec;
  font-size: 1.18rem;
}

.qr-stage {
  position: relative;
  width: min(16.8rem, 100%);
  aspect-ratio: 1;
  margin-top: 1.35rem;
  padding: 0.95rem;
  border: 1px solid rgba(81, 109, 149, 0.16);
  background: #fff;
}

.qr-stage__scanner {
  position: absolute;
  left: 0.95rem;
  right: 0.95rem;
  height: 2px;
  background: linear-gradient(90deg, transparent, #2b82ff, transparent);
  box-shadow: 0 0 10px rgba(32, 126, 255, 0.52);
  animation: scan-line 2.7s linear infinite;
}

.qr-grid {
  display: grid;
  grid-template-columns: repeat(25, minmax(0, 1fr));
  gap: 2px;
  width: 100%;
  height: 100%;
}

.qr-grid__cell {
  background: transparent;
}

.qr-grid__cell--filled,
.qr-grid__cell--finder {
  background: #17243a;
}

.auth-panel__left-copy {
  margin: 1.2rem 0 0;
  color: var(--text-subtle);
  font-size: 1.1rem;
  line-height: 1.7;
}

.auth-panel__divider {
  width: 1px;
  background: linear-gradient(180deg, transparent, var(--line-color), transparent);
}

.auth-tabs {
  display: flex;
  align-items: center;
  gap: 1.35rem;
  margin-bottom: 1.65rem;
}

.auth-tabs__item {
  border: none;
  background: transparent;
  padding: 0.1rem 0;
  font-size: 1.32rem;
  font-weight: 600;
  color: #4e6487;
  border-bottom: 2px solid transparent;
}

.auth-tabs__item--active {
  color: var(--brand-blue);
  border-bottom-color: var(--brand-blue);
}

.auth-form {
  display: grid;
  gap: 1rem;
}

.auth-form__field {
  display: grid;
  gap: 0.45rem;
}

.auth-form__field span {
  color: #667d9f;
  font-size: 1.03rem;
}

.auth-form__field input {
  height: 3.25rem;
  border: 1px solid rgba(99, 122, 158, 0.2);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.9);
  color: #1d2f4b;
  font-size: 1.14rem;
}

.auth-form__field input:focus-visible {
  outline: none;
  border-color: rgba(31, 109, 240, 0.6);
  box-shadow: 0 0 0 3px rgba(31, 109, 240, 0.14);
}

.password-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.7rem;
}

.password-field__toggle {
  min-width: 4.9rem;
  border: 1px solid rgba(99, 122, 158, 0.2);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.92);
  color: #566b8d;
  font-size: 1.05rem;
}

.auth-form__tip {
  margin: 0.15rem 0 0;
  color: #607594;
  font-size: 1.02rem;
}

.auth-form__actions {
  margin-top: 0.55rem;
}

.auth-form__submit {
  width: 100%;
  min-height: 3.35rem;
  justify-content: center;
  border-radius: 0;
  background: linear-gradient(135deg, #1f6df0, #327ff8);
  box-shadow: 0 10px 24px rgba(31, 109, 240, 0.25);
  font-size: 1.2rem;
  letter-spacing: 0.04em;
}

.auth-form__submit:disabled {
  opacity: 0.8;
  cursor: progress;
}

.auth-form__links {
  display: flex;
  align-items: center;
  gap: 1.3rem;
  margin-top: 0.55rem;
}

.auth-form__links a {
  color: #33506f;
  text-decoration: none;
  font-size: 1.03rem;
}

.auth-form__links a:hover {
  color: #1f6df0;
}

@keyframes scan-line {
  0% {
    top: 0.95rem;
    opacity: 0;
  }

  12%,
  88% {
    opacity: 1;
  }

  100% {
    top: calc(100% - 1.1rem);
    opacity: 0;
  }
}

@media (max-width: 1080px) {
  .login-stage {
    margin-top: 2.5rem;
  }

  .auth-panel {
    grid-template-columns: 1fr;
  }

  .auth-panel__divider {
    width: auto;
    height: 1px;
  }
}

@media (max-width: 760px) {
  .login-page__header {
    padding: 1rem 1rem 0;
  }

  .login-page__region {
    display: none;
  }

  .login-stage {
    width: calc(100% - 1.2rem);
    margin-top: 1.6rem;
  }

  .login-hero {
    text-align: left;
    margin-bottom: 1.2rem;
  }

  .login-hero h1 {
    font-size: 2.1rem;
  }

  .login-hero p {
    font-size: 1rem;
    margin-top: 0.6rem;
  }

  .auth-panel__left,
  .auth-panel__right {
    padding: 1.35rem;
  }

  .auth-form__links {
    justify-content: space-between;
  }
}
</style>
