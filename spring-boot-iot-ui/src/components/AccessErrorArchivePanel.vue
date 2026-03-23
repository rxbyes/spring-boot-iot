<template>
  <StandardWorkbenchPanel
    title="接入失败归档台"
    description="查看 MQTT / $dp 接入失败归档、契约快照与原始报文，快速回放失败上下文。"
    show-header-actions
    show-filters
    :show-applied-filters="hasAppliedFilters"
    show-notices
    show-toolbar
    show-pagination
  >
    <template #header-actions>
      <StandardChoiceGroup
        :model-value="viewMode"
        :options="viewModeOptions"
        responsive
        @update:modelValue="handleModeChange"
      />
    </template>

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

    <template #notices>
      <div class="access-error-notice-grid">
        <el-alert
          title="失败归档基于 `iot_device_access_error_log` 查询，可直接查看 failureStage、契约快照和原始报文。"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
        <el-alert
          :title="statsSummaryText"
          type="success"
          :closable="false"
          show-icon
          class="stats-alert"
        />
      </div>
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
          <StandardButton
            action="refresh"
            link
            :disabled="!canJumpWithSearch"
            @click="jumpToSystemLog()"
          >
            跳转异常观测台
          </StandardButton>
          <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
        </template>
      </StandardTableToolbar>
    </template>

    <el-table
      v-loading="loading"
      class="access-error-table"
      :data="tableData"
      border
      stripe
      style="width: 100%"
    >
      <StandardTableTextColumn prop="traceId" label="TraceId" :min-width="200" />
      <StandardTableTextColumn label="失败阶段" :width="150">
        <template #default="{ row }">
          {{ getFailureStageLabel(row.failureStage) }}
        </template>
      </StandardTableTextColumn>
      <StandardTableTextColumn prop="deviceCode" label="设备编码" :min-width="140" />
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
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <StandardRowActions variant="table" gap="wide">
            <StandardActionLink @click="handleDetail(row)">详情</StandardActionLink>
            <StandardActionLink :disabled="!canJumpToTrace(row)" @click="jumpToMessageTrace(row)">追踪</StandardActionLink>
            <StandardActionLink :disabled="!canJumpToSystemLog(row)" @click="jumpToSystemLog(row)">观测</StandardActionLink>
          </StandardRowActions>
        </template>
      </el-table-column>
    </el-table>

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
    eyebrow="接入失败详情"
    :title="detailTitle"
    :subtitle="detailSubtitle"
    :tags="detailTags"
    :loading="detailLoading"
    :error-message="detailErrorMessage"
    :empty="!hasDetail"
  >
    <section class="detail-panel detail-panel--hero">
      <div class="detail-section-header">
        <div>
          <h3>失败概览</h3>
          <p>先看失败阶段、归档时间、设备与协议编码，再决定是回查异常观测台还是继续复盘接入链路。</p>
        </div>
      </div>
      <div class="detail-summary-grid">
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">失败阶段</span>
          <strong class="detail-summary-card__value">{{ getFailureStageLabel(detailData.failureStage) }}</strong>
          <p class="detail-summary-card__hint">异常编码：{{ formatValue(detailData.errorCode) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">归档时间</span>
          <strong class="detail-summary-card__value">{{ detailDisplayTime }}</strong>
          <p class="detail-summary-card__hint">日志 ID：{{ formatValue(detailData.id) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">设备编码</span>
          <strong class="detail-summary-card__value">{{ formatValue(detailData.deviceCode) }}</strong>
          <p class="detail-summary-card__hint">产品标识：{{ formatValue(detailData.productKey) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">协议编码</span>
          <strong class="detail-summary-card__value">{{ formatValue(detailData.protocolCode) }}</strong>
          <p class="detail-summary-card__hint">路由类型：{{ formatValue(detailData.topicRouteType) }}</p>
        </article>
        <article class="detail-summary-card">
          <span class="detail-summary-card__label">TraceId</span>
          <strong class="detail-summary-card__value">{{ formatValue(detailData.traceId) }}</strong>
          <p class="detail-summary-card__hint">可回查异常观测台与链路追踪台</p>
        </article>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>接入上下文</h3>
          <p>统一展示 topic、clientId、messageType、payload 信息和异常摘要，避免只剩一条异常消息无法定位。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field">
          <span class="detail-field__label">产品标识</span>
          <strong class="detail-field__value">{{ formatValue(detailData.productKey) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">消息类型</span>
          <strong class="detail-field__value">{{ formatValue(detailData.messageType) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">ClientId</span>
          <strong class="detail-field__value">{{ formatValue(detailData.clientId) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">请求通道</span>
          <strong class="detail-field__value">{{ formatValue(detailData.requestMethod) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">载荷长度</span>
          <strong class="detail-field__value">{{ formatValue(detailData.payloadSize) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">载荷编码</span>
          <strong class="detail-field__value">{{ formatValue(detailData.payloadEncoding) }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">是否截断</span>
          <strong class="detail-field__value">{{ detailPayloadTruncatedText }}</strong>
        </div>
        <div class="detail-field">
          <span class="detail-field__label">异常类型</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailData.exceptionClass) }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">Topic</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailData.topic) }}</strong>
        </div>
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">异常摘要</span>
          <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailData.errorMessage) }}</strong>
        </div>
      </div>
      <div class="detail-notice detail-notice--danger">
        <span class="detail-notice__label">排查建议</span>
        <strong class="detail-notice__value">{{ detailRouteAdvice }}</strong>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>契约快照</h3>
          <p>对照 expected / actual protocol、产品归属和路由来源，快速确认失败是否来自建档契约或协议漂移。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">contractSnapshot</span>
          <div class="detail-field__value detail-field__value--pre">{{ detailContractSnapshot }}</div>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-section-header">
        <div>
          <h3>原始报文</h3>
          <p>保留失败发生时的原始 payload 快照，用于核对编码、topic 结构和协议解码前输入。</p>
        </div>
      </div>
      <div class="detail-grid">
        <div class="detail-field detail-field--full">
          <span class="detail-field__label">rawPayload</span>
          <div class="detail-field__value detail-field__value--pre">{{ detailPayload }}</div>
        </div>
      </div>
    </section>
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
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import type { DeviceAccessErrorLog, DeviceAccessErrorStats } from '@/types/api';
import { formatDateTime, prettyJson } from '@/utils/format';

type ObservabilityViewMode = 'message-trace' | 'access-error';

type ViewModeOption = {
  label: string;
  value: ObservabilityViewMode;
};

const props = defineProps<{
  viewMode: ObservabilityViewMode;
  viewModeOptions: ViewModeOption[];
}>();

const emit = defineEmits<{
  (event: 'change-view-mode', value: ObservabilityViewMode): void;
}>();

const route = useRoute();
const router = useRouter();

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
    return `建议先回查 TraceId（${detailData.value.traceId}）对应的异常观测台，再切回链路追踪核对主链路是否已落消息日志。`;
  }
  if (detailData.value.deviceCode) {
    return `建议按设备编码 ${detailData.value.deviceCode} 回查异常观测台和链路追踪台，确认失败发生在建档校验前还是分发阶段。`;
  }
  return '建议结合 Topic、协议编码和契约快照继续排查失败发生点。';
});
const detailTags = computed(() => {
  if (!hasDetail.value) {
    return [];
  }
  return [
    { label: getFailureStageLabel(detailData.value.failureStage), type: 'warning' as const },
    ...(detailData.value.errorCode ? [{ label: `Code ${detailData.value.errorCode}`, type: 'danger' as const }] : []),
    ...(detailData.value.traceId ? [{ label: `Trace ${detailData.value.traceId}`, type: 'info' as const }] : [])
  ];
});
const canJumpWithSearch = computed(() =>
  Boolean(appliedFilters.traceId || appliedFilters.deviceCode || appliedFilters.productKey || appliedFilters.topic)
);
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

function handleModeChange(value: ObservabilityViewMode | string | number | boolean) {
  if (value === 'message-trace' || value === 'access-error') {
    emit('change-view-mode', value);
  }
}

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
  searchForm.traceId = readQueryValue('traceId');
  searchForm.protocolCode = readQueryValue('protocolCode');
  searchForm.failureStage = readQueryValue('failureStage');
  searchForm.deviceCode = readQueryValue('deviceCode');
  searchForm.productKey = readQueryValue('productKey');
  searchForm.topic = readQueryValue('topic');
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

function jumpToMessageTrace(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
  router.push({
    path: '/message-trace',
    query: {
      traceId: source.traceId || undefined,
      deviceCode: source.deviceCode || undefined,
      productKey: source.productKey || undefined,
      topic: source.topic || undefined
    }
  });
}

function jumpToSystemLog(row?: Partial<DeviceAccessErrorLog>) {
  const source = row || appliedFilters;
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

function getFailureStageLabel(value?: string | null) {
  switch (value) {
    case 'protocol_decode':
      return '协议解码失败';
    case 'topic_parse':
      return 'Topic 解析失败';
    case 'device_validate':
      return '设备校验失败';
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

.access-error-notice-grid {
  display: grid;
  gap: 0.72rem;
}
</style>
