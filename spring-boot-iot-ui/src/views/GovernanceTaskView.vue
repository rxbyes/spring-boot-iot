<template>
  <StandardPageShell class="governance-task-view">
    <StandardWorkbenchPanel
      title="治理任务台"
      :description="`统一查看合同发布、风险绑定等治理待办，当前共 ${pagination.total} 项。`"
      show-toolbar
      show-pagination
    >
      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前 ${pagination.total} 项`,
            `待处理 ${openCount} 项`,
            activeScopeLabel
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <PanelCard
        title="任务导向"
        description="这里直接回答当前产品或对象还能否进入治理闭环，以及当前卡在哪个控制面环节。"
      >
        <div class="governance-task-summary">
          <div class="governance-task-summary__item">
            <span>当前任务范围</span>
            <strong>{{ activeScopeLabel }}</strong>
          </div>
          <div class="governance-task-summary__item">
            <span>默认工作状态</span>
            <strong>{{ activeStatusLabel }}</strong>
          </div>
        </div>
      </PanelCard>

      <div class="governance-task-result">
        <div v-if="taskList.length" class="governance-task-list">
          <article
            v-for="item in taskList"
            :key="String(item.id)"
            class="governance-task-card"
          >
            <header class="governance-task-card__header">
              <div class="governance-task-card__heading">
                <strong>{{ workItemCodeLabel(item.workItemCode, item.snapshotJson) }}</strong>
                <span>{{ workItemAnchor(item) }}</span>
              </div>
              <div class="governance-task-card__status-group">
                <span v-if="item.priorityLevel" class="governance-task-card__priority">{{ item.priorityLevel }}</span>
                <span class="governance-task-card__status">{{ workStatusLabel(item.workStatus) }}</span>
              </div>
            </header>
            <p class="governance-task-card__reason">{{ item.blockingReason || '暂无阻塞说明，建议优先查看对象上下文。' }}</p>
            <section v-if="hasRecommendation(item) || hasImpact(item)" class="governance-task-card__decision">
              <div v-if="hasRecommendation(item)" class="governance-task-card__decision-block">
                <div class="governance-task-card__decision-header">
                  <strong>{{ item.recommendation?.suggestedAction || item.recommendation?.recommendationType || 'Recommendation' }}</strong>
                  <span v-if="recommendationConfidenceText(item.recommendation)" class="governance-task-card__decision-tag">
                    {{ recommendationConfidenceText(item.recommendation) }}
                  </span>
                </div>
                <div v-if="recommendationEvidenceItems(item).length" class="governance-task-card__evidence-list">
                  <article
                    v-for="(evidence, index) in recommendationEvidenceItems(item)"
                    :key="`${String(item.id)}-evidence-${index}`"
                    class="governance-task-card__evidence-item"
                  >
                    <strong>{{ evidence.evidenceType || evidence.sourceType || 'EVIDENCE' }}</strong>
                    <span>{{ evidence.title || evidence.summary || evidence.sourceId || '--' }}</span>
                  </article>
                </div>
              </div>
              <div v-if="hasImpact(item)" class="governance-task-card__decision-block">
                <div class="governance-task-card__decision-header">
                  <strong>{{ impactSummaryText(item.impact) }}</strong>
                  <span class="governance-task-card__decision-tag">{{ rollbackabilityText(item.impact, item.rollback) }}</span>
                </div>
                <p v-if="impactPlanSummary(item.impact, item.rollback)" class="governance-task-card__decision-copy">
                  {{ impactPlanSummary(item.impact, item.rollback) }}
                </p>
              </div>
            </section>
            <dl class="governance-task-card__meta">
              <div>
                <dt>主题</dt>
                <dd>{{ item.subjectType || snapshotValue(item.snapshotJson, 'coverageType') || '--' }} / {{ item.subjectId ?? snapshotValue(item.snapshotJson, 'dimensionKey') ?? '--' }}</dd>
              </div>
              <div>
                <dt>产品</dt>
                <dd>{{ item.productId ?? snapshotValue(item.snapshotJson, 'productId') ?? '--' }}</dd>
              </div>
              <div>
                <dt>风险指标</dt>
                <dd>{{ item.riskMetricId ?? snapshotValue(item.snapshotJson, 'riskMetricId') ?? snapshotValue(item.snapshotJson, 'metricIdentifier') ?? '--' }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ item.updateTime || item.createTime || '--' }}</dd>
              </div>
            </dl>
            <div v-if="canOperateWorkItem(item) || canReplayWorkItem(item) || canDispatchWorkItem(item) || canExplainDecision(item)" class="governance-task-card__actions">
              <StandardButton v-if="canExplainDecision(item)" @click="handleOpenDecisionContext(item)">决策说明</StandardButton>
              <StandardButton v-if="canDispatchWorkItem(item)" @click="handleDispatchWorkItem(item)">去处理</StandardButton>
              <StandardButton v-if="canReplayWorkItem(item)" @click="handleOpenReplay(item)">复盘</StandardButton>
              <StandardButton v-if="canOperateWorkItem(item)" @click="handleWorkItemAction('ack', item)">确认</StandardButton>
              <StandardButton v-if="canOperateWorkItem(item)" @click="handleWorkItemAction('block', item)">阻塞</StandardButton>
              <StandardButton v-if="canOperateWorkItem(item)" @click="handleWorkItemAction('close', item)">关闭</StandardButton>
            </div>
          </article>
        </div>
        <div v-else class="governance-task-empty">
          <strong>当前没有匹配的治理任务</strong>
          <span>可调整 query 条件或回到产品、风险对象、审批台继续排查。</span>
        </div>
      </div>

      <template #pagination>
        <StandardPagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </template>
    </StandardWorkbenchPanel>

    <StandardDetailDrawer
      v-model="decisionContextVisible"
      title="决策说明"
      subtitle="解释这条治理任务为何排在这里，以及建议先处理什么。"
      :loading="decisionContextLoading"
      :error-message="decisionContextErrorMessage"
      :empty="!decisionContextData"
    >
      <div class="governance-task-detail-stack">
        <section class="governance-task-detail-section">
          <h3>优先级与问题</h3>
          <div class="governance-task-detail-grid">
            <div class="governance-task-detail-field">
              <span>优先级</span>
              <strong>{{ decisionContextData?.priorityLevel || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>影响范围</span>
              <strong>{{ decisionContextData?.affectedCount != null ? decisionContextData.affectedCount : '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>问题摘要</span>
              <strong>{{ decisionContextData?.problemSummary || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>推荐动作</span>
              <strong>{{ decisionContextData?.recommendedAction || '--' }}</strong>
            </div>
          </div>
        </section>

        <section v-if="decisionReasonCodes.length" class="governance-task-detail-section">
          <h3>排序依据</h3>
          <div class="governance-task-decision-chip-list">
            <span
              v-for="reasonCode in decisionReasonCodes"
              :key="reasonCode"
              class="governance-task-decision-chip"
            >
              {{ reasonCode }}
            </span>
          </div>
        </section>

        <section v-if="decisionAffectedModules.length" class="governance-task-detail-section">
          <h3>影响模块</h3>
          <div class="governance-task-decision-chip-list">
            <span
              v-for="module in decisionAffectedModules"
              :key="module"
              class="governance-task-decision-chip"
            >
              {{ module }}
            </span>
          </div>
        </section>

        <section v-if="decisionContextData?.rollbackable != null || decisionContextData?.rollbackPlanSummary" class="governance-task-detail-section">
          <h3>回滚说明</h3>
          <div class="governance-task-detail-grid">
            <div class="governance-task-detail-field">
              <span>可回滚</span>
              <strong>{{ decisionContextData?.rollbackable == null ? '--' : (decisionContextData.rollbackable ? '是' : '否') }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>回滚摘要</span>
              <strong>{{ decisionContextData?.rollbackPlanSummary || '--' }}</strong>
            </div>
          </div>
        </section>
      </div>
    </StandardDetailDrawer>

    <StandardDetailDrawer
      v-model="replayVisible"
      title="治理链路复盘"
      subtitle="统一回看任务对应的批次上下文、消息轨迹和治理缺口。"
      :loading="replayLoading"
      :error-message="replayErrorMessage"
      :empty="!replayData"
    >
      <div class="governance-task-detail-stack">
        <section class="governance-task-detail-section">
          <h3>复盘概览</h3>
          <div class="governance-task-detail-grid">
            <div class="governance-task-detail-field">
              <span>发布批次</span>
              <strong>{{ replayData?.releaseBatchId != null ? `发布批次 ${replayData.releaseBatchId}` : '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>TraceId</span>
              <strong>{{ replayData?.traceId || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>设备编码</span>
              <strong>{{ replayData?.deviceCode || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>产品标识</span>
              <strong>{{ replayData?.productKey || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>命中消息</span>
              <strong>{{ replayData?.matchedMessageCount ?? 0 }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>命中失败归档</span>
              <strong>{{ replayData?.matchedAccessErrorCount ?? 0 }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.gapSummary" class="governance-task-detail-section">
          <h3>治理缺口</h3>
          <div class="governance-task-gap-grid">
            <div class="governance-task-gap-card">
              <span>待补绑定</span>
              <strong>{{ `待补绑定 ${replayData.gapSummary.missingBindingCount ?? 0}` }}</strong>
            </div>
            <div class="governance-task-gap-card">
              <span>待补策略</span>
              <strong>{{ `待补策略 ${replayData.gapSummary.missingPolicyCount ?? 0}` }}</strong>
            </div>
            <div class="governance-task-gap-card">
              <span>待补指标目录</span>
              <strong>{{ `待补指标目录 ${replayData.gapSummary.missingRiskMetricCount ?? 0}` }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.batchReconciliation" class="governance-task-detail-section">
          <h3>批次对账</h3>
          <div class="governance-task-detail-grid">
            <div class="governance-task-detail-field">
              <span>对账结果</span>
              <strong>{{ booleanLabel(replayData.batchReconciliation.consistent, '一致', '需处理') }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>快照可用</span>
              <strong>{{ booleanLabel(replayData.batchReconciliation.snapshotAvailable) }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>发布状态</span>
              <strong>{{ replayData.batchReconciliation.releaseStatus || '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>审批单</span>
              <strong>{{ replayData.batchReconciliation.approvalOrderId ?? '--' }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>缺失正式字段</span>
              <strong>{{ replayData.batchReconciliation.missingCurrentFieldCount ?? 0 }}</strong>
            </div>
            <div class="governance-task-detail-field">
              <span>多余正式字段</span>
              <strong>{{ replayData.batchReconciliation.extraCurrentFieldCount ?? 0 }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.replayChainSteps?.length" class="governance-task-detail-section">
          <h3>复盘链路</h3>
          <div class="governance-task-chain-list">
            <article
              v-for="(step, index) in replayData.replayChainSteps"
              :key="`${step.stepCode || '--'}-${index}`"
              class="governance-task-chain-item"
            >
              <strong>{{ step.stepCode || step.stepName || '--' }}</strong>
              <span>{{ step.stepName || '--' }} · {{ replayChainStatusLabel(step.status) }}</span>
              <span>{{ step.summary || '--' }}</span>
              <span>{{ step.nextAction || '--' }}</span>
            </article>
          </div>
        </section>

        <section class="governance-task-detail-section">
          <h3>复盘结论回写</h3>
          <div class="governance-task-feedback-form">
            <label class="governance-task-feedback-field">
              <span>推荐结论</span>
              <strong>{{ replayRecommendedDecision }}</strong>
            </label>
            <label class="governance-task-feedback-field">
              <span>采纳结论</span>
              <select
                data-testid="task-replay-adopted-decision"
                v-model="replayFeedback.adoptedDecision"
                class="governance-task-feedback-input"
              >
                <option value="">请选择</option>
                <option value="PROMOTE">PROMOTE</option>
                <option value="PUBLISH">PUBLISH</option>
                <option value="CREATE_POLICY">CREATE_POLICY</option>
                <option value="REPLAY">REPLAY</option>
                <option value="IGNORE">IGNORE</option>
              </select>
            </label>
            <label class="governance-task-feedback-field">
              <span>执行结果</span>
              <select
                data-testid="task-replay-execution-outcome"
                v-model="replayFeedback.executionOutcome"
                class="governance-task-feedback-input"
              >
                <option value="">请选择</option>
                <option value="SUCCESS">SUCCESS</option>
                <option value="PARTIAL_SUCCESS">PARTIAL_SUCCESS</option>
                <option value="FAILED">FAILED</option>
                <option value="IGNORED">IGNORED</option>
              </select>
            </label>
            <label class="governance-task-feedback-field">
              <span>根因分类</span>
              <select
                data-testid="task-replay-root-cause"
                v-model="replayFeedback.rootCauseCode"
                class="governance-task-feedback-input"
              >
                <option value="">请选择</option>
                <option value="MISSING_POLICY">MISSING_POLICY</option>
                <option value="MISSING_BINDING">MISSING_BINDING</option>
                <option value="MISSING_RISK_METRIC">MISSING_RISK_METRIC</option>
                <option value="APPROVAL_BLOCKED">APPROVAL_BLOCKED</option>
                <option value="DATA_QUALITY">DATA_QUALITY</option>
                <option value="OTHER">OTHER</option>
              </select>
            </label>
            <label class="governance-task-feedback-field governance-task-feedback-field--full">
              <span>操作结论</span>
              <textarea
                data-testid="task-replay-operator-summary"
                v-model="replayFeedback.operatorSummary"
                class="governance-task-feedback-input governance-task-feedback-textarea"
                rows="3"
                placeholder="请说明本次复盘最终采纳了什么结论、为何这样处理。"
              />
            </label>
          </div>
          <div class="governance-task-feedback-actions">
            <StandardButton @click="handleSubmitReplayFeedback">
              {{ replaySubmitting ? '提交中...' : '提交复盘结论' }}
            </StandardButton>
          </div>
        </section>
      </div>
    </StandardDetailDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from '@/utils/message'

import { productApi } from '@/api/product'
import { resolveRequestErrorMessage } from '@/api/request'
import {
  getRiskGovernanceReplay,
  submitGovernanceReplayFeedback,
  type RiskGovernanceReplay,
  type RiskGovernanceReplayQuery
} from '@/api/riskGovernance'
import {
  ackGovernanceWorkItem,
  blockGovernanceWorkItem,
  closeGovernanceWorkItem,
  getGovernanceWorkItemDecisionContext,
  pageGovernanceWorkItems
} from '@/api/governanceWorkItem'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import type {
  GovernanceDecisionContext,
  GovernanceReplayFeedbackPayload,
  GovernanceImpactSnapshot,
  GovernanceRecommendationSnapshot,
  GovernanceRollbackSnapshot,
  GovernanceWorkItem,
  GovernanceWorkItemPageQuery
} from '@/types/api'
import { buildGovernanceTaskDispatchLocation } from '@/utils/governanceTaskDispatch'

const route = useRoute()
const router = useRouter()
const { pagination, applyPageResult, setPageNum, setPageSize } = useServerPagination()

const taskList = ref<GovernanceWorkItem[]>([])
const decisionContextVisible = ref(false)
const decisionContextLoading = ref(false)
const decisionContextErrorMessage = ref('')
const decisionContextData = ref<GovernanceDecisionContext | null>(null)
const replayVisible = ref(false)
const replayLoading = ref(false)
const replayErrorMessage = ref('')
const replayData = ref<RiskGovernanceReplay | null>(null)
const replaySourceItem = ref<GovernanceWorkItem | null>(null)
const replaySubmitting = ref(false)
const replayFeedback = ref<GovernanceReplayFeedbackPayload>({
  adoptedDecision: '',
  executionOutcome: '',
  rootCauseCode: '',
  operatorSummary: ''
})
const initialPageNum = parseNumberQuery(route.query.pageNum)
const initialPageSize = parseNumberQuery(route.query.pageSize)

if (initialPageSize != null) {
  setPageSize(initialPageSize)
}
if (initialPageNum != null) {
  setPageNum(initialPageNum)
}

const queryState = computed(() => buildQueryFromRoute())
const openCount = computed(() => taskList.value.filter((item) => item.workStatus === 'OPEN').length)
const decisionReasonCodes = computed(() => decisionContextData.value?.reasonCodes ?? [])
const decisionAffectedModules = computed(() => decisionContextData.value?.affectedModules ?? [])
const replayRecommendedDecision = computed(() => normalizeText(replayFeedback.value.recommendedDecision) || '--')
const replayCanSubmit = computed(() =>
  Boolean(normalizeText(replayFeedback.value.adoptedDecision))
  && Boolean(normalizeText(replayFeedback.value.executionOutcome))
  && Boolean(normalizeText(replayFeedback.value.rootCauseCode))
  && Boolean(normalizeText(replayFeedback.value.operatorSummary))
  && !replaySubmitting.value
)
const activeScopeLabel = computed(() => {
  const query = queryState.value
  if (query.productId != null) {
    return `产品 ${query.productId}`
  }
  if (query.subjectType || query.subjectId != null) {
    return `${query.subjectType || '主题'} ${query.subjectId ?? ''}`.trim()
  }
  if (query.workItemCode) {
    return workItemCodeLabel(query.workItemCode)
  }
  return '全部治理任务'
})
const activeStatusLabel = computed(() => workStatusLabel(queryState.value.workStatus))

onMounted(() => {
  void loadWorkItems()
})

function buildQueryFromRoute(): GovernanceWorkItemPageQuery {
  return {
    workItemCode: parseStringQuery(route.query.workItemCode),
    workStatus: parseStringQuery(route.query.workStatus) || 'OPEN',
    subjectType: parseStringQuery(route.query.subjectType),
    subjectId: parseNumberQuery(route.query.subjectId),
    productId: parseNumberQuery(route.query.productId),
    riskMetricId: parseNumberQuery(route.query.riskMetricId),
    assigneeUserId: parseNumberQuery(route.query.assigneeUserId),
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function loadWorkItems() {
  try {
    const response = await pageGovernanceWorkItems(queryState.value)
    taskList.value = applyPageResult(response.data)
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '治理任务加载失败'))
  }
}

function handleRefresh() {
  void loadWorkItems()
}

function handlePageChange(page: number) {
  setPageNum(page)
  void loadWorkItems()
}

function handleSizeChange(size: number) {
  setPageSize(size)
  void loadWorkItems()
}

function workItemCodeLabel(code?: string | null, snapshotJson?: string | null) {
  const coverageType = snapshotValue(snapshotJson, 'coverageType')
  switch (code) {
    case 'PENDING_PRODUCT_GOVERNANCE':
      return '待治理产品'
    case 'PENDING_CONTRACT_RELEASE':
      return '待发布合同'
    case 'PENDING_RISK_BINDING':
      return '待绑定风险点'
    case 'PENDING_THRESHOLD_POLICY':
      return '待补阈值'
    case 'PENDING_LINKAGE_PLAN':
      return coverageType === 'LINKAGE' ? '待补联动规则' : '待补联动预案'
    case 'PENDING_REPLAY':
      return '待运营复盘'
    default:
      return code || '--'
  }
}

function workStatusLabel(status?: string | null) {
  switch (status) {
    case 'ACKED':
      return '已确认'
    case 'BLOCKED':
      return '已阻塞'
    case 'RESOLVED':
      return '已解决'
    case 'CLOSED':
      return '已关闭'
    case 'OPEN':
      return '待处理'
    default:
      return status || '--'
  }
}

function workItemAnchor(item: GovernanceWorkItem) {
  return item.productKey
    || item.deviceCode
    || item.traceId
    || snapshotValue(item.snapshotJson, 'productKey')
    || snapshotValue(item.snapshotJson, 'deviceCode')
    || snapshotValue(item.snapshotJson, 'dimensionLabel')
    || snapshotValue(item.snapshotJson, 'metricIdentifier')
    || '--'
}

function snapshotValue(snapshotJson: string | null | undefined, key: string) {
  const snapshot = parseSnapshot(snapshotJson)
  const value = snapshot?.[key]
  return typeof value === 'string' || typeof value === 'number' ? String(value) : undefined
}

function parseSnapshot(snapshotJson: string | null | undefined) {
  if (!snapshotJson) {
    return undefined
  }
  try {
    const parsed = JSON.parse(snapshotJson) as Record<string, unknown>
    return typeof parsed === 'object' && parsed != null ? parsed : undefined
  } catch {
    return undefined
  }
}

function hasRecommendation(item: GovernanceWorkItem) {
  return Boolean(normalizeText(item.recommendation?.suggestedAction))
    || Boolean(normalizeText(item.recommendation?.recommendationType))
    || recommendationConfidenceText(item.recommendation) != null
    || recommendationEvidenceItems(item).length > 0
}

function hasImpact(item: GovernanceWorkItem) {
  return impactSummaryText(item.impact) !== '--'
    || rollbackabilityText(item.impact, item.rollback) !== '--'
    || Boolean(impactPlanSummary(item.impact, item.rollback))
}

function recommendationConfidenceText(recommendation?: GovernanceRecommendationSnapshot | null) {
  const confidence = recommendation?.confidence
  return typeof confidence === 'number' && Number.isFinite(confidence) ? confidence.toFixed(2) : undefined
}

function recommendationEvidenceItems(item: GovernanceWorkItem) {
  return (item.recommendation?.evidenceItems ?? []).slice(0, 3)
}

function impactSummaryText(impact?: GovernanceImpactSnapshot | null) {
  if (!impact) {
    return '--'
  }
  const count = typeof impact.affectedCount === 'number' && Number.isFinite(impact.affectedCount)
    ? String(impact.affectedCount)
    : '--'
  const types = (impact.affectedTypes ?? []).filter((value): value is string => Boolean(normalizeText(value)))
  return types.length ? `${count} · ${types.join(', ')}` : count
}

function rollbackabilityText(impact?: GovernanceImpactSnapshot | null, rollback?: GovernanceRollbackSnapshot | null) {
  const rollbackable = impact?.rollbackable ?? rollback?.rollbackable
  if (rollbackable == null) {
    return '--'
  }
  return rollbackable ? 'Rollbackable' : 'Manual rollback'
}

function impactPlanSummary(impact?: GovernanceImpactSnapshot | null, rollback?: GovernanceRollbackSnapshot | null) {
  return normalizeText(impact?.rollbackPlanSummary) || normalizeText(rollback?.rollbackPlanSummary)
}

function canOperateWorkItem(item: GovernanceWorkItem) {
  return item.id != null && item.workStatus !== 'CLOSED' && item.workStatus !== 'RESOLVED'
}

function canReplayWorkItem(item: GovernanceWorkItem) {
  return item.workItemCode === 'PENDING_REPLAY'
    && (
      item.productId != null
      || item.releaseBatchId != null
      || Boolean(normalizeText(item.traceId))
      || Boolean(normalizeText(item.deviceCode))
      || Boolean(normalizeText(item.productKey))
      || Boolean(snapshotValue(item.snapshotJson, 'traceId'))
      || Boolean(snapshotValue(item.snapshotJson, 'deviceCode'))
      || Boolean(snapshotValue(item.snapshotJson, 'productKey'))
      || snapshotNumberValue(item.snapshotJson, 'releaseBatchId') != null
    )
}

function canExplainDecision(item: GovernanceWorkItem) {
  return item.id != null
}

function canDispatchWorkItem(item: GovernanceWorkItem) {
  return buildGovernanceTaskDispatchLocation(item) != null
}

async function handleDispatchWorkItem(item: GovernanceWorkItem) {
  const location = buildGovernanceTaskDispatchLocation(item)
  if (!location) {
    return
  }
  await router.push(location)
}

async function handleOpenReplay(item: GovernanceWorkItem) {
  if (!canReplayWorkItem(item)) {
    return
  }
  replaySourceItem.value = item
  resetReplayFeedback(item)
  replayVisible.value = true
  replayLoading.value = true
  replayErrorMessage.value = ''
  replayData.value = null
  try {
    const replayQuery = await buildReplayQuery(item)
    if (!replayQuery) {
      throw new Error('当前任务缺少可复盘上下文')
    }
    const response = await getRiskGovernanceReplay(replayQuery)
    replayData.value = response.data ?? null
    if (!normalizeText(replayFeedback.value.rootCauseCode)) {
      replayFeedback.value.rootCauseCode = defaultReplayRootCause(response.data ?? null)
    }
  } catch (error) {
    replayErrorMessage.value = resolveRequestErrorMessage(error, '治理链路复盘加载失败')
  } finally {
    replayLoading.value = false
  }
}

async function handleSubmitReplayFeedback() {
  if (!replayCanSubmit.value) {
    ElMessage.error('请先补全复盘结论后再提交')
    return
  }
  const sourceItem = replaySourceItem.value
  if (!sourceItem) {
    ElMessage.error('当前复盘上下文不存在')
    return
  }
  replaySubmitting.value = true
  try {
    await submitGovernanceReplayFeedback(compactReplayFeedbackPayload({
      workItemId: sourceItem.id,
      approvalOrderId: sourceItem.approvalOrderId ?? null,
      releaseBatchId: replayData.value?.releaseBatchId ?? sourceItem.releaseBatchId ?? null,
      traceId: replayData.value?.traceId ?? normalizeText(sourceItem.traceId) ?? null,
      deviceCode: replayData.value?.deviceCode ?? normalizeText(sourceItem.deviceCode) ?? null,
      productKey: replayData.value?.productKey ?? normalizeText(sourceItem.productKey) ?? null,
      recommendedDecision: normalizeText(replayFeedback.value.recommendedDecision) ?? null,
      adoptedDecision: replayFeedback.value.adoptedDecision,
      executionOutcome: replayFeedback.value.executionOutcome,
      rootCauseCode: replayFeedback.value.rootCauseCode,
      operatorSummary: normalizeText(replayFeedback.value.operatorSummary) ?? null
    }))
    ElMessage.success('复盘结论已回写')
    replayVisible.value = false
    replaySourceItem.value = null
    await loadWorkItems()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '复盘结论回写失败'))
  } finally {
    replaySubmitting.value = false
  }
}

async function handleOpenDecisionContext(item: GovernanceWorkItem) {
  if (item.id == null) {
    return
  }
  decisionContextVisible.value = true
  decisionContextLoading.value = true
  decisionContextErrorMessage.value = ''
  decisionContextData.value = null
  try {
    const response = await getGovernanceWorkItemDecisionContext(item.id)
    decisionContextData.value = response.data ?? null
  } catch (error) {
    decisionContextErrorMessage.value = resolveRequestErrorMessage(error, '治理任务决策说明加载失败')
  } finally {
    decisionContextLoading.value = false
  }
}

async function handleWorkItemAction(action: 'ack' | 'block' | 'close', item: GovernanceWorkItem) {
  if (item.id == null) {
    return
  }
  const actionMap = {
    ack: {
      title: '确认治理任务',
      message: '确认将该治理任务标记为已确认并进入跟进状态吗？',
      confirmButtonText: '确认',
      comment: '治理任务已确认，进入跟进状态。',
      successMessage: '治理任务已确认',
      execute: ackGovernanceWorkItem
    },
    block: {
      title: '阻塞治理任务',
      message: '确认将该治理任务标记为阻塞并保留在工作台吗？',
      confirmButtonText: '确认阻塞',
      comment: '治理任务存在阻塞，待补外部条件。',
      successMessage: '治理任务已标记阻塞',
      execute: blockGovernanceWorkItem
    },
    close: {
      title: '关闭治理任务',
      message: '确认关闭该治理任务吗？关闭后仅在历史状态中保留。',
      confirmButtonText: '确认关闭',
      comment: '治理任务已人工关闭。',
      successMessage: '治理任务已关闭',
      execute: closeGovernanceWorkItem
    }
  } as const
  const config = actionMap[action]
  try {
    await confirmAction({
      title: config.title,
      message: config.message,
      confirmButtonText: config.confirmButtonText,
      type: action === 'close' ? 'warning' : 'primary'
    })
    await config.execute(item.id, {
      comment: config.comment
    })
    ElMessage.success(config.successMessage)
    await loadWorkItems()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    ElMessage.error(resolveRequestErrorMessage(error, `${config.title}失败`))
  }
}

async function buildReplayQuery(item: GovernanceWorkItem): Promise<RiskGovernanceReplayQuery | null> {
  const query: RiskGovernanceReplayQuery = {}
  const traceId = normalizeText(item.traceId) || snapshotValue(item.snapshotJson, 'traceId')
  const deviceCode = normalizeText(item.deviceCode) || snapshotValue(item.snapshotJson, 'deviceCode')
  const releaseBatchId = item.releaseBatchId ?? snapshotNumberValue(item.snapshotJson, 'releaseBatchId') ?? null
  let productKey = normalizeText(item.productKey) || snapshotValue(item.snapshotJson, 'productKey')
  if (!productKey && item.productId != null) {
    const response = await productApi.getProductById(item.productId)
    productKey = normalizeText(response.data?.productKey)
  }
  if (traceId) {
    query.traceId = traceId
  }
  if (deviceCode) {
    query.deviceCode = deviceCode
  }
  if (productKey) {
    query.productKey = productKey
  }
  if (releaseBatchId != null) {
    query.releaseBatchId = releaseBatchId
  }
  return Object.keys(query).length > 0 ? query : null
}

function parseStringQuery(value: unknown) {
  if (Array.isArray(value)) {
    return parseStringQuery(value[0])
  }
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed || undefined
}

function normalizeText(value?: string | null) {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed || undefined
}

function parseNumberQuery(value: unknown) {
  const text = parseStringQuery(value)
  if (!text || !/^-?\d+$/.test(text)) {
    return undefined
  }
  return Number(text)
}

function snapshotNumberValue(snapshotJson: string | null | undefined, key: string) {
  const snapshot = parseSnapshot(snapshotJson)
  const value = snapshot?.[key]
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && /^-?\d+$/.test(value.trim())) {
    return Number(value.trim())
  }
  return undefined
}

function replayChainStatusLabel(status?: string | null) {
  switch (status) {
    case 'READY':
      return '已就绪'
    case 'ACTION_REQUIRED':
      return '待处理'
    case 'MISSING':
      return '缺失'
    case 'SKIPPED':
      return '已跳过'
    default:
      return status || '--'
  }
}

function booleanLabel(value?: boolean | null, trueLabel = '是', falseLabel = '否') {
  if (value == null) {
    return '--'
  }
  return value ? trueLabel : falseLabel
}

function resetReplayFeedback(item: GovernanceWorkItem) {
  const recommendedDecision = normalizeText(item.recommendation?.recommendationType) || 'REPLAY'
  replayFeedback.value = {
    workItemId: item.id,
    approvalOrderId: item.approvalOrderId ?? null,
    releaseBatchId: item.releaseBatchId ?? null,
    traceId: normalizeText(item.traceId) ?? null,
    deviceCode: normalizeText(item.deviceCode) ?? null,
    productKey: normalizeText(item.productKey) ?? null,
    recommendedDecision,
    adoptedDecision: recommendedDecision,
    executionOutcome: '',
    rootCauseCode: '',
    operatorSummary: ''
  }
}

function defaultReplayRootCause(data?: RiskGovernanceReplay | null) {
  if ((data?.gapSummary?.missingPolicyCount ?? 0) > 0) {
    return 'MISSING_POLICY'
  }
  if ((data?.gapSummary?.missingBindingCount ?? 0) > 0) {
    return 'MISSING_BINDING'
  }
  if ((data?.gapSummary?.missingRiskMetricCount ?? 0) > 0) {
    return 'MISSING_RISK_METRIC'
  }
  return ''
}

function compactReplayFeedbackPayload(payload: GovernanceReplayFeedbackPayload) {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => value !== null && value !== undefined && value !== '')
  ) as GovernanceReplayFeedbackPayload
}
</script>

<style scoped>
.governance-task-view,
.governance-task-result,
.governance-task-list,
.governance-task-empty {
  display: grid;
}

.governance-task-view {
  gap: 0.75rem;
}

.governance-task-detail-stack,
.governance-task-detail-section,
.governance-task-chain-list,
.governance-task-feedback-form,
.governance-task-feedback-actions {
  display: grid;
}

.governance-task-detail-stack {
  gap: 1rem;
}

.governance-task-detail-section {
  gap: 0.75rem;
}

.governance-task-detail-section h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
}

.governance-task-detail-grid,
.governance-task-gap-grid,
.governance-task-feedback-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 0.75rem;
}

.governance-task-detail-field,
.governance-task-gap-card,
.governance-task-chain-item,
.governance-task-feedback-field {
  display: grid;
  gap: 0.35rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.88);
  padding: 0.85rem 1rem;
}

.governance-task-detail-field span,
.governance-task-gap-card span,
.governance-task-chain-item span,
.governance-task-feedback-field span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-task-detail-field strong,
.governance-task-gap-card strong,
.governance-task-chain-item strong,
.governance-task-feedback-field strong {
  color: var(--text-heading);
}

.governance-task-feedback-field--full {
  grid-column: 1 / -1;
}

.governance-task-feedback-input {
  width: 100%;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.98);
  color: var(--text-heading);
  padding: 0.55rem 0.7rem;
  font: inherit;
}

.governance-task-feedback-textarea {
  resize: vertical;
  min-height: 5.5rem;
}

.governance-task-feedback-actions {
  justify-content: flex-end;
}

.governance-task-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.governance-task-summary__item,
.governance-task-card,
.governance-task-empty {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.9);
  padding: 0.9rem 1rem;
}

.governance-task-summary__item {
  display: grid;
  gap: 0.3rem;
}

.governance-task-summary__item span,
.governance-task-card__heading span,
.governance-task-card__meta dt,
.governance-task-card__reason,
.governance-task-empty span {
  color: var(--text-caption);
}

.governance-task-summary__item strong,
.governance-task-card__heading strong,
.governance-task-card__meta dd,
.governance-task-empty strong {
  color: var(--text-heading);
}

.governance-task-list {
  gap: 0.75rem;
}

.governance-task-card {
  display: grid;
  gap: 0.8rem;
}

.governance-task-card__decision,
.governance-task-card__decision-block,
.governance-task-card__evidence-list,
.governance-task-card__evidence-item {
  display: grid;
}

.governance-task-card__decision {
  gap: 0.6rem;
}

.governance-task-card__decision-block {
  gap: 0.5rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(250, 250, 245, 0.92);
  padding: 0.8rem 0.9rem;
}

.governance-task-card__decision-header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.governance-task-card__decision-tag {
  border-radius: var(--radius-pill);
  background: rgba(26, 77, 46, 0.08);
  color: var(--accent-deep);
  padding: 0.18rem 0.55rem;
  font-size: 0.78rem;
}

.governance-task-card__evidence-list {
  gap: 0.4rem;
}

.governance-task-card__evidence-item {
  gap: 0.2rem;
}

.governance-task-card__evidence-item span,
.governance-task-card__decision-copy {
  color: var(--text-caption);
  margin: 0;
  line-height: 1.5;
}

.governance-task-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.governance-task-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.governance-task-card__status-group,
.governance-task-decision-chip-list {
  display: flex;
  flex-wrap: wrap;
}

.governance-task-card__heading {
  display: grid;
  gap: 0.25rem;
}

.governance-task-card__status-group {
  gap: 0.45rem;
  justify-content: flex-end;
}

.governance-task-card__priority,
.governance-task-card__status {
  border-radius: var(--radius-pill);
  background: var(--info-bg);
  color: var(--accent-deep);
  padding: 0.2rem 0.55rem;
  font-size: 0.78rem;
}

.governance-task-card__priority {
  background: rgba(153, 103, 8, 0.12);
  color: #8c5a00;
}

.governance-task-decision-chip-list {
  gap: 0.55rem;
}

.governance-task-decision-chip {
  border-radius: var(--radius-pill);
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-heading);
  padding: 0.28rem 0.7rem;
  font-size: 0.82rem;
}

.governance-task-card__reason {
  margin: 0;
  line-height: 1.6;
}

.governance-task-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.governance-task-card__meta div {
  display: grid;
  gap: 0.25rem;
}

.governance-task-card__meta dt,
.governance-task-card__meta dd {
  margin: 0;
}

.governance-task-empty {
  justify-items: start;
  gap: 0.35rem;
}

@media (max-width: 720px) {
  .governance-task-summary,
  .governance-task-card__meta {
    grid-template-columns: 1fr;
  }

  .governance-task-card__header {
    flex-direction: column;
  }
}
</style>
