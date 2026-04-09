<template>
  <div class="product-model-designer-workspace">
    <div v-if="!product" class="product-model-designer__empty">
      <strong>请先选择产品</strong>
      <p>需要先选中产品，才能继续查看契约字段。</p>
    </div>

    <div v-else-if="loading" class="detail-notice">
      <span class="detail-notice__label">加载中</span>
      <strong class="detail-notice__value">正在加载契约字段...</strong>
    </div>

    <div v-else-if="loadErrorMessage" class="detail-notice detail-notice--danger">
      <span class="detail-notice__label">加载失败</span>
      <strong class="detail-notice__value">{{ loadErrorMessage }}</strong>
    </div>

    <template v-else>
      <section class="product-model-designer__summary-sheet">
        <div class="product-model-designer__summary-copy">
          <span class="product-model-designer__summary-kicker">契约字段</span>
          <h3 class="product-model-designer__summary-title">基于现有上报手动提炼契约字段</h3>
          <p class="product-model-designer__summary-description">
            在同一页面完成样本录入、字段提取、结果确认和正式字段查看，不再打开二层抽屉。
          </p>
        </div>

        <div class="product-model-designer__summary-actions">
          <StandardButton
            action="confirm"
            data-testid="start-contract-field"
            @click="focusSampleStage"
          >
            {{ entryActionText }}
          </StandardButton>
          <StandardButton
            action="delete"
            data-testid="contract-field-rollback-submit"
            :loading="rollbackLoading"
            :disabled="!canRollbackCurrentBatch"
            @click="handleRollbackCurrentBatch"
          >
            提交回滚审批
          </StandardButton>
        </div>

        <div class="product-model-designer__summary-grid">
          <article class="product-model-designer__summary-card">
            <span>已生效</span>
            <strong>{{ models.length }}</strong>
          </article>
          <article class="product-model-designer__summary-card">
            <span>本次识别</span>
            <strong>{{ compareRows.length }}</strong>
          </article>
          <article class="product-model-designer__summary-card">
            <span>待生效</span>
            <strong>{{ selectedApplyItems.length }}</strong>
          </article>
        </div>
      </section>

      <section
        ref="sampleStageRef"
        class="product-model-designer__stage"
        data-testid="contract-field-sample-stage"
      >
        <header class="product-model-designer__stage-head">
          <div>
            <h3>样本输入</h3>
            <p>只支持基于手动粘贴的上报 JSON 提取契约字段。</p>
          </div>
        </header>

        <div class="product-model-designer__sample-toolbar">
          <div class="product-model-designer__field-group">
            <span class="product-model-designer__field-label">样本类型</span>
            <div class="product-model-designer__choice-group" role="tablist" aria-label="样本类型">
              <button
                v-for="option in sampleTypeOptions"
                :key="option.value"
                type="button"
                class="product-model-designer__choice-button"
                :class="{ 'product-model-designer__choice-button--active': sampleType === option.value }"
                :data-testid="`sample-type-${option.value}`"
                @click="sampleType = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div class="product-model-designer__field-group">
            <span class="product-model-designer__field-label">设备结构</span>
            <div class="product-model-designer__choice-group" role="tablist" aria-label="设备结构">
              <button
                v-for="option in deviceStructureOptions"
                :key="option.value"
                type="button"
                class="product-model-designer__choice-button"
                :class="{ 'product-model-designer__choice-button--active': deviceStructure === option.value }"
                :data-testid="`device-structure-${option.value}`"
                @click="handleDeviceStructureChange(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div class="product-model-designer__toolbar-actions">
            <StandardButton action="query" @click="formatSamplePayload">格式化 JSON</StandardButton>
            <StandardButton
              action="confirm"
              :loading="compareLoading"
              data-testid="contract-field-compare-submit"
              @click="handleCompare"
            >
              提取契约字段
            </StandardButton>
          </div>
        </div>

        <section
          v-if="deviceStructure === 'composite'"
          class="product-model-designer__relation-stage"
        >
          <div class="product-model-designer__relation-head">
            <div>
              <strong>复合设备关系映射</strong>
              <p>当前页只暴露父设备编码、逻辑通道编码和子设备编码，其他归一策略按内部固定口径处理。</p>
            </div>
            <StandardButton action="query" :loading="relationLoading" @click="handleLoadRelations">
              读取已有关系
            </StandardButton>
          </div>

          <div class="product-model-designer__relation-grid">
            <label class="product-model-designer__input-field">
              <span>父设备编码</span>
              <ElInput
                v-model="parentDeviceCode"
                data-testid="composite-parent-device-code"
                placeholder="请输入父设备编码，如 SK00EA0D1307986"
              />
            </label>
          </div>

          <div class="product-model-designer__relation-list">
            <div
              v-for="row in relationMappings"
              :key="row.key"
              class="product-model-designer__relation-row"
            >
              <ElInput
                v-model="row.logicalChannelCode"
                :data-testid="`relation-logical-${row.key}`"
                placeholder="逻辑通道编码"
              />
              <ElInput
                v-model="row.childDeviceCode"
                :data-testid="`relation-child-${row.key}`"
                placeholder="子设备编码"
              />
              <button
                type="button"
                class="product-model-designer__row-action"
                :disabled="relationMappings.length === 1"
                @click="removeRelationRow(row.key)"
              >
                删除
              </button>
            </div>
          </div>

          <div class="product-model-designer__relation-actions">
            <StandardButton action="add" @click="addRelationRow">新增映射</StandardButton>
          </div>
        </section>

        <div class="product-model-designer__payload-stage">
          <label class="product-model-designer__input-field">
            <span>上报 JSON</span>
            <ElInput
              v-model="samplePayload"
              type="textarea"
              :rows="12"
              data-testid="contract-field-sample-input"
              :placeholder="samplePayloadPlaceholder"
              @blur="tryAutoFormatSamplePayload"
            />
          </label>
          <p v-if="samplePayloadError" class="product-model-designer__input-error">
            {{ samplePayloadError }}
          </p>
        </div>
      </section>

      <section class="product-model-designer__stage">
        <header class="product-model-designer__stage-head">
          <div>
            <h3>识别结果</h3>
            <p>当前只展示基于手动样本识别出的 compare 结果。</p>
          </div>
        </header>

        <ProductModelGovernanceCompareTable
          v-if="compareRows.length"
          :rows="compareRows"
          :decision-state="decisionState"
          @change-decision="handleDecisionChange"
        />

        <div v-else class="product-model-designer__empty">
          <strong>暂无识别结果</strong>
          <p>贴上报数据并完成提取后，这里会展示本次识别出的字段。</p>
        </div>
      </section>

      <section class="product-model-designer__stage">
        <header class="product-model-designer__stage-head">
          <div>
            <h3>本次生效</h3>
            <p>确认后将把当前选中的字段写入正式字段。</p>
          </div>
        </header>

        <div v-if="selectedApplyEntries.length" class="product-model-designer__apply-list">
          <article
            v-for="entry in selectedApplyEntries"
            :key="entry.key"
            class="product-model-designer__apply-card"
          >
            <div class="product-model-designer__apply-card-head">
              <strong>{{ entry.item.modelName }}</strong>
              <span>{{ applyDecisionLabel(entry.decision) }}</span>
            </div>
            <div class="product-model-designer__apply-card-meta">
              <span>{{ entry.item.modelType }}</span>
              <span>{{ entry.item.identifier }}</span>
              <span>{{ compareStatusLabel(entry.row.compareStatus) }}</span>
            </div>
            <p>{{ applyEvidenceSummary(entry.row) }}</p>
          </article>
        </div>

        <div v-else class="product-model-designer__empty">
          <strong>当前还没有待生效字段</strong>
          <p>请先在识别结果中选择要纳入的字段。</p>
        </div>

        <div class="product-model-designer__apply-footer">
          <p>{{ footerSummaryText }}</p>
          <label class="product-model-designer__input-field product-model-designer__approver-field">
            <span>复核人用户 ID</span>
            <ElInput
              v-model="governanceApproverId"
              data-testid="governance-approver-id"
              placeholder="发布或回滚关键动作都需要填写复核人 ID"
            />
          </label>
          <StandardButton
            action="confirm"
            :loading="applyLoading"
            :disabled="!selectedApplyItems.length"
            @click="handleApply"
          >
            确认并提交审批
          </StandardButton>
        </div>

        <div
          v-if="rollbackResult"
          class="product-model-designer__approval-inline"
        >
          <p
            class="product-model-designer__input-error"
            data-testid="contract-field-rollback-receipt"
          >
            {{ rollbackReceiptText }}
          </p>
          <div class="product-model-designer__approval-actions">
            <StandardButton
              v-if="rollbackApprovalOrderId"
              action="query"
              :loading="rollbackApprovalLoading"
              @click="refreshRollbackApprovalDetail"
            >
              刷新审批状态
            </StandardButton>
            <StandardButton
              v-if="canResubmitRollbackApproval"
              action="confirm"
              :loading="rollbackResubmitLoading"
              data-testid="contract-field-rollback-resubmit"
              @click="handleResubmitRollbackApproval"
            >
              原单重提
            </StandardButton>
          </div>
        </div>

        <div
          v-if="rollbackResult"
          class="product-model-designer__receipt"
          data-testid="contract-field-rollback-status"
        >
          <article class="product-model-designer__summary-card">
            <span>审批状态</span>
            <strong>{{ approvalStatusLabel(rollbackReceiptStatus) }}</strong>
          </article>
          <article
            v-if="rollbackApprovalOrderId"
            class="product-model-designer__summary-card"
          >
            <span>审批单</span>
            <strong>{{ rollbackApprovalOrderId }}</strong>
          </article>
          <article
            v-if="rollbackApprovalComment"
            class="product-model-designer__summary-card"
          >
            <span>审批意见</span>
            <strong>{{ rollbackApprovalComment }}</strong>
          </article>
          <article
            v-if="rollbackExecutionTimeText"
            class="product-model-designer__summary-card"
          >
            <span>执行时间</span>
            <strong>{{ rollbackExecutionTimeText }}</strong>
          </article>
        </div>

        <div
          v-if="applyResult"
          class="product-model-designer__receipt"
          data-testid="contract-field-apply-receipt"
        >
          <article class="product-model-designer__summary-card">
            <span>{{ applyCreatedLabel }}</span>
            <strong>{{ applyReceiptCounts.created }}</strong>
          </article>
          <article class="product-model-designer__summary-card">
            <span>{{ applyUpdatedLabel }}</span>
            <strong>{{ applyReceiptCounts.updated }}</strong>
          </article>
          <article class="product-model-designer__summary-card">
            <span>{{ applySkippedLabel }}</span>
            <strong>{{ applyReceiptCounts.skipped }}</strong>
          </article>
          <article
            v-if="applyReceiptIdentifierValue"
            class="product-model-designer__summary-card"
          >
            <span>{{ applyReceiptIdentifierLabel }}</span>
            <strong>{{ applyReceiptIdentifierValue }}</strong>
          </article>
        </div>

        <section
          v-if="applyResult"
          class="product-model-designer__approval-stage"
          data-testid="contract-field-apply-approval-status"
        >
          <div class="product-model-designer__approval-head">
            <div>
              <strong>审批跟踪</strong>
              <p>{{ applyApprovalSummaryText }}</p>
            </div>
            <div class="product-model-designer__approval-actions">
              <StandardButton
                v-if="applyApprovalOrderId"
                action="query"
                :loading="applyApprovalLoading"
                @click="refreshApplyApprovalDetail"
              >
                刷新审批状态
              </StandardButton>
              <StandardButton
                v-if="canResubmitApplyApproval"
                action="confirm"
                :loading="applyResubmitLoading"
                data-testid="contract-field-apply-resubmit"
                @click="handleResubmitApplyApproval"
              >
                原单重提
              </StandardButton>
              <StandardButton
                v-if="applyReceiptStatus === 'REJECTED'"
                action="query"
                data-testid="contract-field-apply-reset"
                @click="prepareNewApplyApproval"
              >
                修改内容后新建审批
              </StandardButton>
            </div>
          </div>

          <div class="product-model-designer__receipt">
            <article class="product-model-designer__summary-card">
              <span>审批状态</span>
              <strong>{{ approvalStatusLabel(applyReceiptStatus) }}</strong>
            </article>
            <article
              v-if="applyApprovalOrderId"
              class="product-model-designer__summary-card"
            >
              <span>审批单</span>
              <strong>{{ applyApprovalOrderId }}</strong>
            </article>
            <article
              v-if="applyApprovalComment"
              class="product-model-designer__summary-card"
            >
              <span>审批意见</span>
              <strong>{{ applyApprovalComment }}</strong>
            </article>
            <article
              v-if="applyExecutionTimeText"
              class="product-model-designer__summary-card"
            >
              <span>执行时间</span>
              <strong>{{ applyExecutionTimeText }}</strong>
            </article>
          </div>
        </section>
      </section>

      <section class="product-model-designer__stage">
        <header class="product-model-designer__stage-head">
          <div>
            <h3>当前已生效字段</h3>
            <p>这里只展示当前已经正式生效的字段。</p>
          </div>
        </header>

        <div class="product-model-designer__formal-tabs" role="tablist" aria-label="正式字段类型">
          <button
            v-for="item in typeOptions"
            :key="item.value"
            type="button"
            class="product-model-designer__formal-tab"
            :class="{ 'product-model-designer__formal-tab--active': activeType === item.value }"
            @click="activeType = item.value"
          >
            <span>{{ item.label }}</span>
            <strong>{{ countByType(item.value) }}</strong>
          </button>
        </div>

        <div v-if="activeModels.length" class="product-model-designer__formal-list">
          <article v-for="model in activeModels" :key="String(model.id)" class="product-model-designer__formal-card">
            <div class="product-model-designer__formal-card-head">
              <div v-if="isRenamingModel(model)" class="product-model-designer__formal-rename">
                <ElInput
                  :model-value="renamingModelName"
                  :data-testid="`formal-model-name-input-${model.id}`"
                  placeholder="请输入正式中文名称"
                  @update:model-value="(value) => renamingModelName = typeof value === 'string' ? value : ''"
                />
                <div class="product-model-designer__formal-rename-actions">
                  <StandardButton
                    :data-testid="`formal-model-name-save-${model.id}`"
                    action="confirm"
                    :loading="renameSubmitting"
                    @click="handleRenameModel(model)"
                  >
                    保存
                  </StandardButton>
                  <StandardButton action="cancel" @click="cancelRenameModel">取消</StandardButton>
                </div>
              </div>
              <div v-else class="product-model-designer__formal-title">
                <strong>{{ model.modelName }}</strong>
                <span>{{ model.identifier }}</span>
              </div>
              <StandardButton
                v-if="model.id !== undefined && model.id !== null && !isRenamingModel(model)"
                :data-testid="`formal-model-rename-${model.id}`"
                action="query"
                link
                @click="startRenameModel(model)"
              >
                改名
              </StandardButton>
            </div>
            <div class="product-model-designer__formal-card-meta">
              <span>{{ model.modelType }}</span>
              <span>{{ model.dataType || model.eventType || formatServiceSummary(model) || '--' }}</span>
              <span>排序 {{ model.sortNo ?? '--' }}</span>
            </div>
            <div v-if="model.modelType === 'property'" class="product-model-designer__formal-card-actions">
              <span class="product-model-designer__formal-card-state">
                {{ resolveTrendMetricStateLabel(model) }}
              </span>
              <StandardButton
                v-if="model.id !== undefined && model.id !== null"
                :data-testid="`formal-model-trend-measure-${model.id}`"
                action="query"
                link
                :loading="isTrendMetricSubmitting(model.id, 'measure')"
                :disabled="trendMetricSubmitting"
                @click="handleSetTrendMetric(model, 'measure')"
              >
                设为监测趋势
              </StandardButton>
              <StandardButton
                v-if="model.id !== undefined && model.id !== null"
                :data-testid="`formal-model-trend-status-${model.id}`"
                action="query"
                link
                :loading="isTrendMetricSubmitting(model.id, 'status')"
                :disabled="trendMetricSubmitting"
                @click="handleSetTrendMetric(model, 'status')"
              >
                设为状态趋势
              </StandardButton>
              <StandardButton
                v-if="model.id !== undefined && model.id !== null && resolveTrendMetricConfig(model)"
                :data-testid="`formal-model-trend-remove-${model.id}`"
                action="delete"
                link
                :loading="isTrendMetricSubmitting(model.id, 'remove')"
                :disabled="trendMetricSubmitting"
                @click="handleRemoveTrendMetric(model)"
              >
                取消趋势展示
              </StandardButton>
            </div>
            <p>{{ model.description?.trim() || emptyDescriptionMap[model.modelType] }}</p>
          </article>
        </div>

        <div v-else class="product-model-designer__empty">
          <strong>暂无物模型</strong>
          <p>{{ emptyDescriptionMap[activeType] }}</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

import StandardButton from '@/components/StandardButton.vue'
import ProductModelGovernanceCompareTable from '@/components/product/ProductModelGovernanceCompareTable.vue'
import { deviceApi } from '@/api/device'
import { governanceApprovalApi } from '@/api/governanceApproval'
import { productApi, type ProductContractReleaseRollbackResult } from '@/api/product'
import type {
  GovernanceApprovalOrderDetail,
  GovernanceApprovalStatus,
  IdType,
  Product,
  ProductAddPayload,
  ProductModel,
  ProductModelGovernanceApplyItem,
  ProductModelGovernanceApplyResult,
  ProductModelGovernanceCompareResult,
  ProductModelGovernanceCompareRow,
  ProductModelGovernanceDecision,
  ProductModelType
} from '@/types/api'
import { ElMessage } from '@/utils/message'
import {
  buildProductMetadataJson,
  createProductObjectInsightMetricFromModel,
  findProductObjectInsightMetric,
  parseProductObjectInsightMetrics,
  removeProductObjectInsightMetric,
  upsertProductObjectInsightMetric
} from '@/utils/productObjectInsightConfig'

type GovernanceDecisionUi = ProductModelGovernanceDecision | 'observe' | 'review' | 'ignore'
type SampleType = 'business' | 'status'
type DeviceStructure = 'single' | 'composite'

interface RelationMappingRow {
  key: string
  logicalChannelCode: string
  childDeviceCode: string
}

interface GovernanceApprovalPayloadExecution<TResult> {
  executedAt?: string | null
  result?: TResult | null
}

interface GovernanceApprovalPayload<TResult, TRequest = unknown> {
  version?: number | null
  request?: TRequest | null
  execution?: GovernanceApprovalPayloadExecution<TResult> | null
}

const props = defineProps<{
  product: Product | null
}>()

const emit = defineEmits<{
  (event: 'product-updated', value: Product): void
}>()

const typeOptions: Array<{ label: string; value: ProductModelType }> = [
  { label: '属性', value: 'property' },
  { label: '事件', value: 'event' },
  { label: '服务', value: 'service' }
]

const sampleTypeOptions: Array<{ label: string; value: SampleType }> = [
  { label: '业务数据', value: 'business' },
  { label: '状态数据', value: 'status' }
]

const deviceStructureOptions: Array<{ label: string; value: DeviceStructure }> = [
  { label: '单台设备', value: 'single' },
  { label: '复合设备', value: 'composite' }
]

const emptyDescriptionMap: Record<ProductModelType, string> = {
  property: '当前还没有属性字段。',
  event: '当前还没有事件字段。',
  service: '当前还没有服务字段。'
}

const loading = ref(false)
const compareLoading = ref(false)
const applyLoading = ref(false)
const rollbackLoading = ref(false)
const relationLoading = ref(false)
const loadErrorMessage = ref('')
const samplePayloadError = ref('')
const models = ref<ProductModel[]>([])
const compareResult = ref<ProductModelGovernanceCompareResult | null>(null)
const applyResult = ref<ProductModelGovernanceApplyResult | null>(null)
const rollbackResult = ref<ProductContractReleaseRollbackResult | null>(null)
const applyApprovalDetail = ref<GovernanceApprovalOrderDetail | null>(null)
const rollbackApprovalDetail = ref<GovernanceApprovalOrderDetail | null>(null)
const governanceApproverId = ref('')
const latestReleaseBatchId = ref<string | number | null>(null)
const decisionState = ref<Record<string, GovernanceDecisionUi>>({})
const sampleType = ref<SampleType>('business')
const deviceStructure = ref<DeviceStructure>('single')
const samplePayload = ref('')
const parentDeviceCode = ref('')
const relationMappings = ref<RelationMappingRow[]>([createRelationRow()])
const activeType = ref<ProductModelType>('property')
const sampleStageRef = ref<HTMLElement | null>(null)
const applyApprovalLoading = ref(false)
const rollbackApprovalLoading = ref(false)
const applyResubmitLoading = ref(false)
const rollbackResubmitLoading = ref(false)
const renamingModelId = ref<IdType | null>(null)
const renamingModelName = ref('')
const renameSubmitting = ref(false)
const productSnapshot = ref<Product | null>(null)
const trendMetricSubmitting = ref(false)
const trendMetricSubmittingKey = ref('')

const compareRows = computed<ProductModelGovernanceCompareRow[]>(() => compareResult.value?.compareRows ?? [])
const activeModels = computed(() => models.value.filter((model) => model.modelType === activeType.value))
const selectedApplyEntries = computed(() =>
  compareRows.value
    .map((row) => ({ row, decision: decisionState.value[rowKey(row)] }))
    .filter((item): item is { row: ProductModelGovernanceCompareRow; decision: ProductModelGovernanceDecision } =>
      item.decision === 'create' || item.decision === 'update'
    )
    .map(({ row, decision }) => ({
      key: rowKey(row),
      row,
      decision,
      item: buildApplyItem(row, decision)
    }))
)
const selectedApplyItems = computed<ProductModelGovernanceApplyItem[]>(() => selectedApplyEntries.value.map((entry) => entry.item))
const canRollbackCurrentBatch = computed(() =>
  Boolean(latestReleaseBatchId.value) && !applyLoading.value && !rollbackLoading.value && !rollbackApprovalLoading.value
)
const entryActionText = computed(() => (models.value.length ? '继续核对字段' : '开始补齐契约'))
const footerSummaryText = computed(() => {
  if (selectedApplyItems.value.length) {
    return `已选 ${selectedApplyItems.value.length} 项，确认后将提交审批`
  }
  if (compareRows.value.length) {
    return `已识别 ${compareRows.value.length} 个字段，请选择需要生效的项`
  }
  return '贴上报数据后，系统会提取契约字段'
})
const applyApprovalOrderId = computed<IdType | null>(() =>
  applyApprovalDetail.value?.order?.id ?? applyResult.value?.approvalOrderId ?? null
)
const rollbackApprovalOrderId = computed<IdType | null>(() =>
  rollbackApprovalDetail.value?.order?.id ?? rollbackResult.value?.approvalOrderId ?? null
)
const applyReceiptStatus = computed<GovernanceApprovalStatus | null>(() =>
  applyApprovalDetail.value?.order?.status ?? applyResult.value?.approvalStatus ?? null
)
const rollbackReceiptStatus = computed<GovernanceApprovalStatus | null>(() =>
  rollbackApprovalDetail.value?.order?.status ?? rollbackResult.value?.approvalStatus ?? null
)
const applyApprovalPayload = computed(() =>
  parseApprovalPayload<ProductModelGovernanceApplyResult>(applyApprovalDetail.value?.order?.payloadJson)
)
const rollbackApprovalPayload = computed(() =>
  parseApprovalPayload<ProductContractReleaseRollbackResult>(rollbackApprovalDetail.value?.order?.payloadJson)
)
const applyExecutedResult = computed(() => applyApprovalPayload.value?.execution?.result ?? null)
const rollbackExecutedResult = computed(() => rollbackApprovalPayload.value?.execution?.result ?? null)
const applyExecutionCompleted = computed(() =>
  applyReceiptStatus.value === 'APPROVED' && Boolean(applyExecutedResult.value)
)
const rollbackExecutionCompleted = computed(() =>
  rollbackReceiptStatus.value === 'APPROVED' && Boolean(rollbackExecutedResult.value)
)
const applyReceiptCounts = computed(() => ({
  created: applyExecutedResult.value?.createdCount ?? applyResult.value?.createdCount ?? 0,
  updated: applyExecutedResult.value?.updatedCount ?? applyResult.value?.updatedCount ?? 0,
  skipped: applyExecutedResult.value?.skippedCount ?? applyResult.value?.skippedCount ?? 0
}))
const applyCreatedLabel = computed(() => (applyExecutionCompleted.value ? '本次新增生效' : '本次申请新增'))
const applyUpdatedLabel = computed(() => (applyExecutionCompleted.value ? '本次修订生效' : '本次申请修订'))
const applySkippedLabel = computed(() => (applyExecutionCompleted.value ? '本次暂不生效' : '本次申请暂不生效'))
const applyReceiptIdentifierLabel = computed(() =>
  applyExecutionCompleted.value && applyExecutedResult.value?.releaseBatchId !== undefined && applyExecutedResult.value?.releaseBatchId !== null
    ? '发布批次'
    : '审批单'
)
const applyReceiptIdentifierValue = computed(() => {
  if (!applyResult.value && !applyApprovalOrderId.value) {
    return null
  }
  return applyReceiptIdentifierLabel.value === '发布批次'
    ? (applyExecutedResult.value?.releaseBatchId ?? applyResult.value?.releaseBatchId ?? null)
    : applyApprovalOrderId.value
})
const applyApprovalComment = computed(() =>
  resolveApprovalComment(applyApprovalDetail.value, applyReceiptStatus.value)
)
const rollbackApprovalComment = computed(() =>
  resolveApprovalComment(rollbackApprovalDetail.value, rollbackReceiptStatus.value)
)
const canResubmitApplyApproval = computed(() =>
  applyReceiptStatus.value === 'REJECTED' && Boolean(applyApprovalOrderId.value) && !applyResubmitLoading.value
)
const canResubmitRollbackApproval = computed(() =>
  rollbackReceiptStatus.value === 'REJECTED' && Boolean(rollbackApprovalOrderId.value) && !rollbackResubmitLoading.value
)
const applyExecutionTimeText = computed(() => applyApprovalPayload.value?.execution?.executedAt ?? null)
const rollbackExecutionTimeText = computed(() =>
  rollbackApprovalPayload.value?.execution?.executedAt ?? rollbackExecutedResult.value?.rollbackTime ?? null
)
const applyApprovalSummaryText = computed(() => {
  const approvalOrderId = applyApprovalOrderId.value
  switch (applyReceiptStatus.value) {
    case 'APPROVED':
      return applyExecutedResult.value?.releaseBatchId !== undefined && applyExecutedResult.value?.releaseBatchId !== null
        ? `审批通过后已生成发布批次 ${applyExecutedResult.value.releaseBatchId}`
        : `审批单 ${approvalOrderId ?? '--'} 已通过`
    case 'REJECTED':
      return `审批单 ${approvalOrderId ?? '--'} 已驳回，可修正后重新提交`
    case 'CANCELLED':
      return `审批单 ${approvalOrderId ?? '--'} 已撤销`
    case 'PENDING':
      return `审批单 ${approvalOrderId ?? '--'} 已提交，待复核人处理`
    default:
      return '审批单已提交，等待状态同步'
  }
})
const rollbackReceiptText = computed(() => {
  if (!rollbackResult.value) {
    return ''
  }
  const targetBatchId = rollbackExecutedResult.value?.targetBatchId
    ?? rollbackResult.value.targetBatchId
    ?? rollbackResult.value.rolledBackBatchId
    ?? '--'
  if (rollbackExecutionCompleted.value) {
    return `回滚审批已通过，已回滚批次 ${rollbackExecutedResult.value?.rolledBackBatchId ?? targetBatchId}，恢复字段 ${rollbackExecutedResult.value?.restoredFieldCount ?? 0} 项`
  }
  if (rollbackReceiptStatus.value === 'REJECTED') {
    return rollbackApprovalComment.value
      ? `批次 ${targetBatchId} 的回滚审批已驳回：${rollbackApprovalComment.value}`
      : `批次 ${targetBatchId} 的回滚审批已驳回`
  }
  if (rollbackReceiptStatus.value === 'CANCELLED') {
    return `批次 ${targetBatchId} 的回滚审批已撤销`
  }
  return `批次 ${targetBatchId} 的回滚审批已提交，审批单 ${rollbackApprovalOrderId.value ?? '--'}`
})
const samplePayloadPlaceholder = computed(() =>
  deviceStructure.value === 'composite'
    ? '请粘贴单台父设备的复合上报 JSON，例如 {"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:14:06.000Z":10.86}}}'
    : '请粘贴单台设备的上报 JSON，例如 {"device-001":{"temperature":{"2026-04-05T20:14:06.000Z":26.5}}}'
)
const trendMetricRows = computed(() =>
  parseProductObjectInsightMetrics(productSnapshot.value?.metadataJson ?? props.product?.metadataJson)
)

watch(
  () => props.product,
  (product) => {
    productSnapshot.value = product ? { ...product } : null
  },
  { immediate: true, deep: true }
)

watch(
  () => props.product?.id,
  async (productId, previousProductId) => {
    if (!productId) {
      models.value = []
      loadErrorMessage.value = ''
      resetSession()
      return
    }
    if (productId !== previousProductId) {
      resetSession()
    }
    await loadModels(productId)
  },
  { immediate: true }
)

function createRelationRow(): RelationMappingRow {
  return {
    key: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    logicalChannelCode: '',
    childDeviceCode: ''
  }
}

function rowKey(row: ProductModelGovernanceCompareRow) {
  return `${row.modelType}:${row.identifier}`
}

function countByType(type: ProductModelType) {
  return models.value.filter((model) => model.modelType === type).length
}

function formatServiceSummary(model: ProductModel) {
  const hasInput = Boolean(model.serviceInputJson?.trim())
  const hasOutput = Boolean(model.serviceOutputJson?.trim())
  if (hasInput && hasOutput) return '已配置'
  if (hasInput) return '仅输入'
  if (hasOutput) return '仅输出'
  return ''
}

function isRenamingModel(model: ProductModel) {
  return model.id !== undefined && model.id !== null && String(renamingModelId.value) === String(model.id)
}

function resolveTrendMetricConfig(model: ProductModel) {
  return findProductObjectInsightMetric(trendMetricRows.value, model.identifier)
}

function resolveTrendMetricStateLabel(model: ProductModel) {
  const metric = resolveTrendMetricConfig(model)
  if (!metric || metric.includeInTrend === false || metric.enabled === false) {
    return '当前未加入对象洞察趋势'
  }
  return metric.group === 'measure' ? '当前为监测趋势重点' : '当前为状态趋势重点'
}

function isTrendMetricSubmitting(modelId: IdType, group: 'measure' | 'status' | 'remove') {
  return trendMetricSubmittingKey.value === `${String(modelId)}:${group}`
}

async function loadModels(productId: string | number) {
  loading.value = true
  loadErrorMessage.value = ''
  try {
    const [modelResponse, releaseResponse] = await Promise.all([
      productApi.listProductModels(productId),
      productApi.pageProductContractReleaseBatches(productId, { pageNum: 1, pageSize: 1 })
    ])
    models.value = modelResponse.data ?? []
    latestReleaseBatchId.value = releaseResponse.data?.records?.[0]?.id ?? null
  } catch (error) {
    models.value = []
    latestReleaseBatchId.value = null
    loadErrorMessage.value = error instanceof Error ? error.message : '加载产品物模型失败'
  } finally {
    loading.value = false
  }
}

async function loadApprovalOrderDetail(
  orderId: IdType | null | undefined,
  target: 'apply' | 'rollback',
  options: { silent?: boolean } = {}
) {
  if (orderId === undefined || orderId === null || orderId === '') {
    return
  }
  const loadingRef = target === 'apply' ? applyApprovalLoading : rollbackApprovalLoading
  const detailRef = target === 'apply' ? applyApprovalDetail : rollbackApprovalDetail
  loadingRef.value = true
  try {
    const response = await governanceApprovalApi.getOrderDetail(orderId)
    detailRef.value = response.data ?? null
  } catch (error) {
    if (options.silent) {
      ElMessage.warning('审批单已提交，但审批状态暂时读取失败，请稍后刷新')
    } else {
      ElMessage.error(error instanceof Error ? error.message : '读取审批状态失败')
    }
  } finally {
    loadingRef.value = false
  }
}

async function refreshApplyApprovalDetail() {
  await loadApprovalOrderDetail(applyApprovalOrderId.value, 'apply')
}

async function refreshRollbackApprovalDetail() {
  await loadApprovalOrderDetail(rollbackApprovalOrderId.value, 'rollback')
}

function startRenameModel(model: ProductModel) {
  renamingModelId.value = model.id ?? null
  renamingModelName.value = model.modelName?.trim() || model.identifier
}

function cancelRenameModel() {
  renamingModelId.value = null
  renamingModelName.value = ''
}

async function handleRenameModel(model: ProductModel) {
  const productId = props.product?.id
  if (productId === undefined || productId === null || model.id === undefined || model.id === null) {
    return
  }
  const nextModelName = renamingModelName.value.trim()
  if (!nextModelName) {
    ElMessage.warning('正式字段名称不能为空')
    return
  }
  renameSubmitting.value = true
  try {
    const response = await productApi.updateProductModel(productId, model.id, {
      modelType: model.modelType,
      identifier: model.identifier,
      modelName: nextModelName,
      dataType: model.dataType || undefined,
      specsJson: model.specsJson || undefined,
      eventType: model.eventType || undefined,
      serviceInputJson: model.serviceInputJson || undefined,
      serviceOutputJson: model.serviceOutputJson || undefined,
      sortNo: model.sortNo ?? undefined,
      requiredFlag: model.requiredFlag ?? undefined,
      description: model.description || undefined
    })
    const updatedModel = response.data ?? {
      ...model,
      modelName: nextModelName
    }
    models.value = models.value.map((item) =>
      String(item.id) === String(model.id)
        ? {
            ...item,
            ...updatedModel
          }
        : item
    )
    ElMessage.success('正式字段名称已更新')
    cancelRenameModel()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新正式字段名称失败')
  } finally {
    renameSubmitting.value = false
  }
}

async function handleSetTrendMetric(model: ProductModel, group: 'measure' | 'status') {
  const product = productSnapshot.value ?? props.product
  if (!product?.id) {
    return
  }

  const nextRows = upsertProductObjectInsightMetric(
    trendMetricRows.value,
    createProductObjectInsightMetricFromModel(model, group)
  )
  await persistTrendMetricConfig(product, nextRows, `${String(model.id ?? model.identifier)}:${group}`, `${model.modelName}已加入对象洞察趋势`)
}

async function handleRemoveTrendMetric(model: ProductModel) {
  const product = productSnapshot.value ?? props.product
  if (!product?.id) {
    return
  }

  const existingMetric = resolveTrendMetricConfig(model)
  if (!existingMetric) {
    return
  }

  const nextRows = removeProductObjectInsightMetric(trendMetricRows.value, model.identifier)
  await persistTrendMetricConfig(
    product,
    nextRows,
    `${String(model.id ?? model.identifier)}:remove`,
    `${model.modelName}已取消对象洞察趋势`
  )
}

async function persistTrendMetricConfig(
  product: Product,
  rows: ReturnType<typeof parseProductObjectInsightMetrics>,
  submittingKey: string,
  successMessage: string
) {
  trendMetricSubmitting.value = true
  trendMetricSubmittingKey.value = submittingKey
  try {
    const metadataJson = buildProductMetadataJson(rows, product.metadataJson)
    const response = await productApi.updateProduct(product.id, buildProductUpdatePayload(product, metadataJson))
    const updatedProduct: Product = response.data ?? {
      ...product,
      metadataJson: metadataJson ?? null
    }
    productSnapshot.value = updatedProduct
    emit('product-updated', updatedProduct)
    ElMessage.success(successMessage)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新对象洞察趋势配置失败')
  } finally {
    trendMetricSubmitting.value = false
    trendMetricSubmittingKey.value = ''
  }
}

async function handleResubmitApplyApproval() {
  if (!applyApprovalOrderId.value || !canResubmitApplyApproval.value) {
    return
  }
  const approverUserId = resolveApproverUserId()
  if (!approverUserId) {
    return
  }
  applyResubmitLoading.value = true
  try {
    await governanceApprovalApi.resubmitOrder(applyApprovalOrderId.value, {
      approverUserId
    })
    if (applyResult.value) {
      applyResult.value = {
        ...applyResult.value,
        approvalStatus: 'PENDING',
        executionPending: true
      }
    }
    applyApprovalDetail.value = null
    ElMessage.success('审批单已重新提交')
    await loadApprovalOrderDetail(applyApprovalOrderId.value, 'apply')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '原审批单重提失败')
  } finally {
    applyResubmitLoading.value = false
  }
}

async function handleResubmitRollbackApproval() {
  if (!rollbackApprovalOrderId.value || !canResubmitRollbackApproval.value) {
    return
  }
  const approverUserId = resolveApproverUserId()
  if (!approverUserId) {
    return
  }
  rollbackResubmitLoading.value = true
  try {
    await governanceApprovalApi.resubmitOrder(rollbackApprovalOrderId.value, {
      approverUserId
    })
    if (rollbackResult.value) {
      rollbackResult.value = {
        ...rollbackResult.value,
        approvalStatus: 'PENDING',
        executionPending: true
      }
    }
    rollbackApprovalDetail.value = null
    ElMessage.success('回滚审批单已重新提交')
    await loadApprovalOrderDetail(rollbackApprovalOrderId.value, 'rollback')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '回滚审批单重提失败')
  } finally {
    rollbackResubmitLoading.value = false
  }
}

function prepareNewApplyApproval() {
  applyResult.value = null
  applyApprovalDetail.value = null
  applyApprovalLoading.value = false
  applyResubmitLoading.value = false
  ElMessage.success('已退出当前审批回执，请重新提取字段后提交新审批')
  focusSampleStage()
}

function focusSampleStage() {
  nextTick(() => {
    const stage = sampleStageRef.value
    if (stage && typeof stage.scrollIntoView === 'function') {
      stage.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
    const field = stage?.querySelector('textarea, input') as HTMLElement | null
    field?.focus?.()
  })
}

function handleDeviceStructureChange(value: DeviceStructure) {
  deviceStructure.value = value
  samplePayloadError.value = ''
  if (value === 'single') {
    parentDeviceCode.value = ''
    relationMappings.value = [createRelationRow()]
  }
}

function addRelationRow() {
  relationMappings.value = [...relationMappings.value, createRelationRow()]
}

function removeRelationRow(key: string) {
  if (relationMappings.value.length === 1) {
    relationMappings.value = [createRelationRow()]
    return
  }
  relationMappings.value = relationMappings.value.filter((item) => item.key !== key)
}

function normalizeRelationMappings() {
  return relationMappings.value
    .map((item) => ({
      logicalChannelCode: item.logicalChannelCode.trim(),
      childDeviceCode: item.childDeviceCode.trim()
    }))
    .filter((item) => item.logicalChannelCode && item.childDeviceCode)
}

async function handleLoadRelations() {
  const normalizedParentDeviceCode = parentDeviceCode.value.trim()
  if (!normalizedParentDeviceCode) {
    ElMessage.warning('请先填写父设备编码')
    return
  }
  relationLoading.value = true
  samplePayloadError.value = ''
  try {
    const response = await deviceApi.listDeviceRelations(normalizedParentDeviceCode)
    const items = (response.data ?? []).map((item) => ({
      key: `${item.logicalChannelCode}-${item.childDeviceCode}`,
      logicalChannelCode: item.logicalChannelCode,
      childDeviceCode: item.childDeviceCode
    }))
    relationMappings.value = items.length ? items : [createRelationRow()]
  } catch (error) {
    samplePayloadError.value = error instanceof Error ? error.message : '读取设备关系失败'
  } finally {
    relationLoading.value = false
  }
}

function tryAutoFormatSamplePayload() {
  const trimmed = samplePayload.value.trim()
  if (!trimmed) {
    samplePayloadError.value = ''
    return true
  }
  try {
    samplePayload.value = JSON.stringify(JSON.parse(trimmed), null, 2)
    samplePayloadError.value = ''
    return true
  } catch (error) {
    samplePayloadError.value = error instanceof Error ? error.message : 'JSON 格式不正确'
    return false
  }
}

function formatSamplePayload() {
  if (!tryAutoFormatSamplePayload()) {
    ElMessage.warning('当前 JSON 还不能格式化，请先修正格式')
  }
}

function validateBeforeCompare() {
  const hasValidSample = tryAutoFormatSamplePayload()
  if (!hasValidSample) {
    return false
  }
  if (!samplePayload.value.trim()) {
    samplePayloadError.value = '请输入上报 JSON'
    return false
  }
  if (deviceStructure.value === 'composite') {
    if (!parentDeviceCode.value.trim()) {
      samplePayloadError.value = '复合设备模式下必须填写父设备编码'
      return false
    }
    if (!normalizeRelationMappings().length) {
      samplePayloadError.value = '复合设备模式下至少需要 1 条映射关系'
      return false
    }
  }
  samplePayloadError.value = ''
  return true
}

async function handleCompare() {
  if (!props.product?.id || !validateBeforeCompare()) {
    return
  }
  compareLoading.value = true
  applyResult.value = null
  rollbackResult.value = null
  applyApprovalDetail.value = null
  rollbackApprovalDetail.value = null
  try {
    const response = await productApi.compareProductModelGovernance(props.product.id, {
      manualExtract: {
        sampleType: sampleType.value,
        deviceStructure: deviceStructure.value,
        samplePayload: samplePayload.value.trim(),
        parentDeviceCode: deviceStructure.value === 'composite' ? parentDeviceCode.value.trim() || undefined : undefined,
        relationMappings: deviceStructure.value === 'composite' ? normalizeRelationMappings() : undefined
      }
    })
    compareResult.value = response.data ?? null
    decisionState.value = Object.fromEntries(
      (response.data?.compareRows ?? []).map((row) => [rowKey(row), defaultDecisionForRow(row)])
    )
  } catch (error) {
    compareResult.value = null
    decisionState.value = {}
    ElMessage.error(error instanceof Error ? error.message : '提取契约字段失败')
  } finally {
    compareLoading.value = false
  }
}

function defaultDecisionForRow(row: ProductModelGovernanceCompareRow): GovernanceDecisionUi {
  switch (row.compareStatus) {
    case 'double_aligned':
      return 'create'
    case 'formal_exists':
      return row.formalModel?.modelId ? 'update' : 'ignore'
    case 'suspected_conflict':
      return 'review'
    case 'manual_only':
    case 'runtime_only':
    case 'evidence_insufficient':
    default:
      return 'observe'
  }
}

function buildApplyItem(row: ProductModelGovernanceCompareRow, decision: ProductModelGovernanceDecision): ProductModelGovernanceApplyItem {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  return {
    decision,
    targetModelId: decision === 'update' ? row.formalModel?.modelId ?? undefined : undefined,
    modelType: row.modelType,
    identifier: row.identifier,
    modelName: source?.modelName || row.identifier,
    dataType: source?.dataType ?? undefined,
    specsJson: source?.specsJson ?? undefined,
    eventType: source?.eventType ?? undefined,
    serviceInputJson: source?.serviceInputJson ?? undefined,
    serviceOutputJson: source?.serviceOutputJson ?? undefined,
    sortNo: source?.sortNo ?? undefined,
    requiredFlag: source?.requiredFlag ?? undefined,
    description: source?.description ?? undefined,
    compareStatus: row.compareStatus
  }
}

function handleDecisionChange(payload: { key: string; decision: GovernanceDecisionUi }) {
  decisionState.value = {
    ...decisionState.value,
    [payload.key]: payload.decision
  }
}

async function handleApply() {
  if (!props.product?.id || !selectedApplyItems.value.length) {
    return
  }
  const approverUserId = resolveApproverUserId()
  if (!approverUserId) {
    return
  }
  applyLoading.value = true
  try {
    const response = await productApi.applyProductModelGovernance(props.product.id, {
      items: selectedApplyItems.value
    }, {
      approverUserId
    })
    applyResult.value = response.data ?? null
    rollbackResult.value = null
    compareResult.value = null
    decisionState.value = {}
    applyApprovalDetail.value = null
    rollbackApprovalDetail.value = null
    ElMessage.success('契约字段审批已提交')
    await loadApprovalOrderDetail(response.data?.approvalOrderId ?? null, 'apply', { silent: true })
    await loadModels(props.product.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '契约字段审批提交失败')
  } finally {
    applyLoading.value = false
  }
}

async function handleRollbackCurrentBatch() {
  if (!props.product?.id || !latestReleaseBatchId.value) {
    ElMessage.warning('当前没有可回滚的发布批次')
    return
  }
  const approverUserId = resolveApproverUserId()
  if (!approverUserId) {
    return
  }
  rollbackLoading.value = true
  try {
    const response = await productApi.rollbackProductContractReleaseBatch(latestReleaseBatchId.value, approverUserId)
    rollbackResult.value = response.data ?? null
    applyResult.value = null
    compareResult.value = null
    decisionState.value = {}
    applyApprovalDetail.value = null
    rollbackApprovalDetail.value = null
    ElMessage.success('合同回滚审批已提交')
    await loadApprovalOrderDetail(response.data?.approvalOrderId ?? null, 'rollback', { silent: true })
    await loadModels(props.product.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '合同回滚审批提交失败')
  } finally {
    rollbackLoading.value = false
  }
}

function resolveApproverUserId() {
  const normalized = governanceApproverId.value.trim()
  if (!normalized) {
    ElMessage.warning('请先输入复核人用户 ID')
    return null
  }
  if (!/^\d+$/.test(normalized)) {
    ElMessage.warning('复核人用户 ID 必须为正整数')
    return null
  }
  return normalized
}

function approvalStatusLabel(status: GovernanceApprovalStatus | null | undefined) {
  return {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    CANCELLED: '已撤销'
  }[status ?? ''] ?? '--'
}

function compareStatusLabel(status: ProductModelGovernanceCompareRow['compareStatus']) {
  return {
    double_aligned: '可直接生效',
    manual_only: '继续观察',
    runtime_only: '继续观察',
    formal_exists: '已有正式字段',
    suspected_conflict: '待确认',
    evidence_insufficient: '继续观察'
  }[status] ?? status
}

function applyDecisionLabel(decision: ProductModelGovernanceDecision) {
  return decision === 'update' ? '纳入修订' : '纳入新增'
}

function formatServiceHint(inputJson?: string | null, outputJson?: string | null) {
  if (inputJson?.trim() && outputJson?.trim()) {
    return '输入/输出已定义'
  }
  if (inputJson?.trim()) {
    return '仅定义输入'
  }
  if (outputJson?.trim()) {
    return '仅定义输出'
  }
  return ''
}

function applyEvidenceSummary(row: ProductModelGovernanceCompareRow) {
  const source = row.manualCandidate ?? row.runtimeCandidate ?? row.formalModel
  if (!source) {
    return '当前没有可用于正式应用的证据摘要。'
  }
  const dataHint = source.dataType || source.eventType || formatServiceHint(source.serviceInputJson, source.serviceOutputJson)
  const sourceTables = source.sourceTables?.length ? source.sourceTables.join(' / ') : '未标注来源'
  return [dataHint, sourceTables, source.description?.trim() || '当前没有补充说明。']
    .filter(Boolean)
    .join(' · ')
}

function buildProductUpdatePayload(product: Product, metadataJson?: string) {
  const payload: ProductAddPayload = {
    productKey: product.productKey,
    productName: product.productName,
    protocolCode: product.protocolCode,
    nodeType: product.nodeType,
    dataFormat: product.dataFormat ?? undefined,
    manufacturer: product.manufacturer ?? undefined,
    description: product.description ?? undefined,
    metadataJson,
    status: product.status ?? 1
  }
  return payload
}

function parseApprovalPayload<TResult>(payloadJson?: string | null): GovernanceApprovalPayload<TResult> | null {
  if (!payloadJson?.trim()) {
    return null
  }
  try {
    return JSON.parse(payloadJson) as GovernanceApprovalPayload<TResult>
  } catch {
    return null
  }
}

function resolveApprovalComment(
  detail: GovernanceApprovalOrderDetail | null,
  status: GovernanceApprovalStatus | null
) {
  if (!detail || !status || status === 'PENDING') {
    return ''
  }
  const transition = [...(detail.transitions ?? [])]
    .reverse()
    .find((item) => item?.toStatus === status && item.transitionComment?.trim() && item.transitionComment !== 'submit')
  return transition?.transitionComment?.trim() || detail.order?.approvalComment?.trim() || ''
}

function resetSession() {
  compareResult.value = null
  applyResult.value = null
  rollbackResult.value = null
  applyApprovalDetail.value = null
  rollbackApprovalDetail.value = null
  applyApprovalLoading.value = false
  rollbackApprovalLoading.value = false
  applyResubmitLoading.value = false
  rollbackResubmitLoading.value = false
  decisionState.value = {}
  governanceApproverId.value = ''
  latestReleaseBatchId.value = null
  sampleType.value = 'business'
  deviceStructure.value = 'single'
  samplePayload.value = ''
  parentDeviceCode.value = ''
  relationMappings.value = [createRelationRow()]
  samplePayloadError.value = ''
  cancelRenameModel()
}
</script>

<style scoped>
.product-model-designer-workspace,
.product-model-designer__summary-sheet,
.product-model-designer__summary-grid,
.product-model-designer__stage,
.product-model-designer__approval-stage,
.product-model-designer__sample-toolbar,
.product-model-designer__choice-group,
.product-model-designer__relation-stage,
.product-model-designer__relation-grid,
.product-model-designer__relation-list,
.product-model-designer__payload-stage,
.product-model-designer__apply-list,
.product-model-designer__formal-list,
.product-model-designer__receipt,
.product-model-designer__formal-tabs {
  display: grid;
}

.product-model-designer-workspace {
  gap: 1rem;
}

.product-model-designer__summary-sheet,
.product-model-designer__stage,
.product-model-designer__approval-stage {
  gap: 0.92rem;
  padding: 0.96rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: 0.82rem;
  background: white;
}

.product-model-designer__summary-sheet {
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.9rem 1rem;
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand-light) 18%, white), white);
}

.product-model-designer__summary-copy {
  display: grid;
  gap: 0.34rem;
}

.product-model-designer__summary-kicker {
  display: inline-flex;
  width: max-content;
  color: color-mix(in srgb, var(--brand) 68%, var(--text-caption));
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.product-model-designer__summary-title,
.product-model-designer__stage-head h3,
.product-model-designer__relation-head strong,
.product-model-designer__formal-card-head strong,
.product-model-designer__apply-card-head strong {
  margin: 0;
  color: var(--text-heading);
}

.product-model-designer__summary-title {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', 'Songti SC', 'STSong', serif;
  font-size: clamp(1.34rem, 2vw, 1.78rem);
  line-height: 1.2;
}

.product-model-designer__summary-description,
.product-model-designer__stage-head p,
.product-model-designer__relation-head p,
.product-model-designer__apply-card p,
.product-model-designer__formal-card p,
.product-model-designer__empty p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.64;
}

.product-model-designer__summary-actions {
  display: flex;
  align-items: start;
  justify-content: flex-end;
}

.product-model-designer__summary-grid,
.product-model-designer__receipt {
  grid-column: 1 / -1;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.72rem;
}

.product-model-designer__summary-card,
.product-model-designer__apply-card,
.product-model-designer__formal-card,
.product-model-designer__formal-tab {
  display: grid;
  gap: 0.28rem;
}

.product-model-designer__summary-card {
  min-width: 0;
  padding: 0.8rem 0.88rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: 0.72rem;
  background: color-mix(in srgb, var(--brand-light) 10%, white);
}

.product-model-designer__summary-card span,
.product-model-designer__field-label,
.product-model-designer__formal-tab span,
.product-model-designer__apply-card-meta,
.product-model-designer__formal-card-meta,
.product-model-designer__input-field span {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.56;
}

.product-model-designer__summary-card strong,
.product-model-designer__formal-tab strong {
  color: var(--text-heading);
  font-size: 1.06rem;
  line-height: 1.38;
}

.product-model-designer__stage-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.product-model-designer__approval-head,
.product-model-designer__approval-inline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.product-model-designer__approval-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.56rem;
  align-items: center;
  justify-content: flex-end;
}

.product-model-designer__sample-toolbar {
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 0.82rem;
  align-items: end;
}

.product-model-designer__field-group {
  display: grid;
  gap: 0.36rem;
}

.product-model-designer__choice-group {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.48rem;
}

.product-model-designer__choice-button,
.product-model-designer__row-action,
.product-model-designer__formal-tab {
  border: 1px solid var(--panel-border);
  border-radius: 0.7rem;
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
}

.product-model-designer__choice-button {
  padding: 0.5rem 0.76rem;
}

.product-model-designer__choice-button--active,
.product-model-designer__formal-tab--active {
  border-color: color-mix(in srgb, var(--brand) 44%, white);
  color: var(--brand);
  background: color-mix(in srgb, var(--brand-light) 20%, white);
}

.product-model-designer__toolbar-actions {
  display: flex;
  gap: 0.56rem;
  align-items: center;
}

.product-model-designer__relation-stage {
  gap: 0.78rem;
  padding: 0.88rem 0.94rem;
  border-radius: 0.8rem;
  border: 1px solid color-mix(in srgb, var(--brand) 12%, var(--panel-border));
  background: color-mix(in srgb, var(--brand-light) 10%, white);
}

.product-model-designer__relation-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.product-model-designer__relation-list,
.product-model-designer__payload-stage,
.product-model-designer__apply-list,
.product-model-designer__formal-list {
  gap: 0.72rem;
}

.product-model-designer__relation-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 0.56rem;
}

.product-model-designer__row-action {
  padding: 0.5rem 0.78rem;
  align-self: end;
}

.product-model-designer__input-field {
  display: grid;
  gap: 0.36rem;
}

.product-model-designer__input-error {
  margin: 0;
  color: #c2410c;
  font-size: 0.82rem;
  line-height: 1.5;
}

.product-model-designer__apply-card,
.product-model-designer__formal-card {
  padding: 0.88rem 0.94rem;
  border: 1px solid color-mix(in srgb, var(--brand) 8%, var(--panel-border));
  border-radius: 0.76rem;
  background: white;
}

.product-model-designer__apply-card-head,
.product-model-designer__formal-card-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.product-model-designer__formal-title,
.product-model-designer__formal-rename {
  display: grid;
  gap: 0.42rem;
}

.product-model-designer__formal-rename {
  flex: 1;
}

.product-model-designer__formal-rename-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.56rem;
}

.product-model-designer__apply-card-meta,
.product-model-designer__formal-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.42rem 0.8rem;
}

.product-model-designer__formal-card-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem 0.72rem;
}

.product-model-designer__formal-card-state {
  color: var(--text-secondary);
  font-size: 0.82rem;
  line-height: 1.5;
}

.product-model-designer__apply-footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
  padding-top: 0.2rem;
}

.product-model-designer__apply-footer p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.56;
}

.product-model-designer__approver-field {
  min-width: min(24rem, 100%);
}

.product-model-designer__formal-tabs {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.product-model-designer__formal-tab {
  width: 100%;
  padding: 0.82rem 0.88rem;
  text-align: left;
}

.product-model-designer__empty {
  display: grid;
  gap: 0.4rem;
  padding: 0.9rem 1rem;
  border: 1px dashed color-mix(in srgb, var(--brand) 20%, var(--panel-border));
  border-radius: 0.72rem;
}

@media (max-width: 960px) {
  .product-model-designer__summary-sheet,
  .product-model-designer__sample-toolbar,
  .product-model-designer__summary-grid,
  .product-model-designer__receipt,
  .product-model-designer__formal-tabs {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .product-model-designer__approval-head,
  .product-model-designer__approval-inline,
  .product-model-designer__relation-head,
  .product-model-designer__apply-footer,
  .product-model-designer__stage-head,
  .product-model-designer__apply-card-head,
  .product-model-designer__formal-card-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-model-designer__choice-group,
  .product-model-designer__relation-row {
    grid-template-columns: 1fr;
  }

  .product-model-designer__approval-actions {
    justify-content: flex-start;
  }
}
</style>
