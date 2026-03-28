<template>
  <div class="page-stack file-payload-debug-view">
    <section class="file-payload-debug-command-strip">
      <div class="file-payload-debug-command-strip__copy">
        <h1 class="file-payload-debug-command-strip__title">数据校验台</h1>
        <p class="file-payload-debug-command-strip__judgement">先确定设备，再核对文件快照和固件聚合。</p>
        <p class="file-payload-debug-command-strip__meta">{{ validationStripStatus }}</p>
      </div>
      <div class="file-payload-debug-command-strip__actions">
        <StandardButton action="refresh" plain @click="handleOpenTraceWorkbench">链路追踪台</StandardButton>
      </div>
    </section>

    <StandardWorkbenchPanel
      title="设备查询与校验结果"
      description="保留单设备校验节奏，按快照、聚合和原始响应四段查看结果。"
      show-filters
      :show-inline-state="showInlineState"
    >
      <template #filters>
        <StandardListFilterHeader :model="{ deviceCode }">
          <template #primary>
            <el-form-item>
              <el-input
                id="file-debug-device-code"
                v-model="deviceCode"
                clearable
                placeholder="设备编码，例如 demo-device-01"
                prefix-icon="Search"
                @keyup.enter="refreshAll"
              />
            </el-form-item>
          </template>
          <template #actions>
            <StandardButton action="query" :loading="isLoading" :disabled="!normalizedDeviceCode" @click="refreshAll">
              {{ isLoading ? '加载中...' : '刷新数据' }}
            </StandardButton>
            <StandardButton action="reset" :disabled="isLoading" @click="handleReset">重置</StandardButton>
          </template>
        </StandardListFilterHeader>
      </template>

      <template #inline-state>
        <StandardInlineState :message="inlineStateMessage" :tone="inlineStateTone" />
      </template>

      <div class="page-stack">
        <section class="two-column-grid file-payload-debug-view__results">
          <PanelCard
            eyebrow="文件快照 C.3"
            title="文件快照校验"
            description="按时间线核对最近一次 C.3 文件消息是否完整落地。"
          >
            <div v-if="fileSnapshots.length" class="timeline">
              <article v-for="item in fileSnapshots" :key="item.transferId" class="timeline-item">
                <h3>{{ item.dataSetId || item.transferId }}</h3>
                <p>类型：{{ item.fileType || '--' }} / 长度：{{ item.binaryLength ?? '--' }}</p>
                <p>更新时间：{{ formatDateTime(item.updatedTime || item.timestamp) }}</p>
              </article>
            </div>
            <EmptyState
              v-else
              title="暂无文件快照"
              description="当前设备还没有返回 C.3 文件快照，可刷新后继续核查。"
            />
          </PanelCard>

          <PanelCard
            eyebrow="固件聚合 C.4"
            title="固件聚合校验"
            description="按分包进度、完成状态和 MD5 校验结果查看当前聚合情况。"
          >
            <div v-if="firmwareAggregates.length" class="timeline">
              <article v-for="item in firmwareAggregates" :key="item.transferId" class="timeline-item">
                <h3>{{ item.dataSetId || item.transferId }}</h3>
                <p>
                  分包：{{ item.receivedPacketCount ?? 0 }} / {{ item.totalPackets ?? '--' }}
                  <span v-if="item.receivedPacketIndexes?.length">（{{ item.receivedPacketIndexes.join(', ') }}）</span>
                </p>
                <p>聚合状态：{{ item.completed ? '已完成' : '进行中' }}</p>
                <p>MD5 校验：{{ formatMd5(item.md5Matched) }}</p>
                <p>更新时间：{{ formatDateTime(item.updatedTime || item.timestamp) }}</p>
              </article>
            </div>
            <EmptyState
              v-else
              title="暂无固件聚合记录"
              description="当前设备还没有返回 C.4 聚合结果，可刷新后继续核查。"
            />
          </PanelCard>
        </section>

        <section class="two-column-grid">
          <ResponsePanel
            eyebrow="文件快照响应"
            title="文件快照原始响应"
            :body="fileSnapshots"
          />
          <ResponsePanel
            eyebrow="固件聚合响应"
            title="固件聚合原始响应"
            :body="firmwareAggregates"
          />
        </section>
      </div>
    </StandardWorkbenchPanel>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getDeviceFileSnapshots, getDeviceFirmwareAggregates } from '../api/iot';
import EmptyState from '../components/EmptyState.vue';
import PanelCard from '../components/PanelCard.vue';
import ResponsePanel from '../components/ResponsePanel.vue';
import StandardInlineState from '../components/StandardInlineState.vue';
import StandardListFilterHeader from '../components/StandardListFilterHeader.vue';
import StandardWorkbenchPanel from '../components/StandardWorkbenchPanel.vue';
import { recordActivity } from '../stores/activity';
import type { DeviceFileSnapshot, DeviceFirmwareAggregate } from '../types/api';
import { formatDateTime } from '../utils/format';
import {
  describeDiagnosticSource,
  persistDiagnosticContext,
  resolveDiagnosticContext
} from '../utils/iotAccessDiagnostics';

const route = useRoute();
const router = useRouter();
const restoredDiagnosticContext = computed(() => resolveDiagnosticContext(route.query as Record<string, unknown>));
const defaultDeviceCode = computed(() => restoredDiagnosticContext.value?.deviceCode || 'demo-device-01');
const deviceCode = ref(defaultDeviceCode.value);
const isLoading = ref(false);
const errorMessage = ref('');
const lastFetchTime = ref<string | null>(null);
const fileSnapshots = ref<DeviceFileSnapshot[]>([]);
const firmwareAggregates = ref<DeviceFirmwareAggregate[]>([]);
const normalizedDeviceCode = computed(() => deviceCode.value.trim());
const inlineStateMessage = computed(() => {
  if (errorMessage.value) {
    return errorMessage.value;
  }
  if (lastFetchTime.value) {
    return `最近一次抓取：${formatDateTime(lastFetchTime.value)}，文件快照 ${fileSnapshots.value.length} 条，固件聚合 ${firmwareAggregates.value.length} 条。`;
  }
  return '';
});
const inlineStateTone = computed<'info' | 'error'>(() => (errorMessage.value ? 'error' : 'info'));
const showInlineState = computed(() => Boolean(inlineStateMessage.value));
const validationStripStatus = computed(() => {
  const sourceLabel = restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`
    : '';
  const summary = lastFetchTime.value
    ? `当前设备 ${normalizedDeviceCode.value || '--'}，文件快照 ${fileSnapshots.value.length} 条，固件聚合 ${firmwareAggregates.value.length} 条，最近刷新 ${formatDateTime(lastFetchTime.value)}。`
    : `当前设备 ${normalizedDeviceCode.value || '--'}，等待刷新校验结果。`;
  return [sourceLabel, summary].filter(Boolean).join(' · ');
});

function persistValidationContext(snapshotCount: number, aggregateCount: number) {
  persistDiagnosticContext({
    sourcePage: 'file-debug',
    deviceCode: normalizedDeviceCode.value,
    traceId: restoredDiagnosticContext.value?.traceId || undefined,
    productKey: restoredDiagnosticContext.value?.productKey || undefined,
    topic: restoredDiagnosticContext.value?.topic || undefined,
    reportStatus: snapshotCount || aggregateCount ? 'validated' : 'timeline-missing',
    capturedAt: new Date().toISOString()
  });
}

function formatMd5(value?: boolean | null) {
  if (value === true) {
    return '匹配';
  }
  if (value === false) {
    return '不匹配';
  }
  return '--';
}

async function refreshAll() {
  if (!normalizedDeviceCode.value) {
    errorMessage.value = '请输入设备编码后再刷新。';
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';

  try {
    const [snapshotResponse, firmwareResponse] = await Promise.all([
      getDeviceFileSnapshots(normalizedDeviceCode.value),
      getDeviceFirmwareAggregates(normalizedDeviceCode.value)
    ]);

    fileSnapshots.value = snapshotResponse.data;
    firmwareAggregates.value = firmwareResponse.data;
    lastFetchTime.value = new Date().toISOString();
    persistValidationContext(snapshotResponse.data.length, firmwareResponse.data.length);

    recordActivity({
      module: '数据校验台',
      action: '刷新校验数据',
      request: { deviceCode: normalizedDeviceCode.value },
      response: {
        fileSnapshots: snapshotResponse.data.length,
        firmwareAggregates: firmwareResponse.data.length
      },
      ok: true,
      detail: `设备 ${normalizedDeviceCode.value} 文件快照 ${snapshotResponse.data.length} 条，固件聚合 ${firmwareResponse.data.length} 条`
    });
  } catch (error) {
    errorMessage.value = (error as Error).message;
    persistValidationContext(0, 0);
    recordActivity({
      module: '数据校验台',
      action: '刷新校验数据',
      request: { deviceCode: normalizedDeviceCode.value },
      response: { message: errorMessage.value },
      ok: false,
      detail: `刷新失败：${errorMessage.value}`
    });
  } finally {
    isLoading.value = false;
  }
}

function handleReset() {
  deviceCode.value = defaultDeviceCode.value;
  errorMessage.value = '';
  lastFetchTime.value = null;
  fileSnapshots.value = [];
  firmwareAggregates.value = [];
}

function handleOpenTraceWorkbench() {
  router.push('/message-trace');
}
</script>

<style scoped>
.file-payload-debug-view {
  min-width: 0;
}

.file-payload-debug-command-strip {
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

.file-payload-debug-command-strip__copy {
  min-width: 0;
}

.file-payload-debug-command-strip__title {
  margin: 0;
  color: var(--text-heading);
  font-size: 1.22rem;
}

.file-payload-debug-command-strip__judgement {
  margin: 0.42rem 0 0;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.6;
}

.file-payload-debug-command-strip__meta {
  margin: 0.3rem 0 0;
  color: var(--text-tertiary);
  font-size: 0.8rem;
  line-height: 1.6;
}

.file-payload-debug-command-strip__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.72rem;
}

.file-payload-debug-view__results {
  align-items: start;
}

@media (max-width: 900px) {
  .file-payload-debug-command-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .file-payload-debug-command-strip__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
