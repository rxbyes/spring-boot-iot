<template>
  <StandardWorkbenchPanel
    title="接入失败归档台"
    description="查看 MQTT / $dp 接入失败归档、契约快照与原始报文，并决定下一步回链路追踪、异常观测还是治理修正。"
    show-filters
    :show-applied-filters="hasAppliedFilters"
    :show-inline-state="showInlineState"
    show-toolbar
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
              placeholder="快速搜索（TraceId）"
              clearable
              prefix-icon="Search"
              @keyup.enter="handleQuickSearch"
              @clear="handleClearQuickSearch"
            />
          </el-form-item>
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
              v-model="searchForm.failureStage"
              placeholder="失败阶段"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
        </template>
        <template #advanced>
          <el-form-item>
            <el-input
              v-model="searchForm.protocolCode"
              placeholder="协议编码"
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
          <el-form-item>
            <el-input
              v-model="searchForm.topic"
              placeholder="Topic"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="searchForm.clientId"
              placeholder="ClientId"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
        </template>
        <template #actions>
          <StandardButton action="query" @click="handleSearch">查询</StandardButton>
          <StandardButton action="reset" @click="handleReset">重置</StandardButton>
        </template>
      </StandardListFilterHeader>
      <div v-if="appliedFilters.traceId.trim()" class="access-error-quick-search-tag">
        <el-tag closable class="access-error-quick-search-tag__chip" @close="handleClearQuickSearch">
          快速搜索：{{ appliedFilters.traceId.trim() }}
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

    <template #inline-state>
      <StandardInlineState :message="inlineStateMessage" tone="info" />
    </template>

    <template #toolbar>
      <StandardTableToolbar
        compact
        :meta-items="[
          `当前结果 ${pagination.total} 条`,
          `近1小时 ${accessErrorStats.recentHourCount} 条`,
          `近24小时 ${accessErrorStats.recent24HourCount} 条`,
          `设备 ${accessErrorStats.distinctDeviceCount} 台`
        ]"
      >
        <template #right>
          <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
        </template>
      </StandardTableToolbar>
    </template>

    <div
      v-loading="loading"
      class="access-error-table-wrap standard-list-surface"
      element-loading-text="正在刷新失败归档列表"
      element-loading-background="var(--loading-mask-bg)"
    >
      <div v-if="tableData.length > 0" class="access-error-mobile-list standard-mobile-record-list">
        <div class="access-error-mobile-list__grid standard-mobile-record-grid">
          <article
            v-for="row in tableData"
            :key="row.id || row.traceId || row.deviceCode"
            class="access-error-mobile-card standard-mobile-record-card"
          >
            <div class="access-error-mobile-card__header">
              <div class="access-error-mobile-card__heading">
                <strong class="access-error-mobile-card__title">{{ formatValue(row.traceId) }}</strong>
                <span class="access-error-mobile-card__sub">{{ formatValue(row.deviceCode) }}</span>
              </div>
              <span class="access-error-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ getFailureStageLabel(row.failureStage) }}
              </span>
            </div>

            <div class="access-error-mobile-card__meta">
              <span class="access-error-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ formatValue(row.productKey) }}
              </span>
              <span class="access-error-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ formatValue(row.protocolCode) }}
              </span>
              <span class="access-error-mobile-card__meta-item standard-mobile-record-card__meta-item">
                {{ formatValue(row.errorCode) }}
              </span>
            </div>

            <div class="access-error-mobile-card__info">
              <div class="access-error-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">产品标识</span>
                <strong class="standard-mobile-record-card__field-value">{{ formatValue(row.productKey) }}</strong>
              </div>
              <div class="access-error-mobile-card__field">
                <span class="standard-mobile-record-card__field-label">归档时间</span>
                <strong class="standard-mobile-record-card__field-value">{{ formatDateTime(row.createTime) }}</strong>
              </div>
              <div class="access-error-mobile-card__field access-error-mobile-card__field--full">
                <span class="standard-mobile-record-card__field-label">Topic</span>
                <strong class="standard-mobile-record-card__field-value">{{ formatValue(row.topic) }}</strong>
              </div>
              <div class="access-error-mobile-card__field access-error-mobile-card__field--full">
                <span class="standard-mobile-record-card__field-label">异常摘要</span>
                <strong class="standard-mobile-record-card__field-value">{{ formatInlineText(row.errorMessage) }}</strong>
              </div>
            </div>

            <StandardWorkbenchRowActions
              variant="card"
              :direct-items="getArchiveDirectActions(row)"
              @command="(command) => handleArchiveRowAction(command, row)"
            />
          </article>
        </div>
      </div>

      <el-table
        class="access-error-table"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <StandardTableTextColumn
          prop="traceId"
          label="Trace / 设备"
          :min-width="220"
          secondary-prop="deviceCode"
        />
        <StandardTableTextColumn label="失败阶段" :width="150">
          <template #default="{ row }">
            {{ getFailureStageLabel(row.failureStage) }}
          </template>
        </StandardTableTextColumn>
        <StandardTableTextColumn prop="productKey" label="产品标识" :min-width="140" />
        <StandardTableTextColumn prop="protocolCode" label="协议编码" :min-width="130" />
        <StandardTableTextColumn prop="errorCode" label="异常编码" :min-width="120" />
        <StandardTableTextColumn prop="exceptionClass" label="异常类型" :min-width="180" />
        <StandardTableTextColumn prop="topic" label="Topic" :min-width="220" />
        <StandardTableTextColumn label="异常摘要" :min-width="240">
          <template #default="{ row }">
            {{ formatInlineText(row.errorMessage) }}
          </template>
        </StandardTableTextColumn>
        <StandardTableTextColumn label="归档时间" :width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </StandardTableTextColumn>
        <el-table-column
          label="操作"
          :width="accessErrorActionColumnWidth"
          fixed="right"
          class-name="standard-row-actions-column"
          :show-overflow-tooltip="false"
        >
          <template #default="{ row }">
            <StandardWorkbenchRowActions
              variant="table"
              :direct-items="getArchiveDirectActions(row)"
              @command="(command) => handleArchiveRowAction(command, row)"
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

  <StandardDetailDrawer
    v-model="detailVisible"
    :title="detailTitle"
    :subtitle="detailSubtitle"
    :tags="detailTags"
    :loading="detailLoading"
    :error-message="detailErrorMessage"
    :empty="!hasDetail"
  >
    <div class="access-error-detail-workbench">
      <section class="access-error-detail-workbench__stage" data-testid="access-error-detail-summary-stage">
        <div class="access-error-detail-workbench__stage-header">
          <h3>失败态势与处理概况</h3>
        </div>

        <div class="access-error-detail-workbench__summary-grid">
          <article
            v-for="card in detailSummaryCards"
            :key="card.key"
            class="access-error-detail-workbench__summary-card"
          >
            <span class="access-error-detail-workbench__summary-label">{{ card.label }}</span>
            <span class="access-error-detail-workbench__summary-value">{{ card.value }}</span>
            <span v-if="card.hint" class="access-error-detail-workbench__summary-hint">{{ card.hint }}</span>
          </article>
        </div>
      </section>

      <section
        class="access-error-detail-workbench__stage access-error-detail-workbench__stage--subtle"
        data-testid="access-error-detail-identity-stage"
      >
        <div class="access-error-detail-workbench__stage-header">
          <h3>链路与主体台账</h3>
        </div>

        <div class="access-error-detail-workbench__fact-table">
          <div
            v-for="item in detailIdentityItems"
            :key="item.key"
            :class="[
              'access-error-detail-workbench__fact-row',
              { 'access-error-detail-workbench__fact-row--stacked': item.wide }
            ]"
          >
            <span class="access-error-detail-workbench__fact-label">{{ item.label }}</span>
            <span class="access-error-detail-workbench__fact-value">{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section
        class="access-error-detail-workbench__stage access-error-detail-workbench__stage--subtle"
        data-testid="access-error-detail-diagnosis-stage"
      >
        <div class="access-error-detail-workbench__stage-header">
          <h3>异常诊断与回跳</h3>
        </div>

        <div class="access-error-detail-workbench__fact-table">
          <div
            v-for="item in detailDiagnosisItems"
            :key="item.key"
            :class="[
              'access-error-detail-workbench__fact-row',
              { 'access-error-detail-workbench__fact-row--stacked': item.wide }
            ]"
          >
            <span class="access-error-detail-workbench__fact-label">{{ item.label }}</span>
            <span class="access-error-detail-workbench__fact-value">{{ item.value }}</span>
          </div>
        </div>

        <div class="access-error-detail-workbench__notice access-error-detail-workbench__notice--danger">
          <span class="access-error-detail-workbench__notice-label">排查建议</span>
          <strong class="access-error-detail-workbench__notice-value">{{ detailRouteAdvice }}</strong>
          <div class="access-error-detail-workbench__notice-actions">
            <StandardButton action="refresh" link :disabled="!canJumpToSystemLog(detailData)" @click="jumpToSystemLog(detailData)">
              异常观测台
            </StandardButton>
            <StandardButton action="refresh" link :disabled="!canJumpToTrace(detailData)" @click="jumpToMessageTrace(detailData)">
              链路追踪台
            </StandardButton>
            <StandardButton action="refresh" link :disabled="!detailData.productKey" @click="jumpToProductGovernance">
              产品定义中心
            </StandardButton>
            <StandardButton action="refresh" link :disabled="!detailData.deviceCode" @click="jumpToDeviceGovernance">
              设备资产中心
            </StandardButton>
          </div>
        </div>
      </section>

      <section class="access-error-detail-workbench__stage" data-testid="access-error-detail-snapshot-stage">
        <div class="access-error-detail-workbench__stage-header">
          <h3>契约与报文快照</h3>
        </div>

        <div class="access-error-detail-workbench__payload-stack">
          <article class="access-error-detail-workbench__payload-panel">
            <div class="access-error-detail-workbench__payload-header">契约快照</div>
            <pre class="access-error-detail-workbench__code-block">{{ detailContractSnapshot }}</pre>
          </article>
          <article class="access-error-detail-workbench__payload-panel">
            <div class="access-error-detail-workbench__payload-header">原始报文</div>
            <pre class="access-error-detail-workbench__code-block">{{ detailPayload }}</pre>
          </article>
        </div>
      </section>
    </div>
  </StandardDetailDrawer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { accessErrorApi, type DeviceAccessErrorQueryParams } from '@/api/accessError';
import { isHandledRequestError } from '@/api/request';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import type { DeviceAccessErrorLog, DeviceAccessErrorStats } from '@/types/api';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { formatDateTime, prettyJson } from '@/utils/format';
import {
  buildDiagnosticRouteQuery,
  persistDiagnosticContext,
  resolveDiagnosticContext
} from '@/utils/iotAccessDiagnostics';

interface DetailSummaryCard {
  key: string;
  label: string;
  value: string;
  hint?: string;
}

interface DetailLedgerItem {
  key: string;
  label: string;
  value: string;
  wide?: boolean;
}

const route = useRoute();
const router = useRouter();
const accessErrorActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [
    { command: 'detail', label: '详情' },
    { command: 'trace', label: '追踪' },
    { command: 'observe', label: '观测' }
  ]
});

const searchForm = reactive({
  traceId: '',
  protocolCode: '',
  failureStage: '',
  deviceCode: '',
  productKey: '',
  topic: '',
  clientId: '',
  errorCode: '',
  exceptionClass: ''
});
const appliedFilters = reactive({
  traceId: '',
  protocolCode: '',
  failureStage: '',
  deviceCode: '',
  productKey: '',
  topic: '',
  clientId: '',
  errorCode: '',
  exceptionClass: ''
});
const quickSearchKeyword = ref('');
const showAdvancedFilters = ref(false);

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination();

const loading = ref(false);
const statsLoading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailErrorMessage = ref('');
const tableData = ref<DeviceAccessErrorLog[]>([]);
const detailData = ref<Partial<DeviceAccessErrorLog>>({});
const restoredDiagnosticContext = ref(resolveDiagnosticContext(route.query as Record<string, unknown>));

const createEmptyStats = (): DeviceAccessErrorStats => ({
  total: 0,
  recentHourCount: 0,
  recent24HourCount: 0,
  distinctTraceCount: 0,
  distinctDeviceCount: 0,
  topFailureStages: [],
  topErrorCodes: [],
  topExceptionClasses: [],
  topProtocolCodes: [],
  topTopics: []
});

const accessErrorStats = ref<DeviceAccessErrorStats>(createEmptyStats());
const hasDetail = computed(() => Object.keys(detailData.value).length > 0);
const detailTitle = computed(() => detailData.value.deviceCode || detailData.value.traceId || '接入失败详情');
const detailSubtitle = computed(() => detailData.value.errorMessage || '查看接入失败归档详情');
const detailDisplayTime = computed(() => formatDateTime(detailData.value.createTime));
const detailPayload = computed(() => prettyJson(detailData.value.rawPayload || '--'));
const detailContractSnapshot = computed(() => prettyJson(detailData.value.contractSnapshot || '--'));
const detailPayloadTruncatedText = computed(() => {
  if (detailData.value.payloadTruncated === 1) {
    return '是';
  }
  if (detailData.value.payloadTruncated === 0) {
    return '否';
  }
  return '--';
});
const detailRouteAdvice = computed(() => {
  if (detailData.value.traceId) {
    return `建议先到链路追踪台核对失败阶段（TraceId：${detailData.value.traceId}），再视证据回异常观测台或治理页修正。`;
  }
  if (detailData.value.deviceCode) {
    return `建议先按设备编码 ${detailData.value.deviceCode} 回链路追踪台补齐主链路证据，再决定去异常观测台还是治理页修正。`;
  }
  return '建议先结合 Topic、协议编码和契约快照定位失败发生点，再决定回链路追踪台还是治理页。';
});
const detailSummaryCards = computed<DetailSummaryCard[]>(() => [
  {
    key: 'failureStage',
    label: '失败阶段',
    value: getFailureStageLabel(detailData.value.failureStage),
    hint: `异常编码：${formatValue(detailData.value.errorCode)}`
  },
  {
    key: 'archiveTime',
    label: '归档时间',
    value: detailDisplayTime.value,
    hint: `日志 ID：${formatValue(detailData.value.id)}`
  },
  {
    key: 'deviceCode',
    label: '设备编码',
    value: formatValue(detailData.value.deviceCode),
    hint: `产品标识：${formatValue(detailData.value.productKey)}`
  },
  {
    key: 'protocolCode',
    label: '协议编码',
    value: formatValue(detailData.value.protocolCode),
    hint: `请求通道：${formatValue(detailData.value.requestMethod)}`
  }
]);
const detailIdentityItems = computed<DetailLedgerItem[]>(() => [
  { key: 'traceId', label: 'TraceId', value: formatValue(detailData.value.traceId), wide: true },
  { key: 'productKey', label: '产品标识', value: formatValue(detailData.value.productKey) },
  { key: 'clientId', label: 'ClientId', value: formatValue(detailData.value.clientId) },
  { key: 'gatewayDeviceCode', label: '网关设备', value: formatValue(detailData.value.gatewayDeviceCode) },
  { key: 'subDeviceCode', label: '子设备', value: formatValue(detailData.value.subDeviceCode) },
  { key: 'topicRouteType', label: '路由类型', value: formatValue(detailData.value.topicRouteType) },
  { key: 'topic', label: 'Topic', value: formatValue(detailData.value.topic), wide: true }
]);
const detailDiagnosisItems = computed<DetailLedgerItem[]>(() => [
  { key: 'messageType', label: '消息类型', value: formatValue(detailData.value.messageType) },
  { key: 'payloadSize', label: '载荷长度', value: formatValue(detailData.value.payloadSize) },
  { key: 'payloadEncoding', label: '载荷编码', value: formatValue(detailData.value.payloadEncoding) },
  { key: 'payloadTruncated', label: '是否截断', value: detailPayloadTruncatedText.value },
  { key: 'exceptionClass', label: '异常类型', value: formatValue(detailData.value.exceptionClass), wide: true },
  { key: 'errorMessage', label: '异常摘要', value: formatValue(detailData.value.errorMessage), wide: true }
]);
const detailTags = computed(() => {
  if (!hasDetail.value) {
    return [];
  }
  return [
    { label: getFailureStageLabel(detailData.value.failureStage), type: 'warning' as const },
    ...(detailData.value.errorCode ? [{ label: `Code ${detailData.value.errorCode}`, type: 'danger' as const }] : [])
  ];
});
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
    { key: 'traceId', label: 'TraceId' },
    { key: 'deviceCode', label: '设备编码' },
    { key: 'productKey', label: '产品标识' },
    { key: 'failureStage', label: (value) => `失败阶段：${getFailureStageLabel(value)}` },
    { key: 'protocolCode', label: '协议编码', advanced: true },
    { key: 'errorCode', label: '异常编码', advanced: true },
    { key: 'exceptionClass', label: '异常类型', advanced: true },
    { key: 'topic', label: 'Topic', advanced: true },
    { key: 'clientId', label: 'ClientId', advanced: true }
  ],
  defaults: {
    traceId: '',
    protocolCode: '',
    failureStage: '',
    deviceCode: '',
    productKey: '',
    topic: '',
    clientId: '',
    errorCode: '',
    exceptionClass: ''
  }
});
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return '';
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`;
});
const statsSummaryText = computed(() => {
  if (statsLoading.value) {
    return '正在加载失败归档统计概览...';
  }
  const topStage = accessErrorStats.value.topFailureStages[0]?.label || '--';
  const topProtocol = accessErrorStats.value.topProtocolCodes[0]?.label || '--';
  return `失败总量 ${accessErrorStats.value.total}，近1小时 ${accessErrorStats.value.recentHourCount}，近24小时 ${accessErrorStats.value.recent24HourCount}，Trace ${accessErrorStats.value.distinctTraceCount}，高频阶段 ${topStage}，高频协议 ${topProtocol}`;
});
const inlineStateMessage = computed(() => {
  const sourceText = restoredDiagnosticContext.value
    ? '已承接关联排障上下文'
    : '失败归档基于 iot_device_access_error_log 查询';
  return `${sourceText} · ${statsSummaryText.value}`;
});
const showInlineState = computed(() => Boolean(inlineStateMessage.value));

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.traceId;
}

function applyQuickSearchKeywordToFilters() {
  searchForm.traceId = quickSearchKeyword.value.trim();
}

function syncAdvancedFilterState() {
  showAdvancedFilters.value = Boolean(
    searchForm.protocolCode.trim()
      || searchForm.errorCode.trim()
      || searchForm.exceptionClass.trim()
      || searchForm.topic.trim()
      || searchForm.clientId.trim()
  );
}

function readQueryValue(key: keyof DeviceAccessErrorQueryParams) {
  const value = route.query[key];
  return typeof value === 'string' ? value : '';
}

function applyRouteQuery() {
  const context = resolveDiagnosticContext(route.query as Record<string, unknown>);
  restoredDiagnosticContext.value = context;
  searchForm.traceId = readQueryValue('traceId') || context?.traceId || '';
  searchForm.protocolCode = readQueryValue('protocolCode');
  searchForm.failureStage = readQueryValue('failureStage');
  searchForm.deviceCode = readQueryValue('deviceCode') || context?.deviceCode || '';
  searchForm.productKey = readQueryValue('productKey') || context?.productKey || '';
  searchForm.topic = readQueryValue('topic') || context?.topic || '';
  searchForm.clientId = readQueryValue('clientId');
  searchForm.errorCode = readQueryValue('errorCode');
  searchForm.exceptionClass = readQueryValue('exceptionClass');
  syncQuickSearchKeywordFromFilters();
  syncAdvancedFilterState();
}

function buildFilterQueryParams(): DeviceAccessErrorQueryParams {
  return {
    traceId: appliedFilters.traceId,
    protocolCode: appliedFilters.protocolCode,
    failureStage: appliedFilters.failureStage,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    topic: appliedFilters.topic,
    clientId: appliedFilters.clientId,
    errorCode: appliedFilters.errorCode,
    exceptionClass: appliedFilters.exceptionClass
  };
}

function buildQueryParams(): DeviceAccessErrorQueryParams {
  return {
    ...buildFilterQueryParams(),
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  };
}

function notifyRequestError(error: unknown, fallbackMessage: string) {
  if (!isHandledRequestError(error)) {
    ElMessage.error(error instanceof Error ? error.message : fallbackMessage);
  }
}

async function loadTableData() {
  loading.value = true;
  try {
    const response = await accessErrorApi.pageAccessErrors(buildQueryParams());
    if (response.code === 200) {
      tableData.value = applyPageResult(response.data);
    }
  } catch (error) {
    tableData.value = [];
    resetTotal();
    notifyRequestError(error, '获取接入失败归档失败');
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  statsLoading.value = true;
  try {
    accessErrorStats.value = createEmptyStats();
    const response = await accessErrorApi.getAccessErrorStats(buildFilterQueryParams());
    if (response.code === 200 && response.data) {
      accessErrorStats.value = { ...createEmptyStats(), ...response.data };
    }
  } catch (error) {
    accessErrorStats.value = createEmptyStats();
    notifyRequestError(error, '获取失败归档统计失败');
  } finally {
    statsLoading.value = false;
  }
}

function resetSearchForm() {
  searchForm.traceId = '';
  searchForm.protocolCode = '';
  searchForm.failureStage = '';
  searchForm.deviceCode = '';
  searchForm.productKey = '';
  searchForm.topic = '';
  searchForm.clientId = '';
  searchForm.errorCode = '';
  searchForm.exceptionClass = '';
  quickSearchKeyword.value = '';
  showAdvancedFilters.value = false;
}

function triggerSearch(resetPageFirst = false) {
  applyQuickSearchKeywordToFilters();
  syncAdvancedFilterState();
  syncAppliedFilters();
  if (resetPageFirst) {
    resetPage();
  }
  loadTableData();
  loadStats();
}

function handleSearch() {
  triggerSearch(true);
}

function handleReset() {
  resetSearchForm();
  triggerSearch(true);
}

function handleRefresh() {
  loadTableData();
  loadStats();
}

function handleQuickSearch() {
  triggerSearch(true);
}

function handleClearQuickSearch() {
  quickSearchKeyword.value = '';
  triggerSearch(true);
}

function toggleAdvancedFilters() {
  showAdvancedFilters.value = !showAdvancedFilters.value;
}

function handleClearAppliedFilters() {
  handleReset();
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key);
  syncQuickSearchKeywordFromFilters();
  syncAdvancedFilterState();
  triggerSearch(true);
}

function handleSizeChange(size: number) {
  setPageSize(size);
  loadTableData();
}

function handlePageChange(page: number) {
  setPageNum(page);
  loadTableData();
}

function getArchiveDirectActions(row: DeviceAccessErrorLog) {
  return [
    { command: 'detail', label: '详情' },
    { command: 'trace', label: '追踪', disabled: !canJumpToTrace(row) },
    { command: 'observe', label: '观测', disabled: !canJumpToSystemLog(row) }
  ];
}

function handleArchiveRowAction(command: string | number, row: DeviceAccessErrorLog) {
  if (command === 'detail') {
    void handleDetail(row);
    return;
  }
  if (command === 'trace') {
    if (!canJumpToTrace(row)) {
      return;
    }
    jumpToMessageTrace(row);
    return;
  }
  if (command === 'observe') {
    if (!canJumpToSystemLog(row)) {
      return;
    }
    jumpToSystemLog(row);
  }
}

async function handleDetail(row: DeviceAccessErrorLog) {
  if (row.id === undefined || row.id === null || row.id === '') {
    ElMessage.warning('当前失败归档缺少主键，无法查看详情');
    return;
  }

  detailVisible.value = true;
  detailLoading.value = true;
  detailErrorMessage.value = '';
  detailData.value = { ...row };
  try {
    const response = await accessErrorApi.getAccessErrorById(row.id);
    if (response.code === 200 && response.data) {
      detailData.value = { ...row, ...response.data };
    }
  } catch (error) {
    detailErrorMessage.value = error instanceof Error ? error.message : '获取失败归档详情失败';
    notifyRequestError(error, '获取失败归档详情失败');
  } finally {
    detailLoading.value = false;
  }
}

function canJumpToTrace(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
  return Boolean(source.traceId || source.deviceCode || source.productKey || source.topic);
}

function canJumpToSystemLog(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
  return Boolean(source.traceId || source.deviceCode || source.productKey || source.topic);
}

function persistAccessErrorContext(source?: Partial<DeviceAccessErrorLog>) {
  persistDiagnosticContext({
    sourcePage: 'access-error',
    traceId: source?.traceId || appliedFilters.traceId || restoredDiagnosticContext.value?.traceId || undefined,
    deviceCode: source?.deviceCode || appliedFilters.deviceCode || restoredDiagnosticContext.value?.deviceCode || undefined,
    productKey: source?.productKey || appliedFilters.productKey || restoredDiagnosticContext.value?.productKey || undefined,
    topic: source?.topic || appliedFilters.topic || restoredDiagnosticContext.value?.topic || undefined,
    reportStatus: 'failed',
    capturedAt: new Date().toISOString()
  });
}

function jumpToMessageTrace(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
  persistAccessErrorContext(source);
  router.push({
    path: '/message-trace',
    query: buildDiagnosticRouteQuery({
      sourcePage: 'access-error',
      traceId: source.traceId || undefined,
      deviceCode: source.deviceCode || undefined,
      productKey: source.productKey || undefined,
      topic: source.topic || undefined,
      capturedAt: new Date().toISOString()
    })
  });
}

function jumpToSystemLog(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
  persistAccessErrorContext(source);
  router.push({
    path: '/system-log',
    query: {
      traceId: source.traceId || undefined,
      deviceCode: source.deviceCode || undefined,
      productKey: source.productKey || undefined,
      requestUrl: source.topic || undefined,
      requestMethod: source.topic ? 'MQTT' : undefined,
      errorCode: 'errorCode' in source ? source.errorCode || undefined : undefined,
      exceptionClass: 'exceptionClass' in source ? source.exceptionClass || undefined : undefined
    }
  });
}

function jumpToProductGovernance() {
  if (!detailData.value.productKey) {
    return;
  }
  persistAccessErrorContext(detailData.value);
  router.push({
    path: '/products',
    query: {
      productKey: detailData.value.productKey,
      traceId: detailData.value.traceId || undefined
    }
  });
}

function jumpToDeviceGovernance() {
  if (!detailData.value.deviceCode) {
    return;
  }
  persistAccessErrorContext(detailData.value);
  router.push({
    path: '/devices',
    query: {
      deviceCode: detailData.value.deviceCode,
      productKey: detailData.value.productKey || undefined,
      traceId: detailData.value.traceId || undefined
    }
  });
}

function getFailureStageLabel(value?: string | null) {
  switch (value) {
    case 'ingress':
      return '接入预处理失败';
    case 'protocol_decode':
      return '协议解码失败';
    case 'topic_parse':
      return 'Topic 解析失败';
    case 'device_contract':
      return '设备契约失败';
    case 'device_validate':
      return '设备校验失败';
    case 'message_log':
      return '消息日志失败';
    case 'payload_apply':
      return '载荷应用失败';
    case 'telemetry_persist':
      return '遥测落库失败';
    case 'device_state':
      return '设备状态失败';
    case 'risk_dispatch':
      return '风险派发失败';
    case 'product_validate':
      return '产品校验失败';
    case 'message_dispatch':
      return '消息分发失败';
    default:
      return formatValue(value);
  }
}

function formatValue(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '--';
  }
  return String(value);
}

function formatInlineText(value?: string | null) {
  if (!value) {
    return '--';
  }
  const normalized = String(value).replace(/\s+/g, ' ').trim();
  return normalized || '--';
}

watch(
  () => [
    route.query.mode,
    route.query.traceId,
    route.query.protocolCode,
    route.query.failureStage,
    route.query.deviceCode,
    route.query.productKey,
    route.query.topic,
    route.query.clientId,
    route.query.errorCode,
    route.query.exceptionClass
  ],
  (current, previous) => {
    if (JSON.stringify(current) === JSON.stringify(previous)) {
      return;
    }
    if (route.query.mode !== 'access-error') {
      return;
    }
    applyRouteQuery();
    resetPage();
    syncAppliedFilters();
    loadTableData();
    loadStats();
  }
);

watch(detailVisible, (visible) => {
  if (!visible) {
    detailLoading.value = false;
    detailErrorMessage.value = '';
    detailData.value = {};
  }
});

onMounted(() => {
  applyRouteQuery();
  syncAppliedFilters();
  loadTableData();
  loadStats();
});
</script>

<style scoped>
.access-error-quick-search-tag {
  margin-top: 0.72rem;
}

.access-error-quick-search-tag__chip {
  margin: 0;
}

.access-error-detail-workbench {
  display: grid;
  gap: 1rem;
}

.access-error-detail-workbench__stage,
.access-error-detail-workbench__payload-panel {
  display: grid;
  gap: 0.9rem;
  padding: 1rem;
  border: 1px solid var(--panel-border);
  border-radius: calc(var(--radius-lg) + 4px);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-inset-highlight-78);
}

.access-error-detail-workbench__stage--subtle,
.access-error-detail-workbench__payload-panel {
  background: rgba(255, 255, 255, 0.9);
}

.access-error-detail-workbench__stage-header h3 {
  margin: 0;
  color: var(--text-heading);
  font-size: 16px;
  line-height: 1.4;
}

.access-error-detail-workbench__summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.access-error-detail-workbench__summary-card {
  display: grid;
  gap: 0.38rem;
  min-width: 0;
  padding: 1rem 1.05rem;
  border: 1px solid rgba(203, 213, 225, 0.86);
  border-radius: calc(var(--radius-md) + 2px);
  background:
    linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(244, 248, 255, 0.94) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.access-error-detail-workbench__summary-label,
.access-error-detail-workbench__fact-label,
.access-error-detail-workbench__notice-label {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
}

.access-error-detail-workbench__summary-value {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.access-error-detail-workbench__summary-hint {
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
}

.access-error-detail-workbench__fact-table {
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: calc(var(--radius-md) + 2px);
  background: rgba(255, 255, 255, 0.96);
}

.access-error-detail-workbench__fact-row {
  display: grid;
  grid-template-columns: 8.5rem minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.access-error-detail-workbench__fact-row:last-child {
  border-bottom: none;
}

.access-error-detail-workbench__fact-row--stacked {
  grid-template-columns: minmax(0, 1fr);
}

.access-error-detail-workbench__fact-label,
.access-error-detail-workbench__fact-value {
  min-width: 0;
  padding: 0.88rem 1rem;
  line-height: 1.6;
}

.access-error-detail-workbench__fact-label {
  display: flex;
  align-items: center;
  background: rgba(248, 250, 252, 0.96);
  border-right: 1px solid rgba(226, 232, 240, 0.92);
}

.access-error-detail-workbench__fact-row--stacked .access-error-detail-workbench__fact-label {
  border-right: none;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
}

.access-error-detail-workbench__fact-value,
.access-error-detail-workbench__notice-value {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 400;
  overflow-wrap: anywhere;
}

.access-error-detail-workbench__notice {
  display: grid;
  gap: 0.75rem;
  padding: 0.92rem 1rem;
  border: 1px solid rgba(203, 213, 225, 0.92);
  border-radius: calc(var(--radius-md) + 2px);
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.98) 0%, rgba(244, 248, 255, 0.94) 100%);
}

.access-error-detail-workbench__notice--danger {
  border-color: color-mix(in srgb, var(--danger, #d45d5d) 26%, var(--panel-border));
  background: color-mix(in srgb, #fff7f7 72%, rgba(255, 255, 255, 0.92));
}

.access-error-detail-workbench__notice-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.access-error-detail-workbench__payload-stack {
  display: grid;
  gap: 1rem;
}

.access-error-detail-workbench__payload-header {
  margin: -1rem -1rem 0;
  padding: 0.78rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.45;
  background: rgba(248, 250, 252, 0.96);
}

.access-error-detail-workbench__code-block {
  margin: 0;
  min-height: 11rem;
  padding: 1rem 0;
  border: none;
  background: transparent;
  color: var(--text-heading);
  font-size: 13px;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.access-error-table-wrap {
  min-width: 0;
}

.access-error-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.access-error-mobile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.72rem;
}

.access-error-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.access-error-mobile-card__title {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.access-error-mobile-card__sub {
  overflow: hidden;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.access-error-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.access-error-mobile-card__meta-item {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.access-error-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.access-error-mobile-card__field {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.access-error-mobile-card__field--full {
  grid-column: 1 / -1;
}

.access-error-mobile-card__field .standard-mobile-record-card__field-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.access-error-table {
  display: block;
}

@media (max-width: 640px) {
  .access-error-detail-workbench__summary-grid,
  .access-error-detail-workbench__fact-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .access-error-detail-workbench__fact-label {
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  }

  .access-error-mobile-list {
    display: block;
  }

  .access-error-table {
    display: none;
  }

  .access-error-mobile-card__info {
    grid-template-columns: 1fr;
  }
}
</style>
