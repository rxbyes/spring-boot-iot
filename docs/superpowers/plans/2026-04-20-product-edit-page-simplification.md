# Product Edit Page Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify the product edit page so it keeps the same four form sections as the create-product drawer, preserves object insight configuration, and removes the extra edit-governance chrome.

**Architecture:** Keep the existing product edit entry point and update API flow intact. Constrain the change to the frontend edit workspace component and its tests: the edit form should render as a plain form body with `基础档案 / 接入基线 / 补充说明 / 对象洞察配置`, while the surrounding product workbench drawer and backend contracts remain unchanged. Document the new UI contract in the existing product/workbench governance docs.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, Element Plus form primitives, existing product workbench view, Vitest, Vue Test Utils, Markdown docs under `docs/`.

---

## File Map

- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
  - Remove the edit-only heading and inline state, keep the same field groups as the create drawer, preserve `validate()/clearValidate()` exposure, and retain object insight configuration plus submit/cancel footer.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
  - Replace the old “edit governance” assertions with the new minimal form contract before touching the component.
- Modify: `docs/02-业务功能与流程说明.md`
  - Record that product edit now reuses the create-form information structure and no longer exposes a separate edit-governance header.
- Modify: `docs/08-变更记录与技术债清单.md`
  - Record the April 20 product edit simplification and the verification commands.
- Modify: `docs/15-前端优化与治理计划.md`
  - Add a long-term rule preventing `ProductEditWorkspace` from regrowing a private heading/inline-state layer when the create drawer already covers the same information.

### Task 1: Lock The Simplified Edit Contract In A Failing Test

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`

- [ ] **Step 1: Rewrite the focused component test to describe the new minimal edit form**

```ts
it('renders edit mode with the same core form sections as create mode and without extra governance chrome', async () => {
  const wrapper = mount(ProductEditWorkspace, {
    props: {
      model: {
        productKey: 'south-monitor',
        productName: '南方监测终端',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        description: '南方场站监测终端',
        metadataJson: '',
        status: 1
      },
      objectInsightMetrics: [],
      rules: {
        productKey: [{ required: true, message: '请输入产品 Key', trigger: 'blur' }]
      },
      editing: true,
      submitLoading: false,
      refreshState: 'warning',
      refreshMessage: '最新档案已取回；你已开始编辑，当前未自动覆盖表单。'
    },
    global: {
      stubs: {
        StandardButton: StandardButtonStub,
        ProductObjectInsightConfigEditor: ProductObjectInsightConfigEditorStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true
      }
    }
  })

  expect(wrapper.find('.product-edit-workspace__journal-head').exists()).toBe(false)
  expect(wrapper.find('.standard-inline-state-stub').exists()).toBe(false)
  expect(wrapper.text()).toContain('基础档案')
  expect(wrapper.text()).toContain('接入基线')
  expect(wrapper.text()).toContain('补充说明')
  expect(wrapper.text()).toContain('对象洞察配置')
  expect(wrapper.text()).not.toContain('编辑治理')
  expect(wrapper.text()).not.toContain('最新档案已取回')
  expect(wrapper.get('[data-testid="product-edit-cancel"]').text()).toContain('取消编辑')
  expect(wrapper.get('[data-testid="product-edit-submit"]').text()).toContain('保存')
})
```

- [ ] **Step 2: Run the focused component test to verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductEditWorkspace.test.ts
```

Expected: FAIL because the current component still renders `.product-edit-workspace__journal-head`, still imports `StandardInlineState`, and still shows the refresh warning copy.

### Task 2: Simplify `ProductEditWorkspace` To Match The Create Drawer Structure

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`

- [ ] **Step 1: Replace the template with a plain form body that matches the create drawer sections**

```vue
<template>
  <div class="ops-drawer-stack product-edit-workspace">
    <el-form
      ref="formRef"
      :model="model"
      :rules="rules"
      label-position="top"
      class="ops-drawer-form"
    >
      <section class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <h3>基础档案</h3>
        </div>
        <div class="ops-drawer-grid">
          <el-form-item label="产品 Key" prop="productKey">
            <el-input
              id="product-key"
              v-model="model.productKey"
              :disabled="editing"
              placeholder="请输入产品 Key，例如 accept-http-product-01"
            />
          </el-form-item>
          <el-form-item label="产品名称" prop="productName">
            <el-input id="product-name" v-model="model.productName" placeholder="请输入产品名称" />
          </el-form-item>
          <el-form-item label="厂商">
            <el-input v-model="model.manufacturer" placeholder="请输入厂商名称" />
          </el-form-item>
        </div>
      </section>

      <section class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <h3>接入基线</h3>
        </div>
        <div class="ops-drawer-grid">
          <el-form-item label="协议编码" prop="protocolCode">
            <el-input id="protocol-code" v-model="model.protocolCode" placeholder="请输入协议编码，例如 mqtt-json" />
          </el-form-item>
          <el-form-item label="节点类型" prop="nodeType">
            <el-select v-model="model.nodeType" placeholder="请选择节点类型">
              <el-option label="直连设备" :value="1" />
              <el-option label="网关设备" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="产品能力">
            <el-select
              :model-value="productCapabilityType"
              placeholder="请选择产品能力"
              @update:model-value="emit('update:productCapabilityType', $event)"
            >
              <el-option label="监测型" value="MONITORING" />
              <el-option label="采集型" value="COLLECTING" />
              <el-option label="预警型" value="WARNING" />
              <el-option label="视频型" value="VIDEO" />
              <el-option label="待确认" value="UNKNOWN" />
            </el-select>
          </el-form-item>
          <el-form-item label="数据格式">
            <el-input id="data-format" v-model="model.dataFormat" placeholder="请输入数据格式，例如 JSON" />
          </el-form-item>
          <el-form-item label="产品状态">
            <el-select v-model="model.status" placeholder="请选择产品状态">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
      </section>

      <section class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <h3>补充说明</h3>
        </div>
        <div class="ops-drawer-grid">
          <el-form-item label="说明" class="ops-drawer-grid__full">
            <el-input
              v-model="model.description"
              type="textarea"
              :rows="5"
              placeholder="请输入产品说明、接入约束或适用场景"
            />
          </el-form-item>
        </div>
      </section>

      <ProductObjectInsightConfigEditor
        :model-value="objectInsightMetrics"
        :available-models="availableModels"
        @update:model-value="emit('update:objectInsightMetrics', $event)"
      />
    </el-form>

    <div class="product-edit-workspace__footer">
      <StandardButton data-testid="product-edit-cancel" action="cancel" @click="emit('cancel')">
        {{ cancelText }}
      </StandardButton>
      <StandardButton
        data-testid="product-edit-submit"
        action="confirm"
        :loading="submitLoading"
        @click="emit('submit')"
      >
        {{ submitText }}
      </StandardButton>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Remove the edit-only script/style branches but preserve caller compatibility**

```ts
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import StandardButton from '@/components/StandardButton.vue'
import ProductObjectInsightConfigEditor from '@/components/product/ProductObjectInsightConfigEditor.vue'

const submitText = computed(() => (props.editing ? '保存' : '新增'))
const cancelText = computed(() => (props.editing ? '取消编辑' : '取消'))
```

```css
.product-edit-workspace {
  display: grid;
  gap: 1rem;
}

.product-edit-workspace__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
```

Implementation notes:
- Delete the `StandardInlineState` import and the `sectionNote / refreshTone / inlineMessage` computed branches.
- Keep `refreshState` and `refreshMessage` in the prop contract for now so `ProductWorkbenchView.vue` can continue passing them without creating a larger refactor in this task.
- Preserve the existing `validate()` / `clearValidate()` `defineExpose()` block unchanged.

- [ ] **Step 3: Re-run the focused component test to verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductEditWorkspace.test.ts
```

Expected: PASS with the simplified form structure and no edit-governance heading/inline-state.

### Task 3: Run A Focused Product Workbench Regression Pass

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Verify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Confirm the existing workbench tests still cover the edit entry path**

Use the existing assertions around the edit workspace stub as the regression target:

```ts
expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('edit')
expect(wrapper.find('.product-edit-workspace-stub').exists()).toBe(true)
```

- [ ] **Step 2: Run the edit workspace test together with the product workbench view test**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: PASS, proving that the simplified component still mounts through the existing edit entry path and that the list/workbench orchestration was not broken.

### Task 4: Update The Product And Frontend Governance Docs

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the product workflow doc with the new edit-page contract**

Add a sentence near the `/products` product-definition-center section:

```md
- 产品定义中心当前继续保留列表侧“新增产品”标准表单抽屉；`编辑档案` 也已收口为同一套基础表单语法，只保留 `基础档案 / 接入基线 / 补充说明 / 对象洞察配置` 四段，不再额外渲染“编辑治理”头部或内联说明提示。
```

- [ ] **Step 2: Record the change and verification in the changelog**

Add a new `2026-04-20` entry near the top of `docs/08-变更记录与技术债清单.md`:

```md
- 2026-04-20：产品定义中心编辑页已按新增产品表单收口。`ProductEditWorkspace` 当前只保留 `基础档案 / 接入基线 / 补充说明 / 对象洞察配置` 四段，去掉原先的“编辑治理”头部和编辑态内联提示，继续保留 `产品 Key` 禁用、`保存 / 取消编辑` 按钮差异。定向验证通过：`npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts`。
```

- [ ] **Step 3: Add the long-term frontend rule that blocks the old edit chrome from returning**

Add a rule near the product workbench/frontend governance bullets in `docs/15-前端优化与治理计划.md`:

```md
- `ProductEditWorkspace` 必须继续和新增产品表单保持同级信息结构：只保留 `基础档案 / 接入基线 / 补充说明 / 对象洞察配置` 四段，不得重新长出“编辑治理”私有头部、说明墙或编辑态内联提示；编辑态差异只允许保留 `产品 Key` 禁用与按钮文案差异。
```

### Task 5: Run Final Frontend Verification And Review Docs Scope

**Files:**
- Verify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- Verify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Verify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Verify: `README.md`
- Verify: `AGENTS.md`

- [ ] **Step 1: Run the focused frontend tests after docs land**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: PASS.

- [ ] **Step 2: Run the frontend build to catch template/type regressions**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected: Vite production build completes successfully with no template/type errors caused by the simplified edit component.

- [ ] **Step 3: Review `README.md` and `AGENTS.md` for required updates**

Check:

```bash
Select-String -Path 'README.md','AGENTS.md' -Pattern '编辑治理|编辑档案|产品定义中心'
```

Expected: No update needed unless one of those files explicitly describes the old edit-governance heading behavior. If the search only finds generic product-center references, leave both files unchanged and mention that in the final report.
