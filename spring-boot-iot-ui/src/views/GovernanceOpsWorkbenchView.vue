<template>
  <StandardPageShell class="governance-ops-view">
    <StandardWorkbenchPanel
      title="治理运维台"
      :description="`统一查看字段漂移、合同差异与风险指标缺失告警，当前共 ${pagination.total} 条。`"
      show-toolbar
      show-pagination
    >
      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前 ${pagination.total} 条`,
            `待处理 ${openCount} 条`,
            activeScopeLabel
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <PanelCard
        title="运维导向"
        description="这里集中回看配置变更、字段漂移和合同差异，避免控制面变更无声失真。"
      >
        <div class="governance-ops-summary">
          <div class="governance-ops-summary__item">
            <span>当前告警范围</span>
            <strong>{{ activeScopeLabel }}</strong>
          </div>
          <div class="governance-ops-summary__item">
            <span>默认告警状态</span>
            <strong>{{ activeStatusLabel }}</strong>
          </div>
        </div>
      </PanelCard>

      <div class="governance-ops-result">
        <div v-if="alertList.length" class="governance-ops-list">
          <article
            v-for="item in alertList"
            :key="String(item.id)"
            class="governance-ops-card"
          >
            <header class="governance-ops-card__header">
              <div class="governance-ops-card__heading">
                <strong>{{ alertTypeLabel(item.alertType) }}</strong>
                <span>{{ item.alertTitle || item.alertCode || item.dimensionLabel || item.traceId || '--' }}</span>
              </div>
              <span class="governance-ops-card__status">{{ alertStatusLabel(item.alertStatus) }}</span>
            </header>
            <p class="governance-ops-card__message">{{ item.alertMessage || '暂无告警说明。' }}</p>
            <section v-if="hasRecommendation(item) || hasImpact(item)" class="governance-ops-card__decision">
              <div v-if="hasRecommendation(item)" class="governance-ops-card__decision-block">
                <div class="governance-ops-card__decision-header">
                  <strong>{{ item.recommendation?.suggestedAction || item.recommendation?.recommendationType || 'Recommendation' }}</strong>
                  <span v-if="recommendationConfidenceText(item.recommendation)" class="governance-ops-card__decision-tag">
                    {{ recommendationConfidenceText(item.recommendation) }}
                  </span>
                </div>
                <div v-if="recommendationEvidenceItems(item).length" class="governance-ops-card__evidence-list">
                  <article
                    v-for="(evidence, index) in recommendationEvidenceItems(item)"
                    :key="`${String(item.id)}-evidence-${index}`"
                    class="governance-ops-card__evidence-item"
                  >
                    <strong>{{ evidence.evidenceType || evidence.sourceType || 'EVIDENCE' }}</strong>
                    <span>{{ evidence.title || evidence.summary || evidence.sourceId || '--' }}</span>
                  </article>
                </div>
              </div>
              <div v-if="hasImpact(item)" class="governance-ops-card__decision-block">
                <div class="governance-ops-card__decision-header">
                  <strong>{{ impactSummaryText(item.impact) }}</strong>
                  <span class="governance-ops-card__decision-tag">{{ rollbackabilityText(item.impact, item.rollback) }}</span>
                </div>
                <p v-if="impactPlanSummary(item.impact, item.rollback)" class="governance-ops-card__decision-copy">
                  {{ impactPlanSummary(item.impact, item.rollback) }}
                </p>
              </div>
            </section>
            <dl class="governance-ops-card__meta">
              <div>
                <dt>严重级别</dt>
                <dd>{{ item.severityLevel || '--' }}</dd>
              </div>
              <div>
                <dt>影响范围</dt>
                <dd>{{ item.affectedCount ?? '--' }}</dd>
              </div>
              <div>
                <dt>主题</dt>
                <dd>{{ item.subjectType || '--' }} / {{ item.subjectId ?? '--' }}</dd>
              </div>
              <div>
                <dt>最后出现</dt>
                <dd>{{ item.lastSeenTime || item.createTime || '--' }}</dd>
              </div>
            </dl>
            <div v-if="canOperateAlert(item) || hasReplayContext(item)" class="governance-ops-card__actions">
              <StandardButton v-if="hasReplayContext(item)" v-permission="'system:governance-ops:replay'" @click="handleOpenReplay(item)">复盘</StandardButton>
              <StandardButton v-if="canOperateAlert(item)" v-permission="'system:governance-ops:ack'" @click="handleAlertAction('ack', item)">确认</StandardButton>
              <StandardButton v-if="canOperateAlert(item)" v-permission="'system:governance-ops:suppress'" @click="handleAlertAction('suppress', item)">抑制</StandardButton>
              <StandardButton v-if="canOperateAlert(item)" v-permission="'system:governance-ops:close'" @click="handleAlertAction('close', item)">关闭</StandardButton>
            </div>
          </article>
        </div>
        <div v-else class="governance-ops-empty">
          <strong>当前没有匹配的治理运维告警</strong>
          <span>可调整 query 条件，或继续在链路追踪、审计中心和审批台交叉复核。</span>
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
      v-model="replayVisible"
      title="治理链路复盘"
      subtitle="统一回看发布批次、消息轨迹、失败归档与治理缺口。"
      :loading="replayLoading"
      :error-message="replayErrorMessage"
      :empty="!replayData"
    >
      <div class="governance-ops-detail-stack">
        <section class="governance-ops-detail-section">
          <h3>复盘概览</h3>
          <div class="governance-ops-detail-grid">
            <div class="governance-ops-detail-field">
              <span>发布批次</span>
              <strong>{{ replayData?.releaseBatchId != null ? `发布批次 ${replayData.releaseBatchId}` : '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>TraceId</span>
              <strong>{{ replayData?.traceId || '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>设备编码</span>
              <strong>{{ replayData?.deviceCode || '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>产品标识</span>
              <strong>{{ replayData?.productKey || '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>命中消息</span>
              <strong>{{ replayData?.matchedMessageCount ?? 0 }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>命中失败归档</span>
              <strong>{{ replayData?.matchedAccessErrorCount ?? 0 }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.gapSummary" class="governance-ops-detail-section">
          <h3>治理缺口</h3>
          <div class="governance-ops-gap-grid">
            <div class="governance-ops-gap-card">
              <span>待补绑定</span>
              <strong>{{ `待补绑定 ${replayData.gapSummary.missingBindingCount ?? 0}` }}</strong>
            </div>
            <div class="governance-ops-gap-card">
              <span>待补策略</span>
              <strong>{{ `待补策略 ${replayData.gapSummary.missingPolicyCount ?? 0}` }}</strong>
            </div>
            <div class="governance-ops-gap-card">
              <span>待补指标目录</span>
              <strong>{{ `待补指标目录 ${replayData.gapSummary.missingRiskMetricCount ?? 0}` }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.batchReconciliation" class="governance-ops-detail-section">
          <h3>批次对账</h3>
          <div class="governance-ops-detail-grid">
            <div class="governance-ops-detail-field">
              <span>对账结果</span>
              <strong>{{ booleanLabel(replayData.batchReconciliation.consistent, '一致', '需处理') }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>快照可用</span>
              <strong>{{ booleanLabel(replayData.batchReconciliation.snapshotAvailable) }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>发布状态</span>
              <strong>{{ replayData.batchReconciliation.releaseStatus || '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>审批单</span>
              <strong>{{ replayData.batchReconciliation.approvalOrderId ?? '--' }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>缺失正式字段</span>
              <strong>{{ replayData.batchReconciliation.missingCurrentFieldCount ?? 0 }}</strong>
            </div>
            <div class="governance-ops-detail-field">
              <span>多余正式字段</span>
              <strong>{{ replayData.batchReconciliation.extraCurrentFieldCount ?? 0 }}</strong>
            </div>
          </div>
        </section>

        <section v-if="replayData?.replayChainSteps?.length" class="governance-ops-detail-section">
          <h3>复盘链路</h3>
          <div class="governance-ops-chain-list">
            <article
              v-for="(step, index) in replayData.replayChainSteps"
              :key="`${step.stepCode || '--'}-${index}`"
              class="governance-ops-chain-item"
            >
              <strong>{{ step.stepCode || step.stepName || '--' }}</strong>
              <span>{{ step.stepName || '--' }} · {{ replayChainStatusLabel(step.status) }}</span>
              <span>{{ step.summary || '--' }}</span>
              <span>{{ step.nextAction || '--' }}</span>
            </article>
          </div>
        </section>

        <section class="governance-ops-detail-section">
          <h3>复盘结论回写</h3>
          <div class="governance-ops-feedback-form">
            <label class="governance-ops-feedback-field">
              <span>推荐结论</span>
              <strong>{{ replayRecommendedDecision }}</strong>
            </label>
            <label class="governance-ops-feedback-field">
              <span>采纳结论</span>
              <select
                data-testid="ops-replay-adopted-decision"
                v-model="replayFeedback.adoptedDecision"
                class="governance-ops-feedback-input"
              >
                <option value="">请选择</option>
                <option value="PROMOTE">PROMOTE</option>
                <option value="PUBLISH">PUBLISH</option>
                <option value="CREATE_POLICY">CREATE_POLICY</option>
                <option value="REPLAY">REPLAY</option>
                <option value="IGNORE">IGNORE</option>
              </select>
            </label>
            <label class="governance-ops-feedback-field">
              <span>执行结果</span>
              <select
                data-testid="ops-replay-execution-outcome"
                v-model="replayFeedback.executionOutcome"
                class="governance-ops-feedback-input"
              >
                <option value="">请选择</option>
                <option value="SUCCESS">SUCCESS</option>
                <option value="PARTIAL_SUCCESS">PARTIAL_SUCCESS</option>
                <option value="FAILED">FAILED</option>
                <option value="IGNORED">IGNORED</option>
              </select>
            </label>
            <label class="governance-ops-feedback-field">
              <span>根因分类</span>
              <select
                data-testid="ops-replay-root-cause"
                v-model="replayFeedback.rootCauseCode"
                class="governance-ops-feedback-input"
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
            <label class="governance-ops-feedback-field governance-ops-feedback-field--full">
              <span>操作结论</span>
              <textarea
                data-testid="ops-replay-operator-summary"
                v-model="replayFeedback.operatorSummary"
                class="governance-ops-feedback-input governance-ops-feedback-textarea"
                rows="3"
                placeholder="请说明本次复盘最终采纳了什么结论、为何这样处理。"
              />
            </label>
          </div>
          <div class="governance-ops-feedback-actions">
            <StandardButton v-permission="'system:governance-ops:replay-feedback'" @click="handleSubmitReplayFeedback">
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
import { useRoute } from 'vue-router'
import { ElMessage } from '@/utils/message'

import { resolveRequestErrorMessage } from '@/api/request'
import { ackGovernanceOpsAlert, closeGovernanceOpsAlert, pageGovernanceOpsAlerts, suppressGovernanceOpsAlert } from '@/api/governanceOpsAlert'
import {
  getRiskGovernanceReplay,
  submitGovernanceReplayFeedback,
  type RiskGovernanceReplay,
  type RiskGovernanceReplayQuery
} from '@/api/riskGovernance'
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
  GovernanceImpactSnapshot,
  GovernanceOpsAlert,
  GovernanceOpsAlertPageQuery,
  GovernanceReplayFeedbackPayload,
  GovernanceRecommendationSnapshot,
  GovernanceRollbackSnapshot
} from '@/types/api'

const route = useRoute()
const { pagination, applyPageResult, setPageNum, setPageSize } = useServerPagination()

const alertList = ref<GovernanceOpsAlert[]>([])
const replayVisible = ref(false)
const replayLoading = ref(false)
const replayErrorMessage = ref('')
const replayData = ref<RiskGovernanceReplay | null>(null)
const replaySourceAlert = ref<GovernanceOpsAlert | null>(null)
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
const openCount = computed(() => alertList.value.filter((item) => item.alertStatus === 'OPEN').length)
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
  if (query.alertType) {
    return alertTypeLabel(query.alertType)
  }
  return '全部治理运维告警'
})
const activeStatusLabel = computed(() => alertStatusLabel(queryState.value.alertStatus))

onMounted(() => {
  void loadAlerts()
})

function buildQueryFromRoute(): GovernanceOpsAlertPageQuery {
  return {
    alertType: parseStringQuery(route.query.alertType),
    alertStatus: parseStringQuery(route.query.alertStatus) || 'OPEN',
    subjectType: parseStringQuery(route.query.subjectType),
    subjectId: parseNumberQuery(route.query.subjectId),
    productId: parseNumberQuery(route.query.productId),
    riskMetricId: parseNumberQuery(route.query.riskMetricId),
    severityLevel: parseStringQuery(route.query.severityLevel),
    assigneeUserId: parseNumberQuery(route.query.assigneeUserId),
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
}

async function loadAlerts() {
  try {
    const response = await pageGovernanceOpsAlerts(queryState.value)
    alertList.value = applyPageResult(response.data)
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '治理运维告警加载失败'))
  }
}

function handleRefresh() {
  void loadAlerts()
}

function handlePageChange(page: number) {
  setPageNum(page)
  void loadAlerts()
}

function handleSizeChange(size: number) {
  setPageSize(size)
  void loadAlerts()
}

function alertTypeLabel(type?: string | null) {
  switch (type) {
    case 'FIELD_DRIFT':
      return '字段漂移告警'
    case 'CONTRACT_DIFF':
      return '合同差异告警'
    case 'MISSING_RISK_METRIC':
      return '风险指标缺失告警'
    default:
      return type || '--'
  }
}

function alertStatusLabel(status?: string | null) {
  switch (status) {
    case 'ACKED':
      return '已确认'
    case 'SUPPRESSED':
      return '已抑制'
    case 'RESOLVED':
      return '已恢复'
    case 'CLOSED':
      return '已关闭'
    case 'OPEN':
      return '待处理'
    default:
      return status || '--'
  }
}

function canOperateAlert(item: GovernanceOpsAlert) {
  return item.id != null && item.alertStatus !== 'CLOSED' && item.alertStatus !== 'RESOLVED'
}

function hasReplayContext(item: GovernanceOpsAlert) {
  return item.releaseBatchId != null
    || Boolean(normalizeText(item.traceId))
    || Boolean(normalizeText(item.deviceCode))
    || Boolean(normalizeText(item.productKey))
}

function hasRecommendation(item: GovernanceOpsAlert) {
  return Boolean(normalizeText(item.recommendation?.suggestedAction))
    || Boolean(normalizeText(item.recommendation?.recommendationType))
    || recommendationConfidenceText(item.recommendation) != null
    || recommendationEvidenceItems(item).length > 0
}

function hasImpact(item: GovernanceOpsAlert) {
  return impactSummaryText(item.impact) !== '--'
    || rollbackabilityText(item.impact, item.rollback) !== '--'
    || Boolean(impactPlanSummary(item.impact, item.rollback))
}

function recommendationConfidenceText(recommendation?: GovernanceRecommendationSnapshot | null) {
  const confidence = recommendation?.confidence
  return typeof confidence === 'number' && Number.isFinite(confidence) ? confidence.toFixed(2) : undefined
}

function recommendationEvidenceItems(item: GovernanceOpsAlert) {
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

async function handleOpenReplay(item: GovernanceOpsAlert) {
  if (!hasReplayContext(item)) {
    return
  }
  replaySourceAlert.value = item
  resetReplayFeedback(item)
  replayVisible.value = true
  replayLoading.value = true
  replayErrorMessage.value = ''
  replayData.value = null
  try {
    const response = await getRiskGovernanceReplay(buildReplayQuery(item))
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
  const sourceAlert = replaySourceAlert.value
  if (!sourceAlert) {
    ElMessage.error('当前复盘上下文不存在')
    return
  }
  replaySubmitting.value = true
  try {
    await submitGovernanceReplayFeedback(compactReplayFeedbackPayload({
      releaseBatchId: replayData.value?.releaseBatchId ?? sourceAlert.releaseBatchId ?? null,
      traceId: replayData.value?.traceId ?? normalizeText(sourceAlert.traceId) ?? null,
      deviceCode: replayData.value?.deviceCode ?? normalizeText(sourceAlert.deviceCode) ?? null,
      productKey: replayData.value?.productKey ?? normalizeText(sourceAlert.productKey) ?? null,
      recommendedDecision: normalizeText(replayFeedback.value.recommendedDecision) ?? null,
      adoptedDecision: replayFeedback.value.adoptedDecision,
      executionOutcome: replayFeedback.value.executionOutcome,
      rootCauseCode: replayFeedback.value.rootCauseCode,
      operatorSummary: normalizeText(replayFeedback.value.operatorSummary) ?? null
    }))
    ElMessage.success('复盘结论已回写')
    replayVisible.value = false
    replaySourceAlert.value = null
    await loadAlerts()
  } catch (error) {
    ElMessage.error(resolveRequestErrorMessage(error, '复盘结论回写失败'))
  } finally {
    replaySubmitting.value = false
  }
}

async function handleAlertAction(action: 'ack' | 'suppress' | 'close', item: GovernanceOpsAlert) {
  if (item.id == null) {
    return
  }
  const actionMap = {
    ack: {
      title: '确认治理告警',
      message: '确认将该治理运维告警标记为已确认吗？',
      confirmButtonText: '确认',
      comment: '治理告警已确认，进入持续跟进。',
      successMessage: '治理告警已确认',
      execute: ackGovernanceOpsAlert
    },
    suppress: {
      title: '抑制治理告警',
      message: '确认暂时抑制该治理运维告警吗？',
      confirmButtonText: '确认抑制',
      comment: '治理告警暂时抑制，等待下一轮检测。',
      successMessage: '治理告警已抑制',
      execute: suppressGovernanceOpsAlert
    },
    close: {
      title: '关闭治理告警',
      message: '确认关闭该治理运维告警吗？',
      confirmButtonText: '确认关闭',
      comment: '治理告警已人工关闭。',
      successMessage: '治理告警已关闭',
      execute: closeGovernanceOpsAlert
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
    await loadAlerts()
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    ElMessage.error(resolveRequestErrorMessage(error, `${config.title}失败`))
  }
}

function buildReplayQuery(item: GovernanceOpsAlert): RiskGovernanceReplayQuery {
  return {
    traceId: normalizeText(item.traceId),
    deviceCode: normalizeText(item.deviceCode),
    productKey: normalizeText(item.productKey),
    releaseBatchId: item.releaseBatchId ?? null
  }
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

function resetReplayFeedback(item: GovernanceOpsAlert) {
  const recommendedDecision = normalizeText(item.recommendation?.recommendationType) || 'REPLAY'
  replayFeedback.value = {
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
.governance-ops-view,
.governance-ops-result,
.governance-ops-list,
.governance-ops-empty {
  display: grid;
}

.governance-ops-view {
  gap: 0.75rem;
}

.governance-ops-detail-stack,
.governance-ops-detail-section,
.governance-ops-chain-list,
.governance-ops-feedback-form,
.governance-ops-feedback-actions {
  display: grid;
}

.governance-ops-detail-stack {
  gap: 1rem;
}

.governance-ops-detail-section {
  gap: 0.75rem;
}

.governance-ops-detail-section h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 1rem;
}

.governance-ops-detail-grid,
.governance-ops-gap-grid,
.governance-ops-feedback-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 0.75rem;
}

.governance-ops-detail-field,
.governance-ops-gap-card,
.governance-ops-chain-item,
.governance-ops-feedback-field {
  display: grid;
  gap: 0.35rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.88);
  padding: 0.85rem 1rem;
}

.governance-ops-detail-field span,
.governance-ops-gap-card span,
.governance-ops-chain-item span,
.governance-ops-feedback-field span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-ops-detail-field strong,
.governance-ops-gap-card strong,
.governance-ops-chain-item strong,
.governance-ops-feedback-field strong {
  color: var(--text-heading);
}

.governance-ops-feedback-field--full {
  grid-column: 1 / -1;
}

.governance-ops-feedback-input {
  width: 100%;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.98);
  color: var(--text-heading);
  padding: 0.55rem 0.7rem;
  font: inherit;
}

.governance-ops-feedback-textarea {
  resize: vertical;
  min-height: 5.5rem;
}

.governance-ops-feedback-actions {
  justify-content: flex-end;
}

.governance-ops-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.governance-ops-summary__item,
.governance-ops-card,
.governance-ops-empty {
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.9);
  padding: 0.9rem 1rem;
}

.governance-ops-summary__item {
  display: grid;
  gap: 0.3rem;
}

.governance-ops-summary__item span,
.governance-ops-card__heading span,
.governance-ops-card__meta dt,
.governance-ops-card__message,
.governance-ops-empty span {
  color: var(--text-caption);
}

.governance-ops-summary__item strong,
.governance-ops-card__heading strong,
.governance-ops-card__meta dd,
.governance-ops-empty strong {
  color: var(--text-heading);
}

.governance-ops-list {
  gap: 0.75rem;
}

.governance-ops-card {
  display: grid;
  gap: 0.8rem;
}

.governance-ops-card__decision,
.governance-ops-card__decision-block,
.governance-ops-card__evidence-list,
.governance-ops-card__evidence-item {
  display: grid;
}

.governance-ops-card__decision {
  gap: 0.6rem;
}

.governance-ops-card__decision-block {
  gap: 0.5rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(250, 248, 240, 0.92);
  padding: 0.8rem 0.9rem;
}

.governance-ops-card__decision-header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.governance-ops-card__decision-tag {
  border-radius: var(--radius-pill);
  background: rgba(138, 90, 34, 0.12);
  color: var(--warning);
  padding: 0.18rem 0.55rem;
  font-size: 0.78rem;
}

.governance-ops-card__evidence-list {
  gap: 0.4rem;
}

.governance-ops-card__evidence-item {
  gap: 0.2rem;
}

.governance-ops-card__evidence-item span,
.governance-ops-card__decision-copy {
  color: var(--text-caption);
  margin: 0;
  line-height: 1.5;
}

.governance-ops-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.governance-ops-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.governance-ops-card__heading {
  display: grid;
  gap: 0.25rem;
}

.governance-ops-card__status {
  border-radius: var(--radius-pill);
  background: var(--warning-bg);
  color: var(--warning);
  padding: 0.2rem 0.55rem;
  font-size: 0.78rem;
}

.governance-ops-card__message {
  margin: 0;
  line-height: 1.6;
}

.governance-ops-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.governance-ops-card__meta div {
  display: grid;
  gap: 0.25rem;
}

.governance-ops-card__meta dt,
.governance-ops-card__meta dd {
  margin: 0;
}

.governance-ops-empty {
  justify-items: start;
  gap: 0.35rem;
}

@media (max-width: 720px) {
  .governance-ops-summary,
  .governance-ops-card__meta {
    grid-template-columns: 1fr;
  }

  .governance-ops-card__header {
    flex-direction: column;
  }
}
</style>
