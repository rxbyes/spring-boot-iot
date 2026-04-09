# 设备资产中心详情抽屉扁平台账治理式改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/devices` 详情抽屉改造成“轻头部 + 双概览板 + 宽窄混排主账本 + 补充区”的扁平台账治理式工作台，并保持未登记设备复用同一套详情语法。

**Architecture:** 继续由 `DeviceWorkbenchView.vue` 承担 `StandardDetailDrawer` 容器、补数状态和底部动作，所有详情结构和样式都留在 `DeviceDetailWorkbench.vue`。实现顺序遵循 TDD：先锁定已登记与未登记设备的新结构测试，再重写组件模板/计算属性/样式，最后收口视图文案、同步文档并跑前端门禁。

**Tech Stack:** Vue 3、TypeScript、Vitest、Element Plus、共享设计 token、Markdown 文档

---

## File Map

- `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`
  设备详情主组件。负责轻头部、摘要带、双概览板、宽窄混排账本和补充区。
- `spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts`
  设备详情组件测试。负责锁定已登记/未登记设备的新语法和旧文案不回流。
- `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
  设备资产列表页和详情抽屉容器。只做 `StandardDetailDrawer` 容器、副标题和 tags 等轻联动。
- `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
  视图级测试。负责确认详情仍委托给 `DeviceDetailWorkbench`，并检查抽屉副标题已切换到新语气。
- `docs/06-前端开发与CSS规范.md`
  更新“设备详情不要再用三列等宽小卡墙承载长字段”的长期规则。
- `docs/15-前端优化与治理计划.md`
  记录本轮详情页治理结论和防回退规则。
- `docs/08-变更记录与技术债清单.md`
  记录 2026-04-01 设备详情扁平台账治理式改造完成。

### Task 1: 锁定已登记设备的新详情结构

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`

- [ ] **Step 1: 写失败测试，锁定已登记设备的扁平台账工作台结构**

```ts
it('renders the registered device as a flat governance workbench with hero, overview boards and mixed ledgers', () => {
  const wrapper = mount(DeviceDetailWorkbench, {
    props: {
      device: registeredDevice
    }
  })

  expect(wrapper.get('[data-testid="device-detail-hero"]').text()).toContain('东侧位移计 01')
  expect(wrapper.get('[data-testid="device-detail-hero"]').text()).toContain('资产信息完整，可继续核对部署、运行与认证台账。')
  expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('资产状态与接入概况')
  expect(wrapper.get('[data-testid="device-detail-overview-pair"]').text()).toContain('资产概览')
  expect(wrapper.get('[data-testid="device-detail-overview-pair"]').text()).toContain('运行概览')
  expect(wrapper.get('[data-testid="device-detail-identity-stage"]').text()).toContain('身份与部署台账')
  expect(wrapper.get('[data-testid="device-detail-runtime-stage"]').text()).toContain('运行与认证台账')
  expect(wrapper.findAll('.device-detail-workbench__ledger-cell--wide')).toHaveLength(7)
  expect(wrapper.text()).not.toContain('把设备身份、产品归属和部署上下文压回同一层台账')
  expect(wrapper.text()).not.toContain('把运行节奏和接入凭据放在同一侧')
})
```

- [ ] **Step 2: 运行单测，确认它先因为新结构不存在而失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/device/DeviceDetailWorkbench.test.ts -t "renders the registered device as a flat governance workbench with hero, overview boards and mixed ledgers"
```

Expected: FAIL，原因应为 `data-testid="device-detail-hero"` / `device-detail-overview-pair` 或 `.device-detail-workbench__ledger-cell--wide` 数量与当前实现不匹配。

- [ ] **Step 3: 写最小实现，把已登记设备切到“轻头部 + 双概览板 + 宽窄混排账本”**

```vue
<section class="device-detail-workbench__hero" data-testid="device-detail-hero">
  <div>
    <h3>{{ heroTitle }}</h3>
    <p>{{ heroMessage }}</p>
  </div>
  <div class="device-detail-workbench__hero-tags">
    <span
      v-for="tag in heroTags"
      :key="tag.key"
      class="device-detail-workbench__hero-tag"
      :class="`device-detail-workbench__hero-tag--${tag.tone}`"
    >
      {{ tag.label }}
    </span>
  </div>
</section>

<div class="device-detail-workbench__overview-pair" data-testid="device-detail-overview-pair">
  <section class="device-detail-workbench__stage device-detail-workbench__stage--overview">
    <div class="device-detail-workbench__stage-header">
      <div>
        <h3>资产概览</h3>
        <p>先确认设备身份、产品归属与部署位置。</p>
      </div>
    </div>
    <div class="device-detail-workbench__ledger-grid device-detail-workbench__ledger-grid--mixed">
      <article
        v-for="cell in assetOverviewCells"
        :key="cell.key"
        class="device-detail-workbench__ledger-cell"
        :class="{ 'device-detail-workbench__ledger-cell--wide': cell.wide }"
      >
        <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
        <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
      </article>
    </div>
  </section>

  <section class="device-detail-workbench__stage device-detail-workbench__stage--overview">
    <div class="device-detail-workbench__stage-header">
      <div>
        <h3>运行概览</h3>
        <p>先确认最近在线、离线与上报节奏。</p>
      </div>
    </div>
    <div class="device-detail-workbench__ledger-grid device-detail-workbench__ledger-grid--mixed">
      <article
        v-for="cell in runtimeOverviewCells"
        :key="cell.key"
        class="device-detail-workbench__ledger-cell"
        :class="{ 'device-detail-workbench__ledger-cell--wide': cell.wide }"
      >
        <span class="device-detail-workbench__cell-label">{{ cell.label }}</span>
        <strong class="device-detail-workbench__cell-value">{{ cell.value }}</strong>
      </article>
    </div>
  </section>
</div>
```

```ts
const heroTitle = computed(() => device.value.deviceName || device.value.deviceCode || '设备详情')
const heroMessage = computed(() =>
  isRegistered.value
    ? '资产信息完整，可继续核对部署、运行与认证台账。'
    : '当前仍是未登记上报，优先确认失败阶段与来源记录。'
)
const heroTags = computed(() =>
  isRegistered.value
    ? [
        { key: 'registration', label: getRegistrationStatusText(device.value.registrationStatus), tone: 'success' },
        { key: 'online', label: getOnlineStatusText(device.value.onlineStatus), tone: device.value.onlineStatus === 1 ? 'success' : 'neutral' },
        { key: 'activate', label: getActivateStatusText(device.value.activateStatus), tone: device.value.activateStatus === 1 ? 'success' : 'warning' },
        { key: 'status', label: getDeviceStatusText(device.value.deviceStatus), tone: device.value.deviceStatus === 1 ? 'success' : 'warning' }
      ]
    : [
        { key: 'registration', label: getRegistrationStatusText(device.value.registrationStatus), tone: 'warning' },
        { key: 'source', label: getSourceTypeText(device.value.assetSourceType), tone: 'neutral' }
      ]
)
```

- [ ] **Step 4: 重新运行已登记设备测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/device/DeviceDetailWorkbench.test.ts -t "renders the registered device as a flat governance workbench with hero, overview boards and mixed ledgers"
```

Expected: PASS

- [ ] **Step 5: 提交这一段最小可工作的改动**

```bash
git add spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts
git commit -m "feat: flatten registered device detail workbench"
```

### Task 2: 锁定未登记设备和混排样式的防回退

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`

- [ ] **Step 1: 写失败测试，锁定未登记设备仍复用同一扁平语法**

```ts
it('renders the unregistered device with the same flat syntax and wide source fields', () => {
  const wrapper = mount(DeviceDetailWorkbench, {
    props: {
      device: unregisteredDevice
    }
  })

  expect(wrapper.get('[data-testid="device-detail-hero"]').text()).toContain('TEMP-UNREG-009')
  expect(wrapper.get('[data-testid="device-detail-hero"]').text()).toContain('当前仍是未登记上报，优先确认失败阶段与来源记录。')
  expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('资产状态与接入概况')
  expect(wrapper.get('[data-testid="device-detail-source-stage"]').text()).toContain('来源档案与失败摘要')
  expect(wrapper.get('[data-testid="device-detail-payload-stage"]').text()).toContain('最近载荷')
  expect(wrapper.findAll('[data-testid="device-detail-source-stage"] .device-detail-workbench__ledger-cell--wide')).toHaveLength(2)
  expect(wrapper.text()).not.toContain('统一把未登记来源、协议上下文和失败摘要收在一层')
})
```

- [ ] **Step 2: 运行单测，确认未登记设备断言先失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/device/DeviceDetailWorkbench.test.ts -t "renders the unregistered device with the same flat syntax and wide source fields"
```

Expected: FAIL，原因应为未登记设备当前没有轻头部、没有宽字段 `wide` 控制，或者仍保留旧说明文案。

- [ ] **Step 3: 写最小实现，把未登记设备也切到混排账本与统一响应式**

```ts
const sourceCells = computed<DetailCell[]>(() => [
  { key: 'deviceCode', label: '设备编码', value: toDisplayText(device.value.deviceCode) },
  { key: 'productKey', label: '产品标识', value: toDisplayText(device.value.productKey) },
  { key: 'protocolCode', label: '协议编码', value: toDisplayText(device.value.protocolCode) },
  { key: 'sourceRecordId', label: '来源记录', value: toDisplayText(device.value.sourceRecordId) },
  { key: 'lastReportTopic', label: 'Topic', value: toDisplayText(device.value.lastReportTopic), wide: true },
  { key: 'lastErrorMessage', label: '失败摘要', value: toDisplayText(device.value.lastErrorMessage), wide: true }
])
```

```css
.device-detail-workbench__overview-pair,
.device-detail-workbench__ledger-pair,
.device-detail-workbench__support-pair {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.device-detail-workbench__ledger-grid--mixed {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.device-detail-workbench__ledger-cell--wide {
  grid-column: 1 / -1;
}

@media (max-width: 960px) {
  .device-detail-workbench__overview-pair,
  .device-detail-workbench__ledger-pair,
  .device-detail-workbench__support-pair,
  .device-detail-workbench__ledger-grid--mixed {
    grid-template-columns: minmax(0, 1fr);
  }

  .device-detail-workbench__ledger-cell--wide {
    grid-column: auto;
  }
}
```

- [ ] **Step 4: 重新运行未登记设备测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/device/DeviceDetailWorkbench.test.ts -t "renders the unregistered device with the same flat syntax and wide source fields"
```

Expected: PASS

- [ ] **Step 5: 提交未登记设备与响应式收口**

```bash
git add spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts
git commit -m "feat: align unregistered device detail layout"
```

### Task 3: 收口详情抽屉容器文案与视图测试

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 写失败测试，锁定抽屉壳层只保留轻量副标题**

```ts
it('keeps the drawer shell thin and uses the new device detail subtitle copy', () => {
  const source = readFileSync(resolve(import.meta.dirname, '../../views/DeviceWorkbenchView.vue'), 'utf8')

  expect(source).toContain('<DeviceDetailWorkbench :device="detailData" />')
  expect(source).toContain("统一查看资产判断、部署台账、运行台账与建档补充。")
  expect(source).toContain("当前设备仍未登记，详情按失败来源和最近载荷组织。")
  expect(source).not.toContain("统一查看设备资产概况、身份部署、运行状态、接入凭据与建档补充。")
  expect(source).not.toContain("当前设备尚未登记，详情来自最近一次失败归档，用于补档和排障。")
})
```

- [ ] **Step 2: 运行视图测试，确认文案断言先失败**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts -t "keeps the drawer shell thin and uses the new device detail subtitle copy"
```

Expected: FAIL，原因应为 `detailSubtitle` 仍是旧描述。

- [ ] **Step 3: 更新 `detailSubtitle`，保持抽屉壳层只做轻提示**

```ts
const detailSubtitle = computed(() =>
  detailIsRegistered.value
    ? '统一查看资产判断、部署台账、运行台账与建档补充。'
    : '当前设备仍未登记，详情按失败来源和最近载荷组织。'
)
```

- [ ] **Step 4: 重新运行视图测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts -t "keeps the drawer shell thin and uses the new device detail subtitle copy"
```

Expected: PASS

- [ ] **Step 5: 提交详情抽屉容器文案收口**

```bash
git add spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts
git commit -m "refactor: streamline device detail drawer copy"
```

### Task 4: 同步文档并跑前端门禁

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: 更新前端规范文档，明确设备详情的新长期规则**

```md
- `/devices` 的设备详情当前固定采用“轻头部 + 资产状态与接入概况 + 首屏双概览板 + 宽窄混排主账本 + 底部补充区”的扁平台账治理式；不得再回流 `3` 列等宽小卡墙去承接设备编码、部署位置、最近在线/离线/上报这类长字段。
```

- [ ] **Step 2: 更新治理计划文档，记录本轮防回退结论**

```md
- 设备资产中心详情抽屉若继续精修，优先通过概览板层级、宽字段跨列和主账本混排提升可读性；不要再靠缩字体、缩 padding 或继续堆等宽小卡来解决拥挤问题。
```

- [ ] **Step 3: 更新变更记录，落一条 2026-04-01 变更摘要**

```md
- 2026-04-01：`/devices` 详情抽屉已升级为“扁平台账治理式”工作台，新增轻头部和双概览板，已登记/未登记设备统一改为宽窄混排账本，不再使用 `3` 列等宽小卡墙承载长字段。
```

- [ ] **Step 4: 跑完整前端验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/device/DeviceDetailWorkbench.test.ts
npm run test -- src/__tests__/views/DeviceWorkbenchView.test.ts
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected: 所有命令通过，输出中不应出现本轮引入的测试失败、guard 违规或构建错误。

- [ ] **Step 5: 复核 README/AGENTS 后提交文档与验证结果**

```bash
git add docs/06-前端开发与CSS规范.md docs/15-前端优化与治理计划.md docs/08-变更记录与技术债清单.md
git commit -m "docs: record device detail layout governance"
```

补充检查：

- `README.md` 与 `AGENTS.md` 仅在实现后确认“无需改动”，不额外创建平行文档。
