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
                <strong>{{ workItemCodeLabel(item.workItemCode) }}</strong>
                <span>{{ item.productKey || item.deviceCode || item.traceId || '--' }}</span>
              </div>
              <span class="governance-task-card__status">{{ workStatusLabel(item.workStatus) }}</span>
            </header>
            <p class="governance-task-card__reason">{{ item.blockingReason || '暂无阻塞说明，建议优先查看对象上下文。' }}</p>
            <dl class="governance-task-card__meta">
              <div>
                <dt>主题</dt>
                <dd>{{ item.subjectType || '--' }} / {{ item.subjectId ?? '--' }}</dd>
              </div>
              <div>
                <dt>产品</dt>
                <dd>{{ item.productId ?? '--' }}</dd>
              </div>
              <div>
                <dt>风险指标</dt>
                <dd>{{ item.riskMetricId ?? '--' }}</dd>
              </div>
              <div>
                <dt>更新时间</dt>
                <dd>{{ item.updateTime || item.createTime || '--' }}</dd>
              </div>
            </dl>
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
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { pageGovernanceWorkItems } from '@/api/governanceWorkItem'
import PanelCard from '@/components/PanelCard.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import { useServerPagination } from '@/composables/useServerPagination'
import type { GovernanceWorkItem, GovernanceWorkItemPageQuery } from '@/types/api'

const route = useRoute()
const { pagination, applyPageResult, setPageNum, setPageSize } = useServerPagination()

const taskList = ref<GovernanceWorkItem[]>([])
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
  const response = await pageGovernanceWorkItems(queryState.value)
  taskList.value = applyPageResult(response.data)
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

function workItemCodeLabel(code?: string | null) {
  switch (code) {
    case 'PENDING_CONTRACT_RELEASE':
      return '待发布合同'
    case 'PENDING_RISK_BINDING':
      return '待绑定风险点'
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
.governance-task-view,
.governance-task-result,
.governance-task-list,
.governance-task-empty {
  display: grid;
}

.governance-task-view {
  gap: 0.75rem;
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

.governance-task-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.governance-task-card__heading {
  display: grid;
  gap: 0.25rem;
}

.governance-task-card__status {
  border-radius: var(--radius-pill);
  background: var(--info-bg);
  color: var(--accent-deep);
  padding: 0.2rem 0.55rem;
  font-size: 0.78rem;
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
