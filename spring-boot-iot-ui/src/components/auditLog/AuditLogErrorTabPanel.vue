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

    <section v-if="detailClusterMode !== 'all'" class="audit-log-system-panel__cluster-stage standard-list-surface">
      <div class="audit-log-system-panel__stage-header">
        <div class="audit-log-system-panel__stage-copy">
          <h3>异常分组主表</h3>
          <p>按异常簇聚合展示，展开行直接查看对应的真实异常记录。</p>
        </div>
        <div class="audit-log-system-panel__stage-meta">
          <span>异常簇 {{ clusterRows.length }}</span>
          <span v-if="selectedCluster">已展开 1 个异常簇</span>
          <span v-else>点击异常簇展开明细</span>
        </div>
      </div>

      <StandardInlineState
        v-if="!clusterLoading && clusterRows.length === 0"
        message="当前筛选条件下没有异常簇。"
        tone="info"
      />

      <div
        v-loading="clusterLoading"
        class="audit-log-system-panel__cluster-table-wrap"
        element-loading-text="正在刷新异常分组主表"
        element-loading-background="var(--loading-mask-bg)"
      >
        <el-table
          v-if="clusterRows.length > 0"
          data-testid="system-error-grouped-table"
          class="audit-log-system-panel__cluster-table"
          :data="clusterRows"
          row-key="clusterKey"
          :expand-row-keys="expandedClusterRowKeys"
          border
          stripe
          :row-class-name="resolveClusterRowClassName"
          @expand-change="handleClusterExpandChange"
        >
          <el-table-column type="expand" width="56">
            <template #default="{ row }">
              <div v-if="isClusterExpanded(row)" class="audit-log-system-panel__inline-detail">
                <div class="audit-log-system-panel__detail-header">
                  <div class="audit-log-system-panel__stage-copy">
                    <h3>异常簇明细</h3>
                    <p>
                      {{ formatValue(row.operationModule) }} /
                      {{ formatValue(row.exceptionClass) }} /
                      {{ formatValue(row.errorCode) }}
                    </p>
                  </div>
                  <StandardButton action="reset" @click="emit('collapse-cluster')">收起明细</StandardButton>
                </div>

                <div
                  v-loading="loading"
                  class="audit-log-system-panel__inline-detail-surface"
                  element-loading-text="正在刷新异常明细"
                  element-loading-background="var(--loading-mask-bg)"
                >
                  <el-table
                    v-if="tableData.length > 0"
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
                      <template #default="{ row: detailRow }">
                        <el-tag :type="getOperationResultTag(detailRow.operationResult)" round>
                          {{ getOperationResultName(detailRow.operationResult) }}
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
                      <template #default="{ row: detailRow }">
                        <StandardWorkbenchRowActions
                          variant="table"
                          :direct-items="getAuditDirectActions(detailRow)"
                          @command="emit('audit-row-action', { command: $event, row: detailRow })"
                        />
                      </template>
                    </el-table-column>
                  </el-table>

                  <StandardInlineState
                    v-else-if="!loading"
                    message="该异常簇下暂无匹配明细。"
                    tone="info"
                  />
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
              </div>
            </template>
          </el-table-column>
          <el-table-column label="异常簇" min-width="260">
            <template #default="{ row }">
              <button type="button" class="audit-log-cluster-cell audit-log-cluster-cell--button" @click.stop="handleClusterRowClick(row)">
                <strong>{{ formatValue(row.operationModule) }}</strong>
                <span>{{ formatValue(row.exceptionClass) }}</span>
                <div class="audit-log-cluster-cell__tags">
                  <el-tag size="small" effect="plain">{{ formatValue(row.errorCode) }}</el-tag>
                  <el-tag v-if="isClusterExpanded(row)" size="small" type="success">已展开</el-tag>
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

    <section v-else class="audit-log-system-panel__fallback-stage standard-list-surface">
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
import StandardButton from '@/components/StandardButton.vue'
import StandardInlineState from '@/components/StandardInlineState.vue'
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue'
import StandardPagination from '@/components/StandardPagination.vue'
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue'
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'
import { computed } from 'vue'

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
  appliedQuickSearchValue: string
  activeFilterTags: Array<Record<string, unknown>>
  hasAppliedFilters: boolean
  showInlineState: boolean
  inlineMessage: string
  clusterLoading: boolean
  clusterErrorMessage: string
  clusterRows: SystemErrorClusterRow[]
  selectedClusterKey: string
  selectedCluster: SystemErrorClusterRow | null
  detailClusterMode: 'clustered' | 'all'
  loading: boolean
  tableData: AuditLogRecord[]
  pagination: PaginationModel
  auditActionColumnWidth: number | string
  formatValue: (value: unknown) => string
  getOperationResultName: (value?: number | null) => string
  getOperationResultTag: (value?: number | null) => string
  getAuditDirectActions: (row: AuditLogRecord) => WorkbenchActionItem[]
}>()

const emit = defineEmits<{
  (event: 'update:quickSearchKeyword', value: string): void
  (event: 'search'): void
  (event: 'reset'): void
  (event: 'quick-search'): void
  (event: 'clear-quick-search'): void
  (event: 'toggle-advanced'): void
  (event: 'clear-applied-filters'): void
  (event: 'remove-applied-filter', key: string): void
  (event: 'update-search-field', payload: { field: SearchFieldKey; value: string | number | undefined }): void
  (event: 'select-cluster', clusterKey: string): void
  (event: 'collapse-cluster'): void
  (event: 'selection-change', rows: AuditLogRecord[]): void
  (event: 'audit-row-action', payload: { command: string | number | object; row: AuditLogRecord }): void
  (event: 'size-change', size: number): void
  (event: 'page-change', page: number): void
}>()

const expandedClusterRowKeys = computed(() =>
  props.detailClusterMode === 'clustered' && props.selectedClusterKey ? [props.selectedClusterKey] : []
)

function normalizeOptionalNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined
}

function emitFieldUpdate(field: SearchFieldKey, value: string | number | undefined) {
  emit('update-search-field', { field, value })
}

function resolveClusterTarget(row: SystemErrorClusterRow) {
  return row.latestRequestUrl || row.latestRequestMethod || ''
}

function resolveClusterRowClassName({ row }: { row: SystemErrorClusterRow }) {
  return row.clusterKey === props.selectedClusterKey ? 'is-selected' : ''
}

function isClusterExpanded(row: SystemErrorClusterRow) {
  return props.detailClusterMode === 'clustered' && row.clusterKey === props.selectedClusterKey
}

function handleClusterRowClick(row: SystemErrorClusterRow) {
  if (!row?.clusterKey) {
    return
  }
  if (isClusterExpanded(row)) {
    emit('collapse-cluster')
    return
  }
  emit('select-cluster', row.clusterKey)
}

function handleClusterExpandChange(row: SystemErrorClusterRow, expandedRows: SystemErrorClusterRow[]) {
  if (!row?.clusterKey) {
    return
  }
  const isExpanded = Array.isArray(expandedRows)
    ? expandedRows.some((item) => item?.clusterKey === row.clusterKey)
    : false
  if (isExpanded) {
    emit('select-cluster', row.clusterKey)
    return
  }
  if (row.clusterKey === props.selectedClusterKey) {
    emit('collapse-cluster')
  }
}
</script>

<style scoped>
.audit-log-system-panel__cluster-stage {
  display: grid;
  gap: 0.82rem;
  margin-top: 0.88rem;
  padding: 1rem;
}

.audit-log-system-panel__detail-header,
.audit-log-system-panel__stage-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  min-width: 0;
}

.audit-log-system-panel__detail-header {
  margin: 0.92rem 0 0.72rem;
}

.audit-log-system-panel__stage-copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.audit-log-system-panel__stage-copy h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 0.98rem;
  line-height: 1.3;
}

.audit-log-system-panel__stage-copy p,
.audit-log-system-panel__stage-meta {
  margin: 0;
  color: var(--text-caption);
  font-size: 0.8rem;
  line-height: 1.45;
}

.audit-log-system-panel__stage-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.audit-log-cluster-cell {
  display: grid;
  gap: 0.24rem;
  min-width: 0;
}

.audit-log-cluster-cell strong,
.audit-log-cluster-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-log-cluster-cell strong {
  color: var(--text-heading);
}

.audit-log-cluster-cell span {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.audit-log-cluster-cell__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.audit-log-system-panel__cluster-table-wrap {
  min-width: 0;
}

.audit-log-system-panel__cluster-table :deep(.el-table__row.is-selected > td) {
  background: color-mix(in srgb, var(--el-color-primary-light-9) 82%, white);
}

.audit-log-system-panel__cluster-table :deep(.el-table__expanded-cell) {
  padding: 0;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 28%, white);
}

.audit-log-cluster-cell--button {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.audit-log-system-panel__inline-detail {
  display: grid;
  gap: 0.82rem;
  padding: 1rem 1rem 1.08rem;
}

.audit-log-system-panel__inline-detail-surface,
.audit-log-system-panel__fallback-stage {
  display: grid;
  gap: 0.82rem;
}

.audit-log-system-panel__fallback-stage {
  margin-top: 0.88rem;
  padding: 1rem;
}

@media (max-width: 640px) {
  .audit-log-system-panel__detail-header,
  .audit-log-system-panel__stage-header {
    flex-direction: column;
  }
}
</style>
