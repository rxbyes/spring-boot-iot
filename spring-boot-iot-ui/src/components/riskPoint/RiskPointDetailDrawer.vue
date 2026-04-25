<template>
  <StandardDetailDrawer
    :model-value="modelValue"
    :title="drawerTitle"
    :subtitle="drawerSubtitle"
    :tags="drawerTags"
    tag-layout="title-inline"
    :loading="showLoadingState"
    loading-text="正在加载风险对象详情..."
    :error-message="errorMessage"
    :empty="showEmptyState"
    empty-text="暂无风险对象详情"
    @update:modelValue="handleModelValueChange"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>绑定概况</h3>
          <p>列表不再承载绑定状态摘要，当前风险点的绑定态势、治理残留和后续入口统一收口到这里查看。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">绑定状态</span>
          <strong class="detail-summary-card__value">{{ bindingStateLabel }}</strong>
          <p class="detail-summary-card__hint">{{ bindingStateHint }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">正式绑定设备</span>
          <strong class="detail-summary-card__value">{{ boundDeviceCount }} 台已绑定设备</strong>
          <p class="detail-summary-card__hint">已纳入正式监测关系的设备都在下方按设备分组展示。</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">正式测点</span>
          <strong class="detail-summary-card__value">{{ boundMetricCount }} 个正式测点</strong>
          <p class="detail-summary-card__hint">详情内统一展示测点名称、标识和绑定来源，不再回流到列表里堆叠。</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">待治理记录</span>
          <strong class="detail-summary-card__value">待治理 {{ summaryPendingCount }} 条</strong>
          <p class="detail-summary-card__hint">{{ pendingGovernanceHint }}</p>
        </article>
      </div>
    </section>

    <section v-if="summaryPendingCount > 0" class="detail-panel">
      <div class="detail-notice">
        <span class="detail-notice__label">治理提醒</span>
        <strong class="detail-notice__value">当前仍有 {{ summaryPendingCount }} 条待治理记录，可继续进入“风险绑定工作台”并切换到“待治理转正”完成收口。</strong>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>正式绑定设备</h3>
          <p>按设备分组展示当前已生效的正式测点绑定，避免客户再回到主表里理解多行堆叠摘要。</p>
        </div>
      </div>

      <div v-if="bindingGroups.length === 0" class="standard-list-empty-state">
        <EmptyState title="暂无正式绑定" description="当前风险点还没有正式绑定设备和测点，可继续进入维护绑定或待治理转正。" />
      </div>

      <div v-else class="detail-binding-group-list">
        <article
          v-for="group in bindingGroups"
          :key="String(group.deviceId)"
          class="detail-binding-group-card"
        >
          <div class="detail-binding-group-card__header">
            <div>
              <h4>{{ group.deviceName || '未命名设备' }}</h4>
              <p>
                {{ group.deviceCode || '--' }}
                ·
                {{ isDeviceOnlyGroup(group) ? '设备级正式绑定' : `${group.metricCount} 个正式测点` }}
              </p>
            </div>
          </div>

          <div v-if="isDeviceOnlyGroup(group)" class="detail-binding-device-only-card">
            <strong>设备级正式绑定</strong>
            <p>{{ getGroupCapabilityLabel(group) }}</p>
            <p v-if="isAiEventReserved(group)">AI 事件扩展预留</p>
          </div>

          <div v-else class="detail-binding-metric-list">
            <div
              v-for="metric in group.metrics"
              :key="String(metric.bindingId)"
              class="detail-binding-metric-row"
            >
              <div>
                <strong>{{ metric.metricName || metric.metricIdentifier }}</strong>
                <p>{{ metric.metricIdentifier }}</p>
              </div>
              <el-tag type="info">{{ getBindingSourceLabel(metric.bindingSource) }}</el-tag>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>风险点档案</h3>
          <p>把对象本身的组织、区域、负责人、等级与留档信息集中查看，避免与绑定态势信息混排。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">所属组织</span>
          <strong class="detail-field__value">{{ displayRiskPoint?.orgName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">所属区域</span>
          <strong class="detail-field__value">{{ displayRiskPoint?.regionName || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">负责人</span>
          <strong class="detail-field__value">{{ responsibleUserText }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">负责人电话</span>
          <strong class="detail-field__value">{{ displayRiskPoint?.responsiblePhone || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">风险点编号</span>
          <strong class="detail-field__value">{{ displayRiskPoint?.riskPointCode || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">风险点等级</span>
          <strong class="detail-field__value">{{ riskPointLevelText }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">当前风险态势</span>
          <strong class="detail-field__value">{{ currentRiskLevelText }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">状态</span>
          <strong class="detail-field__value">{{ statusText }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">描述</span>
          <strong class="detail-field__value detail-field__value--plain">{{ displayRiskPoint?.description || '--' }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">备注</span>
          <strong class="detail-field__value detail-field__value--plain">{{ displayRiskPoint?.remark || '--' }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">创建时间</span>
          <strong class="detail-field__value">{{ formatDateTime(displayRiskPoint?.createTime) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">更新时间</span>
          <strong class="detail-field__value">{{ formatDateTime(displayRiskPoint?.updateTime) }}</strong>
        </div>
      </div>
    </section>

    <template #footer>
      <div class="detail-actions">
        <StandardButton
          data-testid="detail-edit-action"
          :disabled="!displayRiskPoint"
          v-permission="'risk:point:update'"
          @click="emit('edit')"
        >
          编辑风险点
        </StandardButton>
        <StandardButton
          data-testid="detail-binding-workbench-action"
          action="confirm"
          :disabled="!displayRiskPoint"
          @click="emit('binding-workbench')"
        >
          风险绑定工作台
        </StandardButton>
      </div>
    </template>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import { getRiskPointById, listBindingGroups, type RiskPoint, type RiskPointBindingDeviceGroup, type RiskPointBindingSummary } from '@/api/riskPoint'
import type { IdType } from '@/types/api'
import { formatDateTime } from '@/utils/format'
import { getRiskLevelTagType, getRiskLevelText } from '@/utils/riskLevel'
import {
  getDeviceCapabilityLabel,
  isAiEventReserved,
  isDeviceOnlyBindingMode,
  resolveBindingGroupCapabilityType
} from '@/utils/riskPointDeviceBindingCapability'
import { DEFAULT_RISK_POINT_LEVEL_OPTIONS, getRiskPointLevelText } from '@/utils/riskPointLevel'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    riskPointId?: IdType | null
    initialRiskPoint?: RiskPoint | null
    initialSummary?: RiskPointBindingSummary | null
  }>(),
  {
    riskPointId: undefined,
    initialRiskPoint: null,
    initialSummary: null
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  close: []
  edit: []
  'binding-workbench': []
}>()

const loading = ref(false)
const errorMessage = ref('')
const detail = ref<RiskPoint | null>(null)
const bindingGroups = ref<RiskPointBindingDeviceGroup[]>([])
let latestRequestId = 0

const displayRiskPoint = computed(() => detail.value || props.initialRiskPoint || null)
const showLoadingState = computed(() => loading.value && !displayRiskPoint.value)
const showEmptyState = computed(() => !showLoadingState.value && !errorMessage.value && !displayRiskPoint.value)
const summaryPendingCount = computed(() => props.initialSummary?.pendingBindingCount ?? 0)
const boundDeviceCount = computed(() => {
  if (bindingGroups.value.length > 0) {
    return bindingGroups.value.length
  }
  return props.initialSummary?.boundDeviceCount ?? 0
})
const boundMetricCount = computed(() => {
  if (bindingGroups.value.length > 0) {
    return bindingGroups.value.reduce((total, group) => total + Number(group.metricCount || group.metrics?.length || 0), 0)
  }
  return props.initialSummary?.boundMetricCount ?? 0
})
const hasFormalBindings = computed(() => boundDeviceCount.value > 0 || boundMetricCount.value > 0)
const bindingStateLabel = computed(() => {
  const hasFormal = hasFormalBindings.value
  const hasPending = summaryPendingCount.value > 0
  if (hasFormal && hasPending) {
    return '已绑定 / 待治理'
  }
  if (hasFormal) {
    return '已绑定'
  }
  if (hasPending) {
    return '待治理'
  }
  return '未绑定'
})
const bindingStateHint = computed(() => {
  if (hasFormalBindings.value && summaryPendingCount.value > 0) {
    return '正式绑定已生效，但仍有待治理记录待继续收口。'
  }
  if (hasFormalBindings.value) {
    return '当前风险点已经形成正式监测关系，可继续按设备维护测点。'
  }
  if (summaryPendingCount.value > 0) {
    return '当前尚未形成正式绑定，建议优先进入待治理转正完成首轮收口。'
  }
  return '当前还没有正式绑定设备和测点，可从详情底部继续进入维护流程。'
})
const pendingGovernanceHint = computed(() => {
  if (summaryPendingCount.value > 0) {
    return '待治理与正式绑定在工作台内分域执行，可统一进入后再决定下一步。'
  }
  return '当前没有待治理残留，后续可直接在风险绑定工作台维护正式绑定。'
})
const drawerTitle = computed(() => displayRiskPoint.value?.riskPointName || '风险对象详情')
const drawerSubtitle = computed(() =>
  displayRiskPoint.value?.riskPointCode
    ? `风险点编号：${displayRiskPoint.value.riskPointCode}`
    : '查看风险点档案与设备绑定详情'
)
const riskPointLevelText = computed(() => getRiskPointLevelText(displayRiskPoint.value?.riskPointLevel, DEFAULT_RISK_POINT_LEVEL_OPTIONS))
const currentRiskLevelText = computed(() => getRiskLevelText(displayRiskPoint.value?.currentRiskLevel || displayRiskPoint.value?.riskLevel))
const statusText = computed(() => (Number(displayRiskPoint.value?.status) === 0 ? '启用' : '停用'))
const responsibleUserText = computed(() => {
  if (!displayRiskPoint.value?.responsibleUser) {
    return '未指定负责人'
  }
  return displayRiskPoint.value.responsibleUserName || String(displayRiskPoint.value.responsibleUser)
})
const drawerTags = computed(() => [
  {
    label: riskPointLevelText.value,
    type: 'info' as const
  },
  {
    label: currentRiskLevelText.value,
    type: getRiskLevelTagType(displayRiskPoint.value?.currentRiskLevel || displayRiskPoint.value?.riskLevel)
  },
  {
    label: statusText.value,
    type: Number(displayRiskPoint.value?.status) === 0 ? 'success' as const : 'info' as const
  }
])

const resetState = () => {
  latestRequestId += 1
  loading.value = false
  errorMessage.value = ''
  detail.value = null
  bindingGroups.value = []
}

const loadDrawerData = async () => {
  if (!props.modelValue || !props.riskPointId) {
    resetState()
    return
  }

  const requestId = ++latestRequestId
  loading.value = !props.initialRiskPoint
  errorMessage.value = ''
  detail.value = null
  bindingGroups.value = []

  const [detailResult, bindingGroupResult] = await Promise.allSettled([
    getRiskPointById(props.riskPointId),
    listBindingGroups(props.riskPointId)
  ])

  if (requestId !== latestRequestId) {
    return
  }

  if (detailResult.status === 'fulfilled' && detailResult.value.code === 200) {
    detail.value = detailResult.value.data || null
  } else if (!props.initialRiskPoint) {
    errorMessage.value = '加载风险对象详情失败'
  }

  if (bindingGroupResult.status === 'fulfilled' && bindingGroupResult.value.code === 200) {
    bindingGroups.value = bindingGroupResult.value.data || []
  } else {
    bindingGroups.value = []
  }

  loading.value = false
}

const handleModelValueChange = (value: boolean) => {
  emit('update:modelValue', value)
  if (!value) {
    emit('close')
  }
}

const getBindingSourceLabel = (bindingSource?: string | null) => {
  switch ((bindingSource || '').trim().toUpperCase()) {
    case 'PENDING_PROMOTION':
      return '待治理转正'
    case 'MANUAL':
      return '人工维护'
    default:
      return '未知来源'
  }
}

const isDeviceOnlyGroup = (group: RiskPointBindingDeviceGroup) => isDeviceOnlyBindingMode(group)
const getGroupCapabilityLabel = (group: RiskPointBindingDeviceGroup) =>
  `${getDeviceCapabilityLabel(resolveBindingGroupCapabilityType(group))} · 设备级正式绑定`

watch(
  () => [props.modelValue, props.riskPointId],
  () => {
    void loadDrawerData()
  },
  { immediate: true }
)

watch(
  () => props.initialRiskPoint,
  () => {
    if (!props.modelValue) {
      return
    }
    detail.value = null
  }
)
</script>

<style scoped>
.detail-panel {
  display: grid;
  gap: 1rem;
  padding: 1.15rem 1.2rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-card-soft);
}

.detail-panel--hero {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 249, 255, 0.95)),
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand) 8%, transparent), transparent 35%);
}

.detail-section-header h3,
.detail-binding-group-card__header h4 {
  margin: 0;
  color: var(--text-heading);
}

.detail-section-header p,
.detail-binding-group-card__header p,
.detail-summary-card__hint,
.detail-binding-metric-row p {
  margin: 0;
  color: var(--text-secondary);
}

.detail-summary-grid,
.detail-grid,
.detail-binding-group-list,
.detail-binding-metric-list {
  display: grid;
  gap: 0.9rem;
}

.detail-binding-device-only-card {
  display: grid;
  gap: 0.35rem;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

.detail-binding-device-only-card p {
  margin: 0;
  color: var(--text-secondary);
}

.detail-summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
}

.detail-summary-card,
.detail-binding-group-card {
  display: grid;
  gap: 0.45rem;
  padding: 1rem;
  border: 1px solid color-mix(in srgb, var(--brand) 10%, var(--panel-border));
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.94);
}

.detail-summary-card__label,
.detail-field__label,
.detail-notice__label {
  font-size: var(--type-caption-size);
  color: var(--text-caption);
}

.detail-summary-card__value,
.detail-field__value,
.detail-notice__value,
.detail-binding-metric-row strong {
  color: var(--text-heading);
}

.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-field {
  display: grid;
  gap: 0.3rem;
}

.detail-field--full {
  grid-column: 1 / -1;
}

.detail-field__value--plain {
  font-weight: 500;
  line-height: 1.7;
}

.detail-notice {
  display: grid;
  gap: 0.35rem;
  padding: 1rem 1.05rem;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--brand) 18%, var(--panel-border));
  background: color-mix(in srgb, var(--brand) 6%, white);
}

.detail-binding-group-card__header,
.detail-binding-metric-row,
.detail-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.detail-binding-metric-row {
  padding: 0.8rem 0.85rem;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--brand) 4%, white);
}

.detail-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-binding-group-card__header,
  .detail-binding-metric-row,
  .detail-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-actions {
    width: 100%;
  }
}
</style>
