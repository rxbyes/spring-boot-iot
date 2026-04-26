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
                  {{ formatValue(row.traceId || row.operationModule) }}
                </strong>
                <span class="audit-log-mobile-card__sub">
                  {{ formatValue(row.deviceCode) }}
                </span>
              </div>
              <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ getOperationResultName(row.operationResult) }}
              </span>
            </div>

            <div class="audit-log-mobile-card__meta">
              <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ formatValue(row.requestMethod) }}
              </span>
              <span class="audit-log-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ formatValue(row.errorCode) }}
              </span>
            </div>

            <div class="audit-log-mobile-card__info">
              <div class="audit-log-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">异常模块</span>
                <strong class="standard-mobile-record-card__field-value">
                  {{ formatValue(row.operationModule) }}
                </strong>
              </div>
              <div class="audit-log-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">设备编码</span>
                <strong class="standard-mobile-record-card__field-value">
                  {{ formatValue(row.deviceCode) }}
                </strong>
              </div>
              <div class="audit-log-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">产品标识</span>
                <strong class="standard-mobile-record-card__field-value">
                  {{ formatValue(row.productKey) }}
                </strong>
              </div>
              <div class="audit-log-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">异常类型</span>
                <strong class="standard-mobile-record-card__field-value">
                  {{ formatValue(row.exceptionClass) }}
                </strong>
              </div>
              <div class="audit-log-mobile-card__field audit-log-mobile-card__field--full">
                <span class="standard-mobile-record-card__field-label">异常摘要</span>
                <strong class="standard-mobile-record-card__field-value">
                  {{ formatValue(row.resultMessage) }}
                </strong>
              </div>
            </div>

            <StandardWorkbenchRowActions
              variant="card"
              :direct-items="getAuditDirectActions(row)"
              @command="emit('audit-row-action', { command: $event, row })"
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
        @selection-change="emit('selection-change', $event)"
      >
        <el-table-column type="selection" width="48" />
        <StandardTableTextColumn prop="operationModule" label="操作模块" :width="150" />
        <StandardTableTextColumn prop="operationMethod" label="操作方法" :min-width="180" />
        <StandardTableTextColumn prop="requestUrl" label="请求URL/目标" :min-width="220" />
        <el-table-column prop="requestMethod" label="请求方法/通道" width="120" />
        <StandardTableTextColumn prop="traceId" label="TraceId" :min-width="180" />
        <StandardTableTextColumn prop="deviceCode" label="设备编码" :min-width="140" />
        <StandardTableTextColumn prop="productKey" label="产品标识" :min-width="140" />
        <StandardTableTextColumn prop="errorCode" label="异常编码" :min-width="120" />
        <StandardTableTextColumn prop="exceptionClass" label="异常类型" :min-width="180" />
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
  </div>
</template>

<script setup lang="ts">
import type { AuditLogRecord } from '@/api/auditLog';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';

interface SystemRequestMethodOption {
  label: string;
  value: string;
}

interface SearchFormModel {
  traceId: string;
  deviceCode: string;
  productKey: string;
  operationModule: string;
  requestMethod: string;
  requestUrl: string;
  errorCode: string;
  exceptionClass: string;
  operationResult: number | undefined;
}

interface PaginationModel {
  pageNum: number;
  pageSize: number;
  total: number;
}

interface WorkbenchActionItem {
  key?: string;
  command: string;
  label: string;
  disabled?: boolean;
  permission?: string;
}

type SearchFieldKey =
  | 'deviceCode'
  | 'productKey'
  | 'operationModule'
  | 'requestMethod'
  | 'requestUrl'
  | 'errorCode'
  | 'exceptionClass'
  | 'operationResult';

defineProps<{
  searchForm: SearchFormModel;
  quickSearchKeyword: string;
  showAdvancedFilters: boolean;
  advancedFilterHint: string;
  requestMethodOptions: SystemRequestMethodOption[];
  appliedQuickSearchValue: string;
  activeFilterTags: Array<Record<string, unknown>>;
  hasAppliedFilters: boolean;
  showInlineState: boolean;
  inlineMessage: string;
  loading: boolean;
  tableData: AuditLogRecord[];
  pagination: PaginationModel;
  auditActionColumnWidth: number | string;
  formatValue: (value: unknown) => string;
  getOperationResultName: (value?: number | null) => string;
  getOperationResultTag: (value?: number | null) => string;
  getAuditDirectActions: (row: AuditLogRecord) => WorkbenchActionItem[];
}>();

const emit = defineEmits<{
  (event: 'update:quickSearchKeyword', value: string): void;
  (event: 'search'): void;
  (event: 'reset'): void;
  (event: 'quick-search'): void;
  (event: 'clear-quick-search'): void;
  (event: 'toggle-advanced'): void;
  (event: 'clear-applied-filters'): void;
  (event: 'remove-applied-filter', key: string): void;
  (event: 'update-search-field', payload: { field: SearchFieldKey; value: string | number | undefined }): void;
  (event: 'selection-change', rows: AuditLogRecord[]): void;
  (event: 'audit-row-action', payload: { command: string | number | object; row: AuditLogRecord }): void;
  (event: 'size-change', size: number): void;
  (event: 'page-change', page: number): void;
}>();

function normalizeOptionalNumber(value: unknown) {
  return typeof value === 'number' ? value : undefined;
}

function emitFieldUpdate(field: SearchFieldKey, value: string | number | undefined) {
  emit('update-search-field', { field, value });
}
</script>
