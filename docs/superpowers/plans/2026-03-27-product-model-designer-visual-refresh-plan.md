# Product Model Designer Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/products` 内把产品物模型设计器升级为更完整的大工作台抽屉，强化候选提炼首屏视觉、统一正式模型模式结构，并保持与主系统品牌橙和蓝灰体系一致。

**Architecture:** 继续复用现有 `ProductModelDesignerDrawer.vue` 单组件承接抽屉主体，不新增一级路由，也不改动后端业务语义。本轮实现采用“测试先行 + 模板重构 + 样式重构 + 文档回写”的顺序，在 `codex/dev` 当前工作区直接开发，不切换工作树或分支。

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, Vite, CSS variables, StandardDetailDrawer

---

## 文件结构

- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

说明：

- `ProductModelDesignerDrawer.vue` 继续作为唯一视觉重构承载文件，本轮不拆新共享组件。
- `ProductModelDesignerDrawer.test.ts` 负责锁定新布局骨架、双模式可读性和既有兼容行为。
- `ProductWorkbenchView.test.ts` 只做最小入口回归，确保抽屉升级后 `/products` 入口不退化。
- `docs/02` 回写页面业务结构，`docs/08` 回写变更记录，`docs/15` 回写新的前端治理规则。

## Task 1: 用失败测试锁定新的设计器骨架和兼容行为

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 在设计器组件单测中添加“候选提炼舞台布局”失败测试**

在 `spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts` 追加以下测试：

```ts
it('renders candidate mode as a staged workspace with hero, overview and confirm rail', async () => {
  const wrapper = mountDrawer()
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.product-model-designer__hero-stage').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__hero-product').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__overview').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__candidate-workspace').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__candidate-rail').exists()).toBe(true)
  expect(wrapper.text()).toContain('基于真实上报提炼产品契约')
  expect(wrapper.text()).toContain('真实证据概览')
})
```

- [ ] **Step 2: 在设计器组件单测中添加“正式模型统一资产条”失败测试**

继续在同一文件追加测试，验证正式模型模式下的概览条和统一卡片语义：

```ts
it('renders formal mode with type overview cards and unified model cards', async () => {
  mockListProductModels.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: [
      {
        id: 2001,
        modelType: 'property',
        identifier: 'temperature',
        modelName: '温度',
        dataType: 'decimal',
        specsJson: '{"unit":"℃"}',
        sortNo: 10,
        requiredFlag: 1,
        description: '设备温度正式契约'
      }
    ]
  })

  const wrapper = mountDrawer()
  await flushPromises()
  await nextTick()

  await findButtonByText(wrapper, '正式模型')!.trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.find('.product-model-designer__formal-overview').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__formal-stage').exists()).toBe(true)
  expect(wrapper.find('.product-model-designer__card-surface').exists()).toBe(true)
  expect(wrapper.text()).toContain('统一维护产品正式物模型')
  expect(wrapper.text()).toContain('温度')
  expect(wrapper.text()).toContain('设备温度正式契约')
})
```

- [ ] **Step 3: 在产品工作台视图测试里锁定设计器仍由 `/products` 承接**

修改 `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` 中的 `ProductModelDesignerDrawerStub`，补充新的大工作台标题元素，保持入口测试仍然能验证抽屉内容打开：

```ts
const ProductModelDesignerDrawerStub = defineComponent({
  name: 'ProductModelDesignerDrawer',
  props: ['modelValue', 'product'],
  template: `
    <section v-if="modelValue" class="product-model-designer-drawer-stub">
      <h2>基于真实上报提炼产品契约</h2>
      <h3>属性模型</h3>
      <h3>事件模型</h3>
      <h3>服务模型</h3>
      <p>暂无物模型</p>
    </section>
  `
})
```

- [ ] **Step 4: 运行定向前端测试，确认当前实现红灯**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ProductModelDesignerDrawer.test.ts --run
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- `ProductModelDesignerDrawer.test.ts` 新增布局测试失败
- 失败原因是新类名、主标题或新结构尚未实现
- `ProductWorkbenchView.test.ts` 如因 stub 文案差异失败，也应明确体现入口断言与新设计骨架不一致

- [ ] **Step 5: 提交测试脚手架**

```bash
git add spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "test: lock product model designer visual refresh layout"
```

## Task 2: 重构设计器模板，建立双模式舞台结构

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`

- [ ] **Step 1: 先把抽屉显式加宽，并给头部加上动态舞台标题**

在 `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue` 的 `StandardDetailDrawer` 调用处增加 `size`，并替换 hero 顶部文案：

```vue
<StandardDetailDrawer
  :model-value="modelValue"
  class="product-model-designer"
  eyebrow="产品物模型设计器"
  :title="drawerTitle"
  :subtitle="drawerSubtitle"
  size="72rem"
  :loading="loading"
  loading-text="正在加载产品物模型..."
  :error-message="errorMessage"
  :empty="!product"
  empty-text="请先选择产品后再打开物模型设计器"
  @update:modelValue="emit('update:modelValue', $event)"
>
```

在 `<script setup lang="ts">` 中新增舞台文案计算：

```ts
const designerStageTitle = computed(() =>
  designerMode.value === 'candidates' ? '基于真实上报提炼产品契约' : '统一维护产品正式物模型'
)

const designerStageHint = computed(() =>
  designerMode.value === 'candidates' ? '仅提炼真实证据' : '维护已确认契约'
)
```

- [ ] **Step 2: 把当前 hero 区替换为“舞台头部层”**

将原有 `.product-model-designer__hero` 模板替换为以下结构：

```vue
<section class="detail-panel detail-panel--hero product-model-designer__hero-stage">
  <div class="product-model-designer__hero-copy">
    <p class="product-model-designer__hero-kicker">物模型目录</p>
    <h3>{{ designerStageTitle }}</h3>
    <p class="product-model-designer__hero-description">
      按属性、事件、服务三类维护产品契约，继续复用现有产品定义中心，不新增一级路由。
    </p>
    <div class="product-model-designer__hero-product">
      <span>{{ product?.productName || '--' }}</span>
      <span>{{ product?.productKey || '--' }}</span>
      <span>{{ product?.protocolCode || '--' }}</span>
      <span>{{ product?.nodeType === 1 ? '直连设备' : '网关设备' }}</span>
    </div>
  </div>

  <div class="product-model-designer__hero-actions">
    <div class="product-model-designer__mode-switcher" role="tablist" aria-label="设计器模式">
      <button
        type="button"
        class="product-model-designer__mode-chip"
        :class="{ 'product-model-designer__mode-chip--active': designerMode === 'candidates' }"
        @click="designerMode = 'candidates'"
      >
        候选提炼
      </button>
      <button
        type="button"
        class="product-model-designer__mode-chip"
        :class="{ 'product-model-designer__mode-chip--active': designerMode === 'formal' }"
        @click="designerMode = 'formal'"
      >
        正式模型
      </button>
    </div>
    <p class="product-model-designer__hero-action-hint">{{ designerStageHint }}</p>
    <StandardButton
      v-if="designerMode === 'candidates'"
      action="refresh"
      :disabled="!product"
      @click="handleReloadDesigner"
    >
      重新提炼
    </StandardButton>
    <StandardButton
      v-else
      action="add"
      :disabled="!product"
      @click="handleOpenCreateForm"
    >
      新增{{ activeTypeLabel }}
    </StandardButton>
  </div>
</section>
```

- [ ] **Step 3: 在候选提炼模式中建立“1 主 3 辅总览 + 三段工作区”**

把当前候选提炼模板中的 `.product-model-designer__summary` 和 `.product-model-designer__candidate-layout` 替换为新骨架：

```vue
<section v-if="designerMode === 'candidates'" class="detail-panel product-model-designer__section">
  <div class="product-model-designer__overview">
    <article class="product-model-designer__overview-lead">
      <span class="product-model-designer__summary-label">真实证据概览</span>
      <strong>先提炼，再确认，再沉淀为正式物模型</strong>
      <p>属性优先来源于 `iot_device_property` 与消息快照，事件和服务只有在存在真实证据时才生成候选。</p>
      <div class="product-model-designer__overview-meta">
        <span>最近提炼：{{ formatDateTime(candidateSummary.lastExtractedAt) }}</span>
        <span>待人工确认：{{ candidateSummary.needsReviewCount ?? 0 }}</span>
      </div>
    </article>

    <article class="product-model-designer__overview-metric">
      <span class="product-model-designer__summary-label">属性</span>
      <strong>{{ candidateSummary.propertyCandidateCount ?? 0 }}</strong>
      <p>原始证据 {{ candidateSummary.propertyEvidenceCount ?? 0 }} 条</p>
    </article>
    <article class="product-model-designer__overview-metric">
      <span class="product-model-designer__summary-label">事件</span>
      <strong>{{ candidateSummary.eventCandidateCount ?? 0 }}</strong>
      <p>{{ candidateSummary.eventHint || '已发现可提炼事件候选' }}</p>
    </article>
    <article class="product-model-designer__overview-metric">
      <span class="product-model-designer__summary-label">服务</span>
      <strong>{{ candidateSummary.serviceCandidateCount ?? 0 }}</strong>
      <p>{{ candidateSummary.serviceHint || '已发现可提炼服务候选' }}</p>
    </article>
  </div>

  <div class="product-model-designer__candidate-workspace">
    <aside class="product-model-designer__candidate-nav">...</aside>
    <div class="product-model-designer__candidate-body">...</div>
    <aside class="product-model-designer__candidate-rail">...</aside>
  </div>
</section>
```

- [ ] **Step 4: 重构候选卡骨架，统一成“标题区 + 摘要区 + 编辑区 + 说明区”**

在 `visibleCandidates.length` 分支下，把单张候选卡替换成下面的结构：

```vue
<article
  v-for="candidate in visibleCandidates"
  :key="candidateKey(candidate)"
  class="product-model-designer__candidate-card card-surface"
>
  <header class="product-model-designer__candidate-card-header">
    <div class="product-model-designer__candidate-card-title">
      <strong>{{ candidateDraft(candidate).modelName }}</strong>
      <span>{{ candidate.identifier }}</span>
    </div>
    <label class="product-model-designer__candidate-card-check">
      <el-checkbox
        :model-value="isCandidateSelected(candidate)"
        @update:modelValue="updateCandidateSelection(candidate, $event)"
      />
      <span>写入正式模型</span>
    </label>
  </header>

  <div class="product-model-designer__candidate-card-tags">
    <el-tag round>{{ candidateTypeLabel(candidate.modelType) }}</el-tag>
    <el-tag round>{{ candidateGroupLabel(candidate.groupKey) }}</el-tag>
    <el-tag round>{{ candidate.needsReview ? '待人工确认' : '可直接采纳' }}</el-tag>
  </div>

  <div class="product-model-designer__candidate-card-summary">
    <span>数据类型：{{ candidate.dataType || '--' }}</span>
    <span>最近上报：{{ formatDateTime(candidate.lastReportTime) }}</span>
    <span>来源：{{ (candidate.sourceTables ?? []).join('、') || '--' }}</span>
  </div>

  <div class="product-model-designer__candidate-card-editor">
    <label class="product-model-designer__candidate-field">
      <span>建议名称</span>
      <el-input
        :model-value="candidateDraft(candidate).modelName"
        placeholder="补充候选名称"
        @update:modelValue="updateCandidateDraft(candidate, 'modelName', $event)"
      />
    </label>

    <div class="product-model-designer__candidate-evidence-card">
      <span>证据标签</span>
      <strong>置信度 {{ formatConfidence(candidate.confidence) }}</strong>
      <small>原始证据 {{ candidate.evidenceCount ?? 0 }} 条，消息证据 {{ candidate.messageEvidenceCount ?? 0 }} 条</small>
    </div>

    <label class="product-model-designer__candidate-field product-model-designer__candidate-field--full">
      <span>中文备注</span>
      <el-input
        type="textarea"
        :model-value="candidateDraft(candidate).description"
        :rows="3"
        placeholder="补充边界、命名规范和入库说明"
        @update:modelValue="updateCandidateDraft(candidate, 'description', $event)"
      />
    </label>
  </div>

  <p class="product-model-designer__description">
    {{ candidateDraft(candidate).description }}
  </p>
</article>
```

- [ ] **Step 5: 重构正式模型模式，增加“分类概览条 + 统一资产卡列表”**

将 `v-else` 分支替换成新骨架：

```vue
<section v-else class="detail-panel product-model-designer__section">
  <div class="product-model-designer__formal-overview" role="tablist" aria-label="产品物模型类型">
    <button
      v-for="item in typeOptions"
      :key="item.value"
      type="button"
      class="product-model-designer__formal-overview-card"
      :class="{ 'product-model-designer__formal-overview-card--active': activeType === item.value }"
      @click="activeType = item.value"
    >
      <span>{{ item.label }}</span>
      <strong>{{ countByType(item.value) }}</strong>
      <small>{{ emptyDescriptionMap[item.value] }}</small>
    </button>
  </div>

  <div class="product-model-designer__formal-stage">
    <div v-if="activeModels.length" class="product-model-designer__list">
      <article
        v-for="model in activeModels"
        :key="String(model.id)"
        class="product-model-designer__card card-surface"
      >
        <header class="product-model-designer__card-header">
          <div class="product-model-designer__card-heading">
            <strong>{{ model.modelName }}</strong>
            <span>{{ model.identifier }}</span>
          </div>
          <el-tag :type="model.requiredFlag === 1 ? 'warning' : 'info'" round>
            {{ model.requiredFlag === 1 ? '必填' : '选填' }}
          </el-tag>
        </header>

        <div class="product-model-designer__card-summary">
          <span>排序：{{ model.sortNo ?? '--' }}</span>
          <span v-if="model.modelType === 'property'">数据类型：{{ model.dataType || '--' }}</span>
          <span v-else-if="model.modelType === 'event'">事件类型：{{ model.eventType || '--' }}</span>
          <span v-else>服务输入/输出：{{ formatServiceSummary(model) }}</span>
        </div>

        <p class="product-model-designer__description">
          {{ model.description?.trim() || emptyDescriptionMap[model.modelType] }}
        </p>

        <StandardRowActions variant="editor" gap="comfortable">
          <StandardActionLink @click="handleEdit(model)">编辑</StandardActionLink>
          <StandardActionLink action="delete" @click="handleDelete(model)">删除</StandardActionLink>
        </StandardRowActions>
      </article>
    </div>

    <div v-else class="product-model-designer__empty">
      <strong>暂无物模型</strong>
      <p>{{ emptyDescriptionMap[activeType] }}</p>
    </div>
  </div>
</section>
```

- [ ] **Step 6: 运行组件与视图定向测试，确认结构已经转绿**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ProductModelDesignerDrawer.test.ts --run
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected:

- 新增的舞台布局测试通过
- 既有“候选接口失败降级”测试继续通过
- 产品工作台入口测试继续通过

- [ ] **Step 7: 提交模板重构**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "feat: rebuild product model designer workspace layout"
```

## Task 3: 重构样式，统一舞台视觉、资产卡骨架与响应式策略

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`

- [ ] **Step 1: 删除旧的 summary/type-switcher 布局样式，建立新的舞台层样式骨架**

在 `ProductModelDesignerDrawer.vue` 的 `<style scoped>` 中，用以下样式替换旧的 hero、summary、candidate-layout、type-switcher 相关块：

```css
.product-model-designer :deep(.el-drawer__body) {
  display: grid;
  gap: 1rem;
}

.product-model-designer__hero-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(18rem, 24rem);
  gap: 1rem;
  align-items: start;
}

.product-model-designer__hero-copy {
  display: grid;
  gap: 0.6rem;
}

.product-model-designer__hero-kicker {
  margin: 0;
  color: color-mix(in srgb, var(--brand) 62%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.product-model-designer__hero-copy h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: clamp(1.35rem, 2vw, 1.8rem);
  line-height: 1.2;
}

.product-model-designer__hero-product {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.product-model-designer__hero-product span {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.32rem 0.72rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-secondary);
  font-size: 0.82rem;
}
```

- [ ] **Step 2: 建立新的总览卡与工作区栅格样式**

继续在同一 `<style scoped>` 中新增以下布局样式：

```css
.product-model-designer__overview {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) repeat(3, minmax(0, 0.88fr));
  gap: 0.85rem;
}

.product-model-designer__overview-lead,
.product-model-designer__overview-metric,
.product-model-designer__candidate-rail,
.product-model-designer__formal-overview-card,
.card-surface {
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__overview-lead {
  display: grid;
  gap: 0.45rem;
  padding: 1.05rem 1.15rem;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 12%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.96));
}

.product-model-designer__candidate-workspace {
  display: grid;
  grid-template-columns: minmax(11rem, 12rem) minmax(0, 1fr) minmax(18rem, 20rem);
  gap: 1rem;
  align-items: start;
}
```

- [ ] **Step 3: 强化候选卡和正式模型卡的统一资产卡语义**

补充以下样式块，统一卡片标题、摘要和编辑区层次：

```css
.card-surface {
  display: grid;
  gap: 0.85rem;
  padding: 1rem 1.05rem;
}

.product-model-designer__candidate-card-title,
.product-model-designer__card-heading {
  display: grid;
  gap: 0.24rem;
}

.product-model-designer__candidate-card-summary,
.product-model-designer__card-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem 0.8rem;
}

.product-model-designer__candidate-card-summary span,
.product-model-designer__card-summary span {
  display: inline-flex;
  align-items: center;
  min-height: 1.75rem;
  padding: 0.28rem 0.62rem;
  border: 1px solid color-mix(in srgb, var(--accent) 10%, transparent);
  border-radius: var(--radius-pill);
  background: rgba(247, 250, 255, 0.96);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.product-model-designer__candidate-card-editor {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(14rem, 0.78fr);
  gap: 0.75rem;
}

.product-model-designer__candidate-evidence-card {
  display: grid;
  gap: 0.3rem;
  align-content: start;
  padding: 0.85rem 0.9rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(252, 253, 255, 0.96);
}
```

- [ ] **Step 4: 加入正式模型概览条与移动端响应式收口**

在样式末尾新增以下规则，确保正式模型模式和移动端同步收口：

```css
.product-model-designer__formal-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.product-model-designer__formal-overview-card {
  display: grid;
  gap: 0.3rem;
  padding: 0.95rem 1rem;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.product-model-designer__formal-overview-card--active {
  border-color: color-mix(in srgb, var(--brand) 36%, white);
  box-shadow: var(--shadow-brand);
  transform: translateY(-1px);
}

@media (max-width: 1100px) {
  .product-model-designer__hero-stage,
  .product-model-designer__overview,
  .product-model-designer__candidate-workspace,
  .product-model-designer__candidate-card-editor {
    grid-template-columns: 1fr;
  }

  .product-model-designer__candidate-rail {
    order: 3;
  }
}

@media (max-width: 768px) {
  .product-model-designer__hero-actions,
  .product-model-designer__formal-overview {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 5: 运行测试和构建，确认样式重构没有破坏行为**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ProductModelDesignerDrawer.test.ts --run
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
npm run build
```

Expected:

- 组件测试通过
- 工作台入口测试通过
- `vite build` 通过

- [ ] **Step 6: 提交视觉重构**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "feat: polish product model designer visual system"
```

## Task 4: 回写业务与前端治理文档

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 在业务文档中补充新页面结构说明**

在 `docs/02-业务功能与流程说明.md` 的产品物模型设计器条目下，补充这类说明：

```md
- 产品物模型设计器当前采用大工作台抽屉结构：顶部以“产品身份 + 模式切换 + 主操作”构成舞台头部，中部按“总览指标 + 候选治理工作区 / 正式模型概览条 + 模型列表”组织，不新增一级路由。
```

- [ ] **Step 2: 在变更记录中登记这次视觉升级**

在 `docs/08-变更记录与技术债清单.md` 中追加一条 2026-03-27 变更：

```md
- 2026-03-27：`/products` 内产品物模型设计器已升级为大工作台抽屉视觉。候选提炼模式强化为“舞台头部 + 1 主 3 辅总览 + 三段治理工作区”，正式模型模式同步收口到“分类概览条 + 统一资产卡列表”，继续沿用主系统品牌橙、云蓝与蓝灰体系。
```

- [ ] **Step 3: 在前端治理文档中沉淀新规则**

在 `docs/15-前端优化与治理计划.md` 的长期规则区补充：

```md
28. `/products` 内的产品物模型设计器当前固定采用“大工作台抽屉”语法：顶部必须保留产品身份与模式切换舞台层，候选提炼模式保持“1 主 3 辅总览 + 目录 / 主列表 / 确认面板”结构，正式模型模式保持“分类概览条 + 统一资产卡列表”结构；后续优化不得回退到松散的普通卡片堆叠布局。
```

- [ ] **Step 4: 运行最小验证并检查文档差异**

Run:

```bash
cd spring-boot-iot-ui
npm run build
git diff -- docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
```

Expected:

- 前端构建通过
- 文档 diff 只包含本轮页面结构和治理规则更新

- [ ] **Step 5: 提交文档同步**

```bash
git add docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: document product model designer visual refresh"
```

## Task 5: 最终回归与人工验收记录

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: 运行完整前端最小回归**

Run:

```bash
cd spring-boot-iot-ui
npm run test -- src/__tests__/components/ProductModelDesignerDrawer.test.ts --run
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts --run
npm run build
```

Expected:

- `ProductModelDesignerDrawer.test.ts` 全部通过
- `ProductWorkbenchView.test.ts` 通过
- `vite build` 通过

- [ ] **Step 2: 运行本地可访问性检查**

Run:

```bash
powershell -Command "(Invoke-WebRequest -Uri 'http://127.0.0.1:5174/products' -UseBasicParsing).StatusCode"
powershell -Command "((Invoke-RestMethod -Uri 'http://127.0.0.1:9999/actuator/health').status)"
```

Expected:

- `/products` 返回 `200`
- 后端健康状态返回 `UP`

- [ ] **Step 3: 记录人工验收要点**

按以下清单手工检查：

```md
1. 打开 `/products`，点击“物模型设置”后抽屉明显变宽。
2. 候选提炼模式下，顶部舞台头部、总览指标和三段工作区层次清楚。
3. 正式模型模式下，分类概览条与模型卡列表风格统一，不再像简化页。
4. 配色继续匹配主系统品牌橙、云蓝和蓝灰体系，没有额外私有主题。
5. 窄屏或压缩宽度时，三列区域会自动下沉为单列，不出现内容互相挤压。
```

- [ ] **Step 4: 提交最终收口**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue spring-boot-iot-ui/src/__tests__/components/ProductModelDesignerDrawer.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts docs/02-业务功能与流程说明.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "feat: refresh product model designer workspace visuals"
```

## 完成标准

1. 设计器抽屉显式加宽到大工作台尺寸，不再沿用默认详情抽屉宽度。
2. 候选提炼模式完成“舞台头部 + 1 主 3 辅总览 + 目录 / 主列表 / 确认面板”三层收口。
3. 正式模型模式完成“分类概览条 + 统一资产卡列表”收口。
4. 候选卡和正式模型卡共享一套资产卡视觉骨架，模式切换不再割裂。
5. 配色与主系统一致，不引入新的私有主色体系。
6. `ProductModelDesignerDrawer` 定向测试、`ProductWorkbenchView` 定向测试和前端 `build` 全部通过。
