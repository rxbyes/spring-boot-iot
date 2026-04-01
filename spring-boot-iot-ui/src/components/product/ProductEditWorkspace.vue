<template>
  <div class="product-edit-workspace">
    <section class="detail-panel product-edit-workspace__brief-head">
      <div class="product-edit-workspace__summary-copy product-edit-workspace__section-heading">
        <h3 class="product-edit-workspace__brief-title">编辑治理</h3>
        <p class="product-edit-workspace__summary-description product-edit-workspace__section-note">{{ sectionNote }}</p>
      </div>
    </section>

    <section class="detail-panel product-edit-workspace__brief-band">
      <div class="product-edit-workspace__summary-metrics product-edit-workspace__context-strip">
        <article class="product-edit-workspace__brief-item">
          <span>产品 Key</span>
          <strong>{{ formatText(model.productKey) }}</strong>
          <small>编辑状态下保持标识稳定，避免影响设备接入契约。</small>
        </article>
        <article class="product-edit-workspace__brief-item">
          <span>协议编码</span>
          <strong>{{ formatText(model.protocolCode) }}</strong>
          <small>统一对齐协议解码与后续物模型治理基线。</small>
        </article>
        <article class="product-edit-workspace__brief-item">
          <span>节点与状态</span>
          <strong>{{ nodeTypeText }} / {{ statusText }}</strong>
          <small>调整节点类型或状态前先确认设备接入边界与影响范围。</small>
        </article>
      </div>
    </section>

    <StandardInlineState
      class="product-edit-workspace__inline-state"
      :message="inlineMessage"
      :tone="refreshTone"
    />

    <el-form
      ref="formRef"
      :model="model"
      :rules="rules"
      label-position="top"
      class="ops-drawer-form product-edit-workspace__form product-edit-workspace__form-stage"
    >
      <div class="product-edit-workspace__form-intro">
        按基础档案、接入基线和补充说明三段完成本次变更，保存后同步回写当前工作台头部和列表台账。
      </div>

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

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import type { ProductAddPayload } from '@/types/api'

type ProductEditFormState = ProductAddPayload

const props = withDefaults(
  defineProps<{
    model: ProductEditFormState
    rules: FormRules<ProductEditFormState>
    editing?: boolean
    submitLoading?: boolean
    refreshState?: 'info' | 'warning' | 'error' | ''
    refreshMessage?: string
  }>(),
  {
    editing: false,
    submitLoading: false,
    refreshState: '',
    refreshMessage: ''
  }
)

const emit = defineEmits<{
  (event: 'cancel'): void
  (event: 'submit'): void
}>()

const formRef = ref<FormInstance>()

const submitText = computed(() => (props.editing ? '保存' : '新增'))
const cancelText = computed(() => (props.editing ? '取消编辑' : '取消'))
const nodeTypeText = computed(() => (props.model.nodeType === 2 ? '网关设备' : '直连设备'))
const statusText = computed(() => (props.model.status === 0 ? '停用' : '启用'))
const sectionNote = computed(() => {
  if (props.editing) {
    return '当前变更会直接回写产品经营工作台的头部信息、治理语境和列表档案。'
  }
  return '先锁定接入身份、协议边界和节点类型，再把补充说明沉淀到同一份产品档案。'
})
const refreshTone = computed<'info' | 'error'>(() => (props.refreshState === 'error' ? 'error' : 'info'))
const inlineMessage = computed(() => {
  if (props.refreshMessage) {
    return props.refreshMessage
  }
  if (props.model.status === 0) {
    return '停用产品前请先核对关联设备、协议边界和物模型治理是否会受到影响。'
  }
  return '优先核对产品标识、协议编码、节点类型与状态，避免运行中的接入链路发生漂移。'
})

function formatText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  return String(value)
}

async function validate() {
  const valid = await formRef.value?.validate().catch(() => false)
  return Boolean(valid)
}

function clearValidate() {
  formRef.value?.clearValidate?.()
}

defineExpose({
  validate,
  clearValidate
})
</script>

<style scoped>
.product-edit-workspace,
.product-edit-workspace__summary-copy,
.product-edit-workspace__summary-metrics {
  display: grid;
  gap: 0.85rem;
}

.product-edit-workspace {
  gap: 1.08rem;
}

.product-edit-workspace__brief-head,
.product-edit-workspace__brief-band {
  display: grid;
  gap: 0.82rem;
  padding: 1rem 1.04rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.99), rgba(255, 255, 255, 0.99));
  box-shadow: 0 12px 24px rgba(28, 53, 87, 0.05);
}

.product-edit-workspace__summary-description,
.product-edit-workspace__brief-item span,
.product-edit-workspace__brief-item small {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.product-edit-workspace__brief-head {
  justify-items: center;
  text-align: center;
}

.product-edit-workspace__brief-title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.24rem;
  line-height: 1.28;
}

.product-edit-workspace__brief-item strong {
  margin: 0;
  color: var(--text-heading);
}

.product-edit-workspace__summary-metrics {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
}

.product-edit-workspace__brief-item {
  display: grid;
  gap: 0.36rem;
  padding: 0.72rem 0.94rem;
  border-right: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  text-align: center;
}

.product-edit-workspace__brief-item:last-child {
  border-right: none;
}

.product-edit-workspace__brief-item strong {
  font-size: 1.08rem;
}

.product-edit-workspace__section-note {
  max-width: 46rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.72;
}

.product-edit-workspace__form-stage {
  display: grid;
  gap: 1rem;
  padding: 1rem 1.04rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 4px);
  background: linear-gradient(180deg, rgba(252, 253, 255, 0.98), rgba(255, 255, 255, 0.98));
  box-shadow: 0 12px 28px rgba(28, 53, 87, 0.05);
}

.product-edit-workspace__form-intro {
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.72;
}

.product-edit-workspace__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 960px) {
  .product-edit-workspace__summary-metrics {
    grid-template-columns: 1fr;
  }

  .product-edit-workspace__brief-item {
    border-right: none;
    border-top: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  }

  .product-edit-workspace__brief-item:first-child {
    border-top: none;
  }
}
</style>
