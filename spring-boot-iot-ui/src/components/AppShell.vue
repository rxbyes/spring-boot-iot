<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">跳到主内容</a>
    <aside class="app-shell__sidebar">
      <div class="brand-block">
        <p class="brand-block__eyebrow">IoT Debug Surface</p>
        <h1>Spring Boot IoT</h1>
        <p class="brand-block__summary">
          以 `vue-element-admin` 的后台结构为参考，重构成更适合物联网调试的 Vue 3 驾驶舱。
        </p>
      </div>

      <nav aria-label="主导航" class="nav-list">
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          class="nav-list__item"
          :class="{ 'nav-list__item--active': route.path === item.to }"
        >
          <span class="nav-list__icon">{{ item.icon }}</span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.caption }}</small>
          </span>
        </RouterLink>
      </nav>

      <div class="sidebar-foot">
        <p class="sidebar-foot__label">Phase 1 主链路</p>
        <ol class="phase-rail">
          <li>产品建模</li>
          <li>设备建档</li>
          <li>HTTP 上报</li>
          <li>协议解码</li>
          <li>属性落库</li>
          <li>在线更新</li>
        </ol>
      </div>
    </aside>

    <div class="app-shell__main">
      <header class="topbar">
        <div>
          <p class="topbar__eyebrow">Current View</p>
          <h2 class="topbar__title">{{ activeTitle }}</h2>
          <p class="topbar__description">{{ activeDescription }}</p>
        </div>
        <div class="topbar__controls">
          <label class="field">
            <span>API Base URL</span>
            <el-input
              v-model="baseUrlDraft"
              name="api_base_url"
              autocomplete="url"
              spellcheck="false"
              placeholder="例如 http://localhost:9999..."
            />
          </label>
          <el-button class="primary-button" type="primary" @click="saveApiBaseUrl">
            保存地址
          </el-button>
          <SignalBadge :label="connectionLabel" :tone="connectionTone" />
        </div>
      </header>

      <section class="capability-strip" aria-label="当前支持能力">
        <span v-for="tag in capabilityTags" :key="tag">{{ tag }}</span>
      </section>

      <TabsView />

      <main id="main-content" class="content-frame">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';

import { isProxyMode, runtimeState, setApiBaseUrl } from '../stores/runtime';
import SignalBadge from './SignalBadge.vue';
import TabsView from './TabsView.vue';

const route = useRoute();

const navigation = [
  { to: '/', label: '调试驾驶舱', caption: '总览主链路与最近操作', icon: '01' },
  { to: '/products', label: '产品工作台', caption: '新增产品 / 查询产品', icon: '02' },
  { to: '/devices', label: '设备工作台', caption: '新增设备 / 检查状态', icon: '03' },
  { to: '/reporting', label: 'HTTP 上报实验台', caption: '构造 payload / 回放报文', icon: '04' },
  { to: '/insight', label: '设备洞察', caption: '属性 + 日志 + 在线状态', icon: '05' },
  { to: '/file-debug', label: '文件调试台', caption: 'C.3 / C.4 / Redis 聚合', icon: '06' },
  { to: '/future-lab', label: '未来实验室', caption: '图表 / 数字孪生 / 拓扑', icon: '07' }
];

const capabilityTags = [
  'Product CRUD Seed',
  'Device Provisioning',
  'HTTP Simulated Report',
  'mqtt-json Decode',
  'Property Snapshot',
  'Message Log Trace',
  'File Payload Debug',
  'Element Plus UI',
  'ECharts Trend'
];

const baseUrlDraft = ref(runtimeState.apiBaseUrl);

watch(
  () => runtimeState.apiBaseUrl,
  (value) => {
    baseUrlDraft.value = value;
  }
);

const activeTitle = computed(() => String(route.meta.title || '调试台'));
const activeDescription = computed(() => String(route.meta.description || ''));
const connectionLabel = computed(() => (isProxyMode() ? '代理模式' : '直连模式'));
const connectionTone = computed<'brand' | 'success'>(() => (isProxyMode() ? 'brand' : 'success'));

function saveApiBaseUrl() {
  setApiBaseUrl(baseUrlDraft.value);
}
</script>

<style scoped>
.app-shell {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(280px, 320px) 1fr;
}

.skip-link {
  position: absolute;
  top: -40px;
  left: 1rem;
  z-index: 10;
  padding: 0.7rem 1rem;
  background: var(--brand-bright);
  color: var(--ink-strong);
  border-radius: 0.8rem;
}

.skip-link:focus {
  top: 1rem;
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  padding: 1.6rem;
  border-right: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(7, 10, 20, 0.98), rgba(4, 8, 18, 0.9)),
    radial-gradient(circle at top left, rgba(34, 212, 255, 0.18), transparent 35%);
}

.brand-block h1 {
  margin: 0.4rem 0 0.7rem;
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 2.2vw, 2.6rem);
}

.brand-block__eyebrow,
.topbar__eyebrow,
.sidebar-foot__label {
  margin: 0;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.2em;
  color: var(--text-tertiary);
}

.brand-block__summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.nav-list {
  display: grid;
  gap: 0.75rem;
}

.nav-list__item {
  display: flex;
  gap: 0.9rem;
  align-items: center;
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid transparent;
  color: inherit;
  text-decoration: none;
  background: rgba(10, 15, 29, 0.64);
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;
}

.nav-list__item:hover,
.nav-list__item:focus-visible,
.nav-list__item--active {
  transform: translateX(4px);
  border-color: var(--panel-border-strong);
  background: rgba(12, 20, 40, 0.94);
}

.nav-list__icon {
  display: inline-flex;
  width: 2.3rem;
  height: 2.3rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.85rem;
  font-family: var(--font-mono);
  background: rgba(43, 227, 255, 0.14);
  color: var(--brand-bright);
}

.nav-list__item strong,
.nav-list__item small {
  display: block;
}

.nav-list__item small {
  margin-top: 0.3rem;
  color: var(--text-tertiary);
}

.sidebar-foot {
  margin-top: auto;
  padding: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--panel-border);
  background: rgba(7, 12, 24, 0.9);
}

.phase-rail {
  margin: 0.95rem 0 0;
  padding-left: 1.2rem;
  color: var(--text-secondary);
  line-height: 1.8;
}

.app-shell__main {
  padding: 1.6rem;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  align-items: flex-start;
  padding: 1.3rem 1.4rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 0.4rem);
  background:
    linear-gradient(145deg, rgba(8, 13, 28, 0.96), rgba(7, 12, 25, 0.88)),
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.12), transparent 35%);
}

.topbar__title {
  margin: 0.4rem 0 0.5rem;
  font-size: clamp(1.7rem, 2vw, 2.4rem);
}

.topbar__description {
  margin: 0;
  max-width: 52rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.topbar__controls {
  display: flex;
  gap: 0.9rem;
  align-items: flex-end;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.field {
  display: grid;
  gap: 0.45rem;
  min-width: min(22rem, 100%);
}

.field span {
  color: var(--text-tertiary);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.field input {
  min-height: 3rem;
}

.primary-button {
  min-height: 3rem;
  padding: 0 1.1rem;
}

.capability-strip {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin: 1rem 0 0;
}

.capability-strip span {
  padding: 0.55rem 0.8rem;
  border-radius: 999px;
  border: 1px solid var(--panel-border);
  background: rgba(8, 14, 28, 0.78);
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.content-frame {
  margin-top: 1.2rem;
}

@media (max-width: 1080px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    position: static;
    height: auto;
  }

  .topbar {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-list__item {
    transition: none;
  }
}
</style>
