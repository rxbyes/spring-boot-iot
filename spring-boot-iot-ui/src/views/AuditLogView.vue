<template>
  <StandardPageShell class="page-stack audit-log-view" :show-title="false">
    <StandardWorkbenchPanel
      :title="panelTitle"
      :description="pageDescription"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-toolbar
      :show-inline-state="showSystemInlineState"
      show-pagination
    >
      <template #filters>
        <StandardListFilterHeader
          :model="searchForm"
          :show-advanced="showAdvancedFilters"
          show-advanced-toggle
          :advanced-hint="advancedFilterHint"
          @toggle-advanced="toggleAdvancedFilters"
        >
          <template #primary>
            <el-form-item>
              <el-input
                id="quick-search"
                v-model="quickSearchKeyword"
                :placeholder="quickSearchPlaceholder"
                clearable
                prefix-icon="Search"
                @keyup.enter="handleQuickSearch"
                @clear="handleClearQuickSearch"
              />
            </el-form-item>
            <el-form-item v-if="isBusinessMode">
              <el-select v-model="searchForm.operationType" placeholder="操作类型" clearable>
                <el-option
                  v-for="item in businessOperationTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="searchForm.operationModule"
                :placeholder="isSystemMode ? '异常模块' : '操作模块'"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchForm.operationResult" placeholder="操作结果" clearable>
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-select v-model="searchForm.requestMethod" :placeholder="isSystemMode ? '请求通道' : '请求方法'" clearable>
                <el-option
                  v-for="item in systemRequestMethodOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </template>
          <template #advanced>
            <el-form-item v-if="isBusinessMode">
              <el-input
                v-model="searchForm.traceId"
                placeholder="TraceId"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item v-if="isSystemMode">
              <el-input
                v-model="searchForm.requestUrl"
                placeholder="目标 / URL"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <template v-if="isSystemMode">
              <el-form-item>
                <el-input
                  v-model="searchForm.deviceCode"
                  placeholder="设备编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.productKey"
                  placeholder="产品标识"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.errorCode"
                  placeholder="异常编码"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.exceptionClass"
                  placeholder="异常类型"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
            </template>
          </template>
          <template #actions>
            <StandardButton action="query" @click="handleSearch">查询</StandardButton>
            <StandardButton action="reset" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
        <div v-if="appliedQuickSearchValue" class="audit-log-quick-search-tag">
          <el-tag closable class="audit-log-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ appliedQuickSearchValue }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="handleRemoveAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template v-if="showSystemInlineState" #inline-state>
        <StandardInlineState :message="systemInlineMessage" tone="info" />
      </template>

      <section
        v-if="isSystemMode"
        v-loading="slowSummaryLoading"
        class="audit-log-slow-summary standard-list-surface"
        aria-label="性能慢点热点"
        element-loading-text="正在刷新慢点热点"
        element-loading-background="var(--loading-mask-bg)"
      >
        <header class="audit-log-slow-summary__header">
          <div>
            <h3>性能慢点 Top</h3>
          </div>
          <span>{{ slowSummaryRows.length }} 项</span>
        </header>
        <div v-if="slowSummaryErrorMessage" class="audit-log-slow-summary__empty">
          {{ slowSummaryErrorMessage }}
        </div>
        <div v-else-if="slowSummaryRows.length === 0" class="audit-log-slow-summary__empty">
          暂无慢点热点
        </div>
        <div v-else class="audit-log-slow-summary__grid">
          <article
            v-for="row in slowSummaryRows"
            :key="`${row.spanType || 'span'}-${row.domainCode || 'domain'}-${row.eventCode || 'event'}-${row.objectType || 'object'}-${row.objectId || 'id'}`"
            class="audit-log-slow-summary__item"
          >
            <div class="audit-log-slow-summary__title">
              <strong>{{ formatSlowSummaryTitle(row) }}</strong>
              <span>{{ formatValue(row.latestStartedAt) }}</span>
            </div>
            <p>{{ formatSlowSummaryTarget(row) }}</p>
            <div class="audit-log-slow-summary__metrics">
              <span>最大 {{ formatDuration(row.maxDurationMs) }}</span>
              <span>平均 {{ formatDuration(row.avgDurationMs) }}</span>
              <span>{{ formatCount(row.totalCount) }} 次</span>
            </div>
            <div class="audit-log-slow-summary__footer">
              <span>{{ formatValue(row.latestTraceId) }}</span>
              <StandardButton
                action="view"
                link
                :disabled="!row.latestTraceId"
                @click="openTraceEvidenceByTraceId(row.latestTraceId)"
              >
                证据
              </StandardButton>
              <StandardButton
                action="view"
                link
                @click="loadSlowSpanDrilldown(row)"
              >
                明细
              </StandardButton>
            </div>
          </article>
        </div>
        <section
          v-if="activeSlowSummary"
          v-loading="slowSpanLoading"
          class="audit-log-slow-span-drilldown"
          aria-label="慢点调用片段明细"
          element-loading-text="正在刷新慢点明细"
          element-loading-background="var(--loading-mask-bg)"
        >
          <header class="audit-log-slow-span-drilldown__header">
            <div>
              <h3>慢点明细</h3>
              <p>{{ formatSlowSummaryTitle(activeSlowSummary) }} · {{ formatSlowSummaryTarget(activeSlowSummary) }}</p>
            </div>
            <span>{{ slowSpanTotal }} 条</span>
          </header>
          <div v-if="slowSpanErrorMessage" class="audit-log-slow-summary__empty">
            {{ slowSpanErrorMessage }}
          </div>
          <div v-else-if="slowSpanRows.length === 0" class="audit-log-slow-summary__empty">
            暂无慢点明细
          </div>
          <div v-else class="audit-log-slow-span-drilldown__list">
            <article
              v-for="span in slowSpanRows"
              :key="`slow-span-${span.id || span.traceId || span.startedAt}`"
              class="audit-log-slow-span-drilldown__item"
            >
              <div class="audit-log-slow-span-drilldown__title">
                <strong>{{ formatValue(span.spanName || span.spanType) }}</strong>
                <span>{{ formatDuration(span.durationMs) }}</span>
              </div>
              <div class="audit-log-slow-span-drilldown__meta">
                <span>{{ formatValue(span.traceId) }}</span>
                <span>{{ formatValue(span.status) }}</span>
                <span>{{ formatValue(span.startedAt) }}</span>
              </div>
              <div class="audit-log-slow-span-drilldown__footer">
                <span>{{ formatValue(span.eventCode) }} / {{ formatValue(span.objectId) }}</span>
                <StandardButton
                  action="view"
                  link
                  :disabled="!span.traceId"
                  @click="openTraceEvidenceByTraceId(span.traceId)"
                >
                  证据
                </StandardButton>
              </div>
            </article>
          </div>
        </section>
      </section>

      <template #toolbar>
        <StandardTableToolbar
          compact
          :meta-items="[
            `已选 ${selectedRows.length} 项`,
            isSystemMode ? `异常 ${systemStats.total}` : `审计 ${businessStats.total}`,
            isSystemMode
              ? `今日 ${systemStats.todayCount}`
              : `成功 ${businessStats.successCount}`,
            isSystemMode
              ? `链路 ${systemStats.distinctTraceCount}`
              : `失败 ${businessStats.failureCount}`
          ]"
        >
          <template #right>
            <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            <StandardActionMenu
              label="更多操作"
              :items="auditToolbarActions"
              @command="handleToolbarAction"
            />
          </template>
        </StandardTableToolbar>
      </template>

      <div
        v-loading="loading"
        class="audit-log-table-wrap standard-list-surface"
        element-loading-text="正在刷新审计列表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <div v-if="tableData.length > 0" class="audit-log-mobile-list standard-mobile-record-list">
          <div class="audit-log-mobile-list__grid standard-mobile-record-grid">
            <article
              v-for="row in tableData"
              :key="row.id || row.traceId || row.operationTime || row.operationModule"
              class="audit-log-mobile-card standard-mobile-record-card"
            >
              <div class="audit-log-mobile-card__header">
                <div class="audit-log-mobile-card__heading">
                  <strong class="audit-log-mobile-card__title">
                    {{ isSystemMode ? formatValue(row.traceId || row.operationModule) : formatValue(row.operationModule || row.userName) }}
                  </strong>
                  <span class="audit-log-mobile-card__sub">
                    {{ isSystemMode ? formatValue(row.deviceCode) : formatValue(row.userName) }}
                  </span>
                </div>
                <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                  {{ getOperationResultName(row.operationResult) }}
                </span>
              </div>

              <div class="audit-log-mobile-card__meta">
                <span
                  v-if="isBusinessMode"
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ getOperationTypeName(row.operationType || '') }}
                </span>
                <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                  {{ formatValue(row.requestMethod) }}
                </span>
                <span
                  v-if="isSystemMode"
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ formatValue(row.errorCode) }}
                </span>
                <span
                  v-else
                  class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item"
                >
                  {{ formatValue(row.ipAddress) }}
                </span>
              </div>

              <div class="audit-log-mobile-card__info">
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常模块' : '操作模块' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ formatValue(row.operationModule) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '设备编码' : '操作方法' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.deviceCode) : formatValue(row.operationMethod) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '产品标识' : '操作时间' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.productKey) : formatValue(row.operationTime) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常类型' : '操作结果' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.exceptionClass) : getOperationResultName(row.operationResult) }}
                  </strong>
                </div>
                <div class="audit-log-mobile-card__field audit-log-mobile-card__field--full">
                  <span class="standard-mobile-record-card__field-label">
                    {{ isSystemMode ? '异常摘要' : '请求目标' }}
                  </span>
                  <strong class="standard-mobile-record-card__field-value">
                    {{ isSystemMode ? formatValue(row.resultMessage) : formatValue(row.requestUrl) }}
                  </strong>
                </div>
              </div>

              <StandardWorkbenchRowActions
                variant="card"
                :direct-items="getAuditDirectActions(row)"
                @command="(command) => handleAuditRowAction(command, row)"
              />
            </article>
          </div>
        </div>

        <el-table
          ref="tableRef"
          class="audit-log-table"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column v-if="isBusinessMode" prop="operationType" label="操作类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getOperationTypeTag(row.operationType)">
                {{ getOperationTypeName(row.operationType) }}
              </el-tag>
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="operationModule" label="操作模块" :width="150" />
          <StandardTableTextColumn prop="operationMethod" label="操作方法" :min-width="180" />
          <StandardTableTextColumn prop="requestUrl" label="请求URL/目标" :min-width="220" />
          <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
          <StandardTableTextColumn v-if="isSystemMode" prop="traceId" label="TraceId" :min-width="180" />
          <StandardTableTextColumn v-if="isSystemMode" prop="deviceCode" label="设备编码" :min-width="140" />
          <StandardTableTextColumn v-if="isSystemMode" prop="productKey" label="产品标识" :min-width="140" />
          <StandardTableTextColumn v-if="isSystemMode" prop="errorCode" label="异常编码" :min-width="120" />
          <StandardTableTextColumn v-if="isSystemMode" prop="exceptionClass" label="异常类型" :min-width="180" />
          <StandardTableTextColumn v-if="isBusinessMode" prop="userName" label="操作用户" :width="120" />
          <StandardTableTextColumn v-if="isBusinessMode" prop="ipAddress" label="操作IP" :width="150" />
          <StandardTableTextColumn v-if="isSystemMode" prop="resultMessage" label="异常摘要" :min-width="220" />
          <StandardTableTextColumn prop="operationTime" label="操作时间" :width="180" />
          <el-table-column prop="operationResult" label="操作结果" width="100">
            <template #default="{ row }">
              <el-tag :type="getOperationResultTag(row.operationResult)" round>
                {{ getOperationResultName(row.operationResult) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            :width="auditActionColumnWidth"
            fixed="right"
            class-name="standard-row-actions-column"
            :show-overflow-tooltip="false"
          >
            <template #default="{ row }">
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="getAuditDirectActions(row)"
                @command="(command) => handleAuditRowAction(command, row)"
              />
            </template>
          </el-table-column>
        </el-table>
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

    <AuditLogDetailDrawer
      v-model="detailVisible"
      :title="detailDialogTitle"
      :detail="detailData"
      :loading="detailLoading"
      :error-message="detailErrorMessage"
      :show-trace-action="isSystemMode && canJumpToMessageTrace(detailData)"
      :show-access-error-action="isSystemMode && canJumpToMessageTrace(detailData)"
      @jump-message-trace="handleJumpToMessageTrace(detailData)"
      @jump-access-error="handleJumpToAccessError(detailData)"
    />

    <StandardDetailDrawer
      v-model="evidenceDrawerVisible"
      title="TraceId 证据包"
      :subtitle="evidenceDrawerSubtitle"
      :loading="evidenceLoading"
      loading-text="正在加载证据链"
      :error-message="evidenceErrorMessage"
      :empty="evidenceTimeline.length === 0 && !evidenceLoading"
      empty-text="当前 TraceId 暂无业务事件或调用片段证据"
      size="56rem"
      tag-layout="title-inline"
      :tags="evidenceDrawerTags"
    >
      <div class="observability-evidence-drawer">
        <section class="observability-evidence-summary" aria-label="证据链摘要">
          <div
            v-for="item in evidenceSummaryCards"
            :key="item.label"
            class="observability-evidence-summary__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="observability-evidence-section">
          <header class="observability-evidence-section__header">
            <h3>合并时间线</h3>
          </header>
          <ol class="observability-evidence-timeline">
            <li
              v-for="item in evidenceTimeline"
              :key="`${item.itemType || 'item'}-${item.itemId || item.code || item.occurredAt}`"
              class="observability-evidence-timeline__item"
            >
              <span class="observability-evidence-timeline__type">
                {{ getEvidenceItemTypeName(item.itemType) }}
              </span>
              <div class="observability-evidence-timeline__body">
                <div class="observability-evidence-timeline__title">
                  <strong>{{ formatValue(item.code) }}</strong>
                  <span>{{ formatValue(item.status) }}</span>
                </div>
                <p>{{ formatValue(item.name) }}</p>
                <div class="observability-evidence-timeline__meta">
                  <span>{{ formatValue(item.occurredAt) }}</span>
                  <span v-if="item.durationMs !== undefined && item.durationMs !== null">
                    {{ formatDuration(item.durationMs) }}
                  </span>
                  <span v-if="item.objectType || item.objectId">
                    {{ formatValue(item.objectType) }} / {{ formatValue(item.objectId) }}
                  </span>
                </div>
              </div>
            </li>
          </ol>
        </section>

        <section class="observability-evidence-split">
          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>业务事件</h3>
            </header>
            <div v-if="evidenceBusinessEvents.length === 0" class="observability-evidence-empty">暂无业务事件</div>
            <div
              v-for="event in evidenceBusinessEvents"
              :key="`event-${event.id || event.eventCode}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(event.eventCode) }}</strong>
              <span>{{ formatValue(event.eventName) }}</span>
              <small>{{ formatValue(event.domainCode) }} · {{ formatValue(event.resultStatus) }}</small>
            </div>
          </article>

          <article class="observability-evidence-section">
            <header class="observability-evidence-section__header">
              <h3>调用片段</h3>
            </header>
            <div v-if="evidenceSpans.length === 0" class="observability-evidence-empty">暂无调用片段</div>
            <div
              v-for="span in evidenceSpans"
              :key="`span-${span.id || span.spanType}`"
              class="observability-evidence-record"
            >
              <strong>{{ formatValue(span.spanType) }}</strong>
              <span>{{ formatValue(span.spanName) }}</span>
              <small>{{ formatDuration(span.durationMs) }} · {{ formatValue(span.status) }}</small>
            </div>
          </article>
        </section>
      </div>
    </StandardDetailDrawer>

    <CsvColumnSettingDialog
      v-model="exportColumnDialogVisible"
      :title="exportDialogTitle"
      :options="exportColumnOptions"
      :selected-keys="selectedExportColumnKeys"
      :preset-storage-key="exportColumnStorageKey"
      :presets="exportPresets"
      @confirm="handleExportColumnConfirm"
    />
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageLogs, getAuditLogById, deleteAuditLog, getSystemErrorStats, getBusinessAuditStats, type AuditLogRecord } from '@/api/auditLog'
import {
  getTraceEvidence,
  listObservabilitySlowSpanSummaries,
  pageObservabilitySpans,
  type ObservabilitySpan,
  type ObservabilitySpanPageQuery,
  type ObservabilitySlowSpanSummary,
  type ObservabilitySlowSpanSummaryQuery,
  type ObservabilityTraceEvidence
} from '@/api/observability'
import { isHandledRequestError } from '@/api/request'
import type { BusinessAuditStats, SystemErrorStats } from '@/types/api'
import AuditLogDetailDrawer from '@/components/AuditLogDetailDrawer.vue'
import CsvColumnSettingDialog from '@/components/CsvColumnSettingDialog.vue'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardTableToolbar from '@/components/StandardTableToolbar.vue'
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'
import StandardPageShell from '@/components/StandardPageShell.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import { useListAppliedFilters } from '@/composables/useListAppliedFilters'
import { useServerPagination } from '@/composables/useServerPagination'
import { downloadRowsAsCsv, type CsvColumn } from '@/utils/csv'
import {
  loadCsvColumnSelection,
  resolveCsvColumns,
  saveCsvColumnSelection,
  toCsvColumnOptions
} from '@/utils/csvColumns'
import { confirmAction, isConfirmCancelled } from '@/utils/confirm'
import {
  buildDiagnosticRouteQuery,
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticContext
} from '@/utils/iotAccessDiagnostics'
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn'

type AuditLogViewMode = 'business' | 'system'

const route = useRoute()
const router = useRouter()
const viewMode = computed<AuditLogViewMode>(() => (route.path === '/system-log' ? 'system' : 'business'))
const isSystemMode = computed(() => viewMode.value === 'system')
const isBusinessMode = computed(() => viewMode.value === 'business')
const auditActionColumnWidth = computed(() =>
  resolveWorkbenchActionColumnWidth({
    directItems: isSystemMode.value
      ? [
          { command: 'detail', label: '详情' },
          { command: 'evidence', label: '证据' },
          { command: 'trace', label: '追踪' },
          { command: 'delete', label: '删除', permission: 'system:audit:delete' }
        ]
      : [
          { command: 'detail', label: '详情' },
          { command: 'delete', label: '删除', permission: 'system:audit:delete' }
        ]
  })
)
const pageTitle = computed(() => (isSystemMode.value ? '异常观测台' : '审计中心'))
const panelTitle = computed(() => pageTitle.value)
const pageDescription = computed(() =>
  isSystemMode.value
    ? '后台异常核对：按异常模块、TraceId、设备编码与请求通道筛查 system_error，并判断下一步回链路追踪还是治理修正。'
    : '按用户、模块与结果查看审计留痕。'
)
const detailDialogTitle = computed(() => (isSystemMode.value ? '异常详情' : `${pageTitle.value}详情`))
const exportDialogTitle = computed(() => (isSystemMode.value ? '异常观测台导出列设置' : `${pageTitle.value}导出列设置`))
const recordLabel = computed(() => (isSystemMode.value ? '异常记录' : '审计记录'))
const businessOperationTypeOptions = [
  { label: '新增', value: 'insert' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '查询', value: 'select' }
]
const systemRequestMethodOptions = [
  { label: 'MQTT', value: 'MQTT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

// 搜索表单
const searchForm = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const appliedFilters = reactive({
  userName: '',
  operationType: undefined as string | undefined,
  traceId: '',
  deviceCode: '',
  productKey: '',
  operationModule: '',
  requestMethod: '',
  requestUrl: '',
  errorCode: '',
  exceptionClass: '',
  operationResult: undefined as number | undefined
})
const quickSearchKeyword = ref('')
const showAdvancedFilters = ref(false)

// 分页

// 表格数据
const tableData = ref<AuditLogRecord[]>([])
const tableRef = ref()
const selectedRows = ref<AuditLogRecord[]>([])
const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination()
const exportColumns: CsvColumn<any>[] = [
  { key: 'operationType', label: '操作类型', formatter: (value) => getOperationTypeName(String(value || '')) },
  { key: 'operationModule', label: '操作模块' },
  { key: 'operationMethod', label: '操作方法' },
  { key: 'requestUrl', label: '请求URL' },
  { key: 'requestMethod', label: '请求方法' },
  { key: 'traceId', label: 'TraceId' },
  { key: 'deviceCode', label: '设备编码' },
  { key: 'productKey', label: '产品标识' },
  { key: 'errorCode', label: '异常编码' },
  { key: 'exceptionClass', label: '异常类型' },
  { key: 'userName', label: '操作用户' },
  { key: 'ipAddress', label: '操作IP' },
  { key: 'resultMessage', label: '结果消息' },
  { key: 'operationTime', label: '操作时间' },
  { key: 'operationResult', label: '操作结果', formatter: (value) => (Number(value) === 1 ? '成功' : '失败') }
]
const exportColumnStorageKey = computed(() => (isSystemMode.value ? 'system-log-view' : 'business-log-view'))
const exportColumnOptions = toCsvColumnOptions(exportColumns)
const exportPresets = computed(() =>
  isSystemMode.value
    ? [
        { label: '默认模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'resultMessage', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationModule', 'requestUrl', 'requestMethod', 'deviceCode', 'productKey', 'resultMessage', 'operationTime'] },
        { label: '研发模板', keys: ['operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'traceId', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass', 'resultMessage', 'operationResult', 'operationTime'] }
      ]
    : [
        { label: '默认模板', keys: ['operationType', 'operationModule', 'operationMethod', 'requestUrl', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '运维模板', keys: ['operationType', 'operationModule', 'requestMethod', 'userName', 'ipAddress', 'operationTime', 'operationResult'] },
        { label: '管理模板', keys: ['operationType', 'operationModule', 'operationMethod', 'userName', 'operationTime', 'operationResult'] }
      ]
)
const selectedExportColumnKeys = ref<string[]>([])
const exportColumnDialogVisible = ref(false)

// 加载状态
const loading = ref(false)
const statsLoading = ref(false)

const createEmptySystemStats = (): SystemErrorStats => ({
  total: 0,
  todayCount: 0,
  mqttCount: 0,
  systemCount: 0,
  distinctTraceCount: 0,
  distinctDeviceCount: 0,
  topModules: [],
  topExceptionClasses: [],
  topErrorCodes: []
})

const createEmptyBusinessStats = (): BusinessAuditStats => ({
  total: 0,
  todayCount: 0,
  successCount: 0,
  failureCount: 0,
  distinctUserCount: 0,
  topModules: [],
  topUsers: [],
  topOperationTypes: []
})

const systemStats = ref<SystemErrorStats>(createEmptySystemStats())
const businessStats = ref<BusinessAuditStats>(createEmptyBusinessStats())
const slowSummaryRows = ref<ObservabilitySlowSpanSummary[]>([])
const slowSummaryLoading = ref(false)
const slowSummaryErrorMessage = ref('')
const activeSlowSummary = ref<ObservabilitySlowSpanSummary | null>(null)
const slowSpanRows = ref<ObservabilitySpan[]>([])
const slowSpanLoading = ref(false)
const slowSpanErrorMessage = ref('')
const slowSpanTotal = ref(0)
const quickSearchPlaceholder = computed(() => (isSystemMode.value ? '快速搜索（TraceId）' : '快速搜索（操作用户）'))
const advancedFilterKeys = computed<
  Array<'traceId' | 'deviceCode' | 'productKey' | 'requestUrl' | 'errorCode' | 'exceptionClass'>
>(() =>
  isSystemMode.value
    ? ['requestUrl', 'deviceCode', 'productKey', 'errorCode', 'exceptionClass']
    : ['traceId']
)
const {
  tags: activeFilterTags,
  hasAppliedFilters,
  advancedAppliedCount,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'userName', label: '操作用户', isActive: (value) => isBusinessMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationType', label: (value) => `操作类型：${getOperationTypeName(String(value || ''))}`, clearValue: undefined, isActive: (value) => isBusinessMode.value && value !== undefined },
    { key: 'traceId', label: 'TraceId', advanced: true },
    { key: 'operationModule', label: (value) => `${isSystemMode.value ? '异常模块' : '操作模块'}：${String(value || '').trim()}` },
    { key: 'requestMethod', label: (value) => `请求通道：${String(value || '')}`, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'requestUrl', label: '目标 / URL', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'deviceCode', label: '设备编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'productKey', label: '产品标识', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'errorCode', label: '异常编码', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'exceptionClass', label: '异常类型', advanced: true, isActive: (value) => isSystemMode.value && hasFilledFilter(value as string | number | undefined) },
    { key: 'operationResult', label: (value) => `操作结果：${getOperationResultName(Number(value))}`, clearValue: undefined, isActive: (value) => value !== undefined }
  ],
  defaults: {
    userName: '',
    operationType: undefined,
    traceId: '',
    deviceCode: '',
    productKey: '',
    operationModule: '',
    requestMethod: '',
    requestUrl: '',
    errorCode: '',
    exceptionClass: '',
    operationResult: undefined
  }
})
const appliedQuickSearchValue = computed(() => (isSystemMode.value ? appliedFilters.traceId.trim() : appliedFilters.userName.trim()))
const auditToolbarActions = computed(() => [
  {
    key: 'export-config',
    command: 'export-config',
    label: '导出列设置'
  },
  {
    key: 'export-selected',
    command: 'export-selected',
    label: '导出选中',
    disabled: selectedRows.value.length === 0
  },
  {
    key: 'export-current',
    command: 'export-current',
    label: '导出当前结果',
    disabled: tableData.value.length === 0
  },
  {
    key: 'clear-selection',
    command: 'clear-selection',
    label: '清空选中',
    disabled: selectedRows.value.length === 0
  }
])
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return ''
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`
})
const restoredDiagnosticContext = computed(() => {
  if (!isSystemMode.value) {
    return null
  }
  const requestMethod = typeof route.query.requestMethod === 'string' ? route.query.requestMethod : ''
  const requestUrl = typeof route.query.requestUrl === 'string' ? route.query.requestUrl : ''
  return resolveDiagnosticContext({
    ...route.query,
    topic: requestMethod === 'MQTT' ? requestUrl || route.query.topic : route.query.topic
  } as Record<string, unknown>)
})
const systemInlineMessage = computed(() =>
  restoredDiagnosticContext.value
    ? [
        `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`,
        '当前节点：后台异常核对',
        '下一步回链路追踪台或治理页继续排查。'
      ].join(' · ')
    : ''
)
const showSystemInlineState = computed(() => isSystemMode.value && Boolean(systemInlineMessage.value))

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<Partial<AuditLogRecord>>({})
const detailLoading = ref(false)
const detailErrorMessage = ref('')
const evidenceDrawerVisible = ref(false)
const evidenceLoading = ref(false)
const evidenceErrorMessage = ref('')
const evidenceTrace = ref<ObservabilityTraceEvidence | null>(null)
const evidenceTraceId = ref('')

const defaultExportKeys = exportColumns.map((column) => String(column.key))
const evidenceBusinessEvents = computed(() => evidenceTrace.value?.businessEvents ?? [])
const evidenceSpans = computed(() => evidenceTrace.value?.spans ?? [])
const evidenceTimeline = computed(() => evidenceTrace.value?.timeline ?? [])
const evidenceDrawerSubtitle = computed(() =>
  evidenceTraceId.value ? `TraceId：${evidenceTraceId.value}` : '按 TraceId 汇总业务事件与调用片段'
)
const evidenceDrawerTags = computed(() => [
  { label: `事件 ${evidenceBusinessEvents.value.length}`, type: 'primary' as const },
  { label: `片段 ${evidenceSpans.value.length}`, type: 'info' as const }
])
const evidenceSummaryCards = computed(() => [
  { label: 'TraceId', value: formatValue(evidenceTrace.value?.traceId || evidenceTraceId.value) },
  { label: '业务事件', value: String(evidenceBusinessEvents.value.length) },
  { label: '调用片段', value: String(evidenceSpans.value.length) },
  { label: '时间线节点', value: String(evidenceTimeline.value.length) }
])

const reloadExportSelection = () => {
  selectedExportColumnKeys.value = loadCsvColumnSelection(exportColumnStorageKey.value, defaultExportKeys)
}

const hasFilledFilter = (value: string | number | undefined) => {
  if (typeof value === 'string') {
    return value.trim() !== ''
  }
  return value !== undefined
}

const syncQuickSearchKeywordFromFilters = () => {
  quickSearchKeyword.value = isSystemMode.value ? searchForm.traceId : searchForm.userName
}

const applyQuickSearchKeywordToFilters = () => {
  const keyword = quickSearchKeyword.value.trim()
  if (isSystemMode.value) {
    searchForm.traceId = keyword
    return
  }
  searchForm.userName = keyword
}

const syncAdvancedFilterState = () => {
  showAdvancedFilters.value = advancedFilterKeys.value.some((key) => hasFilledFilter(searchForm[key]))
}

const resetSearchForm = () => {
  quickSearchKeyword.value = ''
  searchForm.userName = ''
  searchForm.operationType = undefined
  searchForm.traceId = ''
  searchForm.deviceCode = ''
  searchForm.productKey = ''
  searchForm.operationModule = ''
  searchForm.requestMethod = ''
  searchForm.requestUrl = ''
  searchForm.errorCode = ''
  searchForm.exceptionClass = ''
  searchForm.operationResult = undefined
  showAdvancedFilters.value = false
}

const readRouteQueryValue = (key: string) => {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

const parseOptionalNumber = (value: string) => {
  if (!value) {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const applySystemRouteQuery = () => {
  if (!isSystemMode.value) {
    return
  }
  const context = restoredDiagnosticContext.value
  searchForm.traceId = readRouteQueryValue('traceId') || context?.traceId || ''
  searchForm.deviceCode = readRouteQueryValue('deviceCode') || context?.deviceCode || ''
  searchForm.productKey = readRouteQueryValue('productKey') || context?.productKey || ''
  searchForm.operationModule = readRouteQueryValue('operationModule')
  searchForm.requestMethod = readRouteQueryValue('requestMethod') || (context?.topic ? 'MQTT' : '')
  searchForm.requestUrl = readRouteQueryValue('requestUrl') || context?.topic || ''
  searchForm.errorCode = readRouteQueryValue('errorCode')
  searchForm.exceptionClass = readRouteQueryValue('exceptionClass')
  searchForm.operationResult = parseOptionalNumber(readRouteQueryValue('operationResult'))
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
}

// 获取审计日志查询条件
const buildAuditLogQueryParams = () => ({
  traceId: appliedFilters.traceId,
  operationModule: appliedFilters.operationModule,
  operationResult: appliedFilters.operationResult,
  ...(isBusinessMode.value
    ? {
        userName: appliedFilters.userName,
        operationType: appliedFilters.operationType,
        excludeSystemError: true
      }
    : {
        operationType: 'system_error',
        deviceCode: appliedFilters.deviceCode,
        productKey: appliedFilters.productKey,
        requestMethod: appliedFilters.requestMethod,
        requestUrl: appliedFilters.requestUrl,
        errorCode: appliedFilters.errorCode,
        exceptionClass: appliedFilters.exceptionClass
      })
})

const logPageError = (context: string, error: unknown) => {
  if (!isHandledRequestError(error)) {
    console.error(context, error)
  }
}

// 获取审计日志列表
const getAuditLogList = async () => {
  loading.value = true
  try {
    const res = await pageLogs({
      ...buildAuditLogQueryParams(),
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    if (res.code === 200) {
      tableData.value = applyPageResult(res.data)
    }
  } catch (error) {
    logPageError('获取审计日志列表失败', error)
  } finally {
    loading.value = false
  }
}

// 获取日志统计
const getAuditLogStats = async () => {
  statsLoading.value = true
  try {
    if (isSystemMode.value) {
      systemStats.value = createEmptySystemStats()
      const res = await getSystemErrorStats(buildAuditLogQueryParams())
      if (res.code === 200 && res.data) {
        systemStats.value = { ...createEmptySystemStats(), ...res.data }
      }
      return
    }

    businessStats.value = createEmptyBusinessStats()
    const res = await getBusinessAuditStats(buildAuditLogQueryParams())
    if (res.code === 200 && res.data) {
      businessStats.value = { ...createEmptyBusinessStats(), ...res.data }
    }
  } catch (error) {
    logPageError('获取日志统计失败', error)
  } finally {
    statsLoading.value = false
  }
}

const buildSlowSummaryQueryParams = (): ObservabilitySlowSpanSummaryQuery => ({
  limit: 5,
  minDurationMs: 1
})

const clearSlowSpanDrilldown = () => {
  activeSlowSummary.value = null
  slowSpanRows.value = []
  slowSpanLoading.value = false
  slowSpanErrorMessage.value = ''
  slowSpanTotal.value = 0
}

const getSlowSpanSummaries = async () => {
  if (!isSystemMode.value) {
    slowSummaryRows.value = []
    slowSummaryErrorMessage.value = ''
    slowSummaryLoading.value = false
    clearSlowSpanDrilldown()
    return
  }

  slowSummaryLoading.value = true
  slowSummaryErrorMessage.value = ''
  clearSlowSpanDrilldown()
  try {
    const res = await listObservabilitySlowSpanSummaries(buildSlowSummaryQueryParams())
    if (res.code === 200) {
      slowSummaryRows.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    slowSummaryRows.value = []
    slowSummaryErrorMessage.value = error instanceof Error ? error.message : '获取性能慢点汇总失败'
    logPageError('获取性能慢点汇总失败', error)
  } finally {
    slowSummaryLoading.value = false
  }
}

const setSlowSpanQueryValue = (
  params: ObservabilitySpanPageQuery,
  key: 'spanType' | 'eventCode' | 'domainCode' | 'objectType' | 'objectId',
  value?: string | null
) => {
  const normalized = (value || '').trim()
  if (normalized) {
    params[key] = normalized
  }
}

const buildSlowSpanQueryParams = (row: ObservabilitySlowSpanSummary): ObservabilitySpanPageQuery => {
  const params: ObservabilitySpanPageQuery = {
    minDurationMs: 1,
    pageNum: 1,
    pageSize: 5
  }
  setSlowSpanQueryValue(params, 'spanType', row.spanType)
  setSlowSpanQueryValue(params, 'domainCode', row.domainCode)
  setSlowSpanQueryValue(params, 'eventCode', row.eventCode)
  setSlowSpanQueryValue(params, 'objectType', row.objectType)
  setSlowSpanQueryValue(params, 'objectId', row.objectId)
  return params
}

const loadSlowSpanDrilldown = async (row: ObservabilitySlowSpanSummary) => {
  if (!isSystemMode.value) {
    return
  }
  activeSlowSummary.value = row
  slowSpanRows.value = []
  slowSpanTotal.value = 0
  slowSpanErrorMessage.value = ''
  slowSpanLoading.value = true
  try {
    const res = await pageObservabilitySpans(buildSlowSpanQueryParams(row))
    if (res.code === 200 && res.data) {
      slowSpanRows.value = Array.isArray(res.data.records) ? res.data.records : []
      slowSpanTotal.value = Number(res.data.total || slowSpanRows.value.length)
    }
  } catch (error) {
    slowSpanRows.value = []
    slowSpanErrorMessage.value = error instanceof Error ? error.message : '获取慢点明细失败'
    logPageError('获取慢点明细失败', error)
  } finally {
    slowSpanLoading.value = false
  }
}

// 初始化
onMounted(() => {
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
  getSlowSpanSummaries()
})

watch(viewMode, (newMode, oldMode) => {
  if (newMode === oldMode) {
    return
  }
  resetSearchForm()
  clearSelection()
  resetPage()
  resetTotal()
  detailVisible.value = false
  detailData.value = {}
  detailLoading.value = false
  detailErrorMessage.value = ''
  evidenceDrawerVisible.value = false
  evidenceTrace.value = null
  evidenceTraceId.value = ''
  evidenceLoading.value = false
  evidenceErrorMessage.value = ''
  slowSummaryRows.value = []
  slowSummaryLoading.value = false
  slowSummaryErrorMessage.value = ''
  clearSlowSpanDrilldown()
  exportColumnDialogVisible.value = false
  reloadExportSelection()
  applySystemRouteQuery()
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  getAuditLogList()
  getAuditLogStats()
  getSlowSpanSummaries()
})

watch(
  () => [
    route.query.traceId,
    route.query.deviceCode,
    route.query.productKey,
    route.query.operationModule,
    route.query.requestMethod,
    route.query.requestUrl,
    route.query.errorCode,
    route.query.exceptionClass,
    route.query.operationResult
  ],
  (current, previous) => {
    if (!isSystemMode.value) {
      return
    }
    if (JSON.stringify(current) === JSON.stringify(previous)) {
      return
    }
    applySystemRouteQuery()
    resetPage()
    clearSelection()
    syncAppliedFilters()
    getAuditLogList()
    getAuditLogStats()
    getSlowSpanSummaries()
  }
)

const triggerSearch = (resetPageFirst = false) => {
  applyQuickSearchKeywordToFilters()
  syncAdvancedFilterState()
  syncAppliedFilters()
  if (resetPageFirst) {
    resetPage()
  }
  clearSelection()
  getAuditLogList()
  getAuditLogStats()
  getSlowSpanSummaries()
}

// 处理搜索
const handleSearch = () => {
  triggerSearch(true)
}

// 重置搜索
const handleReset = () => {
  resetSearchForm()
  triggerSearch(true)
}

const handleQuickSearch = () => {
  triggerSearch(true)
}

const handleClearQuickSearch = () => {
  quickSearchKeyword.value = ''
  triggerSearch(true)
}

const toggleAdvancedFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value
}

const handleClearAppliedFilters = () => {
  handleReset()
}

const handleRemoveAppliedFilter = (key: string) => {
  removeAppliedFilter(key)
  syncQuickSearchKeywordFromFilters()
  syncAdvancedFilterState()
  triggerSearch(true)
}

const handleSelectionChange = (rows: AuditLogRecord[]) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleRefresh = () => {
  triggerSearch(false)
}

const formatValue = (value: unknown) => {
  if (value === undefined || value === null) {
    return '--'
  }
  const text = String(value).trim()
  return text ? text : '--'
}

const buildSystemDiagnosticContext = (source?: Partial<AuditLogRecord>): DiagnosticContext => {
  const traceId = source?.traceId || quickSearchKeyword.value.trim() || searchForm.traceId || undefined
  const deviceCode = source?.deviceCode || searchForm.deviceCode || undefined
  const productKey = source?.productKey || searchForm.productKey || undefined
  const requestMethod = source?.requestMethod || searchForm.requestMethod
  const requestUrl = source?.requestUrl || searchForm.requestUrl
  return {
    sourcePage: 'system-log',
    traceId,
    deviceCode,
    productKey,
    topic: requestMethod === 'MQTT' ? requestUrl || undefined : undefined,
    reportStatus: systemStats.value.total > 0 ? 'failed' : 'timeline-missing',
    capturedAt: new Date().toISOString()
  }
}

const persistSystemContext = (source?: Partial<AuditLogRecord>) => {
  persistDiagnosticContext(buildSystemDiagnosticContext(source))
}

const canJumpToMessageTrace = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  return Boolean(context.traceId || context.deviceCode || context.productKey || context.topic)
}

const resolveEvidenceTraceId = (row?: Partial<AuditLogRecord>) =>
  (row?.traceId || searchForm.traceId || appliedFilters.traceId).trim()

const canOpenTraceEvidence = (row?: Partial<AuditLogRecord>) => Boolean(resolveEvidenceTraceId(row))

const openTraceEvidence = async (row?: Partial<AuditLogRecord>) => {
  const traceId = resolveEvidenceTraceId(row)
  if (!traceId) {
    ElMessage.warning('当前记录缺少 TraceId，无法查看证据包')
    return
  }
  evidenceDrawerVisible.value = true
  evidenceTraceId.value = traceId
  evidenceTrace.value = null
  evidenceErrorMessage.value = ''
  evidenceLoading.value = true
  try {
    const res = await getTraceEvidence(traceId)
    if (res.code === 200) {
      evidenceTrace.value = res.data || { traceId, businessEvents: [], spans: [], timeline: [] }
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error('获取 TraceId 证据包失败')
    }
    evidenceErrorMessage.value = error instanceof Error ? error.message : '获取 TraceId 证据包失败'
    logPageError('获取 TraceId 证据包失败', error)
  } finally {
    evidenceLoading.value = false
  }
}

const openTraceEvidenceByTraceId = async (traceId?: string | null) => {
  const normalizedTraceId = (traceId || '').trim()
  if (!normalizedTraceId) {
    ElMessage.warning('当前慢点缺少 TraceId，无法查看证据包')
    return
  }
  await openTraceEvidence({ traceId: normalizedTraceId })
}

const handleJumpToMessageTrace = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  persistSystemContext(row)
  router.push({
    path: '/message-trace',
    query: buildDiagnosticRouteQuery(context)
  })
}

const handleJumpToAccessError = (row?: AuditLogRecord) => {
  const context = buildSystemDiagnosticContext(row)
  persistSystemContext(row)
  router.push({
    path: '/message-trace',
    query: {
      mode: 'access-error',
      ...buildDiagnosticRouteQuery(context),
      errorCode: row?.errorCode || searchForm.errorCode || undefined,
      exceptionClass: row?.exceptionClass || searchForm.exceptionClass || undefined
    }
  })
}

const openExportColumnSetting = () => {
  exportColumnDialogVisible.value = true
}

const handleExportColumnConfirm = (selectedKeys: string[]) => {
  selectedExportColumnKeys.value = selectedKeys
  saveCsvColumnSelection(exportColumnStorageKey.value, selectedKeys)
}

const getResolvedExportColumns = () => resolveCsvColumns(exportColumns, selectedExportColumnKeys.value)

const handleExportSelected = () => {
  downloadRowsAsCsv(`${pageTitle.value}-选中项.csv`, selectedRows.value, getResolvedExportColumns())
}

const handleExportCurrent = () => {
  downloadRowsAsCsv(`${pageTitle.value}-当前结果.csv`, tableData.value, getResolvedExportColumns())
}

const handleToolbarAction = (command: string | number | object) => {
  switch (command) {
    case 'export-config':
      openExportColumnSetting()
      break
    case 'export-selected':
      handleExportSelected()
      break
    case 'export-current':
      handleExportCurrent()
      break
    case 'clear-selection':
      clearSelection()
      break
    default:
      break
  }
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  setPageSize(size)
  getAuditLogList()
}

// 当前页变化
const handlePageChange = (page: number) => {
  setPageNum(page)
  getAuditLogList()
}

const getAuditDirectActions = (row: AuditLogRecord) => {
  if (isSystemMode.value) {
    return [
      { command: 'detail', label: '详情' },
      { command: 'evidence', label: '证据', disabled: !canOpenTraceEvidence(row) },
      { command: 'trace', label: '追踪', disabled: !canJumpToMessageTrace(row) },
      { command: 'delete', label: '删除', permission: 'system:audit:delete' }
    ]
  }

  return [
    { command: 'detail', label: '详情' },
    { command: 'delete', label: '删除', permission: 'system:audit:delete' }
  ]
}

const handleAuditRowAction = (command: string | number | object, row: AuditLogRecord) => {
  if (command === 'detail') {
    void handleDetail(row)
    return
  }
  if (command === 'trace') {
    if (!canJumpToMessageTrace(row)) {
      return
    }
    handleJumpToMessageTrace(row)
    return
  }
  if (command === 'evidence') {
    void openTraceEvidence(row)
    return
  }
  if (command === 'delete') {
    void handleDelete(row)
  }
}

// 查看详情
const handleDetail = async (row: AuditLogRecord) => {
  if (row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('当前日志缺少主键，无法查看详情')
    return
  }

  detailVisible.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailData.value = { ...row }
  try {
    const res = await getAuditLogById(String(row.id))
    if (!res.data || Array.isArray(res.data)) {
      ElMessage.warning(`${recordLabel.value}不存在或已删除`)
      detailVisible.value = false
      return
    }
    detailData.value = { ...row, ...res.data }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      ElMessage.error(`获取${detailDialogTitle.value}失败`)
    }
    detailErrorMessage.value = error instanceof Error ? error.message : `获取${detailDialogTitle.value}失败`
    logPageError('获取日志详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

// 删除
const handleDelete = async (row: AuditLogRecord) => {
  try {
    await confirmAction({
      title: `删除${recordLabel.value}`,
      message: `确认删除当前${recordLabel.value}吗？删除后不可恢复。`,
      type: 'warning',
      confirmButtonText: '确认删除'
    })
    const res = await deleteAuditLog(String(row.id))
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getAuditLogList()
      getAuditLogStats()
    }
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    logPageError('删除失败', error)
  }
}

// 获取操作类型名称
const getOperationTypeName = (type: string) => {
  const map: Record<string, string> = {
    insert: '新增',
    update: '修改',
    delete: '删除',
    select: '查询',
    system_error: '系统异常'
  }
  return map[type] || type
}

// 获取操作类型标签
const getOperationTypeTag = (type: string) => {
  const map: Record<string, string> = {
    insert: 'primary',
    update: 'warning',
    delete: 'danger',
    select: 'info',
    system_error: 'danger'
  }
  return map[type] || 'info'
}

const getOperationResultName = (result?: number | null) => {
  if (result === 1) return '成功'
  if (result === 0) return '失败'
  return '-'
}

const getOperationResultTag = (result?: number | null) => {
  if (result === 1) return 'success'
  if (result === 0) return 'danger'
  return 'info'
}

const formatDuration = (durationMs?: number | null) => {
  if (durationMs === undefined || durationMs === null) {
    return '--'
  }
  return `${durationMs} ms`
}

const formatCount = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '0'
  }
  return String(value)
}

const formatSlowSummaryTitle = (row: ObservabilitySlowSpanSummary) =>
  [row.spanType, row.domainCode].map(formatValue).filter((value) => value !== '--').join(' / ') || '--'

const formatSlowSummaryTarget = (row: ObservabilitySlowSpanSummary) =>
  [row.eventCode, row.objectType, row.objectId].map(formatValue).filter((value) => value !== '--').join(' / ') || '--'

const getEvidenceItemTypeName = (type?: string | null) => {
  if (type === 'BUSINESS_EVENT') {
    return '业务事件'
  }
  if (type === 'SPAN') {
    return '调用片段'
  }
  return formatValue(type)
}

watch(detailVisible, (visible) => {
  if (!visible) {
    detailData.value = {}
    detailLoading.value = false
    detailErrorMessage.value = ''
  }
})

watch(evidenceDrawerVisible, (visible) => {
  if (!visible) {
    evidenceTrace.value = null
    evidenceTraceId.value = ''
    evidenceLoading.value = false
    evidenceErrorMessage.value = ''
  }
})
</script>

<style scoped>
.audit-log-view {
  min-width: 0;
}

.audit-log-quick-search-tag {
  margin-top: 0.72rem;
}

.audit-log-quick-search-tag__chip {
  margin: 0;
}

.audit-log-table-wrap {
  min-width: 0;
}

.audit-log-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.audit-log-mobile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.72rem;
}

.audit-log-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.audit-log-mobile-card__title,
.audit-log-mobile-card__sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-mobile-card__title {
  color: var(--text-heading);
  font-size: 0.96rem;
}

.audit-log-mobile-card__sub {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.audit-log-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-mobile-card__field {
  min-width: 0;
}

.audit-log-mobile-card__field--full {
  grid-column: 1 / -1;
}

.audit-log-mobile-card__field .standard-mobile-record-card__field-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-table {
  display: block;
}

.audit-log-slow-summary {
  display: grid;
  gap: 0.82rem;
  margin-bottom: 0.82rem;
  padding: 1rem;
}

.audit-log-slow-summary__header,
.audit-log-slow-summary__title,
.audit-log-slow-summary__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-summary__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-slow-summary__header > span,
.audit-log-slow-summary__title span,
.audit-log-slow-summary__metrics,
.audit-log-slow-summary__footer > span,
.audit-log-slow-summary__empty {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-summary__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.72rem;
}

.audit-log-slow-summary__item {
  display: grid;
  gap: 0.55rem;
  min-width: 0;
  padding: 0.82rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 90%, transparent);
}

.audit-log-slow-summary__title strong,
.audit-log-slow-summary__item p,
.audit-log-slow-summary__footer > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-summary__title strong {
  color: var(--text-heading);
  font-size: 0.92rem;
}

.audit-log-slow-summary__item p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.audit-log-slow-summary__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-slow-span-drilldown {
  display: grid;
  gap: 0.72rem;
  min-width: 0;
  padding-top: 0.82rem;
  border-top: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
}

.audit-log-slow-span-drilldown__header,
.audit-log-slow-span-drilldown__title,
.audit-log-slow-span-drilldown__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.audit-log-slow-span-drilldown__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.94rem;
  line-height: 1.3;
}

.audit-log-slow-span-drilldown__header p {
  margin: 0.25rem 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__header > span,
.audit-log-slow-span-drilldown__meta,
.audit-log-slow-span-drilldown__footer > span {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.audit-log-slow-span-drilldown__list {
  display: grid;
  gap: 0.62rem;
}

.audit-log-slow-span-drilldown__item {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
  padding: 0.72rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 66%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.audit-log-slow-span-drilldown__title strong,
.audit-log-slow-span-drilldown__footer > span {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-slow-span-drilldown__title span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.audit-log-slow-span-drilldown__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.observability-evidence-drawer {
  display: grid;
  gap: 1rem;
}

.observability-evidence-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.72rem;
}

.observability-evidence-summary__item,
.observability-evidence-section {
  min-width: 0;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 94%, transparent);
}

.observability-evidence-summary__item {
  display: grid;
  gap: 0.28rem;
  padding: 0.82rem 0.9rem;
}

.observability-evidence-summary__item span,
.observability-evidence-record small,
.observability-evidence-timeline__meta,
.observability-evidence-timeline__type {
  color: var(--text-caption);
  font-size: 0.78rem;
}

.observability-evidence-summary__item strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 0.98rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-section {
  padding: 1rem;
}

.observability-evidence-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.82rem;
}

.observability-evidence-section__header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.observability-evidence-timeline {
  display: grid;
  gap: 0.72rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.observability-evidence-timeline__item {
  display: grid;
  grid-template-columns: 4.8rem minmax(0, 1fr);
  gap: 0.75rem;
  min-width: 0;
}

.observability-evidence-timeline__type {
  padding-top: 0.1rem;
}

.observability-evidence-timeline__body,
.observability-evidence-record {
  min-width: 0;
  padding: 0.75rem;
  border: 1px solid color-mix(in srgb, var(--panel-border) 72%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--panel-bg) 86%, transparent);
}

.observability-evidence-timeline__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
}

.observability-evidence-timeline__title strong,
.observability-evidence-record strong {
  overflow: hidden;
  color: var(--text-heading);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-timeline__title span {
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.observability-evidence-timeline__body p {
  margin: 0.34rem 0 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.observability-evidence-timeline__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.45rem;
}

.observability-evidence-split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.observability-evidence-record {
  display: grid;
  gap: 0.25rem;
}

.observability-evidence-record + .observability-evidence-record {
  margin-top: 0.62rem;
}

.observability-evidence-record span {
  overflow: hidden;
  color: var(--text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.observability-evidence-empty {
  color: var(--text-caption);
  font-size: 0.86rem;
}

@media (max-width: 640px) {
  .audit-log-mobile-list {
    display: block;
  }

  .audit-log-table {
    display: none;
  }

  .audit-log-slow-summary__grid {
    grid-template-columns: 1fr;
  }

  .audit-log-mobile-card__info {
    grid-template-columns: 1fr;
  }

  .observability-evidence-summary,
  .observability-evidence-split,
  .observability-evidence-timeline__item {
    grid-template-columns: 1fr;
  }
}

</style>
