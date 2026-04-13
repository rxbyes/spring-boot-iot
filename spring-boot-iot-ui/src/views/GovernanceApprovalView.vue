<template>
  <StandardPageShell class="governance-approval-view">
    <StandardWorkbenchPanel
      title="治理审批台"
      :description="`统一查看治理审批单、状态流转和执行结果，当前共 ${pagination.total} 条。`"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader :model="filters">
          <template #primary>
            <el-form-item>
              <el-input v-model="filters.actionCode" placeholder="动作编码" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.subjectType" placeholder="主题类型" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.subjectId" placeholder="主题 ID" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="filters.status" placeholder="审批状态" clearable>
                <el-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.operatorUserId" placeholder="执行人 ID" clearable @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="filters.approverUserId" placeholder="复核人 ID" clearable @keyup.enter="handleSearch" />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `当前 ${pagination.total} 单`,
            `待审批 ${pendingCount} 单`,
            `我待处理 ${myPendingCount} 单`,
            `我提交 ${mySubmittedCount} 单`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading"
        class="governance-approval-result-panel standard-list-surface"
        element-loading-text="正在刷新治理审批单"
        element-loading-background="var(--loading-mask-bg)"
      >
        <template v-if="orderList.length > 0">
          <div class="governance-approval-mobile-list standard-mobile-record-list">
            <div class="governance-approval-mobile-list__grid standard-mobile-record-grid">
              <article
                v-for="row in orderList"
                :key="String(row.id)"
                class="governance-approval-mobile-card standard-mobile-record-card"
              >
                <div class="governance-approval-mobile-card__header">
                  <div class="governance-approval-mobile-card__heading">
                    <strong class="governance-approval-mobile-card__title">{{ row.actionName || row.actionCode || '--' }}</strong>
                    <span class="governance-approval-mobile-card__sub">
                      {{ row.subjectType || '--' }} · {{ row.subjectId ?? '--' }}
                    </span>
                  </div>
                  <el-tag :type="approvalStatusTagType(row.status)" round>{{ approvalStatusLabel(row.status) }}</el-tag>
                </div>

                <div class="governance-approval-mobile-card__meta">
                  <span class="standard-mobile-record-card__meta-item">执行人 {{ row.operatorUserId ?? '--' }}</span>
                  <span class="standard-mobile-record-card__meta-item">复核人 {{ row.approverUserId ?? '--' }}</span>
                  <span class="standard-mobile-record-card__meta-item">{{ row.createTime || '--' }}</span>
                </div>

                <StandardWorkbenchRowActions
                  variant="card"
                  :direct-items="buildRowActions(row)"
                  @command="(command) => handleRowAction(String(command), row)"
                />
              </article>
            </div>
          </div>

          <el-table :data="orderList" border stripe style="width: 100%">
            <StandardTableTextColumn prop="actionName" label="动作名称" :min-width="160" />
            <StandardTableTextColumn prop="actionCode" label="动作编码" :min-width="220" />
            <StandardTableTextColumn prop="subjectType" label="主题类型" :width="120" />
            <StandardTableTextColumn prop="subjectId" label="主题 ID" :min-width="120" />
            <StandardTableTextColumn prop="operatorUserId" label="执行人" :width="110" />
            <StandardTableTextColumn prop="approverUserId" label="复核人" :width="110" />
            <StandardTableTextColumn prop="createTime" label="提交时间" :width="180" />
            <el-table-column label="审批状态" width="120">
              <template #default="{ row }">
                <el-tag :type="approvalStatusTagType(row.status)" round>{{ approvalStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="260"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="buildRowActions(row)"
                  @command="(command) => handleRowAction(String(command), row)"
                />
              </template>
            </el-table-column>
          </el-table>
        </template>

        <div v-else-if="!loading" class="standard-list-empty-state">
          <EmptyState title="暂无治理审批单" description="可先从产品治理、合同回滚或策略写侧提交新的审批动作。" />
          <div class="standard-list-empty-state__actions">
            <StandardButton v-if="hasAppliedFilters" action="reset" @click="handleClearAppliedFilters">清空筛选条件</StandardButton>
          </div>
        </div>
      </div>

      <template #pagination>
        <div v-if="pagination.total > 0" class="ops-pagination">
          <StandardPagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </StandardWorkbenchPanel>

    <StandardDetailDrawer
      v-model="detailVisible"
      :title="detailTitle"
      subtitle="按审批主单回看请求载荷、执行结果和状态流转。"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :empty="!detailOrder"
    >
      <div class="governance-approval-detail-stack">
        <section class="governance-approval-detail-section">
          <h3>审批概览</h3>
          <div class="governance-approval-detail-grid">
            <div class="governance-approval-detail-field">
              <span>动作名称</span>
              <strong>{{ detailOrder?.actionName || detailOrder?.actionCode || '--' }}</strong>
            </div>
            <div class="governance-approval-detail-field">
              <span>审批状态</span>
              <strong>{{ approvalStatusLabel(detailOrder?.status) }}</strong>
            </div>
            <div class="governance-approval-detail-field">
              <span>主题对象</span>
              <strong>{{ detailOrder?.subjectType || '--' }} · {{ detailOrder?.subjectId ?? '--' }}</strong>
            </div>
            <div class="governance-approval-detail-field">
              <span>治理任务 ID</span>
              <strong>{{ detailOrder?.workItemId ?? '--' }}</strong>
            </div>
            <div class="governance-approval-detail-field">
              <span>审批意见</span>
              <strong>{{ detailOrder?.approvalComment || '--' }}</strong>
            </div>
          </div>
        </section>

        <section class="governance-approval-detail-section">
          <h3>执行结果</h3>
          <pre class="governance-approval-json-preview">{{ executionResultText }}</pre>
        </section>

        <section v-if="shouldShowSimulationSection" class="governance-approval-detail-section">
          <h3>审批预演</h3>
          <p v-if="detailSimulationLoading" class="governance-approval-detail-empty">正在加载审批预演...</p>
          <p v-else-if="detailSimulationErrorMessage" class="governance-approval-detail-empty">{{ detailSimulationErrorMessage }}</p>
          <template v-else-if="detailSimulation">
            <div class="governance-approval-impact-summary">
              <div class="governance-approval-impact-summary__card">
                <span>可执行</span>
                <strong>{{ detailSimulation.executable ? '可执行' : '需人工复核' }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>影响规模</span>
                <strong>{{ `预计影响 ${detailSimulation.affectedCount ?? 0} 项` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>影响类型</span>
                <strong>{{ formatAffectedTypes(detailSimulation.affectedTypes) || '--' }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>回滚性</span>
                <strong>{{ detailSimulation.rollbackable ? '可回滚' : '需人工恢复' }}</strong>
              </div>
            </div>

            <div
              v-if="detailSimulation.recommendation?.evidenceItems?.length"
              class="governance-approval-impact-list"
            >
              <article
                v-for="(item, index) in detailSimulation.recommendation.evidenceItems.slice(0, 3)"
                :key="`${item.sourceId || item.title || '--'}-${index}`"
                class="governance-approval-impact-item"
              >
                <strong>{{ item.title || item.evidenceType || '--' }}</strong>
                <span>{{ item.summary || '--' }}</span>
                <span>{{ item.sourceType || '--' }} · {{ item.sourceId || '--' }}</span>
              </article>
            </div>

            <div class="governance-approval-detail-grid">
              <div v-if="detailSimulation.recommendation" class="governance-approval-detail-field">
                <span>推荐建议</span>
                <strong>
                  {{
                    detailSimulation.recommendation.suggestedAction
                      || detailSimulation.recommendation.recommendationType
                      || '--'
                  }}
                </strong>
              </div>
              <div v-if="detailSimulation.recommendation?.confidence != null" class="governance-approval-detail-field">
                <span>置信度</span>
                <strong>{{ `confidence ${detailSimulation.recommendation.confidence}` }}</strong>
              </div>
              <div v-if="detailSimulation.rollbackPlanSummary" class="governance-approval-detail-field">
                <span>回滚方案</span>
                <strong>{{ detailSimulation.rollbackPlanSummary }}</strong>
              </div>
              <div v-if="detailSimulation.autoDraftEligible && detailSimulation.autoDraftComment" class="governance-approval-detail-field">
                <span>自动草稿</span>
                <strong>{{ detailSimulation.autoDraftComment }}</strong>
              </div>
            </div>
          </template>
          <p v-else class="governance-approval-detail-empty">当前审批单暂未生成预演结果。</p>
        </section>

        <section v-if="shouldShowImpactSection" class="governance-approval-detail-section">
          <h3>发布影响分析</h3>
          <p v-if="detailImpactLoading" class="governance-approval-detail-empty">正在加载发布影响分析...</p>
          <p v-else-if="detailImpactErrorMessage" class="governance-approval-detail-empty">{{ detailImpactErrorMessage }}</p>
          <template v-else-if="detailImpact">
            <div class="governance-approval-impact-summary">
              <div class="governance-approval-impact-summary__card">
                <span>新增</span>
                <strong>{{ `新增 ${detailImpact.addedCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>删除</span>
                <strong>{{ `删除 ${detailImpact.removedCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>变更</span>
                <strong>{{ `变更 ${detailImpact.changedCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>未变更</span>
                <strong>{{ `未变更 ${detailImpact.unchangedCount ?? 0}` }}</strong>
              </div>
            </div>

            <div
              v-if="detailImpact.dependencySummary"
              class="governance-approval-impact-summary governance-approval-impact-summary--dependency"
            >
              <div class="governance-approval-impact-summary__card">
                <span>受影响风险指标</span>
                <strong>{{ `受影响风险指标 ${detailImpact.dependencySummary.affectedRiskMetricCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>受影响风险点绑定</span>
                <strong>{{ `受影响风险点绑定 ${detailImpact.dependencySummary.affectedRiskPointBindingCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>受影响阈值规则</span>
                <strong>{{ `受影响阈值规则 ${detailImpact.dependencySummary.affectedRuleCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>受影响联动</span>
                <strong>{{ `受影响联动 ${detailImpact.dependencySummary.affectedLinkageBindingCount ?? 0}` }}</strong>
              </div>
              <div class="governance-approval-impact-summary__card">
                <span>受影响预案</span>
                <strong>{{ `受影响预案 ${detailImpact.dependencySummary.affectedEmergencyPlanBindingCount ?? 0}` }}</strong>
              </div>
            </div>

            <div v-if="detailImpact.dependencySummary?.affectedRiskMetrics?.length" class="governance-approval-impact-dependency-group">
              <div class="governance-approval-impact-dependency-group__head">
                <strong>受影响风险指标目录</strong>
                <span>{{ `共 ${detailImpact.dependencySummary.affectedRiskMetrics.length} 项` }}</span>
              </div>
              <div class="governance-approval-impact-list">
                <article
                  v-for="item in detailImpact.dependencySummary.affectedRiskMetrics"
                  :key="`${item.riskMetricId || item.contractIdentifier || '--'}`"
                  class="governance-approval-impact-item"
                >
                  <strong>{{ item.riskMetricName || item.contractIdentifier || '--' }}</strong>
                  <span>{{ item.contractIdentifier || '--' }} · {{ item.riskMetricCode || '--' }}</span>
                  <span>{{ item.metricRole || '--' }} · {{ item.lifecycleStatus || '--' }}</span>
                </article>
              </div>
            </div>

            <div v-if="detailImpact.dependencySummary?.affectedRiskPointBindings?.length" class="governance-approval-impact-dependency-group">
              <div class="governance-approval-impact-dependency-group__head">
                <strong>受影响风险点绑定</strong>
                <span>{{ `共 ${detailImpact.dependencySummary.affectedRiskPointBindings.length} 项` }}</span>
              </div>
              <div class="governance-approval-impact-list">
                <article
                  v-for="item in detailImpact.dependencySummary.affectedRiskPointBindings"
                  :key="`${item.bindingId || item.riskPointId || item.deviceCode || '--'}`"
                  class="governance-approval-impact-item"
                >
                  <strong>{{ item.riskPointName || item.deviceCode || '--' }}</strong>
                  <span>{{ item.deviceCode || '--' }} · {{ item.metricIdentifier || '--' }}</span>
                  <StandardButton action="query" link @click="openRiskPointContext(item)">查看风险对象</StandardButton>
                </article>
              </div>
            </div>

            <div v-if="detailImpact.dependencySummary?.affectedRules?.length" class="governance-approval-impact-dependency-group">
              <div class="governance-approval-impact-dependency-group__head">
                <strong>受影响阈值策略</strong>
                <span>{{ `共 ${detailImpact.dependencySummary.affectedRules.length} 项` }}</span>
              </div>
              <div class="governance-approval-impact-list">
                <article
                  v-for="item in detailImpact.dependencySummary.affectedRules"
                  :key="`${item.ruleId || item.ruleName || '--'}`"
                  class="governance-approval-impact-item"
                >
                  <strong>{{ item.ruleName || '--' }}</strong>
                  <span>{{ item.metricIdentifier || '--' }} · {{ item.alarmLevel || '--' }}</span>
                  <StandardButton action="query" link @click="openRuleContext(item)">查看阈值策略</StandardButton>
                </article>
              </div>
            </div>

            <div v-if="detailImpact.dependencySummary?.affectedLinkageBindings?.length" class="governance-approval-impact-dependency-group">
              <div class="governance-approval-impact-dependency-group__head">
                <strong>受影响联动编排</strong>
                <span>{{ `共 ${detailImpact.dependencySummary.affectedLinkageBindings.length} 项` }}</span>
              </div>
              <div class="governance-approval-impact-list">
                <article
                  v-for="item in detailImpact.dependencySummary.affectedLinkageBindings"
                  :key="`${item.bindingId || item.linkageRuleId || '--'}`"
                  class="governance-approval-impact-item"
                >
                  <strong>{{ item.linkageRuleName || '--' }}</strong>
                  <span>{{ item.bindingStatus || '--' }}</span>
                  <StandardButton action="query" link @click="openLinkageContext(item)">查看联动编排</StandardButton>
                </article>
              </div>
            </div>

            <div v-if="detailImpact.dependencySummary?.affectedEmergencyPlanBindings?.length" class="governance-approval-impact-dependency-group">
              <div class="governance-approval-impact-dependency-group__head">
                <strong>受影响应急预案</strong>
                <span>{{ `共 ${detailImpact.dependencySummary.affectedEmergencyPlanBindings.length} 项` }}</span>
              </div>
              <div class="governance-approval-impact-list">
                <article
                  v-for="item in detailImpact.dependencySummary.affectedEmergencyPlanBindings"
                  :key="`${item.bindingId || item.emergencyPlanId || '--'}`"
                  class="governance-approval-impact-item"
                >
                  <strong>{{ item.emergencyPlanName || '--' }}</strong>
                  <span>{{ item.alarmLevel || '--' }} · {{ item.bindingStatus || '--' }}</span>
                  <StandardButton action="query" link @click="openEmergencyPlanContext(item)">查看应急预案</StandardButton>
                </article>
              </div>
            </div>

            <div v-if="detailImpact.impactItems?.length" class="governance-approval-impact-list">
              <article
                v-for="(item, index) in detailImpact.impactItems"
                :key="`${item.identifier || '--'}-${item.changeType || '--'}-${index}`"
                class="governance-approval-impact-item"
              >
                <strong>{{ item.identifier || '--' }}</strong>
                <span>{{ impactChangeTypeLabel(item.changeType) }} · {{ impactModelTypeLabel(item.modelType) }}</span>
                <span v-if="item.changedFields?.length">变更字段 {{ item.changedFields.join(' / ') }}</span>
              </article>
            </div>
            <p v-else class="governance-approval-detail-empty">当前发布批次没有字段级差异明细。</p>
          </template>
          <p v-else class="governance-approval-detail-empty">当前审批单未携带可复盘的发布批次信息。</p>
        </section>

        <section class="governance-approval-detail-section">
          <h3>状态流转</h3>
          <div v-if="detailTransitions.length" class="governance-approval-timeline">
            <article
              v-for="transition in detailTransitions"
              :key="String(transition.id || transition.createTime || transition.toStatus)"
              class="governance-approval-timeline__item"
            >
              <strong>{{ approvalStatusLabel(transition.toStatus) }}</strong>
              <span>{{ transition.transitionComment || '--' }}</span>
              <span>操作人 {{ transition.actorUserId ?? '--' }} · {{ transition.createTime || '--' }}</span>
            </article>
          </div>
          <p v-else class="governance-approval-detail-empty">暂无状态流转记录。</p>
        </section>
      </div>
    </StandardDetailDrawer>

    <StandardFormDrawer
      v-model="actionDrawerVisible"
      :title="actionDrawerTitle"
      :subtitle="actionDrawerSubtitle"
      size="40rem"
      @close="handleActionDrawerClose"
    >
      <div class="governance-approval-action-stack">
        <el-form-item v-if="actionMode === 'resubmit'" label="复核人用户 ID">
          <el-input
            v-model="actionApproverUserId"
            placeholder="请输入新的复核人用户 ID"
          />
        </el-form-item>
        <div v-if="actionMode === 'approve'" class="governance-approval-draft-banner">
          <p v-if="actionSimulationLoading" class="governance-approval-detail-empty">正在加载审批预演...</p>
          <p v-else-if="actionSimulationErrorMessage" class="governance-approval-detail-empty">{{ actionSimulationErrorMessage }}</p>
          <template v-else-if="actionSimulation">
            <strong>审批预演</strong>
            <span>
              {{
                `预计影响 ${actionSimulation.affectedCount ?? 0} 项 · ${formatAffectedTypes(actionSimulation.affectedTypes) || '--'}`
              }}
            </span>
            <span v-if="actionSimulation.rollbackPlanSummary">{{ actionSimulation.rollbackPlanSummary }}</span>
            <div
              v-if="actionSimulation.autoDraftEligible && actionSimulation.autoDraftComment"
              class="governance-approval-draft-banner__actions"
            >
              <span>系统已填入可编辑审批意见草稿。</span>
              <StandardButton action="query" link @click="applyActionDraftComment">重新填入草稿</StandardButton>
            </div>
          </template>
        </div>
        <el-form-item label="处理意见">
          <el-input
            v-model="actionComment"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见，可留空；驳回场景建议明确原因"
          />
        </el-form-item>
      </div>

      <template #footer>
        <StandardDrawerFooter
          :confirm-text="actionConfirmText"
          :confirm-loading="submitLoading"
          @cancel="handleActionDrawerClose"
          @confirm="handleSubmitAction"
        />
      </template>
    </StandardFormDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { governanceApprovalApi } from '@/api/governanceApproval'
import {
  productApi,
  type ProductContractReleaseEmergencyPlanBindingDetail,
  type ProductContractReleaseImpact,
  type ProductContractReleaseLinkageBindingDetail,
  type ProductContractReleaseRiskPointBindingDetail,
  type ProductContractReleaseRuleDetail
} from '@/api/product'
import { isHandledRequestError, resolveRequestErrorMessage } from '@/api/request'
import EmptyState from '@/components/EmptyState.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardDrawerFooter from '@/components/StandardDrawerFooter.vue'
import StandardFormDrawer from '@/components/StandardFormDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { usePermissionStore } from '@/stores/permission'
import {
  buildEmergencyPlanContextLocation,
  buildLinkageContextLocation,
  buildRiskPointContextLocation,
  buildRuleContextLocation
} from '@/utils/governanceImpact'
import type {
  GovernanceApprovalOrder,
  GovernanceApprovalOrderDetail,
  GovernanceApprovalPageQuery,
  GovernanceSimulationResult,
  GovernanceApprovalStatus,
  GovernanceApprovalTransition,
  IdType
} from '@/types/api'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'

type ActionMode = 'approve' | 'reject' | 'cancel' | 'resubmit' | null

const router = useRouter()
const permissionStore = usePermissionStore()
const { pagination, applyPageResult, resetPage, setPageNum, setPageSize } = useServerPagination()

const statusOptions: Array<{ label: string; value: GovernanceApprovalStatus }> = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已撤销', value: 'CANCELLED' }
]

const filters = reactive({
  actionCode: '',
  subjectType: '',
  subjectId: '',
  status: '' as GovernanceApprovalStatus | '',
  operatorUserId: '',
  approverUserId: ''
})
const appliedFilters = reactive({
  actionCode: '',
  subjectType: '',
  subjectId: '',
  status: '' as GovernanceApprovalStatus | '',
  operatorUserId: '',
  approverUserId: ''
})

const loading = ref(false)
const orderList = ref<GovernanceApprovalOrder[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailErrorMessage = ref('')
const detailData = ref<GovernanceApprovalOrderDetail | null>(null)
const detailSimulationLoading = ref(false)
const detailSimulationErrorMessage = ref('')
const detailSimulation = ref<GovernanceSimulationResult | null>(null)
const detailImpactLoading = ref(false)
const detailImpactErrorMessage = ref('')
const detailImpact = ref<ProductContractReleaseImpact | null>(null)

const actionDrawerVisible = ref(false)
const actionMode = ref<ActionMode>(null)
const actionTargetOrder = ref<GovernanceApprovalOrder | null>(null)
const actionSimulationLoading = ref(false)
const actionSimulationErrorMessage = ref('')
const actionSimulation = ref<GovernanceSimulationResult | null>(null)
const actionComment = ref('')
const actionApproverUserId = ref('')
const submitLoading = ref(false)

const currentUserId = computed<number | null>(() => {
  const userId = permissionStore.userInfo?.id
  return typeof userId === 'number' ? userId : typeof userId === 'string' && /^\d+$/.test(userId) ? Number(userId) : null
})

const pendingCount = computed(() => orderList.value.filter((item) => item.status === 'PENDING').length)
const myPendingCount = computed(() => orderList.value.filter((item) => item.status === 'PENDING' && sameId(item.approverUserId, currentUserId.value)).length)
const mySubmittedCount = computed(() => orderList.value.filter((item) => sameId(item.operatorUserId, currentUserId.value)).length)

const detailOrder = computed(() => detailData.value?.order ?? null)
const detailTransitions = computed<GovernanceApprovalTransition[]>(() => detailData.value?.transitions ?? [])
const detailTitle = computed(() => {
  const orderId = detailOrder.value?.id
  return orderId == null ? '审批单详情' : `审批单 ${orderId}`
})

const parsedPayload = computed<Record<string, unknown> | null>(() => parsePayload(detailOrder.value?.payloadJson))
const parsedExecution = computed<Record<string, unknown> | null>(() => toRecord(parsedPayload.value?.execution))
const parsedExecutionResult = computed<Record<string, unknown> | null>(() => toRecord(parsedExecution.value?.result))
const detailReleaseBatchId = computed<IdType | null>(() => {
  return normalizeId(parsedExecutionResult.value?.releaseBatchId) ?? normalizeId(parsedExecutionResult.value?.rolledBackBatchId)
})
const shouldShowSimulationSection = computed(() => {
  return detailOrder.value?.status === 'PENDING'
    || detailSimulation.value != null
    || detailSimulationLoading.value
    || Boolean(detailSimulationErrorMessage.value)
})
const shouldShowImpactSection = computed(() => detailReleaseBatchId.value != null || detailImpact.value != null || detailImpactLoading.value || Boolean(detailImpactErrorMessage.value))
const executionResultText = computed(() => {
  if (!parsedExecution.value) {
    return '暂无执行结果'
  }
  return JSON.stringify(parsedExecution.value, null, 2)
})

const actionDrawerTitle = computed(() => {
  switch (actionMode.value) {
    case 'approve':
      return '审批通过'
    case 'reject':
      return '审批驳回'
    case 'cancel':
      return '撤销审批'
    case 'resubmit':
      return '原单重提'
    default:
      return '治理审批操作'
  }
})

const actionDrawerSubtitle = computed(() => {
  switch (actionMode.value) {
    case 'approve':
      return actionSimulation.value?.autoDraftEligible
        ? '审批通过后，系统会执行对应治理动作并回写执行结果。当前已按预演结果生成可编辑草稿。'
        : '审批通过后，系统会执行对应治理动作并回写执行结果。'
    case 'reject':
      return '驳回时建议明确指出当前审批单需要修正的字段或语义问题。'
    case 'cancel':
      return '撤销后当前审批单会结束，后续需重新提交新的审批动作。'
    case 'resubmit':
      return '重提时可重新指定复核人，并保留本轮补充说明。'
    default:
      return '统一处理治理审批动作。'
  }
})

const actionConfirmText = computed(() => {
  switch (actionMode.value) {
    case 'approve':
      return '确认通过'
    case 'reject':
      return '确认驳回'
    case 'cancel':
      return '确认撤销'
    case 'resubmit':
      return '确认重提'
    default:
      return '确认'
  }
})

const {
  tags: activeFilterTags,
  hasAppliedFilters,
  syncAppliedFilters,
  removeFilter,
  clearFilters
} = useListAppliedFilters({
  form: filters,
  applied: appliedFilters,
  fields: [
    { key: 'actionCode', label: '动作编码' },
    { key: 'subjectType', label: '主题类型' },
    { key: 'subjectId', label: '主题 ID' },
    {
      key: 'status',
      label: '审批状态',
      format: (value) => approvalStatusLabel(value as GovernanceApprovalStatus | null)
    },
    { key: 'operatorUserId', label: '执行人 ID' },
    { key: 'approverUserId', label: '复核人 ID' }
  ],
  reset: () => {
    filters.actionCode = ''
    filters.subjectType = ''
    filters.subjectId = ''
    filters.status = ''
    filters.operatorUserId = ''
    filters.approverUserId = ''
  }
})

onMounted(() => {
  void loadOrders()
})

function approvalStatusLabel(status: GovernanceApprovalStatus | null | undefined) {
  switch (status) {
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'CANCELLED':
      return '已撤销'
    case 'PENDING':
      return '待审批'
    default:
      return '--'
  }
}

function approvalStatusTagType(status: GovernanceApprovalStatus | null | undefined) {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'CANCELLED':
      return 'info'
    case 'PENDING':
      return 'warning'
    default:
      return 'info'
  }
}

function buildQuery(): GovernanceApprovalPageQuery {
  const query: GovernanceApprovalPageQuery = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }
  const actionCode = normalizeText(appliedFilters.actionCode)
  const subjectType = normalizeText(appliedFilters.subjectType)
  const subjectId = normalizeId(appliedFilters.subjectId)
  const status = normalizeStatus(appliedFilters.status)
  const operatorUserId = normalizeId(appliedFilters.operatorUserId)
  const approverUserId = normalizeId(appliedFilters.approverUserId)
  if (actionCode != null) {
    query.actionCode = actionCode
  }
  if (subjectType != null) {
    query.subjectType = subjectType
  }
  if (subjectId != null) {
    query.subjectId = subjectId
  }
  if (status != null) {
    query.status = status
  }
  if (operatorUserId != null) {
    query.operatorUserId = operatorUserId
  }
  if (approverUserId != null) {
    query.approverUserId = approverUserId
  }
  return query
}

async function loadOrders() {
  loading.value = true
  try {
    const response = await governanceApprovalApi.pageOrders(buildQuery())
    applyPageResult(response.data)
    orderList.value = response.data?.records ?? []
  } catch (error) {
    orderList.value = []
    showRequestErrorToast(error, '治理审批单加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOrderDetail(orderId: IdType, silent = false) {
  if (orderId == null) {
    return
  }
  detailLoading.value = !silent
  detailErrorMessage.value = ''
  resetDetailSimulation()
  resetDetailImpact()
  try {
    const response = await governanceApprovalApi.getOrderDetail(orderId, { suppressErrorToast: true })
    detailData.value = response.data ?? null
    await loadDetailSimulation(response.data?.order ?? null)
    await loadDetailImpact()
  } catch (error) {
    detailData.value = null
    detailErrorMessage.value = resolveRequestErrorMessage(error, '审批详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function loadDetailImpact() {
  const batchId = detailReleaseBatchId.value
  if (batchId == null) {
    resetDetailImpact()
    return
  }
  detailImpactLoading.value = true
  detailImpactErrorMessage.value = ''
  try {
    const response = await productApi.getProductContractReleaseBatchImpact(batchId, { suppressErrorToast: true })
    detailImpact.value = response.data ?? null
  } catch (error) {
    detailImpact.value = null
    detailImpactErrorMessage.value = resolveRequestErrorMessage(error, '发布影响分析加载失败')
  } finally {
    detailImpactLoading.value = false
  }
}

async function loadDetailSimulation(order: GovernanceApprovalOrder | null | undefined) {
  if (!order || order.id == null || order.status !== 'PENDING') {
    resetDetailSimulation()
    return
  }
  detailSimulationLoading.value = true
  detailSimulationErrorMessage.value = ''
  try {
    const response = await governanceApprovalApi.simulateOrder(order.id, { suppressErrorToast: true })
    detailSimulation.value = response.data ?? null
  } catch (error) {
    detailSimulation.value = null
    detailSimulationErrorMessage.value = resolveRequestErrorMessage(error, '审批预演加载失败')
  } finally {
    detailSimulationLoading.value = false
  }
}

async function loadActionSimulation(order: GovernanceApprovalOrder | null | undefined) {
  if (!order || order.id == null || actionMode.value !== 'approve' || order.status !== 'PENDING') {
    resetActionSimulation()
    return
  }
  actionSimulationLoading.value = true
  actionSimulationErrorMessage.value = ''
  try {
    const response = await governanceApprovalApi.simulateOrder(order.id, { suppressErrorToast: true })
    actionSimulation.value = response.data ?? null
    if (!normalizeText(actionComment.value) && actionSimulation.value?.autoDraftEligible && actionSimulation.value.autoDraftComment) {
      actionComment.value = actionSimulation.value.autoDraftComment
    }
  } catch (error) {
    actionSimulation.value = null
    actionSimulationErrorMessage.value = resolveRequestErrorMessage(error, '审批预演加载失败')
  } finally {
    actionSimulationLoading.value = false
  }
}

function buildRowActions(row: GovernanceApprovalOrder) {
  const currentUser = currentUserId.value
  const canApprove = row.status === 'PENDING' && (currentUser == null || sameId(row.approverUserId, currentUser))
  const canCancel = row.status === 'PENDING' && (currentUser == null || sameId(row.operatorUserId, currentUser))
  const canResubmit = row.status === 'REJECTED' && (currentUser == null || sameId(row.operatorUserId, currentUser))
  const items: Array<{ command: string; label: string; disabled?: boolean }> = [
    { command: 'detail', label: '详情' }
  ]
  if (canApprove) {
    items.push({ command: 'approve', label: '通过' })
    items.push({ command: 'reject', label: '驳回' })
  }
  if (canCancel) {
    items.push({ command: 'cancel', label: '撤销' })
  }
  if (canResubmit) {
    items.push({ command: 'resubmit', label: '原单重提' })
  }
  return items
}

async function handleRowAction(command: string, row: GovernanceApprovalOrder) {
  if (command === 'detail') {
    detailVisible.value = true
    await loadOrderDetail(row.id)
    return
  }
  actionTargetOrder.value = row
  actionMode.value = command as ActionMode
  resetActionSimulation()
  actionComment.value = ''
  actionApproverUserId.value = ''
  actionDrawerVisible.value = true
  if (command === 'approve') {
    await loadActionSimulation(row)
  }
}

function handleSearch() {
  syncAppliedFilters()
  resetPage()
  void loadOrders()
}

function handleReset() {
  clearFilters()
  resetPage()
  void loadOrders()
}

function handleRefresh() {
  void loadOrders()
}

function handlePageChange(pageNum: number) {
  setPageNum(pageNum)
  void loadOrders()
}

function handleSizeChange(pageSize: number) {
  setPageSize(pageSize)
  void loadOrders()
}

function handleRemoveAppliedFilter(key: string) {
  removeFilter(key)
  resetPage()
  void loadOrders()
}

function handleClearAppliedFilters() {
  clearFilters()
  resetPage()
  void loadOrders()
}

function handleActionDrawerClose() {
  actionDrawerVisible.value = false
  actionMode.value = null
  actionTargetOrder.value = null
  resetActionSimulation()
  actionComment.value = ''
  actionApproverUserId.value = ''
}

function applyActionDraftComment() {
  if (actionSimulation.value?.autoDraftComment) {
    actionComment.value = actionSimulation.value.autoDraftComment
  }
}

async function handleSubmitAction() {
  if (!actionMode.value || !actionTargetOrder.value) {
    return
  }
  const order = actionTargetOrder.value
  const payload = {
    comment: normalizeText(actionComment.value)
  }

  if (actionMode.value === 'resubmit') {
    const approverUserId = normalizeId(actionApproverUserId.value)
    if (approverUserId == null) {
      ElMessage.warning('请先填写新的复核人用户 ID')
      return
    }
    await executeActionWithConfirm(async () => {
      await governanceApprovalApi.resubmitOrder(order.id, {
        approverUserId,
        comment: payload.comment
      })
      ElMessage.success('审批单已重新提交')
    })
    return
  }

  if (actionMode.value === 'approve') {
    await executeActionWithConfirm(async () => {
      await governanceApprovalApi.approveOrder(order.id, payload)
      ElMessage.success('审批单已通过')
    })
    return
  }

  if (actionMode.value === 'reject') {
    await executeActionWithConfirm(async () => {
      await governanceApprovalApi.rejectOrder(order.id, payload)
      ElMessage.success('审批单已驳回')
    })
    return
  }

  if (actionMode.value === 'cancel') {
    await executeActionWithConfirm(async () => {
      await governanceApprovalApi.cancelOrder(order.id, payload)
      ElMessage.success('审批单已撤销')
    })
  }
}

async function executeActionWithConfirm(executor: () => Promise<void>) {
  submitLoading.value = true
  try {
    await confirmAction({
      title: actionDrawerTitle.value,
      message: `${actionDrawerTitle.value}后将更新审批主单状态，请确认继续。`,
      type: actionMode.value === 'approve' ? 'primary' : 'warning',
      confirmButtonText: actionConfirmText.value
    })
    await executor()
    const currentDetailOrderId = detailOrder.value?.id
    handleActionDrawerClose()
    await loadOrders()
    if (detailVisible.value && currentDetailOrderId != null) {
      await loadOrderDetail(currentDetailOrderId, true)
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    showRequestErrorToast(error, `${actionDrawerTitle.value}失败`)
  } finally {
    submitLoading.value = false
  }
}

function showRequestErrorToast(error: unknown, fallbackMessage: string) {
  if (isHandledRequestError(error)) {
    return
  }
  ElMessage.error(resolveRequestErrorMessage(error, fallbackMessage))
}

function sameId(left: IdType | null | undefined, right: IdType | null | undefined) {
  if (left == null || right == null) {
    return false
  }
  return String(left) === String(right)
}

function normalizeStatus(status: GovernanceApprovalStatus | '' | null | undefined) {
  return status || null
}

function normalizeText(value: string | null | undefined) {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

function normalizeId(value: IdType | null | undefined) {
  if (value == null) {
    return null
  }
  const text = String(value).trim()
  if (!text) {
    return null
  }
  return /^\d+$/.test(text) ? Number(text) : text
}

function parsePayload(payloadJson: string | null | undefined): Record<string, unknown> | null {
  if (!payloadJson) {
    return null
  }
  try {
    const parsed = JSON.parse(payloadJson) as Record<string, unknown>
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

function resetDetailImpact() {
  detailImpact.value = null
  detailImpactLoading.value = false
  detailImpactErrorMessage.value = ''
}

function resetDetailSimulation() {
  detailSimulation.value = null
  detailSimulationLoading.value = false
  detailSimulationErrorMessage.value = ''
}

function resetActionSimulation() {
  actionSimulation.value = null
  actionSimulationLoading.value = false
  actionSimulationErrorMessage.value = ''
}

function toRecord(value: unknown): Record<string, unknown> | null {
  return value != null && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : null
}

function formatAffectedTypes(affectedTypes: string[] | null | undefined) {
  return affectedTypes?.filter((item) => typeof item === 'string' && item.trim()).join(' / ') ?? ''
}

function impactChangeTypeLabel(changeType: string | null | undefined) {
  switch (changeType) {
    case 'ADDED':
      return '新增'
    case 'REMOVED':
      return '删除'
    case 'UPDATED':
      return '变更'
    case 'UNCHANGED':
      return '未变更'
    default:
      return changeType || '--'
  }
}

function impactModelTypeLabel(modelType: string | null | undefined) {
  switch (modelType) {
    case 'property':
      return '属性'
    case 'event':
      return '事件'
    case 'service':
      return '服务'
    default:
      return modelType || '--'
  }
}

function openRiskPointContext(detail: ProductContractReleaseRiskPointBindingDetail) {
  void router.push(buildRiskPointContextLocation(detail))
}

function openRuleContext(detail: ProductContractReleaseRuleDetail) {
  void router.push(buildRuleContextLocation(detail))
}

function openLinkageContext(detail: ProductContractReleaseLinkageBindingDetail) {
  void router.push(buildLinkageContextLocation(detail))
}

function openEmergencyPlanContext(detail: ProductContractReleaseEmergencyPlanBindingDetail) {
  void router.push(buildEmergencyPlanContextLocation(detail))
}
</script>

<style scoped>
.governance-approval-view {
  display: grid;
}

.governance-approval-result-panel {
  display: grid;
  gap: 1rem;
}

.governance-approval-mobile-list__grid {
  display: grid;
  gap: 1rem;
}

.governance-approval-mobile-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.governance-approval-mobile-card__heading {
  min-width: 0;
  display: grid;
  gap: 0.35rem;
}

.governance-approval-mobile-card__title {
  color: var(--text-heading);
}

.governance-approval-mobile-card__sub {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-approval-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.governance-approval-detail-stack,
.governance-approval-action-stack {
  display: grid;
  gap: 1rem;
}

.governance-approval-draft-banner {
  display: grid;
  gap: 0.45rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.84);
}

.governance-approval-draft-banner strong {
  color: var(--text-heading);
}

.governance-approval-draft-banner span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-approval-draft-banner__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.governance-approval-detail-section {
  display: grid;
  gap: 0.75rem;
}

.governance-approval-detail-section h3 {
  margin: 0;
  font-size: 1rem;
  color: var(--text-heading);
}

.governance-approval-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 0.75rem;
}

.governance-approval-impact-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.75rem;
}

.governance-approval-impact-summary__card,
.governance-approval-impact-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.84);
}

.governance-approval-impact-summary__card strong,
.governance-approval-impact-item strong {
  color: var(--text-heading);
}

.governance-approval-impact-summary__card span,
.governance-approval-impact-item span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-approval-impact-list {
  display: grid;
  gap: 0.75rem;
}

.governance-approval-impact-dependency-group {
  display: grid;
  gap: 0.75rem;
}

.governance-approval-impact-dependency-group__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.governance-approval-impact-dependency-group__head strong {
  color: var(--text-heading);
}

.governance-approval-impact-dependency-group__head span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-approval-detail-field {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--panel-border);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.84);
}

.governance-approval-detail-field span,
.governance-approval-timeline__item span {
  color: var(--text-caption);
  font-size: 13px;
}

.governance-approval-json-preview {
  margin: 0;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--panel-border);
  background: rgba(16, 24, 40, 0.04);
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.governance-approval-timeline {
  display: grid;
  gap: 0.75rem;
}

.governance-approval-timeline__item {
  display: grid;
  gap: 0.3rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--panel-border);
  background: rgba(255, 255, 255, 0.84);
}

.governance-approval-detail-empty {
  margin: 0;
  color: var(--text-caption);
}
</style>
