# IoT Access Workbench UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改动路由、接口和权限模型的前提下，完成 `接入智维总览`、`产品定义中心`、`设备资产中心`、`链路验证中心`、`异常观测台`、`链路追踪台`、`数据校验台` 的统一 UI 刷新，落实“总览中枢台 + 资产治理专台 + 诊断实验专台”的已批准方案。

**Architecture:** 先补一个极小的 `iotAccess` 共享展示层，只做 Hero 和“先做什么”信号板，不另起第二套设计系统；页面主体继续复用 `StandardWorkbenchPanel`、`PanelCard`、`MetricCard`、`StandardListFilterHeader`、`StandardTableToolbar`、`StandardDetailDrawer` 等现有标准件。实施顺序固定为“共享底座 -> 总览 -> 资产治理页 -> 诊断页 -> 文档与验证”，其中总览负责跨模块判断和入口分发，子页只负责本域处理，不再重复首页化表达。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Vite, CSS variables, shared Standard* components

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
- `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- `docs/02-业务功能与流程说明.md`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### New files to create

- `spring-boot-iot-ui/src/components/iotAccess/IotAccessWorkbenchHero.vue`
- `spring-boot-iot-ui/src/components/iotAccess/IotAccessSignalDeck.vue`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

说明：

- `/device-access` 仍由 `SectionLandingView.vue` 承接，不新建 `DeviceAccessView.vue`。
- `/system-log` 与 `/audit-log` 继续共用 `AuditLogView.vue`；本轮只把 `/system-log` 的系统异常模式做成“异常观测台”诊断专台，`/audit-log` 的审计中心模式不做同构改造。
- `MessageTraceView.vue` 继续保留同路由双模式，不新增失败归档独立路由。
- `README.md` 与 `AGENTS.md` 仅在最终文档核对时确认是否需要一句新的工作台定位说明；若现有表述已足够，则保持不改并在任务记录中说明“已复核，无需更新”。

## Implementation Notes

- 保持现有品牌橙、云蓝、蓝灰 token，不新增页面私有主色。
- 不调整任何 API 入参、权限编码、分页契约和详情抽屉打开逻辑。
- 资产治理页的主战区仍然是台账；本轮不把产品页和设备页改成首页式拼卡布局。
- 诊断页仍然保留现有主工作流，但要补齐更明确的 Hero、结果导向判断和强相关联动。
- 由于当前工作区已存在其他未提交改动，每一步提交都只能 `git add` 本计划列出的文件，不要使用 `git add .`。

### Task 1: 搭建 IoT Access 共享 Hero 与信号板底座

**Files:**
- Create: `spring-boot-iot-ui/src/components/iotAccess/IotAccessWorkbenchHero.vue`
- Create: `spring-boot-iot-ui/src/components/iotAccess/IotAccessSignalDeck.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`

- [ ] **Step 1: 先写共享组件与总览 schema 的失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts` 新增：

```ts
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { defineComponent } from 'vue';

import IotAccessWorkbenchHero from '@/components/iotAccess/IotAccessWorkbenchHero.vue';

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  props: ['to'],
  template: '<a :href="String(to)"><slot /></a>'
});

describe('IotAccessWorkbenchHero', () => {
  it('renders judgement, tags, actions and summary items', () => {
    const wrapper = mount(IotAccessWorkbenchHero, {
      props: {
        eyebrow: '接入智维 / 资产治理',
        title: '产品定义中心',
        judgement: '先补齐产品契约，再处理库存治理。',
        description: '统一承接产品台账、物模型治理提醒与强相关跳转。',
        tags: [
          { label: '工作母版', value: 'A 指挥甲板型' },
          { label: '页面倾向', value: 'B 资产治理型' }
        ],
        actions: [
          { label: '新增产品', to: '/products', variant: 'primary' },
          { label: '查看设备资产', to: '/devices', variant: 'secondary' }
        ],
        summaryItems: [
          { label: '治理主判断', value: '契约完整性优先' },
          { label: '主战区', value: '产品台账' }
        ]
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub
        }
      }
    });

    expect(wrapper.text()).toContain('先补齐产品契约，再处理库存治理。');
    expect(wrapper.findAll('.iot-access-workbench-hero__tag')).toHaveLength(2);
    expect(wrapper.findAll('.iot-access-workbench-hero__summary-item')).toHaveLength(2);
    expect(wrapper.findAll('a')).toHaveLength(2);
  });
});
```

在 `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts` 新增：

```ts
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { defineComponent } from 'vue';

import IotAccessSignalDeck from '@/components/iotAccess/IotAccessSignalDeck.vue';

const MetricCardStub = defineComponent({
  name: 'MetricCard',
  props: ['label', 'value'],
  template: '<div class="metric-card-stub">{{ label }} {{ value }}</div>'
});

describe('IotAccessSignalDeck', () => {
  it('renders lead action and compact metrics', () => {
    const wrapper = mount(IotAccessSignalDeck, {
      props: {
        lead: {
          eyebrow: '先做什么',
          title: '优先处理产品契约阻塞',
          description: '停用库存阻塞和候选待确认都需要先收口。',
          actionLabel: '进入产品定义中心',
          actionTo: '/products'
        },
        metrics: [
          { label: '待确认候选', value: '12' },
          { label: '停用阻塞', value: '4' },
          { label: '最近活跃产品', value: '9' }
        ]
      },
      global: {
        stubs: {
          MetricCard: MetricCardStub,
          RouterLink: defineComponent({
            name: 'RouterLink',
            props: ['to'],
            template: '<a :href="String(to)"><slot /></a>'
          })
        }
      }
    });

    expect(wrapper.text()).toContain('优先处理产品契约阻塞');
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(3);
  });
});
```

在 `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts` 追加：

```ts
it('keeps iot-access overview as a hub instead of a child-page directory clone', () => {
  const config = getSectionHomeConfigByPath('/device-access');
  expect(config?.intro).toContain('先去哪处理');
  expect(config?.description).toContain('中枢');
  expect(config?.cards).toHaveLength(6);
});
```

- [ ] **Step 2: 运行测试确认当前为红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts src/__tests__/utils/sectionHomes.test.ts --run
```

Expected:

- 新组件测试失败，因为组件文件还不存在。
- `sectionHomes.test.ts` 新断言失败，因为 `sectionWorkspaces.ts` 还没有新的中枢定位文案。

- [ ] **Step 3: 写最小共享实现与总览 schema 扩展**

创建 `spring-boot-iot-ui/src/components/iotAccess/IotAccessWorkbenchHero.vue`：

```vue
<template>
  <PanelCard class="iot-access-workbench-hero">
    <div class="iot-access-workbench-hero__layout">
      <div class="iot-access-workbench-hero__main">
        <p class="iot-access-workbench-hero__eyebrow">{{ eyebrow }}</p>
        <h2 class="iot-access-workbench-hero__title">{{ title }}</h2>
        <p class="iot-access-workbench-hero__judgement">{{ judgement }}</p>
        <p v-if="description" class="iot-access-workbench-hero__description">{{ description }}</p>

        <div v-if="tags.length" class="iot-access-workbench-hero__tags">
          <span v-for="tag in tags" :key="`${tag.label}:${tag.value}`" class="iot-access-workbench-hero__tag">
            <small>{{ tag.label }}</small>
            <strong>{{ tag.value }}</strong>
          </span>
        </div>

        <div v-if="actions.length" class="iot-access-workbench-hero__actions">
          <RouterLink
            v-for="action in actions"
            :key="`${action.label}:${action.to}`"
            :to="action.to"
            :class="['iot-access-workbench-hero__action', `iot-access-workbench-hero__action--${action.variant || 'secondary'}`]"
          >
            {{ action.label }}
          </RouterLink>
        </div>
      </div>

      <div v-if="summaryItems.length" class="iot-access-workbench-hero__summary">
        <article
          v-for="item in summaryItems"
          :key="`${item.label}:${item.value}`"
          class="iot-access-workbench-hero__summary-item"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </div>
  </PanelCard>
</template>
```

创建 `spring-boot-iot-ui/src/components/iotAccess/IotAccessSignalDeck.vue`：

```vue
<template>
  <section class="iot-access-signal-deck">
    <PanelCard class="iot-access-signal-deck__lead">
      <p class="iot-access-signal-deck__eyebrow">{{ lead.eyebrow }}</p>
      <h3>{{ lead.title }}</h3>
      <p>{{ lead.description }}</p>
      <RouterLink class="iot-access-signal-deck__action" :to="lead.actionTo">
        {{ lead.actionLabel }}
      </RouterLink>
    </PanelCard>

    <div class="iot-access-signal-deck__metrics">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :badge="metric.badge"
        size="compact"
      />
    </div>
  </section>
</template>
```

在 `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts` 为 `SectionHomeConfig` 增加总览页需要的字段，并先把 `iot-access` 配置改成中枢口径：

```ts
export interface SectionHomeConfig {
  key: string;
  path: string;
  navLabel: string;
  navCaption: string;
  navShort: string;
  title: string;
  description: string;
  intro: string;
  hubJudgement?: string;
  hubLeadTitle?: string;
  hubLeadDescription?: string;
  hubLeadPath?: string;
  menuTitle: string;
  menuHint: string;
  matchKeys: string[];
  matchLabels: string[];
  cards: SectionHomeCard[];
  steps: string[];
}
```

把 `iot-access` 配置中的摘要文案替换为：

```ts
description: '接入智维总览作为中枢台，负责跨模块判断、入口分发和最近动作恢复。',
intro: '这里回答“整体什么情况、先去哪处理”；进入子页后再回答“在这个域里怎么做”。',
hubJudgement: '先完成资产底座，再进入诊断实验台收口链路问题。',
hubLeadTitle: '优先处理资产底座与最近异常联动',
hubLeadDescription: '产品与设备先稳住，链路验证、异常观测、链路追踪、数据校验只保留强相关联动。',
hubLeadPath: '/products',
```

- [ ] **Step 4: 运行测试确认共享底座转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts src/__tests__/utils/sectionHomes.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交共享底座**

```bash
git add spring-boot-iot-ui/src/components/iotAccess/IotAccessWorkbenchHero.vue spring-boot-iot-ui/src/components/iotAccess/IotAccessSignalDeck.vue spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts
git commit -m "feat: add iot access shared workbench hero primitives"
```

### Task 2: 刷新接入智维总览为真正的中枢台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`

- [ ] **Step 1: 先写总览页的失败测试**

创建 `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`：

```ts
import { computed, defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import SectionLandingView from '@/views/SectionLandingView.vue';

const mockRoute = { path: '/device-access' };
const mockRouter = { push: vi.fn() };

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter,
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="String(to)"><slot /></a>'
  })
}));

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    primaryRoleName: '开发人员',
    roleProfile: { focusLabel: '接入智维', featuredPaths: ['/products', '/devices'] },
    userInfo: { accountType: '正式账号', authStatus: '已实名' },
    hasRoutePermission: () => true
  })
}));

vi.mock('@/stores/activity', () => ({
  activityEntries: computed(() => [
    {
      id: '1',
      title: '链路验证中心 · 发送模拟上报',
      detail: '刚刚完成一次 HTTP 模拟上报',
      module: '链路验证中心',
      action: '发送模拟上报',
      ok: true,
      createdAt: '2026-03-27T09:00:00.000Z',
      path: '/reporting'
    }
  ])
}));

describe('SectionLandingView', () => {
  beforeEach(() => {
    mockRoute.path = '/device-access';
  });

  it('renders iot-access as a hub page with recent restore and recommended order', () => {
    const wrapper = mount(SectionLandingView, {
      global: {
        stubs: {
          EmptyState: true,
          PanelCard: defineComponent({
            name: 'PanelCard',
            props: ['eyebrow', 'title', 'description'],
            template: '<section><slot name="header" /><slot /></section>'
          }),
          IotAccessWorkbenchHero: true,
          IotAccessSignalDeck: true
        }
      }
    });

    expect(wrapper.text()).toContain('最近使用');
    expect(wrapper.text()).toContain('推荐处理顺序');
    expect(wrapper.text()).toContain('全部能力');
    expect(wrapper.text()).toContain('链路验证中心');
  });
});
```

- [ ] **Step 2: 运行测试确认总览改造尚未实现**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/SectionLandingView.test.ts --run
```

Expected: FAIL，因为 `SectionLandingView.vue` 还没有接入新的 Hero/Signal Deck 结构。

- [ ] **Step 3: 重写总览首屏结构，但保留“最近使用 / 推荐顺序 / 全部能力”三块职责**

把 `spring-boot-iot-ui/src/views/SectionLandingView.vue` 的首屏改成：

```vue
<template>
  <div class="section-landing page-stack">
    <IotAccessWorkbenchHero
      :eyebrow="`${config?.title || '接入智维'} / 中枢台`"
      :title="config?.title || '分组概览'"
      :judgement="config?.hubJudgement || '先判断优先域，再进入单域专台。'"
      :description="config?.intro || '当前分组用于承接该一级导航下的共性能力。'"
      :tags="heroTags"
      :actions="heroActions"
      :summary-items="heroSummaryItems"
    />

    <IotAccessSignalDeck
      :lead="leadSignal"
      :metrics="heroStats"
    />

    <div class="section-landing__content-grid">
      <PanelCard eyebrow="Recent Activity" title="最近使用" description="优先回到刚处理过的功能。">
        <div v-if="recentActivities.length" class="section-landing__recent-list">
          <RouterLink
            v-for="item in recentActivities"
            :key="item.id"
            :to="item.path"
            class="section-landing__recent-item"
          >
            <div class="section-landing__recent-main">
              <strong>{{ item.title }}</strong>
              <p>{{ item.detail }}</p>
            </div>
            <small>{{ item.time }}</small>
          </RouterLink>
        </div>
        <EmptyState
          v-else
          title="暂无最近使用记录"
          description="当前分组还没有本地操作痕迹，建议先从推荐顺序进入。"
          :action="emptyAction"
        />
      </PanelCard>

      <PanelCard eyebrow="Recommended Flow" title="推荐处理顺序" description="总览负责“先去哪”，子页负责“在这里怎么做”。">
        <div class="section-landing__recommend-list">
          <article
            v-for="action in recommendedActions"
            :key="action.path"
            class="section-landing__recommend-item"
          >
            <span class="section-landing__recommend-stage">{{ action.stage }}</span>
            <div class="section-landing__recommend-main">
              <strong>{{ action.title }}</strong>
              <p>{{ action.description }}</p>
            </div>
            <RouterLink :to="action.path" class="section-landing__text-link">
              {{ action.buttonLabel }}
            </RouterLink>
          </article>
        </div>
      </PanelCard>
    </div>

    <PanelCard eyebrow="Capability" title="全部能力" description="统一保留 6 个专台入口，不再在子页复制能力墙。">
      <div class="section-landing__capability-list">
        <RouterLink
          v-for="card in accessibleCards"
          :key="card.path"
          :to="card.path"
          class="section-landing__capability-item"
        >
          <strong>{{ card.label }}</strong>
          <span>{{ card.description }}</span>
        </RouterLink>
      </div>
    </PanelCard>
  </div>
</template>
```

在同文件脚本里补齐：

```ts
import IotAccessSignalDeck from '@/components/iotAccess/IotAccessSignalDeck.vue';
import IotAccessWorkbenchHero from '@/components/iotAccess/IotAccessWorkbenchHero.vue';

const heroActions = computed(() => {
  const actions = [];
  if (primaryCard.value) {
    actions.push({ label: `进入 ${primaryCard.value.label}`, to: primaryCard.value.path, variant: 'primary' as const });
  }
  if (secondaryCard.value) {
    actions.push({ label: `查看 ${secondaryCard.value.label}`, to: secondaryCard.value.path, variant: 'secondary' as const });
  }
  return actions;
});

const heroSummaryItems = computed(() => [
  { label: '跨模块判断', value: config.value?.hubJudgement || '先判断优先域' },
  { label: '当前角色关注', value: permissionStore.roleProfile.focusLabel || '平台总览' },
  { label: '最近恢复入口', value: recentActivities.value[0]?.title || '暂无最近动作' }
]);

const leadSignal = computed(() => ({
  eyebrow: '先做什么',
  title: config.value?.hubLeadTitle || '先进入高优域',
  description: config.value?.hubLeadDescription || '优先处理最需要进入的单域专台。',
  actionLabel: primaryCard.value ? `进入 ${primaryCard.value.label}` : '查看能力',
  actionTo: primaryCard.value?.path || route.path
}));
```

- [ ] **Step 4: 运行总览测试确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/SectionLandingView.test.ts src/__tests__/utils/sectionHomes.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交总览页改造**

```bash
git add spring-boot-iot-ui/src/views/SectionLandingView.vue spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts
git commit -m "feat: refresh iot access overview as command hub"
```

### Task 3: 把产品定义中心收口为资产治理专台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 用失败测试锁定新的资产治理 Hero**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 追加：

```ts
it('renders a product-governance hero above the ledger workbench', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('产品契约治理');
  expect(wrapper.text()).toContain('先补齐产品契约，再处理库存治理。');
  expect(wrapper.text()).toContain('产品台账');
});
```

并把 stubs 补齐：

```ts
IotAccessWorkbenchHero: defineComponent({
  name: 'IotAccessWorkbenchHero',
  props: ['title', 'judgement'],
  template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
}),
IotAccessSignalDeck: defineComponent({
  name: 'IotAccessSignalDeck',
  template: '<section class="iot-access-signal-deck-stub">产品台账</section>'
}),
```

- [ ] **Step 2: 运行测试确认现有页面还没有资产 Hero**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: FAIL，因为当前页面还直接从 `StandardWorkbenchPanel` 开始。

- [ ] **Step 3: 在产品页顶部加入资产治理 Hero 和治理信号板**

把 `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue` 的根模板改成：

```vue
<template>
  <div class="page-stack product-asset-view">
    <IotAccessWorkbenchHero
      eyebrow="接入智维 / 资产治理"
      title="产品定义中心"
      judgement="先补齐产品契约，再处理库存治理。"
      description="这里回答产品契约是否完整、停用库存是否阻塞，以及物模型治理现在先做什么。"
      :tags="productHeroTags"
      :actions="productHeroActions"
      :summary-items="productHeroSummary"
    />

    <IotAccessSignalDeck
      :lead="productLeadSignal"
      :metrics="productSignalMetrics"
    />

    <StandardWorkbenchPanel
      title="产品台账"
      description="主战区保持为产品台账，查询、批量治理、导出和物模型入口都不绕路。"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      :show-inline-state="showListInlineState"
      show-pagination
    >
```

在同文件脚本里增加：

```ts
const productHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'B 资产治理型' },
  { label: '主战区', value: '产品台账' }
]);

const productHeroActions = computed(() => [
  { label: '新增产品', to: '/products', variant: 'primary' as const },
  { label: '查看设备资产', to: '/devices', variant: 'secondary' as const }
]);

const productHeroSummary = computed(() => [
  { label: '启用产品', value: String(enabledProductCount.value) },
  { label: '停用产品', value: String(disabledProductCount.value) },
  { label: '当前结果', value: String(pagination.total) }
]);

const productLeadSignal = computed(() => ({
  eyebrow: '治理提醒',
  title: '优先处理契约与停用阻塞',
  description: '候选待确认、停用产品库存阻塞、最近活跃产品变化都应该先在这里收口。',
  actionLabel: '打开物模型治理',
  actionTo: '/products'
}));

const productSignalMetrics = computed(() => [
  { label: '启用产品', value: String(enabledProductCount.value) },
  { label: '停用产品', value: String(disabledProductCount.value) },
  { label: '已选项', value: String(selectedRows.value.length) }
]);
```

- [ ] **Step 4: 运行测试确认产品页改造通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交产品资产治理页**

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "feat: refresh product workbench as asset governance page"
```

### Task 4: 把设备资产中心收口为资产运行专台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 先补一个失败测试锁定设备运行 Hero**

在 `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts` 追加：

```ts
it('renders a device-operations hero above the device ledger', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('资产运行 Hero');
  expect(wrapper.text()).toContain('在线率、激活率和拓扑异常');
  expect(wrapper.text()).toContain('设备台账');
});
```

并补齐 stubs：

```ts
IotAccessWorkbenchHero: defineComponent({
  name: 'IotAccessWorkbenchHero',
  props: ['title', 'judgement'],
  template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
}),
IotAccessSignalDeck: defineComponent({
  name: 'IotAccessSignalDeck',
  template: '<section class="iot-access-signal-deck-stub">设备台账</section>'
}),
```

- [ ] **Step 2: 运行测试确认当前设备页还没接入新 Hero**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 给设备页补上资产运行 Hero 与运行态摘要层**

把 `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue` 根模板改为：

```vue
<template>
  <div class="page-stack device-asset-view">
    <IotAccessWorkbenchHero
      eyebrow="接入智维 / 资产治理"
      title="设备资产中心"
      judgement="先判断在线、激活和拓扑异常，再进入设备台账治理。"
      description="这里回答设备运行态是否稳定、拓扑关系是否清晰，以及当前该从哪类设备切入治理。"
      :tags="deviceHeroTags"
      :actions="deviceHeroActions"
      :summary-items="deviceHeroSummary"
    />

    <IotAccessSignalDeck
      :lead="deviceLeadSignal"
      :metrics="deviceSignalMetrics"
    />

    <StandardWorkbenchPanel
      title="设备台账"
      description="主战区保持为设备台账，父子拓扑、导入、更换和对象联动继续留在统一主卡内。"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      :show-inline-state="showListInlineState"
      show-pagination
    >
```

在脚本里增加：

```ts
const deviceHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'B 资产治理型' },
  { label: '主战区', value: '设备台账' }
]);

const deviceHeroActions = computed(() => [
  { label: '新增设备', to: '/devices', variant: 'primary' as const },
  { label: '链路验证中心', to: '/reporting', variant: 'secondary' as const }
]);

const deviceHeroSummary = computed(() => [
  { label: '在线设备', value: String(onlineCount.value) },
  { label: '已激活设备', value: String(activatedCount.value) },
  { label: '未登记设备', value: String(unregisteredCount.value) }
]);

const deviceLeadSignal = computed(() => ({
  eyebrow: '治理提醒',
  title: '优先清理未登记、长时间未上报和拓扑异常设备',
  description: '父子拓扑异常、长期未上报和禁用设备需要在主战区快速聚焦处理。',
  actionLabel: '刷新设备资产',
  actionTo: '/devices'
}));

const deviceSignalMetrics = computed(() => [
  { label: '已登记', value: String(registeredCount.value) },
  { label: '未登记', value: String(unregisteredCount.value) },
  { label: '在线', value: String(onlineCount.value) },
  { label: '已激活', value: String(activatedCount.value) }
]);
```

- [ ] **Step 4: 运行测试确认设备页改造通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交设备资产治理页**

```bash
git add spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "feat: refresh device workbench as asset operations page"
```

### Task 5: 把链路验证中心强化为诊断实验专台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`

- [ ] **Step 1: 用失败测试锁定诊断 Hero 与结果导向首屏**

在 `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts` 追加：

```ts
it('renders a diagnostic hero and result-focused signal deck above the simulator stage', () => {
  const wrapper = mountView();

  expect(wrapper.text()).toContain('先校准设备身份，再发报文，再看时间线。');
  expect(wrapper.text()).toContain('链路验证中心');
  expect(wrapper.text()).toContain('诊断复盘');
});
```

并在 `mountView()` stubs 中增加：

```ts
IotAccessWorkbenchHero: defineComponent({
  name: 'IotAccessWorkbenchHero',
  props: ['title', 'judgement'],
  template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
}),
IotAccessSignalDeck: defineComponent({
  name: 'IotAccessSignalDeck',
  template: '<section class="iot-access-signal-deck-stub">诊断实验专台</section>'
}),
MetricCard: MetricCardStub,
```

- [ ] **Step 2: 运行测试确认链路验证中心还未切到新首屏**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 用 Hero + 信号板替换顶部散装概览卡**

把 `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue` 顶部结构改成：

```vue
<template>
  <div class="page-stack reporting-view ops-workbench iot-access-diagnostic-view">
    <IotAccessWorkbenchHero
      eyebrow="接入智维 / 诊断实验"
      title="链路验证中心"
      judgement="先校准设备身份，再发报文，再看时间线。"
      description="这里回答当前模拟配置是否正确、本次报文是否发送成功，以及下一步应该继续查哪里。"
      :tags="reportingHeroTags"
      :actions="reportingHeroActions"
      :summary-items="reportingHeroSummary"
    />

    <IotAccessSignalDeck
      :lead="reportingLeadSignal"
      :metrics="reportingOverviewMetrics"
    />

    <section class="reporting-main-layout">
      <PanelCard class="reporting-surface reporting-surface--compose">
        <template #header>
          <div class="reporting-surface__header">
            <div class="reporting-surface__heading">
              <p class="reporting-surface__eyebrow">链路验证中心</p>
              <h2 class="reporting-surface__title">模拟上报</h2>
              <p class="reporting-surface__description">
                按设备编码加载接入契约后，完成 HTTP / MQTT 双通道模拟上报。
              </p>
            </div>
            <span class="reporting-surface__badge">左侧模拟上报</span>
          </div>
        </template>
      </PanelCard>

      <PanelCard class="reporting-surface reporting-surface--diagnosis">
        <template #header>
          <div class="reporting-surface__header">
            <div class="reporting-surface__heading">
              <p class="reporting-surface__eyebrow">链路验证中心</p>
              <h2 class="reporting-surface__title">诊断复盘</h2>
              <p class="reporting-surface__description">
                右侧统一查看诊断摘要、实际发送内容、帧预演和最近一次响应结果。
              </p>
            </div>
            <span class="reporting-surface__badge reporting-surface__badge--accent">右侧诊断复盘</span>
          </div>
        </template>
      </PanelCard>
    </section>
  </div>
</template>
```

这一阶段只改顶部概览结构；`reporting-main-layout` 内部现有左侧“模拟上报”卡和右侧“诊断复盘”卡的主体内容保持不变，不要顺手重写表单和时间线逻辑。

在脚本中新增：

```ts
const reportingHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'C 诊断实验室型' },
  { label: '主战区', value: '左操作右结果' }
]);

const reportingHeroActions = computed(() => [
  { label: '开始一次模拟验证', to: '/reporting', variant: 'primary' as const },
  { label: '打开链路追踪台', to: '/message-trace', variant: 'secondary' as const }
]);

const reportingHeroSummary = computed(() => [
  { label: '当前设备', value: resolvedDevice.value?.deviceCode || '--' },
  { label: '当前通道', value: transportMode.value.toUpperCase() },
  { label: '当前状态', value: sendStatusText.value }
]);

const reportingLeadSignal = computed(() => ({
  eyebrow: '诊断建议',
  title: '先把设备身份和 Topic 校准到正确轨道',
  description: '只有设备查询、传输方式和 Topic 建立正确映射，后面的时间线和响应才有诊断意义。',
  actionLabel: '继续链路验证',
  actionTo: '/reporting'
}));
```

- [ ] **Step 4: 运行测试确认链路验证页转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ReportWorkbenchView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交链路验证中心改造**

```bash
git add spring-boot-iot-ui/src/views/ReportWorkbenchView.vue spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts
git commit -m "feat: refresh reporting workbench as diagnostic lab page"
```

### Task 6: 把链路追踪台改成“模式切换清晰 + 辅战区完整”的诊断专台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: 先补失败测试，锁定追踪台 Hero 和辅战区**

在 `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts` 追加：

```ts
it('renders a trace hero and support deck while preserving page-mode switching', async () => {
  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('先判断当前是追踪排查还是失败归档。');
  expect(wrapper.text()).toContain('运维看板');
  expect(wrapper.text()).toContain('最近会话');
});
```

并补齐 stubs：

```ts
IotAccessWorkbenchHero: defineComponent({
  name: 'IotAccessWorkbenchHero',
  props: ['title', 'judgement'],
  template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
}),
IotAccessSignalDeck: defineComponent({
  name: 'IotAccessSignalDeck',
  template: '<section class="iot-access-signal-deck-stub">追踪摘要层</section>'
}),
```

- [ ] **Step 2: 运行测试确认追踪台未完成新首屏**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 把追踪 Hero、信号板和辅战区落位到同一节奏**

把 `spring-boot-iot-ui/src/views/MessageTraceView.vue` 的消息追踪模式改成：

```vue
<template>
  <div class="page-stack message-trace-view">
    <template v-if="!isAccessErrorMode">
      <IotAccessWorkbenchHero
        eyebrow="接入智维 / 诊断实验"
        title="链路追踪台"
        judgement="先判断当前是追踪排查还是失败归档，再进入单域复盘。"
        description="这里回答当前 Trace / session / Topic 链路是否清晰，以及下一步是否应联动到异常观测或失败归档。"
        :tags="traceHeroTags"
        :actions="traceHeroActions"
        :summary-items="traceHeroSummary"
      />

      <IotAccessSignalDeck
        :lead="traceLeadSignal"
        :metrics="traceSignalMetrics"
      />
    </template>

    <AccessErrorArchivePanel
      v-if="isAccessErrorMode"
      :view-mode="pageMode"
      :view-mode-options="pageModeOptions"
      @change-view-mode="handlePageModeChange"
    />

    <StandardWorkbenchPanel
      v-else
      title="链路追踪台"
      description="按 TraceId、设备编码、产品标识与 Topic 串联设备接入消息链路。"
      show-header-actions
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #header-actions>
        <StandardChoiceGroup
          :model-value="pageMode"
          :options="pageModeOptions"
          responsive
          @update:modelValue="handlePageModeChange"
        />
      </template>

      <div class="message-trace-stage">
        <el-table
          v-loading="loading"
          class="message-trace-table"
          :data="tableData"
          border
          stripe
          style="width: 100%"
        />
      </div>
    </StandardWorkbenchPanel>
  </div>
</template>
```

把当前 `#notices` 里的“运维看板”和“最近会话”两块 `PanelCard` 原样搬到表格下方的新 `.message-trace-support-grid` 区域，不改它们内部的数据绑定和按钮逻辑，只改变它们所在的版面层级。

在脚本中新增：

```ts
const traceHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'C 诊断实验室型' },
  { label: '双模式', value: pageMode.value === 'trace' ? '追踪排查' : '失败归档' }
]);

const traceHeroActions = computed(() => [
  { label: pageMode.value === 'trace' ? '查看失败归档' : '回到消息追踪', to: '/message-trace', variant: 'primary' as const },
  { label: '跳转异常观测台', to: '/system-log', variant: 'secondary' as const }
]);

const traceHeroSummary = computed(() => [
  { label: '近 1 小时', value: String(traceStats.recentHourCount) },
  { label: '近 24 小时', value: String(traceStats.recent24HourCount) },
  { label: '失败摘要', value: String(traceStats.dispatchFailureCount) }
]);

const traceLeadSignal = computed(() => ({
  eyebrow: '诊断建议',
  title: pageMode.value === 'trace' ? '优先沿 TraceId 和最近会话继续排查' : '优先查看失败归档与契约快照',
  description: '追踪台不再承担导航中心职能，只保留追踪、归档、异常观测三者之间的强相关联动。',
  actionLabel: pageMode.value === 'trace' ? '继续消息追踪' : '查看失败归档',
  actionTo: '/message-trace'
}));

const traceSignalMetrics = computed(() => opsOverviewMetrics.value);
```

- [ ] **Step 4: 运行测试确认追踪台改造通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/MessageTraceView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交链路追踪台改造**

```bash
git add spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "feat: refresh message trace view as diagnostic trace station"
```

### Task 7: 只在 `/system-log` 模式下把 AuditLogView 刷成异常观测台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: 写失败测试，锁定系统模式有 Hero、业务模式没有 Hero**

创建 `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`：

```ts
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AuditLogView from '@/views/AuditLogView.vue';

const mockRoute = { path: '/system-log', query: {} as Record<string, unknown> };
const mockRouter = { push: vi.fn(), replace: vi.fn() };

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

describe('AuditLogView', () => {
  beforeEach(() => {
    mockRoute.path = '/system-log';
  });

  it('renders a diagnostic hero in system mode', () => {
    const wrapper = mount(AuditLogView, {
      global: {
        directives: {
          loading: () => undefined,
          permission: () => undefined
        },
        stubs: {
          StandardWorkbenchPanel: defineComponent({
            name: 'StandardWorkbenchPanel',
            props: ['title', 'description'],
            template: '<section><h2>{{ title }}</h2><p>{{ description }}</p><slot name="filters" /><slot name="notices" /><slot name="toolbar" /><slot /></section>'
          }),
          IotAccessWorkbenchHero: defineComponent({
            name: 'IotAccessWorkbenchHero',
            props: ['title', 'judgement'],
            template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
          }),
          IotAccessSignalDeck: true,
          StandardListFilterHeader: true,
          StandardAppliedFiltersBar: true,
          StandardTableToolbar: true,
          StandardTableTextColumn: true,
          StandardRowActions: true,
          StandardActionLink: true,
          StandardPagination: true,
          AuditLogDetailDrawer: true
        }
      }
    });

    expect(wrapper.text()).toContain('异常观测台');
    expect(wrapper.text()).toContain('优先聚焦 system_error');
  });
});
```

追加一个业务模式断言：

```ts
it('keeps audit-center mode without the diagnostic hero', () => {
  mockRoute.path = '/audit-log';
  const wrapper = mount(AuditLogView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      stubs: {
        StandardWorkbenchPanel: defineComponent({
          name: 'StandardWorkbenchPanel',
          props: ['title', 'description'],
          template: '<section><h2>{{ title }}</h2><p>{{ description }}</p><slot name="filters" /><slot name="notices" /><slot name="toolbar" /><slot /></section>'
        }),
        IotAccessWorkbenchHero: defineComponent({
          name: 'IotAccessWorkbenchHero',
          props: ['title', 'judgement'],
          template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
        }),
        IotAccessSignalDeck: true,
        StandardListFilterHeader: true,
        StandardAppliedFiltersBar: true,
        StandardTableToolbar: true,
        StandardTableTextColumn: true,
        StandardRowActions: true,
        StandardActionLink: true,
        StandardPagination: true,
        AuditLogDetailDrawer: true
      }
    }
  });
  expect(wrapper.text()).toContain('审计中心');
  expect(wrapper.find('.iot-access-hero-stub').exists()).toBe(false);
});
```

- [ ] **Step 2: 运行测试确认系统模式尚未有新 Hero**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 只对系统异常模式接入诊断 Hero 和信号板**

在 `spring-boot-iot-ui/src/views/AuditLogView.vue` 顶部加入：

```vue
<template>
  <div class="page-stack audit-log-view">
    <template v-if="isSystemMode">
      <IotAccessWorkbenchHero
        eyebrow="接入智维 / 诊断实验"
        title="异常观测台"
        judgement="优先聚焦 system_error，再决定跳去链路追踪还是失败归档。"
        description="这里回答当前异常集中在哪个模块、设备或通道，以及最值得优先筛查的异常是什么。"
        :tags="systemHeroTags"
        :actions="systemHeroActions"
        :summary-items="systemHeroSummary"
      />

      <IotAccessSignalDeck
        :lead="systemLeadSignal"
        :metrics="systemSignalMetrics"
      />
    </template>

    <StandardWorkbenchPanel
      :title="pageTitle"
      :description="pageDescription"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
      show-toolbar
      show-pagination
    >
```

在脚本中新增：

```ts
const systemHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'C 诊断实验室型' },
  { label: '主战区', value: '异常台账' }
]);

const systemHeroActions = computed(() => [
  { label: '跳转链路追踪台', to: '/message-trace', variant: 'primary' as const },
  { label: '查看失败归档', to: '/message-trace?mode=access-error', variant: 'secondary' as const }
]);

const systemHeroSummary = computed(() => [
  { label: '异常总量', value: String(systemStats.total) },
  { label: '今日异常', value: String(systemStats.todayCount) },
  { label: '关联链路', value: String(systemStats.distinctTraceCount) }
]);

const systemLeadSignal = computed(() => ({
  eyebrow: '优先筛查',
  title: '优先沿 TraceId、设备编码和异常模块快速聚焦',
  description: '异常观测台不再重复做能力墙，只保留和追踪、失败归档强关联的两个出口。',
  actionLabel: '继续异常观测',
  actionTo: '/system-log'
}));

const systemSignalMetrics = computed(() => [
  { label: '异常总量', value: String(systemStats.total) },
  { label: '今日异常', value: String(systemStats.todayCount) },
  { label: '关联链路', value: String(systemStats.distinctTraceCount) }
]);
```

- [ ] **Step 4: 运行测试确认两种模式都正确**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/AuditLogView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交异常观测台模式改造**

```bash
git add spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts
git commit -m "feat: refresh system log mode as anomaly observatory"
```

### Task 8: 把数据校验台强化为结果导向的校验工作台

**Files:**
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- Create: `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`

- [ ] **Step 1: 先写失败测试锁定校验 Hero 与四段式结果区**

创建 `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`：

```ts
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import FilePayloadDebugView from '@/views/FilePayloadDebugView.vue';

vi.mock('@/api/iot', () => ({
  getDeviceFileSnapshots: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getDeviceFirmwareAggregates: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
}));

vi.mock('@/stores/activity', () => ({
  recordActivity: vi.fn()
}));

describe('FilePayloadDebugView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders a validation hero and keeps the four-stage validation layout', () => {
    const wrapper = mount(FilePayloadDebugView, {
      global: {
        stubs: {
          StandardWorkbenchPanel: defineComponent({
            name: 'StandardWorkbenchPanel',
            props: ['title', 'description'],
            template: '<section><h2>{{ title }}</h2><p>{{ description }}</p><slot name="filters" /><slot name="inline-state" /><slot /></section>'
          }),
          IotAccessWorkbenchHero: defineComponent({
            name: 'IotAccessWorkbenchHero',
            props: ['title', 'judgement'],
            template: '<section class="iot-access-hero-stub"><h1>{{ title }}</h1><p>{{ judgement }}</p></section>'
          }),
          IotAccessSignalDeck: defineComponent({
            name: 'IotAccessSignalDeck',
            template: '<section class="iot-access-signal-deck-stub">校验摘要层</section>'
          }),
          StandardListFilterHeader: true,
          StandardInlineState: true,
          StandardInfoGrid: true,
          PanelCard: true,
          EmptyState: true,
          ResponsePanel: true
        }
      }
    });

    expect(wrapper.text()).toContain('数据校验台');
    expect(wrapper.text()).toContain('先确定设备，再核对文件快照和固件聚合。');
    expect(wrapper.text()).toContain('文件快照校验');
    expect(wrapper.text()).toContain('固件聚合校验');
  });
});
```

- [ ] **Step 2: 运行测试确认校验台还没有新首屏**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected: FAIL。

- [ ] **Step 3: 增加校验 Hero，并把概况卡纳入统一信号板**

把 `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue` 根模板改成：

```vue
<template>
  <div class="page-stack file-payload-debug-view">
    <IotAccessWorkbenchHero
      eyebrow="接入智维 / 诊断实验"
      title="数据校验台"
      judgement="先确定设备，再核对文件快照、固件聚合和原始响应。"
      description="这里回答文件快照与固件聚合是否完整、MD5 与分包是否正常，以及当前校验结论是否成立。"
      :tags="validationHeroTags"
      :actions="validationHeroActions"
      :summary-items="validationHeroSummary"
    />

    <IotAccessSignalDeck
      :lead="validationLeadSignal"
      :metrics="validationSignalMetrics"
    />

    <StandardWorkbenchPanel
      title="设备查询与校验结果"
      description="保留单设备工作台特性，结果区继续按概况、文件快照、固件聚合、原始响应四段式呈现。"
      show-filters
      :show-inline-state="showInlineState"
    >
      <template #filters>
        <StandardListFilterHeader :model="{ deviceCode }">
          <template #primary>
            <el-form-item>
              <el-input
                id="file-debug-device-code"
                v-model="deviceCode"
                clearable
                placeholder="设备编码，例如 demo-device-01"
                prefix-icon="Search"
                @keyup.enter="refreshAll"
              />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" :loading="isLoading" :disabled="!normalizedDeviceCode" @click="refreshAll">
              {{ isLoading ? '加载中...' : '刷新数据' }}
            </StandardButton>
            <StandardButton action="reset" :disabled="isLoading" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>
      <template #inline-state>
        <StandardInlineState :message="inlineStateMessage" :tone="inlineStateTone" />
      </template>

      <div class="page-stack">
        <section class="two-column-grid file-payload-debug-view__results">
          <PanelCard eyebrow="文件快照 C.3" title="文件快照校验" description="按时间线核对最近一次 C.3 文件消息是否完整落地。" />
          <PanelCard eyebrow="固件聚合 C.4" title="固件聚合校验" description="按分包进度、完成状态和 MD5 校验结果查看当前聚合情况。" />
        </section>
        <section class="two-column-grid">
          <ResponsePanel eyebrow="文件快照响应" title="文件快照原始响应" :body="fileSnapshots" />
          <ResponsePanel eyebrow="固件聚合响应" title="固件聚合原始响应" :body="firmwareAggregates" />
        </section>
      </div>
    </StandardWorkbenchPanel>
  </div>
</template>
```

执行这一步时，删除原来的“文件消息完整性概况”独立 `PanelCard`，因为四个概况字段已经被 `IotAccessSignalDeck` 顶部摘要层接管；下面两段结果区和原始响应区继续保留原来的数据绑定。

在脚本里新增：

```ts
const validationHeroTags = computed(() => [
  { label: '工作母版', value: 'A 指挥甲板型' },
  { label: '页面倾向', value: 'C 诊断实验室型' },
  { label: '单设备模式', value: normalizedDeviceCode.value || '--' }
]);

const validationHeroActions = computed(() => [
  { label: '继续数据校验', to: '/file-debug', variant: 'primary' as const },
  { label: '打开链路追踪台', to: '/message-trace', variant: 'secondary' as const }
]);

const validationHeroSummary = computed(() => [
  { label: '文件快照', value: String(fileSnapshots.value.length) },
  { label: '固件聚合', value: String(firmwareAggregates.value.length) },
  { label: '最近抓取', value: formatDateTime(lastFetchTime.value) }
]);

const validationLeadSignal = computed(() => ({
  eyebrow: '校验建议',
  title: '先验证最近一次抓取，再判断快照和聚合是否完整',
  description: '数据校验台不扩展成目录页，只保留单设备校验所需的最短路径。',
  actionLabel: '刷新校验数据',
  actionTo: '/file-debug'
}));

const validationSignalMetrics = computed(() => validationSummaryItems.value.map((item) => ({
  label: item.label,
  value: String(item.value ?? '--')
})));
```

- [ ] **Step 4: 运行测试确认数据校验台通过**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/FilePayloadDebugView.test.ts --run
```

Expected: PASS。

- [ ] **Step 5: 提交数据校验台改造**

```bash
git add spring-boot-iot-ui/src/views/FilePayloadDebugView.vue spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts
git commit -m "feat: refresh file validation workbench as diagnostic station"
```

### Task 9: 同步业务文档、前端治理规则并执行统一验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: 先在文档里补齐“中枢台 / 资产治理 / 诊断实验”口径**

在 `docs/02-业务功能与流程说明.md` 的 `5.1 已落地的一级工作台` 与接入智维相关段落中补充：

```md
- `接入智维总览` 当前定位为“中枢台”，负责跨模块判断、最近动作恢复、推荐处理顺序和 6 个专台入口。
- `产品定义中心`、`设备资产中心` 当前定位为“资产治理专台”，主战区仍然是台账，不再重复首页入口墙。
- `链路验证中心`、`异常观测台`、`链路追踪台`、`数据校验台` 当前定位为“诊断实验专台”，负责本域判断、结果复盘和强相关联动。
```

在 `docs/06-前端开发与CSS规范.md` 的工作台 / 列表 / 接入智维相关规则中补充：

```md
- `接入智维` 一级模块当前统一采用 `IotAccessWorkbenchHero + IotAccessSignalDeck + 既有 StandardWorkbenchPanel` 语法；总览负责“先去哪”，子页负责“在这里怎么做”。
- `产品定义中心`、`设备资产中心` 的 Hero 必须保持资产治理语气，主战区仍是台账；`链路验证 / 异常观测 / 链路追踪 / 数据校验` 的 Hero 必须保持诊断实验语气，主战区强调结果与下一步动作。
- `/system-log` 的系统异常模式可以接入接入智维诊断语法，但 `/audit-log` 的审计中心模式不得被误改成接入侧专台。
```

在 `docs/15-前端优化与治理计划.md` 补充治理规则：

```md
29. `接入智维总览` 只保留跨模块判断、最近使用、推荐处理顺序和全部能力；子页禁止再复制“能力墙”或“模块目录”。
30. `IotAccessWorkbenchHero` 与 `IotAccessSignalDeck` 只服务接入智维母版语言，不得扩展成第二套全站概览系统；其他工作台若要复用，必须先更新 `06` 与本文件说明边界。
```

在 `docs/08-变更记录与技术债清单.md` 新增一条有效变更：

```md
- 2026-03-27：`接入智维总览`、`产品定义中心`、`设备资产中心`、`链路验证中心`、`异常观测台`、`链路追踪台`、`数据校验台` 已统一切换到“中枢台 / 资产治理专台 / 诊断实验专台”母版语言，继续复用现有品牌 token 与共享列表骨架，不新增私有主题。
```

- [ ] **Step 2: 复核 `README.md` 与 `AGENTS.md`，只在高层定位确实变化时补一句**

若现有表述不足以说明新的页面职责边界，则分别追加：

```md
- `接入智维总览` 负责跨模块判断与入口分发，6 个子页负责单域处理；其中产品/设备倾向资产治理，链路/异常/追踪/校验倾向诊断实验。
```

若现有表述已足够，则保持 `README.md` 与 `AGENTS.md` 不改，并在执行记录里注明：

```text
README.md reviewed: no update needed
AGENTS.md reviewed: no update needed
```

- [ ] **Step 3: 运行受影响单测与共享守卫**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/iotAccess/IotAccessWorkbenchHero.test.ts src/__tests__/components/iotAccess/IotAccessSignalDeck.test.ts src/__tests__/views/SectionLandingView.test.ts src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/utils/sectionHomes.test.ts --run
npm run build
npm run component:guard
npm run list:guard
npm run style:guard
```

Expected: 全部通过。

- [ ] **Step 4: 运行仓库级本地质量门禁**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected:

- Maven、前端构建、守卫脚本、文档拓扑检查全部通过。
- 若该命令受共享环境或本机 JDK/Node 条件阻塞，必须在执行记录中明确标记阻塞原因，不得伪报通过。

- [ ] **Step 5: 提交文档与最终验证结果**

```bash
git add docs/02-业务功能与流程说明.md docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: sync iot access workbench refresh guidance"
```

如果 `README.md` 或 `AGENTS.md` 在 Step 2 中确实发生变更，则把它们追加到同一条 `git add` 命令里；如果未改，保持不加入提交。
