# Product Definition Business Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/products` 中的详情、物模型、查看设备、编辑四个入口统一为同一套经营简报式工作台抽屉，并保持既有接口、列表缓存和前端治理规范不回退。

**Architecture:** `ProductWorkbenchView.vue` 继续负责列表、筛选、缓存与对象级提交流程，但四个对象深度入口统一下沉到新的 `ProductBusinessWorkbenchDrawer.vue`。抽屉内部按“经营头部 + 真实视图切换 + 主舞台内容”组织，并把物模型治理、设备台账、编辑治理分别抽成可嵌入工作区组件，避免继续依赖四套独立抽屉壳。原 `ProductDetailWorkbench.vue` 直接复用于 `经营总览` 视图。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, scoped CSS, existing `StandardDetailDrawer` / `StandardFormDrawer` / `StandardDrawerFooter`

---

## File Structure

### Existing files to modify

- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- `spring-boot-iot-ui/src/components/DeviceListDrawer.vue`
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/DeviceListDrawer.test.ts`
- `docs/06-前端开发与CSS规范.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/15-前端优化与治理计划.md`

### New files to create

- `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`

### Review-only sync targets

- `README.md`
- `AGENTS.md`

## Implementation Notes

- 当前用户已明确要求直接实施，因此本计划保存后继续在当前会话内执行，不再等待额外确认。
- `新增产品` 继续保留现有 `StandardFormDrawer`；统一经营工作台只覆盖已有产品对象的四个深度入口。
- 所有新组件都必须保持 UTF-8 可读，不引入英文 eyebrow。
- `/products` 页面最终不得再直接挂载 `StandardDetailDrawer + StandardFormDrawer(edit) + DeviceListDrawer + ProductModelDesignerDrawer` 四套并行对象入口。
- 旧包装组件若保留，只允许作为兼容包装层复用新的嵌入式 workspace，不得继续维护第二套内层结构。

### Task 1: 为统一经营工作台入口写失败测试

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 先扩展页面测试桩，加入统一工作台组件桩**

在 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 中新增：

```ts
const ProductBusinessWorkbenchDrawerStub = defineComponent({
  name: 'ProductBusinessWorkbenchDrawer',
  props: ['modelValue', 'product', 'activeView'],
  emits: ['update:modelValue', 'saved'],
  template: `
    <section v-if="modelValue" class="product-business-workbench-drawer-stub">
      <h2>{{ product?.productName }}</h2>
      <p data-testid="product-business-workbench-active-view">{{ activeView }}</p>
      <slot />
    </section>
  `
})
```

并在 `mountView()` 的 `stubs` 中加入：

```ts
ProductBusinessWorkbenchDrawer: ProductBusinessWorkbenchDrawerStub
```

- [ ] **Step 2: 写出四个入口共用同一工作台的失败测试**

在同文件追加：

```ts
it('opens the same business workbench drawer with overview as the default detail view', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  ;(wrapper.vm as any).tableData = [
    {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1
    }
  ]
  ;(wrapper.vm as any).viewType = 'card'
  await nextTick()

  await wrapper.get('[data-testid="open-product-detail"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(true)
  expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('overview')
})

it('routes model, devices and edit actions into the same business workbench with different default views', async () => {
  const wrapper = mountView()
  await flushPromises()
  await nextTick()

  const product = {
    id: 1001,
    productKey: 'demo-product',
    productName: '演示产品',
    protocolCode: 'mqtt-json',
    nodeType: 1,
    dataFormat: 'JSON',
    status: 1
  }

  ;(wrapper.vm as any).handleRowAction('model', product)
  await nextTick()
  expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('models')

  ;(wrapper.vm as any).businessWorkbenchVisible = false
  ;(wrapper.vm as any).handleRowAction('devices', product)
  await nextTick()
  expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('devices')

  ;(wrapper.vm as any).businessWorkbenchVisible = false
  ;(wrapper.vm as any).handleRowAction('edit', product)
  await nextTick()
  expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('edit')
})
```

- [ ] **Step 3: 运行页面测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 测试失败，因为页面当前仍然使用独立 `StandardDetailDrawer / DeviceListDrawer / ProductModelDesignerDrawer / StandardFormDrawer(edit)` 状态机。

- [ ] **Step 4: 记录当前红灯原因，不修改生产代码**

预期失败点：

- 无 `ProductBusinessWorkbenchDrawer`
- `detail` / `model` / `devices` / `edit` 仍对应四套独立可见性状态
- 无 `businessWorkbenchVisible` / `businessWorkbenchActiveView`

- [ ] **Step 5: 本任务不提交，直接进入实现**

### Task 2: 新建统一经营工作台抽屉组件

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`

- [ ] **Step 1: 先写抽屉组件失败测试，锁定头部与视图切换**

创建 `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`：

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductBusinessWorkbenchDrawer from '@/components/product/ProductBusinessWorkbenchDrawer.vue'

describe('ProductBusinessWorkbenchDrawer', () => {
  it('renders one business header and keeps the active view in the same drawer', () => {
    const wrapper = mount(ProductBusinessWorkbenchDrawer, {
      props: {
        modelValue: true,
        activeView: 'models',
        product: {
          id: 1001,
          productKey: 'demo-product',
          productName: '演示产品',
          protocolCode: 'mqtt-json',
          nodeType: 1,
          dataFormat: 'JSON',
          status: 1,
          deviceCount: 12,
          onlineDeviceCount: 8,
          thirtyDaysActiveCount: 10
        }
      },
      slots: {
        overview: '<div>overview-slot</div>',
        models: '<div>models-slot</div>',
        devices: '<div>devices-slot</div>',
        edit: '<div>edit-slot</div>'
      }
    })

    expect(wrapper.text()).toContain('演示产品')
    expect(wrapper.text()).toContain('demo-product')
    expect(wrapper.text()).toContain('经营总览')
    expect(wrapper.text()).toContain('物模型治理')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('编辑治理')
    expect(wrapper.text()).toContain('models-slot')
    expect(wrapper.text()).not.toContain('overview-slot')
  })
})
```

- [ ] **Step 2: 运行测试，确认当前红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- 测试失败，因为组件文件尚不存在。

- [ ] **Step 3: 写最小实现，先让头部和视图切换成立**

创建 `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`，至少包含：

```vue
<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-business-workbench"
    size="72rem"
    :title="productTitle"
    :subtitle="drawerSubtitle"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section class="product-business-workbench__hero">
      <div class="product-business-workbench__hero-copy">
        <h3>{{ productTitle }}</h3>
        <p>{{ productKey }}</p>
      </div>
      <div class="product-business-workbench__hero-metrics">
        <article v-for="metric in heroMetrics" :key="metric.key">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </article>
      </div>
    </section>

    <nav class="product-business-workbench__tabs" aria-label="产品经营工作台视图">
      <button
        v-for="view in viewOptions"
        :key="view.key"
        type="button"
        :class="{ 'is-active': activeView === view.key }"
        @click="emit('update:activeView', view.key)"
      >
        {{ view.label }}
      </button>
    </nav>

    <section class="product-business-workbench__stage">
      <slot v-if="activeView === 'overview'" name="overview" />
      <slot v-else-if="activeView === 'models'" name="models" />
      <slot v-else-if="activeView === 'devices'" name="devices" />
      <slot v-else name="edit" />
    </section>
  </StandardDetailDrawer>
</template>
```

- [ ] **Step 4: 重新运行测试，确认转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected:

- 新测试通过。

- [ ] **Step 5: 本任务不提交，继续接线页面**

### Task 3: 在页面内用统一工作台替换四套深度入口状态机

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 以最小改动引入统一状态机**

在 `ProductWorkbenchView.vue` 中新增：

```ts
type ProductWorkbenchViewKey = 'overview' | 'models' | 'devices' | 'edit'

const businessWorkbenchVisible = ref(false)
const businessWorkbenchActiveView = ref<ProductWorkbenchViewKey>('overview')
const businessWorkbenchProduct = ref<Product | null>(null)
```

新增统一打开函数：

```ts
function openProductBusinessWorkbench(row: Product, view: ProductWorkbenchViewKey) {
  businessWorkbenchProduct.value = row
  businessWorkbenchActiveView.value = view
  businessWorkbenchVisible.value = true

  if (view === 'devices') {
    void loadDeviceList(row.productKey)
  }
}
```

- [ ] **Step 2: 把四个入口映射到统一打开函数**

替换以下调用：

```ts
function handleOpenDetail(row: Product) {
  currentProduct.value = row
  void openDetail(row)
  openProductBusinessWorkbench(row, 'overview')
}

function handleEdit(row: Product) {
  currentProduct.value = row
  prepareEditState(row)
  openProductBusinessWorkbench(row, 'edit')
}

function handleOpenDeviceListDrawer(row: Product) {
  currentProduct.value = row
  openProductBusinessWorkbench(row, 'devices')
}

function handleOpenProductModelDesigner(row: Product) {
  currentProduct.value = row
  openProductBusinessWorkbench(row, 'models')
}
```

必要时把原 `handleEdit` 内部的“编辑数据准备”和“显示抽屉”拆成两个函数，避免继续依赖 `formVisible = true`。

- [ ] **Step 3: 在模板里挂上统一工作台，先不删除原壳层**

先在模板底部接入：

```vue
<ProductBusinessWorkbenchDrawer
  v-model="businessWorkbenchVisible"
  v-model:active-view="businessWorkbenchActiveView"
  :product="businessWorkbenchProduct"
>
  <template #overview>
    <ProductDetailWorkbench v-if="detailData || businessWorkbenchProduct" :product="detailData || businessWorkbenchProduct!" />
  </template>
  <template #models>
    <ProductModelDesignerWorkspace :product="businessWorkbenchProduct" />
  </template>
  <template #devices>
    <ProductDeviceListWorkspace
      :product="businessWorkbenchProduct"
      :devices="deviceListData"
      :total-devices="deviceListTotal"
      :online-devices="deviceListOnlineCount"
      :offline-devices="deviceListOfflineCount"
      :loading="devicesLoading"
    />
  </template>
  <template #edit>
    <ProductEditWorkspace ... />
  </template>
</ProductBusinessWorkbenchDrawer>
```

其中 `ProductEditWorkspace` 先直接复用现有表单状态、`formRules`、`handleSubmit`，只把视图壳搬进去。

- [ ] **Step 4: 运行页面测试，确认新的统一入口断言转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 新增统一工作台相关测试通过。
- 旧的“独立详情/设备/物模型抽屉存在”类断言开始需要更新。

- [ ] **Step 5: 删除页面中已失效的四套独立对象入口壳层**

删除或停用：

- `StandardDetailDrawer`（产品详情独立壳）
- `StandardFormDrawer`（仅编辑用；新增产品保留）
- `DeviceListDrawer`
- `ProductModelDesignerDrawer`

保留：

- `StandardFormDrawer`（新增产品）
- 导出列设置弹窗

### Task 4: 抽出物模型、设备、编辑三个可嵌入工作区

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/DeviceListDrawer.vue`
- Modify: related tests

- [ ] **Step 1: 先把设备列表抽成 workspace 并写失败测试**

创建 `ProductDeviceListWorkspace.test.ts`，断言：

- 会渲染“设备运行概览”
- 会渲染四个摘要指标
- 会渲染设备台账表

然后把 `DeviceListDrawer.vue` 中原 `slot` 内部主体抽到 `ProductDeviceListWorkspace.vue`，`DeviceListDrawer.vue` 只保留 `el-drawer` 壳和 header/footer。

- [ ] **Step 2: 抽出物模型治理 workspace 并写失败测试**

创建 `ProductModelDesignerWorkspace.test.ts`，断言：

- 候选模式下存在 `真实证据概览`
- 正式模式下存在类型概览卡和模型卡
- 不依赖外层抽屉也能渲染主体内容

然后把 `ProductModelDesignerDrawer.vue` 改为：

- 外层 `StandardDetailDrawer`
- 内层复用 `ProductModelDesignerWorkspace`
- 表单抽屉继续保留在组件内部，保证正式模型 CRUD 不回退

- [ ] **Step 3: 抽出编辑治理 workspace**

创建 `ProductEditWorkspace.vue`，先接收：

- `model`
- `rules`
- `loading`
- `modeLabel`
- `onSubmit`

模板中先保留：

- 编辑前提醒摘要区
- 复用现有产品表单字段
- 底部固定 `取消 / 保存`

如表单抽离过重，可先在组件中通过 props 传入 `formData` 与 `formRef`，第二轮再抽共享字段区。

- [ ] **Step 4: 运行受影响组件测试**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductDeviceListWorkspace.test.ts \
  src/__tests__/components/DeviceListDrawer.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 新旧组件测试同时通过。

- [ ] **Step 5: 清理遗留状态与样式**

从 `ProductWorkbenchView.vue` 中删除失效状态：

- `detailVisible`
- `productModelDesignerVisible`
- `deviceListDrawerVisible`
- 仅用于旧编辑抽屉显隐的状态

同时删除失效样式类，确保页面最终只剩统一工作台壳层样式和新增产品抽屉样式。

### Task 5: 同步文档并完成验证

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: 回写前端规范**

在 `docs/06-前端开发与CSS规范.md` 补充：

- `/products` 详情、物模型、关联设备、编辑已统一收口为同一 `产品经营工作台抽屉`
- 同一路由内的多深度对象视图优先使用“统一经营头部 + 真实业务视图切换 + 主舞台内容”语法

在 `docs/15-前端优化与治理计划.md` 补充：

- `产品定义中心` 当前固定使用统一经营工作台承接四个产品深度入口
- 不得再把物模型、查看设备、编辑回退成独立并行抽屉

在 `docs/08-变更记录与技术债清单.md` 记录：

- 2026-03-30 `/products` 已完成统一经营工作台重构

- [ ] **Step 2: 检查 `README.md` 与 `AGENTS.md` 是否需要同步**

Run:

```bash
git diff -- README.md AGENTS.md
```

Expected:

- 若无输出，则在最终说明中明确“已检查，无需修改”。

- [ ] **Step 3: 运行最终验证**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductDetailWorkbench.test.ts \
  src/__tests__/components/product/ProductDeviceListWorkspace.test.ts \
  src/__tests__/components/DeviceListDrawer.test.ts --run
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected:

- 相关测试全部通过
- `component:guard`、`list:guard`、`style:guard` 通过
- `npm run build` 通过

- [ ] **Step 4: 交付前复核无并行抽屉回流**

Run:

```bash
cd spring-boot-iot-ui
Select-String -Path src/views/ProductWorkbenchView.vue -Pattern "DeviceListDrawer|ProductModelDesignerDrawer|StandardDetailDrawer" 
```

Expected:

- 页面内只保留统一经营工作台所需的单一对象抽屉壳，不再并行挂载设备列表和物模型独立抽屉。
