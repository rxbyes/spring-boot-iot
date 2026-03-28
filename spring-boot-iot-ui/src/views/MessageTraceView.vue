<template>
  <div class="page-stack message-trace-view message-trace-view--minimal">
    <IotAccessTabWorkspace
      v-model="pageModeValue"
      :items="messageTraceViewTabs"
      default-key="message-trace"
      query-key="mode"
    >
      <template #default="{ activeKey }">
        <AccessErrorArchivePanel
          v-if="activeKey === 'access-error'"
          :view-mode="pageModeValue"
          :view-mode-options="pageModeOptions"
          :show-mode-switch="false"
          @change-view-mode="handlePageModeChange"
        />

        <StandardWorkbenchPanel
          v-else
          title="链路追踪"
          description=""
          title-variant="section"
          show-filters
          :show-applied-filters="hasAppliedFilters"
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
                <el-select v-model="searchForm.messageType" placeholder="消息类型" clearable>
                  <el-option
                    v-for="item in messageTypeOptions"
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
                  v-model="searchForm.topic"
                  placeholder="Topic"
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
          <div v-if="appliedFilters.traceId.trim()" class="message-trace-quick-search-tag">
            <el-tag closable class="message-trace-quick-search-tag__chip" @close="handleClearQuickSearch">
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

        <el-table
          v-loading="loading"
          class="message-trace-table"
          :data="tableData"
          border
          stripe
          style="width: 100%"
        >
          <StandardTableTextColumn prop="traceId" label="TraceId" :min-width="200" />
          <StandardTableTextColumn prop="deviceCode" label="设备编码" :min-width="140" />
          <StandardTableTextColumn prop="productKey" label="产品标识" :min-width="140" />
          <StandardTableTextColumn label="消息类型" :width="120">
            <template #default="{ row }">
              {{ getMessageTypeLabel(row.messageType) }}
            </template>
          </StandardTableTextColumn>
          <StandardTableTextColumn prop="topic" label="Topic" :min-width="220" />
          <StandardTableTextColumn label="Payload 摘要" :min-width="260">
            <template #default="{ row }">
              {{ formatInlineText(row.payload) }}
            </template>
          </StandardTableTextColumn>
          <StandardTableTextColumn label="上报时间" :width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.reportTime || row.createTime) }}
            </template>
          </StandardTableTextColumn>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <StandardRowActions variant="table" gap="wide">
                <StandardActionLink @click="openDetail(row)">详情</StandardActionLink>
                <StandardActionLink :disabled="!canJumpWithRow(row)" @click="jumpToSystemLog(row)">观测</StandardActionLink>
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
      </template>
    </IotAccessTabWorkspace>

    <StandardDetailDrawer
      v-model="detailVisible"
      eyebrow="链路追踪详情"
      :title="detailTitle"
      :subtitle="detailSubtitle"
      :tags="detailTags"
      :empty="!hasDetail"
    >
        <section class="detail-panel detail-panel--hero">
          <div class="detail-section-header">
            <div>
              <h3>消息概览</h3>
              <p>从消息类型、上报时间和 Topic 拓扑快速理解当前接入报文的上下文与排查入口。</p>
            </div>
          </div>
          <div class="detail-summary-grid">
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">消息类型</span>
              <strong class="detail-summary-card__value">{{ getMessageTypeLabel(detailData.messageType) }}</strong>
              <p class="detail-summary-card__hint">产品标识：{{ formatValue(detailData.productKey) }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">上报时间</span>
              <strong class="detail-summary-card__value">{{ detailDisplayTime }}</strong>
              <p class="detail-summary-card__hint">日志 ID：{{ formatValue(detailData.id) }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">设备编码</span>
              <strong class="detail-summary-card__value">{{ formatValue(detailData.deviceCode) }}</strong>
              <p class="detail-summary-card__hint">产品标识：{{ formatValue(detailData.productKey) }}</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">TraceId</span>
              <strong class="detail-summary-card__value">{{ formatValue(detailData.traceId) }}</strong>
              <p class="detail-summary-card__hint">可与异常观测台联动排查</p>
            </article>
            <article class="detail-summary-card">
              <span class="detail-summary-card__label">Topic 节点</span>
              <strong class="detail-summary-card__value">{{ detailTopicSegments }}</strong>
              <p class="detail-summary-card__hint">{{ formatValue(detailData.topic) }}</p>
            </article>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>链路信息</h3>
              <p>统一展示日志主键、TraceId、设备与 Topic，便于与异常观测台、接入智维页面联动定位。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-field__label">日志 ID</span>
              <strong class="detail-field__value">{{ formatValue(detailData.id) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">TraceId</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailData.traceId) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">设备编码</span>
              <strong class="detail-field__value">{{ formatValue(detailData.deviceCode) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">产品标识</span>
              <strong class="detail-field__value">{{ formatValue(detailData.productKey) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">消息类型</span>
              <strong class="detail-field__value">{{ getMessageTypeLabel(detailData.messageType) }}</strong>
            </div>
            <div class="detail-field">
              <span class="detail-field__label">上报时间</span>
              <strong class="detail-field__value">{{ detailDisplayTime }}</strong>
            </div>
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">Topic</span>
              <strong class="detail-field__value detail-field__value--plain">{{ formatValue(detailData.topic) }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>处理时间线</h3>
              <p>按 traceId 异步拉取 Redis 时间线，复盘固定 Pipeline 的阶段顺序、耗时与处理类/方法。</p>
            </div>
          </div>
          <StandardTraceTimeline
            :timeline="detailTimeline"
            :loading="timelineLoading"
            :empty-title="detailTimelineEmptyTitle"
            :empty-description="detailTimelineEmptyDescription"
          />
          <div v-if="timelineExpired" class="detail-notice">
            <span class="detail-notice__label">降级提示</span>
            <strong class="detail-notice__value">时间线已过期，仅保留消息日志。</strong>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-section-header">
            <div>
              <h3>消息内容</h3>
              <p>使用统一深色报文块承载 Payload，长 JSON、原始报文和多行内容在查看时更清晰。</p>
            </div>
          </div>
          <div class="detail-grid">
            <div class="detail-field detail-field--full">
              <span class="detail-field__label">Payload</span>
              <div class="detail-field__value detail-field__value--pre">{{ detailPayload }}</div>
            </div>
          </div>
          <div class="detail-notice">
            <span class="detail-notice__label">排查建议</span>
            <strong class="detail-notice__value">{{ detailRouteAdvice }}</strong>
          </div>
        </section>
    </StandardDetailDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { messageApi, type MessageTraceQueryParams } from '@/api/message';
import AccessErrorArchivePanel from '@/components/AccessErrorArchivePanel.vue';
import IotAccessTabWorkspace from '@/components/iotAccess/IotAccessTabWorkspace.vue';
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardButton from '@/components/StandardButton.vue';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardTraceTimeline from '@/components/StandardTraceTimeline.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import StandardActionLink from '@/components/StandardActionLink.vue';
import StandardRowActions from '@/components/StandardRowActions.vue';
import { useListAppliedFilters } from '@/composables/useListAppliedFilters';
import { useServerPagination } from '@/composables/useServerPagination';
import {
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext,
  type DiagnosticContext
} from '@/utils/iotAccessDiagnostics';
import type {
  DeviceMessageLog,
  MessageFlowOpsOverview,
  MessageFlowRecentSession,
  MessageFlowTimeline,
  MessageTraceStats
} from '@/types/api';
import { formatDateTime, prettyJson } from '@/utils/format';

type ObservabilityViewMode = 'message-trace' | 'access-error';

const route = useRoute();
const router = useRouter();
const pageModeOptions = [
  { label: '链路追踪', value: 'message-trace' as const },
  { label: '失败归档', value: 'access-error' as const }
];
const messageTraceViewTabs = [
  { key: 'message-trace', label: '链路追踪' },
  { key: 'access-error', label: '失败归档' }
];
const pageMode = computed<ObservabilityViewMode>(() =>
  route.query.mode === 'access-error' ? 'access-error' : 'message-trace'
);
const pageModeValue = computed<ObservabilityViewMode>({
  get: () => (route.query.mode === 'access-error' ? 'access-error' : 'message-trace'),
  set: (value) => {
    void handlePageModeChange(value);
  }
});
const isAccessErrorMode = computed(() => pageMode.value === 'access-error');
const isMessageTraceMode = computed(() => pageMode.value === 'message-trace');

const messageTypeOptions = [
  { label: '属性上报', value: 'report' },
  { label: '命令回执', value: 'reply' },
  { label: '上线消息', value: 'online' },
  { label: '离线消息', value: 'offline' }
];

const searchForm = reactive({
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});
const appliedFilters = reactive({
  deviceCode: '',
  productKey: '',
  traceId: '',
  messageType: '',
  topic: ''
});
const quickSearchKeyword = ref('');
const showAdvancedFilters = ref(false);

const { pagination, applyPageResult, resetPage, setPageSize, setPageNum, resetTotal } = useServerPagination();

const loading = ref(false);
const statsLoading = ref(false);
const timelineLoading = ref(false);
const opsLoading = ref(false);
const recentSessionsLoading = ref(false);
const tableData = ref<DeviceMessageLog[]>([]);
const detailVisible = ref(false);
const detailData = ref<Partial<DeviceMessageLog>>({});
const detailTimeline = ref<MessageFlowTimeline | null>(null);
const detailTimelineLookupError = ref(false);
let detailTimelineRequestToken = 0;
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
const createEmptyOpsOverview = (): MessageFlowOpsOverview => ({
  runtimeStartedAt: '',
  sessionCounts: [],
  correlationCounts: [],
  lookupCounts: [],
  stageMetrics: []
});
const opsOverview = ref<MessageFlowOpsOverview>(createEmptyOpsOverview());
const recentMessageFlowSessions = ref<MessageFlowRecentSession[]>([]);

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
const detailTitle = computed(() => detailData.value.deviceCode || detailData.value.traceId || '链路追踪详情');
const detailSubtitle = computed(() => detailData.value.topic || '查看接入消息详情');
const detailDisplayTime = computed(() => formatDateTime(detailData.value.reportTime || detailData.value.createTime));
const detailPayload = computed(() => prettyJson(detailData.value.payload || '--'));
const detailTopicSegments = computed(() => {
  if (!detailData.value.topic) {
    return '--';
  }
  return String(detailData.value.topic).split('/').filter(Boolean).length.toString();
});
const detailRouteAdvice = computed(() => {
  if (detailData.value.traceId) {
    return `可携带当前 TraceId（${detailData.value.traceId}）跳转异常观测台，继续联动排查消息链路。`;
  }
  if (detailData.value.topic) {
    return '可根据当前 Topic 跳转异常观测台，继续联动排查接入链路。';
  }
  return '可结合设备编码与产品标识继续检索异常观测台。';
});
const detailTags = computed(() => {
  if (!hasDetail.value) {
    return [];
  }
  return [
    { label: getMessageTypeLabel(detailData.value.messageType), type: 'primary' as const },
    ...(detailData.value.traceId ? [{ label: `Trace ${detailData.value.traceId}`, type: 'info' as const }] : [])
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
    { key: 'messageType', label: (value) => `消息类型：${getMessageTypeLabel(value)}`, clearValue: '' },
    { key: 'topic', label: 'Topic', advanced: true }
  ],
  defaults: {
    deviceCode: '',
    productKey: '',
    traceId: '',
    messageType: '',
    topic: ''
  }
});
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedCount.value === 0) {
    return '';
  }
  return `更多条件已生效 ${advancedAppliedCount.value} 项`;
});
const traceRuleSummary = computed(() => {
  if (detailTimelineLookupError.value) {
    return '时间线查询异常，优先排查 Redis / message-flow 存储';
  }
  if (timelineExpired.value) {
    return '时间线已过期';
  }
  if (appliedFilters.traceId && tableData.value.length > 0) {
    return '当前 Trace 可继续查 system_error';
  }
  if (!appliedFilters.traceId && recentMessageFlowSessions.value.length > 0) {
    return 'Trace 缺失，优先恢复最近会话';
  }
  return '先看 TraceId，再看最近会话';
});
const traceStripStatus = computed(() => {
  const contextSource = restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`
    : '';
  if (statsLoading.value) {
    return contextSource ? `${contextSource}，当前模式：链路追踪，统计加载中。` : '当前模式：链路追踪，统计加载中。';
  }
  const statsSummary = `当前模式：链路追踪，近1小时 ${traceStats.value.recentHourCount} 条，近24小时 ${traceStats.value.recent24HourCount} 条，失败摘要 ${traceStats.value.dispatchFailureCount} 条。`;
  return contextSource ? `${contextSource}，${statsSummary}` : statsSummary;
});
const opsOverviewMetrics = computed(() => {
  const completedCount = sumSessionCount('COMPLETED');
  const failedCount = sumSessionCount('FAILED');
  const publishedCount = sumCorrelationCount('published');
  const matchedCount = sumCorrelationCount('matched');
  const missedCount = sumCorrelationCount('missed');
  const lookupErrorCount = sumLookupCount('error');

  return [
    {
      label: '完成链路',
      value: formatCount(completedCount),
      badge: { label: 'COMPLETED', tone: 'success' as const }
    },
    {
      label: '失败链路',
      value: formatCount(failedCount),
      badge: { label: failedCount > 0 ? '需关注' : '稳定', tone: failedCount > 0 ? 'danger' as const : 'muted' as const }
    },
    {
      label: '关联发布',
      value: formatCount(publishedCount),
      badge: { label: 'PUBLISHED', tone: 'brand' as const }
    },
    {
      label: '关联命中',
      value: formatCount(matchedCount),
      badge: { label: matchedCount >= publishedCount && publishedCount > 0 ? '良好' : '持续观察', tone: matchedCount >= publishedCount && publishedCount > 0 ? 'success' as const : 'warning' as const }
    },
    {
      label: '关联超时',
      value: formatCount(missedCount),
      badge: { label: missedCount > 0 ? 'MISS' : '无超时', tone: missedCount > 0 ? 'warning' as const : 'muted' as const }
    },
    {
      label: 'Lookup 异常',
      value: formatCount(lookupErrorCount),
      badge: { label: lookupErrorCount > 0 ? '异常' : '正常', tone: lookupErrorCount > 0 ? 'danger' as const : 'success' as const }
    }
  ];
});

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.traceId;
}

function applyQuickSearchKeywordToFilters() {
  searchForm.traceId = quickSearchKeyword.value.trim();
}

function syncAdvancedFilterState() {
  showAdvancedFilters.value = Boolean(searchForm.topic.trim());
}

function handlePageModeChange(value: ObservabilityViewMode | string | number | boolean) {
  if (value !== 'message-trace' && value !== 'access-error') {
    return;
  }
  router.replace({
    path: '/message-trace',
    query: {
      ...route.query,
      mode: value === 'access-error' ? 'access-error' : undefined
    }
  });
}

function readQueryValue(key: keyof MessageTraceQueryParams) {
  const value = route.query[key];
  return typeof value === 'string' ? value : '';
}

function normalizeText(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function pickDiagnosticValue(...values: unknown[]) {
  for (const value of values) {
    const normalized = normalizeText(value);
    if (normalized) {
      return normalized;
    }
  }
  return undefined;
}

interface DiagnosticSourceCandidate {
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  topic?: string | null;
}

function buildVisibleDiagnosticContext(): DiagnosticContext {
  return {
    sourcePage: 'message-trace',
    traceId: pickDiagnosticValue(quickSearchKeyword.value),
    deviceCode: pickDiagnosticValue(searchForm.deviceCode),
    productKey: pickDiagnosticValue(searchForm.productKey),
    topic: pickDiagnosticValue(searchForm.topic),
    capturedAt: new Date().toISOString()
  };
}

function buildDiagnosticContext(source?: DiagnosticSourceCandidate): DiagnosticContext {
  const visibleContext = buildVisibleDiagnosticContext();
  return {
    ...visibleContext,
    traceId: pickDiagnosticValue(source?.traceId, visibleContext.traceId),
    deviceCode: pickDiagnosticValue(source?.deviceCode, visibleContext.deviceCode),
    productKey: pickDiagnosticValue(source?.productKey, visibleContext.productKey),
    topic: pickDiagnosticValue(source?.topic, visibleContext.topic),
    capturedAt: new Date().toISOString()
  };
}

function persistCurrentDiagnosticContext(source?: DiagnosticSourceCandidate) {
  persistDiagnosticContext(buildDiagnosticContext(source));
}

function applyRouteQuery() {
  const resolvedContext = resolveDiagnosticContext(route.query as Record<string, unknown>);
  restoredDiagnosticContext.value = resolvedContext;
  searchForm.deviceCode = readQueryValue('deviceCode') || resolvedContext?.deviceCode || '';
  searchForm.productKey = readQueryValue('productKey') || resolvedContext?.productKey || '';
  searchForm.traceId = readQueryValue('traceId') || resolvedContext?.traceId || '';
  searchForm.messageType = readQueryValue('messageType');
  searchForm.topic = readQueryValue('topic') || resolvedContext?.topic || '';
  syncQuickSearchKeywordFromFilters();
  syncAdvancedFilterState();
}

function buildFilterQueryParams(): MessageTraceQueryParams {
  return {
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

async function loadOpsOverview() {
  if (!isMessageTraceMode.value) {
    return;
  }
  opsLoading.value = true;
  try {
    opsOverview.value = createEmptyOpsOverview();
    const response = await messageApi.getMessageFlowOpsOverview();
    if (response.code === 200 && response.data) {
      opsOverview.value = {
        ...createEmptyOpsOverview(),
        ...response.data,
        sessionCounts: response.data.sessionCounts || [],
        correlationCounts: response.data.correlationCounts || [],
        lookupCounts: response.data.lookupCounts || [],
        stageMetrics: response.data.stageMetrics || []
      };
    }
  } catch (error) {
    opsOverview.value = createEmptyOpsOverview();
    ElMessage.error(error instanceof Error ? error.message : '获取 message-flow 运维指标失败');
  } finally {
    opsLoading.value = false;
  }
}

async function loadRecentMessageFlowSessions() {
  if (!isMessageTraceMode.value) {
    return;
  }
  recentSessionsLoading.value = true;
  try {
    recentMessageFlowSessions.value = [];
    const response = await messageApi.getMessageFlowRecentSessions({ size: 8 });
    if (response.code === 200) {
      recentMessageFlowSessions.value = response.data || [];
    }
  } catch (error) {
    recentMessageFlowSessions.value = [];
    ElMessage.error(error instanceof Error ? error.message : '获取最近 message-flow 会话失败');
  } finally {
    recentSessionsLoading.value = false;
  }
}

function resetSearchForm() {
  searchForm.deviceCode = '';
  searchForm.productKey = '';
  searchForm.traceId = '';
  searchForm.messageType = '';
  searchForm.topic = '';
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
  loadOpsOverview();
  loadRecentMessageFlowSessions();
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

function openDetail(row: DeviceMessageLog) {
  detailData.value = { ...row };
  detailVisible.value = true;
  detailTimelineLookupError.value = false;
  loadDetailTimeline(row.traceId);
}

function applyRecentMessageFlowSession(session: MessageFlowRecentSession) {
  resetSearchForm();
  searchForm.deviceCode = session.deviceCode || '';
  searchForm.topic = session.topic || '';
  if (session.traceId) {
    searchForm.traceId = session.traceId;
    quickSearchKeyword.value = session.traceId;
  } else {
    searchForm.traceId = '';
    quickSearchKeyword.value = '';
  }
  syncAdvancedFilterState();
  persistCurrentDiagnosticContext({
    traceId: session.traceId || null,
    deviceCode: session.deviceCode || null,
    topic: session.topic || null
  });
  triggerSearch(true);
}

function canJumpWithRow(row: DeviceMessageLog) {
  return Boolean(row.traceId || row.deviceCode || row.productKey || row.topic);
}

function jumpToSystemLog(row?: DeviceMessageLog) {
  const context = buildDiagnosticContext(row);
  persistCurrentDiagnosticContext(row);
  router.push({
    path: '/system-log',
    query: {
      traceId: context.traceId || undefined,
      deviceCode: context.deviceCode || undefined,
      productKey: context.productKey || undefined,
      requestUrl: context.topic || undefined,
      requestMethod: context.topic ? 'MQTT' : undefined
    }
  });
}

function jumpToAccessError(row?: DeviceMessageLog) {
  const context = buildDiagnosticContext(row);
  persistCurrentDiagnosticContext(row);
  router.push({
    path: '/message-trace',
    query: {
      mode: 'access-error',
      traceId: context.traceId || undefined,
      deviceCode: context.deviceCode || undefined,
      productKey: context.productKey || undefined,
      topic: context.topic || undefined
    }
  });
}

function getMessageTypeLabel(value?: string | null) {
  switch (value) {
    case 'report':
      return '属性上报';
    case 'reply':
      return '命令回执';
    case 'online':
      return '上线消息';
    case 'offline':
      return '离线消息';
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
      ElMessage.error(response.msg || '获取处理时间线失败');
      return;
    }
    detailTimeline.value = response.data || null;
  } catch (error) {
    if (requestToken !== detailTimelineRequestToken) {
      return;
    }
    detailTimeline.value = null;
    detailTimelineLookupError.value = true;
    ElMessage.error(error instanceof Error ? error.message : '获取处理时间线失败');
  } finally {
    if (requestToken === detailTimelineRequestToken) {
      timelineLoading.value = false;
    }
  }
}

function sumSessionCount(status: string) {
  return opsOverview.value.sessionCounts
    .filter((item) => String(item.status || '').toUpperCase() === status)
    .reduce((total, item) => total + Number(item.count || 0), 0);
}

function sumCorrelationCount(result: string) {
  return opsOverview.value.correlationCounts
    .filter((item) => String(item.result || '').toLowerCase() === result)
    .reduce((total, item) => total + Number(item.count || 0), 0);
}

function sumLookupCount(result: string) {
  return opsOverview.value.lookupCounts
    .filter((item) => String(item.result || '').toLowerCase() === result)
    .reduce((total, item) => total + Number(item.count || 0), 0);
}

function formatCount(value?: number | null) {
  return `${Number(value || 0)}`;
}

function formatCost(value?: number | null) {
  if (value === undefined || value === null) {
    return '--';
  }
  return `${Math.round(Number(value) * 10) / 10} ms`;
}

watch(
  () => [
    route.query.mode,
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
    loadTableData();
    loadTraceStats();
    loadOpsOverview();
    loadRecentMessageFlowSessions();
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
  loadTableData();
  loadTraceStats();
  loadOpsOverview();
  loadRecentMessageFlowSessions();
});
</script>

<style scoped>
.message-trace-view {
  min-width: 0;
}

.message-trace-view--minimal :deep(.standard-workbench-panel__title),
.message-trace-view--minimal :deep(.standard-workbench-panel__title--section) {
  letter-spacing: -0.02em;
}

.message-trace-view--minimal :deep(.table-action-bar__meta) {
  color: var(--text-caption);
}

.message-trace-workspace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem;
}

.message-trace-workspace-grid__section--focus,
.message-trace-panel-focus {
  border-radius: var(--radius-lg);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--brand) 14%, white);
}

.message-trace-panel-focus {
  padding: 0.14rem;
}

.message-trace-support-copy {
  display: grid;
  gap: 0.4rem;
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.65;
}

.message-trace-support-copy p {
  margin: 0;
}

@media (max-width: 900px) {
  .message-trace-workspace-grid {
    grid-template-columns: 1fr;
  }
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
