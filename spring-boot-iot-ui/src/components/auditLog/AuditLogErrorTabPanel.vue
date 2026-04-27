<template>
  <div data-testid="system-log-error-panel" class="audit-log-system-panel audit-log-system-panel--errors">
    <StandardListFilterHeader
      :model="searchForm"
      :show-advanced="showAdvancedFilters"
      show-advanced-toggle
      :advanced-hint="advancedFilterHint"
      @toggle-advanced="emit('toggle-advanced')"
    >
      <template #primary>
        <el-form-item>
          <el-input
            id="quick-search"
            :model-value="quickSearchKeyword"
            placeholder="快速搜索（TraceId）"
            clearable
            prefix-icon="Search"
            @update:model-value="emit('update:quickSearchKeyword', String($event || ''))"
            @keyup.enter="emit('quick-search')"
            @clear="emit('clear-quick-search')"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            :model-value="searchForm.operationModule"
            placeholder="异常模块"
            clearable
            @update:model-value="emitFieldUpdate('operationModule', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
        <el-form-item>
          <el-select
            :model-value="searchForm.operationResult"
            placeholder="操作结果"
            clearable
            @update:model-value="emitFieldUpdate('operationResult', normalizeOptionalNumber($event))"
          >
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select
            :model-value="searchForm.requestMethod"
            placeholder="请求通道"
            clearable
            @update:model-value="emitFieldUpdate('requestMethod', String($event || ''))"
          >
            <el-option
              v-for="item in requestMethodOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </template>

      <template #advanced>
        <el-form-item>
          <el-input
            :model-value="searchForm.requestUrl"
            placeholder="目标 / URL"
            clearable
            @update:model-value="emitFieldUpdate('requestUrl', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            :model-value="searchForm.deviceCode"
            placeholder="设备编码"
            clearable
            @update:model-value="emitFieldUpdate('deviceCode', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            :model-value="searchForm.productKey"
            placeholder="产品标识"
            clearable
            @update:model-value="emitFieldUpdate('productKey', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            :model-value="searchForm.errorCode"
            placeholder="异常编码"
            clearable
            @update:model-value="emitFieldUpdate('errorCode', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            :model-value="searchForm.exceptionClass"
            placeholder="异常类型"
            clearable
            @update:model-value="emitFieldUpdate('exceptionClass', String($event || ''))"
            @keyup.enter="emit('search')"
          />
        </el-form-item>
      </template>

      <template #actions>
        <StandardButton action="query" @click="emit('search')">查询</StandardButton>
        <StandardButton action="reset" @click="emit('reset')">重置</StandardButton>
        <StandardButton action="refresh" link @click="emit('refresh')">刷新列表</StandardButton>
        <StandardActionMenu
          v-if="toolbarActions.length > 0"
          label="更多操作"
          :items="toolbarActions"
          @command="emit('toolbar-action', $event)"
        />
        <StandardButton
          v-if="errorViewMode === 'detail'"
          action="search"
          @click="emit('open-clusters')"
        >
          按异常分组查看
        </StandardButton>
        <StandardButton v-else action="reset" @click="emit('return-to-details')">返回异常明细</StandardButton>
      </template>
    </StandardListFilterHeader>

    <div v-if="appliedQuickSearchValue" class="audit-log-quick-search-tag">
      <el-tag closable class="audit-log-quick-search-tag__chip" @close="emit('clear-quick-search')">
        快速搜索：{{ appliedQuickSearchValue }}
      </el-tag>
    </div>

    <StandardAppliedFiltersBar
      v-if="hasAppliedFilters"
      :tags="activeFilterTags"
      @remove="emit('remove-applied-filter', String($event))"
      @clear="emit('clear-applied-filters')"
    />

    <StandardInlineState v-if="showInlineState" :message="inlineMessage" tone="info" />

    <section
      v-if="errorViewMode === 'detail' && clusterContextSummary"
      class="audit-log-system-panel__cluster-context standard-list-surface"
    >
      <div class="audit-log-system-panel__cluster-context-bar">
        <div class="audit-log-system-panel__cluster-context-copy">
          <strong>当前按分组定位</strong>
          <span>{{ clusterContextSummary }}</span>
        </div>
        <div class="audit-log-system-panel__cluster-context-actions">
          <StandardButton action="reset" @click="emit('clear-cluster-refiner')">清除分组定位</StandardButton>
          <StandardButton
            v-if="canReturnToClusterResults"
            action="search"
            @click="emit('return-to-clusters')"
          >
            返回异常分组结果
          </StandardButton>
        </div>
      </div>
    </section>

    <section
      v-if="errorViewMode === 'clusters'"
      class="audit-log-system-panel__cluster-stage standard-list-surface"
    >
      <div class="audit-log-system-panel__stage-header">
        <div class="audit-log-system-panel__stage-copy">
          <h3>当前筛选条件下的异常分组</h3>
          <p>选择某个异常分组后，会自动返回异常明细并带入分组条件。</p>
        </div>
        <div class="audit-log-system-panel__stage-meta">
          <span>异常簇 {{ clusterRows.length }}</span>
        </div>
      </div>

      <StandardInlineState
        v-if="Boolean(clusterErrorMessage)"
        :message="clusterErrorMessage"
        tone="warning"
      />
      <div v-if="Boolean(clusterErrorMessage)" class="audit-log-system-panel__cluster-actions">
        <StandardButton action="refresh" @click="emit('retry-clusters')">重试</StandardButton>
      </div>

      <StandardInlineState
        v-else-if="!clusterLoading && clusterRows.length === 0"
        message="当前条件下没有异常分组结果"
        tone="info"
      />

      <div
        v-loading="clusterLoading"
        class="audit-log-system-panel__cluster-table-wrap"
        element-loading-text="正在刷新异常分组"
        element-loading-background="var(--loading-mask-bg)"
      >
        <el-table
          v-if="clusterRows.length > 0"
          data-testid="system-error-cluster-table"
          class="audit-log-system-panel__cluster-table"
          :data="clusterRows"
          row-key="clusterKey"
          border
          stripe
        >
          <el-table-column label="异常分组" min-width="260">
            <template #default="{ row }">
              <button
                type="button"
                class="audit-log-cluster-cell audit-log-cluster-cell--button"
                @click.stop="handleClusterRowClick(row)"
              >
                <strong>{{ formatValue(row.operationModule) }}</strong>
                <span>{{ formatValue(row.exceptionClass) }}</span>
                <div class="audit-log-cluster-cell__tags">
                  <el-tag size="small" effect="plain">{{ formatValue(row.errorCode) }}</el-tag>
                </div>
              </button>
            </template>
          </el-table-column>
          <el-table-column prop="count" label="发生次数" width="110" />
          <el-table-column prop="distinctTraceCount" label="影响 Trace" width="110" />
          <el-table-column prop="distinctDeviceCount" label="影响设备" width="110" />
          <StandardTableTextColumn prop="latestOperationTime" label="最近发生时间" :min-width="170" />
          <el-table-column label="最近入口" min-width="180">
            <template #default="{ row }">
              {{ formatValue(resolveClusterTarget(row)) }}
            </template>
          </el-table-column>
          <StandardTableTextColumn prop="latestResultMessage" label="最近异常摘要" :min-width="220" />
        </el-table>
      </div>
    </section>

    <section v-else class="audit-log-system-panel__detail-stage standard-list-surface">
      <div
        v-loading="loading"
        class="audit-log-table-wrap"
        element-loading-text="正在刷新异常明细"
        element-loading-background="var(--loading-mask-bg)"
      >
        <el-table
          ref="tableRef"
          class="audit-log-table"
          :data="tableData"
          border
          stripe
          style="width: 100%"
          @selection-change="emit('selection-change', $event)"
        >
          <el-table-column type="selection" width="48" />
          <StandardTableTextColumn prop="operationModule" label="操作模块" :width="150" />
          <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
          <StandardTableTextColumn prop="errorCode" label="异常编码" :min-width="120" />
          <StandardTableTextColumn prop="resultMessage" label="异常摘要" :min-width="220" />
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
                :menu-items="getAuditMenuActions(row)"
                :max-direct-items="4"
                @command="emit('audit-row-action', { command: $event, row })"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="pagination.total > 0" class="ops-pagination">
        <StandardPagination
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="emit('size-change', Number($event))"
          @current-change="emit('page-change', Number($event))"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { AuditLogRecord, SystemErrorClusterRow } from '@/api/auditLog'
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'

interface SystemRequestMethodOption {
  label: string
  value: string
}

interface SearchFormModel {
  traceId: string
  deviceCode: string
  productKey: string
  operationModule: string
  requestMethod: string
  requestUrl: string
  errorCode: string
  exceptionClass: string
  operationResult: number | undefined
}

interface PaginationModel {
  pageNum: number
  pageSize: number
  total: number
}

interface WorkbenchActionItem {
  key?: string
  command: string
  label: string
  disabled?: boolean
  permission?: string
}

type SearchFieldKey =
  | 'deviceCode'
  | 'productKey'
  | 'operationModule'
  | 'requestMethod'
  | 'requestUrl'
  | 'errorCode'
  | 'exceptionClass'
  | 'operationResult'

const props = defineProps<{
  searchForm: SearchFormModel
  quickSearchKeyword: string
  showAdvancedFilters: boolean
  advancedFilterHint: string
  requestMethodOptions: SystemRequestMethodOption[]
  toolbarActions: WorkbenchActionItem[]
  appliedQuickSearchValue: string
  activeFilterTags: Array<Record<string, unknown>>
  hasAppliedFilters: boolean
  showInlineState: boolean
  inlineMessage: string
  clusterLoading: boolean
  clusterErrorMessage: string
  clusterRows: SystemErrorClusterRow[]
  errorViewMode: 'detail' | 'clusters'
  clusterContextSummary: string
  canReturnToClusterResults: boolean
  loading: boolean
  tableData: AuditLogRecord[]
  pagination: PaginationModel
  auditActionColumnWidth: number | string
  formatValue: (value: unknown) => string
  getOperationResultName: (value?: number | null) => string
  getOperationResultTag: (value?: number | null) => string
  getAuditDirectActions: (row: AuditLogRecord) => WorkbenchActionItem[]
  getAuditMenuActions: (row: AuditLogRecord) => WorkbenchActionItem[]
}>()

const emit = defineEmits<{
  (event: 'update:quickSearchKeyword', value: string): void
  (event: 'search'): void
  (event: 'reset'): void
  (event: 'refresh'): void
  (event: 'toolbar-action', command: string | number | object): void
  (event: 'quick-search'): void
  (event: 'clear-quick-search'): void
  (event: 'toggle-advanced'): void
  (event: 'clear-applied-filters'): void
  (event: 'remove-applied-filter', key: string): void
  (event: 'update-search-field', payload: { field: SearchFieldKey; value: string | number | undefined }): void
  (event: 'open-clusters'): void
  (event: 'return-to-details'): void
  (event: 'retry-clusters'): void
  (event: 'apply-cluster', clusterKey: string): void
  (event: 'clear-cluster-refiner'): void
  (event: 'return-to-clusters'): void
  (event: 'selection-change', rows: AuditLogRecord[]): void
  (event: 'audit-row-action', payload: { command: string | number | object; row: AuditLogRecord }): void
  (event: 'size-change', size: number): void
  (event: 'page-change', page: number): void
}>()

function normalizeOptionalNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined
}

function emitFieldUpdate(field: SearchFieldKey, value: string | number | undefined) {
  emit('update-search-field', { field, value })
}

function resolveClusterTarget(row: SystemErrorClusterRow) {
  return row.latestRequestUrl || row.latestRequestMethod || ''
}

function handleClusterRowClick(row: SystemErrorClusterRow) {
  if (!row?.clusterKey) {
    return
  }
  emit('apply-cluster', row.clusterKey)
}
</script>

<style scoped>
.audit-log-system-panel__cluster-context,
.audit-log-system-panel__cluster-stage,
.audit-log-system-panel__detail-stage {
  margin-top: 0.88rem;
  padding: 1rem;
}

.audit-log-system-panel__cluster-context {
  padding-block: 0.82rem;
}

.audit-log-system-panel__cluster-context-bar,
.audit-log-system-panel__stage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
}

.audit-log-system-panel__cluster-context-copy,
.audit-log-system-panel__stage-copy {
  display: grid;
  gap: 0.3rem;
  min-width: 0;
}

.audit-log-system-panel__cluster-context-copy strong,
.audit-log-system-panel__stage-copy h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-system-panel__cluster-context-copy span,
.audit-log-system-panel__stage-copy p,
.audit-log-system-panel__stage-meta {
  color: var(--text-secondary);
  font-size: 0.86rem;
  line-height: 1.5;
}

.audit-log-system-panel__cluster-context-actions,
.audit-log-system-panel__cluster-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.audit-log-system-panel__cluster-stage {
  display: grid;
  gap: 0.82rem;
}

.audit-log-system-panel__cluster-table-wrap,
.audit-log-table-wrap {
  min-height: 160px;
}

.audit-log-cluster-cell {
  display: grid;
  gap: 0.36rem;
  min-width: 0;
}

.audit-log-cluster-cell--button {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: inherit;
}

.audit-log-cluster-cell strong {
  color: var(--text-heading);
  font-size: 0.95rem;
}

.audit-log-cluster-cell span {
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
}

.audit-log-cluster-cell__tags {
  display: flex;
  align-items: center;
  gap: 0.42rem;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .audit-log-system-panel__cluster-context-bar,
  .audit-log-system-panel__stage-header {
    flex-direction: column;
  }

  .audit-log-system-panel__cluster-context-actions,
  .audit-log-system-panel__cluster-actions {
    width: 100%;
  }
}
</style>
