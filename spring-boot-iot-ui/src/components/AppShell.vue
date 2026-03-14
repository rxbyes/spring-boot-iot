<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">跳到主内容</a>
    <aside class="app-shell__sidebar">
      <div class="brand-block">
        <p class="brand-block__eyebrow">Risk Monitoring OS</p>
        <h1>Spring Boot IoT</h1>
        <p class="brand-block__summary">
          面向一线监测、运维维护和研发调试的风险监测与处置平台，当前以 Phase 1 / Phase 2
          已打通能力为底座，逐步演进为可交付的商业产品。
        </p>
      </div>

      <nav aria-label="主导航" class="nav-sections">
        <section
          v-for="section in navigationSections"
          :key="section.title"
          class="nav-section"
        >
          <p class="nav-section__eyebrow">{{ section.eyebrow }}</p>
          <h2 class="nav-section__title">{{ section.title }}</h2>
          <div class="nav-list">
            <RouterLink
              v-for="item in section.items"
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
          </div>
        </section>
      </nav>

      <div class="sidebar-foot">
        <p class="sidebar-foot__label">商业化演进主轴</p>
        <ol class="phase-rail">
          <li>风险发现与四色分级</li>
          <li>多传感器趋势研判</li>
          <li>上报与分析报告生成</li>
          <li>远程运维与阈值管理</li>
          <li>协议调试与日志审计</li>
          <li>告警闭环与未来预测</li>
        </ol>
      </div>
    </aside>

    <div class="app-shell__main">
      <header class="topbar">
        <div>
          <p class="topbar__eyebrow">Commercial Preview</p>
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

const navigationSections = [
  {
    title: '驾驶舱',
    eyebrow: 'Monitor',
    items: [
      { to: '/', label: '风险监测驾驶舱', caption: '总览风险态势、角色入口与处置闭环', icon: '01' }
    ]
  },
  {
    title: '风险业务',
    eyebrow: 'Risk Ops',
    items: [
      { to: '/insight', label: '风险点工作台', caption: '查看属性、日志、在线与趋势入口', icon: '02' }
    ]
  },
  {
    title: '运维中心',
    eyebrow: 'Ops',
    items: [
      { to: '/devices', label: '设备运维中心', caption: '设备建档、状态检索、在线核查', icon: '03' },
      { to: '/products', label: '产品模板中心', caption: '协议模板、产品建模、设备归属', icon: '04' }
    ]
  },
  {
    title: '研发与调试',
    eyebrow: 'Engineering',
    items: [
      { to: '/reporting', label: '接入回放台', caption: 'HTTP 上报模拟、payload 回放、topic 推演', icon: '05' },
      { to: '/file-debug', label: '文件与固件调试', caption: 'C.3 / C.4 / Redis 聚合 / MD5 校验', icon: '06' },
      { to: '/future-lab', label: '未来演进蓝图', caption: '图表、孪生、规则、告警、OTA 预留', icon: '07' }
    ]
  }
];

const capabilityTags = [
  'Risk Dashboard',
  'Field Workflow',
  'Remote O&M Ready',
  'HTTP / MQTT Access',
  'mqtt-json Decode',
  'C.3 / C.4 File Debug',
  'Protocol Security',
  'Audit Friendly',
  'AI Ready'
];

const baseUrlDraft = ref(runtimeState.apiBaseUrl);

watch(
  () => runtimeState.apiBaseUrl,
  (value) => {
    baseUrlDraft.value = value;
  }
);

const activeTitle = computed(() => String(route.meta.title || '风险监测平台'));
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
  grid-template-columns: minmax(290px, 348px) 1fr;
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
  gap: 1.4rem;
  padding: 1.5rem;
  border-right: 1px solid var(--panel-border);
  background:
    linear-gradient(180deg, rgba(7, 10, 20, 0.98), rgba(4, 8, 18, 0.92)),
    radial-gradient(circle at top left, rgba(34, 212, 255, 0.18), transparent 35%);
  overflow-y: auto;
}

.brand-block h1 {
  margin: 0.4rem 0 0.7rem;
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 2.2vw, 2.6rem);
}

.brand-block__eyebrow,
.topbar__eyebrow,
.sidebar-foot__label,
.nav-section__eyebrow {
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

.nav-sections {
  display: grid;
  gap: 1rem;
}

.nav-section {
  display: grid;
  gap: 0.7rem;
}

.nav-section__title {
  margin: 0;
  font-size: 1rem;
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
  padding: 1.2rem 1.3rem;
  border-radius: calc(var(--radius-lg) + 0.2rem);
  border: 1px solid rgba(83, 122, 185, 0.22);
  background:
    linear-gradient(135deg, rgba(7, 13, 28, 0.94), rgba(11, 19, 39, 0.9)),
    radial-gradient(circle at top right, rgba(255, 161, 67, 0.12), transparent 32%);
  box-shadow: 0 22px 70px rgba(3, 8, 18, 0.28);
}

.topbar__title {
  margin: 0.35rem 0 0.3rem;
  font-size: clamp(1.4rem, 1.9vw, 2.1rem);
}

.topbar__description {
  margin: 0;
  max-width: 48rem;
  color: var(--text-secondary);
  line-height: 1.7;
}

.topbar__controls {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.8rem;
  align-items: end;
}

.field {
  display: grid;
  gap: 0.4rem;
  min-width: min(24rem, 100%);
}

.field span {
  color: var(--text-tertiary);
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.15em;
}

.capability-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin: 1rem 0 0.85rem;
}

.capability-strip span {
  padding: 0.55rem 0.8rem;
  border-radius: 999px;
  border: 1px solid rgba(77, 121, 190, 0.28);
  background: rgba(10, 18, 35, 0.68);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.content-frame {
  margin-top: 1rem;
}

@media (max-width: 1280px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    position: relative;
    height: auto;
    border-right: none;
    border-bottom: 1px solid var(--panel-border);
  }
}

@media (max-width: 900px) {
  .topbar {
    flex-direction: column;
  }

  .topbar__controls {
    width: 100%;
    justify-content: stretch;
  }

  .field {
    min-width: 100%;
  }
}
</style>
