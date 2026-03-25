<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    class="product-model-designer"
    eyebrow="产品物模型设计器"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :loading="loading"
    loading-text="正在加载产品物模型..."
    :error-message="errorMessage"
    :empty="!product"
    empty-text="请先选择产品后再打开物模型设计器"
    @update:modelValue="emit('update:modelValue', $event)"
  >
    <section class="detail-panel detail-panel--hero product-model-designer__hero">
      <div class="product-model-designer__hero-copy">
        <h3>物模型目录</h3>
        <p>按属性、事件、服务三类维护产品契约，继续复用现有产品定义中心，不新增一级路由。</p>
      </div>
      <StandardButton
        action="add"
        :disabled="!product"
        @click="handleOpenCreateForm"
      >
        新增{{ activeTypeLabel }}
      </StandardButton>
    </section>

    <section class="detail-panel product-model-designer__section">
      <div class="product-model-designer__type-switcher" role="tablist" aria-label="产品物模型类型">
        <button
          v-for="item in typeOptions"
          :key="item.value"
          type="button"
          class="product-model-designer__type-chip"
          :class="{ 'product-model-designer__type-chip--active': activeType === item.value }"
          @click="activeType = item.value"
        >
          <span>{{ item.label }}</span>
          <strong>{{ countByType(item.value) }}</strong>
        </button>
      </div>

      <div v-if="activeModels.length" class="product-model-designer__list">
        <article
          v-for="model in activeModels"
          :key="String(model.id)"
          class="product-model-designer__card"
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

          <div class="product-model-designer__meta">
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
    </section>

    <template #footer>
      <StandardDrawerFooter @cancel="emit('update:modelValue', false)">
        <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="emit('update:modelValue', false)">
          关闭
        </StandardButton>
      </StandardDrawerFooter>
    </template>
  </StandardDetailDrawer>

  <StandardFormDrawer
    v-model="formVisible"
    :title="formTitle"
    eyebrow="产品物模型编辑"
    :subtitle="formSubtitle"
    size="40rem"
    @close="handleFormClose"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top" class="ops-drawer-form">
      <section class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <h3>基础信息</h3>
        </div>
        <div class="ops-drawer-grid">
          <el-form-item label="模型类型" prop="modelType">
            <el-select v-model="formData.modelType" placeholder="请选择模型类型" @change="handleFormTypeChange">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="标识符" prop="identifier">
            <el-input v-model="formData.identifier" placeholder="请输入物模型标识，例如 temperature" />
          </el-form-item>
          <el-form-item label="名称" prop="modelName">
            <el-input v-model="formData.modelName" placeholder="请输入物模型名称" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="formData.sortNo" :min="0" :step="10" controls-position="right" />
          </el-form-item>
          <el-form-item label="是否必填">
            <el-switch
              v-model="formData.requiredFlag"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="是"
              inactive-text="否"
            />
          </el-form-item>
          <el-form-item label="说明" class="ops-drawer-grid__full">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="补充业务含义、风险监测要求或联调说明"
            />
          </el-form-item>
        </div>
      </section>

      <section class="ops-drawer-section">
        <div class="ops-drawer-section__header">
          <h3>{{ activeFormTypeLabel }}配置</h3>
        </div>
        <div class="ops-drawer-grid">
          <template v-if="formData.modelType === 'property'">
            <el-form-item label="数据类型" prop="dataType">
              <el-input v-model="formData.dataType" placeholder="请输入数据类型，例如 decimal" />
            </el-form-item>
            <el-form-item label="规格 JSON" class="ops-drawer-grid__full">
              <el-input
                v-model="formData.specsJson"
                type="textarea"
                :rows="5"
                placeholder='请输入合法 JSON，例如 {"unit":"℃","precision":1}'
              />
            </el-form-item>
          </template>

          <template v-else-if="formData.modelType === 'event'">
            <el-form-item label="事件类型" prop="eventType">
              <el-input v-model="formData.eventType" placeholder="请输入事件类型，例如 alert" />
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="输入参数 JSON" class="ops-drawer-grid__full">
              <el-input
                v-model="formData.serviceInputJson"
                type="textarea"
                :rows="5"
                placeholder='请输入合法 JSON，例如 [{"identifier":"target","dataType":"string"}]'
              />
            </el-form-item>
            <el-form-item label="输出参数 JSON" class="ops-drawer-grid__full">
              <el-input
                v-model="formData.serviceOutputJson"
                type="textarea"
                :rows="5"
                placeholder='请输入合法 JSON，例如 [{"identifier":"accepted","dataType":"bool"}]'
              />
            </el-form-item>
          </template>
        </div>
      </section>
    </el-form>

    <template #footer>
      <StandardDrawerFooter
        :confirm-loading="submitLoading"
        :confirm-text="editingModelId ? '保存' : '新增'"
        @cancel="formVisible = false"
        @confirm="handleSubmit"
      >
        <StandardButton action="cancel" class="standard-drawer-footer__button standard-drawer-footer__button--ghost" @click="formVisible = false">
          取消
        </StandardButton>
        <StandardButton
          action="confirm"
          class="standard-drawer-footer__button standard-drawer-footer__button--primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          {{ editingModelId ? '保存' : '新增' }}
        </StandardButton>
      </StandardDrawerFooter>
    </template>
  </StandardFormDrawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import StandardActionLink from '@/components/StandardActionLink.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardRowActions from '@/components/StandardRowActions.vue'
import { productApi } from '@/api/product'
import type { Product, ProductModel, ProductModelType, ProductModelUpsertPayload } from '@/types/api'
import { ElMessage } from '@/utils/message'
import { confirmDelete, isConfirmCancelled } from '@/utils/confirm'

const props = defineProps<{
  modelValue: boolean
  product: Product | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

type ProductModelFormState = ProductModelUpsertPayload

const typeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性模型', value: 'property' },
  { label: '事件模型', value: 'event' },
  { label: '服务模型', value: 'service' }
]

const emptyDescriptionMap: Record<ProductModelType, string> = {
  property: '当前还没有属性模型，可以先定义遥测属性、规格 JSON 和风险监测字段。',
  event: '当前还没有事件模型，可以补齐告警、状态变化或故障上报事件。',
  service: '当前还没有服务模型，可以定义远程命令、输入输出参数和调用约束。'
}

const loading = ref(false)
const submitLoading = ref(false)
const errorMessage = ref('')
const models = ref<ProductModel[]>([])
const activeType = ref<ProductModelType>('property')
const formVisible = ref(false)
const editingModelId = ref<string | number | null>(null)
const formRef = ref<FormInstance>()

const formData = reactive<ProductModelFormState>(createDefaultFormState())

const drawerTitle = computed(() => props.product?.productName || props.product?.productKey || '产品物模型设计器')
const drawerSubtitle = computed(() =>
  props.product?.productKey
    ? `产品 Key：${props.product.productKey}，按属性 / 事件 / 服务三类维护接入契约。`
    : '按属性 / 事件 / 服务三类维护产品接入契约。'
)
const formTitle = computed(() => (editingModelId.value ? '编辑产品物模型' : '新增产品物模型'))
const formSubtitle = computed(() =>
  props.product?.productName
    ? `当前产品：${props.product.productName}`
    : '当前产品未命名'
)
const activeTypeLabel = computed(() => getTypeLabel(activeType.value))
const activeFormTypeLabel = computed(() => getTypeLabel(formData.modelType))
const activeModels = computed(() => models.value.filter((model) => model.modelType === activeType.value))
const formRules = computed<FormRules<ProductModelFormState>>(() => {
  const rules: FormRules<ProductModelFormState> = {
    modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
    identifier: [{ required: true, message: '请输入物模型标识', trigger: 'blur' }],
    modelName: [{ required: true, message: '请输入物模型名称', trigger: 'blur' }]
  }

  if (formData.modelType === 'property') {
    rules.dataType = [{ required: true, message: '属性模型必须填写数据类型', trigger: 'blur' }]
  }

  if (formData.modelType === 'event') {
    rules.eventType = [{ required: true, message: '事件模型必须填写事件类型', trigger: 'blur' }]
  }

  return rules
})

watch(
  () => [props.modelValue, props.product?.id] as const,
  ([visible, productId]) => {
    if (!visible) {
      resetDrawerState()
      return
    }
    if (!productId) {
      models.value = []
      errorMessage.value = ''
      return
    }
    void loadModels(productId)
  },
  { immediate: true }
)

function createDefaultFormState(modelType: ProductModelType = 'property'): ProductModelFormState {
  return {
    modelType,
    identifier: '',
    modelName: '',
    dataType: '',
    specsJson: '',
    eventType: '',
    serviceInputJson: '',
    serviceOutputJson: '',
    sortNo: 10,
    requiredFlag: 0,
    description: ''
  }
}

function resetFormState(modelType: ProductModelType = activeType.value) {
  const nextState = createDefaultFormState(modelType)
  Object.assign(formData, nextState)
}

function resetDrawerState() {
  loading.value = false
  errorMessage.value = ''
  models.value = []
  activeType.value = 'property'
  formVisible.value = false
  editingModelId.value = null
  resetFormState('property')
  formRef.value?.clearValidate()
}

function countByType(type: ProductModelType) {
  return models.value.filter((model) => model.modelType === type).length
}

function getTypeLabel(type: ProductModelType) {
  return typeOptions.find((item) => item.value === type)?.label ?? '物模型'
}

function formatServiceSummary(model: ProductModel) {
  const hasInput = Boolean(model.serviceInputJson?.trim())
  const hasOutput = Boolean(model.serviceOutputJson?.trim())
  if (hasInput && hasOutput) {
    return '已配置'
  }
  if (hasInput) {
    return '仅输入'
  }
  if (hasOutput) {
    return '仅输出'
  }
  return '未配置'
}

function normalizeText(value?: string | null) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

function normalizeJsonField(label: string, value?: string) {
  const normalized = normalizeText(value)
  if (!normalized) {
    return undefined
  }
  try {
    JSON.parse(normalized)
    return normalized
  } catch {
    throw new Error(`${label} 必须是合法 JSON`)
  }
}

function normalizePayload(): ProductModelUpsertPayload {
  const basePayload: ProductModelUpsertPayload = {
    modelType: formData.modelType,
    identifier: formData.identifier.trim(),
    modelName: formData.modelName.trim(),
    sortNo: formData.sortNo ?? undefined,
    requiredFlag: formData.requiredFlag ?? 0,
    description: normalizeText(formData.description)
  }

  if (formData.modelType === 'property') {
    return {
      ...basePayload,
      dataType: normalizeText(formData.dataType),
      specsJson: normalizeJsonField('规格 JSON', formData.specsJson)
    }
  }

  if (formData.modelType === 'event') {
    return {
      ...basePayload,
      eventType: normalizeText(formData.eventType)
    }
  }

  return {
    ...basePayload,
    serviceInputJson: normalizeJsonField('输入参数 JSON', formData.serviceInputJson),
    serviceOutputJson: normalizeJsonField('输出参数 JSON', formData.serviceOutputJson)
  }
}

async function loadModels(productId: string | number) {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await productApi.listProductModels(productId)
    models.value = response.data ?? []
  } catch (error) {
    models.value = []
    errorMessage.value = error instanceof Error ? error.message : '加载产品物模型失败'
  } finally {
    loading.value = false
  }
}

function handleOpenCreateForm() {
  editingModelId.value = null
  resetFormState(activeType.value)
  formVisible.value = true
  formRef.value?.clearValidate()
}

function handleEdit(model: ProductModel) {
  editingModelId.value = model.id
  activeType.value = model.modelType
  Object.assign(formData, {
    modelType: model.modelType,
    identifier: model.identifier,
    modelName: model.modelName,
    dataType: model.dataType ?? '',
    specsJson: model.specsJson ?? '',
    eventType: model.eventType ?? '',
    serviceInputJson: model.serviceInputJson ?? '',
    serviceOutputJson: model.serviceOutputJson ?? '',
    sortNo: model.sortNo ?? 10,
    requiredFlag: model.requiredFlag ?? 0,
    description: model.description ?? ''
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

function handleFormTypeChange(nextType: ProductModelType) {
  if (nextType === 'property') {
    formData.eventType = ''
    formData.serviceInputJson = ''
    formData.serviceOutputJson = ''
    return
  }

  if (nextType === 'event') {
    formData.dataType = ''
    formData.specsJson = ''
    formData.serviceInputJson = ''
    formData.serviceOutputJson = ''
    return
  }

  formData.dataType = ''
  formData.specsJson = ''
  formData.eventType = ''
}

function handleFormClose() {
  editingModelId.value = null
  resetFormState(activeType.value)
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!props.product?.id) {
    ElMessage.warning('请先选择产品后再维护物模型')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  let payload: ProductModelUpsertPayload
  try {
    payload = normalizePayload()
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '请检查 JSON 配置格式')
    return
  }

  submitLoading.value = true
  try {
    if (editingModelId.value) {
      await productApi.updateProductModel(props.product.id, editingModelId.value, payload)
      ElMessage.success('产品物模型更新成功')
    } else {
      await productApi.addProductModel(props.product.id, payload)
      ElMessage.success('产品物模型新增成功')
    }
    activeType.value = payload.modelType
    formVisible.value = false
    await loadModels(props.product.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存产品物模型失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(model: ProductModel) {
  if (!props.product?.id) {
    return
  }

  try {
    await confirmDelete('产品物模型', model.modelName || model.identifier)
    await productApi.deleteProductModel(props.product.id, model.id)
    models.value = models.value.filter((item) => String(item.id) !== String(model.id))
    ElMessage.success('产品物模型删除成功')
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      ElMessage.error(error instanceof Error ? error.message : '删除产品物模型失败')
    }
  }
}
</script>

<style scoped>
.product-model-designer :deep(.el-drawer__body) {
  display: grid;
  gap: 1rem;
}

.product-model-designer__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-model-designer__hero-copy {
  display: grid;
  gap: 0.45rem;
}

.product-model-designer__hero-copy h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.1rem;
}

.product-model-designer__hero-copy p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.product-model-designer__section {
  display: grid;
  gap: 1rem;
}

.product-model-designer__type-switcher {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.product-model-designer__type-chip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(245, 249, 255, 0.94));
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.product-model-designer__type-chip strong {
  color: var(--text-heading);
  font-size: 1rem;
}

.product-model-designer__type-chip--active {
  border-color: color-mix(in srgb, var(--brand) 38%, white);
  background:
    linear-gradient(180deg, rgba(244, 248, 255, 0.98), rgba(238, 245, 255, 0.96)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 45%);
  box-shadow: 0 8px 20px rgba(28, 63, 113, 0.08);
  color: var(--brand);
  transform: translateY(-1px);
}

.product-model-designer__list {
  display: grid;
  gap: 0.9rem;
}

.product-model-designer__card {
  display: grid;
  gap: 0.8rem;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: 0 6px 14px rgba(30, 52, 86, 0.05);
}

.product-model-designer__card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-model-designer__card-heading {
  display: grid;
  gap: 0.3rem;
}

.product-model-designer__card-heading strong {
  color: var(--text-heading);
  font-size: 1rem;
}

.product-model-designer__card-heading span {
  color: var(--text-caption);
  font-size: 0.9rem;
}

.product-model-designer__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.product-model-designer__description {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

.product-model-designer__empty {
  display: grid;
  gap: 0.45rem;
  padding: 1.2rem 1.1rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 28%, white);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(246, 250, 255, 0.92), rgba(255, 255, 255, 0.96));
}

.product-model-designer__empty strong {
  color: var(--text-heading);
}

.product-model-designer__empty p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .product-model-designer__hero {
    flex-direction: column;
  }

  .product-model-designer__type-switcher {
    grid-template-columns: 1fr;
  }
}
</style>
