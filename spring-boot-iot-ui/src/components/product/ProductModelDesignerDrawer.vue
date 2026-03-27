<template>
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
          <span>{{ productNodeTypeLabel }}</span>
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
        <aside class="product-model-designer__candidate-nav">
          <div class="product-model-designer__candidate-nav-header">
            <h3>候选目录</h3>
            <p>按类型和待人工确认状态逐项治理</p>
          </div>
          <button
            v-for="item in candidateNavItems"
            :key="item.key"
            type="button"
            class="product-model-designer__candidate-nav-item"
            :class="{ 'product-model-designer__candidate-nav-item--active': activeCandidateView === item.key }"
            @click="activeCandidateView = item.key"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </button>
          <p class="product-model-designer__candidate-nav-tip">
            优先治理真实证据稳定、命名边界清晰的候选。
          </p>
        </aside>

        <div class="product-model-designer__candidate-body">
          <div class="product-model-designer__candidate-body-header">
            <div>
              <h3>{{ activeCandidateViewTitle }}</h3>
              <p>{{ activeCandidateViewDescription }}</p>
            </div>
            <span class="product-model-designer__candidate-body-meta">
              最近提炼：{{ formatDateTime(candidateSummary.lastExtractedAt) }}
            </span>
          </div>

          <div v-if="visibleCandidates.length" class="product-model-designer__candidate-list">
            <article
              v-for="candidate in visibleCandidates"
              :key="candidateKey(candidate)"
              class="product-model-designer__candidate-card card-surface product-model-designer__card-surface"
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
                <el-tag round>
                  {{ candidate.needsReview ? '待人工确认' : '可直接采纳' }}
                </el-tag>
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
          </div>

          <div v-else class="product-model-designer__empty">
            <strong>当前目录暂无候选</strong>
            <p>{{ activeCandidateViewDescription }}</p>
          </div>
        </div>

        <aside class="product-model-designer__candidate-rail">
          <div class="product-model-designer__candidate-rail-header">
            <h3>确认写入</h3>
            <p>只会写入已勾选候选，已存在正式模型的标识会在服务端自动跳过。</p>
          </div>
          <div class="product-model-designer__confirm-metrics">
            <div>
              <span>拟新增</span>
              <strong>{{ selectedCandidateCount }}</strong>
            </div>
            <div>
              <span>待人工确认</span>
              <strong>{{ candidateSummary.needsReviewCount ?? 0 }}</strong>
            </div>
            <div>
              <span>冲突回执</span>
              <strong>{{ lastConfirmSummary?.conflictCount ?? 0 }}</strong>
            </div>
          </div>
          <p v-if="candidateSummary.eventHint" class="product-model-designer__confirm-hint">
            {{ candidateSummary.eventHint }}
          </p>
          <p v-if="candidateSummary.serviceHint" class="product-model-designer__confirm-hint">
            {{ candidateSummary.serviceHint }}
          </p>
          <StandardButton
            action="confirm"
            :disabled="selectedCandidateCount === 0 || !product"
            :loading="candidateSubmitLoading"
            data-testid="confirm-model-candidates"
            @click="handleConfirmCandidates"
          >
            确认并写入正式物模型
          </StandardButton>
        </aside>
      </div>
    </section>

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
            class="product-model-designer__card card-surface product-model-designer__card-surface"
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
import type {
  Product,
  ProductModel,
  ProductModelCandidate,
  ProductModelCandidateConfirmItem,
  ProductModelCandidateConfirmPayload,
  ProductModelCandidateResult,
  ProductModelCandidateSummary,
  ProductModelType,
  ProductModelUpsertPayload
} from '@/types/api'
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
type DesignerMode = 'candidates' | 'formal'
type CandidateViewKey = ProductModelType | 'review'

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
const candidateSubmitLoading = ref(false)
const errorMessage = ref('')
const models = ref<ProductModel[]>([])
const activeType = ref<ProductModelType>('property')
const designerMode = ref<DesignerMode>('candidates')
const activeCandidateView = ref<CandidateViewKey>('property')
const formVisible = ref(false)
const editingModelId = ref<string | number | null>(null)
const formRef = ref<FormInstance>()
const candidateResult = ref<ProductModelCandidateResult | null>(null)
const lastConfirmSummary = ref<ProductModelCandidateSummary | null>(null)
const candidateSelections = ref<Record<string, boolean>>({})
const candidateDrafts = ref<Record<string, { modelName: string; description: string }>>({})

const formData = reactive<ProductModelFormState>(createDefaultFormState())

const drawerTitle = computed(() => props.product?.productName || props.product?.productKey || '产品物模型设计器')
const drawerSubtitle = computed(() =>
  props.product?.productKey
    ? `产品 Key：${props.product.productKey}，按属性 / 事件 / 服务三类维护接入契约。`
    : '按属性 / 事件 / 服务三类维护产品接入契约。'
)
const designerStageTitle = computed(() =>
  designerMode.value === 'candidates' ? '基于真实上报提炼产品契约' : '统一维护产品正式物模型'
)
const designerStageHint = computed(() =>
  designerMode.value === 'candidates' ? '仅提炼真实证据' : '维护已确认契约'
)
const productNodeTypeLabel = computed(() => {
  if (!props.product) {
    return '--'
  }
  return props.product.nodeType === 1 ? '直连设备' : '网关设备'
})
const formTitle = computed(() => (editingModelId.value ? '编辑产品物模型' : '新增产品物模型'))
const formSubtitle = computed(() =>
  props.product?.productName
    ? `当前产品：${props.product.productName}`
    : '当前产品未命名'
)
const activeTypeLabel = computed(() => getTypeLabel(activeType.value))
const activeFormTypeLabel = computed(() => getTypeLabel(formData.modelType))
const activeModels = computed(() => models.value.filter((model) => model.modelType === activeType.value))
const candidateSummary = computed<ProductModelCandidateSummary>(() => candidateResult.value?.summary ?? createEmptyCandidateSummary())
const allCandidates = computed<ProductModelCandidate[]>(() => [
  ...(candidateResult.value?.propertyCandidates ?? []),
  ...(candidateResult.value?.eventCandidates ?? []),
  ...(candidateResult.value?.serviceCandidates ?? [])
])
const reviewCandidates = computed(() => allCandidates.value.filter((candidate) => Boolean(candidate.needsReview)))
const candidateNavItems = computed(() => [
  { key: 'property' as const, label: '属性候选', count: candidateResult.value?.propertyCandidates?.length ?? 0 },
  { key: 'event' as const, label: '事件候选', count: candidateResult.value?.eventCandidates?.length ?? 0 },
  { key: 'service' as const, label: '服务候选', count: candidateResult.value?.serviceCandidates?.length ?? 0 },
  { key: 'review' as const, label: '待人工确认', count: reviewCandidates.value.length }
])
const visibleCandidates = computed<ProductModelCandidate[]>(() => {
  if (activeCandidateView.value === 'review') {
    return reviewCandidates.value
  }
  if (activeCandidateView.value === 'property') {
    return candidateResult.value?.propertyCandidates ?? []
  }
  if (activeCandidateView.value === 'event') {
    return candidateResult.value?.eventCandidates ?? []
  }
  return candidateResult.value?.serviceCandidates ?? []
})
const activeCandidateViewTitle = computed(() => {
  if (activeCandidateView.value === 'review') {
    return '待人工确认'
  }
  return candidateTypeLabel(activeCandidateView.value)
})
const activeCandidateViewDescription = computed(() => {
  if (activeCandidateView.value === 'review') {
    return '集中处理临时验证字段、命名漂移和边界尚不稳定的候选。'
  }
  if (activeCandidateView.value === 'event') {
    return candidateSummary.value.eventHint || '仅在存在真实事件证据时才会生成事件候选。'
  }
  if (activeCandidateView.value === 'service') {
    return candidateSummary.value.serviceHint || '仅在存在稳定命令证据时才会生成服务候选。'
  }
  return '从真实属性快照和消息证据中提炼出的属性候选。'
})
const selectedCandidates = computed(() =>
  allCandidates.value.filter((candidate) => candidateSelections.value[candidateKey(candidate)])
)
const selectedCandidateCount = computed(() => selectedCandidates.value.length)
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
      candidateResult.value = createEmptyCandidateResult()
      errorMessage.value = ''
      return
    }
    void loadDesignerData(productId)
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
  candidateSubmitLoading.value = false
  errorMessage.value = ''
  models.value = []
  activeType.value = 'property'
  designerMode.value = 'candidates'
  activeCandidateView.value = 'property'
  candidateResult.value = createEmptyCandidateResult()
  lastConfirmSummary.value = null
  candidateSelections.value = {}
  candidateDrafts.value = {}
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

function createEmptyCandidateSummary(): ProductModelCandidateSummary {
  return {
    propertyEvidenceCount: 0,
    propertyCandidateCount: 0,
    eventEvidenceCount: 0,
    eventCandidateCount: 0,
    serviceEvidenceCount: 0,
    serviceCandidateCount: 0,
    needsReviewCount: 0,
    existingModelCount: 0,
    createdCount: 0,
    skippedCount: 0,
    conflictCount: 0,
    eventHint: '暂无真实事件证据。',
    serviceHint: '暂无真实服务证据。',
    lastExtractedAt: null
  }
}

function createEmptyCandidateResult(productId?: string | number): ProductModelCandidateResult {
  return {
    productId: productId ?? '',
    summary: createEmptyCandidateSummary(),
    propertyCandidates: [],
    eventCandidates: [],
    serviceCandidates: []
  }
}

function candidateKey(candidate: ProductModelCandidate) {
  return `${candidate.modelType}:${candidate.identifier}`
}

function candidateDraft(candidate: ProductModelCandidate) {
  const key = candidateKey(candidate)
  return candidateDrafts.value[key] ?? {
    modelName: candidate.modelName,
    description: candidate.description ?? ''
  }
}

function updateCandidateDraft(candidate: ProductModelCandidate, field: 'modelName' | 'description', value: string) {
  const key = candidateKey(candidate)
  const currentDraft = candidateDraft(candidate)
  candidateDrafts.value = {
    ...candidateDrafts.value,
    [key]: {
      ...currentDraft,
      [field]: value
    }
  }
}

function isCandidateSelected(candidate: ProductModelCandidate) {
  return Boolean(candidateSelections.value[candidateKey(candidate)])
}

function updateCandidateSelection(candidate: ProductModelCandidate, value: boolean) {
  candidateSelections.value = {
    ...candidateSelections.value,
    [candidateKey(candidate)]: value
  }
}

function syncCandidateState(result: ProductModelCandidateResult) {
  const nextSelections: Record<string, boolean> = {}
  const nextDrafts: Record<string, { modelName: string; description: string }> = {}
  for (const candidate of [
    ...(result.propertyCandidates ?? []),
    ...(result.eventCandidates ?? []),
    ...(result.serviceCandidates ?? [])
  ]) {
    const key = candidateKey(candidate)
    nextSelections[key] = candidate.modelType === 'property' && !candidate.needsReview
    nextDrafts[key] = {
      modelName: candidate.modelName,
      description: candidate.description ?? ''
    }
  }
  candidateSelections.value = nextSelections
  candidateDrafts.value = nextDrafts
  const firstAvailable = candidateNavItems.value.find((item) => item.count > 0)?.key ?? 'property'
  activeCandidateView.value = firstAvailable
}

async function loadDesignerData(productId: string | number) {
  loading.value = true
  errorMessage.value = ''
  try {
    const [modelResponse, candidateResponse] = await Promise.allSettled([
      productApi.listProductModels(productId),
      productApi.listProductModelCandidates(productId)
    ])

    if (modelResponse.status === 'rejected') {
      throw modelResponse.reason
    }

    models.value = modelResponse.value.data ?? []

    if (candidateResponse.status === 'fulfilled') {
      candidateResult.value = candidateResponse.value.data ?? createEmptyCandidateResult(productId)
      syncCandidateState(candidateResult.value)
      return
    }

    candidateResult.value = createEmptyCandidateResult(productId)
    syncCandidateState(candidateResult.value)
    designerMode.value = 'formal'
  } catch (error) {
    models.value = []
    candidateResult.value = createEmptyCandidateResult(productId)
    errorMessage.value = error instanceof Error ? error.message : '加载产品物模型失败'
  } finally {
    loading.value = false
  }
}

async function handleReloadDesigner() {
  if (!props.product?.id) {
    return
  }
  await loadDesignerData(props.product.id)
}

function handleOpenCreateForm() {
  designerMode.value = 'formal'
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

function buildCandidateConfirmPayload(): ProductModelCandidateConfirmPayload {
  return {
    items: selectedCandidates.value.map<ProductModelCandidateConfirmItem>((candidate) => {
      const draft = candidateDraft(candidate)
      return {
        modelType: candidate.modelType,
        identifier: candidate.identifier,
        modelName: draft.modelName.trim(),
        dataType: candidate.dataType ?? undefined,
        specsJson: candidate.specsJson ?? undefined,
        eventType: candidate.eventType ?? undefined,
        serviceInputJson: candidate.serviceInputJson ?? undefined,
        serviceOutputJson: candidate.serviceOutputJson ?? undefined,
        sortNo: candidate.sortNo ?? undefined,
        requiredFlag: candidate.requiredFlag ?? 0,
        description: normalizeText(draft.description)
      }
    })
  }
}

async function handleConfirmCandidates() {
  if (!props.product?.id) {
    ElMessage.warning('请先选择产品后再维护物模型')
    return
  }
  if (selectedCandidateCount.value === 0) {
    ElMessage.warning('请至少选择一条候选后再确认写入')
    return
  }

  candidateSubmitLoading.value = true
  try {
    const payload = buildCandidateConfirmPayload()
    const response = await productApi.confirmProductModelCandidates(props.product.id, payload)
    lastConfirmSummary.value = response.data ?? null
    ElMessage.success(`已写入 ${response.data?.createdCount ?? 0} 条正式物模型`)
    designerMode.value = 'formal'
    await loadDesignerData(props.product.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认写入正式物模型失败')
  } finally {
    candidateSubmitLoading.value = false
  }
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
    designerMode.value = 'formal'
    formVisible.value = false
    await loadDesignerData(props.product.id)
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
    await loadDesignerData(props.product.id)
    ElMessage.success('产品物模型删除成功')
  } catch (error) {
    if (!isConfirmCancelled(error)) {
      ElMessage.error(error instanceof Error ? error.message : '删除产品物模型失败')
    }
  }
}

function formatDateTime(value?: string | null) {
  return value?.trim() || '--'
}

function formatConfidence(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--'
  }
  return `${Math.round(value * 100)}%`
}

function candidateTypeLabel(type: ProductModelType) {
  return getTypeLabel(type)
}

function candidateGroupLabel(groupKey?: string | null) {
  if (groupKey === 'telemetry') {
    return '业务测点'
  }
  if (groupKey === 'device_status') {
    return '设备状态'
  }
  if (groupKey === 'location') {
    return '定位信息'
  }
  if (groupKey === 'service') {
    return '服务证据'
  }
  if (groupKey === 'event') {
    return '事件证据'
  }
  return '待归类'
}
</script>

<style scoped>
.product-model-designer :deep(.el-drawer__body) {
  display: grid;
  gap: 1rem;
}

.product-model-designer__hero-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(18rem, 24rem);
  gap: 1rem;
  align-items: start;
  padding: 1.15rem 1.2rem;
  border: 1px solid color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  border-radius: calc(var(--radius-lg) + 2px);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 38%),
    radial-gradient(circle at bottom left, color-mix(in srgb, var(--accent) 14%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(245, 249, 255, 0.96));
  box-shadow: var(--shadow-surface-soft-sm);
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

.product-model-designer__hero-description,
.product-model-designer__candidate-nav-header p,
.product-model-designer__candidate-body-header p,
.product-model-designer__candidate-rail-header p,
.product-model-designer__overview-lead p,
.product-model-designer__overview-metric p,
.product-model-designer__empty p {
  margin: 0;
  color: var(--text-caption);
  line-height: 1.6;
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

.product-model-designer__hero-actions {
  display: grid;
  justify-items: end;
  gap: 0.75rem;
  align-content: start;
}

.product-model-designer__mode-switcher {
  display: inline-flex;
  padding: 0.24rem;
  border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--panel-border));
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.92);
}

.product-model-designer__mode-chip,
.product-model-designer__candidate-nav-item,
.product-model-designer__formal-overview-card {
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.product-model-designer__mode-chip {
  min-width: 6.8rem;
  padding: 0.58rem 0.9rem;
  border: 0;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-caption);
  cursor: pointer;
}

.product-model-designer__mode-chip--active {
  background: linear-gradient(180deg, rgba(242, 247, 255, 0.98), rgba(232, 240, 255, 0.94));
  color: var(--brand);
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__hero-action-hint {
  margin: 0;
  color: var(--text-caption-2);
  font-size: 0.84rem;
}

.product-model-designer__section,
.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-stage,
.product-model-designer__card,
.product-model-designer__candidate-card {
  display: grid;
  gap: 0.9rem;
}

.product-model-designer__overview {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) repeat(3, minmax(0, 0.88fr));
  gap: 0.85rem;
}

.product-model-designer__overview-lead,
.product-model-designer__overview-metric,
.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-overview-card,
.product-model-designer__formal-stage,
.card-surface {
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 2px);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.94));
  box-shadow: var(--shadow-surface-soft-sm);
}

.product-model-designer__overview-lead,
.product-model-designer__overview-metric,
.product-model-designer__candidate-nav,
.product-model-designer__candidate-body,
.product-model-designer__candidate-rail,
.product-model-designer__formal-stage {
  padding: 1rem 1.05rem;
}

.product-model-designer__overview-lead {
  display: grid;
  gap: 0.4rem;
  border-color: color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 11%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.94));
}

.product-model-designer__overview-lead strong,
.product-model-designer__overview-metric strong,
.product-model-designer__candidate-evidence-card strong,
.product-model-designer__confirm-metrics strong,
.product-model-designer__card-heading strong,
.product-model-designer__candidate-card-title strong {
  color: var(--text-heading);
}

.product-model-designer__overview-lead strong {
  font-size: 1.2rem;
}

.product-model-designer__overview-metric {
  display: grid;
  gap: 0.3rem;
}

.product-model-designer__overview-metric strong {
  font-size: 1.16rem;
}

.product-model-designer__overview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: var(--text-caption-2);
  font-size: 0.83rem;
}

.product-model-designer__summary-label {
  color: color-mix(in srgb, var(--brand) 58%, var(--text-caption));
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.product-model-designer__candidate-workspace {
  display: grid;
  grid-template-columns: minmax(12rem, 13rem) minmax(0, 1fr) minmax(16rem, 18rem);
  gap: 1rem;
  align-items: start;
}

.product-model-designer__candidate-nav-header,
.product-model-designer__candidate-body-header,
.product-model-designer__candidate-rail-header,
.product-model-designer__card-heading,
.product-model-designer__candidate-card-title {
  display: grid;
  gap: 0.28rem;
}

.product-model-designer__candidate-nav-header h3,
.product-model-designer__candidate-body-header h3,
.product-model-designer__candidate-rail-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
}

.product-model-designer__candidate-nav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
  width: 100%;
  padding: 0.84rem 0.92rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.product-model-designer__candidate-nav-item strong,
.product-model-designer__formal-overview-card strong {
  color: var(--text-heading);
}

.product-model-designer__candidate-nav-item--active {
  border-color: color-mix(in srgb, var(--brand) 34%, white);
  background:
    linear-gradient(180deg, rgba(245, 248, 255, 0.98), rgba(237, 244, 255, 0.96)),
    linear-gradient(90deg, color-mix(in srgb, var(--brand) 24%, transparent), transparent);
  color: var(--brand);
  box-shadow: var(--shadow-brand);
  transform: translateY(-1px);
}

.product-model-designer__candidate-nav-tip {
  margin: 0;
  padding: 0.8rem 0.88rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 20%, white);
  border-radius: var(--radius-lg);
  background: rgba(246, 250, 255, 0.94);
  color: var(--text-caption);
  font-size: 0.84rem;
  line-height: 1.55;
}

.product-model-designer__candidate-body-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.9rem;
}

.product-model-designer__candidate-body-meta {
  color: var(--text-caption);
  font-size: 0.85rem;
}

.product-model-designer__candidate-list,
.product-model-designer__list {
  display: grid;
  gap: 0.9rem;
}

.product-model-designer__card-surface {
  padding: 1rem 1.05rem;
}

.product-model-designer__candidate-card {
  border-color: color-mix(in srgb, var(--brand) 16%, var(--panel-border));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 7%, transparent), transparent 44%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(246, 250, 255, 0.96));
}

.product-model-designer__candidate-card-header,
.product-model-designer__card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-model-designer__candidate-card-title strong,
.product-model-designer__card-heading strong {
  font-size: 1rem;
}

.product-model-designer__candidate-card-title span,
.product-model-designer__card-heading span {
  color: var(--text-caption);
  font-size: 0.9rem;
}

.product-model-designer__candidate-card-check {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--text-secondary);
  white-space: nowrap;
}

.product-model-designer__candidate-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.product-model-designer__candidate-card-summary,
.product-model-designer__card-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  padding: 0.75rem 0;
  border-top: 1px solid color-mix(in srgb, var(--panel-border) 78%, transparent);
  border-bottom: 1px solid color-mix(in srgb, var(--panel-border) 78%, transparent);
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.product-model-designer__candidate-card-editor {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(12rem, 0.78fr);
  gap: 0.75rem;
}

.product-model-designer__candidate-field {
  display: grid;
  gap: 0.36rem;
}

.product-model-designer__candidate-field span,
.product-model-designer__candidate-evidence-card span,
.product-model-designer__confirm-metrics span,
.product-model-designer__formal-overview-card span {
  color: var(--text-caption-2);
  font-size: 0.78rem;
  font-weight: 600;
}

.product-model-designer__candidate-field--full {
  grid-column: 1 / -1;
}

.product-model-designer__candidate-evidence-card,
.product-model-designer__confirm-metrics div {
  display: grid;
  gap: 0.2rem;
  padding: 0.8rem 0.88rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(252, 253, 255, 0.96);
}

.product-model-designer__candidate-evidence-card strong {
  font-size: 0.98rem;
}

.product-model-designer__candidate-evidence-card small {
  color: var(--text-caption);
  line-height: 1.55;
}

.product-model-designer__confirm-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
}

.product-model-designer__confirm-metrics strong {
  font-size: 1.12rem;
}

.product-model-designer__confirm-hint {
  margin: 0;
  padding: 0.82rem 0.9rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 24%, white);
  border-radius: var(--radius-lg);
  background: rgba(246, 250, 255, 0.94);
  color: var(--text-caption);
  line-height: 1.55;
}

.product-model-designer__formal-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.product-model-designer__formal-overview-card {
  display: grid;
  gap: 0.28rem;
  padding: 0.95rem 1rem;
  border: 1px solid var(--panel-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 255, 0.94));
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.product-model-designer__formal-overview-card strong {
  font-size: 1.12rem;
}

.product-model-designer__formal-overview-card small {
  color: var(--text-caption);
  line-height: 1.55;
}

.product-model-designer__formal-overview-card--active {
  border-color: color-mix(in srgb, var(--brand) 38%, white);
  background:
    linear-gradient(180deg, rgba(244, 248, 255, 0.98), rgba(238, 245, 255, 0.96)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 10%, transparent), transparent 45%);
  box-shadow: var(--shadow-brand);
  color: var(--brand);
  transform: translateY(-1px);
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

@media (max-width: 1200px) {
  .product-model-designer__overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-model-designer__candidate-workspace {
    grid-template-columns: minmax(11rem, 12rem) minmax(0, 1fr);
  }

  .product-model-designer__candidate-rail {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .product-model-designer__hero-stage,
  .product-model-designer__overview,
  .product-model-designer__candidate-workspace,
  .product-model-designer__candidate-card-editor,
  .product-model-designer__formal-overview,
  .product-model-designer__confirm-metrics {
    grid-template-columns: 1fr;
  }

  .product-model-designer__hero-actions {
    justify-items: start;
  }

  .product-model-designer__candidate-body-header,
  .product-model-designer__candidate-card-header,
  .product-model-designer__card-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
