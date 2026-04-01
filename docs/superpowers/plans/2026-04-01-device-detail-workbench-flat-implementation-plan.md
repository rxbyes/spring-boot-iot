# 设备详情扁平化 Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/devices` 详情抽屉改造成扁平化双轴台账 workbench，并保持未登记设备复用同一详情语法。

**Architecture:** 新增独立 `DeviceDetailWorkbench` 组件承接详情字段排版、分区和样式；`DeviceWorkbenchView` 保留抽屉容器、补数状态与底部动作。测试先锁定新结构，再做视图接入和文档同步。

**Tech Stack:** Vue 3、TypeScript、Vitest、Element Plus、共享设计 token

---

### Task 1: 锁定设备详情新结构

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/components/device/DeviceDetailWorkbench.test.ts`
- Create: `spring-boot-iot-ui/src/components/device/DeviceDetailWorkbench.vue`

- [ ] **Step 1: 写失败测试**

```ts
it('renders the registered device detail as a flat summary band with paired ledgers', () => {
  const wrapper = mount(DeviceDetailWorkbench, {
    props: {
      device: registeredDevice
    }
  })

  expect(wrapper.get('[data-testid="device-detail-summary-stage"]').text()).toContain('资产状态与接入概况')
  expect(wrapper.get('[data-testid="device-detail-identity-stage"]').text()).toContain('身份与部署台账')
  expect(wrapper.get('[data-testid="device-detail-runtime-stage"]').text()).toContain('运行与认证台账')
  expect(wrapper.get('[data-testid="device-detail-support-pair"]').text()).toContain('关系与建档补充')
  expect(wrapper.text()).not.toContain('资产概览')
  expect(wrapper.text()).not.toContain('资产档案')
  expect(wrapper.text()).not.toContain('拓扑关系')
  expect(wrapper.text()).not.toContain('运维信息')
  expect(wrapper.text()).not.toContain('认证信息')
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test -- DeviceDetailWorkbench`

Expected: FAIL，原因应为 `DeviceDetailWorkbench` 组件不存在或结构不匹配。

- [ ] **Step 3: 写最小实现**

```vue
<DeviceDetailWorkbench :device="detailData" />
```

```vue
<section data-testid="device-detail-summary-stage">...</section>
<section data-testid="device-detail-identity-stage">...</section>
<section data-testid="device-detail-runtime-stage">...</section>
<section data-testid="device-detail-support-pair">...</section>
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test -- DeviceDetailWorkbench`

Expected: PASS

- [ ] **Step 5: 自查样式命名与测试文案**

检查组件类名、`data-testid`、标题命名是否与设计稿一致，避免旧语义混入。

### Task 2: 接入设备页抽屉并移除旧堆叠结构

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`

- [ ] **Step 1: 写失败测试或增强现有断言**

```ts
expect(source).toContain('DeviceDetailWorkbench')
expect(source).not.toContain('<h3>资产概览</h3>')
expect(source).not.toContain('<h3>资产档案</h3>')
```

- [ ] **Step 2: 运行页面测试确认失败**

Run: `npm run test -- DeviceWorkbenchView`

Expected: FAIL，原因应为视图仍保留旧详情模板或未接入新组件。

- [ ] **Step 3: 写最小接入实现**

```vue
<div v-if="detailData" class="device-detail-stack">
  <div v-if="detailRefreshing || detailRefreshErrorMessage" class="device-detail-inline-state">...</div>
  <DeviceDetailWorkbench :device="detailData" />
</div>
```

```ts
import DeviceDetailWorkbench from '@/components/device/DeviceDetailWorkbench.vue'
```

- [ ] **Step 4: 清理旧详情私有样式与无用 helper**

```ts
// 删除仅服务旧详情模板的 computed / helper
const metadataPreview = computed(...)
function maskSecret(...) { ... }
```

- [ ] **Step 5: 运行页面测试确认通过**

Run: `npm run test -- DeviceWorkbenchView`

Expected: PASS

### Task 3: 文档同步与前端门禁验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: 更新规则文档**

```md
- 设备资产中心详情抽屉统一采用“顶部轻摘要带 + 中段双轴台账 + 底部补充区”语法；
  已登记与未登记设备继续共用同一 `StandardDetailDrawer`。
```

- [ ] **Step 2: 记录本轮变更摘要**

```md
- 2026-04-01：`/devices` 详情抽屉已拆为独立 `DeviceDetailWorkbench`，
  已登记设备改为“资产状态与接入概况 + 身份与部署台账 + 运行与认证台账 + 建档补充”。
```

- [ ] **Step 3: 运行前端验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- DeviceDetailWorkbench
npm run test -- DeviceWorkbenchView
npm run build
npm run component:guard
npm run list:guard
```

Expected: 所有命令通过；如失败，先修当前改动引入的问题，再继续。

- [ ] **Step 4: 整理交付说明**

记录受影响文件、验证命令、是否有未完成项，以及本轮同步的文档清单。
