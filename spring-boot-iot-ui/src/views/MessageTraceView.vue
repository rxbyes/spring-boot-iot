<template>
  <div class="message-trace-view">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div>
            <span>消息追踪</span>
            <p class="page-description">按 TraceId、设备编码、产品标识与 Topic 串联设备接入消息链路。</p>
          </div>
        </div>
      </template>

      <el-form :model="searchForm" label-width="96px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="设备编码">
              <el-input
                v-model="searchForm.deviceCode"
                placeholder="请输入设备编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品标识">
              <el-input
                v-model="searchForm.productKey"
                placeholder="请输入产品标识"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="TraceId">
              <el-input
                v-model="searchForm.traceId"
                placeholder="请输入 TraceId"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="消息类型">
              <el-select v-model="searchForm.messageType" placeholder="请选择消息类型" clearable>
                <el-option
                  v-for="item in messageTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="Topic">
              <el-input
                v-model="searchForm.topic"
                placeholder="请输入 Topic 关键字"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" class="text-right">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </el-col>
        </el-row>
      </el-form>

      <el-alert
        title="消息追踪基于 `iot_device_message_log` 分页查询，可与系统日志页通过 TraceId、设备编码和 Topic 联动排查。"
        type="info"
        :closable="false"
        show-icon
        class="view-alert"
      />

      <div class="table-action-bar">
        <div class="table-action-bar__left">
          <span class="table-action-bar__meta">当前结果 {{ pagination.total }} 条</span>
        </div>
        <div class="table-action-bar__right">
          <el-button
            link
            :disabled="!canJumpWithSearch"
            @click="jumpToSystemLog()"
          >
            跳转系统日志
          </el-button>
          <el-button link @click="handleRefresh">刷新列表</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="traceId" label="TraceId" min-width="200" show-overflow-tooltip />
        <el-table-column prop="deviceCode" label="设备编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productKey" label="产品标识" min-width="140" show-overflow-tooltip />
        <el-table-column label="消息类型" width="120">
          <template #default="{ row }">
            {{ getMessageTypeLabel(row.messageType) }}
          </template>
        </el-table-column>
        <el-table-column prop="topic" label="Topic" min-width="220" show-overflow-tooltip />
        <el-table-column label="Payload 摘要" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncateText(row.payload || '--', 120) }}
          </template>
        </el-table-column>
        <el-table-column label="上报时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.reportTime || row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button type="primary" link :disabled="!canJumpWithRow(row)" @click="jumpToSystemLog(row)">系统日志</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />

      <StandardDetailDrawer
        v-model="detailVisible"
        eyebrow="Message Trace Detail"
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
              <p class="detail-summary-card__hint">可与系统日志联动排查</p>
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
              <p>统一展示日志主键、TraceId、设备与 Topic，便于与系统日志、设备接入页面联动定位。</p>
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { messageApi, type MessageTraceQueryParams } from '@/api/message';
import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue';
import type { DeviceMessageLog } from '@/types/api';
import { formatDateTime, prettyJson, truncateText } from '@/utils/format';

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

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const tableData = ref<DeviceMessageLog[]>([]);
const detailVisible = ref(false);
const detailData = ref<Partial<DeviceMessageLog>>({});

const hasDetail = computed(() => Object.keys(detailData.value).length > 0);
const detailTitle = computed(() => detailData.value.deviceCode || detailData.value.traceId || '消息追踪详情');
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
    return `可携带当前 TraceId（${detailData.value.traceId}）跳转系统日志，继续联动排查消息链路。`;
  }
  if (detailData.value.topic) {
    return '可根据当前 Topic 跳转系统日志，继续联动排查接入链路。';
  }
  return '可结合设备编码与产品标识继续检索系统日志。';
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
const canJumpWithSearch = computed(() => Boolean(searchForm.traceId || searchForm.deviceCode || searchForm.productKey || searchForm.topic));

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
      tableData.value = response.data?.records || [];
      pagination.total = Number(response.data?.total || 0);
    }
  } catch (error) {
    tableData.value = [];
    pagination.total = 0;
    ElMessage.error(error instanceof Error ? error.message : '获取消息追踪失败');
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
}

function handleSearch() {
  pagination.pageNum = 1;
  loadTableData();
}

function handleReset() {
  resetSearchForm();
  pagination.pageNum = 1;
  loadTableData();
}

function handleRefresh() {
  loadTableData();
}

function handleSizeChange(size: number) {
  pagination.pageSize = size;
  loadTableData();
}

function handlePageChange(page: number) {
  pagination.pageNum = page;
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
  const source = row || searchForm;
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
    pagination.pageNum = 1;
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
  loadTableData();
});
</script>

<style scoped>
.message-trace-view {
  padding: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-description {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.search-form {
  margin-bottom: 12px;
}

.view-alert {
  margin-bottom: 12px;
}

.text-right {
  text-align: right;
}

.table-action-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.table-action-bar__meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
