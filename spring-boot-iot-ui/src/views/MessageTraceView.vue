<template>
  <StandardPageShell class="page-stack message-trace-view" :show-title="false">
    <IotAccessTabWorkspace
      :items="pageModeOptions"
      :default-key="'message-trace'"
      query-key="mode"
    >
      <template #default="{ activeKey }">
        <AccessErrorArchivePanel v-if="activeKey === 'access-error'" />

        <StandardWorkbenchPanel
          v-else
          title="链路追踪台"
          description="主链路复盘：按 TraceId、设备编码、产品标识与 Topic 串联同一条接入链路，并判断下一步去异常观测、数据校验还是治理修正。"
          show-filters
          :show-applied-filters="showAppliedFilters"
          :show-inline-state="showTraceInlineState"
          show-toolbar
          show-pagination
        >

        <template #filters>
          <StandardListFilterHeader
            :model="searchForm"
            :primary-columns="'minmax(320px, 1.5fr) minmax(200px, 0.72fr) minmax(320px, 1fr) auto auto'"
            :primary-visible-count="5"
          >
            <template #primary>
              <el-form-item>
                <el-input
                  id="quick-search"
                  v-model="quickSearchKeyword"
                  placeholder="快速搜索（TraceId / 设备编码 / 产品标识）"
                  clearable
                  prefix-icon="Search"
                  @keyup.enter="handleQuickSearch"
                  @clear="handleClearQuickSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-select v-model="searchForm.messageType" placeholder="消息类型" clearable>
                  <el-option
                    v-for="item in messageTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="searchForm.topic"
                  placeholder="Topic"
                  clearable
                  @keyup.enter="handleSearch"
                />
              </el-form-item>
              <el-form-item class="message-trace-filter-button-item">
                <StandardButton action="query" @click="handleSearch">查询</StandardButton>
              </el-form-item>
              <el-form-item class="message-trace-filter-button-item">
                <StandardButton action="reset" @click="handleReset">重置</StandardButton>
              </el-form-item>
            </template>
          </StandardListFilterHeader>
        </template>

        <template #applied-filters>
          <div v-if="appliedFilters.keyword.trim()" class="message-trace-quick-search-tag">
            <el-tag closable class="message-trace-quick-search-tag__chip" @close="handleClearQuickSearch">
              快速搜索：{{ appliedFilters.keyword.trim() }}
            </el-tag>
          </div>
          <StandardAppliedFiltersBar
            v-if="activeFilterTags.length > 0"
            :tags="activeFilterTags"
            @remove="handleRemoveAppliedFilter"
            @clear="handleClearAppliedFilters"
          />
        </template>

        <template v-if="traceInboundNotice" #notices>
          <section class="message-trace-inbound-notice" data-testid="message-trace-inbound-notice">
            <div class="message-trace-inbound-notice__copy">
              <span class="message-trace-inbound-notice__eyebrow">{{ traceInboundNotice.eyebrow }}</span>
              <strong>{{ traceInboundNotice.title }}</strong>
              <p>{{ traceInboundNotice.description }}</p>
            </div>
            <div class="message-trace-inbound-notice__meta">
              <span
                v-for="pill in traceInboundNotice.pills"
                :key="pill"
                class="message-trace-inbound-notice__pill"
              >
                {{ pill }}
              </span>
            </div>
          </section>
        </template>

        <template v-if="showTraceInlineState" #inline-state>
          <StandardInlineState :message="traceInlineMessage" tone="info" />
        </template>

        <template #toolbar>
          <StandardTableToolbar
            compact
            :meta-items="[
              `当前结果 ${pagination.total} 条`,
              `近1小时 ${traceStats.recentHourCount} 条`,
              `近24小时 ${traceStats.recent24HourCount} 条`,
              `失败摘要 ${traceStats.dispatchFailureCount} 条`
            ]"
        >
          <template #right>
              <StandardButton action="refresh" link @click="handleRefresh">刷新列表</StandardButton>
            </template>
          </StandardTableToolbar>
        </template>

        <div
          v-loading="loading"
          class="message-trace-table-wrap standard-list-surface"
          element-loading-text="正在刷新链路追踪结果"
          element-loading-background="var(--loading-mask-bg)"
        >
          <div v-if="tableData.length > 0" class="message-trace-mobile-list standard-mobile-record-list">
            <div class="message-trace-mobile-list__grid standard-mobile-record-grid">
              <article
                v-for="row in tableData"
                :key="row.id || row.traceId || row.topic"
                class="message-trace-mobile-card standard-mobile-record-card"
              >
                <div class="message-trace-mobile-card__header">
                  <div class="message-trace-mobile-card__heading">
                    <strong class="message-trace-mobile-card__title">{{ formatValue(row.traceId) }}</strong>
                    <span class="message-trace-mobile-card__sub">{{ formatValue(row.deviceCode) }}</span>
                  </div>
                  <span class="message-trace-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ getMessageTypeLabel(row.messageType) }}
                  </span>
                </div>

                <div class="message-trace-mobile-card__meta">
                  <span class="message-trace-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ formatValue(row.productKey) }}
                  </span>
                  <span class="message-trace-mobile-card__meta-item standard-mobile-record-card__meta-item">
                    {{ formatMessageTraceReportTime(row.reportTime, row.createTime) }}
                  </span>
                </div>

                <div class="message-trace-mobile-card__info">
                  <div class="message-trace-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">TraceId</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatValue(row.traceId) }}</strong>
                  </div>
                  <div class="message-trace-mobile-card__field">
                    <span class="standard-mobile-record-card__field-label">产品标识</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatValue(row.productKey) }}</strong>
                  </div>
                  <div class="message-trace-mobile-card__field message-trace-mobile-card__field--full">
                    <span class="standard-mobile-record-card__field-label">Topic</span>
                    <strong class="standard-mobile-record-card__field-value">{{ formatValue(row.topic) }}</strong>
                  </div>
                  <div class="message-trace-mobile-card__field message-trace-mobile-card__field--full">
                    <span class="standard-mobile-record-card__field-label">Payload 摘要</span>
                    <strong
                      class="standard-mobile-record-card__field-value"
                      :title="formatPayloadHoverText(row.payload)"
                    >
                      {{ formatPayloadPreview(row.payload) }}
                    </strong>
                  </div>
                </div>

                <StandardWorkbenchRowActions
                  variant="card"
                  :direct-items="getTraceDirectActions(row)"
                  @command="(command) => handleTraceRowAction(command, row)"
                />
              </article>
            </div>
          </div>

          <el-table
            class="message-trace-table"
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
            <StandardTableTextColumn prop="productKey" label="产品标识" :min-width="140" />
            <StandardTableTextColumn label="消息类型" :width="120">
              <template #default="{ row }">
                {{ getMessageTypeLabel(row.messageType) }}
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn prop="topic" label="Topic" :min-width="220" />
            <StandardTableTextColumn label="Payload 摘要" :min-width="260" :show-overflow-tooltip="false">
              <template #default="{ row }">
                <span class="message-trace-payload-preview" :title="formatPayloadHoverText(row.payload)">
                  {{ formatPayloadPreview(row.payload) }}
                </span>
              </template>
            </StandardTableTextColumn>
            <StandardTableTextColumn label="上报时间" :width="180">
              <template #default="{ row }">
                {{ formatMessageTraceReportTime(row.reportTime, row.createTime) }}
              </template>
            </StandardTableTextColumn>
            <el-table-column
              label="操作"
              :width="messageTraceActionColumnWidth"
              fixed="right"
              class-name="standard-row-actions-column"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <StandardWorkbenchRowActions
                  variant="table"
                  :direct-items="getTraceDirectActions(row)"
                  @command="(command) => handleTraceRowAction(command, row)"
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
      </template>
    </IotAccessTabWorkspace>

    <StandardDetailDrawer
      v-model="detailVisible"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      tag-layout="title-inline"
      size="68rem"
      :tags="detailTags"
      :empty="!hasDetail"
    >
      <MessageTraceDetailWorkbench
        :detail="detailData"
        :panels="detailPayloadComparison.panels"
        :timeline="detailTimeline"
        :timeline-loading="timelineLoading"
        :timeline-expired="timelineExpired"
        :timeline-lookup-error="detailTimelineLookupError"
        :timeline-empty-title="detailTimelineEmptyTitle"
        :timeline-empty-description="detailTimelineEmptyDescription"
      />
    </StandardDetailDrawer>
  </StandardPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';

import { messageApi, type MessageTraceQueryParams } from '@/api/message';
import AccessErrorArchivePanel from '@/components/AccessErrorArchivePanel.vue';
import MessageTraceDetailWorkbench from '@/components/messageTrace/MessageTraceDetailWorkbench.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import StandardInlineState from '@/components/StandardInlineState.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardPageShell from '@/components/StandardPageShell.vue';
import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue';
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import {
  describeDiagnosticSource,
  resolveDiagnosticContext,
  type DiagnosticContext
} from '@/utils/iotAccessDiagnostics';
import type {
  DeviceMessageLog,
  MessageTraceDetail,
  MessageFlowTimeline,
  MessageTraceStats
} from '@/types/api';
import { resolveWorkbenchActionColumnWidth } from '@/utils/adaptiveActionColumn';
import { formatMessageTraceReportTime, truncateText } from '@/utils/format';
import { resolveMessageTracePayloadComparison } from '@/utils/messageTracePayloadComparison';

type ObservabilityViewMode = 'message-trace' | 'access-error';

const route = useRoute();
const messageTraceActionColumnWidth = resolveWorkbenchActionColumnWidth({
  directItems: [{ command: 'detail', label: '详情' }]
});
const pageModeOptions = [
  { key: 'message-trace', label: '链路追踪' },
  { key: 'access-error', label: '失败归档' }
];
const pageMode = computed<ObservabilityViewMode>(() =>
  route.query.mode === 'access-error' ? 'access-error' : 'message-trace'
);
const isAccessErrorMode = computed(() => pageMode.value === 'access-error');
const isMessageTraceMode = computed(() => pageMode.value === 'message-trace');

const messageTypeOptions = [
  { label: '属性上报', value: 'property' },
  { label: '事件上报', value: 'event' },
  { label: '状态上报', value: 'status' },
  { label: '命令回执', value: 'reply' },
  { label: '服务调用', value: 'service' }
];

const searchForm = reactive({
  keyword: '',
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});
const appliedFilters = reactive({
  keyword: '',
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});
const quickSearchKeyword = ref('');

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination();

const loading = ref(false);
const statsLoading = ref(false);
const timelineLoading = ref(false);
const tableData = ref<DeviceMessageLog[]>([]);
const detailVisible = ref(false);
const detailData = ref<Partial<MessageTraceDetail & DeviceMessageLog>>({});
const detailTimeline = ref<MessageFlowTimeline | null>(null);
const detailTimelineLookupError = ref(false);
let detailTimelineRequestToken = 0;
let detailRequestToken = 0;
const restoredDiagnosticContext = ref<DiagnosticContext | null>(null);
const createEmptyTraceStats = (): MessageTraceStats => ({
  total: 0,
  recentHourCount: 0,
  recent24HourCount: 0,
  distinctTraceCount: 0,
  distinctDeviceCount: 0,
  dispatchFailureCount: 0,
  topMessageTypes: [],
  topProductKeys: [],
  topDeviceCodes: [],
  topTopics: []
});
const traceStats = ref<MessageTraceStats>(createEmptyTraceStats());

const hasDetail = computed(() => Object.keys(detailData.value).length > 0);
const timelineExpired = computed(() =>
  Boolean(hasDetail.value && detailData.value.traceId && !timelineLoading.value && !detailTimeline.value && !detailTimelineLookupError.value)
);
const detailTimelineEmptyTitle = computed(() => {
  if (detailTimelineLookupError.value) {
    return 'message-flow 存储异常/Redis 不可用';
  }
  if (timelineExpired.value) {
    return '时间线已过期，仅保留消息日志';
  }
  if (detailData.value.traceId) {
    return '当前 trace 尚无可用时间线';
  }
  return '当前消息未携带 traceId';
});
const detailTimelineEmptyDescription = computed(() => {
  if (detailTimelineLookupError.value) {
    return '当前 trace 查询返回异常，优先排查 Redis 可用性与 message-flow 存储日志。';
  }
  if (timelineExpired.value) {
    return 'Redis 中的短期时间线已过期，但消息日志、Payload 和基础链路信息仍可继续排查。';
  }
  if (detailData.value.traceId) {
    return '正在等待时间线生成，或当前 trace 对应的 Redis 时间线不存在。';
  }
  return '没有 traceId 时，只能查看消息日志本身，无法继续拉取处理时间线。';
});
const detailTitle = computed(() => '链路追踪详情');
const detailSubtitle = computed(() => '');
const detailPayloadComparison = computed(() =>
  resolveMessageTracePayloadComparison({
    rawPayload: detailData.value.rawPayload ?? detailData.value.payload,
    decryptedPayload: detailData.value.decryptedPayload,
    decodedPayload: detailData.value.decodedPayload,
    timeline: detailTimeline.value,
    timelineExpired: timelineExpired.value
  })
);
const detailTags = computed(() => {
  if (!hasDetail.value) {
    return [];
  }
  return [];
});
const {
  tags: activeFilterTags,
  hasAppliedFilters: hasAppliedFilterTags,
  syncAppliedFilters,
  removeFilter: removeAppliedFilter
} = useListAppliedFilters({
  form: searchForm,
  applied: appliedFilters,
  fields: [
    { key: 'traceId', label: 'TraceId' },
    { key: 'deviceCode', label: '设备编码' },
    { key: 'productKey', label: '产品标识' },
    { key: 'messageType', label: (value) => `消息类型：${getMessageTypeLabel(value)}`, clearValue: '' },
    { key: 'topic', label: 'Topic' }
  ],
  defaults: {
    keyword: '',
    deviceCode: '',
    productKey: '',
    traceId: '',
    messageType: '',
    topic: ''
  }
});
const showAppliedFilters = computed(() =>
  Boolean(appliedFilters.keyword.trim()) || hasAppliedFilterTags.value
);
const traceRuleSummary = computed(() => {
  if (detailTimelineLookupError.value) {
    return '时间线查询异常，优先排查 Redis / message-flow 存储';
  }
  if (timelineExpired.value) {
    return '时间线已过期，可继续结合 Payload、异常观测与治理页排查。';
  }
  return '下一步按证据进入异常观测、数据校验或治理页。';
});
const traceInlineMessage = computed(() => {
  const contextSource = restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`
    : '';
  if (statsLoading.value) {
    return [contextSource, '当前节点：主链路复盘', '正在同步当前主链路复盘结果。'].filter(Boolean).join(' · ');
  }
  return [contextSource, '当前节点：主链路复盘', traceRuleSummary.value].filter(Boolean).join(' · ');
});
const showTraceInlineState = computed(() => Boolean(traceInlineMessage.value));
const traceInboundNotice = computed(() => {
  const context = restoredDiagnosticContext.value;
  if (!context || context.sourcePage !== 'insight' || context.reportStatus !== 'timeline-missing') {
    return null;
  }
  const pills = [
    context.deviceCode ? `设备 ${context.deviceCode}` : '',
    context.traceId ? `Trace ${context.traceId}` : '',
    context.topic ? '带 Topic 回看' : ''
  ].filter(Boolean);
  return {
    eyebrow: `来自${describeDiagnosticSource(context.sourcePage)}`,
    title: '当前正在补 latest 链路',
    description: '沿着 deviceCode、traceId 和 Topic 回到主链路复盘，先确认上报是否落到 message-flow 和 latest 写入链路，再决定是否回到对象洞察继续排查。',
    pills
  };
});

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.keyword;
}

function applyQuickSearchKeywordToFilters() {
  searchForm.keyword = quickSearchKeyword.value.trim();
}

function syncKeywordFilter() {
  appliedFilters.keyword = searchForm.keyword.trim();
}

function readQueryValue(key: keyof MessageTraceQueryParams) {
  const value = route.query[key];
  return typeof value === 'string' ? value : '';
}

function applyRouteQuery() {
  const resolvedContext = resolveDiagnosticContext(route.query as Record<string, unknown>);
  restoredDiagnosticContext.value = resolvedContext;
  searchForm.keyword = readQueryValue('keyword');
  searchForm.deviceCode = readQueryValue('deviceCode') || resolvedContext?.deviceCode || '';
  searchForm.productKey = readQueryValue('productKey') || resolvedContext?.productKey || '';
  searchForm.traceId = readQueryValue('traceId') || resolvedContext?.traceId || '';
  searchForm.messageType = readQueryValue('messageType');
  searchForm.topic = readQueryValue('topic') || resolvedContext?.topic || '';
  syncQuickSearchKeywordFromFilters();
}

function buildFilterQueryParams(): MessageTraceQueryParams {
  return {
    keyword: appliedFilters.keyword,
    deviceCode: appliedFilters.deviceCode,
    productKey: appliedFilters.productKey,
    traceId: appliedFilters.traceId,
    messageType: appliedFilters.messageType,
    topic: appliedFilters.topic
  };
}

function buildQueryParams(): MessageTraceQueryParams {
  return {
    ...buildFilterQueryParams(),
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  };
}

async function loadTableData() {
  if (!isMessageTraceMode.value) {
    return;
  }
  loading.value = true;
  try {
    const response = await messageApi.pageMessageTraceLogs(buildQueryParams());
    if (response.code === 200) {
      tableData.value = applyPageResult(response.data);
    }
  } catch (error) {
    tableData.value = [];
    resetTotal();
    ElMessage.error(error instanceof Error ? error.message : '获取链路追踪失败');
  } finally {
    loading.value = false;
  }
}

async function loadTraceStats() {
  if (!isMessageTraceMode.value) {
    return;
  }
  statsLoading.value = true;
  try {
    traceStats.value = createEmptyTraceStats();
    const response = await messageApi.pageMessageTraceStats(buildFilterQueryParams());
    if (response.code === 200 && response.data) {
      traceStats.value = { ...createEmptyTraceStats(), ...response.data };
    }
  } catch (error) {
    traceStats.value = createEmptyTraceStats();
    ElMessage.error(error instanceof Error ? error.message : '获取链路统计失败');
  } finally {
    statsLoading.value = false;
  }
}

function resetSearchForm() {
  searchForm.keyword = '';
  searchForm.deviceCode = '';
  searchForm.productKey = '';
  searchForm.traceId = '';
  searchForm.messageType = '';
  searchForm.topic = '';
  quickSearchKeyword.value = '';
}

function triggerSearch(resetPageFirst = false) {
  applyQuickSearchKeywordToFilters();
  syncAppliedFilters();
  syncKeywordFilter();
  if (resetPageFirst) {
    resetPage();
  }
  loadTableData();
  loadTraceStats();
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
  loadTraceStats();
}

function handleQuickSearch() {
  triggerSearch(true);
}

function handleClearQuickSearch() {
  quickSearchKeyword.value = '';
  triggerSearch(true);
}

function handleClearAppliedFilters() {
  handleReset();
}

function handleRemoveAppliedFilter(key: string) {
  removeAppliedFilter(key);
  syncQuickSearchKeywordFromFilters();
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

function getTraceDirectActions(_: DeviceMessageLog) {
  return [{ command: 'detail', label: '详情' }];
}

function handleTraceRowAction(command: string | number, row: DeviceMessageLog) {
  if (command === 'detail') {
    openDetail(row);
  }
}

function openDetail(row: DeviceMessageLog) {
  detailData.value = {
    ...row,
    rawPayload: row.payload,
    decryptedPayload: null,
    decodedPayload: null
  };
  detailVisible.value = true;
  detailTimelineLookupError.value = false;
  loadDetailRecord(row);
  loadDetailTimeline(row.traceId);
}

function getMessageTypeLabel(value?: string | null) {
  switch (value) {
    case 'report':
      return '属性上报';
    case 'event':
      return '事件上报';
    case 'status':
      return '状态上报';
    case 'reply':
      return '命令回执';
    case 'service':
      return '服务调用';
    case 'online':
      return '上线消息';
    case 'offline':
      return '离线消息';
    case 'property':
      return '属性上报';
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

function formatPayloadPreview(value?: string | null) {
  return truncateText(formatInlineText(value), 56);
}

function formatPayloadHoverText(value?: string | null) {
  return truncateText(formatInlineText(value), 88);
}

async function loadDetailTimeline(traceId?: string | null) {
  const requestToken = ++detailTimelineRequestToken;
  detailTimeline.value = null;
  detailTimelineLookupError.value = false;
  if (!traceId) {
    timelineLoading.value = false;
    return;
  }
  timelineLoading.value = true;
  try {
    const response = await messageApi.getMessageFlowTrace(traceId);
    if (requestToken !== detailTimelineRequestToken) {
      return;
    }
    if (response.code !== 200) {
      detailTimeline.value = null;
      detailTimelineLookupError.value = true;
      return;
    }
    detailTimeline.value = response.data || null;
  } catch (error) {
    if (requestToken !== detailTimelineRequestToken) {
      return;
    }
    detailTimeline.value = null;
    detailTimelineLookupError.value = true;
  } finally {
    if (requestToken === detailTimelineRequestToken) {
      timelineLoading.value = false;
    }
  }
}

async function loadDetailRecord(row: DeviceMessageLog) {
  const requestToken = ++detailRequestToken;
  try {
    const response = await messageApi.getMessageTraceDetail(row.id);
    if (requestToken !== detailRequestToken) {
      return;
    }
    if (response.code !== 200 || !response.data) {
      return;
    }
    detailData.value = {
      ...detailData.value,
      ...response.data,
      rawPayload: response.data.rawPayload || row.payload || detailData.value.rawPayload || detailData.value.payload
    };
  } catch (error) {
    if (requestToken !== detailRequestToken) {
      return;
    }
  }
}

watch(
  () => [
    route.query.mode,
    route.query.keyword,
    route.query.deviceCode,
    route.query.productKey,
    route.query.traceId,
    route.query.messageType,
    route.query.topic
  ],
  (current, previous) => {
    if (JSON.stringify(current) === JSON.stringify(previous)) {
      return;
    }
    if (!isMessageTraceMode.value) {
      return;
    }
    applyRouteQuery();
    resetPage();
    syncAppliedFilters();
    syncKeywordFilter();
    loadTableData();
    loadTraceStats();
  }
);

watch(detailVisible, (visible) => {
  if (!visible) {
    detailTimelineRequestToken += 1;
    detailData.value = {};
    detailTimeline.value = null;
    timelineLoading.value = false;
    detailTimelineLookupError.value = false;
  }
});

onMounted(() => {
  if (!isMessageTraceMode.value) {
    return;
  }
  applyRouteQuery();
  syncAppliedFilters();
  syncKeywordFilter();
  loadTableData();
  loadTraceStats();
});
</script>

<style scoped>
.message-trace-view {
  min-width: 0;
}

.message-trace-command-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.2rem;
  border-radius: var(--radius-xl);
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 249, 252, 0.96));
  box-shadow: var(--shadow-card);
}

.message-trace-command-strip__copy {
  min-width: 0;
}

.message-trace-command-strip__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.22rem;
}

.message-trace-command-strip__judgement {
  margin: 0.42rem 0 0;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.6;
}

.message-trace-command-strip__meta {
  margin: 0.3rem 0 0;
  color: var(--text-tertiary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.message-trace-command-strip__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.72rem;
}

.message-trace-quick-search-tag {
  margin-top: 0.72rem;
}

.message-trace-quick-search-tag__chip {
  margin: 0;
}

.message-trace-inbound-notice {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.9rem 1rem;
  border-radius: calc(var(--radius-lg) - 0.24rem);
  border: 1px solid color-mix(in srgb, var(--warning) 18%, var(--panel-border));
  background: linear-gradient(180deg, color-mix(in srgb, var(--warning) 8%, white), rgba(255, 255, 255, 0.96));
}

.message-trace-inbound-notice__copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.message-trace-inbound-notice__eyebrow {
  color: color-mix(in srgb, var(--warning) 72%, var(--text-secondary));
  font-size: 0.76rem;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.message-trace-inbound-notice__copy strong {
  color: var(--text-heading);
}

.message-trace-inbound-notice__copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.58;
}

.message-trace-inbound-notice__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.message-trace-inbound-notice__pill {
  display: inline-flex;
  align-items: center;
  min-height: 1.9rem;
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--warning) 12%, white);
  color: color-mix(in srgb, var(--warning) 78%, var(--text-primary));
  font-size: 0.76rem;
  font-weight: 600;
  white-space: nowrap;
}

.message-trace-table-wrap {
  min-width: 0;
}

.message-trace-mobile-list {
  display: none;
  margin-bottom: 0.72rem;
}

.message-trace-mobile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.72rem;
}

.message-trace-mobile-card__heading {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.message-trace-mobile-card__title {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.message-trace-mobile-card__sub {
  overflow: hidden;
  color: var(--text-caption);
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-trace-mobile-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.message-trace-mobile-card__meta-item {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-trace-mobile-card__info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem 0.9rem;
}

.message-trace-mobile-card__field {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.message-trace-mobile-card__field--full {
  grid-column: 1 / -1;
}

.message-trace-mobile-card__field .standard-mobile-record-card__field-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-trace-table {
  display: block;
}

.message-trace-payload-preview {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-trace-notice-grid {
  display: grid;
  gap: 0.72rem;
}

.message-trace-support-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
  gap: 0.9rem;
  margin-top: 0.85rem;
}

.message-trace-ops-card {
  min-width: 0;
}

.message-trace-ops-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.message-trace-ops-runtime {
  margin-top: 0.85rem;
  color: var(--text-tertiary);
  font-size: 0.78rem;
}

.message-trace-ops-empty {
  padding: 1rem 1.1rem;
  border-radius: var(--radius-lg);
  border: 1px dashed var(--line-soft);
  background: var(--surface-subtle);
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.message-trace-stage-table-wrapper {
  margin-top: 0.95rem;
  overflow-x: auto;
}

.message-trace-stage-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.82rem;
}

.message-trace-stage-table th,
.message-trace-stage-table td {
  padding: 0.68rem 0.72rem;
  border-bottom: 1px solid var(--line-soft);
  text-align: left;
  white-space: nowrap;
}

.message-trace-stage-table th {
  color: var(--text-tertiary);
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.message-trace-stage-table td {
  color: var(--text-secondary);
}

.message-trace-stage-table__empty {
  text-align: center;
  color: var(--text-tertiary);
}

.message-trace-recent-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.8rem;
}

.message-trace-recent-list {
  display: grid;
  gap: 0.72rem;
}

.message-trace-recent-item {
  display: grid;
  gap: 0.42rem;
  width: 100%;
  padding: 0.9rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line-soft);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(247, 249, 252, 0.98));
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.message-trace-recent-item:hover {
  border-color: color-mix(in srgb, var(--brand) 22%, var(--line-soft));
  transform: translateY(-1px);
  box-shadow: var(--shadow-report-card-hover);
}

.message-trace-recent-item__header,
.message-trace-recent-item__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.6rem;
}

.message-trace-recent-item__header strong {
  color: var(--text-heading);
  font-size: 0.88rem;
}

.message-trace-recent-item__header span,
.message-trace-recent-item__meta span {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.6;
}

@media (max-width: 1280px) {
  .message-trace-support-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .message-trace-command-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-trace-command-strip__actions {
    width: 100%;
    justify-content: flex-start;
  }

  .message-trace-ops-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .message-trace-mobile-list {
    display: block;
  }

  .message-trace-table {
    display: none;
  }

  .message-trace-mobile-card__info {
    grid-template-columns: 1fr;
  }

  .message-trace-ops-metrics {
    grid-template-columns: 1fr;
  }

  .message-trace-recent-item__header,
  .message-trace-recent-item__meta {
    flex-direction: column;
    align-items: flex-start;
  }
}

</style>
