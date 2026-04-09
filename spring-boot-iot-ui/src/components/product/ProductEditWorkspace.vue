<template>
  <div class="product-edit-workspace">
    <section class="product-edit-workspace__journal-head">
      <div class="product-edit-workspace__summary-copy">
        <h3 class="product-edit-workspace__revision-title">编辑治理</h3>
        <p class="product-edit-workspace__summary-description product-edit-workspace__section-note">{{ sectionNote }}</p>
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
      class="ops-drawer-form product-edit-workspace__form product-edit-workspace__draft-sheet"
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

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import ProductObjectInsightConfigEditor from '@/components/product/ProductObjectInsightConfigEditor.vue'
import type { ProductAddPayload, ProductModel, ProductObjectInsightCustomMetricConfig } from '@/types/api'

type ProductEditFormState = ProductAddPayload

const props = withDefaults(
  defineProps<{
    model: ProductEditFormState
    objectInsightMetrics: ProductObjectInsightCustomMetricConfig[]
    availableModels?: ProductModel[]
    rules: FormRules<ProductEditFormState>
    editing?: boolean
    submitLoading?: boolean
    refreshState?: 'info' | 'warning' | 'error' | ''
    refreshMessage?: string
  }>(),
  {
    editing: false,
    availableModels: () => [],
    submitLoading: false,
    refreshState: '',
    refreshMessage: ''
  }
)

const emit = defineEmits<{
  (event: 'cancel'): void
  (event: 'submit'): void
  (event: 'update:objectInsightMetrics', value: ProductObjectInsightCustomMetricConfig[]): void
}>()

const formRef = ref<FormInstance>()

const submitText = computed(() => (props.editing ? '保存' : '新增'))
const cancelText = computed(() => (props.editing ? '取消编辑' : '取消'))
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
.product-edit-workspace__summary-copy {
  display: grid;
}

.product-edit-workspace {
  gap: 1rem;
}

.product-edit-workspace__journal-head {
  display: grid;
  gap: 0.4rem;
  padding-top: 0.86rem;
  border-top: 1px solid var(--panel-border-strong);
}

.product-edit-workspace__summary-copy {
  gap: 0.34rem;
}

.product-edit-workspace__revision-title {
  margin: 0;
  color: var(--text-heading);
}

.product-edit-workspace__revision-title {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: 1.32rem;
  line-height: 1.2;
}

.product-edit-workspace__summary-description {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.product-edit-workspace__section-note {
  max-width: 42rem;
  font-size: 0.9rem;
}

.product-edit-workspace__draft-sheet {
  display: grid;
  gap: 1rem;
  padding: 1rem 0;
  border-top: 1px solid var(--panel-border-strong);
}

.product-edit-workspace__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
