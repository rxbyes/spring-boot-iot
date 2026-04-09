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
                <span>{{ item.alertTitle || item.alertCode || '--' }}</span>
              </div>
              <span class="governance-ops-card__status">{{ alertStatusLabel(item.alertStatus) }}</span>
            </header>
            <p class="governance-ops-card__message">{{ item.alertMessage || '暂无告警说明。' }}</p>
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { pageGovernanceOpsAlerts } from '@/api/governanceOpsAlert'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import type { GovernanceOpsAlert, GovernanceOpsAlertPageQuery } from '@/types/api'

const route = useRoute()
const { pagination, applyPageResult, setPageNum, setPageSize } = useServerPagination()

const alertList = ref<GovernanceOpsAlert[]>([])
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
  const response = await pageGovernanceOpsAlerts(queryState.value)
  alertList.value = applyPageResult(response.data)
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
    case 'CLOSED':
      return '已关闭'
    case 'OPEN':
      return '待处理'
    default:
      return status || '--'
  }
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

function parseNumberQuery(value: unknown) {
  const text = parseStringQuery(value)
  if (!text || !/^-?\d+$/.test(text)) {
    return undefined
  }
  return Number(text)
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
