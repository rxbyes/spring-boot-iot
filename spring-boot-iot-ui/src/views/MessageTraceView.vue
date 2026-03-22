<template>
  <div class="message-trace-view">
    <StandardWorkbenchPanel
      title="链路追踪台"
      description="按 TraceId、设备编码、产品标识与 Topic 串联设备接入消息链路。"
      show-filters
      :show-applied-filters="hasAppliedFilters"
      show-notices
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
        <div v-if="quickSearchKeyword.trim()" class="message-trace-quick-search-tag">
          <el-tag closable class="message-trace-quick-search-tag__chip" @close="handleClearQuickSearch">
            快速搜索：{{ quickSearchKeyword.trim() }}
          </el-tag>
        </div>
      </template>

      <template #applied-filters>
        <StandardAppliedFiltersBar
          :tags="activeFilterTags"
          @remove="removeAppliedFilter"
          @clear="handleClearAppliedFilters"
        />
      </template>

      <template #notices>
        <el-alert
          title="链路追踪台基于 `iot_device_message_log` 分页查询，可与异常观测台通过 TraceId、设备编码和 Topic 联动排查。"
          type="info"
          :closable="false"
          show-icon
          class="view-alert"
        />
      </template>

      <template #toolbar>
        <StandardTableToolbar compact :meta-items="[ `当前结果 ${pagination.total} 条`, `当前页 ${tableData.length} 条` ]">
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
import StandardAppliedFiltersBar from '@/components/StandardAppliedFiltersBar.vue';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import StandardListFilterHeader from '@/components/StandardListFilterHeader.vue';
import StandardPagination from '@/components/StandardPagination.vue';
import StandardTableTextColumn from '@/components/StandardTableTextColumn.vue';
import StandardTableToolbar from '@/components/StandardTableToolbar.vue';
import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue';
import { useServerPagination } from '@/composables/useServerPagination';
import type { DeviceMessageLog } from '@/types/api';
import { formatDateTime, prettyJson } from '@/utils/format';

const route = useRoute();
const router = useRouter();

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
const tableData = ref<DeviceMessageLog[]>([]);
const detailVisible = ref(false);
const detailData = ref<Partial<DeviceMessageLog>>({});

const hasDetail = computed(() => Object.keys(detailData.value).length > 0);
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
const canJumpWithSearch = computed(() =>
  Boolean(quickSearchKeyword.value.trim() || searchForm.traceId || searchForm.deviceCode || searchForm.productKey || searchForm.topic)
);
const activeFilterTags = computed(() => {
  const tags: Array<{ key: keyof typeof appliedFilters; label: string }> = [];
  if (appliedFilters.traceId.trim()) {
    tags.push({ key: 'traceId', label: `TraceId：${appliedFilters.traceId.trim()}` });
  }
  if (appliedFilters.deviceCode.trim()) {
    tags.push({ key: 'deviceCode', label: `设备编码：${appliedFilters.deviceCode.trim()}` });
  }
  if (appliedFilters.productKey.trim()) {
    tags.push({ key: 'productKey', label: `产品标识：${appliedFilters.productKey.trim()}` });
  }
  if (appliedFilters.messageType) {
    tags.push({ key: 'messageType', label: `消息类型：${getMessageTypeLabel(appliedFilters.messageType)}` });
  }
  if (appliedFilters.topic.trim()) {
    tags.push({ key: 'topic', label: `Topic：${appliedFilters.topic.trim()}` });
  }
  return tags;
});
const hasAppliedFilters = computed(() => activeFilterTags.value.length > 0);
const advancedAppliedFilterCount = computed(() => (appliedFilters.topic.trim() ? 1 : 0));
const advancedFilterHint = computed(() => {
  if (showAdvancedFilters.value || advancedAppliedFilterCount.value === 0) {
    return '';
  }
  return `更多条件已生效 ${advancedAppliedFilterCount.value} 项`;
});

function syncAppliedFilters() {
  appliedFilters.deviceCode = searchForm.deviceCode.trim();
  appliedFilters.productKey = searchForm.productKey.trim();
  appliedFilters.traceId = searchForm.traceId.trim();
  appliedFilters.messageType = searchForm.messageType.trim();
  appliedFilters.topic = searchForm.topic.trim();
}

function syncQuickSearchKeywordFromFilters() {
  quickSearchKeyword.value = searchForm.traceId;
}

function applyQuickSearchKeywordToFilters() {
  searchForm.traceId = quickSearchKeyword.value.trim();
}

function syncAdvancedFilterState() {
  showAdvancedFilters.value = Boolean(searchForm.topic.trim());
}

function readQueryValue(key: keyof MessageTraceQueryParams) {
  const value = route.query[key];
  return typeof value === 'string' ? value : '';
}

function applyRouteQuery() {
  searchForm.deviceCode = readQueryValue('deviceCode');
  searchForm.productKey = readQueryValue('productKey');
  searchForm.traceId = readQueryValue('traceId');
  searchForm.messageType = readQueryValue('messageType');
  searchForm.topic = readQueryValue('topic');
  syncQuickSearchKeywordFromFilters();
  syncAdvancedFilterState();
}

function buildQueryParams(): MessageTraceQueryParams {
  return {
    deviceCode: searchForm.deviceCode,
    productKey: searchForm.productKey,
    traceId: searchForm.traceId,
    messageType: searchForm.messageType,
    topic: searchForm.topic,
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  };
}

async function loadTableData() {
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
  syncAppliedFilters();
  if (resetPageFirst) {
    resetPage();
  }
  loadTableData();
}

function handleSearch() {
  triggerSearch(true);
}

function handleReset() {
  resetSearchForm();
  triggerSearch(true);
}

function handleRefresh() {
  triggerSearch(false);
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

function removeAppliedFilter(key: keyof typeof appliedFilters) {
  switch (key) {
    case 'traceId':
      searchForm.traceId = '';
      break;
    case 'deviceCode':
      searchForm.deviceCode = '';
      break;
    case 'productKey':
      searchForm.productKey = '';
      break;
    case 'messageType':
      searchForm.messageType = '';
      break;
    case 'topic':
      searchForm.topic = '';
      break;
  }
  if (key === 'traceId') {
    syncQuickSearchKeywordFromFilters();
  }
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
}

function canJumpWithRow(row: DeviceMessageLog) {
  return Boolean(row.traceId || row.deviceCode || row.productKey || row.topic);
}

function jumpToSystemLog(row?: DeviceMessageLog) {
  const source = row || {
    traceId: quickSearchKeyword.value.trim() || searchForm.traceId,
    deviceCode: searchForm.deviceCode,
    productKey: searchForm.productKey,
    topic: searchForm.topic
  };
  router.push({
    path: '/system-log',
    query: {
      traceId: source.traceId || undefined,
      deviceCode: source.deviceCode || undefined,
      productKey: source.productKey || undefined,
      requestUrl: source.topic || undefined,
      requestMethod: source.topic ? 'MQTT' : undefined
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

watch(
  () => [
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
    applyRouteQuery();
    resetPage();
    syncAppliedFilters();
    loadTableData();
  }
);

watch(detailVisible, (visible) => {
  if (!visible) {
    detailData.value = {};
  }
});

onMounted(() => {
  applyRouteQuery();
  syncAppliedFilters();
  loadTableData();
});
</script>

<style scoped>
.message-trace-view {
  min-width: 0;
}

.message-trace-quick-search-tag {
  margin-top: 0.72rem;
}

.message-trace-quick-search-tag__chip {
  margin: 0;
}

</style>
